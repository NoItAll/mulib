package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public abstract class AbstractTwoSidedNumericConstraint implements TwoSidedExpressionConstraint {

    protected final NumericExpression lhsExpr;
    protected final NumericExpression rhsExpr;

    protected AbstractTwoSidedNumericConstraint(NumericExpression lhsExpr, NumericExpression rhsExpr) {
        this.lhsExpr = lhsExpr;
        this.rhsExpr = rhsExpr;
    }

    protected static boolean bothExprAreConcrete(NumericExpression lhs, NumericExpression rhs) {
        return lhs instanceof ConcSnumber && rhs instanceof ConcSnumber;
    }

    @Override
    public final NumericExpression getLhs() {
        return lhsExpr;
    }

    @Override
    public final NumericExpression getRhs() {
        return rhsExpr;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "lhs=" + lhsExpr
                + ",rhs=" + rhsExpr
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
        AbstractTwoSidedNumericConstraint oc = (AbstractTwoSidedNumericConstraint) o;
        return this.getLhs().equals(oc.getLhs()) && this.getRhs().equals(oc.getRhs());
    }

    @Override
    public int hashCode() {
        return getLhs().hashCode() + getRhs().hashCode();
    }
}
