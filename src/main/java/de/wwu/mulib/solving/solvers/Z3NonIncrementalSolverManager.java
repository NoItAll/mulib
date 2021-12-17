package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.UnknownSolutionException;

import java.util.ArrayDeque;

public class Z3NonIncrementalSolverManager extends AbstractZ3SolverManager {
    protected ArrayDeque<Expr> expressions;

    public Z3NonIncrementalSolverManager(MulibConfig config) {
        super(config);
        this.expressions = new ArrayDeque<>();
    }

    @Override
    protected void addSolverConstraintRepresentation(Constraint constraint) {
        expressions.push(adapter.transformConstraint(constraint));
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        Status solverStatus = solver.check(expressions.toArray(new Expr[0]));
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
    public boolean checkWithNewConstraint(Constraint c) {
        expressions.addLast(adapter.transformConstraint(c));
        boolean result = isSatisfiable();
        expressions.removeLast();
        return result;
    }
}
