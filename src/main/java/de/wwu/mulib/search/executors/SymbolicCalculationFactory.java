package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Set;
import java.util.stream.StreamSupport;

@SuppressWarnings( { "rawtypes", "unchecked" })
public class SymbolicCalculationFactory extends AbstractCalculationFactory {

    SymbolicCalculationFactory(MulibConfig config, ValueFactory valueFactory) {
        super(config, valueFactory);
    }

    public static SymbolicCalculationFactory getInstance(MulibConfig config, ValueFactory valueFactory) {
        return new SymbolicCalculationFactory(config, valueFactory);
    }

    @Override
    public Sint add(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) lhs).intVal() + ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, sum(lhs, rhs));
    }

    @Override
    public Sint sub(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) lhs).intVal() - ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, sub(lhs, rhs));
    }

    @Override
    public Sint mul(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) lhs).intVal() * ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, mul(lhs, rhs));
    }

    @Override
    public Sint div(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) lhs).intVal() / ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, div(lhs, rhs));
    }

    @Override
    public Sint mod(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) lhs).intVal() % ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSint(se, mod(lhs, rhs));
    }

    @Override
    public Sint neg(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSint(- ((ConcSnumber) i).intVal());
        }
        return valueFactory.wrappingSymSint(se, neg(i));
    }

    @Override
    public Sdouble add(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() + ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, sum(lhs, rhs));
    }

    @Override
    public Sdouble sub(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() - ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, sub(lhs, rhs));
    }

    @Override
    public Sdouble mul(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() * ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, mul(lhs, rhs));
    }

    @Override
    public Sdouble div(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() / ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, div(lhs, rhs));
    }

    @Override
    public Sdouble mod(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(((Sdouble.ConcSdouble) lhs).doubleVal() % ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, mod(lhs, rhs));
    }

    @Override
    public Sdouble neg(SymbolicExecution se, Sdouble d) {
        if (d instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSdouble(- ((Sdouble.ConcSdouble) d).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, neg(d));
    }

    @Override
    public Slong add(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(((Slong.ConcSlong) lhs).longVal() + ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, sum(lhs, rhs));
    }

    @Override
    public Slong sub(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(((Slong.ConcSlong) lhs).longVal() - ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, sub(lhs, rhs));
    }

    @Override
    public Slong mul(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(((Slong.ConcSlong) lhs).longVal() * ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, mul(lhs, rhs));
    }

    @Override
    public Slong div(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(((Slong.ConcSlong) lhs).longVal() / ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, div(lhs, rhs));
    }

    @Override
    public Slong mod(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(((Slong.ConcSlong) lhs).longVal() % ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSlong(se, mod(lhs, rhs));
    }

    @Override
    public Slong neg(SymbolicExecution se, Slong l) {
        if (l instanceof Slong.ConcSlong) {
            return valueFactory.concSlong(- ((Slong.ConcSlong) l).longVal());
        }
        return valueFactory.wrappingSymSlong(se, neg(l));
    }

    @Override
    public Sfloat add(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() + ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, sum(lhs, rhs));
    }

    @Override
    public Sfloat sub(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() - ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, sub(lhs, rhs));
    }

    @Override
    public Sfloat mul(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() * ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, mul(lhs, rhs));
    }

    @Override
    public Sfloat div(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() / ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, div(lhs, rhs));
    }

    @Override
    public Sfloat mod(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(((Sfloat.ConcSfloat) lhs).floatVal() % ((Sfloat.ConcSfloat) rhs).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, mod(lhs, rhs));
    }

    @Override
    public Sfloat neg(SymbolicExecution se, Sfloat f) {
        if (f instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSfloat(- ((Sfloat.ConcSfloat) f).floatVal());
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
    public Sbool not(SymbolicExecution se, Sbool b) {
        if (b instanceof Sbool.ConcSbool) {
            return valueFactory.concSbool(((Sbool.ConcSbool) b).isFalse());
        }
        return valueFactory.wrappingSymSbool(se, not(b));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSbool(((ConcSnumber) lhs).intVal() < ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSbool(((Slong.ConcSlong) lhs).longVal() < ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() < ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() < ((Sfloat.ConcSfloat) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lt(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sint lhs, Sint rhs) {
        if (lhs instanceof ConcSnumber && rhs instanceof ConcSnumber) {
            return valueFactory.concSbool(((ConcSnumber) lhs).intVal() <= ((ConcSnumber) rhs).intVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Slong lhs, Slong rhs) {
        if (lhs instanceof Slong.ConcSlong && rhs instanceof Slong.ConcSlong) {
            return valueFactory.concSbool(((Slong.ConcSlong) lhs).longVal() <= ((Slong.ConcSlong) rhs).longVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            return valueFactory.concSbool(((Sdouble.ConcSdouble) lhs).doubleVal() <= ((Sdouble.ConcSdouble) rhs).doubleVal());
        }
        return valueFactory.wrappingSymSbool(se, lte(lhs, rhs));
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            return valueFactory.concSbool(((Sfloat.ConcSfloat) lhs).doubleVal() <= ((Sfloat.ConcSfloat) rhs).doubleVal());
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
            return valueFactory.concSint(Long.compare(llhs, lrhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        if (lhs instanceof Sdouble.ConcSdouble && rhs instanceof Sdouble.ConcSdouble) {
            double drhs = ((Sdouble.ConcSdouble) rhs).doubleVal();
            double dlhs = ((Sdouble.ConcSdouble) lhs).doubleVal();
            return valueFactory.concSint(Double.compare(dlhs, drhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        if (lhs instanceof Sfloat.ConcSfloat && rhs instanceof Sfloat.ConcSfloat) {
            float frhs = ((Sfloat.ConcSfloat) rhs).floatVal();
            float flhs = ((Sfloat.ConcSfloat) lhs).floatVal();
            return valueFactory.concSint(Float.compare(flhs, frhs));
        }
        return valueFactory.cmp(se, lhs, rhs);
    }

    @Override
    public Slong i2l(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSlong(((ConcSnumber) i).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sfloat i2f(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSfloat(((ConcSnumber) i).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sdouble i2d(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSdouble(((ConcSnumber) i).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sint l2i(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) l).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sfloat l2f(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return valueFactory.concSfloat(((ConcSnumber) l).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sdouble l2d(SymbolicExecution se, Slong l) {
        if (l instanceof ConcSnumber) {
            return valueFactory.concSdouble(((ConcSnumber) l).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) l).getRepresentedExpression());
    }

    @Override
    public Sint f2i(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) f).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Slong f2l(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return valueFactory.concSlong(((ConcSnumber) f).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Sdouble f2d(SymbolicExecution se, Sfloat f) {
        if (f instanceof ConcSnumber) {
            return valueFactory.concSdouble(((ConcSnumber) f).doubleVal());
        }
        return valueFactory.wrappingSymSdouble(se, ((SymNumericExpressionSprimitive) f).getRepresentedExpression());
    }

    @Override
    public Sint d2i(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return valueFactory.concSint(((ConcSnumber) d).intVal());
        }
        return valueFactory.wrappingSymSint(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Slong d2l(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return valueFactory.concSlong(((ConcSnumber) d).longVal());
        }
        return valueFactory.wrappingSymSlong(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Sfloat d2f(SymbolicExecution se, Sdouble d) {
        if (d instanceof ConcSnumber) {
            return valueFactory.concSfloat(((ConcSnumber) d).floatVal());
        }
        return valueFactory.wrappingSymSfloat(se, ((SymNumericExpressionSprimitive) d).getRepresentedExpression());
    }

    @Override
    public Sbyte i2b(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSbyte(((ConcSnumber) i).byteVal());
        }
        return valueFactory.wrappingSymSbyte(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
    }

    @Override
    public Sshort i2s(SymbolicExecution se, Sint i) {
        if (i instanceof ConcSnumber) {
            return valueFactory.concSshort(((ConcSnumber) i).shortVal());
        }
        return valueFactory.wrappingSymSshort(se, ((SymNumericExpressionSprimitive) i).getRepresentedExpression());
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
        if (!sarray.defaultIsSymbolic() && !sarray.shouldBeRepresentedInSolver()) {
            result = sarray.nonSymbolicDefaultElement(se);
        } else {
            result = sarray.symbolicDefault(se);
        }
        addSelectConstraintIfNeeded(se, sarray, index, result);
        sarray.setInCacheForIndexForSelect(index, result);

        return result;
    }

    @Override
    protected SubstitutedVar _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        // Similarly to select, we will notify the solver, if needed, that the representation of the array has changed.
        if (sarray.shouldBeRepresentedInSolver()) {
            // We must reset the cached elements, if a symbolic variable is present and store was used
            sarray.clearCache();
            if (!se.nextIsOnKnownPath()) {
                ArrayConstraint storeConstraint =
                        new ArrayAccessConstraint(sarray.getId(), index, value, ArrayAccessConstraint.Type.STORE);
                se.addNewArrayConstraint(storeConstraint);
            }
        }

        sarray.setInCacheForIndexForStore(index, value);
        return value;
    }

    private static void representArrayViaConstraintsIfNeeded(SymbolicExecution se, Sarray sarray, Sint newIndex) {
        if (sarray.checkIfNeedsToRepresentOldEntries(newIndex, se)) {
            representArrayIfNeeded(se, sarray, null);
        }
    }

    private static ArrayAccessConstraint[] collectInitialArrayAccessConstraints(Sarray sarray, SymbolicExecution se) {
        assert !se.nextIsOnKnownPath();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        ArrayAccessConstraint[] initialConstraints = new ArrayAccessConstraint[cachedIndices.size()];
        int constraintNumber = 0;
        for (Sint i : cachedIndices) {
            SubstitutedVar value = sarray.getFromCacheForIndex(i);
            if (value == null) {
                // We skip nulls
                continue;
            }
            ArrayAccessConstraint ac = new ArrayAccessConstraint(sarray.getId(), i, value, ArrayAccessConstraint.Type.SELECT);
            initialConstraints[constraintNumber] = ac;
            constraintNumber++;
        }
        return initialConstraints;
    }

    private static void checkIfSarrayCanPotentiallyContainUnsetNonSymbolicDefaultAtInitialization(Sarray sarray) {
        if (sarray instanceof Sarray.SarraySarray
                && sarray.isKnownToHaveEveryIndexInitialized()
                && StreamSupport.stream(
                        sarray.getCachedElements().spliterator(), false)
                            .allMatch(s -> s != null && ((Sarray) s).isNull() == Sbool.ConcSbool.FALSE)) {
            sarray.setCannotCurrentlyContainNonSymbolicDefault();
        }
    }

    private static void representArrayIfNeeded(
            SymbolicExecution se,
            Sarray sarray,
            // null if sarray does not belong to a SarraySarray that is to be represented:
            Sint idOfContainingSarraySarray) {
        assert sarray.shouldBeRepresentedInSolver() && !sarray.isRepresentedInSolver();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        Sint sarrayLength = sarray.getLength();
        if (sarrayLength instanceof ConcSnumber
                && ((ConcSnumber) sarrayLength).intVal() == cachedIndices.size()) {
            // This is possible due to the assumption that we at this point only have concrete indices
            sarray.setIsKnownToHaveEveryIndexInitialized();
        }

        if (sarray instanceof Sarray.SarraySarray) {
            Sarray.SarraySarray ss = (Sarray.SarraySarray) sarray;
            for (Sarray entry : ss.getCachedElements()) {
                if (entry == null) {
                    continue;
                }
                if (!entry.isRepresentedInSolver()) {
                    assert !entry.shouldBeRepresentedInSolver();
                    entry.prepareToRepresentSymbolically(se);
                    representArrayIfNeeded(se, entry, ss.getId());
                }
            }
        }

        ArrayConstraint arrayInitializationConstraint;
        // Represent array in constraint solver, if needed
        if (idOfContainingSarraySarray == null || sarray.getId() instanceof ConcSnumber) {
            // In this case the Sarray was not spawned from a select from a SarraySarray
            if (!sarray.isRepresentedInSolver()) {
                checkIfSarrayCanPotentiallyContainUnsetNonSymbolicDefaultAtInitialization(sarray);
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            sarray.getId(),
                            sarrayLength,
                            sarray.isNull(),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.isKnownToHaveEveryIndexInitialized(),
                            sarray.canCurrentlyContainNonSymbolicDefault()
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        } else {
            // In this case we must initialize the Sarray with the possibility of aliasing the elements in the sarray
            Sint nextNumberInitializedSymSarray = se.concSint(se.getNextNumberInitializedSymSarray());
            if (!sarray.isRepresentedInSolver()) {
                checkIfSarrayCanPotentiallyContainUnsetNonSymbolicDefaultAtInitialization(sarray);
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            sarray.getId(),
                            sarrayLength,
                            sarray.isNull(),
                            // Id reserved for this Sarray, if needed
                            nextNumberInitializedSymSarray,
                            idOfContainingSarraySarray,
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraints(sarray, se),
                            sarray.isKnownToHaveEveryIndexInitialized(),
                            sarray.canCurrentlyContainNonSymbolicDefault()
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        }
    }

    private static void addSelectConstraintIfNeeded(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar result) {
        // We will now add a constraint indicating to the solver that at position i a value can be found that previously
        // was not there. This only occurs if the array must be represented via constraints. This, in turn, only
        // is the case if symbolic indices have been used.
        if (sarray.shouldBeRepresentedInSolver()) {
            if (result instanceof Sarray) {
                assert sarray instanceof Sarray.SarraySarray;
                representArrayIfNeeded(se, (Sarray) result, sarray.getId());
            }
            if (!se.nextIsOnKnownPath()) {
                ArrayConstraint selectConstraint =
                        new ArrayAccessConstraint(sarray.getId(), index, result, ArrayAccessConstraint.Type.SELECT);
                se.addNewArrayConstraint(selectConstraint);
            }
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
