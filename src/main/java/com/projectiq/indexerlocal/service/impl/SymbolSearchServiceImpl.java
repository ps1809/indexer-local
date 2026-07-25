package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.*;
import com.projectiq.indexerlocal.model.symbol.SymbolEntry;
import com.projectiq.indexerlocal.model.symbol.SymbolSearchResult;
import com.projectiq.indexerlocal.repository.IndexRepository;
import com.projectiq.indexerlocal.service.SymbolSearchService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of SymbolSearchService that queries the indexed SQLite database
 * for fast, deterministic symbol lookups without filesystem scanning.
 */
@Service
public class SymbolSearchServiceImpl implements SymbolSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final IndexRepository indexRepository;

    public SymbolSearchServiceImpl(JdbcTemplate jdbcTemplate, IndexRepository indexRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.indexRepository = indexRepository;
    }

    // ==================== Search Mode Helpers ====================

    private String buildLikePattern(String query, String mode) {
        if (query == null || query.isEmpty()) {
            return "%";
        }
        String escaped = query.replace("%", "\\%").replace("_", "\\_");
        return switch (mode != null ? mode.toUpperCase() : "PARTIAL") {
            case "EXACT" -> escaped;
            case "PREFIX" -> escaped + "%";
            case "FQN" -> "%" + escaped;
            default -> "%" + escaped + "%";
        };
    }

    // ==================== Class Type Searches ====================

    @Override
    public SymbolSearchResult findClass(String repositoryId, String query, String mode,
                                         String packageName, String visibility,
                                         int page, int size) {
        return searchClassByType("CLASS", repositoryId, query, mode, packageName, visibility, page, size);
    }

    @Override
    public SymbolSearchResult findInterface(String repositoryId, String query, String mode,
                                             String packageName, String visibility,
                                             int page, int size) {
        return searchClassByType("INTERFACE", repositoryId, query, mode, packageName, visibility, page, size);
    }

    @Override
    public SymbolSearchResult findEnum(String repositoryId, String query, String mode,
                                        String packageName, String visibility,
                                        int page, int size) {
        return searchClassByType("ENUM", repositoryId, query, mode, packageName, visibility, page, size);
    }

    @Override
    public SymbolSearchResult findRecord(String repositoryId, String query, String mode,
                                          String packageName, String visibility,
                                          int page, int size) {
        return searchClassByType("RECORD", repositoryId, query, mode, packageName, visibility, page, size);
    }

    private SymbolSearchResult searchClassByType(String classType, String repositoryId,
                                                  String query, String mode,
                                                  String packageName, String visibility,
                                                  int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ci.id, ci.class_name, ci.class_type, ci.visibility, ci.super_class, ci.interfaces, ")
           .append("fi.file_path, fi.file_name ")
           .append("FROM class_info ci ")
           .append("INNER JOIN file_index fi ON ci.file_index_id = fi.id ")
           .append("WHERE ci.class_type = ?");

        List<Object> params = new ArrayList<>();
        params.add(classType);

        String likePattern = buildLikePattern(query, mode);
        sql.append(" AND ci.class_name LIKE ? ESCAPE '\\'");
        params.add(likePattern);

        if (packageName != null && !packageName.isEmpty()) {
            sql.append(" AND fi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + packageName.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        if (visibility != null && !visibility.isEmpty()) {
            sql.append(" AND ci.visibility = ?");
            params.add(visibility.toUpperCase());
        }

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND fi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + repositoryId.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        // Count total
        String countSql = sql.toString().replaceFirst("SELECT ci.id, ci.class_name, ci.class_type, ci.visibility, ci.super_class, ci.interfaces, fi.file_path, fi.file_name", "SELECT COUNT(*)");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        // Apply pagination
        sql.append(" ORDER BY ci.class_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<SymbolEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            SymbolEntry entry = new SymbolEntry();
            entry.setSymbolName(rs.getString("class_name"));
            entry.setSymbolType(rs.getString("class_type"));
            entry.setVisibility(rs.getString("visibility"));
            entry.setFilePath(rs.getString("file_path"));
            entry.setLanguage("java");
            // Derive package from file path
            String filePath = rs.getString("file_path");
            if (filePath != null) {
                entry.setPackageName(derivePackageFromPath(filePath));
            }
            return entry;
        }, params.toArray());

        return new SymbolSearchResult(entries, page, size, totalElements);
    }

    // ==================== Annotation Search ====================

    @Override
    public SymbolSearchResult findAnnotation(String repositoryId, String query, String mode,
                                              String packageName, int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ai.id, ai.annotation_name, ai.full_name, ai.target_type, ai.target_id ")
           .append("FROM annotation_info ai ")
           .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            String likePattern = buildLikePattern(query, mode);
            sql.append(" AND ai.annotation_name LIKE ? ESCAPE '\\'");
            params.add(likePattern);
        }

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND ai.annotation_name LIKE ? ESCAPE '\\'");
            params.add("%" + repositoryId.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        String countSql = sql.toString().replaceFirst("SELECT ai.id, ai.annotation_name, ai.full_name, ai.target_type, ai.target_id", "SELECT COUNT(*)");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        sql.append(" ORDER BY ai.annotation_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<SymbolEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            SymbolEntry entry = new SymbolEntry();
            entry.setSymbolName(rs.getString("annotation_name"));
            entry.setFullyQualifiedName(rs.getString("full_name"));
            entry.setSymbolType("ANNOTATION");
            entry.setLanguage("java");
            return entry;
        }, params.toArray());

        return new SymbolSearchResult(entries, page, size, totalElements);
    }

    // ==================== Method Search ====================

    @Override
    public SymbolSearchResult findMethod(String repositoryId, String query, String mode,
                                          String packageName, String visibility,
                                          int page, int size) {
        return searchMethodsOrConstructors("METHOD", repositoryId, query, mode, packageName, visibility, page, size);
    }

    @Override
    public SymbolSearchResult findConstructor(String repositoryId, String query, String mode,
                                               String packageName, String visibility,
                                               int page, int size) {
        return searchMethodsOrConstructors("CONSTRUCTOR", repositoryId, query, mode, packageName, visibility, page, size);
    }

    private SymbolSearchResult searchMethodsOrConstructors(String methodType, String repositoryId,
                                                            String query, String mode,
                                                            String packageName, String visibility,
                                                            int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mi.id, mi.method_name, mi.return_type, mi.visibility, mi.is_static, mi.is_abstract, ")
           .append("mi.parameters, mi.exceptions, mi.class_id, ci.class_name as parent_class, fi.file_path ")
           .append("FROM method_info mi ")
           .append("INNER JOIN class_info ci ON mi.class_id = ci.id ")
           .append("INNER JOIN file_index fi ON ci.file_index_id = fi.id ")
           .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            String likePattern = buildLikePattern(query, mode);
            sql.append(" AND mi.method_name LIKE ? ESCAPE '\\'");
            params.add(likePattern);
        }

        if (visibility != null && !visibility.isEmpty()) {
            sql.append(" AND mi.visibility = ?");
            params.add(visibility.toUpperCase());
        }

        if (packageName != null && !packageName.isEmpty()) {
            sql.append(" AND fi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + packageName.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND fi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + repositoryId.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        String countSql = sql.toString().replaceFirst(
            "SELECT mi.id, mi.method_name, mi.return_type, mi.visibility, mi.is_static, mi.is_abstract, mi.parameters, mi.exceptions, mi.class_id, ci.class_name as parent_class, fi.file_path",
            "SELECT COUNT(*)");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        sql.append(" ORDER BY mi.method_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<SymbolEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            SymbolEntry entry = new SymbolEntry();
            entry.setSymbolName(rs.getString("method_name"));
            entry.setSymbolType("METHOD");
            entry.setVisibility(rs.getString("visibility"));
            entry.setFilePath(rs.getString("file_path"));
            entry.setParentSymbol(rs.getString("parent_class"));
            entry.setLanguage("java");
            String filePath = rs.getString("file_path");
            if (filePath != null) {
                entry.setPackageName(derivePackageFromPath(filePath));
            }
            return entry;
        }, params.toArray());

        return new SymbolSearchResult(entries, page, size, totalElements);
    }

    // ==================== Field Search ====================

    @Override
    public SymbolSearchResult findField(String repositoryId, String query, String mode,
                                         String packageName, String visibility,
                                         int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT fi.id, fi.field_name, fi.field_type, fi.visibility, fi.is_static, fi.is_final, ")
           .append("fi.class_id, ci.class_name as parent_class, fxi.file_path ")
           .append("FROM field_info fi ")
           .append("INNER JOIN class_info ci ON fi.class_id = ci.id ")
           .append("INNER JOIN file_index fxi ON ci.file_index_id = fxi.id ")
           .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            String likePattern = buildLikePattern(query, mode);
            sql.append(" AND fi.field_name LIKE ? ESCAPE '\\'");
            params.add(likePattern);
        }

        if (visibility != null && !visibility.isEmpty()) {
            sql.append(" AND fi.visibility = ?");
            params.add(visibility.toUpperCase());
        }

        if (packageName != null && !packageName.isEmpty()) {
            sql.append(" AND fxi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + packageName.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND fxi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + repositoryId.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        String countSql = sql.toString().replaceFirst(
            "SELECT fi.id, fi.field_name, fi.field_type, fi.visibility, fi.is_static, fi.is_final, fi.class_id, ci.class_name as parent_class, fxi.file_path",
            "SELECT COUNT(*)");
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        sql.append(" ORDER BY fi.field_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<SymbolEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            SymbolEntry entry = new SymbolEntry();
            entry.setSymbolName(rs.getString("field_name"));
            entry.setSymbolType("FIELD");
            entry.setVisibility(rs.getString("visibility"));
            entry.setFilePath(rs.getString("file_path"));
            entry.setParentSymbol(rs.getString("parent_class"));
            entry.setLanguage("java");
            String filePath = rs.getString("file_path");
            if (filePath != null) {
                entry.setPackageName(derivePackageFromPath(filePath));
            }
            return entry;
        }, params.toArray());

        return new SymbolSearchResult(entries, page, size, totalElements);
    }

    // ==================== Package Search ====================

    @Override
    public SymbolSearchResult findPackage(String repositoryId, String query, String mode,
                                           int page, int size) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ")
           .append("CASE ")
           .append("  WHEN instr(fi.file_path, '/src/main/java/') > 0 ")
           .append("    THEN substr(fi.file_path, instr(fi.file_path, '/src/main/java/') + 15, ")
           .append("           length(fi.file_path) - instr(fi.file_path, '/src/main/java/') - 15 - instr(reverse(fi.file_path), '/') + 1) ")
           .append("  WHEN instr(fi.file_path, '/src/') > 0 ")
           .append("    THEN substr(fi.file_path, instr(fi.file_path, '/src/') + 5, ")
           .append("           length(fi.file_path) - instr(fi.file_path, '/src/') - 5 - instr(reverse(fi.file_path), '/') + 1) ")
           .append("  ELSE '' ")
           .append("END as package_name, fi.file_path ")
           .append("FROM file_index fi ")
           .append("WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            String likePattern = buildLikePattern(query, mode);
            sql.append(" AND (")
               .append("  CASE ")
               .append("    WHEN instr(fi.file_path, '/src/main/java/') > 0 ")
               .append("      THEN substr(fi.file_path, instr(fi.file_path, '/src/main/java/') + 15, ")
               .append("             length(fi.file_path) - instr(fi.file_path, '/src/main/java/') - 15 - instr(reverse(fi.file_path), '/') + 1) ")
               .append("    WHEN instr(fi.file_path, '/src/') > 0 ")
               .append("      THEN substr(fi.file_path, instr(fi.file_path, '/src/') + 5, ")
               .append("             length(fi.file_path) - instr(fi.file_path, '/src/') - 5 - instr(reverse(fi.file_path), '/') + 1) ")
               .append("    ELSE '' ")
               .append("  END")
               .append(") LIKE ? ESCAPE '\\'");
            params.add(likePattern);
        }

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql.append(" AND fi.file_path LIKE ? ESCAPE '\\'");
            params.add("%" + repositoryId.replace("%", "\\%").replace("_", "\\_") + "%");
        }

        // Count distinct packages
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ")";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        sql.append(" ORDER BY package_name ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<SymbolEntry> entries = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            SymbolEntry entry = new SymbolEntry();
            String pkg = rs.getString("package_name");
            entry.setSymbolName(pkg != null ? pkg.replace('/', '.') : "");
            entry.setSymbolType("PACKAGE");
            entry.setFilePath(rs.getString("file_path"));
            entry.setLanguage("java");
            if (pkg != null) {
                entry.setPackageName(pkg.replace('/', '.'));
            }
            return entry;
        }, params.toArray());

        return new SymbolSearchResult(entries, page, size, totalElements);
    }

    // ==================== General Symbol Search ====================

    @Override
    public SymbolSearchResult searchSymbols(String repositoryId, String query, String mode,
                                             String symbolType, String packageName,
                                             String visibility, String module,
                                             int page, int size) {
        // If a specific symbol type is provided, delegate to the appropriate method
        if (symbolType != null && !symbolType.isEmpty()) {
            return switch (symbolType.toUpperCase()) {
                case "CLASS" -> findClass(repositoryId, query, mode, packageName, visibility, page, size);
                case "INTERFACE" -> findInterface(repositoryId, query, mode, packageName, visibility, page, size);
                case "ENUM" -> findEnum(repositoryId, query, mode, packageName, visibility, page, size);
                case "RECORD" -> findRecord(repositoryId, query, mode, packageName, visibility, page, size);
                case "ANNOTATION" -> findAnnotation(repositoryId, query, mode, packageName, page, size);
                case "METHOD" -> findMethod(repositoryId, query, mode, packageName, visibility, page, size);
                case "CONSTRUCTOR" -> findConstructor(repositoryId, query, mode, packageName, visibility, page, size);
                case "FIELD" -> findField(repositoryId, query, mode, packageName, visibility, page, size);
                case "PACKAGE" -> findPackage(repositoryId, query, mode, page, size);
                default -> {
                    // Unknown type, return empty
                    yield new SymbolSearchResult(List.of(), page, size, 0);
                }
            };
        }

        // General search across all types - combine results from classes, methods, fields
        List<SymbolEntry> allResults = new ArrayList<>();

        // Search classes
        SymbolSearchResult classResults = findClass(repositoryId, query, mode, packageName, visibility, 0, Integer.MAX_VALUE);
        allResults.addAll(classResults.getContent());

        // Search methods
        SymbolSearchResult methodResults = findMethod(repositoryId, query, mode, packageName, visibility, 0, Integer.MAX_VALUE);
        allResults.addAll(methodResults.getContent());

        // Search fields
        SymbolSearchResult fieldResults = findField(repositoryId, query, mode, packageName, visibility, 0, Integer.MAX_VALUE);
        allResults.addAll(fieldResults.getContent());

        // Search packages
        SymbolSearchResult pkgResults = findPackage(repositoryId, query, mode, 0, Integer.MAX_VALUE);
        allResults.addAll(pkgResults.getContent());

        long totalElements = allResults.size();

        // Apply pagination
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, (int) totalElements);
        List<SymbolEntry> paged;
        if (fromIndex < totalElements) {
            paged = allResults.subList(fromIndex, toIndex);
        } else {
            paged = List.of();
        }

        return new SymbolSearchResult(paged, page, size, totalElements);
    }

    // ==================== Helper Methods ====================

    private String derivePackageFromPath(String filePath) {
        if (filePath == null) return "";
        // Try to extract package from path like .../src/main/java/com/example/MyClass.java
        String javaSrc = "/src/main/java/";
        int idx = filePath.indexOf(javaSrc);
        if (idx >= 0) {
            String afterSrc = filePath.substring(idx + javaSrc.length());
            int lastSlash = afterSrc.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterSrc.substring(0, lastSlash).replace('/', '.');
            }
        }
        // Try /src/ pattern
        String src = "/src/";
        idx = filePath.indexOf(src);
        if (idx >= 0) {
            String afterSrc = filePath.substring(idx + src.length());
            int lastSlash = afterSrc.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterSrc.substring(0, lastSlash).replace('/', '.');
            }
        }
        return "";
    }
}