package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;

import java.util.Set;

public class SimplePartnerClassObjectSolverRepresentation extends AbstractPartnerClassObjectSolverRepresentation {

    protected SimplePartnerClassObjectSolverRepresentation(
            MulibConfig config,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            PartnerClassObjectInitializationConstraint pic,
            int level) {
        super(config, sps, asr, pic, level);
    }

    protected SimplePartnerClassObjectSolverRepresentation(
            MulibConfig config,
            Sint id,
            Sbool isNull,
            Class<?> clazz,
            boolean defaultIsSymbolic,
            boolean isAliasing,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            int level) {
        super(config, id, isNull, clazz, defaultIsSymbolic, isAliasing, sps, asr, level);
    }

    protected SimplePartnerClassObjectSolverRepresentation(SimplePartnerClassObjectSolverRepresentation apcor, int level) {
        super(apcor, level);
    }

    @Override
    protected PartnerClassObjectSolverRepresentation lazilyGeneratePartnerClassObjectForField(String field) {
        Sint id = Sint.concSint(getNextUntrackedReservedId());
        PartnerClassObjectSolverRepresentation result = new SimplePartnerClassObjectSolverRepresentation(
                config,
                id,
                config.ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL ?
                        Sbool.newInputSymbolicSbool()
                        :
                        Sbool.ConcSbool.FALSE,
                fieldToType.get(field),
                true,
                isAliasing,
                sps,
                asr,
                level
        );
        sps.addRepresentationForId(id, result, level);
        return result;
    }

    @Override
    protected ArraySolverRepresentation lazilyGenerateArrayForField(String field) {
        Class<?> typeOfField = fieldToType.get(field);
        assert Sarray.class.isAssignableFrom(typeOfField);
        Sint id = Sint.concSint(getNextUntrackedReservedId());
        ArraySolverRepresentation result = Sarray.SarraySarray.class.isAssignableFrom(typeOfField) ?
                new SimplePartnerClassArraySolverRepresentation(
                        config,
                        id,
                        Sint.newInputSymbolicSint(),
                        config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL ?
                                Sbool.newInputSymbolicSbool()
                                :
                                Sbool.ConcSbool.FALSE,
                        typeOfField,
                        true,
                        level,
                        false,
                        false,
                        sps,
                        asr
                )
                :
                new PrimitiveValuedArraySolverRepresentation(
                        config,
                        id,
                        Sint.newInputSymbolicSint(),
                        config.ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL ?
                                Sbool.newInputSymbolicSbool()
                                :
                                Sbool.ConcSbool.FALSE,
                        typeOfField,
                        true,
                        level,
                        false,
                        false
                );

        asr.addRepresentationForId(id, result, level);
        return result;
    }

    @Override
    protected Constraint _getField(Constraint guard, String fieldName, Sprimitive value) {
        lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
        ArrayHistorySolverRepresentation currentRepresentation = fieldToRepresentation.get(fieldName);
        return currentRepresentation.select(
                guard,
                Sint.ConcSint.ZERO,
                value,
                true,
                false
        );
    }

    @Override
    protected void _putField(Constraint guard, String fieldName, Sprimitive value) {
        lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
        ArrayHistorySolverRepresentation currentRepresentation = fieldToRepresentation.get(fieldName);
        ArrayHistorySolverRepresentation newRepresentation = currentRepresentation.store(guard, Sint.ConcSint.ZERO, value);
        fieldToRepresentation.put(fieldName, newRepresentation);
    }

    @Override
    public PartnerClassObjectSolverRepresentation copyForNewLevel(int level) {
        return new SimplePartnerClassObjectSolverRepresentation(this, level);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Sint> getPartnerClassIdsKnownToBePossiblyContainedInField(String fieldName) {
        assert PartnerClass.class.isAssignableFrom(fieldToType.get(fieldName));
        lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
        // We know that the array is of fixed size since we represent each field by an array of size 1
        return (Set<Sint>) fieldToRepresentation.get(fieldName).getValuesKnownToPossiblyBeContainedInArray(true);
    }
}
