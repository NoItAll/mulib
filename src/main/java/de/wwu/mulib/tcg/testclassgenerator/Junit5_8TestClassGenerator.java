package de.wwu.mulib.tcg.testclassgenerator;

import de.wwu.mulib.tcg.TcgUtility;

import java.util.*;

public class Junit5_8TestClassGenerator implements TestClassGenerator {

    protected final String indentBy = "    ";
    protected final boolean assumeSetter;

    public Junit5_8TestClassGenerator(boolean assumeSetter) {
        this.assumeSetter = assumeSetter;
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
        testMethodStringBuilders.forEach(sb::append);
        sb.append(generateClassEnd());
        return sb.toString();
    }

    protected String generatePackageDeclaration(String packageName) {
        return "package " + packageName + ";" + System.lineSeparator();
    }

    protected String generateImports(Set<Class<?>> encounteredTypes) {
        StringBuilder sb = new StringBuilder();

        sb.append("import org.junit.*;").append(System.lineSeparator());
        sb.append("import static org.junit.Assert.*;").append(System.lineSeparator());

        encounteredTypes = sortEncounteredTypes(encounteredTypes);

        for (Class<?> typeToImport : encounteredTypes) {
            if (!omitFromImport(typeToImport)) {
                sb.append("import ").append(typeToImport.getName()).append(";").append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator().repeat(2));
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
        return "@SuppressWarnings(\"all\")" + System.lineSeparator();
    }

    protected String generateTestClassDeclaration(String testedClassName) {
        return "public class Test" + testedClassName + " {" + System.lineSeparator();
    }

    protected String generateClassAttributes() {
        return "";
    }

    protected String generateUtilityMethods() {
        if (assumeSetter) {
            return "";
        } else { // Utility method to use reflection instead of setters to set an object's field.
            return indentBy + "protected void " + TcgUtility.REFLECTION_SETTER_METHOD_NAME + "(Object setFor, String fieldName, Object setTo) {" + System.lineSeparator() +
                    indentBy.repeat(2) + "if (fieldName.startsWith(\"this$\")) {" + System.lineSeparator() +
                    indentBy.repeat(3) + "return;" + System.lineSeparator() +
                    indentBy.repeat(2) + "}" + System.lineSeparator() +
                    indentBy.repeat(2) + "try { " + System.lineSeparator() +
                    indentBy.repeat(3) + "Class<?> setForClass = setFor.getClass();" + System.lineSeparator() +
                    indentBy.repeat(3) + "Field setForField = setForClass.getDeclaredField(fieldName);" + System.lineSeparator() +
                    indentBy.repeat(3) + "setForField.setAccessible(true);" + System.lineSeparator() +
                    indentBy.repeat(3) + "setForField.set(setFor, setTo);" + System.lineSeparator() +
                    indentBy.repeat(2) + "} catch (Exception e) {" + System.lineSeparator() +
                    indentBy.repeat(3) + "throw new RuntimeException(e);" + System.lineSeparator() +
                    indentBy.repeat(2) + "}" + System.lineSeparator() +
                    indentBy + "}" + System.lineSeparator();
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
        return "}" + System.lineSeparator();
    }
}
