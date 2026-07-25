package com.projectiq.indexerlocal.service.impl;

import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipEntry;
import com.projectiq.indexerlocal.model.relationshipsearch.RelationshipType;
import com.projectiq.indexerlocal.service.RelationshipSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of RelationshipSearchService that queries the indexed SQLite database
 * for deterministic relationship traversal without filesystem scanning.
 * <p>
 * Reuses data already indexed in class_info, import_info, field_info, method_info tables
 * to resolve inheritance, implementation, reference, dependency, and call relationships.
 */
@Service
public class RelationshipSearchServiceImpl implements RelationshipSearchService {

    private static final Logger log = LoggerFactory.getLogger(RelationshipSearchServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public RelationshipSearchServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== Implementations ====================

    @Override
    public List<RelationshipEntry> findImplementations(String repositoryId, String interfaceName,
                                                        boolean recursive, int maxDepth,
                                                        int page, int size) {
        Set<String> visited = new HashSet<>();
        Set<RelationshipEntry> results = new LinkedHashSet<>();
        findImplementationsRecursive(repositoryId, interfaceName, 0,
                recursive ? maxDepth : 1, visited, results);
        return paginate(new ArrayList<>(results), page, size);
    }

    @Override
    public long countImplementations(String repositoryId, String interfaceName,
                                      boolean recursive, int maxDepth) {
        return findImplementations(repositoryId, interfaceName, recursive,
                maxDepth, 0, Integer.MAX_VALUE).size();
    }

    private void findImplementationsRecursive(String repositoryId, String interfaceName,
                                               int currentDepth, int maxDepth,
                                               Set<String> visited, Set<RelationshipEntry> results) {
        if (currentDepth >= maxDepth || visited.contains(interfaceName)) {
            return;
        }
        visited.add(interfaceName);

        String sql = "SELECT ci.id, ci.class_name, ci.class_type, ci.interfaces, ci.super_class, " +
                "fi.file_path, fi.file_name " +
                "FROM class_info ci " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE ci.interfaces LIKE ?";

        List<Object> params = new ArrayList<>();
        params.add("%" + interfaceName + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY ci.class_name ASC";

        List<RelationshipEntry> directResults = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            entry.setSourceSymbol(rs.getString("class_name"));
            entry.setSourceSymbolType(rs.getString("class_type"));
            entry.setSourceFilePath(rs.getString("file_path"));
            entry.setSourcePackage(derivePackageFromPath(rs.getString("file_path")));
            entry.setSourceModule(deriveModuleFromPath(rs.getString("file_path")));
            entry.setTargetSymbol(interfaceName);
            entry.setTargetSymbolType("INTERFACE");
            entry.setRelationshipType(RelationshipType.IMPLEMENTATION);
            entry.setTraversalDepth(currentDepth + 1);
            return entry;
        }, params.toArray());

        results.addAll(directResults);

        // Recursive: find classes that extend these implementors (transitive inheritance)
        if (currentDepth + 1 < maxDepth) {
            for (RelationshipEntry entry : directResults) {
                String childClassName = entry.getSourceSymbol();
                // Find classes that extend this child
                findInheritorsRecursive(repositoryId, childClassName, currentDepth + 1,
                        maxDepth, new HashSet<>(visited), results);
            }
        }
    }

    // ==================== Inheritors ====================

    @Override
    public List<RelationshipEntry> findInheritors(String repositoryId, String className,
                                                    boolean recursive, int maxDepth,
                                                    int page, int size) {
        Set<String> visited = new HashSet<>();
        Set<RelationshipEntry> results = new LinkedHashSet<>();
        findInheritorsRecursive(repositoryId, className, 0,
                recursive ? maxDepth : 1, visited, results);
        return paginate(new ArrayList<>(results), page, size);
    }

    @Override
    public long countInheritors(String repositoryId, String className,
                                 boolean recursive, int maxDepth) {
        return findInheritors(repositoryId, className, recursive,
                maxDepth, 0, Integer.MAX_VALUE).size();
    }

