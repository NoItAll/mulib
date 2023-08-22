package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.SymSnumber;

import java.util.Arrays;
import java.util.Set;

/**
 * Represents the initialization of an array in the solver backend. This is not done for arrays that should not be
 * represented for or in the solver.
 * There are multiple types of initializing an array, as is shown in {@link Type}.
 */
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
         * The Sarray was initialized when accessing an element in an array that is represented for the solver.
         */
        SARRAY_IN_SARRAY,
        /**
         * The Sarray was initialized when accessing a field of an object that is represented for the solver.
         */
        SARRAY_IN_PARTNER_CLASS_OBJECT
    }

    private final Type type;
    private final Sint arrayId;
    private final Sint reservedId;
    // If is not null: Contains all those array-ids which arrayId can equal to
    private final Set<Sint> potentialIds;
    private final ArrayAccessConstraint[] initialSelectConstraints;
    // Either the ID of the partner class object or the SarraySarray containing this
    private final Sint containingPartnerClassObjectId;
    private final Sint arrayLength;
    private final Sbool isNull;
    private final Class<?> valueType;
    private final boolean defaultIsSymbolic;
    // Only set if type == SARRAY_IN_PARTNER_CLASS_OBJECT
    private final String fieldName;
    // Only set if type == SARRAY_IN_SARRAY
    private final Sint index;

    private ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic,
            String fieldName,
            Sint index) {
        assert isNull != null;
        assert valueType != null;
        assert !Sarray.class.isAssignableFrom(valueType);
        assert arrayId != null;
        assert arrayLength != null;
        assert fieldName == null || index == null;
        assert potentialIds == null || containingPartnerClassObjectId == null;
        assert initialSelectConstraints != null;
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getType() == ArrayAccessConstraint.Type.SELECT);
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getPartnerClassObjectId() == arrayId);
        assert Arrays.stream(initialSelectConstraints).noneMatch(isc -> isc.getIndex() instanceof Sym);
        this.initialSelectConstraints = initialSelectConstraints;
        if (fieldName != null) {
            assert arrayId instanceof SymSnumber;
            this.type = Type.SARRAY_IN_PARTNER_CLASS_OBJECT;
        } else if (index != null) {
            this.type = Type.SARRAY_IN_SARRAY;
        } else if (potentialIds == null && containingPartnerClassObjectId == null) {
            this.type = Type.SIMPLE_SARRAY;
        } else {
            assert arrayId instanceof SymSnumber;
            assert potentialIds != null && !potentialIds.isEmpty();
            this.type = Type.ALIASED_SARRAY;
        }
        this.arrayId = arrayId;
        this.reservedId = reservedId;
        this.potentialIds = potentialIds;
        this.containingPartnerClassObjectId = containingPartnerClassObjectId;
        this.arrayLength = arrayLength;
        this.isNull = isNull;
        this.valueType = valueType;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.fieldName = fieldName;
        this.index = index;
    }

    /**
     * Constructor for symbolically representing a usual array
     * @param arrayId The identifier of the array object to initialize for the solver
     * @param arrayLength The length of the array object to initialize for the solver
     * @param isNull Whether the initialized array can be null
     * @param valueType The component type
     * @param initialSelectConstraints The initial content of the array, represented as SELECT-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, null, null, valueType, initialSelectConstraints,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing an array using aliasing
     * @param arrayId The identifier of the array object to initialize for the solver
     * @param arrayLength The length of the array object to initialize for the solver
     * @param isNull Whether the initialized array can be null
     * @param reservedId The reserved identifier in case this is not a pure alias, but potentially a new array
     * @param potentialIds The aliasing targets
     * @param valueType The component type
     * @param initialSelectConstraints The initial content of the array, represented as SELECT-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Set<Sint> potentialIds,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, potentialIds, reservedId, null, valueType, initialSelectConstraints,
                defaultIsSymbolic, null, null);
    }

    /**
     * Constructor for symbolically representing an array symbolically generated by accessing a
     * partner class object's field or by accessing a sarray of sarrays via an index
     * @param arrayId The identifier of the array object to initialize for the solver
     * @param arrayLength The length of the array object to initialize for the solver
     * @param isNull Whether the initialized array can be null
     * @param reservedId The reserved identifier in case this is not a pure alias, but potentially a new array
     * @param fieldName The name of the field of the non-array object with the identifier containingPartnerClassObjectId,
     *                  if any. Either fieldName or index must be set. Also contains the name of the class to which this field belongs.
     * @param index The index of the array object with the identifier containingPartnerClassObjectId, if any.
     *              Either fieldName or index must be set.
     * @param valueType The component type
     * @param initialSelectConstraints The initial content of this object, encoded in select-constraints
     * @param defaultIsSymbolic Whether the default value when accessing this object's fields is symbolic
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Sint containingPartnerClassObjectId,
            String fieldName,
            Sint index,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean defaultIsSymbolic) {
        this(arrayId, arrayLength, isNull, null, reservedId, containingPartnerClassObjectId, valueType, initialSelectConstraints,
                defaultIsSymbolic, fieldName, index);
    }

    /**
     * Only set if {@link ArrayInitializationConstraint#getType()} is set to {@link Type#SARRAY_IN_PARTNER_CLASS_OBJECT}.
     * @return The field name in which the array was initialized, if any.
     */
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Sint getPartnerClassObjectId() {
        return arrayId;
    }

    /**
     * @return The type of initialization
     */
    public Type getType() {
        return type;
    }

    /**
     * @return The length of the initialized array
     */
    public Sint getArrayLength() {
        return arrayLength;
    }

    /**
     * @return Whether the initialized array is null
     */
    public Sbool getIsNull() {
        return isNull;
    }

    /**
     * @return The id reserved to this array if it can be a new instance rather than referring to an existing array
     */
    public Sint getReservedId() {
        return reservedId;
    }

    /**
     * Only set if {@link ArrayInitializationConstraint#getType()} is set to {@link Type#ALIASED_SARRAY}.
     * @return The set of identifiers that the initialized array can be an alias of.
     */
    public Set<Sint> getPotentialIds() {
        return potentialIds;
    }

    /**
     * Only set if {@link ArrayInitializationConstraint#getType()} is set to {@link Type#SARRAY_IN_PARTNER_CLASS_OBJECT}
     * or {@link Type#SARRAY_IN_SARRAY}.
     * @return The identifier of the {@link Sarray} or {@link de.wwu.mulib.substitutions.PartnerClass} containing
     * this array. In other words: If we access the field of an object or a position in an array that is represented for the solver,
     * the result is a new symbolic object. If this object is an array, an ArrayInitializationConstraint is created
     * where the identifier of the sarray/partnerclass containing this array is returned via this method.
     */
    public Sint getContainingPartnerClassObjectId() {
        return containingPartnerClassObjectId;
    }

    /**
     * Only set if {@link ArrayInitializationConstraint#getType()} is set to {@link Type#SARRAY_IN_SARRAY}.
     * @return The index in which the array is initialized, if any.
     */
    public Sint getIndex() {
        return index;
    }

    /**
     * @return The initial content of the array, encoded as array-selects, at the point of representing it for the solver.
     * This can be empty if the array is lazily initialized, e.g., due to an unknown length.
     */
    public ArrayAccessConstraint[] getInitialSelectConstraints() {
        return initialSelectConstraints;
    }

    /**
     * @return The component type of the array
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * @return true, if the default for accessing unknown elements is symbolic, else false
     */
    public boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    @Override
    public String toString() {
        return "AR_INIT{arrayId=" + arrayId + ",type=" + type
                + ",valueType=" + valueType + "}";
    }
}
