package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.IdentityHavingSubstitutedVarInformation;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Sarray<T extends SubstitutedVar> implements IdentityHavingSubstitutedVar {
    private static final byte NOT_YET_REPRESENTED_IN_SOLVER = 0;
    private static final byte SHOULD_BE_REPRESENTED_IN_SOLVER = 1;
    private static final byte IS_REPRESENTED_IN_SOLVER = 2;
    private static final byte CACHE_IS_BLOCKED = 4;
    private static final byte DEFAULT_IS_SYMBOLIC = 16;
    // representationState encodes several states that are important when representing an Array as a constraint or
    // when interacting with the constraint solver. Each bit represents one possible state.
    private byte representationState;

    // Is null until to-be-represented in constraint solver
    private Sint id;
    private final Sint len;
    // The type of element stored in the array, e.g., Sarray, Sint, ...
    private final Class<T> clazz;
    protected final Map<Sint, T> cachedElements;
    private Sbool isNull;

    /** New instance constructor */
    protected Sarray(Class<T> clazz, Sint len, SymbolicExecution se,
                     boolean defaultIsSymbolic,
                     Sbool isNull) {
        assert clazz != null && len != null;
        this.id = null;
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.clazz = clazz;
        if (defaultIsSymbolic) {
            setDefaultIsSymbolic();
        }
        this.len = len;
        this.cachedElements = new HashMap<>();
        if (len instanceof ConcSnumber) {
            int length = ((ConcSnumber) len).intVal();
            for (int i = 0; i < length; i++) {
                cachedElements.put(se.concSint(i), defaultIsSymbolic ? symbolicDefault(se) : nonSymbolicDefaultElement(se));
            }
        }
        this.isNull = isNull;
    }

    /** Transformation constructor */
    protected Sarray(
            T[] arrayElements,
            MulibValueTransformer mvt) {
        this.id = null;
        this.clazz = (Class<T>) arrayElements.getClass().getComponentType();
        int length = arrayElements.length;
        this.len = (Sint) mvt.transform(length);
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.cachedElements = new HashMap<>();
        for (int i = 0; i < arrayElements.length; i++) {
            cachedElements.put(Sint.concSint(i), arrayElements[i]);
        }
        this.isNull = Sbool.ConcSbool.FALSE;
    }

    /** Copy constructor for all Sarrays but SarraySarray */
    protected Sarray(MulibValueCopier mvt, Sarray<T> s) {
        this(mvt, s, new HashMap<>(s.cachedElements));
    }

    /** Copy constructor for SarraySarrays */
    private Sarray(MulibValueCopier mvt, Sarray<T> s, Map<Sint, T> cachedElements) {
        mvt.registerCopy(s, this);
        this.id = s.getId();
        this.representationState = s.representationState;
        this.clazz = s.clazz;
        this.cachedElements = cachedElements;
        this.len = s.len;
        this.isNull = s.isNull;
    }

    protected final void initializeId(SymbolicExecution se) {
        _initializeId(se.concSint(se.getNextNumberInitializedSymSarray()));
    }

    private void initializeForAliasingAndBlockCache(SymbolicExecution se) {
        _initializeId(se.symSint());
        blockCache();
    }

    protected final void _initializeId(Sint id) {
        if (this.id != null) {
            throw new MulibRuntimeException("Must not set already set id");
        }

        this.representationState |= SHOULD_BE_REPRESENTED_IN_SOLVER;
        this.id = id;
    }

    protected final void setDefaultIsSymbolic() {
        this.representationState |= DEFAULT_IS_SYMBOLIC;
    }

    @Override
    public String toString() {
        return "Sarray[" + id + "]{repState=" + representationState + ",elements=" + cachedElements + "}";
    }

    protected abstract T symbolicDefault(SymbolicExecution se);

    protected abstract T nonSymbolicDefaultElement(SymbolicExecution se);

    public final boolean shouldBeRepresentedInSolver() {
        return (representationState & SHOULD_BE_REPRESENTED_IN_SOLVER) != 0;
    }

    public final boolean isRepresentedInSolver() {
        assert (representationState & IS_REPRESENTED_IN_SOLVER) == 0 || shouldBeRepresentedInSolver();
        return (representationState & IS_REPRESENTED_IN_SOLVER) != 0;
    }

    public void setAsRepresentedInSolver() {
        representationState |= IS_REPRESENTED_IN_SOLVER;
    }

    protected void blockCache() {
        representationState |= CACHE_IS_BLOCKED;
    }

    protected boolean isCacheBlocked() {
        return (representationState & CACHE_IS_BLOCKED) != 0;
    }

    public void clearCache() {
        cachedElements.clear();
    }

    public final boolean defaultIsSymbolic() {
        return (representationState & DEFAULT_IS_SYMBOLIC) != 0;
    }


    /**
     * Returns the type of the elements stored in the Sarray. In the case of Sarray.SarraySarray, the type of
     * element-Sarrays is returned.
     * @return The class that represents the type of elements stored in the Sarray
     * @see SarraySarray#getElementType()
     */
    public final Class<T> getClazz() {
        return clazz;
    }

    /**
     * Is equivalent tp Sarray.getClazz() except for SarraySarray.
     * @return The class that stores the actual type stored in the Sarray
     * @see SarraySarray#getElementType()
     */
    public Class<?> getElementType() {
        return getClazz();
    }

    public abstract T select(Sint i, SymbolicExecution se);

    public abstract void store(Sint i, T val, SymbolicExecution se);

    @Override
    public void prepareToRepresentSymbolically(SymbolicExecution se) {
        initializeId(se);
        assert cachedElements.keySet().stream().allMatch(s -> s instanceof ConcSnumber);
    }

    public final Set<Sint> getCachedIndices() {
        return cachedElements.keySet();
    }

    public final Iterable<T> getCachedElements() {
        return cachedElements.values();
    }

    public final Sint _getLengthWithoutCheckingForIsNull() {
        return len;
    }

    public void nullCheck() {
        _nullCheck();
    }

    private void _nullCheck() {
        if (isNull != Sbool.ConcSbool.FALSE) {
            SymbolicExecution se = SymbolicExecution.get();
            if (isNull.boolChoice(se)) {
                throw new NullPointerException();
            } else {
                setIsNotNull();
            }
        }
    }

    public final Sint getLength() {
        _nullCheck();
        return len;
    }

    public final Sint length() {
        return getLength();
    }

    public final T getFromCacheForIndex(Sint index) {
        return cachedElements.get(index);
    }

    public void setInCacheForIndexForSelect(Sint index, T value) {
        if (!isCacheBlocked()) {
            cachedElements.put(index, value);
        }
    }

    public void setInCacheForIndexForStore(Sint index, T value) {
        if (!isCacheBlocked()) {
            cachedElements.put(index, value);
        }
    }

    public final Sint getId() {
        return id;
    }

    public abstract Sarray<T> copy(MulibValueCopier mvt);

    public final Sbool isNull() {
        return isNull;
    }

    public final void setIsNotNull() {
        isNull = Sbool.ConcSbool.FALSE;
    }

    @SuppressWarnings("rawtypes")
    public static void checkIfValueIsStorableForSarray(Sarray sarray, SubstitutedVar value) {
        if (!(sarray instanceof Sarray.SarraySarray)) {
            // Either is no SarraySarray
            if (value == null && Sprimitive.class.isAssignableFrom(sarray.getClazz())
                    || (value != null && !sarray.getClazz().isInstance(value))) {
                // Then an ArrayStoreException should be thrown if null is to be stored and the Sarray does hold primitive values.
                // If the value is not null, it must be assignable to the type stored in the Sarray.
                throw new ArrayStoreException();
            }
        } else {
            SarraySarray ss = (SarraySarray) sarray;
            // If it is a Sarray,
            if (value == null) {
                // Null is valid for SarraySarray
                return;
            }
            if (ss.elementsAreSarraySarrays()) {
                // If the sarray stores sarrays, we have to compare it accordingly
                if (!(value instanceof SarraySarray)) {
                    throw new ArrayStoreException();
                }
                SarraySarray ssv = (SarraySarray) value;
                if (ss.getElementType().getComponentType() != ssv.getElementType()) {
                    throw new ArrayStoreException();
                }
            } else {
                if (!(value instanceof Sarray)) {
                    throw new ArrayStoreException();
                }
                if (ss.getElementType().getComponentType() != ((Sarray) value).getClazz()) {
                    throw new ArrayStoreException();
                }
            }
        }
    }

    public T getNewValueForSelect(SymbolicExecution se) {
        T result;
        if (!defaultIsSymbolic() && !shouldBeRepresentedInSolver()) {
            result = nonSymbolicDefaultElement(se);
        } else {
            // If symbolic is required, optional aliasing etc. is handled here
            result = symbolicDefault(se);
        }
        return result;
    }

    public static class SintSarray extends Sarray<Sint> {
        /** Transformation constructor */
        public SintSarray(Sint[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SintSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sint.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SintSarray(MulibValueCopier mvt, SintSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sint select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sint val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sint symbolicDefault(SymbolicExecution se) {
            return se.symSint();
        }

        @Override
        protected Sint nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sint.ConcSint.ZERO;
        }

        @Override
        public SintSarray copy(MulibValueCopier mvt) {
            return new SintSarray(mvt, this);
        }
    }

    public static class SdoubleSarray extends Sarray<Sdouble> {

        /** Transformation constructor */
        public SdoubleSarray(Sdouble[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SdoubleSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sdouble.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SdoubleSarray(MulibValueCopier mvt, SdoubleSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sdouble select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sdouble val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sdouble symbolicDefault(SymbolicExecution se) {
            return se.symSdouble();
        }

        @Override
        protected Sdouble nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sdouble.ConcSdouble.ZERO;
        }

        @Override
        public SdoubleSarray copy(MulibValueCopier mvt) {
            return new SdoubleSarray(mvt, this);
        }
    }

    public static class SfloatSarray extends Sarray<Sfloat> {

        /** Transformation constructor */
        public SfloatSarray(Sfloat[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SfloatSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sfloat.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SfloatSarray(MulibValueCopier mvt, SfloatSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sfloat select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sfloat val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sfloat symbolicDefault(SymbolicExecution se) {
            return se.symSfloat();
        }

        @Override
        protected Sfloat nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sfloat.ConcSfloat.ZERO;
        }

        @Override
        public SfloatSarray copy(MulibValueCopier mvt) {
            return new SfloatSarray(mvt, this);
        }
    }

    public static class SlongSarray extends Sarray<Slong> {

        /** Transformation constructor */
        public SlongSarray(Slong[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SlongSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Slong.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SlongSarray(MulibValueCopier mvt, SlongSarray s) {
            super(mvt, s);
        }

        @Override
        public final Slong select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Slong val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Slong symbolicDefault(SymbolicExecution se) {
            return se.symSlong();
        }

        @Override
        protected Slong nonSymbolicDefaultElement(SymbolicExecution se) {
            return Slong.ConcSlong.ZERO;
        }

        @Override
        public SlongSarray copy(MulibValueCopier mvt) {
            return new SlongSarray(mvt, this);
        }
    }

    public static class SshortSarray extends Sarray<Sshort> {

        /** Transformation constructor */
        public SshortSarray(Sshort[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SshortSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sshort.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SshortSarray(MulibValueCopier mvt, SshortSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sshort select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sshort val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sshort symbolicDefault(SymbolicExecution se) {
            return se.symSshort();
        }

        @Override
        protected Sshort nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sshort.ConcSshort.ZERO;
        }

        @Override
        public SshortSarray copy(MulibValueCopier mvt) {
            return new SshortSarray(mvt, this);
        }
    }

    public static class SbyteSarray extends Sarray<Sbyte> {

        /** Transformation constructor */
        public SbyteSarray(Sbyte[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SbyteSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sbyte.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SbyteSarray(MulibValueCopier mvt, SbyteSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sbyte select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sbyte val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sbyte symbolicDefault(SymbolicExecution se) {
            return se.symSbyte();
        }

        @Override
        protected Sbyte nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sbyte.ConcSbyte.ZERO;
        }

        @Override
        public SbyteSarray copy(MulibValueCopier mvt) {
            return new SbyteSarray(mvt, this);
        }
    }

    public static class SboolSarray extends Sarray<Sbool> {

        /** Transformation constructor */
        public SboolSarray(Sbool[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public SboolSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sbool.class, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public SboolSarray(MulibValueCopier mvt, SboolSarray s) {
            super(mvt, s);
        }

        @Override
        public final Sbool select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sbool val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sbool symbolicDefault(SymbolicExecution se) {
            return se.symSbool();
        }

        @Override
        protected Sbool nonSymbolicDefaultElement(SymbolicExecution se) {
            return Sbool.ConcSbool.FALSE;
        }

        @Override
        public SboolSarray copy(MulibValueCopier mvt) {
            return new SboolSarray(mvt, this);
        }
    }

    public static class PartnerClassSarray<T extends PartnerClass> extends Sarray<T> {

        /** Transformation constructor */
        public PartnerClassSarray(T[] values, MulibValueTransformer mvt) {
            super(values, mvt);
        }

        /** New instance constructor */
        public PartnerClassSarray(Class<T> clazz, Sint len, SymbolicExecution se,
                                  boolean defaultIsSymbolic, Sbool isNull) {
            super(clazz, len, se, defaultIsSymbolic, isNull);
        }

        /** Copy constructor */
        public PartnerClassSarray(MulibValueCopier mvt, PartnerClassSarray<T> s) {
            super(mvt, s);
        }

        @Override
        public final T select(Sint i, SymbolicExecution se) {
            T result = (T) se.select(this, i);
            assert result == null || getClazz().isAssignableFrom(result.getClass());
            return result;
        }

        @Override
        public final void store(Sint i, T val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected T symbolicDefault(SymbolicExecution se) {
            throw new NotYetImplementedException(); /// TODO
        }

        @Override
        protected T nonSymbolicDefaultElement(SymbolicExecution se) {
            return null;
        }

        @Override
        public PartnerClassSarray<T> copy(MulibValueCopier mvt) {
            return new PartnerClassSarray<>(mvt, this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static class SarraySarray extends Sarray<Sarray> {

        private final int dim;
        // The type of element stored in the array, but represented as a real array, e.g.: Sint[], Sdouble[][], etc.
        // Sarray.clazz would represent them all as Sarray.
        private final Class<?> elementType;

        /** Transformation constructor */
        public SarraySarray(Sarray[] values, Class<?> elementType, MulibValueTransformer mvt) {
            super(values, mvt);
            this.elementType = elementType;
            this.dim = determineDimFromInnerElementType(elementType);
        }

        /** New instance constructor */
        public SarraySarray(Sint len, SymbolicExecution se,
                            boolean defaultIsSymbolic,
                            Class<?> elementType,
                            Sbool isNull) {
            super(Sarray.class, len, se, defaultIsSymbolic, isNull);
            this.elementType = elementType;
            this.dim = determineDimFromInnerElementType(elementType);
            assert !(len instanceof ConcSnumber) || ((ConcSnumber) len).intVal() == getCachedIndices().size();
            assert elementType.isArray() && dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
        }

        /** Other new instance constructor for MULTIANEWARRAY bytecode. This is not implemented too efficiently
         * if any of the lengths is symbolic.
         */
        @SuppressWarnings("unchecked")
        public SarraySarray(
                Sint[] lengths,
                SymbolicExecution se, Class<?> elementType) {
            super(Sarray.class, lengths[0], se, false, Sbool.ConcSbool.FALSE);
            assert elementType.isArray();
            this.dim = determineDimFromInnerElementType(elementType);
            assert dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
            assert dim >= lengths.length : "Dim is always >= the total number of specified lengths";
            this.elementType = elementType;
            Sint i = Sint.ConcSint.ZERO;
            while (i.ltChoice(getLength(), se)) {
                Sint[] nextLengths = new Sint[lengths.length-1];
                System.arraycopy(lengths, 1, nextLengths, 0, nextLengths.length);
                se.store(this, i,
                        generateNonSymbolicSarrayDependingOnStateForMultiANewArray(
                                nextLengths,
                                (Class<? extends SubstitutedVar>) elementType.getComponentType(),
                                se
                        )
                );
                i = i.add(Sint.ConcSint.ONE, se);
                lengths = nextLengths;
            }
        }

        /** Copy constructor */
        public SarraySarray(MulibValueCopier mvt, SarraySarray s) {
            super(mvt, s, copyArrayElements(mvt, s.cachedElements));
            this.dim = s.dim;
            this.elementType = s.elementType;
        }

        private static Map<Sint, Sarray> copyArrayElements(MulibValueCopier mvt, Map<Sint, Sarray> elementCache) {
            Map<Sint, Sarray> elements = new HashMap<>();
            for (Sint i : elementCache.keySet()) {
                elements.put(i, elementCache.get(i).copy(mvt));
            }
            return elements;
        }

        private static int determineDimFromInnerElementType(Class<?> innerElementType) {
            int i = 1; // The SarraySarray this belongs to also counts
            while (innerElementType.getComponentType() != null) {
                innerElementType = innerElementType.getComponentType();
                i++;
            }
            return i;
        }

        @Override
        public Class<?> getElementType() {
            return elementType;
        }

        public boolean elementsAreSarraySarrays() {
            return dim > 2;
        }

        private Sarray generateNonSymbolicSarrayDependingOnStateForMultiANewArray(
                Sint[] lengths,
                Class<? extends SubstitutedVar> nextInnerElementsType, SymbolicExecution se) {
            if (elementsAreSarraySarrays()) {
                assert nextInnerElementsType.isArray();
                assert lengths.length > 2;
                return se.sarraySarray(
                        lengths,
                        nextInnerElementsType
                );
            }
            assert lengths.length == 1;
            return generateNonSarraySarray(false, lengths[0], nextInnerElementsType.getComponentType(), false, se);
        }

        @SuppressWarnings("unchecked")
        private static Sarray generateNonSarraySarray(boolean canBeNull, Sint len, Class<?> innermostType, boolean defaultIsSymbolic, SymbolicExecution se) {
            assert !innermostType.isArray();
            assert Sprimitive.class.isAssignableFrom(innermostType)
                    || PartnerClass.class.isAssignableFrom(innermostType);
            // Determine which kind of array must be set
            if (Sprimitive.class.isAssignableFrom(innermostType)) {
                if (innermostType == Sint.class) {
                    return se.sintSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Slong.class) {
                    return se.slongSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Sdouble.class) {
                    return se.sdoubleSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Sfloat.class) {
                    return se.sfloatSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Sshort.class) {
                    return se.sshortSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Sbyte.class) {
                    return se.sbyteSarray(len, defaultIsSymbolic, canBeNull);
                } else if (innermostType == Sbool.class) {
                    return se.sboolSarray(len, defaultIsSymbolic, canBeNull);
                } else {
                    throw new NotYetImplementedException();
                }
            } else if (PartnerClass.class.isAssignableFrom(innermostType)) {
                return se.partnerClassSarray(
                        len,
                        (Class<? extends PartnerClass>) innermostType,
                        defaultIsSymbolic
                );
            } else {
                throw new NotYetImplementedException();
            }
        }

        @Override
        public final Sarray select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Sarray val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Sarray symbolicDefault(SymbolicExecution se) {
            assert isRepresentedInSolver();
            Sarray result;
            IdentityHavingSubstitutedVarInformation info =
                    // TODO Avoid using ConcolicNumericContainer-call here
                    se.getAvailableInformationOnIdentityHavingSubstitutedVar((Sint) ConcolicNumericContainer.tryGetSymFromConcolic(this.getId()));
            // TODO Performance enhancement: only check info.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault if
            //  sarrays are allowed to be initialized to null
            boolean canBeNull = info.canContainExplicitNull || info.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
            if (elementsAreSarraySarrays()) {
                assert elementType.getComponentType().isArray();
                result = se.sarraySarray(
                        se.symSint(),
                        elementType.getComponentType(),
                        true,
                        canBeNull
                );
            } else {
                result = generateNonSarraySarray(
                        canBeNull,
                        se.symSint(),
                        elementType.getComponentType(),
                        true,
                        se
                );
            }

            result.initializeForAliasingAndBlockCache(se);
            se.getCalculationFactory().representArrayIfNeeded(se, result, getId());
            return result;
        }

        @Override
        protected Sarray nonSymbolicDefaultElement(SymbolicExecution se) {
            return null;
        }

        @Override
        public SarraySarray copy(MulibValueCopier mvt) {
            return new SarraySarray(mvt, this);
        }

        @Override
        protected void blockCache() {
            for (Map.Entry<Sint, Sarray> entry : cachedElements.entrySet()) {
                Sarray val = entry.getValue();
                if (val == null) {
                    continue;
                }
                val.blockCache();
            }
            super.blockCache();
        }

        @Override
        public void setAsRepresentedInSolver() {
            super.setAsRepresentedInSolver();
            for (Map.Entry<Sint, Sarray> entry : cachedElements.entrySet()) {
                Sarray val = entry.getValue();
                if (val == null) {
                    continue;
                }
                assert val.isRepresentedInSolver();
                val.clearCache();
            }
        }

        @Override
        public void clearCache() {
            for (Map.Entry<Sint, Sarray> entry : cachedElements.entrySet()) {
                Sarray val = entry.getValue();
                if (val == null) {
                    continue;
                }
                val.clearCache();
            }
            super.clearCache();
        }

        @Override
        public void setInCacheForIndexForSelect(Sint index, Sarray value) {
            if (!isCacheBlocked()) {
                cachedElements.put(index, value);
            }
        }

        @Override
        public void setInCacheForIndexForStore(Sint index, Sarray value) {
            if (!isCacheBlocked()) {
                cachedElements.put(index, value);
            }
        }

        @Override
        public void prepareToRepresentSymbolically(SymbolicExecution se) {
            initializeId(se);
            assert cachedElements.keySet().stream().allMatch(s -> s instanceof ConcSnumber);
            for (Map.Entry<Sint, Sarray> entry : cachedElements.entrySet()) {
                Sarray val = entry.getValue();
                if (val == null) {
                    continue;
                }
                val.blockCache();
            }
        }
    }

}
