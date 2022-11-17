package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.exceptions.NotYetImplementedException;

public abstract class AbstractSnumber implements Snumber {

    protected AbstractSnumber() {}

    /**
     * Implementation for ConcSnumbers
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        assert this instanceof ConcSnumber;
        if (!(o instanceof ConcSnumber)) {
            return false;
        }
        ConcSnumber oc = (ConcSnumber) o;
        ConcSnumber thisc = (ConcSnumber) this;
        boolean isFp = isFp();
        boolean oIsFp = oc.isFp();
        if (isFp != oIsFp) {
            return false;
        }
        if (isFp) {
            return oc.doubleVal() == thisc.doubleVal();
        }
        if (o instanceof Sint) {
            return oc.intVal() == thisc.intVal();
        }
        if (o instanceof Slong) {
            return oc.longVal() == thisc.longVal();
        }
        throw new NotYetImplementedException(this + ", " + oc);
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
