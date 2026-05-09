package com.jk.User_Profile_Hub.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Standard paginated response
 * Wraps Spring Data Page into a consistent DTO
 *
 * @param <T> Type of content items
 * @author LastCoderBoy
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    /**
     * Constructor for simple list response without pagination metadata
     *
     * @param content list of items
     */
    public PaginatedResponse(List<T> content) {
        this.content = content;
        this.totalElements = content != null ? content.size() : 0;
        this.totalPages = content != null && !content.isEmpty() ? 1 : 0;
        this.currentPage = 0;
        this.pageSize = content != null ? content.size() : 0;
    }

    public static <T> PaginatedResponse<T> fromPage(Page<T> page) {
        if (page == null) {
            return PaginatedResponse.empty();
        }

        return new PaginatedResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),   // zero-based page index
                page.getSize()
        );
    }


    /**
     * Factory method for empty paginated response
     */
    public static <T> PaginatedResponse<T> empty() {
        return new PaginatedResponse<>(List.of());
    }
}
