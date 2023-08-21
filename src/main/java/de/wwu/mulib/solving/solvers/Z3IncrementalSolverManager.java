package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.throwables.UnknownSolutionException;

/**
 * Incremental version of the Z3 constraint solver. Uses Z3's scopes for backtracking and is more space-efficient and,
 * for depth-first strategies, typically more performant than {@link Z3GlobalLearningSolverManager}.
 */
public final class Z3IncrementalSolverManager extends AbstractZ3SolverManager {

    /**
     * @param config The configuration
     */
    public Z3IncrementalSolverManager(MulibConfig config) {
        super(config);
    }

    @Override
    protected boolean calculateSatisfiabilityWithSolverBoolRepresentation(BoolExpr boolExpr) {
        // We assume all other constraints have already been added
        Status solverStatus = solver.check(boolExpr);
        if (solverStatus == Status.UNKNOWN) {
            throw new UnknownSolutionException("Z3 cannot calculate a solution for the given constraints: "
                    + solver.getReasonUnknown());
        }
        return solverStatus == Status.SATISFIABLE;
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        // We again assume that all other constraints have already been added
        Status solverStatus = solver.check();
        if (solverStatus == Status.UNKNOWN) {
            throw new UnknownSolutionException("Z3 cannot calculate a solution for the given constraints: "
                    + solver.getReasonUnknown());
        }
        return solverStatus == Status.SATISFIABLE;
    }

    @Override
    protected void solverSpecificBacktrackingPoint() {
        solver.push();
    }

    @Override
    protected void solverSpecificBacktrackOnce() {
        solverSpecificBacktrack(1);
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        solver.pop(toBacktrack);
    }

    @Override
    protected void addSolverConstraintRepresentation(BoolExpr boolExpr) {
        solver.add(boolExpr);
    }
}
