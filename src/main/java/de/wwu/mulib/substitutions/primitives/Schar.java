package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Schar extends Sint {
    private Schar() {}

    public static Schar concSchar(char s) {
        return new Schar.ConcSchar(s);
    }

    public static Schar.SymSchar newInputSymbolicSchar() {
        return new Schar.SymScharLeaf();
    }

    public static Schar.SymSchar newExpressionSymbolicSchar(NumericExpression representedExpression) {
        return new Schar.SymSchar(representedExpression);
    }

    public static class ConcSchar extends Schar implements ConcSnumber {
        public static final ConcSchar ZERO = new ConcSchar((char) 0);

        private final char value;

        private ConcSchar(char value) {
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
            return (short) value;
        }

        @Override
        public byte byteVal() {
            return (byte) value;
        }

        @Override
        public char charVal() {
            return value;
        }

        @Override
        public String toString() {
            return "ConcSchar{value=" + value + "}";
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    public static class SymSchar extends Schar implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSchar() {
            this.representedExpression = this;
        }

        private SymSchar(NumericExpression representedExpression) {
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
            return "SymSchar{" + representedExpression.toString() + "}";
        }
    }

    public static class SymScharLeaf extends Schar.SymSchar implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymScharLeaf() {
            id = "Schar" + nextId.incrementAndGet();
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
