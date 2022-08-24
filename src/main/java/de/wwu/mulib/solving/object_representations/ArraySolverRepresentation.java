package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public class ArraySolverRepresentation {
    private ArrayHistorySolverRepresentation currentRepresentation;
    private final Sint arrayId;
    private final Sint length;
    private final Sbool isNull;
    private final int level;

    public ArraySolverRepresentation(Sint arrayId, Sint length, Sbool isNull, int level) {
        this.arrayId = arrayId;
        this.length = length;
        this.isNull = isNull;
        this.level = level;
        this.currentRepresentation = new ArrayHistorySolverRepresentation();
    }

    public Constraint select(Sint index, SubstitutedVar value) {
        return currentRepresentation.select(index, value);
    }

    public void store(Sint index, SubstitutedVar value) {
        ArrayHistorySolverRepresentation ahsr = currentRepresentation.store(index, value);
        this.currentRepresentation = ahsr;
    }

    public ArraySolverRepresentation copyForNewLevel(int level) {
        ArraySolverRepresentation copy = new ArraySolverRepresentation(arrayId, length, isNull, level);
        copy.currentRepresentation = currentRepresentation.copy();
        return copy;
    }

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
        return o instanceof ArraySolverRepresentation && ((ArraySolverRepresentation) o).arrayId == arrayId;
    }

    @Override
    public int hashCode() {
        return arrayId.hashCode();
    }

}
