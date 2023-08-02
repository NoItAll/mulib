package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.TestCase;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.wwu.mulib.tcg.TcgUtility.*;

public class Junit5_8TestMethodGenerator implements TestMethodGenerator {
    protected final Class<?>[] specialCases;
    protected final String assertEqualsDelta;
    protected final Set<Class<?>> encounteredTypes;
    // Temporary store for objects and their argument name in the test method
    // Should be cleared after the String for the TestCase-object was generated.
    protected final Map<Object, String> argumentNamesForObjects;
    // Temporary stores for object types how many of them are already within a method.
    // Should be cleared after the String for the TestCase-object was generated.
    protected final Map<String, Integer> argumentNameToNumberOfOccurrences;
    // An identifier for the currently generated test
    protected int numberOfTest = 0;
    // Full name of class for which test cases are generated
    protected final boolean assumeSetter;
    protected String objectCalleeIdentifier;
    protected Object[] inputObjects;
    protected Object outputObject;
    protected final String nameOfTestedClass;
    protected final String nameOfTestedMethod;
    protected final Method testedMethod;
    protected final String indent;
    protected final boolean generatePostStateChecksForObjectsIfSpecified;

    public Junit5_8TestMethodGenerator(
            Method testedMethod,
            String indent,
            boolean assumeSetter,
            String assertEqualsDelta,
            boolean generatePostStateChecksForObjectsIfSpecified,
            Class<?>... specialCases) {
        this.indent = indent;
        this.assertEqualsDelta = assertEqualsDelta;
        this.specialCases = specialCases;
        this.encounteredTypes = new HashSet<>();
        this.assumeSetter = assumeSetter;
        this.argumentNamesForObjects = new IdentityHashMap<>();
        this.argumentNameToNumberOfOccurrences = new HashMap<>();
        this.testedMethod = testedMethod;
        this.nameOfTestedClass = testedMethod.getDeclaringClass().getSimpleName();
        this.nameOfTestedMethod = testedMethod.getName();
        this.generatePostStateChecksForObjectsIfSpecified = generatePostStateChecksForObjectsIfSpecified;
    }

    @Override
    public StringBuilder generateTestCaseRepresentation(TestCase testCase) {
        this.outputObject = testCase.getReturnValue();
        this.inputObjects = testCase.getInputs();
        StringBuilder result = execute(testCase);
        after();
        return result;
    }

    protected void after() {
        argumentNamesForObjects.clear();
        argumentNameToNumberOfOccurrences.clear();
        inputObjects = null;
        objectCalleeIdentifier = null;
        outputObject = null;
        numberOfTest++;
    }

    @Override
    public Set<Class<?>> getEncounteredTypes() {
        return encounteredTypes;
    }

