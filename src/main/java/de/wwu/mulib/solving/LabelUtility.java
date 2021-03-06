package de.wwu.mulib.solving;

import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Snumber;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashMap;
import java.util.Map;

public class LabelUtility {

    public static Labels getLabels(
            SolverManager solverManager,
            MulibValueTransformer mulibValueTransformer,
            Map<String, SubstitutedVar> idToNamedVar) {
        Map<String, SubstitutedVar> identifiersToSubstitutedVars = new HashMap<>();
        Map<String, Object> idToLabel = new HashMap<>();
        Map<SubstitutedVar, Object> namedVarToLabel = new HashMap<>();
        for (Map.Entry<String, SubstitutedVar> entry : idToNamedVar.entrySet()) {
            Object label = mulibValueTransformer.labelValue(entry.getValue(), solverManager);
            idToLabel.put(entry.getKey(), label);
            SubstitutedVar value = entry.getValue();
            if (value instanceof Sbool) {
                value = ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool) value);
            } else if (value instanceof Snumber) {
                value = ConcolicNumericContainer.tryGetSymFromConcolic((Snumber) value);
            }
            namedVarToLabel.put(value, label);
            identifiersToSubstitutedVars.put(entry.getKey(), value);
        }

        return new StdLabels(identifiersToSubstitutedVars, namedVarToLabel, idToLabel);
    }
}
