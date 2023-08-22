package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

import static de.wwu.mulib.constraints.ConcolicConstraintContainer.getConcSboolFromConcolic;
import static de.wwu.mulib.constraints.ConcolicConstraintContainer.tryGetSymFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.getConcNumericFromConcolic;
import static de.wwu.mulib.expressions.ConcolicNumericContainer.tryGetSymFromConcolic;

/**
 * Calculation factory implementing concolic execution. The returned elements either are an instance of {@link de.wwu.mulib.substitutions.Conc},
 * i.e., not carrying any symbolic information, or are wrappers carrying a {@link ConcolicNumericContainer} or a {@link ConcolicConstraintContainer}.
 * These two container types carry the symbolic value and, additionally, a label for the given execution run.
 */
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
    public Sbool implies(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.implies(se, tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool result = Sbool.concSbool(lhsExpr.isFalse() || rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, result);
        return Sbool.newConstraintSbool(container);
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
            ConcSnumber concResult = Sint.concSint(Math.addExact(lhsExpr.intVal(), rhsExpr.intVal()));
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
            ConcSnumber concResult = Sint.concSint(Math.subtractExact(lhsExpr.intVal(), rhsExpr.intVal()));
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
            ConcSnumber concResult = Sint.concSint(Math.multiplyExact(lhsExpr.intVal(), rhsExpr.intVal()));
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
            ConcSnumber concResult = Sint.concSint(Math.floorDiv(lhsExpr.intVal(), rhsExpr.intVal()));
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
            ConcSnumber concResult = Sint.concSint(Math.floorMod(lhsExpr.intVal(), rhsExpr.intVal()));
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
            ConcSnumber concResult = Sint.concSint(Math.negateExact(iExpr.intVal()));
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
        ConcSnumber concResult = Sdouble.concSdouble(lhsExpr.doubleVal() + rhsExpr.doubleVal());
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
        ConcSnumber concResult = Sdouble.concSdouble(lhsExpr.doubleVal() - rhsExpr.doubleVal());
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
        ConcSnumber concResult = Sdouble.concSdouble(lhsExpr.doubleVal() * rhsExpr.doubleVal());
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
        ConcSnumber concResult = Sdouble.concSdouble(lhsExpr.doubleVal() / rhsExpr.doubleVal());
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
        ConcSnumber concResult = Sdouble.concSdouble(lhsExpr.doubleVal() % rhsExpr.doubleVal());
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
        ConcSnumber concResult = Sdouble.concSdouble(- dExpr.doubleVal());
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
            ConcSnumber concResult = Slong.concSlong(Math.addExact(lhsExpr.longVal(), rhsExpr.longVal()));
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
            ConcSnumber concResult = Slong.concSlong(Math.subtractExact(lhsExpr.longVal(), rhsExpr.longVal()));
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
            ConcSnumber concResult = Slong.concSlong(Math.multiplyExact(lhsExpr.longVal(), rhsExpr.longVal()));
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
            ConcSnumber concResult = Slong.concSlong(Math.floorDiv(lhsExpr.longVal(), rhsExpr.longVal()));
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
            ConcSnumber concResult = Slong.concSlong(Math.floorMod(lhsExpr.longVal(), rhsExpr.longVal()));
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
            ConcSnumber concResult = Slong.concSlong(Math.negateExact(lExpr.longVal()));
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
        ConcSnumber concResult = Sfloat.concSfloat(lhsExpr.floatVal() + rhsExpr.floatVal());
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
        ConcSnumber concResult = Sfloat.concSfloat(lhsExpr.floatVal() - rhsExpr.floatVal());
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
        ConcSnumber concResult = Sfloat.concSfloat(lhsExpr.floatVal() * rhsExpr.floatVal());
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
        ConcSnumber concResult = Sfloat.concSfloat(lhsExpr.floatVal() / rhsExpr.floatVal());
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
        ConcSnumber concResult = Sfloat.concSfloat(lhsExpr.floatVal() % rhsExpr.floatVal());
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
        ConcSnumber concResult = Sfloat.concSfloat(- fExpr.floatVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.isTrue() && rhsExpr.isTrue());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.isTrue() || rhsExpr.isTrue());
        ConcolicConstraintContainer container = new ConcolicConstraintContainer((Sbool.SymSbool) potentiallySymbolic, concResult);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sbool xor(SymbolicExecution se, Sbool lhs, Sbool rhs) {
        Sbool potentiallySymbolic = scf.xor(se,tryGetSymFromConcolic(lhs), tryGetSymFromConcolic(rhs));
        if (potentiallySymbolic instanceof Sbool.ConcSbool) {
            return potentiallySymbolic;
        }
        Sbool.ConcSbool lhsExpr = getConcSboolFromConcolic(lhs);
        Sbool.ConcSbool rhsExpr = getConcSboolFromConcolic(rhs);
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.isTrue() ^ rhsExpr.isTrue());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(bExpr.isFalse());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.intVal() < rhsExpr.intVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.longVal() < rhsExpr.longVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.doubleVal() < rhsExpr.doubleVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.floatVal() < rhsExpr.floatVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.intVal() <= rhsExpr.intVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.longVal() <= rhsExpr.longVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.doubleVal() <= rhsExpr.doubleVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.floatVal() <= rhsExpr.floatVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.intVal() == rhsExpr.intVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.longVal() == rhsExpr.longVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.doubleVal() == rhsExpr.doubleVal());
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
        Sbool.ConcSbool concResult = Sbool.concSbool(lhsExpr.floatVal() == rhsExpr.floatVal());
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
        ConcSnumber conc = Sint.concSint(concComparison);
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
        ConcSnumber conc = Sint.concSint(concComparison);
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
        ConcSnumber conc = Sint.concSint(concComparison);
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
        Sbyte.ConcSbyte conc = Sbyte.concSbyte(iconc.byteVal());
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
        Sshort.ConcSshort conc = Sshort.concSshort(iconc.shortVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sshort.newExpressionSymbolicSshort(container);
    }

    @Override
    public Sint ishl(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.ishl(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() << rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint ishr(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.ishr(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() >> rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint ixor(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.ixor(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() ^ rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint ior(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.ior(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() | rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint iushr(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.iushr(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() >>> rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Sint iand(SymbolicExecution se, Sint i0, Sint i1) {
        Sint potentiallySymbolic = scf.iand(se,(Sint) tryGetSymFromConcolic(i0), (Sint) tryGetSymFromConcolic(i1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(i0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(i1);
        ConcSnumber concResult = Sint.concSint(lhsExpr.intVal() & rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Sint.newExpressionSymbolicSint(container);
    }

    @Override
    public Slong lshl(SymbolicExecution se, Slong l0, Sint l1) {
        Slong potentiallySymbolic = scf.lshl(se,(Slong) tryGetSymFromConcolic(l0), (Sint) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() << rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong lshr(SymbolicExecution se, Slong l0, Sint l1) {
        Slong potentiallySymbolic = scf.lshr(se,(Slong) tryGetSymFromConcolic(l0), (Sint) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() >> rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong lxor(SymbolicExecution se, Slong l0, Slong l1) {
        Slong potentiallySymbolic = scf.lxor(se,(Slong) tryGetSymFromConcolic(l0), (Slong) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() ^ rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong lor(SymbolicExecution se, Slong l0, Slong l1) {
        Slong potentiallySymbolic = scf.lor(se,(Slong) tryGetSymFromConcolic(l0), (Slong) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() | rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong lushr(SymbolicExecution se, Slong l0, Sint l1) {
        Slong potentiallySymbolic = scf.lushr(se,(Slong) tryGetSymFromConcolic(l0), (Sint) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() >>> rhsExpr.intVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Slong land(SymbolicExecution se, Slong l0, Slong l1) {
        Slong potentiallySymbolic = scf.land(se,(Slong) tryGetSymFromConcolic(l0), (Slong) tryGetSymFromConcolic(l1));
        if (potentiallySymbolic instanceof ConcSnumber) {
            return potentiallySymbolic;
        }
        ConcSnumber lhsExpr = getConcNumericFromConcolic(l0);
        ConcSnumber rhsExpr = getConcNumericFromConcolic(l1);
        ConcSnumber concResult = Slong.concSlong(lhsExpr.longVal() & rhsExpr.longVal());
        ConcolicNumericContainer container = new ConcolicNumericContainer((SymNumericExpressionSprimitive) potentiallySymbolic, concResult);
        return Slong.newExpressionSymbolicSlong(container);
    }

    @Override
    public Schar i2c(SymbolicExecution se, Sint i) {
        Schar sym = scf.i2c(se, (Sint) tryGetSymFromConcolic(i));
        if (sym instanceof Schar.ConcSchar) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(i);
        Schar.ConcSchar conc = Schar.concSchar(iconc.charVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Schar.newExpressionSymbolicSchar(container);
    }

    @Override
    protected void _addIndexInBoundsConstraint(SymbolicExecution se, Sbool indexInBounds) {
        // If we do not regard out-of-bound array index-accesses, we simply add a new constraint and proceed.
        // next choice option or once reaching the end of the execution. Find an approach with minimal overhead
        // here.
        Sbool actualConstraint = ConcolicConstraintContainer.tryGetSymFromConcolic(indexInBounds);
        se.addNewConstraint(actualConstraint);
    }

    @Override
    protected Sprimitive getValueToBeUsedForPartnerClassObjectConstraint(Substituted value) {
        if (value instanceof Sbool.SymSbool) {
            return tryGetSymFromConcolic((Sbool.SymSbool) value);
        } else if (value instanceof SymNumericExpressionSprimitive) {
            return tryGetSymFromConcolic((SymNumericExpressionSprimitive) value);
        } else if (value instanceof PartnerClass) {
            assert ((PartnerClass) value).__mulib__isRepresentedInSolver();
            return tryGetSymFromConcolic(((PartnerClass) value).__mulib__getId());
        }
        return (Sprimitive) value;
    }

    private Slong toSlong(Snumber original, Slong sym) {
        if (sym instanceof Slong.ConcSlong) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Slong.ConcSlong conc = Slong.concSlong(iconc.longVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Slong.newExpressionSymbolicSlong(container);
    }

    private Sdouble toSdouble(Snumber original, Sdouble sym) {
        if (sym instanceof Sdouble.ConcSdouble) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sdouble.ConcSdouble conc = Sdouble.concSdouble(iconc.doubleVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sdouble.newExpressionSymbolicSdouble(container);
    }

    private Sfloat toSfloat(Snumber original, Sfloat sym) {
        if (sym instanceof Sfloat.ConcSfloat) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sfloat.ConcSfloat conc = Sfloat.concSfloat(iconc.floatVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sfloat.newExpressionSymbolicSfloat(container);
    }

    private Sint toSint(Snumber original, Sint sym) {
        if (sym instanceof ConcSnumber) {
            return sym;
        }
        ConcSnumber iconc = getConcNumericFromConcolic(original);
        Sint.ConcSint conc = Sint.concSint(iconc.intVal());
        ConcolicNumericContainer container =
                new ConcolicNumericContainer((SymNumericExpressionSprimitive) sym, conc);
        return Sint.newExpressionSymbolicSint(container);
    }
}