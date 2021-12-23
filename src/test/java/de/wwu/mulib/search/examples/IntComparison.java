package de.wwu.mulib.search.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntComparison {

    @Test
    public void testCount() {
        TestUtility.getAllSolutions(this::_testCount, "compare");
    }

    private List<PathSolution> _testCount(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib("compare", IntComparison.class, mb, false);
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    public static Sint compare() {
        SymbolicExecution se = SymbolicExecution.get();

        Sint alwaysLargerVal = Sint.concSint(1);
        Sint alwaysLowerVal = Sint.concSint(0);

        Sint n00 = se.symSint();
        Sint n01 = se.symSint();

        if (se.eqChoice(n00, n01)) {
            alwaysLargerVal = alwaysLargerVal.add(n00, se);
            alwaysLowerVal = alwaysLowerVal.add(n01, se);
        } else {
            throw new Fail();
        }

        Sint n10 = se.symSint();
        Sint n11 = se.symSint();
        Sint n20 = se.symSint();
        Sint n21 = se.symSint();

        if (se.gteChoice(n10, n11)) {
            alwaysLargerVal = alwaysLargerVal.add(n10, se);
            alwaysLowerVal = alwaysLowerVal.add(n11, se);
        } else {
            throw new Fail();
        }

        if (se.ltChoice(n21, n20)) {
            alwaysLargerVal = alwaysLargerVal.add(n20, se);
            alwaysLowerVal = alwaysLowerVal.add(n21, se);
        } else {
            throw new Fail();
        }

        if (se.lteChoice(alwaysLargerVal, alwaysLowerVal)) {
            throw new MulibRuntimeException("This cannot occur.");
        }

        Sint result = alwaysLargerVal.sub(alwaysLowerVal, se);

        if (se.lteChoice(result, Sint.concSint(0))) {
            throw new MulibRuntimeException("This cannot occur.");
        }

        return result;
    }

}
