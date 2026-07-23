package com.projectiq.indexerlocal.controller;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.service.IndexerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for indexing operations.
 */
@RestController
@RequestMapping("/api")
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
    public String indexFile(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "filePath", required = false) String filePath) throws IOException {
        indexerService.indexFile(file, filePath);
        return "File indexed successfully";
    }

    /**
     * Index multiple files.
     */
    @PostMapping("/index/files")
    @ResponseStatus(HttpStatus.OK)
    public String indexFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
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
    public String indexDirectory(@RequestParam("directory") String directory) {
        indexerService.indexDirectory(directory);
        return "Directory indexed successfully: " + directory;
    }

    // ==================== File Query Operations ====================

    /**
     * List all indexed source files.
     */
    @GetMapping("/files")
    public List<FileIndex> listFiles() {
        return indexerService.listFiles();
    }

    /**
     * Get a source file by ID.
     */
    @GetMapping("/files/{id}")
    public FileIndex getFileById(@PathVariable Long id) {
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
    public FileIndex getFileByPath(@RequestParam String path) {
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
    public List<ClassInfo> listClasses() {
        return indexerService.listClasses();
    }

    /**
     * Get a class by ID.
     */
    @GetMapping("/classes/{id}")
    public ClassInfo getClassById(@PathVariable Long id) {
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
    public ClassInfo getClassByName(@RequestParam String name) {
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
    public List<MethodInfo> listMethods() {
        return indexerService.listMethods();
    }

    /**
     * Get a method by ID.
     */
    @GetMapping("/methods/{id}")
    public MethodInfo getMethodById(@PathVariable Long id) {
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
    public MethodInfo getMethodByName(@RequestParam String name) {
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
    public List<FieldInfo> listFields() {
        return indexerService.listFields();
    }

    /**
     * Get a field by ID.
     */
    @GetMapping("/fields/{id}")
    public FieldInfo getFieldById(@PathVariable Long id) {
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
    public FieldInfo getFieldByName(@RequestParam String name) {
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
    public List<SpringComponent> listSpringComponents() {
        return indexerService.listSpringComponents();
    }

    // ==================== Lookup Operations ====================

    /**
     * Search classes by name pattern (partial match).
     */
    @GetMapping("/lookup/classes")
    public List<ClassInfo> lookupClasses(@RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchClassesByName(name);
    }

    /**
     * Search methods by name pattern (partial match).
     */
    @GetMapping("/lookup/methods")
    public List<MethodInfo> lookupMethods(@RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchMethodsByName(name);
    }

    /**
     * Search fields by name pattern (partial match).
     */
    @GetMapping("/lookup/fields")
    public List<FieldInfo> lookupFields(@RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchFieldsByName(name);
    }

    /**
     * Search classes by package/path pattern.
     */
    @GetMapping("/lookup/by-package")
    public List<ClassInfo> lookupByPackage(@RequestParam(required = true, defaultValue = "") String pkg) {
        return indexerService.searchClassesByPackage(pkg);
    }

    /**
     * Search annotations by name pattern (partial match).
     */
    @GetMapping("/lookup/annotations")
    public List<AnnotationInfo> lookupAnnotations(@RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchAnnotationsByName(name);
    }

    /**
     * Get all annotations for a specific target (class/method/field).
     */
    @GetMapping("/lookup/target-annotations")
    public List<AnnotationInfo> lookupTargetAnnotations(@RequestParam String type, @RequestParam Long id) {
        return indexerService.getAnnotationsByTarget(type, id);
    }

    /**
     * Get full class detail including methods, fields, and annotations by class ID.
     */
    @GetMapping("/lookup/class-detail")
    public ClassInfo lookupClassDetail(@RequestParam Long id) {
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
    public List<MethodInfo> lookupClassMethods(@RequestParam Long classId) {
        return indexerService.getMethodsByClassId(classId);
    }

    /**
     * Get fields for a specific class by class ID.
     */
    @GetMapping("/lookup/class-fields")
    public List<FieldInfo> lookupClassFields(@RequestParam Long classId) {
        return indexerService.getFieldsByClassId(classId);
    }

    /**
     * Search files by path pattern.
     */
    @GetMapping("/lookup/files")
    public List<FileIndex> lookupFiles(@RequestParam(required = true, defaultValue = "") String path) {
        return indexerService.searchFilesByPath(path);
    }

    /**
     * Get all available Spring component types.
     */
    @GetMapping("/lookup/component-types")
    public List<String> lookupComponentTypes() {
        return indexerService.getAvailableComponentTypes();
    }

    /**
     * Search Spring components by type (case-insensitive, supports SERVICE, REPOSITORY, CONTROLLER, etc.).
     */
    @GetMapping("/lookup/spring-components")
    public List<SpringComponent> lookupSpringComponents(@RequestParam(required = true, defaultValue = "") String type) {
        return indexerService.searchSpringComponentsByType(type);
    }

    /**
     * Search Spring components by class name pattern.
     */
    @GetMapping("/lookup/spring-components/name")
    public List<SpringComponent> lookupSpringComponentsByName(@RequestParam(required = true, defaultValue = "") String name) {
        return indexerService.searchSpringComponentsByName(name);
    }
}
