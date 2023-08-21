package de.wwu.mulib.substitutions;

import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.throwables.MulibIllegalStateException;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.search.executors.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents arrays
 * @param <T> The component type
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Sarray<T extends SubstitutedVar> extends PartnerClassObject {
    private final Sint len;
    // The type of element stored in the array, e.g., Sarray, Sint, ...
    private final Class<T> clazz;
    protected final Map<Sint, T> cachedElements;

    /**
     * New instance constructor
     * @param componentType The component type
     * @param len The length of the sarray
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param defaultIsSymbolic Whether unknown elements are symbolic by default
     * @param isNull Whether the sarray can be null
     */
    protected Sarray(Class<T> componentType, Sint len, SymbolicExecution se,
                     boolean defaultIsSymbolic,
                     Sbool isNull) {
        this(componentType, len, se, defaultIsSymbolic, isNull, true);
    }

    /**
     * New instance constructor
     * @param componentType The component type
     * @param len The length of the sarray
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param defaultIsSymbolic Whether unknown elements are symbolic by default
     * @param isNull Whether the sarray can be null
     * @param initializeImmediately Whether the array should be initialized immediately at this position.
     *                              This is set to false by {@link PartnerClassSarray} per default to employ a custom mechanism.
     */
    protected Sarray(Class<T> componentType,
                     Sint len,
                     SymbolicExecution se,
                     boolean defaultIsSymbolic,
                     Sbool isNull,
                     boolean initializeImmediately) {
        assert componentType != null && len != null;
        this.id = null;
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.clazz = componentType;
        if (defaultIsSymbolic) {
            __mulib__setDefaultIsSymbolic();
        }
        this.len = len;
        this.isNull = isNull;
        this.cachedElements = new HashMap<>();
        if (initializeImmediately && len instanceof ConcSnumber) {
            _initializeCachedElements(se);
        }
    }

    /**
     * Initializes the elements of this array if the length is concrete.
     * Calls {@link Sarray#getNewValueForSelect(SymbolicExecution, Sint)} to determine the elements for this.
     * @param se The current instance of {@link SymbolicExecution} for this run
     */
    protected void _initializeCachedElements(SymbolicExecution se) {
        if (len instanceof ConcSnumber) {
            int length = ((ConcSnumber) len).intVal();
            for (int i = 0; i < length; i++) {
                Sint index = Sint.concSint(i);
                cachedElements.put(index, getNewValueForSelect(se, index));
            }
        }
    }

    /**
     * Transformation constructor
     * @param arrayElements The elements of this sarray
     */
    protected Sarray(
            T[] arrayElements) {
        this.id = null;
        this.clazz = (Class<T>) arrayElements.getClass().getComponentType();
        int length = arrayElements.length;
        this.len = Sint.concSint(length);
        this.representationState = NOT_YET_REPRESENTED_IN_SOLVER;
        this.cachedElements = new HashMap<>();
        for (int i = 0; i < arrayElements.length; i++) {
            cachedElements.put(Sint.concSint(i), arrayElements[i]);
        }
    }

    /**
     * Copy constructor for all Sarrays but SarraySarray
     * @param mvt The value copier
     * @param s The sarray to copy
     */
    protected Sarray(MulibValueCopier mvt, Sarray<T> s) {
        this(mvt, s, new HashMap<>(s.cachedElements));
    }

    /**
     * Copy constructor for PartnerClassSarray as there a specialized type of copying is implemented
     * @param mvc The value copier
     * @param s To-copy
     * @param cachedElements The already-copied elements of to-copy
     */
    protected Sarray(MulibValueCopier mvc, Sarray<T> s, Map<Sint, T> cachedElements) {
        super(s, mvc);
        this.clazz = s.clazz;
        this.cachedElements = cachedElements;
        this.len = s.len;
    }

    @Override
    public String toString() {
        return "Sarray[" + id + "]{repState=" + representationState + ",elements=" + cachedElements + "}";
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param index The index to generate the symbolic default value for
     * @return The symbolic default element according to this sarrays component type
     */
    protected abstract T symbolicDefault(SymbolicExecution se, Sint index);

    /**
     * @return The concrete default element, e.g. null in the case of {@link PartnerClassSarray}
     */
    protected abstract T nonSymbolicDefaultElement();

    /**
     * Clears the elements of this sarray.
     * This is done if we block the cache since the given array becomes a potential target for symbolic aliasing.
     * Subsequent calls are then delegated to the constraint solver via {@link de.wwu.mulib.constraints.ArrayAccessConstraint}s.
     */
    public void clearCache() {
        cachedElements.clear();
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
     * Is equivalent tp Sarray.getClazz() except for SarraySarray. For SarraySarrays a Java representation of an int
     * is returned, e.g., Sint[][].class
     * TODO Having generated specialized PartnerClassSarrays and SarraySarrays this can be refactored
     * @return The class that stores the actual type stored in the Sarray
     */
    public Class<?> getElementType() {
        return getClazz();
    }

    /**
     * Performs a select operation delegating to symbolic execution
     * @param i The index to select from
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The selected value
     */
    public abstract T select(Sint i, SymbolicExecution se);

    /**
     * Performs a store operation delegating to symbolic execution
     * @param i The index to store in
     * @param val The value to store
     * @param se The current instance of {@link SymbolicExecution} for this run
     */
    public abstract void store(Sint i, T val, SymbolicExecution se);

    /**
     * @return The indices of this sarray
     */
    public final Set<Sint> getCachedIndices() {
        return cachedElements.keySet();
    }

    /**
     * @return The values of this sarray
     */
    public final Collection<T> getCachedElements() {
        return cachedElements.values();
    }

    /**
     * @return The length of ths sarray without performing {@link PartnerClass#__mulib__nullCheck()}
     */
    public final Sint _getLengthWithoutCheckingForIsNull() {
        return len;
    }

    /**
     * @return The length of this sarray after performing {@link PartnerClass#__mulib__nullCheck()}
     */
    public final Sint length() {
        __mulib__nullCheck();
        return len;
    }

    /**
     * @param index The index
     * @return The value from the elements of the array
     */
    public final T getFromCacheForIndex(Sint index) {
        return cachedElements.get(index);
    }

    /**
     * Sets a value for the index if {@link PartnerClass#__mulib__cacheIsBlocked()} is false, else does nothing
     * @param index The index
     * @param value The value
     */
    public void setInCacheForIndex(Sint index, T value) {
        if (!__mulib__cacheIsBlocked()) {
            cachedElements.put(index, value);
        }
    }

    /**
     * Throws an exception since this case must be treated by an external labeler
     * @param o An empty object the values of which shall be set
     * @param solverManager The solver manager
     */
    @Override
    public Object label(Object o, SolverManager solverManager) {
        throw new MulibIllegalStateException("Should not occur");
    }

    /**
     * Helper method for determining whether a value can be stored in a sarray.
     * If this does not work, an ArrayStoreException is thrown.
     * @param sarray The sarray
     * @param value The value
     */
    @SuppressWarnings("rawtypes")
    public static void checkIfValueIsStorableForSarray(Sarray sarray, SubstitutedVar value) {
        if (!(sarray instanceof Sarray.SarraySarray)) {
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

    /**
     * Based on {@link PartnerClass#__mulib__defaultIsSymbolic()}, determines the type of value to be returned
     * for an unknown index. Is also used to fill up the array if it is eagerly filled.
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param index The index
     * @return The value at the index
     */
    public T getNewValueForSelect(SymbolicExecution se, Sint index) {
        T result;
        if (!__mulib__defaultIsSymbolic() && !__mulib__shouldBeRepresentedInSolver()) {
            result = nonSymbolicDefaultElement();
        } else {
            // If symbolic is required, optional aliasing etc. is handled here. If default values are enforced,
            // this is also handled here
            result = symbolicDefault(se, index);
        }
        return result;
    }

    @Override
    public void __mulib__initializeLazyFields(SymbolicExecution se) {
        throw new MulibIllegalStateException("Should not be called for Sarrays");
    }

    @Override
    public Map<String, SubstitutedVar> __mulib__getFieldNameToSubstitutedVar() {
        throw new MulibIllegalStateException("Should not be called for Sarrays");
    }

    @Override
    protected final void __mulib__blockCacheInPartnerClassFields() {
    }

    /**
     * represents int[]
     */
    public static class SintSarray extends Sarray<Sint> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SintSarray(Sint[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SintSarray(Sint[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SintSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sint.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sint symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSint();
        }

        @Override
        protected Sint nonSymbolicDefaultElement() {
            return Sint.ConcSint.ZERO;
        }

        @Override
        public SintSarray copy(MulibValueCopier mvt) {
            return new SintSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return int[].class;
        }
    }

    /**
     * Represents double[]
     */
    public static class SdoubleSarray extends Sarray<Sdouble> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SdoubleSarray(Sdouble[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SdoubleSarray(Sdouble[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SdoubleSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sdouble.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sdouble symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSdouble();
        }

        @Override
        protected Sdouble nonSymbolicDefaultElement() {
            return Sdouble.ConcSdouble.ZERO;
        }

        @Override
        public SdoubleSarray copy(MulibValueCopier mvt) {
            return new SdoubleSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return double[].class;
        }
    }

    /**
     * represents float[]
     */
    public static class SfloatSarray extends Sarray<Sfloat> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SfloatSarray(Sfloat[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SfloatSarray(Sfloat[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SfloatSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sfloat.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sfloat symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSfloat();
        }

        @Override
        protected Sfloat nonSymbolicDefaultElement() {
            return Sfloat.ConcSfloat.ZERO;
        }

        @Override
        public SfloatSarray copy(MulibValueCopier mvt) {
            return new SfloatSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return float[].class;
        }
    }

    /**
     * Represents long[]
     */
    public static class SlongSarray extends Sarray<Slong> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SlongSarray(Slong[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SlongSarray(Slong[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SlongSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Slong.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Slong symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSlong();
        }

        @Override
        protected Slong nonSymbolicDefaultElement() {
            return Slong.ConcSlong.ZERO;
        }

        @Override
        public SlongSarray copy(MulibValueCopier mvt) {
            return new SlongSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return long[].class;
        }
    }

    /**
     * represents short[]
     */
    public static class SshortSarray extends Sarray<Sshort> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SshortSarray(Sshort[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SshortSarray(Sshort[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SshortSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sshort.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sshort symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSshort();
        }

        @Override
        protected Sshort nonSymbolicDefaultElement() {
            return Sshort.ConcSshort.ZERO;
        }

        @Override
        public SshortSarray copy(MulibValueCopier mvt) {
            return new SshortSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return short[].class;
        }
    }

    /**
     * represents char[]
     */
    public static class ScharSarray extends Sarray<Schar> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public ScharSarray(Schar[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public ScharSarray(Schar[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public ScharSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Schar.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
        public ScharSarray(MulibValueCopier mvt, ScharSarray s) {
            super(mvt, s);
        }

        @Override
        public final Schar select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final void store(Sint i, Schar val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected Schar symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSchar();
        }

        @Override
        protected Schar nonSymbolicDefaultElement() {
            return Schar.ConcSchar.ZERO;
        }

        @Override
        public ScharSarray copy(MulibValueCopier mvt) {
            return new ScharSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return char[].class;
        }
    }

    /**
     * represents byte[]
     */
    public static class SbyteSarray extends Sarray<Sbyte> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SbyteSarray(Sbyte[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SbyteSarray(Sbyte[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SbyteSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sbyte.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sbyte symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSbyte();
        }

        @Override
        protected Sbyte nonSymbolicDefaultElement() {
            return Sbyte.ConcSbyte.ZERO;
        }

        @Override
        public SbyteSarray copy(MulibValueCopier mvt) {
            return new SbyteSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return byte[].class;
        }
    }

    /**
     * represents boolean[]
     */
    public static class SboolSarray extends Sarray<Sbool> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public SboolSarray(Sbool[] values) {
            super(values);
        }
        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public SboolSarray(Sbool[] values, MulibValueTransformer mvt) {
            this(values);
        }

        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public SboolSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic, Sbool isNull) {
            super(Sbool.class, len, se, defaultIsSymbolic, isNull);
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
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
        protected Sbool symbolicDefault(SymbolicExecution se, Sint index) {
            return se.symSbool();
        }

        @Override
        protected Sbool nonSymbolicDefaultElement() {
            return Sbool.ConcSbool.FALSE;
        }

        @Override
        public SboolSarray copy(MulibValueCopier mvt) {
            return new SboolSarray(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            return boolean[].class;
        }
    }

    /**
     * Superclass of all arrays of reference-typed elements
     * @param <T> The type of the reference-typed elements
     */
    public static class PartnerClassSarray<T extends PartnerClass> extends Sarray<T> {

        /**
         * Transformation constructor
         * @param values The values
         */
        public PartnerClassSarray(T[] values) {
            super(values);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param mvt Not used
         */
        public PartnerClassSarray(T[] values, MulibValueTransformer mvt) {
            this(values);
        }


        /**
         * New instance constructor
         * @param clazz The component type
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         */
        public PartnerClassSarray(Class<T> clazz, Sint len, SymbolicExecution se,
                                  boolean defaultIsSymbolic, Sbool isNull) {
            super(clazz, len, se, defaultIsSymbolic, isNull);
            assert !Sarray.class.isAssignableFrom(clazz);
        }

        /**
         * New instance constructor for SarraySarray
         * @param clazz The component type
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param isNull Whether the sarray can be null
         * @param initializeImmediately Is always set to be false and only used to differentiate this constructor from
         *                              the other new instance constructor
         *
         *                              TODO Refactor
         */
        protected PartnerClassSarray(
                Class<T> clazz,
                Sint len,
                SymbolicExecution se,
                boolean defaultIsSymbolic,
                Sbool isNull,
                boolean initializeImmediately) {
            super(clazz, len, se, defaultIsSymbolic, isNull, initializeImmediately);
            assert Sarray.class.isAssignableFrom(clazz);
        }

        /**
         * Copy constructor
         * @param mvc The copier
         * @param s To-copy
         */
        public PartnerClassSarray(MulibValueCopier mvc, PartnerClassSarray s) {
            super(mvc, s, copyArrayElementsOfNonSarraySarrayPartnerClassSarray(mvc, s.cachedElements));
        }

        private static Map<Sint, PartnerClass> copyArrayElementsOfNonSarraySarrayPartnerClassSarray(MulibValueCopier mvt, Map<Sint, PartnerClass> elementCache) {
            Map<Sint, PartnerClass> elements = new HashMap<>();
            for (Sint i : elementCache.keySet()) {
                PartnerClass pc = elementCache.get(i);
                elements.put(i, pc == null ? null : (PartnerClass) pc.copy(mvt));
            }
            return elements;
        }

        @Override
        public T select(Sint i, SymbolicExecution se) {
            T result = (T) se.select(this, i);
            assert result == null || getClazz().isAssignableFrom(result.getClass());
            return result;
        }

        @Override
        public void store(Sint i, T val, SymbolicExecution se) {
            se.store(this, i, val);
        }

        @Override
        protected T nonSymbolicDefaultElement() {
            return null;
        }

        @Override
        public PartnerClassSarray<T> copy(MulibValueCopier mvt) {
            return new PartnerClassSarray<>(mvt, this);
        }

        @Override
        public Class<?> __mulib__getOriginalClass() {
            throw new NotYetImplementedException();
        }

        @Override
        protected T symbolicDefault(SymbolicExecution se, Sint index) {
            assert __mulib__defaultIsSymbolic() || __mulib__isRepresentedInSolver();
            boolean canBeNull;
            if (!se.nextIsOnKnownPath()) {
                ArrayInformation info =
                        se.getCalculationFactory().getAvailableInformationOnArray(se, this);
                canBeNull = info.arrayCanPotentiallyContainNull || info.canPotentiallyContainCurrentlyUnrepresentedNonSymbolicDefault;
            } else {
                canBeNull = false;
            }
            T result = generateSymbolicDefault(se, canBeNull);

            if (!result.__mulib__isRepresentedInSolver()) {
                // This can happen if we allow for aliasing
                result.__mulib__prepareForAliasingAndBlockCache(se);
            }

            if (se.nextIsOnKnownPath()) {
                // If this is not a new choice option, we determine nullability by looking at the representation of the object
                // for the solver that was already initialized via a trail
                // We need to do this since the object might be altered since the first time, it was seen. For instance,
                // a null might be stored in the array directly after accessing this current field
                Sbool nullVal;
                assert !Sarray.class.isAssignableFrom(this.getElementType());
                if (result instanceof Sarray) {
                    nullVal = se.getCalculationFactory().getAvailableInformationOnArray(se, (Sarray) result).isNull;
                } else {
                    nullVal = se.getCalculationFactory().getAvailableInformationOnPartnerClassObject(se, result, /* No info on fields is required */null).isNull;
                }
                canBeNull = nullVal instanceof Sbool.SymSbool || ((Sbool.ConcSbool) nullVal).isTrue();
                if (canBeNull) {
                    Sbool isNull = se.symSbool();
                    // Now we actually setIsNull to its value
                    result.__mulib__setIsNull(isNull);
                    // Check whether the trail has been correctly set
                    assert nullVal == ConcolicConstraintContainer.tryGetSymFromConcolic(isNull) : "We assume that no symSbool is taken in between the initializations";
                }
            }

            se.getCalculationFactory().representPartnerClassObjectIfNeeded(se, result, __mulib__getId(), null, index);
            return result;
        }

        /**
         * Generates a new symbolic instance of the component type of this partner class sarray without any checks
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param canBeNull Whether the sarray can be null
         * @return A new instance
         */
        protected T generateSymbolicDefault(SymbolicExecution se, boolean canBeNull) {
            T result = se.getValueFactory().symObject(se, this.getClazz(), canBeNull);
            return result;
        }

        @Override
        public void __mulib__blockCache() {
            if (!__mulib__cacheIsBlocked()) {
                super.__mulib__blockCache();
                for (Map.Entry<Sint, T> entry : cachedElements.entrySet()) {
                    T val = entry.getValue();
                    if (val == null) {
                        continue;
                    }
                    val.__mulib__blockCache();
                }
            }
        }

        @Override
        public void __mulib__prepareToRepresentSymbolically(SymbolicExecution se) {
            super.__mulib__prepareToRepresentSymbolically(se);
            assert cachedElements.keySet().stream().allMatch(s -> s instanceof ConcSnumber);
            for (Map.Entry<Sint, T> entry : cachedElements.entrySet()) {
                T val = entry.getValue();
                if (val == null) {
                    continue;
                }
                // Cache of values must be blocked since they will be represented
                // Cache is cleared after representing each element in CalculationFactory. This is done in setAsRepresentedInSolver
                val.__mulib__blockCache();
            }
        }
    }

    /**
     * Supertype of all sarrays where the component type also is a sarray-type
     */
    @SuppressWarnings("rawtypes")
    public static class SarraySarray extends PartnerClassSarray<Sarray> {
        private final int dim;
        // The type of element stored in the array, but represented as a real array, e.g.: Sint[], Sdouble[][], etc.
        // Sarray.clazz would represent them all as Sarray.
        private final Class<?> elementType;

        /**
         * Transformation constructor
         * @param values The values
         * @param elementType The component type
         */
        public SarraySarray(Sarray[] values, Class<?> elementType) {
            super(values);
            this.elementType = elementType;
            this.dim = determineDimFromInnerElementType(elementType);
        }

        /**
         * Transformation constructor to keep a consistent constructor signature with other
         * partner class objects
         * @param values The values
         * @param elementType The component type in the form of, e.g., Sint[][]
         * @param mvt Not used
         */
        public SarraySarray(Sarray[] values, Class<?> elementType, MulibValueTransformer mvt) {
            this(values, elementType);
        }


        /**
         * New instance constructor
         * @param len The length
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param defaultIsSymbolic Whether the default of this array for uninitialized values is symbolic
         * @param elementType The component type in the form of, e.g., Sint[][]
         * @param isNull Whether the sarray can be null
         */
        public SarraySarray(Sint len, SymbolicExecution se,
                            boolean defaultIsSymbolic,
                            Class<?> elementType,
                            Sbool isNull) {
            super(Sarray.class, len, se, defaultIsSymbolic, isNull, false);
            this.elementType = elementType;
            this.dim = determineDimFromInnerElementType(elementType);
            assert elementType.isArray() && dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
            _initializeCachedElements(se);
            assert !(len instanceof ConcSnumber) || ((ConcSnumber) len).intVal() == getCachedIndices().size();
        }

        /**
         * Other new instance constructor for MULTIANEWARRAY bytecode. This is not implemented too efficiently
         * if any of the lengths is symbolic.
         * @param lengths An array where each element represents the dimensionality in the nested sarray
         * @param se The current instance of {@link SymbolicExecution} for this run
         * @param elementType The component type
         */
        @SuppressWarnings("unchecked")
        public SarraySarray(
                Sint[] lengths,
                SymbolicExecution se, Class<?> elementType) {
            super(Sarray.class, lengths[0], se, false, Sbool.ConcSbool.FALSE, false);
            assert elementType.isArray();
            this.dim = determineDimFromInnerElementType(elementType);
            assert dim >= 2 : "Dim of SarraySarray must be >= 2. For dim == 1 the other built-in arrays should be used";
            assert dim >= lengths.length : "Dim is always >= the total number of specified lengths";
            this.elementType = elementType;
            Sint i = Sint.ConcSint.ZERO;
            while (i.ltChoice(length(), se)) {
                Sint[] nextLengths = new Sint[lengths.length-1];
                System.arraycopy(lengths, 1, nextLengths, 0, nextLengths.length);
                se.store(this,
                        i,
                        generateNonSymbolicSarrayDependingOnStateForMultiANewArray(
                                nextLengths,
                                (Class<? extends SubstitutedVar>) elementType.getComponentType(),
                                se
                        )
                );
                i = i.add(Sint.ConcSint.ONE, se);
            }
            assert !(_getLengthWithoutCheckingForIsNull() instanceof ConcSnumber) || ((ConcSnumber) _getLengthWithoutCheckingForIsNull()).intVal() == getCachedIndices().size();
        }

        /**
         * Copy constructor
         * @param mvt The copier
         * @param s To-copy
         */
        public SarraySarray(MulibValueCopier mvt, SarraySarray s) {
            super(mvt, s);
            this.dim = s.dim;
            this.elementType = s.elementType;
        }

        @Override
        protected void _initializeCachedElements(SymbolicExecution se) {
            if (_getLengthWithoutCheckingForIsNull() instanceof ConcSnumber) {
                int length = ((ConcSnumber) _getLengthWithoutCheckingForIsNull()).intVal();
                for (int i = 0; i < length; i++) {
                    Sarray result;
                    if (!__mulib__defaultIsSymbolic()) {
                        result = nonSymbolicDefaultElement();
                    } else {
                        // If symbolic is required, optional aliasing etc. is handled here
                        result = symbolicDefaultDuringInitializationForConcreteLength(se);
                    }
                    cachedElements.put(Sint.concSint(i), result);
                }
            }
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

        boolean elementsAreSarraySarrays() {
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
            return generateNonSarraySarrayWithSpecifiedIsNull(false, lengths[0], nextInnerElementsType, false, se);
        }

        @SuppressWarnings("unchecked")
        private static Sarray generateNonSarraySarrayWithSpecifiedIsNull(boolean canBeNull, Sint len, Class<?> innermostType, boolean defaultIsSymbolic, SymbolicExecution se) {
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
                        defaultIsSymbolic,
                        canBeNull
                );
            } else {
                throw new NotYetImplementedException();
            }
        }

        private static Sarray generateNonSarraySarrayWithDefaultIsNull(Sint len, Class<?> innermostType, boolean defaultIsSymbolic, SymbolicExecution se) {
            assert !innermostType.isArray();
            assert Sprimitive.class.isAssignableFrom(innermostType)
                    || PartnerClass.class.isAssignableFrom(innermostType);
            // Determine which kind of array must be set
            if (Sprimitive.class.isAssignableFrom(innermostType)) {
                if (innermostType == Sint.class) {
                    return se.sintSarray(len, defaultIsSymbolic);
                } else if (innermostType == Slong.class) {
                    return se.slongSarray(len, defaultIsSymbolic);
                } else if (innermostType == Sdouble.class) {
                    return se.sdoubleSarray(len, defaultIsSymbolic);
                } else if (innermostType == Sfloat.class) {
                    return se.sfloatSarray(len, defaultIsSymbolic);
                } else if (innermostType == Sshort.class) {
                    return se.sshortSarray(len, defaultIsSymbolic);
                } else if (innermostType == Sbyte.class) {
                    return se.sbyteSarray(len, defaultIsSymbolic);
                } else if (innermostType == Sbool.class) {
                    return se.sboolSarray(len, defaultIsSymbolic);
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
        protected Sarray generateSymbolicDefault(SymbolicExecution se, boolean canBeNull) {
            Sarray result;
            if (elementsAreSarraySarrays()) {
                assert elementType.getComponentType().isArray();
                result = se.sarraySarray(
                        se.symSint(),
                        elementType.getComponentType(),
                        true,
                        canBeNull
                );
            } else {
                result = generateNonSarraySarrayWithSpecifiedIsNull(
                        canBeNull,
                        se.symSint(),
                        elementType.getComponentType(),
                        true,
                        se
                );
            }
            return result;
        }

        private Sarray symbolicDefaultDuringInitializationForConcreteLength(SymbolicExecution se) {
            assert __mulib__defaultIsSymbolic()
                    && !__mulib__isRepresentedInSolver()
                    && _getLengthWithoutCheckingForIsNull() instanceof ConcSnumber;
            Sarray result;
            if (elementsAreSarraySarrays()) {
                result = se.sarraySarray(se.symSint(), elementType.getComponentType(), true);
            } else {
                result = generateNonSarraySarrayWithDefaultIsNull(se.symSint(), elementType.getComponentType(), true, se);
            }
            return result;
        }

        @Override
        public SarraySarray copy(MulibValueCopier mvt) {
            return new SarraySarray(mvt, this);
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
    }

}
