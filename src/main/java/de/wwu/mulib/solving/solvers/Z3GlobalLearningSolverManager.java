package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.UnknownSolutionException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Non-incremental version of the Z3 constraint solver. Instead of using scope, we push all constraints into a global
 * scope. For backtracking etc. we push those constraints not as-are but with an simple boolean antecedent in an implication.
 * On backtracking, these bools are deactivated. For as long as the constraint solver has them "in a virtual scope"
 * the antecedent is forced to be on.
 * By keeping all constraints in a global scope, the constraint solver still can learn lemmas which might speed up, e.g.,
 * {@link de.wwu.mulib.search.executors.SearchStrategy#BFS} and {@link de.wwu.mulib.search.executors.SearchStrategy#IDDFS}
 * but is costly in terms of memory.
 */
public class Z3GlobalLearningSolverManager extends AbstractZ3SolverManager {
    private final ArrayDeque<BoolExpr> expressions;
    private final ArrayDeque<BoolExpr> boolImpliers;
    private final Map<BoolExpr, BoolExpr> impliedBy;
    private long boolImplyId = 0;

    /**
     * @param config The configuration
     */
    public Z3GlobalLearningSolverManager(MulibConfig config) {
        super(config);
        this.expressions = new ArrayDeque<>();
        this.boolImpliers = new ArrayDeque<>();
        this.impliedBy = new HashMap<>();
    }

    @Override
    protected void addSolverConstraintRepresentation(BoolExpr boolExpr) {
        // We add the solver's constraint representation after incrementing the level. If the level is still
        // equal to the number of expressions, a constraint has been added without pushing
        if (expressions.size() == getLevel()) {
            // Add the modified constraint instead: boolExpr will be conjoined by the constraint before
            boolExpr = adapter.ctx.mkAnd(boolExpr, expressions.peek());
            expressions.pop();
            boolImpliers.pop();
        }
        expressions.push(boolExpr);
        BoolExpr implies = impliedBy.get(boolExpr);
        if (implies == null) {
            implies = adapter.ctx.mkBoolConst("implier_" + boolImplyId++);
            solver.add(adapter.ctx.mkOr(adapter.ctx.mkNot(implies), boolExpr));
            impliedBy.put(boolExpr, implies);
        }
        boolImpliers.push(implies);
        assert expressions.size() == getLevel();
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        Status solverStatus = solver.check(boolImpliers.toArray(new BoolExpr[0]));
        if (solverStatus == Status.UNKNOWN) {
            throw new UnknownSolutionException("Z3 cannot calculate a solution for the given constraints: "
                    + solver.getReasonUnknown());
        }
        return solverStatus == Status.SATISFIABLE;
    }

    @Override
    protected void solverSpecificBacktrackingPoint() {
        // Nothing to do here since we always stay on the current level in addSolverConstraintRepresentation(BoolExpr)
    }

    @Override
    protected void solverSpecificBacktrackOnce() {
        expressions.pop();
        boolImpliers.pop();
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        for (int i = 0; i < toBacktrack; i++) {
            expressions.pop();
            boolImpliers.pop();
        }
    }

    @Override
    protected boolean calculateSatisfiabilityWithSolverBoolRepresentation(BoolExpr boolExpr) {
        boolImpliers.addLast(boolExpr);
        boolean result = calculateIsSatisfiable();
        boolImpliers.removeLast();
        return result;
    }
}
