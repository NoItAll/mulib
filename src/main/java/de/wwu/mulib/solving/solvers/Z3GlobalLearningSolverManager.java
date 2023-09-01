package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.throwables.UnknownSolutionException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Non-incremental version of the Z3 constraint solver. Instead of using a scope for each
 * backtracking point, we push all constraints into a global scope.
 * For backtracking etc. we push those constraints not as-are but with an simple boolean antecedent in an implication, i.e.,
 * (boolean_leaf -> constraint).
 * For as long as the constraint solver has them "in a virtual scope" the antecedent (boolean_leaf) is forced to be on.
 * On backtracking, the antecedents are not enforced to be true any longer.
 * By keeping all constraints in a global scope, the constraint solver still can learn lemmas which might speed up, e.g.,
 * {@link de.wwu.mulib.search.executors.SearchStrategy#BFS} and {@link de.wwu.mulib.search.executors.SearchStrategy#IDDFS}
 * but is costly in terms of memory.
 */
public final class Z3GlobalLearningSolverManager extends AbstractZ3SolverManager {
    private final ArrayDeque<BoolExpr> boolImpliers;
    private final Map<BoolExpr, BoolExpr> impliedBy;
    private long boolImplyId = 0;

    /**
     * @param config The configuration
     */
    public Z3GlobalLearningSolverManager(MulibConfig config) {
        super(config);
        this.boolImpliers = new ArrayDeque<>();
        this.impliedBy = new HashMap<>();
    }

    @Override
    protected void addSolverConstraintRepresentation(BoolExpr boolExpr) {
        BoolExpr implies = impliedBy.get(boolExpr);
        if (implies == null) {
            // Instead of adding boolExpr, add implier -> boolExpr
            implies = adapter.ctx.mkBoolConst("implier_" + boolImplyId++);
            solver.add(adapter.ctx.mkImplies(implies, boolExpr));
            impliedBy.put(boolExpr, implies);
        }
        if (boolImpliers.size() == getLevel()) {
            // Conjoin the implier to the other impliers, if there
            // already are impliers for this level
            implies = adapter.ctx.mkAnd(boolImpliers.pop(), implies);
        }
        boolImpliers.push(implies);
        assert boolImpliers.size() == getLevel();
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
        boolImpliers.pop();
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        for (int i = 0; i < toBacktrack; i++) {
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
