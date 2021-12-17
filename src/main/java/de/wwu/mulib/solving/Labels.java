package de.wwu.mulib.solving;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Map;

public interface Labels {

    Object getLabelForNamedSubstitutedVar(SubstitutedVar sv);

    Object getLabelForId(String id);

    SubstitutedVar getNamedVar(String id);

    SubstitutedVar[] getNamedVars();

    Sprimitive[] getNamedPrimitiveVars();

    Map<String, SubstitutedVar> getIdToNamedVar();

    Map<String, Sprimitive> getIdToNamedPrimitiveVar();

    Map<String, Object> getIdToLabel();

}
