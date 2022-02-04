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

    public T generateElement(SymbolicExecution se) {
        if (defaultIsSymbolic) {
            return symbolicDefault(se);
        } else {
            return defaultElement(se);
        }
    }

    @Override
    public String toString() {
        return "Sarray{id=" + id + ", elements=" + elements + "}";
    }

    public abstract T symbolicDefault(SymbolicExecution se);

    public abstract T defaultElement(SymbolicExecution se);

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

    public final Class<T> getClazz() {
        return clazz;
    }

    public abstract T select(Sint i, SymbolicExecution se);

    public abstract T store(Sint i, T val, SymbolicExecution se);

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

    public static class SintSarray extends Sarray<Sint> {

        public SintSarray(Sint len, SymbolicExecution se, boolean defaultIsSymbolic) {
            super(Sint.class, len, se, defaultIsSymbolic);
        }

        @Override
        public final Sint select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final Sint store(Sint i, Sint val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sint symbolicDefault(SymbolicExecution se) {
            return se.symSint();
        }

        @Override
        public Sint defaultElement(SymbolicExecution se) {
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
        public final Sdouble store(Sint i, Sdouble val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sdouble symbolicDefault(SymbolicExecution se) {
            return se.symSdouble();
        }

        @Override
        public Sdouble defaultElement(SymbolicExecution se) {
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
        public final Sfloat store(Sint i, Sfloat val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sfloat symbolicDefault(SymbolicExecution se) {
            return se.symSfloat();
        }

        @Override
        public Sfloat defaultElement(SymbolicExecution se) {
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
        public final Slong store(Sint i, Slong val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Slong symbolicDefault(SymbolicExecution se) {
            return se.symSlong();
        }

        @Override
        public Slong defaultElement(SymbolicExecution se) {
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
        public final Sshort store(Sint i, Sshort val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sshort symbolicDefault(SymbolicExecution se) {
            return se.symSshort();
        }

        @Override
        public Sshort defaultElement(SymbolicExecution se) {
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
        public final Sbyte store(Sint i, Sbyte val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sbyte symbolicDefault(SymbolicExecution se) {
            return se.symSbyte();
        }

        @Override
        public Sbyte defaultElement(SymbolicExecution se) {
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
        public final Sbool store(Sint i, Sbool val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sbool symbolicDefault(SymbolicExecution se) {
            return se.symSbool();
        }

        @Override
        public Sbool defaultElement(SymbolicExecution se) {
            return Sbool.FALSE;
        }
    }

    public static class PartnerClassSarray extends Sarray<PartnerClass> {

        public PartnerClassSarray(Class<PartnerClass> clazz, Sint len, SymbolicExecution se,
                                  boolean defaultIsSymbolic) {
            super(clazz, len, se, defaultIsSymbolic);
        }

        @Override
        public final PartnerClass select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final PartnerClass store(Sint i, PartnerClass val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public PartnerClass symbolicDefault(SymbolicExecution se) {
            throw new NotYetImplementedException();
        }

        @Override
        public PartnerClass defaultElement(SymbolicExecution se) {
            return null;
        }
    }

    public static class SarraySarray extends Sarray<Sarray> {

        public SarraySarray(Class<Sarray> clazz, Sint len, SymbolicExecution se,
                            boolean defaultIsSymbolic) {
            super(clazz, len, se, defaultIsSymbolic);
        }

        @Override
        public final Sarray select(Sint i, SymbolicExecution se) {
            return se.select(this, i);
        }

        @Override
        public final Sarray store(Sint i, Sarray val, SymbolicExecution se) {
            return se.store(this, i, val);
        }

        @Override
        public Sarray symbolicDefault(SymbolicExecution se) {
            throw new NotYetImplementedException();
        }

        @Override
        public Sarray defaultElement(SymbolicExecution se) {
            return null;
        }
    }
}
