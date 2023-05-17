package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static de.wwu.mulib.substitutions.primitives.Sint.ConcSint.smallConcSints;

public abstract class Sint extends AbstractSnumber {
    Sint() {}
    public static Sint concSint(int i) {
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

    public static Sint.SymSint newInputSymbolicSint() {
        return new SymSintLeaf();
    }

    public static Sint.SymSint newExpressionSymbolicSint(NumericExpression representedExpression) {
        return new SymSint(representedExpression);
    }

    public final Sint add(Sint rhs, SymbolicExecution se) {
        return se.add(this, rhs);
    }

    public final Sint sub(Sint rhs, SymbolicExecution se) {
        return se.sub(this, rhs);
    }

    public final Sint div(Sint rhs, SymbolicExecution se) {
        return se.div(this, rhs);
    }

    public final Sint mul(Sint rhs, SymbolicExecution se) {
        return se.mul(this, rhs);
    }

    public final Sint mod(Sint rhs, SymbolicExecution se) {
        return se.mod(this, rhs);
    }

    public final Sint neg(SymbolicExecution se) {
        return se.neg(this);
    }

    public final Sbool lt(Sint rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }
    public final Sbool lte(Sint rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }

    public final Sbool gt(Sint rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }

    public final Sbool gte(Sint rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }

    public final Sbool eq(Sint rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }

    public final boolean ltChoice(SymbolicExecution se) {
        return se.ltChoice(this);
    }

    public final boolean lteChoice(SymbolicExecution se) {
        return se.lteChoice(this);
    }

    public final boolean eqChoice(SymbolicExecution se) {
        return se.eqChoice(this);
    }

    public final boolean notEqChoice(SymbolicExecution se) {
        return se.notEqChoice(this);
    }

    public final boolean gtChoice(SymbolicExecution se) {
        return se.gtChoice(this);
    }

    public final boolean gteChoice(SymbolicExecution se) {
        return se.gteChoice(this);
    }

    public final boolean ltChoice(Sint rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }

    public final boolean lteChoice(Sint rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }

    public final boolean eqChoice(Sint rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }

    public final boolean notEqChoice(Sint rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }

    public final boolean gtChoice(Sint rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }

    public final boolean gteChoice(Sint rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
    }

    public final Sdouble i2d(SymbolicExecution se) {
        return se.i2d(this);
    }

    public final Sfloat i2f(SymbolicExecution se) {
        return se.i2f(this);
    }

    public final Slong i2l(SymbolicExecution se) {
        return se.i2l(this);
    }

    public final Sbyte i2b(SymbolicExecution se) {
        return se.i2b(this);
    }

    public final Sshort i2s(SymbolicExecution se) {
        return se.i2s(this);
    }

    public final Schar i2c(SymbolicExecution se) {
        return se.i2c(this);
    }

    public final Sint ishl(Sint i, SymbolicExecution se) {
        return se.ishl(this, i);
    }

    public final Sint ishr(Sint i, SymbolicExecution se) {
        return se.ishr(this, i);
    }

    public final Sint ixor(Sint i, SymbolicExecution se) {
        return se.ixor(this, i);
    }

    public final Sint ior(Sint i, SymbolicExecution se) {
        return se.ior(this, i);
    }

    public final Sint iand(Sint i, SymbolicExecution se) {
        return se.iand(this, i);
    }

    public final Sint iushr(Sint i, SymbolicExecution se) {
        return se.iushr(this, i);
    }

    @Override
    public final boolean isFp() {
        return false;
    }

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
        public static final ConcSint MINUS_ONE;
        public static final ConcSint ZERO;
        public static final ConcSint ONE;
        private final int value;

        private ConcSint(int value) {
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
            return "ConcSint{value=" + value + "}";
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

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
