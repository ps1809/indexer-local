package com.projectiq.indexerlocal.service.context.impl;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.RelationshipSearchService;
import com.projectiq.indexerlocal.service.SpringSearchService;
import com.projectiq.indexerlocal.service.SymbolSearchService;
import com.projectiq.indexerlocal.service.context.SmartContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of SmartContextService.
 * Automatically expands context using indexed relationships
 * with configurable limits and cycle detection.
 */
@Service
public class SmartContextServiceImpl implements SmartContextService {

    private static final Logger log = LoggerFactory.getLogger(SmartContextServiceImpl.class);

    private final SymbolSearchService symbolSearchService;
    private final RelationshipSearchService relationshipSearchService;
    private final SpringSearchService springSearchService;

    public SmartContextServiceImpl(
            SymbolSearchService symbolSearchService,
            RelationshipSearchService relationshipSearchService,
            SpringSearchService springSearchService) {
        this.symbolSearchService = symbolSearchService;
        this.relationshipSearchService = relationshipSearchService;
        this.springSearchService = springSearchService;
    }

    @Override
    public void expandImports(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via imports");
        Set<String> seenSymbols = getSeenSymbols(context);
        int added = 0;

        for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
            if (added >= request.getMaxRelationships()) break;
            if (entry.getFullyQualifiedName() == null) continue;

            try {
                var deps = relationshipSearchService.findDependencies(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        0, Math.min(10, request.getMaxRelationships() - added));

                if (deps != null) {
                    for (var dep : deps) {
                        if (dep.getTargetSymbol() != null && seenSymbols.add(dep.getTargetSymbol())) {
                            ContextEntry depEntry = new ContextEntry("dependency",
                                    dep.getTargetSymbol(), dep.getTargetSymbol(), 3);
                            depEntry.setSource("import_expansion");
                            context.getDependencies().add(depEntry);
                            added++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed import expansion for: {}", entry.getFullyQualifiedName(), e);
            }
        }

        log.debug("Import expansion added {} entries", added);
    }

    @Override
    public void expandDependencies(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via dependencies");
        Set<String> seenSymbols = getSeenSymbols(context);
        int added = 0;

        for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
            if (added >= request.getMaxRelationships()) break;
            if (entry.getFullyQualifiedName() == null) continue;

            try {
                var deps = relationshipSearchService.findDependencies(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        0, Math.min(20, request.getMaxRelationships() - added));

                if (deps != null) {
                    for (var dep : deps) {
                        if (seenSymbols.add(dep.getTargetSymbol())) {
                            ContextEntry depEntry = new ContextEntry("dependency",
                                    dep.getTargetSymbol(), dep.getTargetSymbol(), 4);
                            depEntry.setSource("dependency_expansion");
                            context.getDependencies().add(depEntry);
                            added++;
                        }
                    }
                }

                var dependents = relationshipSearchService.findDependents(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        0, Math.min(10, request.getMaxRelationships() - added));

                if (dependents != null) {
                    for (var dep : dependents) {
                        if (seenSymbols.add(dep.getSourceSymbol())) {
                            ContextEntry depEntry = new ContextEntry("dependency",
                                    dep.getSourceSymbol(), dep.getSourceSymbol(), 4);
                            depEntry.setSource("dependent_expansion");
                            context.getDependencies().add(depEntry);
                            added++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed dependency expansion for: {}", entry.getFullyQualifiedName(), e);
            }
        }

        log.debug("Dependency expansion added {} entries", added);
    }

    @Override
    public void expandCallGraph(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via call graph");
        Set<String> seenSymbols = getSeenSymbols(context);
        int added = 0;

        for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
            if (added >= request.getMaxRelationships()) break;
            if (entry.getFullyQualifiedName() == null) continue;

            try {
                var callees = relationshipSearchService.findCallees(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        0, Math.min(15, request.getMaxRelationships() - added));

                if (callees != null) {
                    for (var callee : callees) {
                        if (seenSymbols.add(callee.getTargetSymbol())) {
                            ContextEntry callEntry = new ContextEntry("call_graph",
                                    callee.getTargetSymbol(), callee.getTargetSymbol(), 3);
                            callEntry.setSource("call_graph_expansion");
                            context.getRelationships().add(callEntry);
                            added++;
                        }
                    }
                }

                var callers = relationshipSearchService.findCallers(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        0, Math.min(15, request.getMaxRelationships() - added));

                if (callers != null) {
                    for (var caller : callers) {
                        if (seenSymbols.add(caller.getSourceSymbol())) {
                            ContextEntry callEntry = new ContextEntry("call_graph",
                                    caller.getSourceSymbol(), caller.getSourceSymbol(), 3);
                            callEntry.setSource("call_graph_expansion");
                            context.getRelationships().add(callEntry);
                            added++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed call graph expansion for: {}", entry.getFullyQualifiedName(), e);
            }
        }

        log.debug("Call graph expansion added {} entries", added);
    }

    @Override
    public void expandInheritance(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via inheritance");
        int added = 0;

        for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
            if (added >= request.getMaxRelationships()) break;
            if (entry.getFullyQualifiedName() == null) continue;

            try {
                var inheritors = relationshipSearchService.findInheritors(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        false, request.getExpansionDepth(),
                        0, Math.min(10, request.getMaxRelationships() - added));

                if (inheritors != null) {
                    for (var inh : inheritors) {
                        ContextEntry inhEntry = new ContextEntry("inheritance",
                                inh.getSourceSymbol(), inh.getSourceSymbol(), 3);
                        inhEntry.setSource("inheritance_expansion");
                        context.getRelationships().add(inhEntry);
                        added++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed inheritance expansion for: {}", entry.getFullyQualifiedName(), e);
            }
        }

        log.debug("Inheritance expansion added {} entries", added);
    }

    @Override
    public void expandInterfaces(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via interfaces");
        int added = 0;

        for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
            if (added >= request.getMaxRelationships()) break;
            if (entry.getFullyQualifiedName() == null) continue;

            try {
                var implementations = relationshipSearchService.findImplementations(
                        request.getRepositoryId(), entry.getFullyQualifiedName(),
                        false, request.getExpansionDepth(),
                        0, Math.min(10, request.getMaxRelationships() - added));

                if (implementations != null) {
                    for (var impl : implementations) {
                        ContextEntry implEntry = new ContextEntry("interface",
                                impl.getSourceSymbol(), impl.getSourceSymbol(), 3);
                        implEntry.setSource("interface_expansion");
                        context.getRelationships().add(implEntry);
                        added++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed interface expansion for: {}", entry.getFullyQualifiedName(), e);
            }
        }

        log.debug("Interface expansion added {} entries", added);
    }

    @Override
    public void expandSpringBeans(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via Spring beans");
        int added = 0;

        try {
            // Add more Spring components from related packages
            for (ContextEntry entry : new ArrayList<>(context.getSymbols())) {
                if (added >= request.getMaxSymbols()) break;
                if (entry.getPackageName() == null) continue;

                var beans = springSearchService.findBeans(
                        request.getRepositoryId(), entry.getPackageName(),
                        null, request.getModuleName(), 0,
                        Math.min(10, request.getMaxSymbols() - added));

                if (beans != null) {
                    for (var bean : beans) {
                        ContextEntry beanEntry = new ContextEntry("spring",
                                bean.getComponentName(), bean.getClassName(), 4);
                        beanEntry.setSymbolType("BEAN");
                        beanEntry.setSource("spring_bean_expansion");
                        context.getSpringComponents().add(beanEntry);
                        added++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed Spring bean expansion", e);
        }

        log.debug("Spring bean expansion added {} entries", added);
    }

    @Override
    public void expandRestEndpoints(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via REST endpoints");
        int added = 0;

        try {
            for (ContextEntry entry : new ArrayList<>(context.getSpringComponents())) {
                if (added >= request.getMaxRelationships()) break;
                if (entry.getPackageName() == null) continue;

                var endpoints = springSearchService.findEndpoints(
                        request.getRepositoryId(), null, null,
                        entry.getName(), entry.getPackageName(),
                        request.getModuleName(), 0,
                        Math.min(10, request.getMaxRelationships() - added));

                if (endpoints != null) {
                    for (var ep : endpoints) {
                        ContextEntry epEntry = new ContextEntry("rest_endpoint",
                                ep.getComponentName(), ep.getClassName(), 4);
                        epEntry.setSource("rest_endpoint_expansion");
                        context.getSpringComponents().add(epEntry);
                        added++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed REST endpoint expansion", e);
        }

        log.debug("REST endpoint expansion added {} entries", added);
    }

    @Override
    public void expandConfiguration(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via configuration");
        int added = 0;

        try {
            // Find configuration classes related to packages in context
            Set<String> seenPackages = context.getSymbols().stream()
                    .map(ContextEntry::getPackageName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String pkg : seenPackages) {
                if (added >= request.getMaxSymbols()) break;

                var configs = springSearchService.findConfigurationClasses(
                        request.getRepositoryId(), pkg,
                        request.getModuleName(), 0,
                        Math.min(10, request.getMaxSymbols() - added));

                if (configs != null) {
                    for (var cfg : configs) {
                        ContextEntry cfgEntry = new ContextEntry("configuration",
                                cfg.getComponentName(), cfg.getClassName(), 5);
                        cfgEntry.setSource("configuration_expansion");
                        context.getConfigurations().add(cfgEntry);
                        added++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed configuration expansion", e);
        }

        log.debug("Configuration expansion added {} entries", added);
    }

    @Override
    public void expandModules(ContextResponse context, ContextRequest request) {
        log.debug("Expanding context via modules");
        // Module expansion is primarily handled in buildModuleContext
        // This can cross-reference modules with symbols already in context
        int added = 0;

        try {
            Set<String> seenModules = context.getSymbols().stream()
                    .map(ContextEntry::getModuleName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            seenModules.add(request.getModuleName());

            for (String mod : seenModules) {
                if (added >= request.getMaxSymbols()) break;
                if (mod == null || mod.isEmpty()) continue;

                var symbols = symbolSearchService.searchSymbols(
                        request.getRepositoryId(), "", "PARTIAL",
                        null, null, null, mod,
                        0, Math.min(20, request.getMaxSymbols() - added));

                if (symbols != null && symbols.getContent() != null) {
                    for (var sym : symbols.getContent()) {
                        ContextEntry symEntry = new ContextEntry("symbol",
                                sym.getSymbolName(), sym.getFullyQualifiedName(), 5);
                        symEntry.setSymbolType(sym.getSymbolType());
                        symEntry.setPackageName(sym.getPackageName());
                        symEntry.setModuleName(mod);
                        symEntry.setFilePath(sym.getFilePath());
                        symEntry.setSource("module_expansion");
                        context.getSymbols().add(symEntry);
                        added++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed module expansion", e);
        }

        log.debug("Module expansion added {} entries", added);
    }

    @Override
    public void expandAll(ContextResponse context, ContextRequest request) {
        log.info("Applying all expansion rules, depth={}", request.getExpansionDepth());

        if (request.isIncludeRelationships()) {
            expandImports(context, request);
            expandDependencies(context, request);
        }

        if (request.isIncludeCallGraph()) {
            expandCallGraph(context, request);
        }

        if (request.isIncludeRelationships()) {
            expandInheritance(context, request);
            expandInterfaces(context, request);
        }

        if (request.isIncludeSpringBeans()) {
            expandSpringBeans(context, request);
            expandRestEndpoints(context, request);
        }

        if (request.isIncludeConfiguration()) {
            expandConfiguration(context, request);
        }

        // Module expansion for deeper depths
        if (request.getExpansionDepth() > 1) {
            expandModules(context, request);
        }

        log.info("All expansion rules applied");
    }

    private Set<String> getSeenSymbols(ContextResponse context) {
        Set<String> seen = new HashSet<>();
        for (ContextEntry e : context.getSymbols()) {
            if (e.getFullyQualifiedName() != null) seen.add(e.getFullyQualifiedName());
        }
        for (ContextEntry e : context.getDependencies()) {
            if (e.getFullyQualifiedName() != null) seen.add(e.getFullyQualifiedName());
        }
        for (ContextEntry e : context.getRelationships()) {
            if (e.getFullyQualifiedName() != null) seen.add(e.getFullyQualifiedName());
        }
        return seen;
    }
}