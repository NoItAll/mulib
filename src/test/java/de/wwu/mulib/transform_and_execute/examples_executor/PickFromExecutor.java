package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.transform_and_execute.examples.GraphEdge;
import de.wwu.mulib.transform_and_execute.examples.PickFrom;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PickFromExecutor {

    @Test
    public void testCheck() {
        TestUtility.getAllSolutions(
                mb -> {
                    runChecks(mb, false);
                },
                "PickFrom.check"
        );
    }

    @Test
    public void testCheckWithEagerSelects() {
        TestUtility.getAllSolutions(
                mb -> {
                    runChecks(mb, true);
                },
                "PickFrom.check"
        );
    }


    private void runChecks(MulibConfig.MulibConfigBuilder mb, boolean runEager) {
        if (runEager) {
            mb.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true)
                    .setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS(true);
        } else {
            mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
        }
        mb.setTRANSF_REGARD_SPECIAL_CASE(List.of(GraphEdge.class));
        List<Solution> resultInts = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ int[].class },
                new Object[] { new int[]{ 3, 1, 2 } }
        );
        assertEquals(3, resultInts.size());
        assertTrue(resultInts.stream().anyMatch(s -> s.returnValue.equals(3)));
        assertTrue(resultInts.stream().anyMatch(s -> s.returnValue.equals(1)));
        assertTrue(resultInts.stream().anyMatch(s -> s.returnValue.equals(2)));


        List<Solution> resultLongs = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ long[].class },
                new Object[] { new long[]{ 3, 1, 2 } }
        );
        assertEquals(3, resultLongs.size());
        assertTrue(resultLongs.stream().anyMatch(s -> s.returnValue.equals(3L)));
        assertTrue(resultLongs.stream().anyMatch(s -> s.returnValue.equals(1L)));
        assertTrue(resultLongs.stream().anyMatch(s -> s.returnValue.equals(2L)));

        List<Solution> resultDoubles = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ double[].class },
                new Object[] { new double[]{ 3d, 1d, 2d } }
        );
        assertEquals(3, resultDoubles.size());
        assertTrue(resultDoubles.stream().anyMatch(s -> s.returnValue.equals(3d)));
        assertTrue(resultDoubles.stream().anyMatch(s -> s.returnValue.equals(1d)));
        assertTrue(resultDoubles.stream().anyMatch(s -> s.returnValue.equals(2d)));

        List<Solution> resultFloats = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ float[].class },
                new Object[] { new float[]{ 3f, 1f, 2f } }
        );
        assertEquals(3, resultFloats.size());
        assertTrue(resultFloats.stream().anyMatch(s -> s.returnValue.equals(3f)));
        assertTrue(resultFloats.stream().anyMatch(s -> s.returnValue.equals(1f)));
        assertTrue(resultFloats.stream().anyMatch(s -> s.returnValue.equals(2f)));

        List<Solution> resultShorts = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ short[].class },
                new Object[] { new short[]{ 3, 1, 2 } }
        );
        assertEquals(3, resultShorts.size());
        assertTrue(resultShorts.stream().anyMatch(s -> s.returnValue.equals((short) 3)));
        assertTrue(resultShorts.stream().anyMatch(s -> s.returnValue.equals((short) 1)));
        assertTrue(resultShorts.stream().anyMatch(s -> s.returnValue.equals((short) 2)));

        List<Solution> resultBytes = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ byte[].class },
                new Object[] { new byte[]{ 3, 1, 2 } }
        );
        assertEquals(3, resultBytes.size());
        assertTrue(resultBytes.stream().anyMatch(s -> s.returnValue.equals((byte) 3)));
        assertTrue(resultBytes.stream().anyMatch(s -> s.returnValue.equals((byte) 1)));
        assertTrue(resultBytes.stream().anyMatch(s -> s.returnValue.equals((byte) 2)));

        List<Solution> resultBools = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ boolean[].class },
                new Object[] { new boolean[]{ true, false, true } }
        );
        assertEquals(runEager ? 3 : 2, resultBools.size());
        assertTrue(resultBools.stream().anyMatch(s -> s.returnValue.equals(true)));
        assertTrue(resultBools.stream().anyMatch(s -> s.returnValue.equals(false)));

        List<Solution> resultChars = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ char[].class },
                new Object[] { new char[]{ 3, 1, 2 } }
        );
        assertEquals(3, resultChars.size());
        assertTrue(resultChars.stream().anyMatch(s -> s.returnValue.equals((char) 3)));
        assertTrue(resultChars.stream().anyMatch(s -> s.returnValue.equals((char) 1)));
        assertTrue(resultChars.stream().anyMatch(s -> s.returnValue.equals((char) 2)));


        List<Solution> resultGraphEdges = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ GraphEdge[].class },
                new Object[] { new GraphEdge[]{ new GraphEdge(1, 2), new GraphEdge(3, 4), null } }
        );
        assertEquals(3, resultGraphEdges.size());
        boolean seenNull = false, seenFirst = false, seenSecond = false;
        for (Solution s : resultGraphEdges) {
            GraphEdge ge = (GraphEdge) s.returnValue;
            if (ge == null) {
                assertFalse(seenNull);
                seenNull = true;
            } else if (ge.getStart() == 1 && ge.getEnd() == 2) {
                assertFalse(seenFirst);
                seenFirst = true;
            } else if (ge.getStart() == 3 && ge.getEnd() == 4) {
                assertFalse(seenSecond);
                seenSecond = true;
            } else {
                fail("Unknown solution: start: " + ge.getStart() + ", end: " + ge.getEnd());
            }
        }

        List<Solution> resultGraphEdgesArray = TestUtility.getUpToNSolutions(
                4,
                "check",
                PickFrom.class,
                mb,
                true,
                new Class[]{ GraphEdge[][].class },
                new Object[] { new GraphEdge[][] { null, new GraphEdge[]{ new GraphEdge(1, 2), new GraphEdge(3, 4), null }, new GraphEdge[0] } }
        );
        assertEquals(3, resultGraphEdgesArray.size());
    }
}