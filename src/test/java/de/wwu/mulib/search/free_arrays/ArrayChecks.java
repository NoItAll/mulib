package de.wwu.mulib.search.free_arrays;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.TestUtility;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrayChecks {

    @Test
    public void checkConcreteArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteSelect0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteSelect1",
                    ArrayChecks.class,
                    1,
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
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteStore1",
                    ArrayChecks.class,
                    1,
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
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());


            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess2",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess3",
                    ArrayChecks.class,
                    1,
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
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());


            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess2",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkConcreteIllegalAccess3",
                    ArrayChecks.class,
                    1,
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
    public void checkSymArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            List<PathSolution> result = TestUtility.executeMulib(
                    "checkSymSelect0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect2",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkSymSelect3",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(0, result.size());

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            result = TestUtility.executeMulib(
                    "checkSymSelect0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(4, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(3, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(2, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(1, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect2",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(4, result.size());
            assertEquals(2, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymSelect3",
                    ArrayChecks.class,
                    1,
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
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));


            result = TestUtility.executeMulib(
                    "checkSymStore1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            result = TestUtility.executeMulib(
                    "checkSymStore0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

            result = TestUtility.executeMulib(
                    "checkSymStore1",
                    ArrayChecks.class,
                    1,
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
                    1,
                    mb,
                    false
            );
            assertEquals(1, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkMultipleArrays1",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(7, result.size());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            result = TestUtility.executeMulib(
                    "checkMultipleArrays0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(11, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));

            result = TestUtility.executeMulib(
                    "checkMultipleArrays1",
                    ArrayChecks.class,
                    1,
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

    @Test
    public void checkSimpleSort() {
        TestUtility.getSolution((mb) -> {
            Optional<PathSolution> result = TestUtility.executeMulibForOne(
                    "simpleSort",
                    ArrayChecks.class,
                    1,
                    mb
            );
            assertTrue(result.isPresent());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            PathSolution pathSolution = result.get();
            Solution s = pathSolution.getInitialSolution();
            Object[] values = (Object[]) s.value;
            assertEquals(1, values[0]);
            assertEquals(1, values[1]);
            assertEquals(5, values[2]);
            assertEquals(8, values[3]);
            assertEquals(17, values[4]);
            assertEquals(27, values[5]);
            assertEquals(39, values[6]);
            assertEquals(42, values[7]);
            assertEquals(56, values[8]);
            assertEquals(78, values[9]);
            return result;
        });
    }

    @Test
    public void checkSimpleSortAlternative() {
        TestUtility.getSolution((mb) -> {
            Optional<PathSolution> result = TestUtility.executeMulibForOne(
                    "simpleSortAlternative",
                    ArrayChecks.class,
                    1,
                    mb
            );
            assertTrue(result.isPresent());
            assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
            PathSolution pathSolution = result.get();
            Solution s = pathSolution.getInitialSolution();
            Object[] values = (Object[]) s.value;
            assertEquals(-81, values[0]);
            assertEquals(-3, values[1]);
            assertEquals(0, values[2]);
            assertEquals(1, values[3]);
            assertEquals(2, values[4]);
            assertEquals(8, values[5]);
            assertEquals(9, values[6]);
            assertEquals(39, values[7]);
            assertEquals(42, values[8]);
            assertEquals(78, values[9]);
            return result;
        });
    }

    // Analogous to https://github.com/wwu-pi/muli-env/blob/59dcc66714d7953f68e741c7515e2f8289afbaf7/muli-runtime/src/test/resources/applications/freeArrays/SimpleSort.muli
    public static Sarray.SintSarray simpleSort() {
        SymbolicExecution se = SymbolicExecution.get();
        int[] b = new int[] {1, 42, 17, 56, 78, 5, 27, 39, 1, 8};

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
        int[] bBeforeFree = {-81, 42, -3, 9, 78, 0, 2, 39, 1, 8};
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
}
