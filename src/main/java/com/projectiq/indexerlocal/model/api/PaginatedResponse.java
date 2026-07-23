package com.projectiq.indexerlocal.model.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Paginated API response model for list endpoints.
 */
@Schema(description = "Paginated API response wrapper")
public class PaginatedResponse<T> {

    @Schema(description = "List of items on this page", example = "[{...}, {...}]")
    private List<T> content;

    @Schema(description = "Page number (0-based)", example = "0")
    private int pageNumber;

    @Schema(description = "Size of each page", example = "20")
    private int pageSize;

    @Schema(description = "Total number of pages", example = "10")
    private long totalPages;

    @Schema(description = "Total number of items across all pages", example = "199")
    private long totalElements;

    @Schema(description = "Indicates if this is the first page", example = "true")
    private boolean isFirstPage;

    @Schema(description = "Indicates if this is the last page", example = "false")
    private boolean isLastPage;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> content, int pageNumber, int pageSize, long totalPages, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.isFirstPage = pageNumber == 0;
        this.isLastPage = pageNumber >= totalPages - 1;
    }

    public static <T> PaginatedResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalPages, long totalElements) {
        return new PaginatedResponse<>(content, pageNumber, pageSize, totalPages, totalElements);
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }

    public void setFirstPage(boolean firstPage) {
        isFirstPage = firstPage;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void setLastPage(boolean lastPage) {
        isLastPage = lastPage;
    }
}