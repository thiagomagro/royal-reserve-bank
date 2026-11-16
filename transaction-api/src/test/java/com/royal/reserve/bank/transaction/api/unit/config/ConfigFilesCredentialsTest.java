package com.royal.reserve.bank.transaction.api.unit.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFilesCredentialsTest {

    private static final Pattern ENVIRONMENT_VARIABLE = Pattern.compile("^\\$\\{[A-Z0-9_]+}$");
    private static final Pattern EUREKA_CREDENTIALS =
            Pattern.compile("^\\$\\{[A-Z0-9_]+}:\\$\\{[A-Z0-9_]+}$");

    @Test
    void configFilesMustNotCommitCredentials_sfind8f9e15f2() throws IOException {
        assertCredentialsAreEnvironmentVariables("../config-files/transaction-api.properties");
        assertCredentialsAreEnvironmentVariables("../config-files/transaction-api-docker.properties");
    }

    private void assertCredentialsAreEnvironmentVariables(String file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(file))) {
            properties.load(input);
        }

        assertEnvironmentVariableIfPresent(properties, "spring.datasource.username");
        assertEnvironmentVariableIfPresent(properties, "spring.datasource.password");

        String eurekaUrl = properties.getProperty("eureka.client.serviceUrl.defaultZone");
        if (eurekaUrl != null) {
            int schemeEnd = eurekaUrl.indexOf("//");
            int userInfoEnd = eurekaUrl.indexOf('@', schemeEnd + 2);
            if (schemeEnd >= 0 && userInfoEnd >= 0) {
                String userInfo = eurekaUrl.substring(schemeEnd + 2, userInfoEnd);
                assertTrue(EUREKA_CREDENTIALS.matcher(userInfo).matches(),
                        () -> "Eureka credentials must use environment variables in " + file);
            }
        }
    }

    private void assertEnvironmentVariableIfPresent(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            assertTrue(ENVIRONMENT_VARIABLE.matcher(value).matches(),
                    () -> key + " must use an environment variable");
        }
    }
}
