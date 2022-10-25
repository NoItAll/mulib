package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.IdentityHavingSubstitutedVar;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Transforms values from outside the search region into the search region-representation of it.
 * Furthermore, generates objects which then can be labeled, potentially using reflection.
 * The generated partner classes make use of this functionality.
 */
public final class MulibValueTransformer {

    private final MulibTransformer mulibTransformer;
    private final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> classesToTransformation;
    // Stores the objects for transformation, i.e., to-transform -> transformed, but also transformed -> transformed (copied)
    private final Map<Object, Object> alreadyTransformedObjects = new IdentityHashMap<>();
    // If the method at hand did not need to be transformed, we do not have to label or transform into the library-/
    // Partner-classes. This is useful for testing and for manually writing such classes.
    private final boolean transformationRequired;
    private int nextIdentityHavingObjectNr = 0;
    public MulibValueTransformer(MulibConfig config, MulibTransformer mulibTransformer) {
        this.mulibTransformer = mulibTransformer;
        this.classesToTransformation = config.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS;
        this.transformationRequired = config.TRANSF_TRANSFORMATION_REQUIRED;
    }

    public void setNextIdentityHavingObjectNr(Object[] findHighestSarrayIdIn) {
        int currentHighestId = -1;
        for (Object o : findHighestSarrayIdIn) {
            if (o instanceof IdentityHavingSubstitutedVar
                    && ((IdentityHavingSubstitutedVar) o).__mulib__getId() instanceof ConcSnumber
                    && currentHighestId < ((ConcSnumber) ((IdentityHavingSubstitutedVar) o).__mulib__getId()).intVal()) {
                currentHighestId = ((ConcSnumber) ((IdentityHavingSubstitutedVar) o).__mulib__getId()).intVal();
            }
        }
        this.nextIdentityHavingObjectNr = currentHighestId + 1;
    }

    public int getNextIdentityHavingObjectNr() {
        return nextIdentityHavingObjectNr;
    }

    public void registerTransformedObject(Object original, Object transformed) {
        alreadyTransformedObjects.put(original, transformed);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean alreadyTransformed(Object o) {
        return alreadyTransformedObjects.containsKey(o);
    }

    public Object getTransformedObject(Object original) {
        return alreadyTransformedObjects.get(original);
    }

    /**
     * Transforms the given object into an instance of a library class or the respective available partner class.
     * If no partner class is available (because currentValue.getClass() is ignored) the original value is copied
     * according to some predefined BiFunction. If there is no such BiFunction, the original
     * value is returned.
     * @param currentValue The value which should be transformed into a library or a partner class.
     * @return The replacement value.
     */
    public Object transform(final Object currentValue) {
        if (!transformationRequired || currentValue == null) {
            return currentValue;
        }
        if (currentValue instanceof SubstitutedVar) {
            return currentValue;
        }
        Object result;
        if ((result = alreadyTransformedObjects.get(currentValue)) != null) {
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
            alreadyTransformedObjects.put(currentValue, result);
            return result;
        }

        // Treat objects
        Class<?> beforeTransformation = currentValue.getClass();
        Class<?> possiblyTransformed = mulibTransformer.transformType(beforeTransformation);
        if (beforeTransformation != possiblyTransformed) {
            assert PartnerClass.class.isAssignableFrom(possiblyTransformed);
            Constructor<?> constr;
            try {
                // Use transformation constructor
                constr = possiblyTransformed.getConstructor(beforeTransformation, MulibValueTransformer.class);
                result = constr.newInstance(currentValue, this);
                assert alreadyTransformedObjects.get(currentValue) == result;
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
                alreadyTransformedObjects.put(currentValue, result);
                return result;
            } else {
                alreadyTransformedObjects.put(currentValue, currentValue);
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
        Class<?> transformed = mulibTransformer.transformType(beforeTransformation, true);
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
            return new Sarray.SarraySarray(transformedValues, transformedComponentType, this);
        }
    }

    // Always returns a Java-primitive array of the respective type.
    private Object _transformArrayToSarrayHelper(
            Object array, Class<?> possiblyTransformed) {
        int length = Array.getLength(array);
        Object values = Array.newInstance(possiblyTransformed, length);
        // Now we can call transform(...) or recursively call this same function to deal with nested arrays
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            Object transformedValue;
            if (value.getClass().isArray()) {
                transformedValue = _transformArrayToSarrayHelper(value, possiblyTransformed.getComponentType());
            } else {
                transformedValue = transform(value);
            }
            Array.set(values, i, transformedValue);
        }
        return values;
    }
}
