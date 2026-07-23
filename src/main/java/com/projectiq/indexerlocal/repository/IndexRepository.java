package com.projectiq.indexerlocal.repository;

import com.projectiq.indexerlocal.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for persisting indexed metadata into SQLite.
 */
@Repository
public class IndexRepository {

    private final JdbcTemplate jdbcTemplate;

    public IndexRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

        jdbcTemplate.execute(sqlCreateFileIndex);
        jdbcTemplate.execute(sqlCreateClassInfo);
        jdbcTemplate.execute(sqlCreateFieldInfo);
        jdbcTemplate.execute(sqlCreateMethodInfo);
        jdbcTemplate.execute(sqlCreateAnnotationInfo);
        jdbcTemplate.execute(sqlCreateImportInfo);
    }

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
        String sql = """
            INSERT INTO file_index (file_path, file_name, class_count, method_count, field_count, annotation_count)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                fileIndex.getFilePath(),
                fileIndex.getFileName(),
                fileIndex.getClassCount(),
                fileIndex.getMethodCount(),
                fileIndex.getFieldCount(),
                fileIndex.getAnnotationCount());

        Long id = jdbcTemplate.queryForObject(
                "SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private Long saveClassInfo(ClassInfo classInfo, Long fileIndexId) {
        String sql = """
            INSERT INTO class_info (file_index_id, class_name, class_type, visibility, super_class, interfaces)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                fileIndexId,
                classInfo.getClassName(),
                classInfo.getClassType(),
                classInfo.getVisibility(),
                classInfo.getSuperClass(),
                classInfo.getInterfaces() != null ? String.join(",", classInfo.getInterfaces()) : null);

        Long id = jdbcTemplate.queryForObject(
                "SELECT last_insert_rowid()", Long.class);
        return id;
    }

    private void saveFieldInfo(com.projectiq.indexerlocal.model.FieldInfo fieldInfo, Long classId) {
        String sql = """
            INSERT INTO field_info (class_id, field_name, field_type, visibility, is_static, is_final)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                classId,
                fieldInfo.getFieldName(),
                fieldInfo.getFieldType(),
                fieldInfo.getVisibility(),
                fieldInfo.isStatic() ? 1 : 0,
                fieldInfo.isFinal() ? 1 : 0);

        // Save field annotations
        if (fieldInfo.getAnnotations() != null && !fieldInfo.getAnnotations().isEmpty()) {
            for (int i = 0; i < fieldInfo.getAnnotations().size(); i++) {
                AnnotationInfo annotationInfo = fieldInfo.getAnnotations().get(i);
                saveAnnotationInfo(annotationInfo, "FIELD", classId);
            }
        }
    }

    private void saveMethodInfo(MethodInfo methodInfo, Long classId) {
        String sql = """
            INSERT INTO method_info (class_id, method_name, method_signature, return_type, visibility, is_static, is_abstract, parameters, exceptions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
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

        // Save method annotations
        if (methodInfo.getAnnotations() != null) {
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotations()) {
                saveAnnotationInfo(annotationInfo, "METHOD", classId);
            }
        }
    }

    private void saveAnnotationInfo(AnnotationInfo annotationInfo, String targetType, Long targetId) {
        String sql = """
            INSERT INTO annotation_info (annotation_name, full_name, target_type, target_id)
            VALUES (?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                annotationInfo.getAnnotationName(),
                annotationInfo.getFullName(),
                targetType,
                targetId);
    }

    private void saveImportInfo(ImportInfo importInfo, Long fileIndexId) {
        String sql = """
            INSERT INTO import_info (file_index_id, import_name, is_static)
            VALUES (?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                fileIndexId,
                importInfo.getImportName(),
                importInfo.isStatic() ? 1 : 0);
    }
}