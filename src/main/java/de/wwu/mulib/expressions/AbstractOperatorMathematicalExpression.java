package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract supertype for all those mathematical expressions that consist of two mathematical expressions and an operator
 */
public abstract class AbstractOperatorMathematicalExpression implements Expression, Sym {

    protected final Expression expr0;
    protected final Expression expr1;

    protected AbstractOperatorMathematicalExpression(Expression expr0, Expression expr1) {
        assert !(expr0 instanceof ConcSnumber) || !(expr1 instanceof ConcSnumber);
        assert !(expr0 instanceof ConcolicMathematicalContainer) && !(expr1 instanceof ConcolicMathematicalContainer);
        if (expr0 instanceof SymSnumber) {
            expr0 = ((SymSnumber) expr0).getRepresentedExpression();
        }
        if (expr1 instanceof SymSnumber) {
            expr1 = ((SymSnumber) expr1).getRepresentedExpression();
        }
        this.expr0 = expr0;
        this.expr1 = expr1;
    }

    /**
     * @return The first expression
     */
    public final Expression getExpr0() {
        return expr0;
    }

    /**
     * @return The second expression
     */
    public final Expression getExpr1() {
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
        AbstractOperatorMathematicalExpression oc = (AbstractOperatorMathematicalExpression) o;
        return this.getExpr0().equals(oc.getExpr0()) && this.getExpr1().equals(oc.getExpr1());
    }

    /**
     * Checks for nested repetitions of the same type.
     * For instance, if {@link Sum} is unrolled, and there is an expression such as
     * ((n0 + n1) + (n2 * n3)), [n0, n1, (n2 * n3)] is returned.
     * @return A list of expressions
     */
    public List<Expression> unrollSameType() {
        List<Expression> result = new ArrayList<>();

        ArrayDeque<AbstractOperatorMathematicalExpression> sameType = new ArrayDeque<>();
        sameType.add(this);
        Class<?> thisClass = getClass();
        while (!sameType.isEmpty()) {
            AbstractOperatorMathematicalExpression n = sameType.poll();
            if (n.getExpr0().getClass() == thisClass) {
                sameType.add((AbstractOperatorMathematicalExpression) n.getExpr0());
            } else {
                result.add(n.getExpr0());
            }
            if (n.getExpr1().getClass() == thisClass) {
                sameType.add((AbstractOperatorMathematicalExpression) n.getExpr1());
            } else {
                result.add(n.getExpr1());
            }

        }

        return result;
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
