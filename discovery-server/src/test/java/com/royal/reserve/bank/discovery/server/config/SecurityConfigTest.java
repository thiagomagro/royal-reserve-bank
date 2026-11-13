package com.royal.reserve.bank.discovery.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.security.user.name=eureka",
        "spring.security.user.password=test-secret",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String INSTANCE_JSON = """
            {
              "instance": {
                "instanceId": "localhost:ACCOUNT-API:8080",
                "hostName": "localhost",
                "app": "ACCOUNT-API",
                "ipAddr": "127.0.0.1",
                "status": "UP",
                "port": {"$": 8080, "@enabled": "true"},
                "securePort": {"$": 443, "@enabled": "false"},
                "dataCenterInfo": {
                  "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                  "name": "MyOwn"
                }
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRegistryWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/eureka/apps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteInstanceWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/eureka/apps/ACCOUNT-API/rogue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRegistrationWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/eureka/apps/ACCOUNT-API")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSTANCE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRegistryWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/eureka/apps")
                        .with(httpBasic("eureka", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRegistryWithValidCredentialsReturnsOk() throws Exception {
        mockMvc.perform(get("/eureka/apps")
                        .with(httpBasic("eureka", "test-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void registrationWithValidCredentialsDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/eureka/apps/ACCOUNT-API")
                        .with(httpBasic("eureka", "test-secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSTANCE_JSON))
                .andExpect(status().isNoContent());
    }
}
