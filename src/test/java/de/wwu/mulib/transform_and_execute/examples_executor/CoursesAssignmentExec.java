package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CoursesAssignmentExec {

    @Test
    public void testScheduling() {
        TestUtility.getAllSolutions(
                this::runScheduling,
                "testScheduling"
        );
    }

    private List<PathSolution> runScheduling(MulibConfig.MulibConfigBuilder builder) {
        List<PathSolution> result = Mulib.getPathSolutions(Courses.class, "schedule", builder);
        assertTrue(!result.isEmpty());
        for (PathSolution ps : result) {
            ArrayList<Courses.Assignment> assignments = (ArrayList<Courses.Assignment>) ps.getSolution().returnValue;
            for (Courses.Assignment a : assignments) {
                assertNotNull(a);
            }
        }
        return result;
    }
}
