package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class BoolIte extends IfThenElse<Constraint> implements Constraint {
    protected BoolIte(Constraint condition, Constraint ifCase, Constraint elseCase) {
        super(condition, ifCase, elseCase);
    }

    public static Constraint newInstance(Constraint condition, Constraint ifCase, Constraint elseCase) {
        if (condition instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) condition).isTrue() ? ifCase : elseCase;
        }
        return new BoolIte(condition, ifCase, elseCase);
    }
}
