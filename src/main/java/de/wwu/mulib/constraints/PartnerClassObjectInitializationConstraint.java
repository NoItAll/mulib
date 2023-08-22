package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

import java.util.Arrays;
import java.util.Set;


/**
 * Represents the initialization of a non-array object in the solver backend. This is not done for objects that should not be
 * represented for or in the solver.
 * There are multiple types of initializing an object, as is shown in {@link PartnerClassObjectInitializationConstraint.Type}.
 */
public class PartnerClassObjectInitializationConstraint implements PartnerClassObjectConstraint {

    public enum Type {
        /**
         * The PartnerClassObject was created outside of potential aliasing.
         */
        SIMPLE_PARTNER_CLASS_OBJECT,
        /**
         * The PartnerClassObject was created using aliasing. This can happen when creating a new
         * PartnerClassObject with defaultIsSymbolic or when accessing a symbolic partner-classes field.
         */
        ALIASED_PARTNER_CLASS_OBJECT,
        /**
         * The PartnerClassObject was initialized when accessing an element of an array that was represented for the solver.
         */
        PARTNER_CLASS_OBJECT_IN_SARRAY,
        /**
         * The PartnerClassObject was initialized when accessing a field of an object that was represented for the solver.
         */
        PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT
    }

    private final Type type;
    private final Class<?> clazz;
    private final Sint partnerClassObjectId;
    private final Sint reservedId;
    // If is not null: Contains all those array-ids which arrayId can equal to. If containintPartnerClassObjectId is set,
    // the aliased targets are derived from there
    private final Set<Sint> potentialIds;
    private final PartnerClassObjectFieldConstraint[] initialGetfields;
    // Either the ID of the partner class object or the SarraySarray containing this
    private final Sint containingPartnerClassObjectId;
    private final Sbool isNull;
    private final boolean defaultIsSymbolic;
    // Only set if type == PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT
    private final String fieldName;
    // Only set if type == PARTNER_CLASS_OBJECT_IN_SARRAY
    private final Sint index;

