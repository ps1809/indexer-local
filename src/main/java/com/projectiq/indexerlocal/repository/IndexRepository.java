package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

/**
 * Repository for persisting indexed metadata into SQLite.
 */
@Repository
public class IndexRepository {

    private final JdbcTemplate jdbcTemplate;

    public IndexRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== Row Mappers ====================

    private final RowMapper<FileIndex> fileRowMapper = (rs, rowNum) -> {
        FileIndex file = new FileIndex();
        file.setId(rs.getLong("id"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileName(rs.getString("file_name"));
        file.setClassCount(rs.getInt("class_count"));
        file.setMethodCount(rs.getInt("method_count"));
        file.setFieldCount(rs.getInt("field_count"));
        file.setAnnotationCount(rs.getInt("annotation_count"));
        return file;
    };

    private final RowMapper<ClassInfo> classRowMapper = (rs, rowNum) -> {
        ClassInfo cls = new ClassInfo();
        cls.setId(rs.getLong("id"));
        cls.setFileIndexId(rs.getLong("file_index_id"));
        cls.setClassName(rs.getString("class_name"));
        cls.setClassType(rs.getString("class_type"));
        cls.setVisibility(rs.getString("visibility"));
        cls.setSuperClass(rs.getString("super_class"));
        String interfaces = rs.getString("interfaces");
        if (interfaces != null && !interfaces.isEmpty()) {
            cls.setInterfaces(List.of(interfaces.split(",")));
        }
        return cls;
    };

    private final RowMapper<MethodInfo> methodRowMapper = (rs, rowNum) -> {
        MethodInfo method = new MethodInfo();
        method.setId(rs.getLong("id"));
        method.setClassId(rs.getLong("class_id"));
        method.setMethodName(rs.getString("method_name"));
        method.setMethodSignature(rs.getString("method_signature"));
        method.setReturnType(rs.getString("return_type"));
        method.setVisibility(rs.getString("visibility"));
        method.setStatic(rs.getInt("is_static") == 1);
        method.setAbstract(rs.getInt("is_abstract") == 1);
        String parameters = rs.getString("parameters");
        if (parameters != null && !parameters.isEmpty()) {
            method.setParameters(List.of(parameters.split(",")));
        }
        String exceptions = rs.getString("exceptions");
        if (exceptions != null && !exceptions.isEmpty()) {
            method.setExceptions(List.of(exceptions.split(",")));
        }
        return method;
    };

    private final RowMapper<FieldInfo> fieldRowMapper = (rs, rowNum) -> {
        FieldInfo field = new FieldInfo();
        field.setId(rs.getLong("id"));
        field.setClassId(rs.getLong("class_id"));
        field.setFieldName(rs.getString("field_name"));
        field.setFieldType(rs.getString("field_type"));
        field.setVisibility(rs.getString("visibility"));
        field.setStatic(rs.getInt("is_static") == 1);
        field.setFinal(rs.getInt("is_final") == 1);
        return field;
    };

    private final RowMapper<SpringComponent> springComponentRowMapper = (rs, rowNum) -> {
        SpringComponent component = new SpringComponent();
        component.setId(rs.getLong("id"));
        component.setClassId(rs.getLong("class_id"));
        component.setComponentType(rs.getString("component_type"));
        component.setClassName(rs.getString("class_name"));
        component.setFileIndexId(rs.getLong("file_index_id"));
        return component;
    };

    // ==================== Schema Initialization ====================

    /**
     * Initialize the database schema.
     */
    public void initSchema() {
        String sqlCreateFileIndex = 
            "CREATE TABLE IF NOT EXISTS file_index (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_path TEXT NOT NULL, " +
            "file_name TEXT NOT NULL, " +
            "class_count INTEGER DEFAULT 0, " +
            "method_count INTEGER DEFAULT 0, " +
            "field_count INTEGER DEFAULT 0, " +
            "annotation_count INTEGER DEFAULT 0, " +
            "indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String sqlCreateClassInfo = 
            "CREATE TABLE IF NOT EXISTS class_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_index_id INTEGER, " +
            "class_name TEXT NOT NULL, " +
            "class_type TEXT, " +
            "visibility TEXT, " +
            "super_class TEXT, " +
            "interfaces TEXT, " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        String sqlCreateFieldInfo = 
            "CREATE TABLE IF NOT EXISTS field_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "class_id INTEGER, " +
            "field_name TEXT NOT NULL, " +
            "field_type TEXT, " +
            "visibility TEXT, " +
            "is_static INTEGER DEFAULT 0, " +
            "is_final INTEGER DEFAULT 0, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id))";

        String sqlCreateMethodInfo = 
            "CREATE TABLE IF NOT EXISTS method_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "class_id INTEGER, " +
            "method_name TEXT NOT NULL, " +
            "method_signature TEXT, " +
            "return_type TEXT, " +
            "visibility TEXT, " +
            "is_static INTEGER DEFAULT 0, " +
            "is_abstract INTEGER DEFAULT 0, " +
            "parameters TEXT, " +
            "exceptions TEXT, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id))";

        String sqlCreateAnnotationInfo = 
            "CREATE TABLE IF NOT EXISTS annotation_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "annotation_name TEXT NOT NULL, " +
            "full_name TEXT, " +
            "target_type TEXT, " +
            "target_id INTEGER)";

        String sqlCreateImportInfo = 
            "CREATE TABLE IF NOT EXISTS import_info (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "file_index_id INTEGER, " +
            "import_name TEXT NOT NULL, " +
            "is_static INTEGER DEFAULT 0, " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        String sqlCreateSpringComponent = 
            "CREATE TABLE IF NOT EXISTS spring_component (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "class_id INTEGER, " +
            "component_type TEXT NOT NULL, " +
            "class_name TEXT NOT NULL, " +
            "file_index_id INTEGER, " +
            "FOREIGN KEY (class_id) REFERENCES class_info(id), " +
            "FOREIGN KEY (file_index_id) REFERENCES file_index(id))";

        jdbcTemplate.execute(sqlCreateFileIndex);
        jdbcTemplate.execute(sqlCreateClassInfo);
        jdbcTemplate.execute(sqlCreateFieldInfo);
        jdbcTemplate.execute(sqlCreateMethodInfo);
        jdbcTemplate.execute(sqlCreateAnnotationInfo);
        jdbcTemplate.execute(sqlCreateImportInfo);
        jdbcTemplate.execute(sqlCreateSpringComponent);
    }

    // ==================== Save Methods ====================

    /**
     * Persist the complete IndexResult into SQLite.
     */
    public void saveIndexResult(IndexResult result) {
        initSchema();

        // Save file indexes
        for (FileIndex fileIndex : result.getFileIndexes()) {
            Long fileIndexId = saveFileIndex(fileIndex);

            // Save classes
            if (fileIndex.getClasses() != null) {
                for (ClassInfo classInfo : fileIndex.getClasses()) {
                    Long classId = saveClassInfo(classInfo, fileIndexId);
                    classInfo.setId(classId);

                    // Save fields
                    if (classInfo.getFields() != null) {
                        for (FieldInfo fieldInfo : classInfo.getFields()) {
                            saveFieldInfo(fieldInfo, classId);
                        }
                    }

                    // Save methods
                    if (classInfo.getMethods() != null) {
                        for (MethodInfo methodInfo : classInfo.getMethods()) {
                            saveMethodInfo(methodInfo, classId);
                        }
                    }

                    // Save annotations
                    if (classInfo.getAnnotations() != null) {
                        for (AnnotationInfo annotationInfo : classInfo.getAnnotations()) {
                            saveAnnotationInfo(annotationInfo, "CLASS", classId);
                        }
                    }
                }
            }

            // Save imports
            if (fileIndex.getImports() != null) {
                for (ImportInfo importInfo : fileIndex.getImports()) {
                    saveImportInfo(importInfo, fileIndexId);
                }
            }
        }
    }

    private Long saveFileIndex(FileIndex fileIndex) {
        String sql = "INSERT INTO file_index (file_path, file_name, class_count, method_count, field_count, annotation_count) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndex.getFilePath(),
                fileIndex.getFileName(),
                fileIndex.getClassCount(),
                fileIndex.getMethodCount(),
                fileIndex.getFieldCount(),
                fileIndex.getAnnotationCount());

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private Long saveClassInfo(ClassInfo classInfo, Long fileIndexId) {
        String sql = "INSERT INTO class_info (file_index_id, class_name, class_type, visibility, super_class, interfaces) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndexId,
                classInfo.getClassName(),
                classInfo.getClassType(),
                classInfo.getVisibility(),
                classInfo.getSuperClass(),
                classInfo.getInterfaces() != null ? String.join(",", classInfo.getInterfaces()) : null);

        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private void saveFieldInfo(FieldInfo fieldInfo, Long classId) {
        String sql = "INSERT INTO field_info (class_id, field_name, field_type, visibility, is_static, is_final) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                classId,
                fieldInfo.getFieldName(),
                fieldInfo.getFieldType(),
                fieldInfo.getVisibility(),
                fieldInfo.isStatic() ? 1 : 0,
                fieldInfo.isFinal() ? 1 : 0);

        if (fieldInfo.getAnnotations() != null && !fieldInfo.getAnnotations().isEmpty()) {
            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotations()) {
                saveAnnotationInfo(annotationInfo, "FIELD", classId);
            }
        }
    }

