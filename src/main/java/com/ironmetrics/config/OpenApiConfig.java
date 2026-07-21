package com.ironmetrics.config;

import com.ironmetrics.auth.api.AuthController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI ironMetricsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Iron Metrics API")
                        .version("v1")
                        .description("REST API for body recomposition, strength training, and nutrition metrics.")
                        .contact(new Contact()
                                .name("Iron Metrics Engineering")
                                .email("engineering@ironmetrics.dev"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(new Server()
                        .url("/api/v1")
                        .description("Versioned REST API base path")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .security(List.of(new SecurityRequirement().addList(BEARER_AUTH)))
                .tags(List.of(
                        new Tag().name("Authentication").description("Registration and JWT login operations."),
                        new Tag().name("Exercises").description("Strength exercise catalog operations."),
                        new Tag().name("Workout Sessions").description("Workout session and set tracking operations."),
                        new Tag().name("Analytics").description("Read-only analytical views backed by the analytics schema.")
                ));
    }

    @Bean
    OperationCustomizer publicEndpointCustomizer() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            if (handlerMethod.getBeanType().equals(AuthController.class)) {
                operation.setSecurity(List.of());
            }

            return operation;
        };
    }
}
