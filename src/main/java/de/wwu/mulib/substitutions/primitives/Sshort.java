package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sshort extends Sint {

    private Sshort() {}

    public static Sshort.ConcSshort concSshort(short s) {
        return new Sshort.ConcSshort(s);
    }

    public static Sshort.SymSshort newInputSymbolicSshort() {
        return new Sshort.SymSshortLeaf();
    }

    public static Sshort.SymSshort newExpressionSymbolicSshort(NumericExpression representedExpression) {
        return new Sshort.SymSshort(representedExpression);
    }
    
    public static final class ConcSshort extends Sshort implements ConcSnumber {
        public static final ConcSshort ZERO = new ConcSshort((short) 0);

        private final short value;

        private ConcSshort(short value) {
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
            return (byte) value;
        }

        @Override
        public char charVal() {
            return (char) value;
        }

        @Override
        public String toString() {
            return "ConcSshort{value=" + value + "}";
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    public static class SymSshort extends Sshort implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSshort() {
            this.representedExpression = this;
        }

        private SymSshort(NumericExpression representedExpression) {
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
            return "SymSshort{" + representedExpression.toString() + "}";
        }
    }

    public static class SymSshortLeaf extends SymSshort implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSshortLeaf() {
            id = "Sshort" + nextId.incrementAndGet();
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
