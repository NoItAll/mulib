package de.wwu.mulib.solving;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Map;

public class StdLabels implements Labels {

    protected final Map<String, Object> identifiersToSVars;
    protected final Map<SubstitutedVar, Object> svariablesToValues;
    protected final Map<String, Object> identifiersToValues;

    public StdLabels(
            Map<String, Object> identifiersToVars,
            Map<SubstitutedVar, Object> svariablesToValues,
            Map<String, Object> identifiersToValues) {
        this.identifiersToSVars = identifiersToVars;
        this.svariablesToValues = Map.copyOf(svariablesToValues);
        this.identifiersToValues = Map.copyOf(identifiersToValues);
    }

    @Override
    public Object getForTrackedSubstitutedVar(SubstitutedVar sv) {
        if (!(sv instanceof Sprimitive)) {
            throw new NotYetImplementedException();
        }
        return svariablesToValues.get(sv);
    }

    @Override
    public Integer getForTrackedSubstitutedVar(Sint i) {
        return (Integer) svariablesToValues.get(i);
    }

    @Override
    public Double getForTrackedSubstitutedVar(Sdouble d) {
        return (Double) svariablesToValues.get(d);
    }

    @Override
    public Boolean getForTrackedSubstitutedVar(Sbool b) {
        return (Boolean) svariablesToValues.get(b);
    }

    @Override
    public Object getForTrackedVar(String identifier) {
        return identifiersToValues.get(identifier);
    }

    @Override
    public SubstitutedVar[] getTrackedVariables() {
        return svariablesToValues.keySet().toArray(new SubstitutedVar[0]);
    }

    @Override
    public Map<String, Object> getIdentifiersToSVars() {
        return identifiersToSVars;
    }

    @Override
    public Map<String, Object> getIdentifiersToValues() {
        return identifiersToValues;
    }

}
