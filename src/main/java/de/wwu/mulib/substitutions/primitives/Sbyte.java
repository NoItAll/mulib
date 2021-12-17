package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.NumericExpression;

public abstract class Sbyte extends Sintegernumber {
    private Sbyte() {}

    public static Sbyte.ConcSbyte newConcSbyte(byte b) {
        return new Sbyte.ConcSbyte(b);
    }

    public static Sbyte.SymSbyte newInputSymbolicSbyte() {
        return new Sbyte.SymSbyte();
    }

    public static Sbyte.SymSbyte newExpressionSymbolicSbyte(NumericExpression representedExpression) {
        return new Sbyte.SymSbyte(representedExpression);
    }
    
    public static final class ConcSbyte extends Sbyte implements ConcSnumber {
        private final byte value;

        private ConcSbyte(byte value) {
            this.value = value;
        }

        @Override
        public int intVal() {
            return value;
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
            return value;
        }

        @Override
        public byte byteVal() {
            return value;
        }

        @Override
        public String additionToToStringBody() {
            return ",val=" + value;
        }
    }

    public static final class SymSbyte extends Sbyte implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSbyte() {
            this.representedExpression = this;
        }

        private SymSbyte(NumericExpression representedExpression) {
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