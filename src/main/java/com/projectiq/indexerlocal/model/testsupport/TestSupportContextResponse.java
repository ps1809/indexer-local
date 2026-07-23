package com.projectiq.indexerlocal.model.testsupport;

import com.projectiq.indexerlocal.model.Dependency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response model for structured unit test context generation.
 */
@Schema(description = "Response containing structured test generation context")
public class TestSupportContextResponse {

    @Schema(description = "Repository ID")
    private String repositoryId;

    @Schema(description = "Repository name")
    private String repositoryName;

    @Schema(description = "Class details included in context")
    private List<TestSupportClassDetail> classDetails;

    @Schema(description = "Method details included in context")
    private List<TestSupportMethodContext> methodDetails;

    @Schema(description = "Spring component contexts")
    private List<TestSupportSpringContext> springContexts;

    @Schema(description = "Dependency metadata")
    private TestSupportDependencyContext dependencyContext;

    @Schema(description = "Test generation metadata")
    private TestGenerationMetadata testGenerationMetadata;

    @Schema(description = "Context generation timestamp")
    private LocalDateTime generatedAt;

    @Schema(description = "Generation duration in milliseconds")
    private Long generationDurationMs;

    public TestSupportContextResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public List<TestSupportClassDetail> getClassDetails() {
        return classDetails;
    }

    public void setClassDetails(List<TestSupportClassDetail> classDetails) {
        this.classDetails = classDetails;
    }

    public List<TestSupportMethodContext> getMethodDetails() {
        return methodDetails;
    }

    public void setMethodDetails(List<TestSupportMethodContext> methodDetails) {
        this.methodDetails = methodDetails;
    }

    public List<TestSupportSpringContext> getSpringContexts() {
        return springContexts;
    }

    public void setSpringContexts(List<TestSupportSpringContext> springContexts) {
        this.springContexts = springContexts;
    }

    public TestSupportDependencyContext getDependencyContext() {
        return dependencyContext;
    }

    public void setDependencyContext(TestSupportDependencyContext dependencyContext) {
        this.dependencyContext = dependencyContext;
    }

    public TestGenerationMetadata getTestGenerationMetadata() {
        return testGenerationMetadata;
    }

