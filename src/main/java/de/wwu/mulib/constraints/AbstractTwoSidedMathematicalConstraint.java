package de.wwu.mulib.constraints;

import de.wwu.mulib.expressions.ConcolicMathematicalContainer;
import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

/**
 * Abstract supertype for those constraints that compare two mathematical expressions
 */
public abstract class AbstractTwoSidedMathematicalConstraint implements TwoSidedExpressionConstraint {

    protected final Expression lhsExpr;
    protected final Expression rhsExpr;

    protected AbstractTwoSidedMathematicalConstraint(Expression lhsExpr, Expression rhsExpr) {
        assert !(lhsExpr instanceof ConcSnumber) || !(rhsExpr instanceof ConcSnumber);
        assert !(lhsExpr instanceof ConcolicMathematicalContainer) && !(rhsExpr instanceof ConcolicMathematicalContainer);
        if (lhsExpr instanceof SymSnumber) {
            lhsExpr = ((SymSnumber) lhsExpr).getRepresentedExpression();
        }
        if (rhsExpr instanceof SymSnumber) {
            rhsExpr = ((SymSnumber) rhsExpr).getRepresentedExpression();
        }
        assert !(lhsExpr instanceof ConcolicMathematicalContainer) && !(rhsExpr instanceof ConcolicMathematicalContainer);
        this.lhsExpr = lhsExpr;
        this.rhsExpr = rhsExpr;
    }

    protected static boolean bothExprAreConcrete(Expression lhs, Expression rhs) {
        return lhs instanceof ConcSnumber && rhs instanceof ConcSnumber;
    }

    @Override
    public final Expression getLhs() {
        return lhsExpr;
    }

    @Override
    public final Expression getRhs() {
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
        AbstractTwoSidedMathematicalConstraint oc = (AbstractTwoSidedMathematicalConstraint) o;
        return this.getLhs().equals(oc.getLhs()) && this.getRhs().equals(oc.getRhs());
    }

    @Override
    public int hashCode() {
        return getLhs().hashCode() + getRhs().hashCode();
    }
}
