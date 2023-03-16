package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public abstract class AbstractPartnerClass implements PartnerClass {
    protected Sint id;
    protected byte representationState;
    protected Sbool isNull;

    protected AbstractPartnerClass() {
        this.isNull = Sbool.ConcSbool.FALSE;
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
    }

    @Override
    public final void __mulib__setIsNamed() {
        this.representationState |= IS_NAMED;
    }

    @Override
    public final boolean __mulib__isNamed() {
        return (this.representationState & IS_NAMED) != 0;
    }

    @Override
    public final void __mulib__setDefaultIsSymbolic() {
        this.representationState |= DEFAULT_IS_SYMBOLIC;
    }

    @Override
    public final boolean __mulib__shouldBeRepresentedInSolver() {
        return (representationState & SHOULD_BE_REPRESENTED_IN_SOLVER) != 0;
    }

    @Override
    public final boolean __mulib__isRepresentedInSolver() {
        assert (representationState & IS_REPRESENTED_IN_SOLVER) == 0 || __mulib__shouldBeRepresentedInSolver();
        return (representationState & IS_REPRESENTED_IN_SOLVER) != 0;
    }

    @Override
    public final void __mulib__setAsRepresentedInSolver() {
        representationState |= IS_REPRESENTED_IN_SOLVER;
    }

    @Override
    public void __mulib__blockCache() {
        representationState |= CACHE_IS_BLOCKED;
    }

    @Override
    public final boolean __mulib__cacheIsBlocked() {
        return (representationState & CACHE_IS_BLOCKED) != 0;
    }

    @Override
    public final boolean __mulib__defaultIsSymbolic() {
        return (representationState & DEFAULT_IS_SYMBOLIC) != 0;
    }

    @Override
    public void __mulib__prepareToRepresentSymbolically(SymbolicExecution se) {
        __mulib___initializeId(se.concSint(se.getNextNumberInitializedSymObject()));
    }

    @Override
    public final void __mulib__prepareForAliasingAndBlockCache(SymbolicExecution se) {
        __mulib___initializeId(se.symSint());
        __mulib__blockCache();
    }

    protected void __mulib___initializeId(Sint id) {
        if (this.id != null) {
            throw new MulibRuntimeException("Must not set already set id");
        }

        this.representationState |= SHOULD_BE_REPRESENTED_IN_SOLVER;
        this.id = id;
    }

    @Override
    public final void __mulib__setIsNull(Sbool b) {
        this.isNull = b;
    }

    @Override
    public final void __mulib__nullCheck() {
        if (__mulib__isNull() == Sbool.ConcSbool.TRUE) {
            throw new NullPointerException();
        } else if (__mulib__isNull() != Sbool.ConcSbool.FALSE) {
            SymbolicExecution se = SymbolicExecution.get();
            if (__mulib__isNull().boolChoice(se)) {
                this.isNull = Sbool.ConcSbool.TRUE;
                throw new NullPointerException();
            } else {
                this.isNull = Sbool.ConcSbool.FALSE;
            }
        }
    }

    @Override
    public final boolean __mulib__isLazilyInitialized() {
        return (representationState & IS_LAZILY_INITIALIZED) != 0;
    }

    @Override
    public final void __mulib__setAsLazilyInitialized() {
        representationState |= IS_LAZILY_INITIALIZED;
    }

    @Override
    public final Sint __mulib__getId() {
        return id;
    }

    @Override
    public final Sbool __mulib__isNull() {
        return isNull;
    }

    @Override
    public final boolean __mulib__isSymbolicAndNotYetInitialized() {
        return __mulib__defaultIsSymbolic() && !__mulib__isLazilyInitialized();
    }

}
