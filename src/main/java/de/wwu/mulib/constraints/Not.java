package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Constraint representing a logical NOT
 */
public class Not implements Constraint {

    private final Constraint constraint;

    private Not(Constraint constraint) {
        assert !(constraint instanceof Sbool.ConcSbool);
        assert !(constraint instanceof ConcolicConstraintContainer);
        this.constraint = constraint;
    }

    /**
     * Constructs a new constraint negating the given constraint
     * @param constraint The constraint to negate
     * @return Either a simplified constraint, or NOT(constraint)
     */
    public static Constraint newInstance(Constraint constraint) {
        if (constraint instanceof Sbool.ConcSbool) {
            return Sbool.concSbool(((Sbool.ConcSbool) constraint).isFalse());
        }
        if (constraint instanceof Sbool.SymSbool) {
            constraint = ((Sbool.SymSbool) constraint).getRepresentedConstraint();
        }
        if (constraint instanceof Not) {
            return ((Not) constraint).getConstraint();
        } else if (constraint instanceof Lt) {
            return Lte.newInstance(((Lt) constraint).getRhs(), ((Lt) constraint).getLhs());
        } else if (constraint instanceof Lte) {
            return Lt.newInstance(((Lte) constraint).getRhs(), ((Lte) constraint).getLhs());
        } {
            return new Not(constraint);
        }
    }

    /**
     * @return The negated constraint
     */
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

    public final boolean isNegationOf(Constraint constraint) {
        if (constraint instanceof Sbool.SymSbool) {
            constraint = ((Sbool.SymSbool) constraint).getRepresentedConstraint();
        }
        return this.constraint == constraint;
    }
}
