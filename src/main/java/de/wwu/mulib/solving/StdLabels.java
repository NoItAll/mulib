package de.wwu.mulib.solving;

import de.wwu.mulib.substitutions.SubstitutedVar;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Standard implementation of {@link Labels} using two {@link Map}s
 */
public class StdLabels implements Labels {

    private final Map<String, SubstitutedVar> identifiersToSVars;
    private final Map<String, Object> identifiersToValues;

    /**
     * @param identifiersToSubstitutedVars The remembering name to the search space representation
     * @param identifiersToOriginalRepresentation The remembering name to the label
     */
    public StdLabels(
            Map<String, SubstitutedVar> identifiersToSubstitutedVars,
            Map<String, Object> identifiersToOriginalRepresentation) {
        this.identifiersToSVars = Collections.unmodifiableMap(identifiersToSubstitutedVars);
        this.identifiersToValues = Collections.unmodifiableMap(identifiersToOriginalRepresentation);
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
    public Map<String, SubstitutedVar> getIdToNamedVar() {
        return identifiersToSVars;
    }

    @Override
    public Map<String, Object> getIdToLabel() {
        return identifiersToValues;
    }

    @Override
    public Collection<String> getNames() {
        return identifiersToValues.keySet();
    }

    @Override
    public String toString() {
        return "StdLabels{" + getIdToLabel() + "}";
    }

}
