package de.wwu.mulib.search.free_arrays;

import de.wwu.mulib.Fail;
import de.wwu.mulib.Mulib;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayChecks {

    @Test
    public void checkConcreteArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteSelect0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteSelect1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            return result;
        }, "checkConcreteArraySelect");
    }

    public static Sbool checkConcreteSelect0() {
        SymbolicExecution se = SymbolicExecution.get();
        for (int i = 0; i < 2; i++) {
            Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
            if (objs.getLength().notEqChoice(se.concSint(2), se)) {
                throw Mulib.fail();
            }
            Sint temp = objs.select(se.concSint(1), se);
            temp = objs.select(se.concSint(0), se);
        }
        return se.concSbool(true);
    }

    public static Sbool checkConcreteSelect1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(12), true);
        Sint first = null;
        Sint second = null;
        for (int i = 0; i < 2; i++) {
            Sint temp = objs.select(se.concSint(1), se);
            if (first == null) {
                first = temp;
            } else {
                if (first != temp) {
                    throw new MulibRuntimeException("Must not occur");
                }
            }
            temp = objs.select(se.concSint(0), se);
            if (second == null) {
                second = temp;
            } else {
                if (second != temp) {
                    throw new MulibRuntimeException("Must not occur");
                }
            }
        }
        return se.concSbool(true);
    }

    @Test
    public void checkConcreteArrayStore() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteStore0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteStore1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            return result;
        }, "checkConcreteArrayStore");
    }

    public static Sbool checkConcreteStore0() {
        SymbolicExecution se = SymbolicExecution.get();
        for (int i = 0; i < 2; i++) {
            Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
            if (objs.getLength().notEqChoice(se.concSint(2), se)) {
                throw Mulib.fail();
            }
            objs.store(se.concSint(1), se.concSint(1), se);
            if (!se.eqChoice(objs.select(se.concSint(1), se), Sint.ConcSint.ONE)) {
                throw new MulibRuntimeException("Must not occur");
            }
            objs.store(se.concSint(1), se.concSint(0), se);
            if (!se.eqChoice(objs.select(se.concSint(1), se), Sint.ConcSint.ZERO)) {
                throw new MulibRuntimeException("Must not occur");
            }
        }
        return se.concSbool(true);
    }

    public static Sbool checkConcreteStore1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(2), true);
        Sint first = null;
        Sint second = null;
        for (int i = 0; i < 2; i++) {
            if (first != null) {
                if (first != objs.select(se.concSint(1), se)) {
                    throw new MulibRuntimeException("Must not occur");
                }
            }
            Sint temp = se.symSint();
            objs.store(se.concSint(1), temp, se);
            if (first == null) {
                first = temp;
            }

            if (second != null) {
                if (second != objs.select(se.concSint(0), se)) {
                    throw new MulibRuntimeException("Must not occur");
                }
            }
            temp = se.symSint();
            objs.store(se.concSint(0), temp, se);
            if (second == null) {
                second = temp;
            }
        }
        return se.concSbool(true);
    }

    @Test
    public void checkConcreteIllegalAccessWithOOB() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setTHROW_EXCEPTION_ON_OOB(true);
            mb.setALLOW_EXCEPTIONS(true);
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());


            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess2",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess3",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));
            return result;
        }, "checkConcreteIllegalAccessWithOOB");
    }

    @Test
    public void checkConcreteIllegalAccess() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setTHROW_EXCEPTION_ON_OOB(false);
            mb.setALLOW_EXCEPTIONS(true);
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());


            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess2",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess3",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));
            return result;
        }, "checkConcreteIllegalAccess");
    }

    public static Sbool checkConcreteIllegalAccess0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        Sint temp = objs.select(se.concSint(0), se);
        objs.store(se.concSint(12), se.concSint(11), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(12), true);
        Sint temp = objs.select(se.concSint(13), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        Sint temp = objs.select(se.concSint(12000), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess3() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        Sint temp = objs.select(se.concSint(-1), se);
        return se.concSbool(true);
    }

    @Test
    public void checkValueDominanceDueToCaching() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "valueDominanceDueToCaching0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertTrue(result.stream().anyMatch(r -> ((Boolean) r.getSolution().returnValue)));
            assertTrue(result.stream().anyMatch(r -> !((Boolean) r.getSolution().returnValue)));
            return result;
        }, "checkValueDominanceDueToCaching");

        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "valueDominanceDueToCaching1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertTrue(result.size() > 0);
            return result;
        }, "checkValueDominanceDueToCaching");
    }

    public static Sbool valueDominanceDueToCaching0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray a = se.sintSarray(se.concSint(2), true);
        Sint index = se.symSint();
        Sint val = a.select(index, se);
        a.store(se.symSint(), se.concSint(12), se);
        boolean found = false;
        for (int i = 0; i < 2; i++) {
            found = found || a.select(se.concSint(i), se).eqChoice(val, se);
        }
        return se.concSbool(found);
    }
    public static Sbool valueDominanceDueToCaching1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray a = se.sintSarray(se.concSint(2), true);
        Sint index = se.symSint();
        Sint val = a.select(index, se);
        a.store(se.symSint(), se.concSint(12), se);

        if (a.select(index, se).eqChoice(val, se)) {
            throw Mulib.fail();
        }

        return se.concSbool(true);
    }

    @Test
    public void checkSymArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkSymSelect0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect2",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect3",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(0, result.size());

            mb.setALLOW_EXCEPTIONS(true);
            mb.setTHROW_EXCEPTION_ON_OOB(true);
            result = TestUtility.executeMulib(
                    "checkSymSelect0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(4, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(3, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(1, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect2",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(4, result.size());
            assertEquals(2, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect3",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());
            return result;
        }, "checkSymArraySelect");
    }

    public static Sbool checkSymSelect0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sint temp = objs.select(se.symSint(), se);
        temp = objs.select(se.symSint(), se);
        return se.concSbool(true);
    }

    public static Sbool checkSymSelect1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(2), true);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sint temp = objs.select(se.symSint(), se);
        return se.concSbool(true);
    }

    public static Sbool checkSymSelect2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(2), true);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sint index0 = se.symSint();
        Sint index1 = se.symSint();
        Sint temp0 = objs.select(index0, se);
        Sint temp1 = objs.select(index1, se);

        // Different values
        if (temp0.eqChoice(temp1, se)) {
            throw Mulib.fail();
        }

        if (index0.eqChoice(index1, se)) {
            // Different values, thus the indices must not equal
            throw new MulibRuntimeException("Must not occur");
        }

        Sint temp2 = objs.select(Sint.concSint(0), se);
        // One of both must be equal to temp2, since the array only has size 2 and temp0 != temp1
        if (temp0.notEqChoice(temp2, se) && temp1.notEqChoice(temp2, se)) {
            throw new MulibRuntimeException("Must not occur");
        }
        Sint temp3 = objs.select(Sint.concSint(1), se);

        if (temp3.eqChoice(temp2, se)) {
            throw new MulibRuntimeException("Must not occur");
        }
        if (temp3.eqChoice(temp1, se)) {
            if (temp2.notEqChoice(temp0, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
        } else {
            if (temp3.notEqChoice(temp0, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
            if (temp2.notEqChoice(temp1, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
        }

        return se.concSbool(true);
    }

    public static Sbool checkSymSelect3() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }

        se.select(objs, se.concSint(2)); // Must fail

        return se.concSbool(true);
    }

    @Test
    public void checkSymArrayStore() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkSymStore0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));


            result = TestUtility.executeMulib(
                    "checkSymStore1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            mb.setALLOW_EXCEPTIONS(true);
            result = TestUtility.executeMulib(
                    "checkSymStore0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymStore1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));

            return result;
        }, "checkSymArrayStore");
    }

    public static Sbool checkSymStore0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
        if (objs.getLength().notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sint temp0 = objs.select(se.symSint(), se);
        Sint conc = se.concSint(1);

        if (temp0.eqChoice(conc, se)) {
            throw Mulib.fail();
        }

        objs.store(se.concSint(0), conc, se);

        Sint index = se.symSint();
        if (index.notEqChoice(se.concSint(0), se)) {
            throw Mulib.fail();
        }

        Sint temp1 = objs.select(index, se);
        if (temp1.eqChoice(temp0, se)) {
            throw new MulibRuntimeException("Must not occur");
        }

        if (temp1.notEqChoice(conc, se)) {
            throw new MulibRuntimeException("Must not occur");
        }

        return se.concSbool(true);
    }

    public static Sbool checkSymStore1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = se.sintSarray(se.concSint(1), true);
        Sint index0 = se.symSint();
        Sint temp0 = objs.select(index0, se);

        Sint temp1 = se.symSint();
        if (temp0.eqChoice(temp1, se)) {
            throw Mulib.fail();
        }

        Sint index1 = se.symSint();
        if (index0.notEqChoice(index1, se)) {
            throw Mulib.fail();
        }

        objs.store(index1, temp1, se);

        Sint temp2 = objs.select(index0, se);

        if (temp2.notEqChoice(temp1, se)) {
            throw new MulibRuntimeException("Must not occur");
        }
        if (temp2.eqChoice(temp0, se)) {
            throw new MulibRuntimeException("Must not occur");
        }

        return se.concSbool(true);
    }

    @Test
    public void checkMultipleArrays() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkMultipleArrays0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkMultipleArrays1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(7, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            mb.setALLOW_EXCEPTIONS(true);
            result = TestUtility.executeMulib(
                    "checkMultipleArrays0",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(11, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkMultipleArrays1",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertEquals(12, result.size());
            assertEquals(7, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));

            return result;

        }, "checkMultipleArrays");
    }

    public static boolean checkMultipleArrays0() {
        SymbolicExecution se = SymbolicExecution.get();
        for (int i = 0; i < 5; i++) {
            Sarray.SintSarray objs = se.sintSarray(se.symSint(), true);
            if (objs.getLength().notEqChoice(se.concSint(1), se)) {
                throw Mulib.fail();
            }
            Sint temp0 = objs.select(se.symSint(), se);
            Sint conc = se.concSint(1);

            if (temp0.eqChoice(conc, se)) {
                throw Mulib.fail();
            }

            objs.store(se.concSint(0), conc, se);

            Sint index = se.symSint();
            if (index.notEqChoice(se.concSint(0), se)) {
                throw Mulib.fail();
            }

            Sint temp1 = objs.select(index, se);
            if (temp1.eqChoice(temp0, se)) {
                throw new MulibRuntimeException("Must not occur");
            }

            if (temp1.notEqChoice(conc, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
        }
        return true;
    }

    public static boolean checkMultipleArrays1() {
        SymbolicExecution se = SymbolicExecution.get();
        if (se.symSbool().boolChoice(se)) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Sarray.SintSarray objs = se.sintSarray(se.concSint(1), true);
            Sint index0 = se.symSint();
            Sint temp0 = objs.select(index0, se);

            Sint temp1 = se.symSint();
            if (temp0.eqChoice(temp1, se)) {
                throw Mulib.fail();
            }

            Sint index1 = se.symSint();
            if (index0.notEqChoice(index1, se)) {
                throw Mulib.fail();
            }

            objs.store(index1, temp1, se);
            objs.store(index0, temp1, se);

            Sint temp2 = objs.select(index0, se);

            if (temp2.notEqChoice(temp1, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
            if (temp2.eqChoice(temp0, se)) {
                throw new MulibRuntimeException("Must not occur");
            }
            if (temp2.gtChoice(se.concSint(12), se)) {
                return true;
            }
        }
        return true;
    }

//    @Test @Disabled(value="Subsumed by FreeArraysExec.testSimpleSort0")
    public void checkSimpleSort() {
        TestUtility.getSolution((mb) -> {
            Optional<PathSolution> result = TestUtility.executeMulibForOne(
                    "simpleSort",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertTrue(result.isPresent());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            PathSolution pathSolution = result.get();
            Solution s = pathSolution.getSolution();
            Object[] values = (Object[]) s.returnValue;
            assertEquals(1, values[0]);
            assertEquals(1, values[1]);
            assertEquals(5, values[2]);
            assertEquals(17, values[3]);
            assertEquals(39, values[4]);
            assertEquals(42, values[5]);
            assertEquals(56, values[6]);
            return result;
        });
    }

//    @Test @Disabled(value="Subsumed by FreeArraysExec.testSimpleSort1")
    public void checkSimpleSortAlternative() {
        TestUtility.getSolution((mb) -> {
            Optional<PathSolution> result = TestUtility.executeMulibForOne(
                    "simpleSortAlternative",
                    ArrayChecks.class,
                    mb,
                    false
            );
            assertTrue(result.isPresent());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            PathSolution pathSolution = result.get();
            Solution s = pathSolution.getSolution();
            Object[] values = (Object[]) s.returnValue;
            assertEquals(-81, values[0]);
            assertEquals(0, values[1]);
            assertEquals(1, values[2]);
            assertEquals(8, values[3]);
            assertEquals(9, values[4]);
            assertEquals(42, values[5]);
            assertEquals(78, values[6]);
            return result;
        });
    }

    // Analogous to https://github.com/wwu-pi/muli-env/blob/59dcc66714d7953f68e741c7515e2f8289afbaf7/muli-runtime/src/test/resources/applications/freeArrays/SimpleSort.muli
    public static Sarray.SintSarray simpleSort() {
        SymbolicExecution se = SymbolicExecution.get();
        int[] b = new int[] {1, 42, 17, 56, 5, 39, 1};

        Sarray.SintSarray idx = se.sintSarray(se.symSint(), true);
        Sarray.SboolSarray usedIdx = se.sboolSarray(se.symSint(), true);
        Sarray.SintSarray a = se.sintSarray(se.symSint(), true);
        if (se.notEqChoice(a.getLength(), se.concSint(b.length))) {
            throw Mulib.fail();
        }
        for (int i = 0; se.concSint(i).ltChoice(se.concSint(b.length), se); i++) {
            if (usedIdx.select(idx.select(se.concSint(i), se), se).boolChoice(se)) {
                throw Mulib.fail();
            }
            a.store(idx.select(se.concSint(i), se), se.concSint(b[i]), se);
            usedIdx.store(idx.select(se.concSint(i), se), Sbool.ConcSbool.TRUE, se);
        }
        for (int i = 0; i < b.length - 1; i++) {
            if (a.select(se.concSint(i), se).gtChoice(a.select(se.concSint(i + 1), se), se)) {
                throw Mulib.fail();
            }
        }
        return a;
    }

    public static Sarray.SintSarray simpleSortAlternative() {
        SymbolicExecution se = SymbolicExecution.get();
        int[] bBeforeFree = {-81, 42, 9, 78, 0, 1, 8};
        int n = bBeforeFree.length;
        Sarray.SintSarray b = se.sintSarray(se.concSint(n), true);
        Sarray.SintSarray idx = se.sintSarray(se.concSint(n), true);
        boolean failed = false;
        for (int i = 0; i < n; i++) {
            if (se.select(idx, se.concSint(i)).ltChoice(se.concSint(0), se)
                    || se.select(idx, se.concSint(i)).gteChoice(se.concSint(n), se)) {
                failed = true; break;
            }
            boolean innerFailed = false;
            for (int j = 0; j < n; j++) {
                if (i != j && idx.select(se.concSint(i), se).eqChoice(idx.select(se.concSint(j), se), se)) {
                    innerFailed = true; break;
                }
            }
            if (innerFailed) {
                failed = true; break;
            }
        }
        if (failed) {
            throw Mulib.fail();
        }
        for (int i = 0; i < n; i++) {
            b.store(se.concSint(i), se.concSint(bBeforeFree[i]), se);
        }

        Sarray.SintSarray a = se.sintSarray(se.concSint(n), true);
        for (int i = 0; i < n; i++) {
            a.store(idx.select(se.concSint(i), se), b.select(se.concSint(i), se), se);
        }

        for (int i = 0; i < n-1; i++) {
            if (a.select(se.concSint(i), se).gtChoice(a.select(se.concSint(i+1), se), se)) {
                throw Mulib.fail();
            }
        }

        return a;
    }

    @Test
    public void checkArrayLabeling() {
        TestUtility.getAllSolutions((mb) -> {
            List<Solution> solutions =
                    TestUtility.getUpToNSolutions(
                            100,
                            "arrayLabeling",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );
            assertEquals(2, solutions.size());
            boolean seen14 = false;
            boolean seen13 = false;
            for (Solution s : solutions) {
                assertTrue(s.returnValue.getClass().isArray());
                Object[] sArray = (Object[]) s.returnValue;
                assertEquals(1, (int) ((Integer) sArray[1]));
                if ((Integer) sArray[0] == 13) {
                    assertFalse(seen13);
                    seen13 = true;
                } else if ((Integer) sArray[0] == 14) {
                    assertFalse(seen14);
                    seen14 = true;
                } else {
                    fail();
                }
            }
            return solutions;
        }, "arrayLabeling");
    }

    public static Sarray.SintSarray arrayLabeling() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray sintSarray = se.sintSarray(se.concSint(2), true);
        Sint i0Index = se.namedSymSint("i0Index");
        Sint i1Index = se.namedSymSint("i1Index");
        Sint i0Value = sintSarray.select(i0Index, se);
        Sint i1Value = sintSarray.select(i1Index, se);
        if (!i0Value.eqChoice(se.concSint(13), se)
                || !i1Value.eqChoice(se.concSint(14), se)
                || i0Index.eqChoice(i1Index, se)) {
            throw Mulib.fail();
        }
        sintSarray.store(se.concSint(1), se.concSint(1), se);
        return sintSarray;
    }


    @Test
    public void testArrayArraySimpleSumWithNonEagerIndices() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            1,
                            "simpleSumNonEager0",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class<?>[0],
                            new Object[0]
                    );
                    assertEquals(1, solutions.size());
                    assertEquals(9, solutions.get(0).returnValue);

                    solutions = TestUtility.getUpToNSolutions(
                            1,
                            "simpleSumNonEager1",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class<?>[0],
                            new Object[0]
                    );
                    assertEquals(1, solutions.size());
                    assertEquals(9, solutions.get(0).returnValue);

                    solutions = TestUtility.getUpToNSolutions(
                            1,
                            "simpleSumNonEager2",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class<?>[0],
                            new Object[0]
                    );
                    assertEquals(1, solutions.size());
                    assertEquals(9, solutions.get(0).returnValue);
                    return solutions;
                },
                "arrayArraySimpleSumWithNonEagerIndices"
        );
    }

    public static Sint simpleSumNonEager0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray vals = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(1), false);
        first.store(se.concSint(0), se.concSint(4), se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(1), false);
        second.store(se.concSint(0), se.concSint(5), se);
        vals.store(se.concSint(0), first, se);
        vals.store(se.concSint(1), second, se);
        Sint index0 = se.symSint();
        Sint index1 = se.symSint();
        if (!se.nextIsOnKnownPath()) {
            se.addNewConstraint(ConcolicConstraintContainer.tryGetSymFromConcolic(se.not(se.eq(index0, index1))));
        }
        Sarray.SintSarray firstAny = (Sarray.SintSarray) vals.select(index0, se);
        Sarray.SintSarray secondAny = (Sarray.SintSarray) vals.select(index1, se);

        Sint selectFromFirstAny = firstAny.select(se.concSint(0), se);
        Sint selectFromSecondAny = secondAny.select(se.concSint(0), se);
        return selectFromFirstAny.add(selectFromSecondAny, se);
    }

    public static Sint simpleSumNonEager1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray vals = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(2), false);
        first.store(se.concSint(0), se.concSint(4), se);
        first.store(se.concSint(1), se.concSint(4), se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(2), false);
        second.store(se.concSint(0), se.concSint(5), se);
        second.store(se.concSint(1), se.concSint(5), se);
        vals.store(se.concSint(0), first, se);
        vals.store(se.concSint(1), second, se);
        Sint index0 = se.symSint();
        Sint index1 = se.symSint();
        if (!se.nextIsOnKnownPath()) {
            se.addNewConstraint(ConcolicConstraintContainer.tryGetSymFromConcolic(se.not(se.eq(index0, index1))));
        }
        Sarray.SintSarray firstAny = (Sarray.SintSarray) vals.select(index0, se);
        Sarray.SintSarray secondAny = (Sarray.SintSarray) vals.select(index1, se);

        Sint selectFromFirstAny = firstAny.select(se.symSint(), se);
        Sint selectFromSecondAny = secondAny.select(se.symSint(), se);
        return selectFromFirstAny.add(selectFromSecondAny, se);
    }

    public static Sint simpleSumNonEager2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray vals = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(2), false);
        Sint index0 = se.symSint();
        Sint index1 = se.symSint();
        if (se.eqChoice(index0, index1)) {
            throw new Fail();
        }
        first.store(index0, se.concSint(4), se);
        first.store(index1, se.concSint(4), se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(2), false);
        second.store(index0, se.concSint(5), se);

        if (se.boolChoice(se.symSbool())) {
            throw new Fail();
        }

        second.store(index1, se.concSint(5), se);
        vals.store(se.concSint(0), first, se);
        vals.store(se.concSint(1), second, se);

        Sarray.SintSarray firstAny = (Sarray.SintSarray) vals.select(se.concSint(0), se);
        Sarray.SintSarray secondAny = (Sarray.SintSarray) vals.select(se.concSint(1), se);

        Sint index2 = se.symSint();
        Sint index3 = se.symSint();
        if (!se.notEqChoice(index2, index3)) {
            throw new Fail();
        }

        Sint selectFromFirstAny = firstAny.select(index2, se);

        if (se.boolChoice(se.symSbool())) {
            throw new Fail();
        }

        Sint selectFromSecondAny = secondAny.select(index3, se);
        return selectFromFirstAny.add(selectFromSecondAny, se);
    }

    @Test
    public void testAssignWithPreproduction() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            3,
                            "assignWithPreproduction0",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );
                    assertEquals(1, solutions.size());

                    Solution singleSolution = solutions.get(0);
                    Object[] returnValue = (Object[]) singleSolution.returnValue;
                    assertEquals(2, returnValue.length);
                    Object[] innerReturnValue = (Object[]) returnValue[0];
                    assertEquals(2, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    assertEquals(0, innerReturnValue[1]);
                    innerReturnValue = (Object[]) returnValue[1];
                    assertEquals(1, innerReturnValue.length);
                    assertEquals(0, innerReturnValue[0]);
                    solutions = TestUtility.getUpToNSolutions(
                            3,
                            "assignWithPreproduction1",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(0, solutions.size());
                    return solutions;
                },
                "testAssignWithPreproduction0"
        );
    }

    public static Sarray.SarraySarray assignWithPreproduction0() {
        SymbolicExecution se = SymbolicExecution.get();

        Sarray.SintSarray machineCapacitiesPerPeriod = se.sintSarray(se.concSint(1), false);
        machineCapacitiesPerPeriod.store(se.concSint(0), se.concSint(5), se);
        Sarray.SarraySarray workloadsWithDeadlineInPeriod = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(2), false);
        first.store(se.concSint(0), se.concSint(2), se);
        first.store(se.concSint(1), se.concSint(2), se);
        workloadsWithDeadlineInPeriod.store(se.concSint(0), first, se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(1), false);
        second.store(se.concSint(0), se.concSint(5), se);
        workloadsWithDeadlineInPeriod.store(se.concSint(1), second, se);

        Sint length = workloadsWithDeadlineInPeriod.length();
        Sarray.SarraySarray overallCapacities = se.sarraySarray(length, Sint[].class, false);

        int i;
        for(i = 0; !se.concSint(i).gteChoice(length, se); ++i) {
            Sarray.SintSarray var8 = copy(machineCapacitiesPerPeriod);
            overallCapacities.store(se.concSint(i), var8, se);
        }

        Sarray.SarraySarray assignmentsPerPeriod = se.sarraySarray(length, Sint[].class, false);
        i = 0;

        while(!se.concSint(i).gteChoice(length, se)) {
            machineCapacitiesPerPeriod = (Sarray.SintSarray)workloadsWithDeadlineInPeriod.select(se.concSint(i), se);
            Sarray.SintSarray var4 = se.sintSarray(machineCapacitiesPerPeriod.length(), false);
            assignmentsPerPeriod.store(se.concSint(i), var4, se);
            int j = 0;

            while(true) {
                Sint lengthOrChosenMachineIndex = machineCapacitiesPerPeriod.length();
                if (se.concSint(j).gteChoice(lengthOrChosenMachineIndex, se)) {
                    ++i;
                    break;
                }

                lengthOrChosenMachineIndex = se.symSint();
                Sint chosenMachinePeriod = se.symSint();
                Sarray.SintSarray intermediate = (Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se);
                Sint chosenCapacity = intermediate.select(lengthOrChosenMachineIndex, se);
                if (chosenMachinePeriod.gtChoice(se.concSint(i), se) || !chosenCapacity.gteChoice(machineCapacitiesPerPeriod.select(se.concSint(j), se), se)) {
                    throw new Fail();
                }

                ((Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se)).store(lengthOrChosenMachineIndex, chosenCapacity.sub(machineCapacitiesPerPeriod.select(se.concSint(j), se), se), se);
                ((Sarray.SintSarray)assignmentsPerPeriod.select(se.concSint(i), se)).store(se.concSint(j), lengthOrChosenMachineIndex, se);
                ++j;
            }
        }

        return assignmentsPerPeriod;
    }

    public static Sarray.SarraySarray assignWithPreproduction1() {
        SymbolicExecution se = SymbolicExecution.get();

        Sarray.SintSarray machineCapacitiesPerPeriod0 = se.sintSarray(se.concSint(1), false);
        machineCapacitiesPerPeriod0.store(se.concSint(0), se.concSint(5), se);
        Sarray.SintSarray machineCapacitiesPerPeriod1 = se.sintSarray(se.concSint(1), false);
        machineCapacitiesPerPeriod1.store(se.concSint(0), se.concSint(5), se);

        Sarray.SarraySarray workloadsWithDeadlineInPeriod = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(0), false);
        workloadsWithDeadlineInPeriod.store(se.concSint(0), first, se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(2), false);
        second.store(se.concSint(0), se.concSint(3), se);
        second.store(se.concSint(1), se.concSint(6), se);
        workloadsWithDeadlineInPeriod.store(se.concSint(1), second, se);

        Sint length = workloadsWithDeadlineInPeriod.length();
        Sarray.SarraySarray overallCapacities = se.sarraySarray(length, Sint[].class, false);
        overallCapacities.store(se.concSint(0), machineCapacitiesPerPeriod0, se);
        overallCapacities.store(se.concSint(1), machineCapacitiesPerPeriod1, se);


        Sarray.SarraySarray assignmentsPerPeriod = se.sarraySarray(length, Sint[].class, false);
        Sarray.SintSarray workloadsOfPeriod = (Sarray.SintSarray)workloadsWithDeadlineInPeriod.select(se.concSint(0), se);
        Sarray.SintSarray temp = se.sintSarray(workloadsOfPeriod.length(), false);
        assignmentsPerPeriod.store(se.concSint(0), temp, se);
        // Skip iteration for i = 0 since there are no workloads there
        Sarray.SintSarray workloadsOfPeriod1 = (Sarray.SintSarray)workloadsWithDeadlineInPeriod.select(se.concSint(1), se);
        Sarray.SintSarray temp1 = se.sintSarray(workloadsOfPeriod1.length(), false);
        assignmentsPerPeriod.store(se.concSint(1), temp1, se);

        Sint lengthOrChosenMachineIndex = se.symSint();
        Sint chosenMachinePeriod = se.symSint();
        Sarray.SintSarray intermediate = (Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se);
        Sint chosenCapacity = intermediate.select(lengthOrChosenMachineIndex, se);
        if (chosenMachinePeriod.gtChoice(se.concSint(1), se) || !chosenCapacity.gteChoice(workloadsOfPeriod1.select(se.concSint(0), se), se)) {
            throw new Fail();
        }

        Sarray.SintSarray tempSarray0 = ((Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se));
        Sint difference = chosenCapacity.sub(workloadsOfPeriod1.select(se.concSint(0), se), se);
        tempSarray0.store(lengthOrChosenMachineIndex, difference, se);
        // Is not symbolic in any way:
        ((Sarray.SintSarray)assignmentsPerPeriod.select(se.concSint(1), se)).store(se.concSint(0), lengthOrChosenMachineIndex, se);


        Sint lengthOrChosenMachineIndex1 = se.symSint();
        Sint chosenMachinePeriod1 = se.symSint();
        Sarray.SintSarray intermediate1 = (Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod1, se);
        Sint chosenCapacity1 = intermediate1.select(lengthOrChosenMachineIndex1, se);
        if (chosenMachinePeriod1.gtChoice(se.concSint(1), se) || !chosenCapacity1.gteChoice(workloadsOfPeriod1.select(se.concSint(1), se), se)) {
            throw new Fail();
        }

        ((Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod1, se)).store(lengthOrChosenMachineIndex1, chosenCapacity1.sub(workloadsOfPeriod1.select(se.concSint(1), se), se), se);
        ((Sarray.SintSarray)assignmentsPerPeriod.select(se.concSint(1), se)).store(se.concSint(1), lengthOrChosenMachineIndex1, se);

        return assignmentsPerPeriod;
    }


    @Test
    public void testAssignWithPreproductionMoreComplex() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            20,
                            "assignWithPreproductionMoreComplex",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );
                    assertEquals(2, solutions.size());
                    boolean seenOne = false;
                    boolean seenZero = false;
                    for (Solution s : solutions) {
                        Object[] valuesOfTwoPeriods = (Object[]) s.returnValue;
                        assertEquals(2, valuesOfTwoPeriods.length);
                        Object[] firstPeriod = (Object[]) valuesOfTwoPeriods[0];
                        assertEquals(1, firstPeriod[0]);
                        assertEquals(0, firstPeriod[1]);
                        Object[] secondPeriod = (Object[]) valuesOfTwoPeriods[1];
                        assertEquals(0, secondPeriod[0]);
                        if (((Integer) secondPeriod[1]) == 1) {
                            seenOne = true;
                        } else {
                            assertEquals(0, secondPeriod[1]);
                            seenZero = true;
                        }
                    }
                    assertTrue(seenOne);
                    assertTrue(seenZero);

                    return solutions;
                },
                "testAssignWithPreproductionMoreComplex"
        );
    }

    public static Sarray.SarraySarray assignWithPreproductionMoreComplex() {
        SymbolicExecution se = SymbolicExecution.get();

        Sarray.SintSarray machineCapacitiesPerPeriod = se.sintSarray(se.concSint(2), false);
        machineCapacitiesPerPeriod.store(se.concSint(0), se.concSint(5), se);
        machineCapacitiesPerPeriod.store(se.concSint(1), se.concSint(2), se);
        Sarray.SarraySarray workloadsWithDeadlineInPeriod = se.sarraySarray(se.concSint(2), Sint[].class, false);
        Sarray.SintSarray first = se.sintSarray(se.concSint(2), false);
        first.store(se.concSint(0), se.concSint(2), se);
        first.store(se.concSint(1), se.concSint(4), se);
        workloadsWithDeadlineInPeriod.store(se.concSint(0), first, se);
        Sarray.SintSarray second = se.sintSarray(se.concSint(2), false);
        second.store(se.concSint(0), se.concSint(5), se);
        second.store(se.concSint(1), se.concSint(1), se);
        workloadsWithDeadlineInPeriod.store(se.concSint(1), second, se);

        Sint length = workloadsWithDeadlineInPeriod.length();
        Sarray.SarraySarray overallCapacities = se.sarraySarray(length, Sint[].class, false);

        int i;
        for(i = 0; !se.concSint(i).gteChoice(length, se); ++i) {
            Sarray.SintSarray var8 = copy(machineCapacitiesPerPeriod);
            overallCapacities.store(se.concSint(i), var8, se);
        }

        Sarray.SarraySarray assignmentsPerPeriod = se.sarraySarray(length, Sint[].class, false);
        i = 0;

        while(!se.concSint(i).gteChoice(length, se)) {
            machineCapacitiesPerPeriod = (Sarray.SintSarray)workloadsWithDeadlineInPeriod.select(se.concSint(i), se);
            Sarray.SintSarray var4 = se.sintSarray(machineCapacitiesPerPeriod.length(), false);
            assignmentsPerPeriod.store(se.concSint(i), var4, se);
            int j = 0;

            while(true) {
                Sint lengthOrChosenMachineIndex = machineCapacitiesPerPeriod.length();
                if (se.concSint(j).gteChoice(lengthOrChosenMachineIndex, se)) {
                    ++i;
                    break;
                }

                lengthOrChosenMachineIndex = se.symSint();
                Sint chosenMachinePeriod = se.symSint();
                Sarray.SintSarray intermediate = (Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se);
                Sint chosenCapacity = intermediate.select(lengthOrChosenMachineIndex, se);
                if (chosenMachinePeriod.gtChoice(se.concSint(i), se) || !chosenCapacity.gteChoice(machineCapacitiesPerPeriod.select(se.concSint(j), se), se)) {
                    throw new Fail();
                }

                ((Sarray.SintSarray)overallCapacities.select(chosenMachinePeriod, se)).store(lengthOrChosenMachineIndex, chosenCapacity.sub(machineCapacitiesPerPeriod.select(se.concSint(j), se), se), se);
                ((Sarray.SintSarray)assignmentsPerPeriod.select(se.concSint(i), se)).store(se.concSint(j), lengthOrChosenMachineIndex, se);
                ++j;
            }
        }

        return assignmentsPerPeriod;
    }

    private static Sarray.SintSarray copy(Sarray.SintSarray var0) {
        SymbolicExecution var4 = SymbolicExecution.get();
        Sarray.SintSarray var1 = var4.sintSarray(var0.length(), false);
        int var3 = 0;

        while(true) {
            Sint var2 = var1.length();
            if (var4.concSint(var3).gteChoice(var2, var4)) {
                return var1;
            }

            var2 = var0.select(var4.concSint(var3), var4);
            var1.store(var4.concSint(var3), var2, var4);
            ++var3;
        }
    }


    @Test
    public void testSarraySarraysWithNulls() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> solutions = TestUtility.executeMulib(
                            "sarraySarraysWithNulls",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertTrue(solutions.stream().anyMatch(s -> s instanceof ExceptionPathSolution && s.getSolution().returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> !(s instanceof ExceptionPathSolution) && ((Integer) s.getSolution().returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithNulls"
        );
    }

    public static Sint sarraySarraysWithNulls() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    return solutions;
                },
                "sarraySarraysWithoutNulls"
        );
    }

    public static Sint sarraySarraysWithoutNulls() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.concSint(0), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls1() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls1",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    return solutions;
                },
                "sarraySarraysWithoutNulls1"
        );
    }

    public static Sint sarraySarraysWithoutNulls1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.concSint(0), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray0.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls2() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls2",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithoutNulls2"
        );
    }

    public static Sint sarraySarraysWithoutNulls2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.symSint(), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.symSint(), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray1.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls3() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls3",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithoutNulls3"
        );
    }

    public static Sint sarraySarraysWithoutNulls3() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.symSint(), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.symSint(), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray1.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        sintSarray0.store(se.symSint(), se.concSint(-5), se);
        sintSarray1.store(se.symSint(), se.concSint(-5), se);
        sintSarray0.store(se.concSint(0), se.concSint(-5), se);
        sintSarray1.store(se.concSint(0), se.concSint(-5), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls4() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls4",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3)); // for sintSarray0
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 0)); // for sintSarray1
                    return solutions;
                },
                "sarraySarraysWithoutNulls4"
        );
    }

    public static Sint sarraySarraysWithoutNulls4() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        Sint firstIndex = se.symSint();
        Sint secondIndex = se.symSint();
        if (firstIndex.eqChoice(secondIndex, se)) {
            throw Mulib.fail();
        }
        sarraySarray.store(firstIndex, sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(secondIndex, sintSarray1, se); // sintSarray1 will have all zeros!
        sintSarray0.store(se.concSint(0), se.concSint(3), se); // Overwrite value in sintSarray0
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraySarrayCache() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            20,
                            "sarraySarraySarrayCache",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );
                    assertEquals(5, solutions.size());
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 4));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 5));
                    return solutions;
                },
                "sarraySarraySarrayCache"
        );
    }

    public static Sint sarraySarraySarrayCache() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarraySarray = se.sarraySarray(se.concSint(2), Sint[][].class, false); // int[][][]
        Sarray.SarraySarray sarraySarray0 = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SarraySarray sarraySarray1 = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray00 = se.sintSarray(se.concSint(1), false); // int[]
        Sarray.SintSarray sintSarray01 = se.sintSarray(se.concSint(1), false); // ...
        Sarray.SintSarray sintSarray10 = se.sintSarray(se.concSint(1), false);
        Sarray.SintSarray sintSarray11 = se.sintSarray(se.concSint(1), false);
        sarraySarraySarray.store(se.concSint(0), sarraySarray0, se);
        sarraySarraySarray.store(se.concSint(1), sarraySarray1, se);
        sarraySarray0.store(se.concSint(0), sintSarray00, se);
        sarraySarray0.store(se.concSint(1), sintSarray01, se);
        sarraySarray1.store(se.concSint(0), sintSarray10, se);
        sarraySarray1.store(se.concSint(1), sintSarray11, se);
        sintSarray00.store(se.concSint(0), se.concSint(1), se);
        sintSarray01.store(se.concSint(0), se.concSint(2), se);
        sintSarray10.store(se.concSint(0), se.concSint(3), se);
        sintSarray11.store(se.concSint(0), se.concSint(4), se);

        Sarray.SarraySarray sarraySarray = (Sarray.SarraySarray) sarraySarraySarray.select(se.symSint(), se);
        Sarray.SintSarray sintSarray = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray.store(se.symSint(), se.concSint(5), se);

        return ((Sarray.SintSarray) sarraySarraySarray.select(se.symSint(), se).select(se.symSint(), se)).select(se.symSint(), se);
    }


    /* ARRAY CHECKS WITH SYMBOLIC LENGTH */
    @Test
    public void testSarraySarraysWithNullsWithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<PathSolution> solutions = TestUtility.executeMulib(
                            "sarraySarraysWithNullsWithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertTrue(solutions.stream().anyMatch(s -> s instanceof ExceptionPathSolution && s.getSolution().returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> !(s instanceof ExceptionPathSolution) && ((Integer) s.getSolution().returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithNulls"
        );
    }

    public static Sint sarraySarraysWithNullsWithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNullsWithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNullsWithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    return solutions;
                },
                "sarraySarraysWithoutNullsWithSymbolicLength"
        );
    }

    public static Sint sarraySarraysWithoutNullsWithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sint sintSarrayLength0 = se.symSint();
        if (sintSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray0 = se.sintSarray(sintSarrayLength0, false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.concSint(0), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls1WithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls1WithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    return solutions;
                },
                "sarraySarraysWithoutNulls1WithSymbolicLength"
        );
    }

    public static Sint sarraySarraysWithoutNulls1WithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sint sintSarrayLength1 = se.symSint();
        if (sintSarrayLength1.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray1 = se.sintSarray(sintSarrayLength1, false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.concSint(0), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray0.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls2WithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls2WithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithoutNulls2WithSymbolicLength"
        );
    }

    public static Sint sarraySarraysWithoutNulls2WithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sint sintSarrayLength0 = se.symSint();
        if (sintSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray0 = se.sintSarray(sintSarrayLength0, false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.symSint(), se.concSint(2), se);
        Sint sintSarrayLength1 = se.symSint();
        if (sintSarrayLength1.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray1 = se.sintSarray(sintSarrayLength1, false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.symSint(), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray1.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls3WithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls3WithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == -1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    return solutions;
                },
                "sarraySarraysWithoutNulls3WithSymbolicLength"
        );
    }

    public static Sint sarraySarraysWithoutNulls3WithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray0 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(0), sintSarray0, se);
        sintSarray0.store(se.symSint(), se.concSint(2), se);
        Sarray.SintSarray sintSarray1 = se.sintSarray(se.concSint(1), false); // int[]
        sarraySarray.store(se.concSint(1), sintSarray1, se);
        sintSarray1.store(se.symSint(), se.concSint(3), se);
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray1.store(se.symSint(), se.concSint(-1), se);
        Sint element = symSelected.select(se.concSint(0), se);
        sintSarray0.store(se.symSint(), se.concSint(-5), se);
        sintSarray1.store(se.symSint(), se.concSint(-5), se);
        sintSarray0.store(se.concSint(0), se.concSint(-5), se);
        sintSarray1.store(se.concSint(0), se.concSint(-5), se);
        return element;
    }

    @Test
    public void testSarraySarraysWithoutNulls4WithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarraySarraysWithoutNulls4WithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3)); // for sintSarray0
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 0)); // for sintSarray1
                    return solutions;
                },
                "sarraySarraysWithoutNulls4WithSymbolicLength"
        );
    }

    public static Sint sarraySarraysWithoutNulls4WithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sint[].class, false); // int[][]
        Sint sintSarrayLength0 = se.symSint();
        if (sintSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray0 = se.sintSarray(sintSarrayLength0, false); // int[]
        Sint firstIndex = se.symSint();
        Sint secondIndex = se.symSint();
        if (firstIndex.eqChoice(secondIndex, se)) {
            throw Mulib.fail();
        }
        sarraySarray.store(firstIndex, sintSarray0, se);
        sintSarray0.store(se.concSint(0), se.concSint(2), se);
        Sint sintSarrayLength1 = se.symSint();
        if (sintSarrayLength1.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SintSarray sintSarray1 = se.sintSarray(sintSarrayLength1, false); // int[]
        sarraySarray.store(secondIndex, sintSarray1, se); // sintSarray1 will have all zeros!
        sintSarray0.store(se.concSint(0), se.concSint(3), se); // Overwrite value in sintSarray0
        Sarray.SintSarray symSelected = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        Sint element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarraySarraySarrayCacheWithSymbolicLength() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            20,
                            "sarraySarraySarrayCacheWithSymbolicLength",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );
                    assertEquals(5, solutions.size());
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 1));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 3));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 4));
                    assertTrue(solutions.stream().anyMatch(s -> ((Integer) s.returnValue) == 5));
                    return solutions;
                },
                "sarraySarraySarrayCacheWithSymbolicLength"
        );
    }

    public static Sint sarraySarraySarrayCacheWithSymbolicLength() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarraySarrayLength = se.symSint();
        if (sarraySarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarraySarray = se.sarraySarray(sarraySarraySarrayLength, Sint[][].class, false); // int[][][]
        Sint sarraySarray0Length = se.symSint();
        if (sarraySarray0Length.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray0 = se.sarraySarray(sarraySarray0Length, Sint[].class, false); // int[][]
        Sarray.SarraySarray sarraySarray1 = se.sarraySarray(se.concSint(2), Sint[].class, false); // int[][]
        Sarray.SintSarray sintSarray00 = se.sintSarray(se.concSint(1), false); // int[]
        Sarray.SintSarray sintSarray01 = se.sintSarray(se.concSint(1), false); // ...
        Sarray.SintSarray sintSarray10 = se.sintSarray(se.concSint(1), false);
        Sarray.SintSarray sintSarray11 = se.sintSarray(se.concSint(1), false);
        sarraySarraySarray.store(se.concSint(0), sarraySarray0, se);
        sarraySarraySarray.store(se.concSint(1), sarraySarray1, se);
        sarraySarray0.store(se.concSint(0), sintSarray00, se);
        sarraySarray0.store(se.concSint(1), sintSarray01, se);
        sarraySarray1.store(se.concSint(0), sintSarray10, se);
        sarraySarray1.store(se.concSint(1), sintSarray11, se);
        sintSarray00.store(se.concSint(0), se.concSint(1), se);
        sintSarray01.store(se.concSint(0), se.concSint(2), se);
        sintSarray10.store(se.concSint(0), se.concSint(3), se);
        sintSarray11.store(se.concSint(0), se.concSint(4), se);

        Sarray.SarraySarray sarraySarray = (Sarray.SarraySarray) sarraySarraySarray.select(se.symSint(), se);
        Sarray.SintSarray sintSarray = (Sarray.SintSarray) sarraySarray.select(se.symSint(), se);
        sintSarray.store(se.symSint(), se.concSint(5), se);

        return ((Sarray.SintSarray) sarraySarraySarray.select(se.symSint(), se).select(se.symSint(), se)).select(se.symSint(), se);
    }


    @Test
    public void testSarrayWithDefaultValues0() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarrayWithDefaultValues0",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertFalse(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> ((Byte) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> ((Byte) s.returnValue) == 3));
                    return solutions;
                },
                "sarrayWithDefaultValues0"
        );
    }

    public static Sbyte sarrayWithDefaultValues0() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sbyte[].class, true); // byte[][]
        Sint sbyteSarrayLength0 = se.symSint();
        if (sbyteSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        if (sarraySarray.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sarray.SbyteSarray sbyteSarray0 = se.sbyteSarray(sbyteSarrayLength0, false); // byte[]
        sarraySarray.store(se.concSint(0), sbyteSarray0, se);
        sbyteSarray0.store(se.concSint(0), se.concSbyte((byte) 2), se);
        Sarray.SbyteSarray sbyteSarray1 = se.sbyteSarray(se.concSint(1), false); // byte[]
        sarraySarray.store(se.concSint(1), sbyteSarray1, se);
        sbyteSarray1.store(se.concSint(0), se.concSbyte((byte) 3), se);
        Sarray.SbyteSarray symSelected = (Sarray.SbyteSarray) sarraySarray.select(se.symSint(), se);
        Sbyte element = symSelected.select(se.concSint(0), se);
        return element;
    }

    @Test
    public void testSarrayWithDefaultValues1() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> pathSolutions = TestUtility.executeMulib(
                            "sarrayWithDefaultValues1",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, pathSolutions.size());
                    assertTrue(pathSolutions.stream().anyMatch(s -> s.getSolution().returnValue instanceof NullPointerException));
                    assertTrue(pathSolutions.stream().anyMatch(s -> !(s.getSolution().returnValue instanceof NullPointerException)));


                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            3,
                            "sarrayWithDefaultValues1",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, solutions.size());
                    assertTrue(solutions.stream().anyMatch(s -> s.returnValue instanceof NullPointerException));
                    assertTrue(solutions.stream().anyMatch(s -> (s.returnValue instanceof Short) && ((Short) s.returnValue) == 3));
                    return solutions;
                },
                "sarrayWithDefaultValues1"
        );
    }

    public static Sshort sarrayWithDefaultValues1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Sshort[].class, false); // short[][]
        Sint sshortSarrayLength0 = se.symSint();
        if (sshortSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SshortSarray sshortSarray0 = se.sshortSarray(sshortSarrayLength0, false); // short[]
        sarraySarray.store(se.concSint(0), sshortSarray0, se);
        sshortSarray0.store(se.concSint(0), se.concSshort((byte) 2), se);
        Sarray.SshortSarray sshortSarray1 = se.sshortSarray(se.concSint(1), false); // short[]
        sshortSarray1.store(se.concSint(0), se.concSshort((byte) 3), se);
        sarraySarray.store(se.concSint(0), sshortSarray1, se);
        Sarray.SshortSarray symSelected = (Sarray.SshortSarray) sarraySarray.select(se.symSint(), se);
        Sshort element = symSelected.select(se.concSint(0), se);
        return element;
    }


    @Test
    public void testSarrayWithDefaultValues2() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> pathSolutions = TestUtility.executeMulib(
                            "sarrayWithDefaultValues2",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, pathSolutions.size());
                    assertTrue(pathSolutions.stream().anyMatch(s -> s.getSolution().returnValue instanceof NullPointerException));
                    assertTrue(pathSolutions.stream().anyMatch(s -> !(s.getSolution().returnValue instanceof NullPointerException)));


                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarrayWithDefaultValues2",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(3, solutions.size());
                    assertEquals(1, solutions.stream().filter(s -> s.returnValue instanceof NullPointerException).count());
                    assertTrue(solutions.stream().anyMatch(s -> (s.returnValue instanceof Long) && ((Long) s.returnValue) == 2));
                    assertTrue(solutions.stream().anyMatch(s -> (s.returnValue instanceof Long) && ((Long) s.returnValue) == 3));
                    return solutions;
                },
                "sarrayWithDefaultValues2"
        );
    }

    public static Slong sarrayWithDefaultValues2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SarraySarray sarraySarray = se.sarraySarray(se.concSint(2), Slong[].class, true); // long[][]
        Sint slongSarrayLength0 = se.symSint();
        if (slongSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        if (sarraySarray.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sarray.SlongSarray slongSarray0 = se.slongSarray(slongSarrayLength0, false); // long[]
        sarraySarray.store(se.concSint(0), slongSarray0, se);
        slongSarray0.store(se.concSint(0), se.concSlong((byte) 2), se);
        Sarray.SlongSarray slongSarray1 = se.slongSarray(se.concSint(1), false); // long[]
        slongSarray1.store(se.concSint(0), se.concSlong((byte) 3), se);
        sarraySarray.store(se.concSint(0), slongSarray1, se);
        Sarray.SlongSarray symSelected = (Sarray.SlongSarray) sarraySarray.select(se.symSint(), se);
        Slong element = symSelected.select(se.symSint(), se);
        if (element.gtChoice(se.concSlong(3), se) || element.ltChoice(se.concSlong(2), se)) {
            throw Mulib.fail();
        }
        return element;
    }


    @Test
    public void testSarrayWithDefaultValues3() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> pathSolutions = TestUtility.executeMulib(
                            "sarrayWithDefaultValues3",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(2, pathSolutions.size());
                    assertTrue(pathSolutions.stream().anyMatch(s -> s.getSolution().returnValue instanceof NullPointerException));
                    assertTrue(pathSolutions.stream().anyMatch(s -> !(s.getSolution().returnValue instanceof NullPointerException)));

                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            5,
                            "sarrayWithDefaultValues3",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(5, solutions.size());
                    long numberNpes = solutions.stream().filter(s -> s.returnValue instanceof NullPointerException).count();
                    assertTrue(numberNpes <= 1);
                    assertEquals(5 - numberNpes, solutions.stream().filter(s -> (s.returnValue instanceof Double) && ((Double) s.returnValue) <= 3 && ((Double) s.returnValue) >= 2).count());
                    return solutions;
                },
                "sarrayWithDefaultValues3"
        );
    }

    public static Sdouble sarrayWithDefaultValues3() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sdouble[].class, true); // double[][]
        Sint sdoubleSarrayLength0 = se.symSint();
        if (sdoubleSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        if (sarraySarray.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sarray.SdoubleSarray sdoubleSarray0 = se.sdoubleSarray(sdoubleSarrayLength0, false); // double[]
        sarraySarray.store(se.concSint(0), sdoubleSarray0, se);
        sdoubleSarray0.store(se.concSint(0), se.concSdouble((byte) 2), se);
        Sarray.SdoubleSarray sdoubleSarray1 = se.sdoubleSarray(se.concSint(1), false); // double[]
        sdoubleSarray1.store(se.concSint(0), se.concSdouble((byte) 3), se);
        sarraySarray.store(se.concSint(0), sdoubleSarray1, se);
        Sarray.SdoubleSarray symSelected = (Sarray.SdoubleSarray) sarraySarray.select(se.symSint(), se);
        Sdouble element = symSelected.select(se.concSint(0), se);
        if (element.gtChoice(se.concSdouble(3), se) || element.ltChoice(se.concSdouble(2), se)) {
            throw Mulib.fail();
        }
        return element;
    }

    @Test
    public void testSarrayWithDefaultValues4() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> pathSolutions = TestUtility.executeMulib(
                            "sarrayWithDefaultValues4",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(3, pathSolutions.size());
                    assertEquals(2, pathSolutions.stream().filter(s -> s.getSolution().returnValue instanceof NullPointerException).count());
                    assertTrue(pathSolutions.stream().anyMatch(s -> s.getSolution().returnValue instanceof Float && ((Float) s.getSolution().returnValue) == 2.1f));

                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            3,
                            "sarrayWithDefaultValues4",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(3, solutions.size());
                    assertEquals(2, solutions.stream().filter(s -> s.returnValue instanceof NullPointerException).count());
                    assertTrue(solutions.stream().anyMatch(s -> (s.returnValue instanceof Float) && ((Float) s.returnValue) == 2.1f));
                    return solutions;
                },
                "sarrayWithDefaultValues4"
        );
    }

    public static Sfloat sarrayWithDefaultValues4() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sfloat[].class, false); // float[][]
        Sint sfloatSarrayLength0 = se.symSint();
        if (sfloatSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        if (sarraySarray.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sarray.SfloatSarray sfloatSarray0 = se.sfloatSarray(sfloatSarrayLength0, true); // float[]
        if (sfloatSarray0.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        sarraySarray.store(se.concSint(0), sfloatSarray0, se);
        sfloatSarray0.store(se.concSint(0), se.concSfloat(3.1f), se);
        Sarray.SfloatSarray sfloatSarray1 = se.sfloatSarray(se.concSint(1), true); // float[]
        sfloatSarray1.store(se.concSint(0), se.concSfloat((float) 2.1), se);
        sarraySarray.store(se.concSint(0), sfloatSarray1, se);
        Sarray.SfloatSarray symSelected = (Sarray.SfloatSarray) sarraySarray.select(se.symSint(), se);
        Sfloat element = symSelected.select(se.symSint(), se);
        return element;
    }


    @Test
    public void testSarrayWithDefaultValues5() {
        TestUtility.getAllSolutions(
                mb -> {
                    mb.setHIGH_LEVEL_FREE_ARRAY_THEORY(true);
                    mb.setENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(true);
                    mb.setALLOW_EXCEPTIONS(true);
                    List<PathSolution> pathSolutions = TestUtility.executeMulib(
                            "sarrayWithDefaultValues5",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(3, pathSolutions.size());
                    assertEquals(2, pathSolutions.stream().filter(s -> s.getSolution().returnValue instanceof NullPointerException).count());
                    assertTrue(pathSolutions.stream().anyMatch(s -> s.getSolution().returnValue instanceof Boolean && ((Boolean) s.getSolution().returnValue)));

                    List<Solution> solutions = TestUtility.getUpToNSolutions(
                            3,
                            "sarrayWithDefaultValues5",
                            ArrayChecks.class,
                            mb,
                            false,
                            new Class[0],
                            new Object[0]
                    );

                    assertEquals(3, solutions.size());
                    assertEquals(2, solutions.stream().filter(s -> s.returnValue instanceof NullPointerException).count());
                    assertTrue(solutions.stream().anyMatch(s -> (s.returnValue instanceof Boolean) && ((Boolean) s.returnValue)));
                    return solutions;
                },
                "sarrayWithDefaultValues5"
        );
    }

    public static Sbool sarrayWithDefaultValues5() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint sarraySarrayLength = se.symSint();
        if (sarraySarrayLength.notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sarray.SarraySarray sarraySarray = se.sarraySarray(sarraySarrayLength, Sbool[].class, false); // bool[][]
        Sint sboolSarrayLength0 = se.symSint();
        if (sboolSarrayLength0.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        if (sarraySarray.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sarray.SboolSarray sboolSarray0 = se.sboolSarray(sboolSarrayLength0, true); // bool[]
        if (sboolSarray0.isNull().boolChoice(se)) {
            throw Mulib.fail();
        }
        Sint storeInSarraySarray0 = se.symSint();
        if (storeInSarraySarray0.notEqChoice(se.concSint(0), se)) {
            throw Mulib.fail();
        }
        sarraySarray.store(storeInSarraySarray0, sboolSarray0, se);
        Sint storeInSboolSarray0 = se.symSint();
        if (storeInSboolSarray0.notEqChoice(se.concSint(0), se)) {
            throw Mulib.fail();
        }
        sboolSarray0.store(storeInSboolSarray0, se.concSbool(false), se);
        Sint sboolSarray1Length = se.symSint();
        if (sboolSarray1Length.notEqChoice(se.concSint(1), se)) {
            throw Mulib.fail();
        }
        Sarray.SboolSarray sboolSarray1 = se.sboolSarray(sboolSarray1Length, true); // bool[]
        Sint storeInSboolSarray1 = se.symSint();
        if (storeInSboolSarray1.notEqChoice(se.concSint(0), se)) {
            throw Mulib.fail();
        }
        sboolSarray1.store(storeInSboolSarray1, se.concSbool(true), se);
        Sint storeInSarraySarray1 = se.symSint();
        if (storeInSarraySarray1.notEqChoice(se.concSint(0), se)) {
            throw Mulib.fail();
        }
        sarraySarray.store(storeInSarraySarray1, sboolSarray1, se);
        Sarray.SboolSarray symSelected = (Sarray.SboolSarray) sarraySarray.select(se.symSint(), se);
        Sbool element = symSelected.select(se.symSint(), se);
        return element;
    }



}
