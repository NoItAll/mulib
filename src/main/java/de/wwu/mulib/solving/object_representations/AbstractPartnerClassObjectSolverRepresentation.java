package de.wwu.mulib.solving.object_representations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint;
import de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
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

/**
 * Representation for all non-array partner class objects FOR the solver.
 * Subtypes must also support lazy initialization of single fields.
 * Primitive-typed fields are initialized immediately, if they are to be initialized lazily
 */
public abstract class AbstractPartnerClassObjectSolverRepresentation implements PartnerClassObjectSolverRepresentation {
    // TODO Find more elegant solution. For instance, for PartnerClassObjectConstraints (GETFIELD and initialization)
    //  store the delta in SymbolicExecution
    private static int NEXT_UNTRACKED_RESERVED_ID = -2;

    /**
     * @return The next identifier not supplied by a {@link ValueFactory} for lazy initialization in the solver backend
     * @see SymbolicExecution#getNextNumberInitializedSymObject()
     */
    public static int getNextUntrackedReservedId() {
        return NEXT_UNTRACKED_RESERVED_ID--;
    }

    /**
     * The configuration
     */
    protected final MulibConfig config;
    /**
     * To reuse all functionality, we simply model a field to be an array with one element of the respective type
     * This representation contains (packageName.className.fieldName, one-elemented array history)-pairs
     */
    protected final Map<String, ArrayHistorySolverRepresentation> fieldToRepresentation;
    /**
     * Contains a map of (packageName.className.fieldName, type of field)-pairs for lazy initialization
     */
    protected final Map<String, Class<?>> fieldToType;
    /**
     * The construct containing partner class object representations
     */
    protected final IncrementalSolverState.SymbolicPartnerClassObjectStates<PartnerClassObjectSolverRepresentation> sps;
    /**
     * The construct containing array solver representations
     */
    protected final IncrementalSolverState.SymbolicPartnerClassObjectStates<ArraySolverRepresentation> asr;
    /**
     * The identifiers
     */
    protected final Sint id;
    /**
     * Whether the object is null or not
     */
    protected final Sbool isNull;
    /**
     * The level this representation is for
     */
    protected final int level;
    /**
     * The class represented by this representation
     */
    protected final Class<?> clazz;
    /**
     * Whether the default value returned by uninitialized fields is symbolic or not
     */
    protected final boolean defaultIsSymbolic;

    /**
     * Constructs a new instance of representation
     * @param config The configuration
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param pic The constraint initializing this representation
     * @param level The level for which we create this representation
     */
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
     * Constructor for generating a representation lazily
     * @param config The configuration
     * @param id The identifier
     * @param isNull Whether the represented object potentially is null
     * @param clazz The represented class
     * @param defaultIsSymbolic Whether the default of uninitialized elements is symbolic
     * @param sps The construct maintaining object representations for potential lazy initialization of fields
     * @param asr The construct maintaining array representations for potential lazy initialization of fields
     * @param level The level
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

    private void initializeFields(
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

    /**
     * Copy constructor
     * @param apcor To-copy
     * @param level The level to copy for
     */
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

    /**
     * Must be overridden to lazily generate a field of a non-array reference-type
     * @param field The field. Should conform to the pattern: packageName.className.fieldName
     * @return The lazily generated non-array partner class representation
     */
    protected abstract PartnerClassObjectSolverRepresentation lazilyGeneratePartnerClassObjectForField(String field);

    /**
     * Must be overridden to lazily generate a field of an array-type
     * @param field The field. Should conform to the pattern: packageName.className.fieldName
     * @return The lazily generated array representation
     */
    protected abstract ArraySolverRepresentation lazilyGenerateArrayForField(String field);

    @Override
    public final void putField(Constraint guard, String fieldName, Sprimitive value) {
        _putField(guard, fieldName, value);
    }

    /**
     * Should be overridden to differentiate between aliasing partner class object solver representations and "simple"
     * solver representations that represent only themselves.
     * @param guard The guard determining whether the getField should be valid
     * @param fieldName The field. Should follow the pattern: packageName.className.fieldName
     * @param value The value that is checked to be retrieved
     * @return The GETFIELD constraint
     */
    protected abstract Constraint _getField(Constraint guard, String fieldName, Sprimitive value);

    /**
     * Should be overridden to differentiate between aliasing partner class object solver representations and "simple"
     * solver representations that represent only themselves.
     * Modifies this representation and, in the case of symbolic aliasing, the other representations conditionally.
     * @param guard The guard determining whether the putfield should be valid
     * @param fieldName The field. Should follow the pattern: packageName.className.fieldName
     * @param value The value that is checked to be put into an instance field
     */
    protected abstract void _putField(Constraint guard, String fieldName, Sprimitive value);

    @Override
    public Sint getId() {
        return id;
    }

    @Override
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
        //// TODO ...if symbolic init to null is allowed, regard this here for lazily initialized objects
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