    protected StringBuilder execute(TestCase tc) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(generateTestMethodAnnotations(tc))
                .append(indent).append(generateTestMethodDeclaration(tc))
                .append(generateStringsForInputs(tc)) // Initializes variables and values for inputs; indent occurs inside
                .append(generateStringForOutput(tc.getReturnValue())) // Initializes variable and value for output; indent occurs inside
                .append(generateAssertionString()) // Indent occurs inside
                .append(indent).append(generateTestMethodEnd());
        return sb;
    }

    protected String generateTestMethodAnnotations(TestCase tc) {
        if (tc.isExceptional()) {
            return generateTestAnnotationForException((Exception) tc.getReturnValue());
        } else {
            return generateTestAnnotationForReturn(tc.getReturnValue());
        }
    }

    protected String generateTestAnnotationForException(Exception e) {
        return "@Test(expected=" + e.getClass().getName() + ".class)" + System.lineSeparator();
    }

    protected String generateTestAnnotationForReturn(Object returnVal) {
        return "@Test" + System.lineSeparator();
    }

    protected String generateTestMethodDeclaration(TestCase tc) {
        return "public void test_" + nameOfTestedMethod + "_" + numberOfTest + "() {" + System.lineSeparator();
    }

    protected String generateTestMethodEnd() {
        return "}" + System.lineSeparator();
    }

    protected String generateStringsForInputs(TestCase tc) {
        Object[] inputs = tc.getInputs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.length; i++) {
            sb.append(generateElementString(inputs[i]));
            if (i == 0 && isObjectMethod()) {
                objectCalleeIdentifier = argumentNamesForObjects.get(inputObjects[i]);
            }
        }
        return sb.toString();
    }

    protected StringBuilder generateElementString(Object o) {
        if (o == null) {
            return new StringBuilder(); // No element string has to be generated for null
        }
        if (isAlreadyCreated(o)) {
            return new StringBuilder();
        }

        Class<?> oc = o.getClass();
        addToEncounteredTypes(oc);
        generateNumberedArgumentName(o);
        if (isPrimitiveClass(oc)) { // TODO Obviously not yet taken for int; - transmit type information via different mean
            return generatePrimitiveString(o);
        } else if (isWrappingClass(oc)) {
            return generateWrappingString(o);
        } else if (isStringClass(oc)) {
            return generateStringString(o);
        } else if(oc.isArray()){
            if(oc.getComponentType().isArray()){
                return generateMultiArrayString(o);
            } else {
                return generateArrayString(o);
            }
        } else {
            return generateObjectString(o);
        }
    }

    protected StringBuilder generateArrayString(Object o) {
        StringBuilder sb = new StringBuilder();
        String arrayName = argumentNamesForObjects.get(o);
        String type = getInnerSimpleTypeForArrayOrSimpleType(o);
        sb.append(indent.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(" ")
                .append(arrayName)
                .append(" = new ")
                .append(type)
                .append("[")
                .append(Array.getLength(o))
                .append("]")
                .append(";")
                .append(System.lineSeparator());

        return appendStoreInArray(o, sb, arrayName);
    }

    protected StringBuilder generateMultiArrayString(Object o) {
        StringBuilder sb = new StringBuilder();
        String arrayName = argumentNamesForObjects.get(o);
        String type = getInnerSimpleTypeForArrayOrSimpleType(o);
        sb.append(indent.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(" ")
                .append(arrayName)
                .append(" = new ")
                .append(type)
                .append("[")
                .append(Array.getLength(o))
                .append("]")
                .append("[")
                .append(Array.getLength(Array.get(o, 0)))
                .append("]")
                .append(";")
                .append(System.lineSeparator());

        return appendStoreInArray(o, sb, arrayName);
    }

    private String getInnerSimpleTypeForArrayOrSimpleType(Object o) {
        String result = o.getClass().getSimpleName();
        int index = result.indexOf("[");
        if(index != -1){
            result = result.substring(0, index);
        }
        return result;
    }

    private StringBuilder appendStoreInArray(Object o, StringBuilder sb, String arrayName) {
        for (int i = 0; i < Array.getLength(o); i++) {
            Object arrayElement = Array.get(o, i);
            sb.append(generateElementString(arrayElement));
            sb.append(arrayName)
                    .append("[")
                    .append(i)
                    .append("] = ")
                    .append(argumentNamesForObjects.get(arrayElement))
                    .append(";")
                    .append(System.lineSeparator());
        }

        return sb;
    }

    protected void addToEncounteredTypes(Class<?> oc) {
        if (oc.isPrimitive()) {
            return;
        }
        if (oc.isArray()) {
            Class<?> componentClass = oc.getComponentType();
            if (componentClass.isPrimitive()) {
                return;
            }
            addToEncounteredTypes(componentClass);
        } else {
            encounteredTypes.add(oc);
        }
    }

    protected boolean isAlreadyCreated(Object o) {
        return argumentNamesForObjects.containsKey(o);
    }

    protected StringBuilder generatePrimitiveString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(argumentNamesForObjects.get(o))
                .append(" = ").append(o);
        return sb;
    }

    protected StringBuilder generateWrappingString(Object o) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(indent.repeat(2))
                    .append(o.getClass().getSimpleName())
                    .append(" ")
                    .append(argumentNamesForObjects.get(o)).append(" = ");
            Field valueField = o.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            sb.append(addCastIfNeeded(o.getClass()));
            sb.append(valueField.get(o)).append(";").append(System.lineSeparator());
            return sb;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean wrappingClassNeedsCast(Class<?> oc) {
        return oc.equals(Float.class) || oc.equals(Short.class) || oc.equals(Byte.class);
    }

    protected String addCastIfNeeded(Class<?> oc) {
        if (wrappingClassNeedsCast(oc)) {
            // Float, Byte, or Short
            return "(" + toFirstLower(oc.getSimpleName()) + ") ";
        } else {
            return "";
        }
    }

    protected String generateNumberedArgumentName(Object o) {
        String numberedArgumentNameForType = argumentNamesForObjects.get(o);
        if (numberedArgumentNameForType != null) {
            return numberedArgumentNameForType;
        }
        Class<?> type = o.getClass();
        String argumentNameForType = getArgumentNameForType(type);
        Integer currentNumberOfArgumentType = argumentNameToNumberOfOccurrences.get(argumentNameForType);
        if (currentNumberOfArgumentType == null) {
            currentNumberOfArgumentType = 0;
        }
        numberedArgumentNameForType = argumentNameForType + currentNumberOfArgumentType;
        argumentNameToNumberOfOccurrences.put(argumentNameForType, currentNumberOfArgumentType + 1);
        argumentNamesForObjects.put(o, numberedArgumentNameForType);
        return numberedArgumentNameForType;
    }

    protected String getArgumentNameForType(Class<?> type) {
        if (type.isArray()) {
            if (type.getComponentType().isArray()) {
                String simpleName = type.getSimpleName();
                return toFirstLower(simpleName.substring(0, simpleName.length() - 4)) + "Ar";
            } else {
                String simpleName = type.getSimpleName();
                return toFirstLower(simpleName.substring(0, simpleName.length() - 2)) + "Ar";
            }
        } else {
            return toFirstLower(type.getSimpleName());
        }
    }

    protected StringBuilder generateObjectString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent.repeat(2));
        if (isSpecialCase(o.getClass())) {
            sb.append(generateSpecialCaseString(o));
            return sb;
        }
        sb.append(generateConstructionString(o))
                .append(generateAttributesStrings(o));
        return sb;
    }

    protected boolean isSpecialCase(Class<?> objectClass) {
        for (Class<?> specialCase : specialCases) {
            if (specialCase.isAssignableFrom(objectClass)) {
                return true;
            }
        }
        return false;
    }

    protected StringBuilder generateSpecialCaseString(Object o) {
        if (o instanceof Collection) {
            return treatCollectionSpecialCase((Collection<?>) o);
        } else if (o instanceof Map) {
            return treatMapSpecialCase((Map<?, ?>) o);
        }
        throw new UnsupportedOperationException("Aside from Collection and Map there currently are no special cases " +
                "for the StdTestCaseGenerator yet.");
    }

    protected StringBuilder treatCollectionSpecialCase(Collection<?> c) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateConstructionString(c));
        String collectionName = argumentNamesForObjects.get(c);
        for (Object o : c) {
            sb.append(generateElementString(o));
            sb.append(collectionName).append(".add(").append(argumentNamesForObjects.get(o)).append(");").append(System.lineSeparator());
        }
        return sb;
    }

    protected StringBuilder treatMapSpecialCase(Map<?, ?> m) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateConstructionString(m));
        String mapName = argumentNamesForObjects.get(m);
        for (Map.Entry<?, ?> e : m.entrySet()) {
            sb.append(generateElementString(e.getKey()));
            sb.append(generateElementString(e.getValue()));
            String keyName = argumentNamesForObjects.get(e.getKey());
            String valueName = argumentNamesForObjects.get(e.getValue());
            sb.append(mapName).append(".put(").append(keyName).append(", ").append(valueName).append(");").append(System.lineSeparator());
        }
        return sb;
    }

    protected StringBuilder generateStringString(Object o) {
        return new StringBuilder(indent.repeat(2))
                .append("String ")
                .append(argumentNamesForObjects.get(o))
                .append(" = \"")
                .append(o.toString())
                .append("\";").append(System.lineSeparator());
    }

    protected StringBuilder generateConstructionString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName()).append(" ");
        sb.append(argumentNamesForObjects.get(o)).append(" = ");
        sb.append("new ").append(o.getClass().getSimpleName()).append("();").append(System.lineSeparator());
        return sb;
    }

    protected String generateAttributesStrings(Object o) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = o.getClass().getDeclaredFields();
        String objectArgumentName = argumentNamesForObjects.get(o);
        for (Field f : fields) {
            if (skipField(f)) {
                continue;
            }
            sb.append(generateSetStatementForObject(objectArgumentName, o, f));
        }
        return sb.toString();
    }

    protected boolean skipField(Field f) {
        return f.getName().contains("jacoco");
    }

    protected String generateSetStatementForObject(String objectArgumentName, Object o, Field f) {
        try {
            StringBuilder sb = new StringBuilder();
            f.setAccessible(true);
            Object fieldValue = f.get(o);
            String fieldName = f.getName();
            sb.append(generateElementString(fieldValue))
                    .append(indent.repeat(2));
            String fieldValueArgumentName = argumentNamesForObjects.get(fieldValue);
            if (assumeSetter) {
                sb.append(objectArgumentName)
                        .append(".set")
                        .append(toFirstUpper(fieldName))
                        .append("(")
                        .append(fieldValueArgumentName)
                        .append(");")
                        .append(System.lineSeparator());
            } else {
                sb.append(TcgUtility.REFLECTION_SETTER_METHOD_NAME)
                        .append("(")
                        .append(objectArgumentName)
                        .append(", \"")
                        .append(f.getName())
                        .append("\", ")
                        .append(fieldValueArgumentName)
                        .append(");")
                        .append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected StringBuilder generateStringForOutput(Object output) {
        return generateElementString(output);
    }

    protected String generateAssertionString() {
        String[] inputObjectNames = new String[inputObjects.length];
        for (int i = 0; i < inputObjectNames.length; i++) {
            inputObjectNames[i] = argumentNamesForObjects.get(inputObjects[i]);
        }
        String outputObjectName = argumentNamesForObjects.get(outputObject);
        StringBuilder sb = new StringBuilder();
        sb.append(indent.repeat(2));
        if (outputObject != null && !outputObject.getClass().isArray()) {
            sb.append("assertEquals(");
            if (TcgUtility.isWrappingClass(outputObject.getClass())) {
                if (outputObject instanceof Integer) {
                    sb.append("(int)");
                } else if (outputObject instanceof Long) {
                    sb.append("(long)");
                } else if (outputObject instanceof Double) {
                    sb.append("(double)");
                } else if (outputObject instanceof Float) {
                    sb.append("(float)");
                } else if (outputObject instanceof Short) {
                    sb.append("(short)");
                } else if (outputObject instanceof Byte) {
                    sb.append("(byte)");
                } else if (outputObject instanceof Character) {
                    sb.append("(char)");
                } else if (outputObject instanceof Boolean) {
                    sb.append("(boolean)");
                }
            }
            sb.append(outputObjectName).append(", ");
        } else if (outputObject != null) {
            sb.append("assertArrayEquals(").append(outputObjectName).append(", ");
        }
        sb.append(generateTestedMethodCallString(inputObjectNames));
        if (outputObject != null) {
            if (isFloatingPointClass(outputObject.getClass())) {
                sb.append(", ").append(assertEqualsDelta);
            }
            sb.append(")");
        }
        sb.append(";").append(System.lineSeparator());
        return sb.toString();
    }

    protected String generateTestedMethodCallString(String[] inputObjectNames) {
        StringBuilder sb = new StringBuilder();
        if (isObjectMethod()) {
            sb.append(objectCalleeIdentifier);
        } else {
            sb.append(nameOfTestedClass);
        }
        sb.append(".").append(nameOfTestedMethod).append("(");
        for (int i = 0; i < inputObjectNames.length; i++) {
            if (i == 0 && isObjectMethod()) {
                continue;
            }
            sb.append(inputObjectNames[i]).append(",");
        }
        if (inputObjectNames.length - (isObjectMethod() ? 1 : 0) != 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }

    private boolean isObjectMethod() {
        return !Modifier.isStatic(testedMethod.getModifiers());
    }
}
