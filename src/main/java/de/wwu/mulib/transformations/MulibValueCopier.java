package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.AssignConcolicLabelEnabledValueFactory;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MulibValueCopier {

    private final Map<Class<?>, BiFunction<MulibValueCopier, Object, Object>> classesToCopyFunction;
    private final boolean isConcolic;
    private final Map<Object, Object> alreadyCopiedObjects = new IdentityHashMap<>();
    private final SymbolicExecution se;

    public MulibValueCopier(
            SymbolicExecution se,
            MulibConfig config) {
        this.se = se;
        this.classesToCopyFunction = config.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS;
        this.isConcolic = config.SEARCH_CONCOLIC;
    }

    public void registerCopy(Object original, Object copy) {
        alreadyCopiedObjects.put(original, copy);
    }

    public boolean alreadyCopied(Object o) {
        return alreadyCopiedObjects.containsKey(o);
    }

    public Object getCopy(Object original) {
        if (original == null) {
            throw new MulibRuntimeException("For null, no copy can be retrieved.");
        }
        Object o = alreadyCopiedObjects.get(original);
        if (o == null) {
            throw new MulibRuntimeException("There is no copy for the given original: " + original);
        }
        return o;
    }

    public Object copy(Object o) {
        return o instanceof Sprimitive ? copySprimitive((Sprimitive) o) : copyNonSprimitive(o);
    }

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
        BiFunction<MulibValueCopier, Object, Object> copier = classesToCopyFunction.get(o.getClass());
        if (copier == null) {
            return o;
        }
        return copier.apply(this, o);
    }

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
        } else if (currentValue instanceof SymNumericExpressionSprimitive) {
            SymNumericExpressionSprimitive s = (SymNumericExpressionSprimitive) ConcolicNumericContainer.tryGetSymFromConcolic((SymNumericExpressionSprimitive) currentValue);
            return ((AssignConcolicLabelEnabledValueFactory) se.getValueFactory()).assignLabel(se, s);
        } else {
            throw new NotYetImplementedException(currentValue.getClass().toString());
        }
    }

}
