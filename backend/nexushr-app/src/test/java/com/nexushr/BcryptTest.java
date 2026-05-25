package com.nexushr;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    @Test
    public void testBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("=================================================");
        String[] candidates = {
            "NexusHR@2026",
            "admin123",
            "admin",
            "password",
            "123456",
            "12345678",
            "nexushr",
            "NexusHR",
            "NexusHR123",
            "NexusHR@123",
            "nexus",
            "nexus123"
        };
        for (String candidate : candidates) {
            if (encoder.matches(candidate, hash)) {
                System.out.println("MATCH FOUND: '" + candidate + "' matches the hash!");
            } else {
                System.out.println("No match for: '" + candidate + "'");
            }
        }
        System.out.println("New hash for NexusHR@2026: " + encoder.encode("NexusHR@2026"));
        System.out.println("New hash for admin123: " + encoder.encode("admin123"));
        System.out.println("=================================================");
    }
}
