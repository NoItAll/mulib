package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.AliasingAndIdsEq;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AliasingAndIdsEqTransfExec {

    @Test
    public void testObjectEqsAndAliasing() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
            List<PathSolution> result = TestUtility.executeMulib(
                    "objectsEq",
                    AliasingAndIdsEq.class,
                    mb,
                    true
            );
            assertEquals(2, result.size());
            Solution s0 = result.get(0).getSolution();
            Solution s1 = result.get(1).getSolution();
            if (((AliasingAndIdsEq.ResultContainer) s0.returnValue).getResultIndicator() == 2) {
                assertEquals(3, ((AliasingAndIdsEq.ResultContainer) s1.returnValue).getResultIndicator());
            } else {
                assertEquals(3, ((AliasingAndIdsEq.ResultContainer) s0.returnValue).getResultIndicator());
                assertEquals(2, ((AliasingAndIdsEq.ResultContainer) s1.returnValue).getResultIndicator());
            }
            assertSame(((AliasingAndIdsEq.ResultContainer) s0.returnValue).getAliasingObject(), ((AliasingAndIdsEq.ResultContainer) s0.returnValue).getAliasingTarget());
            assertSame(((AliasingAndIdsEq.ResultContainer) s1.returnValue).getAliasingObject(), ((AliasingAndIdsEq.ResultContainer) s1.returnValue).getAliasingTarget());

            if (((AliasingAndIdsEq.ResultContainer) s1.returnValue).getResultIndicator() == 2) {
                assertEquals(3, ((AliasingAndIdsEq.ResultContainer) s0.returnValue).getResultIndicator());
            } else {
                assertEquals(2, ((AliasingAndIdsEq.ResultContainer) s0.returnValue).getResultIndicator());
                assertEquals(3, ((AliasingAndIdsEq.ResultContainer) s1.returnValue).getResultIndicator());
            }

            mb.setALIASING_FOR_FREE_OBJECTS(true);
            result = TestUtility.executeMulib(
                    "objectsEq",
                    AliasingAndIdsEq.class,
                    mb,
                    true
            );
            assertEquals(3, result.size());
            s0 = result.get(0).getSolution();
            s1 = result.get(1).getSolution();
            Solution s2 = result.get(2).getSolution();
            boolean seenFirst = false;
            boolean seenSecond = false;
            boolean seenThird = false;
            for (Solution s : List.of(s0, s1, s2)) {
                byte indicator = ((AliasingAndIdsEq.ResultContainer) s.returnValue).getResultIndicator();
                if (indicator == 1) {
                    seenFirst = true;
                } else if (indicator == 2) {
                    seenSecond = true;
                } else if (indicator == 3) {
                    seenThird = true;
                }
                assertSame(
                        ((AliasingAndIdsEq.ResultContainer) s.returnValue).getAliasingObject(),
                        ((AliasingAndIdsEq.ResultContainer) s.returnValue).getAliasingTarget()
                );
            }
            assertTrue(seenFirst);
            assertTrue(seenSecond);
            assertTrue(seenThird);
        }, "AliasingAndIdsEq.objectsEq");
    }

}
