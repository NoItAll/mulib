package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Set;

@SuppressWarnings( { "rawtypes", "unchecked" })
public class SymbolicCalculationFactory extends AbstractCalculationFactory {

    SymbolicCalculationFactory(MulibConfig config) {
        super(config);
    }

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
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) lhs).intVal() - ((ConcSnumber) rhs).intVal());
        }
        return vf.wrappingSymSint(se, sub(lhs, rhs));
    }

    @Override
    public Sint mul(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) lhs).intVal() * ((ConcSnumber) rhs).intVal());
        }
        return vf.wrappingSymSint(se, mul(lhs, rhs));
    }

    @Override
    public Sint div(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) lhs).intVal() / ((ConcSnumber) rhs).intVal());
        }
        return vf.wrappingSymSint(se, div(lhs, rhs));
    }

    @Override
    public Sint mod(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) lhs).intVal() % ((ConcSnumber) rhs).intVal());
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
        return vf.wrappingSymSbool(se, and(lhs, rhs));
    }

    @Override
    public Sbool or(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
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
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSbool(((ConcSnumber) lhs).intVal() < ((ConcSnumber) rhs).intVal());
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
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return vf.concSbool(((ConcSnumber) lhs).intVal() <= ((ConcSnumber) rhs).intVal());
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
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return vf.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return vf.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return vf.wrappingSymSbool(se, eqConstraint);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Constraint eqConstraint = eq(lhs, rhs);
        if (eqConstraint instanceof Sbool.ConcSbool) {
            return (Sbool.ConcSbool) eqConstraint;
        }
        return vf.wrappingSymSbool(se, eqConstraint);
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

    @Override
    public Slong i2l(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSlong(((ConcSnumber) i).longVal());
        }
        return vf.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sfloat i2f(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSfloat(((ConcSnumber) i).floatVal());
        }
        return vf.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sdouble i2d(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSdouble(((ConcSnumber) i).doubleVal());
        }
        return vf.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sint l2i(SymbolicExecution se, ValueFactory vf, Slong l) {
        if (l instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) l).intVal());
        }
        return vf.wrappingSymSint(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sfloat l2f(SymbolicExecution se, ValueFactory vf, Slong l) {
        if (l instanceof ConcSnumber) {
            return vf.concSfloat(((ConcSnumber) l).floatVal());
        }
        return vf.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sdouble l2d(SymbolicExecution se, ValueFactory vf, Slong l) {
        if (l instanceof ConcSnumber) {
            return vf.concSdouble(((ConcSnumber) l).doubleVal());
        }
        return vf.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sint f2i(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) f).intVal());
        }
        return vf.wrappingSymSint(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Slong f2l(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return vf.concSlong(((ConcSnumber) f).longVal());
        }
        return vf.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Sdouble f2d(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return vf.concSdouble(((ConcSnumber) f).doubleVal());
        }
        return vf.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Sint d2i(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return vf.concSint(((ConcSnumber) d).intVal());
        }
        return vf.wrappingSymSint(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Slong d2l(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return vf.concSlong(((ConcSnumber) d).longVal());
        }
        return vf.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Sfloat d2f(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return vf.concSfloat(((ConcSnumber) d).floatVal());
        }
        return vf.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Sbyte i2b(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSbyte(((ConcSnumber) i).byteVal());
        }
        return vf.wrappingSymSbyte(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sshort i2s(SymbolicExecution se, ValueFactory vf, Sint i) {
        if (i instanceof ConcSnumber) {
            return vf.concSshort(((ConcSnumber) i).shortVal());
        }
        return vf.wrappingSymSshort(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    protected SubstitutedVar _selectWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        SubstitutedVar result = sarray.getFromCacheForIndex(index);
        if (result != null) {
            // We don't have to check for the validity of the cached index: we remove all indexes if store was used and
            // symbolic indexes have been used. Thus, the current index is valid.
            return result;
        }

        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);

        // Generate new value
        if (!sarray.defaultIsSymbolic() && sarray.onlyConcreteIndicesUsed()) {
            result = sarray.nonSymbolicDefaultElement(se);
        } else {
            result = sarray.symbolicDefault(se);
        }
        addSelectConstraintIfNeeded(se, sarray, index, result);
        sarray.setInCacheForIndex(index, result);

        return result;
    }

    @Override
    protected SubstitutedVar _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        // Similarly to select, we will notify the solver, if needed, that the representation of the array has changed.
        if (!sarray.onlyConcreteIndicesUsed()) {
            // We must reset the cached elements, if a symbolic variable is present and store was used
            sarray.clearCachedElements();
            if (!se.nextIsOnKnownPath()) {
                ArrayConstraint storeConstraint =
                        new ArrayConstraint(sarray.getId(), index, value, ArrayConstraint.Type.STORE);
                se.addNewArrayConstraint(storeConstraint);
            }
        }

        sarray.setInCacheForIndex(index, value);
        return value;
    }

    private static void representArrayViaConstraintsIfNeeded(SymbolicExecution se, Sarray sarray, Sint index) {
        if (sarray.checkIfNeedsToRepresentOldEntries(index, se) && !se.nextIsOnKnownPath()) {
            Set<Sint> cachedIndices = sarray.getCachedIndices();
            assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";
            for (Sint i : cachedIndices) {
                ArrayConstraint ac =
                        new ArrayConstraint(sarray.getId(), i, sarray.getFromCacheForIndex(i), ArrayConstraint.Type.SELECT);
                se.addNewArrayConstraint(ac);
            }
        }
    }

    private static void addSelectConstraintIfNeeded(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar result) {
        // We will now add a constraint indicating to the solver that at position i a value can be found that previously
        // was not there. This only occurs if the array must be represented via constraints. This, in turn, only
        // is the case if symbolic indices have been used.
        if (!se.nextIsOnKnownPath() && !sarray.onlyConcreteIndicesUsed()) {
            ArrayConstraint selectConstraint =
                    new ArrayConstraint(sarray.getId(), index, result, ArrayConstraint.Type.SELECT);
            se.addNewArrayConstraint(selectConstraint);
        }
    }

    @Override
    protected void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds) {
        se.addNewConstraint(indexInBounds);
    }

    private NumericExpression sum(NumericExpression lhs, NumericExpression rhs) {
        return Sum.newInstance(lhs, rhs);
    }

    private NumericExpression sub(NumericExpression lhs, NumericExpression rhs) {
        return Sub.newInstance(lhs, rhs);
    }

    private NumericExpression mul(NumericExpression lhs, NumericExpression rhs) {
        return Mul.newInstance(lhs, rhs);
    }

    private NumericExpression div(NumericExpression lhs, NumericExpression rhs) {
        return Div.newInstance(lhs, rhs);
    }

    private NumericExpression mod(NumericExpression lhs, NumericExpression rhs) {
        return Mod.newInstance(lhs, rhs);
    }

    private NumericExpression neg(NumericExpression n) {
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

    private Constraint lt(NumericExpression lhs, NumericExpression rhs) {
        return Lt.newInstance(lhs, rhs);
    }

    private Constraint lte(NumericExpression lhs, NumericExpression rhs) {
        return Lte.newInstance(lhs, rhs);
    }

    private Constraint eq(NumericExpression lhs, NumericExpression rhs) {
        return Eq.newInstance(lhs, rhs);
    }
}
