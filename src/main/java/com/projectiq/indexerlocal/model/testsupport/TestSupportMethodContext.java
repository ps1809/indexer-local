package com.projectiq.indexerlocal.model.testsupport;

import com.projectiq.indexerlocal.model.AnnotationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents method context metadata for unit test generation support.
 */
@Schema(description = "Method context metadata for test generation")
public class TestSupportMethodContext {

    @Schema(description = "Method ID in the index")
    private Long methodId;

    @Schema(description = "Class ID this method belongs to")
    private Long classId;

    @Schema(description = "Method name")
    private String methodName;

    @Schema(description = "Full method signature")
    private String methodSignature;

    @Schema(description = "Return type")
    private String returnType;

    @Schema(description = "Visibility modifier (PUBLIC, PROTECTED, PRIVATE)")
    private String visibility;

    @Schema(description = "Whether the method is static")
    private boolean isStatic;

    @Schema(description = "Whether the method is abstract")
    private boolean isAbstract;

    @Schema(description = "Whether the method is final")
    private boolean isFinal;

    @Schema(description = "Method type (STATIC, CONSTRUCTOR, INSTANCE)")
    private String methodType;

    @Schema(description = "Parameter types")
    private List<String> parameterTypes;

    @Schema(description = "Parameter names")
    private List<String> parameterNames;

    @Schema(description = "Full parameter signatures")
    private List<String> parameters;

    @Schema(description = "Declared exceptions")
    private List<String> exceptions;

    @Schema(description = "Method-level annotations")
    private List<AnnotationInfo> annotations;

    public TestSupportMethodContext() {
    }

    // Getters and Setters

    public Long getMethodId() {
        return methodId;
    }

    public void setMethodId(Long methodId) {
        this.methodId = methodId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
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

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }
}