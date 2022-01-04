package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.function.Function;

// The creation of concrete numbers is performed in SymbolicValueFactory.
public class ConcolicValueFactory extends AbstractValueFactory {

    private final SymbolicValueFactory svf;
    
    ConcolicValueFactory(MulibConfig config) {
        super(config);
        this.svf = SymbolicValueFactory.getInstance(config);
    }

    public static ConcolicValueFactory getInstance(MulibConfig config) {
        return new ConcolicValueFactory(config);
    }

    private static <SA extends SymNumericExpressionSprimitive, S, N> S numericConcolicWrapperCreator(
            SymbolicExecution se,
            Function<SymbolicExecution, SA> symCreator,
            Function<Object, ConcSnumber> concSnumberCreator,
            Function<ConcolicNumericContainer, S> resultWrapper) {
        // Symbolic value
        SA sym = symCreator.apply(se);
        // Concrete value
        ConcSnumber conc = concSnumberCreator.apply(se.label(sym));
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
                c -> (Sint.SymSint) Sint.newExpressionSymbolicSint(c)
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSdouble,
                o -> concSdouble((Double) o),
                c -> (Sdouble.SymSdouble) Sdouble.newExpressionSymbolicSdouble(c)
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSfloat,
                o -> concSfloat((Float) o),
                c -> (Sfloat.SymSfloat) Sfloat.newExpressionSymbolicSfloat(c)
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        // Symbolic value
        Sbool.SymSbool sym = svf.symSbool(se);
        // Concrete value
        Sbool.ConcSbool conc = concSbool((Boolean) se.label(sym));
        // Container for both
        ConcolicConstraintContainer container = new ConcolicConstraintContainer(sym, conc);
        return (Sbool.SymSbool) Sbool.newConstraintSbool(container);
    }

    @Override
    public Slong.SymSlong symSlong(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSlong,
                o -> concSlong((Long) o),
                c -> (Slong.SymSlong) Slong.newExpressionSymbolicSlong(c)
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSshort,
                o -> concSshort((Short) o),
                c -> (Sshort.SymSshort) Sshort.newExpressionSymbolicSshort(c)
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                svf::symSbyte,
                o -> concSbyte((Byte) o),
                c -> (Sbyte.SymSbyte) Sbyte.newExpressionSymbolicSbyte(c)
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
}
