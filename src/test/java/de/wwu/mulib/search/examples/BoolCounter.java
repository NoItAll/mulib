package de.wwu.mulib.search.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public class BoolCounter {

    @Test
    public void testBooleanCounterSized4() {
        TestUtility.getAllSolutions(this::_testBooleanCounterSized4, "count4");
    }

    private List<PathSolution> _testBooleanCounterSized4(MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "_count4",
                BoolCounter.class,
                config
        );
        assertEquals(16, result.size());
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        return result;
    }

    private void testIfAllNumbersInRangeAndNoAdditionalSolutions(List<PathSolution> result) {
        for (int i = 0; i < 16; i++) {
            final Integer I = i;
            assertTrue(result.stream().anyMatch(s -> I.equals(((Integer) s.getInitialSolution().value))),
                    "Value " + i + " is expected but cannot be found.");
        }
        for (PathSolution ps : result) {
            assertEquals(1, ps.getCurrentlyInitializedSolutions().size());
        }
    }

    @Test
    public void testBooleanCounterManualBooleansSized4() {
        TestUtility.getAllSolutions(this::_testBooleanCounterManualBooleansSized4, "count4Manual");
    }

    private List<PathSolution> _testBooleanCounterManualBooleansSized4(MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib(
                "_count4Manual",
                BoolCounter.class,
                config
        );

        assertEquals(16, result.size());
        testIfAllNumbersInRangeAndNoAdditionalSolutions(result);
        return result;
    }

    @Test
    public void test4Times3Iterations() {
        TestUtility.getAllSolutions(this::_test4Times3Iterations, "count4Times3Manual");
    }

    private List<PathSolution> _test4Times3Iterations(MulibConfig config) {
        List<PathSolution> resultList = TestUtility.executeMulib(
                "_count4Times3Manual",
                BoolCounter.class,
                1,
                config
        );
        Queue<PathSolution> result = new ArrayDeque<>(resultList);
        assertEquals(4096, result.size());
        int[] count = new int[46];
        while (!result.isEmpty()) {
            Integer res = (Integer) result.remove().getInitialSolution().value;
            count[res]++;
        }

        for (int i = 0; i < count.length; i++) {
            int opposingIndex = count.length - i - 1;
            assertEquals(count[i], count[opposingIndex]);
        }
        return resultList;
    }

    public void testBooleanCounterSized16() {
        final List<PathSolution> result = TestUtility.executeMulib(
                "_count16",
                BoolCounter.class,
                1,
                MulibConfig.get()
        );
        assertEquals(65536, result.size());
    }

    public static Sint _count4() {
        SymbolicExecution se = SymbolicExecution.get();
        Sbool b0 = se.trackedSymSbool("b0");
        Sbool b1 = se.trackedSymSbool("b1");
        Sbool b2 = se.trackedSymSbool("b2");
        Sbool b3 = se.trackedSymSbool("b3");
        Sint count = Sint.newConcSint(0);
        if (se.boolChoice(b0)) {
            count = count.add(Sint.newConcSint(1), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b0))) {
                count = count.add(Sint.newConcSint(1), se);
            }
        }
        if (se.boolChoice(b1)) {
            count = count.add(Sint.newConcSint(2), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b1))) {
                count = count.add(Sint.newConcSint(999999), se);
            }
        }
        if (se.boolChoice(b2)) {
            count = count.add(Sint.newConcSint(4), se);
        }
        if (se.boolChoice(Sbool.newInputSymbolicSbool())) {
            // Should not influence the overall number of solutions
            throw new Fail();
        }
        if (se.boolChoice(b3)) {
            count = count.add(Sint.newConcSint(8), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b3))) {
                count = count.add(Sint.newConcSint(999999), se);
            }
        }
        return count;
    }

    // Check for handling of side-effects and consistency of constraints
    public static Sint _count4Manual() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint count = Sint.newConcSint(0);

        Sbool b0 = se.symSbool();

        Sdouble d0 = se.symSdouble().neg(se);
        Sdouble d1 = Sdouble.newConcSdouble(22.0);
        Sbool b3 = se.gt(d0, d1);

        if (se.boolChoice(b0)) {
            count = count.add(Sint.newConcSint(1), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b0))) {
                count = count.add(Sint.newConcSint(999), se);
            }
        }
        Sbool b1 = se.symSbool();
        Sint i0 = se.symSint();
        Sint i1 = se.symSint();
        Sbool b2 = se.lte(i0, i1);

        if (se.boolChoice(b1)) {
            count = count.add(Sint.newConcSint(2), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b1))) {
                count = count.add(Sint.newConcSint(999999), se);
            }
        }
        if (se.boolChoice(b2)) {
            count = count.add(Sint.newConcSint(4), se);
        }
        if (se.boolChoice(se.symSbool())) {
            // Should not influence the overall number of solutions
            throw new Fail();
        }

        if (se.boolChoice(b3)) {
            count = count.add(Sint.newConcSint(8), se);
            // Should not be reachable
            if (se.boolChoice(se.not(b3))) {
                count = count.add(Sint.newConcSint(999999999), se);
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
        Sint count = Sint.newConcSint(0);

        for (int i = 0; i < 3; i++) {
            Sint i0 = se.symSint();
            Sint i1 = se.trackedSymSint("notImportant");
            // Check backtracking behavior also for long paths
            if (se.ltChoice(i0, i1)) {
                throw new Fail();
            } else if (se.ltChoice(i0, i1.add(se.concSint(22), se))) {
                throw new Fail();
            }

            if (se.ltChoice(i0, i1)) {
                throw new MulibRuntimeException("Should not be possible");
            } else if (se.ltChoice(i0, i1.add(se.concSint(22), se))) {
                throw new MulibRuntimeException("Should not be possible");
            }
            count = se.castTo(count.add(_count4Manual(), se), Sint.class);
        }

        return count;
    }

    public static Sint _count16() {
        SymbolicExecution se = SymbolicExecution.get();
        Sbool b0 = se.symSbool(); Sbool b1 = se.symSbool(); Sbool b2 = se.symSbool(); Sbool b3 = se.symSbool();
        Sbool b4 = se.symSbool(); Sbool b5 = se.symSbool(); Sbool b6 = se.symSbool(); Sbool b7 = se.symSbool();
        Sbool b8 = se.symSbool(); Sbool b9 = se.symSbool(); Sbool b10 = se.symSbool(); Sbool b11 = se.symSbool();
        Sbool b12 = se.symSbool(); Sbool b13 = se.symSbool(); Sbool b14 = se.symSbool(); Sbool b15 = se.symSbool();

        Sint count = Sint.newConcSint(0);
        if (se.boolChoice(b0)) {
            count = count.add(Sint.newConcSint(1), se);
        }
        if (se.boolChoice(b1)) {
            count = count.add(Sint.newConcSint(2), se);
        }
        if (se.boolChoice(b2)) {
            count = count.add(Sint.newConcSint(4), se);
        }
        if (se.boolChoice(b3)) {
            count = count.add(Sint.newConcSint(8), se);
        }
        if (se.boolChoice(b4)) {
            count = count.add(Sint.newConcSint(16), se);
        }
        if (se.boolChoice(b5)) {
            count = count.add(Sint.newConcSint(32), se);
        }
        if (se.boolChoice(b6)) {
            count = count.add(Sint.newConcSint(64), se);
        }
        if (se.boolChoice(b7)) {
            count = count.add(Sint.newConcSint(128), se);
        }
        if (se.boolChoice(b8)) {
            count = count.add(Sint.newConcSint(256), se);
        }
        if (se.boolChoice(b9)) {
            count = count.add(Sint.newConcSint(512), se);
        }
        if (se.boolChoice(b10)) {
            count = count.add(Sint.newConcSint(1024), se);
        }
        if (se.boolChoice(b11)) {
            count = count.add(Sint.newConcSint(2048), se);
        }
        if (se.boolChoice(b12)) {
            count = count.add(Sint.newConcSint(4096), se);
        }
        if (se.boolChoice(b13)) {
            count = count.add(Sint.newConcSint(8192), se);
        }
        if (se.boolChoice(b14)) {
            count = count.add(Sint.newConcSint(16384), se);
        }
        if (se.boolChoice(b15)) {
            count = count.add(Sint.newConcSint(32768), se);
        }
        return count;
    }
}
