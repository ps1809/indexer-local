package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipType;

import java.util.List;

/**
 * Service interface for the Relationship Search Engine.
 * Provides deterministic traversal of indexed repository relationships
 * including inheritance, implementations, references, dependencies,
 * call relationships, and package/module connectivity.
 */
public interface RelationshipSearchService {

    /**
     * Find all classes that implement the given interface.
     *
     * @param repositoryId optional repository filter
     * @param interfaceName the fully qualified interface name
     * @param recursive whether to traverse recursively
     * @param maxDepth maximum traversal depth (ignored if not recursive)
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findImplementations(String repositoryId, String interfaceName,
                                                 boolean recursive, int maxDepth,
                                                 int page, int size);

    /**
     * Find all classes that extend (inherit from) the given class.
     *
     * @param repositoryId optional repository filter
     * @param className the fully qualified class name
     * @param recursive whether to traverse recursively
     * @param maxDepth maximum traversal depth (ignored if not recursive)
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findInheritors(String repositoryId, String className,
                                            boolean recursive, int maxDepth,
                                            int page, int size);

    /**
     * Find all references to the given symbol across the indexed codebase.
     *
     * @param repositoryId optional repository filter
     * @param symbolName the symbol name to find references for
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findReferences(String repositoryId, String symbolName,
                                            int page, int size);

    /**
     * Find all usages of the given symbol (references + inheritance + implementations).
     *
     * @param repositoryId optional repository filter
     * @param symbolName the symbol name to find usages for
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findUsages(String repositoryId, String symbolName,
                                        int page, int size);

    /**
     * Find all dependencies of the given symbol (what it depends on).
     *
     * @param repositoryId optional repository filter
     * @param symbolName the symbol name
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findDependencies(String repositoryId, String symbolName,
                                              int page, int size);

    /**
     * Find all dependents of the given symbol (what depends on it).
     *
     * @param repositoryId optional repository filter
     * @param symbolName the symbol name
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findDependents(String repositoryId, String symbolName,
                                            int page, int size);

    /**
     * Find all callers of the given method.
     *
     * @param repositoryId optional repository filter
     * @param methodName the method name
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findCallers(String repositoryId, String methodName,
                                         int page, int size);

    /**
     * Find all callees (methods called by) the given method.
     *
     * @param repositoryId optional repository filter
     * @param methodName the method name
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findCallees(String repositoryId, String methodName,
                                         int page, int size);

    /**
     * Find all relationships between packages in the repository.
     *
     * @param repositoryId optional repository filter
     * @param packageName optional package filter
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findPackageRelationships(String repositoryId, String packageName,
                                                      int page, int size);

    /**
     * Find all relationships between modules in the repository.
     *
     * @param repositoryId optional repository filter
     * @param moduleName optional module filter
     * @param page page number (0-based)
     * @param size page size
     * @return list of relationship entries
     */
    List<RelationshipEntry> findModuleRelationships(String repositoryId, String moduleName,
                                                     int page, int size);

    /**
     * Count the total number of implementations for pagination.
     */
    long countImplementations(String repositoryId, String interfaceName, boolean recursive, int maxDepth);

    /**
     * Count the total number of inheritors for pagination.
     */
    long countInheritors(String repositoryId, String className, boolean recursive, int maxDepth);

    /**
     * Count the total number of references for pagination.
     */
    long countReferences(String repositoryId, String symbolName);

    /**
     * Count the total number of usages for pagination.
     */
    long countUsages(String repositoryId, String symbolName);

    /**
     * Count the total number of dependencies for pagination.
     */
    long countDependencies(String repositoryId, String symbolName);

    /**
     * Count the total number of dependents for pagination.
     */
    long countDependents(String repositoryId, String symbolName);

    /**
     * Count the total number of callers for pagination.
     */
    long countCallers(String repositoryId, String methodName);

    /**
     * Count the total number of callees for pagination.
     */
    long countCallees(String repositoryId, String methodName);

    /**
     * Count the total number of package relationships for pagination.
     */
    long countPackageRelationships(String repositoryId, String packageName);

    /**
     * Count the total number of module relationships for pagination.
     */
    long countModuleRelationships(String repositoryId, String moduleName);
}