package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

public class Neg implements NumericalExpression, Sym {

    private final NumericalExpression wrapped;

    private Neg(NumericalExpression toWrap) {
        assert !(toWrap instanceof ConcSnumber);
        assert !(toWrap instanceof ConcolicNumericalContainer);
        if (toWrap instanceof SymSnumber) {
            toWrap = ((SymSnumber) toWrap).getRepresentedExpression();
        }
        this.wrapped = toWrap;
    }

    public static NumericalExpression neg(NumericalExpression wrapped) {
        return wrapped instanceof Neg ?
                ((Neg) wrapped).getWrapped()
                :
                new Neg(wrapped);
    }

    @Override
    public String toString() {
        return "-(" + wrapped + ")";
    }

    @Override
    public boolean isFp() {
        return wrapped.isFp();
    }

    public final NumericalExpression getWrapped() {
        return wrapped;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Neg)) {
            return false;
        }
        return wrapped.equals(((Neg) o).getWrapped());
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }
}
