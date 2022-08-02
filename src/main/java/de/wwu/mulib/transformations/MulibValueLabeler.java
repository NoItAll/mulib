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

public class MulibValueLabeler {

    private final Map<Object, Object> searchSpaceRepresentationToLabelObject = new IdentityHashMap<>();
    private final boolean isConcolic;
    private final Map<Class<?>, BiFunction<MulibValueLabeler, Object, Object>> classesToLabelFunction;
    private final boolean transformationRequired;

    public MulibValueLabeler(MulibConfig config, boolean transformationRequired) {
        this.isConcolic = config.CONCOLIC;
        this.classesToLabelFunction = config.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
        this.transformationRequired = transformationRequired;
    }

    public Object labelSprimitive(Sprimitive searchRegionVal, SolverManager solverManager) {
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

    public Object label(Object searchRegionVal, SolverManager solverManager) {
        if (searchRegionVal == null) {
            return null;
        }

        if (searchRegionVal instanceof Sprimitive) {
            return labelSprimitive((Sprimitive) searchRegionVal, solverManager);
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
            int length = (Integer) labelSprimitive(sarray.getLength(), solverManager);
            Object[] result = new Object[length];
            Set<Sint> indices = sarray.getCachedIndices();
            for (Sint i : indices) {
                SubstitutedVar value = sarray.getForIndex(i);
                int labeledIndex = (Integer) labelSprimitive(i, solverManager);
                Object labeledValue = label(value, solverManager);
                result[labeledIndex] = labeledValue;
            }
            return result;
        } else {
            Object result = searchSpaceRepresentationToLabelObject.get(searchRegionVal);
            if (result != null) {
                return result;
            }
            BiFunction<MulibValueLabeler, Object, Object> labelMethod =
                    this.classesToLabelFunction.get(searchRegionVal.getClass());
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
                    result[i] = label(val, solverManager);
                } else if (val instanceof Sprimitive) {
                    result[i] = label(val, solverManager);
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
