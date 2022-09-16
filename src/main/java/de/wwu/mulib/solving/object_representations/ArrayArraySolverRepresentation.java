package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.ArrayInitializationConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

public class ArrayArraySolverRepresentation extends AbstractArraySolverRepresentation implements IArrayArraySolverRepresentation {

    public ArrayArraySolverRepresentation(ArrayInitializationConstraint aic, int level) {
        this(
                aic.getArrayId(),
                aic.getArrayLength(),
                aic.getIsNull(),
                level,
                new ArrayHistorySolverRepresentation(aic.getInitialSelectConstraints()),
                aic.isCompletelyInitialized(),
                aic.getValueType()
        );
        assert aic.getValueType().isArray();

    }

    private ArrayArraySolverRepresentation(
            Sint arrayId,
            Sint length,
            Sbool isNull,
            int level,
            ArrayHistorySolverRepresentation ahsr,
            boolean isCompletelyInitialized,
            Class<?> valueType) {
        super(arrayId, length, isNull, level, ahsr, isCompletelyInitialized, valueType);
    }

    @Override
    public Set<Sint> getPotentialValues() {
        return (Set<Sint>) currentRepresentation.getPotentialValues(); // TODO better name; if is fully initialized, potential values are initialconcreteandstoredvalues
    }

    @Override
    public Set<Sint> getInitialConcreteAndStoredValues() {
        return (Set<Sint>) currentRepresentation.getInitialConcreteAndStoredValues();
    }

    @Override
    public IArrayArraySolverRepresentation copyForNewLevel(int level) {
        return new ArrayArraySolverRepresentation(
                arrayId,
                length,
                isNull,
                level,
                currentRepresentation.copy(),
                isCompletelyInitialized,
                valueType
        );
    }

    @Override
    public Constraint select(Constraint guard, Sint index, Sprimitive selectedValue) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    public void store(Constraint guard, Sint index, Sprimitive storedValue) {
        throw new NotYetImplementedException(); //// TODO
    }
}
