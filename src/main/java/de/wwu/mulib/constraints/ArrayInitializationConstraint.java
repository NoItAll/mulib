package de.wwu.mulib.constraints;

import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

import java.util.Arrays;
import java.util.Set;

public final class ArrayInitializationConstraint implements ArrayConstraint {

    public enum Type {
        SIMPLE_SARRAY, ALIASED_SARRAY, SARRAY_IN_SARRAY
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
    private final boolean isCompletelyInitialized;
    private final boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization;

    private ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization) {
        assert potentialIds == null || containingSarraySarrayId == null;
        assert initialSelectConstraints != null;
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getType() == ArrayAccessConstraint.Type.SELECT);
        assert Arrays.stream(initialSelectConstraints).allMatch(isc -> isc.getArrayId() == arrayId);
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
        this.isCompletelyInitialized = isCompletelyInitialized;
        this.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization = canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization;
    }

    /**
     * Constructor for symbolically representing a usual array
     * @param arrayId The arrayId of the array that is represented
     * @param arrayLength The length of the array that is represented
     * @param isNull The Sbool representing the possibility to be null of the array
     * @param valueType The element types of the array
     * @param initialSelectConstraints The content of the array upon initialization
     * @param isCompletelyInitialized Whether there is a constraint for each of the possible indices of the array
     * @param canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization
     * Whether there can be a value that is not yet set and that has an array-specific default value, such as null for
     * SarraySarrays or Sint.ConcSint.ZERO for SintSarray.
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization) {
        this(arrayId, arrayLength, isNull, null, null, null, valueType, initialSelectConstraints,
                isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization);
    }

    /**
     * Constructor for symbolically representing an array contained in an array of arrays
     * @param arrayId The arrayId of the array that is represented
     * @param arrayLength The length of the array that is represented
     * @param isNull The Sbool representing the possibility to be null of the array
     * @param reservedId The id reserved for forming a potentially new array
     * @param containingSarraySarrayId The id of the array of arrays that is to be represented
     * @param valueType The element types of the array
     * @param initialSelectConstraints The content of the array upon initialization
     * @param isCompletelyInitialized Whether there is a constraint for each of the possible indices of the array
     * @param canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization
     * Whether there can be a value that is not yet set and that has an array-specific default value, such as null for
     * SarraySarrays or Sint.ConcSint.ZERO for SintSarray.
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Sint reservedId,
            Sint containingSarraySarrayId,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization) {
        this(arrayId, arrayLength, isNull, null, reservedId, containingSarraySarrayId, valueType, initialSelectConstraints,
                isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization);
    }

    /**
     * Constructor for symbolically representing an array using aliasing
     * @param arrayId The arrayId of the array that is represented
     * @param arrayLength The length of the array that is represented
     * @param isNull The Sbool representing the possibility to be null of the array
     * @param potentialIds The ids this array might be representing
     * @param valueType The element types of the array
     * @param initialSelectConstraints The content of the array upon initialization
     * @param isCompletelyInitialized Whether there is a constraint for each of the possible indices of the array
     * @param canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization
     * Whether there can be a value that is not yet set and that has an array-specific default value, such as null for
     * SarraySarrays or Sint.ConcSint.ZERO for SintSarray.
     */
    public ArrayInitializationConstraint(
            Sint arrayId,
            Sint arrayLength,
            Sbool isNull,
            Set<Sint> potentialIds,
            Class<?> valueType,
            ArrayAccessConstraint[] initialSelectConstraints,
            boolean isCompletelyInitialized,
            boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization) {
        this(arrayId, arrayLength, isNull, potentialIds, null, null, valueType, initialSelectConstraints,
                isCompletelyInitialized, canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization);
    }

    @Override
    public Sint getArrayId() {
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

    public boolean isCompletelyInitialized() {
        return isCompletelyInitialized;
    }

    public boolean canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization() {
        return canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization;
    }

    @Override
    public String toString() {
        return "AR_INIT{arrayId=" + arrayId + ",type=" + type
                + ",valueType=" + valueType + ",completelyInitialized=" + isCompletelyInitialized
                + ",canPotentiallyContainCurrentlyUnsetNonSymbolicDefaultAtInitialization=" + canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefaultAtInitialization + "}";
    }
}
