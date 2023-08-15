package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.transform_and_execute.examples.SendMoreMoney;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendMoreMoneyExec {

    @Test
    public void testSendMoreMoney() {
        TestUtility.getAllSolutions(
                mb -> {
                    List<PathSolution> result = TestUtility.executeMulib(
                            "search",
                            SendMoreMoney.class,
                            mb,
                            true
                    );
                    assertEquals(1, result.size());
                    Solution s = result.get(0).getSolution();
                    assertEquals(9, s.labels.getLabelForId("s"));
                    assertEquals(5, s.labels.getLabelForId("e"));
                    assertEquals(6, s.labels.getLabelForId("n"));
                    assertEquals(7, s.labels.getLabelForId("d"));
                    assertEquals(1, s.labels.getLabelForId("m"));
                    assertEquals(0, s.labels.getLabelForId("o"));
                    assertEquals(8, s.labels.getLabelForId("r"));
                    assertEquals(5, s.labels.getLabelForId("e"));

                    List<Solution> sols = TestUtility.getUpToNSolutions(
                            100,
                            "search",
                            SendMoreMoney.class,
                            mb
                    );
                    assertEquals(1, sols.size());
                    s = sols.get(0);
                    assertEquals(9, s.labels.getLabelForId("s"));
                    assertEquals(5, s.labels.getLabelForId("e"));
                    assertEquals(6, s.labels.getLabelForId("n"));
                    assertEquals(7, s.labels.getLabelForId("d"));
                    assertEquals(1, s.labels.getLabelForId("m"));
                    assertEquals(0, s.labels.getLabelForId("o"));
                    assertEquals(8, s.labels.getLabelForId("r"));
                    assertEquals(5, s.labels.getLabelForId("e"));
                },
                "SendMoreMoney.search"
        );
    }

}
