package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.SwitchExamples;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwitchExamplesExec {

    @Test
    public void testIntSwitch() {
        TestUtility.getAllSolutions((mcb) -> {
            mcb.setTRANSF_WRITE_TO_FILE(true);
            List<PathSolution> ps = TestUtility.executeMulib("intSwitch", SwitchExamples.class, mcb, true);
            assertEquals(4, ps.size());
        }, "intSwitch");
    }

    @Test
    public void testIntSwitch2() {
        TestUtility.getAllSolutions((mcb) -> {
            mcb.setTRANSF_WRITE_TO_FILE(true);
            List<PathSolution> ps = TestUtility.executeMulib("intSwitch2", SwitchExamples.class, mcb, true);
            assertEquals(4, ps.size());
        }, "intSwitch2");
    }

    @Test
    public void testIntSwitch3() {
        TestUtility.getAllSolutions((mcb) -> {
            mcb.setTRANSF_WRITE_TO_FILE(true);
            List<PathSolution> ps = TestUtility.executeMulib("intSwitch3", SwitchExamples.class, mcb, true);
            assertEquals(15, ps.size());
        }, "intSwitch3");
    }

    @Test
    public void testStringSwitch() {
        TestUtility.getAllSolutions((mcb) -> {
            // String-switch will not be executed symbolically for now
            List<PathSolution> pathSolutions = TestUtility.executeMulib("stringSwitch", SwitchExamples.class, mcb, true, new Class[]{String.class}, new Object[]{""});
            assertEquals(1, pathSolutions.size());
        }, "stringSwitch");
    }

    @Test
    public void testStringSwitch2() {
        TestUtility.getAllSolutions((mcb) -> {
            // String-switch will not be executed symbolically for now
            List<PathSolution> pathSolutions = TestUtility.executeMulib("stringSwitch2", SwitchExamples.class, mcb, true, new Class[]{String.class}, new Object[]{""});
            assertEquals(1, pathSolutions.size());
        }, "stringSwitch2");
    }

    @Test @Disabled // TODO
    public void testEnumSwitch() {
        TestUtility.getAllSolutions((mcb) -> {
            mcb.setTRANSF_WRITE_TO_FILE(true);
            List<PathSolution> ps = TestUtility.executeMulib("enumSwitch", SwitchExamples.class, mcb, true);
            assertEquals(4, ps.size());
        }, "enumSwitch");
    }
}
