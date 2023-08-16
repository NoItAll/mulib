package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.search.NumberUtil;

/**
 * Abstract supertype for all Snumbers. Mostly predefines a a default equals-method and a default hashCode for
 * {@link ConcSnumber}s.
 */
public abstract class AbstractSnumber implements Snumber {

    /**
     * Construct a new AbstractSnumber
     */
    protected AbstractSnumber() {}

    @Override
    public boolean equals(Object o) {
        assert this instanceof ConcSnumber;
        if (this == o) {
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        ConcSnumber oc = (ConcSnumber) o;
        ConcSnumber thisc = (ConcSnumber) this;
        return NumberUtil.eq(oc, thisc);
    }

    @Override
    public int hashCode() {
        if (this instanceof ConcSnumber) {
            return ((ConcSnumber) this).intVal();
        } else {
            return super.hashCode();
        }
    }
}
