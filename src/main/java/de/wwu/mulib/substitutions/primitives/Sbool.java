package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sbool extends Sint implements Sprimitive, Constraint {

    private Sbool() {}

    public static Sbool concSbool(boolean b) {
        return b ? ConcSbool.TRUE : ConcSbool.FALSE;
    }

    public static Sbool newInputSymbolicSbool() {
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
        return se.negatedBoolChoice(this);
    }

    public final Sbool isEqualTo(Sbool other, SymbolicExecution se) {
        return se.or(se.and(this, other), se.and(se.not(this), se.not(other)));
    }

    public final boolean boolChoice(Sbool other, SymbolicExecution se) {
        return se.boolChoice(se.or(se.and(se.not(this), other), se.and(this, se.not(other))));
    }

    public final boolean negatedBoolChoice(Sbool other, SymbolicExecution se) {
        return se.boolChoice(isEqualTo(other, se));
    }

    public static final class ConcSbool extends Sbool implements ConcSnumber {
        public final static ConcSbool TRUE = new ConcSbool(true);
        public final static ConcSbool FALSE = new ConcSbool(false);
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

        public Sbool negate() {
            return isTrue() ? FALSE : TRUE;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ConcSbool && ((ConcSbool) o).isTrue() == isTrue();
        }

        @Override
        public int hashCode() {
            return intVal();
        }

        @Override
        public int intVal() {
            return value ? 1 : 0;
        }

        @Override
        public double doubleVal() {
            return intVal();
        }

        @Override
        public float floatVal() {
            return intVal();
        }

        @Override
        public long longVal() {
            return intVal();
        }

        @Override
        public short shortVal() {
            return (short) intVal();
        }

        @Override
        public byte byteVal() {
            return (byte) intVal();
        }

        @Override
        public String toString() {
            return "ConcSbool{value=" + value + "}";
        }
    }

    public static class SymSbool extends Sbool implements SymSprimitive, SymNumericExpressionSprimitive {
        protected static AtomicLong nextId = new AtomicLong(0);
        private final String id;

        protected final Constraint representedConstraint;

        private SymSbool() {
            id = "SymSbool" + nextId.incrementAndGet();
            representedConstraint = this;
        }

        private SymSbool(Constraint representedConstraint) {
            id = "SymSbool" + nextId.incrementAndGet();
            this.representedConstraint = representedConstraint;
        }

        public final Constraint getRepresentedConstraint() {
            return representedConstraint;
        }

        @Override
        public NumericExpression getRepresentedExpression() {
            return this;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "SymSbool{id=" + id + ( representedConstraint == this ? "}" : ",expr=" + representedConstraint.toString() + "}");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }
}
