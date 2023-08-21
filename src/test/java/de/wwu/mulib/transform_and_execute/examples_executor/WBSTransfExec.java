package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transform_and_execute.examples.apache2_examples.WBSTransf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WBSTransfExec {

    @Test
    public void testWBSTransf() {
        TestUtility.getAllSolutions(this::_testWBSTransf, "launch");
    }

    private List<PathSolution> _testWBSTransf(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "launch",
                WBSTransf.class,
                mb,
                true
        );
        assertEquals(13824, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    @Test
    public void testUpdate0() {
        TestUtility.getAllSolutions(this::_testUpdate0, "update0");
    }

    private List<PathSolution> _testUpdate0(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = Mulib.getPathSolutions("update0", WBSTransf.class, mb, new Class[] { WBSTransf.class }, new WBSTransf());
        assertEquals(24, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    @Test
    public void testUpdate1() {
        TestUtility.getAllSolutions(this::_testUpdate1, "update1");
    }

    private List<PathSolution> _testUpdate1(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new Class[] { WBSTransf.class, int.class, boolean.class, boolean.class }, new WBSTransf(), 1, true, false);
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new Class[] { WBSTransf.class, int.class, boolean.class, boolean.class }, new WBSTransf(), Sint.concSint(12), Sbool.concSbool(false), Sbool.concSbool(true));
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new Class[] { WBSTransf.class, int.class, boolean.class, boolean.class }, new WBSTransf(), Sint.newInputSymbolicSint(), Sbool.concSbool(true), false);
        assertEquals(6, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new Class[] { WBSTransf.class, int.class, boolean.class, boolean.class }, new WBSTransf(), Sint.concSint(12), Sbool.newInputSymbolicSbool(), Sbool.concSbool(true));
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }

    @Test
    public void testUpdate1FindMethodWithoutArgTypes() {
        TestUtility.getAllSolutions(this::_testUpdate1FindMethodWithoutArgTypes, "update1");
    }

    private List<PathSolution> _testUpdate1FindMethodWithoutArgTypes(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new WBSTransf(), 1, true, false);
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new WBSTransf(), Sint.concSint(12), Sbool.concSbool(false), Sbool.concSbool(true));
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new WBSTransf(), Sint.newInputSymbolicSint(), Sbool.concSbool(true), false);
        assertEquals(6, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        result = Mulib.getPathSolutions("update1", WBSTransf.class, mb, new WBSTransf(), Sint.concSint(12), Sbool.newInputSymbolicSbool(), Sbool.concSbool(true));
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}
