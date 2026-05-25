package com.nexushr.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for the NexusHR platform.
 * Configures a shared {@link ObjectMapper} with JSR-310 (Java Time) support
 * and sensible defaults for JSON serialization/deserialization.
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates and configures the primary {@link ObjectMapper} bean.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Registers {@link JavaTimeModule} for {@code java.time.*} support</li>
     *   <li>Disables writing dates as timestamps (uses ISO-8601 strings instead)</li>
     *   <li>Ignores unknown properties during deserialization for forward compatibility</li>
     * </ul>
     *
     * @return the configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