    private void saveMethodInfo(MethodInfo methodInfo, Long classId) {
        String sql = "INSERT INTO method_info (class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                classId,
                methodInfo.getMethodName(),
                methodInfo.getMethodSignature(),
                methodInfo.getReturnType(),
                methodInfo.getVisibility(),
                methodInfo.isStatic() ? 1 : 0,
                methodInfo.isAbstract() ? 1 : 0,
                methodInfo.getParameters() != null ? String.join(",", methodInfo.getParameters()) : null,
                methodInfo.getExceptions() != null ? String.join(",", methodInfo.getExceptions()) : null);

        if (methodInfo.getAnnotations() != null) {
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotations()) {
                saveAnnotationInfo(annotationInfo, "METHOD", classId);
            }
        }
    }

    private void saveAnnotationInfo(AnnotationInfo annotationInfo, String targetType, Long targetId) {
        String sql = "INSERT INTO annotation_info (annotation_name, full_name, target_type, target_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                annotationInfo.getAnnotationName(),
                annotationInfo.getFullName(),
                targetType,
                targetId);
    }

    private void saveImportInfo(ImportInfo importInfo, Long fileIndexId) {
        String sql = "INSERT INTO import_info (file_index_id, import_name, is_static) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                fileIndexId,
                importInfo.getImportName(),
                importInfo.isStatic() ? 1 : 0);
    }

    public Long saveSpringComponent(SpringComponent component) {
        String sql = "INSERT INTO spring_component (class_id, component_type, class_name, file_index_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setLong(1, component.getClassId());
            ps.setString(2, component.getComponentType());
            ps.setString(3, component.getClassName());
            ps.setLong(4, component.getFileIndexId());
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        component.setId(id);
        return id;
    }

    // ==================== Query Methods for REST APIs ====================

    /**
     * List all indexed source files.
     */
    public List<FileIndex> findAllFiles() {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index ORDER BY id";
        return jdbcTemplate.query(sql, fileRowMapper);
    }

    /**
     * Find a source file by ID.
     */
    public FileIndex findFileById(Long id) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE id = ?";
        List<FileIndex> results = jdbcTemplate.query(sql, fileRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a source file by file path.
     */
    public FileIndex findFileByPath(String filePath) {
        String sql = "SELECT id, file_path, file_name, class_count, method_count, field_count, annotation_count FROM file_index WHERE file_path = ?";
        List<FileIndex> results = jdbcTemplate.query(sql, fileRowMapper, filePath);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all Java classes.
     */
    public List<ClassInfo> findAllClasses() {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info ORDER BY id";
        return jdbcTemplate.query(sql, classRowMapper);
    }

    /**
     * Find a class by ID.
     */
    public ClassInfo findClassById(Long id) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE id = ?";
        List<ClassInfo> results = jdbcTemplate.query(sql, classRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a class by name.
     */
    public ClassInfo findClassByName(String className) {
        String sql = "SELECT id, file_index_id, class_name, class_type, visibility, super_class, interfaces FROM class_info WHERE class_name = ? ORDER BY id DESC LIMIT 1";
        List<ClassInfo> results = jdbcTemplate.query(sql, classRowMapper, className);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all methods.
     */
    public List<MethodInfo> findAllMethods() {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info ORDER BY id";
        return jdbcTemplate.query(sql, methodRowMapper);
    }

    /**
     * Find a method by ID.
     */
    public MethodInfo findMethodById(Long id) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE id = ?";
        List<MethodInfo> results = jdbcTemplate.query(sql, methodRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a method by name.
     */
    public MethodInfo findMethodByName(String methodName) {
        String sql = "SELECT id, class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions FROM method_info WHERE method_name = ? ORDER BY id DESC LIMIT 1";
        List<MethodInfo> results = jdbcTemplate.query(sql, methodRowMapper, methodName);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all fields.
     */
    public List<FieldInfo> findAllFields() {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info ORDER BY id";
        return jdbcTemplate.query(sql, fieldRowMapper);
    }

    /**
     * Find a field by ID.
     */
    public FieldInfo findFieldById(Long id) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE id = ?";
        List<FieldInfo> results = jdbcTemplate.query(sql, fieldRowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Find a field by name.
     */
    public FieldInfo findFieldByName(String fieldName) {
        String sql = "SELECT id, class_id, field_name, field_type, visibility, is_static, is_final FROM field_info WHERE field_name = ? ORDER BY id DESC LIMIT 1";
        List<FieldInfo> results = jdbcTemplate.query(sql, fieldRowMapper, fieldName);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * List all Spring components.
     */
    public List<SpringComponent> findAllSpringComponents() {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper);
    }

    /**
     * Filter Spring components by type.
     */
    public List<SpringComponent> findSpringComponentsByType(String componentType) {
        String sql = "SELECT id, class_id, component_type, class_name, file_index_id FROM spring_component WHERE component_type = ? ORDER BY id";
        return jdbcTemplate.query(sql, springComponentRowMapper, componentType);
    }
}