    private void findInheritorsRecursive(String repositoryId, String className,
                                          int currentDepth, int maxDepth,
                                          Set<String> visited, Set<RelationshipEntry> results) {
        if (currentDepth >= maxDepth || visited.contains(className)) {
            return;
        }
        visited.add(className);

        String sql = "SELECT ci.id, ci.class_name, ci.class_type, ci.super_class, ci.interfaces, " +
                "fi.file_path, fi.file_name " +
                "FROM class_info ci " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE ci.super_class LIKE ?";

        List<Object> params = new ArrayList<>();
        params.add("%" + className + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY ci.class_name ASC";

        List<RelationshipEntry> directResults = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            entry.setSourceSymbol(rs.getString("class_name"));
            entry.setSourceSymbolType(rs.getString("class_type"));
            entry.setSourceFilePath(rs.getString("file_path"));
            entry.setSourcePackage(derivePackageFromPath(rs.getString("file_path")));
            entry.setSourceModule(deriveModuleFromPath(rs.getString("file_path")));
            entry.setTargetSymbol(className);
            entry.setTargetSymbolType("CLASS");
            entry.setRelationshipType(RelationshipType.INHERITANCE);
            entry.setTraversalDepth(currentDepth + 1);
            return entry;
        }, params.toArray());

        results.addAll(directResults);

