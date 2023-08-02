package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.tcg.TcgConfig;
import de.wwu.mulib.tcg.testsetreducer.CombinedTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleBackwardsTestSetReducer;
import de.wwu.mulib.tcg.testsetreducer.SimpleForwardsTestSetReducer;
import de.wwu.mulib.transform_and_execute.examples.mit_examples.SatHanoi01Transf;
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
        mb.setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(true, true, true)
                .setCONCOLIC(false)
                .setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(false)
                .setTRANSF_GENERATED_CLASSES_PATH(TestUtility.TEST_BUILD_PATH)
                .setSECONDS_PER_INVOCATION(3)
                .setFIXED_ACTUAL_CP_BUDGET(500000); // Dependent on time budget to terminate
        String result = Mulib.generateTestCases(
                "execForTcg",
                SatHanoi01Transf.class,
                mb,
                Mulib.getMethodFromClass(SatHanoi01Transf.class.getName(), "execForTcg", new Class[] { int.class }),
                TcgConfig.builder().setAssumeSetters(true).setTestSetReducer(new CombinedTestSetReducer(new SimpleForwardsTestSetReducer(), new SimpleBackwardsTestSetReducer()))
        );
        return;
    }
}
