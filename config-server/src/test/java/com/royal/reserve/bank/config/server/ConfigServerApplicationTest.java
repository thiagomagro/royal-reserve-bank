package com.royal.reserve.bank.config.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link ConfigServerApplication} class.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.server.native.searchLocations=file:../config-files"
        })
class ConfigServerApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldServeAccountApiDefaultProfile() {
        ResponseEntity<Environment> response =
                restTemplate.getForEntity("/config-server/account-api/default", Environment.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Environment environment = response.getBody();
        assertThat(environment).isNotNull();
        assertThat(environment.getName()).isEqualTo("account-api");
        assertThat(environment.getPropertySources())
                .extracting(PropertySource::getSource)
                .anySatisfy(source -> assertThat(source.get("spring.data.redis.port"))
                        .hasToString("6379"));
    }
}
