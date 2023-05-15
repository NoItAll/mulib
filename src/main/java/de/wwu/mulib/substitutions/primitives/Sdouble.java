package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sdouble extends Sfpnumber {

    private Sdouble() {}

    public static Sdouble concSdouble(double d) {
        return new ConcSdouble(d);
    }

    public static Sdouble.SymSdouble newInputSymbolicSdouble() {
        return new SymSdoubleLeaf();
    }

    public static Sdouble.SymSdouble newExpressionSymbolicSdouble(NumericExpression representedExpression) {
        return new SymSdouble(representedExpression);
    }

    public final Sdouble add(Sdouble rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    public final Sdouble sub(Sdouble rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    public final Sdouble div(Sdouble rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    public final Sdouble mul(Sdouble rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    public final Sdouble mod(Sdouble rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    public final Sdouble neg(SymbolicExecution se) {
        return se.neg(this);
    }

    public final Sbool lt(Sdouble rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    public final Sbool lte(Sdouble rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    public final Sbool gt(Sdouble rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    public final Sbool gte(Sdouble rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    public final Sbool eq(Sdouble rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    public final Sint cmp(Sdouble rhs, SymbolicExecution se) {
        return se.cmp(this, rhs);
    }

    public final boolean ltChoice(SymbolicExecution se) {
        return se.ltChoice(this);
    }

    public final boolean lteChoice(SymbolicExecution se) {
        return se.lteChoice(this);
    }

    public final boolean eqChoice(SymbolicExecution se) {
        return se.eqChoice(this);
    }

    public final boolean notEqChoice(SymbolicExecution se) {
        return se.notEqChoice(this);
    }

    public final boolean gtChoice(SymbolicExecution se) {
        return se.gtChoice(this);
    }

    public final boolean gteChoice(SymbolicExecution se) {
        return se.gteChoice(this);
    }

    public final boolean ltChoice(Sdouble rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    public final boolean lteChoice(Sdouble rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    public final boolean eqChoice(Sdouble rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    public final boolean notEqChoice(Sdouble rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    public final boolean gtChoice(Sdouble rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    public final boolean gteChoice(Sdouble rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    public final Sfloat d2f(SymbolicExecution se) {
        return se.d2f(this);
    }

    public final Slong d2l(SymbolicExecution se) {
        return se.d2l(this);
    }

    public final Sint d2i(SymbolicExecution se) {
        return se.d2i(this);
    }

    public static final class ConcSdouble extends Sdouble implements ConcSnumber {
        public static final ConcSdouble ZERO = new ConcSdouble(0);
        public static final ConcSdouble ONE = new ConcSdouble(1);
        public static final ConcSdouble MINUS_ONE = new ConcSdouble(-1);
        private final double value;

        private ConcSdouble(double value) {
            this.value = value;
        }

        @Override
        public int intVal() {
            return (int) value;
        }

        @Override
        public double doubleVal() {
            return value;
        }

        @Override
        public float floatVal() {
            return (float) value;
        }

        @Override
        public long longVal() {
            return (long) value;
        }

        @Override
        public short shortVal() {
            return (short) value;
        }

        @Override
        public byte byteVal() {
            return (byte) value;
        }

        @Override
        public char charVal() {
            return (char) value;
        }

        @Override
        public String toString() {
            return "ConcSdouble{value=" + value + "}";
        }
    }

    public static class SymSdouble extends Sdouble implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSdouble() {
            this.representedExpression = this;
        }

        private SymSdouble(NumericExpression representedExpression) {
            this.representedExpression = representedExpression;
        }

        @Override
        public final NumericExpression getRepresentedExpression() {
            return representedExpression;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != getClass()) {
                return false;
            }
            return representedExpression.equals(((SymNumericExpressionSprimitive) o).getRepresentedExpression());
        }

        @Override
        public int hashCode() {
            return representedExpression.hashCode();
        }

        @Override
        public String toString() {
            return "SymSdouble{" + representedExpression.toString() + "}";
        }
    }

    public static class SymSdoubleLeaf extends SymSdouble implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSdoubleLeaf() {
            id = "Sdouble" + nextId.incrementAndGet();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != getClass()) {
                return false;
            }
            return id.equals(((SymSprimitiveLeaf) o).getId());
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
