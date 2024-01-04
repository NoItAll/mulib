package de.wwu.mulib.substitutions;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicMathematicalContainer;
import de.wwu.mulib.expressions.Expression;
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
 * {@link ConcolicMathematicalContainer} or {@link ConcolicConstraintContainer} occurs in the {@link de.wwu.mulib.search.executors.ConcolicCalculationFactory}.
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
            Function<ConcolicMathematicalContainer, S> resultWrapper) {
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
        ConcolicMathematicalContainer container = new ConcolicMathematicalContainer(sym, conc);
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
                s -> svf.symSint(s, (Sint) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Sint) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
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
                s -> svf.symSdouble(s, (Sdouble) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Sdouble) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSfloat(s, (Sfloat) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Sfloat) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Slong symSlong(SymbolicExecution se, Slong lb, Slong ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSlong(s, (Slong) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Slong) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSshort(s, (Sshort) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Sshort) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSbyte(s, (Sbyte) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Sbyte) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Schar symSchar(SymbolicExecution se, Schar lb, Schar ub) {
        return numericConcolicWrapperCreator(
                se,
                s -> svf.symSchar(s, (Schar) ConcolicMathematicalContainer.tryGetSymFromConcolic(lb), (Schar) ConcolicMathematicalContainer.tryGetSymFromConcolic(ub)),
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSint(se, expression);
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSdouble(se, expression);
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSfloat(se, expression);
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSlong(se, expression);
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSshort(se, expression);
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                    && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSbyte(se, expression);
    }

    @Override
    public Schar wrappingSymSchar(SymbolicExecution se, Expression expression) {
        assert !(expression instanceof ConcolicMathematicalContainer)
                && !((expression instanceof SymSnumber)
                && ((SymSnumber) expression).getRepresentedExpression() instanceof ConcolicMathematicalContainer);
        return svf.wrappingSymSchar(se, expression);
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        assert !(constraint instanceof ConcolicConstraintContainer)
                && !((constraint instanceof Sbool.SymSbool)
                    && ((Sbool.SymSbool) constraint).getRepresentedExpression() instanceof ConcolicConstraintContainer);
        return svf.wrappingSymSbool(se, constraint);
    }

    @Override
    public Sint.SymSint cmp(SymbolicExecution se, Expression n0, Expression n1) {
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
        Sshort.SymSshort sym = (Sshort.SymSshort) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sshort.concSshort((Short) o),
                Sshort::newExpressionSymbolicSshort
        );
    }

    @Override
    public Sbyte.SymSbyte assignLabel(SymbolicExecution se, Sbyte.SymSbyte potentiallyToUnwrap) {
        Sbyte.SymSbyte sym = (Sbyte.SymSbyte) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sbyte.concSbyte((Byte) o),
                Sbyte::newExpressionSymbolicSbyte
        );
    }

    @Override
    public Sint.SymSint assignLabel(SymbolicExecution se, Sint.SymSint potentiallyToUnwrap) {
        Sint.SymSint sym = (Sint.SymSint) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sint.concSint((Integer) o),
                Sint::newExpressionSymbolicSint
        );
    }

    @Override
    public Slong.SymSlong assignLabel(SymbolicExecution se, Slong.SymSlong potentiallyToUnwrap) {
        Slong.SymSlong sym = (Slong.SymSlong) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Slong.concSlong((Long) o),
                Slong::newExpressionSymbolicSlong
        );
    }

    @Override
    public Sdouble.SymSdouble assignLabel(SymbolicExecution se, Sdouble.SymSdouble potentiallyToUnwrap) {
        Sdouble.SymSdouble sym = (Sdouble.SymSdouble) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sdouble.concSdouble((Double) o),
                Sdouble::newExpressionSymbolicSdouble
        );
    }

    @Override
    public Sfloat.SymSfloat assignLabel(SymbolicExecution se, Sfloat.SymSfloat potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Sfloat.concSfloat((Float) o),
                Sfloat::newExpressionSymbolicSfloat
        );
    }

    @Override
    public Schar.SymSchar assignLabel(SymbolicExecution se, Schar.SymSchar potentiallyToUnwrap) {
        Sfloat.SymSfloat sym = (Sfloat.SymSfloat) ConcolicMathematicalContainer.tryGetSymFromConcolic(potentiallyToUnwrap);
        return numericConcolicWrapperCreator(
                se,
                (s) -> sym,
                o -> Schar.concSchar((Character) o),
                Schar::newExpressionSymbolicSchar
        );
    }
}
