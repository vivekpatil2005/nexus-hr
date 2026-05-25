package com.nexushr.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper used across all NexusHR modules.
 * Provides a consistent response structure with success status, message, data payload,
 * and timestamp.
 *
 * @param <T> the type of the response data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a successful response with the given data payload.
     *
     * @param data the response data
     * @param <T>  the type of the data
     * @return a successful {@link ApiResponse} containing the data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Creates a successful response with a message and data payload.
     *
     * @param message a human-readable success message
     * @param data    the response data
     * @param <T>     the type of the data
     * @return a successful {@link ApiResponse} containing the message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with the given error message.
     *
     * @param message a human-readable error message
     * @param <T>     the type of the data (will be null)
     * @return an error {@link ApiResponse} containing the message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
