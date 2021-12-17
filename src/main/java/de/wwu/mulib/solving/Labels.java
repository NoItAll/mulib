package de.wwu.mulib.solving;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sdouble;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Map;

public interface Labels {

    Object getForTrackedSubstitutedVar(SubstitutedVar sv);

    Integer getForTrackedSubstitutedVar(Sint i);

    Double getForTrackedSubstitutedVar(Sdouble d);

    Boolean getForTrackedSubstitutedVar(Sbool b);

    Object getForTrackedVar(String identifier);

    SubstitutedVar[] getTrackedVariables();

    Map<String, Object> getIdentifiersToSVars();

    Map<String, Object> getIdentifiersToValues();

}
