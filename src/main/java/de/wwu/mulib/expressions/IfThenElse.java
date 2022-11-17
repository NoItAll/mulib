package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class IfThenElse implements NumericExpression {
    protected final Constraint condition;
    protected final NumericExpression ifCase;
    protected final NumericExpression elseCase;

    protected IfThenElse(Constraint condition, NumericExpression ifCase, NumericExpression elseCase) {
        assert ifCase.isFp() == elseCase.isFp();
        this.condition = condition;
        this.ifCase = ifCase;
        this.elseCase = elseCase;
    }

    public Constraint getCondition() {
        return condition;
    }

    public NumericExpression getIfCase() {
        return ifCase;
    }

    public NumericExpression getElseCase() {
        return elseCase;
    }

    public static NumericExpression newInstance(Constraint condition, NumericExpression ifCase, NumericExpression elseCase) {
        if (condition instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) condition).isTrue() ? ifCase : elseCase;
        }
        return new IfThenElse(condition, ifCase, elseCase);
    }

    @Override
    public boolean isFp() {
        return ifCase.isFp();
    }
}
