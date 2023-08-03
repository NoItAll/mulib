package de.wwu.mulib.tcg.testclassgenerator;

import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.testsetreducer.NullTestSetReducer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

public class JunitJupiterTestClassGenerator implements TestClassGenerator {

    protected final String indentBy = "    ";
    protected final TcgConfig tcgConfig;

    public JunitJupiterTestClassGenerator(TcgConfig tcgConfig) {
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

        sb.append("import org.junit.jupiter.api.Test;").append(System.lineSeparator());
        sb.append("import static org.junit.jupiter.api.Assertions.*;").append(System.lineSeparator());

        if (!tcgConfig.ASSUME_EQUALS_METHODS || !tcgConfig.ASSUME_SETTERS || !tcgConfig.ASSUME_GETTERS) {
            encounteredTypes.add(Field.class);
        }

        if (!tcgConfig.ASSUME_EQUALS_METHODS) {
            encounteredTypes.addAll(List.of(Array.class, List.class, ArrayList.class));
        }
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
        return type.getPackageName().equals("java.lang");
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

    protected String generateUtilityMethods() {
        StringBuilder sb = new StringBuilder();
        if (!tcgConfig.ASSUME_GETTERS || !tcgConfig.ASSUME_SETTERS || !tcgConfig.ASSUME_EQUALS_METHODS) {
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

        if (!tcgConfig.ASSUME_EQUALS_METHODS) {
            sb.append(tcgConfig.INDENT).append("protected void ").append(TcgUtility.REFLECTION_COMPARE_OBJECTS).append("(Object o0, Object o1) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append(TcgUtility.REFLECTION_COMPARE_OBJECTS_INNER).append("(o0, o1, new ArrayList<>());").append(System.lineSeparator())
                    .append(tcgConfig.INDENT).append("}").append(System.lineSeparator());

            sb.append(tcgConfig.INDENT).append("static class ComparedPair {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("final Object o0;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("final Object o1;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("ComparedPair(Object o0, Object o1) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("this.o0 = o0;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("this.o1 = o1;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("@Override").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("public boolean equals(Object o) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("if (!(o instanceof ComparedPair)) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("return false;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("return (o0 == ((ComparedPair) o).o0 && o1 == ((ComparedPair) o).o1)").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(5)).append("|| (o1 == ((ComparedPair) o).o0 && o0 == ((ComparedPair) o).o1);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT).append("}").append(System.lineSeparator());

            sb.append(tcgConfig.INDENT).append("protected void ").append(TcgUtility.REFLECTION_COMPARE_OBJECTS_INNER).append("(Object o0, Object o1, List<ComparedPair> comparedObjects) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("/* Needed to avoid cyclic comparisons */").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("ComparedPair comparedPair = new ComparedPair(o0, o1);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (comparedObjects.contains(comparedPair)) return;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("comparedObjects.add(comparedPair);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (o0 == null && o1 != null) fail(o1 + \" is not null\");").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (o1 == null) fail(o0 + \" is not null\");").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (!o0.getClass().equals(o1.getClass())) fail(\"Objects do not have the same class\");").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("Class<?> c = o0.getClass();").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("/* Check if call to .equals(Object) is definitely safe: */").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if ((c.isPrimitive() && c != void.class)").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append(indentBy.repeat(2)).append("|| c.getName().startsWith(\"java\")) {").append(System.lineSeparator())
//                    .append(tcgConfig.INDENT.repeat(2)).append(indentBy.repeat(2)).append("|| c == Integer.class || c == Long.class").append(System.lineSeparator())
//                    .append(tcgConfig.INDENT.repeat(2)).append(indentBy.repeat(2)).append("|| c == Double.class || c == Float.class || c == Short.class || c == Byte.class || c == Boolean.class").append(System.lineSeparator())
//                    .append(tcgConfig.INDENT.repeat(2)).append(indentBy.repeat(2)).append("|| c == Character.class || c == Boolean.class || c == String.class) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("if (!o0.equals(o1)) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("fail(o0 + \" does not equal \" + o1);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("} else {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("return;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (c.getClass().isArray()) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append(TcgUtility.REFLECTION_COMPARE_ARRAYS_INNER).append("(o0, o1, comparedObjects);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("return;").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("try {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("Field[] fields = c.getDeclaredFields(); // TODO Include fields from inheritance").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("for (Field f : fields) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("f.setAccessible(true);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("Object val0 = f.get(o0);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append("Object val1 = f.get(o1);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(4)).append(TcgUtility.REFLECTION_COMPARE_OBJECTS_INNER).append("(val0, val1, comparedObjects);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("} catch (Exception e) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("throw new RuntimeException(e);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT).append("}").append(System.lineSeparator());

            sb.append(tcgConfig.INDENT).append("protected void ").append(TcgUtility.REFLECTION_COMPARE_ARRAYS_INNER).append("(Object ar0, Object ar1, List<ComparedPair> comparedObjects) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("int lengthAr0 = Array.getLength(ar0);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("int lengthAr1 = Array.getLength(ar1);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("if (lengthAr0 != lengthAr1) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("fail(\"Arrays do not have the same length\");").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("for (int i = 0; i < lengthAr0; i++) {").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("Object elementAr0 = Array.get(ar0, i);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append("Object elementAr1 = Array.get(ar1, i);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(3)).append(TcgUtility.REFLECTION_COMPARE_OBJECTS_INNER).append("(elementAr0, elementAr1, comparedObjects);").append(System.lineSeparator())
                    .append(tcgConfig.INDENT.repeat(2)).append("}").append(System.lineSeparator())
                    .append(tcgConfig.INDENT).append("}").append(System.lineSeparator());
        }

        if (!tcgConfig.ASSUME_GETTERS || !tcgConfig.ASSUME_SETTERS || !tcgConfig.ASSUME_EQUALS_METHODS) {
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