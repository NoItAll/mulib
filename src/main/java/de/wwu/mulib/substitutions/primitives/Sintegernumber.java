package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.search.executors.SymbolicExecution;

public abstract class Sintegernumber extends AbstractSnumber {

    public final Sint add(Sintegernumber rhs, SymbolicExecution se) {
        return se.add(this, rhs, Sint.class);
    }

    public final Sint sub(Sintegernumber rhs, SymbolicExecution se) {
        return se.sub(this, rhs, Sint.class);
    }

    public final Sint div(Sintegernumber rhs, SymbolicExecution se) {
        return se.div(this, rhs, Sint.class);
    }

    public final Sint mul(Sintegernumber rhs, SymbolicExecution se) {
        return se.mul(this, rhs, Sint.class);
    }

    public final Sint mod(Sintegernumber rhs, SymbolicExecution se) {
        return se.mod(this, rhs, Sint.class);
    }

    public final Sint neg(SymbolicExecution se) {
        return se.neg(this, Sint.class);
    }

    @Override
    public final boolean isFp() {
        return false;
    }

}
