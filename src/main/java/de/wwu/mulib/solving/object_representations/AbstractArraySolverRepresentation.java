package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public abstract class AbstractArraySolverRepresentation implements ArraySolverRepresentation {
    protected final MulibConfig config;
    protected ArrayHistorySolverRepresentation currentRepresentation;
    protected final Sint arrayId;
    protected final Sint length;
    protected final Sbool isNull;
    protected final int level;
    protected boolean isCompletelyInitialized;
    protected final Class<?> valueType;
    protected boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    protected final boolean defaultIsSymbolic;

    protected AbstractArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            int level) {
        this.config = config;
        this.arrayId = aic.getPartnerClassObjectId();
        this.length = aic.getArrayLength();
        this.isNull = aic.getIsNull();
        assert isNull != Sbool.ConcSbool.TRUE || arrayId == Sint.ConcSint.MINUS_ONE;
        this.level = level;
        this.valueType = aic.getValueType();
        this.defaultIsSymbolic = aic.defaultIsSymbolic();
        this.isCompletelyInitialized =
                (length instanceof ConcSnumber && ((ConcSnumber) length).intVal() == aic.getInitialSelectConstraints().length);
        if (length instanceof Sym) {
            // We should track index accesses so that unset indices are forced to be the default value only if the
            // default is not symbolic (any value can be returned then) and if the length is symbolic.
            // If the length is not symbolic, we pre-initialize all values.

            // If the value is an array or an object there is special behavior: It can be symbolically initialized to
            // be null which is treated differently from other sarrays and must be treated explicitly. Note that this is
            // different from, e.g., SintSarray, where the default value ConcSint{0} can be assumed by SymSint as well
            // without failing to illustrate special behavior.
            if (valueType.isArray()) {
                this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault = !defaultIsSymbolic || config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
            } else if (PartnerClass.class.isAssignableFrom(valueType)) {
                this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault = !defaultIsSymbolic || config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
            } else if (Sprimitive.class.isAssignableFrom(valueType)) {
                this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault = !defaultIsSymbolic;
            } else {
                throw new NotYetImplementedException();
            }
        } else {
            // Is eagerly initialized, thus there is no unrepresented non-symbolic default
            assert isCompletelyInitialized;
            this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault = false;
        }
        this.currentRepresentation =
                new ArrayHistorySolverRepresentation(
                        aic.getInitialSelectConstraints(),
                        aic.getValueType()
                );
    }

    /**
     * Constructor for generating lazily
     */
    protected AbstractArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault,
            int level) {
        this.length = length;
        this.isCompletelyInitialized = isCompletelyInitialized;
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault =
                canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
        this.config = config;
        this.arrayId = id;
        this.isNull = isNull;
        this.valueType = valueType;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.level = level;
    }


    /**
     * Copy constructor
     */
    protected AbstractArraySolverRepresentation(
            AbstractArraySolverRepresentation aasr,
            int level) {
        this.config = aasr.config;
        this.arrayId = aasr.arrayId;
        this.length = aasr.length;
        this.isNull = aasr.isNull;
        this.level = level;
        this.currentRepresentation = aasr.currentRepresentation.copy();
        this.isCompletelyInitialized = aasr.isCompletelyInitialized;
        this.valueType = aasr.valueType;
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault =
                aasr.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
        this.defaultIsSymbolic = aasr.defaultIsSymbolic;
    }


    @Override
    public final Constraint select(Sint index, Sprimitive selectedValue) {
        assert selectedValue != null;
        return _select(Sbool.ConcSbool.TRUE, index, selectedValue);
    }

    @Override
    public final void store(Sint index, Sprimitive storedValue) {
        _store(Sbool.ConcSbool.TRUE, index, storedValue);
    }

    @Override
    public final Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        return _select(guard, index, selectedValue);
    }

    @Override
    public final void store(Constraint guard, Sint index, Sprimitive storedValue) {
        _store(guard, index, storedValue);
    }

    protected abstract Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue);

    protected abstract void _store(Constraint guard, Sint index, Sprimitive storedValue);

    public Sint getArrayId() {
        return arrayId;
    }

    public Sint getLength() {
        return length;
    }

    public Sbool getIsNull() {
        return isNull;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractArraySolverRepresentation && ((AbstractArraySolverRepresentation) o).arrayId.equals(arrayId);
    }

    @Override
    public int hashCode() {
        return arrayId.hashCode();
    }

    @Override
    public boolean isCompletelyInitialized() {
        return isCompletelyInitialized;
    }

    @Override
    public Class<?> getElementType() {
        return valueType;
    }

    public ArrayHistorySolverRepresentation getCurrentRepresentation() {
        return currentRepresentation;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault() {
        return canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    }

    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }
}
