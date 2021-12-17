package de.wwu.mulib.search.examples;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.expressions.Mul;
import de.wwu.mulib.expressions.Neg;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbsValueMul {

    @Test
    public void testAbsMul() {
        TestUtility.getAllSolutions(c->c.setLABEL_RESULT_VALUE(false), this::_testAbsMul, "absMul");
    }

    private List<PathSolution> _testAbsMul(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "absMul",
                AbsValueMul.class,
                mb,
                false
        );
        assertEquals(4, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        boolean first = false; boolean second = false; boolean third = false; boolean fourth = false;
        for (PathSolution s : result) {
            Sint.SymSint solContent = (Sint.SymSint) s.getInitialSolution().value;
            NumericExpression representedExpression = solContent.getRepresentedExpression();
            assertTrue(representedExpression instanceof Mul);
            NumericExpression expr0 = ((Mul) representedExpression).getExpr0();
            NumericExpression expr1 = ((Mul) representedExpression).getExpr1();
            if (expr0 instanceof Neg && expr1 instanceof Neg) {
                assertTrue(((Neg) expr0).getWrapped() instanceof Sint.SymSint);
                assertTrue(((Neg) expr1).getWrapped() instanceof Sint.SymSint);
                first = true;
            } else if (expr0 instanceof Neg && expr1 instanceof Sint.SymSint) {
                second = true;
            } else if (expr0 instanceof Sint.SymSint && expr1 instanceof Neg) {
                third = true;
            } else if (expr0 instanceof Sint.SymSint && expr1 instanceof Sint.SymSint) {
                fourth = true;
            }
        }
        assertTrue(first && second && third && fourth, "Not all solutions found.");
        return result;
    }

    public static Sint absMul() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint i0 = se.namedSymSint("i0");
        Sint i1 = se.namedSymSint("i1");
        return abs(i0).mul(abs(i1), se);
    }

    public static Sint abs(Sint i) {
        SymbolicExecution se = SymbolicExecution.get();
        if (se.getCpFactory().ltChoice(se, i, Sint.concSint(0))) {
            if (se.getCpFactory().boolChoice(se, Sbool.newInputSymbolicSbool())) {
                throw new Fail();
            }
            return i.neg(se);
        } else {
            return i;
        }
    }

}
