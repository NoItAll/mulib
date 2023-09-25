package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.throwables.NotYetImplementedException;

import java.util.Arrays;

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
     * @return An equivalent formulation of the represented constraint where the negation is
     * "pushed down"; - for instance !(b0 && b1) is transformed to !b0 || !b1;
     */
    public final Constraint tryPushDown() {
        if (constraint instanceof TwoSidedConstraint) {
            TwoSidedConstraint c = (TwoSidedConstraint) constraint;
            if (c instanceof And) {
                return Or.newInstance(Not.newInstance(c.getLhs()), Not.newInstance(c.getRhs()));
            } else if (c instanceof Or) {
                return And.newInstance(Not.newInstance(c.getLhs()), Not.newInstance(c.getRhs()));
            } else if (c instanceof Equivalence) {
                return Xor.newInstance(c.getLhs(), c.getRhs());
            } else if (c instanceof Implication) {
                return And.newInstance(c.getLhs(), Not.newInstance(c.getRhs()));
            } else {
                throw new NotYetImplementedException(c.toString());
            }
        } else if (constraint instanceof BoolIte) {
            BoolIte bi = (BoolIte) constraint;
            return And.newInstance(
                    Or.newInstance(Not.newInstance(bi.getCondition()), Not.newInstance(bi.getIfCase())),
                    Or.newInstance(bi.getCondition(), Not.newInstance(bi.getElseCase()))
            );
        } else if (constraint instanceof In) {
            In in = (In) constraint;
            return Arrays.stream(in.getSet())
                    .map(e -> Not.newInstance(Eq.newInstance(in.getElement(), e)))
                    .reduce(Sbool.ConcSbool.TRUE, And::newInstance);
        }
        return this;
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
