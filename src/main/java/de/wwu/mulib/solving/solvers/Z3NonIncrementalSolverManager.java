package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.exceptions.UnknownSolutionException;

import java.util.ArrayDeque;

/** Currently not further maintained */
public class Z3NonIncrementalSolverManager extends AbstractZ3SolverManager {
    protected ArrayDeque<BoolExpr> expressions;

    public Z3NonIncrementalSolverManager(MulibConfig config) {
        super(config);
        this.expressions = new ArrayDeque<>();
    }

    @Override
    protected void addSolverConstraintRepresentation(Constraint constraint) {
        // We add the solver's constraint representation after incrementing the level. If the level is still
        // equal to the number of expressions, a constraint has been added without pushing!
        if (expressions.size() == getLevel()) {
            // Add the modified constraint instead
            constraint = incrementalSolverState.getConstraints().peek();
            expressions.pop();
        }
        expressions.push(adapter.transformConstraint(constraint));
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
    public boolean checkWithNewArraySelectConstraint(ArrayConstraint ac) {
        throw new NotYetImplementedException(); // TODO
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
