package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents a conditional numeric value
 */
public class NumericIte extends IfThenElse<NumericExpression> implements NumericExpression {
    protected NumericIte(Constraint condition, NumericExpression ifCase, NumericExpression elseCase) {
        super(condition, ifCase, elseCase);
        assert ifCase.isFp() == elseCase.isFp();
    }

    /**
     * Constructs a, potentially simplified, constraint.
     * @param condition The condition
     * @param ifCase The numeric value if condition evaluates to true
     * @param elseCase The numeric value if condition evaluates to false
     * @return Either a simplified NumericExpression, or ITE(condition, ifCase, elseCase)
     */
    public static NumericExpression newInstance(Constraint condition, NumericExpression ifCase, NumericExpression elseCase) {
        if (condition instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) condition).isTrue() ? ifCase : elseCase;
        }
        return new NumericIte(condition, ifCase, elseCase);
    }

    @Override
    public boolean isFp() {
        return ifCase.isFp();
    }
}
