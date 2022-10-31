package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public abstract class AbstractPartnerClassObjectRepresentation implements PartnerClassObjectSolverRepresentation {

    protected final MulibConfig config;
    protected ArrayHistorySolverRepresentation currentRepresentation;
    protected final Sint id;
    protected final Sbool isNull;
    protected final int level;
    protected final Class<?> clazz;
    protected final boolean defaultIsSymbolic;

    protected AbstractPartnerClassObjectRepresentation(
            MulibConfig config,
            PartnerClassObjectInitializationConstraint pic,
            int level) {
        this.config = config;
        this.id = pic.getPartnerClassObjectId();
        this.isNull = pic.getIsNull();
        assert isNull != Sbool.ConcSbool.TRUE || id == Sint.ConcSint.MINUS_ONE;
        this.level = level;
        this.clazz = pic.getClazz();
        this.defaultIsSymbolic = pic.isDefaultIsSymbolic();
    }

    protected AbstractPartnerClassObjectRepresentation(AbstractPartnerClassObjectRepresentation apcor, int level) {
        this.config = apcor.config;
        this.id = apcor.id;
        this.isNull = apcor.isNull;
        this.level = level;
        this.clazz = apcor.clazz;
        this.defaultIsSymbolic = apcor.defaultIsSymbolic;
    }

    @Override
    public Constraint getField(String fieldName, Sprimitive value) {
        assert value != Sint.ConcSint.MINUS_ONE;
        return _getField(Sbool.ConcSbool.TRUE, fieldName, value);
    }

    @Override
    public void putField(String fieldName, Sprimitive value) {
        _putField(Sbool.ConcSbool.TRUE, fieldName, value);
    }

    protected abstract Constraint _getField(Constraint guard, String fieldName, Sprimitive value);

    protected abstract void _putField(Constraint guard, String fieldName, Sprimitive value);

    public MulibConfig getConfig() {
        return config;
    }

    public ArrayHistorySolverRepresentation getCurrentRepresentation() {
        return currentRepresentation;
    }

    @Override
    public Sint getId() {
        return id;
    }

    public Sbool isNull() {
        return isNull;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public Class<?> getClazz() {
        return clazz;
    }

    @Override
    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }
}
