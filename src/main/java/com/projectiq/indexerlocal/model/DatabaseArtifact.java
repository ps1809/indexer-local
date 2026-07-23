package com.projectiq.indexerlocal.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Represents a detected database artifact including datasource configurations,
 * SQL files, migration files, and ORM/migration framework metadata.
 */
public class DatabaseArtifact {

    private String id;
    private String repositoryId;
    private ArtifactType artifactType;
    private Path filePath;
    private String fileName;
    
    // Database information
    private DatabaseType databaseType;
    private String datasourceName;
    private String jdbcUrl;
    private String driverClass;
    private String schemaName;
    
    // SQL file classification
    private SqlFileClassification sqlClassification;
    
    // Framework information
    private String migrationFramework;  // FLYWAY, LIQUIBASE
    private String ormFramework;        // SPRING_DATA_JPA, HIBERNATE, MYBATIS, JDBC, SPRING_JDBC
    
    // Metadata
    private LocalDateTime analyzedAt;

    public enum ArtifactType {
        DATASOURCE_CONFIG,      /* Datasource configuration file */
        SQL_FILE,              /* SQL script file */
        DDL_SCRIPT,            /* DDL script */
        DML_SCRIPT,            /* DML script */
        MIGRATION_SCRIPT,      /* Migration script (Flyway/Liquibase) */
        ORM_CONFIG,            /* ORM configuration */
        MIGRATION_CONFIG,      /* Migration framework configuration */
        DATABASE_METADATA      /* General database metadata */
    }

    public DatabaseArtifact() {
        this.analyzedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public SqlFileClassification getSqlClassification() {
        return sqlClassification;
    }

    public void setSqlClassification(SqlFileClassification sqlClassification) {
        this.sqlClassification = sqlClassification;
    }

    public String getMigrationFramework() {
        return migrationFramework;
    }

    public void setMigrationFramework(String migrationFramework) {
        this.migrationFramework = migrationFramework;
    }

    public String getOrmFramework() {
        return ormFramework;
    }

    public void setOrmFramework(String ormFramework) {
        this.ormFramework = ormFramework;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    @Override
    public String toString() {
        return "DatabaseArtifact{" +
                "artifactType=" + artifactType +
                ", fileName='" + fileName + '\'' +
                ", databaseType=" + databaseType +
                ", sqlClassification=" + sqlClassification +
                ", migrationFramework='" + migrationFramework + '\'' +
                ", ormFramework='" + ormFramework + '\'' +
                '}';
    }
}