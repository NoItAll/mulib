package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.Map;
import java.util.function.Function;

// The creation of concrete numbers is performed in SymbolicValueFactory.
public class ConcolicValueFactory extends AbstractValueFactory implements AssignConcolicLabelEnabledValueFactory {

    private final SymbolicValueFactory svf;
    
    protected ConcolicValueFactory(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        super(config, arrayTypesToSpecializedSarrayClass);
        this.svf = SymbolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
    }

    public static ConcolicValueFactory getInstance(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        return new ConcolicValueFactory(config, arrayTypesToSpecializedSarrayClass);
    }

    @Override
    protected void _addLengthLteZeroConstraint(SymbolicExecution se, Sint len) {
        Sbool inBounds = se.lte(Sint.ConcSint.ZERO, len);
        Constraint actualConstraint = ConcolicConstraintContainer.tryGetSymFromConcolic(inBounds);
        if (actualConstraint instanceof Sbool.SymSbool) {
            actualConstraint = ((Sbool.SymSbool) actualConstraint).getRepresentedConstraint();
        }
        se.addNewConstraint(actualConstraint);
    }

    private static <SA extends SymNumericExpressionSprimitive, S, N> S numericConcolicWrapperCreator(
            SymbolicExecution se,
            Function<SymbolicExecution, SA> symCreator,
            Function<Object, ConcSnumber> concSnumberCreator,
            Function<ConcolicNumericContainer, S> resultWrapper) {
        // Symbolic value
        SA sym = symCreator.apply(se);
        if (!se.isSatisfiable()) {
            throw Mulib.fail();
        }
        // Concrete value
        ConcSnumber conc = concSnumberCreator.apply(se.label(sym));
        // TODO Performance optimization: If nextIsOnKnownPath() is false, we can return the neutral element (e.g. 0 and
        //  false) or 1 to directly account for Sarray-based index-constraints.
        // Container for both
        ConcolicNumericContainer container = new ConcolicNumericContainer(sym, conc);
        return resultWrapper.apply(container);
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSint,
                o -> Sint.concSint((Integer) o),
                // Wrapped in new SymSint; we do not reuse these outer wrappers, thus, we do not call
                // svf.wrappingXYZ(...).
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSdouble,
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSfloat,
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        // Symbolic value
        Sbool.SymSbool sym = svf.symSbool(se);
        if (!se.isSatisfiable()) {
            throw Mulib.fail();
        }
        // Concrete value
        Sbool.ConcSbool conc = Sbool.concSbool((Boolean) se.label(sym));
        // Container for both
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(sym, conc);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSlong,
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSshort,
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSbyte,
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Schar.SymSchar symSchar(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSchar,
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }

    @Override
    public Sint symSint(SymbolicExecution se, Sint lb, Sint ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSint(s, (Sint) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Sint) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Sint.concSint((Integer) o),
                // Wrapped in new SymSint; we do not reuse these outer wrappers, thus, we do not call
                // svf.wrappingXYZ(...).
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Sdouble symSdouble(SymbolicExecution se, Sdouble lb, Sdouble ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSdouble(s, (Sdouble) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Sdouble) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSfloat(s, (Sfloat) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Sfloat) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Slong symSlong(SymbolicExecution se, Slong lb, Slong ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSlong(s, (Slong) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Slong) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSshort(s, (Sshort) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Sshort) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSbyte(s, (Sbyte) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Sbyte) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Schar symSchar(SymbolicExecution se, Schar lb, Schar ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSchar(s, (Schar) ConcolicNumericContainer.tryGetSymFromConcolic(lb), (Schar) ConcolicNumericContainer.tryGetSymFromConcolic(ub)),
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSint(se, numericExpression);
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSdouble(se, numericExpression);
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSfloat(se, numericExpression);
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSlong(se, numericExpression);
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSshort(se, numericExpression);
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                    && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSbyte(se, numericExpression);
    }

    @Override
    public Schar wrappingSymSchar(SymbolicExecution se, NumericExpression numericExpression) {
        assert !(numericExpression instanceof ConcolicNumericContainer)
                && !((numericExpression instanceof SymNumericExpressionSprimitive)
                && ((SymNumericExpressionSprimitive) numericExpression).getRepresentedExpression() instanceof ConcolicNumericContainer);
        return svf.wrappingSymSchar(se, numericExpression);
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        assert !(constraint instanceof ConcolicConstraintContainer)
                && !((constraint instanceof Sbool.SymSbool)
                    && ((Sbool.SymSbool) constraint).getRepresentedExpression() instanceof ConcolicConstraintContainer);
        return svf.wrappingSymSbool(se, constraint);
    }

    @Override
    public Sint.SymSint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1) {
        return svf.cmp(se, n0, n1);
    }

    @Override
    public Sbool.SymSbool assignLabel(SymbolicExecution se, Sbool.SymSbool potentiallyToUnwrap) {
        Sbool.SymSbool sym = (Sbool.SymSbool) ConcolicConstraintContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        // Concrete value
        Sbool.ConcSbool conc = Sbool.concSbool((Boolean) se.label(sym));
        // Container for both
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(sym, conc);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Sshort.SymSshort assignLabel(SymbolicExecution se, Sshort.SymSshort potentiallyToUnwrap) {
        Sshort.SymSshort sym = (Sshort.SymSshort) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte potentiallyToUnwrap) {
        Sbyte.SymSbyte sym = (Sbyte.SymSbyte) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint potentiallyToUnwrap) {
        Sint.SymSint sym = (Sint.SymSint) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sint.concSint((Integer) o),
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong potentiallyToUnwrap) {
        Slong.SymSlong sym = (Slong.SymSlong) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble potentiallyToUnwrap) {
        Sdouble.SymSdouble sym = (Sdouble.SymSdouble) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }
}
