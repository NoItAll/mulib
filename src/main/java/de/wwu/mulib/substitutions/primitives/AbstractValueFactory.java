package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.executors.AliasingInformation;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractValueFactory implements ValueFactory {
    protected final boolean enableInitializeFreeArraysWithNull;
    protected final boolean enableInitializeFreeObjectsWithNull;
    protected final boolean aliasingForFreeObjects;
    protected final boolean throwExceptionOnOOB;
    protected final Function<Snumber, Snumber> tryGetSymFromSnumber;
    protected final Function<Sbool, Sbool> tryGetSymFromSbool;
    private final Map<Class<?>, MethodHandle> arrayTypesToSpecializedConstructor;
    private final Map<Class<?>, MethodHandle> arrayTypesToSpecializedMultiDimensionalSarraySarrayConstructors;
    private final BiFunction<Class<?>, Object[], Sarray.PartnerClassSarray<?>> getSarrayConstructor;
    private final BiFunction<Class<?>, Object[], Sarray.SarraySarray> getMultiDimensionalSarraySarrayConstructor;
    private final boolean transformationRequired;


    protected final MulibConfig config;

    @SuppressWarnings("unchecked")
    protected AbstractValueFactory(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        this.transformationRequired = config.TRANSF_TRANSFORMATION_REQUIRED;
        this.enableInitializeFreeArraysWithNull = config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        this.enableInitializeFreeObjectsWithNull = config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
        this.aliasingForFreeObjects = config.ALIASING_FOR_FREE_OBJECTS;
        this.throwExceptionOnOOB = config.THROW_EXCEPTION_ON_OOB;
        // Generate the initializers for, statically unknown, Sarray subclasses.
        // TODO In the future, we might directly call the constructors in-code to avoid this. We might have callbacks in
        // the constructors to inform the ValueFactories about initialized Sarrays.
        this.arrayTypesToSpecializedConstructor = new HashMap<>();
        this.arrayTypesToSpecializedMultiDimensionalSarraySarrayConstructors = new HashMap<>();
        if (transformationRequired) {
            for (Map.Entry<Class<?>, Class<?>> entry : arrayTypesToSpecializedSarrayClass.entrySet()) {
                try {
                    Class<?> componentType = entry.getKey().getComponentType();
                    Class<?> specializedSarrayType = entry.getValue();
                    Constructor<?> constructor;
                    if (Sarray.SarraySarray.class.isAssignableFrom(specializedSarrayType)) {
                        Constructor<?> multiDimConstructor = specializedSarrayType.getConstructor(Sint[].class, SymbolicExecution.class, Class.class);
                        MethodHandle mh = MethodHandles.lookup().unreflectConstructor(multiDimConstructor);
                        arrayTypesToSpecializedMultiDimensionalSarraySarrayConstructors.put(componentType, mh);
                        constructor = specializedSarrayType.getConstructor(Sint.class, SymbolicExecution.class, boolean.class, Class.class, Sbool.class);
                    } else {
                        constructor = specializedSarrayType.getConstructor(Class.class, Sint.class, SymbolicExecution.class, boolean.class, Sbool.class);
                    }
                    MethodHandle mh = MethodHandles.lookup().unreflectConstructor(constructor);
                    arrayTypesToSpecializedConstructor.put(componentType, mh);
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new MulibRuntimeException(e);
                }
            }
            getSarrayConstructor = (clazz, args) -> {
                MethodHandle mh = arrayTypesToSpecializedConstructor.get(clazz);
                assert args.length == 5;
                assert mh != null;
                try {
                    return (Sarray.PartnerClassSarray<?>) mh.invokeWithArguments(args);
                } catch (Throwable e) {
                    throw new MulibRuntimeException(e);
                }
            };
            getMultiDimensionalSarraySarrayConstructor = (clazz, args) -> {
                MethodHandle mh = arrayTypesToSpecializedMultiDimensionalSarraySarrayConstructors.get(clazz);
                assert args.length == 3;
                assert mh != null;
                try {
                    return (Sarray.SarraySarray) mh.invokeWithArguments(args);
                } catch (Throwable e) {
                    throw new MulibRuntimeException(e);
                }
            };
        } else {
            getSarrayConstructor = (clazz, args) -> {
                assert args.length == 5;
                if (clazz.isArray()) {
                    return new Sarray.SarraySarray((Sint) args[0], (SymbolicExecution) args[1], (boolean) args[2], (Class<?>) args[3], (Sbool) args[4]);
                } else {
                    return new Sarray.PartnerClassSarray((Class<?>) args[0], (Sint) args[1], (SymbolicExecution) args[2], (boolean) args[3], (Sbool) args[4]);
                }
            };
            getMultiDimensionalSarraySarrayConstructor = (clazz, args) -> {
                assert clazz.isArray();
                assert args.length == 3;
                return new Sarray.SarraySarray((Sint[]) args[0], (SymbolicExecution) args[1], (Class<?>) args[2]);
            };

        }
        this.config = config;
        if (config.CONCOLIC) {
            tryGetSymFromSnumber = ConcolicNumericContainer::tryGetSymFromConcolic;
            tryGetSymFromSbool = ConcolicConstraintContainer::tryGetSymFromConcolic;
        } else {
            tryGetSymFromSnumber = s -> s;
            tryGetSymFromSbool = s -> s;
        }
    }

    @Override
    public final Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sintSarray(
                se,
                len,
                defaultIsSymbolic,
                // defaultIsSymbolic also acts as a way to check whether the array should be initializable to null
                enableInitializeFreeArraysWithNull && defaultIsSymbolic
        );
    }

    @Override
    public final Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sdoubleSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sfloatSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return slongSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);

    }

    @Override
    public final Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sshortSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sbyteSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return sboolSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic) {
        return scharSarray(se, len, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean defaultIsSymbolic) {
        return partnerClassSarray(se, len, clazz, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean defaultIsSymbolic) {
        return sarraySarray(se, len, clazz, defaultIsSymbolic, enableInitializeFreeArraysWithNull && defaultIsSymbolic);
    }

    @Override
    public final Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SintSarray result = new Sarray.SintSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sint[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SdoubleSarray result = new Sarray.SdoubleSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sdouble[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SfloatSarray result = new Sarray.SfloatSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sfloat[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SlongSarray result = new Sarray.SlongSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Slong[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SshortSarray result = new Sarray.SshortSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sshort[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SbyteSarray result = new Sarray.SbyteSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sbyte[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SboolSarray result = new Sarray.SboolSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Sbool[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.ScharSarray result = new Sarray.ScharSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(Schar[].class, result, se);
        return result;
    }

    @Override
    public final Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.PartnerClassSarray result = getSarrayConstructor.apply(clazz, new Object[] { clazz, len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE });
        decideOnAddToAliasingAndRepresentation(result.getClass(), result, se);
        return result;
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SarraySarray result = (Sarray.SarraySarray) getSarrayConstructor.apply(clazz, new Object[] { len, se, defaultIsSymbolic, clazz, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE });
        decideOnAddToAliasingAndRepresentation(result.getClass(), result, se);
        return result;
    }

    @Override
    public final Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
        // Never is part of aliasing
        return getMultiDimensionalSarraySarrayConstructor.apply(clazz, new Object[] { lengths, se, clazz });
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf) {
        // defaultIsSymbolic is assumed
        return symObject(se, toGetInstanceOf, enableInitializeFreeObjectsWithNull);
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf, boolean canBeNull) {
        try {
            if (toGetInstanceOf.getEnclosingClass() != null && !Modifier.isStatic(toGetInstanceOf.getModifiers())) {
                throw new NotYetImplementedException("Symbolic initialization of inner non-static class not yet supported");
            }
            Constructor<T> cons = toGetInstanceOf.getDeclaredConstructor(SymbolicExecution.class);
            T result = cons.newInstance(se);
            result.__mulib__setIsNull(canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
            decideOnAddToAliasingAndRepresentation(toGetInstanceOf, result, se);
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new MulibIllegalStateException("The SymbolicExecution-constructor must be there!");
        }
    }

    private void decideOnAddToAliasingAndRepresentation(Class<?> c, PartnerClass pc, SymbolicExecution se) {
        if (config.ALIASING_FOR_FREE_OBJECTS && pc.__mulib__defaultIsSymbolic()) {
            pc.__mulib__prepareForAliasingAndBlockCache(se);
            Sint reservedId = se.concSint(se.getNextNumberInitializedSymObject());
            pc.__mulib__setAsRepresentedInSolver();

            if (!se.nextIsOnKnownPath()) {
                Set<Sint> potentialIds = AliasingInformation.getAliasingTargetIdsForClass(c, config.CONCOLIC);
                Sint id = (Sint) tryGetSymFromSnumber.apply(pc.__mulib__getId());
                Sbool isNull = tryGetSymFromSbool.apply(pc.__mulib__isNull());
                PartnerClassObjectConstraint pcoc;
                if (pc instanceof Sarray) {
                    Sint length = (Sint) tryGetSymFromSnumber.apply(((Sarray<?>) pc).getLength());
                    if (potentialIds.isEmpty()) {
                        pcoc = new ArrayInitializationConstraint(
                                id,
                                length,
                                isNull,
                                c,
                                new ArrayAccessConstraint[0],
                                true
                        );
                    } else {
                        pcoc = new ArrayInitializationConstraint(
                                id,
                                length,
                                isNull,
                                reservedId,
                                potentialIds,
                                c,
                                new ArrayAccessConstraint[0],
                                true
                        );
                    }
                } else {
                    if (potentialIds.isEmpty()) {
                        pcoc = new PartnerClassObjectInitializationConstraint(
                                c,
                                id,
                                isNull,
                                new PartnerClassObjectFieldConstraint[0],
                                true
                        );
                    } else {
                        pcoc = new PartnerClassObjectInitializationConstraint(
                                c,
                                id,
                                isNull,
                                reservedId,
                                potentialIds,
                                new PartnerClassObjectFieldConstraint[0],
                                true
                        );
                    }
                }
                se.addNewPartnerClassObjectConstraint(pcoc);
            }
            AliasingInformation.addAliasingTarget(c, pc);
        }
    }

    private void restrictLength(SymbolicExecution se, Sint len) {
        if (len instanceof ConcSnumber) {
            if (((ConcSnumber) len).intVal() < 0) {
                throw new NegativeArraySizeException();
            }
        } else if (throwExceptionOnOOB) {
            Sbool outOfBounds = se.gt(Sint.ConcSint.ZERO, len);
            if (se.boolChoice(outOfBounds)) {
                throw new NegativeArraySizeException();
            }
        } else if (!se.nextIsOnKnownPath()) {
            _addLengthLteZeroConstraint(se, len);
        }
    }

    protected abstract void _addLengthLteZeroConstraint(SymbolicExecution se, Sint len);
}
