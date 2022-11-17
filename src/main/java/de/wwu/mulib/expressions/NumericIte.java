package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class NumericIte extends IfThenElse<NumericExpression> implements NumericExpression {
    protected NumericIte(Constraint condition, NumericExpression ifCase, NumericExpression elseCase) {
        super(condition, ifCase, elseCase);
        assert ifCase.isFp() == elseCase.isFp();
    }

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
