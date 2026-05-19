package com.example.mentalcompanion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.example.mentalcompanion.mapper")
@ConfigurationPropertiesScan
public class MentalCompanionAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentalCompanionAssistantApplication.class, args);
    }
}

