package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.TestCase;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.wwu.mulib.tcg.TcgUtility.*;

public class JunitJupiterTestMethodGenerator implements TestMethodGenerator {
    protected final Set<Class<?>> encounteredTypes;
    // Temporary store for objects and their argument name in the test method
    // Should be cleared after the String for the TestCase-object was generated.
    protected final Map<Object, String> argumentNamesForObjects;
    // Temporary stores for object types how many of them are already within a method.
    // Should be cleared after the String for the TestCase-object was generated.
    protected final Map<String, Integer> argumentNameToNumberOfOccurrences;
    // An identifier for the currently generated test
    protected int numberOfTest = 0;
    protected String objectCalleeIdentifier;
    protected TestCase testCase;
    protected final String nameOfTestedClass;
    protected final String nameOfTestedMethod;
    protected final Method testedMethod;
    protected final TcgConfig config;

    public JunitJupiterTestMethodGenerator(
            Method testedMethod,
            TcgConfig config) {
        this.config = config;
        this.encounteredTypes = new HashSet<>();
        this.argumentNamesForObjects = new IdentityHashMap<>();
        this.argumentNameToNumberOfOccurrences = new HashMap<>();
        this.testedMethod = testedMethod;
        this.nameOfTestedClass = testedMethod.getDeclaringClass().getSimpleName();
        this.nameOfTestedMethod = testedMethod.getName();
    }

    @Override
    public StringBuilder generateTestCaseRepresentation(TestCase testCase) {
        this.testCase = testCase;
        StringBuilder result = execute();
        after();
        return result;
    }

    protected void after() {
        argumentNamesForObjects.clear();
        argumentNameToNumberOfOccurrences.clear();
        testCase = null;
        objectCalleeIdentifier = null;
        numberOfTest++;
    }

    @Override
    public Set<Class<?>> getEncounteredTypes() {
        return encounteredTypes;
    }

    protected StringBuilder execute() {
        StringBuilder sb = new StringBuilder();
        sb.append(config.INDENT).append(generateTestMethodAnnotations())
                .append(config.INDENT).append(generateTestMethodDeclaration())
                .append(generateStringsForInputs()) // Initializes variables and values for inputs; indent occurs inside
                .append(generateStringForReturnValue()) // Initializes variable and value for output; indent occurs inside
                .append(generateAssertionString()) // Indent occurs inside
                .append(config.INDENT).append(generateTestMethodEnd());
        return sb;
    }

    protected String generateTestMethodAnnotations() {
        if (testCase.isExceptional()) {
            return generateTestAnnotationForException((Exception) testCase.getReturnValue());
        } else {
            return generateTestAnnotationForReturn(testCase.getReturnValue());
        }
    }

    protected String generateTestAnnotationForException(Exception e) {
        return "@Test(expected=" + e.getClass().getName() + ".class)" + System.lineSeparator();
    }

    protected String generateTestAnnotationForReturn(Object returnVal) {
        return "@Test" + System.lineSeparator();
    }

    protected String generateTestMethodDeclaration() {
        return "public void test_" + nameOfTestedMethod + "_" + numberOfTest + "() {" + System.lineSeparator();
    }

    protected String generateTestMethodEnd() {
        return "}" + System.lineSeparator();
    }

