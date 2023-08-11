package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

public class Neg implements NumericExpression, Sym {

    private final NumericExpression wrapped;

    private Neg(NumericExpression toWrap) {
        assert !(toWrap instanceof ConcSnumber);
        assert !(toWrap instanceof ConcolicNumericContainer);
        if (toWrap instanceof SymNumericExpressionSprimitive) {
            toWrap = ((SymNumericExpressionSprimitive) toWrap).getRepresentedExpression();
        }
        this.wrapped = toWrap;
    }

    public static NumericExpression neg(NumericExpression wrapped) {
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

    public final NumericExpression getWrapped() {
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
