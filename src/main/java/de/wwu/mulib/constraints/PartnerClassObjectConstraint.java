package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.HashMap;
import java.util.Map;

public interface PartnerClassObjectConstraint {

    Sint getPartnerClassObjectId();

    static Map<String, SubstitutedVar> getLastValues(
            PartnerClassObjectConstraint[] pcocs) {
        PartnerClassObjectInitializationConstraint pcoic = (PartnerClassObjectInitializationConstraint) pcocs[0];
        Map<String, SubstitutedVar> result = new HashMap<>();
        for (PartnerClassObjectFieldConstraint p : pcoic.getInitialGetfields()) {
            result.put(p.getFieldName(), p.getValue());
        }
        for (int i = 1; i < pcocs.length; i++) {
            if (pcocs[i] instanceof PartnerClassObjectInitializationConstraint) {
                continue; // Can happen during aliasing; - we just take the first initialization constraint in this case
            }
            PartnerClassObjectFieldConstraint pcofc = (PartnerClassObjectFieldConstraint) pcocs[i];
            result.put(pcofc.getFieldName(), pcofc.getValue());
        }
        return result;
    }

}
