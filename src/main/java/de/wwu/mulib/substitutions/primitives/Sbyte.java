package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents byte values. All methods w.r.t. calculation are implemented in {@link Sint}
 */
public abstract class Sbyte extends Sint {

    private Sbyte() {}

    /**
     * @param b A byte to wrap
     * @return The representation of a concrete byte
     */
    public static Sbyte.ConcSbyte concSbyte(byte b) {
        return new Sbyte.ConcSbyte(b);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sbyte.SymSbyte newInputSymbolicSbyte() {
        return new Sbyte.SymSbyteLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Sbyte.SymSbyte newExpressionSymbolicSbyte(NumericalExpression representedExpression) {
        return new Sbyte.SymSbyte(representedExpression);
    }

    /**
     * Class representing a concrete Sbyte
     */
    public static final class ConcSbyte extends Sbyte implements ConcSnumber {
        /**
         * Zero
         */
        public static final ConcSbyte ZERO = new ConcSbyte((byte) 0);
        private final byte value;

        ConcSbyte(byte value) {
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
        public char charVal() {
            return (char) value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * Class for wrapping numeric expressions
     */
    public static class SymSbyte extends Sbyte implements SymSnumber {
        private final NumericalExpression representedExpression;

        private SymSbyte() {
            this.representedExpression = this;
        }

        private SymSbyte(NumericalExpression representedExpression) {
            this.representedExpression = representedExpression;
        }

        @Override
        public NumericalExpression getRepresentedExpression() {
            return representedExpression;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != getClass()) {
                return false;
            }
            return representedExpression.equals(((SymSnumber) o).getRepresentedExpression());
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

    /**
     * Class for representing simple symbolic bytes; - there are no composite numeric expressions here
     */
    public static class SymSbyteLeaf extends SymSbyte implements SymSprimitiveLeaf {
        private static final AtomicLong nextId = new AtomicLong(0);
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