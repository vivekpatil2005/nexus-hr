package com.nexushr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NexusHR — AI-Enabled Enterprise HR & Workforce Intelligence Platform.
 *
 * <p>Main entry point for the modular monolith Spring Boot application.
 * All modules are assembled here and scanned via the base package.</p>
 *
 * @author Zidio Development
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.nexushr")
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class NexusHrApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusHrApplication.class, args);
    }
}
