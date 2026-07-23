package com.projectiq.indexerlocal.controller;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.service.IndexerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST controller for indexing operations.
 * Provides endpoints for file indexing, querying, and lookup operations.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Index Controller", description = "APIs for code indexing and metadata retrieval")
public class IndexController {

    private final IndexerService indexerService;

    public IndexController(IndexerService indexerService) {
        this.indexerService = indexerService;
    }

    // ==================== File Indexing Operations ====================

    /**
     * Index a single file.
     */
    @PostMapping("/index/file")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Index a single file", description = "Indexes the content of a single Java file for metadata retrieval")
    public String indexFile(
            @Parameter(description = "Java file to index") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional file path") @RequestParam(value = "filePath", required = false) String filePath) throws IOException {
        indexerService.indexFile(file, filePath);
        return "File indexed successfully";
    }

    /**
     * Index multiple files.
     */
    @PostMapping("/index/files")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Index multiple files", description = "Indexes the content of multiple Java files for metadata retrieval")
    public String indexFiles(
            @Parameter(description = "Java files to index") @RequestParam("files") MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            indexerService.indexFile(file, null);
        }
        return files.length + " files indexed successfully";
    }

    /**
     * Index a directory.
     */
    @PostMapping("/index/directory")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Index a directory", description = "Indexes all Java files in the specified directory")
    public String indexDirectory(
            @Parameter(description = "Directory path to index") @RequestParam("directory") String directory) {
        indexerService.indexDirectory(directory);
        return "Directory indexed successfully: " + directory;
    }

    // ==================== File Query Operations ====================

    /**
     * List all indexed source files.
     */
    @GetMapping("/files")
    @Operation(summary = "List indexed files", description = "Returns all indexed source files in the database")
    public List<FileIndex> listFiles() {
        return indexerService.listFiles();
    }

    /**
     * Get a source file by ID.
     */
    @GetMapping("/files/{id}")
    @Operation(summary = "Get file by ID", description = "Retrieves a specific indexed file by its database ID")
    public FileIndex getFileById(@Parameter(description = "File database ID") @PathVariable Long id) {
        FileIndex file = indexerService.getFileById(id);
        if (file == null) {
            throw new RuntimeException("File not found with id: " + id);
        }
        return file;
    }

    /**
     * Get a source file by path.
     */
    @GetMapping("/files/path")
    @Operation(summary = "Get file by path", description = "Retrieves a specific indexed file by its file path")
    public FileIndex getFileByPath(@Parameter(description = "File path") @RequestParam String path) {
        FileIndex file = indexerService.getFileByPath(path);
        if (file == null) {
            throw new RuntimeException("File not found with path: " + path);
        }
        return file;
    }

    // ==================== Class Query Operations ====================

    /**
     * List all Java classes.
     */
    @GetMapping("/classes")
    @Operation(summary = "List indexed classes", description = "Returns all indexed Java classes")
    public List<ClassInfo> listClasses() {
        return indexerService.listClasses();
    }

    /**
     * Get a class by ID.
     */
    @GetMapping("/classes/{id}")
    @Operation(summary = "Get class by ID", description = "Retrieves a specific indexed class by its database ID")
    public ClassInfo getClassById(@Parameter(description = "Class database ID") @PathVariable Long id) {
        ClassInfo cls = indexerService.getClassById(id);
        if (cls == null) {
            throw new RuntimeException("Class not found with id: " + id);
        }
        return cls;
    }

    /**
     * Get a class by name.
     */
    @GetMapping("/classes/name")
    @Operation(summary = "Get class by name", description = "Retrieves a specific indexed class by its fully qualified name")
    public ClassInfo getClassByName(@Parameter(description = "Fully qualified class name") @RequestParam String name) {
        ClassInfo cls = indexerService.getClassByName(name);
        if (cls == null) {
            throw new RuntimeException("Class not found with name: " + name);
        }
        return cls;
    }

    // ==================== Method Query Operations ====================

    /**
     * List all methods.
     */
    @GetMapping("/methods")
    @Operation(summary = "List indexed methods", description = "Returns all indexed Java methods")
    public List<MethodInfo> listMethods() {
        return indexerService.listMethods();
    }

    /**
     * Get a method by ID.
     */
    @GetMapping("/methods/{id}")
    @Operation(summary = "Get method by ID", description = "Retrieves a specific indexed method by its database ID")
    public MethodInfo getMethodById(@Parameter(description = "Method database ID") @PathVariable Long id) {
        MethodInfo method = indexerService.getMethodById(id);
        if (method == null) {
            throw new RuntimeException("Method not found with id: " + id);
        }
        return method;
    }

    /**
     * Get a method by name.
     */
    @GetMapping("/methods/name")
    @Operation(summary = "Get method by name", description = "Retrieves a specific indexed method by its name")
    public MethodInfo getMethodByName(@Parameter(description = "Method name") @RequestParam String name) {
        MethodInfo method = indexerService.getMethodByName(name);
        if (method == null) {
            throw new RuntimeException("Method not found with name: " + name);
        }
        return method;
    }

    // ==================== Field Query Operations ====================

    /**
     * List all fields.
     */
    @GetMapping("/fields")
    @Operation(summary = "List indexed fields", description = "Returns all indexed Java fields")
    public List<FieldInfo> listFields() {
        return indexerService.listFields();
    }

    /**
     * Get a field by ID.
     */
    @GetMapping("/fields/{id}")
    @Operation(summary = "Get field by ID", description = "Retrieves a specific indexed field by its database ID")
    public FieldInfo getFieldById(@Parameter(description = "Field database ID") @PathVariable Long id) {
        FieldInfo field = indexerService.getFieldById(id);
        if (field == null) {
            throw new RuntimeException("Field not found with id: " + id);
        }
        return field;
    }

    /**
     * Get a field by name.
     */
    @GetMapping("/fields/name")
    @Operation(summary = "Get field by name", description = "Retrieves a specific indexed field by its name")
    public FieldInfo getFieldByName(@Parameter(description = "Field name") @RequestParam String name) {
        FieldInfo field = indexerService.getFieldByName(name);
        if (field == null) {
            throw new RuntimeException("Field not found with name: " + name);
        }
        return field;
    }

    // ==================== Spring Component Query Operations ====================

    /**
     * List all Spring components.
     */
    @GetMapping("/spring-components")
    @Operation(summary = "List Spring components", description = "Returns all indexed Spring components (controllers, services, repositories, etc.)")
    public List<SpringComponent> listSpringComponents() {
        return indexerService.listSpringComponents();
    }

    // ==================== Lookup Operations ====================

    /**
     * Search classes by name pattern (partial match).
     */
    @GetMapping("/lookup/classes")
    @Operation(summary = "Search classes by name", description = "Searches indexed classes by name pattern using partial matching")
    public List<ClassInfo> lookupClasses(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchClassesByName(name);
    }

    /**
     * Search methods by name pattern (partial match).
     */
    @GetMapping("/lookup/methods")
    @Operation(summary = "Search methods by name", description = "Searches indexed methods by name pattern using partial matching")
    public List<MethodInfo> lookupMethods(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchMethodsByName(name);
    }

    /**
     * Search fields by name pattern (partial match).
     */
    @GetMapping("/lookup/fields")
    @Operation(summary = "Search fields by name", description = "Searches indexed fields by name pattern using partial matching")
    public List<FieldInfo> lookupFields(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchFieldsByName(name);
    }

    /**
     * Search classes by package/path pattern.
     */
    @GetMapping("/lookup/by-package")
    @Operation(summary = "Search classes by package", description = "Searches indexed classes by their package or path pattern")
    public List<ClassInfo> lookupByPackage(
            @Parameter(description = "Package name pattern") @RequestParam(required = true, defaultValue = "") String pkg) {
        return indexerService.searchClassesByPackage(pkg);
    }

    /**
     * Search annotations by name pattern (partial match).
     */
    @GetMapping("/lookup/annotations")
    @Operation(summary = "Search annotations", description = "Searches indexed annotations by name pattern using partial matching")
    public List<AnnotationInfo> lookupAnnotations(
            @Parameter(description = "Annotation name pattern") @RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchAnnotationsByName(name);
    }

    /**
     * Get all annotations for a specific target (class/method/field).
     */
    @GetMapping("/lookup/target-annotations")
    @Operation(summary = "Get target annotations", description = "Retrieves all annotations for a specific indexed target entity")
    public List<AnnotationInfo> lookupTargetAnnotations(
            @Parameter(description = "Target type (CLASS, METHOD, FIELD)") @RequestParam String type,
            @Parameter(description = "Target entity ID") @RequestParam Long id) {
        return indexerService.getAnnotationsByTarget(type, id);
    }

    /**
     * Get full class detail including methods, fields, and annotations by class ID.
     */
    @GetMapping("/lookup/class-detail")
    @Operation(summary = "Get class detail", description = "Retrieves full class details including nested methods, fields, and annotations")
    public ClassInfo lookupClassDetail(
            @Parameter(description = "Class database ID") @RequestParam Long id) {
        ClassInfo detail = indexerService.getClassDetailById(id);
        if (detail == null) {
            throw new RuntimeException("Class not found with id: " + id);
        }
        return detail;
    }

    /**
     * Get methods for a specific class by class ID.
     */
    @GetMapping("/lookup/class-methods")
    @Operation(summary = "Get class methods", description = "Retrieves all methods belonging to a specific indexed class")
    public List<MethodInfo> lookupClassMethods(
            @Parameter(description = "Class database ID") @RequestParam Long classId) {
        return indexerService.getMethodsByClassId(classId);
    }

    /**
     * Get fields for a specific class by class ID.
     */
    @GetMapping("/lookup/class-fields")
    @Operation(summary = "Get class fields", description = "Retrieves all fields belonging to a specific indexed class")
    public List<FieldInfo> lookupClassFields(
            @Parameter(description = "Class database ID") @RequestParam Long classId) {
        return indexerService.getFieldsByClassId(classId);
    }

    /**
     * Search files by path pattern.
     */
    @GetMapping("/lookup/files")
    @Operation(summary = "Search indexed files", description = "Searches indexed files by their file path pattern")
    public List<FileIndex> lookupFiles(
            @Parameter(description = "Path search pattern") @RequestParam(required = true, defaultValue = "") String path) {
        return indexerService.searchFilesByPath(path);
    }

    /**
     * Get all available Spring component types.
     */
    @GetMapping("/lookup/component-types")
    @Operation(summary = "Get component types", description = "Returns all available Spring component types found in the indexed codebase")
    public List<String> lookupComponentTypes() {
        return indexerService.getAvailableComponentTypes();
    }

    /**
     * Search Spring components by type (case-insensitive, supports SERVICE, REPOSITORY, CONTROLLER, etc.).
     */
    @GetMapping("/lookup/spring-components")
    @Operation(summary = "Search Spring components by type", description = "Searches indexed Spring components by their component type")
    public List<SpringComponent> lookupSpringComponents(
            @Parameter(description = "Component type (SERVICE, REPOSITORY, CONTROLLER, etc.)") @RequestParam(required = true, defaultValue = "") String type) {
        return indexerService.searchSpringComponentsByType(type);
    }

    /**
     * Search Spring components by class name pattern.
     */
    @GetMapping("/lookup/spring-components/name")
    @Operation(summary = "Search Spring components by name", description = "Searches indexed Spring components by their class name pattern")
    public List<SpringComponent> lookupSpringComponentsByName(
            @Parameter(description = "Class name pattern") @RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchSpringComponentsByName(name);
    }

    // ==================== Repository Summary Operations ====================

    /**
     * Get repository summary statistics from indexed metadata.
     */
    @GetMapping("/repository-summary")
    @Operation(summary = "Get repository summary", description = "Returns overall statistics and summary of the indexed codebase")
    public RepositorySummary getRepositorySummary() {
        return indexerService.getRepositorySummary();
    }
}