package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Arrays;
import java.util.Set;

public final class ArrayInitializationConstraint implements ArrayConstraint {

    /**
     * Denotes the circumstances of initialization of the array.
     */
    public enum Type {
        /**
         * The Sarray was created outside of potential aliasing.
         */
        SIMPLE_SARRAY,
        /**
         * The Sarray was created using aliasing. This can happen when creating a new
         * Sarray with defaultIsSymbolic or when accessing a symbolic partner-classes field.
         */
        ALIASED_SARRAY,
        /**
         * The Sarray was initialized when accessing an element in a free array.
         */
        SARRAY_IN_SARRAY
    }

    private final Type type;
    private final Sint arrayId;
    private final Sint reservedId;
    // If is not null: Contains all those array-ids which arrayId can equal to
    private final Set<Sint> potentialIds;
    private final ArrayAccessConstraint[] initialSelectConstraints;
    private final Sint containingSarraySarrayId;
    private final Sint arrayLength;
    private final Sbool isNull;
    private final Class<?> valueType;
    private final boolean defaultIsSymbolic;

    private ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        assert potentialIds == null || containingSarraySarrayId == null;
        assert initialSelectConstraints != null;
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getType() == ArrayAccessConstraint.Type.SELECT);
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getPartnerClassObjectId() == arrayId);
        assert Arrays.stream(initialSelectConstraints).noneMatch(isc -> isc.getIndex() instanceof Sym);
        this.initialSelectConstraints = initialSelectConstraints;
        if (potentialIds == null && containingSarraySarrayId == null) {
            this.type = Type.SIMPLE_SARRAY;
        } else if (potentialIds == null) {
            this.type = Type.SARRAY_IN_SARRAY;
        } else {
            this.type = Type.ALIASED_SARRAY;
        }
        this.arrayId = arrayId;
        this.reservedId = reservedId;
        this.potentialIds = potentialIds;
        this.containingSarraySarrayId = containingSarraySarrayId;
        this.arrayLength = arrayLength;
        this.isNull = isNull;
        this.valueType = valueType;
        this.defaultIsSymbolic = defaultIsSymbolic;
    }

    /**
     * Constructor for symbolically representing a usual array
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, null, null, valueType, initialSelectConstraints,
                defaultIsSymbolic);
    }

    /**
     * Constructor for symbolically representing an array contained in an array of arrays
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, reservedId, containingSarraySarrayId, valueType, initialSelectConstraints,
                defaultIsSymbolic);
    }

    /**
     * Constructor for symbolically representing an array using aliasing
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, potentialIds, null, null, valueType, initialSelectConstraints,
                defaultIsSymbolic);
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return arrayId;
    }

    public Type getType() {
        return type;
    }

    public Sint getArrayLength() {
        return arrayLength;
    }

    public Sbool getIsNull() {
        return isNull;
    }

    public Sint getReservedId() {
        return reservedId;
    }

    public Set<Sint> getPotentialIds() {
        return potentialIds;
    }

    public Sint getContainingSarraySarrayId() {
        return containingSarraySarrayId;
    }

    public ArrayAccessConstraint[] getInitialSelectConstraints() {
        return initialSelectConstraints;
    }

    public Class<?> getValueType() {
        return valueType;
    }

    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    @Override
    public String toString() {
        return "AR_INIT{arrayId=" + arrayId + ",type=" + type
                + ",valueType=" + valueType + "}";
    }
}
