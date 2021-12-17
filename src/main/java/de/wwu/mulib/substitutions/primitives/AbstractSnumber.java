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
    public final Sbool lt(Snumber rhs, SymbolicExecution se) {
        return se.lt(this, rhs);
    }
    @Override
    public final Sbool lte(Snumber rhs, SymbolicExecution se) {
        return se.lte(this, rhs);
    }
    @Override
    public final Sbool gt(Snumber rhs, SymbolicExecution se) {
        return se.gt(this, rhs);
    }
    @Override
    public final Sbool gte(Snumber rhs, SymbolicExecution se) {
        return se.gte(this, rhs);
    }
    @Override
    public final Sbool eq(Snumber rhs, SymbolicExecution se) {
        return se.eq(this, rhs);
    }
    @Override
    public final Sint cmp(Snumber rhs, SymbolicExecution se) {
        return se.cmp(this, rhs);
    }
    @Override
    public final boolean ltChoice(SymbolicExecution se) {
        return se.ltChoice(this);
    }
    @Override
    public final boolean lteChoice(SymbolicExecution se) {
        return se.lteChoice(this);
    }
    @Override
    public final boolean eqChoice(SymbolicExecution se) {
        return se.eqChoice(this);
    }
    @Override
    public final boolean notEqChoice(SymbolicExecution se) {
        return se.notEqChoice(this);
    }
    @Override
    public final boolean gtChoice(SymbolicExecution se) {
        return se.gtChoice(this);
    }
    @Override
    public final boolean gteChoice(SymbolicExecution se) {
        return se.gteChoice(this);
    }
    @Override
    public final boolean  ltChoice(Snumber rhs, SymbolicExecution se) {
        return se.ltChoice(this, rhs);
    }
    @Override
    public final boolean lteChoice(Snumber rhs, SymbolicExecution se) {
        return se.lteChoice(this, rhs);
    }
    @Override
    public final boolean  eqChoice(Snumber rhs, SymbolicExecution se) {
        return se.eqChoice(this, rhs);
    }
    @Override
    public final boolean notEqChoice(Snumber rhs, SymbolicExecution se) {
        return se.notEqChoice(this, rhs);
    }
    @Override
    public final boolean  gtChoice(Snumber rhs, SymbolicExecution se) {
        return se.gtChoice(this, rhs);
    }
    @Override
    public final boolean gteChoice(Snumber rhs, SymbolicExecution se) {
        return se.gteChoice(this, rhs);
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
