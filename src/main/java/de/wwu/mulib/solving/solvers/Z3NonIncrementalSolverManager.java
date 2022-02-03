package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.UnknownSolutionException;

import java.util.ArrayDeque;

public class Z3NonIncrementalSolverManager extends AbstractZ3SolverManager {
    protected ArrayDeque<BoolExpr> expressions;

    public Z3NonIncrementalSolverManager(MulibConfig config) {
        super(config);
        this.expressions = new ArrayDeque<>();
    }

    @Override
    protected void addSolverConstraintRepresentation(BoolExpr boolExpr) {
        // We add the solver's constraint representation after incrementing the level. If the level is still
        // equal to the number of expressions, a constraint has been added without pushing
        if (expressions.size() == getLevel()) {
            // Add the modified constraint instead
            boolExpr = transformConstraint(_getConstraints().peek());
            expressions.pop();
        }
        expressions.push(boolExpr);
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        Status solverStatus = solver.check(expressions.toArray(new BoolExpr[0]));
        if (solverStatus == Status.UNKNOWN) {
            throw new UnknownSolutionException("Z3 cannot calculate a solution for the given constraints: "
                    + solver.getReasonUnknown());
        }
        return solverStatus == Status.SATISFIABLE;
    }

    @Override
    protected void solverSpecificBacktrackingPoint() {
        // Nothing to do here
    }

    @Override
    protected void solverSpecificBacktrackOnce() {
        expressions.pop();
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        for (int i = 0; i < toBacktrack; i++) {
            expressions.pop();
        }
    }

    @Override
    protected boolean calculateSatisfiabilityWithSolverBoolRepresentation(BoolExpr boolExpr) {
        expressions.addLast(boolExpr);
        boolean result = calculateIsSatisfiable();
        expressions.removeLast();
        return result;
    }
}
