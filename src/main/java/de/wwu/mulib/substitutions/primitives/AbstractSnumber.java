package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSnumber implements Snumber {

    protected static AtomicLong nextId = new AtomicLong(0);
    protected final long id;

    protected AbstractSnumber() {
        id = nextId.incrementAndGet();
    }

    @Override
    public final <T extends Sprimitive> T castTo(Class<T> castToClass, SymbolicExecution se) {
        return se.castTo(this, castToClass);
    }

    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public final long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
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
        return this.getClass().getSimpleName() + "{id=" + id
                + (this instanceof SymNumericExpressionSprimitive
                && ((SymNumericExpressionSprimitive) this).getRepresentedExpression() != this ?
                    ",e=" + ((SymNumericExpressionSprimitive) this).getRepresentedExpression()
                    :
                    "")
                + additionToToStringBody()
                + "}";
    }

}
