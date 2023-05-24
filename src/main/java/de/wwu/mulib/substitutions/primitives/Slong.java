package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Slong extends AbstractSnumber {

    private Slong() {}

    public static Slong.ConcSlong concSlong(long l) {
        return new ConcSlong(l);
    }

    public static Slong.SymSlong newInputSymbolicSlong() {
        return new SymSlongLeaf();
    }

    public static Slong.SymSlong newExpressionSymbolicSlong(NumericExpression representedExpression) {
        return new SymSlong(representedExpression);
    }

    @Override
    public boolean isFp() {
        return false;
    }

    public final Slong add(Slong rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    public final Slong sub(Slong rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    public final Slong div(Slong rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    public final Slong mul(Slong rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    public final Slong mod(Slong rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    public final Slong neg(SymbolicExecution se) {
        return se.neg(this);
    }

    public final Sbool lt(Slong rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    public final Sbool lte(Slong rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    public final Sbool gt(Slong rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    public final Sbool gte(Slong rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    public final Sbool eq(Slong rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    public final Sint cmp(Slong rhs, SymbolicExecution se) {
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
    public final boolean ltChoice(Slong rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    public final boolean lteChoice(Slong rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    public final boolean eqChoice(Slong rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    public final boolean notEqChoice(Slong rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    public final boolean gtChoice(Slong rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    public final boolean gteChoice(Slong rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    public final Sint l2i(SymbolicExecution se) {
        return se.l2i(this);
    }

    public final Sdouble l2d(SymbolicExecution se) {
        return se.l2d(this);
    }

    public final Sfloat l2f(SymbolicExecution se) {
        return se.l2f(this);
    }

    public final Slong lshl(Sint l, SymbolicExecution se) {
        return se.lshl(this, l);
    }

    public final Slong lshr(Sint l, SymbolicExecution se) {
        return se.lshr(this, l);
    }

    public final Slong lxor(Slong l, SymbolicExecution se) {
        return se.lxor(this, l);
    }

    public final Slong lor(Slong l, SymbolicExecution se) {
        return se.lor(this, l);
    }

    public final Slong land(Slong l, SymbolicExecution se) {
        return se.land(this, l);
    }

    public final Slong lushr(Sint l, SymbolicExecution se) {
        return se.lushr(this, l);
    }

    public static final class ConcSlong extends Slong implements ConcSnumber {
        public static final ConcSlong ZERO = new ConcSlong(0);
        public static final ConcSlong ONE = new ConcSlong(1);
        public static final ConcSlong MINUS_ONE = new ConcSlong(-1);
        private final long value;

        private ConcSlong(long value) {
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
            return value;
        }

        @Override
        public long longVal() {
            return value;
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
            return "ConcSlong{value=" + value + "}";
        }

        @Override
        public int hashCode() {
            return (int) value;
        }
    }

    public static class SymSlong extends Slong implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSlong() {
            this.representedExpression = this;
        }

        private SymSlong(NumericExpression representedExpression) {
            this.representedExpression = representedExpression;
        }

        @Override
        public NumericExpression getRepresentedExpression() {
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
            return "SymSlong{" + representedExpression.toString() + "}";
        }
    }

    public static class SymSlongLeaf extends SymSlong implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSlongLeaf() {
            id = "Slong" + nextId.incrementAndGet();
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