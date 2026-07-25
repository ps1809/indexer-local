package com.projectiq.indexerlocal.service.ai.impl;

import com.projectiq.indexerlocal.model.ai.Chunk;
import com.projectiq.indexerlocal.service.*;
import com.projectiq.indexerlocal.service.ai.ChunkGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of ChunkGenerationService.
 * Generates AI-ready semantic chunks from indexed repository data.
 */
@Service
public class ChunkGenerationServiceImpl implements ChunkGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ChunkGenerationServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final SymbolSearchService symbolSearchService;
    private final SpringSearchService springSearchService;
    private final RelationshipSearchService relationshipSearchService;
    private final BuildSearchService buildSearchService;
    private final RepositorySearchService repositorySearchService;

    // In-memory chunk store (in production, use a database)
    private final Map<String, Chunk> chunkStore = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> repositoryChunks = new ConcurrentHashMap<>();

    public ChunkGenerationServiceImpl(
            JdbcTemplate jdbcTemplate,
            SymbolSearchService symbolSearchService,
            SpringSearchService springSearchService,
            RelationshipSearchService relationshipSearchService,
            BuildSearchService buildSearchService,
            RepositorySearchService repositorySearchService) {
        this.jdbcTemplate = jdbcTemplate;
        this.symbolSearchService = symbolSearchService;
        this.springSearchService = springSearchService;
        this.relationshipSearchService = relationshipSearchService;
        this.buildSearchService = buildSearchService;
        this.repositorySearchService = repositorySearchService;
    }

    @Override
    public List<Chunk> generateAllChunks(String repositoryId) {
        log.info("Generating all chunks for repository: {}", repositoryId);
        List<Chunk> allChunks = new ArrayList<>();

        allChunks.addAll(generateClassChunks(repositoryId));
        allChunks.addAll(generateInterfaceChunks(repositoryId));
        allChunks.addAll(generateMethodChunks(repositoryId));
        allChunks.addAll(generatePackageChunks(repositoryId));
        allChunks.addAll(generateModuleMetadataChunks(repositoryId));
        allChunks.addAll(generateConfigurationChunks(repositoryId));
        allChunks.addAll(generateDocumentationChunks(repositoryId));
        allChunks.addAll(generateSpringComponentChunks(repositoryId));
        allChunks.addAll(generateRestApiChunks(repositoryId));
        allChunks.addAll(generateBuildMetadataChunks(repositoryId));

        // Store chunks
        for (Chunk chunk : allChunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} chunks for repository: {}", allChunks.size(), repositoryId);
        return allChunks;
    }

    @Override
    public List<Chunk> generateModuleChunks(String repositoryId, String module) {
        log.info("Generating chunks for module: {} in repository: {}", module, repositoryId);
        List<Chunk> moduleChunks = new ArrayList<>();

        // Generate class chunks for the module
        var classResult = symbolSearchService.searchSymbols(repositoryId, "", "PARTIAL",
                "CLASS", null, null, module, 0, Integer.MAX_VALUE);
        if (classResult != null && classResult.getContent() != null) {
            for (var entry : classResult.getContent()) {
                String chunkId = "class:" + repositoryId + ":" + entry.getFullyQualifiedName();
                Chunk chunk = new Chunk(chunkId, repositoryId, "CLASS", buildClassContent(entry.getSymbolName(), entry.getFullyQualifiedName()));
                chunk.setModule(module);
                chunk.setPackageName(entry.getPackageName());
                chunk.setSymbol(entry.getSymbolName());
                chunk.setLanguage("java");
                chunk.addRelationship("contains", "module:" + module);
                moduleChunks.add(chunk);
            }
        }

        // Store
        for (Chunk chunk : moduleChunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        return moduleChunks;
    }

    @Override
    public List<Chunk> generateClassChunks(String repositoryId) {
        log.info("Generating class chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        var classResult = symbolSearchService.searchSymbols(repositoryId, "", "PARTIAL",
                "CLASS", null, null, null, 0, Integer.MAX_VALUE);
        if (classResult != null && classResult.getContent() != null) {
            for (var entry : classResult.getContent()) {
                String chunkId = "class:" + repositoryId + ":" + entry.getFullyQualifiedName();
                Chunk chunk = new Chunk(chunkId, repositoryId, "CLASS",
                        buildClassContent(entry.getSymbolName(), entry.getFullyQualifiedName()));
                chunk.setSymbol(entry.getSymbolName());
                chunk.setPackageName(entry.getPackageName());
                chunk.setModule(deriveModuleFromPath(entry.getFilePath()));
                chunk.setLanguage("java");
                chunk.setContentHash(computeHash(chunk.getContent()));

                // Add relationships from indexed data
                chunk.addRelationship("file", entry.getFilePath() != null ? entry.getFilePath() : "");
                if (entry.getPackageName() != null) {
                    chunk.addRelationship("package", entry.getPackageName());
                }

                chunks.add(chunk);
            }
        }

        // Store
        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} class chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateInterfaceChunks(String repositoryId) {
        log.info("Generating interface chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        var interfaceResult = symbolSearchService.searchSymbols(repositoryId, "", "PARTIAL",
                "INTERFACE", null, null, null, 0, Integer.MAX_VALUE);
        if (interfaceResult != null && interfaceResult.getContent() != null) {
            for (var entry : interfaceResult.getContent()) {
                String chunkId = "interface:" + repositoryId + ":" + entry.getFullyQualifiedName();
                Chunk chunk = new Chunk(chunkId, repositoryId, "INTERFACE",
                        buildInterfaceContent(entry.getSymbolName(), entry.getFullyQualifiedName()));
                chunk.setSymbol(entry.getSymbolName());
                chunk.setPackageName(entry.getPackageName());
                chunk.setModule(deriveModuleFromPath(entry.getFilePath()));
                chunk.setLanguage("java");
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunk.addRelationship("file", entry.getFilePath() != null ? entry.getFilePath() : "");
                chunk.addRelationship("package", entry.getPackageName() != null ? entry.getPackageName() : "");

                chunks.add(chunk);
            }
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} interface chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateMethodChunks(String repositoryId) {
        log.info("Generating method chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        var methodResult = symbolSearchService.searchSymbols(repositoryId, "", "PARTIAL",
                "METHOD", null, null, null, 0, Integer.MAX_VALUE);
        if (methodResult != null && methodResult.getContent() != null) {
            for (var entry : methodResult.getContent()) {
                String chunkId = "method:" + repositoryId + ":" + entry.getFullyQualifiedName();
                Chunk chunk = new Chunk(chunkId, repositoryId, "METHOD",
                        buildMethodContent(entry.getSymbolName(), entry.getParentSymbol(), entry.getFullyQualifiedName()));
                chunk.setSymbol(entry.getSymbolName());
                chunk.setPackageName(entry.getPackageName());
                chunk.setModule(deriveModuleFromPath(entry.getFilePath()));
                chunk.setLanguage("java");
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunk.addRelationship("class", entry.getParentSymbol() != null ? entry.getParentSymbol() : "");
                chunk.addRelationship("file", entry.getFilePath() != null ? entry.getFilePath() : "");

                chunks.add(chunk);
            }
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} method chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generatePackageChunks(String repositoryId) {
        log.info("Generating package chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        var pkgResult = symbolSearchService.searchSymbols(repositoryId, "", "PARTIAL",
                "PACKAGE", null, null, null, 0, Integer.MAX_VALUE);
        if (pkgResult != null && pkgResult.getContent() != null) {
            Set<String> seenPackages = new HashSet<>();
            for (var entry : pkgResult.getContent()) {
                String pkgName = entry.getSymbolName();
                if (pkgName == null || pkgName.isEmpty() || !seenPackages.add(pkgName)) continue;

                String chunkId = "package:" + repositoryId + ":" + pkgName;
                Chunk chunk = new Chunk(chunkId, repositoryId, "PACKAGE",
                        buildPackageContent(pkgName, repositoryId));
                chunk.setSymbol(pkgName);
                chunk.setPackageName(pkgName);
                chunk.setModule(deriveModuleFromPath(entry.getFilePath()));
                chunk.setLanguage("java");
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunks.add(chunk);
            }
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} package chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateModuleMetadataChunks(String repositoryId) {
        log.info("Generating module metadata chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        var moduleResult = buildSearchService.findModules(repositoryId, null, null, 0, Integer.MAX_VALUE);
        if (moduleResult != null && moduleResult.getContent() != null) {
            for (var entry : moduleResult.getContent()) {
                String chunkId = "module:" + repositoryId + ":" + entry.getModuleName();
                Chunk chunk = new Chunk(chunkId, repositoryId, "MODULE",
                        buildModuleContent(entry));
                chunk.setSymbol(entry.getModuleName());
                chunk.setModule(entry.getModuleName());
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunk.addRelationship("buildFile", entry.getBuildFilePath() != null ? entry.getBuildFilePath() : "");
                if (entry.getParentModule() != null) {
                    chunk.addRelationship("parentModule", entry.getParentModule());
                }

                chunks.add(chunk);
            }
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} module metadata chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateConfigurationChunks(String repositoryId) {
        log.info("Generating configuration chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        // Generate chunks from indexed configuration files
        try {
            String sql = "SELECT cf.id, cf.file_name, cf.configuration_type, cf.file_format, " +
                    "cf.environment_profile, fi.file_path " +
                    "FROM configuration_files cf " +
                    "INNER JOIN file_index fi ON 1=1 " +
                    "WHERE fi.file_path LIKE ?";
            List<Object> params = new ArrayList<>();
            params.add("%" + repositoryId + "%");

            jdbcTemplate.query(sql, (rs) -> {
                String fileName = rs.getString("file_name");
                String configType = rs.getString("configuration_type");
                String fileFormat = rs.getString("file_format");
                String profile = rs.getString("environment_profile");
                String filePath = rs.getString("file_path");

                String chunkId = "config:" + repositoryId + ":" + fileName;
                Chunk chunk = new Chunk(chunkId, repositoryId, "CONFIGURATION",
                        buildConfigurationContent(fileName, configType, fileFormat, profile));
                chunk.setSymbol(fileName);
                chunk.setModule(deriveModuleFromPath(filePath));
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunk.addRelationship("file", filePath != null ? filePath : "");
                if (profile != null) {
                    chunk.addRelationship("profile", profile);
                }

                chunks.add(chunk);
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Failed to generate configuration chunks from database: {}", e.getMessage());
            // Generate from search service as fallback
            try {
                var configs = springSearchService.findConfigurationClasses(repositoryId, null, null, 0, 100);
                if (configs != null) {
                    for (var cfg : configs) {
                        String chunkId = "config:" + repositoryId + ":" + cfg.getClassName();
                        Chunk chunk = new Chunk(chunkId, repositoryId, "CONFIGURATION",
                                "Configuration class: " + cfg.getClassName() + "\nPackage: " + cfg.getPackageName() + "\nType: " + cfg.getComponentType());
                        chunk.setSymbol(cfg.getClassName());
                        chunk.setPackageName(cfg.getPackageName());
                        chunk.setModule(cfg.getModule());
                        chunk.setLanguage("java");
                        chunk.setContentHash(computeHash(chunk.getContent()));
                        chunks.add(chunk);
                    }
                }
            } catch (Exception e2) {
                log.warn("Fallback configuration chunk generation also failed: {}", e2.getMessage());
            }
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} configuration chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateDocumentationChunks(String repositoryId) {
        log.info("Generating documentation chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        // Generate documentation chunks from file metadata and README
        try {
            String sql = "SELECT fi.file_path, fi.file_name, fi.file_classification " +
                    "FROM file_index fi " +
                    "WHERE fi.file_path LIKE ? AND (fi.file_name LIKE ? OR fi.file_name LIKE ?)";
            List<Object> params = new ArrayList<>();
            params.add("%" + repositoryId + "%");
            params.add("README%");
            params.add("%.md");

            jdbcTemplate.query(sql, (rs) -> {
                String filePath = rs.getString("file_path");
                String fileName = rs.getString("file_name");

                String chunkId = "doc:" + repositoryId + ":" + fileName;
                Chunk chunk = new Chunk(chunkId, repositoryId, "DOCUMENTATION",
                        buildDocumentationContent(fileName, filePath));
                chunk.setSymbol(fileName);
                chunk.setModule(deriveModuleFromPath(filePath));
                chunk.setContentHash(computeHash(chunk.getContent()));
                chunk.addRelationship("file", filePath != null ? filePath : "");

                chunks.add(chunk);
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Failed to generate documentation chunks from database: {}", e.getMessage());
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} documentation chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateSpringComponentChunks(String repositoryId) {
        log.info("Generating Spring component chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        try {
            var controllers = springSearchService.findControllers(repositoryId, null, null, 0, Integer.MAX_VALUE);
            addSpringChunks(controllers, "SPRING_COMPONENT", chunks, repositoryId);

            var services = springSearchService.findServices(repositoryId, null, null, 0, Integer.MAX_VALUE);
            addSpringChunks(services, "SPRING_COMPONENT", chunks, repositoryId);

            var repos = springSearchService.findRepositories(repositoryId, null, null, 0, Integer.MAX_VALUE);
            addSpringChunks(repos, "SPRING_COMPONENT", chunks, repositoryId);

            var components = springSearchService.findComponents(repositoryId, null, null, 0, Integer.MAX_VALUE);
            addSpringChunks(components, "SPRING_COMPONENT", chunks, repositoryId);

            var configs = springSearchService.findConfigurationClasses(repositoryId, null, null, 0, Integer.MAX_VALUE);
            addSpringChunks(configs, "SPRING_COMPONENT", chunks, repositoryId);

        } catch (Exception e) {
            log.warn("Failed to generate Spring component chunks: {}", e.getMessage());
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} Spring component chunks", chunks.size());
        return chunks;
    }

    private void addSpringChunks(List<?> results, String chunkType, List<Chunk> chunks, String repositoryId) {
        if (results == null) return;
        for (Object obj : results) {
            if (obj instanceof com.projectiq.indexerlocal.model.springsearch.SpringSearchResult) {
                var entry = (com.projectiq.indexerlocal.model.springsearch.SpringSearchResult) obj;
                String chunkId = "spring:" + repositoryId + ":" + entry.getClassName();
                Chunk chunk = new Chunk(chunkId, repositoryId, chunkType,
                        buildSpringComponentContent(entry));
                chunk.setSymbol(entry.getComponentName());
                chunk.setPackageName(entry.getPackageName());
                chunk.setModule(entry.getModule());
                chunk.setLanguage("java");
                chunk.setContentHash(computeHash(chunk.getContent()));

                chunk.addRelationship("type", entry.getComponentType() != null ? entry.getComponentType() : "");
                chunk.addRelationship("file", entry.getFilePath() != null ? entry.getFilePath() : "");

                chunks.add(chunk);
            }
        }
    }

    @Override
    public List<Chunk> generateRestApiChunks(String repositoryId) {
        log.info("Generating REST API chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        try {
            var endpoints = springSearchService.findEndpoints(repositoryId, null, null, null, null, null, 0, Integer.MAX_VALUE);
            if (endpoints != null) {
                for (var endpoint : endpoints) {
                    String chunkId = "rest:" + repositoryId + ":" + endpoint.getClassName() + ":" + endpoint.getRestPath();
                    Chunk chunk = new Chunk(chunkId, repositoryId, "REST_API",
                            buildRestApiContent(endpoint));
                    chunk.setSymbol(endpoint.getComponentName());
                    chunk.setPackageName(endpoint.getPackageName());
                    chunk.setModule(endpoint.getModule());
                    chunk.setLanguage("java");
                    chunk.setContentHash(computeHash(chunk.getContent()));

                    chunk.addRelationship("controller", endpoint.getClassName() != null ? endpoint.getClassName() : "");
                    if (endpoint.getHttpMethod() != null) chunk.addRelationship("httpMethod", endpoint.getHttpMethod());
                    if (endpoint.getRestPath() != null) chunk.addRelationship("path", endpoint.getRestPath());

                    chunks.add(chunk);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate REST API chunks: {}", e.getMessage());
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} REST API chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> generateBuildMetadataChunks(String repositoryId) {
        log.info("Generating build metadata chunks for repository: {}", repositoryId);
        List<Chunk> chunks = new ArrayList<>();

        try {
            var mvnProjects = buildSearchService.findMavenProjects(repositoryId, null, null, null, 0, Integer.MAX_VALUE);
            if (mvnProjects != null && mvnProjects.getContent() != null) {
                for (var entry : mvnProjects.getContent()) {
                    String chunkId = "build:" + repositoryId + ":" + entry.getModuleName() + ":maven";
                    Chunk chunk = new Chunk(chunkId, repositoryId, "BUILD_METADATA",
                            buildBuildContent(entry));
                    chunk.setSymbol(entry.getModuleName() != null ? entry.getModuleName() : "root");
                    chunk.setModule(entry.getModuleName());
                    chunk.setContentHash(computeHash(chunk.getContent()));

                    chunk.addRelationship("buildSystem", "MAVEN");
                    if (entry.getBuildFilePath() != null) chunk.addRelationship("buildFile", entry.getBuildFilePath());

                    chunks.add(chunk);
                }
            }

            var gradleProjects = buildSearchService.findGradleProjects(repositoryId, null, null, 0, Integer.MAX_VALUE);
            if (gradleProjects != null && gradleProjects.getContent() != null) {
                for (var entry : gradleProjects.getContent()) {
                    String chunkId = "build:" + repositoryId + ":" + entry.getModuleName() + ":gradle";
                    Chunk chunk = new Chunk(chunkId, repositoryId, "BUILD_METADATA",
                            buildBuildContent(entry));
                    chunk.setSymbol(entry.getModuleName() != null ? entry.getModuleName() : "root");
                    chunk.setModule(entry.getModuleName());
                    chunk.setContentHash(computeHash(chunk.getContent()));

                    chunk.addRelationship("buildSystem", "GRADLE");
                    if (entry.getBuildFilePath() != null) chunk.addRelationship("buildFile", entry.getBuildFilePath());

                    chunks.add(chunk);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate build metadata chunks: {}", e.getMessage());
        }

        for (Chunk chunk : chunks) {
            chunkStore.put(chunk.getId(), chunk);
            repositoryChunks.computeIfAbsent(repositoryId, k -> ConcurrentHashMap.newKeySet()).add(chunk.getId());
        }

        log.info("Generated {} build metadata chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<Chunk> incrementalUpdate(String repositoryId) {
        log.info("Running incremental chunk update for repository: {}", repositoryId);
        // In a full implementation, this would compare content hashes and only regenerate changed chunks
        // For MVP, regenerate all chunks
        return generateAllChunks(repositoryId);
    }

    @Override
    public List<Chunk> getChunks(String repositoryId) {
        Set<String> chunkIds = repositoryChunks.getOrDefault(repositoryId, Collections.emptySet());
        return chunkIds.stream()
                .map(chunkStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Chunk getChunk(String chunkId) {
        return chunkStore.get(chunkId);
    }

    @Override
    public void deleteChunks(String repositoryId) {
        Set<String> chunkIds = repositoryChunks.remove(repositoryId);
        if (chunkIds != null) {
            chunkIds.forEach(chunkStore::remove);
        }
        log.info("Deleted chunks for repository: {}", repositoryId);
    }

    @Override
    public void deleteChunk(String chunkId) {
        Chunk chunk = chunkStore.remove(chunkId);
        if (chunk != null && chunk.getRepositoryId() != null) {
            Set<String> chunkIds = repositoryChunks.get(chunk.getRepositoryId());
            if (chunkIds != null) {
                chunkIds.remove(chunkId);
            }
        }
    }

    // ==================== Content Builders ====================

    private String buildClassContent(String className, String fqn) {
        return "Class: " + className + "\n" +
                "Fully Qualified Name: " + (fqn != null ? fqn : className) + "\n" +
                "Type: CLASS\n" +
                "Language: java\n";
    }

    private String buildInterfaceContent(String interfaceName, String fqn) {
        return "Interface: " + interfaceName + "\n" +
                "Fully Qualified Name: " + (fqn != null ? fqn : interfaceName) + "\n" +
                "Type: INTERFACE\n" +
                "Language: java\n";
    }

    private String buildMethodContent(String methodName, String className, String fqn) {
        return "Method: " + methodName + "\n" +
                "Class: " + (className != null ? className : "Unknown") + "\n" +
                "Fully Qualified Name: " + (fqn != null ? fqn : methodName) + "\n" +
                "Type: METHOD\n" +
                "Language: java\n";
    }

    private String buildPackageContent(String packageName, String repositoryId) {
        return "Package: " + packageName + "\n" +
                "Repository: " + repositoryId + "\n" +
                "Type: PACKAGE\n";
    }

    private String buildModuleContent(com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("Module: ").append(entry.getModuleName() != null ? entry.getModuleName() : "Unknown").append("\n");
        sb.append("Build System: ").append(entry.getBuildSystem() != null ? entry.getBuildSystem() : "Unknown").append("\n");
        sb.append("Type: MODULE\n");
        if (entry.getGroupId() != null) sb.append("Group: ").append(entry.getGroupId()).append("\n");
        if (entry.getArtifactId() != null) sb.append("Artifact: ").append(entry.getArtifactId()).append("\n");
        if (entry.getVersion() != null) sb.append("Version: ").append(entry.getVersion()).append("\n");
        if (entry.getPackaging() != null) sb.append("Packaging: ").append(entry.getPackaging()).append("\n");
        return sb.toString();
    }

    private String buildConfigurationContent(String fileName, String configType, String fileFormat, String profile) {
        return "Configuration File: " + fileName + "\n" +
                "Type: " + (configType != null ? configType : "Unknown") + "\n" +
                "Format: " + (fileFormat != null ? fileFormat : "Unknown") + "\n" +
                "Profile: " + (profile != null ? profile : "default") + "\n" +
                "Type: CONFIGURATION\n";
    }

    private String buildDocumentationContent(String fileName, String filePath) {
        return "Documentation File: " + fileName + "\n" +
                "Path: " + (filePath != null ? filePath : "Unknown") + "\n" +
                "Type: DOCUMENTATION\n";
    }

    private String buildSpringComponentContent(com.projectiq.indexerlocal.model.springsearch.SpringSearchResult entry) {
        return "Component: " + entry.getComponentName() + "\n" +
                "Class: " + (entry.getClassName() != null ? entry.getClassName() : "Unknown") + "\n" +
                "Type: " + (entry.getComponentType() != null ? entry.getComponentType() : "Unknown") + "\n" +
                "Package: " + (entry.getPackageName() != null ? entry.getPackageName() : "Unknown") + "\n" +
                "Module: " + (entry.getModule() != null ? entry.getModule() : "Unknown") + "\n" +
                "Category: SPRING_COMPONENT\n";
    }

    private String buildRestApiContent(com.projectiq.indexerlocal.model.springsearch.SpringSearchResult endpoint) {
        return "Endpoint: " + (endpoint.getComponentName() != null ? endpoint.getComponentName() : "Unknown") + "\n" +
                "Path: " + (endpoint.getRestPath() != null ? endpoint.getRestPath() : "/") + "\n" +
                "Method: " + (endpoint.getHttpMethod() != null ? endpoint.getHttpMethod() : "GET") + "\n" +
                "Controller: " + (endpoint.getClassName() != null ? endpoint.getClassName() : "Unknown") + "\n" +
                "Type: REST_API\n";
    }

    private String buildBuildContent(com.projectiq.indexerlocal.model.buildsearch.BuildSearchResult entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("Module: ").append(entry.getModuleName() != null ? entry.getModuleName() : "Unknown").append("\n");
        sb.append("Build System: ").append(entry.getBuildSystem() != null ? entry.getBuildSystem() : "Unknown").append("\n");
        sb.append("Type: BUILD_METADATA\n");
        if (entry.getGroupId() != null) sb.append("Group: ").append(entry.getGroupId()).append("\n");
        if (entry.getArtifactId() != null) sb.append("Artifact: ").append(entry.getArtifactId()).append("\n");
        if (entry.getVersion() != null) sb.append("Version: ").append(entry.getVersion()).append("\n");
        return sb.toString();
    }

    private String computeHash(String content) {
        if (content == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }

    private String deriveModuleFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        if (filePath.contains("/src/")) {
            int srcIdx = filePath.indexOf("/src/");
            String beforeSrc = filePath.substring(0, srcIdx);
            int lastSlash = beforeSrc.lastIndexOf('/');
            if (lastSlash >= 0) {
                return beforeSrc.substring(lastSlash + 1);
            }
            return beforeSrc;
        }
        return "";
    }
}