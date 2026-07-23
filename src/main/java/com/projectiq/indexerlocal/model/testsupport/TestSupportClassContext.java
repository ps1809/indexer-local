package com.projectiq.indexerlocal.model.testsupport;

import com.projectiq.indexerlocal.model.AnnotationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents class context metadata for unit test generation support.
 */
@Schema(description = "Class context metadata for test generation")
public class TestSupportClassContext {

    @Schema(description = "Class ID in the index")
    private Long classId;

    @Schema(description = "Fully qualified class name")
    private String fullyQualifiedName;

    @Schema(description = "Simple class name")
    private String className;

    @Schema(description = "Package name")
    private String packageName;

    @Schema(description = "Class type (CLASS, INTERFACE, ENUM, RECORD)")
    private String classType;

    @Schema(description = "Visibility modifier (PUBLIC, PROTECTED, PRIVATE)")
    private String visibility;

    @Schema(description = "Superclass name if any")
    private String superClass;

    @Schema(description = "Implemented interfaces")
    private List<String> interfaces;

    @Schema(description = "Class-level annotations")
    private List<AnnotationInfo> annotations;

    @Schema(description = "Whether the class is abstract")
    private boolean isAbstract;

    @Schema(description = "Whether the class is final")
    private boolean isFinal;

    @Schema(description = "Generic type parameters")
    private String genericTypeParameters;

    @Schema(description = "Source file path")
    private String sourceFilePath;

    public TestSupportClassContext() {
    }

    // Getters and Setters

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public String getGenericTypeParameters() {
        return genericTypeParameters;
    }

    public void setGenericTypeParameters(String genericTypeParameters) {
        this.genericTypeParameters = genericTypeParameters;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }
}