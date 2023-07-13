package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

import java.util.Arrays;
import java.util.Set;

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
         * The PartnerClassObject was initialized when accessing an element in a free array.
         */
        PARTNER_CLASS_OBJECT_IN_SARRAY,
        /**
         * The PartnerClassObject was initialized when accessing a field of a free object
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
            assert partnerClassObjectId instanceof SymNumericExpressionSprimitive;
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


    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return partnerClassObjectId;
    }

    public Sint getReservedId() {
        return reservedId;
    }

    public Set<Sint> getPotentialIds() {
        return potentialIds;
    }

    public PartnerClassObjectFieldConstraint[] getInitialGetfields() {
        return initialGetfields;
    }

    public Sint getContainingPartnerClassObjectId() {
        return containingPartnerClassObjectId;
    }

    public Sbool isNull() {
        return isNull;
    }

    public boolean isDefaultIsSymbolic() {
        return defaultIsSymbolic;
    }

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
