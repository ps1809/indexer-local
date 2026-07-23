package com.projectiq.indexerlocal.model.testsupport;

import com.projectiq.indexerlocal.model.AnnotationInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Represents Spring component context metadata for unit test generation support.
 */
@Schema(description = "Spring component context metadata for test generation")
public class TestSupportSpringContext {

    @Schema(description = "Spring component ID")
    private Long componentId;

    @Schema(description = "Component type (SERVICE, REPOSITORY, CONTROLLER, etc.)")
    private String componentType;

    @Schema(description = "Component name")
    private String componentName;

    @Schema(description = "Class name")
    private String className;

    @Schema(description = "Package name")
    private String packageName;

    @Schema(description = "Bean name if specified")
    private String beanName;

    @Schema(description = "Whether component has @Autowired dependencies")
    private boolean hasAutowired;

    @Schema(description = "Whether component has @Inject dependencies")
    private boolean hasInject;

    @Schema(description = "Whether component has @Resource dependencies")
    private boolean hasResource;

    @Schema(description = "Whether component uses constructor injection")
    private boolean hasConstructorInjection;

    @Schema(description = "Whether component uses setter injection")
    private boolean hasSetterInjection;

    @Schema(description = "Whether component has @Transactional annotation")
    private boolean hasTransactional;

    @Schema(description = "Whether component is a @Configuration class")
    private boolean isConfiguration;

    @Schema(description = "All Spring-related annotations")
    private List<AnnotationInfo> annotations;

    @Schema(description = "Injected dependency class names")
    private List<String> injectedDependencies;

    public TestSupportSpringContext() {
    }

    // Getters and Setters

    public Long getComponentId() {
        return componentId;
    }

    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
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

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public boolean isHasAutowired() {
        return hasAutowired;
    }

    public void setHasAutowired(boolean hasAutowired) {
        this.hasAutowired = hasAutowired;
    }

    public boolean isHasInject() {
        return hasInject;
    }

    public void setHasInject(boolean hasInject) {
        this.hasInject = hasInject;
    }

    public boolean isHasResource() {
        return hasResource;
    }

    public void setHasResource(boolean hasResource) {
        this.hasResource = hasResource;
    }

    public boolean isHasConstructorInjection() {
        return hasConstructorInjection;
    }

    public void setHasConstructorInjection(boolean hasConstructorInjection) {
        this.hasConstructorInjection = hasConstructorInjection;
    }

    public boolean isHasSetterInjection() {
        return hasSetterInjection;
    }

    public void setHasSetterInjection(boolean hasSetterInjection) {
        this.hasSetterInjection = hasSetterInjection;
    }

    public boolean isHasTransactional() {
        return hasTransactional;
    }

    public void setHasTransactional(boolean hasTransactional) {
        this.hasTransactional = hasTransactional;
    }

    public boolean isConfiguration() {
        return isConfiguration;
    }

    public void setConfiguration(boolean configuration) {
        isConfiguration = configuration;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationInfo> annotations) {
        this.annotations = annotations;
    }

    public List<String> getInjectedDependencies() {
        return injectedDependencies;
    }

    public void setInjectedDependencies(List<String> injectedDependencies) {
        this.injectedDependencies = injectedDependencies;
    }
}