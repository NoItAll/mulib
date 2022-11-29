package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.MulibContext;
import de.wwu.mulib.examples.free_arrays.CapacityAssignmentProblem;
import de.wwu.mulib.examples.free_arrays.DlspVariant;
import de.wwu.mulib.examples.free_arrays.MachineCAP;
import de.wwu.mulib.examples.free_arrays.MachineCAP.Machine;
import de.wwu.mulib.examples.sac22_mulib_benchmark.GraphColoring;
import de.wwu.mulib.examples.sac22_mulib_benchmark.NQueens;
import de.wwu.mulib.examples.sac22_mulib_benchmark.Partition3;
import de.wwu.mulib.examples.sac22_mulib_benchmark.TspSolver;
import de.wwu.mulib.examples.sac22_mulib_benchmark.hanoi.SatHanoi01;
import de.wwu.mulib.examples.sac22_mulib_benchmark.wbs.WBS;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.Solution;
import de.wwu.mulib.solving.Solvers;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.wwu.mulib.search.executors.SearchStrategy.DFS;
import static de.wwu.mulib.search.executors.SearchStrategy.DSAS;

public final class ExamplesExecutor {
    private ExamplesExecutor(){}

    private static final boolean runChecks = false;

    public static void main(String[] args) {
        Mulib.setLogLevel(Level.FINE);
        Mulib.log.info("Starting...");
        String chosenMethod = args[0];
        String chosenConfig = args[1];
        int numberIterations = args.length > 2 ? Integer.parseInt(args[2]) : 35;
        for (int i = 0; i < numberIterations; i++) {
            MulibConfig.MulibConfigBuilder b = MulibConfig.builder()
                    // For the benchmark, we to not validate the classes nor do we write them as class files. Rather,
                    // they are only loaded and used.
                    .setTRANSF_VALIDATE_TRANSFORMATION(false)
                    .setTRANSF_WRITE_TO_FILE(false);
            switch (chosenConfig) {
                case "DFSN":
                    b.setGLOBAL_SEARCH_STRATEGY(DFS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS, DFS)
                            .setGLOBAL_SOLVER_TYPE(Solvers.Z3_GLOBAL_LEARNING); // Improved implementation compared to the SAC benchmark!
                    break;
                case "DFS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                            .setGLOBAL_SEARCH_STRATEGY(DFS);
                    break;
                case "PDFS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                            .setGLOBAL_SEARCH_STRATEGY(DFS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DFS, DFS, DFS);
                    break;
                case "PDSAS":
                    b.setGLOBAL_SOLVER_TYPE(Solvers.Z3_INCREMENTAL)
                            .setGLOBAL_SEARCH_STRATEGY(DSAS)
                            .setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(DSAS, DSAS, DSAS)
                            // Direct access makes sense for request of DSAS
                            .setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques.DIRECT_ACCESS);
                    break;
                default:
                    throw new IllegalStateException();
            }
            boolean computeSolutions = args.length > 4 && args[4].equals("S");
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
                case "PCAP":
                    pathSolutions = runPrimitiveEncodingCapacityAssignment(
                            args[3].equals("HL") ? b.setHIGH_LEVEL_FREE_ARRAY_THEORY(true) : b.setHIGH_LEVEL_FREE_ARRAY_THEORY(false),
                            computeSolutions);
                    break;
                case "MCAP":
                    pathSolutions = runMachineContainerEncodingCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MCAP_REDUCED":
                    pathSolutions = runMachineContainerEncodingCapacityAssignmentReduced(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MPCAP":
                    pathSolutions = runMultiPeriodCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MPMCAP":
                    pathSolutions = runMultiPeriodMachineContainerCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MPMCAP_SPA":
                    pathSolutions = runMultiPeriodMachineContainerCapacityAssignmentSparser(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "DLSPV":
                    pathSolutions = runDlspVariant(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MCAPMF":
                    pathSolutions = runMachineContainerEncodingCapacityAssignmentMutateFieldValues(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                case "MPMCAPMF":
                    pathSolutions = runMultiPeriodMachineContainerCapacityAssignmenMutateFieldValues(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false),
                            computeSolutions);
                    break;
                default:
                    throw new IllegalStateException();
            }
            if (pathSolutions != null) { Mulib.log.info("" + pathSolutions.size()); }
        }
        Mulib.log.info("Search ended.");
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
        builder.setTRANSF_TRANSFORMATION_REQUIRED(false);
        return Mulib.executeMulib("exec", TspSolver.class, builder);
    }

    private static List<PathSolution> runGraphColoring(MulibConfig.MulibConfigBuilder builder) {
        return Mulib.executeMulib("exec", GraphColoring.class, builder);
    }

    private static List<PathSolution> runPrimitiveEncodingCapacityAssignment(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        int[] machines = new int[] { 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2 };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        MulibContext mc = Mulib.getMulibContext(
                "assign",
                CapacityAssignmentProblem.class,
                builder,
                machines,
                workloads
        );
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() != 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[] _machines = new int[machines.length];
                System.arraycopy(machines, 0, _machines, 0, machines.length);
                int[] result = (int[]) s.returnValue;
                Mulib.log.info(Arrays.toString(result));
                if (result.length != workloads.length) {
                    throw new MulibIllegalStateException();
                }
                for (int i = 0; i < result.length; i++) {
                    _machines[result[i]] = _machines[result[i]] - workloads[i];
                    if (_machines[result[i]] < 0) {
                        throw new MulibIllegalStateException();
                    }
                }
                Mulib.log.info(Arrays.toString(_machines));
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMachineContainerEncodingCapacityAssignment(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        MulibContext mc = Mulib.getMulibContext(
                "assign",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                Machine[] _machines = MachineCAP.copy(machines);
                int[] result = (int[]) s.returnValue;
                Mulib.log.info(Arrays.toString(result));
                for (int i = 0; i < result.length; i++) {
                    _machines[result[i]].i = _machines[result[i]].i - workloads[i];
                    if (_machines[result[i]].i < 0) {
                        throw new MulibIllegalStateException();
                    }
                }
                Mulib.log.info(Arrays.stream(_machines).map(m -> m.i).collect(Collectors.toList()).toString());
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMachineContainerEncodingCapacityAssignmentReduced(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3)
        };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 4, 3 };
        MulibContext mc = Mulib.getMulibContext(
                "assign",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[] result = (int[]) s.returnValue;
                Machine[] _machines = MachineCAP.copy(machines);
                Mulib.log.info(Arrays.toString(result));
                for (int i = 0; i < result.length; i++) {
                    _machines[result[i]].i = _machines[result[i]].i - workloads[i];
                    if (_machines[result[i]].i < 0) {
                        throw new MulibIllegalStateException();
                    }
                }
                Mulib.log.info(Arrays.stream(_machines).map(m -> m.i).collect(Collectors.toList()).toString());
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMachineContainerEncodingCapacityAssignmentMutateFieldValues(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        MulibContext mc = Mulib.getMulibContext(
                "assignMutateFieldValue",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[] result = (int[]) s.returnValue;
                Mulib.log.info(Arrays.toString(result));
                for (int i = 0; i < result.length; i++) {
                    machines[result[i]].i = machines[result[i]].i - workloads[i];
                    if (machines[result[i]].i < 0) {
                        throw new MulibIllegalStateException();
                    }
                }
                Mulib.log.info(Arrays.stream(machines).map(m -> m.i).collect(Collectors.toList()).toString());
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodCapacityAssignment(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        int[] machines = new int[] { 5, 3, 2, 5, 3, 2, 5, 3, 2 };
        int[][] twoPeriodsWorkloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        MulibContext mc = Mulib.getMulibContext(
                "assignWithPreproduction",
                CapacityAssignmentProblem.class,
                builder,
                machines,
                twoPeriodsWorkloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, twoPeriodsWorkloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, twoPeriodsWorkloads);
            Mulib.log.info("#Sols " + sols.size());
        }
        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[][][] result = (int[][][]) s.returnValue;
                Mulib.log.info(
                        Arrays.stream(result).map(iis ->
                                        Arrays.stream(iis)
                                                .map(Arrays::toString)
                                                .reduce((s0, s1) -> s0 + "," + s1))
                                .collect(Collectors.toList()).toString()
                );
                // Generate array of machines for both periods;
                int[][] machinesForTwoPeriods = new int[twoPeriodsWorkloads.length][];
                for (int i = 0; i < twoPeriodsWorkloads.length; i++) {
                    machinesForTwoPeriods[i] = new int[machines.length];
                    for (int j = 0; j < machines.length; j++) {
                        machinesForTwoPeriods[i][j] = machines[j];
                    }
                }
                for (int i = 0; i < twoPeriodsWorkloads.length; i++) {
                    for (int j = 0; j < twoPeriodsWorkloads[i].length; j++) {
                        int workload = twoPeriodsWorkloads[i][j];
                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]] =
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]] - workload;
                        if (machinesForTwoPeriods[result[i][j][0]][result[i][j][1]] < 0) {
                            throw new MulibIllegalStateException();
                        }
                    }
                }
                Mulib.log.info(
                        Arrays.stream(machinesForTwoPeriods)
                                .map(Arrays::toString)
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodMachineContainerCapacityAssignment(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[][] workloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        MulibContext mc = Mulib.getMulibContext(
                "assignWithPreproduction",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[][][] result = (int[][][]) s.returnValue;
                Mulib.log.info(
                        Arrays.stream(result).map(iis ->
                                        Arrays.stream(iis)
                                                .map(Arrays::toString)
                                                .reduce((s0, s1) -> s0 + "," + s1))
                                .collect(Collectors.toList()).toString()
                );
                // Generate array of machines for both periods;
                Machine[][] machinesForTwoPeriods = new Machine[workloads.length][];
                for (int i = 0; i < workloads.length; i++) {
                    machinesForTwoPeriods[i] = new Machine[machines.length];
                    for (int j = 0; j < machines.length; j++) {
                        machinesForTwoPeriods[i][j] = new Machine(machines[j].i);
                    }
                }
                for (int i = 0; i < workloads.length; i++) {
                    for (int j = 0; j < workloads[i].length; j++) {
                        int workload = workloads[i][j];
                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                        if (machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0) {
                            throw new MulibIllegalStateException();
                        }
                    }
                }
                Mulib.log.info(
                        Arrays.stream(machinesForTwoPeriods)
                                .map(m -> Arrays.toString(Arrays.stream(m).map(e -> e.i).toArray(Integer[]::new)))
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodMachineContainerCapacityAssignmentSparser(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(6), new Machine(7), new Machine(8),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[][] workloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 6, 7, 8 }, { 1, 5, 2, 3, 1, 5, 2, 3, 6, 7, 8 } };
        MulibContext mc = Mulib.getMulibContext(
                "assignWithPreproduction",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[][][] result = (int[][][]) s.returnValue;
                Mulib.log.info(
                        Arrays.stream(result).map(iis ->
                                        Arrays.stream(iis)
                                                .map(Arrays::toString)
                                                .reduce((s0, s1) -> s0 + "," + s1))
                                .collect(Collectors.toList()).toString()
                );
                // Generate array of machines for both periods;
                Machine[][] machinesForTwoPeriods = new Machine[workloads.length][];
                for (int i = 0; i < workloads.length; i++) {
                    machinesForTwoPeriods[i] = new Machine[machines.length];
                    for (int j = 0; j < machines.length; j++) {
                        machinesForTwoPeriods[i][j] = new Machine(machines[j].i);
                    }
                }
                for (int i = 0; i < workloads.length; i++) {
                    for (int j = 0; j < workloads[i].length; j++) {
                        int workload = workloads[i][j];
                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                        if (machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0) {
                            throw new MulibIllegalStateException();
                        }
                    }
                }
                Mulib.log.info(
                        Arrays.stream(machinesForTwoPeriods)
                                .map(m -> Arrays.toString(Arrays.stream(m).map(e -> e.i).toArray(Integer[]::new)))
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodMachineContainerCapacityAssignmenMutateFieldValues(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[][] workloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        MulibContext mc = Mulib.getMulibContext(
                "assignWithPreproductionMutateFieldValue",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, workloads);
        } else {
            sols = mc.getUpToNSolutions(10, machines, workloads);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            List<Solution> _sols = sols;
            if (!computeSolutions) {
                if (ps.size() < 1) {
                    throw new MulibIllegalStateException();
                }
                if (ps.stream().anyMatch(p -> p instanceof ExceptionPathSolution)) {
                    throw new MulibIllegalStateException();
                }
                _sols = ps.stream().map(PathSolution::getSolution).collect(Collectors.toList());
            }
            for (Solution s : _sols) {
                int[][][] result = (int[][][]) s.returnValue;
                Mulib.log.info(
                        Arrays.stream(result).map(iis ->
                                        Arrays.stream(iis)
                                                .map(Arrays::toString)
                                                .reduce((s0, s1) -> s0 + "," + s1))
                                .collect(Collectors.toList()).toString()
                );
                // Generate array of machines for both periods;
                Machine[][] machinesForTwoPeriods = new Machine[workloads.length][];
                for (int i = 0; i < workloads.length; i++) {
                    machinesForTwoPeriods[i] = new Machine[machines.length];
                    for (int j = 0; j < machines.length; j++) {
                        machinesForTwoPeriods[i][j] = new Machine(machines[j].i);
                    }
                }
                for (int i = 0; i < workloads.length; i++) {
                    for (int j = 0; j < workloads[i].length; j++) {
                        int workload = workloads[i][j];
                        machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i =
                                machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i - workload;
                        if (machinesForTwoPeriods[result[i][j][0]][result[i][j][1]].i < 0) {
                            throw new MulibIllegalStateException();
                        }
                    }
                }
                Mulib.log.info(
                        Arrays.stream(machinesForTwoPeriods)
                                .map(m -> Arrays.toString(Arrays.stream(m).map(e -> e.i).toArray(Integer[]::new)))
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static List<PathSolution> runDlspVariant(
            MulibConfig.MulibConfigBuilder builder,
            boolean computeSolutions) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_FIRST_PATH_SOLUTION(true).setSECONDS_PER_INVOCATION(30);
        DlspVariant.Machine[] machines = new DlspVariant.Machine[] {
                new DlspVariant.Machine(1, 4, new int[] { 1, 2, 3 }),
                new DlspVariant.Machine(2, 5, new int[] { 1, 4 }),
                new DlspVariant.Machine(3, 10, new int[] { 4, 5 }),
                new DlspVariant.Machine(4, 3, new int[] { 1, 2, 3, 4, 5 }),
                new DlspVariant.Machine(5, 2, new int[] { 1, 3, 5 }),
                new DlspVariant.Machine(6, 4, new int[] { 2, 4 }),
                new DlspVariant.Machine(7, 1, new int[] { 1, 2, 3, 4, 5 }),
                new DlspVariant.Machine(8, 2, new int[] { 3 }),
                new DlspVariant.Machine(9, 6, new int[] { 2, 5 })
        };
        DlspVariant.Product[][] products = new DlspVariant.Product[][] {
                { new DlspVariant.Product(1,1), new DlspVariant.Product(1,2), new DlspVariant.Product(4,4), new DlspVariant.Product(3,2), new DlspVariant.Product(1,5),
                        new DlspVariant.Product(4,9), new DlspVariant.Product(1,3), new DlspVariant.Product(2,5), new DlspVariant.Product(1,4),},
                { new DlspVariant.Product(4,4), new DlspVariant.Product(4,1), new DlspVariant.Product(2,6), new DlspVariant.Product(5,2), new DlspVariant.Product(2,4),
                        new DlspVariant.Product(4,5), new DlspVariant.Product(3,2), new DlspVariant.Product(4,3), new DlspVariant.Product(4,10) },
                { new DlspVariant.Product(1,1), new DlspVariant.Product(1,2), new DlspVariant.Product(4,4), new DlspVariant.Product(3,2), new DlspVariant.Product(1,5),
                        new DlspVariant.Product(4,9), new DlspVariant.Product(1,3), new DlspVariant.Product(2,5), new DlspVariant.Product(1,4),},
                { new DlspVariant.Product(4,4), new DlspVariant.Product(4,1), new DlspVariant.Product(2,6), new DlspVariant.Product(5,2), new DlspVariant.Product(2,4),
                        new DlspVariant.Product(4,5), new DlspVariant.Product(3,2), new DlspVariant.Product(4,3), new DlspVariant.Product(4,10) }
        };
        MulibContext mc = Mulib.getMulibContext(
                "assign",
                DlspVariant.class,
                builder,
                machines,
                products
        );
        List<PathSolution> ps = null;
        List<Solution> sols = null;
        if (!computeSolutions) {
            ps = mc.getAllPathSolutions(machines, products);
        } else {
            sols = mc.getUpToNSolutions(10, machines, products);
            Mulib.log.info("#Sols " + sols.size());
        }

        if (runChecks) {
            DlspVariant.Product[][] result;
            if (!computeSolutions) {
                if (ps.size() != 1 || ps.get(0) instanceof ExceptionPathSolution) {
                    throw new MulibIllegalStateException();
                }
                result = (DlspVariant.Product[][]) ps.get(0).getSolution().returnValue;
            } else {
                result = (DlspVariant.Product[][]) sols.get(0).returnValue;
            }
            for (DlspVariant.Product[] prodsForPeriod : result) {
                boolean[] seen = new boolean[prodsForPeriod.length + 1];
                for (int j = 0; j < prodsForPeriod.length; j++) {
                    DlspVariant.Product p = prodsForPeriod[j];
                    DlspVariant.Machine prod = p.producedBy;
                    if (prod.number == 0
                            || prod.productionCapacity < p.requiredCapacity
                            || Arrays.stream(prod.producibleProductTypes).noneMatch(type -> type == p.type)
                            || seen[prod.number]) {
                        throw new MulibIllegalStateException();
                    }
                    seen[prod.number] = true;
                }
                for (int j = 1; j < seen.length; j++) {
                    if (!seen[j]) {
                        throw new MulibIllegalStateException();
                    }
                }
                Mulib.log.info(
                        Arrays.stream(result)
                                .map(prods ->
                                        Arrays.stream(prods)
                                                .map(p -> "Product: type=" + p.type + ",reqCap=" + p.requiredCapacity + ",producedBy=" + p.producedBy.number + "\r\n")
                                                .collect(Collectors.toList())
                                ).collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }
}
