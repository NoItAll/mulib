package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;

public abstract class Sshort extends Sint {
    public static final ConcSshort ZERO = new ConcSshort((short) 0);

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
        public String additionToToStringBody() {
            return ",val=" + value;
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
    }
}
