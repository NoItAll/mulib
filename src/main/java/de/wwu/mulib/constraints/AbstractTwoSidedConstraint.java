package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Abstract supertype for those constraints that are composed of two other constraints/booleans
 */
public abstract class AbstractTwoSidedConstraint implements TwoSidedConstraint {

    protected final Constraint lhs;
    protected final Constraint rhs;

    protected AbstractTwoSidedConstraint(Constraint lhs, Constraint rhs) {
        assert !(lhs instanceof Sbool.ConcSbool) || !(rhs instanceof Sbool.ConcSbool);
        assert !(lhs instanceof ConcolicConstraintContainer) && !(rhs instanceof ConcolicConstraintContainer);
        if (lhs instanceof Sbool.SymSbool) {
            lhs = ((Sbool.SymSbool) lhs).getRepresentedConstraint();
        }
        if (rhs instanceof Sbool.SymSbool) {
            rhs = ((Sbool.SymSbool) rhs).getRepresentedConstraint();
        }
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public Constraint getLhs() {
        return lhs;
    }

    @Override
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
        if (this == o) {
            return true;
        }
        if (!this.getClass().equals(o.getClass())) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        AbstractTwoSidedConstraint oc = (AbstractTwoSidedConstraint) o;
        return this.getLhs().equals(oc.getLhs()) && this.getRhs().equals(oc.getRhs());
    }


    private Integer hashCode = null;
    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = getLhs().hashCode() + getRhs().hashCode();
        }
        return hashCode;
    }
}
