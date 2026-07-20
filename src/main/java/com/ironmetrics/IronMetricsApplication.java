package com.ironmetrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IronMetricsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IronMetricsApplication.class, args);
    }
}
