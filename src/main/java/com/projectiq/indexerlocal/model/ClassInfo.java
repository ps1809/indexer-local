package com.projectiq.indexerlocal.model;

import java.util.List;

/**
 * Represents metadata extracted from a Java class/interface/enum/record.
 */
public class ClassInfo {
    
    private Long id;
    private Long fileIndexId;
    private String className;
    private String classType; // CLASS, INTERFACE, ENUM, RECORD
    private String visibility; // PUBLIC, PROTECTED, PRIVATE
    private String superClass;
    private List<String> interfaces;
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    private List<AnnotationInfo> annotations;
    
    public ClassInfo() {
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }
}