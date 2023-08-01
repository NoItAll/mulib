package de.wwu.mulib.solving;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StdLabels implements Labels {

    protected final Map<String, SubstitutedVar> identifiersToSVars;
    protected final Map<SubstitutedVar, Object> svariablesToValues;
    protected final Map<String, Object> identifiersToValues;

    public StdLabels(
            Map<String, SubstitutedVar> identifiersToSubstitutedVars,
            Map<SubstitutedVar, Object> substitutedVarsToOriginalRepresentation,
            Map<String, Object> identifiersToOriginalRepresentation) {
        this.identifiersToSVars = Collections.unmodifiableMap(identifiersToSubstitutedVars);
        this.svariablesToValues = Collections.unmodifiableMap(substitutedVarsToOriginalRepresentation);
        this.identifiersToValues = Collections.unmodifiableMap(identifiersToOriginalRepresentation);
    }

    @Override
    public Object getLabelForNamedSubstitutedVar(SubstitutedVar sv) {
        return svariablesToValues.get(sv);
    }

    @Override
    public Object getLabelForId(String id) {
        return identifiersToValues.get(id);
    }

    @Override
    public SubstitutedVar getNamedVar(String id) {
        return identifiersToSVars.get(id);
    }

    @Override
    public SubstitutedVar[] getNamedVars() {
        return svariablesToValues.keySet().toArray(new SubstitutedVar[0]);
    }

    @Override
    public Sprimitive[] getNamedPrimitiveVars() {
        return svariablesToValues.keySet().stream().filter(sv -> sv instanceof Sprimitive).toArray(Sprimitive[]::new);
    }

    @Override
    public Map<String, SubstitutedVar> getIdToNamedVar() {
        return identifiersToSVars;
    }

    @Override
    public Map<String, Object> getIdToLabel() {
        return identifiersToValues;
    }

    public Object[] getLabels() {
        return identifiersToValues.values().toArray(new Object[0]);
    }

    @Override
    public String toString() {
        return "StdLabels{" + getIdToLabel() + "}";
    }

}
