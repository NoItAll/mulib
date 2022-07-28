package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sfloat extends Sfpnumber {

    private Sfloat() {}

    public static Sfloat concSfloat(float d) {
        return new ConcSfloat(d);
    }

    public static Sfloat newInputSymbolicSfloat() {
        return new SymSfloat();
    }

    public static Sfloat newExpressionSymbolicSfloat(NumericExpression representedExpression) {
        return new SymSfloat(representedExpression);
    }


    public final Sfloat add(Sfloat rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    public final Sfloat sub(Sfloat rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    public final Sfloat div(Sfloat rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    public final Sfloat mul(Sfloat rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    public final Sfloat mod(Sfloat rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    public final Sfloat neg(SymbolicExecution se) {
        return se.neg(this);
    }

    public final Sbool lt(Sfloat rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    public final Sbool lte(Sfloat rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    public final Sbool gt(Sfloat rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    public final Sbool gte(Sfloat rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    public final Sbool eq(Sfloat rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    public final Sint cmp(Sfloat rhs, SymbolicExecution se) {
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

    public final boolean ltChoice(Sfloat rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    public final boolean lteChoice(Sfloat rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    public final boolean eqChoice(Sfloat rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    public final boolean notEqChoice(Sfloat rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    public final boolean gtChoice(Sfloat rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    public final boolean gteChoice(Sfloat rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    public final Sint f2i(SymbolicExecution se) {
        return se.f2i(this);
    }

    public final Sdouble f2d(SymbolicExecution se) {
        return se.f2d(this);
    }

    public final Slong f2l(SymbolicExecution se) {
        return se.f2l(this);
    }

    public static final class ConcSfloat extends Sfloat implements ConcSnumber {
        public static final ConcSfloat ZERO = new ConcSfloat(0);
        public static final ConcSfloat ONE = new ConcSfloat(1);
        public static final ConcSfloat MINUS_ONE = new ConcSfloat(-1);
        private final float value;

        private ConcSfloat(float value) {
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
        public String additionToToStringBody() {
            return "val=" + value;
        }
    }

    public static class SymSfloat extends Sfloat implements SymNumericExpressionSprimitive {
        protected static AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private final NumericExpression representedExpression;

        private SymSfloat() {
            this.representedExpression = this;
            id = "SymSfloat" + nextId.incrementAndGet();
        }

        private SymSfloat(NumericExpression representedExpression) {
            this.representedExpression = representedExpression;
            id = "SymSfloat" + nextId.incrementAndGet();
        }

        @Override
        public final NumericExpression getRepresentedExpression() {
            return representedExpression;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
