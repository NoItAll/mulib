package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sbyte extends Sint {

    private Sbyte() {}

    public static Sbyte concSbyte(byte b) {
        return new Sbyte.ConcSbyte(b);
    }

    public static Sbyte.SymSbyte newInputSymbolicSbyte() {
        return new Sbyte.SymSbyteLeaf();
    }

    public static Sbyte.SymSbyte newExpressionSymbolicSbyte(NumericExpression representedExpression) {
        return new Sbyte.SymSbyte(representedExpression);
    }

    public static final class ConcSbyte extends Sbyte implements ConcSnumber {
        public static final ConcSbyte ZERO = new ConcSbyte((byte) 0);
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
        public String toString() {
            return "ConcSbyte{value=" + value + "}";
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    public static class SymSbyte extends Sbyte implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSbyte() {
            this.representedExpression = this;
        }

        private SymSbyte(NumericExpression representedExpression) {
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
            return "SymSbyte{" + representedExpression.toString() + "}";
        }
    }

    public static class SymSbyteLeaf extends SymSbyte implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSbyteLeaf() {
            id = "Sbyte" + nextId.incrementAndGet();
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