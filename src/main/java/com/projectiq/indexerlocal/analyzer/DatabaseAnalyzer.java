package com.projectiq.indexerlocal.analyzer;

import com.projectiq.indexerlocal.model.DatabaseArtifact;
import com.projectiq.indexerlocal.model.DatabaseType;
import com.projectiq.indexerlocal.model.SqlFileClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Discovers, analyzes, and classifies all database-related artifacts within a repository.
 * Does NOT connect to any database or execute SQL scripts.
 */
@Component
public class DatabaseAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseAnalyzer.class);

    // JDBC driver patterns for database type detection
    private static final Map<String, DatabaseType> DRIVER_TO_DATABASE_TYPE = new HashMap<>();
    static {
        DRIVER_TO_DATABASE_TYPE.put("oracle.jdbc", DatabaseType.ORACLE);
        DRIVER_TO_DATABASE_TYPE.put("com.mysql.cj.jdbc", DatabaseType.MYSQL);
        DRIVER_TO_DATABASE_TYPE.put("com.mysql.jdbc", DatabaseType.MYSQL);
        DRIVER_TO_DATABASE_TYPE.put("org.mariadb.jdbc", DatabaseType.MARIADB);
        DRIVER_TO_DATABASE_TYPE.put("com.microsoft.sqlserver.jdbc", DatabaseType.MICROSOFT_SQL_SERVER);
        DRIVER_TO_DATABASE_TYPE.put("com.db2.jdbc", DatabaseType.DB2);
        DRIVER_TO_DATABASE_TYPE.put("org.sqlite.jdbc", DatabaseType.SQLITE);
        DRIVER_TO_DATABASE_TYPE.put("org.h2.jdbc", DatabaseType.H2);
    }

    // URL patterns for database type detection
    private static final Map<String, DatabaseType> URL_TO_DATABASE_TYPE = new HashMap<>();
    static {
        URL_TO_DATABASE_TYPE.put("jdbc:oracle:", DatabaseType.ORACLE);
        URL_TO_DATABASE_TYPE.put("jdbc:mysql:", DatabaseType.MYSQL);
        URL_TO_DATABASE_TYPE.put("jdbc:mariadb:", DatabaseType.MARIADB);
        URL_TO_DATABASE_TYPE.put("jdbc:sqlserver:", DatabaseType.MICROSOFT_SQL_SERVER);
        URL_TO_DATABASE_TYPE.put("jdbc:db2:", DatabaseType.DB2);
        URL_TO_DATABASE_TYPE.put("jdbc:sqlite:", DatabaseType.SQLITE);
        URL_TO_DATABASE_TYPE.put("jdbc:h2:", DatabaseType.H2);
    }

    // MongoDB patterns
    private static final List<String> MONGODB_PATTERNS = List.of(
            "mongodb://", "mongo:", "spring.data.mongodb", "MongoDatabase",
            "org.mongodb.morphia", "com.mongodb.client"
    );

    // Cassandra patterns
    private static final List<String> CASSANDRA_PATTERNS = List.of(
            "cassandra:", "c*ql:", "springs.cassandra", "com.datastax.oss",
            "org.apache.cassandra"
    );

    // Redis patterns
    private static final List<String> REDIS_PATTERNS = List.of(
            "redis://", "spring.data.redis", "LettuceConnectionFactory",
            "JedisConnectionFactory", "io.lettuce.core", "redis.clients.jedis"
    );

    // Elasticsearch patterns
    private static final List<String> ELASTICSEARCH_PATTERNS = List.of(
            "elasticsearch:", "spring.data.elasticsearch", "ElasticsearchRestClient",
            "org.elasticsearch.client"
    );

    // Flyway patterns
    private static final Set<String> FLYWAY_KEYWORDS = Set.of(
            "flyway", "FlywayMigrationInitializer", "org.flywaydb.core.Flyway",
            "spring.flyway.enabled", "flyway.baselineOnMigrate"
    );

    // Liquibase patterns
    private static final Set<String> LIQUIBASE_KEYWORDS = Set.of(
            "liquibase", "LiquibaseDatabasechangelog", "org.liquibase.core",
            "spring.liquibase.enabled", "liquibase.changeLogFile"
    );

    // ORM framework patterns
    private static final Map<String, String> ORM_FRAMEWORK_PATTERNS = new HashMap<>();
    static {
        ORM_FRAMEWORK_PATTERNS.put("spring.data.jpa", "SPRING_DATA_JPA");
        ORM_FRAMEWORK_PATTERNS.put("org.springframework.orm", "SPRING_DATA_JPA");
        ORM_FRAMEWORK_PATTERNS.put("hibernate", "HIBERNATE");
        ORM_FRAMEWORK_PATTERNS.put("org.hibernate.SessionFactory", "HIBERNATE");
        ORM_FRAMEWORK_PATTERNS.put("mybatis", "MYBATIS");
        ORM_FRAMEWORK_PATTERNS.put("org.mybatis.spring", "MYBATIS");
        ORM_FRAMEWORK_PATTERNS.put("SqlSessionFactoryBean", "MYBATIS");
        ORM_FRAMEWORK_PATTERNS.put("jdbc.namedparametertable", "JDBC");
        ORM_FRAMEWORK_PATTERNS.put("spring.jdbc", "SPRING_JDBC");
    }

    // SQL file naming patterns for classification
    private static final List<ClassificationPattern> SQL_CLASSIFICATION_PATTERNS = List.of(
        // Flyway migrations
        new ClassificationPattern("V.*\\.sql$", SqlFileClassification.MIGRATION),
        new ClassificationPattern("v.*_.*\\.sql$", SqlFileClassification.MIGRATION),
        
        // Liquibase changelogs
        new ClassificationPattern(".*changelog.*\\.xml", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*changelog.*\\.yaml", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*changelog.*\\.yml", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*db\\.changelog.*\\.xml", SqlFileClassification.MIGRATION),
        
        // Seed data scripts
        new ClassificationPattern(".*seed.*\\.sql", SqlFileClassification.SEED_DATA),
        new ClassificationPattern(".*initial.*\\.sql", SqlFileClassification.SEED_DATA),
        new ClassificationPattern(".*data.*init.*\\.sql", SqlFileClassification.SEED_DATA),
        new ClassificationPattern(".*fixtures.*\\.sql", SqlFileClassification.SEED_DATA),
        new ClassificationPattern(".*dump.*\\.sql", SqlFileClassification.SEED_DATA),
        new ClassificationPattern("sample.*\\.sql", SqlFileClassification.SEED_DATA),
        
        // Migration scripts
        new ClassificationPattern(".*migrate.*\\.sql", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*migration.*\\.sql", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*upgrade.*\\.sql", SqlFileClassification.MIGRATION),
        new ClassificationPattern(".*schema.*update.*\\.sql", SqlFileClassification.DDL),
        
        // Stored procedures
        new ClassificationPattern(".*proc.*\\.sql", SqlFileClassification.STORED_PROCEDURE),
        new ClassificationPattern(".*stored.*proc.*\\.sql", SqlFileClassification.STORED_PROCEDURE),
        
        // Triggers
        new ClassificationPattern(".*trigger.*\\.sql", SqlFileClassification.TRIGGER),
        
        // Views
        new ClassificationPattern(".*view.*\\.sql", SqlFileClassification.VIEW),
        
        // Functions
        new ClassificationPattern(".*function.*\\.sql", SqlFileClassification.FUNCTION),
        
        // DDL scripts
        new ClassificationPattern(".*schema.*\\.sql", SqlFileClassification.DDL),
        new ClassificationPattern(".*ddl.*\\.sql", SqlFileClassification.DDL),
        new ClassificationPattern(".*create.*\\.sql", SqlFileClassification.DDL),
        new ClassificationPattern(".*table.*\\.sql", SqlFileClassification.DDL),
        new ClassificationPattern(".*index.*\\.sql", SqlFileClassification.DDL),
        
        // DML scripts
        new ClassificationPattern(".*insert.*\\.sql", SqlFileClassification.DML),
        new ClassificationPattern(".*update.*data.*\\.sql", SqlFileClassification.DML),
        new ClassificationPattern(".*bulk.*\\.sql", SqlFileClassification.DML),
        new ClassificationPattern(".*export.*\\.sql", SqlFileClassification.DML),
        
        // Utility scripts
        new ClassificationPattern(".*util.*\\.sql", SqlFileClassification.UTILITY),
        new ClassificationPattern(".*common.*\\.sql", SqlFileClassification.UTILITY),
        new ClassificationPattern(".*constants.*\\.sql", SqlFileClassification.UTILITY)
    );

    // Directories that typically contain SQL files
    private static final Set<String> SQL_SEARCH_DIRECTORIES = Set.of(
            "sql", "db", "database", "databases", "migrations", "migration",
            "scripts", "script", "data", "resources/sql", "config/sql",
            "conf/sql", "configuration/sql", "db/migration",
            "src/main/resources/db/migration", "src/main/resources/sql"
    );

    // Directories to skip during SQL file search
    private static final Set<String> SKIP_DIRECTORIES = Set.of(
            "node_modules", ".git", "target", "build", "dist", ".idea",
            ".vscode", ".eggs", ".m2", ".gradle"
    );

    /**
     * Analyze all database artifacts in a repository workspace.
     * 
     * @param repositoryId the repository ID
     * @param workspacePath the workspace path to scan
     * @return list of detected database artifacts
     */
    public List<DatabaseArtifact> analyzeDatabases(String repositoryId, Path workspacePath) {
        logger.info("Starting database analysis for repository: {}, workspace: {}", repositoryId, workspacePath);

        List<DatabaseArtifact> artifacts = new ArrayList<>();

        // Step 1: Detect datasource configurations
        artifacts.addAll(detectDatasourceConfigurations(repositoryId, workspacePath));

        // Step 2: Detect SQL files
        artifacts.addAll(detectSqlFiles(repositoryId, workspacePath));

        // Step 3: Detect migration frameworks
        artifacts.addAll(detectMigrationFrameworks(repositoryId, workspacePath));

        // Step 4: Detect ORM frameworks
        artifacts.addAll(detectOrmFrameworks(repositoryId, workspacePath));

        // Step 5: Detect database types from Technology Stack detection
        // (Uses ConfigurationAnalyzer results and pom.xml/build.gradle dependencies)

        logger.info("Database analysis completed for repository: {}. Found {} artifacts.", 
                repositoryId, artifacts.size());

        return artifacts;
    }

    /**
     * Calculate database analysis statistics.
     */
    public Map<String, Object> calculateStatistics(String repositoryId, List<DatabaseArtifact> artifacts) {
        logger.info("Calculating database statistics for repository: {}", repositoryId);

        Map<String, Object> stats = new LinkedHashMap<>();

        // Total databases detected
        Set<DatabaseType> detectedDatabases = artifacts.stream()
                .map(DatabaseArtifact::getDatabaseType)
                .filter(d -> d != null && d != DatabaseType.UNKNOWN)
                .collect(Collectors.toSet());
        stats.put("totalDatabasesDetected", detectedDatabases.size());
        stats.put("detectedDatabaseTypes", Arrays.asList(detectedDatabases.toArray(new DatabaseType[0])));

        // Total datasource configurations
        long datasourceCount = artifacts.stream()
                .filter(a -> a.getArtifactType() == DatabaseArtifact.ArtifactType.DATASOURCE_CONFIG)
                .count();
        stats.put("totalDatasourceConfigurations", datasourceCount);

        // Total SQL files
        long totalSqlFiles = artifacts.stream()
                .filter(a -> isSqlFile(a))
                .count();
        stats.put("totalSqlFiles", totalSqlFiles);

        // SQL files by category
        Map<String, Long> sqlByCategory = new LinkedHashMap<>();
        for (SqlFileClassification classification : SqlFileClassification.values()) {
            if (classification == SqlFileClassification.UNKNOWN) continue;
            long count = artifacts.stream()
                    .filter(a -> isSqlFile(a))
                    .filter(a -> a.getSqlClassification() == classification)
                    .count();
            if (count > 0) {
                sqlByCategory.put(classification.name(), count);
            }
        }
        stats.put("sqlFilesByCategory", sqlByCategory);

        // Migration script count
        long migrationScriptCount = artifacts.stream()
                .filter(a -> isSqlFile(a) && a.getSqlClassification() == SqlFileClassification.MIGRATION)
                .count();
        stats.put("migrationScriptCount", migrationScriptCount);

        // ORM framework count
        Set<String> ormFrameworks = artifacts.stream()
                .map(DatabaseArtifact::getOrmFramework)
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toSet());
        stats.put("totalOrmFrameworks", ormFrameworks.size());
        stats.put("detectedOrmFrameworks", new ArrayList<>(ormFrameworks));

        // Migration framework count
        Set<String> migrationFrameworks = artifacts.stream()
                .map(DatabaseArtifact::getMigrationFramework)
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toSet());
        stats.put("totalMigrationFrameworks", migrationFrameworks.size());
        stats.put("detectedMigrationFrameworks", new ArrayList<>(migrationFrameworks));

        logger.info("Database statistics calculated for repository: {}. Databases: {}, SQL files: {}, ORM: {}, Migration: {}",
                repositoryId, detectedDatabases.size(), totalSqlFiles, ormFrameworks.size(), migrationFrameworks.size());

        return stats;
    }

    // ==================== Detection Methods ====================

    private List<DatabaseArtifact> detectDatasourceConfigurations(String repositoryId, Path workspacePath) {
        List<DatabaseArtifact> artifacts = new ArrayList<>();

        // Search for datasource configuration files
        List<Path> configFiles = findConfigurationFiles(workspacePath);

        for (Path configFile : configFiles) {
            DatabaseArtifact artifact = new DatabaseArtifact();
            artifact.setRepositoryId(repositoryId);
            artifact.setArtifactType(DatabaseArtifact.ArtifactType.DATASOURCE_CONFIG);
            artifact.setFilePath(configFile);
            artifact.setFileName(configFile.getFileName().toString());

            // Extract datasource metadata from file content
            extractDatasourceMetadata(artifact, configFile);

            if (artifact.getDatabaseType() != null && artifact.getDatabaseType() != DatabaseType.UNKNOWN) {
                artifacts.add(artifact);
                logger.info("Found datasource configuration: {} -> {}", artifact.getFileName(), artifact.getDatabaseType());
            }
        }

        // Check application.properties/yml for datasource config presence
        List<Path> springConfigs = findSpringConfigurations(workspacePath);
        for (Path springConfig : springConfigs) {
            DatabaseArtifact artifact = new DatabaseArtifact();
            artifact.setRepositoryId(repositoryId);
            artifact.setArtifactType(DatabaseArtifact.ArtifactType.DATASOURCE_CONFIG);
            artifact.setFilePath(springConfig);
            artifact.setFileName(springConfig.getFileName().toString());

            extractDatasourceMetadata(artifact, springConfig);

            if (artifact.getJdbcUrl() != null || artifact.getDatabaseType() != null) {
                // Check if already added from previous search
                boolean alreadyAdded = artifacts.stream()
                        .anyMatch(a -> a.getFilePath().equals(springConfig));
                if (!alreadyAdded && (artifact.getJdbcUrl() != null || artifact.getDatabaseType() != null)) {
                    artifacts.add(artifact);
                    logger.info("Found Spring datasource config: {} -> {}", artifact.getFileName(), 
                            artifact.getDatabaseType() != null ? artifact.getDatabaseType() : "configured");
                }
            }
        }

        return artifacts;
    }

    private List<DatabaseArtifact> detectSqlFiles(String repositoryId, Path workspacePath) {
        List<DatabaseArtifact> artifacts = new ArrayList<>();

        // Search in known SQL directories
        for (String sqlDir : SQL_SEARCH_DIRECTORIES) {
            Path searchPath = workspacePath.resolve(sqlDir);
            if (Files.exists(searchPath)) {
                discoverSqlFiles(repositoryId, searchPath, artifacts);
            }
        }

        // Also search the entire workspace for SQL files in common locations
        discoverSqlFilesInDirectory(repositoryId, workspacePath, artifacts, 0, 4);

        return artifacts;
    }

    private List<DatabaseArtifact> detectMigrationFrameworks(String repositoryId, Path workspacePath) {
        List<DatabaseArtifact> artifacts = new ArrayList<>();

        // Check for Flyway configuration in application.properties/yml
        List<Path> springConfigs = findSpringConfigurations(workspacePath);
        for (Path config : springConfigs) {
            try {
                String content = Files.readString(config);
                
                if (content.contains("spring.flyway") || content.contains("flyway.enabled")) {
                    DatabaseArtifact artifact = new DatabaseArtifact();
                    artifact.setRepositoryId(repositoryId);
                    artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                    artifact.setFilePath(config);
                    artifact.setFileName(config.getFileName().toString());
                    artifact.setMigrationFramework("FLYWAY");
                    artifacts.add(artifact);
                    logger.info("Flyway migration framework detected in: {}", config.getFileName());
                }

                if (content.contains("spring.liquibase")) {
                    DatabaseArtifact artifact = new DatabaseArtifact();
                    artifact.setRepositoryId(repositoryId);
                    artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                    artifact.setFilePath(config);
                    artifact.setFileName(config.getFileName().toString());
                    artifact.setMigrationFramework("LIQUIBASE");
                    artifacts.add(artifact);
                    logger.info("Liquibase migration framework detected in: {}", config.getFileName());
                }
            } catch (IOException e) {
                logger.warn("Failed to read configuration file: {}", config, e);
            }
        }

        // Check pom.xml for Flyway/Liquibase dependencies
        Path[] buildFiles = { workspacePath.resolve("pom.xml") };
        for (Path buildFile : buildFiles) {
            if (Files.exists(buildFile)) {
                try {
                    String content = Files.readString(buildFile);
                    
                    if (content.contains("flyway")) {
                        DatabaseArtifact artifact = new DatabaseArtifact();
                        artifact.setRepositoryId(repositoryId);
                        artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                        artifact.setFilePath(buildFile);
                        artifact.setFileName(buildFile.getFileName().toString());
                        artifact.setMigrationFramework("FLYWAY");
                        artifacts.add(artifact);
                        logger.info("Flyway dependency found in: {}", buildFile.getFileName());
                    }

                    if (content.contains("liquibase")) {
                        DatabaseArtifact artifact = new DatabaseArtifact();
                        artifact.setRepositoryId(repositoryId);
                        artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                        artifact.setFilePath(buildFile);
                        artifact.setFileName(buildFile.getFileName().toString());
                        artifact.setMigrationFramework("LIQUIBASE");
                        artifacts.add(artifact);
                        logger.info("Liquibase dependency found in: {}", buildFile.getFileName());
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read build file: {}", buildFile, e);
                }
            }
        }

        // Check build.gradle for Flyway/Liquibase dependencies
        Path[] gradleFiles = { workspacePath.resolve("build.gradle"), workspacePath.resolve("build.gradle.kts") };
        for (Path gradleFile : gradleFiles) {
            if (Files.exists(gradleFile)) {
                try {
                    String content = Files.readString(gradleFile);
                    
                    if (content.contains("flyway") || content.contains("org.flywaydb")) {
                        DatabaseArtifact artifact = new DatabaseArtifact();
                        artifact.setRepositoryId(repositoryId);
                        artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                        artifact.setFilePath(gradleFile);
                        artifact.setFileName(gradleFile.getFileName().toString());
                        artifact.setMigrationFramework("FLYWAY");
                        artifacts.add(artifact);
                        logger.info("Flyway dependency found in: {}", gradleFile.getFileName());
                    }

                    if (content.contains("liquibase") || content.contains("org.liquibase")) {
                        DatabaseArtifact artifact = new DatabaseArtifact();
                        artifact.setRepositoryId(repositoryId);
                        artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_CONFIG);
                        artifact.setFilePath(gradleFile);
                        artifact.setFileName(gradleFile.getFileName().toString());
                        artifact.setMigrationFramework("LIQUIBASE");
                        artifacts.add(artifact);
                        logger.info("Liquibase dependency found in: {}", gradleFile.getFileName());
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read build file: {}", gradleFile, e);
                }
            }
        }

        return artifacts;
    }

    private List<DatabaseArtifact> detectOrmFrameworks(String repositoryId, Path workspacePath) {
        List<DatabaseArtifact> artifacts = new ArrayList<>();

        // Check for ORM frameworks in pom.xml and build.gradle dependencies
        Path[] buildFiles = { workspacePath.resolve("pom.xml") };
        for (Path buildFile : buildFiles) {
            if (Files.exists(buildFile)) {
                try {
                    String content = Files.readString(buildFile);
                    
                    for (Map.Entry<String, String> entry : ORM_FRAMEWORK_PATTERNS.entrySet()) {
                        if (content.contains(entry.getKey())) {
                            DatabaseArtifact artifact = new DatabaseArtifact();
                            artifact.setRepositoryId(repositoryId);
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.ORM_CONFIG);
                            artifact.setFilePath(buildFile);
                            artifact.setFileName(buildFile.getFileName().toString());
                            artifact.setOrmFramework(entry.getValue());
                            artifacts.add(artifact);
                            logger.info("ORM framework '{}' detected in: {}", entry.getValue(), buildFile.getFileName());
                            break; // One ORM per file is sufficient
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read build file: {}", buildFile, e);
                }
            }
        }

        return artifacts;
    }

    // ==================== Metadata Extraction ====================

    private void extractDatasourceMetadata(DatabaseArtifact artifact, Path filePath) {
        try {
            String content = Files.readString(filePath);
            
            // Extract JDBC URL patterns
            for (Map.Entry<String, DatabaseType> entry : URL_TO_DATABASE_TYPE.entrySet()) {
                if (content.contains(entry.getKey())) {
                    artifact.setDatabaseType(entry.getValue());
                    
                    // Try to extract the actual JDBC URL
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.contains("jdbc") && trimmed.contains(":")) {
                            // Extract URL from spring.datasource.url or similar
                            if (trimmed.contains("=")) {
                                String value = trimmed.split("=")[1].trim().replaceAll("[\"';]", "");
                                artifact.setJdbcUrl(value);
                            } else if (trimmed.contains("url:")) {
                                String value = trimmed.substring(4).trim().replaceAll("[\"']", "");
                                artifact.setJdbcUrl(value);
                            }
                        }
                    }
                    break;
                }
            }

            // Extract driver class
            for (Map.Entry<String, DatabaseType> entry : DRIVER_TO_DATABASE_TYPE.entrySet()) {
                if (content.contains(entry.getKey())) {
                    if (artifact.getDatabaseType() == null) {
                        artifact.setDatabaseType(entry.getValue());
                    }
                    String[] lines = content.split("\n");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.toLowerCase().contains("driver") && trimmed.contains(".")) {
                            if (trimmed.contains("=")) {
                                artifact.setDriverClass(trimmed.split("=")[1].trim().replaceAll("[\"';]", ""));
                            } else if (trimmed.contains(":")) {
                                artifact.setDriverClass(trimmed.substring(6).trim().replaceAll("[\"']", ""));
                            }
                        }
                    }
                    break;
                }
            }

            // Extract datasource name
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.toLowerCase().contains("datasource") || trimmed.toLowerCase().contains("spring.datasource")) {
                    if (trimmed.contains("=")) {
                        // Look for name property
                        if (trimmed.toLowerCase().contains("name")) {
                            String value = trimmed.split("=")[1].trim().replaceAll("[\"']", "");
                            if (!value.isEmpty()) {
                                artifact.setDatasourceName(value);
                            }
                        }
                    }
                }
            }

            // Extract schema name
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.toLowerCase().contains("schema")) {
                    if (trimmed.contains("=")) {
                        artifact.setSchemaName(trimmed.split("=")[1].trim().replaceAll("[\"']", ""));
                    } else if (trimmed.contains(":")) {
                        artifact.setSchemaName(trimmed.substring(6).trim().replaceAll("[\"']", ""));
                    }
                }
            }

        } catch (IOException e) {
            logger.warn("Failed to extract metadata from file: {}", filePath, e);
        }
    }

    private void discoverSqlFiles(String repositoryId, Path searchPath, List<DatabaseArtifact> artifacts) {
        try {
            Files.walk(searchPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".sql"))
                .forEach(sqlFile -> {
                    DatabaseArtifact artifact = new DatabaseArtifact();
                    artifact.setRepositoryId(repositoryId);
                    artifact.setFilePath(sqlFile);
                    artifact.setFileName(sqlFile.getFileName().toString());
                    artifact.setSqlClassification(classifySqlFile(sqlFile));
                    
                    // Determine the appropriate artifact type based on classification
                    switch (artifact.getSqlClassification()) {
                        case MIGRATION:
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_SCRIPT);
                            break;
                        case DDL:
                        case VIEW:
                        case TRIGGER:
                        case FUNCTION:
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.DDL_SCRIPT);
                            break;
                        case DML:
                        case SEED_DATA:
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.DML_SCRIPT);
                            break;
                        case STORED_PROCEDURE:
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.SQL_FILE);
                            break;
                        default:
                            artifact.setArtifactType(DatabaseArtifact.ArtifactType.SQL_FILE);
                            break;
                    }

                    // Set migration framework if detected from filename (Flyway patterns)
                    if (artifact.getSqlClassification() == SqlFileClassification.MIGRATION) {
                        String fileName = sqlFile.getFileName().toString();
                        if (fileName.matches("V.*\\.sql")) {
                            artifact.setMigrationFramework("FLYWAY");
                        }
                    }

                    artifacts.add(artifact);
                });
        } catch (IOException e) {
            logger.warn("Failed to walk directory for SQL files: {}", searchPath, e);
        }
    }

    private void discoverSqlFilesInDirectory(String repositoryId, Path dir, List<DatabaseArtifact> artifacts, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) return;

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String dirName = entry.getFileName().toString().toLowerCase();
                    if (!SKIP_DIRECTORIES.contains(dirName)) {
                        discoverSqlFilesInDirectory(repositoryId, entry, artifacts, currentDepth + 1, maxDepth);
                    }
                } else if (entry.toString().toLowerCase().endsWith(".sql")) {
                    // Check if path contains SQL-related directory names
                    boolean isInSqlDir = isInSqlRelatedDirectory(entry);
                    if (isInSqlDir || currentDepth <= 3) {
                        DatabaseArtifact artifact = new DatabaseArtifact();
                        artifact.setRepositoryId(repositoryId);
                        artifact.setFilePath(entry);
                        artifact.setFileName(entry.getFileName().toString());
                        artifact.setSqlClassification(classifySqlFile(entry));
                        
                        switch (artifact.getSqlClassification()) {
                            case MIGRATION:
                                artifact.setArtifactType(DatabaseArtifact.ArtifactType.MIGRATION_SCRIPT);
                                break;
                            case DDL:
                            case VIEW:
                            case TRIGGER:
                            case FUNCTION:
                                artifact.setArtifactType(DatabaseArtifact.ArtifactType.DDL_SCRIPT);
                                break;
                            case DML:
                            case SEED_DATA:
                                artifact.setArtifactType(DatabaseArtifact.ArtifactType.DML_SCRIPT);
                                break;
                            default:
                                artifact.setArtifactType(DatabaseArtifact.ArtifactType.SQL_FILE);
                                break;
                        }

                        // Set migration framework for Flyway patterns
                        if (artifact.getSqlClassification() == SqlFileClassification.MIGRATION) {
                            String fileName = entry.getFileName().toString();
                            if (fileName.matches("V.*\\.sql")) {
                                artifact.setMigrationFramework("FLYWAY");
                            }
                        }

                        artifacts.add(artifact);
                    }
                }
            }
        } catch (IOException e) {
            // Skip directories we can't read
        }
    }

    private boolean isInSqlRelatedDirectory(Path filePath) {
        String path = filePath.toString().toLowerCase();
        for (String dir : SQL_SEARCH_DIRECTORIES) {
            if (path.contains(dir.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private SqlFileClassification classifySqlFile(Path sqlFile) {
        String fileName = sqlFile.getFileName().toString().toLowerCase();
        
        for (ClassificationPattern pattern : SQL_CLASSIFICATION_PATTERNS) {
            if (fileName.matches(pattern.regex)) {
                return pattern.classification;
            }
        }

        // Check parent directories for context
        Path parent = sqlFile.getParent();
        while (parent != null) {
            String parentName = parent.getFileName().toString().toLowerCase();
            if ("migration".equals(parentName) || "migrations".equals(parentName)) {
                return SqlFileClassification.MIGRATION;
            }
            if ("seed".equals(parentName) || "fixtures".equals(parentName)) {
                return SqlFileClassification.SEED_DATA;
            }
            if (parentName.equals("sql") || parentName.equals("db") || parentName.equals("databases")) {
                break; // Let file name determine classification
            }
            Path grandparent = parent.getParent();
            if (grandparent == null) break;
            parent = grandparent;
        }

        return SqlFileClassification.UNKNOWN;
    }

    // ==================== Helper Methods ====================

    private boolean isSqlFile(DatabaseArtifact artifact) {
        return artifact.getArtifactType() == DatabaseArtifact.ArtifactType.SQL_FILE
                || artifact.getArtifactType() == DatabaseArtifact.ArtifactType.DDL_SCRIPT
                || artifact.getArtifactType() == DatabaseArtifact.ArtifactType.DML_SCRIPT
                || artifact.getArtifactType() == DatabaseArtifact.ArtifactType.MIGRATION_SCRIPT;
    }

    private List<Path> findConfigurationFiles(Path workspacePath) {
        List<Path> found = new ArrayList<>();
        
        // Find properties and yml/yaml files that contain datasource configurations
        String[] extensions = { ".properties", ".yml", ".yaml" };
        for (String ext : extensions) {
            try {
                Files.walk(workspacePath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(ext))
                    .filter(p -> {
                        String fileName = p.getFileName().toString().toLowerCase();
                        return fileName.contains("application") 
                                || fileName.contains("datasource") 
                                || fileName.contains("db")
                                || fileName.contains("database");
                    })
                    .forEach(found::add);
            } catch (IOException e) {
                // Skip directories we can't walk
            }
        }

        return found;
    }

    private List<Path> findSpringConfigurations(Path workspacePath) {
        List<Path> found = new ArrayList<>();
        
        String[] springFiles = { "application.properties", "application.yml", "application.yaml",
                                   "bootstrap.properties", "bootstrap.yml" };
        
        for (String fileName : springFiles) {
            Path path = workspacePath.resolve("src/main/resources").resolve(fileName);
            if (Files.exists(path)) {
                found.add(path);
            } else {
                // Try other standard locations
                Path[] alternativePaths = {
                    workspacePath.resolve(fileName),
                    workspacePath.resolve("src/main/resources/bootstrap").resolve(fileName)
                };
                for (Path altPath : alternativePaths) {
                    if (Files.exists(altPath)) {
                        found.add(altPath);
                        break;
                    }
                }
            }
        }

        return found;
    }

    // ==================== Inner Classes ====================

    private static class ClassificationPattern {
        final String regex;
        final SqlFileClassification classification;

        ClassificationPattern(String regex, SqlFileClassification classification) {
            this.regex = regex;
            this.classification = classification;
        }
    }
}