package de.wwu.mulib.tcg.testclassgenerator;

import de.wwu.mulib.tcg.TcgUtility;

import java.util.*;

public class Junit5_8TestClassGenerator implements TestClassGenerator {

    protected final String indentBy = "    ";
    protected final boolean assumeSetter;

    public Junit5_8TestClassGenerator() {
        this.assumeSetter = true;
    }

    @Override
    public String generateTestClassString(
            String packageName,
            String testedClassName,
            Set<Class<?>> encounteredTypes,
            List<StringBuilder> testMethodStringBuilders) {
        StringBuilder sb = new StringBuilder();
        sb.append(generatePackageDeclaration(packageName));
        sb.append(generateImports(encounteredTypes));
        sb.append(generateTestClassAnnotations());
        sb.append(generateTestClassDeclaration(testedClassName));
        sb.append(generateClassAttributes());
        sb.append(generateUtilityMethods());
        sb.append(generateBeforeClassMethod());
        sb.append(generateAfterClassMethod());
        sb.append(generateBeforeMethod());
        sb.append(generateAfterMethod());
        sb.append(generateClassEnd());
        return sb.toString();
    }

    protected String generatePackageDeclaration(String packageName) {
        return "package " + packageName + ";\r\n";
    }

    protected String generateImports(Set<Class<?>> encounteredTypes) {
        StringBuilder sb = new StringBuilder();

        sb.append("import org.junit.*;\r\n");
        sb.append("import static org.junit.Assert.*;\r\n");

        encounteredTypes = sortEncounteredTypes(encounteredTypes);

        for (Class<?> typeToImport : encounteredTypes) {
            if (!omitFromImport(typeToImport)) {
                sb.append("import ").append(typeToImport.getName()).append(";\r\n");
            }
        }
        sb.append("\r\n\r\n");
        return sb.toString();
    }

    protected boolean omitFromImport(Class<?> type) {
        return type.getName().startsWith("java.lang.");
    }

    protected SortedSet<Class<?>> sortEncounteredTypes(Set<Class<?>> encounteredTypes) {
        SortedSet<Class<?>> result = new TreeSet<>(Comparator.comparing(Class::getName));
        result.addAll(encounteredTypes);
        return result;
    }

    protected String generateTestClassAnnotations() {
        return "@SuppressWarnings(\"all\")\r\n";
    }

    protected String generateTestClassDeclaration(String testedClassName) {
        return "public class Test" + testedClassName + " {\r\n";
    }

    protected String generateClassAttributes() {
        return "";
    }

    protected String generateUtilityMethods() {
        if (assumeSetter) {
            return "";
        } else { // Utility method to use reflection instead of setters to set an object's field.
            return indentBy + "protected void " + TcgUtility.REFLECTION_SETTER_METHOD_NAME + "(Object setFor, String fieldName, Object setTo) {\r\n" +
                    indentBy.repeat(2) + "if (fieldName.startsWith(\"this$\")) {\r\n" +
                    indentBy.repeat(3) + "return;\r\n" +
                    indentBy.repeat(2) + "}\r\n" +
                    indentBy.repeat(2) + "try { \r\n" +
                    indentBy.repeat(3) + "Class<?> setForClass = setFor.getClass();\r\n" +
                    indentBy.repeat(3) + "Field setForField = setForClass.getDeclaredField(fieldName);\r\n" +
                    indentBy.repeat(3) + "setForField.setAccessible(true);\r\n" +
                    indentBy.repeat(3) + "setForField.set(setFor, setTo);\r\n" +
                    indentBy.repeat(2) + "} catch (Exception e) {\r\n" +
                    indentBy.repeat(3) + "throw new RuntimeException(e);\r\n" +
                    indentBy.repeat(2) + "}\r\n" +
                    indentBy + "}\r\n";
        }
    }

    protected String generateBeforeClassMethod() {
        return "";
    }

    protected String generateAfterClassMethod() {
        return "";
    }

    protected String generateBeforeMethod() {
        return "";
    }

    protected String generateAfterMethod() {
        return "";
    }

    protected String generateClassEnd() {
        return "}\r\n";
    }
}
