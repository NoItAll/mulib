package de.wwu.mulib.search.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoolCounter {

    @Test
    public void testBooleanCounterSized4() {
        TestUtility.getAllSolutions(this::_testBooleanCounterSized4, "count4");
    }

    private Boolean _testBooleanCounterSized4(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "_count4",
                BoolCounter.class,
                mb,
                false
        );
        assertEquals(16, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        List<Solution> solutions = TestUtility.getUpToNSolutions(
                100, // Only 16 possible
                "_count4",
                BoolCounter.class,
                mb,
                false,
                new Class[0],
                new Object[0]
        );
        assertEquals(16, solutions.size());
        return true;
    }

    private void testIfAllNumbersInRangeAndNoAdditionalSolutions(List<PathSolution> result) {
        for (int i = 0; i < 16; i++) {
            final Integer I = i;
            assertTrue(result.stream().anyMatch(s -> I.equals((s.getSolution().returnValue))),
                    "Value " + i + " is expected but cannot be found.");
        }
    }

    @Test
    public void testBooleanCounterManualBooleansSized4() {
        TestUtility.getAllSolutions(this::_testBooleanCounterManualBooleansSized4, "count4Manual");
    }

    private List<PathSolution> _testBooleanCounterManualBooleansSized4(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "_count4Manual",
                BoolCounter.class,
                mb,
                false
        );
        assertEquals(16, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        return result;
    }

    @Test
    public void test4Times3Iterations() {
        TestUtility.getAllSolutions(this::_test4Times3Iterations, "count4Times3Manual");
    }

    private List<PathSolution> _test4Times3Iterations(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> resultList = TestUtility.executeMulib(
                "_count4Times3Manual",
                BoolCounter.class,
                mb,
                false
        );
        Queue<PathSolution> result = new ArrayDeque<>(resultList);
        assertEquals(4096, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        int[] count = new int[46];
        while (!result.isEmpty()) {
            Integer res = (Integer) result.remove().getSolution().returnValue;
            count[res]++;
        }

        for (int i = 0; i < count.length; i++) {
            int opposingIndex = count.length - i - 1;
            assertEquals(count[i], count[opposingIndex]);
        }
        return resultList;
    }

    public static Sint _count4() {
        SymbolicExecution se = SymbolicExecution.get();
        Sbool b0 = se.namedSymSbool("b0");
        Sbool b1 = se.namedSymSbool("b1");
        Sbool b2 = se.namedSymSbool("b2");
        Sbool b3 = se.namedSymSbool("b3");
        Sint count = Sint.concSint(0);
        if (se.boolChoice(b0)) {
            count = count.add(Sint.concSint(1), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b0))) {
                count = count.add(Sint.concSint(1), se);
            }
        }
        if (se.boolChoice(b1)) {
            count = count.add(Sint.concSint(2), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b1))) {
                count = count.add(Sint.concSint(999999), se);
            }
        }
        if (se.boolChoice(b2)) {
            count = count.add(Sint.concSint(4), se);
        }
        if (se.boolChoice(se.symSbool())) {
            // Should not influence the overall number of solutions
            throw Mulib.fail();
        }
        if (se.boolChoice(b3)) {
            count = count.add(Sint.concSint(8), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b3))) {
                count = count.add(Sint.concSint(999999), se);
            }
        }
        return count;
    }

    // Check for handling of side-effects and consistency of constraints
    public static Sint _count4Manual() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint count = Sint.concSint(0);

        Sbool b0 = se.symSbool();

        Sdouble d0 = se.symSdouble().neg(se);
        Sdouble d1 = Sdouble.concSdouble(22.0);
        Sbool b3 = se.gt(d0, d1);

        if (se.boolChoice(b0)) {
            count = count.add(Sint.concSint(1), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b0))) {
                throw new MulibRuntimeException("Must not occur");
            }
        }
        Sbool b1 = se.symSbool();
        Sint i0 = se.symSint();
        Sint i1 = se.symSint();
        Sbool b2 = se.lte(i0, i1);

        if (se.boolChoice(b1)) {
            count = count.add(Sint.concSint(2), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b1))) {
                throw new MulibRuntimeException("Must not occur");
            }
        }
        if (se.boolChoice(b2)) {
            count = count.add(Sint.concSint(4), se);
        }
        if (se.boolChoice(se.symSbool())) {
            // Should not influence the overall number of solutions
            throw Mulib.fail();
        }

        if (se.boolChoice(b3)) {
            count = count.add(Sint.concSint(8), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b3))) {
                throw new MulibRuntimeException("Must not occur");
            }
        }
        Sbool notb3 = se.not(b3);

        Sbool b3AndNotb3 = se.and(b3, notb3);
        if (se.boolChoice(b3AndNotb3)) {
            throw new MulibRuntimeException("Should not be possible");
        }

        return count;
    }

    public static Sint _count4Times3Manual() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint count = Sint.concSint(0);

        for (int i = 0; i < 3; i++) {
            Sint i0 = se.symSint();
            Sint i1 = se.symSint();
            // Check backtracking behavior also for long paths
            if (se.ltChoice(i0, i1)) {
                throw Mulib.fail();
            } else if (se.ltChoice(i0, i1.add(se.concSint(22), se))) {
                throw Mulib.fail();
            }

            if (se.ltChoice(i0, i1)) {
                throw new MulibRuntimeException("Should not be possible");
            } else if (se.ltChoice(i0, i1.add(se.concSint(22), se))) {
                throw new MulibRuntimeException("Should not be possible");
            }
            count = count.add(_count4Manual(), se);
        }

        return count;
    }
}
