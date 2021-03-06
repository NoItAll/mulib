package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.solving.Solvers;
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

public final class JavaSMTSolverManager extends AbstractIncrementalEnabledSolverManager<Model> {

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
    protected void addSolverConstraintRepresentation(Constraint constraint) {
        BooleanFormula b = adapter.transformConstraint(constraint);
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
    protected void solverSpecificBacktrackingPoint() {
        solver.push();
    }

    @Override
    public boolean checkWithNewConstraint(Constraint c) {
        BooleanFormula b = adapter.transformConstraint(c);
        boolean result;
        try {
            result = !solver.isUnsatWithAssumptions(Collections.singleton(b));
        } catch (SolverException | InterruptedException e) {
            throw new MulibRuntimeException(e);
        }
        return result;
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
    public Object getLabel(Sprimitive var) {
        if (!isSatisfiable()) {
            throw new MulibRuntimeException("Must be satisfiable.");
        }
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
        Object result = toPrimitiveOrString(var, getCurrentModel().evaluate(f));
        assert result != null;
        return result;
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
        private final boolean treatSboolsAsInts;

        JavaSMTMulibAdapter(MulibConfig config, SolverContext context) {
            FormulaManager formulaManager = context.getFormulaManager();
            booleanFormulaManager = formulaManager.getBooleanFormulaManager();
            integerFormulaManager = formulaManager.getIntegerFormulaManager();
            rationalFormulaManager = formulaManager.getRationalFormulaManager();
            this.treatSboolsAsInts = config.TREAT_BOOLEANS_AS_INTS;
        }

        Formula getFormulaForNumericExpression(NumericExpression numericExpression) {
            return numericExpressionStore.get(numericExpression);
        }

        BooleanFormula getBooleanFormulaForConstraint(Constraint c) {
            return booleanFormulaStore.get(c);
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
            if (a instanceof Gt) {
                fpCase = rationalFormulaManager::greaterThan;
                integerCase = integerFormulaManager::greaterThan;
            } else if (a instanceof Eq) {
                fpCase = rationalFormulaManager::equal;
                integerCase = integerFormulaManager::equal;
            } else if (a instanceof Lt) {
                fpCase = rationalFormulaManager::lessThan;
                integerCase = integerFormulaManager::lessThan;
            } else if (a instanceof Gte) {
                fpCase = rationalFormulaManager::greaterOrEquals;
                integerCase = integerFormulaManager::greaterOrEquals;
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
                if (n instanceof Sint) {
                    result = transformSintegerNumber((Sint) n);
                } else if (n instanceof Sfpnumber) {
                    result = transformSfpnumber((Sfpnumber) n);
                } else if (n instanceof Slong) {
                    result = _transformSnumber(
                            (Slong) n,
                            () -> n instanceof Slong.ConcSlong,
                            () -> n instanceof Slong.SymSlong,
                            () -> integerFormulaManager.makeNumber(((Slong.ConcSlong) n).longVal()),
                            () -> integerFormulaManager.makeVariable(((Slong.SymSlong) n).getInternalName())
                    );
                } else {
                    throw new NotYetImplementedException();
                }
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
                        () -> rationalFormulaManager.makeVariable(f.getInternalName())
                );
            } else if (f instanceof Sfloat) {
                result = _transformSnumber(
                        f,
                        () -> f instanceof Sfloat.ConcSfloat,
                        () -> f instanceof Sfloat.SymSfloat,
                        () -> rationalFormulaManager.makeNumber(((Sfloat.ConcSfloat) f).floatVal()),
                        () -> rationalFormulaManager.makeVariable(f.getInternalName())
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
            if (i instanceof Sshort) {
                result = _transformSnumber(
                        i,
                        () -> i instanceof Sshort.ConcSshort,
                        () -> i instanceof Sshort.SymSshort,
                        () -> integerFormulaManager.makeNumber(((Sshort.ConcSshort) i).shortVal()),
                        () -> integerFormulaManager.makeVariable(i.getInternalName())
                );
            } else if (i instanceof Sbyte) {
                result = _transformSnumber(
                        i,
                        () -> i instanceof Sbyte.ConcSbyte,
                        () -> i instanceof Sbyte.SymSbyte,
                        () -> integerFormulaManager.makeNumber(((Sbyte.ConcSbyte) i).intVal()),
                        () -> integerFormulaManager.makeVariable(i.getInternalName())
                );
            } else if (i instanceof Sbool) {
                if (!treatSboolsAsInts) {
                    throw new MulibRuntimeException("Must not occur");
                }
                return _transformSnumber(
                        i,
                        () -> i instanceof Sbool.ConcSbool,
                        () -> i instanceof Sbool.SymSbool,
                        () -> integerFormulaManager.makeNumber(((Sbool.ConcSbool) i).intVal()),
                        () -> integerFormulaManager.makeVariable(i.getInternalName() + "_int")
                );
            } else {
                result = _transformSnumber(
                        i,
                        () -> i instanceof Sint.ConcSint,
                        () -> i instanceof Sint.SymSint,
                        () -> integerFormulaManager.makeNumber(((Sint.ConcSint) i).intVal()),
                        () -> integerFormulaManager.makeVariable(i.getInternalName())
                );
            }
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
                    result = booleanFormulaManager.makeVariable(b.getInternalName());
                } else {
                    result = transformConstraint(representedConstraint);
                }
            } else {
                throw new NotYetImplementedException();
            }
            booleanFormulaStore.put(b, result);
            return result;
        }
    }
}