package de.wwu.mulib.solving;

import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashMap;
import java.util.Map;

public class LabelService {

    public static Labels getLabels(
            SolverManager solverManager,
            MulibValueTransformer mulibValueTransformer,
            Map<String, SubstitutedVar> idToNamedVar) {
        Map<String, Object> idToLabel = new HashMap<>();
        Map<SubstitutedVar, Object> namedVarToLabel = new HashMap<>();
        for (Map.Entry<String, SubstitutedVar> entry : idToNamedVar.entrySet()) {
            Object label = mulibValueTransformer.labelValue(entry.getValue(), solverManager);
            idToLabel.put(entry.getKey(), label);
            namedVarToLabel.put(entry.getValue(), label);
        }

        return new StdLabels(idToNamedVar, namedVarToLabel, idToLabel);
    }
}
