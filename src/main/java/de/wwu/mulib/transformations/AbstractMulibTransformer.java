package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TransformationUtility.*;
import static de.wwu.mulib.transformations.soot_transformations.SootMulibTransformer.addPrefixToName;

/**
 * Core piece of Mulib. The MulibTransformer accepts classes and generates partner classes for them, according
 * to a specified configuration.
 */
public abstract class AbstractMulibTransformer<T> implements MulibTransformer {
    // For synchronizing file writes
    protected static final Object syncObject = new Object();
    protected final String generatedClassesPathPattern;
    protected final boolean writeToFile;
    protected final boolean validate;
    protected final boolean tryUseSystemClassLoader;
    protected final boolean includePackageName;
    protected final List<String> ignoreFromPackages;
    protected final List<Class<?>> ignoreClasses;
    protected final List<Class<?>> ignoreSubclassesOf;
    protected final List<Class<?>> regardSpecialCase;
    protected final Queue<Class<?>> classesToTransform = new ArrayDeque<>();
    protected final Set<Class<?>> explicitlyAddedClasses = new HashSet<>();
    protected final List<Class<?>> concretizeFor;
    protected final List<Class<?>> generalizeMethodCallsFor;

    // original class -> class transformed for symbolic execution
    protected final Map<String, Class<?>> transformedClasses = new HashMap<>();
    protected final MulibConfig config;
    protected final Map<String, T> transformedClassNodes = new HashMap<>();


    protected final ClassLoader classLoader;

    /**
     * Constructs an instance of MulibTranformer according to the configuration.
     * @param config The configuration.
     */
    public AbstractMulibTransformer(MulibConfig config) {
        this.classLoader = config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER ?
                ClassLoader.getSystemClassLoader() :
                generateMulibClassLoader();
        this.tryUseSystemClassLoader = config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        this.writeToFile = config.TRANSF_WRITE_TO_FILE;
        this.validate = config.TRANSF_VALIDATE_TRANSFORMATION;
        String generatedClassesPath = config.TRANSF_GENERATED_CLASSES_PATH;
        this.generatedClassesPathPattern = generatedClassesPath + "%s.class";
        this.ignoreFromPackages = config.TRANSF_IGNORE_FROM_PACKAGES;
        this.ignoreClasses = config.TRANSF_IGNORE_CLASSES;
        this.ignoreSubclassesOf = config.TRANSF_IGNORE_SUBCLASSES_OF;
        this.regardSpecialCase = config.TRANSF_REGARD_SPECIAL_CASE;
        this.concretizeFor = config.TRANSF_CONCRETIZE_FOR;
        this.generalizeMethodCallsFor = config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        this.includePackageName = config.TRANSF_INCLUDE_PACKAGE_NAME;
        this.config = config;
    }

    /* PUBLIC METHODS */

