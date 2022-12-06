package de.wwu.mulib.search.executors;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.*;
import java.util.stream.Collectors;

public final class AliasingInformation {
    private static final ThreadLocal<AliasingInformation> currentAliasingInformation = new ThreadLocal<>();
    private final Map<Class<?>, Collection<PartnerClass>> aliasingMap = new HashMap<>();
    public static Collection<PartnerClass> getAliasingTargetsForClass(Class<?> c) {
        Map<Class<?>, Collection<PartnerClass>> map = getAliasingInfo().aliasingMap;
        return map.entrySet().stream()
                .filter(e -> c.isAssignableFrom(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList());
    }

    public static Set<Sint> getAliasingTargetIdsForClass(Class<?> c, boolean isConcolic) {
        return getAliasingTargetsForClass(c).stream()
                .map(pc -> pc == null ?
                        Sint.ConcSint.MINUS_ONE
                        :
                        (Sint) (isConcolic ? ConcolicNumericContainer.tryGetSymFromConcolic(pc.__mulib__getId()) : pc.__mulib__getId()))
                .collect(Collectors.toCollection(HashSet::new));
    }

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

    public static void resetAliasingTargets() {
        AliasingInformation result = currentAliasingInformation.get();
        if (result != null) {
            result.aliasingMap.clear();
        }
    }
}
