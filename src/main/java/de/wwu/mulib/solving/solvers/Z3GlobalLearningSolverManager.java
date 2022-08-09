package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Status;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.UnknownSolutionException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class Z3GlobalLearningSolverManager extends AbstractZ3SolverManager {
    protected final ArrayDeque<BoolExpr> expressions;
    protected final ArrayDeque<BoolExpr> boolImpliers;
    protected final Map<BoolExpr, BoolExpr> impliedBy;
    private static long boolImplyId = 0;

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
            // Add the modified constraint instead
            boolExpr = transformConstraint(_getConstraints().peek());
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
