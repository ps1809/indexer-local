package com.projectiq.indexerlocal.model;

import java.util.List;

/**
 * Represents metadata extracted from a Java class/interface/enum/record.
 */
public class ClassInfo {
    
    private Long id;
    private Long fileIndexId;
    private String fileName;
    private String filePath;
    private String className;
    private String classType; // CLASS, INTERFACE, ENUM, RECORD
    private String visibility; // PUBLIC, PROTECTED, PRIVATE
    private String superClass;
    private List<String> interfaces;
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    private List<MethodInfo> constructors;
    private List<AnnotationInfo> annotations;
    private String package_;
    private String genericTypeParameters;
    private boolean abstract_;
    private boolean final_;
    
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public List<MethodInfo> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<MethodInfo> constructors) {
        this.constructors = constructors;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }

    public String getPackage_() {
        return package_;
    }

    public void setPackage_(String package_) {
        this.package_ = package_;
    }

    public String getGenericTypeParameters() {
        return genericTypeParameters;
    }

    public void setGenericTypeParameters(String genericTypeParameters) {
        this.genericTypeParameters = genericTypeParameters;
    }

    public boolean isAbstract() {
        return abstract_;
    }

    public void setAbstract(boolean abstract_) {
        this.abstract_ = abstract_;
    }

    public boolean isFinal() {
        return final_;
    }

    public void setFinal(boolean final_) {
        this.final_ = final_;
    }
}
