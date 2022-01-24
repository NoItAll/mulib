package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import static de.wwu.mulib.constraints.ConcolicConstraintContainer.getConcSboolFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.getConcNumericFromConcolic;
import static de.wwu.mulib.constraints.ConcolicConstraintContainer.tryGetSymFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.tryGetSymFromConcolic;

public final class ConcolicCalculationFactory implements CalculationFactory {

    private final SymbolicCalculationFactory scf;
    
    ConcolicCalculationFactory(MulibConfig config) {
        this.scf = SymbolicCalculationFactory.getInstance(config);
    }

    public static ConcolicCalculationFactory getInstance(MulibConfig config) {
        return new ConcolicCalculationFactory(config);
    }

    @Override
    public Sint add(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.add(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSint(Math.addExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint sub(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.sub(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSint(Math.subtractExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint mul(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.mul(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSint(Math.multiplyExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint div(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.div(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSint(Math.floorDiv(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint mod(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.mod(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSint(Math.floorMod(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint neg(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sint potentiallySymbolic = scf.neg(se, vf, (Sint) tryGetSymFromConcolic(i));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        try {
            ConcSnumber iExpr = getConcNumericFromConcolic(i);
            ConcSnumber concResult = vf.concSint(Math.negateExact(iExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sdouble add(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.add(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() + rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble sub(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.sub(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() - rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mul(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.mul(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() * rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble div(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.div(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() / rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mod(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.mod(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSdouble(lhsExpr.doubleVal() % rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble neg(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        Sdouble potentiallySymbolic = scf.neg(se, vf, (Sdouble) tryGetSymFromConcolic(d));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber dExpr = getConcNumericFromConcolic(d);
        ConcSnumber concResult = vf.concSdouble(- dExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Slong add(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.add(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSlong(Math.addExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong sub(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.sub(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSlong(Math.subtractExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong mul(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.mul(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSlong(Math.multiplyExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong div(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.div(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSlong(Math.floorDiv(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong mod(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.mod(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = vf.concSlong(Math.floorMod(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong neg(SymbolicExecution se, ValueFactory vf, Slong l) {
        Slong potentiallySymbolic = scf.neg(se, vf, (Slong) tryGetSymFromConcolic(l));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lExpr = getConcNumericFromConcolic(l);
        try {
            ConcSnumber concResult = vf.concSlong(Math.negateExact(lExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sfloat add(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.add(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() + rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat sub(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.sub(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() - rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mul(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.mul(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() * rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat div(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.div(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() / rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mod(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.mod(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = vf.concSfloat(lhsExpr.floatVal() % rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat neg(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        Sfloat potentiallySymbolic = scf.neg(se, vf, (Sfloat) tryGetSymFromConcolic(f));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber fExpr = getConcNumericFromConcolic(f);
        ConcSnumber concResult = vf.concSfloat(- fExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sbool and(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.and(se, vf, tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.isTrue() && rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool or(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.or(se, vf, tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.isTrue() || rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool not(SymbolicExecution se, ValueFactory vf, Sbool b) {
        Sbool potentiallySymbolic = scf.not(se, vf, tryGetSymFromConcolic(b));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool bExpr = getConcSboolFromConcolic(b);
        Sbool.ConcSbool concResult = vf.concSbool(bExpr.isFalse());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.lt(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() < rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.lt(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() < rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.lt(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() < rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.lt(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() < rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.lte(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() <= rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.lte(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() <= rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.lte(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() <= rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.lte(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() <= rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.eq(se, vf, (Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.intVal() == rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.eq(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.longVal() == rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.eq(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.doubleVal() == rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.eq(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = vf.concSbool(lhsExpr.floatVal() == rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs) {
        Sint sym = scf.cmp(se, vf, (Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Long.compare(lhsExpr.longVal(), rhsExpr.longVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs) {
        Sint sym = scf.cmp(se, vf, (Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Double.compare(lhsExpr.doubleVal(), rhsExpr.doubleVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs) {
        Sint sym = scf.cmp(se, vf, (Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Float.compare(lhsExpr.floatVal(), rhsExpr.floatVal());
        ConcSnumber conc = vf.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Slong i2l(SymbolicExecution se, ValueFactory vf, Sint i) {
        Slong sym = scf.i2l(se, vf, (Sint) tryGetSymFromConcolic(i));
        return toSlong(vf, i, sym);
    }

    @Override
    public Sfloat i2f(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sfloat sym = scf.i2f(se, vf, (Sint) tryGetSymFromConcolic(i));
        return toSfloat(vf, i, sym);
    }

    @Override
    public Sdouble i2d(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sdouble sym = scf.i2d(se, vf, (Sint) tryGetSymFromConcolic(i));
        return toSdouble(vf, i, sym);
    }

    @Override
    public Sint l2i(SymbolicExecution se, ValueFactory vf, Slong l) {
        Sint sym = scf.l2i(se, vf, (Slong) tryGetSymFromConcolic(l));
        return toSint(vf, l, sym);
    }

    @Override
    public Sfloat l2f(SymbolicExecution se, ValueFactory vf, Slong l) {
        Sfloat sym = scf.l2f(se, vf, (Slong) tryGetSymFromConcolic(l));
        return toSfloat(vf, l, sym);
    }

    @Override
    public Sdouble l2d(SymbolicExecution se, ValueFactory vf, Slong l) {
        Sdouble sym = scf.l2d(se, vf, (Slong) tryGetSymFromConcolic(l));
        return toSdouble(vf, l, sym);
    }

    @Override
    public Sint f2i(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        Sint sym = scf.f2i(se, vf, (Sfloat) tryGetSymFromConcolic(f));
        return toSint(vf, f, sym);
    }

    @Override
    public Slong f2l(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        Slong sym = scf.f2l(se, vf, (Sfloat) tryGetSymFromConcolic(f));
        return toSlong(vf, f, sym);
    }

    @Override
    public Sdouble f2d(SymbolicExecution se, ValueFactory vf, Sfloat f) {
        Sdouble sym = scf.f2d(se, vf, (Sfloat) tryGetSymFromConcolic(f));
        return toSdouble(vf, f, sym);
    }

    @Override
    public Sint d2i(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        Sint sym = scf.d2i(se, vf, (Sdouble) tryGetSymFromConcolic(d));
        return toSint(vf, d, sym);
    }

    @Override
    public Slong d2l(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        Slong sym = scf.d2l(se, vf, (Sdouble) tryGetSymFromConcolic(d));
        return toSlong(vf, d, sym);
    }

    @Override
    public Sfloat d2f(SymbolicExecution se, ValueFactory vf, Sdouble d) {
        Sfloat sym = scf.d2f(se, vf, (Sdouble) tryGetSymFromConcolic(d));
        return toSfloat(vf, d, sym);
    }

    @Override
    public Sbyte i2b(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sbyte sym = scf.i2b(se, vf, (Sint) tryGetSymFromConcolic(i));
        if (sym instanceof Sbyte.ConcSbyte) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(i);
        Sbyte.ConcSbyte conc = vf.concSbyte(iconc.byteVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sbyte.newExpressionSymbolicSbyte(container);
    }

    @Override
    public Sshort i2s(SymbolicExecution se, ValueFactory vf, Sint i) {
        Sshort sym = scf.i2s(se, vf, (Sint) tryGetSymFromConcolic(i));
        if (sym instanceof Sshort.ConcSshort) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(i);
        Sshort.ConcSshort conc = vf.concSshort(iconc.shortVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sshort.newExpressionSymbolicSshort(container);
    }

    @Override
    public SubstitutedVar select(SymbolicExecution se, ValueFactory vf, Sarray sarray, Sint index) {
        throw new NotYetImplementedException(); //// TODO
    }

    @Override
    public SubstitutedVar store(SymbolicExecution se, ValueFactory vf, Sarray sarray, Sint index, SubstitutedVar value) {
        throw new NotYetImplementedException(); //// TODO
    }

    private Slong toSlong(ValueFactory vf, Snumber original, Slong sym) {
        if (sym instanceof Slong.ConcSlong) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Slong.ConcSlong conc = vf.concSlong(iconc.longVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Slong.newExpressionSymbolicSlong(container);
    }

    private Sdouble toSdouble(ValueFactory vf, Snumber original, Sdouble sym) {
        if (sym instanceof Sdouble.ConcSdouble) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sdouble.ConcSdouble conc = vf.concSdouble(iconc.doubleVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    private Sfloat toSfloat(ValueFactory vf, Snumber original, Sfloat sym) {
        if (sym instanceof Sfloat.ConcSfloat) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sfloat.ConcSfloat conc = vf.concSfloat(iconc.floatVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    private Sint toSint(ValueFactory vf, Snumber original, Sint sym) {
        if (sym instanceof Sint.ConcSint) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sint.ConcSint conc = vf.concSint(iconc.intVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }
}
