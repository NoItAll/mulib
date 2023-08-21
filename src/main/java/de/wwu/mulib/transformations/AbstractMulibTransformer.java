package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.model.ModelMethods;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.PartnerClassObject;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.util.Utility;

import java.lang.reflect.*;
import java.util.*;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_INDICATOR;
import static de.wwu.mulib.transformations.TransformationUtility.*;

/**
 * Core piece of Mulib. The MulibTransformer accepts classes and generates partner classes for them, according
 * to a specified configuration.
 */
public abstract class AbstractMulibTransformer<T> implements MulibTransformer {
    /**
     * For instance, to synchronize writing class files to the file system
     */
    protected static final Object syncObject = new Object();
    /**
     * "%s.class"
     */
    protected final String generatedClassesPathPattern;
    /**
     * Contains all those methods that are replaced with specific method calls
     */
    protected final Map<Method, Method> replaceMethodCallWithOtherMethodCall;
    // Queues up all classes that are transformed
    private final Queue<Class<?>> classesToTransform = new ArrayDeque<>();
    // Classes that are explicitly added must be transformed
    private final Set<Class<?>> explicitlyAddedClasses = new HashSet<>();

    /**
     * original class -> class transformed for symbolic execution
     */
    protected final Map<String, Class<?>> transformedClasses = new HashMap<>();
    /**
     * The configuration
     */
    protected final MulibConfig config;
    /**
     * original class name -> bytecode framework specific representation of the class in the search region, i.e., the partner class.
     * Also contains the specially generated sarray subtypes that later end up as classes and can be retrieved via
     * {@link #getArrayTypesToSpecializedSarrayClass()}.
     * For instance ClassNode for ASM or SootClass for Soot.
     * A valid instance should be set as soon as possible to avoid any issues due to cycles. {@link #transformEnrichAndValidate(String)}
     * will put an entry after executing {@link #transformClassNode(Object)}.
     */
    protected final Map<String, T> transformedClassNodes = new HashMap<>();
    /**
     * The class loader used for loading partner classes.
     * If {@link MulibConfig#TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER} is not true, a new class loader is used by Mulib that
     * does not need to write class files to the file system.
     * @see MulibClassLoader
     */
    protected final ClassLoader classLoader;

    /**
     * Constructs an instance of MulibTranformer according to the configuration.
     * @param config The configuration.
     */
    public AbstractMulibTransformer(MulibConfig config) {
        this.classLoader = config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER ?
                ClassLoader.getSystemClassLoader() :
                generateMulibClassLoader();
        String generatedClassesPath = config.TRANSF_GENERATED_CLASSES_PATH;
        this.generatedClassesPathPattern = generatedClassesPath + "%s.class";
        this.config = config;
        Map<Method, Method> replacementMethods = new HashMap<>(config.TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD);
        if (config.TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH) {
            replacementMethods.putAll(ModelMethods.readDefaultModelMethods(this));
        }
        this.replaceMethodCallWithOtherMethodCall = replacementMethods;
        this.addTransformedClass(Object.class.getName(), PartnerClassObject.class);
    }

    /**
     * Transforms the specified classes. Even if the passed classes are ignored according to the configuration,
     * they will be transformed as they have been explicitly stated to be transformed.
     * These classes that are dependencies for the specified classes are transformed according to the {@link MulibConfig}.
     * The classes are first transformed, alongside with all their referenced classes (if not set to be ignored).
     * Then, we replace direct accesses to fields with synthesized method calls. In these method calls
     * techniques like symbolic aliasing, checks for whether this object can in fact be null, etc. are performed.
     * Then, special method treatment according to {@link MulibConfig#TRANSF_TREAT_SPECIAL_METHOD_CALLS} is performed.
     * Thereafter, it is checked whether we should try to load classes using the system class loader and/or write them
     * to disk.
     * Finally, we validate the classes, if configured to do so.
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

            Map<String, T> typeStringToGeneratedSpecificPartnerClassSarrayClass = getArrayTypeNameToGeneratedSpecializedPartnerClassSarrayClass();
            transformedClassNodes.putAll(typeStringToGeneratedSpecificPartnerClassSarrayClass);

            if (config.TRANSF_TREAT_SPECIAL_METHOD_CALLS) {
                // Treat defined method calls
                for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                    treatSpecialMethodCallsInClassNodesMethods(entry.getValue());
                }
            }

            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                maybeCheckIsValidWrittenClassNode(entry.getValue());
                // Write class node to class file
                if (config.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER && !config.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER) {
                    try {
                        Class<?> loadedClass = classLoader.loadClass(getNameToLoadOfClassNode(entry.getValue()));
                        transformedClasses.put(entry.getKey(), loadedClass);
                        // If loading succeeded there already is a class file in the build
                        // To trigger all subroutines, we still will transform everything. We just won't put the class
                        // into this.transformedClasses
                    } catch (ClassNotFoundException ignored) {
                        maybeWriteToFile(entry.getValue());
                    } catch (ClassFormatError e) {
                        throw new MulibRuntimeException("Non-overwritten class is erroneous", e);
                    }
                } else {
                    maybeWriteToFile(entry.getValue());
                }
            }

            for (Map.Entry<String, T> entry : transformedClassNodes.entrySet()) {
                if (transformedClasses.get(entry.getKey()) != null) {
                    // Is already loaded
                    continue;
                }
                try {
                    Class<?> result = classLoader.loadClass(getNameToLoadOfClassNode(entry.getValue()));
                    transformedClasses.put(entry.getKey(), result);
                } catch (ClassNotFoundException e) {
                    throw new MulibRuntimeException(e);
                }
            }

            maybeCheckAreValidInitializedClasses(transformedClasses.values());
        }
    }

    /**
     * According to the configuration, alter the bytecode
     * @param classNode The representation of the class
     */
    protected abstract void treatSpecialMethodCallsInClassNodesMethods(T classNode);

