package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SymbolicValueFactory extends AbstractValueFactory {
    private final StampedLock atomicSymSintLock = new StampedLock();
    private final StampedLock atomicSymSdoubleLock = new StampedLock();
    private final StampedLock atomicSymSfloatLock = new StampedLock();
    private final StampedLock atomicSymSboolLock = new StampedLock();
    private final StampedLock atomicSymSlongLock = new StampedLock();
    private final StampedLock atomicSymSbyteLock = new StampedLock();
    private final StampedLock atomicSymSshortLock = new StampedLock();

    private final List<Sint.SymSint> createdAtomicSymSints = new ArrayList<>();
    private final List<Sdouble.SymSdouble> createdAtomicSymSdoubles = new ArrayList<>();
    private final List<Sfloat.SymSfloat> createdAtomicSymSfloats = new ArrayList<>();
    private final List<Sbool.SymSbool> createdAtomicSymSbools = new ArrayList<>();
    private final List<Slong.SymSlong> createdAtomicSymSlongs = new ArrayList<>();
    private final List<Sshort.SymSshort> createdAtomicSymSshorts = new ArrayList<>();
    private final List<Sbyte.SymSbyte> createdAtomicSymSbytes = new ArrayList<>();

    private final MulibConfig config;
    SymbolicValueFactory(MulibConfig config) {
        super(config);
        this.config = config;
    }

    public static SymbolicValueFactory getInstance(MulibConfig config) {
        return new SymbolicValueFactory(config);
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf) {
        throw new NotYetImplementedException();
    }

    @Override
    protected void _addLengthLteZeroConstraint(SymbolicExecution se, Sint len) {
        Sbool inBounds = se.lte(Sint.ConcSint.ZERO, len);
        se.addNewConstraint(inBounds);
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                () -> (Sint.SymSint) Sint.newInputSymbolicSint(),
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock,
                optionalSintRestriction(se, config)
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSdoubles,
                () -> (Sdouble.SymSdouble) Sdouble.newInputSymbolicSdouble(),
                se.getNextNumberInitializedAtomicSymSdoubles(),
                atomicSymSdoubleLock,
                optionalSdoubleRestriction(se, config)
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSfloats,
                () -> (Sfloat.SymSfloat) Sfloat.newInputSymbolicSfloat(),
                se.getNextNumberInitializedAtomicSymSfloats(),
                atomicSymSfloatLock,
                optionalSfloatRestriction(se, config)
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbools,
                () -> (Sbool.SymSbool) Sbool.newInputSymbolicSbool(),
                se.getNextNumberInitializedAtomicSymSbools(),
                atomicSymSboolLock,
                (b) -> symSboolDomain(se, b)
        );
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSlongs,
                () -> (Slong.SymSlong) Slong.newInputSymbolicSlong(),
                se.getNextNumberInitializedAtomicSymSlongs(),
                atomicSymSlongLock,
                optionalSlongRestriction(se, config)
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        Sshort.SymSshort result = returnIfExistsElseCreate(
                createdAtomicSymSshorts,
                () -> (Sshort.SymSshort) Sshort.newInputSymbolicSshort(),
                se.getNextNumberInitializedAtomicSymSshorts(),
                atomicSymSshortLock,
                optionalSshortRestriction(se, config)
        );
        return result;
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbytes,
                () -> (Sbyte.SymSbyte) Sbyte.newInputSymbolicSbyte(),
                se.getNextNumberInitializedAtomicSymSbytes(),
                atomicSymSbyteLock,
                optionalSbyteRestriction(se, config)
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sint.SymSint) Sint.newExpressionSymbolicSint(e),
                numericExpression,
                optionalSintRestriction(se, config)
        );
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sdouble.SymSdouble) Sdouble.newExpressionSymbolicSdouble(e),
                numericExpression,
                optionalSdoubleRestriction(se, config)
        );
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sfloat.SymSfloat) Sfloat.newExpressionSymbolicSfloat(e),
                numericExpression,
                optionalSfloatRestriction(se, config)
        );
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Slong.SymSlong) Slong.newExpressionSymbolicSlong(e),
                numericExpression,
                optionalSlongRestriction(se, config)
        );
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sshort.SymSshort) Sshort.newExpressionSymbolicSshort(e),
                numericExpression,
                optionalSshortRestriction(se, config)
        );
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sbyte.SymSbyte) Sbyte.newExpressionSymbolicSbyte(e),
                numericExpression,
                optionalSbyteRestriction(se, config)
        );
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        return returnWrapperIfExistsElseCreate(
                e -> (Sbool.SymSbool) Sbool.newConstraintSbool(e),
                constraint,
                (b) -> symSboolDomain(se, b)
        );
    }

    @Override
    public Sint.SymSint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                () -> (Sint.SymSint) Sint.newInputSymbolicSint(),
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock,
                (newSymSint) -> cmpDomain(se, n0, n1, newSymSint)
        );
    }

    private static Sint.SymSint cmpDomain(
            SymbolicExecution se,
            final NumericExpression n0,
            final NumericExpression n1,
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

    private Sbool.SymSbool symSboolDomain(SymbolicExecution se, final Sbool.SymSbool b) {
        if (config.TREAT_BOOLEANS_AS_INTS && !se.nextIsOnKnownPath()) {
            assert !se.getCurrentChoiceOption().isEvaluated();
            Constraint eitherZeroOrOne = Or.newInstance(
                    And.newInstance(b, Eq.newInstance(b, Sint.ConcSint.ONE)),
                    And.newInstance(Not.newInstance(b), Eq.newInstance(b, Sint.ConcSint.ZERO))
            );
            se.addNewConstraint(eitherZeroOrOne);
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

    private static Consumer<Sint.SymSint> optionalSintRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSINT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSINT_LB.get(), config.SYMSINT_UB.get())
                :
                r -> {};
    }

    private static Consumer<Sdouble.SymSdouble> optionalSdoubleRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSDOUBLE_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSDOUBLE_LB.get(), config.SYMSDOUBLE_UB.get())
                :
                r -> {};
    }

    private static Consumer<Sfloat.SymSfloat> optionalSfloatRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSFLOAT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSFLOAT_LB.get(), config.SYMSFLOAT_UB.get())
                :
                r -> {};
    }

    private static Consumer<Slong.SymSlong> optionalSlongRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSLONG_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSLONG_LB.get(), config.SYMSLONG_UB.get())
                :
                r -> {};
    }

    private static Consumer<Sshort.SymSshort> optionalSshortRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSSHORT_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSSHORT_LB.get(), config.SYMSSHORT_UB.get())
                :
                r -> {};
    }

    private static Consumer<Sbyte.SymSbyte> optionalSbyteRestriction(SymbolicExecution se, MulibConfig config) {
        return config.SYMSBYTE_LB.isPresent() ?
                r -> symNumericExpressionSprimitiveDomain(se, r, config.SYMSBYTE_LB.get(), config.SYMSBYTE_UB.get())
                :
                r -> {};
    }

    private static <T extends Snumber> T returnIfExistsElseCreate(
            List<T> created,
            Supplier<T> creationFunction,
            int currentNumber,
            StampedLock lock,
            Consumer<T> optionalRestriction) {
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
        optionalRestriction.accept(result);
        return result;
    }

    private static <K, T extends Snumber> T returnWrapperIfExistsElseCreate(
            Function<K, T> creationFunction,
            K toWrap,
            Consumer<T> optionalRestriction) {
        T result = creationFunction.apply(toWrap);
        optionalRestriction.accept(result);
        return result;
    }
}