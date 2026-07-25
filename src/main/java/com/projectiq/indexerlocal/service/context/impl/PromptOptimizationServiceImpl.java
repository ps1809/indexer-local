package com.projectiq.indexerlocal.service.context.impl;

import com.projectiq.indexerlocal.model.context.ContextEntry;
import com.projectiq.indexerlocal.model.context.ContextRequest;
import com.projectiq.indexerlocal.model.context.ContextResponse;
import com.projectiq.indexerlocal.service.context.PromptOptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PromptOptimizationService.
 * Produces compact, deterministic AI-ready context by removing duplicates,
 * reducing token count, ranking by relevance, and removing noise.
 *
 * Ranking Priority:
 * 1. Requested Symbol
 * 2. Direct Dependencies
 * 3. Direct Call Graph
 * 4. Related Spring Components
 * 5. Same Package
 * 6. Same Module
 * 7. Repository Context
 */
@Service
public class PromptOptimizationServiceImpl implements PromptOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(PromptOptimizationServiceImpl.class);

    @Override
    public void removeDuplicateSymbols(ContextResponse context) {
        Set<String> seen = new HashSet<>();
        List<ContextEntry> deduplicated = new ArrayList<>();

        for (ContextEntry entry : context.getSymbols()) {
            String key = buildDedupKey(entry);
            if (seen.add(key)) {
                deduplicated.add(entry);
            }
        }

        int removed = context.getSymbols().size() - deduplicated.size();
        if (removed > 0) {
            log.debug("Removed {} duplicate symbols", removed);
        }
        context.setSymbols(deduplicated);
    }

    @Override
    public void removeDuplicateDependencies(ContextResponse context) {
        Set<String> seen = new HashSet<>();
        List<ContextEntry> deduplicated = new ArrayList<>();

        for (ContextEntry entry : context.getDependencies()) {
            String key = buildDedupKey(entry);
            if (seen.add(key)) {
                deduplicated.add(entry);
            }
        }

        int removed = context.getDependencies().size() - deduplicated.size();
        if (removed > 0) {
            log.debug("Removed {} duplicate dependencies", removed);
        }
        context.setDependencies(deduplicated);
    }

    @Override
    public void removeDuplicateRelationships(ContextResponse context) {
        Set<String> seen = new HashSet<>();
        List<ContextEntry> deduplicated = new ArrayList<>();

        for (ContextEntry entry : context.getRelationships()) {
            String key = buildDedupKey(entry);
            if (seen.add(key)) {
                deduplicated.add(entry);
            }
        }

        int removed = context.getRelationships().size() - deduplicated.size();
        if (removed > 0) {
            log.debug("Removed {} duplicate relationships", removed);
        }
        context.setRelationships(deduplicated);
    }

    @Override
    public void removeAllDuplicates(ContextResponse context) {
        removeDuplicateSymbols(context);
        removeDuplicateDependencies(context);
        removeDuplicateRelationships(context);

        // Also deduplicate across sections - remove entries already in symbols from other lists
        Set<String> symbolKeys = context.getSymbols().stream()
                .map(this::buildDedupKey)
                .collect(Collectors.toSet());

        context.getDependencies().removeIf(e -> symbolKeys.contains(buildDedupKey(e)));
        context.getRelationships().removeIf(e -> symbolKeys.contains(buildDedupKey(e)));
        context.getSpringComponents().removeIf(e -> symbolKeys.contains(buildDedupKey(e)));
        context.getConfigurations().removeIf(e -> symbolKeys.contains(buildDedupKey(e)));
    }

    @Override
    public void reduceTokens(ContextResponse context, ContextRequest request) {
        int maxTokens = request.getMaxTokens();
        int currentTokens = context.getTotalEstimatedTokens();

        if (currentTokens <= maxTokens) {
            return;
        }

        log.debug("Token reduction needed: {} tokens, max {}", currentTokens, maxTokens);

        // Collect all entries with their priorities and sort by priority (ascending - remove low priority first)
        List<Map.Entry<ContextEntry, Integer>> allEntries = new ArrayList<>();
        int sectionPriority;

        sectionPriority = 1;
        for (ContextEntry e : context.getSymbols()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 2;
        for (ContextEntry e : context.getDependencies()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 3;
        for (ContextEntry e : context.getRelationships()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 4;
        for (ContextEntry e : context.getSpringComponents()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 5;
        for (ContextEntry e : context.getConfigurations()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 6;
        for (ContextEntry e : context.getDocumentation()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        sectionPriority = 7;
        for (ContextEntry e : context.getModules()) {
            allEntries.add(Map.entry(e, e.getPriority() * sectionPriority));
        }

        // Sort by effective priority (ascending - entries to remove first)
        allEntries.sort(Comparator.comparingInt(Map.Entry::getValue));

        // Remove entries until we're under the token limit
        int tokensToRemove = currentTokens - maxTokens;
        Set<ContextEntry> toRemove = new HashSet<>();

        for (var entry : allEntries) {
            if (tokensToRemove <= 0) break;
            ContextEntry ce = entry.getKey();
            if (ce.getEstimatedTokens() > 0) {
                toRemove.add(ce);
                tokensToRemove -= ce.getEstimatedTokens();
            }
        }

        // Remove from all lists
        context.getSymbols().removeAll(toRemove);
        context.getDependencies().removeAll(toRemove);
        context.getRelationships().removeAll(toRemove);
        context.getSpringComponents().removeAll(toRemove);
        context.getConfigurations().removeAll(toRemove);
        context.getDocumentation().removeAll(toRemove);
        context.getModules().removeAll(toRemove);

        context.calculateTotals();
        log.debug("Token reduction complete: {} tokens remaining", context.getTotalEstimatedTokens());
    }

    @Override
    public void compressContext(ContextResponse context) {
        // Merge duplicate entries that have the same fully qualified name
        Map<String, ContextEntry> merged = new LinkedHashMap<>();

        for (ContextEntry entry : context.getSymbols()) {
            String key = entry.getFullyQualifiedName() != null ? entry.getFullyQualifiedName() : entry.getName();
            merged.merge(key, entry, (existing, incoming) -> {
                existing.setPriority(Math.min(existing.getPriority(), incoming.getPriority()));
                return existing;
            });
        }
        context.setSymbols(new ArrayList<>(merged.values()));

        // Sort by priority for deterministic output
        sortByPriority(context.getSymbols());
        sortByPriority(context.getDependencies());
        sortByPriority(context.getRelationships());
        sortByPriority(context.getSpringComponents());
        sortByPriority(context.getConfigurations());
        sortByPriority(context.getDocumentation());
        sortByPriority(context.getModules());

        context.calculateTotals();
    }

    @Override
    public void rankByPriority(ContextResponse context, ContextRequest request) {
        // Sort each section by priority (lower number = higher priority)
        sortByPriority(context.getSymbols());
        sortByPriority(context.getDependencies());
        sortByPriority(context.getRelationships());
        sortByPriority(context.getSpringComponents());
        sortByPriority(context.getConfigurations());
        sortByPriority(context.getDocumentation());
        sortByPriority(context.getModules());
    }

    @Override
    public void removeNoise(ContextResponse context, ContextRequest request) {
        // Remove entries with very high priority (low relevance)
        int noiseThreshold = 10;

        int before = context.getTotalEntries();

        context.getSymbols().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getDependencies().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getRelationships().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getSpringComponents().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getConfigurations().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getDocumentation().removeIf(e -> e.getPriority() >= noiseThreshold);
        context.getModules().removeIf(e -> e.getPriority() >= noiseThreshold);

        context.calculateTotals();
        int removed = before - context.getTotalEntries();
        if (removed > 0) {
            log.debug("Removed {} noise entries", removed);
        }
    }

    @Override
    public ContextResponse optimize(ContextResponse context, ContextRequest request) {
        log.debug("Starting context optimization");

        // Step 1: Remove all duplicates
        removeAllDuplicates(context);

        // Step 2: Rank by priority
        rankByPriority(context, request);

        // Step 3: Compress context
        compressContext(context);

        // Step 4: Remove noise
        removeNoise(context, request);

        // Step 5: Reduce tokens if needed
        reduceTokens(context, request);

        // Step 6: Final ranking
        rankByPriority(context, request);

        context.setOptimized(true);
        context.calculateTotals();

        log.debug("Context optimization complete: {} entries, {} tokens",
                context.getTotalEntries(), context.getTotalEstimatedTokens());
        return context;
    }

    private String buildDedupKey(ContextEntry entry) {
        if (entry.getFullyQualifiedName() != null) {
            return entry.getType() + ":" + entry.getFullyQualifiedName();
        }
        return entry.getType() + ":" + entry.getName() + ":" + entry.getFilePath();
    }

    private void sortByPriority(List<ContextEntry> entries) {
        entries.sort(Comparator.comparingInt(ContextEntry::getPriority)
                .thenComparing(ContextEntry::getName, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}