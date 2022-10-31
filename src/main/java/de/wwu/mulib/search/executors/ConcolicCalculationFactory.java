package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.And;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static de.wwu.mulib.constraints.ConcolicConstraintContainer.getConcSboolFromConcolic;
import static de.wwu.mulib.constraints.ConcolicConstraintContainer.tryGetSymFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.getConcNumericFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.tryGetSymFromConcolic;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ConcolicCalculationFactory extends AbstractCalculationFactory {

    private final SymbolicCalculationFactory scf;

    ConcolicCalculationFactory(MulibConfig config, ValueFactory vf) {
        super(
                config,
                vf,
                ConcolicNumericContainer::tryGetSymFromConcolic,
                ConcolicConstraintContainer::tryGetSymFromConcolic
        );
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

    private static Constraint getEqOfConcolic(SubstitutedVar e, SymbolicExecution se) {
        Sbool eq;
        if (e instanceof Sarray) {
            e = ((Sarray<?>) e).__mulib__getId();
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

    @Override
    protected void additionalChecksAfterSelect(
            Sarray s,
            SymbolicExecution se) {
        // TODO Possibly prune the amount of constraints via the given index?
        // Evaluate relabeling
        if (se.nextIsOnKnownPath() || !s.__mulib__shouldBeRepresentedInSolver() || se.getCurrentChoiceOption().reevaluationNeeded()) {
            return;
        }
        assert StreamSupport.stream(s.getCachedElements().spliterator(), false).allMatch(e -> e instanceof Snumber || e instanceof Sarray || e == null) : "Failed with: " + s.getCachedElements(); // Also holds for Sbool
        Iterable<SubstitutedVar> symbolicValues = (Iterable<SubstitutedVar>) s.getCachedElements();
        List<Constraint> currentConcolicMapping = new ArrayList<>();
        for (SubstitutedVar e : symbolicValues) {
            if (e == null) {
                continue;
            }
            currentConcolicMapping.add(tryGetSymFromConcolic((Sbool) getEqOfConcolic(e, se)));
        }
        Constraint currentAssignment = And.newInstance(currentConcolicMapping);
        if (!se.checkWithNewConstraint(currentAssignment)) {
            markForReevaluation(se);
        }
    }

    private static void markForReevaluation(SymbolicExecution se) {
        if (!se.getCurrentChoiceOption().reevaluationNeeded()) {
            se.getCurrentChoiceOption().setReevaluationNeeded();
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

    @Override
    protected SubstitutedVar getValueToBeRepresentedInSarray(SubstitutedVar value) {
        if (value instanceof Sbool.SymSbool) {
            return tryGetSymFromConcolic((Sbool.SymSbool) value);
        } else if (value instanceof SymNumericExpressionSprimitive) {
            return tryGetSymFromConcolic((SymNumericExpressionSprimitive) value);
        } else if (value instanceof PartnerClass) {
            return tryGetSymFromConcolic(((PartnerClass) value).__mulib__getId());
        }
        return value;
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