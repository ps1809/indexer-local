package com.projectiq.indexerlocal.extractor;

import com.projectiq.indexerlocal.model.*;

import java.util.*;

/**
 * Deterministic Java source code parser using character-by-character scanning.
 * No regular expressions are used anywhere in this class.
 * All parsing is O(n) linear time with respect to source length.
 */
public class DeterministicJavaParser {

    private static final Set<String> MODIFIER_KEYWORDS = Set.of(
        "public", "private", "protected", "static", "final", "abstract",
        "synchronized", "native", "strictfp", "default", "transient", "volatile",
        "sealed", "non-sealed"
    );

    private static final Set<String> CONTROL_FLOW_KEYWORDS = Set.of(
        "if", "for", "while", "switch", "catch", "try", "synchronized", "do",
        "return", "throw", "else", "case", "break", "continue", "finally",
        "new", "assert"
    );

    private static final Set<String> TYPE_KEYWORDS = Set.of(
        "void", "boolean", "byte", "char", "short", "int", "long", "float", "double"
    );

    private static final Set<String> FIELD_MODIFIERS = Set.of(
        "public", "private", "protected", "static", "final", "volatile", "transient"
    );

    private static final Set<String> CLASS_TYPE_KEYWORDS = Set.of(
        "class", "interface", "enum", "record"
    );

    private final String src;
    private int pos;
    private final int len;

    public DeterministicJavaParser(String source) {
        this.src = source != null ? source : "";
        this.pos = 0;
        this.len = this.src.length();
    }

    // ========================================================================
    // Low-level scanning utilities
    // ========================================================================

    private char peek() {
        return pos < len ? src.charAt(pos) : '\0';
    }

    private char peekAt(int offset) {
        int idx = pos + offset;
        return idx < len ? src.charAt(idx) : '\0';
    }

    private char advance() {
        return pos < len ? src.charAt(pos++) : '\0';
    }

    private boolean isAtEnd() {
        return pos >= len;
    }

    private void skipWhitespace() {
        while (pos < len && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private void skipLineComment() {
        while (pos < len && src.charAt(pos) != '\n') {
            pos++;
        }
        if (pos < len) pos++; // skip the newline
    }

    private void skipBlockComment() {
        // Already past /*
        while (pos < len - 1) {
            if (src.charAt(pos) == '*' && src.charAt(pos + 1) == '/') {
                pos += 2;
                return;
            }
            pos++;
        }
        pos = len; // unterminated block comment
    }

    private void skipWhitespaceAndComments() {
        while (pos < len) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                pos += 2;
                skipBlockComment();
            } else {
                break;
            }
        }
    }

