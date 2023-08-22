package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Represents a conditional numeric value
 */
public class NumericalIte extends IfThenElse<NumericalExpression> implements NumericalExpression {
    protected NumericalIte(Constraint condition, NumericalExpression ifCase, NumericalExpression elseCase) {
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
    public static NumericalExpression newInstance(Constraint condition, NumericalExpression ifCase, NumericalExpression elseCase) {
        if (condition instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) condition).isTrue() ? ifCase : elseCase;
        }
        return new NumericalIte(condition, ifCase, elseCase);
    }

    @Override
    public boolean isFp() {
        return ifCase.isFp();
    }
}
