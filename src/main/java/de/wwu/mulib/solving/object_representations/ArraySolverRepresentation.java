package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public class ArraySolverRepresentation {
    protected ArrayHistorySolverRepresentation currentRepresentation;
    protected final Sint arrayId;
    protected final Sint length;
    protected final Sbool isNull;
    protected final int level;

    public ArraySolverRepresentation(Sint arrayId, Sint length, Sbool isNull, int level) {
        this(arrayId, length, isNull, level, new ArrayHistorySolverRepresentation());
    }

    protected ArraySolverRepresentation(
            Sint arrayId,
            Sint length,
            Sbool isNull,
            int level,
            ArrayHistorySolverRepresentation ahsr) {
        this.arrayId = arrayId;
        this.length = length;
        this.isNull = isNull;
        this.level = level;
        this.currentRepresentation = ahsr;
    }

    public Constraint select(Sint index, SubstitutedVar selectedValue) {
        return currentRepresentation.select(index, selectedValue);
    }

    public void store(Sint index, SubstitutedVar storedValue) {
        ArrayHistorySolverRepresentation ahsr = currentRepresentation.store(index, storedValue);
        this.currentRepresentation = ahsr;
    }

    public ArraySolverRepresentation copyForNewLevel(int level) {
        return new ArraySolverRepresentation(arrayId, length, isNull, level, currentRepresentation.copy());
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
        return o instanceof ArraySolverRepresentation && ((ArraySolverRepresentation) o).arrayId.equals(arrayId);
    }

    @Override
    public int hashCode() {
        return arrayId.hashCode();
    }
}
