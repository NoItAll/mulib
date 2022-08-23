package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public abstract class Sarray<T extends SubstitutedVar> implements SubstitutedVar {
    private Sint id;
    private final Sint len;
    // The type of element stored in the array, e.g., Sarray, Sint, ...
    private final Class<T> clazz;
    protected final Map<Sint, T> cachedElements;
    private final boolean defaultIsSymbolic;
    private boolean onlyConcreteIndicesUsed;
    private Sbool isNull;

    /** New instance constructor */
    protected Sarray(Class<T> clazz, Sint len, SymbolicExecution se,
                     boolean defaultIsSymbolic,
                     Sbool isNull) {
        assert clazz != null && len != null;
        this.id = null;
        this.onlyConcreteIndicesUsed = true;
        this.clazz = clazz;
        this.cachedElements = new HashMap<>();
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.len = len;
        if (!defaultIsSymbolic) {
            if (len instanceof ConcSnumber) {
                int length = ((ConcSnumber) len).intVal();
                for (int i = 0; i < length; i++) {
                    cachedElements.put(se.concSint(i), nonSymbolicDefaultElement(se));
                }
            } else {
                throw new NotYetImplementedException("Behavior if length is not concrete and default is not symbolic is not yet implemented");
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
        this.defaultIsSymbolic = false;
        this.onlyConcreteIndicesUsed = true;
        this.cachedElements = new HashMap<>();
        for (int i = 0; i < arrayElements.length; i++) {
            cachedElements.put((Sint) mvt.transform(i), arrayElements[i]);
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
        this.onlyConcreteIndicesUsed = s.onlyConcreteIndicesUsed;
        this.clazz = s.clazz;
        this.cachedElements = cachedElements;
        this.defaultIsSymbolic = s.defaultIsSymbolic;
        this.len = s.len;
        this.isNull = s.isNull;
    }

    public void initializeId(Sint id) {
        if (this.id != null) {
            throw new MulibRuntimeException("Must not set already set id");
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return "Sarray{id=" + id + ", elements=" + cachedElements + "}";
    }

    public abstract T symbolicDefault(SymbolicExecution se);

    public abstract T nonSymbolicDefaultElement(SymbolicExecution se);

    public final boolean onlyConcreteIndicesUsed() {
        return onlyConcreteIndicesUsed;
    }

    public final boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
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

    public abstract T select(Sint i, SymbolicExecution se);

    public abstract void store(Sint i, T val, SymbolicExecution se);

    // If the new constraint is not concrete, we must account for non-deterministic accesses. Therefore,
    // we will add all current stored pairs (i.e. all relevant stores) as constraints to the constraint stack.
    public final boolean checkIfNeedsToRepresentOldEntries(Sint i, SymbolicExecution se) {
        if (onlyConcreteIndicesUsed) {
            if (i instanceof SymNumericExpressionSprimitive) {
                initializeId(se.concSint(se.getNextNumberInitializedSymSarray()));
                onlyConcreteIndicesUsed = false;
                // We do not have to add any constraints if we are on a known path or if there are not yet any elements.
                return !cachedElements.isEmpty(); // We do not need to check for !se.nextIsOnKnownPath() since this can only ever be executed once
            }
        }
        return false;
    }

    public final Set<Sint> getCachedIndices() {
        return cachedElements.keySet();
    }

    public final Collection<T> getCachedElements() {
        return cachedElements.values();
    }

    public final void clearCachedElements() {
        cachedElements.clear();
    }

    public final Sint getLength() {
        return len;
    }

    public final Sint length() {
        return getLength();
    }

    public final T getFromCacheForIndex(Sint index) {
        return cachedElements.get(index);
    }

    public final void setInCacheForIndex(Sint index, T value) {
        cachedElements.put(index, value);
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
        public Sint symbolicDefault(SymbolicExecution se) {
            return se.symSint();
        }

        @Override
        public Sint nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Sdouble symbolicDefault(SymbolicExecution se) {
            return se.symSdouble();
        }

        @Override
        public Sdouble nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Sfloat symbolicDefault(SymbolicExecution se) {
            return se.symSfloat();
        }

        @Override
        public Sfloat nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Slong symbolicDefault(SymbolicExecution se) {
            return se.symSlong();
        }

        @Override
        public Slong nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Sshort symbolicDefault(SymbolicExecution se) {
            return se.symSshort();
        }

        @Override
        public Sshort nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Sbyte symbolicDefault(SymbolicExecution se) {
            return se.symSbyte();
        }

        @Override
        public Sbyte nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public Sbool symbolicDefault(SymbolicExecution se) {
            return se.symSbool();
        }

        @Override
        public Sbool nonSymbolicDefaultElement(SymbolicExecution se) {
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
        public T symbolicDefault(SymbolicExecution se) {
            throw new NotYetImplementedException(); /// TODO
        }

        @Override
        public T nonSymbolicDefaultElement(SymbolicExecution se) {
            return null; /// TODO NullValuePattern there
        }

        @Override
        public PartnerClassSarray<T> copy(MulibValueCopier mvt) {
            return new PartnerClassSarray<T>(mvt, this);
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
            assert elementType.isArray();
            this.dim = determineDimFromInnerElementType(elementType);
            assert dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
        }

        /** Other new instance constructor for MULTIANEWARRAY bytecode */
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
                        generateNonSymbolicSarrayDependingOnState(
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

        private static Map<Sint, Sarray> copyArrayElements(MulibValueCopier mvt, Map<Sint, Sarray> elements) {
            Map<Sint, Sarray> result = new HashMap<>();
            for (Map.Entry<Sint, Sarray> entry : elements.entrySet()) {
                result.put(entry.getKey(), entry.getValue().copy(mvt));
            }
            return result;
        }

        private static int determineDimFromInnerElementType(Class<?> innerElementType) {
            int i = 1; // The SarraySarray this belongs to also counts
            while (innerElementType.getComponentType() != null) {
                innerElementType = innerElementType.getComponentType();
                i++;
            }
            return i;
        }

        public Class<?> getElementType() {
            return elementType;
        }

        public boolean elementsAreSarraySarrays() {
            return dim > 2;
        }

        private Sarray generateNonSymbolicSarrayDependingOnState(
                Sint[] lengths,
                Class<? extends SubstitutedVar> nextInnerElementsType, SymbolicExecution se) {
            if (elementsAreSarraySarrays()) {
                assert nextInnerElementsType.isArray();
                assert lengths.length > 2;
                return SymbolicExecution.sarraySarray(
                        lengths,
                        nextInnerElementsType,
                        se
                );
            }
            return generateNonSarraySarray(lengths[0], nextInnerElementsType.getComponentType(), false, se);
        }

        @SuppressWarnings("unchecked")
        private static Sarray generateNonSarraySarray(Sint len, Class<?> innermostType, boolean defaultIsSymbolic, SymbolicExecution se) {
            assert !innermostType.isArray();
            assert Sprimitive.class.isAssignableFrom(innermostType)
                    || PartnerClass.class.isAssignableFrom(innermostType);
            // Determine which kind of array must be set
            if (Sprimitive.class.isAssignableFrom(innermostType)) {
                if (innermostType == Sint.class) {
                    return SymbolicExecution.sintSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Slong.class) {
                    return SymbolicExecution.slongSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Sdouble.class) {
                    return SymbolicExecution.sdoubleSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Sfloat.class) {
                    return SymbolicExecution.sfloatSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Sshort.class) {
                    return SymbolicExecution.sshortSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Sbyte.class) {
                    return SymbolicExecution.sbyteSarray(len, defaultIsSymbolic, se);
                } else if (innermostType == Sbool.class) {
                    return SymbolicExecution.sboolSarray(len, defaultIsSymbolic, se);
                } else {
                    throw new NotYetImplementedException();
                }
            } else if (PartnerClass.class.isAssignableFrom(innermostType)) {
                return SymbolicExecution.partnerClassSarray(
                        (Class<? extends PartnerClass>) innermostType,
                        len,
                        defaultIsSymbolic,
                        se
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
        public Sarray symbolicDefault(SymbolicExecution se) {
            if (elementsAreSarraySarrays()) {
                assert elementType.getComponentType().isArray();
                return se.sarraySarray(se.symSint(), elementType, defaultIsSymbolic());
            } else {
                return generateNonSarraySarray(se.symSint(), elementType.getComponentType(), true, se);
            }
        }

        @Override
        public Sarray nonSymbolicDefaultElement(SymbolicExecution se) {
            return null;
        }

        @Override
        public SarraySarray copy(MulibValueCopier mvt) {
            return new SarraySarray(mvt, this);
        }
    }
}
