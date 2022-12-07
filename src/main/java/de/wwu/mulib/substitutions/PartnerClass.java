package de.wwu.mulib.substitutions;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformations.MulibValueCopier;

import java.util.Map;

public interface PartnerClass extends SubstitutedVar {

    byte NOT_YET_REPRESENTED_IN_SOLVER = 0;
    byte SHOULD_BE_REPRESENTED_IN_SOLVER = 1;
    byte IS_REPRESENTED_IN_SOLVER = 2;
    byte CACHE_IS_BLOCKED = 4;
    byte IS_LAZILY_INITIALIZED = 8;
    byte DEFAULT_IS_SYMBOLIC = 16;

    Object label(Object originalContainer, SolverManager solverManager);

    Object copy(MulibValueCopier mulibValueCopier);

    Class<?> __mulib__getOriginalClass();

    void __mulib__setIsNull(Sbool isNull);

    void __mulib__initializeLazyFields(SymbolicExecution se);

    Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar();

    Map<String, Class<?>> __mulib__getFieldNameToType();

    Sint __mulib__getId();

    void __mulib__prepareToRepresentSymbolically(SymbolicExecution se);

    void __mulib__prepareForAliasingAndBlockCache(SymbolicExecution se);

    Sbool __mulib__isNull();

    boolean __mulib__isRepresentedInSolver();

    boolean __mulib__shouldBeRepresentedInSolver();

    void __mulib__nullCheck();

    boolean __mulib__defaultIsSymbolic();

    void __mulib__setDefaultIsSymbolic();

    void __mulib__setAsRepresentedInSolver();

    boolean __mulib__cacheIsBlocked();

    void __mulib__blockCache();

    boolean __mulib__isLazilyInitialized();

    void __mulib__setAsLazilyInitialized();

    boolean __mulib__isSymbolicAndNotYetInitialized();
}
