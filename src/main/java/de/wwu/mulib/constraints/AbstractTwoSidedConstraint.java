package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

public abstract class AbstractTwoSidedConstraint implements TwoSidedConstraint {

    protected final Constraint lhs;
    protected final Constraint rhs;

    protected AbstractTwoSidedConstraint(Constraint lhs, Constraint rhs) {
        assert !(lhs instanceof Sbool.ConcSbool) || !(rhs instanceof Sbool.ConcSbool);
        this.lhs = lhs instanceof Sbool.SymSbool ? ((Sbool.SymSbool) lhs).getRepresentedConstraint() : lhs;
        this.rhs = rhs instanceof Sbool.SymSbool ? ((Sbool.SymSbool) rhs).getRepresentedConstraint() : rhs;
    }

    public Constraint getLhs() {
        return lhs;
    }

    public Constraint getRhs() {
        return rhs;
    }

    protected static boolean bothConstraintsAreConcrete(Constraint lhs, Constraint rhs) {
        return lhs instanceof Sbool.ConcSbool && rhs instanceof Sbool.ConcSbool;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "lhs=" + lhs
                + ",rhs=" + rhs
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
        AbstractTwoSidedConstraint oc = (AbstractTwoSidedConstraint) o;
        return this.getLhs().equals(oc.getLhs()) && this.getRhs().equals(oc.getRhs());
    }

    @Override
    public int hashCode() {
        return getLhs().hashCode() + getRhs().hashCode();
    }
}
