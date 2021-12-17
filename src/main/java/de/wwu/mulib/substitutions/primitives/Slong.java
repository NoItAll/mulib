package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

public abstract class Slong extends AbstractSnumber {
    private Slong() {}

    public static ConcSlong newConcSlong(long l) {
        return new ConcSlong(l);
    }

    public static SymSlong newInputSymbolicSlong() {
        return new SymSlong();
    }

    public static SymSlong newExpressionSymbolicSlong(NumericExpression representedExpression) {
        return new SymSlong(representedExpression);
    }

    @Override
    public boolean isFp() {
        return false;
    }

    public final Slong add(Slong rhs, SymbolicExecution se) {
        return se.add(this, rhs, Slong.class);
    }

    public final Slong sub(Slong rhs, SymbolicExecution se) {
        return se.sub(this, rhs, Slong.class);
    }

    public final Slong div(Slong rhs, SymbolicExecution se) {
        return se.div(this, rhs, Slong.class);
    }

    public final Slong mul(Slong rhs, SymbolicExecution se) {
        return se.mul(this, rhs, Slong.class);
    }

    public final Slong mod(Slong rhs, SymbolicExecution se) {
        return se.mod(this, rhs, Slong.class);
    }

    public final Slong neg(SymbolicExecution se) {
        return se.neg(this, Slong.class);
    }

    public static final class ConcSlong extends Slong implements ConcSnumber {
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
        public String additionToToStringBody() {
            return ",val=" + value;
        }
    }

    public static final class SymSlong extends Slong implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSlong() {
            this.representedExpression = this;
        }

        private SymSlong(NumericExpression representedExpression) {
            if (representedExpression.isFp()) {
                throw new MulibRuntimeException("Represented NumericExpression must be an integer.");
            }
            this.representedExpression = representedExpression;
        }

        @Override
        public NumericExpression getRepresentedExpression() {
            return representedExpression;
        }
    }
}