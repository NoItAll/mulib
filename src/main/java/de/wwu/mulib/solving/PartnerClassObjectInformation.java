package de.wwu.mulib.solving;

import de.wwu.mulib.solving.object_representations.PartnerClassObjectSolverRepresentation;
import de.wwu.mulib.substitutions.primitives.Sbool;

public class PartnerClassObjectInformation {

    public final String forField;
    public final Sbool isNull;
    public final boolean fieldCanPotentiallyContainExplicitNull;

    public PartnerClassObjectInformation(
            PartnerClassObjectSolverRepresentation asr,
            String field) {
        this.isNull = asr.isNull();
        this.forField = field;
        if (field != null) {
            this.fieldCanPotentiallyContainExplicitNull = asr.partnerClassFieldCanPotentiallyContainNull(field);
        } else {
            this.fieldCanPotentiallyContainExplicitNull = false;
        }
    }

}
