package com.nexushr.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper for list endpoints.
 * Mirrors Spring Data's {@link org.springframework.data.domain.Page} structure
 * in a serialization-friendly format.
 *
 * @param <T> the type of elements in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    /** The page content. */
    private List<T> content;

    /** Zero-based page index. */
    private int page;

    /** Requested page size. */
    private int size;

    /** Total number of elements across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Whether this is the last page. */
    private boolean last;
}
