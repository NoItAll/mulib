package de.wwu.mulib.substitutions;

import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Sarray<T extends SubstitutedVar> implements IdentityHavingSubstitutedVar {
    private static final byte NOT_YET_REPRESENTED_IN_SOLVER = 0;
    private static final byte SHOULD_BE_REPRESENTED_IN_SOLVER = 1;
    private static final byte IS_REPRESENTED_IN_SOLVER = 2;
    private Sint id;
    private final Sint len;
    // The type of element stored in the array, e.g., Sarray, Sint, ...
    private final Class<T> clazz;
    protected SarrayElementCache<T> cachedElements;
    private final boolean defaultIsSymbolic;
    private byte representationState;
    private Sbool isNull;

    /**
     * Interface for ways of representing the cache of an array.
     * The cache of an array consists of the Sint->SubstitutedValue key-value pairs
     * for which we definitely know that they are contained in the array. The cache is used
     * to ensure that we only create array-based constraints if necessary. Particularly, for the
     * case where no symbolic indices are used for the array, no constraint should be added.
     * @param <U> The type of SubstitutedVar stored in the Sarray.
     */
    private interface SarrayElementCache<U extends SubstitutedVar> {
        /**
         * Gets the value associated with the index
         * @param index The index
         * @return The value, i.e. v = a[index]
         */
        U get(Sint index);

        /**
         * Puts the value at the position of the index. This method is used when we select from an array with an
         * index that is not yet contained in the cache within the search.
         * region.
         * @param index The index
         * @param val The value
         */
        void put(Sint index, U val);

        /**
         * Stores a value at the position of the index. This method is used, when we store a value in the array
         * within the search region. This is important to differentiate from selecting, since storing might
         * need to reset the cache of aliased Sarrays.
         * @param index The index
         * @param val The value
         * @see AliasingSarrayElementCache
         */
        void store(Sint index, U val);

        /**
         * Clears the cache. This will be executed once a value is stored while the Sarray needs to be represented
         * in the constraint solver. We cannot be sure anymore that another value has not been overwritten.
         * TODO: Special case clearSymbolic if a concrete index is used to store
         */
        void clear();

        /**
         * @return The set of indices in the cache
         */
        Set<Sint> keySet();

        /**
         * @return The set of values in the cache
         */
        Collection<U> values();

        /**
         * Not all caches might be able to store symbolic indices (currently for experimenting with the performance).
         * @return A SarrayElementCache capable of storing a symbolic index or this. If the current SarrayElementCache
         * already is capable, returns this.
         */
        SarrayElementCache<U> transitionToSymbolicIndexEnabled();

        /**
         * It can be necessary to copy an Sarray element-wise and then initialize a new array with the copied elements.
         * This method creates a new SarrayElement of the same type as 'this' with the new elements.
         * @param elements The elements with which to populate the new SarrayElementCache
         * @return The new SarrayElementCache
         */
        SarrayElementCache<U> newInstance(Map<Sint, U> elements);

        /**
         * @return true if this SarrayElementCache can store symbolic indices, else false.
         */
        boolean canStoreSymbolicIndices();

        /**
         * @return true if this SarrayElementCache specifically tracks the possible sarrays that might be contained.
         */
        boolean tracksPossibleSarrayValues();

        /**
         * @return true if this SarrayElementCache has a Set of potential candidates useable for caching.
         */
        boolean isAliasing();

        /**
         * @return The number of cached key-value-pairs
         */
        int getNumberCachedItems();

        void addToPotentiallyAliasedSarrays(Sarray<U> sarray);
    }

    /**
     * Uses a Map to store the Sint->SubstitutedVar pairs. Thus is also able to store symbolic indices.
     * @param <U> The type of SubstitutedVar used.
     */
    private static class MapSarrayElementCache<U extends SubstitutedVar> implements SarrayElementCache<U> {
        private final Map<Sint, U> elements;

        MapSarrayElementCache() {
            elements = new HashMap<>();
        }

        MapSarrayElementCache(Map<Sint, U> elements) {
            this.elements = elements;
        }

        MapSarrayElementCache(U[] values) {
            elements = new HashMap<>(values.length);
            int i = 0;
            for (U u : values) {
                elements.put(Sint.concSint(i), u);
                i++;
            }
        }

        MapSarrayElementCache(SarrayElementCache<U> sec) {
            if (sec instanceof MapSarrayElementCache) {
                this.elements = new HashMap<>(((MapSarrayElementCache<U>) sec).elements);
            } else {
                this.elements = new HashMap<>();
                Iterable<Sint> indices = sec.keySet();
                for (Sint i : indices) {
                    elements.put(i, sec.get(i));
                }
            }
        }

        @Override
        public U get(Sint index) {
            return elements.get(index);
        }

        @Override
        public void put(Sint index, U val) {
            elements.put(index, val);
        }

        @Override
        public void store(Sint index, U val) {
            if (index instanceof SymNumericExpressionSprimitive) {
                elements.clear();
            }
            elements.put(index, val);
        }

        @Override
        public void clear() {
            elements.clear();
        }

        @Override
        public Set<Sint> keySet() {
            return Collections.unmodifiableSet(elements.keySet());
        }

        @Override
        public Collection<U> values() {
            return Collections.unmodifiableCollection(elements.values());
        }

        @Override
        public SarrayElementCache<U> transitionToSymbolicIndexEnabled() {
            return this;
        }

        @Override
        public SarrayElementCache<U> newInstance(Map<Sint, U> elements) {
            return new MapSarrayElementCache<>(elements);
        }

        @Override
        public boolean canStoreSymbolicIndices() {
            return true;
        }

        @Override
        public boolean tracksPossibleSarrayValues() {
            return false;
        }

        @Override
        public boolean isAliasing() {
            return false;
        }

        @Override
        public int getNumberCachedItems() {
            return elements.size();
        }

        @Override
        public void addToPotentiallyAliasedSarrays(Sarray<U> sarray) {
            throw new MulibRuntimeException("Should have been transformed beforehand");
        }
    }

    /**
     * Uses an array to store the Sint->SubstitutedVar pairs. Thus is not able to store symbolic indices.
     * @param <U> The type of SubstitutedVar used.
     */
    private static class ArraySarrayElementCache<U extends SubstitutedVar> implements SarrayElementCache<U> {
        private final U[] values;
        ArraySarrayElementCache(U[] values) {
//            assert Arrays.stream(values).allMatch(Objects::nonNull); // TODO
            this.values = (U[]) new SubstitutedVar[values.length];
            System.arraycopy(values, 0, this.values, 0, values.length);
        }

        ArraySarrayElementCache(Map<Sint, U> elements) {
            this.values = (U[]) new SubstitutedVar[elements.size()];
            for (Map.Entry<Sint, U> entry : elements.entrySet()) {
                assert entry.getKey() instanceof ConcSnumber;
                values[((ConcSnumber) entry.getKey()).intVal()] = entry.getValue();
            }
            assert Arrays.stream(values).allMatch(Objects::nonNull);
        }

        @Override
        public U get(Sint index) {
            if (index instanceof SymNumericExpressionSprimitive) {
                return null;
            }
            return values[((ConcSnumber) index).intVal()];
        }

        @Override
        public void put(Sint index, U val) {
            assert index instanceof ConcSnumber;
            values[((ConcSnumber) index).intVal()] = val;
        }

        @Override
        public void store(Sint index, U val) {
            assert !(index instanceof SymNumericExpressionSprimitive);
            put(index, val);
        }

        @Override
        public void clear() {
            throw new MulibIllegalStateException("This should not happen. When it becomes necessary to clear the " +
                    "cache of an array, the array should already be represented in the constraint solver. Thus, a " +
                    "cache which supports symbolic indices should be used.");
        }

        @Override
        public Set<Sint> keySet() {
            return IntStream.range(0, values.length).mapToObj(Sint::concSint).collect(Collectors.toSet());
        }

        @Override
        public Collection<U> values() {
            return List.of(values);
        }

        @Override
        public SarrayElementCache<U> transitionToSymbolicIndexEnabled() {
            return new MapSarrayElementCache<>(values);
        }

        @Override
        public SarrayElementCache<U> newInstance(Map<Sint, U> elements) {
            return new ArraySarrayElementCache<>(elements);
        }

        @Override
        public boolean canStoreSymbolicIndices() {
            return false;
        }

        @Override
        public boolean tracksPossibleSarrayValues() {
            return false;
        }

        @Override
        public boolean isAliasing() {
            return false;
        }

        @Override
        public int getNumberCachedItems() {
            return values.length;
        }

        @Override
        public void addToPotentiallyAliasedSarrays(Sarray<U> sarray) {
            throw new MulibRuntimeException("Should have been transformed beforehand");
        }
    }

    private static class AliasingSarrayElementCache<U extends SubstitutedVar> implements SarrayElementCache<U> {
        private final SarrayElementCache<U> delegateTo;
        private final Set<Sarray<U>> potentiallyAliasedSarrays;

        AliasingSarrayElementCache(Sarray cacheIsFor, SarrayElementCache<U> delegateTo, Set<Sarray<U>> sarrays) {
            this.delegateTo = delegateTo;
            this.potentiallyAliasedSarrays = sarrays;
            if (cacheIsFor != null) {
                for (Sarray s : sarrays) {
                    assert s.cachedElements.isAliasing();
                    s.cachedElements.addToPotentiallyAliasedSarrays(cacheIsFor);
                }
            }
        }

        @Override
        public U get(Sint index) {
            return delegateTo.get(index);
        }

        @Override
        public void put(Sint index, U val) {
            delegateTo.put(index, val);
        }

        @Override
        public void store(Sint index, U val) {
            for (Sarray<U> s : potentiallyAliasedSarrays) {
                // Delete the cache so that we do not have to worry about having stale caches
                s.clearCachedElements();
            }
            delegateTo.store(index, val);
        }

        @Override
        public void clear() {
            delegateTo.clear();
        }

        @Override
        public Set<Sint> keySet() {
            return delegateTo.keySet();
        }

        @Override
        public Collection<U> values() {
            return delegateTo.values();
        }

        @Override
        public SarrayElementCache<U> transitionToSymbolicIndexEnabled() {
            if (canStoreSymbolicIndices()) {
                return this;
            } else {
                return new AliasingSarrayElementCache<>(
                        null,
                        delegateTo.transitionToSymbolicIndexEnabled(),
                        potentiallyAliasedSarrays
                );
            }
        }

        @Override
        public SarrayElementCache<U> newInstance(Map<Sint, U> elements) {
            return new AliasingSarrayElementCache<>(
                    null,
                    this.delegateTo.newInstance(elements),
                    new HashSet<>(potentiallyAliasedSarrays)
            );
        }

        @Override
        public boolean canStoreSymbolicIndices() {
            return delegateTo.canStoreSymbolicIndices();
        }

        @Override
        public boolean tracksPossibleSarrayValues() {
            return delegateTo.tracksPossibleSarrayValues();
        }

        @Override
        public boolean isAliasing() {
            return true;
        }

        @Override
        public int getNumberCachedItems() {
            return delegateTo.getNumberCachedItems();
        }

        @Override
        public void addToPotentiallyAliasedSarrays(Sarray<U> sarray) {
            potentiallyAliasedSarrays.add(sarray);
        }
    }

    private static class SarraySarrayTrackingElementCache implements SarrayElementCache<Sarray> {
        private final SarrayElementCache<Sarray> delegateTo;
        private final Set<Sarray> potentiallyContainedElements;
        private final boolean isCompletelyInitialized;

        SarraySarrayTrackingElementCache(SarrayElementCache<Sarray> delegateTo, boolean isCompletelyInitialized) {
            this.delegateTo = delegateTo;
            Collection<Sarray> values = delegateTo.values();
            for (Sarray s : values) {
                if (s == null) {
                    continue;
                }
                s.cachedElements = new AliasingSarrayElementCache<>(s, s.cachedElements, new HashSet<>());
            }
            this.potentiallyContainedElements = new HashSet<>(values);
            this.isCompletelyInitialized = isCompletelyInitialized;
        }

        SarraySarrayTrackingElementCache(
                SarrayElementCache<Sarray> delegateTo,
                Set<Sarray> potentiallyContainedElements,
                boolean isCompletelyInitialized) {
            this.delegateTo = delegateTo;
            this.potentiallyContainedElements = potentiallyContainedElements;
            this.isCompletelyInitialized = isCompletelyInitialized;
        }

        @Override
        public Sarray<?> get(Sint index) {
            return delegateTo.get(index);
        }

        @Override
        public void put(Sint index, Sarray val) {
            delegateTo.put(index, val);
            if (!isCompletelyInitialized) {
                potentiallyContainedElements.add(val);
            }
        }

        @Override
        public void store(Sint index, Sarray val) {
            delegateTo.store(index, val);
            potentiallyContainedElements.add(val);
        }

        @Override
        public void clear() {
            delegateTo.clear();
        }

        @Override
        public Set<Sint> keySet() {
            return delegateTo.keySet();
        }

        @Override
        public Collection<Sarray> values() {
            return delegateTo.values();
        }

        @Override
        public SarrayElementCache<Sarray> transitionToSymbolicIndexEnabled() {
            if (!delegateTo.canStoreSymbolicIndices()) {
                return new SarraySarrayTrackingElementCache(
                        new MapSarrayElementCache<>(
                                ((ArraySarrayElementCache<Sarray>) delegateTo).values),
                        isCompletelyInitialized
                );
            } else {
                return this;
            }
        }

        @Override
        public SarrayElementCache<Sarray> newInstance(Map<Sint, Sarray> elements) {
            return new SarraySarrayTrackingElementCache(
                    delegateTo.newInstance(elements),
                    potentiallyContainedElements,
                    isCompletelyInitialized
            );
        }

        @Override
        public boolean canStoreSymbolicIndices() {
            return delegateTo.canStoreSymbolicIndices();
        }

        @Override
        public boolean tracksPossibleSarrayValues() {
            return true;
        }

        @Override
        public boolean isAliasing() {
            return delegateTo.isAliasing();
        }

        @Override
        public int getNumberCachedItems() {
            return delegateTo.getNumberCachedItems();
        }

        @Override
        public void addToPotentiallyAliasedSarrays(Sarray<Sarray> sarray) {
            delegateTo.addToPotentiallyAliasedSarrays(sarray);
        }

        public Set<Sarray> getPotentiallyContainedElements() {
            return potentiallyContainedElements;
        }
    }

    /** New instance constructor */
    protected Sarray(Class<T> clazz, Sint len, SymbolicExecution se,
                     boolean defaultIsSymbolic,
                     Sbool isNull) {
        assert clazz != null && len != null;
        this.id = null;
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.clazz = clazz;
        this.defaultIsSymbolic = defaultIsSymbolic;
        this.len = len;
        if (!defaultIsSymbolic) {
            if (len instanceof ConcSnumber) {
                int length = ((ConcSnumber) len).intVal();
                T[] values = (T[]) new SubstitutedVar[length];
                for (int i = 0; i < length; i++) {
                    values[i] = nonSymbolicDefaultElement(se);
                }
                this.cachedElements = new ArraySarrayElementCache<T>(values);
            } else {
                throw new NotYetImplementedException("Behavior if length is not concrete and default is not symbolic " +
                        "is not yet implemented");
            }
        } else {
            this.cachedElements = new MapSarrayElementCache<>();
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
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.cachedElements = new ArraySarrayElementCache<T>(arrayElements);
        this.isNull = Sbool.ConcSbool.FALSE;
    }

    /** Copy constructor for all Sarrays but SarraySarray */
    protected Sarray(MulibValueCopier mvt, Sarray<T> s) {
        this(mvt, s, new MapSarrayElementCache<>(s.cachedElements));
    }

    /** Copy constructor for SarraySarrays */
    private Sarray(MulibValueCopier mvt, Sarray<T> s, SarrayElementCache<T> cachedElements) {
        mvt.registerCopy(s, this);
        this.id = s.getId();
        this.representationState = s.representationState;
        this.clazz = s.clazz;
        this.cachedElements = cachedElements;
        this.defaultIsSymbolic = s.defaultIsSymbolic;
        this.len = s.len;
        this.isNull = s.isNull;
    }

    final void initializeId(SymbolicExecution se) {
        _initializeId(se.concSint(se.getNextNumberInitializedSymSarray()));
    }

    private void initializeIdForAliasing(SymbolicExecution se) {
        _initializeId(se.symSint());
    }

    void _initializeId(Sint id) {
        if (this.id != null) {
            throw new MulibRuntimeException("Must not set already set id");
        }
        this.representationState = SHOULD_BE_REPRESENTED_IN_SOLVER;
        this.id = id;
    }

    @Override
    public String toString() {
        return "Sarray{id=" + id + ", elements=" + cachedElements + "}";
    }

    public abstract T symbolicDefault(SymbolicExecution se);

    public abstract T nonSymbolicDefaultElement(SymbolicExecution se);

    public final boolean shouldBeRepresentedInSolver() {
        return representationState == SHOULD_BE_REPRESENTED_IN_SOLVER || representationState == IS_REPRESENTED_IN_SOLVER;
    }

    public final boolean isRepresentedInSolver() {
        return representationState == IS_REPRESENTED_IN_SOLVER;
    }

    public final void setAsRepresentedInSolver() {
        representationState = IS_REPRESENTED_IN_SOLVER;
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

    // If the new constraint is not concrete, we must account for non-deterministic accesses. Therefore,
    // we will add all current stored pairs (i.e. all relevant stores) as constraints to the constraint stack.
    public final boolean checkIfNeedsToRepresentOldEntries(Sint i, SymbolicExecution se) {
        if (!shouldBeRepresentedInSolver()) {
            if (i instanceof SymNumericExpressionSprimitive) {
                prepareToRepresentSymbolically(se);
                // We do not have to add any constraints if we are on a known path or if there are not yet any elements.
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepareToRepresentSymbolically(SymbolicExecution se) {
        initializeId(se);
        this.cachedElements = cachedElements.transitionToSymbolicIndexEnabled();
    }

    public final Set<Sint> getCachedIndices() {
        return cachedElements.keySet();
    }

    public final Iterable<T> getCachedElements() {
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

    public final void setInCacheForIndexForSelect(Sint index, T value) {
        cachedElements.put(index, value);
    }

    public final void setInCacheForIndexForStore(Sint index, T value) {
        cachedElements.store(index, value);
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

        private static SarrayElementCache<Sarray> copyArrayElements(MulibValueCopier mvt, SarrayElementCache<Sarray> elementCache) {
            Map<Sint, Sarray> elements = new HashMap<>();
            for (Sint i : elementCache.keySet()) {
                elements.put(i, elementCache.get(i).copy(mvt));
            }
            return elementCache.newInstance(elements);
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
            Sarray result;
            if (elementsAreSarraySarrays()) {
                assert elementType.getComponentType().isArray();
                result = se.sarraySarray(se.symSint(), elementType, defaultIsSymbolic());
            } else {
                result = generateNonSarraySarray(se.symSint(), elementType.getComponentType(), true, se);
            }
            result.initializeIdForAliasing(se);
            assert cachedElements.tracksPossibleSarrayValues();
            result.cachedElements = new AliasingSarrayElementCache(
                    result,
                    result.cachedElements,
                    ((SarraySarrayTrackingElementCache) cachedElements).getPotentiallyContainedElements()
            );
            return result;
        }

        @Override
        public Sarray nonSymbolicDefaultElement(SymbolicExecution se) {
            return null;
        }

        @Override
        public SarraySarray copy(MulibValueCopier mvt) {
            return new SarraySarray(mvt, this);
        }

        @Override
        public void prepareToRepresentSymbolically(SymbolicExecution se) {
            initializeId(se);
            Sint length = length();
            assert cachedElements.keySet().stream().allMatch(s -> s instanceof ConcSnumber);
            this.cachedElements =
                    new SarraySarrayTrackingElementCache(
                            cachedElements.transitionToSymbolicIndexEnabled(),
                            length instanceof ConcSnumber && ((ConcSnumber) length).intVal() == cachedElements.getNumberCachedItems()
                    );
        }
    }
}
