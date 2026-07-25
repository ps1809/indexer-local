package com.projectiq.indexerlocal.model.symbol;

import java.util.List;

/**
 * Represents a paginated result from a symbol search operation.
 * Contains the search results and pagination metadata.
 */
public class SymbolSearchResult {

    private final List<SymbolEntry> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public SymbolSearchResult(List<SymbolEntry> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }

    public List<SymbolEntry> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}