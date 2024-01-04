package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents char values
 */
public abstract class Schar extends Sint {
    private Schar() {}

    /**
     * Might cache some values
     * @param c A char to wrap
     * @return The representation of a concrete char
     */
    public static Schar.ConcSchar concSchar(char c) {
        return new Schar.ConcSchar(c);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Schar.SymSchar newInputSymbolicSchar() {
        return new Schar.SymScharLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param id The identifier of the leaf
     * @return A new leaf
     */
    public static SymSchar newInputSymbolicSchar(long id) {
        return new SymScharLeaf(id);
    }

    /**
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Schar.SymSchar newExpressionSymbolicSchar(Expression representedExpression) {
        return new Schar.SymSchar(representedExpression);
    }

    /**
     * Class representing a concrete Schar
     */
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
    public static class SymSchar extends Schar implements SymSnumber {
        private final Expression representedExpression;

        private SymSchar() {
            this.representedExpression = this;
        }

        private SymSchar(Expression representedExpression) {
            this.representedExpression = representedExpression;
        }

        @Override
        public Expression getRepresentedExpression() {
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
            return "SymSchar{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic chars; - there are no composite numeric expressions here
     */
    public static class SymScharLeaf extends Schar.SymSchar implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        SymScharLeaf() {
            id = "Schar" + nextId.incrementAndGet();
        }

        SymScharLeaf(long nextId) {
            id = "Schar" + nextId;
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
