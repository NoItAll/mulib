package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents a conditional numeric value
 */
public class ExpressionIte extends IfThenElse<Expression> implements Expression {
    protected ExpressionIte(Constraint condition, Expression ifCase, Expression elseCase) {
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
    public static Expression newInstance(Constraint condition, Expression ifCase, Expression elseCase) {
        if (condition instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) condition).isTrue() ? ifCase : elseCase;
        }
        return new ExpressionIte(condition, ifCase, elseCase);
    }

    @Override
    public boolean isFp() {
        return ifCase.isFp();
    }
}
