package com.nexushr.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NexusHrIntegrationTest {

    static PostgreSQLContainer<?> postgres;

    static {
        try {
            // Attempt to start Testcontainers if Docker environment is fully compatible
            postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("nexushr")
                    .withUsername("nexushr")
                    .withPassword("nexushr_dev_2026");
            postgres.start();
        } catch (Exception e) {
            // If Docker is incompatible or not running, fall back to the running docker-compose Postgres instance
            System.err.println("Testcontainers initialization failed: " + e.getMessage() + ". Falling back to local running Postgres container on port 5432.");
            postgres = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            // Fallback connection to the running postgres container on host
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/nexushr");
            registry.add("spring.datasource.username", () -> "nexushr");
            registry.add("spring.datasource.password", () -> "nexushr_dev_2026");
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void contextLoads() {
        assertNotNull(restTemplate);
    }

    @Test
    public void testActuatorHealth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testLoginWithSeededCredentials() {
        Map<String, String> loginRequest = Map.of(
                "usernameOrEmail", "admin",
                "password", "admin123"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginRequest, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
    }
}
