package com.projectiq.indexerlocal.model;

/**
 * Represents metadata extracted from a Java import statement.
 */
public class ImportInfo {
    
    private Long id;
    private Long fileIndexId;
    private String importName;
    private boolean isStatic;
    
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
}