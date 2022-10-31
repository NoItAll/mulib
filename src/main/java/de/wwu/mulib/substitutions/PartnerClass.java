package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformations.MulibValueCopier;

import java.util.Map;

public interface PartnerClass extends SubstitutedVar {

    Object label(Object originalContainer, SolverManager solverManager);

    Object copy(MulibValueCopier mulibValueTransformer);

    Class<?> __mulib__getOriginalClass();

    default Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar() {
        throw new MulibIllegalStateException("Should not occur");
    }

    default Map<String, Class<?>> __mulib__getFieldNameToType() {
        throw new MulibIllegalStateException("Should not occur");
    }

    byte NOT_YET_REPRESENTED_IN_SOLVER = 0;
    byte SHOULD_BE_REPRESENTED_IN_SOLVER = 1;
    byte IS_REPRESENTED_IN_SOLVER = 2;
    byte CACHE_IS_BLOCKED = 4;
    byte DEFAULT_IS_SYMBOLIC = 16;

    default Sint __mulib__getId() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default void __mulib__prepareToRepresentSymbolically(SymbolicExecution se) {
        throw new MulibIllegalStateException("Must not occur");
    }

    default Sbool __mulib__isNull() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default boolean __mulib__isRepresentedInSolver() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default boolean __mulib__shouldBeRepresentedInSolver() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default void __mulib__nullCheck() {
        if (__mulib__isNull() == Sbool.ConcSbool.TRUE) {
            throw new NullPointerException();
        } else if (__mulib__isNull() != Sbool.ConcSbool.FALSE) {
            SymbolicExecution se = SymbolicExecution.get();
            if (__mulib__isNull().boolChoice(se)) {
                __mulib__setIsNull();
                throw new NullPointerException();
            } else {
                __mulib__setIsNotNull();
            }
        }
    }

    // TODO Not needed to be publically exposed, but simpler to implement __mulib__nullCheck() this way as a default method
    default void __mulib__setIsNull() {
        throw new MulibIllegalStateException("Must not occur");
    }

    // TODO Not needed to be publically exposed, but simpler to implement __mulib__nullCheck() this way as a default method
    default void __mulib__setIsNotNull() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default boolean __mulib__defaultIsSymbolic() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default void __mulib__setAsRepresentedInSolver() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default boolean __mulib__cacheIsBlocked() {
        throw new MulibIllegalStateException("Must not occur");
    }

    default void __mulib__blockCache() {
        throw new MulibIllegalStateException("Must not occur");
    }
}
