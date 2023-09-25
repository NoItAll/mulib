package de.wwu.mulib.solving;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.solving.solvers.*;
import de.wwu.mulib.throwables.NotYetImplementedException;

/**
 * Enumeration of all constraint solvers supported by Mulib
 */
public enum Solvers {
    /**
     * Incremental version of Z3
     * @see Z3IncrementalSolverManager
     */
    Z3_INCREMENTAL,
    /**
     * Global version of Z3,
     * @see Z3GlobalLearningSolverManager
     */
    Z3_GLOBAL_LEARNING,
    JACOP,
    /**
     * Z3 via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_Z3,
    /**
     * SMTInterpol via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_SMTINTERPOL,
    /**
     * Princess via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_PRINCESS,
    /**
     * CVC4 via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_CVC4,
    /**
     * CVC5 via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_CVC5,
    /**
     * MathSat5 via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_MATHSAT5,
    /**
     * YICES2 via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_YICES2,
    /**
     * Boolector via JavaSMT
     * @see JavaSMTSolverManager
     */
    JSMT_BOOLECTOR,
    /**
     * CP-SAT
     */
    CP_SAT;

    /**
     * @param config The configuration
     * @return The solver manager according to the configuration
     */
    public static SolverManager getSolverManager(MulibConfig config) {
        switch (config.SOLVER_GLOBAL_TYPE) {
            case Z3_INCREMENTAL:
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
            case CP_SAT:
                return new CpSatSolverManager(config);
            default:
                throw new NotYetImplementedException();
        }
    }
}
