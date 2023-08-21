package de.wwu.mulib.transform_and_execute.examples_executor;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transform_and_execute.examples.generated_methods_check.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratedMethodsCheckExec {

    @Test
    public void testHasNoClinit0() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasNoClinit.class,
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasNoClinit.class,
                            mb,
                            Sint.newInputSymbolicSint()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit1() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasNoClinit.class,
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasNoClinit.class,
                            mb,
                            Slong.newInputSymbolicSlong()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit2() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasNoClinit.class,
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasNoClinit.class,
                            mb,
                            Sdouble.newInputSymbolicSdouble()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit3() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasNoClinit.class,
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasNoClinit.class,
                            mb,
                            Sfloat.newInputSymbolicSfloat()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit4() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasNoClinit.class,
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasNoClinit.class,
                            mb,
                            Sshort.newInputSymbolicSshort()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit5() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasNoClinit.class,
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasNoClinit.class,
                            mb,
                            Sbyte.newInputSymbolicSbyte()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasNoClinit6() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasNoClinit.class,
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasNoClinit.class,
                            mb,
                            Sbool.newInputSymbolicSbool()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasNoClinit"
        );
    }

    @Test
    public void testHasClinitInitialized0() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sint.newInputSymbolicSint()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized1() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasClinitInitialized.class,
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Slong.newInputSymbolicSlong()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized2() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasClinitInitialized.class,
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sdouble.newInputSymbolicSdouble()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized3() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sfloat.newInputSymbolicSfloat()
                    );
                    assertEquals(3, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized4() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sshort.newInputSymbolicSshort()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized5() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasClinitInitialized.class,
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sbyte.newInputSymbolicSbyte()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitInitialized6() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasClinitInitialized.class,
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasClinitInitialized.class,
                            mb,
                            Sbool.newInputSymbolicSbool()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitInitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized0() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkINT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sint.newInputSymbolicSint()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized1() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkLONG_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Slong.newInputSymbolicSlong()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized2() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkDOUBLE_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sdouble.newInputSymbolicSdouble()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized3() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkFLOAT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sfloat.newInputSymbolicSfloat()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized4() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkSHORT_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sshort.newInputSymbolicSshort()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized5() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBYTE_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sbyte.newInputSymbolicSbyte()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testHasClinitUninitialized6() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "checkBOOL_CONST",
                            HasClinitUninitialized.class,
                            mb,
                            Sbool.newInputSymbolicSbool()
                    );
                    assertEquals(2, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testHasClinitUninitialized"
        );
    }

    @Test
    public void testCyclicInputClasses() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc",
                            CyclicInputClasses.class,
                            mb,
                            new CyclicInputClasses(), new CyclicInputClasses()
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    PathSolution ps = result.get(0);
                    Object returnValue = ps.getSolution().returnValue;
                    assertInstanceOf(CyclicInputClasses.class, returnValue);
                    CyclicInputClasses cic = (CyclicInputClasses) returnValue;
                    assertEquals(84, (cic.getS()));
                    assertSame(cic.getCyclic().getCyclic().getCyclic(), cic.getCyclic());
                    assertSame(cic.getCyclic().getCyclic(), cic.getCyclic().getCyclic().getCyclic().getCyclic());
                    
                },
                "testCyclicInputClasses"
        );
    }

    @Test
    public void testIndirectCyclicInputClassesCalc0() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc0",
                            IndirectCyclicInputClasses0.class,
                            mb,
                            new IndirectCyclicInputClasses0()
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    assertEquals(3, result.get(0).getSolution().returnValue);
                    
                },
                "testIndirectCyclicInputClassesCalc0"
        );
    }

    @Test
    public void testIndirectCyclicInputClassesCalc1() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc1",
                            IndirectCyclicInputClasses0.class,
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    assertEquals(3, result.get(0).getSolution().returnValue);
                    
                },
                "testIndirectCyclicInputClassesCalc1"
        );
    }

    @Test
    public void testNeedsToPreinitializeFieldsCalc0() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc0",
                            NeedsToPreinitializeFields.class,
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc0"
        );
    }

    @Test
    public void testNeedsToPreinitializeFieldsCalc1() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc1",
                            NeedsToPreinitializeFields.class,
                            mb,
                            new NeedsToPreinitializeFields()
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "calc1",
                            NeedsToPreinitializeFields.class,
                            mb,
                            new NeedsToPreinitializeFields(1)
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "calc1",
                            NeedsToPreinitializeFields.class,
                            mb,
                            new NeedsToPreinitializeFields(1, 1)
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            "calc1",
                            NeedsToPreinitializeFields.class,
                            mb,
                            new NeedsToPreinitializeFields(1, 1, 1)
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc1"
        );
    }

    @Test
    public void testNeedsToPreinitializeFieldsCalc2() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc2",
                            NeedsToPreinitializeFields.class,
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc2"
        );
    }

    @Test
    public void testNeedsToPreinitializeFieldsCalc3() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc3",
                            NeedsToPreinitializeFields.class,
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc3"
        );
    }

    @Test
    public void testNeedsToPreinitializeFieldsCalc4() {
        TestUtility.getAllSolutions(mb -> {
                    List<PathSolution> result = Mulib.getPathSolutions(
                            "calc4",
                            NeedsToPreinitializeFields.class,
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc4"
        );
    }

}
