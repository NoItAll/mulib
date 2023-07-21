package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.Knapsack2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KnapsackExec {

    @Test
    public void testKnapsack() {
        Mulib.setLogLevel(Level.FINE);
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "findKnapsack",
                    Knapsack2.class,
                    mb,
                    true
            );
            assertEquals(1023, result.size());
            for (PathSolution ps : result) {
                ArrayList<Knapsack2.Item> returnValue = (ArrayList<Knapsack2.Item>) ps.getSolution().returnValue;
                int accWeight = 0;
                for (Knapsack2.Item it : returnValue) {
                    accWeight += it.weight;
                }
                assertTrue(accWeight <= 4600);
            }
        }, "findKnapsack");
    }

}
