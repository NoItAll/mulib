package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents short values
 */
public abstract class Sshort extends Sint {

    private Sshort() {}

    /**
     * Might cache some values
     * @param s A short to wrap
     * @return The representation of a concrete short
     */
    public static Sshort.ConcSshort concSshort(short s) {
        return new Sshort.ConcSshort(s);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sshort.SymSshort newInputSymbolicSshort() {
        return new Sshort.SymSshortLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param id The identifier of the leaf
     * @return A new leaf
     */
    public static SymSshort newInputSymbolicSshort(long id) {
        return new SymSshortLeaf(id);
    }

    /**
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Sshort.SymSshort newExpressionSymbolicSshort(NumericalExpression representedExpression) {
        return new Sshort.SymSshort(representedExpression);
    }

    /**
     * Class representing a concrete Sshort
     */
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
    public static class SymSshort extends Sshort implements SymSnumber {
        private final NumericalExpression representedExpression;

        private SymSshort() {
            this.representedExpression = this;
        }

        private SymSshort(NumericalExpression representedExpression) {
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
            return "SymSshort{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic chars; - there are no composite numeric expressions here
     */
    public static class SymSshortLeaf extends SymSshort implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        SymSshortLeaf() {
            id = "Sshort" + nextId.incrementAndGet();
        }

        SymSshortLeaf(long nextId) {
            id = "Sshort" + nextId;
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
