package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.HashSet;
import java.util.Set;

/**
 * Representation of an array of partner class objects that potentially is an alias of another, pre-existing array.
 */
public class AliasingPartnerClassArraySolverRepresentation extends AliasingPrimitiveValuedArraySolverRepresentation implements PartnerClassArraySolverRepresentation {

    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps;
    private final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr;

    /**
     * Constructs a new instance
     * @param config The configuration
     * @param aic The constraint initializing this representation
     * @param level The level
     * @param potentialIds The identifiers of potential aliasing targets
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param containingContainerIsCompletelyInitialized Whether all values of the container (field or array) are known
     */
    AliasingPartnerClassArraySolverRepresentation(
            MulibConfig config,
            ArrayInitializationConstraint aic,
            int level,
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            boolean containingContainerIsCompletelyInitialized) {
        super(config, aic, level, potentialIds, asr, containingContainerIsCompletelyInitialized);
        this.sps = sps;
        this.asr = asr;
        assert aic.getValueType().isArray() || PartnerClass.class.isAssignableFrom(aic.getValueType());
    }

    /**
     * Copy constructor
     * @param aaasr To-copy
     * @param level The level to copy for
     */
    AliasingPartnerClassArraySolverRepresentation(
            AliasingPartnerClassArraySolverRepresentation aaasr,
            int level) {
        super(aaasr, level);
        this.asr = aaasr.asr;
        this.sps = aaasr.sps;
    }

    /**
     * Constructor for generating lazily
     * @param config The configuration
     * @param id The identifier, will be constrained to either be reservedId or one of the potential identifiers
     * @param length The length
     * @param isNull Whether the array is null
     * @param valueType The component type
     * @param defaultIsSymbolic Whether the default value is symbolic
     * @param level The level to create this representation for
     * @param reservedId The identifier id is equal, if this represents a new array
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param cannotBeNewInstance Whether this is can be a new instance
     * @param isCompletelyInitialized Whether we know all values of this array
     * @param canPotentiallyContainCurrentlyUnrepresentedDefaults Whether there might be values contained in this array
     *                                                            that are not yet known.
     * @param potentialIds The identifiers one of which 'id' is equal to, i.e., aliasing targets
     */
    protected AliasingPartnerClassArraySolverRepresentation(
            MulibConfig config,
            Sint id,
            Sint length,
            Sbool isNull,
            Class<?> valueType,
            boolean defaultIsSymbolic,
            int level,
            Sint reservedId,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            boolean cannotBeNewInstance,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedDefaults,
            Set<Sint> potentialIds) {
        super(config, id, length, isNull, valueType, defaultIsSymbolic, level, reservedId, asr, cannotBeNewInstance,
                isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedDefaults, potentialIds);
        this.sps = sps;
        this.asr = asr;
    }

    @Override @SuppressWarnings("unchecked")
    public Set<Sint> getValuesKnownToPossiblyBeContainedInArray() {
        Set<Sint> result;
        if (!cannotBeNewInstance) {
            result = (Set<Sint>) currentRepresentation.getValuesKnownToPossiblyBeContainedInArray(isCompletelyInitialized);
        } else {
            result = new HashSet<>();
        }

        for (IncrementalSolverState.PartnerClassObjectRepresentation<ArraySolverRepresentation> ar : aliasedArrays) {
            ArraySolverRepresentation asr = getAliasLevelSafe(ar);
            assert asr instanceof PartnerClassArraySolverRepresentation && asr.getLevel() >= level;
            result.addAll(((PartnerClassArraySolverRepresentation) asr).getValuesKnownToPossiblyBeContainedInArray());
        }

        return result;
    }

    @Override
    public AliasingPartnerClassArraySolverRepresentation copyForNewLevel(int level) {
        return new AliasingPartnerClassArraySolverRepresentation(
                this, level
        );
    }

    @Override
    protected Constraint _select(Constraint guard, Sint index, Sprimitive selectedValue) {
        assert selectedValue != Sint.ConcSint.MINUS_ONE;
        return super._select(guard, index, selectedValue);
    }

    @Override
    protected void _store(Constraint guard, Sint index, Sprimitive storedValue) {
        if (storedValue == null) {
            storedValue = Sint.ConcSint.MINUS_ONE;
        }
        super._store(guard, index, storedValue);
    }
}
