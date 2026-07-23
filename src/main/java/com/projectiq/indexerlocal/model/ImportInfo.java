package com.projectiq.indexerlocal.model;

/**
 * Represents metadata extracted from a Java import statement.
 */
public class ImportInfo {
    
    private Long id;
    private Long fileIndexId;
    private String importName;
    private boolean isStatic;
    private String importType; // NORMAL, STATIC, WILDCARD
    
    public ImportInfo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFileIndexId() {
        return fileIndexId;
    }

    public void setFileIndexId(Long fileIndexId) {
        this.fileIndexId = fileIndexId;
    }

    public String getImportName() {
        return importName;
    }

    public void setImportName(String importName) {
        this.importName = importName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String getImportType() {
        return importType;
    }

    public void setType(String importType) {
        this.importType = importType;
    }

    public void setIsWildcard(boolean isWildcard) {
        // Wildcard imports can be determined from importName containing .*
        this.importType = importType != null ? importType : this.importType;
    }

    public void setImportStatement(String statement) {
        // For wildcard imports, store the package name without .*
        if (statement != null && statement.endsWith(".*")) {
            this.importName = statement.substring(0, statement.length() - 2);
        } else {
            this.importName = statement;
        }
    }
}
