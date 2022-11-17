package de.wwu.mulib.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
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
import de.wwu.mulib.solving.Solvers;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.wwu.mulib.search.executors.SearchStrategy.DFS;
import static de.wwu.mulib.search.executors.SearchStrategy.DSAS;

public final class ExamplesExecutor {
    private ExamplesExecutor(){}

    private static boolean runChecks = true;

    public static void main(String[] args) {
        Mulib.log.log(Level.INFO, "Starting...");
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
                            args[3].equals("HL") ? b.setHIGH_LEVEL_FREE_ARRAY_THEORY(true) : b.setHIGH_LEVEL_FREE_ARRAY_THEORY(false));
                    break;
                case "MCAP":
                    pathSolutions = runMachineContainerEncodingCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                case "MPCAP":
                    pathSolutions = runMultiPeriodCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                case "MPMCAP":
                    pathSolutions = runMultiPeriodMachineContainerCapacityAssignment(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                case "DLSPV":
                    pathSolutions = runDlspVariant(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                case "MCAPMF":
                    pathSolutions = runMachineContainerEncodingCapacityAssignmentMutateFieldValues(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                case "MPMCAPMF":
                    pathSolutions = runMultiPeriodMachineContainerCapacityAssignmenMutateFieldValues(
                            args[3].equals("E") ? b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(true) : b.setUSE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(false));
                    break;
                default:
                    throw new IllegalStateException();
            }
            Mulib.log.log(Level.INFO, "" + pathSolutions.size());
        }
        Mulib.log.log(Level.INFO, "Search ended.");
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

    private static List<PathSolution> runPrimitiveEncodingCapacityAssignment(MulibConfig.MulibConfigBuilder builder) {
        builder.setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        int[] machines = new int[] { 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2, 5, 3, 2 };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        List<PathSolution> ps = Mulib.executeMulib(
                "assign",
                CapacityAssignmentProblem.class,
                builder,
                machines,
                workloads
        );
        if (ps.size() != 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            int[] result = (int[]) ps.get(0).getSolution().returnValue;
            Mulib.log.log(Level.INFO, Arrays.toString(result));
            if (result.length != workloads.length) {
                throw new MulibIllegalStateException();
            }
            for (int i = 0; i < result.length; i++) {
                machines[result[i]] = machines[result[i]] - workloads[i];
                if (machines[result[i]] < 0) {
                    throw new MulibIllegalStateException();
                }
            }
            Mulib.log.log(Level.INFO, Arrays.toString(machines));
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMachineContainerEncodingCapacityAssignment(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        List<PathSolution> ps = Mulib.executeMulib(
                "assign",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );

        if (ps.size() != 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            int[] result = (int[]) ps.get(0).getSolution().returnValue;
            Mulib.log.log(Level.INFO,Arrays.toString(result));
            for (int i = 0; i < result.length; i++) {
                machines[result[i]].i = machines[result[i]].i - workloads[i];
                if (machines[result[i]].i < 0) {
                    throw new MulibIllegalStateException();
                }
            }
            Mulib.log.log(Level.INFO, Arrays.stream(machines).map(m -> m.i).collect(Collectors.toList()).toString());
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMachineContainerEncodingCapacityAssignmentMutateFieldValues(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[] workloads = new int[] { 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3, 1, 2, 4, 3 };
        List<PathSolution> ps = Mulib.executeMulib(
                "assignMutateFieldValue",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );

        if (ps.size() != 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            int[] result = (int[]) ps.get(0).getSolution().returnValue;
            Mulib.log.log(Level.INFO,Arrays.toString(result));
            for (int i = 0; i < result.length; i++) {
                machines[result[i]].i = machines[result[i]].i - workloads[i];
                if (machines[result[i]].i < 0) {
                    throw new MulibIllegalStateException();
                }
            }
            Mulib.log.log(Level.INFO, Arrays.stream(machines).map(m -> m.i).collect(Collectors.toList()).toString());
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodCapacityAssignment(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        int[] machines = new int[] { 5, 3, 2, 5, 3, 2, 5, 3, 2 };
        int[][] twoPeriodsWorkloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        List<PathSolution> ps = Mulib.executeMulib(
                "assignWithPreproduction",
                CapacityAssignmentProblem.class,
                builder,
                machines,
                twoPeriodsWorkloads
        );
        if (ps.size() < 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            for (PathSolution p : ps) {
                int[][][] result = (int[][][]) p.getSolution().returnValue;
                Mulib.log.log(
                        Level.INFO,
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
                Mulib.log.log(
                        Level.INFO,
                        Arrays.stream(machinesForTwoPeriods)
                                .map(Arrays::toString)
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodMachineContainerCapacityAssignment(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[][] workloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        List<PathSolution> ps = Mulib.executeMulib(
                "assignWithPreproduction",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        if (ps.size() < 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            for (PathSolution p : ps) {
                int[][][] result = (int[][][]) p.getSolution().returnValue;
                Mulib.log.log(
                        Level.INFO,
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
                Mulib.log.log(
                        Level.INFO,
                        Arrays.stream(machinesForTwoPeriods)
                                .map(m -> Arrays.toString(Arrays.stream(m).map(e -> e.i).toArray(Integer[]::new)))
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    private static List<PathSolution> runMultiPeriodMachineContainerCapacityAssignmenMutateFieldValues(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
        Machine[] machines = new Machine[] {
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2),
                new Machine(5), new Machine(3), new Machine(2)
        };
        int[][] workloads = new int[][] { { 1, 4, 3, 1, 1, 4, 3, 1, 1, 4, 3, 1  }, { 1, 5, 2, 3, 1, 5, 2, 3, 1, 5, 2, 3 } };
        List<PathSolution> ps = Mulib.executeMulib(
                "assignWithPreproductionMutateFieldValue",
                MachineCAP.class,
                builder,
                machines,
                workloads
        );
        if (ps.size() < 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            for (PathSolution p : ps) {
                int[][][] result = (int[][][]) p.getSolution().returnValue;
                Mulib.log.log(
                        Level.INFO,
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
                Mulib.log.log(
                        Level.INFO,
                        Arrays.stream(machinesForTwoPeriods)
                                .map(m -> Arrays.toString(Arrays.stream(m).map(e -> e.i).toArray(Integer[]::new)))
                                .collect(Collectors.toList()).toString()
                );
            }
        }
        return ps;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static List<PathSolution> runDlspVariant(MulibConfig.MulibConfigBuilder builder) {
        builder.setHIGH_LEVEL_FREE_ARRAY_THEORY(true).setENLIST_LEAVES(true).setLOG_TIME_FOR_EACH_PATH_SOLUTION(true);
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
        List<PathSolution> ps = Mulib.executeMulib(
                "assign",
                DlspVariant.class,
                builder,
                machines,
                products
        );
        if (ps.size() != 1 || ps.get(0) instanceof ExceptionPathSolution) {
            throw new MulibIllegalStateException();
        }
        if (runChecks) {
            DlspVariant.Product[][] result = (DlspVariant.Product[][]) ps.get(0).getSolution().returnValue;
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
                Mulib.log.log(
                        Level.INFO,
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
