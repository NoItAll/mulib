package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.StdLabels;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.ConcSprimitive;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymSprimitive;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class JavaSMTSolverManager extends AbstractIncrementalEnabledSolverManager<Model> {

    private static final Object syncObject = new Object();
    private final SolverContext context;
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
                this.context = SolverContextFactory.createSolverContext(
                        config,
                        BasicLogManager.create(config),
                        shutdown.getNotifier(),
                        chosenSolver
                );
                this.adapter = new JavaSMTMulibAdapter(context);
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
            currentModel = null;
        } catch (InterruptedException e) {
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
        currentModel = null;
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

    protected Model calculateCurrentModel() {
        try {
            return solver.getModel();
        } catch (SolverException e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    public Labels getLabels(Map<String, Object> labelsToVars) {
        Map<Sprimitive, Formula> primitives = adapter.getPrimitiveStore();
        Map<SubstitutedVar, Object> svariablesToValues = new HashMap<>();
        Map<String, Object> identifiersToValues = new HashMap<>();
        if (!isSatisfiable()) { // Explicit is sometimes needed to generate labeling.
            throw new MulibRuntimeException("Model from which labels should be derived is not satisfiable");
        }
        if (labelSymbolicValues && !primitives.isEmpty()) {
            try {
                Model model = solver.getModel();
                for (Map.Entry<String, Object> entry : labelsToVars.entrySet()) {
                    if (entry.getValue() instanceof ConcSprimitive) {
                        ConcSprimitive p = (ConcSprimitive) entry.getValue();
                        svariablesToValues.put(p, p);
                        identifiersToValues.put(entry.getKey(), p);
                        continue;
                    } else if (!(entry.getValue() instanceof SymSprimitive)) {
                        throw new NotYetImplementedException();
                    }
                    SymSprimitive p = (SymSprimitive) entry.getValue();
                    Formula f = primitives.get(p);
                    if (f == null) {
                        if (p instanceof NumericExpression) {
                            f = adapter.transformNumeral((NumericExpression) p);
                        } else if (p instanceof Constraint) {
                            f = adapter.transformConstraint((Constraint) p);
                        }
                    }
                    Object value = model.evaluate(f);
                    value = toPrimitiveOrString(p, value);
                    svariablesToValues.put(p, value);
                    identifiersToValues.put(entry.getKey(), value);
                }
            } catch (SolverException e) {
                throw new MulibRuntimeException(e);
            }
        }
        return new StdLabels(labelsToVars, svariablesToValues, identifiersToValues);
    }

    @Override
    public Object getLabel(SubstitutedVar var) {
        return toPrimitiveOrString((Sprimitive) var, getCurrentModel().evaluate(adapter.getFormulaForPrimitive((Sprimitive) var)));
    }

    private static Object toPrimitiveOrString(Sprimitive p, Object o) {
        if (o instanceof BigInteger) {
            if (p instanceof Sint) {
                return ((BigInteger) o).intValue();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (o instanceof Boolean) {
            return o;
        } else {
            throw new NotYetImplementedException();
        }
    }
}