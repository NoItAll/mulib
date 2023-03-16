package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.Fail;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.function.Function;

// The creation of concrete numbers is performed in SymbolicValueFactory.
public class ConcolicValueFactory extends AbstractValueFactory implements AssignConcolicLabelEnabledValueFactory {

    private final SymbolicValueFactory svf;
    
    ConcolicValueFactory(MulibConfig config) {
        super(config);
        this.svf = SymbolicValueFactory.getInstance(config);
    }

    public static ConcolicValueFactory getInstance(MulibConfig config) {
        return new ConcolicValueFactory(config);
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
            throw new Fail();
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
                o -> concSint((Integer) o),
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
                o -> concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSfloat,
                o -> concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        // Symbolic value
        Sbool.SymSbool sym = svf.symSbool(se);
        if (!se.isSatisfiable()) {
            throw new Fail();
        }
        // Concrete value
        Sbool.ConcSbool conc = concSbool((Boolean) se.label(sym));
        // Container for both
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(sym, conc);
        return Sbool.newConstraintSbool(container);
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSlong,
                o -> concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSshort,
                o -> concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSbyte,
                o -> concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Schar.SymSchar symSchar(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                this::symSchar,
                o -> concSchar((Character) o),
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
        Sbool.ConcSbool conc = concSbool((Boolean) se.label(sym));
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
                o -> concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte potentiallyToUnwrap) {
        Sbyte.SymSbyte sym = (Sbyte.SymSbyte) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint potentiallyToUnwrap) {
        Sint.SymSint sym = (Sint.SymSint) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> concSint((Integer) o),
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong potentiallyToUnwrap) {
        Slong.SymSlong sym = (Slong.SymSlong) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble potentiallyToUnwrap) {
        Sdouble.SymSdouble sym = (Sdouble.SymSdouble) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicNumericContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }
}
