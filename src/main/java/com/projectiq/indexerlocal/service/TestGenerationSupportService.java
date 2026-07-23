package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.model.testsupport.*;
import com.projectiq.indexerlocal.repository.DependencyRepository;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.repository.RepositoryRepository;
import com.projectiq.indexerlocal.repository.TestGenerationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for providing unit test generation support metadata.
 * Retrieves structured information from persisted index for future AI-powered test generation.
 */
@Service
public class TestGenerationSupportService {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationSupportService.class);

    private final IndexRepository indexRepository;
    private final RepositoryRepository repositoryRepository;
    private final DependencyRepository dependencyRepository;
    private final TestGenerationRepository testGenerationRepository;

    public TestGenerationSupportService(IndexRepository indexRepository,
                                         RepositoryRepository repositoryRepository,
                                         DependencyRepository dependencyRepository,
                                         TestGenerationRepository testGenerationRepository) {
        this.indexRepository = indexRepository;
        this.repositoryRepository = repositoryRepository;
        this.dependencyRepository = dependencyRepository;
        this.testGenerationRepository = testGenerationRepository;
    }

    /**
     * Get classes available for test generation in a repository.
     */
    public List<TestSupportClassContext> getClassesForTestGeneration(String repositoryId) {
        long startTime = System.currentTimeMillis();
        logger.info("Retrieving classes for test generation for repository: {}", repositoryId);

        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        // Get all classes from the index
        List<ClassInfo> allClasses = indexRepository.findAllClasses();

        // Filter classes that belong to this repository (by file path)
        List<TestSupportClassContext> classContexts = allClasses.stream()
                .filter(cls -> isClassInRepository(cls, repositoryId))
                .map(this::mapToClassContext)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} classes for test generation for repository: {}. Duration: {}ms",
                classContexts.size(), repositoryId, duration);

        // Record history
        recordHistory(repositoryId, "CLASSES", null, classContexts.size(), 0, duration, "SUCCESS", null);

        return classContexts;
    }

    /**
     * Get complete metadata for a specific class.
     */
    public TestSupportClassDetail getClassDetail(String repositoryId, Long classId) {
        long startTime = System.currentTimeMillis();
        logger.info("Retrieving class detail for classId: {} in repository: {}", classId, repositoryId);

        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        ClassInfo classInfo = indexRepository.findClassDetailById(classId);
        if (classInfo == null) {
            throw new IllegalArgumentException("Class not found with ID: " + classId);
        }

        TestSupportClassDetail detail = new TestSupportClassDetail();

        // Map class context
        TestSupportClassContext classContext = mapToClassContext(classInfo);
        detail.setClassContext(classContext);

        // Map constructors
        List<TestSupportMethodContext> constructors = new ArrayList<>();
        if (classInfo.getConstructors() != null) {
            constructors = classInfo.getConstructors().stream()
                    .map(this::mapToMethodContext)
                    .collect(Collectors.toList());
        }
        detail.setConstructors(constructors);

        // Map methods
        List<TestSupportMethodContext> methods = new ArrayList<>();
        if (classInfo.getMethods() != null) {
            methods = classInfo.getMethods().stream()
                    .map(this::mapToMethodContext)
                    .collect(Collectors.toList());
        }
        detail.setMethods(methods);

        // Map fields
        List<TestSupportFieldContext> fields = new ArrayList<>();
        if (classInfo.getFields() != null) {
            fields = classInfo.getFields().stream()
                    .map(this::mapToFieldContext)
                    .collect(Collectors.toList());
        }
        detail.setFields(fields);

        // Get Spring context if available
        TestSupportSpringContext springContext = getSpringContextForClass(repositoryId, classId);
        detail.setSpringContext(springContext);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved class detail for classId: {}. Methods: {}, Fields: {}, Constructors: {}. Duration: {}ms",
                classId, methods.size(), fields.size(), constructors.size(), duration);

        // Record history
        recordHistory(repositoryId, "CLASS_DETAIL", "classId=" + classId, 1, methods.size(), duration, "SUCCESS", null);

        return detail;
    }

    /**
     * Get complete metadata for a specific method.
     */
    public TestSupportMethodContext getMethodDetail(String repositoryId, Long methodId) {
        long startTime = System.currentTimeMillis();
        logger.info("Retrieving method detail for methodId: {} in repository: {}", methodId, repositoryId);

        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        MethodInfo methodInfo = indexRepository.findMethodById(methodId);
        if (methodInfo == null) {
            throw new IllegalArgumentException("Method not found with ID: " + methodId);
        }

        TestSupportMethodContext methodContext = mapToMethodContext(methodInfo);

        // Get annotations for the method
        List<AnnotationInfo> annotations = indexRepository.findAnnotationsByTarget("METHOD", methodId);
        methodContext.setAnnotations(annotations);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved method detail for methodId: {}. Duration: {}ms", methodId, duration);

        // Record history
        recordHistory(repositoryId, "METHOD_DETAIL", "methodId=" + methodId, 0, 1, duration, "SUCCESS", null);

        return methodContext;
    }

    /**
     * Generate structured unit test context from persisted metadata.
     */
    public TestSupportContextResponse generateTestContext(String repositoryId, TestSupportContextRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Generating test context for repository: {}", repositoryId);

        Repository repository = validateRepositoryExistsAndIndexed(repositoryId);

        TestSupportContextResponse response = new TestSupportContextResponse();
        response.setRepositoryId(repositoryId);
        response.setRepositoryName(repository.getRepositoryName());

        List<TestSupportClassDetail> classDetails = new ArrayList<>();
        List<TestSupportMethodContext> methodDetails = new ArrayList<>();
        int totalMethods = 0;

        // Process class IDs
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            for (Long classId : request.getClassIds()) {
                try {
                    TestSupportClassDetail classDetail = getClassDetailInternal(repositoryId, classId, request.isPublicMethodsOnly());
                    classDetails.add(classDetail);
                    totalMethods += classDetail.getMethods() != null ? classDetail.getMethods().size() : 0;
                } catch (Exception e) {
                    logger.warn("Failed to retrieve class detail for classId: {}", classId, e);
                }
            }
        }

        // Process method IDs
        if (request.getMethodIds() != null && !request.getMethodIds().isEmpty()) {
            for (Long methodId : request.getMethodIds()) {
                try {
                    TestSupportMethodContext methodContext = getMethodDetail(repositoryId, methodId);
                    methodDetails.add(methodContext);
                } catch (Exception e) {
                    logger.warn("Failed to retrieve method detail for methodId: {}", methodId, e);
                }
            }
        }

        response.setClassDetails(classDetails);
        response.setMethodDetails(methodDetails);

        // Include Spring context if requested
        if (request.isIncludeSpringContext()) {
            List<TestSupportSpringContext> springContexts = getSpringContextsForRepository(repositoryId);
            response.setSpringContexts(springContexts);
        }

        // Include dependency context if requested
        if (request.isIncludeDependencyContext()) {
            TestSupportContextResponse.TestSupportDependencyContext depContext = buildDependencyContext(repositoryId);
            response.setDependencyContext(depContext);
        }

        // Build test generation metadata
        TestSupportContextResponse.TestGenerationMetadata testMetadata = buildTestGenerationMetadata(
                classDetails, methodDetails, repositoryId);
        response.setTestGenerationMetadata(testMetadata);

        long duration = System.currentTimeMillis() - startTime;
        response.setGenerationDurationMs(duration);
        response.setGeneratedAt(LocalDateTime.now());

        logger.info("Generated test context for repository: {}. Classes: {}, Methods: {}. Duration: {}ms",
                repositoryId, classDetails.size(), totalMethods, duration);

        // Record history
        String params = "classIds=" + request.getClassIds() + ",methodIds=" + request.getMethodIds();
        recordHistory(repositoryId, "CONTEXT", params, classDetails.size(), totalMethods, duration, "SUCCESS", null);

        return response;
    }

    /**
     * Get test generation statistics for a repository.
     */
    public Map<String, Object> getStatistics(String repositoryId) {
        validateRepositoryExistsAndIndexed(repositoryId);
        return testGenerationRepository.getStatistics(repositoryId);
    }

    /**
     * Get test generation history for a repository.
     */
    public List<TestGenerationHistory> getHistory(String repositoryId, int page, int size) {
        validateRepositoryExistsAndIndexed(repositoryId);
        return testGenerationRepository.findByRepositoryIdWithPagination(repositoryId, page, size);
    }

    // ==================== Private Helper Methods ====================

    private Repository validateRepositoryExistsAndIndexed(String repositoryId) {
        Repository repository = repositoryRepository.findByRepositoryId(repositoryId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repositoryId);
        }
        if (repository.getStatus() != RepositoryStatus.INDEXED &&
            repository.getStatus() != RepositoryStatus.READY) {
            throw new IllegalStateException("Repository is not indexed: " + repositoryId);
        }
        return repository;
    }

    private boolean isClassInRepository(ClassInfo classInfo, String repositoryId) {
        // Check if the class belongs to this repository by examining file path
        if (classInfo.getFileIndexId() != null) {
            FileIndex fileIndex = indexRepository.findFileById(classInfo.getFileIndexId());
            if (fileIndex != null && fileIndex.getFilePath() != null) {
                return fileIndex.getFilePath().contains(repositoryId);
            }
        }
        return false;
    }

    private TestSupportClassContext mapToClassContext(ClassInfo classInfo) {
        TestSupportClassContext context = new TestSupportClassContext();
        context.setClassId(classInfo.getId());
        context.setClassName(classInfo.getClassName());
        context.setClassType(classInfo.getClassType());
        context.setVisibility(classInfo.getVisibility());
        context.setSuperClass(classInfo.getSuperClass());
        context.setInterfaces(classInfo.getInterfaces());
        context.setAnnotations(classInfo.getAnnotations());
        context.setAbstract(classInfo.isAbstract());
        context.setFinal(classInfo.isFinal());
        context.setGenericTypeParameters(classInfo.getGenericTypeParameters());

        // Build fully qualified name
        String packageName = classInfo.getPackage_();
        if (packageName != null && !packageName.isEmpty()) {
            context.setPackageName(packageName);
            context.setFullyQualifiedName(packageName + "." + classInfo.getClassName());
        } else {
            context.setFullyQualifiedName(classInfo.getClassName());
        }

        // Get source file path
        if (classInfo.getFileIndexId() != null) {
            FileIndex fileIndex = indexRepository.findFileById(classInfo.getFileIndexId());
            if (fileIndex != null) {
                context.setSourceFilePath(fileIndex.getFilePath());
            }
        }

        return context;
    }

    private TestSupportMethodContext mapToMethodContext(MethodInfo methodInfo) {
        TestSupportMethodContext context = new TestSupportMethodContext();
        context.setMethodId(methodInfo.getId());
        context.setClassId(methodInfo.getClassId());
        context.setMethodName(methodInfo.getMethodName());
        context.setMethodSignature(methodInfo.getMethodSignature());
        context.setReturnType(methodInfo.getReturnType());
        context.setVisibility(methodInfo.getVisibility());
        context.setStatic(methodInfo.isStatic());
        context.setAbstract(methodInfo.isAbstract());
        context.setFinal(methodInfo.isFinal());
        context.setMethodType(methodInfo.getMethodType());
        context.setParameterTypes(methodInfo.getParameterTypes());
        context.setParameterNames(methodInfo.getParameterNames());
        context.setParameters(methodInfo.getParameters());
        context.setExceptions(methodInfo.getExceptions());
        context.setAnnotations(methodInfo.getAnnotations());
        return context;
    }

    private TestSupportFieldContext mapToFieldContext(FieldInfo fieldInfo) {
        TestSupportFieldContext context = new TestSupportFieldContext();
        context.setFieldId(fieldInfo.getId());
        context.setClassId(fieldInfo.getClassId());
        context.setFieldName(fieldInfo.getFieldName());
        context.setFieldType(fieldInfo.getFieldType());
        context.setVisibility(fieldInfo.getVisibility());
        context.setStatic(fieldInfo.isStatic());
        context.setFinal(fieldInfo.isFinal());
        context.setDefaultValue(fieldInfo.getDefaultValue());
        context.setAnnotations(fieldInfo.getAnnotations());
        return context;
    }

    private TestSupportSpringContext getSpringContextForClass(String repositoryId, Long classId) {
        List<SpringComponent> components = indexRepository.findSpringComponentsByRepository(repositoryId);
        for (SpringComponent component : components) {
            if (component.getClassId() != null && component.getClassId().equals(classId)) {
                return mapToSpringContext(component);
            }
        }
        return null;
    }

    private List<TestSupportSpringContext> getSpringContextsForRepository(String repositoryId) {
        List<SpringComponent> components = indexRepository.findSpringComponentsByRepository(repositoryId);
        return components.stream()
                .map(this::mapToSpringContext)
                .collect(Collectors.toList());
    }

    private TestSupportSpringContext mapToSpringContext(SpringComponent component) {
        TestSupportSpringContext context = new TestSupportSpringContext();
        context.setComponentId(component.getId());
        context.setComponentType(component.getComponentType());
        context.setComponentName(component.getComponentName());
        context.setClassName(component.getClassName());
        context.setPackageName(component.getPackageName());
        context.setBeanName(component.getBeanName());
        context.setHasAutowired(component.hasAutowired());
        context.setHasInject(component.hasInject());
        context.setHasResource(component.hasResource());
        context.setHasConstructorInjection(component.isHasConstructorInjection());
        context.setHasSetterInjection(component.isHasSetterInjection());
        context.setHasTransactional(component.hasTransactional());
        context.setConfiguration(component.isConfiguration());
        context.setAnnotations(component.getAllAnnotations());
        return context;
    }

    private TestSupportContextResponse.TestSupportDependencyContext buildDependencyContext(String repositoryId) {
        TestSupportContextResponse.TestSupportDependencyContext context =
                new TestSupportContextResponse.TestSupportDependencyContext();

        List<Dependency> dependencies = dependencyRepository.findByRepositoryId(repositoryId);

        List<Dependency> externalDeps = dependencies.stream()
                .filter(d -> !d.isInternal())
                .collect(Collectors.toList());

        List<Dependency> internalDeps = dependencies.stream()
                .filter(Dependency::isInternal)
                .collect(Collectors.toList());

        context.setExternalDependencies(externalDeps);
        context.setInternalDependencies(internalDeps);

        // Detect test frameworks
        List<String> testFrameworks = new ArrayList<>();
        List<String> mockingFrameworks = new ArrayList<>();

        for (Dependency dep : dependencies) {
            String artifactId = dep.getArtifactId();
            if (artifactId != null) {
                if (artifactId.contains("junit") || artifactId.contains("testng")) {
                    testFrameworks.add(artifactId);
                }
                if (artifactId.contains("mockito") || artifactId.contains("mockk") ||
                    artifactId.contains("easymock") || artifactId.contains("powermock")) {
                    mockingFrameworks.add(artifactId);
                }
            }
        }

        context.setTestFrameworks(testFrameworks);
        context.setMockingFrameworks(mockingFrameworks);

        // Get build system type from dependencies
        if (!dependencies.isEmpty()) {
            Dependency firstDep = dependencies.get(0);
            if (firstDep.getConfiguration() != null) {
                context.setBuildSystemType("GRADLE");
            } else {
                context.setBuildSystemType("MAVEN");
            }
        }

        return context;
    }

    private TestSupportContextResponse.TestGenerationMetadata buildTestGenerationMetadata(
            List<TestSupportClassDetail> classDetails,
            List<TestSupportMethodContext> methodDetails,
            String repositoryId) {

        TestSupportContextResponse.TestGenerationMetadata metadata =
                new TestSupportContextResponse.TestGenerationMetadata();

        // Find candidate public methods
        List<TestSupportContextResponse.CandidateMethod> candidates = new ArrayList<>();

        for (TestSupportClassDetail classDetail : classDetails) {
            if (classDetail.getMethods() != null) {
                for (TestSupportMethodContext method : classDetail.getMethods()) {
                    if ("PUBLIC".equalsIgnoreCase(method.getVisibility())) {
                        TestSupportContextResponse.CandidateMethod candidate =
                                new TestSupportContextResponse.CandidateMethod();
                        candidate.setMethodId(method.getMethodId());
                        candidate.setClassId(method.getClassId());
                        candidate.setClassName(classDetail.getClassContext().getClassName());
                        candidate.setMethodName(method.getMethodName());
                        candidate.setMethodSignature(method.getMethodSignature());
                        candidate.setReturnType(method.getReturnType());
                        candidate.setParameterCount(method.getParameters() != null ? method.getParameters().size() : 0);
                        candidate.setExceptions(method.getExceptions());
                        candidate.setStatic(method.isStatic());
                        candidates.add(candidate);
                    }
                }
            }
        }

        // Add standalone method details
        for (TestSupportMethodContext method : methodDetails) {
            if ("PUBLIC".equalsIgnoreCase(method.getVisibility())) {
                TestSupportContextResponse.CandidateMethod candidate =
                        new TestSupportContextResponse.CandidateMethod();
                candidate.setMethodId(method.getMethodId());
                candidate.setClassId(method.getClassId());
                candidate.setMethodName(method.getMethodName());
                candidate.setMethodSignature(method.getMethodSignature());
                candidate.setReturnType(method.getReturnType());
                candidate.setParameterCount(method.getParameters() != null ? method.getParameters().size() : 0);
                candidate.setExceptions(method.getExceptions());
                candidate.setStatic(method.isStatic());
                candidates.add(candidate);
            }
        }

        metadata.setCandidateMethods(candidates);
        metadata.setTotalClassesAnalyzed(classDetails.size());
        metadata.setTotalMethodsAnalyzed(candidates.size());

        // Build dependency summary
        Map<String, Integer> depSummary = new HashMap<>();
        List<SpringComponent> springComponents = indexRepository.findSpringComponentsByRepository(repositoryId);
        depSummary.put("totalSpringComponents", springComponents.size());
        depSummary.put("services", (int) springComponents.stream().filter(SpringComponent::isService).count());
        depSummary.put("repositories", (int) springComponents.stream().filter(SpringComponent::isRepository).count());
        depSummary.put("controllers", (int) springComponents.stream()
                .filter(c -> c.isController() || c.isRestController()).count());
        metadata.setDependencySummary(depSummary);

        // Build mock requirements
        List<TestSupportContextResponse.MockRequirement> mockRequirements = new ArrayList<>();
        for (SpringComponent component : springComponents) {
            if (component.hasAutowired() || component.hasInject() || component.hasResource()) {
                TestSupportContextResponse.MockRequirement mock = new TestSupportContextResponse.MockRequirement();
                mock.setClassName(component.getClassName());
                mock.setMockType(component.getComponentType());
                mock.setSpringComponent(true);
                if (component.isHasConstructorInjection()) {
                    mock.setInjectionType("CONSTRUCTOR");
                } else if (component.isHasSetterInjection()) {
                    mock.setInjectionType("SETTER");
                } else {
                    mock.setInjectionType("FIELD");
                }
                mockRequirements.add(mock);
            }
        }
        metadata.setRequiredMocks(mockRequirements);

        // Configuration requirements
        List<String> configRequirements = new ArrayList<>();
        for (SpringComponent component : springComponents) {
            if (component.isConfiguration()) {
                configRequirements.add(component.getClassName());
            }
        }
        metadata.setConfigurationRequirements(configRequirements);

        return metadata;
    }

    private TestSupportClassDetail getClassDetailInternal(String repositoryId, Long classId, boolean publicMethodsOnly) {
        ClassInfo classInfo = indexRepository.findClassDetailById(classId);
        if (classInfo == null) {
            throw new IllegalArgumentException("Class not found with ID: " + classId);
        }

        TestSupportClassDetail detail = new TestSupportClassDetail();

        // Map class context
        TestSupportClassContext classContext = mapToClassContext(classInfo);
        detail.setClassContext(classContext);

        // Map constructors
        List<TestSupportMethodContext> constructors = new ArrayList<>();
        if (classInfo.getConstructors() != null) {
            constructors = classInfo.getConstructors().stream()
                    .map(this::mapToMethodContext)
                    .collect(Collectors.toList());
        }
        detail.setConstructors(constructors);

        // Map methods (filter by visibility if requested)
        List<TestSupportMethodContext> methods = new ArrayList<>();
        if (classInfo.getMethods() != null) {
            methods = classInfo.getMethods().stream()
                    .filter(m -> !publicMethodsOnly || "PUBLIC".equalsIgnoreCase(m.getVisibility()))
                    .map(this::mapToMethodContext)
                    .collect(Collectors.toList());
        }
        detail.setMethods(methods);

        // Map fields
        List<TestSupportFieldContext> fields = new ArrayList<>();
        if (classInfo.getFields() != null) {
            fields = classInfo.getFields().stream()
                    .map(this::mapToFieldContext)
                    .collect(Collectors.toList());
        }
        detail.setFields(fields);

        // Get Spring context if available
        TestSupportSpringContext springContext = getSpringContextForClass(repositoryId, classId);
        detail.setSpringContext(springContext);

        return detail;
    }

    private void recordHistory(String repositoryId, String requestType, String params,
                               int classesRetrieved, int methodsRetrieved,
                               long durationMs, String status, String errorMessage) {
        try {
            TestGenerationHistory history = new TestGenerationHistory();
            history.setRepositoryId(repositoryId);
            history.setRequestType(requestType);
            history.setRequestParameters(params);
            history.setClassesRetrieved(classesRetrieved);
            history.setMethodsRetrieved(methodsRetrieved);
            history.setExecutionDurationMs(durationMs);
            history.setStatus(status);
            history.setErrorMessage(errorMessage);
            testGenerationRepository.save(history);
        } catch (Exception e) {
            logger.warn("Failed to record test generation history: {}", e.getMessage());
        }
    }
}