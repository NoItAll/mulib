package de.wwu.mulib.search.values;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class SymbolicValueFactory implements ValueFactory {

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
    private final Map<Constraint, Sbool> createdSymSboolWrappers = new HashMap<>();



    private <T> T returnIfExistsElseCreate(List<T> created, Supplier<T> creationFunction, int currentNumber, ReadWriteLock lock) {
        lock.readLock().lock();
        if (created.size() > currentNumber) {
            T result = created.get(currentNumber);
            lock.readLock().unlock();
            return result;
        } else {
            lock.readLock().unlock();
            lock.writeLock().lock();
            T result;
            // Re-check time between acquisition
            if (created.size() > currentNumber) {
                result = created.get(currentNumber);
                lock.writeLock().unlock();
                return result;
            }
            result = creationFunction.get();
            created.add(result);
            lock.writeLock().unlock();
            return result;
        }
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSints,
                Sint::newInputSymbolicSint,
                se.getNextNumberInitializedAtomicSymSints(),
                atomicSymSintLock
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSdoubles,
                Sdouble::newInputSymbolicSdouble,
                se.getNextNumberInitializedAtomicSymSdoubles(),
                atomicSymSdoubleLock
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSfloats,
                Sfloat::newInputSymbolicSfloat,
                se.getNextNumberInitializedAtomicSymSfloats(),
                atomicSymSfloatLock
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbools,
                Sbool::newInputSymbolicSbool,
                se.getNextNumberInitializedAtomicSymSbools(),
                atomicSymSboolLock
        );
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSlongs,
                Slong::newInputSymbolicSlong,
                se.getNextNumberInitializedAtomicSymSlongs(),
                atomicSymSlongLock
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSshorts,
                Sshort::newInputSymbolicSshort,
                se.getNextNumberInitializedAtomicSymSshorts(),
                atomicSymSshortLock
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return returnIfExistsElseCreate(
                createdAtomicSymSbytes,
                Sbyte::newInputSymbolicSbyte,
                se.getNextNumberInitializedAtomicSymSbytes(),
                atomicSymSbyteLock
        );
    }

    private <K, T> T returnWrapperIfExistsElseCreate(Map<K, T> created, Function<K, T> creationFunction, K toWrap, ReadWriteLock lock) {
        lock.readLock().lock();
        T result = (T) created.get(toWrap);
        lock.readLock().unlock();
        if (result == null) {
            lock.writeLock().lock();
            result = (T) created.get(toWrap);
            // Re-check time between acquisition
            if (result != null) {
                lock.writeLock().unlock();
                return result;
            }
            result = creationFunction.apply(toWrap);
            created.put(toWrap, result);
            lock.writeLock().unlock();
        }
        return result;
    }

    @Override
    public Sint.SymSint wrappingSymSint(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSintWrappers,
                Sint.SymSint::newExpressionSymbolicSint,
                numericExpression,
                wrappingSymSintLock
        );
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSdoubleWrappers,
                Sdouble::newExpressionSymbolicSdouble,
                numericExpression,
                wrappingSymSdoubleLock
        );
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSfloatWrappers,
                Sfloat::newExpressionSymbolicSfloat,
                numericExpression,
                wrappingSymSfloatLock
        );
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSlongWrappers,
                Slong::newExpressionSymbolicSlong,
                numericExpression,
                wrappingSymSlongLock
        );
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSshortWrappers,
                Sshort::newExpressionSymbolicSshort,
                numericExpression,
                wrappingSymSshortLock
        );
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(NumericExpression numericExpression) {
        return returnWrapperIfExistsElseCreate(
                createdSymSbyteWrappers,
                Sbyte::newExpressionSymbolicSbyte,
                numericExpression,
                wrappingSymSbyteLock
        );
    }

    @Override
    public Sbool wrappingSymSbool(Constraint constraint) {
        return returnWrapperIfExistsElseCreate(
                createdSymSboolWrappers,
                Sbool::newConstraintSbool,
                constraint,
                wrappingSymSboolLock
        );
    }

    @Override
    public Sint.ConcSint concSint(int i) {
        return Sint.newConcSint(i);
    }

    @Override
    public Sdouble.ConcSdouble concSdouble(double d) {
        return Sdouble.newConcSdouble(d);
    }

    @Override
    public Sfloat.ConcSfloat concSfloat(float f) {
        return Sfloat.newConcSfloat(f);
    }

    @Override
    public Sbool.ConcSbool concSbool(boolean b) {
        return Sbool.newConcSbool(b);
    }

    @Override
    public Slong.ConcSlong concSlong(long l) {
        return Slong.newConcSlong(l);
    }

    @Override
    public Sshort.ConcSshort concSshort(short s) {
        return Sshort.newConcSshort(s);
    }

    @Override
    public Sbyte.ConcSbyte concSbyte(byte b) {
        return Sbyte.newConcSbyte(b);
    }
}
