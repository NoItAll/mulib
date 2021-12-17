package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sbool implements Sprimitive, Constraint {
    private static AtomicLong nextId = new AtomicLong(0);
    private final long id;

    private Sbool() {
        id = nextId.incrementAndGet();
    }

    public final static ConcSbool TRUE;
    public final static ConcSbool FALSE;
    static {
        TRUE = new ConcSbool(true);
        FALSE = new ConcSbool(false);
    }

    public static ConcSbool newConcSbool(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static SymSbool newInputSymbolicSbool() {
        return new SymSbool();
    }

    public static Sbool newConstraintSbool(Constraint c) {
        if (c instanceof ConcSbool || c instanceof SymSbool) {
            return (Sbool) c;
        }
        return new SymSbool(c);
    }

    public final Sbool and(Sbool rhs, SymbolicExecution se) {
       return se.and(this, rhs);
    }

    public final Sbool or(Sbool rhs, SymbolicExecution se) {
        return se.or(this, rhs);
    }

    public final Sbool not(SymbolicExecution se) {
        return se.not(this);
    }

    public final boolean boolChoice(SymbolicExecution se) {
        return se.boolChoice(this);
    }

    public final boolean negatedBoolChoice(SymbolicExecution se) {
        return se.boolChoice(se.not(this));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{id=" + id
                + additionToToStringBody()
                + "}";
    }

    @Override
    public final long getId() {
        return id;
    }

    public static final class ConcSbool extends Sbool implements ConcSprimitive {

        private final boolean value;
        private ConcSbool(final boolean value) {
            this.value = value;
        }

        public boolean isTrue() {
            return value;
        }

        public boolean isFalse() {
            return !value;
        }

        public Sbool.ConcSbool negate() {
            return isTrue() ? FALSE : TRUE;
        }

        @Override
        public String additionToToStringBody() {
            return ",val=" + value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ConcSbool && ((ConcSbool) o).isTrue() == isTrue();
        }

        @Override
        public int hashCode() {
            return isTrue() ? 1 : 0;
        }
    }

    public static class SymSbool extends Sbool implements SymSprimitive {
        private final Constraint representedConstraint;

        private SymSbool() {
            representedConstraint = this;
        }

        private SymSbool(Constraint representedConstraint) {
            if (representedConstraint == null) {
                throw new IllegalStateException("Must not happen.");
            }
            this.representedConstraint = representedConstraint;
        }

        public final Constraint getRepresentedConstraint() {
            return representedConstraint;
        }

        @Override
        public String additionToToStringBody() {
            return this.getRepresentedConstraint() != this ?
                    ",c=" + this.getRepresentedConstraint()
                    :
                    "";
        }
    }

    public final <T extends Sprimitive> T castTo(Class<T> castToClass, SymbolicExecution se) {
        return se.castTo(this, castToClass);
    }
}
