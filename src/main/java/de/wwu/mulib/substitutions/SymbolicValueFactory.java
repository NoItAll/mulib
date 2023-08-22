package de.wwu.mulib.substitutions;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread-safe value factory for symbolic values.
 * Caches all {@link SymSprimitiveLeaf}s created by it for reuse. The counters in {@link SymbolicExecution} are used
 * to determine the cached {@link SymSprimitiveLeaf}s of the respective type to use next.
 * Due to mutability, {@link de.wwu.mulib.substitutions.PartnerClass} objects are not cached and instead use the number
 * provided by this factory to set the identifier {@link PartnerClass#__mulib__getId()}.
 */
public class SymbolicValueFactory extends AbstractValueFactory {
    private final StampedLock atomicSymSintLock = new StampedLock();
    private final StampedLock atomicSymSdoubleLock = new StampedLock();
    private final StampedLock atomicSymSfloatLock = new StampedLock();
    private final StampedLock atomicSymSboolLock = new StampedLock();
    private final StampedLock atomicSymSlongLock = new StampedLock();
    private final StampedLock atomicSymSbyteLock = new StampedLock();
    private final StampedLock atomicSymSshortLock = new StampedLock();
    private final StampedLock atomicSymScharLock = new StampedLock();

    private final List<Sint.SymSint> createdAtomicSymSints = new ArrayList<>();
    private final List<Sdouble.SymSdouble> createdAtomicSymSdoubles = new ArrayList<>();
    private final List<Sfloat.SymSfloat> createdAtomicSymSfloats = new ArrayList<>();
    private final List<Sbool.SymSbool> createdAtomicSymSbools = new ArrayList<>();
    private final List<Slong.SymSlong> createdAtomicSymSlongs = new ArrayList<>();
    private final List<Sshort.SymSshort> createdAtomicSymSshorts = new ArrayList<>();
    private final List<Sbyte.SymSbyte> createdAtomicSymSbytes = new ArrayList<>();
    private final List<Schar.SymSchar> createdAtomicSymSchars = new ArrayList<>();

    private final MulibConfig config;

    SymbolicValueFactory(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        super(config, arrayTypesToSpecializedSarrayClass);
        this.config = config;
    }

    public static SymbolicValueFactory getInstance(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        return new SymbolicValueFactory(config, arrayTypesToSpecializedSarrayClass);
    }

    @Override
    protected void _addZeroLteLengthConstraint(SymbolicExecution se, Sint len) {
        Sbool inBounds = se.lte(Sint.ConcSint.ZERO, len);
        se.addNewConstraint(inBounds);
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                Sint::newInputSymbolicSint,
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock,
                optionalSintRestriction(se, config)
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSdoubles,
                Sdouble::newInputSymbolicSdouble,
                se.getNextNumberInitializedAtomicSymSdoubles(),
                atomicSymSdoubleLock,
                optionalSdoubleRestriction(se, config)
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSfloats,
                Sfloat::newInputSymbolicSfloat,
                se.getNextNumberInitializedAtomicSymSfloats(),
                atomicSymSfloatLock,
                optionalSfloatRestriction(se, config)
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbools,
                Sbool::newInputSymbolicSbool,
                se.getNextNumberInitializedAtomicSymSbools(),
                atomicSymSboolLock,
                (b) -> symSboolDomain(se, b)
        );
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSlongs,
                Slong::newInputSymbolicSlong,
                se.getNextNumberInitializedAtomicSymSlongs(),
                atomicSymSlongLock,
                optionalSlongRestriction(se, config)
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSshorts,
                Sshort::newInputSymbolicSshort,
                se.getNextNumberInitializedAtomicSymSshorts(),
                atomicSymSshortLock,
                optionalSshortRestriction(se, config)
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbytes,
                Sbyte::newInputSymbolicSbyte,
                se.getNextNumberInitializedAtomicSymSbytes(),
                atomicSymSbyteLock,
                optionalSbyteRestriction(se, config)
        );
    }

    @Override
    public Schar.SymSchar symSchar(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSchars,
                Schar::newInputSymbolicSchar,
                se.getNextNumberInitializedAtomicSymSchars(),
                atomicSymScharLock,
                optionalScharRestriction(se, config)
        );
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se, Sint lb, Sint ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                Sint::newInputSymbolicSint,
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se, Sdouble lb, Sdouble ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSdoubles,
                Sdouble::newInputSymbolicSdouble,
                se.getNextNumberInitializedAtomicSymSdoubles(),
                atomicSymSdoubleLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSfloats,
                Sfloat::newInputSymbolicSfloat,
                se.getNextNumberInitializedAtomicSymSfloats(),
                atomicSymSfloatLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se, Slong lb, Slong ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSlongs,
                Slong::newInputSymbolicSlong,
                se.getNextNumberInitializedAtomicSymSlongs(),
                atomicSymSlongLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSshorts,
                Sshort::newInputSymbolicSshort,
                se.getNextNumberInitializedAtomicSymSshorts(),
                atomicSymSshortLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbytes,
                Sbyte::newInputSymbolicSbyte,
                se.getNextNumberInitializedAtomicSymSbytes(),
                atomicSymSbyteLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Schar.SymSchar symSchar(SymbolicExecution se, Schar lb, Schar ub) {
        return returnIfExistsElseCreate(
                createdAtomicSymSchars,
                Schar::newInputSymbolicSchar,
                se.getNextNumberInitializedAtomicSymSchars(),
                atomicSymScharLock,
                i -> symNumericExpressionSprimitiveDomain(se, i, lb, ub)
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Sint::newExpressionSymbolicSint,
                numericalExpression,
                optionalSintRestriction(se, config)
        );
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Sdouble::newExpressionSymbolicSdouble,
                numericalExpression,
                optionalSdoubleRestriction(se, config)
        );
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Sfloat::newExpressionSymbolicSfloat,
                numericalExpression,
                optionalSfloatRestriction(se, config)
        );
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Slong::newExpressionSymbolicSlong,
                numericalExpression,
                optionalSlongRestriction(se, config)
        );
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Sshort::newExpressionSymbolicSshort,
                numericalExpression,
                optionalSshortRestriction(se, config)
        );
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Sbyte::newExpressionSymbolicSbyte,
                numericalExpression,
                optionalSbyteRestriction(se, config)
        );
    }

    @Override
    public Schar wrappingSymSchar(SymbolicExecution se, NumericalExpression numericalExpression) {
        return returnWrapperIfExistsElseCreate(
                Schar::newExpressionSymbolicSchar,
                numericalExpression,
                optionalScharRestriction(se, config)
        );
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        return returnWrapperIfExistsElseCreate(
                Sbool::newConstraintSbool,
                constraint,
                (b) -> symSboolDomain(se, b)
        );
    }

    @Override
    public Sint.SymSint cmp(SymbolicExecution se, NumericalExpression n0, NumericalExpression n1) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                Sint::newInputSymbolicSint,
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock,
                (newSymSint) -> cmpDomain(se, n0, n1, newSymSint)
        );
    }

    private static Sint.SymSint cmpDomain(
            SymbolicExecution se,
            final NumericalExpression n0,
            final NumericalExpression n1,
            final Sint.SymSint toRestrict) {
        if (!se.nextIsOnKnownPath()) {
            assert !se.getCurrentChoiceOption().isEvaluated();
            Constraint zeroOneOrMinusOne = Or.newInstance(
                    And.newInstance(Lt.newInstance(n1, n0), Eq.newInstance(toRestrict, Sint.ConcSint.ONE)),
                    And.newInstance(Eq.newInstance(n0, n1), Eq.newInstance(toRestrict, Sint.ConcSint.ZERO)),
                    And.newInstance(Lt.newInstance(n0, n1), Eq.newInstance(toRestrict, Sint.ConcSint.MINUS_ONE))
            );
            se.addNewConstraint(zeroOneOrMinusOne);
        }
        return toRestrict;
    }


    // TODO The following is needed for temporarily fixing concolic-execution + treat_sbools_as_sints
    //  It will become of interest in general if we want to implement a non-direct-substitution variant for creating constraints
    //  The issue is that the current concolic execution-related factories make use of some SymbolicExecution-methods
    //  that trigger the trail. For instance it might happen that se.and is called while being protected via !se.nextIsOnKnownPath().
    //  This will lead to confusion since the trail is being changed
    private final StampedLock zeroOrOneDelegateLock = new StampedLock();
    private final Map<Sbool.SymSbool, Sbool.SymSbool> createdZeroOrOneDelegates = new HashMap<>();
    private Sbool.SymSbool symSboolDomain(SymbolicExecution se, final Sbool.SymSbool b) {
        if (config.VALS_TREAT_BOOLEANS_AS_INTS) {
            Sbool.SymSbool leaf;
            if (b instanceof Sbool.SymSboolLeaf) {
                leaf = b;
            } else {
                long stamp = zeroOrOneDelegateLock.readLock();
                leaf = createdZeroOrOneDelegates.get(b);
                zeroOrOneDelegateLock.unlockRead(stamp);
                if (leaf == null) {
                    leaf = Sbool.newInputSymbolicSbool();
                    stamp = zeroOrOneDelegateLock.writeLock();
                    createdZeroOrOneDelegates.put(b, leaf);
                    zeroOrOneDelegateLock.unlockWrite(stamp);
                }
            }
            if (!se.nextIsOnKnownPath()) {
                assert !se.getCurrentChoiceOption().isEvaluated();
                Constraint eitherZeroOrOne;
                if (leaf == b) {
                    eitherZeroOrOne = Or.newInstance(
                            And.newInstance(b, Eq.newInstance(b, Sint.ConcSint.ONE)),
                            And.newInstance(Not.newInstance(b), Eq.newInstance(b, Sint.ConcSint.ZERO))
                    );
                } else {
                    eitherZeroOrOne = Or.newInstance(
                            And.newInstance(b, leaf, Eq.newInstance(leaf, Sint.ConcSint.ONE)),
                            And.newInstance(Not.newInstance(b), Not.newInstance(leaf), Eq.newInstance(leaf, Sint.ConcSint.ZERO))
                    );
                }
                se.addNewConstraint(eitherZeroOrOne);
            }
            return leaf;
        }
        return b;
    }

    private static <T extends Snumber> T symNumericExpressionSprimitiveDomain(
            SymbolicExecution se,
            final T i,
            Snumber lb,
            Snumber ub) {
        if (!se.nextIsOnKnownPath()) {
            assert !se.getCurrentChoiceOption().isEvaluated();
            Constraint lowerAndUpperBound = And.newInstance(
                    Lte.newInstance(i, ub),
                    Lte.newInstance(lb, i));
            se.addNewConstraint(lowerAndUpperBound);
        }
        return i;
    }

    private static Function<Sint.SymSint, Sint.SymSint> optionalSintRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSINT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSINT_LB.get(), config.VALS_SYMSINT_UB.get())
                :
                r -> r;
    }

    private static Function<Sdouble.SymSdouble, Sdouble.SymSdouble> optionalSdoubleRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSDOUBLE_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSDOUBLE_LB.get(), config.VALS_SYMSDOUBLE_UB.get())
                :
                r -> r;
    }

    private static Function<Sfloat.SymSfloat, Sfloat.SymSfloat> optionalSfloatRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSFLOAT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSFLOAT_LB.get(), config.VALS_SYMSFLOAT_UB.get())
                :
                r -> r;
    }

    private static Function<Slong.SymSlong, Slong.SymSlong> optionalSlongRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSLONG_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSLONG_LB.get(), config.VALS_SYMSLONG_UB.get())
                :
                r -> r;
    }

    private static Function<Sshort.SymSshort, Sshort.SymSshort> optionalSshortRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSSHORT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSSHORT_LB.get(), config.VALS_SYMSSHORT_UB.get())
                :
                r -> r;
    }

    private static Function<Schar.SymSchar, Schar.SymSchar> optionalScharRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSCHAR_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSCHAR_LB.get(), config.VALS_SYMSCHAR_UB.get())
                :
                r -> r;
    }

    private static Function<Sbyte.SymSbyte, Sbyte.SymSbyte> optionalSbyteRestriction(SymbolicExecution se, MulibConfig config) {
        return config.VALS_SYMSBYTE_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.VALS_SYMSBYTE_LB.get(), config.VALS_SYMSBYTE_UB.get())
                :
                r -> r;
    }

    private static <T extends Snumber> T returnIfExistsElseCreate(
            List<T> created,
            Supplier<T> creationFunction,
            int currentNumber,
            StampedLock lock,
            Function<T, T> optionalRestriction) {
        long stamp = lock.readLock();
        T result;
        if (created.size() > currentNumber) {
            result = created.get(currentNumber);
            lock.unlockRead(stamp);
        } else {
            lock.unlockRead(stamp);
            stamp = lock.writeLock();
            // Re-check time between acquisition
            if (created.size() > currentNumber) {
                result = created.get(currentNumber);
                lock.unlockWrite(stamp);
            } else {
                result = creationFunction.get();
                created.add(result);
                lock.unlockWrite(stamp);
            }
        }
        return optionalRestriction.apply(result);
    }

    private static <K, T extends Snumber> T returnWrapperIfExistsElseCreate(
            Function<K, T> creationFunction,
            K toWrap,
            Function<T, T> optionalRestriction) {
        T result = creationFunction.apply(toWrap);
        return optionalRestriction.apply(result);
    }
}