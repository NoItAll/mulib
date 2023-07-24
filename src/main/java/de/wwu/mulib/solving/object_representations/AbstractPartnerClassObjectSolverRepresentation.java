package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.IncrementalSolverState;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.StringConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPartnerClassObjectSolverRepresentation implements PartnerClassObjectSolverRepresentation {
    // TODO Find more elegant solution. For instance, for PartnerClassObjectConstraints (GETFIELD and initialization)
    //  store the delta in SymbolicExecution
    private static int NEXT_UNTRACKED_RESERVED_ID = -2;

    public static int getNextUntrackedReservedId() {
        // See SymbolicExecution.getNextNumberInitializedSymObject
        return NEXT_UNTRACKED_RESERVED_ID--;
    }

    protected final MulibConfig config;
    // To reuse all functionality, we simply model a field to be an array with one element of the respective type
    protected final Map<String, ArrayHistorySolverRepresentation> fieldToRepresentation;
    protected final Map<String, Class<?>> fieldToType;
    protected final IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps;
    protected final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr;
    protected final Sint id;
    protected final Sbool isNull;
    protected final int level;
    protected final Class<?> clazz;
    protected final boolean defaultIsSymbolic;
    protected AbstractPartnerClassObjectSolverRepresentation(
            MulibConfig config,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            PartnerClassObjectInitializationConstraint pic,
            int level) {
        this(
                config,
                pic.getPartnerClassObjectId(),
                pic.isNull(),
                pic.getClazz(),
                pic.isDefaultIsSymbolic(),
                sps,
                asr,
                level
        );
        initializeFields(pic.getInitialGetfields(), fieldToType);
    }

    /**
     * Constructor for generating lazily
     */
    protected AbstractPartnerClassObjectSolverRepresentation(
            MulibConfig config,
            Sint id,
            Sbool isNull,
            Class<?> clazz,
            boolean defaultIsSymbolic,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps,
            IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr,
            int level) {
        this.config = config;
        this.id = id;
        this.isNull = isNull;
        assert isNull != Sbool.ConcSbool.TRUE || id == Sint.ConcSint.MINUS_ONE;
        this.level = level;
        this.clazz = clazz;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.fieldToRepresentation = new HashMap<>();
        this.sps = sps;
        this.asr = asr;
        this.fieldToType = new HashMap<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            if (!f.getName().contains(StringConstants._TRANSFORMATION_PREFIX) && !Modifier.isStatic(f.getModifiers())) {
                fieldToType.put(f.getDeclaringClass().getName() + "." + f.getName(), f.getType());
            }
        }
    }

    protected void initializeFields(
            PartnerClassObjectFieldConstraint[] initialGetfields,
            Map<String, Class<?>> fieldTypes) {
        // We treat single fields as arrays of length 1
        Map<String, ArrayAccessConstraint> arraySelects = initialGetfieldsToArraySelects(initialGetfields);
        for (Map.Entry<String, Class<?>> entry : fieldTypes.entrySet()) {
            ArrayAccessConstraint aac = arraySelects.get(entry.getKey());
            if (aac == null) {
                if (!PartnerClass.class.isAssignableFrom(entry.getValue())) {
                    // We generate an arbitrary primitive symbolic value for the field of the correct type
                    aac = generateArrayAccessConstraintForIndexWithSymbolicValue(id, entry.getValue());
                } else {
                    // PartnerClass fields will be lazily initialized
                    continue;
                }
            }
            fieldToRepresentation.put(
                    entry.getKey(),
                    new ArrayHistorySolverRepresentation(
                            new ArrayAccessConstraint[] { aac },
                            entry.getValue()
                    )
            );
        }
    }

    private static ArrayAccessConstraint generateArrayAccessConstraintForIndexWithSymbolicValue(Sint id, Class<?> c) {
        return new ArrayAccessConstraint(id, Sint.ConcSint.ZERO, generateSymSprimitive(c), ArrayAccessConstraint.Type.SELECT);
    }

    private static Sprimitive generateSymSprimitive(Class<?> c) {
        Sprimitive val;
        if (Sint.class.isAssignableFrom(c)) {
            if (Sbool.class.isAssignableFrom(c)) {
                val = Sbool.newInputSymbolicSbool();
            } else if (Sbyte.class.isAssignableFrom(c)) {
                val = Sbyte.newInputSymbolicSbyte();
            } else if (Sshort.class.isAssignableFrom(c)) {
                val = Sshort.newInputSymbolicSshort();
            } else if (Schar.class.isAssignableFrom(c)) {
                val = Schar.newInputSymbolicSchar();
            } else {
                assert c == Sint.class;
                val = Sint.newInputSymbolicSint();
            }
        } else if (Sdouble.class.isAssignableFrom(c)) {
            val = Sdouble.newInputSymbolicSdouble();
        } else if (Slong.class.isAssignableFrom(c)) {
            val = Slong.newInputSymbolicSlong();
        } else if (Sfloat.class.isAssignableFrom(c)) {
            val = Sfloat.newInputSymbolicSfloat();
        } else {
            throw new NotYetImplementedException();
        }
        return val;
    }

    private static Map<String, ArrayAccessConstraint> initialGetfieldsToArraySelects(PartnerClassObjectFieldConstraint[] getfields) {
        Map<String, ArrayAccessConstraint> result = new HashMap<>();
        for (PartnerClassObjectFieldConstraint p : getfields) {
            assert !result.containsKey(p.getFieldName());
            result.put(p.getFieldName(), new ArrayAccessConstraint(
                    p.getPartnerClassObjectId(),
                    Sint.ConcSint.ZERO,
                    p.getValue(),
                    ArrayAccessConstraint.Type.SELECT
            ));
        }
        return result;
    }

    protected AbstractPartnerClassObjectSolverRepresentation(AbstractPartnerClassObjectSolverRepresentation apcor, int level) {
        this.config = apcor.config;
        this.id = apcor.id;
        this.isNull = apcor.isNull;
        this.level = level;
        this.clazz = apcor.clazz;
        this.defaultIsSymbolic = apcor.defaultIsSymbolic;
        this.fieldToRepresentation = new HashMap<>();
        for (Map.Entry<String, ArrayHistorySolverRepresentation> entry : apcor.fieldToRepresentation.entrySet()) {
            this.fieldToRepresentation.put(entry.getKey(), entry.getValue().copy());
        }
        this.fieldToType = new HashMap<>(apcor.fieldToType);
        this.sps = apcor.sps;
        this.asr = apcor.asr;
    }

    @Override
    public final Constraint getField(String fieldName, Sprimitive value) {
        assert value != Sint.ConcSint.MINUS_ONE;
        return _getField(Sbool.ConcSbool.TRUE, fieldName, value);
    }

    @Override
    public final void putField(String fieldName, Sprimitive value) {
        _putField(Sbool.ConcSbool.TRUE, fieldName, value);
    }

    @Override
    public final Constraint getField(Constraint guard, String fieldName, Sprimitive value) {
        assert value != Sint.ConcSint.MINUS_ONE;
        return _getField(guard, fieldName, value);
    }

    @Override
    public final PartnerClassObjectSolverRepresentation lazilyGenerateAndSetPartnerClassFieldIfNeeded(String fieldName) {
        assert fieldToType.containsKey(fieldName);
        int level = sps.getCurrentLevel();
        if (this.level != level) {
            PartnerClassObjectSolverRepresentation partnerClassObjectSolverRepresentation = copyForNewLevel(level);
            sps.getRepresentationForId(id).addNewRepresentation(partnerClassObjectSolverRepresentation, level);
            return partnerClassObjectSolverRepresentation.lazilyGenerateAndSetPartnerClassFieldIfNeeded(fieldName);
        }
        if (!_fieldIsSet(fieldName)) {
            Class<?> typeOfField = fieldToType.get(fieldName);
            if (Sarray.class.isAssignableFrom(typeOfField)) {
                ArraySolverRepresentation fieldVal = lazilyGenerateArrayForField(fieldName);
                assert fieldVal.getArrayId() != null;
                ArrayAccessConstraint aac = new ArrayAccessConstraint(id, Sint.ConcSint.ZERO, fieldVal.getArrayId(), ArrayAccessConstraint.Type.SELECT);
                fieldToRepresentation.put(
                        fieldName,
                        new ArrayHistorySolverRepresentation(new ArrayAccessConstraint[] { aac }, typeOfField)
                );
            } else if (PartnerClass.class.isAssignableFrom(typeOfField)) {
                PartnerClassObjectSolverRepresentation fieldVal = lazilyGeneratePartnerClassObjectForField(fieldName);
                assert fieldVal.getId() != null;
                sps.addRepresentationForId(fieldVal.getId(), fieldVal, level);
                ArrayAccessConstraint aac = new ArrayAccessConstraint(id, Sint.ConcSint.ZERO, fieldVal.getId(), ArrayAccessConstraint.Type.SELECT);
                fieldToRepresentation.put(
                        fieldName,
                        new ArrayHistorySolverRepresentation(new ArrayAccessConstraint[] { aac }, typeOfField)
                );
            } else {
                assert Sprimitive.class.isAssignableFrom(typeOfField);
                Sprimitive val = generateSymSprimitive(typeOfField);
                ArrayAccessConstraint aac = new ArrayAccessConstraint(id, Sint.ConcSint.ZERO, val, ArrayAccessConstraint.Type.SELECT);
                fieldToRepresentation.put(
                        fieldName,
                        new ArrayHistorySolverRepresentation(new ArrayAccessConstraint[]{ aac }, typeOfField)
                );
            }
        }
        assert !fieldToRepresentation.get(fieldName).isEmpty();
        return this;
    }

    protected abstract PartnerClassObjectSolverRepresentation lazilyGeneratePartnerClassObjectForField(String field);

    protected abstract ArraySolverRepresentation lazilyGenerateArrayForField(String field);

    @Override
    public final void putField(Constraint guard, String fieldName, Sprimitive value) {
        _putField(guard, fieldName, value);
    }

    protected abstract Constraint _getField(Constraint guard, String fieldName, Sprimitive value);

    protected abstract void _putField(Constraint guard, String fieldName, Sprimitive value);

    @Override
    public Sint getId() {
        return id;
    }

    public Sbool isNull() {
        return isNull;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public Class<?> getClazz() {
        return clazz;
    }

    @Override
    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    @Override
    public boolean partnerClassFieldCanPotentiallyContainNull(String field) {
        Class<?> typeOfField = fieldToType.get(field);
        Set<Sint> relevantValues = getPartnerClassIdsKnownToBePossiblyContainedInField(field, false); //// TODO change to true or...
        //// TODO ...if symbolic init to null is allowed, regard this here
        return relevantValues.stream()
                .anyMatch(s -> {
                    if (s == Sint.ConcSint.MINUS_ONE) {
                        return true;
                    }
                    IncrementalSolverState.PartnerClassObjectRepresentation<PartnerClassObjectSolverRepresentation>
                            pcosr = sps.getRepresentationForId(s);
                    if (!typeOfField.isArray()
                    && !Sarray.class.isAssignableFrom(typeOfField)
                    ) {
                        PartnerClassObjectSolverRepresentation partnerClassObjectConstraint =
                                pcosr.getNewestRepresentation();
                        return partnerClassObjectConstraint.isNull() instanceof Sym;
                    } else {
                        ArraySolverRepresentation asr = (ArraySolverRepresentation) pcosr.getNewestRepresentation();
                        return asr.getIsNull() instanceof Sym;
                    }
                });
    }

    @Override
    public boolean _fieldIsSet(String field) {
        return fieldToRepresentation.containsKey(field);
    }
}
