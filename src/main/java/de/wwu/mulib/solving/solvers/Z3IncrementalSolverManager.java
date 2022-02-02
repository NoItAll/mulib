package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.UnknownSolutionException;
import de.wwu.mulib.substitutions.primitives.Sbool;

public final class Z3IncrementalSolverManager extends AbstractZ3SolverManager {

    public Z3IncrementalSolverManager(MulibConfig config) {
        super(config);
    }

    @Override
    public boolean checkWithNewArraySelectConstraint(ArrayConstraint ac) {
        BoolExpr expr = newArraySelectConstraint(incrementalSolverState.getCurrentArrayRepresentation(ac.getArrayId()), ac.getIndex(), ac.getValue());
        // We assume all other constraints have already been added
        Status solverStatus = solver.check(expr);
        if (solverStatus == Status.UNKNOWN) {
            throw new UnknownSolutionException("Z3 cannot calculate a solution for the given constraints: "
                    + solver.getReasonUnknown());
        }
        return solverStatus == Status.SATISFIABLE;
    }

    @Override
    public boolean checkWithNewConstraint(Constraint constraint) {
        if (constraint instanceof Sbool.ConcSbool) return ((Sbool.ConcSbool) constraint).isTrue();
        // We assume all other constraints have already been added
        Status solverStatus = solver.check(adapter.transformConstraint(constraint));
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
    protected void addSolverConstraintRepresentation(Constraint constraint) {
        BoolExpr boolExpr = adapter.transformConstraint(constraint);
        solver.add(boolExpr);
    }
}
