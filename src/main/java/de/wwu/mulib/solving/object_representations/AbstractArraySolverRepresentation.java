package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public abstract class AbstractArraySolverRepresentation implements ArraySolverRepresentation {
    protected ArrayHistorySolverRepresentation currentRepresentation;
    protected final Sint arrayId;
    protected final Sint length;
    protected final Sbool isNull;
    protected final int level;

    protected AbstractArraySolverRepresentation(
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
}
