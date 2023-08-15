package de.wwu.mulib.search;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.*;

/**
 * Utility class for comparing {@link ConcSnumber}
 */
public class NumberUtil {

    /**
     * Evaluates whether lhs < rhs
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if lhs < rhs, else false
     */
    public static boolean lt(ConcSnumber lhs, ConcSnumber rhs) {
        return compareConcSnumber(lhs, rhs) == -1;
    }

    /**
     * Evaluates whether lhs <= rhs
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if lhs <= rhs, else false
     */
    public static boolean lte(ConcSnumber lhs, ConcSnumber rhs) {
        int result = compareConcSnumber(lhs, rhs);
        return result == -1 || result == 0;
    }

    /**
     * Evaluates whether lhs == rhs
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if lhs == rhs, else false
     */
    public static boolean eq(ConcSnumber lhs, ConcSnumber rhs) {
        int result = compareConcSnumber(lhs, rhs);
        return result == 0;
    }

    private static int compareConcSnumber(ConcSnumber lhs, ConcSnumber rhs) {
        if (lhs instanceof Sint && rhs instanceof Sint) {
            return Integer.compare(lhs.intVal(), rhs.intVal());
        } else if (lhs instanceof Sfpnumber || rhs instanceof Sfpnumber) {
            if (lhs instanceof Sdouble || rhs instanceof Sdouble) {
                return Double.compare(lhs.doubleVal(), rhs.doubleVal());
            } else {
                return Float.compare(lhs.floatVal(), rhs.floatVal());
            }
        } else if (lhs instanceof Slong || rhs instanceof Slong) {
            return Long.compare(lhs.longVal(), rhs.longVal());
        } else {
            throw new NotYetImplementedException();
        }
    }
}