    /**
     * Transforms the specified classes. Even if the passed classes are ignored according to the configuration,
     * they will be transformed as they have been explicitly stated to be transformed.
     * These classes that are dependencies for the specified classes are transformed according to the configuration
     * of the MulibTransformer instance.
     * @param toTransform Those classes that are transformed, even if they have been set to be ignored.
     */
    public void transformAndLoadClasses(Class<?>... toTransform) {
        synchronized (syncObject) {
            List<Class<?>> definitelyTransform = Arrays.asList(toTransform);
            explicitlyAddedClasses.addAll(definitelyTransform);
            classesToTransform.addAll(definitelyTransform);

            while (!classesToTransform.isEmpty()) {
                transformClass(classesToTransform.poll());
            }


            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                // Optionally, conduct some checks and write class node to class file
                if (!usedLoadedVersion.contains(entry.getKey())) {
                    maybeWriteToFile(entry.getValue());
                }
                maybeCheckIsValidWrittenClassNode(entry.getValue());
                if (transformedClasses.get(entry.getKey()) != null) {
                    continue;
                }
                try {
                    Class<?> result = classLoader.loadClass(getNameToLoadOfClassNode(entry.getValue()));
                    transformedClasses.put(entry.getKey(), result);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new MulibRuntimeException(e);
                }
            }

            maybeCheckAreValidInitializedClasses(transformedClasses.values());
        }
    }

    protected abstract String getNameToLoadOfClassNode(T classNode);

    /**
     * Returns the partner class for the given class.
     * @param beforeTransformation The class the partner class of which is to be returned.
     * @return The partner class.
     */
    public Class<?> getTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            throw new MulibRuntimeException("Class has not been transformed: " + beforeTransformation);
        }
        return result;
    }

    /**
     * @param beforeTransformation The class before transforming it into the partner class.
     * @return Either the partner class, or, if not available, the original class.
     */
    public Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            return beforeTransformation;
        }
        return result;
    }

    public void setPartnerClass(Class<?> original, Class<?> partnerClass) {
        transformedClasses.put(original.getName(), partnerClass);
    }

    protected abstract boolean isInterface(T classNode);

    protected abstract T getClassNodeForName(String name);

    protected T transformEnrichAndValidate(String toTransformName) {
        T result;
        if ((result = this.transformedClassNodes.get(toTransformName)) != null) {
            return result;
        }
        T originalCn = getClassNodeForName(toTransformName);
        result = transformClassNode(originalCn);
        transformedClassNodes.put(toTransformName, result);
        if (!isInterface(originalCn)) {
            // Synthesize additional constructors
            // Constructor for free object-initialization:
            // We do not need to ensure that all previously primitive fields are properly initialized since
            // each field is set to its symbolic value.
            generateAndAddSymbolicExecutionConstructor(originalCn, result);
            // Constructor for translating input objects to the Mulib-representation
            // We do not need to ensure that all previously primitive fields are properly initialized since we get the
            // values from primitive fields which, per default, have the value 0
            generateAndAddTransformationConstructor(originalCn, result);
            // Constructor for copying input objects, transformed by the transformationConstructor
            // We do not need to ensure that all previously primitive fields are properly initialized since this should
            // already hold for the copied object.
            generateAndAddCopyConstructor(originalCn, result);
            // Method for copying called-upon object
            generateAndAddCopyMethod(originalCn, result);
            // Method for labeling original object
            generateAndAddLabelTypeMethod(originalCn, result);
            // Method for returning original type's class
            generateAndAddOriginalClassMethod(originalCn, result);
            // Method for setting static fields, if any
            generateOrEnhanceClinit(originalCn, result);
        }
        ensureInitializedLibraryTypeFieldsInConstructors(result);

        return result;
    }

    /**
     * Adds a constructor for symbolically initializing a new instance of a class <init>(LSymbolicExecution;)
     * @param old The old class
     * @param result The new class to which the symbolic initialization-constructor should be added
     */
    protected abstract void generateAndAddSymbolicExecutionConstructor(T old, T result);

    /**
     * Generates a constructor for transforming an object of the class old to an object of type result
     * <init>(LobjectOfOriginalClass;LMulibValueTransformer;)
     * @param old The to-be-transformed class
     * @param result The class to an object of which the object of old should be transformed
     */
    protected abstract void generateAndAddTransformationConstructor(T old, T result);

    /**
     * Generates a constructor with the parameter types <init>(LobjectOfPartnerClass;LMulibValueTransformer;)
     * @param old The old class
     * @param result The new class to which the new copy constructor should be added
     */
    protected abstract void generateAndAddCopyConstructor(T old, T result);

    /**
     * Adds a method for calling the copy constructor
     * @param old The old class
     * @param result The new class to which said method should be added
     * @see AbstractMulibTransformer#generateAndAddCopyConstructor(Object, Object)
     */
    protected abstract void generateAndAddCopyMethod(T old, T result);

    /**
     * Adds a method to label an object of the transformed class, i.e., transform an object of the type of result to
     * an object of the type of old
     * @param old The old class to an instance of which the object of the result class should be transformed
     * @param result The to-be-transformed class
     */
    protected abstract void generateAndAddLabelTypeMethod(T old, T result);

    /**
     * Adds a method that returns the ClassConstant of the old class
     * @param old The old class, for which the class constant should be returned
     * @param result The new class to which said method should be added
     */
    protected abstract void generateAndAddOriginalClassMethod(T old, T result);

    /**
     * Ensure that all static primitives are initialized to the default element for fields
     * @param old The old class with or without a clinit
     * @param result The new class with a clinit if old had a clinit, else without
     */
    protected abstract void generateOrEnhanceClinit(T old, T result);

    /**
     * Since Java assumes default values for un-initialized primitive fields, we have to check whether
     * we still have to assign a value to those fields in all constructors.
     * @param result The result for which additional safety checks and initializations must be added
     * @see AbstractMulibTransformer#generateOrEnhanceClinit(Object, Object)
     */
    protected abstract void ensureInitializedLibraryTypeFieldsInConstructors(T result);

    /* METHODS FOR CHECKING HOW CLASSES SHOULD BE TREATED. */

    /**
     * Checks whether, given the configuration, the method of the class represented by the given path, should be concretized.
     * @param methodOwner The path of the class.
     * @return true, if the method should be concretized, else false.
     */
    public boolean shouldBeConcretizedFor(String methodOwner) {
        Class<?> c = getClassForPath(methodOwner);
        return shouldBeConcretizedFor(c);
    }

    /**
     * Checks whether, given the configuration, the method of the class represented by the given path, should be generalized.
     * @param methodOwner The path of the class.
     * @return true, if the method should be generalized, else false.
     */
    public boolean shouldTryToUseGeneralizedMethodCall(String methodOwner) {
        Class<?> c = getClassForPath(methodOwner);
        return shouldTryToUseGeneralizedMethodCall(c);
    }

    /**
     * Checks whether, given the configuration, the method of the class, should be concretized.
     * @param methodOwner The class.
     * @return true, if the method should be concretized, else false.
     */
    public boolean shouldBeConcretizedFor(Class<?> methodOwner) {
        return concretizeFor.contains(methodOwner);
    }

    /**
     * Checks whether, given the configuration, the method of the class, should be generalized.
     * @param methodOwner The class.
     * @return true, if the method should be generalized, else false.
     */
    public boolean shouldTryToUseGeneralizedMethodCall(Class<?> methodOwner) {
        return generalizeMethodCallsFor.contains(methodOwner);
    }

    /**
     * Checks whether, according to the configuration, a class should be transformed, i.e., whether a partner
     * class should be generated.
     * @param classAsPath The path of the class.
     * @return true, if the class should be transformed, otherwise false.
     */
    public boolean shouldBeTransformed(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath);
        return !isIgnored(c);
    }

    public boolean shouldBeTransformedFromDesc(String desc) {
        if (desc.startsWith("[")) {
            int last = desc.lastIndexOf('[');
            desc = desc.substring(last + 1);
        }
        if (desc.length() == 1 && primitiveTypes.contains(desc)) {
            return true;
        }
        assert (desc.startsWith("L") && desc.endsWith(";"));
        return shouldBeTransformed(desc.substring(1, desc.length() - 1));
    }

    /* TO OVERRIDE */
    protected abstract MulibClassLoader<T> generateMulibClassLoader();

    // Check if the class, represented by the path, is about to be transformed or has already been transformed.
    protected boolean isAlreadyTransformedOrToBeTransformedPath(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath);
        return classesToTransform.contains(c) // Should not be already enqueued for transformation
                || transformedClasses.containsKey(c.getName()); // Should not already be transformed
    }

    // Checks if a class is ignored according to some configuration. Ignored classes are not transformed.
    protected boolean isIgnored(Class<?> toTransform) {
        return shouldBeConcretizedFor(toTransform)
                || shouldTryToUseGeneralizedMethodCall(toTransform)
                || (
                !explicitlyAddedClasses.contains(toTransform)
                        && regardSpecialCase.stream().noneMatch(x -> x.equals(toTransform))
                        && (ignoreFromPackages.stream().anyMatch(x -> toTransform.getPackageName().startsWith(x)) // (Multi-dimensional) arrays of primitives are treated separately in the transformation
                        || ignoreClasses.stream().anyMatch(x -> x.equals(toTransform))
                        || ignoreSubclassesOf.stream().anyMatch(x -> x.isAssignableFrom(toTransform)))
        );
    }

    /* METHODS FOR INTERACTING WITH CUSTOM CLASSLOADER */
    // Gets the partner class to the class represented by its name.
    public Class<?> getTransformedClass(String className) {
        return transformedClasses.get(className);
    }

    // Gets the ASM representation of the partner class of the class represented by its name.
    public T getTransformedClassNode(String className) {
        return transformedClassNodes.get(className);
    }

    // Adds the transformed class to the map with <class name, partner class>.
    protected void addTransformedClass(String className, Class<?> c) {
        transformedClasses.put(className, c);
    }

    // If set contains toTransformName: partnerclass-class was loaded and does not have to be written anymore
    private Set<String> usedLoadedVersion = new HashSet<>();
    // Transforms one class. Checks whether the class is ignored and whether the class has already been transformed.
    protected void transformClass(Class<?> toTransform) {
        if (!(classLoader instanceof MulibClassLoader)) {
            try {
                synchronized (syncObject) {
                    String transformedName = addPrefixToName(toTransform.getName());
                    Class<?> loadedClass = getClass().getClassLoader().loadClass(transformedName);
                    // If loading succeeded there already is a class file in the build
                    transformedClasses.putIfAbsent(toTransform.getName(), loadedClass);
                    transformedClassNodes.putIfAbsent(toTransform.getName(), getClassNodeForName(transformedName));
                    usedLoadedVersion.add(toTransform.getName());
                    return;
                }
            } catch (ClassNotFoundException ignored) {
            } catch (ClassFormatError e) {
                throw new MulibRuntimeException(e);
            }
        }
        if (isIgnored(toTransform) || transformedClassNodes.containsKey(toTransform.getName())) {
            return;
        }
        transformEnrichAndValidate(toTransform.getName());
    }

    /**
     * Loads the representation T of the class to be transformed. Transforms this representation
     * @param toTransform The representation of the class to be loaded
     * @return the transformed representation
     */
    protected abstract T transformClassNode(T toTransform);

    /* TRANSFORMING CLASS TO PARTNER CLASS */
    protected String calculateSignatureForSarrayIfNecessary(String desc) {
        // Check if is array of arrays or array of objects, in both cases, we want to set a parameter
        if (desc.startsWith("[[") || desc.startsWith("[L")) {
            String result = _calculateSignatureForSarrayIfNecessary(desc);
            return result;
        }
        return null;
    }

    public abstract MulibClassFileWriter<T> generateMulibClassFileWriter();

    private String _calculateSignatureForSarrayIfNecessary(String fieldNodeDesc) {
        // Check if is array of arrays or array of objects, in both cases, we want to set a parameter
        if (fieldNodeDesc.startsWith("[[")) {
            // Is a nested sarray
            return "L" + sarraySarrayCp + "<" + _calculateSignatureForSarrayIfNecessary(fieldNodeDesc.substring(1)) +  ">;";
        } else if (fieldNodeDesc.startsWith("[L")) {
            // Is a PartnerClassSarray
            return "L" + partnerClassSarrayCp + "<" + decideOnReplaceDesc(fieldNodeDesc.substring(1)) + ">;";
        }
        return decideOnReplaceDesc(fieldNodeDesc);
    }

    protected static String transformToSarrayCpWithSarray(String originalArDesc) {
        assert originalArDesc.startsWith("[");
        if (originalArDesc.startsWith("[[")) {
            return sarraySarrayCp;
        }
        if (originalArDesc.startsWith("[L")) {
            return partnerClassSarrayCp;
        }
        if (originalArDesc.startsWith("[I")) {
            return sintSarrayCp;
        }
        if (originalArDesc.startsWith("[J")) {
            return slongSarrayCp;
        }
        if (originalArDesc.startsWith("[D")) {
            return sdoubleSarrayCp;
        }
        if (originalArDesc.startsWith("[F")) {
            return sfloatSarrayCp;
        }
        if (originalArDesc.startsWith("[S")) {
            return sshortSarrayCp;
        }
        if (originalArDesc.startsWith("[B")) {
            return sbyteSarrayCp;
        }
        if (originalArDesc.startsWith("[Z")) {
            return sshortSarrayCp;
        }
        if (originalArDesc.startsWith("[C")) {
            return partnerClassSarrayCp;
        }
        throw new MulibRuntimeException("Array type is not treated: " + originalArDesc);
    }

    protected String transformToSarrayCpWithPrimitiveArray(String originalCp) {
        assert originalCp.startsWith("[");
        int nestingLevel;
        for (nestingLevel = 0; nestingLevel < originalCp.length(); nestingLevel++) {
            if (originalCp.charAt(nestingLevel) != '[') {
                break;
            }
        }
        String result = originalCp.substring(0, nestingLevel);
        result += decideOnReplaceDesc(originalCp.substring(nestingLevel));
        return result;
    }

    // Does NOT return Sarray-descs!
    protected String getTransformedArrayTypeDesc(String arrayTypeDesc) {
        assert arrayTypeDesc.startsWith("[");
        String newTypeDesc = arrayTypeDesc.replace("[", "");
        newTypeDesc = decideOnReplaceDesc(newTypeDesc);
        newTypeDesc = "[".repeat(arrayTypeDesc.lastIndexOf('[') + 1) + newTypeDesc;
        return newTypeDesc;
    }


    /* PROTECTED METHODS FOR TRANSFORMING PARAMETERS AND TYPES */

    protected void decideOnAddToClassesToTransform(String path) {
        if (path.startsWith("[")) {
            path = path.substring(path.lastIndexOf('[') + 1);
        }
        if (path.startsWith("L")) {
            assert path.endsWith(";");
            path = path.substring(1, path.length() - 1);
        }
        classesToTransform.add(getClassForPath(path));
    }

    protected final boolean calculateReflectionRequiredForField(int access) {
        if (Modifier.isStatic(access)) {
            return false;
        }
        if (tryUseSystemClassLoader) {
            return Modifier.isPrivate(access) || Modifier.isFinal(access);
        } else {
            // Otherwise, it is in another module and friendly as well as protected fields cannot be accessed
            // without reflection
            return !Modifier.isPublic(access) || Modifier.isFinal(access);
        }
    }

    // Decide on the value by means of which we replace the given descriptor.
    protected String decideOnReplaceDesc(String currentDesc) {
        char startChar = currentDesc.charAt(0);
        switch (startChar) {
            case 'I': // int
                return sintDesc;
            case 'D': // double
                return sdoubleDesc;
            case 'Z': // boolean
                return sboolDesc;
            case 'J': // long
                return slongDesc;
            case 'F': // float
                return sfloatDesc;
            case 'S': // short
                return sshortDesc;
            case 'B': // byte
                return sbyteDesc;
            case 'C': // char
                throw new NotYetImplementedException();
            case 'V':
                return currentDesc;
            case '[': // array
                char nextChar = currentDesc.charAt(1);
                switch (nextChar) {
                    case 'I': // int
                        return sintSarrayDesc;
                    case 'J': // long
                        return slongSarrayDesc;
                    case 'D': // double
                        return sdoubleSarrayDesc;
                    case 'F': // float
                        return sfloatSarrayDesc;
                    case 'S': // short
                        return sshortSarrayDesc;
                    case 'B': // byte
                        return sbyteSarrayDesc;
                    case 'Z': // boolean
                        return sboolSarrayDesc;
                    case 'L':
                        return partnerClassSarrayDesc;
                    case '[':
                        return sarraySarrayDesc;
                    default:
                        throw new NotYetImplementedException(String.valueOf(nextChar));
                }
            case 'L': // object
                String typeName = currentDesc.substring(1, currentDesc.length() - 1);
                if (shouldBeTransformed(typeName)) {
                    currentDesc = addPrefix(currentDesc);
                }
                return currentDesc;
            default:
                throw new IllegalStateException();
        }
    }

    // Transform a method descriptor.
    protected String transformMethodDesc(String mdesc) {
        String[] splitMethodDesc = splitMethodDesc(mdesc);

        return addPrefixToParameterPart(splitMethodDesc[0]) + decideOnReplaceDesc(splitMethodDesc[1]);
    }

    // Adds the prefix to those parts of the parameter where needed.
    protected String addPrefixToParameterPart(String parameterPart) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parameterPart.length() - 1; i++) { // Without parentheses
            char firstPosition = parameterPart.charAt(i);
            StringBuilder currentDesc = new StringBuilder(String.valueOf(firstPosition));
            while (firstPosition == '[') {
                i++;
                // Get type of array
                currentDesc.append(parameterPart.charAt(i));
                firstPosition = parameterPart.charAt(i);
            }
            if (currentDesc.toString().contains("L")) {
                int endOfObjectDesc = parameterPart.indexOf(';', i);
                i++;
                currentDesc.append(parameterPart, i, endOfObjectDesc + 1);
                i = endOfObjectDesc;
            }
            sb.append(decideOnReplaceDesc(currentDesc.toString()));
        }

        return "(" + sb + ")";
    }

    /* OPTIONALLY EXECUTED METHODS */
    // Evaluates the validity of the class by initializing an instance and thus using the JVM's on-board measures
    // for validation.
    protected void maybeCheckAreValidInitializedClasses(Collection<Class<?>> generatedClasses) {
        if (validate) {
            for (Class<?> generatedClass : generatedClasses) {
                try {
                    if (Modifier.isAbstract(generatedClass.getModifiers())) {
                        continue;
                    }
                    if (generatedClass.getNestHost() != null) {
                        continue;
                    }
                    if (determineNestHostFieldName(generatedClass.getName().replace(_TRANSFORMATION_PREFIX, "")) != null) {
                        Class<?> hostFieldClass = generatedClass.getEnclosingClass();
                        generatedClass.getDeclaredConstructor(new Class[]{hostFieldClass, SymbolicExecution.class}).newInstance(null, null);
                    } else {
                        generatedClass.getDeclaredConstructor(new Class[]{SymbolicExecution.class}).newInstance(new Object[]{null});
                    }
                } catch (NoSuchMethodException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due to missing constructor.", e);
                } catch (IllegalAccessException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to illegal access exception.", e);
                } catch (InstantiationException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to it being an abstract class.", e);
                } catch (InvocationTargetException e) {
                    throw new MulibRuntimeException("Validation of generated class failed due "
                            + "to the constructor throwing an exception.", e);
                }
            }
        }
    }

    // Evaluates the validity of the class by using the ASM CheckClassAdapter. This does not find "everything"
    // but gives better information on what might have gone wrong.
    protected void maybeCheckIsValidWrittenClassNode(T classNode) {
        if (validate) {
            // Following the documentation of CheckClassAdapter from here on
            generateMulibClassFileWriter().validateClassNode(classNode);
        }
    }

    // Writes the class to a file to be evaluated using a decompiler.
    protected void maybeWriteToFile(T classNode) {
        if (writeToFile) {
            synchronized (syncObject) {
                generateMulibClassFileWriter().writeClassToFile(generatedClassesPathPattern, includePackageName, classNode);
            }
        }
    }
}