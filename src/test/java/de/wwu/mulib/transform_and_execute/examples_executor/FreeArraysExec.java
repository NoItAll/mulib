package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.CapacityAssignmentProblem;
import de.wwu.mulib.transform_and_execute.examples.free_arrays.SimpleSort0;
import de.wwu.mulib.transform_and_execute.examples.free_arrays.SimpleSort1;
import org.junit.jupiter.api.Test;

import java.util.List;
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
                            mb,
                            true,
                            new Class[] { int[].class },
                            new Object[] { new int[] { -81, 42, 9, 78, 0, 1, 8 } }
                    );
                    assertTrue(result.isPresent());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    PathSolution pathSolution = result.get();
                    Solution s = pathSolution.getSolution();
                    Object[] values = (Object[]) s.returnValue;
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
                            mb,
                            true,
                            new Class[] { int[].class },
                            new Object[] { new int[] { 1, 1, 5, 17, 39, 42, 56 } }
                    );
                    assertTrue(result.isPresent(), mb.build().toString());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution), mb.build().toString());
                    PathSolution pathSolution = result.get();
                    Solution s = pathSolution.getSolution();
                    Object[] values = (Object[]) s.returnValue;
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

    @Test
    public void testPrimitiveEncodingCapacityAssignment() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    List<Solution> result = TestUtility.getUpToNSolutions(
                            3,
                            "assign",
                            CapacityAssignmentProblem.class,
                            mb,
                            new Class[] { int[].class, int[].class },
                            new Object[] { new int[] { 5, 3, 2 }, new int[] { 1, 2, 4, 3 } }
                    );
                    assertEquals(1, result.size());
                    Solution singleSolution = result.get(0);
                    int workload0AssignedTo = (Integer) singleSolution.labels.getLabelForId("workload_0");
                    int workload1AssignedTo = (Integer) singleSolution.labels.getLabelForId("workload_1");
                    int workload2AssignedTo = (Integer) singleSolution.labels.getLabelForId("workload_2");
                    int workload3AssignedTo = (Integer) singleSolution.labels.getLabelForId("workload_3");
                    assertEquals(0, workload0AssignedTo);
                    assertEquals(0, workload2AssignedTo);
                    assertEquals(2, workload1AssignedTo);
                    assertEquals(1, workload3AssignedTo);
                    Object[] returnValue = (Object[]) singleSolution.returnValue;
                    assertEquals(0, returnValue[0]);
                    assertEquals(2, returnValue[1]);
                    assertEquals(0, returnValue[2]);
                    assertEquals(1, returnValue[3]);
                    return result;
                },
                "CapacityAssignmentProblem.assign"
        );
    }

    @Test
    public void testArrayArrayEncodingCapacityAssignment() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    List<Solution> result = TestUtility.getUpToNSolutions(
                            1,
                            "assign",
                            CapacityAssignmentProblem.class,
                            mb,
                            new Class[] { int[].class, int[][].class },
                            new Object[] { new int[] { 5, 3, 2 }, new int[][] { { 1, 2, 4, 3 }, { 5, 2, 3 } } }
                    );
                    assertEquals(1, result.size());
                    Solution singleSolution = result.get(0);
                    Object[] returnValue = (Object[]) singleSolution.returnValue;
                    assertEquals(2, returnValue.length);
                    Object[] innerReturnValue = (Object[]) returnValue[0];
                    assertEquals(4, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    assertEquals(2, innerReturnValue[1]);
                    assertEquals(0, innerReturnValue[2]);
                    assertEquals(1, innerReturnValue[3]);
                    innerReturnValue = (Object[]) returnValue[1];
                    assertEquals(3, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    assertEquals(2, innerReturnValue[1]);
                    assertEquals(1, innerReturnValue[2]);
                    return result;
                },
                "CapacityAssignmentProblem.assign"
        );
    }

    @Test
    public void testArrayArrayEncodingCapacityAssignmentWithPreProduction() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true);
                    List<Solution> result = TestUtility.getUpToNSolutions(
                            1,
                            "assignWithPreproduction",
                            CapacityAssignmentProblem.class,
                            mb,
                            new Class[] { int[].class, int[][].class },
                            new Object[] { new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } } }
                    );
                    assertEquals(1, result.size(), "Result size is: " + result.size() + ", with config: " + mb.build());
                    return result;
                },
                "CapacityAssignmentProblem.assignWithPreproduction"
        );
    }

    @Test
    public void testArrayArrayEncodingCapacityAssignmentWithPreProductionMultipleSolutions() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true);
                    List<Solution> result = TestUtility.getUpToNSolutions(
                            200,
                            "assignWithPreproduction",
                            CapacityAssignmentProblem.class,
                            mb,
                            new Class[] { int[].class, int[][].class },
                            new Object[] { new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } } }
                    );
                    assertEquals(3, result.size());
                    return result;
                },
                "CapacityAssignmentProblem.assignWithPreproduction"
        );
    }
}
