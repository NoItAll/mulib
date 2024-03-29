package de.wwu.mulib.search.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InfiniteLoop {

    @Test
    public void checkSymbolicInfiniteLoop() {
        TestUtility.getAllSolutions(this::_checkSymbolicInfiniteLoop, "infiniteSymbolic");
    }

    private List<PathSolution> _checkSymbolicInfiniteLoop(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "infiniteSymbolic",
                InfiniteLoop.class,
                mb,
                false
        );
        assertTrue(result.size() > 0);
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    public static void infiniteSymbolic() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint i = se.concSint(0);
        while (se.boolChoice(se.symSbool())) {
            se.add(i, se.concSint(1));
        }
    }

    @Test
    public void checkSymbolicInfiniteLoopAlt() {
        TestUtility.getAllSolutions(this::_checkSymbolicInfiniteLoopAlt, "infiniteSymbolicAlt");
    }

    private List<PathSolution> _checkSymbolicInfiniteLoopAlt(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "infiniteSymbolicAlt",
                InfiniteLoop.class,
                mb,
                false
        );
        assertEquals(0, result.size());
        return result;
    }

    public static void infiniteSymbolicAlt() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint i = se.concSint(0);
        Sbool b = se.symSbool();
        if (!se.boolChoice(b)) {
            throw Mulib.fail();
        }
        while (se.boolChoice(b)) {
            se.add(i, se.concSint(1));
        }
    }

    @Test
    public void checkSymbolicInfiniteLoopAlt2() {
        TestUtility.getAllSolutions(this::_checkSymbolicInfiniteLoopAlt2, "infiniteSymbolicAlt2");
    }

    private List<PathSolution> _checkSymbolicInfiniteLoopAlt2(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "infiniteSymbolicAlt2",
                InfiniteLoop.class,
                mb,
                false
        );
        assertEquals(0, result.size());
        return result;
    }

    public static void infiniteSymbolicAlt2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint i = se.concSint(0);
        Sbool b;
        do {
            b = se.symSbool();
            if (!se.boolChoice(b)) {
                throw Mulib.fail();
            }
            se.add(i, se.concSint(1));
        } while (se.boolChoice(b));
    }

}
