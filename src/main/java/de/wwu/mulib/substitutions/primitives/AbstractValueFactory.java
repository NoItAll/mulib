package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

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
    public final Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sintSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sdoubleSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sfloatSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return slongSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);

    }

    @Override
    public final Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sshortSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sbyteSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return sboolSarray(se, len, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements) {
        return partnerClassSarray(se, len, clazz, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements) {
        return sarraySarray(se, len, clazz, freeElements, enableInitializeFreeArraysWithNull);
    }

    @Override
    public final Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SintSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SdoubleSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SfloatSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SlongSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SshortSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SbyteSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SboolSarray(len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.PartnerClassSarray(clazz, len, se, freeElements, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements, boolean canBeNull) {
        restrictLength(se, len);
        return new Sarray.SarraySarray(len, se, freeElements, clazz, canBeNull ? se.symSbool() : Sbool.ConcSbool.FALSE);
    }

    @Override
    public final Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
        return new Sarray.SarraySarray(lengths, se, clazz);
    }

    private void restrictLength(SymbolicExecution se, Sint len) {
        if (len instanceof ConcSnumber) {
            if (((ConcSnumber) len).intVal() < 0) {
                throw new NegativeArraySizeException();
            }
        } else if (throwExceptionOnOOB) {
            Sbool outOfBounds = se.gte(Sint.ConcSint.ZERO, len);
            if (se.boolChoice(outOfBounds)) {
                throw new NegativeArraySizeException();
            }
        } else if (!se.nextIsOnKnownPath()) {
            _addLengthLteZeroConstraint(se, len);
        }
    }

    protected abstract void _addLengthLteZeroConstraint(SymbolicExecution se, Sint len);
}
