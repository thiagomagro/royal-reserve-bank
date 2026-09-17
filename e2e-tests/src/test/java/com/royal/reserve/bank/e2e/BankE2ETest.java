package com.royal.reserve.bank.e2e;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(StackExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class BankE2ETest {

    private static final String FALLBACK_MESSAGE = "Oops! Something went wrong, please try again later!";
    private static DockerCompose dockerCompose;
    private static Auth0TokenProvider tokenProvider;

    @BeforeAll
    static void setUp() {
        Path repoRoot = Path.of(System.getProperty("e2e.repoRoot", ".")).toAbsolutePath().normalize();
        dockerCompose = new DockerCompose(repoRoot);
        tokenProvider = new Auth0TokenProvider(repoRoot);
        RestAssured.baseURI = "http://localhost:8080";
        RestAssured.config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 30_000)
                        .setParam("http.socket.timeout", 30_000)
                        .setParam("http.connection-manager.timeout", 30_000));
    }

    @Test
    @Order(1)
    void shouldObtainAuth0Token() {
        String token = tokenProvider.token();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        given()
                .when()
                .get("/api/account")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    @Order(2)
    void shouldListAllServicesUpInEureka() {
        Response eureka = given()
                .header("Accept", "application/json")
                .when()
                .get("/eureka/apps")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();
        List<Map<String, Object>> applications = eureka.jsonPath().getList("applications.application");
        for (String serviceName : List.of("ACCOUNT-API", "TRANSACTION-API", "ASSET-MANAGEMENT-API",
                "NOTIFICATION-API", "API-GATEWAY")) {
            assertServiceUp(applications, serviceName);
        }

        given()
                .when()
                .get("/discovery-server")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("Eureka"));
    }

    @Test
    @Order(3)
    void shouldServeConfigThroughGateway() {
        Response response = authenticated()
                .when()
                .get("/config-server/account-api/default")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();
        List<Map<String, Object>> propertySources = response.jsonPath().getList("propertySources");
        assertThat(propertySources).isNotEmpty();
        assertThat(propertySources.stream()
                .map(source -> (Map<?, ?>) source.get("source"))
                .anyMatch(source -> source.containsKey("spring.data.mongodb.uri"))).isTrue();
        assertThat(response.jsonPath().getString("name")).isEqualTo("account-api");
    }

    @Test
    @Order(4)
    void shouldCreateAndReadAccount() {
        String holderName = "E2E-" + UUID.randomUUID();
        Response createResponse = Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> authenticated()
                        .contentType(ContentType.JSON)
                        .body(Map.of(
                                "accountHolderName", holderName,
                                "balance", 1000.50,
                                "currency", "USD"))
                        .when()
                        .post("/api/account"), response -> response.statusCode() == 201);
        createResponse.then()
                .log().ifValidationFails()
                .body(org.hamcrest.Matchers.containsString(holderName));

        Response response = authenticated()
                .when()
                .get("/api/account")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();
        List<Map<String, Object>> accounts = response.jsonPath().getList("");
        assertThat(accounts).anySatisfy(account ->
                assertThat(account.get("accountHolderName")).isEqualTo(holderName));
    }

    @Test
    @Order(5)
    void shouldReturnAssetAvailability() {
        Response response = authenticated()
                .queryParam("assetCode", "BTC")
                .queryParam("assetCode", "SEC")
                .when()
                .get("/api/asset-management")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().response();
        List<Map<String, Object>> assets = response.jsonPath().getList("");
        assertThat(assets).hasSize(2);
        assertThat(assets).allSatisfy(asset -> assertThat(asset.get("assetAvailable")).isEqualTo(true));
    }

    @Test
    @Order(6)
    void shouldProcessTransactionAndNotify() {
        int notificationsBefore = countNotificationLines();
        Response transactionResponse = Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(() -> authenticated()
                        .contentType(ContentType.JSON)
                        .body(Map.of("transactionItemsDtoList", List.of(Map.of(
                                "assetCode", "BTC",
                                "assetName", "Bitcoin",
                                "value", 1000))))
                        .when()
                        .post("/api/transaction"), response ->
                        response.statusCode() == 201
                                && "Transaction completed successfully!".equals(response.asString()));
        transactionResponse.then()
                .log().ifValidationFails()
                .body(org.hamcrest.Matchers.equalTo("Transaction completed successfully!"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> countNotificationLines() > notificationsBefore);
    }

    @Test
    @Order(7)
    void shouldFallbackWhenAssetManagementIsDown() {
        dockerCompose.stop("asset-management-api");
        Awaitility.await()
                .atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    Response response = authenticated()
                            .contentType(ContentType.JSON)
                            .body(Map.of("transactionItemsDtoList", List.of(Map.of(
                                    "assetCode", "BTC",
                                    "assetName", "Bitcoin",
                                    "value", 1000))))
                            .when()
                            .post("/api/transaction");
                    return response.statusCode() == 201 && FALLBACK_MESSAGE.equals(response.asString());
                });

        given()
                .when()
                .get("http://localhost:8082/actuator/circuitbreakers")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("circuitBreakers.asset-management.state", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString()));

        given()
                .when()
                .get("http://localhost:8082/actuator/health")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("components.circuitBreakers", org.hamcrest.Matchers.notNullValue());
    }

    @AfterAll
    static void restoreAssetManagement() {
        if (dockerCompose != null) {
            dockerCompose.start("asset-management-api");
        }
    }

    private io.restassured.specification.RequestSpecification authenticated() {
        return given().auth().oauth2(tokenProvider.token());
    }

    private int countNotificationLines() {
        return (int) dockerCompose.logs("notification-api").lines()
                .filter(line -> line.contains("Received notification for transaction"))
                .count();
    }

    private void assertServiceUp(List<Map<String, Object>> applications, String serviceName) {
        assertThat(applications)
                .as("Eureka registration for %s", serviceName)
                .anySatisfy(application -> {
                    assertThat(application.get("name")).isEqualTo(serviceName);
                    assertThat(application.get("instance").toString()).contains("status=UP");
                });
    }
}
