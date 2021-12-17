package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Sint extends AbstractSnumber {
    public static final ConcSint MINUS_ONE = new ConcSint(-1);
    public static final ConcSint ZERO = new ConcSint(0);
    public static final ConcSint ONE = new ConcSint(1);

    Sint() {}

    private static final Map<Integer, ConcSint> cache = Collections.synchronizedMap(new HashMap<>());

    public static Sint concSint(int i) {
        Integer I = i;
        ConcSint result = cache.get(I);
        if (result == null) {
            result = new ConcSint(i);
            cache.put(I, result);
        }
        return result;
    }

    public static Sint newInputSymbolicSint() {
        return new SymSint();
    }

    public static Sint newExpressionSymbolicSint(NumericExpression representedExpression) {
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


    @Override
    public final boolean isFp() {
        return false;
    }

    public static final class ConcSint extends Sint implements ConcSnumber {
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
        public String additionToToStringBody() {
            return ",val=" + value;
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
    }
}
