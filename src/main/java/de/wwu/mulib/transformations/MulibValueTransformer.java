package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Transforms values from outside the search region into the search region-representation of it.
 * Furthermore, generates objects which then can be labeled, potentially using reflection.
 * The generated partner classes make use of this functionality.
 */
public final class MulibValueTransformer {

    private final MulibTransformer mulibTransformer;
    private final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToTransformation;
    private final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToCopyFunction;
    private final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToLabelFunction;
    // Stores the objects for transformation, i.e., to-transform -> transformed, but also transformed -> transformed (copied)
    private final Map<Object, Object> alreadyCreatedObjects = new IdentityHashMap<>();
    private final Map<Object, Object> searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();
    // If the method at hand did not need to be transformed, we do not have to label or transform into the library-/
    // Partner-classes. This is useful for testing and for manually writing such classes.
    private final boolean transformationRequired;
    private final boolean isConcolic;
    private long nextSarrayId = 0;
    private final SymbolicExecution se;
    public MulibValueTransformer(MulibConfig config, MulibTransformer mulibTransformer, boolean transformationRequired) {
        this.mulibTransformer = mulibTransformer;
        this.classesToCopyFunction = config.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS;
        this.classesToTransformation = config.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS;
        this.classesToLabelFunction = config.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
        this.transformationRequired = transformationRequired;
        this.isConcolic = config.CONCOLIC;
        this.se = null;
    }

    public MulibValueTransformer(
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToCopyFunction,
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToTransformation,
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToLabelFunction,
            boolean transformationRequired,
            MulibTransformer mulibTransformer,
            long nextSarrayId,
            boolean isConcolic,
            SymbolicExecution se) {
        this.mulibTransformer = mulibTransformer;
        this.classesToCopyFunction = classesToCopyFunction;
        this.classesToTransformation = classesToTransformation;
        this.classesToLabelFunction = classesToLabelFunction;
        this.transformationRequired = transformationRequired;
        this.nextSarrayId = nextSarrayId;
        this.isConcolic = isConcolic;
        this.se = se;
    }

    public void setNextSarrayId(Object[] findHighestSarrayIdIn) {
        long currentHighestId = -1;
        for (Object o : findHighestSarrayIdIn) {
            if (o instanceof Sarray && currentHighestId < ((Sarray<?>) o).getId()) {
                currentHighestId = ((Sarray<?>) o).getId();
            }
        }
        this.nextSarrayId = currentHighestId + 1;
    }

    public long getNextSarrayIdAndIncrement() {
        return nextSarrayId++;
    }

    public boolean alreadyCreated(Object o) {
        return alreadyCreatedObjects.containsKey(o);
    }

    public void registerCopy(Object original, Object copy) {
        alreadyCreatedObjects.put(original, copy);
    }

    // TODO Simplify? Only use getCopy and getTransformedValue. Other public methods can drop away. See initializeObjectFieldInSpecialConstructor
    public Object getCopy(Object original) {
        if (original == null) {
            throw new MulibRuntimeException("For null, no copy can be retrieved.");
        }
        Object o = alreadyCreatedObjects.get(original);
        if (o == null) {
            throw new MulibRuntimeException("There is no copy for the given original: " + original);
        }
        return o;
    }

    public Object copySearchRegionRepresentationOfNonSprimitive(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof PartnerClass) {
            // TODO Currently, we register copies in the copy-constructor to avoid reflection. This can be optimized
            //  e.g. via a lambda with a constructor passed as an argument
            return ((PartnerClass) o).copy(this);
        }

        Object result;
        if ((result = this.alreadyCreatedObjects.get(o)) != null) {
            return result;
        }

