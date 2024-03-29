package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.transform_and_execute.examples.*;
import de.wwu.mulib.transform_and_execute.examples.apache2_examples.*;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.InfiniteLoop;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExExec {

    @Test
    public void testExSymExeI2F_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExeI2F_trueExec,
                "testExSymExeI2F_trueExec"
        );
    }

    private List<PathSolution> _testExSymExeI2F_trueExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                ExSymExeI2F_true.class,
                mb,
                true
        );
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void testExSymExeLongBytecodes_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExeLongBytecodes_trueExec,
                "testExSymExeLongBytecodes_trueExec"
        );
    }

    private List<PathSolution> _testExSymExeLongBytecodes_trueExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                ExSymExeLongBytecodes_true.class,
                mb,
                true
        );
        assertEquals(3, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void testExSymExeResearch_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExeResearch_trueExec,
                "testExSymExeResearch_trueExec"
        );
    }

    private List<PathSolution> _testExSymExeResearch_trueExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                ExSymExeResearch_true.class,
                mb,
                true
        );
        assertEquals(4, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void testExSymExe15_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExe15_trueExec,
                "testExSymExe15_trueExec"
        );
    }

    private static final Object syncObject = new Object(); // TODO As long as static is not properly handled, we need to enfore this
    private List<PathSolution> _testExSymExe15_trueExec(MulibConfig.MulibConfigBuilder mb) {
        synchronized (syncObject) {
            List<PathSolution> result = TestUtility.executeMulib(
                    "exec",
                    ExSymExe15_true.class,
                    mb,
                    true
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
            return result;
        }
    }

    @Test
    public void testExSymExe14_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExe14_trueExec,
                "testExSymExe14_trueExec"
        );
    }

    private List<PathSolution> _testExSymExe14_trueExec(MulibConfig.MulibConfigBuilder mb) {
        synchronized (syncObject) {
            List<PathSolution> result = TestUtility.executeMulib(
                    "exec",
                    ExSymExe14_true.class,
                    mb,
                    true
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
            return result;
        }
    }

    @Test
    public void testExSymExe12_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExSymExe12_trueExec,
                "testExSymExe12_trueExec"
        );
    }

    private List<PathSolution> _testExSymExe12_trueExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                ExSymExe12_true.class,
                mb,
                true
        );
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void testExException_trueExec() {
        TestUtility.getAllSolutions(
                this::_testExException_trueExec,
                "testExException_true"
        );
    }

    private List<PathSolution> _testExException_trueExec(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "exec",
                ExException_true.class,
                mb,
                true
        );
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void testExSymExeLongBytecodes_falseExec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            ExSymExeLongBytecodes_false.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertEquals(1, result.stream().filter(ps -> ps instanceof ThrowablePathSolution).count());
                    assertEquals(1, result.stream().filter(ps -> !(ps instanceof ThrowablePathSolution)).count());
                },
                "testExSymExeLongBytecodes_falseExec"
        );
    }

    @Test
    public void testExSymExeResearch_falseExec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            ExSymExeResearch_false.class,
                            mb,
                            true
                    );

                    assertEquals(4, result.size());
                    assertEquals(2, result.stream().filter(ps -> ps instanceof ThrowablePathSolution).count());
                    assertEquals(2, result.stream().filter(ps -> !(ps instanceof ThrowablePathSolution)).count());
                },
                "testExSymExeResearch_falseExec"
        );
    }

    @Test
    public void testExSymExeF2L_falseExec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            ExSymExeF2L_false.class,
                            mb,
                            true
                    );
                    assertEquals(2, result.size());
                    assertEquals(1, result.stream().filter(ps -> ps instanceof ThrowablePathSolution).count());
                    assertEquals(1, result.stream().filter(ps -> !(ps instanceof ThrowablePathSolution)).count());
                },
                "testExSymExeF2L_falseExec"
        );
    }

    @Test
    public void testExSymExeLCMP_falseExec0() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec0",
                            ExSymExeLCMP_false.class,
                            mb,
                            true
                    );

                    assertEquals(1, result.size());
                    assertEquals(1, result.stream().filter(ps -> (ps instanceof ThrowablePathSolution)).count());
                },
                "testExSymExeLCMP_falseExec0"
        );
    }

    @Test
    public void testExSymExeLCMP_falseExec1() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec1",
                            ExSymExeLCMP_false.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size()); 
                    assertEquals(1, result.stream().filter(ps -> (ps instanceof ThrowablePathSolution)).count());
                    assertEquals(1, result.stream().filter(ps -> !(ps instanceof ThrowablePathSolution)).count());
                },
                "testExSymExeLCMP_falseExec1"
        );
    }

    @Test
    public void testBooleanIntChecksExec0() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec0",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec0"
        );
    }

    @Test
    public void testBooleanIntChecksExec1() {
        TestUtility.getAllSolutions(
                mb -> {

                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec1",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec1"
        );
    }

    @Test
    public void testBooleanIntChecksExec2() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec2",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec2"
        );
    }

    @Test
    public void testBooleanIntChecksExec3() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec3",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec3"
        );
    }

    @Test
    public void testBooleanIntChecksExec4() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec4",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec4"
        );
    }

    @Test
    public void testBooleanIntChecksExec5() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec5",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testBooleanIntChecksExec5"
        );
    }

    @Test
    public void testBooleanIntChecksExec6() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec6",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                    assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ThrowablePathSolution)));
                },
                "testBooleanIntChecksExec6"
        );
    }

    @Test
    public void testBooleanIntChecksExec7() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec7",
                            BooleanIntChecks.class,
                            mb,
                            true
                    );

                    assertEquals(2, result.size());
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                    assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ThrowablePathSolution)));
                },
                "testBooleanIntChecksExec7"
        );
    }

    @Test
    public void testWBSProp1Exec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            WBSProp1.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ThrowablePathSolution)));
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testWBSProp1Exec"
        );
    }

    @Test
    public void testWBSProp2Exec() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            WBSProp2.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testWBSProp2Exec"
        );
    }

    @Test
    public void testWBSProp3Exec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            WBSProp3.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ThrowablePathSolution)));
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testWBSProp3Exec"
        );
    }

    @Test
    public void testWBSProp4Exec() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec",
                            WBSProp4.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ThrowablePathSolution)));
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testWBSProp4Exec"
        );
    }

    @Test
    public void testInfiniteLoopExec0() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec0",
                            InfiniteLoop.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testInfiniteLoopExec0"
        );
    }

    @Test
    public void testSort() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setBUDGET_FIXED_ACTUAL_CP(12);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "sort",
                            Sort.class,
                            mb,
                            true
                    );
                    assertFalse(result.isEmpty());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                    Sort sort = new Sort();
                    for (PathSolution ps : result) {
                        Solution s = ps.getSolution();
                        int[] input = (int[]) s.labels.getLabelForId("arg1");
                        int[] output = (int[]) s.returnValue;
                        int[] sortedInput = sort.sort(input);
                        assertArrayEquals(output, sortedInput);
                    }
                },
                "sort"
        );
    }

    @Test
    public void testTSPDriver() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setBUDGET_FIXED_ACTUAL_CP(30)
                            // TODO If we implement representing objects symbolically not using the custom procedure,
                            //  deactivate this.
                            .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "execute",
                            TSP.class,
                            mb,
                            true
                    );
                    assertFalse(result.isEmpty());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
                    for (PathSolution ps : result) {
                        Solution s = ps.getSolution();
                        TSP input = (TSP) s.labels.getLabelForId("arg0");
                        int output = (Integer) s.returnValue;
                        int calculatedOutput = input.solve();
                        TSP inputAfterExec = (TSP) s.labels.getLabelForId("arg0AfterExec");
                        assertEquals(output, calculatedOutput);
                        assertEquals(input.nCalls, inputAfterExec.nCalls);
                        assertArrayEquals(input.getD(), inputAfterExec.getD());
                        assertEquals(input.getBest(), inputAfterExec.getBest());
                        assertArrayEquals(input.getVisited(), inputAfterExec.getVisited());
                        assertNotSame(input, inputAfterExec);
                    }
                },
                "driver"
        );
    }

    @Test @Disabled(value="Jump-back budget is not yet implemented") // TODO
    public void testInfiniteLoopExec1() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setSEARCH_ALLOW_EXCEPTIONS(true);
                    List<PathSolution> result = TestUtility.executeMulib(
                            "exec1",
                            InfiniteLoop.class,
                            mb,
                            true
                    );
                    assertTrue(result.stream().anyMatch(ps -> ps instanceof ThrowablePathSolution));
                },
                "testInfiniteLoopExec1"
        );
    }
}
