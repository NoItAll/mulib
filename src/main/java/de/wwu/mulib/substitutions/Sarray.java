package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.LinkedHashMap;
import java.util.Set;

public abstract class Sarray<T extends SubstitutedVar> implements SubstitutedVar {
    private final long id;
    private final Sint len;
    private final Class<T> clazz;
    private final LinkedHashMap<Sint, T> elements;

    private final boolean defaultIsSymbolic;

    private boolean onlyConcreteIndicesUsed;
    private boolean storeWasUsed;

    private Sarray(Class<T> clazz, Sint len, SymbolicExecution se,
                   boolean defaultIsSymbolic) {
        this.id = se.getNextNumberInitializedSarray();
        this.storeWasUsed = false;
        this.onlyConcreteIndicesUsed = true;
        this.clazz = clazz;
        this.elements = new LinkedHashMap<>();
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.len = len;
    }

    @Override
    public String toString() {
        return "Sarray{id=" + id + ", elements=" + elements + "}";
    }

    public abstract T symbolicDefault(SymbolicExecution se);

    public abstract T nonSymbolicDefaultElement(SymbolicExecution se);

    public final boolean storeWasUsed() {
        return storeWasUsed;
    }

    public final boolean onlyConcreteIndicesUsed() {
        return onlyConcreteIndicesUsed;
    }

    public final boolean defaultIsSymbolic() {
        return defaultIsSymbolic;
    }

    public final void setStoreWasUsed() {
        storeWasUsed = true;
    }

    /**
     * Returns the type of the elements stored in the Sarray. In the case of Sarray.SarraySarray, the type of
     * Sarray is returned.
     * @return The class that represents the type of elements stored in the Sarray
     * @see SarraySarray#getInnerElementType()
     */
    public final Class<T> getClazz() {
        return clazz;
    }

    public abstract T select(Sint i, SymbolicExecution se);

    public abstract void store(Sint i, T val, SymbolicExecution se);

    // If the new constraint is not concrete, we must account for non-deterministic accesses. Therefore,
    // we will add all current stored pairs (i.e. all relevant stores) as constraints to the constraint stack.
    public boolean checkIfNeedsToRepresentOldEntries(Sint i, SymbolicExecution se) {
        if (onlyConcreteIndicesUsed) {
            if (i instanceof Sint.SymSint) {
                onlyConcreteIndicesUsed = false;
                // We do not have to add any constraints if we are on a known path or if there are not yet any elements.
                return !elements.isEmpty(); // We do not need to check for !se.nextIsOnKnownPath() since this can only ever be executed once
            }
        }
        return false;
    }

    public Set<Sint> getCachedIndices() {
        return elements.keySet();
    }

    public Sint getLength() {
        return len;
    }

    public T getForIndex(Sint index) {
        return elements.get(index);
    }

    public T setForIndex(Sint index, T value) {
        elements.remove(index);
        return elements.put(index, value);
    }