    private PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            PartnerClassObjectFieldConstraint[] initialGetfields,
            boolean defaultIsSymbolic,
            String fieldName,
            Sint index) {
        assert clazz != null;
        assert !Sarray.class.isAssignableFrom(clazz);
        assert !clazz.isArray();
        assert partnerClassObjectId != null;
        assert potentialIds == null || containingPartnerClassObjectId == null;
        assert initialGetfields != null;
        assert Arrays.stream(initialGetfields).allMatch(isc -> isc.getType() == PartnerClassObjectFieldConstraint.Type.GETFIELD);
        assert Arrays.stream(initialGetfields).allMatch(isc -> isc.getPartnerClassObjectId() == partnerClassObjectId);
        this.clazz = clazz;
        this.initialGetfields = initialGetfields;
        if (fieldName != null) {
            this.type = Type.PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT;
        } else if (index != null) {
            this.type = Type.PARTNER_CLASS_OBJECT_IN_SARRAY;
        } else if (potentialIds == null && containingPartnerClassObjectId == null) {
            this.type = Type.SIMPLE_PARTNER_CLASS_OBJECT;
        } else {
            assert partnerClassObjectId instanceof SymSnumber;
            assert potentialIds != null && !potentialIds.isEmpty();
            this.type = Type.ALIASED_PARTNER_CLASS_OBJECT;
        }
        this.partnerClassObjectId = partnerClassObjectId;
        this.reservedId = reservedId;
        this.potentialIds = potentialIds;
        this.containingPartnerClassObjectId = containingPartnerClassObjectId;
        this.isNull = isNull;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.fieldName = fieldName;
        this.index = index;
    }

    /**
     * Constructor for symbolically representing a usual partner class object
     * @param clazz The class of the non-array object to represent
     * @param partnerClassObjectId The identifier of the non-array object to initialize for the solver
     * @param isNull Whether the initialized non-array object can be null
     * @param initialGetfields The initial content of this object, encoded in getfield-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            PartnerClassObjectFieldConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, null, null, null, initialGetfields,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing a partner class object using aliasing
     * @param clazz The class of the non-array object to represent
     * @param partnerClassObjectId The identifier of the non-array object to initialize for the solver
     * @param isNull Whether the initialized non-array object can be null
     * @param reservedId The reserved identifier in case this is not a pure alias, but potentially a new object
     * @param potentialIds The aliasing targets
     * @param initialGetfields The initial content of this object, encoded in getfield-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Sint reservedId,
            Set<Sint> potentialIds,
            PartnerClassObjectFieldConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, potentialIds, reservedId, null, initialGetfields,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing an array symbolically generated by accessing a
     * partner class object's field or by accessing a sarray of partner class objects
     * @param clazz The class of the non-array object to represent
     * @param partnerClassObjectId The identifier of the non-array object to initialize for the solver
     * @param isNull Whether the initialized non-array object can be null
     * @param reservedId The reserved identifier in case this is not a pure alias, but potentially a new object
     * @param containingPartnerClassObjectId The identifier of the object which is accessed and which causes the current object
     *                                       to be initialized for the solver. In other words: If we access the field of
     *                                       an object or a position in an array that is represented for the solver, the
     *                                       result is a new symbolic object. If this object is not an array, a
     *                                       PartnerClassObjectInitializationConstraint is created where the identifier
     *                                       of the sarray/partnerclass containing this non-array object is returned via
     *                                       this method.
     * @param fieldName The name of the field of the non-array object with the identifier containingPartnerClassObjectId,
     *                  if any. Either fieldName or index must be set. Also contains the name of the class to which this field belongs.
     * @param index The index of the array object with the identifier containingPartnerClassObjectId, if any.
     *              Either fieldName or index must be set.
     * @param initialGetfields The initial content of this object, encoded in getfield-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            String fieldName,
            Sint index,
            PartnerClassObjectFieldConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, null, reservedId, containingPartnerClassObjectId, initialGetfields,
                defaultIsSymbolic, fieldName, index);
    }

    /**
     * Only set if {@link PartnerClassObjectInitializationConstraint#getType()} is set to {@link PartnerClassObjectInitializationConstraint.Type#PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT}.
     * @return The field name in which the object was initialized, if any.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return The clazz that the initialized object is an instance of
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @return The type of initialization
     */
    public Type getType() {
        return type;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return partnerClassObjectId;
    }

    /**
     * @return The id reserved to this array if it can be a new instance rather than referring to an existing array
     */
    public Sint getReservedId() {
        return reservedId;
    }

    /**
     * Only set if {@link PartnerClassObjectInitializationConstraint#getType()} is set to {@link PartnerClassObjectInitializationConstraint.Type#ALIASED_PARTNER_CLASS_OBJECT}.
     * @return The set of identifiers that the initialized array can be an alias of.
     */
    public Set<Sint> getPotentialIds() {
        return potentialIds;
    }

    /**
     * @return The initial content of the object, encoded as object-getfields, at the point of representing it for the solver.
     * This can be empty if the object is lazily initialized.
     */
    public PartnerClassObjectFieldConstraint[] getInitialGetfields() {
        return initialGetfields;
    }

    /**
     * Only set if {@link PartnerClassObjectInitializationConstraint#getType()} is set to {@link PartnerClassObjectInitializationConstraint.Type#PARTNER_CLASS_OBJECT_IN_PARTNER_CLASS_OBJECT}
     * or {@link PartnerClassObjectInitializationConstraint.Type#PARTNER_CLASS_OBJECT_IN_SARRAY}.
     * @return The identifier of the {@link Sarray} or {@link de.wwu.mulib.substitutions.PartnerClass} containing
     * this object. In other words: If we access the field of an object or a position in an array that is represented for the solver,
     * the result is a new symbolic object. If this object is not an array, a PartnerClassObjectInitializationConstraint is created
     * where the identifier of the sarray/partnerclass containing this non-array object is returned via this method.
     */
    public Sint getContainingPartnerClassObjectId() {
        return containingPartnerClassObjectId;
    }

    /**
     * @return Whether the initialized object is null
     */
    public Sbool isNull() {
        return isNull;
    }

    /**
     * @return true, if the default for accessing unknown elements is symbolic, else false
     */
    public boolean isDefaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    /**
     * Only set if {@link PartnerClassObjectInitializationConstraint#getType()} is set to {@link PartnerClassObjectInitializationConstraint.Type#PARTNER_CLASS_OBJECT_IN_SARRAY}.
     * @return The index in which the array is initialized, if any.
     */
    public Sint getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return String.format("POInit{type=%s, clazz=%s, id=%s, reservedId=%s, potentialIds=%s, " +
                "initialGetFields=%s, containingPartnerClassObjectId=%s, isNull=%s, " +
                "defaultIsSymbolic=%s, fieldName=%s}",
                type.toString(), clazz.getSimpleName(), partnerClassObjectId.toString(),
                reservedId == null ? "{}" : reservedId.toString(),
                potentialIds == null ? "{}" : potentialIds.toString(),
                Arrays.toString(initialGetfields),
                containingPartnerClassObjectId == null ? "{}" : containingPartnerClassObjectId.toString(),
                isNull.toString(),
                String.valueOf(defaultIsSymbolic),
                fieldName
        );
    }
}
