package com.royal.reserve.bank.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public final class StackExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(StackExtension.class);
    private static final Set<String> REQUIRED_SERVICES = Set.of(
            "ACCOUNT-API",
            "TRANSACTION-API",
            "ASSET-MANAGEMENT-API",
            "NOTIFICATION-API",
            "API-GATEWAY"
    );

    private final DockerCompose dockerCompose;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StackExtension() {
        this(Path.of(System.getProperty("e2e.repoRoot", ".")).toAbsolutePath().normalize());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store rootStore = context.getRoot().getStore(NAMESPACE);
        rootStore.getOrComputeIfAbsent(StackExtension.class, key -> {
            StackExtension resource = new StackExtension(repoRoot());
            resource.start();
            return resource;
        }, StackExtension.class);
    }

    private StackExtension(Path repoRoot) {
        this.dockerCompose = new DockerCompose(repoRoot);
    }

    private Path repoRoot() {
        return Path.of(System.getProperty("e2e.repoRoot", ".")).toAbsolutePath().normalize();
    }

    private void start() {
        if (Boolean.getBoolean("e2e.skipCompose")) {
            return;
        }
        dockerCompose.down();
        dockerCompose.up();
        Awaitility.await()
                .atMost(Duration.ofMinutes(8))
                .pollInterval(Duration.ofSeconds(5))
                .ignoreExceptions()
                .until(this::allRequiredServicesUp);
    }

    private boolean allRequiredServicesUp() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080/eureka/apps"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            JsonNode applications = objectMapper.readTree(response.body()).path("applications").path("application");
            Set<String> upServices = new HashSet<>();
            for (JsonNode application : applications) {
                if (REQUIRED_SERVICES.contains(application.path("name").asText())
                        && "UP".equals(application.path("instance").path(0).path("status").asText())) {
                    upServices.add(application.path("name").asText());
                }
            }
            return upServices.containsAll(REQUIRED_SERVICES);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public void close() {
        if (!Boolean.getBoolean("e2e.skipCompose") && !Boolean.getBoolean("e2e.keepStack")) {
            dockerCompose.down();
        }
    }
}
