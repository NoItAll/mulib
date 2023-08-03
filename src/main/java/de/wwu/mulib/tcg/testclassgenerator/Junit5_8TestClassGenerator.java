package de.wwu.mulib.tcg.testclassgenerator;

import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

public class Junit5_8TestClassGenerator implements TestClassGenerator {

    protected final String indentBy = "    ";
    protected final TcgConfig tcgConfig;

    public Junit5_8TestClassGenerator(TcgConfig tcgConfig) {
        this.tcgConfig = tcgConfig;
    }

    @Override
    public String generateTestClassString(
            String packageName,
            String testedClassName,
            Set<Class<?>> encounteredTypes,
            int initialNumberTestCases,
            int reducedNumberOfTestcases,
            List<StringBuilder> testMethodStringBuilders) {
        StringBuilder sb = new StringBuilder();
        sb.append(generatePackageDeclaration(packageName));
        sb.append(generateImports(encounteredTypes));
        sb.append(generateTestClassAnnotations());
        sb.append(generateTestClassDeclaration(testedClassName, initialNumberTestCases, reducedNumberOfTestcases));
        sb.append(generateClassAttributes());
        sb.append(generateUtilityMethods());
        sb.append(generateBeforeClassMethod());
        sb.append(generateAfterClassMethod());
        sb.append(generateBeforeMethod());
        sb.append(generateAfterMethod());
        sb.append(indentBy).append("/* TEST CASES */").append(System.lineSeparator());
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

    protected String generateTestClassDeclaration(String testedClassName, int initialNumberTestCases, int reducedNumberOfTestcases) {
        LocalDateTime now = LocalDateTime.now();
        String result = "/**" + System.lineSeparator();
        result += " * Generation date: " + now.toLocalDate() + " at " + now.toLocalTime().getHour() + ":" + now.toLocalTime().getMinute() + System.lineSeparator();
        result += " * This class contains automatically generated test cases using the symbolic execution engine Mulib." + System.lineSeparator();
        result += " * It was generated using the following configuration: " + System.lineSeparator();
        result += " * " + indentBy + tcgConfig.toString() + System.lineSeparator(); // TODO: Make prettier
        if (!(tcgConfig.TEST_SET_REDUCER instanceof NullTestSetReducer)) {
            result += " * The overall set of test cases was reduced from " + initialNumberTestCases + (initialNumberTestCases > 1 ? " test cases" : " test case ")
                    + " to " + reducedNumberOfTestcases + (reducedNumberOfTestcases > 1 ? " test cases." : " test case.") + System.lineSeparator();
        }
        result += " */" + System.lineSeparator();
        result += "public class Test" + testedClassName + " {" + System.lineSeparator();
        return result;
    }

    protected String generateClassAttributes() {
        return "";
    }

    protected Object getWithReflection(Object getFrom, String fieldName) {
        try {
            Class<?> getFromClass = getFrom.getClass();
            Field getFromField = getFromClass.getDeclaredField(fieldName);
            getFromField.setAccessible(true);
            return getFromField.get(getFrom);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    protected String generateUtilityMethods() {
        StringBuilder sb = new StringBuilder();
        if (!tcgConfig.ASSUME_GETTERS || !tcgConfig.ASSUME_SETTERS) {
            sb.append(indentBy).append("/* UTILITY METHODS */").append(System.lineSeparator());
        }
        if (!tcgConfig.ASSUME_SETTERS) {
            // Utility method to use reflection instead of setters to set an object's field.
            sb.append(indentBy).append("/* Method for setting a field value for an object if no setter can be used */").append(System.lineSeparator())
                    .append(indentBy).append("protected void ").append(TcgUtility.REFLECTION_SETTER_METHOD_NAME).append("(Object setFor, String fieldName, Object setTo) {").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("if (fieldName.startsWith(\"this$\")) {").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("return;").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("}").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("try { ").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("Class<?> setForClass = setFor.getClass();").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("Field setForField = setForClass.getDeclaredField(fieldName);").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("setForField.setAccessible(true);").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("setForField.set(setFor, setTo);").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("} catch (Exception e) {").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("throw new RuntimeException(e);").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("}").append(System.lineSeparator())
                    .append(indentBy).append("}").append(System.lineSeparator());
        }
        if (!tcgConfig.ASSUME_GETTERS) {
            sb.append(indentBy).append("/* Method for retrieving a field value for an object if no getter can be used */").append(System.lineSeparator())
                    .append(indentBy).append("protected Object ").append(TcgUtility.REFLECTION_GETTER_METHOD_NAME).append("(Object getFrom, String fieldName) {").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("try {").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("Class<?> getFromClass = getFrom.getClass();").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("Field getFromField = getFromClass.getDeclaredField(fieldName);").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("getFromField.setAccessible(true);").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("return getFromField.get(getFrom);").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("} catch (Exception e) {").append(System.lineSeparator())
                    .append(indentBy.repeat(3)).append("throw new RuntimeException(e);").append(System.lineSeparator())
                    .append(indentBy.repeat(2)).append("}").append(System.lineSeparator())
                    .append(indentBy).append("}").append(System.lineSeparator());
        }

        if (!tcgConfig.ASSUME_GETTERS || !tcgConfig.ASSUME_SETTERS) {
            sb.append(System.lineSeparator());
        }
        return sb.toString();
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
