package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

/**
 * Abstract implementation of a representation of an array for the constraint solver.
 * Keeps track of the main metadata, such as the identifier, the length, whether the array can be null, the component type,
 * and the level this representation is for.
 * Furthermore keeps track of other specific metadata, i.e. whether the array is completely initialized, if the
 * array returns symbolic values for unknown elements, and whether unrepresented non symbolic default values (such as
 * 'null' for references or '0' for ints) might be present in the array
 */
public abstract class AbstractArraySolverRepresentation implements ArraySolverRepresentation {
    /**
     * The configuration
     */
    protected final MulibConfig config;
    /**
     * The object holding information on the (index, value)-pairs in the array and is used for selecting and
     * storing new array-stores
     */
    protected ArrayHistorySolverRepresentation currentRepresentation;
    /**
     * The identifier of the array
     */
    protected final Sint arrayId;
    /**
     * The length of the array
     */
    protected final Sint length;
    /**
     * Whether the array is null or not
     */
    protected final Sbool isNull;
    /**
     * The level of this array
     */
    protected final int level;
    /**
     * Whether the array is completely initialized
     */
    protected boolean isCompletelyInitialized;
    /**
     * The component type
     */
    protected final Class<?> valueType;
    /**
     * Whether the array can potentially contain non-symbolic default values that are not yet seen
     */
    protected boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    /**
     * Whether the default values of this array are symbolic
     */
    protected final boolean defaultIsSymbolic;

    /**
     * @param config The configuration
     * @param aic The array initialization constraint
     * @param level The level
     */
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
     * @param config The configuration
     * @param id The identifier
     * @param length The length of the array
     * @param isNull Whether the array is null
     * @param valueType The component type
     * @param defaultIsSymbolic Whether the default value is symbolic
     * @param isCompletelyInitialized Whether the array is completely initialized
     * @param canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault Whether there potentially are unrepresented
     *                                                                      non-symbolic default values
     * @param level The level of this representation
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
        this.currentRepresentation = new ArrayHistorySolverRepresentation(new ArrayAccessConstraint[0], valueType);
    }


    /**
     * Copy constructor
     * @param aasr The representation to copy
     * @param level The level to copy for
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

    /**
     * Should be overridden to differentiate between aliasing array solver representations and "simple" array
     * solver representations that represent only themselves.
     * @param guard The guard determining whether the select should be valid
     * @param index The index to read from
     * @param selectedValue The value that is checked to be selected
     * @return The select constraint
     */
    protected abstract Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue);

    /**
     * Should be overridden to differentiate between aliasing array solver representations and "simple" array
     * solver representations that represent only themselves.
     * Modifies this representation and, in the case of symbolic aliasing, the other representations conditionally.
     * @param guard The guard determining whether the store should be valid
     * @param index The index to read from
     * @param storedValue The value that is checked to be stored
     */
    protected abstract void _store(Constraint guard, Sint index, Sprimitive storedValue);

    @Override
    public Sint getArrayId() {
        return arrayId;
    }

    @Override
    public Sint getLength() {
        return length;
    }

    @Override
    public Sbool getIsNull() {
        return isNull;
    }

    @Override
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

    @Override
    public boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault() {
        return canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
    }

    @Override
    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }
}
