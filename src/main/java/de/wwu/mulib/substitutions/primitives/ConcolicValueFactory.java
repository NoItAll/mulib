package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Eq;
import de.wwu.mulib.expressions.ConcolicNumericContainer;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

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

    protected void restrictLength(SymbolicExecution se, Sint len) {
        if (len instanceof Sint.ConcSint) {
            if (((Sint.ConcSint) len).intVal() < 0) {
                throw new NegativeArraySizeException();
            }
        } else if (throwExceptionOnOOB) {
            // Must be SymSbool since Sint.ZERO is concrete and len has been checked
            Sbool.SymSbool outOfBounds = (Sbool.SymSbool) se.gte(Sint.ConcSint.ZERO, len);
            if (se.boolChoice(outOfBounds)) {
                throw new NegativeArraySizeException();
            }
        } else if (!se.nextIsOnKnownPath()) {
            Sbool inBounds = se.lte(Sint.ConcSint.ZERO, len);
            Constraint actualConstraint = ConcolicConstraintContainer.tryGetSymFromConcolic(inBounds);
            if (actualConstraint instanceof Sbool.SymSbool) {
                actualConstraint = ((Sbool.SymSbool) actualConstraint).getRepresentedConstraint();
            }
            se.addNewConstraint(actualConstraint);
        }
    }

    @Override
    public Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SintSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SdoubleSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SfloatSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SlongSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SshortSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SbyteSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SboolSarray(len, se, freeElements);
    }

    @Override
    public Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.PartnerClassSarray(clazz, len, se, freeElements);
    }

    @Override
    public Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements) {
        restrictLength(se, len);
        return new Sarray.SarraySarray(len, se, freeElements, clazz);
    }

    @Override
    public Sarray.SarraySarray sarrarSarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz) {
        restrictLength(se, lengths[0]);
        return new Sarray.SarraySarray(lengths, se, clazz);
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
        // TODO Performance optimization: If nextIsOnKnownPath() is false, we can return the neutral element (e.g. 0 and
        //  false) or 1 to directly account for Sarray-based index-constraints.
        // TODO addTemporaryAssumption for each concolic value yields high overhead. Potentially, we should add an
        //  implementation for free concolic arrays in a way that avoids the need for these types of consistency checks.
        se.addTemporaryAssumption(Eq.newInstance(sym, conc));
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
