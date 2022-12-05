package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractValueFactory implements ValueFactory {
    protected final boolean enableInitializeFreeArraysWithNull;
    protected final boolean enableInitializeFreeObjectsWithNull;
    protected final boolean aliasingForFreeArrays;
    protected final boolean aliasingForFreeObjects;
    protected final boolean throwExceptionOnOOB;

    protected final MulibConfig config;

    public AbstractValueFactory(MulibConfig config) {
        this.enableInitializeFreeArraysWithNull = config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        this.enableInitializeFreeObjectsWithNull = config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
        this.aliasingForFreeArrays = config.ALIASING_FOR_FREE_ARRAYS;
        this.aliasingForFreeObjects = config.ALIASING_FOR_FREE_OBJECTS;
        this.throwExceptionOnOOB = config.THROW_EXCEPTION_ON_OOB;
        this.config = config;
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
        return new Sarray.SintSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SdoubleSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SfloatSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SlongSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SshortSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SbyteSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SboolSarray(len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.PartnerClassSarray(clazz, len, se, defaultIsSymbolic, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean defaultIsSymbolic, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SarraySarray(len, se, defaultIsSymbolic, clazz, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
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
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new MulibIllegalStateException("The SymbolicExecution-constructor must be there!");
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
