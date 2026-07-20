package com.ironmetrics.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("iron_metrics_test")
            .withUsername("iron_metrics")
            .withPassword("iron_metrics_test_password");

    static {
        POSTGRES.start();
    }

    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/core");
    }
}