        // Recursive: find inheritors of these inheritors
        if (currentDepth + 1 < maxDepth) {
            for (RelationshipEntry entry : directResults) {
                String childClassName = entry.getSourceSymbol();
                findInheritorsRecursive(repositoryId, childClassName, currentDepth + 1,
                        maxDepth, new HashSet<>(visited), results);
            }
        }
    }

    // ==================== References ====================

    @Override
    public List<RelationshipEntry> findReferences(String repositoryId, String symbolName,
                                                    int page, int size) {
        List<Object> params = new ArrayList<>();

        // Search in import_info for references to this symbol
        String sql = "SELECT ii.import_name, fi.file_path, fi.file_name " +
                "FROM import_info ii " +
                "INNER JOIN file_index fi ON ii.file_index_id = fi.id " +
                "WHERE ii.import_name LIKE ?";

        String likePattern = "%" + symbolName + "%";
        params.add(likePattern);

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY ii.import_name ASC";

        List<RelationshipEntry> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String importName = rs.getString("import_name");
            String filePath = rs.getString("file_path");

            entry.setSourceSymbol(importName);
            entry.setSourceSymbolType("IMPORT");
            entry.setSourceFilePath(filePath);
            entry.setSourcePackage(derivePackageFromPath(filePath));
            entry.setSourceModule(deriveModuleFromPath(filePath));
            entry.setTargetSymbol(symbolName);
            entry.setTargetSymbolType("REFERENCED");
            entry.setRelationshipType(RelationshipType.REFERENCE);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());

        return paginate(results, page, size);
    }

    @Override
    public long countReferences(String repositoryId, String symbolName) {
        return findReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Usages ====================

    @Override
    public List<RelationshipEntry> findUsages(String repositoryId, String symbolName,
                                                int page, int size) {
        Set<RelationshipEntry> combined = new LinkedHashSet<>();

        // References
        combined.addAll(findReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));

        // Inheritors
        combined.addAll(findInheritors(repositoryId, symbolName, false, 1, 0, Integer.MAX_VALUE));

        // Implementations
        combined.addAll(findImplementations(repositoryId, symbolName, false, 1, 0, Integer.MAX_VALUE));

        // Also search in field types and method return types
        combined.addAll(findFieldReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));
        combined.addAll(findMethodReturnReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));

        return paginate(new ArrayList<>(combined), page, size);
    }

    @Override
    public long countUsages(String repositoryId, String symbolName) {
        return findUsages(repositoryId, symbolName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Dependencies ====================

    @Override
    public List<RelationshipEntry> findDependencies(String repositoryId, String symbolName,
                                                      int page, int size) {
        Set<RelationshipEntry> combined = new LinkedHashSet<>();

        // What this class depends on - imports, field types, super class, interfaces
        String sql = "SELECT ci.class_name, ci.super_class, ci.interfaces, " +
                "fi.file_path " +
                "FROM class_info ci " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE ci.class_name LIKE ?";

        List<Object> params = new ArrayList<>();
        params.add("%" + symbolName + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " LIMIT 1";

        String filePath = jdbcTemplate.query(sql, (rs, rowNum) -> {
            return rs.getString("file_path");
        }, params.toArray()).stream().findFirst().orElse(null);

        if (filePath != null) {
            // Find imports for this file
            String importSql = "SELECT ii.import_name FROM import_info ii " +
                    "INNER JOIN file_index fi ON ii.file_index_id = fi.id " +
                    "WHERE fi.file_path = ? ORDER BY ii.import_name";

            List<String> imports = jdbcTemplate.query(importSql, (rs, rowNum) -> rs.getString("import_name"),
                    filePath);

            for (String imp : imports) {
                if (imp != null && !imp.isEmpty()) {
                    RelationshipEntry entry = new RelationshipEntry();
                    entry.setSourceSymbol(symbolName);
                    entry.setSourceSymbolType("CLASS");
                    entry.setSourceFilePath(filePath);
                    entry.setSourcePackage(derivePackageFromPath(filePath));
                    entry.setSourceModule(deriveModuleFromPath(filePath));
                    entry.setTargetSymbol(imp);
                    entry.setTargetSymbolType("IMPORT");
                    entry.setRelationshipType(RelationshipType.DEPENDENCY);
                    entry.setTraversalDepth(1);
                    combined.add(entry);
                }
            }
        }

        return paginate(new ArrayList<>(combined), page, size);
    }

    @Override
    public long countDependencies(String repositoryId, String symbolName) {
        return findDependencies(repositoryId, symbolName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Dependents ====================

    @Override
    public List<RelationshipEntry> findDependents(String repositoryId, String symbolName,
                                                    int page, int size) {
        Set<RelationshipEntry> combined = new LinkedHashSet<>();

        // Classes that import or reference this symbol
        combined.addAll(findReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));

        // Classes whose field type matches
        combined.addAll(findFieldReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));

        // Classes whose method return type matches
        combined.addAll(findMethodReturnReferences(repositoryId, symbolName, 0, Integer.MAX_VALUE));

        return paginate(new ArrayList<>(combined), page, size);
    }

    @Override
    public long countDependents(String repositoryId, String symbolName) {
        return findDependents(repositoryId, symbolName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Callers / Callees ====================

    @Override
    public List<RelationshipEntry> findCallers(String repositoryId, String methodName,
                                                 int page, int size) {
        // Find methods that return or reference the given method name
        // This is based on method return types and method names in the index
        List<Object> params = new ArrayList<>();

        String sql = "SELECT mi.method_name, mi.return_type, ci.class_name, fi.file_path " +
                "FROM method_info mi " +
                "INNER JOIN class_info ci ON mi.class_id = ci.id " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE mi.return_type LIKE ? OR mi.method_name LIKE ?";

        String likePattern = "%" + methodName + "%";
        params.add(likePattern);
        params.add(likePattern);

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY mi.method_name ASC";

        List<RelationshipEntry> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String classMethod = rs.getString("class_name") + "." + rs.getString("method_name");
            entry.setSourceSymbol(classMethod);
            entry.setSourceSymbolType("METHOD");
            entry.setSourceFilePath(rs.getString("file_path"));
            entry.setSourcePackage(derivePackageFromPath(rs.getString("file_path")));
            entry.setSourceModule(deriveModuleFromPath(rs.getString("file_path")));
            entry.setTargetSymbol(methodName);
            entry.setTargetSymbolType("METHOD");
            entry.setRelationshipType(RelationshipType.CALL);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());

        return paginate(results, page, size);
    }

    @Override
    public long countCallers(String repositoryId, String methodName) {
        return findCallers(repositoryId, methodName, 0, Integer.MAX_VALUE).size();
    }

    @Override
    public List<RelationshipEntry> findCallees(String repositoryId, String methodName,
                                                 int page, int size) {
        // Find what methods/fields this method uses - based on return types
        List<Object> params = new ArrayList<>();

        String sql = "SELECT mi.method_name, mi.return_type, mi.parameters, ci.class_name, fi.file_path " +
                "FROM method_info mi " +
                "INNER JOIN class_info ci ON mi.class_id = ci.id " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE mi.method_name LIKE ?";

        params.add("%" + methodName + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY mi.method_name ASC LIMIT 1";

        List<RelationshipEntry> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String returnType = rs.getString("return_type");
            entry.setSourceSymbol(methodName);
            entry.setSourceSymbolType("METHOD");
            entry.setSourceFilePath(rs.getString("file_path"));
            entry.setSourcePackage(derivePackageFromPath(rs.getString("file_path")));
            entry.setSourceModule(deriveModuleFromPath(rs.getString("file_path")));
            entry.setTargetSymbol(returnType != null ? returnType : "void");
            entry.setTargetSymbolType("TYPE");
            entry.setRelationshipType(RelationshipType.CALL);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());

        return paginate(results, page, size);
    }

    @Override
    public long countCallees(String repositoryId, String methodName) {
        return findCallees(repositoryId, methodName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Package / Module Relationships ====================

    @Override
    public List<RelationshipEntry> findPackageRelationships(String repositoryId, String packageName,
                                                              int page, int size) {
        Set<RelationshipEntry> results = new LinkedHashSet<>();

        // Find all import relationships grouped by package
        String sql = "SELECT ii.import_name, fi.file_path " +
                "FROM import_info ii " +
                "INNER JOIN file_index fi ON ii.file_index_id = fi.id " +
                "WHERE 1=1";

        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        if (packageName != null && !packageName.isEmpty()) {
            sql += " AND (fi.file_path LIKE ? OR ii.import_name LIKE ?)";
            params.add("%" + packageName + "%");
            params.add(packageName + "%");
        }

        sql += " ORDER BY fi.file_path, ii.import_name LIMIT 1000";

        List<RelationshipEntry> entries = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String filePath = rs.getString("file_path");
            String importName = rs.getString("import_name");
            String srcPkg = derivePackageFromPath(filePath);
            String targetPkg = importName != null && importName.contains(".") ?
                    importName.substring(0, importName.lastIndexOf('.')) : "";

            entry.setSourceSymbol(srcPkg);
            entry.setSourceSymbolType("PACKAGE");
            entry.setSourceFilePath(filePath);
            entry.setSourcePackage(srcPkg);
            entry.setSourceModule(deriveModuleFromPath(filePath));
            entry.setTargetSymbol(targetPkg);
            entry.setTargetSymbolType("PACKAGE");
            entry.setTargetPackage(targetPkg);
            entry.setRelationshipType(RelationshipType.PACKAGE);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());

        results.addAll(entries);
        return paginate(new ArrayList<>(results), page, size);
    }

    @Override
    public long countPackageRelationships(String repositoryId, String packageName) {
        return findPackageRelationships(repositoryId, packageName, 0, Integer.MAX_VALUE).size();
    }

    @Override
    public List<RelationshipEntry> findModuleRelationships(String repositoryId, String moduleName,
                                                             int page, int size) {
        Set<RelationshipEntry> results = new LinkedHashSet<>();

        // Find class-to-class relationships organized by module
        String sql = "SELECT ci1.class_name as source_class, ci2.class_name as target_class, " +
                "fi1.file_path as source_path, fi2.file_path as target_path, 'REFERENCE' as rel_type " +
                "FROM import_info ii " +
                "INNER JOIN file_index fi1 ON ii.file_index_id = fi1.id " +
                "INNER JOIN class_info ci1 ON fi1.id = ci1.file_index_id " +
                "INNER JOIN file_index fi2 ON ii.import_name LIKE '%' || fi2.file_name " +
                "INNER JOIN class_info ci2 ON fi2.id = ci2.file_index_id " +
                "WHERE 1=1";

        List<Object> params = new ArrayList<>();

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi1.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        if (moduleName != null && !moduleName.isEmpty()) {
            sql += " AND (fi1.file_path LIKE ? OR fi2.file_path LIKE ?)";
            params.add("%" + moduleName + "%");
            params.add("%" + moduleName + "%");
        }

        sql += " GROUP BY ci1.class_name, ci2.class_name ORDER BY ci1.class_name LIMIT 500";

        List<RelationshipEntry> entries = jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String sourcePath = rs.getString("source_path");
            String targetPath = rs.getString("target_path");

            entry.setSourceSymbol(rs.getString("source_class"));
            entry.setSourceSymbolType("CLASS");
            entry.setSourceFilePath(sourcePath);
            entry.setSourcePackage(derivePackageFromPath(sourcePath));
            entry.setSourceModule(deriveModuleFromPath(sourcePath));
            entry.setTargetSymbol(rs.getString("target_class"));
            entry.setTargetSymbolType("CLASS");
            entry.setTargetFilePath(targetPath);
            entry.setTargetPackage(derivePackageFromPath(targetPath));
            entry.setTargetModule(deriveModuleFromPath(targetPath));
            entry.setRelationshipType(RelationshipType.MODULE);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());

        results.addAll(entries);
        return paginate(new ArrayList<>(results), page, size);
    }

    @Override
    public long countModuleRelationships(String repositoryId, String moduleName) {
        return findModuleRelationships(repositoryId, moduleName, 0, Integer.MAX_VALUE).size();
    }

    // ==================== Helper Methods ====================

    private List<RelationshipEntry> findFieldReferences(String repositoryId, String symbolName,
                                                          int page, int size) {
        List<Object> params = new ArrayList<>();

        String sql = "SELECT fi.field_name, fi.field_type, ci.class_name, fxi.file_path " +
                "FROM field_info fi " +
                "INNER JOIN class_info ci ON fi.class_id = ci.id " +
                "INNER JOIN file_index fxi ON ci.file_index_id = fxi.id " +
                "WHERE fi.field_type LIKE ?";

        params.add("%" + symbolName + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fxi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY fi.field_name ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String filePath = rs.getString("file_path");
            String className = rs.getString("class_name");

            entry.setSourceSymbol(className + "." + rs.getString("field_name"));
            entry.setSourceSymbolType("FIELD");
            entry.setSourceFilePath(filePath);
            entry.setSourcePackage(derivePackageFromPath(filePath));
            entry.setSourceModule(deriveModuleFromPath(filePath));
            entry.setTargetSymbol(symbolName);
            entry.setTargetSymbolType("REFERENCED");
            entry.setRelationshipType(RelationshipType.REFERENCE);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());
    }

    private List<RelationshipEntry> findMethodReturnReferences(String repositoryId, String symbolName,
                                                                 int page, int size) {
        List<Object> params = new ArrayList<>();

        String sql = "SELECT mi.method_name, mi.return_type, ci.class_name, fi.file_path " +
                "FROM method_info mi " +
                "INNER JOIN class_info ci ON mi.class_id = ci.id " +
                "INNER JOIN file_index fi ON ci.file_index_id = fi.id " +
                "WHERE mi.return_type LIKE ?";

        params.add("%" + symbolName + "%");

        if (repositoryId != null && !repositoryId.isEmpty()) {
            sql += " AND fi.file_path LIKE ?";
            params.add("%" + repositoryId + "%");
        }

        sql += " ORDER BY mi.method_name ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RelationshipEntry entry = new RelationshipEntry();
            String filePath = rs.getString("file_path");
            String className = rs.getString("class_name");

            entry.setSourceSymbol(className + "." + rs.getString("method_name"));
            entry.setSourceSymbolType("METHOD");
            entry.setSourceFilePath(filePath);
            entry.setSourcePackage(derivePackageFromPath(filePath));
            entry.setSourceModule(deriveModuleFromPath(filePath));
            entry.setTargetSymbol(symbolName);
            entry.setTargetSymbolType("REFERENCED");
            entry.setRelationshipType(RelationshipType.REFERENCE);
            entry.setTraversalDepth(1);
            return entry;
        }, params.toArray());
    }

    private <T> List<T> paginate(List<T> list, int page, int size) {
        if (size <= 0) {
            return List.of();
        }
        int fromIndex = page * size;
        if (fromIndex >= list.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, list.size());
        return list.subList(fromIndex, toIndex);
    }

    private String derivePackageFromPath(String filePath) {
        if (filePath == null) return "";
        String javaSrc = "/src/main/java/";
        int idx = filePath.indexOf(javaSrc);
        if (idx >= 0) {
            String afterSrc = filePath.substring(idx + javaSrc.length());
            int lastSlash = afterSrc.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterSrc.substring(0, lastSlash).replace('/', '.');
            }
        }
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

    private String deriveModuleFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        if (filePath.contains("/src/")) {
            int srcIdx = filePath.indexOf("/src/");
            String beforeSrc = filePath.substring(0, srcIdx);
            int lastSlash = beforeSrc.lastIndexOf('/');
            if (lastSlash >= 0) {
                return beforeSrc.substring(lastSlash + 1);
            }
            return beforeSrc;
        }
        String normalized = filePath.replace('\\', '/');
        int firstSlash = normalized.indexOf('/');
        if (firstSlash > 0) {
            return normalized.substring(0, firstSlash);
        }
        return "";
    }
}