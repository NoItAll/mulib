package de.wwu.mulib.search.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntComparison {

    @Test
    public void testCount() {
        TestUtility.getAllSolutions(this::_testCount, "compare");
    }

    private List<PathSolution> _testCount(MulibConfig config) {
        List<PathSolution> result = TestUtility.executeMulib("compare", IntComparison.class, config);
        assertEquals(1, result.size());
        return result;
    }

    public static Sint compare() {
        SymbolicExecution se = SymbolicExecution.get();

        Sint alwaysLargerVal = Sint.newConcSint(1);
        Sint alwaysLowerVal = Sint.newConcSint(0);

        Sint n00 = se.symSint();
        Sint n01 = se.symSint();

        if (se.getCpFactory().eqChoice(se, n00, n01)) {
            alwaysLargerVal = alwaysLargerVal.add(n00, se);
            alwaysLowerVal = alwaysLowerVal.add(n01, se);
        } else {
            throw new Fail();
        }

        Sint n10 = se.symSint();
        Sint n11 = se.symSint();
        Sint n20 = se.symSint();
        Sint n21 = se.symSint();

        if (se.getCpFactory().gteChoice(se, n10, n11)) {
            alwaysLargerVal = alwaysLargerVal.add(n10, se);
            alwaysLowerVal = alwaysLowerVal.add(n11, se);
        } else {
            throw new Fail();
        }

        if (se.getCpFactory().ltChoice(se, n21, n20)) {
            alwaysLargerVal = alwaysLargerVal.add(n20, se);
            alwaysLowerVal = alwaysLowerVal.add(n21, se);
        } else {
            throw new Fail();
        }

        if (se.getCpFactory().lteChoice(se, alwaysLargerVal, alwaysLowerVal)) {
            throw new MulibRuntimeException("This cannot occur.");
        }

        Sint result = alwaysLargerVal.sub(alwaysLowerVal, se);

        if (se.getCpFactory().lteChoice(se, result, Sint.newConcSint(0))) {
            throw new MulibRuntimeException("This cannot occur.");
        }

        return result;
    }

}
