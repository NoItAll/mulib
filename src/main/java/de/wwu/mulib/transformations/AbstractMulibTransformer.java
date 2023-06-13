package de.wwu.mulib.transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.model.ModelMethods;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.lang.reflect.*;
import java.util.*;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;
import static de.wwu.mulib.transformations.TransformationUtility.*;

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
    protected final Set<String> ignoreFromPackages;
    protected final Set<Class<?>> ignoreClasses;
    protected final Set<Class<?>> ignoreSubclassesOf;
    protected final Set<Class<?>> regardSpecialCase;
    protected final Map<Method, Method> replaceMethodCallOfNonSubstitutedClassWith;
    protected final Map<Class<?>, Class<?>> replaceToBeTransformedClassWithSpecifiedClass;
    protected final Queue<Class<?>> classesToTransform = new ArrayDeque<>();
    protected final Set<Class<?>> explicitlyAddedClasses = new HashSet<>();
    protected final Set<Class<?>> concretizeFor;
    protected final Set<Class<?>> generalizeMethodCallsFor;
    protected final boolean overWriteFileForSystemClassLoader;

    // original class -> class transformed for symbolic execution
    protected final Map<String, Class<?>> transformedClasses = new HashMap<>();
    protected final MulibConfig config;
    protected final Map<String, T> transformedClassNodes = new HashMap<>();
    protected final Collection<Field> accessibleStaticFieldsOfTransformedClasses = new HashSet<>();

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
        this.overWriteFileForSystemClassLoader = config.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
        this.config = config;
        this.replaceToBeTransformedClassWithSpecifiedClass = config.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS;
        Map<Method, Method> replacementMethods = new HashMap<>(config.TRANSF_REPLACE_METHOD_CALL_OF_NON_SUBSTITUTED_CLASS_WITH);
        if (config.TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH) {
            replacementMethods.putAll(ModelMethods.readDefaultModelMethods(this));
        }
        this.replaceMethodCallOfNonSubstitutedClassWith = replacementMethods;
    }

    /* PUBLIC METHODS */

    public static Collection<Field> getAccessibleStaticFields(Class<?> c) {
        Set<Field> result = new HashSet<>();
        Field[] fs = c.getDeclaredFields();
        for (Field f : fs) {
            if (Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            if (Modifier.isStatic(f.getModifiers())) {
                if (!f.trySetAccessible()) {
                    Mulib.log.warning("Setting static field " + f.getName() + " in "
                            + f.getDeclaringClass().getName() + " failed. It will not be regarded while backtracking!");
                    continue;
                }
                result.add(f);
            }
        }
        return result;
    }

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

            // Replace GETFIELD and PUTFIELD
            // This must happen now, since only now we have assured that the respective methods have been generated for
            // each class
            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                T classNode = getClassNodeForName(entry.getKey());
                // Replace accesses to fields with methods conducting mor checks
                replaceGetFieldsAndPutFieldsWithGeneratedMethods(classNode, entry.getValue());
                replaceStaticFieldInsnsWithGeneratedMethods(classNode, entry.getValue());
                // Insert method calls checking whether 'this' is null into each method. This is done to account for
                // symbolic aliasing
                generateNullChecksForMethods(classNode, entry.getValue());
            }


            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                maybeCheckIsValidWrittenClassNode(entry.getValue());
                // Optionally, conduct some checks and write class node to class file
                if (!usedLoadedAndAlreadyWrittenVersion.contains(entry.getKey())) {
                    maybeWriteToFile(entry.getValue());
                }
            }

            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
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

            for (Map.Entry<String, Class<?>> entry : transformedClasses.entrySet()) {
                // Get static fields
                Collection<Field> staticFieldsOfTransformedClass = getAccessibleStaticFields(entry.getValue());
                if (staticFieldsOfTransformedClass.isEmpty()) {
                    continue;
                }
                accessibleStaticFieldsOfTransformedClasses.addAll(staticFieldsOfTransformedClass);
            }

            maybeCheckAreValidInitializedClasses(transformedClasses.values());
        }
    }

    private Map<Field, Field> getFieldsOfTransformedClassesToOriginalClasses(
            Collection<Field> accessibleStaticFieldsOfTransformedClasses) {
        Map<Field, Field> result = new HashMap<>();
        try {
            for (Field f : accessibleStaticFieldsOfTransformedClasses) {
                if (f.getName().contains(_TRANSFORMATION_PREFIX)) {
                    continue;
                }
                Class<?> originalClass = classLoader.loadClass(f.getDeclaringClass().getName().replace(_TRANSFORMATION_PREFIX, ""));
                Field of = originalClass.getDeclaredField(f.getName());
                result.put(f, of);
            }
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new MulibRuntimeException(e);
        }
        return result;
    }

    @Override
    public Map<Field, Field> getAccessibleStaticFieldsOfTransformedClassesToOriginalClasses() {
        if (tryUseSystemClassLoader && !overWriteFileForSystemClassLoader) {
            // In this case we might not have added all transformed classes into the map. They were loaded by the
            // class loader implicitly
            Map<Field, Field> result = new HashMap<>();
            for (Map.Entry<String, Class<?>> entry : transformedClasses.entrySet()) {
                Collection<Field> staticFields = getAccessibleStaticFieldsAndConnectedAccessibleStaticFields(entry.getValue());
                result.putAll(getFieldsOfTransformedClassesToOriginalClasses(staticFields));
            }
            return result;
        } else {
            return getFieldsOfTransformedClassesToOriginalClasses(accessibleStaticFieldsOfTransformedClasses);
        }
    }

    @Override
    public Collection<Field> getAccessibleStaticFieldsAndConnectedAccessibleStaticFields(Class<?> clazz) {
        synchronized (syncObject) {
            Collection<Field> result = new HashSet<>();
            T classNode = getClassNodeForName(clazz.getName());
            Collection<T> classNodes = getConnectedClassNodes(classNode);
            for (T t : classNodes) {
                String name = getNameToLoadOfClassNode(t);
                Class<?> c = TransformationUtility.getClassForName(name);
                result.addAll(getAccessibleStaticFields(c));
            }
            return result;
        }
    }

    protected abstract Collection<T> getConnectedClassNodes(T classNode);

    /**
     * Transforms type. Arrays are transformed to their respective subclass of Sarray.
     * @param toTransform Type to transform
     * @param sarraysToRealArrayTypes Should, e.g., Sint[].class be returned instead of SintSarray?
     * @return Transformed type
     */
    @Override
    public Class<?> transformType(Class<?> toTransform, boolean sarraysToRealArrayTypes) {
        synchronized (syncObject) {
            if (toTransform == null) {
                throw new MulibRuntimeException("Type to transform must not be null.");
            }
            if (SubstitutedVar.class.isAssignableFrom(toTransform)) {
                return toTransform;
            }
            if (toTransform == int.class) {
                return Sint.class;
            } else if (toTransform == long.class) {
                return Slong.class;
            } else if (toTransform == double.class) {
                return Sdouble.class;
            } else if (toTransform == float.class) {
                return Sfloat.class;
            } else if (toTransform == short.class) {
                return Sshort.class;
            } else if (toTransform == byte.class) {
                return Sbyte.class;
            } else if (toTransform == boolean.class) {
                return Sbool.class;
            } else if (toTransform == char.class) {
                return Schar.class;
            } else if (toTransform == String.class) {
                return String.class; // TODO Free Strings
            } else if (toTransform.isArray()) {
                Class<?> componentType = toTransform.getComponentType();
                if (componentType.isArray()) {
                    if (sarraysToRealArrayTypes) {
                        int nesting = 1; // Already is outer array
                        while (componentType.isArray()) {
                            nesting++; // Always at least one
                            componentType = componentType.getComponentType();
                        }
                        @SuppressWarnings("redundant")
                        Class<?> transformedInnermostComponentType = transformType(componentType);
                        Class<?> result = transformedInnermostComponentType;
                        // Now wrap the innermost transformed component type in arrays
                        for (int i = 0; i < nesting; i++) {
                            result = Array.newInstance(result, 0).getClass();
                        }
                        return result;
                    } else {
                        return Sarray.SarraySarray.class;
                    }
                } else if (componentType == int.class) {
                    return sarraysToRealArrayTypes ? Sint[].class : Sarray.SintSarray.class;
                } else if (componentType == long.class) {
                    return sarraysToRealArrayTypes ? Slong[].class : Sarray.SlongSarray.class;
                } else if (componentType == double.class) {
                    return sarraysToRealArrayTypes ? Sdouble[].class : Sarray.SdoubleSarray.class;
                } else if (componentType == float.class) {
                    return sarraysToRealArrayTypes ? Sfloat[].class : Sarray.SfloatSarray.class;
                } else if (componentType == short.class) {
                    return sarraysToRealArrayTypes ? Sshort[].class : Sarray.SshortSarray.class;
                } else if (componentType == boolean.class) {
                    return sarraysToRealArrayTypes ? Sbool[].class : Sarray.SboolSarray.class;
                } else if (componentType == byte.class) {
                    return sarraysToRealArrayTypes ? Sbyte[].class : Sarray.SbyteSarray.class;
                } else if (componentType == char.class) {
                    return sarraysToRealArrayTypes ? Schar[].class : Sarray.ScharSarray.class;
                } else {
                    return sarraysToRealArrayTypes ? Array.newInstance(transformType(componentType), 0).getClass() : Sarray.PartnerClassSarray.class;
                }
            } else {
                return getPossiblyTransformedClass(toTransform);
            }
        }
    }

    @Override
    public Class<?> transformMulibTypeBack(Class<?> toTransform) {
        if (toTransform == null) {
            throw new MulibRuntimeException("Type to transform must not be null.");
        }
        if (!SubstitutedVar.class.isAssignableFrom(toTransform)) {
            return toTransform;
        }
        if (toTransform == Sint.class) {
            return int.class;
        } else if (toTransform == Slong.class) {
            return long.class;
        } else if (toTransform == Sdouble.class) {
            return double.class;
        } else if (toTransform == Sfloat.class) {
            return float.class;
        } else if (toTransform == Sshort.class) {
            return short.class;
        } else if (toTransform == Sbyte.class) {
            return byte.class;
        } else if (toTransform == Sbool.class) {
            return boolean.class;
        } else if (toTransform == Schar.class) {
            return char.class;
        } else if (toTransform == String.class) {
            return String.class; // TODO Free Strings
        } else if (Sarray.class.isAssignableFrom(toTransform)) {
            if (toTransform == Sarray.SintSarray.class || toTransform == Sint[].class) {
                return int[].class;
            } else if (toTransform == Sarray.SlongSarray.class) {
                return long[].class;
            } else if (toTransform == Sarray.SdoubleSarray.class) {
                return double[].class;
            } else if (toTransform == Sarray.SfloatSarray.class) {
                return float[].class;
            } else if (toTransform == Sarray.SshortSarray.class) {
                return short[].class;
            } else if (toTransform == Sarray.SbyteSarray.class) {
                return byte[].class;
            } else if (toTransform == Sarray.SboolSarray.class) {
                return boolean[].class;
            } else if (toTransform == Sarray.ScharSarray.class) {
                return char[].class;
            } else {
                assert Sarray.PartnerClassSarray.class.isAssignableFrom(toTransform);
                throw new NotYetImplementedException();
            }
        } else if (toTransform.isArray()) {
            if (toTransform == Sint[].class) {
                return int[].class;
            } else if (toTransform == Slong[].class) {
                return long[].class;
            } else if (toTransform == Sdouble[].class) {
                return double[].class;
            } else if (toTransform == Sfloat[].class) {
                return float[].class;
            } else if (toTransform == Sshort[].class) {
                return short[].class;
            } else if (toTransform == Sbyte[].class) {
                return byte[].class;
            } else if (toTransform == Sbool[].class) {
                return boolean[].class;
            } else if (toTransform == Schar[].class) {
                return char[].class;
            } else {
                return Array.newInstance(transformMulibTypeBack(toTransform.getComponentType()), 0).getClass();
            }
        } else {
            return getNonTransformedClassFromPartnerClass(toTransform);
        }
    }

    private Class<?> getNonTransformedClassFromPartnerClass(Class<?> c) {
        String name = c.getName();
        String nameWithoutTransformationPrefix = name.replace(_TRANSFORMATION_PREFIX, "");
        try {
            return Class.forName(nameWithoutTransformationPrefix);
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException(e);
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
            try {
                result = Class.forName(addPrefixToName(beforeTransformation.getName()));
                transformedClasses.put(beforeTransformation.getName(), result);
                return result;
            } catch (Exception e) {}
            return beforeTransformation;
        }
        return result;
    }

    @Override
    public void setPartnerClass(Class<?> original, Class<?> partnerClass) {
        transformedClasses.put(original.getName(), partnerClass);
    }

    @Override
    public final Collection<Field> getAccessibleStaticFieldsOfTransformedClasses() {
        return accessibleStaticFieldsOfTransformedClasses;
    }

    public String addPrefixToPath(String addTo) {
        return _addPrefix(false, addTo);
    }

    public String addPrefixToName(String addTo) {
        Class<?> originalClass = getClassForName(addTo);
        String modelClassOrOriginal = replaceToBeTransformedClassWithSpecifiedClass.getOrDefault(originalClass, originalClass).getName();
        return _addPrefix(true, modelClassOrOriginal);
    }

    private static String _addPrefix(boolean useDot, String addTo) {
        if (addTo == null) {
            return null;
        }
        if (addTo.startsWith("java")) {
            // Starting with "java" is forbidden
            addTo = _TRANSFORMATION_PREFIX + addTo;
        }
        int actualNameIndex = addTo.lastIndexOf(useDot ? '.' : '/') + 1;
        String packageName = addTo.substring(0, actualNameIndex);
        String actualName = addTo.substring(actualNameIndex);
        String[] innerClassSplit = actualName.split("\\$");
        StringBuilder resultBuilder = new StringBuilder(packageName);
        for (int i = 0; i < innerClassSplit.length; i++) {
            String s = innerClassSplit[i];
            resultBuilder.append(_TRANSFORMATION_PREFIX)
                    .append(s);
            if (i < innerClassSplit.length - 1) {
                resultBuilder.append('$');
            }
        }
        return resultBuilder.toString();
    }

    protected abstract boolean isInterface(T classNode);

    protected abstract T getClassNodeForName(String name);

    protected final T transformEnrichAndValidate(String toTransformName) {
        Class<?> toTransform = getClassForName(toTransformName);
        Class<?> replacement = replaceToBeTransformedClassWithSpecifiedClass.get(toTransform);
        if (replacement != null) {
            toTransformName = replacement.getName();

        }
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
            // Method for generating the id-, representationState-, and isNull-field
            decideOnGenerateIdAndStateAndIsNullFieldAndMethods(originalCn, result);
            // Replace GETFIELD and PUTFIELD with methods to add additional logic. Generate those methods here
            generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(originalCn, result);
            // Insert instructions for checking for null and setting to neutral element, if not yet initialized
            ensureInitializedLibraryTypeFieldsInConstructors(result);
        }
        return result;
    }

    protected abstract void generateNullChecksForMethods(T old, T result);

    protected abstract void decideOnGenerateIdAndStateAndIsNullFieldAndMethods(T old, T result);

    protected abstract void generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(T old, T result);

    protected abstract void replaceGetFieldsAndPutFieldsWithGeneratedMethods(T old, T result);

    protected abstract void replaceStaticFieldInsnsWithGeneratedMethods(T old, T result);


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
        // TODO "/" in classAsPath is not really always respected but does not disturb since it is later replaced via '.' anyway. Still, this is unclean
        Class<?> c = getClassForPath(classAsPath);
        return !isIgnored(c);
    }

    /* TO OVERRIDE */
    protected abstract MulibClassLoader<T> generateMulibClassLoader();

    // Check if the class, represented by the path, is about to be transformed or has already been transformed.
    protected boolean isAlreadyTransformedOrToBeTransformedPath(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath);
        return classesToTransform.contains(c) // Should not be already enqueued for transformation
                || transformedClasses.containsKey(c.getName()); // Should not already be transformed
    }

    protected void decideOnWhetherStillNeedsToBeAddedToTransformationQueue(String name) {
        if (shouldBeTransformed(name) && isAlreadyTransformedOrToBeTransformedPath(name)) {
            addToClassesToTransform(name);
        }
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
    public Class<?> getTransformedClassForOriginalClassName(String originalClassName) {
        return transformedClasses.get(originalClassName);
    }

    public Class<?> getTransformedClassForTransformedClassName(String transformedClassName) {
        for (Map.Entry<String, Class<?>> e : transformedClasses.entrySet()) {
            if (e.getValue().getName().equals(transformedClassName)) {
                return e.getValue();
            }
        }
        throw new MulibRuntimeException("Class not found");
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
    private final Set<String> usedLoadedAndAlreadyWrittenVersion = new HashSet<>();
    // Transforms one class. Checks whether the class is ignored and whether the class has already been transformed.
    protected final void transformClass(Class<?> toTransform) {
        if (!(classLoader instanceof MulibClassLoader)) {
            try {
                synchronized (syncObject) {
                    if (!overWriteFileForSystemClassLoader) {
                        String transformedName = addPrefixToName(toTransform.getName());
                        Class<?> loadedClass = classLoader.loadClass(transformedName);
                        // If loading succeeded there already is a class file in the build
                        transformedClasses.putIfAbsent(toTransform.getName(), loadedClass);
                        transformedClassNodes.putIfAbsent(toTransform.getName(), getClassNodeForName(transformedName));
                        usedLoadedAndAlreadyWrittenVersion.add(toTransform.getName());
                        return;
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (ClassFormatError e) {
                throw new MulibRuntimeException(e);
            }
        }
        if (isIgnored(toTransform) || isAlreadyTransformedOrToBeTransformedPath(toTransform.getName())) {
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

    public abstract MulibClassFileWriter<T> generateMulibClassFileWriter();

    /* PROTECTED METHODS FOR TRANSFORMING PARAMETERS AND TYPES */

    protected void addToClassesToTransform(String path) {
        if (path.startsWith("[")) {
            path = path.substring(path.lastIndexOf('[') + 1);
        }
        if (path.startsWith("L")) {
            assert path.endsWith(";");
            path = path.substring(1, path.length() - 1);
        }
        Class<?> c = getClassForPath(path);
        classesToTransform.add(c);
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