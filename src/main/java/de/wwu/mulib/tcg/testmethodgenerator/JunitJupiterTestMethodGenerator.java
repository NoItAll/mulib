package de.wwu.mulib.tcg.testmethodgenerator;

import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.TcgUtility;
import de.wwu.mulib.tcg.TestCase;
import de.wwu.mulib.throwables.MulibIllegalStateException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.wwu.mulib.tcg.TcgUtility.*;

/**
 * Generates executable Junit Jupiter test methods
 */
public class JunitJupiterTestMethodGenerator implements TestMethodGenerator {
    /**
     * The encountered types for importing
     */
    protected final Set<Class<?>> encounteredTypes;
    /**
     * Temporary store for objects and their argument name in the test method.
     * Should be cleared after the String for the TestCase-object was generated.
     */
    protected final Map<Object, String> argumentNamesForObjects;
    /**
     * Temporary stores for object types how many of them are already within a method.
     * Should be cleared after the String for the TestCase-object was generated.
     */
    protected final Map<String, Integer> argumentNameToNumberOfOccurrences;
    /**
     * An identifier for the currently generated test
     */
    protected int numberOfTest = 0;
    /**
     * The String from {@link JunitJupiterTestMethodGenerator#argumentNamesForObjects} for the object calling
     * the method under test if the method under test is not static
     */
    protected String objectCalleeIdentifier;
    /**
     * The test case to generate a representation of
     */
    protected TestCase testCase;
    /**
     * The name of the class declaring the method under test
     */
    protected final String nameOfTestedClass;
    /**
     * The name of the method under test
     */
    protected final String nameOfTestedMethod;
    /**
     * The method under test
     */
    protected final Method testedMethod;
    /**
     * The configuration
     */
    protected final TcgConfig config;

    /**
     * @param testedMethod The method under test
     * @param config The configuration
     */
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

    /**
     * Is executed after generating the test case representation.
     * Resets temporary variables such as {@link JunitJupiterTestMethodGenerator#argumentNamesForObjects}
     */
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

    /**
     * Executes several steps to generate the representation
     * @return The representation of the method under test
     */
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

    /**
     * @return The annotation for the test method
     */
    protected String generateTestMethodAnnotations() {
        if (testCase.isExceptional()) {
            return generateTestAnnotationForException((Exception) testCase.getReturnValue());
        } else {
            return generateTestAnnotationForReturn();
        }
    }

    private String generateTestAnnotationForException(Exception e) {
        return "@Test(expected=" + e.getClass().getName() + ".class)" + System.lineSeparator();
    }

    private String generateTestAnnotationForReturn() {
        return "@Test" + System.lineSeparator();
    }

    /**
     * @return The method declaration of the method testing the method under test
     */
    protected String generateTestMethodDeclaration() {
        return "public void test_" + nameOfTestedMethod + "_" + numberOfTest + "() {" + System.lineSeparator();
    }

    /**
     * @return An end to the method, e.g., "}"
     */
    protected String generateTestMethodEnd() {
        return "}" + System.lineSeparator();
    }

    /**
     * @return A String initializing the single inputs
     */
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

