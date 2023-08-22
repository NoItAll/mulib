package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericalContainer;
import de.wwu.mulib.substitutions.AssignConcolicLabelEnabledValueFactory;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Copies a search space representation.
 * Is used for {@link de.wwu.mulib.constraints.PartnerClassObjectRememberConstraint}s as well as for copying
 * arguments to the search region.
 */
public class MulibValueCopier {

    private final boolean isConcolic;
    private final Map<Object, Object> alreadyCopiedObjects = new IdentityHashMap<>();
    private final SymbolicExecution se;

    /**
     * @param se The current instance of symbolic execution
     * @param config The configuration
     */
    public MulibValueCopier(
            SymbolicExecution se,
            MulibConfig config) {
        this.se = se;
        this.isConcolic = config.SEARCH_CONCOLIC;
    }

    /**
     * Registers a copy to break cycles.
     * @param original The original search region representation
     * @param copy The copy of the original search region representation
     */
    public void registerCopy(Object original, Object copy) {
        alreadyCopiedObjects.put(original, copy);
    }

    /**
     * @param original The original search region representation
     * @return true, if 'original' was already copied.
     */
    public boolean alreadyCopied(Object original) {
        return alreadyCopiedObjects.containsKey(original);
    }

    /**
     * @param original
     * @return A copy. Throws an exception if no copy could be found
     */
    public Object getCopy(Object original) {
        if (original == null) {
            return null;
        }
        Object o = alreadyCopiedObjects.get(original);
        if (o == null) {
            throw new MulibRuntimeException("There is no copy for the given original: " + original);
        }
        return o;
    }

    /**
     * @param o Any object
     * @return The copy
     */
    public Object copy(Object o) {
        return o instanceof Sprimitive ? copySprimitive((Sprimitive) o) : copyNonSprimitive(o);
    }

    /**
     * @param o Any non-{@link Sprimitive} object
     * @return The copy
     */
    public Object copyNonSprimitive(Object o) {
        if (o == null) {
            return null;
        }

        Object result;
        if ((result = alreadyCopiedObjects.get(o)) != null) {
            return result;
        }

        if (o instanceof PartnerClass) {
            result = ((PartnerClass) o).copy(this);
            return result;
        }
        return o;
    }

    /**
     * Only needed if {@link MulibConfig#SEARCH_CONCOLIC} is set.
     * @param o Any {@link Sprimitive}-object
     * @return A copy.
     */
    public Object copySprimitive(Sprimitive o) {
        if (!isConcolic) {
            return o;
        }
        Object result;
        if ((result = alreadyCopiedObjects.get(o)) != null) {
            return result;
        }
        if (o instanceof Sym) {
            result = _potentiallyUnpackAndRelabelConcolic((Sym) o);
        } else {
            result = o;
        }
        registerCopy(o, result);
        return result;
    }

    private Object _potentiallyUnpackAndRelabelConcolic(Sym currentValue) {
        // Check if we need to unpack concolic values
        if (currentValue instanceof Sbool.SymSbool) {
            Sbool.SymSbool s = (Sbool.SymSbool) ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool.SymSbool) currentValue);
            return ((AssignConcolicLabelEnabledValueFactory) se.getValueFactory()).assignLabel(se, s);
        } else if (currentValue instanceof SymSnumber) {
            SymSnumber s = (SymSnumber) ConcolicNumericalContainer.tryGetSymFromConcolic((SymSnumber) currentValue);
            return ((AssignConcolicLabelEnabledValueFactory) se.getValueFactory()).assignLabel(se, s);
        } else {
            throw new NotYetImplementedException(currentValue.getClass().toString());
        }
    }

}
