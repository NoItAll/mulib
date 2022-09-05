package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class JavaSMTSolverManager extends AbstractIncrementalEnabledSolverManager<Model, BooleanFormula, ArrayFormula> {

    private static final Object syncObject = new Object();
    private final ProverEnvironment solver;
    private final JavaSMTMulibAdapter adapter;

    public JavaSMTSolverManager(MulibConfig mulibConfig) {
        super(mulibConfig);
        synchronized (syncObject) {
            Configuration config = Configuration.defaultConfiguration();
            ShutdownManager shutdown = ShutdownManager.create();
            try {
                Solvers solverType = mulibConfig.GLOBAL_SOLVER_TYPE;
                SolverContextFactory.Solvers chosenSolver;
                switch (solverType) {
                    case JSMT_Z3:
                        chosenSolver = SolverContextFactory.Solvers.Z3;
                        break;
                    case JSMT_SMTINTERPOL:
                        chosenSolver = SolverContextFactory.Solvers.SMTINTERPOL;
                        break;
                    case JSMT_PRINCESS:
                        chosenSolver = SolverContextFactory.Solvers.PRINCESS;
                        break;
                    case JSMT_CVC4:
                        chosenSolver = SolverContextFactory.Solvers.CVC4;
                        break;
                    case JSMT_MATHSAT5:
                        chosenSolver = SolverContextFactory.Solvers.MATHSAT5;
                        break;
                    case JSMT_YICES2:
                        chosenSolver = SolverContextFactory.Solvers.YICES2;
                        break;
                    case JSMT_BOOLECTOR:
                        chosenSolver = SolverContextFactory.Solvers.BOOLECTOR;
                        break;
                    default:
                        throw new NotYetImplementedException();
                }
                SolverContext context = SolverContextFactory.createSolverContext(
                        config,
                        BasicLogManager.create(config),
                        shutdown.getNotifier(),
                        chosenSolver
                );
                this.adapter = new JavaSMTMulibAdapter(mulibConfig, context);
                this.solver = context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS);
            } catch (InvalidConfigurationException e) {
                throw new MulibRuntimeException(e);
            }
        }
    }

    @Override
    protected void addSolverConstraintRepresentation(BooleanFormula b) {
        try {
            solver.addConstraint(b);
        } catch (Exception e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        try {
            return !solver.isUnsat();
        } catch (SolverException | InterruptedException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected ArrayFormula createCompletelyNewArrayRepresentation(ArrayInitializationConstraint ac) {
        return adapter.newArrayExprFromType(ac.getArrayId(), ac.getValueType());
    }

    @Override
    protected ArrayFormula createNewArrayRepresentationForStore(ArrayAccessConstraint ac, ArrayFormula oldRepresentation) {
        assert ac.getType() == ArrayAccessConstraint.Type.STORE;
        // We do not need to add anything to the constraint store. This constraint will "be made true" via array-nesting.
        return adapter.newArrayExprFromStore(
                oldRepresentation,
                ac.getIndex(),
                ac.getValue()
        );
    }

    @Override
    protected void addArraySelectConstraint(ArrayFormula arrayRepresentation, Sint index, SubstitutedVar value) {
        BooleanFormula selectConstraint = newArraySelectConstraint(arrayRepresentation, index, value);
        try {
            solver.addConstraint(selectConstraint);
        } catch (InterruptedException e) {
            throw new MulibRuntimeException(e);
        }
    }

    protected BooleanFormula newArraySelectConstraint(ArrayFormula arrayFormula, Sint index, SubstitutedVar value) {
        return adapter.transformSelectConstraint(arrayFormula, index, value);
    }

    @Override
    protected BooleanFormula transformConstraint(Constraint c) {
        return adapter.transformConstraint(c);
    }

    @Override
    protected void solverSpecificBacktrackOnce() {
        solverSpecificBacktrack(1);
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        for (int i = 0; i < toBacktrack; i++) {
            solver.pop();
        }
    }

    @Override
    protected boolean calculateSatisfiabilityWithSolverBoolRepresentation(BooleanFormula f) {
        try {
            return !solver.isUnsatWithAssumptions(Collections.singleton(f));
        } catch (SolverException | InterruptedException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected void solverSpecificBacktrackingPoint() {
        solver.push();
    }


    @Override
    protected Model calculateCurrentModel() {
        try {
            return solver.getModel();
        } catch (SolverException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected Object labelSymSprimitive(SymSprimitive var) {
        Formula f = var instanceof Sbool ?
                adapter.getBooleanFormulaForConstraint((Constraint) var)
                :
                adapter.getFormulaForNumericExpression((NumericExpression) var);
        if (f == null) {
            if (var instanceof Constraint) {
                f = adapter.transformConstraint((Constraint) var);
            } else {
                f = adapter.transformNumeral((NumericExpression) var);
            }
        }
        assert f != null;
        try {
            Object result = toPrimitiveOrString(var, getCurrentModel().evaluate(f));
            assert result != null;
            return result;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MulibRuntimeException(t);
        }
    }

    private static Object toPrimitiveOrString(Sprimitive p, Object o) {
        if (o instanceof BigInteger) {
            if (p instanceof Sint) {
                if (p instanceof Sshort) {
                    return ((BigInteger) o).shortValue();
                } else if (p instanceof Sbyte) {
                    return ((BigInteger) o).byteValue();
                } else {
                    return ((BigInteger) o).intValue();
                }
            } else if (p instanceof Slong) {
                return ((BigInteger) o).longValue();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Rational) {
            if (p instanceof Sdouble) {
                return ((Rational) o).doubleValue();
            } else {
                return ((Rational) o).floatValue();
            }
        } else if (o instanceof Boolean) {
            return o;
        } else {
            throw new NotYetImplementedException();
        }
    }

    protected static final class JavaSMTMulibAdapter {
        private final Map<NumericExpression, NumeralFormula> numericExpressionStore = new WeakHashMap<>();
        private final Map<Object, BooleanFormula> booleanFormulaStore = new WeakHashMap<>();
        private final BooleanFormulaManager booleanFormulaManager;
        private final IntegerFormulaManager integerFormulaManager;
        private final RationalFormulaManager rationalFormulaManager;
        private final ArrayFormulaManager arrayFormulaManager;
        private final boolean treatSboolsAsInts;

        JavaSMTMulibAdapter(MulibConfig config, SolverContext context) {
            FormulaManager formulaManager = context.getFormulaManager();
            booleanFormulaManager = formulaManager.getBooleanFormulaManager();
            integerFormulaManager = formulaManager.getIntegerFormulaManager();
            rationalFormulaManager = formulaManager.getRationalFormulaManager();
            arrayFormulaManager = formulaManager.getArrayFormulaManager();
            this.treatSboolsAsInts = config.TREAT_BOOLEANS_AS_INTS;
        }

        Formula getFormulaForNumericExpression(NumericExpression numericExpression) {
            return numericExpressionStore.get(numericExpression);
        }

        BooleanFormula getBooleanFormulaForConstraint(Constraint c) {
            return booleanFormulaStore.get(c);
        }

        private Formula transformSubstitutedVar(SubstitutedVar sv) {
            if (sv instanceof Sbool) {
                return transformSbool((Sbool) sv);
            } else if (sv instanceof Snumber) {
                return transformSnumber((Snumber) sv);
            } else {
                throw new NotYetImplementedException();
            }
        }

        private NumeralFormula transformSnumber(Snumber n) {
            NumeralFormula result = numericExpressionStore.get(n);
            if (result != null) {
                return result;
            }
            if (n instanceof Sint) {
                result = transformSintegerNumber((Sint) n);
            } else if (n instanceof Sfpnumber) {
                result = transformSfpnumber((Sfpnumber) n);
            } else if (n instanceof Slong) {
                result = _transformSnumber(
                        n,
                        () -> n instanceof Slong.ConcSlong,
                        () -> n instanceof Slong.SymSlong,
                        () -> integerFormulaManager.makeNumber(((Slong.ConcSlong) n).longVal()),
                        () -> integerFormulaManager.makeVariable(((SymSprimitive) n).getId())
                );
            } else {
                throw new NotYetImplementedException();
            }
            numericExpressionStore.put(n, result);
            return result;
        }

        BooleanFormula transformConstraint(Constraint c) {
            BooleanFormula result;
            if (c instanceof Sbool) {
                result = transformSbool((Sbool) c);
            } else if (c instanceof Not) {
                result = booleanFormulaManager.not(transformConstraint(((Not) c).getConstraint()));
            } else if (c instanceof AbstractTwoSidedNumericConstraint) {
                result = transformAbstractNumericTwoSidedConstraint((AbstractTwoSidedNumericConstraint) c);
            } else if (c instanceof AbstractTwoSidedConstraint) {
                result = transformAbstractTwoSidedConstraint((AbstractTwoSidedConstraint) c);
            } else {
                throw new NotYetImplementedException();
            }
            return result;
        }

        private BooleanFormula transformAbstractTwoSidedConstraint(AbstractTwoSidedConstraint c) {
            if (c instanceof And) {
                return booleanFormulaManager.and(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
            } else if (c instanceof Or) {
                return booleanFormulaManager.or(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
            } else if (c instanceof Xor) {
                return booleanFormulaManager.xor(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
            } else {
                throw new NotYetImplementedException();
            }
        }

        private BooleanFormula transformAbstractNumericTwoSidedConstraint(AbstractTwoSidedNumericConstraint a) {
            BooleanFormula result;
            NumeralFormula lhs = transformNumeral(a.getLhs());
            NumeralFormula rhs = transformNumeral(a.getRhs());

            BiFunction<NumeralFormula, NumeralFormula, BooleanFormula> fpCase;
            BiFunction<NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula, BooleanFormula> integerCase;
            if (a instanceof Eq) {
                fpCase = rationalFormulaManager::equal;
                integerCase = integerFormulaManager::equal;
            } else if (a instanceof Lt) {
                fpCase = rationalFormulaManager::lessThan;
                integerCase = integerFormulaManager::lessThan;
            } else if (a instanceof Lte) {
                fpCase = rationalFormulaManager::lessOrEquals;
                integerCase = integerFormulaManager::lessOrEquals;
            } else {
                throw new NotYetImplementedException();
            }
            if (a.getLhs().isFp() || a.getRhs().isFp()) {
                result = fpCase.apply(lhs, rhs);
            } else {
                result = integerCase.apply((NumeralFormula.IntegerFormula) lhs, (NumeralFormula.IntegerFormula) rhs);
            }

            return result;
        }

        public NumeralFormula transformNumeral(NumericExpression n) {
            NumeralFormula result;

            if (n instanceof AbstractOperatorNumericExpression) {
                AbstractOperatorNumericExpression o = (AbstractOperatorNumericExpression) n;
                NumericExpression lhs = o.getExpr0();
                NumericExpression rhs = o.getExpr1();
                NumeralFormula elhs = transformNumeral(lhs);
                NumeralFormula erhs = transformNumeral(rhs);
                BiFunction<NumeralFormula, NumeralFormula, NumeralFormula> fpCase;
                BiFunction<NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula, NumeralFormula> integerCase;
                if (n instanceof Sum) {
                    fpCase = rationalFormulaManager::add;
                    integerCase = integerFormulaManager::add;
                } else if (n instanceof Mul) {
                    fpCase = rationalFormulaManager::multiply;
                    integerCase = integerFormulaManager::multiply;
                } else if (n instanceof Sub) {
                    fpCase = rationalFormulaManager::subtract;
                    integerCase = integerFormulaManager::subtract;
                } else {
                    throw new NotYetImplementedException();
                }
                if (n.isFp()) {
                    result = fpCase.apply(elhs, erhs);
                } else {
                    result = integerCase.apply((NumeralFormula.IntegerFormula) elhs, (NumeralFormula.IntegerFormula) erhs);
                }
            } else if (n instanceof AbstractExpressionWrappingExpression) {
                if (n instanceof Neg) {
                    if (n.isFp()) {
                        result = rationalFormulaManager.negate(transformNumeral(((Neg) n).getWrapped()));
                    } else {
                        result = integerFormulaManager.negate((NumeralFormula.IntegerFormula) transformNumeral(((Neg) n).getWrapped()));
                    }
                } else {
                    throw new NotYetImplementedException();
                }
            } else {
                result = transformSnumber((Snumber) n);
            }

            return result;
        }

        private NumeralFormula _transformSnumber(
                Snumber n,
                Supplier<Boolean> isConc,
                Supplier<Boolean> isSym,
                Supplier<NumeralFormula> makeConc,
                Supplier<NumeralFormula> makeSym) {
            if (isConc.get()) {
                return makeConc.get();
            } else if (isSym.get()) {
                SymNumericExpressionSprimitive _i = (SymNumericExpressionSprimitive) n;
                if (_i.getRepresentedExpression() == _i) {
                    return makeSym.get();
                } else {
                    return transformNumeral(_i.getRepresentedExpression());
                }
            } else {
                throw new NotYetImplementedException();
            }
        }

        private NumeralFormula transformSfpnumber(Sfpnumber f) {
            NumeralFormula result = numericExpressionStore.get(f);
            if (result != null) {
                return result;
            }
            if (f instanceof Sdouble) {
                result = _transformSnumber(
                        f,
                        () -> f instanceof Sdouble.ConcSdouble,
                        () -> f instanceof Sdouble.SymSdouble,
                        () -> rationalFormulaManager.makeNumber(((Sdouble.ConcSdouble) f).doubleVal()),
                        () -> rationalFormulaManager.makeVariable(((SymSprimitive) f).getId())
                );
            } else if (f instanceof Sfloat) {
                result = _transformSnumber(
                        f,
                        () -> f instanceof Sfloat.ConcSfloat,
                        () -> f instanceof Sfloat.SymSfloat,
                        () -> rationalFormulaManager.makeNumber(((Sfloat.ConcSfloat) f).floatVal()),
                        () -> rationalFormulaManager.makeVariable(((SymSprimitive) f).getId())
                );
            }
            numericExpressionStore.put(f, result);
            return result;
        }

        private NumeralFormula transformSintegerNumber(Sint i) {
            NumeralFormula result = numericExpressionStore.get(i);
            if (result != null) {
                return result;
            }
            Supplier<NumeralFormula> makeSym;
            if (i instanceof Sbool) {
                if (!treatSboolsAsInts) {
                    throw new MulibRuntimeException("Must not occur");
                }
                makeSym =  () -> integerFormulaManager.makeVariable(((SymSprimitive) i).getId() + "_int");
            } else {
                makeSym = () -> integerFormulaManager.makeVariable(((SymSprimitive) i).getId());
            }
            result = _transformSnumber(
                    i,
                    () -> i instanceof ConcSnumber,
                    () -> i instanceof SymSprimitive,
                    () -> integerFormulaManager.makeNumber(((ConcSnumber) i).intVal()),
                    makeSym
            );
            numericExpressionStore.put(i, result);
            return result;
        }

        private BooleanFormula transformSbool(Sbool b) {
            BooleanFormula result = booleanFormulaStore.get(b);
            if (result != null) {
                return result;
            }
            if (b instanceof Sbool.ConcSbool) {
                result = booleanFormulaManager.makeBoolean(((Sbool.ConcSbool) b).isTrue());
            } else if (b instanceof Sbool.SymSbool) {
                Constraint representedConstraint = ((Sbool.SymSbool) b).getRepresentedConstraint();
                if (representedConstraint == b) {
                    result = booleanFormulaManager.makeVariable(((SymSprimitive) b).getId());
                } else {
                    result = transformConstraint(representedConstraint);
                }
            } else {
                throw new NotYetImplementedException();
            }
            booleanFormulaStore.put(b, result);
            return result;
        }

        private ArrayFormula newArrayExprFromType(Sint arrayId, Class<?> type) {
            FormulaType arraySort;
            if (Sprimitive.class.isAssignableFrom(type)) {
                if (Sbool.class.isAssignableFrom(type)) {
                    arraySort = FormulaType.BooleanType;
                } else if (Sint.class.isAssignableFrom(type) || Slong.class.isAssignableFrom(type)) {
                    arraySort = FormulaType.IntegerType;
                } else if (Sfpnumber.class.isAssignableFrom(type)) {
                    arraySort = FormulaType.RationalType;
                } else {
                    throw new NotYetImplementedException();
                }
            } else {
                throw new NotYetImplementedException();
            }
            try {
                return arrayFormulaManager.makeArray(
                        // In the mutable case, multiple array expressions might represent the array
                        "Sarray" + arrayId,
                        FormulaType.IntegerType,
                        arraySort
                );
            } catch (Throwable t) {
                throw new MulibRuntimeException(t);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public ArrayFormula newArrayExprFromStore(ArrayFormula oldRepresentation, Sint index, SubstitutedVar value) {
            Formula f = transformSubstitutedVar(value);
            Formula i = transformSintegerNumber(index);
            return arrayFormulaManager.store(oldRepresentation, i, f);
        }

        public BooleanFormula transformSelectConstraint(ArrayFormula array, Sint index, SubstitutedVar var) {
            Formula selectExpr = arrayFormulaManager.select(array, transformSintegerNumber(index));
            Formula value = transformSubstitutedVar(var);
            BooleanFormula result;
            if (var instanceof Sbool) {
                result = booleanFormulaManager.equivalence((BooleanFormula) selectExpr, (BooleanFormula) value);
            } else if (var instanceof Sint) {
                result = integerFormulaManager.equal(
                        (NumeralFormula.IntegerFormula) selectExpr, (NumeralFormula.IntegerFormula) value);
            } else if (var instanceof Sfpnumber) {
                result = rationalFormulaManager.equal(
                        (NumeralFormula.RationalFormula) selectExpr, (NumeralFormula.RationalFormula) value);
            } else {
                throw new NotYetImplementedException();
            }
            return result;
        }
    }
}