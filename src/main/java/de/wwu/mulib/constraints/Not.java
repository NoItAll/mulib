package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public class Not implements Constraint {

    private final Constraint constraint;

    private Not(Constraint constraint) {
        this.constraint = constraint;
    }

    public static Constraint newInstance(Constraint constraint) {
        if (constraint instanceof Sbool.ConcSbool) {
            return Sbool.newConcSbool(!((Sbool.ConcSbool) constraint).isTrue());
        } else {
            return new Not(constraint);
        }
    }

    public final Constraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return "!(" + constraint + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!this.getClass().equals(o.getClass())) {
            return false;
        }
        return constraint.equals(((Not) o).getConstraint());
    }

    @Override
    public int hashCode() {
        return constraint.hashCode();
    }
}