    private Map<Field, Field> getFieldsOfTransformedClassesToOriginalClasses(
            Collection<Field> accessibleStaticFieldsOfTransformedClasses) {
        Map<Field, Field> result = new HashMap<>();
        try {
            for (Field f : accessibleStaticFieldsOfTransformedClasses) {
                if (f.getName().contains(_TRANSFORMATION_INDICATOR)) {
                    continue;
                }
                Class<?> originalClass = classLoader.loadClass(f.getDeclaringClass().getName().replace(_TRANSFORMATION_INDICATOR, ""));
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
        Collection<Field> accessibleStaticFieldsOfTransformedClasses = new HashSet<>();
        for (Map.Entry<String, Class<?>> entry : transformedClasses.entrySet()) {
            // Get static fields
            Collection<Field> staticFieldsOfTransformedClass = Utility.getAccessibleStaticFields(entry.getValue());
            if (staticFieldsOfTransformedClass.isEmpty()) {
                continue;
            }
            accessibleStaticFieldsOfTransformedClasses.addAll(staticFieldsOfTransformedClass);
        }
        return getFieldsOfTransformedClassesToOriginalClasses(accessibleStaticFieldsOfTransformedClasses);
    }

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
                        return getTransformedSpecializedPartnerClassSarrayClass(toTransform);
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
                    return sarraysToRealArrayTypes ? Array.newInstance(transformType(componentType), 0).getClass() : getTransformedSpecializedPartnerClassSarrayClass(toTransform);
                }
            } else {
                return getPossiblyTransformedClass(toTransform);
            }
        }
    }

    /**
     * @param clazz The array type with a reference-typed component type
     * @return The specialized class generated by Mulib for this array type
     */
    protected abstract Class<?> getTransformedSpecializedPartnerClassSarrayClass(Class<?> clazz);

    @Override
    public Class<?> transformMulibTypeBackIfNeeded(Class<?> toTransform) {
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
                return Array.newInstance(transformMulibTypeBackIfNeeded(toTransform.getComponentType()), 0).getClass();
            }
        } else if (PartnerClass.class.isAssignableFrom(toTransform)) {
            return getNonTransformedClassFromPartnerClass(toTransform);
        } else {
            return toTransform;
        }
    }

    private Class<?> getNonTransformedClassFromPartnerClass(Class<?> c) {
        String name = c.getName();
        String nameWithoutTransformationPrefix = name.replace(_TRANSFORMATION_INDICATOR, "");
        try {
            return Class.forName(nameWithoutTransformationPrefix);
        } catch (ClassNotFoundException e) {
            throw new MulibRuntimeException(e);
        }
    }

    /**
     * @param classNode The representation of a class of the bytecode framework
     * @return The name used for the class
     */
    protected abstract String getNameToLoadOfClassNode(T classNode);

    @Override
    public Class<?> getTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            throw new MulibRuntimeException("Class has not been transformed: " + beforeTransformation);
        }
        return result;
    }


    @Override
    public Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation) {
        Class<?> result = transformedClasses.get(beforeTransformation.getName());
        if (result == null) {
            try {
                result = Class.forName(addTransformationIndicatorToName(beforeTransformation.getName()));
                transformedClasses.put(beforeTransformation.getName(), result);
                return result;
            } catch (Exception ignored) {}
            return beforeTransformation;
        }
        return result;
    }

    @Override
    public void setPartnerClass(Class<?> original, Class<?> partnerClass) {
        transformedClasses.put(original.getName(), partnerClass);
    }

    /**
     * @param addTo The path to which to add the indicator to
     * @return The String with the {@link StringConstants#_TRANSFORMATION_INDICATOR}
     */
    protected String addTransformationIndicatorToPath(String addTo) {
        return addTransformationIndicatorToName(addTo.replace("/", ".")).replace(".", "/");
    }

    /**
     * Assumes that inner classes are indicated via a '$'. All inner classes also receive the
     * {@link StringConstants#_TRANSFORMATION_INDICATOR}.
     * @param addTo The class name to which to add the indicator to
     * @return The String with the {@link StringConstants#_TRANSFORMATION_INDICATOR}
     */
    protected String addTransformationIndicatorToName(String addTo) {
        Class<?> originalClass = getClassForName(addTo, classLoader);
        String modelClassOrOriginal = config.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS.getOrDefault(originalClass, originalClass).getName();
        return _addPrefix(modelClassOrOriginal);
    }

    private static String _addPrefix(String addTo) {
        if (addTo == null) {
            return null;
        }
        if (addTo.startsWith("java")) {
            // Starting with "java" is forbidden
            addTo = _TRANSFORMATION_INDICATOR + addTo;
        }
        int actualNameIndex = addTo.lastIndexOf('.') + 1;
        String packageName = addTo.substring(0, actualNameIndex);
        String actualName = addTo.substring(actualNameIndex);
        String[] innerClassSplit = actualName.split("\\$");
        StringBuilder resultBuilder = new StringBuilder(packageName);
        for (int i = 0; i < innerClassSplit.length; i++) {
            String s = innerClassSplit[i];
            resultBuilder.append(_TRANSFORMATION_INDICATOR)
                    .append(s);
            if (i < innerClassSplit.length - 1) {
                resultBuilder.append('$');
            }
        }
        return resultBuilder.toString();
    }

    /**
     * @return A map of (array type name, bytecode framework's representation of class of type)-pairs
     */
    protected abstract Map<String, T> getArrayTypeNameToGeneratedSpecializedPartnerClassSarrayClass();

    /**
     * @return A map of (specialized array type class name of class generated by Mulib, original type name)-pairs
     */
    protected abstract Map<String, String> getSpecializedArrayTypeNameToOriginalTypeName();

    /**
     * @param classNode The bytecode framework's representation of a class
     * @return true, if the class is an interface, else false
     */
    protected abstract boolean isInterface(T classNode);

    /**
     * @param name The name of the class
     * @return The bytecode framework's representation of class
     */
    protected abstract T getClassNodeForName(String name);

    /**
     * Loads the class specified by 'toTransformName', then checks whether it should be replaced by a specified class
     * in the search region according to {@link MulibConfig#TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS}.
     * Then, checks if the class has already been added to {@link #transformedClassNodes} and, if this is the case,
     * will return this already transformed instance.
     * If this is not the case, the original class node will be retrieved and transformed using {@link #transformClassNode(Object)}.
     * If this is not an interface the following additions to the class will be made:
     * {@link #generateAndAddSymbolicExecutionConstructor(Object, Object)},
     * {@link #generateAndAddTransformationConstructor(Object, Object)},
     * {@link #generateAndAddCopyConstructor(Object, Object)},
     * {@link #generateAndAddLabelTypeMethod(Object, Object)} (!this is not in use currently!),
     * {@link #generateAndAddOriginalClassMethod(Object, Object)},
     * {@link #generateOrReplaceClinit(Object, Object)},
     * {@link #generateBlockCacheInPartnerClassFieldsAndInitializeLazyFieldsAndGetFieldNameToSubstitutedVar(Object, Object)},
     * {@link #generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(Object, Object)},
     * and {@link #ensureInitializedLibraryTypeFieldsInConstructors(Object)}.
     * @param toTransformName The name of the class that should be transformed
     * @return The bytecode framework's representation of the search region's representation
     */
    protected final T transformEnrichAndValidate(String toTransformName) {
        Class<?> toTransform = getClassForName(toTransformName, classLoader);
        Class<?> replacement = config.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS.get(toTransform);
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
            // Method for setting primitive static fields
            generateOrReplaceClinit(originalCn, result);
            // Method for generating the id-, representationState-, and isNull-field
            generateBlockCacheInPartnerClassFieldsAndInitializeLazyFieldsAndGetFieldNameToSubstitutedVar(originalCn, result);
            // Replace GETFIELD and PUTFIELD with methods to add additional logic. Generate those methods here
            generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(originalCn, result);
            // Insert instructions for checking for null and setting to neutral element, if not yet initialized
            ensureInitializedLibraryTypeFieldsInConstructors(result);
        }
        return result;
    }

    /**
     * Inserts a call to {@link PartnerClass#__mulib__nullCheck()} before any other instruction in a method.
     * Methods exempt from this are summarized in {@link #replaceGetFieldsAndPutFieldsWithGeneratedMethods(Object, Object)}.
     * @param old The representation of the original class
     * @param result The representation of the partner class
     */
    protected abstract void generateNullChecksForMethods(T old, T result);

    /**
     * Generates the methods {@link de.wwu.mulib.substitutions.PartnerClass#__mulib__initializeLazyFields(SymbolicExecution)}
     * and {@link de.wwu.mulib.substitutions.PartnerClass#__mulib__getFieldNameToSubstitutedVar()}
     * @param old The representation of the original class
     * @param result The representation of the partner class
     * @see PartnerClassObject#__mulib__blockCacheInPartnerClassFields()
     */
    protected abstract void generateBlockCacheInPartnerClassFieldsAndInitializeLazyFieldsAndGetFieldNameToSubstitutedVar(T old, T result);

    /**
     * Generates special getters and setters that will replace any GETFIELD and PUTFIELD instructions
     * for the respective field. In these getters and setters, {@link PartnerClass#__mulib__nullCheck()} should be invoked.
     * Furthermore, if {@link PartnerClass#__mulib__isToBeLazilyInitialized()} is true
     * {@link PartnerClass#__mulib__initializeLazyFields(SymbolicExecution)} should be called.
     * If {@link PartnerClass#__mulib__cacheIsBlocked()} is true, {@link SymbolicExecution#getField(PartnerClass, String, Class)}
     * or {@link SymbolicExecution#putField(PartnerClass, String, SubstitutedVar)} should be called, instead of returning/setting the field.
     * The generated methods must contain the {@link StringConstants#_TRANSFORMATION_INDICATOR} in their name.
     * @param old The representation of the original class
     * @param result The representation of the partner class
     */
    protected abstract void generateAccessorAndSetterMethodsForFieldsAndDiscardIsFinal(T old, T result);

    /**
     * Actually replaces the GETFIELD and PUTFIELD instructions in the bytecode of most methods. Excluded are:
     * constructors ("<init>"), methods containing {@link StringConstants#_TRANSFORMATION_INDICATOR} in their name,
     * any method containing a type of the mulib framework not in the {@link de.wwu.mulib.substitutions} package,
     * and abstract methods.
     * @param old The representation of the original class
     * @param result The representation of the partner class
     */
    protected abstract void replaceGetFieldsAndPutFieldsWithGeneratedMethods(T old, T result);

    /**
     * Replaces GETSTATIC and PUTSTATIC bytecode instructions by {@link SymbolicExecution#getStaticField(String)}
     * and {@link SymbolicExecution#setStaticField(String, Object)}.
     * The list of methods excluded from this is defined in {@link #replaceGetFieldsAndPutFieldsWithGeneratedMethods(Object, Object)}.
     * @param old The representation of the original class
     * @param result The representation of the partner class
     */
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
     * TODO This is not currently used and might be removed.
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
    protected abstract void generateOrReplaceClinit(T old, T result);

    /**
     * Since Java assumes default values for un-initialized primitive fields, we have to check whether
     * we still have to assign a value to those fields in all constructors.
     * @param result The result for which additional safety checks and initializations must be added
     * @see AbstractMulibTransformer#generateOrReplaceClinit(Object, Object)
     */
    protected abstract void ensureInitializedLibraryTypeFieldsInConstructors(T result);

    /* METHODS FOR CHECKING HOW CLASSES SHOULD BE TREATED. */

    /**
     * Checks whether, given the configuration, the method of the class, should be concretized.
     * @param methodOwner The class.
     * @return true, if the method should be concretized, else false.
     */
    public boolean shouldBeConcretizedFor(Class<?> methodOwner) {
        return config.TRANSF_CONCRETIZE_FOR.contains(methodOwner);
    }

    /**
     * Checks whether, given the configuration, the method of the class, should be generalized.
     * @param methodOwner The class.
     * @return true, if the method should be generalized, else false.
     */
    public boolean shouldTryToUseGeneralizedMethodCall(Class<?> methodOwner) {
        return config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR.contains(methodOwner);
    }

    /**
     * Checks whether, according to the configuration, a class should be transformed, i.e., whether a partner
     * class should be generated.
     * @param classAsPath The path of the class.
     * @return true, if the class should be transformed, otherwise false.
     */
    public boolean shouldBeTransformed(String classAsPath) {
        // TODO "/" in classAsPath is not really always respected but does not disturb since it is later replaced via '.' anyway. Still, this is unclean
        Class<?> c = getClassForPath(classAsPath, classLoader);
        return !isIgnored(c);
    }

    /* TO OVERRIDE */
    protected abstract MulibClassLoader<T> generateMulibClassLoader();

    // Check if the class, represented by the path, is about to be transformed or has already been transformed.
    protected boolean isAlreadyTransformedOrToBeTransformedPath(String classAsPath) {
        Class<?> c = getClassForPath(classAsPath, classLoader);
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
                        && config.TRANSF_REGARD_SPECIAL_CASE.stream().noneMatch(x -> x.equals(toTransform))
                        && (config.TRANSF_IGNORE_FROM_PACKAGES.stream().anyMatch(x -> toTransform.getPackageName().startsWith(x)) // (Multi-dimensional) arrays of primitives are treated separately in the transformation
                        || config.TRANSF_IGNORE_CLASSES.stream().anyMatch(x -> x.equals(toTransform)))
        );
    }

    /* METHODS FOR INTERACTING WITH CUSTOM CLASSLOADER */

    /**
     * For {@link MulibClassLoader}
     * @param originalClassName The class name of the original class
     * @return The class representation of the bytecode framework
     */
    protected Class<?> getTransformedClassForOriginalClassName(String originalClassName) {
        return transformedClasses.get(originalClassName);
    }

    /**
     * For subclasses of {@link MulibClassLoader}
     * @param className The class name of the original class
     * @return The class representation of the bytecode framework
     */
    public T getTransformedClassNode(String className) {
        return transformedClassNodes.get(className);
    }

    /**
     * For {@link MulibClassLoader}.
     * Adds a transformed class node to {@link #transformedClasses}
     * @param className The class name of the original class
     * @param c The transformed class
     */
    protected void addTransformedClass(String className, Class<?> c) {
        transformedClasses.put(className, c);
    }

    /**
     * Transforms one class. Checks whether the class is ignored and whether the class has already been transformed.
     * @param toTransform The original class to transform
     */
    protected final void transformClass(Class<?> toTransform) {
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


    /**
     * @return A new or cached instance
     */
    public abstract MulibClassFileWriter<T> generateMulibClassFileWriter();

    /**
     * Adds the class that needs to be transformed to the queue.
     * If it is an array, determines the innermost component type, loads the class, and
     * adds this to {@link #classesToTransform}.
     * @param path The class name or path
     */
    protected void addToClassesToTransform(String path) {
        if (path.startsWith("[")) {
            path = path.substring(path.lastIndexOf('[') + 1);
        }
        if (path.startsWith("L")) {
            assert path.endsWith(";");
            path = path.substring(1, path.length() - 1);
        }
        Class<?> c = getClassForPath(path, classLoader);
        classesToTransform.add(c);
    }

    /* OPTIONALLY EXECUTED METHODS */
    // Evaluates the validity of the class by initializing an instance and thus using the JVM's on-board measures
    // for validation.
    private void maybeCheckAreValidInitializedClasses(Collection<Class<?>> generatedClasses) {
        if (config.TRANSF_VALIDATE_TRANSFORMATION) {
            for (Class<?> generatedClass : generatedClasses) {
                try {
                    if (Modifier.isAbstract(generatedClass.getModifiers())) {
                        continue;
                    }
                    if (generatedClass.getNestHost() != null) {
                        continue;
                    }
                    if (determineNestHostFieldName(generatedClass.getName().replace(_TRANSFORMATION_INDICATOR, "")) != null) {
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
    private void maybeCheckIsValidWrittenClassNode(T classNode) {
        if (config.TRANSF_VALIDATE_TRANSFORMATION) {
            // Following the documentation of CheckClassAdapter from here on
            generateMulibClassFileWriter().validateClassNode(classNode);
        }
    }

    // Writes the class to a file to be evaluated using a decompiler.
    private void maybeWriteToFile(T classNode) {
        if (config.TRANSF_WRITE_TO_FILE) {
            synchronized (syncObject) {
                generateMulibClassFileWriter().writeClassToFile(generatedClassesPathPattern, config.TRANSF_INCLUDE_PACKAGE_NAME, classNode);
            }
        }
    }
}