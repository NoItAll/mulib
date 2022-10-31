package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

public class SimplePartnerClassObjectRepresentation extends AbstractPartnerClassObjectRepresentation {
    protected SimplePartnerClassObjectRepresentation(MulibConfig config, PartnerClassObjectInitializationConstraint pic, int level) {
        super(config, pic, level);
    }

    protected SimplePartnerClassObjectRepresentation(SimplePartnerClassObjectRepresentation apcor, int level) {
        super(apcor, level);
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void _putField(Constraint guard, String fieldName, Sprimitive value) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    public PartnerClassObjectSolverRepresentation copyForNewLevel(int level) {
        return new SimplePartnerClassObjectRepresentation(this, level);
    }
}
