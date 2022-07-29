package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Sshort extends Sint {

    private Sshort() {}

    public static Sshort concSshort(short s) {
        return new Sshort.ConcSshort(s);
    }

    public static Sshort newInputSymbolicSshort() {
        return new Sshort.SymSshort();
    }

    public static Sshort newExpressionSymbolicSshort(NumericExpression representedExpression) {
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
        public String toString() {
            return "ConcSshort{value=" + value + " }";
        }
    }

    public static class SymSshort extends Sshort implements SymNumericExpressionSprimitive {
        protected static AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private final NumericExpression representedExpression;

        private SymSshort() {
            this.representedExpression = this;
            id = "SymSshort" + nextId.incrementAndGet();
        }

        private SymSshort(NumericExpression representedExpression) {
            this.representedExpression = representedExpression;
            id = "SymShort" + nextId.incrementAndGet();
        }

        @Override
        public NumericExpression getRepresentedExpression() {
            return representedExpression;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "SymSshort{id=" + id + ( representedExpression == this ? "}" : ",expr=" + representedExpression.toString() + "}");
        }
    }
}