    protected String generateStringsForInputs() {
        Object[] inputs = testCase.getInputs();
        StringBuilder sb = new StringBuilder();
        if (inputs.length > 0) {
            sb.append(config.INDENT.repeat(2)).append("/* Initialize inputs */").append(System.lineSeparator());
        }
        for (int i = 0; i < inputs.length; i++) {
            sb.append(generateElementString(inputs[i]));
            if (i == 0 && isObjectMethod()) {
                objectCalleeIdentifier = argumentNamesForObjects.get(testCase.getInputs()[i]);
            }
        }
        sb.append(System.lineSeparator());

        if (config.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED) {
            Object[] inputsAfterExec = testCase.getInputsAfterExecution();
            if (inputsAfterExec.length > 0) {
                sb.append(config.INDENT.repeat(2)).append("/* Initialize the state of object inputs after executing the method */").append(System.lineSeparator());
                for (Object o : inputsAfterExec) {
                    sb.append(generateElementString(o));
                }
                sb.append(System.lineSeparator());
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
        sb.append(config.INDENT.repeat(2))
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
        sb.append(config.INDENT.repeat(2))
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
            sb.append(config.INDENT.repeat(2)).append(arrayName)
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
        sb.append(config.INDENT.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(argumentNamesForObjects.get(o))
                .append(" = ").append(o);
        return sb;
    }

    protected StringBuilder generateWrappingString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.INDENT.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(" ")
                .append(argumentNamesForObjects.get(o)).append(" = ");
        sb.append(addCastIfNeeded(o.getClass()));
        sb.append(o).append(";").append(System.lineSeparator());
        return sb;
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
            return toFirstLower(getArgumentNameForType(type.getComponentType())) + "Ar";
        } else {
            return toFirstLower(type.getSimpleName());
        }
    }

    protected StringBuilder generateObjectString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.INDENT.repeat(2));
        if (isSpecialCase(o.getClass())) {
            sb.append(generateSpecialCaseString(o));
            return sb;
        }
        sb.append(generateConstructionString(o))
                .append(generateAttributesStrings(o));
        return sb;
    }

    protected boolean isSpecialCase(Class<?> objectClass) {
        for (Class<?> specialCase : config.SPECIAL_CASES) {
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
        return new StringBuilder(config.INDENT.repeat(2))
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
                    .append(config.INDENT.repeat(2));
            String fieldValueArgumentName = argumentNamesForObjects.get(fieldValue);
            if (config.ASSUME_SETTERS) {
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

    protected StringBuilder generateStringForReturnValue() {
        return generateElementString(testCase.getReturnValue());
    }

    protected String generateAssertionString() {
        String[] inputObjectNames = new String[this.testCase.getInputs().length];
        for (int i = 0; i < inputObjectNames.length; i++) {
            inputObjectNames[i] = argumentNamesForObjects.get(this.testCase.getInputs()[i]);
        }
        StringBuilder sb = new StringBuilder();
        String methodUnderTestCall = generateTestedMethodCallString(inputObjectNames);
        if (!testedMethod.getReturnType().equals(Void.TYPE)) {
            sb.append(config.INDENT.repeat(2)).append("/* Assert correctness of the output value */").append(System.lineSeparator());
        }
        addAssertion(sb, this.testCase.getReturnValue(), methodUnderTestCall);
        sb.append(System.lineSeparator());

        if (config.GENERATE_POST_STATE_CHECKS_FOR_OBJECTS_IF_SPECIFIED) {
            Object[] inputsAfterExec = testCase.getInputsAfterExecution();
            if (inputsAfterExec.length > 0) {
                sb.append(config.INDENT.repeat(2)).append("/* Assert correctness of state of outputs objects after executing the method */").append(System.lineSeparator());
                for (Object iae : inputsAfterExec) {
                    Object inputBeforeExecution = testCase.getInputPreExecutionForInputAfterExecution(iae);
                    String nameOfInputAfterExec = this.argumentNamesForObjects.get(inputBeforeExecution);
                    assert nameOfInputAfterExec != null;
                    addAssertion(sb, iae, nameOfInputAfterExec);
                }
            }
        }
        return sb.toString();
    }

    protected void addAssertion(StringBuilder addTo, Object objectToAssert, String assertWith) {
        String objectName = argumentNamesForObjects.get(objectToAssert);
        assert objectName != null;
        addTo.append(config.INDENT.repeat(2));
        if (objectToAssert != null && !objectToAssert.getClass().isArray()) {
            if (config.ASSUME_EQUALS_METHODS
                    || objectToAssert.getClass().isPrimitive()
                    || TcgUtility.isWrappingClass(objectToAssert.getClass())) {
                addTo.append("assertEquals(");
            } else {
                addTo.append(REFLECTION_COMPARE_OBJECTS).append("(");
            }
            if (TcgUtility.isWrappingClass(objectToAssert.getClass())) {
                if (objectToAssert instanceof Integer) {
                    addTo.append("(int)");
                } else if (objectToAssert instanceof Long) {
                    addTo.append("(long)");
                } else if (objectToAssert instanceof Double) {
                    addTo.append("(double)");
                } else if (objectToAssert instanceof Float) {
                    addTo.append("(float)");
                } else if (objectToAssert instanceof Short) {
                    addTo.append("(short)");
                } else if (objectToAssert instanceof Byte) {
                    addTo.append("(byte)");
                } else if (objectToAssert instanceof Character) {
                    addTo.append("(char)");
                } else if (objectToAssert instanceof Boolean) {
                    addTo.append("(boolean)");
                }
            }
            addTo.append(objectName).append(", ");
        } else if (objectToAssert != null) {
            if (config.ASSUME_EQUALS_METHODS || objectToAssert.getClass().getComponentType().isPrimitive()) {
                addTo.append("assertArrayEquals(").append(objectName).append(", ");
            } else {
                addTo.append(REFLECTION_COMPARE_OBJECTS).append("(").append(objectName).append(", ");
            }
        } else {
            addTo.append("assertNull(");
        }
        addTo.append(assertWith);
        if (objectToAssert != null) {
            if (isFloatingPointClass(objectToAssert.getClass())) {
                addTo.append(", ").append(config.MAX_FP_DELTA);
            }
        }

        addTo.append(")").append(";").append(System.lineSeparator());
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
