package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SymbolicValueFactory extends AbstractValueFactory {
    // TODO Evaluate normal synchronized-statement and performance difference
    private final ReadWriteLock atomicSymSintLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSdoubleLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSfloatLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSboolLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSlongLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSbyteLock = new ReentrantReadWriteLock();
    private final ReadWriteLock atomicSymSshortLock = new ReentrantReadWriteLock();

    private final List<Sint.SymSint> createdAtomicSymSints = new ArrayList<>();
    private final List<Sdouble.SymSdouble> createdAtomicSymSdoubles = new ArrayList<>();
    private final List<Sfloat.SymSfloat> createdAtomicSymSfloats = new ArrayList<>();
    private final List<Sbool.SymSbool> createdAtomicSymSbools = new ArrayList<>();
    private final List<Slong.SymSlong> createdAtomicSymSlongs = new ArrayList<>();
    private final List<Sshort.SymSshort> createdAtomicSymSshorts = new ArrayList<>();
    private final List<Sbyte.SymSbyte> createdAtomicSymSbytes = new ArrayList<>();

    private final ReadWriteLock wrappingSymSintLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSdoubleLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSfloatLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSboolLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSshortLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSlongLock = new ReentrantReadWriteLock();
    private final ReadWriteLock wrappingSymSbyteLock = new ReentrantReadWriteLock();

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
    public Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SintSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SdoubleSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SfloatSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SlongSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SshortSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SbyteSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SboolSarray(len, se, freeElements);
    }

    @Override
    public Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.PartnerClassSarray(clazz, len, se, freeElements);
    }

    @Override
    public Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SarraySarray(len, se, freeElements, clazz);
    }

    @Override
    public Sarray.SarraySarray sarrarSarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
        return new Sarray.SarraySarray(lengths, se, clazz);
    }

    @Override
    public <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf) {
        throw new NotYetImplementedException();
    }

    private void restrictLength(SymbolicExecution se, Sint len) {
        if (len instanceof ConcSnumber) {
            if (((ConcSnumber) len).intVal() < 0) {
                throw new NegativeArraySizeException();
            }
        } else if (throwExceptionOnOOB) {
            Constraint outOfBounds = se.gte(Sint.ConcSint.ZERO, len);
            if (se.boolChoice(outOfBounds)) {
                throw new NegativeArraySizeException();
            }
        } else if (!se.nextIsOnKnownPath()) {
            Constraint inBounds = se.lte(Sint.ConcSint.ZERO, len);
            se.addNewConstraint(inBounds);
        }
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
            ReadWriteLock lock,
            Consumer<T> optionalRestriction) {
        lock.readLock().lock();
        T result;
        if (created.size() > currentNumber) {
            result = created.get(currentNumber);
            lock.readLock().unlock();
        } else {
            lock.readLock().unlock();
            lock.writeLock().lock();
            // Re-check time between acquisition
            if (created.size() > currentNumber) {
                result = created.get(currentNumber);
                lock.writeLock().unlock();
            } else {
                result = creationFunction.get();
                created.add(result);
                lock.writeLock().unlock();
            }
        }
        optionalRestriction.accept(result);
        return result;
    }

    private static <K, T extends Snumber> T returnWrapperIfExistsElseCreate(
            Map<K, T> created,
            Function<K, T> creationFunction,
            K toWrap,
            ReadWriteLock lock,
            Consumer<T> optionalRestriction) {
        lock.readLock().lock();
        T result = (T) created.get(toWrap);
        lock.readLock().unlock();
        if (result == null) {
            lock.writeLock().lock();
            result = (T) created.get(toWrap);
            // Re-check time between acquisition
            if (result != null) {
                lock.writeLock().unlock();
            } else {
                result = creationFunction.apply(toWrap);
                created.put(toWrap, result);
                lock.writeLock().unlock();
            }
        }
        optionalRestriction.accept(result);
        return result;
    }
}
