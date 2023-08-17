package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static de.wwu.mulib.substitutions.primitives.Sint.ConcSint.smallConcSints;

/**
 * Represents int values
 */
public abstract class Sint extends AbstractSnumber {
    Sint() {}

    /**
     * Might cache some values
     * @param i An int to wrap
     * @return The representation of a concrete int
     */
    public static Sint.ConcSint concSint(int i) {
        if (i >= 0 && i < smallConcSints.length) {
            return smallConcSints[i];
        }
        Integer I = i;
        ConcSint result = ConcSint.cache.get(I);
        if (result == null) {
            result = new ConcSint(i);
            ConcSint.cache.put(I, result);
        }
        return result;
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sint.SymSint newInputSymbolicSint() {
        return new SymSintLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param representedExpression The numeric expression to wrap
     * @return A symbolic value wrapping a numeric expression
     */
    public static Sint.SymSint newExpressionSymbolicSint(NumericExpression representedExpression) {
        return new SymSint(representedExpression);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' + other
     */
    public final Sint add(Sint rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' - other
     */
    public final Sint sub(Sint rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' / other
     */
    public final Sint div(Sint rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' * other
     */
    public final Sint mul(Sint rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' % other
     */
    public final Sint mod(Sint rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return -'this'
     */
    public final Sint neg(SymbolicExecution se) {
        return se.neg(this);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' < other
     */
    public final Sbool lt(Sint rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }
    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' <= other
     */
    public final Sbool lte(Sint rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' > other
     */
    public final Sbool gt(Sint rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' >= other
     */
    public final Sbool gte(Sint rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' == other
     */
    public final Sbool eq(Sint rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
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
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' < other is assumed in the following, else false
     */
    public final boolean ltChoice(Sint rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' <= other is assumed in the following, else false
     */
    public final boolean lteChoice(Sint rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' == other is assumed in the following, else false
     */
    public final boolean eqChoice(Sint rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' != other is assumed in the following, else false
     */
    public final boolean notEqChoice(Sint rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' > other is assumed in the following, else false
     */
    public final boolean gtChoice(Sint rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true, if 'this' >= other is assumed in the following, else false
     */
    public final boolean gteChoice(Sint rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' < other is assumed in the following, else false
     */
    public final boolean ltChoice(SymbolicExecution se, long id) {
        return se.ltChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' <= other is assumed in the following, else false
     */
    public final boolean lteChoice(SymbolicExecution se, long id) {
        return se.lteChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' == other is assumed in the following, else false
     */
    public final boolean eqChoice(SymbolicExecution se, long id) {
        return se.eqChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' != other is assumed in the following, else false
     */
    public final boolean notEqChoice(SymbolicExecution se, long id) {
        return se.notEqChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' > other is assumed in the following, else false
     */
    public final boolean gtChoice(SymbolicExecution se, long id) {
        return se.gtChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' >= other is assumed in the following, else false
     */
    public final boolean gteChoice(SymbolicExecution se, long id) {
        return se.gteChoice(this, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' < other is assumed in the following, else false
     */
    public final boolean ltChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.ltChoice(this, rhs, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' <= other is assumed in the following, else false
     */
    public final boolean lteChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.lteChoice(this, rhs, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' == other is assumed in the following, else false
     */
    public final boolean eqChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.eqChoice(this, rhs, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' != other is assumed in the following, else false
     */
    public final boolean notEqChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.notEqChoice(this, rhs, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' > other is assumed in the following, else false
     */
    public final boolean gtChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.gtChoice(this, rhs, id);
    }

    /**
     * @param rhs The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to identify this in the coverage cfg
     * @return true, if 'this' >= other is assumed in the following, else false
     */
    public final boolean gteChoice(Sint rhs, SymbolicExecution se, long id) {
        return se.gteChoice(this, rhs, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a double
     */
    public final Sdouble i2d(SymbolicExecution se) {
        return se.i2d(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a float
     */
    public final Sfloat i2f(SymbolicExecution se) {
        return se.i2f(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a long
     */
    public final Slong i2l(SymbolicExecution se) {
        return se.i2l(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a byte
     */
    public final Sbyte i2b(SymbolicExecution se) {
        return se.i2b(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a short
     */
    public final Sshort i2s(SymbolicExecution se) {
        return se.i2s(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' transformed to a char
     */
    public final Schar i2c(SymbolicExecution se) {
        return se.i2c(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param i Number to shift by
     * @return 'this' arithmetically shifted to the left
     */
    public final Sint ishl(Sint i, SymbolicExecution se) {
        return se.ishl(this, i);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param i Number to shift by
     * @return 'this' arithmetically shifted to the right
     */
    public final Sint ishr(Sint i, SymbolicExecution se) {
        return se.ishr(this, i);
    }

    /**
     * @param i The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' ^ i
     */
    public final Sint ixor(Sint i, SymbolicExecution se) {
        return se.ixor(this, i);
    }

    /**
     * @param i The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' | i
     */
    public final Sint ior(Sint i, SymbolicExecution se) {
        return se.ior(this, i);
    }
    /**
     * @param i The other int
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return 'this' & i
     */
    public final Sint iand(Sint i, SymbolicExecution se) {
        return se.iand(this, i);
    }
    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param i Number to shift by
     * @return 'this' logically shifted to the right
     */
    public final Sint iushr(Sint i, SymbolicExecution se) {
        return se.iushr(this, i);
    }

    @Override
    public final boolean isFp() {
        return false;
    }

    /**
     * Class representing a concrete Sint
     */
    public static final class ConcSint extends Sint implements ConcSnumber {
        static final Map<Integer, ConcSint> cache = Collections.synchronizedMap(new HashMap<>());
        static final ConcSint[] smallConcSints = new ConcSint[100];

        static {
            MINUS_ONE = new ConcSint(-1);
            ConcSint zero = new ConcSint(0);
            ConcSint one = new ConcSint(1);
            ZERO = zero;
            ONE = one;
            smallConcSints[0] = zero;
            smallConcSints[1] = one;
            for (int i = 2; i < 100; i++) {
                smallConcSints[i] = new ConcSint(i);
            }
        }

        /**
         * -1
         */
        public static final ConcSint MINUS_ONE;
        /**
         * 0
         */
        public static final ConcSint ZERO;
        /**
         * 1
         */
        public static final ConcSint ONE;
        private final int value;

        ConcSint(int value) {
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
            return (float) value;
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
            return value;
        }
    }

    /**
     * Class for wrapping numeric expressions
     */
    public static class SymSint extends Sint implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSint() {
            this.representedExpression = this;
        }

        private SymSint(NumericExpression representedExpression) {
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
            return "SymSint{" + representedExpression.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic ints; - there are no composite numeric expressions here
     */
    public static class SymSintLeaf extends SymSint implements SymSprimitiveLeaf {
        protected static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        private SymSintLeaf() {
            id = "Sint" + nextId.incrementAndGet();
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
