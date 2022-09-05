package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SymbolicValueFactory extends AbstractValueFactory {
    // TODO Evaluate normal synchronized-statement and performance difference; also: Object
    private final Object atomicSymSintLock = new Object();
    private final Object atomicSymSdoubleLock = new Object();
    private final Object atomicSymSfloatLock = new Object();
    private final Object atomicSymSboolLock = new Object();
    private final Object atomicSymSlongLock = new Object();
    private final Object atomicSymSbyteLock = new Object();
    private final Object atomicSymSshortLock = new Object();

    private final List<Sint.SymSint> createdAtomicSymSints = new ArrayList<>();
    private final List<Sdouble.SymSdouble> createdAtomicSymSdoubles = new ArrayList<>();
    private final List<Sfloat.SymSfloat> createdAtomicSymSfloats = new ArrayList<>();
    private final List<Sbool.SymSbool> createdAtomicSymSbools = new ArrayList<>();
    private final List<Slong.SymSlong> createdAtomicSymSlongs = new ArrayList<>();
    private final List<Sshort.SymSshort> createdAtomicSymSshorts = new ArrayList<>();
    private final List<Sbyte.SymSbyte> createdAtomicSymSbytes = new ArrayList<>();

    private final Object wrappingSymSintLock = new Object();
    private final Object wrappingSymSdoubleLock = new Object();
    private final Object wrappingSymSfloatLock = new Object();
    private final Object wrappingSymSboolLock = new Object();
    private final Object wrappingSymSshortLock = new Object();
    private final Object wrappingSymSlongLock = new Object();
    private final Object wrappingSymSbyteLock = new Object();

    private final Map<NumericExpression, Sint.SymSint> createdSymSintWrappers = new HashMap<>();
    private final Map<NumericExpression, Sdouble.SymSdouble> createdSymSdoubleWrappers = new HashMap<>();
    private final Map<NumericExpression, Sfloat.SymSfloat> createdSymSfloatWrappers = new HashMap<>();
    private final Map<NumericExpression, Slong.SymSlong> createdSymSlongWrappers = new HashMap<>();
    private final Map<NumericExpression, Sshort.SymSshort> createdSymSshortWrappers = new HashMap<>();
    private final Map<NumericExpression, Sbyte.SymSbyte> createdSymSbyteWrappers = new HashMap<>();
    private final Map<Constraint, Sbool.SymSbool> createdSymSboolWrappers = new HashMap<>();

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
                createdSymSintWrappers,
                e -> (Sint.SymSint) Sint.newExpressionSymbolicSint(e),
                numericExpression,
                wrappingSymSintLock,
                optionalSintRestriction(se, config)
        );
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSdoubleWrappers,
                e -> (Sdouble.SymSdouble) Sdouble.newExpressionSymbolicSdouble(e),
                numericExpression,
                wrappingSymSdoubleLock,
                optionalSdoubleRestriction(se, config)
        );
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSfloatWrappers,
                e -> (Sfloat.SymSfloat) Sfloat.newExpressionSymbolicSfloat(e),
                numericExpression,
                wrappingSymSfloatLock,
                optionalSfloatRestriction(se, config)
        );
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSlongWrappers,
                e -> (Slong.SymSlong) Slong.newExpressionSymbolicSlong(e),
                numericExpression,
                wrappingSymSlongLock,
                optionalSlongRestriction(se, config)
        );
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSshortWrappers,
                e -> (Sshort.SymSshort) Sshort.newExpressionSymbolicSshort(e),
                numericExpression,
                wrappingSymSshortLock,
                optionalSshortRestriction(se, config)
        );
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSbyteWrappers,
                e -> (Sbyte.SymSbyte) Sbyte.newExpressionSymbolicSbyte(e),
                numericExpression,
                wrappingSymSbyteLock,
                optionalSbyteRestriction(se, config)
        );
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        return returnWrapperIfExistsElseCreate(
                createdSymSboolWrappers,
                e -> (Sbool.SymSbool) Sbool.newConstraintSbool(e),
                constraint,
                wrappingSymSboolLock,
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
            Object lock,
            Consumer<T> optionalRestriction) {
        T result;
        synchronized (lock) {
            if (created.size() > currentNumber) {
                result = created.get(currentNumber);
            } else {
                result = creationFunction.get();
                created.add(result);
            }
        }
        optionalRestriction.accept(result);
        return result;
    }

    private static <K, T extends Snumber> T returnWrapperIfExistsElseCreate(
            Map<K, T> created,
            Function<K, T> creationFunction,
            K toWrap,
            Object lock,
            Consumer<T> optionalRestriction) {
        T result;
        synchronized (lock) {
            result = (T) created.get(toWrap);
            if (result == null) {
                result = creationFunction.apply(toWrap);
                created.put(toWrap, result);
            }
        }
        optionalRestriction.accept(result);
        return result;
    }
}