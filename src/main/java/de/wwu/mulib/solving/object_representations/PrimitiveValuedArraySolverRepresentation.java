package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays.
 */
public class PrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation {

    public PrimitiveValuedArraySolverRepresentation(ArrayInitializationConstraint aic, int level) {
        this(
                aic.getArrayId(),
                aic.getArrayLength(),
                aic.getIsNull(),
                level,
                new ArrayHistorySolverRepresentation(),
                aic.isCompletelyInitialized()
        );
        for (ArrayAccessConstraint aac : aic.getInitialSelectConstraints()) {
            select(aac.getIndex(), aac.getValue());
        }
    }

    private PrimitiveValuedArraySolverRepresentation(
            Sint arrayId,
            Sint length,
            Sbool isNull,
            int level,
            ArrayHistorySolverRepresentation ahsr,
            boolean isCompletelyInitialized) {
        super(arrayId, length, isNull, level, ahsr, isCompletelyInitialized);
    }

    @Override
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        return currentRepresentation.select(guard, index, selectedValue);
    }

    @Override
    public void store(Constraint guard, Sint index, Sprimitive storedValue) {
        this.currentRepresentation = currentRepresentation.store(guard, index, storedValue);
    }

    @Override
    public PrimitiveValuedArraySolverRepresentation copyForNewLevel(int level) {
        return new PrimitiveValuedArraySolverRepresentation(
                arrayId,
                length,
                isNull,
                level,
                currentRepresentation.copy(),
                isCompletelyInitialized
        );
    }

    @Override
    public Set<? extends Sprimitive> getPotentialValues() {
        return currentRepresentation.getPotentialValues(); // TODO better name; if is fully initialized, potential values are initialconcreteandstoredvalues
    }

    @Override
    public Set<? extends Sprimitive> getInitialConcreteAndStoredValues() {
        return currentRepresentation.getInitialConcreteAndStoredValues();
    }

}
