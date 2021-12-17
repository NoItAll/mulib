package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

public abstract class Sdouble extends Sfpnumber {

    private Sdouble() {}

    public static ConcSdouble newConcSdouble(double d) {
        return new ConcSdouble(d);
    }

    public static ConcSdouble newConcSdouble(int i) {
        return new ConcSdouble(i);
    }

    public static SymSdouble newInputSymbolicSdouble() {
        return new SymSdouble();
    }

    public static SymSdouble newExpressionSymbolicSdouble(NumericExpression representedExpression) {
        return new SymSdouble(representedExpression);
    }

    public final Sdouble add(Sdouble rhs, SymbolicExecution se) {
        return se.add(this, rhs, Sdouble.class);
    }

    public final Sdouble sub(Sdouble rhs, SymbolicExecution se) {
        return se.sub(this, rhs, Sdouble.class);
    }

    public final Sdouble div(Sdouble rhs, SymbolicExecution se) {
        return se.div(this, rhs, Sdouble.class);
    }

    public final Sdouble mul(Sdouble rhs, SymbolicExecution se) {
        return se.mul(this, rhs, Sdouble.class);
    }

    public final Sdouble mod(Sdouble rhs, SymbolicExecution se) {
        return se.mod(this, rhs, Sdouble.class);
    }

    public final Sdouble neg(SymbolicExecution se) {
        return se.neg(this, Sdouble.class);
    }

    public static final class ConcSdouble extends Sdouble implements ConcSnumber {
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
        public String additionToToStringBody() {
            return ",val=" + value;
        }
    }

    public static class SymSdouble extends Sdouble implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSdouble() {
            this.representedExpression = this;
        }

        private SymSdouble(NumericExpression representedExpression) {
            if (!representedExpression.isFp()) {
                throw new MulibRuntimeException("Represented NumericExpression must be a double.");
            }
            this.representedExpression = representedExpression;
        }

        @Override
        public final NumericExpression getRepresentedExpression() {
            return representedExpression;
        }
    }
}
