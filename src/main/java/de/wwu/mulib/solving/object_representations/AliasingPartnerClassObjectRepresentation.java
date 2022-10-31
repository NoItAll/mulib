package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.HashSet;
import java.util.Set;

public class AliasingPartnerClassObjectRepresentation extends AbstractPartnerClassObjectRepresentation {
    protected final Sint reservedId;
    protected final Constraint metadataConstraintForPotentialIds;
    protected final Set<IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation>> aliasedObjects;

    protected AliasingPartnerClassObjectRepresentation(
            MulibConfig config,
            PartnerClassObjectInitializationConstraint pic,
            int level,
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates) {
        super(config, pic, level);
        this.reservedId = pic.getReservedId();
        assert getId() instanceof SymNumericExpressionSprimitive;
        assert reservedId instanceof ConcSnumber;
        assert potentialIds != null && potentialIds.size() > 0 : "There always must be at least one potential aliasing candidate";
        this.aliasedObjects = new HashSet<>(); // Is filled in getMetadataConstraintForPotentialIds
        this.metadataConstraintForPotentialIds = getMetadataConstraintForPotentialIds(potentialIds, symbolicPartnerClassObjectStates);
    }

    private Constraint getMetadataConstraintForPotentialIds(
            Set<Sint> potentialIds,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> symbolicPartnerClassObjectStates) {
        //// TODO
        throw new NotYetImplementedException();
    }

    protected AliasingPartnerClassObjectRepresentation(AliasingPartnerClassObjectRepresentation apcor, int level) {
        super(apcor, level);
        this.reservedId = apcor.reservedId;
        this.metadataConstraintForPotentialIds = apcor.metadataConstraintForPotentialIds;
        this.aliasedObjects = apcor.aliasedObjects;
        throw new NotYetImplementedException();
    }

    public Set<Sprimitive> getValuesForFieldKnownToBePossiblyContained(String fieldName) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    public PartnerClassObjectSolverRepresentation copyForNewLevel(int level) {
        return new AliasingPartnerClassObjectRepresentation(this, level);
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    protected void _putField(Constraint guard, String fieldName, Sprimitive value) {
        throw new NotYetImplementedException(); //// TODO
    }
}
