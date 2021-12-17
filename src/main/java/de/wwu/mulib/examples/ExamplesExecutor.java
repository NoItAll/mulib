package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.examples.sac22_mulib_benchmark.*;

import java.util.List;

import static de.wwu.mulib.search.executors.SearchStrategy.*;

public class ExamplesExecutor {
    private ExamplesExecutor(){}

    public static void main(String[] args) {
        System.out.println("Starting...");
        int numberIterations = args.length > 2 ? Integer.parseInt(args[2]) : 35;
        for (int i = 0; i < numberIterations; i++) {
            MulibConfig.MulibConfigBuilder b = MulibConfig.builder()
                    // For the benchmark, we to not validate the classes nor do we write them as class files. Rather,
                    // they are only loaded and used.
                    .setTRANSF_VALIDATE_TRANSFORMATION(false)
                    .setTRANSF_WRITE_TO_FILE(false);
            String chosenMethod = args[0];
            String chosenConfig = args[1];
            switch (chosenConfig) {
                case "DFSN":
                    b.setGLOBAL_SEARCH_STRATEGY(DFS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS, DFS)
                            .setGLOBAL_SOLVER_TYPE(Solvers.Z3_NON_INCREMENTAL);
                    break;
                case "DFS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                            .setGLOBAL_SEARCH_STRATEGY(DFS);
                    break;
                case "PDFS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                            .setGLOBAL_SEARCH_STRATEGY(DFS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS, DFS);
                    break;
                case "PDSAS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3)
                            .setGLOBAL_SEARCH_STRATEGY(DSAS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
                            // Direct access makes sense for request of DSAS
                            .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS);
                    break;
                default:
                    throw new IllegalStateException();
            }
            List<PathSolution> pathSolutions;
            switch (chosenMethod) {
                case "NQ":
                    pathSolutions = runNQueens(b);
                    break;
                case "WBS":
                    pathSolutions = runWBS(b);
                    break;
                case "P3":
                    pathSolutions = runPartition3(b);
                    break;
                case "H":
                    pathSolutions = runHanoi(b);
                    break;
                case "TSP":
                    pathSolutions = runTsp(b);
                    break;
                case "GC":
                    pathSolutions = runGraphColoring(b);
                    break;
                default:
                    throw new IllegalStateException();
            }
            System.out.println(pathSolutions.size());
        }
        System.out.println("Search ended.");
    }

    private static List<PathSolution> runNQueens(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib(
                "solve",
                NQueens.class,
                builder
        );
    }

    private static List<PathSolution> runPartition3(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib("exec", Partition3.class, builder);
    }

    private static List<PathSolution> runWBS(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib("launch", WBS.class, builder);
    }

    private static List<PathSolution> runHanoi(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib("exec", SatHanoi01.class, builder);
    }

    private static List<PathSolution> runTsp(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulibWithoutTransformation("exec", TspSolver.class, builder);
    }

    private static List<PathSolution> runGraphColoring(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib("exec", GraphColoring.class, builder);
    }
}
