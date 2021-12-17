package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.StdLabels;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.ConcSprimitive;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymSprimitive;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractZ3SolverManager extends AbstractIncrementalEnabledSolverManager<Model> {

    private static final Object syncObject = new Object();
    protected final Solver solver;
    protected final Z3MulibAdapter adapter;

    public AbstractZ3SolverManager(MulibConfig config) {
        super(config);
        synchronized (syncObject) {
            Context context = new Context();
            solver = context.mkSolver();
            adapter = new Z3MulibAdapter(context);
            currentModel = null;
        }
    }


    @Override
    protected Model calculateCurrentModel() {
        return solver.getModel();
    }

    @Override
    public Labels getLabels(Map<String, Object> labelsToVars) {
        Map<Sprimitive, Expr> primitives = adapter.getPrimitiveStore();
        Map<NumericExpression, Expr> numericExpressionExprMap = adapter.getNumericExpressionsCache();
        Map<Constraint, BoolExpr> boolExprMap = adapter.getBoolExprCache();
        Map<SubstitutedVar, Object> svariablesToValues = new HashMap<>();
        Map<String, Object> identifiersToValues = new HashMap<>();
        if (!isSatisfiable()) { // Explicit is sometimes needed to generate labeling.
            throw new MulibRuntimeException("Model from which labels should be derived is not satisfiable");
        }
        if (labelSymbolicValues && !labelsToVars.isEmpty()) {
            Model m = getCurrentModel();
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
                Expr val = primitives.get(p);
                if (val == null) { // TODO Refactor
                    val = numericExpressionExprMap.get(p);
                    if (val == null) {
                        val = boolExprMap.get(p);
                    }
                }
                if (val == null) {
                    if (p instanceof NumericExpression) {
                        val = adapter.transformNumericExpr((NumericExpression) p);
                    } else if (p instanceof Constraint) {
                        val = adapter.transformConstraint((Constraint) p);
                    }
                }
                Expr labeledExpr = m.eval(val, true);
                Object value = toPrimitiveOrString(labeledExpr);
                svariablesToValues.put(p, value);
                identifiersToValues.put(entry.getKey(), value);
            }
        }
        return new StdLabels(labelsToVars, svariablesToValues, identifiersToValues);
    }

    @Override
    public Object getLabel(SubstitutedVar var) {
        return toPrimitiveOrString(getCurrentModel().eval(adapter.getExprForPrimitive((Sprimitive) var), true)); // TODO More than just primitives, also in JavaSMTSolverManager
    }

    private static Object toPrimitiveOrString(Expr e) {
        if (e.isIntNum()) {
            return ((IntNum) e).getInt();
        } else if (e.isRatNum()) {
            RatNum ratNum = (RatNum) e;
            return ((double) ratNum.getNumerator().getInt64()) / ratNum.getDenominator().getInt64();
        } else if (e.isBool()) {
            return e.isTrue();
        } else {
            throw new NotYetImplementedException();
        }
    }

}
