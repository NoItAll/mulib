package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.Expression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents long values
 */
public abstract class Slong extends AbstractSnumber {

    private Slong() {}

    /**
     * Might cache some values
     * @param l A long to wrap
     * @return The representation of a concrete long
     */
    public static Slong.ConcSlong concSlong(long l) {
        return new ConcSlong(l);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Slong.SymSlong newInputSymbolicSlong() {
        return new SymSlongLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param id The identifier of the leaf
     * @return A new leaf
     */
    public static SymSlong newInputSymbolicSlong(long id) {
        return new SymSlongLeaf(id);
    }

    /**
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Slong.SymSlong newExpressionSymbolicSlong(Expression representedExpression) {
        return new SymSlong(representedExpression);
    }

    @Override
    public boolean isFp() {
        return false;
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' + other
     */
    public final Slong add(Slong rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' - other
     */
    public final Slong sub(Slong rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' / other
     */
    public final Slong div(Slong rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' * other
     */
    public final Slong mul(Slong rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' % other
     */
    public final Slong mod(Slong rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return -'this'
     */
    public final Slong neg(SymbolicExecution se) {
        return se.neg(this);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' < other
     */
    public final Sbool lt(Slong rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' <= other
     */
    public final Sbool lte(Slong rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' > other
     */
    public final Sbool gt(Slong rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' >= other
     */
    public final Sbool gte(Slong rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' == other
     */
    public final Sbool eq(Slong rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' LCMP other
     */
    public final Sint cmp(Slong rhs, SymbolicExecution se) {
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
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' < other is assumed in the following, else false
     */
    public final boolean ltChoice(Slong rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' <= other is assumed in the following, else false
     */
    public final boolean lteChoice(Slong rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' == other is assumed in the following, else false
     */
    public final boolean eqChoice(Slong rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' != other is assumed in the following, else false
     */
    public final boolean notEqChoice(Slong rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' > other is assumed in the following, else false
     */
    public final boolean gtChoice(Slong rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    /**
     * @param rhs The other long
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' >= other is assumed in the following, else false
     */
    public final boolean gteChoice(Slong rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to an int
     */
    public final Sint l2i(SymbolicExecution se) {
        return se.l2i(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a double
     */
    public final Sdouble l2d(SymbolicExecution se) {
        return se.l2d(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a float
     */
    public final Sfloat l2f(SymbolicExecution se) {
        return se.l2f(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param l Number to shift by
     * @return 'this' arithmetically shifted to the left
     */
    public final Slong lshl(Sint l, SymbolicExecution se) {
        return se.lshl(this, l);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param l Number to shift by
     * @return 'this' arithmetically shifted to the right
     */
    public final Slong lshr(Sint l, SymbolicExecution se) {
        return se.lshr(this, l);
    }

    /**
     * @param l The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' ^ l
     */
    public final Slong lxor(Slong l, SymbolicExecution se) {
        return se.lxor(this, l);
    }

    /**
     * @param l The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' | l
     */
    public final Slong lor(Slong l, SymbolicExecution se) {
        return se.lor(this, l);
    }

    /**
     * @param l The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' & l
     */
    public final Slong land(Slong l, SymbolicExecution se) {
        return se.land(this, l);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param l Number to shift by
     * @return 'this' logically shifted to the right
     */
    public final Slong lushr(Sint l, SymbolicExecution se) {
        return se.lushr(this, l);
    }

    /**
     * Class representing a concrete Slong
     */
    public static final class ConcSlong extends Slong implements ConcSnumber {
        /**
         * 0
         */
        public static final ConcSlong ZERO = new ConcSlong(0);
        /**
         * 1
         */
        public static final ConcSlong ONE = new ConcSlong(1);
        /**
         * -1
         */
        public static final ConcSlong MINUS_ONE = new ConcSlong(-1);
        private final long value;

        ConcSlong(long value) {
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
            return (char) value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public int hashCode() {
            return (int) value;
        }
    }

    /**
     * Class for wrapping numeric expressions
     */
    public static class SymSlong extends Slong implements SymSnumber {
        private final Expression representedExpression;

        private SymSlong() {
            this.representedExpression = this;
        }

        private SymSlong(Expression representedExpression) {
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
            return "SymSlong{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic longs; - there are no composite numeric expressions here
     */
    public static class SymSlongLeaf extends SymSlong implements SymSprimitiveLeaf {
        private static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        SymSlongLeaf() {
            id = "Slong" + nextId.incrementAndGet();
        }

        SymSlongLeaf(long nextId) {
            id = "Slong" + nextId;
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