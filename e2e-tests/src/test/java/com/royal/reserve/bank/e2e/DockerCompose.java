package com.royal.reserve.bank.e2e;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DockerCompose {

    private final Path repoRoot;

    public DockerCompose(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public void up() {
        runCompose(List.of("up", "-d"));
    }

    public void down() {
        runCompose(List.of("down", "-v", "--remove-orphans"));
    }

    public void stop(String service) {
        runCompose(List.of("stop", service));
    }

    public void start(String service) {
        runCompose(List.of("start", service));
    }

    public String logs(String service) {
        return run(List.of("docker", "logs", service), false);
    }

    private void runCompose(List<String> arguments) {
        List<String> command = new ArrayList<>(composeCommand());
        command.addAll(arguments);
        run(command, true);
    }

    private List<String> composeCommand() {
        return List.of(
                "docker",
                "compose",
                "-f",
                repoRoot.resolve("docker-compose.yml").toString(),
                "-f",
                repoRoot.resolve("docker-compose.e2e.yml").toString()
        );
    }

    private String run(List<String> command, boolean inheritOutput) {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(repoRoot.toFile())
                .redirectErrorStream(true);
        if (inheritOutput) {
            processBuilder.inheritIO();
        }
        try {
            Process process = processBuilder.start();
            String output = inheritOutput ? "" : read(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Command failed with exit code " + exitCode + ": " + String.join(" ", command)
                                + (output.isBlank() ? "" : "\n" + output));
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to run command: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running command: " + String.join(" ", command), e);
        }
    }

    private String read(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toString();
        }
    }
}
