package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

public abstract class AbstractOperatorNumericExpression implements NumericExpression {

    protected final NumericExpression expr0;
    protected final NumericExpression expr1;
    protected final boolean explicitlyTreatAsInteger;

    protected AbstractOperatorNumericExpression(NumericExpression expr0, NumericExpression expr1) {
        this(expr0, expr1, false);
    }

    protected AbstractOperatorNumericExpression(NumericExpression expr0, NumericExpression expr1, boolean explicitlyTreatAsInteger) {
        this.expr0 = expr0 instanceof SymNumericExpressionSprimitive ?
                ((SymNumericExpressionSprimitive) expr0).getRepresentedExpression()
                :
                expr0;
        this.expr1 = expr1 instanceof SymNumericExpressionSprimitive ?
                ((SymNumericExpressionSprimitive) expr1).getRepresentedExpression()
                :
                expr1;
        this.explicitlyTreatAsInteger = explicitlyTreatAsInteger;
    }

    protected static boolean bothExprAreConcrete(NumericExpression expr0, NumericExpression expr1) {
        return expr0 instanceof ConcSnumber && expr1 instanceof ConcSnumber;
    }

    public final NumericExpression getExpr0() {
        return expr0;
    }

    public final NumericExpression getExpr1() {
        return expr1;
    }

    @Override
    public final boolean isFp() {
        return !explicitlyTreatAsInteger && (expr0.isFp() || expr1.isFp());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "expr0=" + expr0
                + ",expr1=" + expr1
                + ",explicitlyTreatAsInt=" + explicitlyTreatAsInteger
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

    @Override
    public int hashCode() {
        return getExpr0().hashCode() + getExpr1().hashCode();
    }
}
