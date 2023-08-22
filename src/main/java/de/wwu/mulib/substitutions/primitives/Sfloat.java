package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents float values
 */
public abstract class Sfloat extends Sfpnumber {

    private Sfloat() {}

    /**
     * Might cache some values
     * @param f A float to wrap
     * @return The representation of a concrete float
     */
    public static Sfloat.ConcSfloat concSfloat(float f) {
        return new ConcSfloat(f);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sfloat.SymSfloat newInputSymbolicSfloat() {
        return new SymSfloatLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Sfloat.SymSfloat newExpressionSymbolicSfloat(NumericalExpression representedExpression) {
        return new SymSfloat(representedExpression);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' + other
     */
    public final Sfloat add(Sfloat rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' - other
     */
    public final Sfloat sub(Sfloat rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' / other
     */
    public final Sfloat div(Sfloat rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' * other
     */
    public final Sfloat mul(Sfloat rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' % other
     */
    public final Sfloat mod(Sfloat rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return -'this'
     */
    public final Sfloat neg(SymbolicExecution se) {
        return se.neg(this);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' < other
     */
    public final Sbool lt(Sfloat rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' <= other
     */
    public final Sbool lte(Sfloat rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' > other
     */
    public final Sbool gt(Sfloat rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' >= other
     */
    public final Sbool gte(Sfloat rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' == other
     */
    public final Sbool eq(Sfloat rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' FCMP other
     */
    public final Sint cmp(Sfloat rhs, SymbolicExecution se) {
        return se.cmp(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' < 0 is assumed in the following, else false
     */
    public final boolean ltChoice(SymbolicExecution se) {
        return se.ltChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' <= 0 is assumed in the following, else false
     */
    public final boolean lteChoice(SymbolicExecution se) {
        return se.lteChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' == 0 is assumed in the following, else false
     */
    public final boolean eqChoice(SymbolicExecution se) {
        return se.eqChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' != 0 is assumed in the following, else false
     */
    public final boolean notEqChoice(SymbolicExecution se) {
        return se.notEqChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' > 0 is assumed in the following, else false
     */
    public final boolean gtChoice(SymbolicExecution se) {
        return se.gtChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' >= 0 is assumed in the following, else false
     */
    public final boolean gteChoice(SymbolicExecution se) {
        return se.gteChoice(this);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' < other is assumed in the following, else false
     */
    public final boolean ltChoice(Sfloat rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' <= other is assumed in the following, else false
     */
    public final boolean lteChoice(Sfloat rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' == other is assumed in the following, else false
     */
    public final boolean eqChoice(Sfloat rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' != other is assumed in the following, else false
     */
    public final boolean notEqChoice(Sfloat rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' > other is assumed in the following, else false
     */
    public final boolean gtChoice(Sfloat rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    /**
     * @param rhs The other float
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' >= other is assumed in the following, else false
     */
    public final boolean gteChoice(Sfloat rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to an int
     */
    public final Sint f2i(SymbolicExecution se) {
        return se.f2i(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a double
     */
    public final Sdouble f2d(SymbolicExecution se) {
        return se.f2d(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a long
     */
    public final Slong f2l(SymbolicExecution se) {
        return se.f2l(this);
    }

    /**
     * Class representing a concrete Sfloat
     */
    public static final class ConcSfloat extends Sfloat implements ConcSnumber {
        /**
         * 0
         */
        public static final ConcSfloat ZERO = new ConcSfloat(0);
        /**
         * 1
         */
        public static final ConcSfloat ONE = new ConcSfloat(1);
        /**
         * -1
         */
        public static final ConcSfloat MINUS_ONE = new ConcSfloat(-1);
        private final float value;

        ConcSfloat(float value) {
            this.value = value;
        }

        @Override
        public int intVal() {
            return (int) value;
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
            return (long) value;
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
            return (char) value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Class for wrapping numeric expressions
     */
    public static class SymSfloat extends Sfloat implements SymSnumber {
        private final NumericalExpression representedExpression;

        private SymSfloat() {
            this.representedExpression = this;
        }

        private SymSfloat(NumericalExpression representedExpression) {
            this.representedExpression = representedExpression;
        }

        @Override
        public final NumericalExpression getRepresentedExpression() {
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
            return "SymSfloat{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic floats; - there are no composite numeric expressions here
     */
    public static class SymSfloatLeaf extends SymSfloat implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSfloatLeaf() {
            id = "Sfloat" + nextId.incrementAndGet();
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
