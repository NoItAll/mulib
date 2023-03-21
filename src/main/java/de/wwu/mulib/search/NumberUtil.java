package de.wwu.mulib.search;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.*;

public class NumberUtil {

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
        if (lhs instanceof Sint && rhs instanceof Sint) {
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
