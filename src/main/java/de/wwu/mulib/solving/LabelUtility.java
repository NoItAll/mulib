package de.wwu.mulib.solving;

import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashMap;
import java.util.Map;

public class LabelUtility {

    public static Labels getLabels(
            SolverManager solverManager,
            MulibValueTransformer mulibValueTransformer,
            Map<String, SubstitutedVar> idToNamedVar) {
        Map<String, Object> idToLabel = new HashMap<>();
        Map<SubstitutedVar, Object> namedVarToLabel = new HashMap<>();
        for (Map.Entry<String, SubstitutedVar> entry : idToNamedVar.entrySet()) {
            Object label = mulibValueTransformer.labelValue(entry.getValue(), solverManager);
            idToLabel.put(entry.getKey(), label);
            SubstitutedVar value = entry.getValue();
            if (value instanceof Sbool.SymSbool) {
                if (((Sbool.SymSbool) value).getRepresentedConstraint() instanceof ConcolicConstraintContainer) {
                    value = ((ConcolicConstraintContainer) ((Sbool.SymSbool) value).getRepresentedConstraint()).getSym();
                }
            } else if (value instanceof SymNumericExpressionSprimitive
                    && ((SymNumericExpressionSprimitive) value).getRepresentedExpression() instanceof ConcolicNumericContainer) {
                value = ((ConcolicNumericContainer) ((SymNumericExpressionSprimitive) value).getRepresentedExpression()).getSym();
            }
            namedVarToLabel.put(value, label);
        }

        return new StdLabels(idToNamedVar, namedVarToLabel, idToLabel);
    }
}
