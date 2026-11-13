package com.royal.reserve.bank.config.server.integration;

import com.royal.reserve.bank.config.server.ConfigServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Config Server authentication.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.user.password=test-secret",
                "eureka.client.enabled=false",
                "spring.cloud.config.server.native.searchLocations=file:../config-files"
        })
class ConfigServerSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void configIsUnauthorizedWithoutCredentials() {
        assertThat(restTemplate.getForEntity("/config-server/transaction-api/default", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void configIsUnauthorizedWithWrongPassword() {
        TestRestTemplate client = restTemplate.withBasicAuth("config-client", "wrong-password");

        assertThat(client.getForEntity("/config-server/transaction-api/default", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void configIsAvailableWithValidCredentials() {
        TestRestTemplate client = restTemplate.withBasicAuth("config-client", "test-secret");

        assertThat(client.getForEntity("/config-server/transaction-api/default", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void prometheusEndpointIsAvailableWithoutCredentials() {
        assertThat(restTemplate.getForEntity("/actuator/prometheus", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void applicationDoesNotStartWithBlankPassword() {
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ConfigServerApplication.class)
                    .run(
                            "--spring.security.user.password=",
                            "--eureka.client.enabled=false",
                            "--server.port=0",
                            "--spring.cloud.config.server.native.searchLocations=file:../config-files")) {
            }
        }).hasRootCauseMessage("CONFIG_SERVER_PASSWORD must be set");
    }
}