    private void skipStringLiteral() {
        // pos is at opening "
        pos++; // skip opening "
        // Check for text block """
        if (pos + 1 < len && src.charAt(pos) == '"' && src.charAt(pos + 1) == '"') {
            pos += 2; // skip the other two "
            // Scan for closing """
            while (pos < len - 2) {
                if (src.charAt(pos) == '"' && src.charAt(pos + 1) == '"' && src.charAt(pos + 2) == '"') {
                    pos += 3;
                    return;
                }
                pos++;
            }
            pos = len;
            return;
        }
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == '\\') {
                pos += 2; // skip escape sequence
            } else if (c == '"') {
                pos++;
                return;
            } else if (c == '\n') {
                return; // unterminated string
            } else {
                pos++;
            }
        }
    }

    private void skipCharLiteral() {
        // pos is at opening '
        pos++; // skip opening '
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == '\\') {
                pos += 2;
            } else if (c == '\'') {
                pos++;
                return;
            } else if (c == '\n') {
                return;
            } else {
                pos++;
            }
        }
    }

    private boolean isIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c);
    }

    private boolean isIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private String readIdentifier() {
        skipWhitespaceAndComments();
        if (isAtEnd() || !isIdentifierStart(peek())) {
            return null;
        }
        int start = pos;
        while (pos < len && isIdentifierPart(src.charAt(pos))) {
            pos++;
        }
        return src.substring(start, pos);
    }

    private boolean isAtKeyword(String keyword) {
        if (pos + keyword.length() > len) return false;
        for (int i = 0; i < keyword.length(); i++) {
            if (src.charAt(pos + i) != keyword.charAt(i)) return false;
        }
        // Make sure it's not part of a longer identifier
        int afterKeyword = pos + keyword.length();
        if (afterKeyword < len && isIdentifierPart(src.charAt(afterKeyword))) {
            return false;
        }
        return true;
    }

    private boolean tryConsumeKeyword(String keyword) {
        skipWhitespaceAndComments();
        if (isAtKeyword(keyword)) {
            pos += keyword.length();
            return true;
        }
        return false;
    }

    private boolean tryConsume(char c) {
        skipWhitespaceAndComments();
        if (peek() == c) {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Read a Java type including generics, arrays, and varargs.
     * Returns null if no type can be read.
     */
    private String readType() {
        skipWhitespaceAndComments();
        if (isAtEnd()) return null;

        // Handle wildcard '?'
        if (peek() == '?') {
            pos++;
            StringBuilder sb = new StringBuilder("?");
            skipWhitespaceAndComments();
            if (isAtKeyword("extends")) {
                pos += "extends".length();
                skipWhitespaceAndComments();
                String bound = readType();
                if (bound != null) sb.append(" extends ").append(bound);
            } else if (isAtKeyword("super")) {
                pos += "super".length();
                skipWhitespaceAndComments();
                String bound = readType();
                if (bound != null) sb.append(" super ").append(bound);
            }
            return sb.toString();
        }

        // Read base identifier or primitive
        String base = readIdentifier();
        if (base == null) return null;

        StringBuilder typeBuilder = new StringBuilder(base);

        // Handle qualified names (e.g., java.util.List)
        while (peek() == '.') {
            int savedPos = pos;
            pos++; // skip '.'
            skipWhitespaceAndComments();
            String next = readIdentifier();
            if (next != null) {
                typeBuilder.append('.').append(next);
            } else {
                pos = savedPos;
                break;
            }
        }

        // Handle generic type parameters <...>
        skipWhitespaceAndComments();
        if (peek() == '<') {
            String genericPart = readBalanced('<', '>');
            if (genericPart != null) {
                typeBuilder.append(genericPart);
            }
        }

        // Handle array brackets []
        skipWhitespaceAndComments();
        while (peek() == '[') {
            int savedPos = pos;
            pos++;
            skipWhitespaceAndComments();
            if (peek() == ']') {
                pos++;
                typeBuilder.append("[]");
            } else {
                pos = savedPos;
                break;
            }
            skipWhitespaceAndComments();
        }

        // Handle varargs ...
        if (pos + 2 < len && src.charAt(pos) == '.' && src.charAt(pos + 1) == '.' && src.charAt(pos + 2) == '.') {
            pos += 3;
            typeBuilder.append("...");
        }

        return typeBuilder.toString();
    }

    /**
     * Read a balanced pair of delimiters including everything between them.
     * Handles nested pairs, strings, comments.
     */
    private String readBalanced(char open, char close) {
        skipWhitespaceAndComments();
        if (peek() != open) return null;

        int start = pos;
        pos++; // skip opening delimiter
        int depth = 1;

        while (pos < len && depth > 0) {
            char c = src.charAt(pos);
            if (c == open) {
                depth++;
                pos++;
            } else if (c == close) {
                depth--;
                pos++;
            } else if (c == '"') {
                skipStringLiteral();
            } else if (c == '\'') {
                skipCharLiteral();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                pos += 2;
                skipBlockComment();
            } else {
                pos++;
            }
        }

        return src.substring(start, pos);
    }

    /**
     * Skip to the next semicolon or opening brace at the current brace depth.
     * Used to skip unrecognized declarations.
     */
    private void skipToNextStatement() {
        int depth = 0;
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == '{') {
                depth++;
                pos++;
            } else if (c == '}') {
                if (depth == 0) {
                    pos++;
                    return;
                }
                depth--;
                pos++;
            } else if (c == ';' && depth == 0) {
                pos++;
                return;
            } else if (c == '"') {
                skipStringLiteral();
            } else if (c == '\'') {
                skipCharLiteral();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                pos += 2;
                skipBlockComment();
            } else {
                pos++;
            }
        }
    }

    /**
     * Skip a brace-delimited body, handling strings, comments, and nested braces.
     */
    private void skipBody() {
        skipWhitespaceAndComments();
        if (peek() != '{') return;
        pos++; // skip opening {
        int depth = 1;
        while (pos < len && depth > 0) {
            char c = src.charAt(pos);
            if (c == '{') {
                depth++;
                pos++;
            } else if (c == '}') {
                depth--;
                pos++;
            } else if (c == '"') {
                skipStringLiteral();
            } else if (c == '\'') {
                skipCharLiteral();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                pos += 2;
                skipBlockComment();
            } else {
                pos++;
            }
        }
    }

    /**
     * Extract a brace-delimited body, returning its content.
     */
    private String extractBody() {
        skipWhitespaceAndComments();
        if (peek() != '{') return "";
        pos++; // skip opening {
        int start = pos;
        int depth = 1;
        while (pos < len && depth > 0) {
            char c = src.charAt(pos);
            if (c == '{') {
                depth++;
                pos++;
            } else if (c == '}') {
                depth--;
                pos++;
            } else if (c == '"') {
                skipStringLiteral();
            } else if (c == '\'') {
                skipCharLiteral();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '/') {
                pos += 2;
                skipLineComment();
            } else if (c == '/' && pos + 1 < len && src.charAt(pos + 1) == '*') {
                pos += 2;
                skipBlockComment();
            } else {
                pos++;
            }
        }
        return src.substring(start, pos - 1);
    }

    // ========================================================================
    // Annotation parsing
    // ========================================================================

    private AnnotationInfo readAnnotation() {
        skipWhitespaceAndComments();
        if (peek() != '@') return null;
        pos++; // skip @

        AnnotationInfo info = new AnnotationInfo();
        String name = readIdentifier();
        if (name == null) return null;

        // Handle qualified annotation names
        while (peek() == '.') {
            pos++;
            String next = readIdentifier();
            if (next != null) {
                name = name + "." + next;
            } else {
                break;
            }
        }

        info.setAnnotationName(name);
        info.setFullName(name);

        skipWhitespaceAndComments();
        // Check for annotation arguments
        if (peek() == '(') {
            String args = readBalanced('(', ')');
            if (args != null) {
                // Strip outer parens
                String inner = args.substring(1, args.length() - 1).trim();
                info.setArguments(inner);
                info.setFullName(name + args);
                // Count arguments
                if (!inner.isEmpty()) {
                    int argCount = 1;
                    int depth = 0;
                    for (int i = 0; i < inner.length(); i++) {
                        char c = inner.charAt(i);
                        if (c == '(' || c == '<' || c == '{' || c == '[') depth++;
                        else if (c == ')' || c == '>' || c == '}' || c == ']') depth--;
                        else if (c == ',' && depth == 0) argCount++;
                    }
                    info.setArgumentCount(argCount);
                }
            }
        }

        return info;
    }

    private List<AnnotationInfo> readAnnotations() {
        List<AnnotationInfo> annotations = new ArrayList<>();
        while (true) {
            skipWhitespaceAndComments();
            if (peek() != '@') break;
            // Check if this is a type annotation or a real annotation
            // vs. an @ in some other context
            int savedPos = pos;
            AnnotationInfo ann = readAnnotation();
            if (ann != null) {
                annotations.add(ann);
            } else {
                pos = savedPos;
                break;
            }
        }
        return annotations;
    }

    // ========================================================================
    // Package extraction
    // ========================================================================

    public static String extractPackage(String source) {
        DeterministicJavaParser parser = new DeterministicJavaParser(source);
        parser.skipWhitespaceAndComments();

        // Scan for "package" keyword
        while (!parser.isAtEnd()) {
            parser.skipWhitespaceAndComments();
            if (parser.isAtKeyword("package")) {
                parser.pos += "package".length();
                parser.skipWhitespaceAndComments();
                // Read qualified package name
                StringBuilder pkg = new StringBuilder();
                String ident = parser.readIdentifier();
                if (ident == null) return "";
                pkg.append(ident);
                while (parser.peek() == '.') {
                    parser.pos++;
                    String next = parser.readIdentifier();
                    if (next != null) {
                        pkg.append('.').append(next);
                    } else {
                        break;
                    }
                }
                return pkg.toString();
            }
            // Skip to next line or statement
            if (parser.peek() == ';') {
                parser.pos++;
            } else if (parser.isAtKeyword("import") || CLASS_TYPE_KEYWORDS.stream().anyMatch(kw -> parser.isAtKeyword(kw))) {
                break; // past the package declaration area
            } else {
                parser.pos++;
            }
        }
        return "";
    }

    // ========================================================================
    // Import extraction
    // ========================================================================

    public static List<ImportInfo> extractImports(String source) {
        List<ImportInfo> imports = new ArrayList<>();
        DeterministicJavaParser parser = new DeterministicJavaParser(source);

        while (!parser.isAtEnd()) {
            parser.skipWhitespaceAndComments();
            if (parser.isAtEnd()) break;

            if (parser.isAtKeyword("import")) {
                parser.pos += "import".length();
                parser.skipWhitespaceAndComments();

                boolean isStatic = false;
                if (parser.isAtKeyword("static")) {
                    isStatic = true;
                    parser.pos += "static".length();
                    parser.skipWhitespaceAndComments();
                }

                // Read qualified import name
                StringBuilder imp = new StringBuilder();
                String ident = parser.readIdentifier();
                if (ident == null) continue;
                imp.append(ident);

                boolean isWildcard = false;
                while (parser.peek() == '.') {
                    parser.pos++;
                    parser.skipWhitespaceAndComments();
                    if (parser.peek() == '*') {
                        parser.pos++;
                        isWildcard = true;
                        break;
                    }
                    String next = parser.readIdentifier();
                    if (next != null) {
                        imp.append('.').append(next);
                    } else {
                        break;
                    }
                }

                // Skip to semicolon
                while (!parser.isAtEnd() && parser.peek() != ';') {
                    parser.pos++;
                }
                if (!parser.isAtEnd()) parser.pos++; // skip ;

                ImportInfo info = new ImportInfo();
                info.setImportStatement(imp.toString());
                info.setType(isStatic ? "STATIC" : (isWildcard ? "WILDCARD" : "NORMAL"));
                info.setIsWildcard(isWildcard);
                imports.add(info);
            } else {
                // Skip non-import content
                if (CLASS_TYPE_KEYWORDS.stream().anyMatch(kw -> parser.isAtKeyword(kw))) {
                    break; // reached class declaration, stop looking for imports
                }
                parser.pos++;
            }
        }

        return imports;
    }

    // ========================================================================
    // Class extraction
    // ========================================================================

    public static List<ClassInfo> extractClasses(String source, String filePath, String fileName) {
        List<ClassInfo> classes = new ArrayList<>();
        DeterministicJavaParser parser = new DeterministicJavaParser(source);

        // Skip package and imports first
        parser.skipWhitespaceAndComments();
        // Skip past package declaration
        if (parser.isAtKeyword("package")) {
            while (!parser.isAtEnd() && parser.peek() != ';') parser.pos++;
            if (!parser.isAtEnd()) parser.pos++;
        }
        // Skip past import declarations
        while (!parser.isAtEnd()) {
            parser.skipWhitespaceAndComments();
            if (parser.isAtKeyword("import")) {
                while (!parser.isAtEnd() && parser.peek() != ';') parser.pos++;
                if (!parser.isAtEnd()) parser.pos++;
            } else {
                break;
            }
        }

        // Now scan for type declarations
        while (!parser.isAtEnd()) {
            parser.skipWhitespaceAndComments();
            if (parser.isAtEnd()) break;

            // Collect annotations before the type declaration
            List<AnnotationInfo> annotations = parser.readAnnotations();

            // Collect modifiers
            List<String> modifiers = new ArrayList<>();
            while (true) {
                parser.skipWhitespaceAndComments();
                boolean foundModifier = false;
                for (String mod : MODIFIER_KEYWORDS) {
                    if (parser.isAtKeyword(mod)) {
                        modifiers.add(mod);
                        parser.pos += mod.length();
                        foundModifier = true;
                        break;
                    }
                }
                if (!foundModifier) break;
            }

            parser.skipWhitespaceAndComments();

            // Check for type keyword
            String classType = null;
            for (String kw : CLASS_TYPE_KEYWORDS) {
                if (parser.isAtKeyword(kw)) {
                    classType = kw.toUpperCase();
                    parser.pos += kw.length();
                    break;
                }
            }

            if (classType == null) {
                // Not a type declaration, skip
                if (!parser.isAtEnd()) {
                    parser.skipToNextStatement();
                }
                continue;
            }

            parser.skipWhitespaceAndComments();

            // Read class name
            String className = parser.readIdentifier();
            if (className == null) {
                parser.skipToNextStatement();
                continue;
            }

            ClassInfo classInfo = new ClassInfo();
            classInfo.setFileName(fileName);
            classInfo.setFilePath(filePath);
            classInfo.setClassName(className);
            classInfo.setClassType(classType);

            // Set visibility
            classInfo.setVisibility(determineVisibility(modifiers));
            classInfo.setAbstract(modifiers.contains("abstract"));
            classInfo.setFinal(modifiers.contains("final"));
            classInfo.setAnnotations(annotations.isEmpty() ? new ArrayList<>() : annotations);

            parser.skipWhitespaceAndComments();

            // Handle record parameters - skip them
            if ("RECORD".equals(classType) && parser.peek() == '(') {
                parser.readBalanced('(', ')');
                parser.skipWhitespaceAndComments();
            }

            // Handle generic type parameters on the class
            if (parser.peek() == '<') {
                String typeParams = parser.readBalanced('<', '>');
                classInfo.setGenericTypeParameters(typeParams);
                parser.skipWhitespaceAndComments();
            }

            // Handle extends clause
            if (parser.isAtKeyword("extends")) {
                parser.pos += "extends".length();
                parser.skipWhitespaceAndComments();
                String superType = parser.readType();
                if (superType != null) {
                    // Extract just the class name without generics for superClass
                    int angleIdx = superType.indexOf('<');
                    classInfo.setSuperClass(angleIdx >= 0 ? superType.substring(0, angleIdx) : superType);
                }
                parser.skipWhitespaceAndComments();
            }

            // Handle implements clause
            if (parser.isAtKeyword("implements")) {
                parser.pos += "implements".length();
                List<String> interfaces = new ArrayList<>();
                while (true) {
                    parser.skipWhitespaceAndComments();
                    String iface = parser.readType();
                    if (iface == null) break;
                    interfaces.add(iface);
                    parser.skipWhitespaceAndComments();
                    if (parser.peek() == ',') {
                        parser.pos++;
                    } else {
                        break;
                    }
                }
                classInfo.setInterfaces(interfaces);
                parser.skipWhitespaceAndComments();
            }

            // Handle permits clause (sealed classes)
            if (parser.isAtKeyword("permits")) {
                parser.pos += "permits".length();
                while (!parser.isAtEnd() && parser.peek() != '{') {
                    parser.pos++;
                }
            }

            // Extract class body
            String classBody = parser.extractBody();

            // Parse fields and methods from the body
            List<FieldInfo> fields = extractFields(classBody);
            List<MethodInfo> allMethods = extractMethods(classBody, className);

            // Separate constructors from methods
            List<MethodInfo> methods = new ArrayList<>();
            List<MethodInfo> constructors = new ArrayList<>();
            for (MethodInfo m : allMethods) {
                if ("CONSTRUCTOR".equals(m.getMethodType())) {
                    constructors.add(m);
                } else {
                    methods.add(m);
                }
            }

            classInfo.setFields(fields);
            classInfo.setMethods(methods);
            classInfo.setConstructors(constructors.isEmpty() ? null : constructors);

            classes.add(classInfo);
        }

        return classes;
    }

    // ========================================================================
    // Field extraction
    // ========================================================================

    public static List<FieldInfo> extractFields(String classBody) {
        List<FieldInfo> fields = new ArrayList<>();
        if (classBody == null || classBody.isEmpty()) return fields;

        // Use line-based scanning with brace depth tracking
        int braceDepth = 0;
        List<AnnotationInfo> pendingAnnotations = new ArrayList<>();

        int lineStart = 0;
        int bodyLen = classBody.length();

        while (lineStart < bodyLen) {
            // Find end of line
            int lineEnd = lineStart;
            while (lineEnd < bodyLen && classBody.charAt(lineEnd) != '\n') {
                lineEnd++;
            }

            String rawLine = classBody.substring(lineStart, lineEnd);
            String line = rawLine.trim();
            lineStart = lineEnd + 1;

            if (line.isEmpty()) continue;

            // Track brace depth - count braces outside strings and comments
            int prevBraceDepth = braceDepth;
            braceDepth += countBraceDepthChange(line);

            // Collect annotations
            if (line.startsWith("@")) {
                // Parse annotation from line
                DeterministicJavaParser annParser = new DeterministicJavaParser(line);
                AnnotationInfo ann = annParser.readAnnotation();
                if (ann != null) {
                    pendingAnnotations.add(ann);
                }
                continue;
            }

            // Only consider lines at class level (braceDepth was 0 before this line)
            if (prevBraceDepth > 0) continue;

            // Only consider lines ending with ';'
            if (!line.endsWith(";")) continue;

            // Skip lines that look like method declarations
            boolean looksLikeMethod = false;
            for (int i = 1; i < line.length(); i++) {
                if (line.charAt(i) == '(' && Character.isJavaIdentifierStart(line.charAt(i - 1))) {
                    // Check it's not inside a string or generic
                    if (!isInsideStringOrGeneric(line, i)) {
                        looksLikeMethod = true;
                        break;
                    }
                }
            }
            if (looksLikeMethod) continue;

            // Skip control flow keywords
            String firstToken = firstWord(line);
            if (CONTROL_FLOW_KEYWORDS.contains(firstToken)) continue;

            // Remove trailing semicolon
            String decl = line.substring(0, line.length() - 1).trim();

            // Remove initializer (everything after top-level '=')
            String defaultValue = null;
            int eqIndex = findTopLevelEquals(decl);
            if (eqIndex >= 0) {
                defaultValue = decl.substring(eqIndex + 1).trim();
                decl = decl.substring(0, eqIndex).trim();
            }

            // Tokenize deterministically
            List<String> tokens = splitByWhitespace(decl);
            if (tokens.size() < 2) continue;

            // Consume leading modifiers
            List<String> modifierList = new ArrayList<>();
            int typeStartIndex = 0;
            for (int i = 0; i < tokens.size(); i++) {
                if (FIELD_MODIFIERS.contains(tokens.get(i))) {
                    modifierList.add(tokens.get(i));
                    typeStartIndex = i + 1;
                } else {
                    break;
                }
            }

            int remainingTokens = tokens.size() - typeStartIndex;
            if (remainingTokens < 2) continue;

            // Last token is the field name
            String fieldName = tokens.get(tokens.size() - 1);

            // Skip if it looks like a method signature
            if ("void".equalsIgnoreCase(fieldName)) continue;

            // Everything between modifiers and field name is the type
            StringBuilder typeBuilder = new StringBuilder();
            for (int i = typeStartIndex; i < tokens.size() - 1; i++) {
                if (i > typeStartIndex) typeBuilder.append(' ');
                typeBuilder.append(tokens.get(i));
            }
            String fieldType = typeBuilder.toString().trim();

            // Skip void type
            if ("void".equals(fieldType)) continue;

            // Determine visibility
            String visibility = determineVisibility(modifierList);

            FieldInfo field = new FieldInfo();
            field.setFieldName(fieldName);
            field.setFieldType(fieldType);
            field.setVisibility(visibility);
            field.setStatic(modifierList.contains("static"));
            field.setFinal(modifierList.contains("final"));
            if (defaultValue != null) {
                field.setDefaultValue(defaultValue);
            }
            if (!pendingAnnotations.isEmpty()) {
                field.setAnnotations(new ArrayList<>(pendingAnnotations));
            }

            fields.add(field);
            pendingAnnotations.clear();
        }

        return fields;
    }

    // ========================================================================
    // Method extraction
    // ========================================================================

    public static List<MethodInfo> extractMethods(String classBody, String className) {
        List<MethodInfo> methods = new ArrayList<>();
        if (classBody == null || classBody.isEmpty()) return methods;

        DeterministicJavaParser parser = new DeterministicJavaParser(classBody);
        List<AnnotationInfo> pendingAnnotations = new ArrayList<>();

        while (!parser.isAtEnd()) {
            parser.skipWhitespaceAndComments();
            if (parser.isAtEnd()) break;

            // Check for closing brace (end of class body - shouldn't happen since we extracted body)
            if (parser.peek() == '}') {
                parser.pos++;
                continue;
            }

            // Collect annotations
            List<AnnotationInfo> annotations = parser.readAnnotations();
            if (!annotations.isEmpty()) {
                pendingAnnotations.addAll(annotations);
            }

            parser.skipWhitespaceAndComments();
            if (parser.isAtEnd()) break;

            // Check for nested type declarations (inner class/interface/enum/record)
            boolean isNestedType = false;
            int savedPos = parser.pos;
            // Skip modifiers to check for type keyword
            List<String> preModifiers = new ArrayList<>();
            while (true) {
                parser.skipWhitespaceAndComments();
                boolean foundMod = false;
                for (String mod : MODIFIER_KEYWORDS) {
                    if (parser.isAtKeyword(mod)) {
                        preModifiers.add(mod);
                        parser.pos += mod.length();
                        foundMod = true;
                        break;
                    }
                }
                if (!foundMod) break;
            }
            parser.skipWhitespaceAndComments();
            for (String kw : CLASS_TYPE_KEYWORDS) {
                if (parser.isAtKeyword(kw)) {
                    isNestedType = true;
                    break;
                }
            }
            parser.pos = savedPos;

            if (isNestedType) {
                // Skip the entire nested type declaration
                // Skip modifiers
                while (true) {
                    parser.skipWhitespaceAndComments();
                    boolean foundMod = false;
                    for (String mod : MODIFIER_KEYWORDS) {
                        if (parser.isAtKeyword(mod)) {
                            parser.pos += mod.length();
                            foundMod = true;
                            break;
                        }
                    }
                    if (!foundMod) break;
                }
                parser.skipWhitespaceAndComments();
                // Skip type keyword
                for (String kw : CLASS_TYPE_KEYWORDS) {
                    if (parser.isAtKeyword(kw)) {
                        parser.pos += kw.length();
                        break;
                    }
                }
                // Skip to opening brace and skip body
                while (!parser.isAtEnd() && parser.peek() != '{') {
                    parser.pos++;
                }
                parser.skipBody();
                pendingAnnotations.clear();
                continue;
            }

            // Collect modifiers
            List<String> modifiers = new ArrayList<>();
            while (true) {
                parser.skipWhitespaceAndComments();
                boolean foundModifier = false;
                for (String mod : MODIFIER_KEYWORDS) {
                    if (parser.isAtKeyword(mod)) {
                        modifiers.add(mod);
                        parser.pos += mod.length();
                        foundModifier = true;
                        break;
                    }
                }
                if (!foundModifier) break;
            }

            parser.skipWhitespaceAndComments();
            if (parser.isAtEnd()) break;

            // Check for static initializer block: static { ... }
            if (modifiers.size() == 1 && modifiers.contains("static") && parser.peek() == '{') {
                parser.skipBody();
                pendingAnnotations.clear();
                continue;
            }

            // Check for instance initializer block: { ... }
            if (modifiers.isEmpty() && parser.peek() == '{') {
                parser.skipBody();
                pendingAnnotations.clear();
                continue;
            }

            // Try to read a type
            int typeStartPos = parser.pos;
            String type = parser.readType();

            if (type == null) {
                // Can't read a type, skip to next statement
                parser.skipToNextStatement();
                pendingAnnotations.clear();
                continue;
            }

            parser.skipWhitespaceAndComments();

            // Check if this is just a type followed by ';' (e.g., enum constant or something weird)
            if (parser.peek() == ';') {
                parser.pos++;
                pendingAnnotations.clear();
                continue;
            }

            // Check if this is a constructor (type is the class name and next char is '(')
            boolean isConstructor = false;
            String methodName;

            if (type.equals(className) && parser.peek() == '(') {
                // This is a constructor
                isConstructor = true;
                methodName = className;
            } else {
                // Try to read method name
                String name = parser.readIdentifier();
                if (name == null) {
                    // Not a method declaration
                    parser.skipToNextStatement();
                    pendingAnnotations.clear();
                    continue;
                }

                // Check if it's a control flow keyword
                if (CONTROL_FLOW_KEYWORDS.contains(name)) {
                    parser.skipToNextStatement();
                    pendingAnnotations.clear();
                    continue;
                }

                parser.skipWhitespaceAndComments();

                // Check for '(' - if not present, this might be a field declaration
                if (parser.peek() != '(') {
                    // This is likely a field declaration, not a method
                    // Skip to semicolon
                    while (!parser.isAtEnd() && parser.peek() != ';' && parser.peek() != '{') {
                        parser.pos++;
                    }
                    if (parser.peek() == ';') parser.pos++;
                    else if (parser.peek() == '{') parser.skipBody();
                    pendingAnnotations.clear();
                    continue;
                }

                methodName = name;
            }

            // Read parameter list
            String paramStr = parser.readBalanced('(', ')');
            if (paramStr == null) {
                parser.skipToNextStatement();
                pendingAnnotations.clear();
                continue;
            }

            // Parse parameters
            List<String> paramList = parseParameterList(paramStr.substring(1, paramStr.length() - 1).trim());

            parser.skipWhitespaceAndComments();

            // Read optional throws clause
            List<String> exceptions = new ArrayList<>();
            if (parser.isAtKeyword("throws")) {
                parser.pos += "throws".length();
                parser.skipWhitespaceAndComments();
                // Read exception types
                while (true) {
                    String exType = parser.readType();
                    if (exType == null) break;
                    exceptions.add(exType);
                    parser.skipWhitespaceAndComments();
                    if (parser.peek() == ',') {
                        parser.pos++;
                        parser.skipWhitespaceAndComments();
                    } else {
                        break;
                    }
                }
            }

            parser.skipWhitespaceAndComments();

            // Read body or semicolon
            if (parser.peek() == '{') {
                parser.skipBody();
            } else if (parser.peek() == ';') {
                parser.pos++; // abstract method or interface method
            } else {
                // Unexpected - skip to next statement
                parser.skipToNextStatement();
            }

            // Build MethodInfo
            MethodInfo method = new MethodInfo();
            method.setMethodName(methodName);
            method.setReturnType(isConstructor ? null : type);
            method.setVisibility(determineVisibility(modifiers));
            method.setStatic(modifiers.contains("static"));
            method.setAbstract(modifiers.contains("abstract"));
            method.setFinal(modifiers.contains("final"));
            method.setParameters(paramList);
            method.setExceptions(exceptions);
            method.setMethodType(isConstructor ? "CONSTRUCTOR" : (modifiers.contains("static") ? "STATIC" : "INSTANCE"));

            if (!pendingAnnotations.isEmpty()) {
                method.setAnnotations(new ArrayList<>(pendingAnnotations));
            }

            methods.add(method);
            pendingAnnotations.clear();
        }

        return methods;
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private static String determineVisibility(List<String> modifiers) {
        if (modifiers.contains("public")) return "PUBLIC";
        if (modifiers.contains("protected")) return "PROTECTED";
        if (modifiers.contains("private")) return "PRIVATE";
        return "PACKAGE_PRIVATE";
    }

    private static String firstWord(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        int start = i;
        while (i < s.length() && s.charAt(i) != ' ') i++;
        return s.substring(start, i);
    }

    /**
     * Split a string by whitespace without using regex.
     */
    static List<String> splitByWhitespace(String s) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) break;
            int start = i;
            while (i < n && !Character.isWhitespace(s.charAt(i))) i++;
            tokens.add(s.substring(start, i));
        }
        return tokens;
    }

    /**
     * Find the index of the first top-level '=' (not inside generics, parens, strings).
     */
    private static int findTopLevelEquals(String s) {
        int angleDepth = 0;
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        boolean inChar = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') inChar = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '\'') { inChar = true; continue; }
            if (c == '<') angleDepth++;
            else if (c == '>') angleDepth--;
            else if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;
            else if (c == '{') braceDepth++;
            else if (c == '}') braceDepth--;
            else if (c == '[') bracketDepth++;
            else if (c == ']') bracketDepth--;
            else if (c == '=' && angleDepth == 0 && parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                // Make sure it's not == or != or <= or >=
                if (i + 1 < s.length() && s.charAt(i + 1) == '=') {
                    i++; // skip ==
                    continue;
                }
                if (i > 0 && (s.charAt(i - 1) == '!' || s.charAt(i - 1) == '<' || s.charAt(i - 1) == '>')) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Count the net change in brace depth from a line.
     * Ignores braces inside strings and comments.
     */
    private static int countBraceDepthChange(String line) {
        int change = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inLineComment) break;
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') inChar = false;
                continue;
            }
            if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                inLineComment = true;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '\'') { inChar = true; continue; }
            if (c == '{') change++;
            else if (c == '}') change--;
        }
        return change;
    }

    /**
     * Check if a '(' at the given position is inside a string literal or generic type.
     */
    private static boolean isInsideStringOrGeneric(String line, int parenPos) {
        boolean inString = false;
        boolean inChar = false;
        int angleDepth = 0;

        for (int i = 0; i < parenPos; i++) {
            char c = line.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\\') { i++; continue; }
                if (c == '\'') inChar = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '\'') { inChar = true; continue; }
            if (c == '<') angleDepth++;
            else if (c == '>') angleDepth--;
        }
        return inString || angleDepth > 0;
    }

    /**
     * Parse a parameter list string into individual parameter strings.
     * Handles nested generics correctly.
     */
    private static List<String> parseParameterList(String params) {
        List<String> result = new ArrayList<>();
        if (params == null || params.trim().isEmpty()) return result;

        int depth = 0;
        int start = 0;
        for (int i = 0; i < params.length(); i++) {
            char c = params.charAt(i);
            if (c == '<' || c == '(' || c == '[') depth++;
            else if (c == '>' || c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                String param = params.substring(start, i).trim();
                if (!param.isEmpty()) {
                    result.add(normalizeParameter(param));
                }
                start = i + 1;
            }
        }
        // Last parameter
        String lastParam = params.substring(start).trim();
        if (!lastParam.isEmpty()) {
            result.add(normalizeParameter(lastParam));
        }

        return result;
    }

    /**
     * Normalize a parameter string to "name : type" format.
     */
    private static String normalizeParameter(String param) {
        // Remove annotations from parameter
        List<String> tokens = splitByWhitespace(param);
        if (tokens.size() < 2) return param;

        // Skip annotations (tokens starting with @)
        int nameIdx = tokens.size() - 1;
        int typeStart = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).startsWith("@")) {
                typeStart = i + 1;
            } else {
                break;
            }
        }

        // Skip modifiers like 'final'
        while (typeStart < tokens.size() - 1 && "final".equals(tokens.get(typeStart))) {
            typeStart++;
        }

        if (typeStart >= tokens.size() - 1) return param;

        // Build type from all tokens except the last one
        StringBuilder typeBuilder = new StringBuilder();
        for (int i = typeStart; i < tokens.size() - 1; i++) {
            if (i > typeStart) typeBuilder.append(' ');
            typeBuilder.append(tokens.get(i));
        }
        String type = typeBuilder.toString();
        String name = tokens.get(tokens.size() - 1);

        return name + " : " + type;
    }
}