    public long getId() {
        return id;
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
                if (ss.getInnerElementType().getComponentType() != ssv.getInnerElementType()) {
                    throw new ArrayStoreException();
                }
                return;
            } else {
                if (!(value instanceof Sarray)) {
                    throw new ArrayStoreException();
                }
                if (ss.getInnerElementType().getComponentType() != ((Sarray) value).getClazz()) {
                    throw new ArrayStoreException();
                }
            }
        }
    }

    public static class SintSarray extends Sarray<Sint> {

        public SintSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sint.class, len, se, defaultIsSymbolic);
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
            return Sint.ZERO;
        }
    }

    public static class SdoubleSarray extends Sarray<Sdouble> {

        public SdoubleSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sdouble.class, len, se, defaultIsSymbolic);
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
            return Sdouble.ZERO;
        }
    }

    public static class SfloatSarray extends Sarray<Sfloat> {

        public SfloatSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sfloat.class, len, se, defaultIsSymbolic);
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
            return Sfloat.ZERO;
        }
    }

    public static class SlongSarray extends Sarray<Slong> {

        public SlongSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Slong.class, len, se, defaultIsSymbolic);
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
            return Slong.ZERO;
        }
    }

    public static class SshortSarray extends Sarray<Sshort> {

        public SshortSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sshort.class, len, se, defaultIsSymbolic);
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
            return Sshort.ZERO;
        }
    }

    public static class SbyteSarray extends Sarray<Sbyte> {

        public SbyteSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sbyte.class, len, se, defaultIsSymbolic);
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
            return Sbyte.ZERO;
        }
    }

    public static class SboolSarray extends Sarray<Sbool> {

        public SboolSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sbool.class, len, se, defaultIsSymbolic);
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
            return Sbool.FALSE;
        }
    }

    public static class PartnerClassSarray<T extends PartnerClass> extends Sarray<T> {

        public PartnerClassSarray(Class<T> clazz, Sint len, SymbolicExecution se,
                                  boolean defaultIsSymbolic) {
            super(clazz, len, se, defaultIsSymbolic);
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
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class SarraySarray extends Sarray<Sarray> {

        private final int dim;
        private final Class<? extends SubstitutedVar> innerElementType;

        public SarraySarray(Sint len, SymbolicExecution se,
                            boolean defaultIsSymbolic,
                            Class<? extends SubstitutedVar> innerElementType) {
            super(Sarray.class, len, se, defaultIsSymbolic);
            this.innerElementType = innerElementType;
            assert innerElementType.isArray();
            this.dim = determineDimFromInnerElementType(innerElementType);
            assert dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
        }

        private static int determineDimFromInnerElementType(Class<?> innerElementType) {
            int i = 1; // The SarraySarray this belongs to also counts
            while (innerElementType.getComponentType() != null) {
                innerElementType = innerElementType.getComponentType();
                i++;
            }
            return i;
        }

        public Class<? extends SubstitutedVar> getInnerElementType() {
            return innerElementType;
        }

        public boolean elementsAreSarraySarrays() {
            return dim > 2;
        }

        @SuppressWarnings("unchecked")
        public SarraySarray(
                Sint len, Sint[] innerLengths,
                SymbolicExecution se, Class<? extends SubstitutedVar> innerElementType) {
            super(Sarray.class, len, se, false);
            assert innerElementType.isArray();
            this.dim = determineDimFromInnerElementType(innerElementType);
            assert dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
            assert dim >= innerLengths.length + 1 : "Dim is always >= the total number of specified lengths";
            this.innerElementType = innerElementType;
            Sint i = Sint.ZERO;
            while (i.ltChoice(len, se)) {
                Sint[] nextInnerLengths = new Sint[innerLengths.length-1];
                System.arraycopy(innerLengths, 1, nextInnerLengths, 0, nextInnerLengths.length);
                se.store(this, i,
                        generateNonSymbolicSarrayDependingOnState(
                                innerLengths[0],
                                nextInnerLengths,
                                (Class<? extends SubstitutedVar>) innerElementType.getComponentType(),
                                se
                        )
                );
                i = i.add(Sint.ONE, se);
            }
        }

        private Sarray generateNonSymbolicSarrayDependingOnState(
                Sint len, Sint[] innerLengths,
                Class<? extends SubstitutedVar> nextInnerElementsType, SymbolicExecution se) {
            if (elementsAreSarraySarrays()) {
                assert nextInnerElementsType.isArray();
                assert innerLengths.length > 2;
                return SymbolicExecution.sarraySarray(
                        len,
                        innerLengths,
                        nextInnerElementsType,
                        se
                );
            }
            return generateNonSarraySarray(len, nextInnerElementsType.getComponentType(), false, se);
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
                return SymbolicExecution.sarraySarray(innerElementType, se.symSint(), defaultIsSymbolic(), se);
            } else {
                return generateNonSarraySarray(se.symSint(), innerElementType.getComponentType(), defaultIsSymbolic(), se);
            }
        }

        @Override
        public Sarray nonSymbolicDefaultElement(SymbolicExecution se) {
            return null;
        }
    }
}
