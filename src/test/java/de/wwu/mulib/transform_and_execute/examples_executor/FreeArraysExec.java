package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.MulibContext;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.CapacityAssignmentProblem;
import de.wwu.mulib.transform_and_execute.examples.free_arrays.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
                    int[] values = (int[]) s.returnValue;
                    assertEquals(-81, values[0]);
                    assertEquals(0, values[1]);
                    assertEquals(1, values[2]);
                    assertEquals(8, values[3]);
                    assertEquals(9, values[4]);
                    assertEquals(42, values[5]);
                    assertEquals(78, values[6]);
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
                    int[] values = (int[]) s.returnValue;
                    assertEquals(1, values[0], mb.build().toString());
                    assertEquals(1, values[1], mb.build().toString());
                    assertEquals(5, values[2], mb.build().toString());
                    assertEquals(17, values[3], mb.build().toString());
                    assertEquals(39, values[4], mb.build().toString());
                    assertEquals(42, values[5], mb.build().toString());
                    assertEquals(56, values[6], mb.build().toString());
                    
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
                    int[] returnValue = (int[]) singleSolution.returnValue;
                    assertEquals(0, returnValue[0]);
                    assertEquals(2, returnValue[1]);
                    assertEquals(0, returnValue[2]);
                    assertEquals(1, returnValue[3]);
                    
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
                    int[][] returnValue = (int[][]) singleSolution.returnValue;
                    assertEquals(2, returnValue.length);
                    int[] innerReturnValue = returnValue[0];
                    assertEquals(4, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    assertEquals(2, innerReturnValue[1]);
                    assertEquals(0, innerReturnValue[2]);
                    assertEquals(1, innerReturnValue[3]);
                    innerReturnValue = returnValue[1];
                    assertEquals(3, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    assertEquals(2, innerReturnValue[1]);
                    assertEquals(1, innerReturnValue[2]);
                    
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
                    
                },
                "CapacityAssignmentProblem.assignWithPreproduction"
        );
    }

    @Test
    public void testArrayArrayEncodingCapacityAssignmentWithPreProductionMultipleSolutions() {
        TestUtility.getAllSolutions(
                (mb) -> withPreproduction(true, mb),
                "CapacityAssignmentProblem.assignWithPreproduction"
        );
    }

    @Test
    public void testArrayArrayEncodingCapacityAssignmentWithPreProductionMultipleSolutionsAndNonEagerIndices() {
        TestUtility.getAllSolutions(
                (mb) -> withPreproduction(false, mb),
                "CapacityAssignmentProblem.assignWithPreproduction"
        );
    }

    private static void withPreproduction(boolean eagerArrayIndices, MulibConfig.MulibConfigBuilder mb) {
        mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
        mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(eagerArrayIndices);
        List<Solution> result;
        result = TestUtility.getUpToNSolutions(
                200,
                "assignWithPreproduction",
                CapacityAssignmentProblem.class,
                mb,
                new Class[] { int[].class, int[][].class },
                new Object[] { new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } } }
        );
        assertEquals(3, result.size());

        MulibContext mc = Mulib.getMulibContext(
                CapacityAssignmentProblem.class,
                "assignWithPreproduction",
                mb,
                int[].class,
                int[][].class
        );
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } });
        assertTrue(result.isEmpty());
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1, 1 }, { 1, 5, 2, 3 } });
        assertTrue(result.isEmpty());
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 1, 4, 3, 1, 1 }, { 5, 2, 4 } });
        assertTrue(result.isEmpty());
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 5, 2, 3 }, {} });
        assertEquals(1, result.size());
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 3, 3, 2 }, { 3, 3, 3 } });
        assertTrue(result.isEmpty());
        result =
                mc.getUpToNSolutions(200, new int[] { 5, 3, 2 }, new int[][] { { 4 }, { 5, 3, 2 } });
        if (eagerArrayIndices) {
            assertEquals(6, result.size());
        } else {
            // Since we have no other named variables, the size of solutions might differ. Here, eager indices returns
            // several copies of the valid solutions { [[0], [0,1,2]], [[0], [0,1,1]] }
            assertEquals(2, result.size());
        }
    }

    @Test
    public void testFreeArraysOfObjects() {
        TestUtility.getAllSolutions(
                (mb) -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> result = TestUtility.getUpToNSolutions(
                            5,
                            "assignMaterials",
                            FreeArraysOfObjects.class,
                            mb,
                            new Class[] { FreeArraysOfObjects.Truck[].class, FreeArraysOfObjects.Material[].class },
                            new Object[] {
                                    new FreeArraysOfObjects.Truck[] {
                                            new FreeArraysOfObjects.Truck(5),
                                            new FreeArraysOfObjects.Truck(3),
                                            new FreeArraysOfObjects.Truck(2) },
                                    new FreeArraysOfObjects.Material[] {
                                            new FreeArraysOfObjects.Material(1, 4), // exchanged Material 0 and 1 for test case order (see below)
                                            new FreeArraysOfObjects.Material(0, 1),
                                            new FreeArraysOfObjects.Material(2, 3),
                                            new FreeArraysOfObjects.Material(3, 1) }
                            }
                    );
                    assertEquals(3, result.size());
                    // All three solutions must be sound
                    boolean seenFirst = false;
                    boolean seenSecond = false;
                    boolean seenThird = false;
                    for (Solution s : result) {
                        boolean seenFirstTruck = false;
                        boolean seenSecondTruck = false;
                        boolean seenThirdTruck = false;
                        FreeArraysOfObjects.Truck[] trucks = (FreeArraysOfObjects.Truck[]) s.returnValue;
                        byte currentSolution = -1;
                        for (FreeArraysOfObjects.Truck t : trucks) {
                            if (t.capacity == 5) {
                                seenFirstTruck = true;
                                assertTrue(t.loadedWeight == 4 || t.loadedWeight == 5);
                                // Implications:
                                assertTrue(t.loadedWeight != 4 || t.loadedMaterials.length == 1);
                                assertTrue(t.loadedWeight != 5 || t.loadedMaterials.length == 2);
                                assertEquals(1, t.loadedMaterials[0].number);
                                assertEquals(4, t.loadedMaterials[0].weight);
                                if (t.loadedWeight == 5) {
                                    // Two solutions
                                    if (t.loadedMaterials[1].number == 0) {
                                        assertEquals(1, t.loadedMaterials[1].weight);
                                        assertEquals(-1, currentSolution);
                                        currentSolution = 1;
                                        seenFirst = true;
                                    } else {
                                        assertEquals(3, t.loadedMaterials[1].number);
                                        assertEquals(1, t.loadedMaterials[1].weight);
                                        assertEquals(-1, currentSolution);
                                        currentSolution = 2;
                                        seenSecond = true;
                                    }
                                } else {
                                    assertEquals(4, t.loadedWeight);
                                    assertEquals(-1, currentSolution);
                                    currentSolution = 3;
                                    seenThird = true;
                                }
                            } else if (t.capacity == 3) {
                                seenSecondTruck = true;
                                assertEquals(3, t.loadedWeight);
                                assertEquals(1, t.loadedMaterials.length);
                                assertEquals(3, t.loadedMaterials[0].weight);
                                assertEquals(2, t.loadedMaterials[0].number);
                            } else if (t.capacity == 2) {
                                seenThirdTruck = true;
                                assertTrue(t.loadedWeight == 1 || t.loadedWeight == 2);
                                if (t.loadedWeight == 1) {
                                    assertEquals(1, t.loadedMaterials.length);
                                    assertEquals(1, t.loadedMaterials[0].weight);
                                    if (t.loadedMaterials[0].number == 0) {
                                        assertEquals(2, currentSolution);
                                    } else {
                                        assertEquals(3, t.loadedMaterials[0].number);
                                        assertEquals(1, currentSolution);
                                    }
                                } else {
                                    assertEquals(2, t.loadedMaterials.length);
                                    assertEquals(3, currentSolution);
                                    assertEquals(0, t.loadedMaterials[0].number);
                                    assertEquals(1, t.loadedMaterials[0].weight);
                                    assertEquals(3, t.loadedMaterials[1].number);
                                    assertEquals(1, t.loadedMaterials[1].weight);
                                }
                            } else {
                                fail();
                            }
                        }
                        assertTrue(currentSolution > 0);
                        assertTrue(seenFirstTruck && seenSecondTruck && seenThirdTruck);
                    }
                    assertTrue(seenFirst && seenSecond && seenThird);
                    
                },
                "FreeArraysOfObjects.assignMaterials"
        );
    }

    @Test
    public void testMachineContainerEncoding() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false);
                    MachineCAP.Machine[] machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    List<PathSolution> ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(1, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[] result = (int[]) p.getSolution().returnValue;
                        for (int i = 0; i < result.length; i++) {
                            machines[result[i]].i = machines[result[i]].i - workloads[i];
                            if (machines[result[i]].i < 0) {
                                fail();
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1 };
                    ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    List<Solution> sols = Mulib.getMulibContext(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(5, machines, workloads);
                    assertEquals(5, sols.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(7), new MachineCAP.Machine(8)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 5, 7, 8 };
                    sols = Mulib.getMulibContext(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(10, machines, workloads);
                    assertEquals(2, sols.size());

                    // NOW FOR EAGER
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true).setMAX_PATH_SOLUTIONS(10);
                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertTrue(ps.size() >= 10);
                    for (PathSolution p : ps) {
                        MachineCAP.Machine[] _machines = MachineCAP.copy(machines);
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[] result = (int[]) p.getSolution().returnValue;
                        for (int i = 0; i < result.length; i++) {
                            _machines[result[i]].i = _machines[result[i]].i - workloads[i];
                            if (_machines[result[i]].i < 0) {
                                fail();
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1 };
                    ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    sols = Mulib.getMulibContext(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(5, machines, workloads);
                    assertEquals(5, sols.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(7), new MachineCAP.Machine(8)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 5, 7, 8 };
                    sols = Mulib.getMulibContext(
                            "assign",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(10, machines, workloads);
                    assertEquals(2, sols.size());
                },
                "assign"
        );
    }

    @Test
    public void testMachineContainerEncodingMutateField() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false);
                    MachineCAP.Machine[] machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    List<PathSolution> ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(1, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[] result = (int[]) p.getSolution().returnValue;
                        for (int i = 0; i < result.length; i++) {
                            machines[result[i]].i = machines[result[i]].i - workloads[i];
                            if (machines[result[i]].i < 0) {
                                fail();
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1 };
                    ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    List<Solution> sols = Mulib.getMulibContext(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(5, machines, workloads);
                    assertEquals(5, sols.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(7), new MachineCAP.Machine(8)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 5, 7, 8 };
                    sols = Mulib.getMulibContext(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(10, machines, workloads);
                    assertEquals(2, sols.size());

                    // NOW FOR EAGER
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true).setMAX_PATH_SOLUTIONS(10);
                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertTrue(ps.size() >= 10);
                    for (PathSolution p : ps) {
                        MachineCAP.Machine[] _machines = MachineCAP.copy(machines);
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[] result = (int[]) p.getSolution().returnValue;
                        for (int i = 0; i < result.length; i++) {
                            _machines[result[i]].i = _machines[result[i]].i - workloads[i];
                            if (_machines[result[i]].i < 0) {
                                fail();
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1 };
                    ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    ps = Mulib.executeMulib(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3 };
                    sols = Mulib.getMulibContext(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(5, machines, workloads);
                    assertEquals(5, sols.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2),
                            new MachineCAP.Machine(5), new MachineCAP.Machine(7), new MachineCAP.Machine(8)
                    };
                    workloads = new int[] { 1, 2, 4, 3, 5, 7, 8 };
                    sols = Mulib.getMulibContext(
                            "assignMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    ).getUpToNSolutions(10, machines, workloads);
                    assertEquals(2, sols.size());
                },
                "assignMutateFieldValue"
        );
    }

    @Test
    public void testMultiPeriodMachineContainerEncoding() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false);
                    MachineCAP.Machine[] machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    int[][] workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    List<PathSolution> ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(1, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[][][] result = (int[][][]) p.getSolution().returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    MulibContext mc = Mulib.getMulibContext("assignWithPreproduction", MachineCAP.class, mb, machines, workloads);

                    List<Solution> sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(3, sols.size());

                    for (Solution p : sols) {
                        int[][][] result = (int[][][]) p.returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(0, sols.size());

                    // NOW EAGER
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true);
                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(3, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[][][] result = (int[][][]) p.getSolution().returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproduction",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    mc = Mulib.getMulibContext("assignWithPreproduction", MachineCAP.class, mb, machines, workloads);

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(3, sols.size());

                    for (Solution p : sols) {
                        int[][][] result = (int[][][]) p.returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(0, sols.size());
                },
                "assignWithPreproduction"
        );
    }

    @Test
    public void testMultiPeriodMachineContainerEncodingMutateField() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false);
                    MachineCAP.Machine[] machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    int[][] workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    List<PathSolution> ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(1, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[][][] result = (int[][][]) p.getSolution().returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    MulibContext mc = Mulib.getMulibContext("assignWithPreproductionMutateFieldValue", MachineCAP.class, mb, machines, workloads);

                    List<Solution> sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(3, sols.size());

                    for (Solution p : sols) {
                        int[][][] result = (int[][][]) p.returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(0, sols.size());

                    // NOW EAGER
                    mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true);
                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );

                    assertEquals(3, ps.size());
                    for (PathSolution p : ps) {
                        assertFalse(p instanceof ExceptionPathSolution);
                        int[][][] result = (int[][][]) p.getSolution().returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(1)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    ps = Mulib.executeMulib(
                            "assignWithPreproductionMutateFieldValue",
                            MachineCAP.class,
                            mb,
                            machines,
                            workloads
                    );
                    assertEquals(0, ps.size());

                    machines = new MachineCAP.Machine[] {
                            new MachineCAP.Machine(5), new MachineCAP.Machine(3), new MachineCAP.Machine(2)
                    };
                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3 } };
                    mc = Mulib.getMulibContext("assignWithPreproductionMutateFieldValue", MachineCAP.class, mb, machines, workloads);

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(3, sols.size());

                    for (Solution p : sols) {
                        int[][][] result = (int[][][]) p.returnValue;
                        // Generate array of machines for both periods;
                        MachineCAP.Machine[][] machinesForTwoPeriods = new MachineCAP.Machine[workloads.length][];
                        for (int i = 0; i < workloads.length; i++) {
                            machinesForTwoPeriods[i] = new MachineCAP.Machine[machines.length];
                            for (int j = 0; j < machines.length; j++) {
                                machinesForTwoPeriods[i][j] = new MachineCAP.Machine(machines[j].i);
                            }
                        }
                        for (int i = 0; i < workloads.length; i++) {
                            for (int j = 0; j < workloads[i].length; j++) {
                                int workload = workloads[i][j];
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                                assertFalse(machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0);
                            }
                        }
                    }

                    workloads = new int[][] { { 1, 4, 3, 1 }, { 1, 5, 2, 3, 1 } };

                    sols = mc.getUpToNSolutions(10, machines, workloads);
                    assertEquals(0, sols.size());
                },
                "assignWithPreproductionMutateFieldValue"
        );
    }
    
    @Test
    public void testDlspVariant() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false);
                    DlspVariant.Machine[] machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 1, 3, 5 }),
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };
                    DlspVariant.Product[][] products = new DlspVariant.Product[][]{
                            {new DlspVariant.Product(1, 1), new DlspVariant.Product(1, 2), new DlspVariant.Product(4, 4), new DlspVariant.Product(3, 2), new DlspVariant.Product(1, 5),
                                    new DlspVariant.Product(4, 9), new DlspVariant.Product(1, 3), new DlspVariant.Product(2, 5), new DlspVariant.Product(1, 4),},
                            {new DlspVariant.Product(4, 4), new DlspVariant.Product(4, 1), new DlspVariant.Product(2, 6), new DlspVariant.Product(5, 2), new DlspVariant.Product(2, 4),
                                    new DlspVariant.Product(4, 5), new DlspVariant.Product(3, 2), new DlspVariant.Product(4, 3), new DlspVariant.Product(4, 10)},
                    };
                    List<PathSolution> ps = Mulib.executeMulib(
                            "assign",
                            DlspVariant.class,
                            mb,
                            machines,
                            products
                    );
                    assertEquals(1, ps.size());

                    DlspVariant.Product[][] result = (DlspVariant.Product[][]) ps.get(0).getSolution().returnValue;
                    for (DlspVariant.Product[] prodsForPeriod : result) {
                        boolean[] seen = new boolean[prodsForPeriod.length + 1];
                        for (DlspVariant.Product p : prodsForPeriod) {
                            DlspVariant.Machine prod = p.producedBy;
                            assertNotEquals(0, prod.number);
                            assertFalse(prod.productionCapacity < p.requiredCapacity);
                            assertFalse(Arrays.stream(prod.producibleProductTypes).noneMatch(type -> type == p.type));
                            assertFalse(seen[prod.number]);
                            seen[prod.number] = true;
                        }
                        for (int j = 1; j < seen.length; j++) {
                            assertTrue(seen[j]);
                        }
                    }

                    machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 3, 5 }), // Removed '1'
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };
                    products = new DlspVariant.Product[][]{
                            {new DlspVariant.Product(1, 1), new DlspVariant.Product(1, 2), new DlspVariant.Product(4, 4), new DlspVariant.Product(3, 2), new DlspVariant.Product(1, 5),
                                    new DlspVariant.Product(4, 9), new DlspVariant.Product(1, 3), new DlspVariant.Product(2, 5), new DlspVariant.Product(1, 4),},
                            {new DlspVariant.Product(4, 4), new DlspVariant.Product(4, 1), new DlspVariant.Product(2, 6), new DlspVariant.Product(5, 2), new DlspVariant.Product(2, 4),
                                    new DlspVariant.Product(4, 5), new DlspVariant.Product(3, 2), new DlspVariant.Product(4, 3), new DlspVariant.Product(4, 10)},
                    };
                    ps = Mulib.executeMulib(
                            "assign",
                            DlspVariant.class,
                            mb,
                            machines,
                            products
                    );
                    assertEquals(0, ps.size());

                    MulibContext mc = Mulib.getMulibContext("assign", DlspVariant.class, mb, machines, products);

                    List<Solution> sols = mc.getUpToNSolutions(10, machines, products);
                    assertEquals(0, sols.size());

                    machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 1, 3, 5 }), // Re-added '1'
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };

                    sols = mc.getUpToNSolutions(10, machines, products);
                    assertEquals(1, sols.size());

                    // NOW EAGER
                    machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 1, 3, 5 }),
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };
                    products = new DlspVariant.Product[][]{
                            {new DlspVariant.Product(1, 1), new DlspVariant.Product(1, 2), new DlspVariant.Product(4, 4), new DlspVariant.Product(3, 2), new DlspVariant.Product(1, 5),
                                    new DlspVariant.Product(4, 9), new DlspVariant.Product(1, 3), new DlspVariant.Product(2, 5), new DlspVariant.Product(1, 4),},
                            {new DlspVariant.Product(4, 4), new DlspVariant.Product(4, 1), new DlspVariant.Product(2, 6), new DlspVariant.Product(5, 2), new DlspVariant.Product(2, 4),
                                    new DlspVariant.Product(4, 5), new DlspVariant.Product(3, 2), new DlspVariant.Product(4, 3), new DlspVariant.Product(4, 10)},
                    };
                    ps = Mulib.executeMulib(
                            "assign",
                            DlspVariant.class,
                            mb,
                            machines,
                            products
                    );
                    assertEquals(1, ps.size());

                    result = (DlspVariant.Product[][]) ps.get(0).getSolution().returnValue;
                    for (DlspVariant.Product[] prodsForPeriod : result) {
                        boolean[] seen = new boolean[prodsForPeriod.length + 1];
                        for (DlspVariant.Product p : prodsForPeriod) {
                            DlspVariant.Machine prod = p.producedBy;
                            assertNotEquals(0, prod.number);
                            assertFalse(prod.productionCapacity < p.requiredCapacity);
                            assertFalse(Arrays.stream(prod.producibleProductTypes).noneMatch(type -> type == p.type));
                            assertFalse(seen[prod.number]);
                            seen[prod.number] = true;
                        }
                        for (int j = 1; j < seen.length; j++) {
                            assertTrue(seen[j]);
                        }
                    }

                    machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 3, 5 }), // Removed '1'
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };
                    products = new DlspVariant.Product[][]{
                            {new DlspVariant.Product(1, 1), new DlspVariant.Product(1, 2), new DlspVariant.Product(4, 4), new DlspVariant.Product(3, 2), new DlspVariant.Product(1, 5),
                                    new DlspVariant.Product(4, 9), new DlspVariant.Product(1, 3), new DlspVariant.Product(2, 5), new DlspVariant.Product(1, 4),},
                            {new DlspVariant.Product(4, 4), new DlspVariant.Product(4, 1), new DlspVariant.Product(2, 6), new DlspVariant.Product(5, 2), new DlspVariant.Product(2, 4),
                                    new DlspVariant.Product(4, 5), new DlspVariant.Product(3, 2), new DlspVariant.Product(4, 3), new DlspVariant.Product(4, 10)},
                    };
                    ps = Mulib.executeMulib(
                            "assign",
                            DlspVariant.class,
                            mb,
                            machines,
                            products
                    );
                    assertEquals(0, ps.size());

                    mc = Mulib.getMulibContext("assign", DlspVariant.class, mb, machines, products);

                    sols = mc.getUpToNSolutions(10, machines, products);
                    assertEquals(0, sols.size());

                    machines = new DlspVariant.Machine[] {
                            new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                            new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                            new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                            new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(5, 2, new int[] { 1, 3, 5 }), // Re-added '1'
                            new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                            new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                            new DlspVariant.Machine(8, 2, new int[] { 3 }),
                            new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
                    };

                    sols = mc.getUpToNSolutions(10, machines, products);
                    assertEquals(1, sols.size());
                },
                "assign"
        );
    }

}
