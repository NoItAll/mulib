package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents double values
 */
public abstract class Sdouble extends Sfpnumber {

    private Sdouble() {}

    /**
     * Might cache some values
     * @param d A double to wrap
     * @return The representation of a concrete double
     */
    public static Sdouble.ConcSdouble concSdouble(double d) {
        return new ConcSdouble(d);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sdouble.SymSdouble newInputSymbolicSdouble() {
        return new SymSdoubleLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Sdouble.SymSdouble newExpressionSymbolicSdouble(NumericalExpression representedExpression) {
        return new SymSdouble(representedExpression);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' + other
     */
    public final Sdouble add(Sdouble rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' - other
     */
    public final Sdouble sub(Sdouble rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' / other
     */
    public final Sdouble div(Sdouble rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' * other
     */
    public final Sdouble mul(Sdouble rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' % other
     */
    public final Sdouble mod(Sdouble rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return -'this'
     */
    public final Sdouble neg(SymbolicExecution se) {
        return se.neg(this);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' < other
     */
    public final Sbool lt(Sdouble rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' <= other
     */
    public final Sbool lte(Sdouble rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' > other
     */
    public final Sbool gt(Sdouble rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' >= other
     */
    public final Sbool gte(Sdouble rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' == other
     */
    public final Sbool eq(Sdouble rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' DCMP other
     */
    public final Sint cmp(Sdouble rhs, SymbolicExecution se) {
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
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' < 0 is assumed in the following, else false
     */
    public final boolean ltChoice(Sdouble rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' <= 0 is assumed in the following, else false
     */
    public final boolean lteChoice(Sdouble rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' == 0 is assumed in the following, else false
     */
    public final boolean eqChoice(Sdouble rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' != 0 is assumed in the following, else false
     */
    public final boolean notEqChoice(Sdouble rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' > 0 is assumed in the following, else false
     */
    public final boolean gtChoice(Sdouble rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    /**
     * @param rhs The other double
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' >= 0 is assumed in the following, else false
     */
    public final boolean gteChoice(Sdouble rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a float
     */
    public final Sfloat d2f(SymbolicExecution se) {
        return se.d2f(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a long
     */
    public final Slong d2l(SymbolicExecution se) {
        return se.d2l(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to an int
     */
    public final Sint d2i(SymbolicExecution se) {
        return se.d2i(this);
    }

    /**
     * Class representing a concrete Sdouble
     */
    public static final class ConcSdouble extends Sdouble implements ConcSnumber {
        /**
         * 0
         */
        public static final ConcSdouble ZERO = new ConcSdouble(0);
        /**
         * 1
         */
        public static final ConcSdouble ONE = new ConcSdouble(1);
        /**
         * -1
         */
        public static final ConcSdouble MINUS_ONE = new ConcSdouble(-1);
        private final double value;

        ConcSdouble(double value) {
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
            return (float) value;
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
    public static class SymSdouble extends Sdouble implements SymSnumber {
        private final NumericalExpression representedExpression;

        private SymSdouble() {
            this.representedExpression = this;
        }

        private SymSdouble(NumericalExpression representedExpression) {
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
            return "SymSdouble{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic doubles; - there are no composite numeric expressions here
     */
    public static class SymSdoubleLeaf extends SymSdouble implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSdoubleLeaf() {
            id = "Sdouble" + nextId.incrementAndGet();
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
