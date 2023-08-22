package de.wwu.mulib.substitutions;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicNumericalContainer;
import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Map;
import java.util.function.Function;

/**
 * Value factory for concolic execution.
 * Uses an instance of {@link SymbolicValueFactory} to generate symbolic values.
 * For the generation of leafs, these symbolic values are labeled using the constraint solver and a
 * wrapping {@link Sprimitive} is returned.
 * The construction of wrappers is completely delegated to the {@link SymbolicValueFactory}; - the wrapping into the
 * {@link ConcolicNumericalContainer} or {@link ConcolicConstraintContainer} occurs in the {@link de.wwu.mulib.search.executors.ConcolicCalculationFactory}.
 */
public class ConcolicValueFactory extends AbstractValueFactory implements AssignConcolicLabelEnabledValueFactory {

    private final SymbolicValueFactory svf;

    ConcolicValueFactory(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        super(config, arrayTypesToSpecializedSarrayClass);
        this.svf = SymbolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
    }

    static ConcolicValueFactory getInstance(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        return new ConcolicValueFactory(config, arrayTypesToSpecializedSarrayClass);
    }

    @Override
    protected void _addZeroLteLengthConstraint(SymbolicExecution se, Sint len) {
        Sbool inBounds = se.lte(Sint.ConcSint.ZERO, len);
        Constraint actualConstraint = ConcolicConstraintContainer.tryGetSymFromConcolic(inBounds);
        if (actualConstraint instanceof Sbool.SymSbool) {
            actualConstraint = ((Sbool.SymSbool) actualConstraint).getRepresentedConstraint();
        }
        se.addNewConstraint(actualConstraint);
    }

    private <SA extends SymSnumber, S, N> S numericConcolicWrapperCreator(
            SymbolicExecution se,
            Function<SymbolicExecution, SA> symCreator,
            Function<Object, ConcSnumber> concSnumberCreator,
            Function<ConcolicNumericalContainer, S> resultWrapper) {
        // Symbolic value
        SA sym = symCreator.apply(se);
        if (!se.isSatisfiable()) {
            // Must be called anyway for labeling
            throw Mulib.fail();
        }
        // Concrete value
        ConcSnumber conc = concSnumberCreator.apply(se.label(sym));
        // TODO Performance optimization: If nextIsOnKnownPath() is false, we can return the neutral element (e.g. 0 and
        //  false) or 1 to directly account for Sarray-based index-constraints.
        // Container for both
        ConcolicNumericalContainer container = new ConcolicNumericalContainer(sym, conc);
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
            // Must be called anyway for labeling
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
                s -> svf.symSint(s, (Sint) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Sint) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
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
                s -> svf.symSdouble(s, (Sdouble) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Sdouble) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSfloat(s, (Sfloat) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Sfloat) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Slong symSlong(SymbolicExecution se, Slong lb, Slong ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSlong(s, (Slong) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Slong) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSshort(s, (Sshort) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Sshort) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSbyte(s, (Sbyte) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Sbyte) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Schar symSchar(SymbolicExecution se, Schar lb, Schar ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSchar(s, (Schar) ConcolicNumericalContainer.tryGetSymFromConcolic(lb), (Schar) ConcolicNumericalContainer.tryGetSymFromConcolic(ub)),
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSint(se, numericalExpression);
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSdouble(se, numericalExpression);
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSfloat(se, numericalExpression);
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSlong(se, numericalExpression);
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSshort(se, numericalExpression);
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                    && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSbyte(se, numericalExpression);
    }

    @Override
    public Schar wrappingSymSchar(SymbolicExecution se, NumericalExpression numericalExpression) {
        assert !(numericalExpression instanceof ConcolicNumericalContainer)
                && !((numericalExpression instanceof SymSnumber)
                && ((SymSnumber) numericalExpression).getRepresentedExpression() instanceof ConcolicNumericalContainer);
        return svf.wrappingSymSchar(se, numericalExpression);
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        assert !(constraint instanceof ConcolicConstraintContainer)
                && !((constraint instanceof Sbool.SymSbool)
                    && ((Sbool.SymSbool) constraint).getRepresentedExpression() instanceof ConcolicConstraintContainer);
        return svf.wrappingSymSbool(se, constraint);
    }

    @Override
    public Sint.SymSint cmp(SymbolicExecution se, NumericalExpression n0, NumericalExpression n1) {
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
        Sshort.SymSshort sym = (Sshort.SymSshort) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte potentiallyToUnwrap) {
        Sbyte.SymSbyte sym = (Sbyte.SymSbyte) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint potentiallyToUnwrap) {
        Sint.SymSint sym = (Sint.SymSint) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sint.concSint((Integer) o),
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong potentiallyToUnwrap) {
        Slong.SymSlong sym = (Slong.SymSlong) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble potentiallyToUnwrap) {
        Sdouble.SymSdouble sym = (Sdouble.SymSdouble) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Schar.SymSchar assignLabel(SymbolicExecution se, Schar.SymSchar potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicNumericalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }
}
