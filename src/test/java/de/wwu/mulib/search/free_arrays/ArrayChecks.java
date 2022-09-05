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
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
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
            Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(12), true, se);
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
            Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(2), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
        Sint temp = objs.select(se.concSint(0), se);
        objs.store(se.concSint(12), se.concSint(11), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(12), true, se);
        Sint temp = objs.select(se.concSint(13), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
        Sint temp = objs.select(se.concSint(12000), se);
        return se.concSbool(true);
    }

    public static Sbool checkConcreteIllegalAccess3() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sint temp = objs.select(se.symSint(), se);
        temp = objs.select(se.symSint(), se);
        return se.concSbool(true);
    }

    public static Sbool checkSymSelect1() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(2), true, se);
        if (objs.getLength().notEqChoice(se.concSint(2), se)) {
            throw Mulib.fail();
        }
        Sint temp = objs.select(se.symSint(), se);
        return se.concSbool(true);
    }

    public static Sbool checkSymSelect2() {
        SymbolicExecution se = SymbolicExecution.get();
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(2), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(1), true, se);
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
            Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
            Sarray.SintSarray objs = SymbolicExecution.sintSarray(se.concSint(1), true, se);
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

        Sarray.SintSarray idx = SymbolicExecution.sintSarray(se.symSint(), true, se);
        Sarray.SboolSarray usedIdx = SymbolicExecution.sboolSarray(se.symSint(), true, se);
        Sarray.SintSarray a = SymbolicExecution.sintSarray(se.symSint(), true, se);
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
        Sarray.SintSarray b = SymbolicExecution.sintSarray(se.concSint(n), true, se);
        Sarray.SintSarray idx = SymbolicExecution.sintSarray(se.concSint(n), true, se);
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

        Sarray.SintSarray a = SymbolicExecution.sintSarray(se.concSint(n), true, se);
        for (int i = 0; i < n; i++) {
            a.store(idx.select(se.concSint(i), se), b.select(se.concSint(i), se), se);
        }

        for (int i = 0; i < n-1; i++) {
            if (a.select(se.concSint(i, se), se).gtChoice(a.select(se.concSint(i+1), se), se)) {
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

}
