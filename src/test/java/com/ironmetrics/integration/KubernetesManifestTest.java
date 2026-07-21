package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class KubernetesManifestTest {

    private static final Path K8S_DIR = Path.of("k8s");

    @Test
    void shouldConfigureApiDeploymentWithVersionedProbesAndRuntimeEnvironment() throws IOException {
        Map<?, ?> deployment = manifest("api.yaml", "Deployment", "iron-metrics-api");
        Map<?, ?> service = manifest("api.yaml", "Service", "iron-metrics-api");
        Map<?, ?> configMap = manifest("configmap.yaml", "ConfigMap", "iron-metrics-api-config");
        Map<?, ?> secret = manifest("secret.yaml", "Secret", "iron-metrics-api-secret");

        Map<?, ?> container = namedMap(listAt(deployment, "spec", "template", "spec", "containers"), "name", "iron-metrics-api");

        assertThat(valueAt(container, "image")).isEqualTo("iron-metrics:local");
        assertThat(valueAt(container, "imagePullPolicy")).isEqualTo("IfNotPresent");
        assertThat(valueAt(container, "livenessProbe", "httpGet", "path"))
                .isEqualTo("/api/v1/actuator/health/liveness");
        assertThat(valueAt(container, "readinessProbe", "httpGet", "path"))
                .isEqualTo("/api/v1/actuator/health/readiness");
        assertThat(valueAt(container, "livenessProbe", "httpGet", "port")).isEqualTo("http");
        assertThat(valueAt(container, "readinessProbe", "httpGet", "port")).isEqualTo("http");
        assertThat(valueAt(service, "spec", "ports", 0, "targetPort")).isEqualTo("http");

        assertThat(listAt(container, "envFrom"))
                .anySatisfy(envFrom -> assertThat(valueAt(envFrom, "configMapRef", "name"))
                        .isEqualTo("iron-metrics-api-config"))
                .anySatisfy(envFrom -> assertThat(valueAt(envFrom, "secretRef", "name"))
                        .isEqualTo("iron-metrics-api-secret"));

        assertThat(valueAt(configMap, "data", "SPRING_PROFILES_ACTIVE")).isEqualTo("prod");
        assertThat(valueAt(configMap, "data", "IRON_METRICS_DB_URL"))
                .isEqualTo("jdbc:postgresql://iron-metrics-postgres:5432/iron_metrics");
        assertThat(valueAt(configMap, "data", "IRON_METRICS_DB_USER")).isEqualTo("iron_metrics");
        assertThat(mapAt(secret, "stringData").keySet().stream().map(Object::toString).toList())
                .contains("IRON_METRICS_DB_PASSWORD", "IRON_METRICS_JWT_SECRET");
    }

    @Test
    void shouldProvideSingleInstancePostgresAndKustomizationResourcesForMinikube() throws IOException {
        Map<?, ?> deployment = manifest("postgres.yaml", "Deployment", "iron-metrics-postgres");
        Map<?, ?> service = manifest("postgres.yaml", "Service", "iron-metrics-postgres");
        Map<?, ?> claim = manifest("postgres.yaml", "PersistentVolumeClaim", "iron-metrics-postgres-data");
        Map<?, ?> kustomization = loadAll("kustomization.yaml").getFirst();

        assertThat(valueAt(deployment, "spec", "replicas")).isEqualTo(1);
        Map<?, ?> postgres = namedMap(listAt(deployment, "spec", "template", "spec", "containers"), "name", "postgres");
        assertThat(valueAt(postgres, "image")).isEqualTo("postgres:16-alpine");
        assertThat(valueAt(postgres, "livenessProbe", "exec", "command")).isInstanceOf(List.class);
        assertThat(valueAt(postgres, "readinessProbe", "exec", "command")).isInstanceOf(List.class);
        assertThat(valueAt(service, "spec", "ports", 0, "port")).isEqualTo(5432);
        assertThat(valueAt(claim, "spec", "resources", "requests", "storage")).isEqualTo("1Gi");

        assertThat(listAt(kustomization, "resources").stream().map(Object::toString).toList())
                .containsExactlyInAnyOrder(
                        "namespace.yaml",
                        "configmap.yaml",
                        "secret.yaml",
                        "postgres.yaml",
                        "api.yaml"
                );
    }

    private Map<?, ?> manifest(String fileName, String kind, String name) throws IOException {
        return loadAll(fileName).stream()
                .filter(document -> kind.equals(document.get("kind")))
                .filter(document -> name.equals(valueAt(document, "metadata", "name")))
                .findFirst()
                .orElseGet(() -> fail("Expected %s/%s in %s".formatted(kind, name, fileName)));
    }

    private List<Map<?, ?>> loadAll(String fileName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(K8S_DIR.resolve(fileName))) {
            List<Map<?, ?>> documents = new ArrayList<>();
            new Yaml().loadAll(inputStream).forEach(document -> {
                if (document instanceof Map<?, ?> map) {
                    documents.add(map);
                }
            });
            return documents;
        }
    }

    private Map<?, ?> mapAt(Object source, Object... path) {
        Object value = valueAt(source, path);
        assertThat(value).isInstanceOf(Map.class);
        return (Map<?, ?>) value;
    }

    private List<?> listAt(Object source, Object... path) {
        Object value = valueAt(source, path);
        assertThat(value).isInstanceOf(List.class);
        return (List<?>) value;
    }

    private Object valueAt(Object source, Object... path) {
        Object current = source;
        for (Object segment : path) {
            if (segment instanceof Integer index) {
                assertThat(current).isInstanceOf(List.class);
                current = ((List<?>) current).get(index);
                continue;
            }

            assertThat(current).isInstanceOf(Map.class);
            current = ((Map<?, ?>) current).get(segment);
        }
        return current;
    }

    private Map<?, ?> namedMap(List<?> values, String key, String expectedName) {
        return values.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(value -> expectedName.equals(value.get(key)))
                .findFirst()
                .orElseGet(() -> fail("Expected item named %s".formatted(expectedName)));
    }
}
