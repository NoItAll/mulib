package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.exceptions.UnknownSolutionException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.Labels;
import de.wwu.mulib.solving.StdLabels;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.ConcSprimitive;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sprimitive;
import de.wwu.mulib.substitutions.primitives.SymSprimitive;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Z3IncrementalSolverManager extends AbstractZ3SolverManager {

    public Z3IncrementalSolverManager(MulibConfig config) {
        super(config);
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
        currentModel = null;
    }

    @Override
    protected void addSolverConstraintRepresentation(Constraint constraint) {
        BoolExpr boolExpr = adapter.transformConstraint(constraint);
        solver.add(boolExpr);
    }
}
