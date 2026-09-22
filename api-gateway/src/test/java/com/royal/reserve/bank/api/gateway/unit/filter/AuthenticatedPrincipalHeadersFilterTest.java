package com.royal.reserve.bank.api.gateway.unit.filter;

import com.royal.reserve.bank.api.gateway.filter.AuthHeaders;
import com.royal.reserve.bank.api.gateway.filter.AuthenticatedPrincipalHeadersFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the {@link AuthenticatedPrincipalHeadersFilter} class.
 */
class AuthenticatedPrincipalHeadersFilterTest {

    private final AuthenticatedPrincipalHeadersFilter filter = new AuthenticatedPrincipalHeadersFilter();

    /**
     * Test that client-supplied identity headers are replaced with the JWT subject and permissions.
     */
    @Test
    void testFilterWithJwtAuthentication() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/account")
                .header(AuthHeaders.SUBJECT, "evil")
                .header(AuthHeaders.PERMISSIONS, "accounts:admin")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("auth0|alice")
                .claim("permissions", List.of("accounts:read", "accounts:admin"))
                .claim("scope", "openid profile")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();

        // When
        filter.filter(exchange, e -> {
            forwardedHeaders.set(e.getRequest().getHeaders());
            return reactor.core.publisher.Mono.empty();
        })
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        .block();

        // Then
        assertEquals("auth0|alice", forwardedHeaders.get().getFirst(AuthHeaders.SUBJECT));
        assertEquals("accounts:read,accounts:admin,openid,profile",
                forwardedHeaders.get().getFirst(AuthHeaders.PERMISSIONS));
    }

    /**
     * Test that client-supplied identity headers are removed when there is no authentication.
     */
    @Test
    void testFilterWithoutAuthenticationStripsSpoofedHeaders() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/account")
                .header(AuthHeaders.SUBJECT, "evil")
                .header(AuthHeaders.PERMISSIONS, "accounts:admin")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();

        // When
        filter.filter(exchange, e -> {
            forwardedHeaders.set(e.getRequest().getHeaders());
            return reactor.core.publisher.Mono.empty();
        }).block();

        // Then
        assertNull(forwardedHeaders.get().getFirst(AuthHeaders.SUBJECT));
        assertNull(forwardedHeaders.get().getFirst(AuthHeaders.PERMISSIONS));
        assertFalse(forwardedHeaders.get().containsKey(AuthHeaders.SUBJECT));
    }
}
