package com.projectiq.indexerlocal.controller.v1;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.model.api.ApiResponse;
import com.projectiq.indexerlocal.model.api.PaginatedResponse;
import com.projectiq.indexerlocal.service.IndexerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * v1 REST controller for code indexing and metadata retrieval operations.
 * Follows REST naming conventions and returns standardized API responses.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Index API (v1)", description = "Versioned API for code indexing and metadata retrieval")
public class IndexControllerV1 {

    private final IndexerService indexerService;

    public IndexControllerV1(IndexerService indexerService) {
        this.indexerService = indexerService;
    }

    // ==================== File Indexing Operations ====================

    /**
     * Index a single Java file.
     * POST /api/v1/index/files
     */
    @PostMapping("/index/files")
    @Operation(summary = "Index a single file", description = "Indexes the content of a single Java file for metadata retrieval")
    public ResponseEntity<ApiResponse<String>> indexFile(
            @Parameter(description = "Java file to index") @RequestPart("file") @Valid MultipartFile file,
            @Parameter(description = "Optional file path") @RequestPart(value = "filePath", required = false) String filePath) throws IOException {
        indexerService.indexFile(file, filePath);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("File indexed successfully"));
    }

    /**
     * Index multiple Java files.
     * POST /api/v1/index/files/batch
     */
    @PostMapping("/index/files/batch")
    @Operation(summary = "Index multiple files", description = "Indexes the content of multiple Java files for metadata retrieval")
    public ResponseEntity<ApiResponse<String>> indexFiles(
            @Parameter(description = "Java files to index") @RequestPart("files") MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            indexerService.indexFile(file, null);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage(files.length + " files indexed successfully"));
    }

    /**
     * Index an entire directory.
     * POST /api/v1/index/directory
     */
    @PostMapping("/index/directory")
    @Operation(summary = "Index a directory", description = "Indexes all Java files in the specified directory")
    public ResponseEntity<ApiResponse<String>> indexDirectory(
            @Parameter(description = "Directory path to index") @RequestParam("directory") @NotBlank String directory) {
        indexerService.indexDirectory(directory);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("Directory indexed successfully: " + directory));
    }

    // ==================== File Operations ====================

    /**
     * List all indexed files with optional pagination.
     * GET /api/v1/files?page=0&size=20
     */
    @GetMapping("/files")
    @Operation(summary = "List indexed files", description = "Returns all indexed source files with optional pagination support")
    public ResponseEntity<ApiResponse<PaginatedResponse<FileIndex>>> listFiles(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        List<FileIndex> allFiles = indexerService.listFiles();
        int totalElements = allFiles.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<FileIndex> pageContent = allFiles.stream()
                .skip(fromIndex)
                .limit(size)
                .toList();

        PaginatedResponse<FileIndex> paginatedResponse = PaginatedResponse.of(
                pageContent, page, size, totalPages, totalElements);

        return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", paginatedResponse));
    }

    /**
     * Get a file by its ID.
     * GET /api/v1/files/{id}
     */
    @GetMapping("/files/{id}")
    @Operation(summary = "Get file by ID", description = "Retrieves a specific indexed file by its database ID")
    public ResponseEntity<Object> getFileById(
            @Parameter(description = "File database ID") @PathVariable Long id) {
        FileIndex file = indexerService.getFileById(id);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FileIndex>notFound("File with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("File retrieved successfully", file));
    }

    /**
     * Get a file by its path.
     * GET /api/v1/files/path/{path}
     */
    @GetMapping("/files/path/{path}")
    @Operation(summary = "Get file by path", description = "Retrieves a specific indexed file by its file path")
    public ResponseEntity<Object> getFileByPath(
            @Parameter(description = "File path") @PathVariable @NotBlank String path) {
        FileIndex file = indexerService.getFileByPath(path);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FileIndex>notFound("File with path: " + path));
        }
        return ResponseEntity.ok(ApiResponse.success("File retrieved successfully", file));
    }

    // ==================== Class Operations ====================

    /**
     * List all indexed classes with optional pagination.
     * GET /api/v1/classes?page=0&size=20
     */
    @GetMapping("/classes")
    @Operation(summary = "List indexed classes", description = "Returns all indexed Java classes with optional pagination support")
    public ResponseEntity<ApiResponse<PaginatedResponse<ClassInfo>>> listClasses(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        List<ClassInfo> allClasses = indexerService.listClasses();
        int totalElements = allClasses.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<ClassInfo> pageContent = allClasses.stream()
                .skip(fromIndex)
                .limit(size)
                .toList();

        PaginatedResponse<ClassInfo> paginatedResponse = PaginatedResponse.of(
                pageContent, page, size, totalPages, totalElements);

        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", paginatedResponse));
    }

    /**
     * Get a class by its ID.
     * GET /api/v1/classes/{id}
     */
    @GetMapping("/classes/{id}")
    @Operation(summary = "Get class by ID", description = "Retrieves a specific indexed class by its database ID")
    public ResponseEntity<Object> getClassById(
            @Parameter(description = "Class database ID") @PathVariable Long id) {
        ClassInfo cls = indexerService.getClassById(id);
        if (cls == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ClassInfo>notFound("Class with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Class retrieved successfully", cls));
    }

    /**
     * Get a class by its fully qualified name.
     * GET /api/v1/classes/name/{name}
     */
    @GetMapping("/classes/name/{name}")
    @Operation(summary = "Get class by name", description = "Retrieves a specific indexed class by its fully qualified name")
    public ResponseEntity<Object> getClassByName(
            @Parameter(description = "Fully qualified class name") @PathVariable @NotBlank String name) {
        ClassInfo cls = indexerService.getClassByName(name);
        if (cls == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ClassInfo>notFound("Class with name: " + name));
        }
        return ResponseEntity.ok(ApiResponse.success("Class retrieved successfully", cls));
    }

    // ==================== Method Operations ====================

    /**
     * List all indexed methods with optional pagination.
     * GET /api/v1/methods?page=0&size=20
     */
    @GetMapping("/methods")
    @Operation(summary = "List indexed methods", description = "Returns all indexed Java methods with optional pagination support")
    public ResponseEntity<ApiResponse<PaginatedResponse<MethodInfo>>> listMethods(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        List<MethodInfo> allMethods = indexerService.listMethods();
        int totalElements = allMethods.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<MethodInfo> pageContent = allMethods.stream()
                .skip(fromIndex)
                .limit(size)
                .toList();

        PaginatedResponse<MethodInfo> paginatedResponse = PaginatedResponse.of(
                pageContent, page, size, totalPages, totalElements);

        return ResponseEntity.ok(ApiResponse.success("Methods retrieved successfully", paginatedResponse));
    }

    /**
     * Get a method by its ID.
     * GET /api/v1/methods/{id}
     */
    @GetMapping("/methods/{id}")
    @Operation(summary = "Get method by ID", description = "Retrieves a specific indexed method by its database ID")
    public ResponseEntity<Object> getMethodById(
            @Parameter(description = "Method database ID") @PathVariable Long id) {
        MethodInfo method = indexerService.getMethodById(id);
        if (method == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<MethodInfo>notFound("Method with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Method retrieved successfully", method));
    }

    /**
     * Get a method by its name.
     * GET /api/v1/methods/name/{name}
     */
    @GetMapping("/methods/name/{name}")
    @Operation(summary = "Get method by name", description = "Retrieves a specific indexed method by its name")
    public ResponseEntity<Object> getMethodByName(
            @Parameter(description = "Method name") @PathVariable @NotBlank String name) {
        MethodInfo method = indexerService.getMethodByName(name);
        if (method == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<MethodInfo>notFound("Method with name: " + name));
        }
        return ResponseEntity.ok(ApiResponse.success("Method retrieved successfully", method));
    }

    // ==================== Field Operations ====================

    /**
     * List all indexed fields with optional pagination.
     * GET /api/v1/fields?page=0&size=20
     */
    @GetMapping("/fields")
    @Operation(summary = "List indexed fields", description = "Returns all indexed Java fields with optional pagination support")
    public ResponseEntity<ApiResponse<PaginatedResponse<FieldInfo>>> listFields(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        List<FieldInfo> allFields = indexerService.listFields();
        int totalElements = allFields.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<FieldInfo> pageContent = allFields.stream()
                .skip(fromIndex)
                .limit(size)
                .toList();

        PaginatedResponse<FieldInfo> paginatedResponse = PaginatedResponse.of(
                pageContent, page, size, totalPages, totalElements);

        return ResponseEntity.ok(ApiResponse.success("Fields retrieved successfully", paginatedResponse));
    }

    /**
     * Get a field by its ID.
     * GET /api/v1/fields/{id}
     */
    @GetMapping("/fields/{id}")
    @Operation(summary = "Get field by ID", description = "Retrieves a specific indexed field by its database ID")
    public ResponseEntity<Object> getFieldById(
            @Parameter(description = "Field database ID") @PathVariable Long id) {
        FieldInfo field = indexerService.getFieldById(id);
        if (field == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FieldInfo>notFound("Field with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Field retrieved successfully", field));
    }

    /**
     * Get a field by its name.
     * GET /api/v1/fields/name/{name}
     */
    @GetMapping("/fields/name/{name}")
    @Operation(summary = "Get field by name", description = "Retrieves a specific indexed field by its name")
    public ResponseEntity<Object> getFieldByName(
            @Parameter(description = "Field name") @PathVariable @NotBlank String name) {
        FieldInfo field = indexerService.getFieldByName(name);
        if (field == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<FieldInfo>notFound("Field with name: " + name));
        }
        return ResponseEntity.ok(ApiResponse.success("Field retrieved successfully", field));
    }

    // ==================== Spring Component Operations ====================

    /**
     * List all Spring components with optional pagination.
     * GET /api/v1/spring-components?page=0&size=20
     */
    @GetMapping("/spring-components")
    @Operation(summary = "List Spring components", description = "Returns all indexed Spring components with optional pagination support")
    public ResponseEntity<ApiResponse<PaginatedResponse<SpringComponent>>> listSpringComponents(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Size of each page") @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        List<SpringComponent> allComponents = indexerService.listSpringComponents();
        int totalElements = allComponents.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<SpringComponent> pageContent = allComponents.stream()
                .skip(fromIndex)
                .limit(size)
                .toList();

        PaginatedResponse<SpringComponent> paginatedResponse = PaginatedResponse.of(
                pageContent, page, size, totalPages, totalElements);

        return ResponseEntity.ok(ApiResponse.success("Spring components retrieved successfully", paginatedResponse));
    }

    /**
     * Search Spring components by type.
     * GET /api/v1/spring-components/search?type=SERVICE
     */
    @GetMapping("/spring-components/search")
    @Operation(summary = "Search Spring components by type", description = "Searches indexed Spring components by their component type")
    public ResponseEntity<ApiResponse<List<SpringComponent>>> lookupSpringComponents(
            @Parameter(description = "Component type (SERVICE, REPOSITORY, CONTROLLER, etc.)") @RequestParam(required = true, defaultValue = "") String type) {
        List<SpringComponent> components = indexerService.searchSpringComponentsByType(type);
        return ResponseEntity.ok(ApiResponse.success("Spring components retrieved successfully", components));
    }

    /**
     * Search Spring components by class name.
     * GET /api/v1/spring-components/search/name?name=MyClass
     */
    @GetMapping("/spring-components/search/name")
    @Operation(summary = "Search Spring components by name", description = "Searches indexed Spring components by their class name pattern")
    public ResponseEntity<ApiResponse<List<SpringComponent>>> lookupSpringComponentsByName(
            @Parameter(description = "Class name pattern") @RequestParam(required = true, defaultValue = "") String name) {
        List<SpringComponent> components = indexerService.searchSpringComponentsByName(name);
        return ResponseEntity.ok(ApiResponse.success("Spring components retrieved successfully", components));
    }

    // ==================== Lookup Operations ====================

    /**
     * Search classes by name pattern (partial match).
     * GET /api/v1/search/classes?name=pattern
     */
    @GetMapping("/search/classes")
    @Operation(summary = "Search classes by name", description = "Searches indexed classes by name pattern using partial matching")
    public ResponseEntity<ApiResponse<List<ClassInfo>>> lookupClasses(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        List<ClassInfo> classes = indexerService.searchClassesByName(name);
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", classes));
    }

    /**
     * Search methods by name pattern (partial match).
     * GET /api/v1/search/methods?name=pattern
     */
    @GetMapping("/search/methods")
    @Operation(summary = "Search methods by name", description = "Searches indexed methods by name pattern using partial matching")
    public ResponseEntity<ApiResponse<List<MethodInfo>>> lookupMethods(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        List<MethodInfo> methods = indexerService.searchMethodsByName(name);
        return ResponseEntity.ok(ApiResponse.success("Methods retrieved successfully", methods));
    }

    /**
     * Search fields by name pattern (partial match).
     * GET /api/v1/search/fields?name=pattern
     */
    @GetMapping("/search/fields")
    @Operation(summary = "Search fields by name", description = "Searches indexed fields by name pattern using partial matching")
    public ResponseEntity<ApiResponse<List<FieldInfo>>> lookupFields(
            @Parameter(description = "Name search pattern") @RequestParam(required = true, defaultValue = "") String name) {
        List<FieldInfo> fields = indexerService.searchFieldsByName(name);
        return ResponseEntity.ok(ApiResponse.success("Fields retrieved successfully", fields));
    }

    /**
     * Search classes by package/path pattern.
     * GET /api/v1/search/classes/by-package?package=com.example
     */
    @GetMapping("/search/classes/by-package")
    @Operation(summary = "Search classes by package", description = "Searches indexed classes by their package or path pattern")
    public ResponseEntity<ApiResponse<List<ClassInfo>>> lookupByPackage(
            @Parameter(description = "Package name pattern") @RequestParam(required = true, defaultValue = "") String pkg) {
        List<ClassInfo> classes = indexerService.searchClassesByPackage(pkg);
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved successfully", classes));
    }

    /**
     * Search annotations by name pattern (partial match).
     * GET /api/v1/search/annotations?name=pattern
     */
    @GetMapping("/search/annotations")
    @Operation(summary = "Search annotations", description = "Searches indexed annotations by name pattern using partial matching")
    public ResponseEntity<ApiResponse<List<AnnotationInfo>>> lookupAnnotations(
            @Parameter(description = "Annotation name pattern") @RequestParam(required = true, defaultValue = "") String name) {
        List<AnnotationInfo> annotations = indexerService.searchAnnotationsByName(name);
        return ResponseEntity.ok(ApiResponse.success("Annotations retrieved successfully", annotations));
    }

    /**
     * Get all annotations for a specific target (class/method/field).
     * GET /api/v1/search/target-annotations?type=CLASS&id=123
     */
    @GetMapping("/search/target-annotations")
    @Operation(summary = "Get target annotations", description = "Retrieves all annotations for a specific indexed target entity")
    public ResponseEntity<ApiResponse<List<AnnotationInfo>>> lookupTargetAnnotations(
            @Parameter(description = "Target type (CLASS, METHOD, FIELD)") @RequestParam String type,
            @Parameter(description = "Target entity ID") @RequestParam Long id) {
        List<AnnotationInfo> annotations = indexerService.getAnnotationsByTarget(type, id);
        return ResponseEntity.ok(ApiResponse.success("Annotations retrieved successfully", annotations));
    }

    /**
     * Get full class detail including methods, fields, and annotations by class ID.
     * GET /api/v1/search/classes/detail?id=123
     */
    @GetMapping("/search/classes/detail")
    @Operation(summary = "Get class detail", description = "Retrieves full class details including nested methods, fields, and annotations")
    public ResponseEntity<Object> lookupClassDetail(
            @Parameter(description = "Class database ID") @RequestParam Long id) {
        ClassInfo detail = indexerService.getClassDetailById(id);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ClassInfo>notFound("Class with id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Class detail retrieved successfully", detail));
    }

    /**
     * Get methods for a specific class by class ID.
     * GET /api/v1/search/classes/{classId}/methods
     */
    @GetMapping("/search/classes/{classId}/methods")
    @Operation(summary = "Get class methods", description = "Retrieves all methods belonging to a specific indexed class")
    public ResponseEntity<ApiResponse<List<MethodInfo>>> lookupClassMethods(
            @Parameter(description = "Class database ID") @PathVariable Long classId) {
        List<MethodInfo> methods = indexerService.getMethodsByClassId(classId);
        return ResponseEntity.ok(ApiResponse.success("Class methods retrieved successfully", methods));
    }

    /**
     * Get fields for a specific class by class ID.
     * GET /api/v1/search/classes/{classId}/fields
     */
    @GetMapping("/search/classes/{classId}/fields")
    @Operation(summary = "Get class fields", description = "Retrieves all fields belonging to a specific indexed class")
    public ResponseEntity<ApiResponse<List<FieldInfo>>> lookupClassFields(
            @Parameter(description = "Class database ID") @PathVariable Long classId) {
        List<FieldInfo> fields = indexerService.getFieldsByClassId(classId);
        return ResponseEntity.ok(ApiResponse.success("Class fields retrieved successfully", fields));
    }

    /**
     * Search files by path pattern.
     * GET /api/v1/search/files?path=pattern
     */
    @GetMapping("/search/files")
    @Operation(summary = "Search indexed files", description = "Searches indexed files by their file path pattern")
    public ResponseEntity<ApiResponse<List<FileIndex>>> lookupFiles(
            @Parameter(description = "Path search pattern") @RequestParam(required = true, defaultValue = "") String path) {
        List<FileIndex> files = indexerService.searchFilesByPath(path);
        return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", files));
    }

    /**
     * Get all available Spring component types.
     * GET /api/v1/search/component-types
     */
    @GetMapping("/search/component-types")
    @Operation(summary = "Get component types", description = "Returns all available Spring component types found in the indexed codebase")
    public ResponseEntity<ApiResponse<List<String>>> lookupComponentTypes() {
        List<String> types = indexerService.getAvailableComponentTypes();
        return ResponseEntity.ok(ApiResponse.success("Component types retrieved successfully", types));
    }

    // ==================== Repository Summary Operations ====================

    /**
     * Get repository summary statistics from indexed metadata.
     * GET /api/v1/repository-summary
     */
    @GetMapping("/repository-summary")
    @Operation(summary = "Get repository summary", description = "Returns overall statistics and summary of the indexed codebase")
    public ResponseEntity<ApiResponse<RepositorySummary>> getRepositorySummary() {
        RepositorySummary summary = indexerService.getRepositorySummary();
        return ResponseEntity.ok(ApiResponse.success("Repository summary retrieved successfully", summary));
    }
}