package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.ConcolicNumericalContainer;
import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

/**
 * Abstract supertype for those constraints that compare two numeric expressions
 */
public abstract class AbstractTwoSidedNumericConstraint implements TwoSidedExpressionConstraint {

    protected final NumericalExpression lhsExpr;
    protected final NumericalExpression rhsExpr;

    protected AbstractTwoSidedNumericConstraint(NumericalExpression lhsExpr, NumericalExpression rhsExpr) {
        assert !(lhsExpr instanceof ConcSnumber) || !(rhsExpr instanceof ConcSnumber);
        assert !(lhsExpr instanceof ConcolicNumericalContainer) && !(rhsExpr instanceof ConcolicNumericalContainer);
        if (lhsExpr instanceof SymSnumber) {
            lhsExpr = ((SymSnumber) lhsExpr).getRepresentedExpression();
        }
        if (rhsExpr instanceof SymSnumber) {
            rhsExpr = ((SymSnumber) rhsExpr).getRepresentedExpression();
        }
        assert !(lhsExpr instanceof ConcolicNumericalContainer) && !(rhsExpr instanceof ConcolicNumericalContainer);
        this.lhsExpr = lhsExpr;
        this.rhsExpr = rhsExpr;
    }

    protected static boolean bothExprAreConcrete(NumericalExpression lhs, NumericalExpression rhs) {
        return lhs instanceof ConcSnumber && rhs instanceof ConcSnumber;
    }

    @Override
    public final NumericalExpression getLhs() {
        return lhsExpr;
    }

    @Override
    public final NumericalExpression getRhs() {
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
