package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.executors.AliasingInformation;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractValueFactory implements ValueFactory {
    protected final boolean enableInitializeFreeArraysWithNull;
    protected final boolean enableInitializeFreeObjectsWithNull;
    protected final boolean aliasingForFreeObjects;
    protected final boolean throwExceptionOnOOB;
    protected final Function<Snumber, Snumber> tryGetSymFromSnumber;
    protected final Function<Sbool, Sbool> tryGetSymFromSbool;


    protected final MulibConfig config;

    public AbstractValueFactory(MulibConfig config) {
        this.enableInitializeFreeArraysWithNull = config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        this.enableInitializeFreeObjectsWithNull = config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
        this.aliasingForFreeObjects = config.ALIASING_FOR_FREE_OBJECTS;
        this.throwExceptionOnOOB = config.THROW_EXCEPTION_ON_OOB;
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
    public Sint.ConcSint concSint(int i) {
        return (Sint.ConcSint) Sint.concSint(i);
    }

    @Override
    public Sdouble.ConcSdouble concSdouble(double d) {
        return (Sdouble.ConcSdouble) Sdouble.concSdouble(d);
    }

    @Override
    public Sfloat.ConcSfloat concSfloat(float f) {
        return (Sfloat.ConcSfloat) Sfloat.concSfloat(f);
    }

    @Override
    public Sbool.ConcSbool concSbool(boolean b) {
        return (Sbool.ConcSbool) Sbool.concSbool(b);
    }

    @Override
    public Slong.ConcSlong concSlong(long l) {
        return (Slong.ConcSlong) Slong.concSlong(l);
    }

    @Override
    public Sshort.ConcSshort concSshort(short s) {
        return (Sshort.ConcSshort) Sshort.concSshort(s);
    }

    @Override
    public Sbyte.ConcSbyte concSbyte(byte b) {
        return (Sbyte.ConcSbyte) Sbyte.concSbyte(b);
    }

    @Override
    public Schar.ConcSchar concSchar(char c) {
        return (Schar.ConcSchar) Schar.concSchar(c);
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
        Sarray.PartnerClassSarray result = new Sarray.PartnerClassSarray(clazz, len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(clazz, result, se);
        return result;
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        Sarray.SarraySarray result = new Sarray.SarraySarray(len, se, defaultIsSymbolic, clazz, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
        decideOnAddToAliasingAndRepresentation(clazz, result, se);
        return result;
    }

    @Override
    public final Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
        // Never is part of aliasing
        return new Sarray.SarraySarray(lengths, se, clazz);
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf) {
        // defaultIsSymbolic is assumed
        return symObject(se, toGetInstanceOf, enableInitializeFreeObjectsWithNull);
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf, boolean canBeNull) {
        try {
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
                                pc.__mulib__getFieldNameToType(),
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
                                pc.__mulib__getFieldNameToType(),
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