    /**
     * @param o The object to initialize
     * @return A String initializing an object, if such a String was not already created, else returns an empty StringBuilder
     */
    protected StringBuilder generateElementString(Object o) {
        if (o == null || isAlreadyCreated(o)) {
            return new StringBuilder(); // No element string has to be generated for null or an already created element
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

    /**
     * @param o An array
     * @return A String initializing an array and storing its values
     */
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
                .append("];")
                .append(System.lineSeparator());

        return appendStoreInArray(o, sb, arrayName);
    }

    private static int getDimensionsOfArray(Class<?> arClass) {
        Class<?> current = arClass;
        int i = 0;
        while (current.isArray()) {
            i++;
            current = current.getComponentType();
        }
        return i;
    }

    /**
     * @param o The array
     * @return A String generating a multi-dimensional array and setting its content
     */
    protected StringBuilder generateMultiArrayString(Object o) {
        StringBuilder sb = new StringBuilder();
        String arrayName = argumentNamesForObjects.get(o);
        String type = getInnerSimpleTypeForArrayOrSimpleType(o);
        int length = Array.getLength(o);
        int dimensionsOfArray = getDimensionsOfArray(o.getClass());
        if (dimensionsOfArray < 2) {
            throw new MulibIllegalStateException("Dimensions of array must be >= 2 to generate a multi-array string.");
        }
        sb.append(config.INDENT.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(" ")
                .append(arrayName)
                .append(" = new ")
                .append(type)
                .append("[")
                .append(length)
                .append("]")
                .append("[]".repeat(dimensionsOfArray - 1))
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

    /**
     * Adds a type that was encountered while creating the test representation
     * @param oc The type
     */
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


    private boolean isAlreadyCreated(Object o) {
        return argumentNamesForObjects.containsKey(o);
    }

    /**
     * @param o The object representing a primitive
     * @return A String initializing a primitive value
     */
    protected StringBuilder generatePrimitiveString(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.INDENT.repeat(2))
                .append(o.getClass().getSimpleName())
                .append(argumentNamesForObjects.get(o))
                .append(" = ").append(o).append(";").append(System.lineSeparator());
        return sb;
    }

    /**
     * @param o The wrapping object
     * @return A String initializing a wrapping object
     */
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

    private boolean wrappingClassNeedsCast(Class<?> oc) {
        return oc.equals(Float.class) || oc.equals(Short.class)
                || oc.equals(Byte.class) || oc.equals(Character.class);
    }

    private String addCastIfNeeded(Class<?> oc) {
        if (wrappingClassNeedsCast(oc)) {
            // Float, Byte, or Short
            return "(" + toFirstLower(oc.getSimpleName()) + ") ";
        } else {
            return "";
        }
    }

    /**
     * @param o The object to name
     * @return A new name for o, if o has not yet been named, else the known name is returned
     */
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

    /**
     * @param type The type
     * @return A name for a type
     */
    protected String getArgumentNameForType(Class<?> type) {
        if (type.isArray()) {
            return toFirstLower(getArgumentNameForType(type.getComponentType())) + "Ar";
        } else {
            return toFirstLower(type.getSimpleName());
        }
    }

    /**
     * @param o The object
     * @return A String representing the initialization of a non-primitive and a non-wrapper object, array, or String.
     * If this is a special case {@link JunitJupiterTestMethodGenerator#generateSpecialCaseString(Object)} is called
     */
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

    private boolean isSpecialCase(Class<?> objectClass) {
        for (Class<?> specialCase : config.SPECIAL_CASES) {
            if (specialCase.isAssignableFrom(objectClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This must be implemented by overriding JunitJupiterTestMethodGenerator
     * @param o The object
     * @return A String initializing a type that was specified {@link TcgConfig#SPECIAL_CASES}
     */
    protected StringBuilder generateSpecialCaseString(Object o) {
        if (o instanceof Collection) {
            return treatCollectionSpecialCase((Collection<?>) o);
        } else if (o instanceof Map) {
            return treatMapSpecialCase((Map<?, ?>) o);
        }
        throw new UnsupportedOperationException("Aside from Collection and Map there currently are no special cases " +
                "for the StdTestCaseGenerator yet.");
    }

    private StringBuilder treatCollectionSpecialCase(Collection<?> c) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateConstructionString(c));
        String collectionName = argumentNamesForObjects.get(c);
        for (Object o : c) {
            sb.append(generateElementString(o));
            sb.append(collectionName).append(".add(").append(argumentNamesForObjects.get(o)).append(");").append(System.lineSeparator());
        }
        return sb;
    }

    private StringBuilder treatMapSpecialCase(Map<?, ?> m) {
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

    private StringBuilder generateStringString(Object o) {
        return new StringBuilder(config.INDENT.repeat(2))
                .append("String ")
                .append(argumentNamesForObjects.get(o))
                .append(" = \"")
                .append(o.toString())
                .append("\";").append(System.lineSeparator());
    }

    private StringBuilder generateConstructionString(Object o) {
        if (config.ASSUME_PUBLIC_ZERO_ARGS_CONSTRUCTORS) {
            return generateConstructionStringWithZeroArgsConstructor(o);
        } else {
            return generateConstructionStringWithReflection(o);
        }
    }

    private StringBuilder generateConstructionStringWithZeroArgsConstructor(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName()).append(" ");
        sb.append(argumentNamesForObjects.get(o)).append(" = ");
        sb.append("new ").append(o.getClass().getSimpleName()).append("();").append(System.lineSeparator());
        return sb;
    }

    private StringBuilder generateConstructionStringWithReflection(Object o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getClass().getSimpleName()).append(" ")
                .append(argumentNamesForObjects.get(o))
                .append(" = (").append(o.getClass().getSimpleName()).append(") ")
                .append(REFLECTION_NEW_INSTANCE).append("(").append(o.getClass().getSimpleName()).append(".class);").append(System.lineSeparator());
        return sb;
    }

    private String generateAttributesStrings(Object o) {
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

    /**
     * @param f The field
     * @return true, if this field should not be set
     */
    protected boolean skipField(Field f) {
        return f.getName().contains("jacoco");
    }

    private String generateSetStatementForObject(String objectArgumentName, Object o, Field f) {
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

    private StringBuilder generateStringForReturnValue() {
        return generateElementString(testCase.getReturnValue());
    }

    /**
     * @return A String asserting in which the correctness of the input objects values is asserted.
     */
    protected String generateAssertionString() {
        String[] inputObjectNames = new String[this.testCase.getInputs().length];
        for (int i = 0; i < inputObjectNames.length; i++) {
            inputObjectNames[i] = argumentNamesForObjects.get(this.testCase.getInputs()[i]);
        }
        StringBuilder sb = new StringBuilder();
        if (!testedMethod.getReturnType().equals(void.class)) {
            String methodUnderTestCall = generateTestedMethodCallString(inputObjectNames);
            sb.append(config.INDENT.repeat(2)).append("/* Assert correctness of the output value */").append(System.lineSeparator());
            addAssertion(sb, this.testCase.getReturnValue(), methodUnderTestCall);
            sb.append(System.lineSeparator());
        }

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

    private void addAssertion(StringBuilder addTo, Object objectToAssert, String assertWith) {
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

    /**
     * @param inputObjectNames The names of input objects
     * @return A String in which the method under test is called with the inputs
     */
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
