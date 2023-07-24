package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.transform_and_execute.examples.RememberSupport;
import de.wwu.mulib.transform_and_execute.examples.RememberSupport.A;
import de.wwu.mulib.transform_and_execute.examples.RememberSupport.B;
import de.wwu.mulib.transform_and_execute.examples.RememberSupport.C;
import de.wwu.mulib.transform_and_execute.examples.RememberSupport.D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RememberSupportExec {

    @Test
    public void testCheck() {
        TestUtility.getAllSolutions(
                mb -> {
                    // TODO If support for solver-internal theories for symbolic aliasing is introduced, get rid of this config:
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true); // This test uses aliasing and free objects in some parts
                    // No nulls or aliasing
                    mb.setENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL(false)
                            .setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(false)
                            .setALIASING_FOR_FREE_OBJECTS(false);
                    List<PathSolution> result0 = TestUtility.executeMulib(
                            "check0",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    assertEquals(1, result0.size());
                    D d0Label = (D) result0.get(0).getSolution().labels.getLabelForId("d");
                    checkDIsAfterSetup(d0Label);
                    D d0Return = (D) result0.get(0).getSolution().returnValue;
                    checkDIsAfterSetup(d0Return);

                    List<PathSolution> result1 = TestUtility.executeMulib(
                            "check1",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    assertEquals(1, result1.size());
                    D d1Label = (D) result1.get(0).getSolution().labels.getLabelForId("d");
                    assertNull(d1Label.getA());
                    assertEquals(0, d1Label.getVal());
                    D d1Return = (D) result1.get(0).getSolution().returnValue;
                    assertNull(d1Return.getA());
                    assertEquals(0, d1Return.getVal());

                    D d2Input = new D();
                    d2Input.setVal((char) (Character.MAX_VALUE - 2));
                    d2Input.setA(new A());
                    d2Input.getA().setVal(75);
                    List<PathSolution> result2 = TestUtility.executeMulib(
                            "check2",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ D.class },
                            new Object[] { d2Input }
                    );
                    assertEquals(1, result2.size());
                    D d2Label = (D) result2.get(0).getSolution().labels.getLabelForId("d");
                    assertEquals((char) (Character.MAX_VALUE - 2), d2Label.getVal());
                    assertEquals(75, d2Label.getA().getVal());
                    assertNull(d2Label.getA().getB());
                    D d2Return = (D) result2.get(0).getSolution().returnValue;
                    checkDIsAfterSetup(d2Return);

                    D d3Input = new D();
                    d3Input.setVal((char) (Character.MAX_VALUE - 2));
                    d3Input.setA(new A());
                    d3Input.getA().setVal(75);
                    List<PathSolution> result3 = TestUtility.executeMulib(
                            "check3",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ D.class },
                            new Object[] { d3Input }
                    );
                    assertEquals(1, result3.size());
                    D d3Label = (D) result3.get(0).getSolution().labels.getLabelForId("d");
                    checkDIsAfterSetup(d3Label);
                    D d3Return = (D) result3.get(0).getSolution().returnValue;
                    checkDIsAfterSetup(d3Return);

                    List<PathSolution> result4 = TestUtility.executeMulib(
                            "check4",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    assertEquals(1, result4.size());
                    D d4Label = (D) result4.get(0).getSolution().labels.getLabelForId("d");
                    assertEquals(200, d4Label.getVal());
                    assertEquals(201, d4Label.getA().getVal());
                    D d4Return = (D) result4.get(0).getSolution().returnValue;
                    assertEquals(200, d4Return.getVal());
                    assertEquals(22, d4Return.getA().getVal());

                    List<PathSolution> result5 = TestUtility.executeMulib(
                            "check5",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    assertEquals(1, result5.size());
                    D d5Label = (D) result5.get(0).getSolution().labels.getLabelForId("d");
                    assertEquals(200, d5Label.getVal());
                    assertEquals(201, d5Label.getA().getVal());
                    assertEquals(202, d5Label.getA().getB().getVal());
                    D d5Return = (D) result5.get(0).getSolution().returnValue;
                    assertEquals(200, d5Return.getVal());
                    assertEquals(22, d5Return.getA().getVal());
                    List<PathSolution> result6_0 = TestUtility.executeMulib(
                            "check6",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    // Only possible with aliasing
                    assertEquals(0, result6_0.size());

                    List<PathSolution> result7_0 = TestUtility.executeMulib(
                            "check7",
                            RememberSupport.class,
                            mb,
                            true,
                            new Class[]{ },
                            new Object[] { }
                    );
                    // Only possible with aliasing
                    assertEquals(0, result7_0.size());
                },
                "RememberSupport.check0"
        );
    }

    private static void checkDIsAfterSetup(D d) {
        A da = d.getA();
        B dab = d.getA().getB();
        C dabc = d.getA().getB().getC();
        A dabca = d.getA().getB().getC().getA();
        B dabcb = d.getA().getB().getC().getB();
        assertSame(dab, dabcb);
        assertNotSame(da, dabca);
        assertEquals(42, da.getVal());
        assertEquals(1338d, dab.getVal());
        assertEquals(1, dab.getCAr().length);
        assertEquals(1, dab.getCAr()[0].length);
        assertTrue(dabc.isVal());
        assertEquals(43, dabca.getVal());
    }

}