    public void setTestGenerationMetadata(TestGenerationMetadata testGenerationMetadata) {
        this.testGenerationMetadata = testGenerationMetadata;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getGenerationDurationMs() {
        return generationDurationMs;
    }

    public void setGenerationDurationMs(Long generationDurationMs) {
        this.generationDurationMs = generationDurationMs;
    }

    /**
     * Dependency context for test generation.
     */
    @Schema(description = "Dependency context for test generation")
    public static class TestSupportDependencyContext {

        @Schema(description = "External dependencies")
        private List<Dependency> externalDependencies;

        @Schema(description = "Internal project dependencies")
        private List<Dependency> internalDependencies;

        @Schema(description = "Build system type")
        private String buildSystemType;

        @Schema(description = "Test framework dependencies detected")
        private List<String> testFrameworks;

        @Schema(description = "Mocking frameworks detected")
        private List<String> mockingFrameworks;

        public TestSupportDependencyContext() {
        }

        // Getters and Setters

        public List<Dependency> getExternalDependencies() {
            return externalDependencies;
        }

        public void setExternalDependencies(List<Dependency> externalDependencies) {
            this.externalDependencies = externalDependencies;
        }

        public List<Dependency> getInternalDependencies() {
            return internalDependencies;
        }

        public void setInternalDependencies(List<Dependency> internalDependencies) {
            this.internalDependencies = internalDependencies;
        }

        public String getBuildSystemType() {
            return buildSystemType;
        }

        public void setBuildSystemType(String buildSystemType) {
            this.buildSystemType = buildSystemType;
        }

        public List<String> getTestFrameworks() {
            return testFrameworks;
        }

        public void setTestFrameworks(List<String> testFrameworks) {
            this.testFrameworks = testFrameworks;
        }

        public List<String> getMockingFrameworks() {
            return mockingFrameworks;
        }

        public void setMockingFrameworks(List<String> mockingFrameworks) {
            this.mockingFrameworks = mockingFrameworks;
        }
    }

    /**
     * Test generation metadata.
     */
    @Schema(description = "Test generation metadata")
    public static class TestGenerationMetadata {

        @Schema(description = "Candidate public methods for testing")
        private List<CandidateMethod> candidateMethods;

        @Schema(description = "Summary of dependencies")
        private Map<String, Integer> dependencySummary;

        @Schema(description = "Required mocks (metadata only)")
        private List<MockRequirement> requiredMocks;

        @Schema(description = "Configuration requirements")
        private List<String> configurationRequirements;

        @Schema(description = "Total classes analyzed")
        private int totalClassesAnalyzed;

        @Schema(description = "Total methods analyzed")
        private int totalMethodsAnalyzed;

        public TestGenerationMetadata() {
        }

        // Getters and Setters

        public List<CandidateMethod> getCandidateMethods() {
            return candidateMethods;
        }

        public void setCandidateMethods(List<CandidateMethod> candidateMethods) {
            this.candidateMethods = candidateMethods;
        }

        public Map<String, Integer> getDependencySummary() {
            return dependencySummary;
        }

        public void setDependencySummary(Map<String, Integer> dependencySummary) {
            this.dependencySummary = dependencySummary;
        }

        public List<MockRequirement> getRequiredMocks() {
            return requiredMocks;
        }

        public void setRequiredMocks(List<MockRequirement> requiredMocks) {
            this.requiredMocks = requiredMocks;
        }

        public List<String> getConfigurationRequirements() {
            return configurationRequirements;
        }

        public void setConfigurationRequirements(List<String> configurationRequirements) {
            this.configurationRequirements = configurationRequirements;
        }

        public int getTotalClassesAnalyzed() {
            return totalClassesAnalyzed;
        }

        public void setTotalClassesAnalyzed(int totalClassesAnalyzed) {
            this.totalClassesAnalyzed = totalClassesAnalyzed;
        }

        public int getTotalMethodsAnalyzed() {
            return totalMethodsAnalyzed;
        }

        public void setTotalMethodsAnalyzed(int totalMethodsAnalyzed) {
            this.totalMethodsAnalyzed = totalMethodsAnalyzed;
        }
    }

    /**
     * Candidate method for test generation.
     */
    @Schema(description = "Candidate method for test generation")
    public static class CandidateMethod {

        @Schema(description = "Method ID")
        private Long methodId;

        @Schema(description = "Class ID")
        private Long classId;

        @Schema(description = "Class name")
        private String className;

        @Schema(description = "Method name")
        private String methodName;

        @Schema(description = "Method signature")
        private String methodSignature;

        @Schema(description = "Return type")
        private String returnType;

        @Schema(description = "Parameter count")
        private int parameterCount;

        @Schema(description = "Exception declarations")
        private List<String> exceptions;

        @Schema(description = "Whether method is static")
        private boolean isStatic;

        public CandidateMethod() {
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

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
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

        public int getParameterCount() {
            return parameterCount;
        }

        public void setParameterCount(int parameterCount) {
            this.parameterCount = parameterCount;
        }

        public List<String> getExceptions() {
            return exceptions;
        }

        public void setExceptions(List<String> exceptions) {
            this.exceptions = exceptions;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public void setStatic(boolean aStatic) {
            isStatic = aStatic;
        }
    }

    /**
     * Mock requirement metadata.
     */
    @Schema(description = "Mock requirement metadata")
    public static class MockRequirement {

        @Schema(description = "Class name to mock")
        private String className;

        @Schema(description = "Type of mock (INTERFACE, CLASS, ABSTRACT_CLASS)")
        private String mockType;

        @Schema(description = "Injection type (CONSTRUCTOR, FIELD, SETTER)")
        private String injectionType;

        @Schema(description = "Whether it's a Spring component")
        private boolean isSpringComponent;

        public MockRequirement() {
        }

        // Getters and Setters

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMockType() {
            return mockType;
        }

        public void setMockType(String mockType) {
            this.mockType = mockType;
        }

        public String getInjectionType() {
            return injectionType;
        }

        public void setInjectionType(String injectionType) {
            this.injectionType = injectionType;
        }

        public boolean isSpringComponent() {
            return isSpringComponent;
        }

        public void setSpringComponent(boolean springComponent) {
            isSpringComponent = springComponent;
        }
    }
}