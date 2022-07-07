package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sbyte extends Sint {
    public static final ConcSbyte ZERO = new ConcSbyte((byte) 0);

    private Sbyte() {}

    public static Sbyte concSbyte(byte b) {
        return new Sbyte.ConcSbyte(b);
    }

    public static Sbyte newInputSymbolicSbyte() {
        return new Sbyte.SymSbyte();
    }

    public static Sbyte newExpressionSymbolicSbyte(NumericExpression representedExpression) {
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
            return "val=" + value;
        }
    }

    public static class SymSbyte extends Sbyte implements SymNumericExpressionSprimitive {
        protected static AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private final NumericExpression representedExpression;

        private SymSbyte() {
            this.representedExpression = this;
            id = "SymSbyte" + nextId.incrementAndGet();
        }

        private SymSbyte(NumericExpression representedExpression) {
            this.representedExpression = representedExpression;
            id = "SymSbyte" + nextId.incrementAndGet();
        }

        @Override
        public NumericExpression getRepresentedExpression() {
            return representedExpression;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}