package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractArraySolverRepresentation implements ArraySolverRepresentation {
    protected ArrayHistorySolverRepresentation currentRepresentation;
    protected final Sint arrayId;
    protected final Sint length;
    protected final Sbool isNull;
    protected final int level;
    protected final boolean isCompletelyInitialized;
    protected final Class<?> valueType;

    protected AbstractArraySolverRepresentation(
            Sint arrayId,
            Sint length,
            Sbool isNull,
            int level,
            ArrayHistorySolverRepresentation ahsr,
            boolean isCompletelyInitialized,
            Class<?> valueType) {
        this.arrayId = arrayId;
        this.length = length;
        this.isNull = isNull;
        this.level = level;
        this.currentRepresentation = ahsr;
        this.isCompletelyInitialized = isCompletelyInitialized;
        this.valueType = valueType;
    }

    @Override
    public final Constraint select(Sint index, Sprimitive selectedValue) {
        return select(Sbool.ConcSbool.TRUE, index, selectedValue);
    }

    @Override
    public final void store(Sint index, Sprimitive storedValue) {
        store(Sbool.ConcSbool.TRUE, index, storedValue);
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

    @Override
    public boolean isCompletelyInitialized() {
        return isCompletelyInitialized;
    }
}
