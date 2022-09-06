package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import static de.wwu.mulib.constraints.ConcolicConstraintContainer.getConcSboolFromConcolic;
import static de.wwu.mulib.constraints.ConcolicConstraintContainer.tryGetSymFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.getConcNumericFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.tryGetSymFromConcolic;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ConcolicCalculationFactory extends AbstractCalculationFactory {

    private final SymbolicCalculationFactory scf;

    ConcolicCalculationFactory(MulibConfig config, ValueFactory vf) {
        super(config, vf);
        this.scf = SymbolicCalculationFactory.getInstance(config, vf);
    }

    public static ConcolicCalculationFactory getInstance(MulibConfig config, ValueFactory vf) {
        return new ConcolicCalculationFactory(config, vf);
    }

    @Override
    public Sint add(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.add(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSint(Math.addExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint sub(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.sub(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSint(Math.subtractExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint mul(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.mul(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSint(Math.multiplyExact(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint div(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.div(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSint(Math.floorDiv(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint mod(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sint potentiallySymbolic = scf.mod(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSint(Math.floorMod(lhsExpr.intVal(), rhsExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sint neg(SymbolicExecution se, Sint i) {
        Sint potentiallySymbolic = scf.neg(se,(Sint) tryGetSymFromConcolic(i));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        try {
            ConcSnumber iExpr = getConcNumericFromConcolic(i);
            ConcSnumber concResult = valueFactory.concSint(Math.negateExact(iExpr.intVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Sint.newExpressionSymbolicSint(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sdouble add(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.add(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSdouble(lhsExpr.doubleVal() + rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble sub(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.sub(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSdouble(lhsExpr.doubleVal() - rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mul(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.mul(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSdouble(lhsExpr.doubleVal() * rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble div(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.div(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSdouble(lhsExpr.doubleVal() / rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble mod(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sdouble potentiallySymbolic = scf.mod(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSdouble(lhsExpr.doubleVal() % rhsExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Sdouble neg(SymbolicExecution se, Sdouble d) {
        Sdouble potentiallySymbolic = scf.neg(se,(Sdouble) tryGetSymFromConcolic(d));
        if (potentiallySymbolic instanceof Sdouble.ConcSdouble) {
            return potentiallySymbolic;
        }
        ConcSnumber dExpr = getConcNumericFromConcolic(d);
        ConcSnumber concResult = valueFactory.concSdouble(- dExpr.doubleVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    @Override
    public Slong add(SymbolicExecution se, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.add(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.addExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong sub(SymbolicExecution se, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.sub(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.subtractExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong mul(SymbolicExecution se, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.mul(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.multiplyExact(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong div(SymbolicExecution se, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.div(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.floorDiv(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong mod(SymbolicExecution se, Slong lhs, Slong rhs) {
        Slong potentiallySymbolic = scf.mod(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.floorMod(lhsExpr.longVal(), rhsExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Slong neg(SymbolicExecution se, Slong l) {
        Slong potentiallySymbolic = scf.neg(se,(Slong) tryGetSymFromConcolic(l));
        if (potentiallySymbolic instanceof Slong.ConcSlong) {
            return potentiallySymbolic;
        }
        ConcSnumber lExpr = getConcNumericFromConcolic(l);
        try {
            ConcSnumber concResult = valueFactory.concSlong(Math.negateExact(lExpr.longVal()));
            ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
            return Slong.newExpressionSymbolicSlong(container);
        } catch (ArithmeticException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Sfloat add(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.add(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSfloat(lhsExpr.floatVal() + rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat sub(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.sub(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSfloat(lhsExpr.floatVal() - rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mul(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.mul(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSfloat(lhsExpr.floatVal() * rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat div(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.div(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSfloat(lhsExpr.floatVal() / rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat mod(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sfloat potentiallySymbolic = scf.mod(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        ConcSnumber concResult = valueFactory.concSfloat(lhsExpr.floatVal() % rhsExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sfloat neg(SymbolicExecution se, Sfloat f) {
        Sfloat potentiallySymbolic = scf.neg(se,(Sfloat) tryGetSymFromConcolic(f));
        if (potentiallySymbolic instanceof Sfloat.ConcSfloat) {
            return potentiallySymbolic;
        }
        ConcSnumber fExpr = getConcNumericFromConcolic(f);
        ConcSnumber concResult = valueFactory.concSfloat(- fExpr.floatVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    @Override
    public Sbool and(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.and(se,tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.isTrue() && rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool or(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.or(se,tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.isTrue() || rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool not(SymbolicExecution se, Sbool b) {
        Sbool potentiallySymbolic = scf.not(se,tryGetSymFromConcolic(b));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool bExpr = getConcSboolFromConcolic(b);
        Sbool.ConcSbool concResult = valueFactory.concSbool(bExpr.isFalse());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.lt(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.intVal() < rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.lt(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.longVal() < rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.lt(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.doubleVal() < rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lt(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.lt(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.floatVal() < rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.lte(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.intVal() <= rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.lte(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.longVal() <= rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.lte(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.doubleVal() <= rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool lte(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.lte(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.floatVal() <= rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sint lhs, Sint rhs) {
        Sbool potentiallySymbolic = scf.eq(se,(Sint) tryGetSymFromConcolic(lhs), (Sint) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.intVal() == rhsExpr.intVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Slong lhs, Slong rhs) {
        Sbool potentiallySymbolic = scf.eq(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.longVal() == rhsExpr.longVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sbool potentiallySymbolic = scf.eq(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.doubleVal() == rhsExpr.doubleVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool eq(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sbool potentiallySymbolic = scf.eq(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        Sbool.ConcSbool concResult = valueFactory.concSbool(lhsExpr.floatVal() == rhsExpr.floatVal());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Slong lhs, Slong rhs) {
        Sint sym = scf.cmp(se,(Slong) tryGetSymFromConcolic(lhs), (Slong) tryGetSymFromConcolic(rhs));
        if (sym instanceof ConcSnumber) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Long.compare(lhsExpr.longVal(), rhsExpr.longVal());
        ConcSnumber conc = valueFactory.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        Sint sym = scf.cmp(se,(Sdouble) tryGetSymFromConcolic(lhs), (Sdouble) tryGetSymFromConcolic(rhs));
        if (sym instanceof ConcSnumber) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Double.compare(lhsExpr.doubleVal(), rhsExpr.doubleVal());
        ConcSnumber conc = valueFactory.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint cmp(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        Sint sym = scf.cmp(se,(Sfloat) tryGetSymFromConcolic(lhs), (Sfloat) tryGetSymFromConcolic(rhs));
        if (sym instanceof ConcSnumber) {
            return sym;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(lhs);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(rhs);
        int concComparison = Float.compare(lhsExpr.floatVal(), rhsExpr.floatVal());
        ConcSnumber conc = valueFactory.concSint(concComparison);
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Slong i2l(SymbolicExecution se, Sint i) {
        Slong sym = scf.i2l(se,(Sint) tryGetSymFromConcolic(i));
        return toSlong(i, sym);
    }

    @Override
    public Sfloat i2f(SymbolicExecution se, Sint i) {
        Sfloat sym = scf.i2f(se,(Sint) tryGetSymFromConcolic(i));
        return toSfloat(i, sym);
    }

    @Override
    public Sdouble i2d(SymbolicExecution se, Sint i) {
        Sdouble sym = scf.i2d(se,(Sint) tryGetSymFromConcolic(i));
        return toSdouble(i, sym);
    }

    @Override
    public Sint l2i(SymbolicExecution se, Slong l) {
        Sint sym = scf.l2i(se,(Slong) tryGetSymFromConcolic(l));
        return toSint(l, sym);
    }

    @Override
    public Sfloat l2f(SymbolicExecution se, Slong l) {
        Sfloat sym = scf.l2f(se,(Slong) tryGetSymFromConcolic(l));
        return toSfloat(l, sym);
    }

    @Override
    public Sdouble l2d(SymbolicExecution se, Slong l) {
        Sdouble sym = scf.l2d(se,(Slong) tryGetSymFromConcolic(l));
        return toSdouble(l, sym);
    }

    @Override
    public Sint f2i(SymbolicExecution se, Sfloat f) {
        Sint sym = scf.f2i(se,(Sfloat) tryGetSymFromConcolic(f));
        return toSint(f, sym);
    }

    @Override
    public Slong f2l(SymbolicExecution se, Sfloat f) {
        Slong sym = scf.f2l(se,(Sfloat) tryGetSymFromConcolic(f));
        return toSlong(f, sym);
    }

    @Override
    public Sdouble f2d(SymbolicExecution se, Sfloat f) {
        Sdouble sym = scf.f2d(se,(Sfloat) tryGetSymFromConcolic(f));
        return toSdouble(f, sym);
    }

    @Override
    public Sint d2i(SymbolicExecution se, Sdouble d) {
        Sint sym = scf.d2i(se,(Sdouble) tryGetSymFromConcolic(d));
        return toSint(d, sym);
    }

    @Override
    public Slong d2l(SymbolicExecution se, Sdouble d) {
        Slong sym = scf.d2l(se,(Sdouble) tryGetSymFromConcolic(d));
        return toSlong(d, sym);
    }

    @Override
    public Sfloat d2f(SymbolicExecution se, Sdouble d) {
        Sfloat sym = scf.d2f(se,(Sdouble) tryGetSymFromConcolic(d));
        return toSfloat(d, sym);
    }

    @Override
    public Sbyte i2b(SymbolicExecution se, Sint i) {
        Sbyte sym = scf.i2b(se,(Sint) tryGetSymFromConcolic(i));
        if (sym instanceof Sbyte.ConcSbyte) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(i);
        Sbyte.ConcSbyte conc = valueFactory.concSbyte(iconc.byteVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sbyte.newExpressionSymbolicSbyte(container);
    }

    @Override
    public Sshort i2s(SymbolicExecution se, Sint i) {
        Sshort sym = scf.i2s(se,(Sint) tryGetSymFromConcolic(i));
        if (sym instanceof Sshort.ConcSshort) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(i);
        Sshort.ConcSshort conc = valueFactory.concSshort(iconc.shortVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sshort.newExpressionSymbolicSshort(container);
    }

    @Override
    protected SubstitutedVar _selectWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index) {
        SubstitutedVar result = sarray.getFromCacheForIndex(index);
        if (result != null) {
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
        evaluateRelabeling(sarray, se); // TODO Possibly prune the amount of constraints via the given index?
        return result;
    }

    private static Constraint getEqOfConcolic(SubstitutedVar e, SymbolicExecution se) {
        Sbool eq;
        if (e instanceof Sarray) {
            e = ((Sarray<?>) e).getId();
        }

        if (e instanceof Sbool) {
            Sbool.ConcSbool conc = getConcSboolFromConcolic((Sbool) e);
            eq = conc.isEqualTo((Sbool) e, se);
        } else if (e instanceof Snumber) {
            ConcSnumber conc = getConcNumericFromConcolic((NumericExpression) e);
            if (e instanceof Sint) {
                eq = se.eq((Sint) e, (Sint) conc);
            } else if (e instanceof Sdouble) {
                eq = se.eq((Sdouble) e, (Sdouble) conc);
            } else if (e instanceof Sfloat) {
                eq = se.eq((Sfloat) e, (Sfloat) conc);
            } else if (e instanceof Slong) {
                eq = se.eq((Slong) e, (Slong) conc);
            } else {
                throw new NotYetImplementedException();
            }
        } else {
            throw new NotYetImplementedException();
        }
        return tryGetSymFromConcolic(eq);
    }

    private static void evaluateRelabeling(
            Sarray<?> s,
            SymbolicExecution se) {
        if (se.nextIsOnKnownPath() || !s.shouldBeRepresentedInSolver() || se.getCurrentChoiceOption().reevaluationNeeded()) {
            return;
        }
        assert StreamSupport.stream(s.getCachedElements().spliterator(), false).allMatch(e -> e instanceof Snumber || e instanceof Sarray) : "Failed with: " + s.getCachedElements(); // Also holds for Sbool
        Iterable<SubstitutedVar> symbolicValues = (Iterable<SubstitutedVar>) s.getCachedElements();
        List<Constraint> currentConcolicMapping = new ArrayList<>();
        for (SubstitutedVar e : symbolicValues) {
            currentConcolicMapping.add(tryGetSymFromConcolic((Sbool) getEqOfConcolic(e, se)));
        }
        Constraint currentAssignment = And.newInstance(currentConcolicMapping);
        if (!se.checkWithNewConstraint(currentAssignment)) {
            markForReevaluation(se);
        }
    }

    @Override
    protected SubstitutedVar _storeWithSymbolicIndexes(SymbolicExecution se, Sarray sarray, Sint index, SubstitutedVar value) {
        representArrayViaConstraintsIfNeeded(se, sarray, index);
        checkIndexAccess(sarray, index, se);
        Sarray.checkIfValueIsStorableForSarray(sarray, value);

        if (sarray.shouldBeRepresentedInSolver()) {
            sarray.clearCachedElements();
            if (!se.nextIsOnKnownPath()) {
                SubstitutedVar inner;
                if (value instanceof Sbool) {
                    inner = ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool) value);
                } else if (value instanceof Snumber) {
                    inner = ConcolicNumericContainer.tryGetSymFromConcolic((Snumber) value);
                } else {
                    throw new NotYetImplementedException();
                }
                ArrayConstraint storeConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromConcolic(sarray.getId()),
                                (Sint) tryGetSymFromConcolic(index),
                                inner,
                                ArrayAccessConstraint.Type.STORE
                        );
                se.addNewArrayConstraint(storeConstraint);
            }
        }
        sarray.setInCacheForIndexForStore(index, value);
        return value;
    }

    private static void markForReevaluation(SymbolicExecution se) {
        if (!se.getCurrentChoiceOption().reevaluationNeeded()) {
            se.getCurrentChoiceOption().setReevaluationNeeded();
        }
    }

    private static void representArrayViaConstraintsIfNeeded(SymbolicExecution se, Sarray sarray, Sint newIndex) {
        if (sarray.checkIfNeedsToRepresentOldEntries(newIndex, se)) {
            representArrayIfNeeded(se, sarray, null);
        }
    }

    private static void representArrayIfNeeded(SymbolicExecution se, Sarray sarray, Sint idOfContainingSarraySarray) {
        assert sarray.shouldBeRepresentedInSolver() && !sarray.isRepresentedInSolver();
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        if (sarray instanceof Sarray.SarraySarray) {
            Sarray.SarraySarray ss = (Sarray.SarraySarray) sarray;
            for (Sarray entry : ss.getCachedElements()) {
                if (!entry.isRepresentedInSolver()) {
                    assert !entry.shouldBeRepresentedInSolver();
                    entry.prepareToRepresentOldEntries(se);
                    representArrayIfNeeded(se, entry, (Sint) tryGetSymFromConcolic(ss.getId()));
                }
            }
        }

        ArrayConstraint arrayInitializationConstraint;
        if (idOfContainingSarraySarray == null || sarray.getId() instanceof ConcSnumber) {
            if (!sarray.isRepresentedInSolver()) {
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromConcolic(sarray.getId()),
                            (Sint) tryGetSymFromConcolic(sarray.getLength()),
                            tryGetSymFromConcolic(sarray.isNull()),
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraintsAndPotentiallyInitializeSarrays(sarray, se)
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        } else {
            Sint nextNumberInitializedSymSarray = se.concSint(se.getNextNumberInitializedSymSarray());
            if (!sarray.isRepresentedInSolver()) {
                if (!se.nextIsOnKnownPath()) {
                    arrayInitializationConstraint = new ArrayInitializationConstraint(
                            (Sint) tryGetSymFromConcolic(sarray.getId()),
                            (Sint) tryGetSymFromConcolic(sarray.getLength()),
                            tryGetSymFromConcolic(sarray.isNull()),
                            // Id reserved for this Sarray, if needed
                            nextNumberInitializedSymSarray,
                            idOfContainingSarraySarray,
                            sarray.getElementType(),
                            collectInitialArrayAccessConstraintsAndPotentiallyInitializeSarrays(sarray, se)
                    );
                    se.addNewArrayConstraint(arrayInitializationConstraint);
                }
                sarray.setAsRepresentedInSolver();
            }
        }
    }

    private static ArrayAccessConstraint[] collectInitialArrayAccessConstraintsAndPotentiallyInitializeSarrays(Sarray sarray, SymbolicExecution se) {
        Set<Sint> cachedIndices = sarray.getCachedIndices();
        assert cachedIndices.stream().noneMatch(i -> i instanceof Sym) : "The Sarray should have already been represented in the constraint system";

        ArrayAccessConstraint[] initialConstraints = new ArrayAccessConstraint[cachedIndices.size()];
        if (!se.nextIsOnKnownPath()) {
            int constraintNumber = 0;
            for (Sint i : cachedIndices) {
                SubstitutedVar value = sarray.getFromCacheForIndex(i);
                Sprimitive val;
                if (value instanceof Sarray) {
                    val = tryGetSymFromConcolic(((Sarray<?>) value).getId());
                } else if (value instanceof Sbool) {
                    val = tryGetSymFromConcolic((Sbool) value);
                } else if (value instanceof Snumber) {
                    val = tryGetSymFromConcolic((Snumber) value);
                } else {
                    throw new NotYetImplementedException();
                }

                ArrayAccessConstraint ac = new ArrayAccessConstraint(
                        (Sint) tryGetSymFromConcolic(sarray.getId()),
                        (Sint) tryGetSymFromConcolic(i),
                        val,
                        ArrayAccessConstraint.Type.SELECT
                );
                initialConstraints[constraintNumber] = ac;
                constraintNumber++;
            }
        }
        return initialConstraints;
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
                if (result instanceof Sbool.SymSbool) {
                    result = ConcolicConstraintContainer.tryGetSymFromConcolic((Sbool.SymSbool) result);
                } else if (result instanceof SymNumericExpressionSprimitive) {
                    result = ConcolicNumericContainer.tryGetSymFromConcolic((SymNumericExpressionSprimitive) result);
                } else if (result instanceof Sarray) {
                    result = tryGetSymFromConcolic(((Sarray<?>) result).getId());
                }
                ArrayConstraint selectConstraint =
                        new ArrayAccessConstraint(
                                (Sint) tryGetSymFromConcolic(sarray.getId()),
                                (Sint) ConcolicNumericContainer.tryGetSymFromConcolic(index),
                                result,
                                ArrayAccessConstraint.Type.SELECT
                        );
                se.addNewArrayConstraint(selectConstraint);
            }
        }
    }

    @Override
    protected void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds) {
        // If we do not regard out-of-bound array index-accesses, we simply add a new constraint and proceed.
        // next choice option or once reaching the end of the execution. Find an approach with minimal overhead
        // here.
        Sbool actualConstraint = ConcolicConstraintContainer.tryGetSymFromConcolic(indexInBounds);
        se.addNewConstraint(actualConstraint);
        Sbool.ConcSbool isLabeledIndexInBounds = ConcolicConstraintContainer.getConcSboolFromConcolic(indexInBounds);
        if (isLabeledIndexInBounds.isFalse()) {
            markForReevaluation(se);
        }
    }

    private Slong toSlong(Snumber original, Slong sym) {
        if (sym instanceof Slong.ConcSlong) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Slong.ConcSlong conc = valueFactory.concSlong(iconc.longVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Slong.newExpressionSymbolicSlong(container);
    }

    private Sdouble toSdouble(Snumber original, Sdouble sym) {
        if (sym instanceof Sdouble.ConcSdouble) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sdouble.ConcSdouble conc = valueFactory.concSdouble(iconc.doubleVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    private Sfloat toSfloat(Snumber original, Sfloat sym) {
        if (sym instanceof Sfloat.ConcSfloat) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sfloat.ConcSfloat conc = valueFactory.concSfloat(iconc.floatVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    private Sint toSint(Snumber original, Sint sym) {
        if (sym instanceof ConcSnumber) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sint.ConcSint conc = valueFactory.concSint(iconc.intVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }
}