package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

import java.util.function.Function;

// The creation of concrete numbers is performed in SymbolicValueFactory.
public class ConcolicValueFactory extends SymbolicValueFactory {

    ConcolicValueFactory(MulibConfig config) {
        super(config);
    }

    public static ConcolicValueFactory getInstance(MulibConfig config) {
        return new ConcolicValueFactory(config);
    }

    private static <SA extends Sprimitive, S, N> S numericConcolicWrapperCreator(
            SymbolicExecution se,
            Function<SymbolicExecution, SA> symCreator,
            Function<Object, ConcSnumber> concSnumberCreator,
            Function<ConcolicNumericContainer, S> resultWrapper) {
        // Symbolic value
        SA sym = symCreator.apply(se);
        // Concrete value
        ConcSnumber conc = concSnumberCreator.apply(se.label(sym));
        // Container for both
        ConcolicNumericContainer container = new ConcolicNumericContainer((NumericExpression) sym, conc);
        return resultWrapper.apply(container);
    }

    @Override
    public Sint.SymSint symSint(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                super::symSint,
                o -> concSint((Integer) o),
                // Wrapped in new SymSint; we do not reuse these outer wrappers, thus, we do not call
                // super.wrappingXYZ(...).
                c -> (Sint.SymSint) Sint.newExpressionSymbolicSint(c)
        );
    }

    @Override
    public Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                super::symSdouble,
                o -> concSdouble((Double) o),
                c -> (Sdouble.SymSdouble) Sdouble.newExpressionSymbolicSdouble(c)
        );
    }

    @Override
    public Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                super::symSfloat,
                o -> concSfloat((Float) o),
                c -> (Sfloat.SymSfloat) Sfloat.newExpressionSymbolicSfloat(c)
        );
    }

    @Override
    public Sbool.SymSbool symSbool(SymbolicExecution se) {
        // Symbolic value
        Sbool.SymSbool sym = super.symSbool(se);
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
                super::symSlong,
                o -> concSlong((Long) o),
                c -> (Slong.SymSlong) Slong.newExpressionSymbolicSlong(c)
        );
    }

    @Override
    public Sshort.SymSshort symSshort(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                super::symSshort,
                o -> concSshort((Short) o),
                c -> (Sshort.SymSshort) Sshort.newExpressionSymbolicSshort(c)
        );
    }

    @Override
    public Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return numericConcolicWrapperCreator(
                se,
                super::symSbyte,
                o -> concSbyte((Byte) o),
                c -> (Sbyte.SymSbyte) Sbyte.newExpressionSymbolicSbyte(c)
        );
    }

    @Override
    public Sint.SymSint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        // TODO If SymbolicExecution has been used as a fallback, numericExpression is not necessarily a ConcolicNumericContainer
        if (numericExpression instanceof ConcolicNumericContainer) {
            // We simply wrap the new ConcolicContainer
            return (Sint.SymSint) Sint.newExpressionSymbolicSint(numericExpression);
        } else {
            return super.wrappingSymSint(se, numericExpression);
        }
    }

    @Override
    public Sdouble.SymSdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        if (numericExpression instanceof ConcolicNumericContainer) {
            return (Sdouble.SymSdouble) Sdouble.newExpressionSymbolicSdouble(numericExpression);
        } else {
            return super.wrappingSymSdouble(se, numericExpression);
        }
    }

    @Override
    public Sfloat.SymSfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        if (numericExpression instanceof ConcolicNumericContainer) {
            return (Sfloat.SymSfloat) Sfloat.newExpressionSymbolicSfloat(numericExpression);
        } else {
            return super.wrappingSymSfloat(se, numericExpression);
        }
    }

    @Override
    public Slong.SymSlong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        if (numericExpression instanceof ConcolicNumericContainer) {
            return (Slong.SymSlong) Slong.newExpressionSymbolicSlong(numericExpression);
        } else {
            return super.wrappingSymSlong(se, numericExpression);
        }
    }

    @Override
    public Sshort.SymSshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        if (numericExpression instanceof ConcolicNumericContainer) {
            return (Sshort.SymSshort) Sshort.newExpressionSymbolicSshort(numericExpression);
        } else {
            return super.wrappingSymSshort(se, numericExpression);
        }
    }

    @Override
    public Sbyte.SymSbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression) {
        assert numericExpression instanceof ConcolicNumericContainer; // TODO for now
        if (numericExpression instanceof ConcolicNumericContainer) {
            return (Sbyte.SymSbyte) Sbyte.newExpressionSymbolicSbyte(numericExpression);
        } else {
            return super.wrappingSymSbyte(se, numericExpression);
        }
    }

    @Override
    public Sbool.SymSbool wrappingSymSbool(SymbolicExecution se, Constraint constraint) {
        assert constraint instanceof ConcolicConstraintContainer; // TODO for now
        if (constraint instanceof ConcolicConstraintContainer) {
            return (Sbool.SymSbool) Sbool.newConstraintSbool(constraint);
        } else {
            return super.wrappingSymSbool(se, constraint);
        }
    }
}
