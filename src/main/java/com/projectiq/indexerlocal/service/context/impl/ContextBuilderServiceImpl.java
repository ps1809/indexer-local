package com.projectiq.indexerlocal.service.context.impl;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.context.ContextBuilderService;
import com.projectiq.indexerlocal.service.context.PromptOptimizationService;
import com.projectiq.indexerlocal.service.context.SmartContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of ContextBuilderService.
 * Builds complete development context from repository search results
 * using only indexed data without filesystem scanning.
 */
@Service
public class ContextBuilderServiceImpl implements ContextBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilderServiceImpl.class);

    private final SymbolSearchService symbolSearchService;
    private final SpringSearchService springSearchService;
    private final RelationshipSearchService relationshipSearchService;
    private final BuildSearchService buildSearchService;
    private final RepositorySearchService repositorySearchService;
    private final SmartContextService smartContextService;
    private final PromptOptimizationService promptOptimizationService;

    public ContextBuilderServiceImpl(
            SymbolSearchService symbolSearchService,
            SpringSearchService springSearchService,
            RelationshipSearchService relationshipSearchService,
            BuildSearchService buildSearchService,
            RepositorySearchService repositorySearchService,
            SmartContextService smartContextService,
            PromptOptimizationService promptOptimizationService) {
        this.symbolSearchService = symbolSearchService;
        this.springSearchService = springSearchService;
        this.relationshipSearchService = relationshipSearchService;
        this.buildSearchService = buildSearchService;
        this.repositorySearchService = repositorySearchService;
        this.smartContextService = smartContextService;
        this.promptOptimizationService = promptOptimizationService;
    }

    @Override
    public ContextResponse buildContext(ContextRequest request) {
        log.info("Building context: query={}, repositoryId={}, type={}",
                request.getQuery(), request.getRepositoryId(), request.getContextType());

        String contextType = request.getContextType();
        if (contextType == null) {
            contextType = "symbol";
        }

        ContextResponse response = switch (contextType.toLowerCase()) {
            case "symbol" -> buildSymbolContext(request);
            case "file" -> buildFileContext(request);
            case "package" -> buildPackageContext(request);
            case "module" -> buildModuleContext(request);
            case "repository" -> buildRepositoryContext(request);
            default -> buildSymbolContext(request);
        };

        // Apply smart expansion
        if (request.getExpansionDepth() > 0) {
            smartContextService.expandAll(response, request);
        }

        // Apply optimization
        response = promptOptimizationService.optimize(response, request);
        response.calculateTotals();

        log.info("Context built: {} entries, {} estimated tokens",
                response.getTotalEntries(), response.getTotalEstimatedTokens());
        return response;
    }

    @Override
    public ContextResponse buildSymbolContext(ContextRequest request) {
        log.info("Building symbol context: symbol={}, type={}",
                request.getSymbolName(), request.getSymbolType());

        ContextResponse response = createBaseResponse(request, "symbol");

        // Search for the requested symbol
        var symbolResult = symbolSearchService.searchSymbols(
                request.getRepositoryId(),
                request.getSymbolName() != null ? request.getSymbolName() : request.getQuery(),
                "PARTIAL",
                request.getSymbolType(),
                request.getPackageName(),
                null,
                request.getModuleName(),
                0,
                request.getMaxSymbols());

        if (symbolResult != null && symbolResult.getContent() != null) {
            for (var entry : symbolResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("symbol", entry.getSymbolName(),
                        entry.getFullyQualifiedName(), 1);
                ctxEntry.setSymbolType(entry.getSymbolType());
                ctxEntry.setPackageName(entry.getPackageName());
                ctxEntry.setFilePath(entry.getFilePath());
                ctxEntry.setVisibility(entry.getVisibility());
                ctxEntry.setSource("symbol_search");
                response.getSymbols().add(ctxEntry);
            }
        }

        // Add dependencies if requested
        if (request.isIncludeDependencies() && request.getSymbolName() != null) {
            addDependencies(request, response);
        }

        // Add Spring components if requested
        if (request.isIncludeSpringBeans()) {
            addSpringComponents(request, response);
        }

        // Add configuration if requested
        if (request.isIncludeConfiguration()) {
            addConfiguration(request, response);
        }

        response.setExpansionDepth(0);
        return response;
    }

    @Override
    public ContextResponse buildFileContext(ContextRequest request) {
        log.info("Building file context: filePath={}", request.getFilePath());

        ContextResponse response = createBaseResponse(request, "file");

        if (request.getFilePath() != null) {
            // Search for symbols in the file
            var symbolResult = symbolSearchService.searchSymbols(
                    request.getRepositoryId(),
                    request.getFilePath(),
                    "PARTIAL",
                    null, null, null, null,
                    0, request.getMaxSymbols());

            if (symbolResult != null && symbolResult.getContent() != null) {
                for (var entry : symbolResult.getContent()) {
                    ContextEntry ctxEntry = new ContextEntry("symbol", entry.getSymbolName(),
                            entry.getFullyQualifiedName(), 2);
                    ctxEntry.setSymbolType(entry.getSymbolType());
                    ctxEntry.setPackageName(entry.getPackageName());
                    ctxEntry.setFilePath(entry.getFilePath());
                    ctxEntry.setSource("file_context");
                    response.getSymbols().add(ctxEntry);

                    ContextEntry fileEntry = new ContextEntry("file", entry.getSymbolName(),
                            entry.getFullyQualifiedName(), 2);
                    fileEntry.setFilePath(entry.getFilePath());
                    fileEntry.setSource("file_context");
                    response.getFiles().add(fileEntry);
                }
            }
        }

        response.setExpansionDepth(0);
        return response;
    }

    @Override
    public ContextResponse buildPackageContext(ContextRequest request) {
        log.info("Building package context: package={}", request.getPackageName());

        ContextResponse response = createBaseResponse(request, "package");

        // Find symbols in the package
        var symbolResult = symbolSearchService.searchSymbols(
                request.getRepositoryId(),
                "",
                "PARTIAL",
                null,
                request.getPackageName(),
                null, null,
                0, request.getMaxSymbols());

        if (symbolResult != null && symbolResult.getContent() != null) {
            Set<String> seenFiles = new HashSet<>();
            for (var entry : symbolResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("symbol", entry.getSymbolName(),
                        entry.getFullyQualifiedName(), 3);
                ctxEntry.setSymbolType(entry.getSymbolType());
                ctxEntry.setPackageName(entry.getPackageName());
                ctxEntry.setFilePath(entry.getFilePath());
                ctxEntry.setSource("package_context");
                response.getSymbols().add(ctxEntry);

                if (entry.getFilePath() != null && seenFiles.add(entry.getFilePath())) {
                    ContextEntry fileEntry = new ContextEntry("file", entry.getFilePath(),
                            null, 3);
                    fileEntry.setFilePath(entry.getFilePath());
                    fileEntry.setSource("package_context");
                    response.getFiles().add(fileEntry);
                }
            }
        }

        // Add package relationships
        addPackageRelationships(request, response);

        response.setExpansionDepth(0);
        return response;
    }

    @Override
    public ContextResponse buildModuleContext(ContextRequest request) {
        log.info("Building module context: module={}", request.getModuleName());

        ContextResponse response = createBaseResponse(request, "module");

        // Find build modules
        var moduleResult = buildSearchService.findModules(
                request.getRepositoryId(),
                request.getModuleName(),
                null,
                0, request.getMaxSymbols());

        if (moduleResult != null && moduleResult.getContent() != null) {
            for (var entry : moduleResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("module", entry.getModuleName(),
                        entry.getBuildFilePath(), 4);
                ctxEntry.setSource("module_context");
                response.getModules().add(ctxEntry);
            }
        }

        // Find symbols in the module
        var symbolResult = symbolSearchService.searchSymbols(
                request.getRepositoryId(),
                "",
                "PARTIAL",
                null, null, null,
                request.getModuleName(),
                0, request.getMaxSymbols());

        if (symbolResult != null && symbolResult.getContent() != null) {
            for (var entry : symbolResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("symbol", entry.getSymbolName(),
                        entry.getFullyQualifiedName(), 4);
                ctxEntry.setSymbolType(entry.getSymbolType());
                ctxEntry.setPackageName(entry.getPackageName());
                ctxEntry.setModuleName(request.getModuleName());
                ctxEntry.setFilePath(entry.getFilePath());
                ctxEntry.setSource("module_context");
                response.getSymbols().add(ctxEntry);
            }
        }

        response.setExpansionDepth(0);
        return response;
    }

    @Override
    public ContextResponse buildRepositoryContext(ContextRequest request) {
        log.info("Building repository context: repositoryId={}", request.getRepositoryId());

        ContextResponse response = createBaseResponse(request, "repository");

        // Find repository info
        var repoResult = repositorySearchService.findRepositories(
                request.getRepositoryId(), null, null,
                null, null, "EXACT",
                0, 1);

        if (repoResult != null && repoResult.getContent() != null && !repoResult.getContent().isEmpty()) {
            var repo = repoResult.getContent().get(0);
            ContextEntry repoEntry = new ContextEntry("repository",
                    repo.getRepositoryName(),
                    repo.getRepositoryId(), 5);
            repoEntry.setDescription(repo.getName());
            repoEntry.setSource("repository_context");
            response.getFiles().add(repoEntry);
        }

        // Find all modules in the repository
        var moduleResult = buildSearchService.findModules(
                request.getRepositoryId(), null, null,
                0, request.getMaxSymbols());

        if (moduleResult != null && moduleResult.getContent() != null) {
            for (var entry : moduleResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("module", entry.getModuleName(),
                        entry.getBuildFilePath(), 5);
                ctxEntry.setSource("repository_context");
                response.getModules().add(ctxEntry);
            }
        }

        // Find top-level symbols
        var symbolResult = symbolSearchService.searchSymbols(
                request.getRepositoryId(),
                "",
                "PARTIAL",
                null, null, null, null,
                0, request.getMaxSymbols());

        if (symbolResult != null && symbolResult.getContent() != null) {
            Set<String> seenFiles = new HashSet<>();
            for (var entry : symbolResult.getContent()) {
                ContextEntry ctxEntry = new ContextEntry("symbol", entry.getSymbolName(),
                        entry.getFullyQualifiedName(), 5);
                ctxEntry.setSymbolType(entry.getSymbolType());
                ctxEntry.setPackageName(entry.getPackageName());
                ctxEntry.setFilePath(entry.getFilePath());
                ctxEntry.setSource("repository_context");
                response.getSymbols().add(ctxEntry);

                if (entry.getFilePath() != null && seenFiles.add(entry.getFilePath())) {
                    ContextEntry fileEntry = new ContextEntry("file", entry.getFilePath(),
                            null, 5);
                    fileEntry.setFilePath(entry.getFilePath());
                    fileEntry.setSource("repository_context");
                    response.getFiles().add(fileEntry);
                }
            }
        }

        response.setExpansionDepth(0);
        return response;
    }

    // ==================== Helper Methods ====================

    private ContextResponse createBaseResponse(ContextRequest request, String contextType) {
        ContextResponse response = new ContextResponse();
        response.setContextType(contextType);
        response.setRepositoryId(request.getRepositoryId());
        response.setQuery(request.getQuery());
        response.setSymbolName(request.getSymbolName());
        response.setPackageName(request.getPackageName());
        response.setModuleName(request.getModuleName());
        return response;
    }

    private void addDependencies(ContextRequest request, ContextResponse response) {
        try {
            var dependencies = relationshipSearchService.findDependencies(
                    request.getRepositoryId(),
                    request.getSymbolName(),
                    0, request.getMaxRelationships());

            if (dependencies != null) {
                for (var dep : dependencies) {
                    ContextEntry depEntry = new ContextEntry("dependency", dep.getTargetSymbol(),
                            dep.getTargetSymbol(), 2);
                    depEntry.setSource("dependency_expansion");
                    response.getDependencies().add(depEntry);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch dependencies for symbol: {}", request.getSymbolName(), e);
        }
    }

    private void addSpringComponents(ContextRequest request, ContextResponse response) {
        try {
            var controllers = springSearchService.findControllers(
                    request.getRepositoryId(), request.getPackageName(),
                    request.getModuleName(), 0, request.getMaxSymbols());

            if (controllers != null) {
                for (var ctrl : controllers) {
                    ContextEntry entry = new ContextEntry("spring", ctrl.getComponentName(),
                            ctrl.getClassName(), 4);
                    entry.setSymbolType("CONTROLLER");
                    entry.setSource("spring_expansion");
                    response.getSpringComponents().add(entry);
                }
            }

            var services = springSearchService.findServices(
                    request.getRepositoryId(), request.getPackageName(),
                    request.getModuleName(), 0, request.getMaxSymbols());

            if (services != null) {
                for (var svc : services) {
                    ContextEntry entry = new ContextEntry("spring", svc.getComponentName(),
                            svc.getClassName(), 4);
                    entry.setSymbolType("SERVICE");
                    entry.setSource("spring_expansion");
                    response.getSpringComponents().add(entry);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Spring components", e);
        }
    }

    private void addConfiguration(ContextRequest request, ContextResponse response) {
        try {
            var configs = springSearchService.findConfigurationClasses(
                    request.getRepositoryId(), request.getPackageName(),
                    request.getModuleName(), 0, request.getMaxSymbols());

            if (configs != null) {
                for (var cfg : configs) {
                    ContextEntry entry = new ContextEntry("configuration", cfg.getComponentName(),
                            cfg.getClassName(), 5);
                    entry.setSource("configuration_expansion");
                    response.getConfigurations().add(entry);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch configuration", e);
        }
    }

    private void addPackageRelationships(ContextRequest request, ContextResponse response) {
        try {
            var pkgRelationships = relationshipSearchService.findPackageRelationships(
                    request.getRepositoryId(), request.getPackageName(),
                    0, request.getMaxRelationships());

            if (pkgRelationships != null) {
                for (var rel : pkgRelationships) {
                    ContextEntry entry = new ContextEntry("relationship",
                            rel.getRelationshipType() != null ? rel.getRelationshipType().name() : "UNKNOWN",
                            rel.getTargetSymbol(), 3);
                    entry.setSource("package_relationship");
                    response.getRelationships().add(entry);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch package relationships", e);
        }
    }
}