package com.projectiq.indexerlocal.model;

import java.util.List;

/**
 * Represents the index result for a single Java source file.
 */
public class FileIndex {
    
    private Long id;
    private String filePath;
    private String fileName;
    private long classCount;
    private long methodCount;
    private long fieldCount;
    private long annotationCount;
    private List<ClassInfo> classes;
    private List<ImportInfo> imports;
    
    public FileIndex() {
    }

    public FileIndex(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.classes = new java.util.ArrayList<>();
        this.imports = new java.util.ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getClassCount() {
        return classCount;
    }

    public void setClassCount(long classCount) {
        this.classCount = classCount;
    }

    public long getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(long methodCount) {
        this.methodCount = methodCount;
    }

    public long getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(long fieldCount) {
        this.fieldCount = fieldCount;
    }

    public long getAnnotationCount() {
        return annotationCount;
    }

    public void setAnnotationCount(long annotationCount) {
        this.annotationCount = annotationCount;
    }

    public List<ClassInfo> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassInfo> classes) {
        this.classes = classes;
    }

    public List<ImportInfo> getImports() {
        return imports;
    }

    public void setImports(List<ImportInfo> imports) {
        this.imports = imports;
    }
}