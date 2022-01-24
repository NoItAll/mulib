package de.wwu.mulib.search.free_arrays;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrayChecks {

    @Test
    public void checkConcreteArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
            Sint temp = objs.store(se.concSint(1), se.symSint(), se);
            if (first == null) {
                first = temp;
            }

            if (second != null) {
                if (second != objs.select(se.concSint(0), se)) {
                    throw new MulibRuntimeException("Must not occur");
                }
            }
            temp = objs.store(se.concSint(0), se.symSint(), se);
            if (second == null) {
                second = temp;
            }
        }
        return se.concSbool(true);
    }

    @Test
    public void checkConcreteIllegalAccessWithOOB() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));

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
        }, "checkConcreteIllegalAccessWithOOB");
    }

    @Test
    public void checkConcreteIllegalAccess() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
    public void checkSymArraySelect() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setCONCOLIC(false); /// TODO currently not regarded
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

            mb.setTHROW_EXCEPTION_ON_OOB(true);
            result = TestUtility.executeMulib(
                    "checkSymSelect0",
                    ArrayChecks.class,
                    1,
                    mb,
                    false
            );
            assertEquals(3, result.size());
            assertEquals(1, result.stream().filter(ps -> !(ps instanceof ExceptionPathSolution)).count());
            assertEquals(2, result.stream().filter(ps -> ps instanceof ExceptionPathSolution).count());

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

    @Test
    public void checkSymArrayStore() {
        TestUtility.getAllSolutions((mb) -> {
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(ps -> !(ps instanceof ExceptionPathSolution)));
            assertTrue(result.stream().anyMatch(ps -> ps instanceof ExceptionPathSolution));

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
            mb.setCONCOLIC(false); /// TODO currently not regarded
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
            assertEquals(6, result.size());
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

    /// TODO GENERATE_NEW_SYM_AFTER_STORE in tests

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

    @Test @Disabled
    public void checkArrayParameterAndResult() { /// TODO As soon as transformation works, check this
        TestUtility.getAllSolutions((mb) -> {
            throw new NotYetImplementedException();
        }, "checkArrayParameterAndResult");
    }
}
