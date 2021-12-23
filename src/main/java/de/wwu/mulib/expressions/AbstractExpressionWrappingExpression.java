package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

public abstract class AbstractExpressionWrappingExpression implements NumericExpression, Sym {

    protected final NumericExpression wrapped;

    protected AbstractExpressionWrappingExpression(NumericExpression toWrap) {
        assert !(toWrap instanceof ConcSnumber);
        assert !(toWrap instanceof ConcolicNumericContainer);
        if (toWrap instanceof SymNumericExpressionSprimitive) {
            toWrap = ((SymNumericExpressionSprimitive) toWrap).getRepresentedExpression();
        }
        this.wrapped = toWrap;
    }


    @Override
    public boolean isFp() {
        return wrapped.isFp();
    }

    public final NumericExpression getWrapped() {
        return wrapped;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "wrapped=" + wrapped
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!this.getClass().equals(o.getClass())) {
            return false;
        }
        return wrapped.equals(((AbstractExpressionWrappingExpression) o).getWrapped());
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }
}
