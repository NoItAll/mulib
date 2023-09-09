package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.testsetreducer.*;
import de.wwu.mulib.transform_and_execute.examples.Sort;
import de.wwu.mulib.transform_and_execute.examples.TSP;
import de.wwu.mulib.transform_and_execute.examples.apache2_examples.WBSTransf;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatHanoi01Transf;
import de.wwu.mulib.util.Utility;
import org.junit.jupiter.api.Test;

public class TcgTests {

    @Test
    public void testWbsTransf() {
        TestUtility.getAllSolutions(mb -> {
            mb.setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false).setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false).setBUDGET_GLOBAL_TIME_IN_SECONDS(3).setBUDGET_FIXED_ACTUAL_CP(64);
            String result = Mulib.generateTestCases(WBSTransf.class, "driver", mb, Utility.getMethodFromClass(WBSTransf.class, "update", new Class[] {int.class, boolean.class, boolean.class}),
                    TcgConfig.builder()
                            .setTestSetReducer(new CompetingTestSetReducer(
                                    new SequentialCombinedTestSetReducer(new SimpleGreedyTestSetReducer(), new SimpleBackwardsTestSetReducer()),
                                    new SequentialCombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer())
                            )));
            return;
        }, "wbs");
    }

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
                .setBUDGET_GLOBAL_TIME_IN_SECONDS(2)
                .setBUDGET_FIXED_ACTUAL_CP(200);
        String result = Mulib.generateTestCases(
                SatHanoi01Transf.class,
                "execForTcg",
                mb,
                Utility.getMethodFromClass(SatHanoi01Transf.class, "execForTcg", new Class[] { int.class }),
                TcgConfig.builder()
                        .setAssumePublicZeroArgsConstructor(true)
                        .setAssumeSetters(true)
                        .setAssumeEqualsMethods(true)
                        .setTestSetReducer(new SequentialCombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer()))
        );
        return;
    }

    @Test
    public void testTSPDriver() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setBUDGET_FIXED_ACTUAL_CP(48)
                            .setSEARCH_CONCOLIC(false)
                            .setBUDGET_MAX_EXCEEDED(150_000)
                            .setBUDGET_GLOBAL_TIME_IN_SECONDS(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH)
                            // TODO If we implement representing objects symbolically not using the custom procedure,
                            //  deactivate this.
                            .setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(true);
                    String result = Mulib.generateTestCases(
                            TSP.class,
                            "driver",
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
                    mb.setBUDGET_FIXED_ACTUAL_CP(50)
                            .setSEARCH_CONCOLIC(false)
                            .setBUDGET_MAX_EXCEEDED(150_000)
                            .setBUDGET_GLOBAL_TIME_IN_SECONDS(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, false, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH);
                    String result = Mulib.generateTestCases(
                            Sort.class,
                            "sort",
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
                    mb.setBUDGET_FIXED_ACTUAL_CP(60)
                            .setSEARCH_CONCOLIC(false)
                            .setBUDGET_MAX_EXCEEDED(150_000)
                            .setBUDGET_GLOBAL_TIME_IN_SECONDS(3)
                            .setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, true, false)
                            .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                            .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH);
                    String result = Mulib.generateTestCases(
                            Sort.class,
                            "sort",
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
