package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.expressions.NumericExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Sint extends Sintegernumber {
    public static final ConcSint MINUS_ONE = new ConcSint(-1);
    public static final ConcSint ZERO = new ConcSint(0);
    public static final ConcSint ONE = new ConcSint(1);

    private Sint() {}

    private static Map<Integer, ConcSint> cache = Collections.synchronizedMap(new HashMap<>());

    public static ConcSint newConcSint(int i) {
        Integer I = i;
        ConcSint result = cache.get(I);
        if (result != null) {
            return result;
        }
        result = new ConcSint(i);
        cache.put(I, result);
        return result;
    }


    public static SymSint newInputSymbolicSint() {
        return new SymSint();
    }

    public static SymSint newExpressionSymbolicSint(NumericExpression representedExpression) {
        return new SymSint(representedExpression);
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

    public static final class SymSint extends Sint implements SymNumericExpressionSprimitive {
        private final NumericExpression representedExpression;

        private SymSint() {
            this.representedExpression = this;
        }

        private SymSint(NumericExpression representedExpression) {
            if (representedExpression.isFp()) {
                throw new MulibRuntimeException("Represented NumericExpression must be an integer.");
            }
            this.representedExpression = representedExpression;
        }

        @Override
        public NumericExpression getRepresentedExpression() {
            return representedExpression;
        }
    }
}
