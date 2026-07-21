package com.ironmetrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PowerBiContractDocumentationTest {

    @Test
    void shouldDocumentPowerBiAnalyticsContract() throws IOException {
        String contract = Files.readString(Path.of("docs/power-bi.md"));

        assertThat(contract)
                .contains("analytics.training_volume_daily")
                .contains("analytics.exercise_progression_daily")
                .contains("analytics.body_weight_daily")
                .contains("Relacionamentos")
                .contains("Medidas iniciais")
                .contains("iron_metrics_bi")
                .contains("iron_metrics_analytics_reader")
                .contains("read-only");
    }

    @Test
    void shouldProvideOperationalScriptForBiReadonlyUserWithoutHardcodedPassword() throws IOException {
        String script = Files.readString(Path.of("ops/postgres/create-bi-readonly-user.sql"));

        assertThat(script)
                .contains("iron_metrics_bi")
                .contains("iron_metrics_analytics_reader")
                .contains("GRANT iron_metrics_analytics_reader TO iron_metrics_bi")
                .contains("-v bi_password=");
        assertThat(script)
                .doesNotContain("iron_metrics_dev_password")
                .doesNotContain("Password123!");
    }
}
