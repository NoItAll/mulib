package de.wwu.mulib.solving;

import de.wwu.mulib.solving.object_representations.PartnerClassObjectSolverRepresentation;
import de.wwu.mulib.substitutions.primitives.Sbool;

/**
 * Contains metadata for a non-array object that is represented for/int the solver
 */
public class PartnerClassObjectInformation {
    /**
     * For which field do we want information?
     */
    public final String forField;
    /**
     * Can the object be null?
     */
    public final Sbool isNull;
    /**
     * Can the field potentially contain a null?
     */
    public final boolean fieldCanPotentiallyContainNull;

    public PartnerClassObjectInformation(
            PartnerClassObjectSolverRepresentation asr,
            String field) {
        this.isNull = asr.isNull();
        this.forField = field;
        if (field != null) {
            this.fieldCanPotentiallyContainNull = asr.partnerClassFieldCanPotentiallyContainNull(field);
        } else {
            this.fieldCanPotentiallyContainNull = false;
        }
    }

}
