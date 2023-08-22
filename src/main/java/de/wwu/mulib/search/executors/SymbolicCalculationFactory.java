package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

/**
 * The default symbolic calculation factory. Constructs wrapper objects containing constraint or
 * numeric expressions.
 */
public class SymbolicCalculationFactory extends AbstractCalculationFactory {

    SymbolicCalculationFactory(MulibConfig config, ValueFactory valueFactory) {
        super(
                config,
                valueFactory,
                snumber -> snumber,
                sbool -> sbool
        );
    }

    public static SymbolicCalculationFactory getInstance(MulibConfig config, ValueFactory valueFactory) {
        return new SymbolicCalculationFactory(config, valueFactory);
    }

    @Override
    public Sbool implies(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        if (lhs instanceof Sbool.ConcSbool && rhs instanceof Sbool.ConcSbool) {
            return Sbool.concSbool(((Sbool.ConcSbool) lhs).isFalse() || ((Sbool.ConcSbool) rhs).isTrue());
        }
        return valueFactory.wrappingSymSbool(se, Implication.newInstance(lhs, rhs));
    }

    @Override
    public Sint add(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) lhs).intVal() + ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, sum(lhs, rhs));
    }

    @Override
    public Sint sub(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) lhs).intVal() - ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, sub(lhs, rhs));
    }

    @Override
    public Sint mul(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) lhs).intVal() * ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, mul(lhs, rhs));
    }

    @Override
    public Sint div(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) lhs).intVal() / ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, div(lhs, rhs));
    }

    @Override
    public Sint mod(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) lhs).intVal() % ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, mod(lhs, rhs));
    }

    @Override
    public Sint neg(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Sint.concSint(- ((ConcSnumber) i).intVal());
        }
        return valueFactory.wrappingSymSint(se, neg(i));
    }

    @Override
    public Sdouble add(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() + ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, sum(lhs, rhs));
    }

    @Override
    public Sdouble sub(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() - ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, sub(lhs, rhs));
    }

    @Override
    public Sdouble mul(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() * ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, mul(lhs, rhs));
    }

    @Override
    public Sdouble div(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() / ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, div(lhs, rhs));
    }

    @Override
    public Sdouble mod(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() % ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, mod(lhs, rhs));
    }

    @Override
    public Sdouble neg(SymbolicExecution se, Sdouble d) {
        if (d instanceof Sdouble.ConcSdouble) {
            return Sdouble.concSdouble(- ((Sdouble.ConcSdouble) d).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, neg(d));
    }

    @Override
    public Slong add(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Slong.concSlong(((Slong.ConcSlong) lhs).longVal() + ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, sum(lhs, rhs));
    }

    @Override
    public Slong sub(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Slong.concSlong(((Slong.ConcSlong) lhs).longVal() - ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, sub(lhs, rhs));
    }

    @Override
    public Slong mul(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Slong.concSlong(((Slong.ConcSlong) lhs).longVal() * ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, mul(lhs, rhs));
    }

    @Override
    public Slong div(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Slong.concSlong(((Slong.ConcSlong) lhs).longVal() / ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, div(lhs, rhs));
    }

    @Override
    public Slong mod(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Slong.concSlong(((Slong.ConcSlong) lhs).longVal() % ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, mod(lhs, rhs));
    }

    @Override
    public Slong neg(SymbolicExecution se, Slong l) {
        if (l instanceof Slong.ConcSlong) {
            return Slong.concSlong(- ((Slong.ConcSlong) l).longVal());
        }
        return valueFactory.wrappingSymSlong(se, neg(l));
    }

    @Override
    public Sfloat add(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() + ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, sum(lhs, rhs));
    }

    @Override
    public Sfloat sub(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() - ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, sub(lhs, rhs));
    }

    @Override
    public Sfloat mul(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() * ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, mul(lhs, rhs));
    }

    @Override
    public Sfloat div(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() / ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, div(lhs, rhs));
    }

    @Override
    public Sfloat mod(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() % ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, mod(lhs, rhs));
    }

    @Override
    public Sfloat neg(SymbolicExecution se, Sfloat f) {
        if (f instanceof Sfloat.ConcSfloat) {
            return Sfloat.concSfloat(- ((Sfloat.ConcSfloat) f).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, neg(f));
    }

    @Override
    public Sbool and(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            if (((Sbool.ConcSbool) lhs).isFalse()) {
                return Sbool.ConcSbool.FALSE;
            } else {
                return rhs;
            }
        }
        if (rhs instanceof Sbool.ConcSbool) {
            if (((Sbool.ConcSbool) rhs).isFalse()) {
                return Sbool.ConcSbool.FALSE;
            } else {
                return lhs;
            }
        }
        return valueFactory.wrappingSymSbool(se, and(lhs, rhs));
    }

    @Override
    public Sbool or(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        if (lhs instanceof Sbool.ConcSbool) {
            if (((Sbool.ConcSbool) lhs).isTrue()) {
                return Sbool.ConcSbool.TRUE;
            } else {
                return rhs;
            }
        }
        if (rhs instanceof Sbool.ConcSbool) {
            if (((Sbool.ConcSbool) rhs).isTrue()) {
                return Sbool.ConcSbool.TRUE;
            } else {
                return lhs;
            }
        }
        return valueFactory.wrappingSymSbool(se, or(lhs, rhs));
    }

    @Override
    public Sbool xor(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        Constraint result = Xor.newInstance(lhs, rhs);
        if (result instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) result;
        }
        return valueFactory.wrappingSymSbool(se, result);
    }

    @Override
    public Sbool not(SymbolicExecution se, Sbool b) {
        if (b instanceof Sbool.ConcSbool) {
            return Sbool.concSbool(((Sbool.ConcSbool) b).isFalse());
        }
        return valueFactory.wrappingSymSbool(se, not(b));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sbool.concSbool(((ConcSnumber) lhs).intVal() < ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Sbool.concSbool(((Slong.ConcSlong) lhs).longVal() < ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sbool.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() < ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sbool.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() < ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return Sbool.concSbool(((ConcSnumber) lhs).intVal() <= ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return Sbool.concSbool(((Slong.ConcSlong) lhs).longVal() <= ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return Sbool.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() <= ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return Sbool.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() <= ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sint lhs, Sint rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return valueFactory.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Slong lhs, Slong rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return valueFactory.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return valueFactory.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return valueFactory.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            long lrhs = ((Slong.ConcSlong) rhs).longVal();
            long llhs = ((Slong.ConcSlong) lhs).longVal();
            return Sint.concSint(Long.compare(llhs, lrhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            double drhs = ((Sdouble.ConcSdouble) rhs).doubleVal();
            double dlhs = ((Sdouble.ConcSdouble) lhs).doubleVal();
            return Sint.concSint(Double.compare(dlhs, drhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            float frhs = ((Sfloat.ConcSfloat) rhs).floatVal();
            float flhs = ((Sfloat.ConcSfloat) lhs).floatVal();
            return Sint.concSint(Float.compare(flhs, frhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Slong i2l(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) i).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Sfloat i2f(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Sfloat.concSfloat(((ConcSnumber) i).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Sdouble i2d(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Sdouble.concSdouble(((ConcSnumber) i).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Schar i2c(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Schar.concSchar(((ConcSnumber) i).charVal());
        }
        return valueFactory.wrappingSymSchar(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Sint l2i(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) l).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymSnumber) l).getRepresentedExpression());
    }

    @Override
    public Sfloat l2f(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return Sfloat.concSfloat(((ConcSnumber) l).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymSnumber) l).getRepresentedExpression());
    }

    @Override
    public Sdouble l2d(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return Sdouble.concSdouble(((ConcSnumber) l).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymSnumber) l).getRepresentedExpression());
    }

    @Override
    public Sint f2i(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) f).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymSnumber) f).getRepresentedExpression());
    }

    @Override
    public Slong f2l(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) f).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymSnumber) f).getRepresentedExpression());
    }

    @Override
    public Sdouble f2d(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return Sdouble.concSdouble(((ConcSnumber) f).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymSnumber) f).getRepresentedExpression());
    }

    @Override
    public Sint d2i(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) d).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymSnumber) d).getRepresentedExpression());
    }

    @Override
    public Slong d2l(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) d).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymSnumber) d).getRepresentedExpression());
    }

    @Override
    public Sfloat d2f(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return Sfloat.concSfloat(((ConcSnumber) d).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymSnumber) d).getRepresentedExpression());
    }

    @Override
    public Sbyte i2b(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Sbyte.concSbyte(((ConcSnumber) i).byteVal());
        }
        return valueFactory.wrappingSymSbyte(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Sshort i2s(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return Sshort.concSshort(((ConcSnumber) i).shortVal());
        }
        return valueFactory.wrappingSymSshort(se, ((SymSnumber) i).getRepresentedExpression());
    }

    @Override
    public Sint ishl(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() << ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, ShiftLeft.newInstance(i0, i1));
    }

    @Override
    public Sint ishr(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() >> ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, ShiftRight.newInstance(i0, i1));
    }

    @Override
    public Sint ixor(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() ^ ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, NumericalXor.newInstance(i0, i1));
    }

    @Override
    public Sint ior(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() | ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, NumericalOr.newInstance(i0, i1));
    }

    @Override
    public Sint iushr(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() >>> ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, LogicalShiftRight.newInstance(i0, i1));
    }

    @Override
    public Sint iand(SymbolicExecution se, Sint i0, Sint i1) {
        if (i0 instanceof ConcSnumber && i1 instanceof ConcSnumber) {
            return Sint.concSint(((ConcSnumber) i0).intVal() & ((ConcSnumber) i1).intVal());
        }
        return valueFactory.wrappingSymSint(se, ShiftLeft.newInstance(i0, i1));
    }

    @Override
    public Slong lshl(SymbolicExecution se, Slong l0, Sint l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() << ((ConcSnumber) l1).intVal());
        }
        return valueFactory.wrappingSymSlong(se, ShiftLeft.newInstance(l0, l1));
    }

    @Override
    public Slong lshr(SymbolicExecution se, Slong l0, Sint l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() >> ((ConcSnumber) l1).intVal());
        }
        return valueFactory.wrappingSymSlong(se, ShiftRight.newInstance(l0, l1));
    }

    @Override
    public Slong lxor(SymbolicExecution se, Slong l0, Slong l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() ^ ((ConcSnumber) l1).longVal());
        }
        return valueFactory.wrappingSymSlong(se, NumericalXor.newInstance(l0, l1));
    }

    @Override
    public Slong lor(SymbolicExecution se, Slong l0, Slong l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() | ((ConcSnumber) l1).longVal());
        }
        return valueFactory.wrappingSymSlong(se, NumericalOr.newInstance(l0, l1));
    }

    @Override
    public Slong lushr(SymbolicExecution se, Slong l0, Sint l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() >>> ((ConcSnumber) l1).intVal());
        }
        return valueFactory.wrappingSymSlong(se, LogicalShiftRight.newInstance(l0, l1));
    }

    @Override
    public Slong land(SymbolicExecution se, Slong l0, Slong l1) {
        if (l0 instanceof ConcSnumber && l1 instanceof ConcSnumber) {
            return Slong.concSlong(((ConcSnumber) l0).longVal() & ((ConcSnumber) l1).longVal());
        }
        return valueFactory.wrappingSymSlong(se, NumericalAnd.newInstance(l0, l1));
    }

    @Override
    protected Sprimitive getValueToBeUsedForPartnerClassObjectConstraint(Substituted value) {
        if (value instanceof PartnerClass) {
            assert ((PartnerClass) value).__mulib__isRepresentedInSolver();
            return ((PartnerClass) value).__mulib__getId();
        }
        return (Sprimitive) value;
    }

    @Override
    protected void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds) {
        se.addNewConstraint(indexInBounds);
    }

    private NumericalExpression sum(NumericalExpression lhs, NumericalExpression rhs) {
        return Sum.newInstance(lhs, rhs);
    }

    private NumericalExpression sub(NumericalExpression lhs, NumericalExpression rhs) {
        return Sub.newInstance(lhs, rhs);
    }

    private NumericalExpression mul(NumericalExpression lhs, NumericalExpression rhs) {
        return Mul.newInstance(lhs, rhs);
    }

    private NumericalExpression div(NumericalExpression lhs, NumericalExpression rhs) {
        return Div.newInstance(lhs, rhs);
    }

    private NumericalExpression mod(NumericalExpression lhs, NumericalExpression rhs) {
        return Mod.newInstance(lhs, rhs);
    }

    private NumericalExpression neg(NumericalExpression n) {
        return Neg.neg(n);
    }

    private Constraint and(Constraint lhs, Constraint rhs) {
        return And.newInstance(lhs, rhs);
    }

    private Constraint or(Constraint lhs, Constraint rhs) {
        return Or.newInstance(lhs, rhs);
    }

    private Constraint not(Constraint b) {
        return Not.newInstance(b);
    }

    private Constraint lt(NumericalExpression lhs, NumericalExpression rhs) {
        return Lt.newInstance(lhs, rhs);
    }

    private Constraint lte(NumericalExpression lhs, NumericalExpression rhs) {
        return Lte.newInstance(lhs, rhs);
    }

    private Constraint eq(NumericalExpression lhs, NumericalExpression rhs) {
        return Eq.newInstance(lhs, rhs);
    }
}
