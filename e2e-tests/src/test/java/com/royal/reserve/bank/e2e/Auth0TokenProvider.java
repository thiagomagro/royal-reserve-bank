package com.royal.reserve.bank.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gets real client-credentials tokens from the live Auth0 tenant.
 *
 * <p>The tenant is alive and reachable for this suite, so there is intentionally no local-JWKS
 * fallback.</p>
 */
public final class Auth0TokenProvider {

    private final Path environmentFile;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String accessToken;

    public Auth0TokenProvider(Path repoRoot) {
        this.environmentFile = repoRoot.resolve("postman/postman-environment.json");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    public synchronized String token() {
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = requestToken();
        }
        return accessToken;
    }

    private String requestToken() {
        try {
            JsonNode environment = objectMapper.readTree(Files.readString(environmentFile));
            String provider = value(environment, "oidc_provider");
            String clientId = value(environment, "client_id");
            String secret = value(environment, "secret");
            String audience = value(environment, "audience");
            String requestBody = objectMapper.createObjectNode()
                    .put("client_id", clientId)
                    .put("client_secret", secret)
                    .put("audience", audience)
                    .put("grant_type", "client_credentials")
                    .toString();

            HttpRequest request = HttpRequest.newBuilder(URI.create(provider))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Auth0 tenant is unreachable for client-credentials token request (HTTP "
                                + response.statusCode() + ")");
            }
            String token = objectMapper.readTree(response.body()).path("access_token").asText();
            if (token.isBlank()) {
                throw new IllegalStateException("Auth0 tenant is unreachable: token response had no access_token");
            }
            return token;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof IllegalStateException illegalStateException
                    && illegalStateException.getMessage().startsWith("Auth0 tenant is unreachable")) {
                throw illegalStateException;
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "Auth0 tenant is unreachable; the suite requires the live Auth0 client-credentials endpoint", e);
        }
    }

    private String value(JsonNode environment, String key) {
        String value = null;
        for (JsonNode variable : environment.path("values")) {
            if (key.equals(variable.path("key").asText())) {
                value = variable.path("value").asText();
                break;
            }
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Auth0 tenant is unreachable: missing " + key + " in postman environment");
        }
        return value;
    }
}
