package de.wwu.mulib.solving;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.solving.solvers.JavaSMTSolverManager;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.solving.solvers.Z3GlobalLearningSolverManager;
import de.wwu.mulib.solving.solvers.Z3IncrementalSolverManager;

public enum Solvers {
    Z3,
    Z3_GLOBAL_LEARNING,
    JACOP,
    JSMT_Z3,
    JSMT_SMTINTERPOL,
    JSMT_PRINCESS,
    JSMT_CVC4,
    JSMT_MATHSAT5,
    JSMT_YICES2,
    JSMT_BOOLECTOR;

    public static SolverManager getSolverManager(MulibConfig config) {

        switch (config.GLOBAL_SOLVER_TYPE) {
            case Z3:
                return new Z3IncrementalSolverManager(config);
            case Z3_GLOBAL_LEARNING:
                return new Z3GlobalLearningSolverManager(config);
            case JSMT_Z3:
            case JSMT_SMTINTERPOL:
            case JSMT_PRINCESS:
            case JSMT_CVC4:
            case JSMT_MATHSAT5:
            case JSMT_YICES2:
            case JSMT_BOOLECTOR:
                return new JavaSMTSolverManager(config);
            default:
                throw new NotYetImplementedException();
        }
    }
}
