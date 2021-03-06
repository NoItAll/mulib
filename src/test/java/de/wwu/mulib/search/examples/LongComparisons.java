package de.wwu.mulib.search.examples;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Slong;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LongComparisons {

    @Test
    public void testAbs0() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib("abs0", LongComparisons.class, mb, false);
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    return result;
                },
                "testAbs0"
        );
    }

    @Test
    public void testAbs1() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib("abs1", LongComparisons.class, mb, false);
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    return result;
                },
                "testAbs1"
        );
    }

    public static int abs0() {
        SymbolicExecution se = SymbolicExecution.get();
        Slong l = se.namedSymSlong("l");
        Sint cmp = l.cmp(SymbolicExecution.concSlong(0, se), se);
        if (cmp.eqChoice(SymbolicExecution.concSint(0, se), se)) {
            if (!l.eqChoice(SymbolicExecution.concSlong(0, se), se)) {
                throw new MulibRuntimeException("Must not occur!");
            }
        }
        if (cmp.eqChoice(SymbolicExecution.concSint(1, se), se)) {
            if (!l.gtChoice(SymbolicExecution.concSlong(0, se), se)) {
                throw new MulibRuntimeException("Must not occur!");
            }
        }
        if (cmp.eqChoice(SymbolicExecution.concSint(-1, se), se)) {
            if (!l.ltChoice(SymbolicExecution.concSlong(0, se), se)) {
                throw new MulibRuntimeException("Must not occur!");
            }
        }
        l = cmp.gtChoice(se) ? l.neg(se) : l;
        if (l.gtChoice(SymbolicExecution.concSlong(0, se), se)) {
            throw new MulibRuntimeException("Must not occur!");
        }
        return 0;
    }

    public static int abs1() {
        SymbolicExecution se = SymbolicExecution.get();
        Slong l = se.namedSymSlong("l");
        Sint cmp = l.cmp(SymbolicExecution.concSlong(0, se), se);
        l = cmp.gtChoice(se) ? l.neg(se) : l;
        if (l.gtChoice(SymbolicExecution.concSlong(0, se), se)) {
            throw new MulibRuntimeException("Must not occur!");
        }
        return 0;
    }
}
