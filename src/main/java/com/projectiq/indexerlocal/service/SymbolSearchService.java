package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.model.symbol.SymbolSearchResult;

/**
 * Service interface for the Symbol Search Engine.
 * Provides fast, deterministic search over indexed Java symbols
 * without filesystem scanning.
 */
public interface SymbolSearchService {

    /**
     * Search for classes by name.
     *
     * @param repositoryId optional repository filter
     * @param query the search query
     * @param mode search mode (EXACT, PREFIX, PARTIAL, FQN)
     * @param packageName optional package filter
     * @param visibility optional visibility filter
     * @param page page number (0-based)
     * @param size page size
     * @return paginated symbol search results
     */
    SymbolSearchResult findClass(String repositoryId, String query, String mode,
                                  String packageName, String visibility,
                                  int page, int size);

    /**
     * Search for interfaces by name.
     */
    SymbolSearchResult findInterface(String repositoryId, String query, String mode,
                                      String packageName, String visibility,
                                      int page, int size);

    /**
     * Search for enums by name.
     */
    SymbolSearchResult findEnum(String repositoryId, String query, String mode,
                                 String packageName, String visibility,
                                 int page, int size);

    /**
     * Search for records by name.
     */
    SymbolSearchResult findRecord(String repositoryId, String query, String mode,
                                   String packageName, String visibility,
                                   int page, int size);

    /**
     * Search for annotation types by name.
     */
    SymbolSearchResult findAnnotation(String repositoryId, String query, String mode,
                                       String packageName, int page, int size);

    /**
     * Search for methods by name.
     */
    SymbolSearchResult findMethod(String repositoryId, String query, String mode,
                                   String packageName, String visibility,
                                   int page, int size);

    /**
     * Search for constructors by name.
     */
    SymbolSearchResult findConstructor(String repositoryId, String query, String mode,
                                        String packageName, String visibility,
                                        int page, int size);

    /**
     * Search for fields by name.
     */
    SymbolSearchResult findField(String repositoryId, String query, String mode,
                                  String packageName, String visibility,
                                  int page, int size);

    /**
     * Search for packages by name.
     */
    SymbolSearchResult findPackage(String repositoryId, String query, String mode,
                                    int page, int size);

    /**
     * General symbol search across all symbol types.
     */
    SymbolSearchResult searchSymbols(String repositoryId, String query, String mode,
                                      String symbolType, String packageName,
                                      String visibility, String module,
                                      int page, int size);
}