package de.wwu.mulib.search;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.values.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public class NumberUtil {

    public static Snumber addConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        return newConcsNumberViaOperator(lhs, rhs,
                Integer::sum,
                Long::sum,
                Float::sum,
                Double::sum
        );
    }

    public static Snumber subConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        return newConcsNumberViaOperator(lhs, rhs,
                (l,r) -> l-r,
                (l,r) -> l-r,
                (l,r) -> l-r,
                (l,r) -> l-r
        );
    }

    public static Snumber mulConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        return newConcsNumberViaOperator(lhs, rhs,
                (l,r) -> l*r,
                (l,r) -> l*r,
                (l,r) -> l*r,
                (l,r) -> l*r
        );
    }

    public static Snumber divConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        return newConcsNumberViaOperator(lhs, rhs,
                (l,r) -> l/r,
                (l,r) -> l/r,
                (l,r) -> l/r,
                (l,r) -> l/r
        );
    }

    public static Snumber modConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        return newConcsNumberViaOperator(lhs, rhs,
                (l,r) -> l%r,
                (l,r) -> l%r,
                (l,r) -> l%r,
                (l,r) -> l%r
        );
    }

    public static Snumber neg(ConcSnumber n) {
        return newConcsNumber(
                n,
                (i) -> -i,
                (l) -> -l,
                (f) -> -f,
                (d) -> -d
        );
    }

    public static Snumber abs(ConcSnumber n) {
        return newConcsNumber(
                n,
                Math::abs,
                Math::abs,
                Math::abs,
                Math::abs
        );
    }

    public static boolean gt(ConcSnumber lhs, ConcSnumber rhs) {
        return compareConcSnumber(lhs, rhs) == 1;
    }

    public static boolean gte(ConcSnumber lhs, ConcSnumber rhs) {
        int result = compareConcSnumber(lhs, rhs);
        return result == 1 || result == 0;
    }

    public static boolean lt(ConcSnumber lhs, ConcSnumber rhs) {
        return compareConcSnumber(lhs, rhs) == -1;
    }

    public static boolean lte(ConcSnumber lhs, ConcSnumber rhs) {
        int result = compareConcSnumber(lhs, rhs);
        return result == -1 || result == 0;
    }

    public static boolean eq(ConcSnumber lhs, ConcSnumber rhs) {
        int result = compareConcSnumber(lhs, rhs);
        return result == 0;
    }

    public static NumericExpression getRepresentedExpression(Snumber n) {
        return n instanceof SymNumericExpressionSprimitive ?
                ((SymNumericExpressionSprimitive) n).getRepresentedExpression()
                :
                n;
    }

    public static Snumber newWrappingSnumberDependingOnLhsAndRhs(
            Snumber lhs, Snumber rhs,
            NumericExpression expressionToWrap,
            ValueFactory valueFactory) {
        if (lhs instanceof Sintegernumber && rhs instanceof Sintegernumber) {
            return valueFactory.wrappingSymSint(expressionToWrap);
        } else if (lhs instanceof Sdouble || rhs instanceof Sdouble) {
            return valueFactory.wrappingSymSdouble(expressionToWrap);
        } else if (lhs instanceof Sfloat || rhs instanceof Sfloat) {
            return valueFactory.wrappingSymSfloat(expressionToWrap);
        } else if (lhs instanceof Slong || rhs instanceof Slong) {
            return valueFactory.wrappingSymSlong(expressionToWrap);
        } else {
            throw new NotYetImplementedException();
        }
    }

    public static Snumber newWrappingSnumberDependingOnSnumber(
            Snumber n,
            NumericExpression expressionToWrap,
            ValueFactory valueFactory) {
        if (n instanceof Sdouble) {
            return valueFactory.wrappingSymSdouble(expressionToWrap);
        } else if (n instanceof Sfloat) {
            return valueFactory.wrappingSymSfloat(expressionToWrap);
        } else if (n instanceof Sintegernumber) {
            return valueFactory.wrappingSymSint(expressionToWrap);
        } else if (n instanceof Slong) {
            return valueFactory.wrappingSymSlong(expressionToWrap);
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static Snumber newConcsNumberViaOperator(
            ConcSnumber lhs, ConcSnumber rhs,
            BiFunction<Integer, Integer, Integer> integerOperatorFunction,
            BiFunction<Long, Long, Long> longOperatorFunction,
            BiFunction<Float, Float, Float> floatOperatorFunction,
            BiFunction<Double, Double, Double> doubleOperatorFunction) {
        if (lhs instanceof Sintegernumber && rhs instanceof Sintegernumber) {
            return Sint.newConcSint(integerOperatorFunction.apply(lhs.intVal(), rhs.intVal()));
        } else if (lhs instanceof Sdouble || rhs instanceof Sdouble) {
            return Sdouble.newConcSdouble(doubleOperatorFunction.apply(lhs.doubleVal(), rhs.doubleVal()));
        } else if (lhs instanceof Sfloat || rhs instanceof Sfloat) {
            return Sfloat.newConcSfloat(floatOperatorFunction.apply(lhs.floatVal(), rhs.floatVal()));
        } else if (lhs instanceof Slong || rhs instanceof Slong) {
            return Slong.newConcSlong(longOperatorFunction.apply(lhs.longVal(), rhs.longVal()));
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static Snumber newConcsNumber(
            ConcSnumber n,
            Function<Integer, Integer> integerOperatorFunction,
            Function<Long, Long> longOperatorFunction,
            Function<Float, Float> floatOperatorFunction,
            Function<Double, Double> doubleOperatorFunction) {
        if (n instanceof Sintegernumber) {
            return Sint.newConcSint(integerOperatorFunction.apply(n.intVal()));
        } else if (n instanceof Slong) {
            return Slong.ConcSlong.newConcSlong(longOperatorFunction.apply(n.longVal()));
        } else if (n instanceof Sdouble) {
            return Sdouble.newConcSdouble(doubleOperatorFunction.apply(n.doubleVal()));
        } else if (n instanceof Sfloat) {
            return Sfloat.newConcSfloat(floatOperatorFunction.apply(n.floatVal()));
        } else {
            throw new NotYetImplementedException();
        }
    }

    private static boolean isFpNumber(Number n) {
        return n instanceof Double || n instanceof Float;
    }

    private static final double MAX_EQUALS_DELTA = 10e-8;
    private static int compareNumbers(Number lhs, Number rhs) {
        if (isFpNumber(lhs) || isFpNumber(rhs)) {
            double dlhs = lhs.doubleValue();
            double drhs = rhs.doubleValue();
            if (Math.abs(dlhs - drhs) < MAX_EQUALS_DELTA) {
                return 0;
            } else {
                return dlhs < drhs ? -1 : 1;
            }
        } else if (lhs instanceof Long || rhs instanceof Long) {
            long llhs = lhs.longValue();
            long lrhs = rhs.longValue();
            if (llhs == lrhs) {
                return 0;
            } else {
                return llhs < lrhs ? -1 : 1;
            }
        } else {
            int ilhs = lhs.intValue();
            int irhs = rhs.intValue();
            if (irhs == ilhs) {
                return 0;
            } else {
                return ilhs < irhs ? -1 : 1;
            }
        }
    }

    public static int compareConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        Number nlhs;
        Number nrhs;
        if (lhs instanceof Sintegernumber && rhs instanceof Sintegernumber) {
            nlhs = lhs.intVal();
            nrhs = rhs.intVal();
        } else if (lhs instanceof Sfpnumber || rhs instanceof Sfpnumber) {
            nlhs = lhs instanceof Sdouble ? lhs.doubleVal() : lhs.floatVal();
            nrhs = rhs instanceof Sdouble ? rhs.doubleVal() : rhs.floatVal();
        } else if (lhs instanceof Slong || rhs instanceof Slong) {
            nlhs = lhs.longVal();
            nrhs = rhs.longVal();
        } else {
            throw new NotYetImplementedException();
        }
        return compareNumbers(nlhs, nrhs);
    }
}
