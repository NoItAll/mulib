package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.primitives.*;

public final class ConcolicCalculationFactory extends SymbolicCalculationFactory {

    ConcolicCalculationFactory(MulibConfig config) {
        super(config);
    }

    public static ConcolicCalculationFactory getInstance(MulibConfig config) {
        return new ConcolicCalculationFactory(config);
    }

    @Override
    public Sint add(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = super.add(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSint(lhsExpr.intVal() + rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint sub(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = super.sub(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSint(lhsExpr.intVal() - rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint mul(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = super.mul(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSint(lhsExpr.intVal() * rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint div(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = super.div(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSint(lhsExpr.intVal() / rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint mod(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = super.mod(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSint(lhsExpr.intVal() % rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint neg(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sint potentiallySymbolic = super.neg(se, vf, i);
        if (potentiallySymbolic instanceof Sint.ConcSint) {
            return potentiallySymbolic;
        }
        ConcSnumber iExpr = getConcNumericFromConcolic(i);
        ConcSnumber concResult = vf.concSint(-iExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sdouble add(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = super.add(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() + rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble sub(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = super.sub(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() - rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mul(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = super.mul(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() * rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble div(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = super.div(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() / rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mod(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = super.mod(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() % rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble neg(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        Sdouble potentiallySymbolic = super.neg(se, vf, d);
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber dExpr = getConcNumericFromConcolic(d);
        ConcSnumber concResult = vf.concSdouble(- dExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Slong add(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = super.add(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSlong(lhsExpr.longVal() + rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong sub(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = super.sub(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSlong(lhsExpr.longVal() - rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong mul(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = super.mul(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSlong(lhsExpr.longVal() * rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong div(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = super.div(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSlong(lhsExpr.longVal() / rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong mod(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = super.mod(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSlong(lhsExpr.longVal() % rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong neg(SymbolicExecution se, ValueFactory vf, Slong l) {
        Slong potentiallySymbolic = super.neg(se, vf, l);
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lExpr = getConcNumericFromConcolic(l);
        ConcSnumber concResult = vf.concSlong(- lExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Sfloat add(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = super.add(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() + rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat sub(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = super.sub(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() - rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mul(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = super.mul(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() * rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat div(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = super.div(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() / rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mod(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = super.mod(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() % rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat neg(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        Sfloat potentiallySymbolic = super.neg(se, vf, f);
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber fExpr = getConcNumericFromConcolic(f);
        ConcSnumber concResult = vf.concSfloat(- fExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer(potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sbool and(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = super.and(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.isTrue() && rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool or(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = super.and(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.isTrue() || rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool not(SymbolicExecution se, ValueFactory vf, Sbool b) {
        Sbool potentiallySymbolic = super.not(se, vf, b);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool bExpr = getConcSboolFromConcolic(b);
        Sbool.ConcSbool concResult = vf.concSbool(bExpr.isFalse());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = super.lt(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() < rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = super.lt(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() < rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = super.lt(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() < rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = super.lt(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() < rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = super.lte(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() <= rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = super.lte(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() <= rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = super.lte(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() <= rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = super.lte(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() <= rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = super.eq(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() == rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = super.eq(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() == rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = super.eq(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() == rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = super.eq(se, vf, lhs, rhs);
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() == rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sint sym = super.cmp(se, vf, lhs, rhs);
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Long.compare(lhsExpr.longVal(), rhsExpr.longVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer(sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sint sym = super.cmp(se, vf, lhs, rhs);
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Double.compare(lhsExpr.doubleVal(), rhsExpr.doubleVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer(sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sint sym = super.cmp(se, vf, lhs, rhs);
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Float.compare(lhsExpr.floatVal(), rhsExpr.floatVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer(sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    private static NumericExpression tryGetRepresentedNumericExpression(NumericExpression ne) {
        return ne instanceof SymNumericExpressionSprimitive ? ((SymNumericExpressionSprimitive) ne).getRepresentedExpression() : ne;
    }

    private static Constraint tryGetRepresentedConstraint(Constraint c) {
        return c instanceof Sbool.SymSbool ? ((Sbool.SymSbool) c).getRepresentedConstraint() : c;
    }

    private static NumericExpression tryGetSymNumericFromConcolic(NumericExpression ne) {
        NumericExpression representedExpression = tryGetRepresentedNumericExpression(ne);
        return representedExpression instanceof ConcolicNumericContainer ?
                ((ConcolicNumericContainer) representedExpression).getSym()
                :
                representedExpression;
    }

    private static Constraint tryGetSymConstraintFromConcolic(Constraint c) {
        Constraint representedConstraint = tryGetRepresentedConstraint(c);
        return representedConstraint instanceof ConcolicConstraintContainer ?
                ((ConcolicConstraintContainer) representedConstraint).getSym()
                :
                representedConstraint;
    }

    private static ConcSnumber getConcNumericFromConcolic(NumericExpression ne) {
        NumericExpression representedExpression = tryGetRepresentedNumericExpression(ne);
        return representedExpression instanceof ConcolicNumericContainer ?
                ((ConcolicNumericContainer) representedExpression).getConc()
                :
                (ConcSnumber) representedExpression;
    }

    private static Sbool.ConcSbool getConcSboolFromConcolic(Constraint c) {
        Constraint representedConstraint = tryGetRepresentedConstraint(c);
        return representedConstraint instanceof ConcolicConstraintContainer ?
                ((ConcolicConstraintContainer) representedConstraint).getConc()
                :
                (Sbool.ConcSbool) representedConstraint;
    }

    /* Overridden to get actual expression/constraint from concolic container */

    @Override
    protected NumericExpression sum(NumericExpression lhs, NumericExpression rhs) {
        return Sum.newInstance(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected NumericExpression sub(NumericExpression lhs, NumericExpression rhs) {
        return Sub.newInstance(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected NumericExpression mul(NumericExpression lhs, NumericExpression rhs) {
        return Mul.newInstance(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected NumericExpression div(NumericExpression lhs, NumericExpression rhs) {
        return Div.newInstance(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected NumericExpression mod(NumericExpression lhs, NumericExpression rhs) {
        return Mod.newInstance(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected NumericExpression neg(NumericExpression n) {
        return Neg.neg(tryGetSymNumericFromConcolic(n));
    }

    @Override
    protected Constraint and(Constraint lhs, Constraint rhs) {
        return new And(tryGetSymConstraintFromConcolic(lhs), tryGetSymConstraintFromConcolic(rhs));
    }

    @Override
    protected Constraint or(Constraint lhs, Constraint rhs) {
        return new Or(tryGetSymConstraintFromConcolic(lhs), tryGetSymConstraintFromConcolic(rhs));
    }

    @Override
    protected Constraint not(Constraint b) {
        return new Not(tryGetSymConstraintFromConcolic(b));
    }

    @Override
    protected Constraint lt(NumericExpression lhs, NumericExpression rhs) {
        return new Lt(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected Constraint lte(NumericExpression lhs, NumericExpression rhs) {
        return new Lte(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }

    @Override
    protected Constraint eq(NumericExpression lhs, NumericExpression rhs) {
        return new Eq(tryGetSymNumericFromConcolic(lhs), tryGetSymNumericFromConcolic(rhs));
    }
}
