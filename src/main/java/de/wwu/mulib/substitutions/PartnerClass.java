package de.wwu.mulib.substitutions;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformations.MulibValueCopier;

import java.util.Map;

/**
 * Interface representing a Sarray or another partner class.
 * It is necessary to define it as an interface since these methods should also be addressable by interfaces types.
 * Generated partner classes (not interfaces!) should instead (implicitly) extend {@link PartnerClassObject} that also
 * documents the methods that still need to be overridden.
 */
public interface PartnerClass extends SubstitutedVar {

    /**
     * State if object is not represented for/in the solver
     */
    byte NOT_YET_REPRESENTED_IN_SOLVER = 0;
    /**
     * State if object should be represented for/in the solver. Typically is set just before representing it.
     */
    byte SHOULD_BE_REPRESENTED_IN_SOLVER = 1;
    /**
     * State if object has been represented for/in the solver.
     */
    byte IS_REPRESENTED_IN_SOLVER = 2;
    /**
     * State if object cannot access its field, or, in the case of Sarrays, it's array-content. Instead,
     * the constraint solver must be contacted to retrieve a valid value
     */
    byte CACHE_IS_BLOCKED = 4;
    /**
     * State if this object is already lazily initialized
     */
    byte IS_LAZILY_INITIALIZED = 8;
    /**
     * State if this object was created symbolically and potentially needs to be initialized lazily
     */
    byte DEFAULT_IS_SYMBOLIC = 16;
    /**
     * State if object is remembered
     */
    byte IS_REMEMBERED = 32;

    /**
     * Labels the object
     * @param originalContainer An empty object the values of which shall be set
     * @param solverManager The solver manager
     * @return The labeled object
     */
    // Deprecated until remember support is fully consolidated
    @Deprecated
    Object label(Object originalContainer, SolverManager solverManager);

    /**
     * @param mulibValueCopier The value copier used for copying.
     * @return A copy of this object. Is typically done when remembering the non-lazily initialized object, or when
     * invoking the search region with some arguments.
     */
    Object copy(MulibValueCopier mulibValueCopier);

    /**
     * @return The class this class is a partner class of
     */
    Class<?> __mulib__getOriginalClass();

    /**
     * Mutates the state of this object so that it is assured that {@link PartnerClass#__mulib__isRemembered()}
     * returns true
     */
    void __mulib__setIsRemembered();

    /**
     * @return true, if this object is remembered via a {@link de.wwu.mulib.constraints.PartnerClassObjectRememberConstraint},
     * else false.
     */
    boolean __mulib__isRemembered();

    /**
     * Sets the value of whether this object is null to the given value
     * @param isNull The null value
     */
    void __mulib__setIsNull(Sbool isNull);

    /**
     * Initializes the fields of this object
     * @param se The current instance of {@link SymbolicExecution} for this execution run
     */
    void __mulib__initializeLazyFields(SymbolicExecution se);

    /**
     * Implementing classes should also add the result of super.__mulib__getFieldNameToSubstitutedVar() to
     * the resulting map.
     * @return A map of (packageName.className.fieldName, value)-pairs
     */
    Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar();

    /**
     * @return The identifier, if any of this partner class object
     */
    Sint __mulib__getId();

    /**
     * Prepares the object to be represented in a constraint solver. The object is represented as a new object, not
     * as a potential symbolic alias of another object.
     * It sets the identifier to a concrete value, and changes the state so that
     * {@link PartnerClass#__mulib__shouldBeRepresentedInSolver()} returns true
     * @param se The current instance of {@link SymbolicExecution} for this execution run
     */
    void __mulib__prepareToRepresentSymbolically(SymbolicExecution se);

    /**
     * Prepares the object to be represented in a constraint solver. The object is represented as a new object or
     * as a potential symbolic alias of another object.
     * It sets the identifier to a symbolic value, and changes the state so that
     * {@link PartnerClass#__mulib__shouldBeRepresentedInSolver()} returns true.
     * Furthermore, the state is altered so that {@link PartnerClass#__mulib__cacheIsBlocked()} returns true
     * @param se The current instance of {@link SymbolicExecution} for this execution run
     */
    void __mulib__prepareForAliasingAndBlockCache(SymbolicExecution se);

    /**
     * @return Whether this object is null or not
     */
    Sbool __mulib__isNull();

    /**
     * @return true, if this object is represented for/in the constraint solver, else false
     */
    boolean __mulib__isRepresentedInSolver();

    /**
     * This method is typically called before representing the object for/in the constraint solver
     * @return true, if this object should be represented for/in the constraint solver, else false.
     */
    boolean __mulib__shouldBeRepresentedInSolver();

    /**
     * Throws a {@link NullPointerException} if {@link PartnerClass#__mulib__isNull()} can be true
     */
    void __mulib__nullCheck();

    /**
     * @return true, if uninitialized objects retrieved from fields/the array's content are symbolic per default, else false
     */
    boolean __mulib__defaultIsSymbolic();

    /**
     * Changes the state so that {@link PartnerClass#__mulib__defaultIsSymbolic()} returns true.
     * Should be used with care only when initializing an object. It is not possible to change this setting
     * at any time.
     */
    void __mulib__setDefaultIsSymbolic();

    /**
     * Changes the state so that {@link PartnerClass#__mulib__isRepresentedInSolver()} ()} returns true
     */
    void __mulib__setAsRepresentedInSolver();

    /**
     * @return true if the fields of this object or the content of the array should not be accessed to retrieve a value,
     * else false
     */
    boolean __mulib__cacheIsBlocked();

    /**
     * Changes the state so that {@link PartnerClass#__mulib__blockCache()} ()} returns true
     */
    void __mulib__blockCache();

    /**
     * @return true if this object has already been lazily initialized, else false
     */
    boolean __mulib__isLazilyInitialized();

    /**
     * Changes the state so that {@link PartnerClass#__mulib__isLazilyInitialized()} ()} returns true
     */
    void __mulib__setAsLazilyInitialized();

    /**
     * @return true if this object is a candidate for symbolic representation and has not yet been lazily initialized,
     * else false
     */
    boolean __mulib__isSymbolicAndNotYetLazilyInitialized();

    /**
     * @return true if this object is a candidate for symbolic representation and has not yet been lazily initialized and
     * also was not represented in the solver, else false
     */
    boolean __mulib__isToBeLazilyInitialized();
}
