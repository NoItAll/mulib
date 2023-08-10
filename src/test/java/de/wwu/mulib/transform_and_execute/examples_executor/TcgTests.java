package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.testsetreducer.CombinedTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleBackwardsTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleForwardsTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleGreedyTestSetReducer;
import de.wwu.mulib.transform_and_execute.examples.Sort;
import de.wwu.mulib.transform_and_execute.examples.TSP;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatHanoi01Transf;
import de.wwu.mulib.util.Utility;
import org.junit.jupiter.api.Test;

public class TcgTests {

    @Test
    public void testSatHanoi01Transf() {
        TestUtility.getAllSolutions(
                this::_testSatHanoi01,
                "exec"
        );
    }

    private void _testSatHanoi01(MulibConfig.MulibConfigBuilder mb) {
        mb.setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(false, true, false)
                .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH)
                .setSECONDS_PER_INVOCATION(2)
                .setFIXED_ACTUAL_CP_BUDGET(200);
        String result = Mulib.generateTestCases(
                "execForTcg",
                SatHanoi01Transf.class,
                mb,
                Utility.getMethodFromClass(SatHanoi01Transf.class.getName(), "execForTcg", new Class[] { int.class }),
                TcgConfig.builder()
                        .setAssumePublicZeroArgsConstructor(true)
                        .setAssumeSetters(true)
                        .setAssumeEqualsMethods(true)
                        .setTestSetReducer(new CombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer()))
        );
        return;
    }

    @Test
    public void testTSPDriver() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setFIXED_ACTUAL_CP_BUDGET(48)
                            .setCONCOLIC(false)
                            .setMAX_EXCEEDED_BUDGETS(150_000)
                            .setSECONDS_PER_INVOCATION(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH)
                            // TODO If we implement representing objects symbolically not using the custom procedure,
                            //  deactivate this.
                            .setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    String result = Mulib.generateTestCases(
                            "driver",
                            TSP.class,
                            mb,
                            Utility.getMethodFromClass(TSP.class, "solve"),
                            TcgConfig.builder()
                                    .setAssumePublicZeroArgsConstructor(true)
                                    .setAssumeSetters(false)
                                    .setAssumeEqualsMethods(false)
                                    .setTestSetReducer(new SimpleGreedyTestSetReducer())
                    );
                },
                "driver"
        );
    }

    @Test
    public void testSortDriver() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setFIXED_ACTUAL_CP_BUDGET(50)
                            .setCONCOLIC(false)
                            .setMAX_EXCEEDED_BUDGETS(150_000)
                            .setSECONDS_PER_INVOCATION(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH);
                    String result = Mulib.generateTestCases(
                            "sort",
                            Sort.class,
                            mb,
                            Utility.getMethodFromClass(Sort.class, "sort", int[].class),
                            TcgConfig.builder()
                                    .setAssumePublicZeroArgsConstructor(false)
                                    .setAssumeSetters(false)
                                    .setAssumeEqualsMethods(false)
                                    .setTestSetReducer(new SimpleGreedyTestSetReducer())
                    );
                },
                "driver"
        );
    }

    @Test
    public void testSortDriverWithEarlyTermination() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setFIXED_ACTUAL_CP_BUDGET(60)
                            .setCONCOLIC(false)
                            .setMAX_EXCEEDED_BUDGETS(150_000)
                            .setSECONDS_PER_INVOCATION(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, true, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH);
                    String result = Mulib.generateTestCases(
                            "sort",
                            Sort.class,
                            mb,
                            Utility.getMethodFromClass(Sort.class, "sort", int[].class),
                            TcgConfig.builder()
                                    .setAssumePublicZeroArgsConstructor(false)
                                    .setAssumeSetters(false)
                                    .setAssumeEqualsMethods(true)
                                    .setTestSetReducer(new SimpleGreedyTestSetReducer())
                    );
                },
                "driver"
        );
    }
}
