package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.free_arrays.SimpleSort0;
import de.wwu.mulib.transform_and_execute.examples.free_arrays.SimpleSort1;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FreeArraysExec {
    @Test
    public void testSimpleSort0() {
        TestUtility.getSolution(
                (mb) -> {
                    Optional<PathSolution> result = TestUtility.executeMulibForOne(
                            "sort",
                            SimpleSort0.class,
                            1,
                            mb,
                            true,
                            new Class[] { int[].class },
                            new Object[] { new int[] { -81, 42, 9, 78, 0, 1, 8 } }
                    );
                    assertTrue(result.isPresent());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    PathSolution pathSolution = result.get();
                    Solution s = pathSolution.getInitialSolution();
                    Object[] values = (Object[]) s.value;
                    assertEquals(-81, values[0]);
                    assertEquals(0, values[1]);
                    assertEquals(1, values[2]);
                    assertEquals(8, values[3]);
                    assertEquals(9, values[4]);
                    assertEquals(42, values[5]);
                    assertEquals(78, values[6]);
                    return result;
                }
        );
    }

    @Test
    public void testSimpleSort1() {
        TestUtility.getSolution(
                (mb) -> {
                    Optional<PathSolution> result = TestUtility.executeMulibForOne(
                            "sort",
                            SimpleSort1.class,
                            1,
                            mb,
                            true,
                            new Class[] { int[].class },
                            new Object[] { new int[] { 1, 1, 5, 17, 39, 42, 56 } }
                    );
                    assertTrue(result.isPresent(), mb.build().toString());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution), mb.build().toString());
                    PathSolution pathSolution = result.get();
                    Solution s = pathSolution.getInitialSolution();
                    Object[] values = (Object[]) s.value;
                    assertEquals(1, values[0], mb.build().toString());
                    assertEquals(1, values[1], mb.build().toString());
                    assertEquals(5, values[2], mb.build().toString());
                    assertEquals(17, values[3], mb.build().toString());
                    assertEquals(39, values[4], mb.build().toString());
                    assertEquals(42, values[5], mb.build().toString());
                    assertEquals(56, values[6], mb.build().toString());
                    return result;
                }
        );
    }
}