        if (o instanceof Sarray) {
            result = ((Sarray<?>) o).copy(this);
            registerCopy(o, result);
            return result;
        }
        BiFunction<MulibValueTransformer, Object, Object> copier = classesToCopyFunction.get(o.getClass());
        if (copier == null) {
            return o;
        }
        return copier.apply(this, o);
    }

    public Object copySprimitive(Sprimitive o) {
        if (!isConcolic) {
            return o;
        }
        Object result;
        if ((result = this.alreadyCreatedObjects.get(o)) != null) {
            return result;
        }
        if (o instanceof Sym) {
            result = _potentiallyUnpackAndRelabelConcolic((Sym) o);
        } else {
            result = o;
        }
        registerCopy(o, result);
        return result;
    }

    public MulibValueTransformer copyFromPrototype(SymbolicExecution se) {
        return new MulibValueTransformer(classesToCopyFunction, classesToTransformation, 
                classesToLabelFunction, transformationRequired, mulibTransformer, nextSarrayId, isConcolic, se);
    }

    private Object _potentiallyUnpackAndRelabelConcolic(Sym currentValue) {
        // Check if we need to unpack concolic values
        if (currentValue instanceof Sbool.SymSbool) {
            Sbool.SymSbool s = (Sbool.SymSbool) ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool.SymSbool) currentValue);
            return ((AssignConcolicLabelEnabledValueFactory) se.getValueFactory()).assignLabel(se, s);
        } else if (currentValue instanceof SymNumericExpressionSprimitive) {
            SymNumericExpressionSprimitive s = (SymNumericExpressionSprimitive) ConcolicNumericContainer.tryGetSymFromConcolic((SymNumericExpressionSprimitive) currentValue);
            return ((AssignConcolicLabelEnabledValueFactory) se.getValueFactory()).assignLabel(se, s);
        } else {
            throw new NotYetImplementedException(currentValue.getClass().toString());
        }
    }

    /**
     * Transforms the given object into an instance of a library class or the respective available partner class.
     * If no partner class is available (because currentValue.getClass() is ignored) the original value is copied
     * according to some predefined BiFunction. If there is no such BiFunction, the original
     * value is returned.
     * @param currentValue The value which should be transformed into a library or a partner class.
     * @return The replacement value.
     */
    public Object transformValue(final Object currentValue) {
        if (!transformationRequired || currentValue == null) {
            return currentValue;
        }
        if (currentValue instanceof SubstitutedVar) {
            return currentValue;
        }
        Object result;
        if ((result = alreadyCreatedObjects.get(currentValue)) != null) {
            return result;
        }
        if (currentValue instanceof Integer) {
            return Sint.concSint((Integer) currentValue);
        } else if (currentValue instanceof Double) {
            return Sdouble.concSdouble((Double) currentValue);
        } else if (currentValue instanceof Boolean) {
            return Sbool.concSbool((Boolean) currentValue);
        } else if (currentValue instanceof Long) {
            return Slong.concSlong((Long) currentValue);
        } else if (currentValue instanceof Float) {
            return Sfloat.concSfloat((Float) currentValue);
        } else if (currentValue instanceof Short) {
            return Sshort.concSshort((Short) currentValue);
        } else if (currentValue instanceof Byte) {
            return Sbyte.concSbyte((Byte) currentValue);
        } else if (currentValue instanceof Character) {
            throw new NotYetImplementedException();
        } else if (currentValue.getClass().isArray()) {
            result = transformArrayToSarray(currentValue);
            alreadyCreatedObjects.put(currentValue, result);
            return result;
        }

        // Treat objects
        Class<?> beforeTransformation = currentValue.getClass();
        Class<?> possiblyTransformed = transformType(beforeTransformation);
        if (beforeTransformation != possiblyTransformed) {
            assert PartnerClass.class.isAssignableFrom(possiblyTransformed);
            Constructor<?> constr;
            try {
                // Use transformation constructor
                constr = possiblyTransformed.getConstructor(beforeTransformation, MulibValueTransformer.class);
                result = constr.newInstance(currentValue, this);
                assert alreadyCreatedObjects.get(currentValue) == result;
                return result;
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException e) {
                e.printStackTrace();
                throw new MulibRuntimeException(e);
            }
        } else {
            BiFunction<MulibValueTransformer, Object, Object> transformationFunction = classesToTransformation.get(beforeTransformation);
            if (transformationFunction != null) {
                result = transformationFunction.apply(this, currentValue);
                alreadyCreatedObjects.put(currentValue, result);
                return result;
            } else {
                alreadyCreatedObjects.put(currentValue, currentValue);
                return currentValue;
            }
        }
    }

    // Accounts for the fact that we transform array to sarray by providing a Java-primitive-array value of the respective type.
    // Always returns an instance of Sarray.
    private Sarray<?> transformArrayToSarray(Object array) {
        if (array == null) {
            return null;
        }
        Class<?> beforeTransformation = array.getClass();
        int length = Array.getLength(array);
        Class<?> componentType = beforeTransformation.getComponentType();
        // Maintain information of array-arrays; - create, e.g., Sint[] instead of SintSarray:
        Class<?> transformed = transformType(beforeTransformation, true);
        Class<?> transformedComponentType = transformed.getComponentType();
        // Get innermost type
        Class<?> innermostTransformedComponentType = transformedComponentType;
        while (innermostTransformedComponentType.isArray()) {
            innermostTransformedComponentType = innermostTransformedComponentType.getComponentType();
        }
        if (componentType == transformedComponentType || !SubstitutedVar.class.isAssignableFrom(innermostTransformedComponentType)) {
            throw new MulibIllegalStateException("To use Sarrays, the component type must be substituted. Given: " + transformed);
        }

        if (!componentType.isArray()) {
            // If the elements are not themselves arrays, we create either a SprimitiveSarray, or a PartnerClassSarray
            if (componentType.isPrimitive()) {
                if (componentType == int.class) {
                    Sint[] transformedValues = (Sint[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SintSarray(transformedValues, this);
                } else if (componentType == long.class) {
                    Slong[] transformedValues = (Slong[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SlongSarray(transformedValues, this);
                } else if (componentType == double.class) {
                    Sdouble[] transformedValues = (Sdouble[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SdoubleSarray(transformedValues, this);
                } else if (componentType == float.class) {
                    Sfloat[] transformedValues = (Sfloat[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SfloatSarray(transformedValues, this);
                } else if (componentType == short.class) {
                    Sshort[] transformedValues = (Sshort[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SshortSarray(transformedValues, this);
                } else if (componentType == byte.class) {
                    Sbyte[] transformedValues = (Sbyte[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SbyteSarray(transformedValues, this);
                } else if (componentType == boolean.class) {
                    Sbool[] transformedValues = (Sbool[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                    return new Sarray.SboolSarray(transformedValues, this);
                } else {
                    throw new NotYetImplementedException(array.toString());
                }
            } else {
                PartnerClass[] transformedValues = (PartnerClass[]) _transformArrayToSarrayHelper(array, transformedComponentType);
                return new Sarray.PartnerClassSarray<>(transformedValues, this);
            }
        } else {
            // Component is array! We need to generate a SarraySarray
            Sarray<?>[] transformedValues = new Sarray[length];
            for (int i = 0; i < length; i++) {
                Object currentValue = Array.get(array, i);
                Sarray<?> transformedValue = transformArrayToSarray(currentValue);
                transformedValues[i] = transformedValue;
            }
            return new Sarray.SarraySarray(transformedValues, (Class<? extends SubstitutedVar>) transformedComponentType, this);
        }
    }

    // Always returns a Java-primitive array of the respective type.
    private Object _transformArrayToSarrayHelper(
            Object array, Class<?> possiblyTransformed) {
        int length = Array.getLength(array);
        Object values = Array.newInstance(possiblyTransformed, length);
        // Now we can call transformValue(...)
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            Object transformedValue;
            if (value.getClass().isArray()) {
                transformedValue = _transformArrayToSarrayHelper(value, possiblyTransformed.getComponentType());
            } else {
                transformedValue = transformValue(value);
            }
            Array.set(values, i, transformedValue);
        }
        return values;
    }
    /**
     * Transforms type. Arrays are transformed to their respective subclass of Sarray.
     * @param toTransform Type to transform
     * @return Transformed type
     */
    public Class<?> transformType(Class<?> toTransform) {
        return transformType(toTransform, false);
    }

    /**
     * Transforms type. Arrays are transformed to their respective subclass of Sarray.
     * @param toTransform Type to transform
     * @param sarraysToRealArrayTypes Should, e.g., Sint[].class be returned insted of SintSarray?
     * @return Transformed type
     */
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
                throw new NotYetImplementedException(toTransform.getName());
            }
        } else {
            return mulibTransformer.getPossiblyTransformedClass(toTransform);
        }
    }

    private Object labelArray(Object o, SolverManager solverManager) {
        Class<?> componentType = o.getClass().getComponentType();
        int length = Array.getLength(o);
        Object[] result = new Object[length];
        if (componentType.isArray()) {
            for (int i = 0; i < length; i++) {
                result[i] = labelArray(Array.get(o, i), solverManager);
            }
        } else {
            for (int i = 0; i < length; i++) {
                Object val = Array.get(o, i);
                if (val instanceof PartnerClass) {
                    result[i] = labelValue(val, solverManager);
                } else if (val instanceof Sprimitive) {
                    result[i] = labelValue(val, solverManager);
                } else {
                    return o;
                }
            }
        }
        return result;
    }

    private static Object labelConcSnumber(ConcSnumber searchRegionVal) {
        if (searchRegionVal instanceof Sbool) {
            return ((Sbool.ConcSbool) searchRegionVal).isTrue();
        }
        if (searchRegionVal instanceof Sint) {
            if (searchRegionVal instanceof Sshort) {
                return searchRegionVal.shortVal();
            } else if (searchRegionVal instanceof Sbyte) {
                return searchRegionVal.byteVal();
            } else {
                return searchRegionVal.intVal();
            }
        } else if (searchRegionVal instanceof Slong) {
            return searchRegionVal.longVal();
        } else if (searchRegionVal instanceof Sdouble) {
            return searchRegionVal.doubleVal();
        } else if (searchRegionVal instanceof Sfloat) {
            return searchRegionVal.floatVal();
        } else {
            throw new NotYetImplementedException();
        }
    }

    public Object labelPrimitiveValue(Sprimitive searchRegionVal, SolverManager solverManager) {
        if (searchRegionVal instanceof ConcSnumber) {
            return labelConcSnumber((ConcSnumber) searchRegionVal);
        } else {
            if (searchRegionVal instanceof Sbool.SymSbool) {
                Constraint c = ((Sbool.SymSbool) searchRegionVal).getRepresentedConstraint();
                if (c instanceof ConcolicConstraintContainer) {
                    return ((ConcolicConstraintContainer) c).getConc().isTrue();
                }
            } else {
                NumericExpression ne = ((SymNumericExpressionSprimitive) searchRegionVal).getRepresentedExpression();
                if (ne instanceof ConcolicNumericContainer) {
                    return labelConcSnumber(((ConcolicNumericContainer) ne).getConc());
                }
            }
            return solverManager.getLabel(searchRegionVal);
        }
    }

    public Object labelValue(Object searchRegionVal, SolverManager solverManager) {
        if (searchRegionVal == null) {
            return null;
        }

        if (searchRegionVal instanceof Sprimitive) {
            return labelPrimitiveValue((Sprimitive) searchRegionVal, solverManager);
        } else if (searchRegionVal instanceof PartnerClass) {
            Object result = searchSpaceRepresentationToLabelObject.get(searchRegionVal);
            if (result != null) {
                return result;
            }
            if (transformationRequired) {
                Object emptyLabelObject = createEmptyLabelObject(((PartnerClass) searchRegionVal).getOriginalClass());
                searchSpaceRepresentationToLabelObject.put(searchRegionVal, emptyLabelObject);
                result = ((PartnerClass) searchRegionVal).label(
                        emptyLabelObject,
                        this,
                        solverManager
                );
                assert emptyLabelObject == result;
                return result;
            } else {
                return searchRegionVal;
            }
        } else if (searchRegionVal.getClass().isArray()) {
            Object labeledArray = searchSpaceRepresentationToLabelObject.get(searchRegionVal);
            if (labeledArray != null) {
                return labeledArray;
            }
            labeledArray = labelArray(searchRegionVal, solverManager);
            searchSpaceRepresentationToLabelObject.put(searchRegionVal, labeledArray);
            return labeledArray;
        } else if (searchRegionVal instanceof Sarray) {
            Sarray sarray = (Sarray) searchRegionVal;
            int length = (Integer) labelPrimitiveValue(sarray.getLength(), solverManager);
            Object[] result = new Object[length];
            Set<Sint> indices = sarray.getCachedIndices();
            for (Sint i : indices) {
                SubstitutedVar value = sarray.getForIndex(i);
                int labeledIndex = (Integer) labelPrimitiveValue(i, solverManager);
                Object labeledValue = labelValue(value, solverManager);
                result[labeledIndex] = labeledValue;
            }
            return result;
        } else {
            Object result = searchSpaceRepresentationToLabelObject.get(searchRegionVal);
            if (result != null) {
                return result;
            }
            BiFunction<MulibValueTransformer, Object, Object> labelMethod = this.classesToLabelFunction.get(searchRegionVal.getClass());
            if (labelMethod == null) {
                searchSpaceRepresentationToLabelObject.put(searchRegionVal, searchRegionVal);
                return searchRegionVal;
            } else {
                result = labelMethod.apply(this, searchRegionVal);
                assert searchRegionVal.getClass() == result.getClass();
                searchSpaceRepresentationToLabelObject.put(searchRegionVal, result);
                return result;
            }
        }
    }


    private Object createEmptyLabelObject(Class<?> clazz) {
        Constructor<?> constructor = getOrGenerateZeroArgsConstructor(clazz);
        try {
            Object result = constructor.newInstance();
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new MulibRuntimeException(e);
        }
    }

    // Cache for generated constructors
    private static final Map<Class<?>, Constructor<?>> classToZeroArgsConstructor = new HashMap<>();
    private static Constructor<?> getOrGenerateZeroArgsConstructor(final Class<?> toGenerateFor) {
        Class<?> current = toGenerateFor;
        // Gather superclasses
        ArrayDeque<Class<?>> deque = new ArrayDeque<>();
        Constructor<?> previousConstructor;
        do {
            previousConstructor = classToZeroArgsConstructor.get(current);
            if (previousConstructor != null) {
                break;
            }
            assert !deque.contains(current);
            deque.addFirst(current);
            current = current.getSuperclass();
        } while (current != null);

        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> currentConstructor;
        // Starting from the most abstract class, generate suitable constructors
        while (!deque.isEmpty()) {
            current = deque.pollFirst();
            if (previousConstructor == null) {
                currentConstructor = rf.newConstructorForSerialization(current);
            } else {
                currentConstructor = rf.newConstructorForSerialization(current, previousConstructor);
            }
            if (currentConstructor == null) {
                throw new MulibRuntimeException("Failed to generate a constructor.");
            }
            classToZeroArgsConstructor.put(current, currentConstructor);
            previousConstructor = currentConstructor;
        }

        return classToZeroArgsConstructor.get(toGenerateFor);
    }
}
