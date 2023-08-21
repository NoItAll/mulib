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
                            HasNoClinit.class,
                            "checkINT_CONST",
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkINT_CONST",
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
                            HasNoClinit.class,
                            "checkLONG_CONST",
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkLONG_CONST",
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
                            HasNoClinit.class,
                            "checkDOUBLE_CONST",
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkDOUBLE_CONST",
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
                            HasNoClinit.class,
                            "checkFLOAT_CONST",
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkFLOAT_CONST",
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
                            HasNoClinit.class,
                            "checkSHORT_CONST",
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkSHORT_CONST",
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
                            HasNoClinit.class,
                            "checkBYTE_CONST",
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkBYTE_CONST",
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
                            HasNoClinit.class,
                            "checkBOOL_CONST",
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasNoClinit.class,
                            "checkBOOL_CONST",
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
                            HasClinitInitialized.class,
                            "checkINT_CONST",
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkINT_CONST",
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
                            HasClinitInitialized.class,
                            "checkLONG_CONST",
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkLONG_CONST",
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
                            HasClinitInitialized.class,
                            "checkDOUBLE_CONST",
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkDOUBLE_CONST",
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
                            HasClinitInitialized.class,
                            "checkFLOAT_CONST",
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkFLOAT_CONST",
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
                            HasClinitInitialized.class,
                            "checkSHORT_CONST",
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkSHORT_CONST",
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
                            HasClinitInitialized.class,
                            "checkBYTE_CONST",
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkBYTE_CONST",
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
                            HasClinitInitialized.class,
                            "checkBOOL_CONST",
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitInitialized.class,
                            "checkBOOL_CONST",
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
                            HasClinitUninitialized.class,
                            "checkINT_CONST",
                            mb,
                            1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkINT_CONST",
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
                            HasClinitUninitialized.class,
                            "checkLONG_CONST",
                            mb,
                            (long) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkLONG_CONST",
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
                            HasClinitUninitialized.class,
                            "checkDOUBLE_CONST",
                            mb,
                            (double) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkDOUBLE_CONST",
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
                            HasClinitUninitialized.class,
                            "checkFLOAT_CONST",
                            mb,
                            (float) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkFLOAT_CONST",
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
                            HasClinitUninitialized.class,
                            "checkSHORT_CONST",
                            mb,
                            (short) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkSHORT_CONST",
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
                            HasClinitUninitialized.class,
                            "checkBYTE_CONST",
                            mb,
                            (byte) 1 // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkBYTE_CONST",
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
                            HasClinitUninitialized.class,
                            "checkBOOL_CONST",
                            mb,
                            false // Val
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            HasClinitUninitialized.class,
                            "checkBOOL_CONST",
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
                            CyclicInputClasses.class,
                            "calc",
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
                            IndirectCyclicInputClasses0.class,
                            "calc0",
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
                            IndirectCyclicInputClasses0.class,
                            "calc1",
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
                            NeedsToPreinitializeFields.class,
                            "calc0",
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
                            NeedsToPreinitializeFields.class,
                            "calc1",
                            mb,
                            new NeedsToPreinitializeFields()
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            NeedsToPreinitializeFields.class,
                            "calc1",
                            mb,
                            new NeedsToPreinitializeFields(1)
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            NeedsToPreinitializeFields.class,
                            "calc1",
                            mb,
                            new NeedsToPreinitializeFields(1, 1)
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    result = Mulib.getPathSolutions(
                            NeedsToPreinitializeFields.class,
                            "calc1",
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
                            NeedsToPreinitializeFields.class,
                            "calc2",
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
                            NeedsToPreinitializeFields.class,
                            "calc3",
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
                            NeedsToPreinitializeFields.class,
                            "calc4",
                            mb
                    );
                    assertEquals(1, result.size());
                    assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
                    
                },
                "testNeedsToPreinitializeFieldsCalc4"
        );
    }

}
