package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.executors.CalculationFactory;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

/**
 * Represents an array with Sint --> Sprimitive. Is also used to symbolically represent an array of arrays
 * using the IDs of the respective Sarrays.
 */
public class PrimitiveValuedArraySolverRepresentation extends AbstractArraySolverRepresentation {

    protected PrimitiveValuedArraySolverRepresentation(CalculationFactory calculationFactory, Sint arrayId, Sint length, Sbool isNull, int level) {
        this(calculationFactory, arrayId, length, isNull, level, new ArrayHistorySolverRepresentation());
    }

    public PrimitiveValuedArraySolverRepresentation(CalculationFactory calculationFactory, Sint arrayId, Sint length, Sbool isNull, int level, ArrayHistorySolverRepresentation ahsr) {
        super(calculationFactory, arrayId, length, isNull, level, ahsr);
    }

    @Override
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        return currentRepresentation.select(calculationFactory, guard, index, selectedValue);
    }

    @Override
    public void store(Constraint guard, Sint index, Sprimitive storedValue) {
        this.currentRepresentation = currentRepresentation.store(calculationFactory, guard, index, storedValue);
    }

    @Override
    public PrimitiveValuedArraySolverRepresentation copyForNewLevel(int level) {
        return new PrimitiveValuedArraySolverRepresentation(calculationFactory, arrayId, length, isNull, level, currentRepresentation.copy());
    }

    @Override
    public Set<? extends Sprimitive> getPotentialValues() {
        return currentRepresentation.getPotentialValues();
    }

}
