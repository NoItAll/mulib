package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

public abstract class Sfloat extends Sfpnumber {

    private Sfloat() {}

    public static ConcSfloat newConcSfloat(float d) {
        return new ConcSfloat(d);
    }

    public static ConcSfloat newConcSfloat(int i) {
        return new ConcSfloat(i);
    }

    public static SymSfloat newInputSymbolicSfloat() {
        return new SymSfloat();
    }

    public static SymSfloat newExpressionSymbolicSfloat(NumericExpression representedExpression) {
        return new SymSfloat(representedExpression);
    }


    public final Sfloat add(Sfloat rhs, SymbolicExecution se) {
        return se.add(this, rhs, Sfloat.class);
    }

    public final Sfloat sub(Sfloat rhs, SymbolicExecution se) {
        return se.sub(this, rhs, Sfloat.class);
    }

    public final Sfloat div(Sfloat rhs, SymbolicExecution se) {
        return se.div(this, rhs, Sfloat.class);
    }

    public final Sfloat mul(Sfloat rhs, SymbolicExecution se) {
        return se.mul(this, rhs, Sfloat.class);
    }

    public final Sfloat mod(Sfloat rhs, SymbolicExecution se) {
        return se.mod(this, rhs, Sfloat.class);
    }

    public final Sfloat neg(SymbolicExecution se) {
        return se.neg(this, Sfloat.class);
    }

    public static final class ConcSfloat extends Sfloat implements ConcSnumber {
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
            return ",val=" + value;
        }
    }

    public static class SymSfloat extends Sfloat implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSfloat() {
            this.representedExpression = this;
        }

        private SymSfloat(NumericExpression representedExpression) {
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
