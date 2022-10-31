package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Arrays;
import java.util.Map;
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
        PARTNER_CLASS_OBJECT_IN_SARRAY
    }

    private final Type type;
    private final Class<?> clazz;
    private final Sint partnerClassObjectId;
    private final Sint reservedId;
    // If is not null: Contains all those array-ids which arrayId can equal to
    private final Set<Sint> potentialIds;
    private final PartnerClassObjectFieldAccessConstraint[] initialGetfields;
    private final Sint containingSarraySarrayId;
    private final Sbool isNull;
    private final Map<String, Class<?>> fieldTypes;
    private final boolean defaultIsSymbolic;

    private PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Map<String, Class<?>> fieldTypes,
            PartnerClassObjectFieldAccessConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        assert potentialIds == null || containingSarraySarrayId == null;
        assert initialGetfields != null && clazz != null;
        assert Arrays.stream(initialGetfields).allMatch(isc -> isc.getType() == PartnerClassObjectFieldAccessConstraint.Type.GETFIELD);
        assert Arrays.stream(initialGetfields).allMatch(isc -> isc.getPartnerClassObjectId() == partnerClassObjectId);
        this.clazz = clazz;
        this.initialGetfields = initialGetfields;
        if (potentialIds == null && containingSarraySarrayId == null) {
            this.type = Type.SIMPLE_PARTNER_CLASS_OBJECT;
        } else if (potentialIds == null) {
            this.type = Type.PARTNER_CLASS_OBJECT_IN_SARRAY;
        } else {
            this.type = Type.ALIASED_PARTNER_CLASS_OBJECT;
        }
        this.partnerClassObjectId = partnerClassObjectId;
        this.reservedId = reservedId;
        this.potentialIds = potentialIds;
        this.containingSarraySarrayId = containingSarraySarrayId;
        this.isNull = isNull;
        this.fieldTypes = fieldTypes;
        this.defaultIsSymbolic = defaultIsSymbolic;
    }

    /**
     * Constructor for symbolically representing a usual partner class object
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Map<String, Class<?>> fieldTypes,
            PartnerClassObjectFieldAccessConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, null, null, null, fieldTypes, initialGetfields,
                defaultIsSymbolic);
    }

    /**
     * Constructor for symbolically representing a partner class object contained in an array of partner class objects
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Map<String, Class<?>> fieldTypes,
            PartnerClassObjectFieldAccessConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, null, reservedId, containingSarraySarrayId, fieldTypes, initialGetfields,
                defaultIsSymbolic);
    }

    /**
     * Constructor for symbolically representing a partner class object using aliasing
     */
    public PartnerClassObjectInitializationConstraint(
            Class<?> clazz,
            Sint partnerClassObjectId,
            Sbool isNull,
            Set<Sint> potentialIds,
            Map<String, Class<?>> fieldTypes,
            PartnerClassObjectFieldAccessConstraint[] initialGetfields,
            boolean defaultIsSymbolic) {
        this(clazz, partnerClassObjectId, isNull, potentialIds, null, null, fieldTypes, initialGetfields,
                defaultIsSymbolic);
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

    public PartnerClassObjectFieldAccessConstraint[] getInitialGetfields() {
        return initialGetfields;
    }

    public Sint getContainingSarraySarrayId() {
        return containingSarraySarrayId;
    }

    public Sbool getIsNull() {
        return isNull;
    }

    public Map<String, Class<?>> getFieldTypes() {
        return fieldTypes;
    }

    public boolean isDefaultIsSymbolic() {
        return defaultIsSymbolic;
    }
}
