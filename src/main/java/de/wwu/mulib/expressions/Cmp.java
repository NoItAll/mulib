package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;

/**
 * The bytecode instructions to compare a long or a double do not return a boolean but rather a
 * 1, 0, or -1. If this case can not be simplified to a simple boolean, we instead return a symbolic Sint
 * wrapping cmp that must be regarded as a special, self-referring constraint.
 */
public final class Cmp extends AbstractOperatorNumericExpression {

    private final Constraint value1Larger;
    private final Constraint value1EqValue2;
    private final Constraint value1Less;

    private Cmp(NumericExpression lhs, NumericExpression rhs) {
        super(lhs, rhs);
        this.value1Larger = Gt.newInstance(lhs, rhs);
        this.value1EqValue2 = Eq.newInstance(lhs, rhs);
        this.value1Less = Lt.newInstance(lhs, rhs);
    }

    public static NumericExpression newInstance(
            NumericExpression lhs,
            NumericExpression rhs) {
        if (bothExprAreConcrete(lhs, rhs)) {
            ConcSnumber clhs = (ConcSnumber) lhs;
            ConcSnumber crhs = (ConcSnumber) rhs;
            int cmp = NumberUtil.compareConcSnumber(clhs, crhs);
            return Sint.ConcSint.newConcSint(cmp);
        }
        return new Cmp(lhs, rhs);
    }

    public Constraint getValue1Larger() {
        return value1Larger;
    }

    public Constraint getValue1EqValue2() {
        return value1EqValue2;
    }

    public Constraint getValue1Less() {
        return value1Less;
    }

    public Constraint createDisjunction(Sint.SymSint sym) {
        return Or.newInstance(
                And.newInstance(value1Larger, Eq.newInstance(sym, Sint.ONE)),
                And.newInstance(value1EqValue2, Eq.newInstance(sym, Sint.ZERO)),
                And.newInstance(value1Less, Eq.newInstance(sym, Sint.MINUS_ONE))
        );
    }
}
