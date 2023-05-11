package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.tcg.TestCases;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
    protected final TestCases testCases;
    protected final Iterator<TestCase> it;
    protected String objectCalleeIdentifier;
    protected final List<Object> inputObjects;
    protected Object outputObject;

    public Junit5_8TestMethodGenerator(TestCases testCases, boolean assumeSetter, String assertEqualsDelta, Class<?>... specialCases) {
        this.assertEqualsDelta = assertEqualsDelta;
        this.specialCases = specialCases;
        this.encounteredTypes = new HashSet<>();
        this.assumeSetter = assumeSetter;
        this.inputObjects = new ArrayList<>();
        this.testCases = testCases;
        this.it = testCases.iterator();
        this.argumentNamesForObjects = new IdentityHashMap<>();
        this.argumentNameToNumberOfOccurrences = new HashMap<>();
    }

    @Override
    public StringBuilder generateNextTestCaseRepresentation() {
        if (!it.hasNext()) {
            throw new IllegalStateException("No test cases");
        }
        StringBuilder result = execute(it.next());
        after();
        return result;
    }

    @Override
    public boolean hasNextTestCase() {
        return it.hasNext();
    }

    protected void after() {
        argumentNamesForObjects.clear();
        argumentNameToNumberOfOccurrences.clear();
        inputObjects.clear();
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
        sb.append(generateTestMethodAnnotations(tc));
        sb.append(generateTestMethodDeclaration(tc));
        sb.append(generateStringsForInputs(tc));
        sb.append(generateStringForOutput(tc));
        sb.append(generateAssertionString());
        sb.append(generateTestMethodEnd());
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
        return "@Test(expected=" + e.getClass().getName() + ".class)\r\n";
    }

    protected String generateTestAnnotationForReturn(Object returnVal) {
        return "@Test\r\n";
    }

    protected String generateTestMethodDeclaration(TestCase tc) {
        return "public void test_" + testCases.getNameOfTestedMethod() + "_" + numberOfTest + "() {\r\n";
    }

    protected String generateTestMethodEnd() {
        return "}\r\n";
    }

    protected String generateStringsForInputs(TestCase tc) {
        Object[] inputs = tc.getSolution().labels.getLabels();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.length; i++) {
            sb.append(generateElementString(inputs[i]));
            if (i == 0 && isObjectMethod()) {
                objectCalleeIdentifier = argumentNamesForObjects.get(inputObjects.get(i));
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
        if (isPrimitiveClass(oc)) {
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
        sb.append(o.getClass().getSimpleName())
                .append(" ")
                .append(arrayName)
                .append(" = new ")
                .append(type)
                .append("[")
                .append(Array.getLength(o))
                .append("]")
                .append(";\r\n");

        return appendStoreInArray(o, sb, arrayName);
    }

    protected StringBuilder generateMultiArrayString(Object o) {
        StringBuilder sb = new StringBuilder();
        String arrayName = argumentNamesForObjects.get(o);
        String type = getInnerSimpleTypeForArrayOrSimpleType(o);
        sb.append(o.getClass().getSimpleName())
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
                .append(";\r\n");

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
                    .append(";\r\n");
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
        sb.append(o.getClass().getSimpleName());
        sb.append(argumentNamesForObjects.get(o));
        sb.append(" = ").append(o);
        return sb;
    }

    protected StringBuilder generateWrappingString(Object o) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(o.getClass().getSimpleName());
            sb.append(" ");
            sb.append(argumentNamesForObjects.get(o));
            sb.append(" = ");
            Field valueField = o.getClass().getDeclaredField("value");
            boolean accessible = valueField.canAccess(o);
            valueField.setAccessible(true);
            sb.append(addCastIfNeeded(o.getClass()));
            sb.append(valueField.get(o)).append(";\r\n");
            valueField.setAccessible(accessible);
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
        if (isSpecialCase(o.getClass())) {
            return generateSpecialCaseString(o);
        }
        sb.append(generateConstructionString(o));
        sb.append(generateAttributesStrings(o));
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
            sb.append(collectionName).append(".add(").append(argumentNamesForObjects.get(o)).append(");\r\n");
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
            sb.append(mapName).append(".put(").append(keyName).append(", ").append(valueName).append(");\r\n");
        }
        return sb;
    }

    protected StringBuilder generateStringString(Object o) {
        return new StringBuilder("String ").append(argumentNamesForObjects.get(o)).append(" = \"").append(o.toString()).append("\";\r\n");
    }

    protected StringBuilder generateConstructionString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName()).append(" ");
        sb.append(argumentNamesForObjects.get(o)).append(" = ");
        sb.append("new ").append(o.getClass().getSimpleName()).append("();\r\n");
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
            sb.append(generateElementString(fieldValue));
            String fieldValueArgumentName = argumentNamesForObjects.get(fieldValue);
            if (assumeSetter) {
                sb.append(objectArgumentName)
                        .append(".set")
                        .append(toFirstUpper(fieldName))
                        .append("(")
                        .append(fieldValueArgumentName)
                        .append(");\r\n");
            } else {
                sb.append(TcgUtility.REFLECTION_SETTER_METHOD_NAME)
                        .append("(")
                        .append(objectArgumentName)
                        .append(", \"")
                        .append(f.getName())
                        .append("\", ")
                        .append(fieldValueArgumentName)
                        .append(");\r\n");
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
        String[] inputObjectNames = new String[inputObjects.size()];
        for (int i = 0; i < inputObjectNames.length; i++) {
            inputObjectNames[i] = argumentNamesForObjects.get(inputObjects.get(i));
        }
        String outputObjectName = argumentNamesForObjects.get(outputObject);
        StringBuilder sb = new StringBuilder();
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
        sb.append(";\r\n");
        return sb.toString();
    }

    protected String generateTestedMethodCallString(String[] inputObjectNames) {
        StringBuilder sb = new StringBuilder();
        if (isObjectMethod()) {
            sb.append(objectCalleeIdentifier);
        } else {
            sb.append(testCases.getNameOfTestedClass());
        }
        sb.append(".").append(testCases.getNameOfTestedMethod()).append("(");
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
        return !Modifier.isStatic(testCases.getTestedMethod().getModifiers());
    }
}
