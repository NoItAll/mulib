package de.wwu.mulib.substitutions.primitives;

public abstract class AbstractSnumber implements Snumber {

    protected AbstractSnumber() {}

    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }

        boolean thisIsConcSnumber = this instanceof ConcSnumber;
        boolean oIsConcSnumber = o instanceof ConcSnumber;

        if (thisIsConcSnumber != oIsConcSnumber) {
            return false;
        }

        if (thisIsConcSnumber) {
            ConcSnumber co = (ConcSnumber) o;
            ConcSnumber cthis = ((ConcSnumber) this);
            boolean oIsFp = co.isFp();
            boolean thisIsFp = this.isFp();

            if (oIsFp != thisIsFp) {
                return false;
            }

            if (oIsFp) {
                return co.doubleVal() == cthis.doubleVal();
            } else {
                return co.longVal() == cthis.longVal();
            }
        } else {
            return o == this;
        }
    }

    @Override
    public int hashCode() {
        if (this instanceof ConcSnumber) {
            return ((ConcSnumber) this).intVal();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + (this instanceof SymNumericExpressionSprimitive
                && ((SymNumericExpressionSprimitive) this).getRepresentedExpression() != this ?
                    "e=" + ((SymNumericExpressionSprimitive) this).getRepresentedExpression() + ", "
                    :
                    "")
                + additionToToStringBody()
                + "}";
    }

}
