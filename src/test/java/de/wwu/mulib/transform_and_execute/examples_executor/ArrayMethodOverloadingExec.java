package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.ArrayMethodOverloading;
import de.wwu.mulib.transform_and_execute.examples.GraphEdge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayMethodOverloadingExec {

    @Test
    public void testArrayListScenarioCheck() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setTRANSF_REGARD_SPECIAL_CASE(List.of(ArrayMethodOverloading.class, GraphEdge.class));
                    List<PathSolution> result0 = TestUtility.executeMulib(
                            "check",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ int[][].class },
                            new Object[] { new int[][] { new int[]{ 1 } } }
                    );
                    assertEquals(1, result0.size());
                    assertEquals(1, result0.get(0).getSolution().returnValue);
                    List<PathSolution> result1 = TestUtility.executeMulib(
                            "check",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ double[][].class },
                            new Object[] { new double[][] { new double[]{ 1, 2 } } }
                    );
                    assertEquals(1, result1.size());
                    assertEquals(2, result1.get(0).getSolution().returnValue);
                    List<PathSolution> result2 = TestUtility.executeMulib(
                            "check",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ GraphEdge[].class },
                            new Object[] { new GraphEdge[] { new GraphEdge(3, 4) } }
                    );
                    assertEquals(1, result2.size());
                    assertEquals(3, ((GraphEdge) result2.get(0).getSolution().returnValue).getStart());
                    assertEquals(4, ((GraphEdge) result2.get(0).getSolution().returnValue).getEnd());
                    List<PathSolution> result3 = TestUtility.executeMulib(
                            "check",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ GraphEdge[][].class },
                            new Object[] { new GraphEdge[][] { null, new GraphEdge[]{ new GraphEdge(5, 6), new GraphEdge(7, 8), new GraphEdge(9, 10) } } }
                    );
                    assertEquals(1, result3.size());
                    assertEquals(7, ((GraphEdge) result3.get(0).getSolution().returnValue).getStart());
                    assertEquals(8, ((GraphEdge) result3.get(0).getSolution().returnValue).getEnd());


                    List<PathSolution> result4 = TestUtility.executeMulib(
                            "check1",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ GraphEdge[][].class },
                            new Object[] { new GraphEdge[][] { null, new GraphEdge[]{ new GraphEdge(9, 10), null } } }
                    );
                    assertEquals(1, result4.size());
                    assertTrue(result4.get(0).getSolution().returnValue instanceof GraphEdge[][][]);
                    {
                        GraphEdge[][][] result = (GraphEdge[][][]) result4.get(0).getSolution().returnValue;
                        assertEquals(1, result.length);
                        assertNull(result[0][0]);
                        assertEquals(9, result[0][1][0].getStart());
                        assertEquals(10, result[0][1][0].getEnd());
                        assertNull(result[0][1][1]);
                    }

                    List<PathSolution> result5 = TestUtility.executeMulib(
                            "check1",
                            ArrayMethodOverloading.class,
                            mb,
                            true,
                            new Class[]{ GraphEdge[][][].class },
                            new Object[] { new GraphEdge[][][] { new GraphEdge[][] {}, null, new GraphEdge[][] { null, new GraphEdge[]{ new GraphEdge(5, 6), new GraphEdge(7, 8), new GraphEdge(9, 10) } } } }
                    );
                    assertEquals(1, result5.size());
                    {
                        GraphEdge[][] result = (GraphEdge[][]) result5.get(0).getSolution().returnValue;
                        assertNull(result[0]);
                        assertEquals(5, result[1][0].getStart());
                        assertEquals(6, result[1][0].getEnd());

                        assertEquals(7, result[1][1].getStart());
                        assertEquals(8, result[1][1].getEnd());

                        assertEquals(9, result[1][2].getStart());
                        assertEquals(10, result[1][2].getEnd());
                    }
                },
                "ArrayMethodOverloading.check"
        );
    }
}
