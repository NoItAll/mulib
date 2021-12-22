package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

public abstract class AbstractOperatorNumericExpression implements NumericExpression, Sym {

    protected final NumericExpression expr0;
    protected final NumericExpression expr1;

    protected AbstractOperatorNumericExpression(NumericExpression expr0, NumericExpression expr1) {
        assert !(expr0 instanceof ConcSnumber) || !(expr1 instanceof ConcSnumber);
        this.expr0 = expr0 instanceof SymNumericExpressionSprimitive ?
                ((SymNumericExpressionSprimitive) expr0).getRepresentedExpression()
                :
                expr0;
        this.expr1 = expr1 instanceof SymNumericExpressionSprimitive ?
                ((SymNumericExpressionSprimitive) expr1).getRepresentedExpression()
                :
                expr1;
    }

    public final NumericExpression getExpr0() {
        return expr0;
    }

    public final NumericExpression getExpr1() {
        return expr1;
    }

    private byte cachedIsFp = -1;
    @Override
    public boolean isFp() {
        if (cachedIsFp == -1) {
            cachedIsFp = (byte) ((expr0.isFp() || expr1.isFp()) ? 1 : 0);
        }
        return cachedIsFp == 1;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "expr0=" + expr0
                + ",expr1=" + expr1
                + ",isFp=" + isFp()
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
        AbstractOperatorNumericExpression oc = (AbstractOperatorNumericExpression) o;
        return this.getExpr0().equals(oc.getExpr0()) && this.getExpr1().equals(oc.getExpr1());
    }

    private int cachedHash = -1;
    @Override
    public int hashCode() {
        if (cachedHash == -1) {
            cachedHash = getExpr0().hashCode() + getExpr1().hashCode();
        }
        return cachedHash;
    }
}
