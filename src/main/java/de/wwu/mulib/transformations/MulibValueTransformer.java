package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
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
    private final Map<Object, Object> alreadyCreatedObjects = new IdentityHashMap<>();
    private final Map<Object, Object> searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();
    // If the method at hand did not need to be transformed, we do not have to label or transform into the library-/
    // Partner-classes. This is useful for testing and for manually writing such classes.
    private final boolean transformationRequired;

    public MulibValueTransformer(MulibConfig config, MulibTransformer mulibTransformer, boolean transformationRequired) {
        this.mulibTransformer = mulibTransformer;
        this.classesToCopyFunction = config.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS;
        this.classesToTransformation = config.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS;
        this.classesToLabelFunction = config.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
        this.transformationRequired = transformationRequired;
    }

    public MulibValueTransformer(
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToCopyFunction,
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToTransformation,
            Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToLabelFunction,
            boolean transformationRequired,
            MulibTransformer mulibTransformer) {
        this.mulibTransformer = mulibTransformer;
        this.classesToCopyFunction = classesToCopyFunction;
        this.classesToTransformation = classesToTransformation;
        this.classesToLabelFunction = classesToLabelFunction;
        this.transformationRequired = transformationRequired;
    }

    public boolean alreadyCreated(Object o) {
        return alreadyCreatedObjects.containsKey(o);
    }

    public void registerCopy(Object original, Object copy) {
        alreadyCreatedObjects.put(original, copy);
    }

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

    public Object copySearchRegionRepresentation(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof PartnerClass) {
            return ((PartnerClass) o).copy(this);
        } else if (o instanceof Sprimitive) {
            return o;
        }
        BiFunction<MulibValueTransformer, Object, Object> copier = classesToCopyFunction.get(o.getClass());
        if (copier == null) {
            return o;
        }
        return copier.apply(this, o);
    }

    public MulibValueTransformer copyFromPrototype() {
        return new MulibValueTransformer(classesToCopyFunction, classesToTransformation, 
                classesToLabelFunction, transformationRequired, mulibTransformer);
    }

    /**
     * Transforms the given object into an instance of a library class or the respective available partner class.
     * If no partner class is available (because currentValue.getClass() is ignored) the original value is copied
     * according to some predefined BiFunction. If there is no such BiFunction, the original
     * value is returned.
     * @param currentValue The value which should be transformed into a library or a partner class.
     * @return The replacement value.
     */
    public Object transformValue(Object currentValue) {
        if (!transformationRequired) {
            return currentValue;
        }
        if (currentValue == null) {
            return null;
        }
        if (currentValue instanceof SubstitutedVar) {
            return currentValue;
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
        }
        Class<?> beforeTransformation = currentValue.getClass();
        Class<?> possiblyTransformed = mulibTransformer.getPossiblyTransformedClass(beforeTransformation);
        if (beforeTransformation != possiblyTransformed) {
            assert PartnerClass.class.isAssignableFrom(possiblyTransformed);
            try {
                // Use transformation constructor
                Constructor<?> constr = possiblyTransformed.getDeclaredConstructor(beforeTransformation, MulibValueTransformer.class);
                Object result = constr.newInstance(currentValue, this);
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
                return transformationFunction.apply(this, currentValue);
            } else {
                return currentValue;
            }
        }
    }

    public Class<?> transformType(Class<?> toTransform) {
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
            throw new NotYetImplementedException();
        } else {
            return mulibTransformer.getTransformedClass(toTransform);
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
