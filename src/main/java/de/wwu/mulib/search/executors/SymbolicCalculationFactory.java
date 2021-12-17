package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

public class SymbolicCalculationFactory implements CalculationFactory {

    SymbolicCalculationFactory(MulibConfig config) {}

    public static SymbolicCalculationFactory getInstance(MulibConfig config) {
        return new SymbolicCalculationFactory(config);
    }

    @Override
    public Sint add(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) lhs).intVal() + ((ConcSnumber) rhs).intVal());
        }
        return vf.wrappingSymSint(se, sum(lhs, rhs));
    }

    @Override
    public Sint sub(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSint(((Sint.ConcSint) lhs).intVal() - ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSint(se, sub(lhs, rhs));
    }

    @Override
    public Sint mul(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSint(((Sint.ConcSint) lhs).intVal() * ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSint(se, mul(lhs, rhs));
    }

    @Override
    public Sint div(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSint(((Sint.ConcSint) lhs).intVal() / ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSint(se, div(lhs, rhs));
    }

    @Override
    public Sint mod(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSint(((Sint.ConcSint) lhs).intVal() % ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSint(se, mod(lhs, rhs));
    }

    @Override
    public Sint neg(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSint(- ((ConcSnumber) i).intVal());
        }
        return vf.wrappingSymSint(se, neg(i));
    }

    @Override
    public Sdouble add(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() + ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSdouble(se, sum(lhs, rhs));
    }

    @Override
    public Sdouble sub(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() - ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSdouble(se, sub(lhs, rhs));
    }

    @Override
    public Sdouble mul(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() * ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSdouble(se, mul(lhs, rhs));
    }

    @Override
    public Sdouble div(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() / ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSdouble(se, div(lhs, rhs));
    }

    @Override
    public Sdouble mod(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() % ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSdouble(se, mod(lhs, rhs));
    }

    @Override
    public Sdouble neg(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        if (d instanceof Sdouble.ConcSdouble) {
            return vf.concSdouble(- ((Sdouble.ConcSdouble) d).doubleVal());
        }
        return vf.wrappingSymSdouble(se, neg(d));
    }

    @Override
    public Slong add(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSlong(((Slong.ConcSlong) lhs).longVal() + ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSlong(se, sum(lhs, rhs));
    }

    @Override
    public Slong sub(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSlong(((Slong.ConcSlong) lhs).longVal() - ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSlong(se, sub(lhs, rhs));
    }

    @Override
    public Slong mul(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSlong(((Slong.ConcSlong) lhs).longVal() * ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSlong(se, mul(lhs, rhs));
    }

    @Override
    public Slong div(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSlong(((Slong.ConcSlong) lhs).longVal() / ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSlong(se, div(lhs, rhs));
    }

    @Override
    public Slong mod(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSlong(((Slong.ConcSlong) lhs).longVal() % ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSlong(se, mod(lhs, rhs));
    }

    @Override
    public Slong neg(SymbolicExecution se, ValueFactory vf, Slong l) {
        if (l instanceof Slong.ConcSlong) {
            return vf.concSlong(- ((Slong.ConcSlong) l).longVal());
        }
        return vf.wrappingSymSlong(se, neg(l));
    }

    @Override
    public Sfloat add(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() + ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return vf.wrappingSymSfloat(se, sum(lhs, rhs));
    }

    @Override
    public Sfloat sub(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() - ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return vf.wrappingSymSfloat(se, sub(lhs, rhs));
    }

    @Override
    public Sfloat mul(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() * ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return vf.wrappingSymSfloat(se, mul(lhs, rhs));
    }

    @Override
    public Sfloat div(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() / ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return vf.wrappingSymSfloat(se, div(lhs, rhs));
    }

    @Override
    public Sfloat mod(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() % ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return vf.wrappingSymSfloat(se, mod(lhs, rhs));
    }

    @Override
    public Sfloat neg(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        if (f instanceof Sfloat.ConcSfloat) {
            return vf.concSfloat(- ((Sfloat.ConcSfloat) f).floatVal());
        }
        return vf.wrappingSymSfloat(se, neg(f));
    }

    @Override
    public Sbool and(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        if (lhs instanceof Sbool.ConcSbool && rhs instanceof Sbool.ConcSbool) {
            return vf.concSbool(((Sbool.ConcSbool) lhs).isTrue() && ((Sbool.ConcSbool) rhs).isTrue());
        }
        return vf.wrappingSymSbool(se, and(lhs, rhs));
    }

    @Override
    public Sbool or(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        if (lhs instanceof Sbool.ConcSbool && rhs instanceof Sbool.ConcSbool) {
            return vf.concSbool(((Sbool.ConcSbool) lhs).isTrue() || ((Sbool.ConcSbool) rhs).isTrue());
        }
        return vf.wrappingSymSbool(se, or(lhs, rhs));
    }

    @Override
    public Sbool not(SymbolicExecution se, ValueFactory vf, Sbool b) {
        if (b instanceof Sbool.ConcSbool) {
            return vf.concSbool(((Sbool.ConcSbool) b).isFalse());
        }
        return vf.wrappingSymSbool(se, not(b));
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSbool(((Sint.ConcSint) lhs).intVal() < ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSbool(((Slong.ConcSlong) lhs).longVal() < ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() < ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() < ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSbool(((Sint.ConcSint) lhs).intVal() <= ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSbool(((Slong.ConcSlong) lhs).longVal() <= ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() <= ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() <= ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof Sint.ConcSint && rhs instanceof Sint.ConcSint) {
            return vf.concSbool(((Sint.ConcSint) lhs).intVal() == ((Sint.ConcSint) rhs).intVal());
        }
        return vf.wrappingSymSbool(se, eq(lhs, rhs));
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return vf.concSbool(((Slong.ConcSlong) lhs).longVal() == ((Slong.ConcSlong) rhs).longVal());
        }
        return vf.wrappingSymSbool(se, eq(lhs, rhs));
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return vf.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() == ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, eq(lhs, rhs));
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return vf.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() == ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return vf.wrappingSymSbool(se, eq(lhs, rhs));
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            long lrhs = ((Slong.ConcSlong) rhs).longVal();
            long llhs = ((Slong.ConcSlong) lhs).longVal();
            return vf.concSint(Long.compare(llhs, lrhs));
        }
        return vf.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            double drhs = ((Sdouble.ConcSdouble) rhs).doubleVal();
            double dlhs = ((Sdouble.ConcSdouble) lhs).doubleVal();
            return vf.concSint(Double.compare(dlhs, drhs));
        }
        return vf.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            float frhs = ((Sfloat.ConcSfloat) rhs).floatVal();
            float flhs = ((Sfloat.ConcSfloat) lhs).floatVal();
            return vf.concSint(Float.compare(flhs, frhs));
        }
        return vf.cmp(se, lhs, rhs);
    }

    protected NumericExpression sum(NumericExpression lhs, NumericExpression rhs) {
        return Sum.newInstance(lhs, rhs);
    }

    protected NumericExpression sub(NumericExpression lhs, NumericExpression rhs) {
        return Sub.newInstance(lhs, rhs);
    }

    protected NumericExpression mul(NumericExpression lhs, NumericExpression rhs) {
        return Mul.newInstance(lhs, rhs);
    }

    protected NumericExpression div(NumericExpression lhs, NumericExpression rhs) {
        return Div.newInstance(lhs, rhs);
    }

    protected NumericExpression mod(NumericExpression lhs, NumericExpression rhs) {
        return Mod.newInstance(lhs, rhs);
    }

    protected NumericExpression neg(NumericExpression n) {
        return Neg.neg(n);
    }

    protected Constraint and(Sbool lhs, Sbool rhs) {
        return new And(lhs, rhs);
    }

    protected Constraint or(Sbool lhs, Sbool rhs) {
        return new Or(lhs, rhs);
    }

    protected Constraint not(Sbool b) {
        return new Not(b);
    }

    protected Constraint lt(Snumber lhs, Snumber rhs) {
        return new Lt(lhs, rhs);
    }

    protected Constraint lte(Snumber lhs, Snumber rhs) {
        return new Lte(lhs, rhs);
    }

    protected Constraint eq(Snumber lhs, Snumber rhs) {
        return new Eq(lhs, rhs);
    }
}
