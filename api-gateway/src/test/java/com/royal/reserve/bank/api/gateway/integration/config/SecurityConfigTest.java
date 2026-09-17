package com.royal.reserve.bank.api.gateway.integration.config;

import com.royal.reserve.bank.api.gateway.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link SecurityConfig} class using {@link WebTestClient}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
@AutoConfigureWebTestClient
class SecurityConfigTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityWebFilterChain securityWebFilterChain;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityWebFilterChain).isNotNull();
        assertThat(securityConfig.getJwkSetUri()).endsWith("/.well-known/jwks.json");
    }

    @Test
    void protectedRouteWithoutTokenIsUnauthorized() {
        webTestClient.get().uri("/api/account")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
    }

    @Test
    void protectedRouteWithInvalidTokenIsUnauthorized() {
        webTestClient.get().uri("/api/transaction")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, ".*invalid_token.*");
    }

    @Test
    void discoveryRoutesArePermitted() {
        webTestClient.get().uri("/discovery-server")
                .exchange()
                .expectStatus().value(status -> assertThat(status)
                        .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void jwtFilterCapturesBearerToken() {
        webTestClient.get().uri("/api/asset-management")
                .header(HttpHeaders.AUTHORIZATION, "Bearer captured-token")
                .exchange()
                .expectStatus().isUnauthorized();
        assertThat(securityConfig.getJwtToken()).isEqualTo("captured-token");
    }
}
