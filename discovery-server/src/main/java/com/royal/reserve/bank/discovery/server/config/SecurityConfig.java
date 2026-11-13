package com.royal.reserve.bank.discovery.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures HTTP Basic security for the discovery server.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain. All endpoints require HTTP Basic with the configured
     * {@code spring.security.user} credentials; CSRF is ignored only for machine-to-machine
     * {@code /eureka/**} requests.
     *
     * @param httpSecurity the HttpSecurity object used for configuring the security filter chain
     * @return the configured SecurityFilterChain object
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return httpSecurity.build();
    }
}
