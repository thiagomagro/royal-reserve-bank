package com.royal.reserve.bank.api.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Global filter that propagates the authenticated identity to downstream services.
 * Any client-supplied identity headers are always stripped so they cannot be spoofed;
 * when the caller authenticated with a JWT, its subject and permissions are forwarded
 * via the {@link AuthHeaders#SUBJECT} and {@link AuthHeaders#PERMISSIONS} headers.
 */
@Component
public class AuthenticatedPrincipalHeadersFilter implements GlobalFilter, Ordered {

    /**
     * Runs after the security filter chain, which executes before gateway filters.
     *
     * @return the order of this filter
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Strips spoofable identity headers and, for JWT-authenticated requests, sets them
     * from the authenticated token before forwarding the request downstream.
     *
     * @param exchange the current server exchange
     * @param chain    the gateway filter chain
     * @return a Mono signalling when the filter chain completes
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange strippedExchange = stripIdentityHeaders(exchange);
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                        return chain.filter(withIdentityHeaders(strippedExchange, jwtAuthentication.getToken()));
                    }
                    return chain.filter(strippedExchange);
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(strippedExchange)));
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(AuthHeaders.SUBJECT);
                    headers.remove(AuthHeaders.PERMISSIONS);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(AuthHeaders.SUBJECT, jwt.getSubject())
                .header(AuthHeaders.PERMISSIONS, String.join(",", permissionsOf(jwt)))
                .build();
        return exchange.mutate().request(request).build();
    }

    private Set<String> permissionsOf(Jwt jwt) {
        Set<String> permissions = new LinkedHashSet<>();
        List<String> permissionsClaim = jwt.getClaimAsStringList("permissions");
        if (permissionsClaim != null) {
            permissions.addAll(permissionsClaim);
        }
        String scopeClaim = jwt.getClaimAsString("scope");
        if (scopeClaim != null) {
            permissions.addAll(Arrays.asList(scopeClaim.split(" ")));
        }
        return permissions;
    }
}
