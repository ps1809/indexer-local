package com.projectiq.indexerlocal.model.relationshipsearch;

/**
 * Represents a single relationship entry returned by the Relationship Search Engine.
 */
public class RelationshipEntry {

    private String sourceSymbol;
    private String sourceSymbolType;
    private String sourceFilePath;
    private String sourcePackage;
    private String sourceModule;
    private Integer sourceLineNumber;

    private String targetSymbol;
    private String targetSymbolType;
    private String targetFilePath;
    private String targetPackage;
    private String targetModule;
    private Integer targetLineNumber;

    private RelationshipType relationshipType;
    private int traversalDepth;

    public RelationshipEntry() {
    }

    public String getSourceSymbol() {
        return sourceSymbol;
    }

    public void setSourceSymbol(String sourceSymbol) {
        this.sourceSymbol = sourceSymbol;
    }

    public String getSourceSymbolType() {
        return sourceSymbolType;
    }

    public void setSourceSymbolType(String sourceSymbolType) {
        this.sourceSymbolType = sourceSymbolType;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public String getSourcePackage() {
        return sourcePackage;
    }

    public void setSourcePackage(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public Integer getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setSourceLineNumber(Integer sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
    }

    public String getTargetSymbol() {
        return targetSymbol;
    }

    public void setTargetSymbol(String targetSymbol) {
        this.targetSymbol = targetSymbol;
    }

    public String getTargetSymbolType() {
        return targetSymbolType;
    }

    public void setTargetSymbolType(String targetSymbolType) {
        this.targetSymbolType = targetSymbolType;
    }

    public String getTargetFilePath() {
        return targetFilePath;
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public Integer getTargetLineNumber() {
        return targetLineNumber;
    }

    public void setTargetLineNumber(Integer targetLineNumber) {
        this.targetLineNumber = targetLineNumber;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public int getTraversalDepth() {
        return traversalDepth;
    }

    public void setTraversalDepth(int traversalDepth) {
        this.traversalDepth = traversalDepth;
    }
}