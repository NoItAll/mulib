package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;
import static de.wwu.mulib.transformations.TransformationUtility.determineNestHostFieldName;
import static de.wwu.mulib.transformations.TransformationUtility.getClassForPath;
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
    protected final boolean overWriteFileForSystemClassLoader;

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
        this.overWriteFileForSystemClassLoader = config.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
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
                if (!usedLoadedAndAlreadyWrittenVersion.contains(entry.getKey())) {
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

    /**
     * Transforms type. Arrays are transformed to their respective subclass of Sarray.
     * @param toTransform Type to transform
     * @param sarraysToRealArrayTypes Should, e.g., Sint[].class be returned insted of SintSarray?
     * @return Transformed type
     */
    @Override
    public Class<?> transformType(Class<?> toTransform, boolean sarraysToRealArrayTypes) {
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
            } else {
                assert componentType != char.class;
                return sarraysToRealArrayTypes ? Array.newInstance(transformType(componentType), 0).getClass() : Sarray.PartnerClassSarray.class;
            }
        } else {
            return getPossiblyTransformedClass(toTransform);
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
    private final Set<String> usedLoadedAndAlreadyWrittenVersion = new HashSet<>();
    // Transforms one class. Checks whether the class is ignored and whether the class has already been transformed.
    protected void transformClass(Class<?> toTransform) {
        if (!(classLoader instanceof MulibClassLoader)) {
            try {
                synchronized (syncObject) {
                    if (!overWriteFileForSystemClassLoader) {
                        String transformedName = addPrefixToName(toTransform.getName());
                        Class<?> loadedClass = getClass().getClassLoader().loadClass(transformedName);
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

    public abstract MulibClassFileWriter<T> generateMulibClassFileWriter();

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