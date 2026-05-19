package com.example.mentalcompanion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Excel excel, Mail mail) {

    public record Excel(String workflowPath, String exportDir) {
    }

    public record Mail(boolean enabled, String alertReceiver) {
    }
}

