package de.wwu.mulib.search.executors;

import de.wwu.mulib.throwables.MulibIllegalStateException;
import de.wwu.mulib.expressions.ConcolicNumericalContainer;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class capturing information on aliasing for objects created with {@link de.wwu.mulib.Mulib#freeObject(Class)} etc.
 * This class is implemented using {@link ThreadLocal}s rather than a single version for each {@link AbstractMulibExecutor}
 * as it is not expected that aliasing is used in many scenarios.
 * To enable aliasing in the initialization of new symbolic objects, {@link de.wwu.mulib.MulibConfig#FREE_INIT_ALIASING_FOR_FREE_OBJECTS}
 * must be enabled.
 * TODO This does not yet regard subtyping
 */
public final class AliasingInformation {
    private static final ThreadLocal<AliasingInformation> currentAliasingInformation = new ThreadLocal<>();
    private final Map<Class<?>, Collection<PartnerClass>> aliasingMap = new HashMap<>();

    /**
     * For the current thread (and thus for each {@link SymbolicExecution} in separate), returns the potential
     * aliasing targets, i.e., those instances that have been initialized symbolically beforehand.
     * @param c The class for which to retrieve aliasing information for the current run of {@link SymbolicExecution}
     * @return The aliasing targets
     */
    public static Collection<PartnerClass> getAliasingTargetsForClass(Class<?> c) {
        Map<Class<?>, Collection<PartnerClass>> map = getAliasingInfo().aliasingMap;
        return map.entrySet().stream()
                .filter(e -> c.isAssignableFrom(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());
    }

    /**
     * For the current thread (and thus for each {@link SymbolicExecution} in separate), returns the potential
     * aliasing targets' identifiers, i.e., those instances that have been initialized symbolically beforehand.
     * @param c The class for which to retrieve aliasing information for the current run of {@link SymbolicExecution}
     * @param isConcolic Whether or not we currently run concolic execution. In this case, we would need to unwrap
     *                   the identifier from its {@link ConcolicNumericalContainer}.
     * @return The aliasing targets' identifiers
     */
    public static Set<Sint> getAliasingTargetIdsForClass(Class<?> c, boolean isConcolic) {
        return getAliasingTargetsForClass(c).stream()
                .map(pc -> pc == null ?
                        Sint.ConcSint.MINUS_ONE
                        :
                        (Sint) (isConcolic ? ConcolicNumericalContainer.tryGetSymFromConcolic(pc.__mulib__getId()) : pc.__mulib__getId()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Adds an aliasing target for the given class
     * @param c The class
     * @param val The aliasing target
     */
    public static void addAliasingTarget(Class<?> c, PartnerClass val) {
        if (val.__mulib__getId() == null) {
            throw new MulibIllegalStateException("To set an object as a global aliasing target, its ID must be initialized");
        }
        getAliasingInfo().aliasingMap.computeIfAbsent(c, e -> new ArrayList<>()).add(val);
    }

    private static AliasingInformation getAliasingInfo() {
        AliasingInformation result = currentAliasingInformation.get();
        if (result == null) {
            result = new AliasingInformation();
            currentAliasingInformation.set(result);
        }
        return result;
    }

    /**
     * Resets the currently stored aliasing targets so that a new {@link SymbolicExecution} can start filling it up again.
     */
    public static void resetAliasingTargets() {
        AliasingInformation result = currentAliasingInformation.get();
        if (result != null) {
            result.aliasingMap.clear();
        }
    }
}
