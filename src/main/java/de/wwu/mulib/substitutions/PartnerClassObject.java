package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformations.MulibValueCopier;

import java.util.HashMap;
import java.util.Map;

/**
 * Supertype of all generated partner classes.
 * Basically is to partner classes of Mulib's Muli implementation what Java's {@link Object} is to all objects, with
 * the exception that interfaces cannot extend this. Thus, interfaces must implement {@link PartnerClass}.
 * Contains the metadata relevant for the Mulib backend.
 * Classes implementing this method must implement the following methods:
 * {@link PartnerClassObject#__mulib__blockCacheInPartnerClassFields()} : should include a call to super.__mulib__blockCacheInPartnerClassFields()
 * {@link PartnerClassObject#__mulib__initializeLazyFields(SymbolicExecution)} : should include a call to super.__mulib__initializeLazyFields(...)
 * {@link PartnerClassObject#copy(MulibValueCopier)} : should include a call to the super-copy constructor
 * {@link PartnerClassObject#__mulib__getFieldNameToSubstitutedVar()} : should include a call to
 * super.__mulib__getFieldNameToSubstitutedVar() for also retrieving those fields
 * {@link PartnerClassObject#__mulib__getOriginalClass()}
 * {@link PartnerClassObject#PartnerClassObject(PartnerClassObject, MulibValueCopier)} : An own constructor should be created calling super
 */
public class PartnerClassObject implements PartnerClass {
    /**
     * The identifier.
     * If this partner class object is not represented in the solver, this is null.
     */
    protected Sint id = null;
    /**
     * A byte containing information on the various states of this partner class object.
     */
    protected byte representationState;
    /**
     * Whether the partner class object can be null.
     * This is mutable since we change it after knowing the result of calling
     * {@link PartnerClassObject#__mulib__nullCheck()}.
     */
    protected Sbool isNull;

    /**
     * Constructor for creating a new instance.
     * {@link PartnerClassObject#isNull} is set to {@link de.wwu.mulib.substitutions.primitives.Sbool.ConcSbool#FALSE}.
     * {@link PartnerClassObject#representationState} is set to {@link PartnerClass#NOT_YET_REPRESENTED_IN_SOLVER}.
     */
    protected PartnerClassObject() {
        this.isNull = Sbool.ConcSbool.FALSE;
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
    }

    /**
     * Constructor for copying.
     * The metadata, such as the identifier, the representation state, as well as whether 'toCopy' is null is copied
     * and the copy is registered in the copier.
     * @param toCopy To-copy
     * @param mvc The copier used for this copy procedure
     */
    protected PartnerClassObject(PartnerClassObject toCopy, MulibValueCopier mvc) {
        mvc.registerCopy(toCopy, this);
        this.representationState = toCopy.representationState;
        this.id = toCopy.id;
        this.isNull = toCopy.isNull;
    }

    @Override
    public final void __mulib__setIsRemembered() {
        this.representationState |= IS_REMEMBERED;
    }

    @Override
    public final boolean __mulib__isRemembered() {
        return (this.representationState & IS_REMEMBERED) != 0;
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
        if (!__mulib__cacheIsBlocked()) {
            representationState |= CACHE_IS_BLOCKED;
            this.__mulib__blockCacheInPartnerClassFields();
        }
    }

    /**
     * Blocks the cache of all reference-typed classes in all fields
     */
    protected void __mulib__blockCacheInPartnerClassFields() {
    }

    @Override
    public void __mulib__initializeLazyFields(SymbolicExecution se) {
    }

    @Override
    public Object label(Object originalContainer, SolverManager solverManager) {
        throw new MulibIllegalStateException("Must be overridden by implementing subclasses");
    }

    @Override
    public Object copy(MulibValueCopier mulibValueCopier) {
        return new PartnerClassObject(this, mulibValueCopier);
    }

    @Override
    public Class<?> __mulib__getOriginalClass() {
        return Object.class;
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
        __mulib__initializeId(Sint.concSint(se.getNextNumberInitializedSymObject()));
        this.representationState |= SHOULD_BE_REPRESENTED_IN_SOLVER;
    }

    @Override
    public final void __mulib__prepareForAliasingAndBlockCache(SymbolicExecution se) {
        __mulib__initializeId(se.symSint());
        this.representationState |= SHOULD_BE_REPRESENTED_IN_SOLVER;
        __mulib__blockCache();
    }

    @Override
    public Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar() {
        return new HashMap<>();
    }

    private void __mulib__initializeId(Sint id) {
        if (this.id != null) {
            throw new MulibRuntimeException("Must not set already set id");
        }
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
        assert !(this instanceof Sarray);
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
    public final boolean __mulib__isSymbolicAndNotYetLazilyInitialized() {
        return __mulib__defaultIsSymbolic() && !__mulib__isLazilyInitialized();
    }

    @Override
    public final boolean __mulib__isToBeLazilyInitialized() {
        return __mulib__defaultIsSymbolic() && !__mulib__isLazilyInitialized() && !__mulib__isRepresentedInSolver();
    }

}
