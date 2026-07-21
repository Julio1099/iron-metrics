package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContainerBuildConfigurationTest {

    @Test
    void shouldUseMultiStageDockerBuildWithJava21Runtime() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"));

        assertThat(dockerfile)
                .contains("FROM maven:")
                .contains("AS build")
                .contains("FROM eclipse-temurin:21-jre-alpine")
                .contains("COPY --from=build")
                .contains("USER ironmetrics")
                .contains("EXPOSE 8080");
    }

    @Test
    void shouldExcludeGeneratedAndLocalFilesFromDockerBuildContext() throws IOException {
        String dockerignore = Files.readString(Path.of(".dockerignore"));

        assertThat(dockerignore)
                .contains("target")
                .contains(".git")
                .contains(".idea")
                .contains(".env")
                .contains("*.log");
    }
}
