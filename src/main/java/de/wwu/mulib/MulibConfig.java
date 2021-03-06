package de.wwu.mulib;

import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiFunction;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class MulibConfig {

    /* Trees */
    public final String TREE_INDENTATION;
    public final boolean ENLIST_LEAVES;

    /* Search */
    public final SearchStrategy GLOBAL_SEARCH_STRATEGY;
    public final List<SearchStrategy> ADDITIONAL_PARALLEL_SEARCH_STRATEGIES;
    public final long PARALLEL_TIMEOUT_IN_MS;
    public final ChoiceOptionDeques CHOICE_OPTION_DEQUE_TYPE;
    public final Optional<Long> ACTIVATE_PARALLEL_FOR;
    public final boolean CONCOLIC;

    /* Values */
    public final Optional<Sint> SYMSINT_LB;
    public final Optional<Sint> SYMSINT_UB;

    public final Optional<Slong> SYMSLONG_LB;
    public final Optional<Slong> SYMSLONG_UB;

    public final Optional<Sdouble> SYMSDOUBLE_LB;
    public final Optional<Sdouble> SYMSDOUBLE_UB;

    public final Optional<Sfloat> SYMSFLOAT_LB;
    public final Optional<Sfloat> SYMSFLOAT_UB;

    public final Optional<Sshort> SYMSSHORT_LB;
    public final Optional<Sshort> SYMSSHORT_UB;

    public final Optional<Sbyte> SYMSBYTE_LB;
    public final Optional<Sbyte> SYMSBYTE_UB;

    public final boolean TREAT_BOOLEANS_AS_INTS;

    /* Solver */
    public final Solvers GLOBAL_SOLVER_TYPE;
    public final boolean GLOBAL_AVOID_SAT_CHECKS;
    public final boolean LABEL_RESULT_VALUE;

    /* Budget */
    public final Optional<Long> FIXED_POSSIBLE_CP_BUDGET;
    public final Optional<Long> FIXED_ACTUAL_CP_BUDGET;
    public final Optional<Long> INCR_ACTUAL_CP_BUDGET;

    public final Optional<Long> NANOSECONDS_PER_INVOCATION;
    public final Optional<Long> MAX_FAILS;
    public final Optional<Long> MAX_PATH_SOLUTIONS;
    public final Optional<Long> MAX_EXCEEDED_BUDGETS;

    /* Transformation */
    public final List<String> TRANSF_IGNORE_FROM_PACKAGES;
    public final List<Class<?>> TRANSF_IGNORE_CLASSES;
    public final List<Class<?>> TRANSF_IGNORE_SUBCLASSES_OF;
    public final List<Class<?>> TRANSF_REGARD_SPECIAL_CASE;
    public final List<Class<?>> TRANSF_CONCRETIZE_FOR;
    public final List<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
    public final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS;
    public final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS;
    public final Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
    public final boolean TRANSF_WRITE_TO_FILE;
    public final String TRANSF_GENERATED_CLASSES_PATH;
    public final boolean TRANSF_VALIDATE_TRANSFORMATION;
    // For the system classloader to work, it is required that TRANSF_GENERATED_CLASSES_PATH is set to the same root
    // folder of the usual system classes.
    public final boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
    public final boolean TRANSF_INCLUDE_PACKAGE_NAME;

    public static MulibConfigBuilder builder() {
        return new MulibConfigBuilder();
    }

    public static MulibConfig get() {
        return builder().build();
    }

    public final static class MulibConfigBuilder {
        private String TREE_INDENTATION;
        private boolean LABEL_RESULT_VALUE;
        private boolean ENLIST_LEAVES;
        private boolean CONCRETIZE_IF_NEEDED;
        private List<String> TRANSF_IGNORE_FROM_PACKAGES;
        private List<Class<?>> TRANSF_IGNORE_CLASSES;
        private List<Class<?>> TRANSF_IGNORE_SUBCLASSES_OF;
        private List<Class<?>> TRANSF_REGARD_SPECIAL_CASE;
        private List<Class<?>> TRANSF_CONCRETIZE_FOR;
        private List<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        private Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS;
        private Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS;
        private Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
        private SearchStrategy GLOBAL_SEARCH_STRATEGY;
        private boolean CONCOLIC;
        private List<SearchStrategy> ADDITIONAL_PARALLEL_SEARCH_STRATEGIES;
        private ChoiceOptionDeques CHOICE_OPTION_DEQUE_TYPE;
        private long ACTIVATE_PARALLEL_FOR;
        private Solvers GLOBAL_SOLVER_TYPE;
        private boolean GLOBAL_AVOID_SAT_CHECKS;
        private long FIXED_POSSIBLE_CP_BUDGET;
        private long FIXED_ACTUAL_CP_BUDGET;
        private long INCR_ACTUAL_CP_BUDGET;
        private long SECONDS_PER_INVOCATION;
        private long MAX_FAILS;
        private long MAX_PATH_SOLUTIONS;
        private long MAX_EXCEEDED_BUDGETS;
        private boolean TRANSF_WRITE_TO_FILE;
        private String TRANSF_GENERATED_CLASSES_PATH;
        private boolean TRANSF_VALIDATE_TRANSFORMATION;
        private boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        private boolean TRANSF_INCLUDE_PACKAGE_NAME;
        private long PARALLEL_TIMEOUT_IN_MS;
        private Optional<Integer> SYMSINT_LB;
        private Optional<Integer> SYMSINT_UB;
        private Optional<Long> SYMSLONG_LB;
        private Optional<Long> SYMSLONG_UB;
        private Optional<Double> SYMSDOUBLE_LB;
        private Optional<Double> SYMSDOUBLE_UB;
        private Optional<Float> SYMSFLOAT_LB;
        private Optional<Float> SYMSFLOAT_UB;
        private Optional<Short> SYMSSHORT_LB;
        private Optional<Short> SYMSSHORT_UB;
        private Optional<Byte> SYMSBYTE_LB;
        private Optional<Byte> SYMSBYTE_UB;
        private boolean TREAT_BOOLEANS_AS_INTS;

        private MulibConfigBuilder() {
            // Defaults
            this.CONCRETIZE_IF_NEEDED = true;
            this.LABEL_RESULT_VALUE = true;
            this.ENLIST_LEAVES = false;
            this.CONCOLIC = false;
            this.TREE_INDENTATION = "    ";
            this.GLOBAL_SEARCH_STRATEGY = SearchStrategy.DFS;
            this.GLOBAL_SOLVER_TYPE = Solvers.Z3;
            this.FIXED_POSSIBLE_CP_BUDGET = 0;
            this.FIXED_ACTUAL_CP_BUDGET =   0;
            this.INCR_ACTUAL_CP_BUDGET =    0;
            this.SECONDS_PER_INVOCATION =   0;
            this.MAX_FAILS =                0;
            this.MAX_PATH_SOLUTIONS =       0;
            this.MAX_EXCEEDED_BUDGETS =     0;
            this.ACTIVATE_PARALLEL_FOR =    2;
            this.GLOBAL_AVOID_SAT_CHECKS = true;
            this.CHOICE_OPTION_DEQUE_TYPE = ChoiceOptionDeques.SIMPLE;
            this.TRANSF_IGNORE_CLASSES = List.of(
                    Mulib.class
            );
            this.TRANSF_IGNORE_FROM_PACKAGES = List.of(
                    "java", "de.wwu.mulib.substitutions", "de.wwu.mulib.transformations", "de.wwu.mulib.exceptions",
                    "de.wwu.mulib.expressions", "de.wwu.mulib.search", "de.wwu.mulib.solving"
            );
            this.TRANSF_CONCRETIZE_FOR = List.of(
            );
            this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = List.of(
                    PrintStream.class
            );
            this.TRANSF_IGNORE_SUBCLASSES_OF = List.of(
            );
            this.TRANSF_REGARD_SPECIAL_CASE = List.of(
            );
            this.TRANSF_WRITE_TO_FILE = true;
            this.TRANSF_GENERATED_CLASSES_PATH = "build/classes/java/";
            this.TRANSF_INCLUDE_PACKAGE_NAME = false;
            this.TRANSF_VALIDATE_TRANSFORMATION = false;
            this.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS = new HashMap<>();
            this.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS = new HashMap<>();
            this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS = new HashMap<>();
            this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = false;
            this.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES = Collections.emptyList();
            this.PARALLEL_TIMEOUT_IN_MS = 5000;
            this.SYMSINT_LB =    Optional.empty();
            this.SYMSINT_UB =    Optional.empty();
            this.SYMSDOUBLE_LB = Optional.empty();
            this.SYMSDOUBLE_UB = Optional.empty();
            this.SYMSFLOAT_LB =  Optional.empty();
            this.SYMSFLOAT_UB =  Optional.empty();
            this.SYMSLONG_LB =   Optional.empty();
            this.SYMSLONG_UB =   Optional.empty();
            this.SYMSSHORT_LB =  Optional.empty();
            this.SYMSSHORT_UB =  Optional.empty();
            this.SYMSBYTE_LB =   Optional.empty();
            this.SYMSBYTE_UB =   Optional.empty();
            this.TREAT_BOOLEANS_AS_INTS = false;
        }

        public MulibConfigBuilder setENLIST_LEAVES(boolean ENLIST_LEAVES) {
            this.ENLIST_LEAVES = ENLIST_LEAVES;
            return this;
        }

        public MulibConfigBuilder setCONCRETIZE_IF_NEEDED(boolean CONCRETIZE_IF_NEEDED) {
            this.CONCRETIZE_IF_NEEDED = CONCRETIZE_IF_NEEDED;
            return this;
        }

        public MulibConfigBuilder setTREE_INDENTATION(String TREE_INDENTATION) {
            this.TREE_INDENTATION = TREE_INDENTATION;
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORE_FROM_PACKAGES(List<String> TRANSF_IGNORE_FROM_PACKAGES) {
            this.TRANSF_IGNORE_FROM_PACKAGES = TRANSF_IGNORE_FROM_PACKAGES;
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORE_CLASSES(List<Class<?>> TRANSF_IGNORE_CLASSES) {
            this.TRANSF_IGNORE_CLASSES = TRANSF_IGNORE_CLASSES;
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORE_SUBCLASSES_OF(List<Class<?>> TRANSF_IGNORE_SUBCLASSES_OF) {
            this.TRANSF_IGNORE_SUBCLASSES_OF = TRANSF_IGNORE_SUBCLASSES_OF;
            return this;
        }

        public MulibConfigBuilder setTRANSF_REGARD_SPECIAL_CASE(List<Class<?>> TRANSF_REGARD_SPECIAL_CASE) {
            this.TRANSF_REGARD_SPECIAL_CASE = TRANSF_REGARD_SPECIAL_CASE;
            return this;
        }

        public MulibConfigBuilder setTRANSF_CONCRETIZE_FOR(List<Class<?>> TRANSF_CONCRETIZE_FOR) {
            this.TRANSF_CONCRETIZE_FOR = TRANSF_CONCRETIZE_FOR;
            return this;
        }

        public MulibConfigBuilder setTRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR(List<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR) {
            this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
            return this;
        }

        public MulibConfigBuilder setGLOBAL_SEARCH_STRATEGY(SearchStrategy GLOBAL_SEARCH_STRATEGY) {
            this.GLOBAL_SEARCH_STRATEGY = GLOBAL_SEARCH_STRATEGY;
            return this;
        }

        public MulibConfigBuilder setGLOBAL_SOLVER_TYPE(Solvers GLOBAL_SOLVER_TYPE) {
            this.GLOBAL_SOLVER_TYPE = GLOBAL_SOLVER_TYPE;
            return this;
        }

        public MulibConfigBuilder setFIXED_POSSIBLE_CP_BUDGET(long FIXED_POSSIBLE_CP_BUDGET) {
            this.FIXED_POSSIBLE_CP_BUDGET = FIXED_POSSIBLE_CP_BUDGET;
            return this;
        }

        public MulibConfigBuilder setFIXED_ACTUAL_CP_BUDGET(long FIXED_ACTUAL_CP_BUDGET) {
            this.FIXED_ACTUAL_CP_BUDGET = FIXED_ACTUAL_CP_BUDGET;
            return this;
        }

        public MulibConfigBuilder setINCR_ACTUAL_CP_BUDGET(long INCR_ACTUAL_CP_BUDGET) {
            this.INCR_ACTUAL_CP_BUDGET = INCR_ACTUAL_CP_BUDGET;
            return this;
        }

        public MulibConfigBuilder setTRANSF_WRITE_TO_FILE(boolean TRANSF_WRITE_TO_FILE) {
            this.TRANSF_WRITE_TO_FILE = TRANSF_WRITE_TO_FILE;
            return this;
        }

        public MulibConfigBuilder setTRANSF_GENERATED_CLASSES_PATH(String TRANSF_GENERATED_CLASSES_PATH) {
            this.TRANSF_GENERATED_CLASSES_PATH = TRANSF_GENERATED_CLASSES_PATH;
            this.TRANSF_INCLUDE_PACKAGE_NAME = true;
            return this;
        }

        public MulibConfigBuilder setTRANSF_VALIDATE_TRANSFORMATION(boolean TRANSF_VALIDATE_TRANSFORMATION) {
            this.TRANSF_VALIDATE_TRANSFORMATION = TRANSF_VALIDATE_TRANSFORMATION;
            return this;
        }

        public MulibConfigBuilder setGLOBAL_AVOID_SAT_CHECKS(boolean GLOBAL_AVOID_SAT_CHECKS) {
            throw new NotYetImplementedException();
//            this.GLOBAL_AVOID_SAT_CHECKS = GLOBAL_AVOID_SAT_CHECKS;
//            return this;
        }

        public MulibConfigBuilder setSECONDS_PER_INVOCATION(long SECONDS_PER_INVOCATION) {
            this.SECONDS_PER_INVOCATION = SECONDS_PER_INVOCATION;
            return this;
        }

        public MulibConfigBuilder setMAX_FAILS(long MAX_FAILS) {
            this.MAX_FAILS = MAX_FAILS;
            return this;
        }

        public MulibConfigBuilder setMAX_PATH_SOLUTIONS(long MAX_PATH_SOLUTIONS) {
            this.MAX_PATH_SOLUTIONS = MAX_PATH_SOLUTIONS;
            return this;
        }

        public MulibConfigBuilder setMAX_EXCEEDED_BUDGETS(long MAX_EXCEEDED_BUDGETS) {
            this.MAX_EXCEEDED_BUDGETS = MAX_EXCEEDED_BUDGETS;
            return this;
        }

        public MulibConfigBuilder setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(SearchStrategy... searchStrategies) {
            this.setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(Arrays.asList(searchStrategies));
            return this;
        }

        public MulibConfigBuilder setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(List<SearchStrategy> searchStrategies) {
            this.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES = searchStrategies;
            return this;
        }

        public MulibConfigBuilder setPARALLEL_TIMEOUT_IN_MS(long ms) {
            this.PARALLEL_TIMEOUT_IN_MS = ms;
            return this;
        }

        public MulibConfigBuilder setCHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques choiceOptionDequeType) {
            this.CHOICE_OPTION_DEQUE_TYPE = choiceOptionDequeType;
            return this;
        }

        public MulibConfigBuilder setACTIVATE_PARALLEL_FOR(long setFor) {
            this.ACTIVATE_PARALLEL_FOR = setFor;
            return this;
        }

        public MulibConfigBuilder setLABEL_RESULT_VALUE(boolean b) {
            this.LABEL_RESULT_VALUE = b;
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS(Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> m) {
            this.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS = m;
            return this;
        }

        public MulibConfigBuilder addTRANSF_IGNORED_CLASS_TO_COPY_FUNCTION(Class<?> clazz, BiFunction<MulibValueTransformer, Object, Object> copyFunction) {
            this.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS.put(clazz, copyFunction);
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS(Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> m) {
            this.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS = m;
            return this;
        }

        public MulibConfigBuilder addTRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS(Class<?> clazz, BiFunction<MulibValueTransformer, Object, Object> transformation) {
            this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS.put(clazz, transformation);
            return this;
        }

        public MulibConfigBuilder setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(boolean b) {
            this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = b;
            return this;
        }

        public MulibConfigBuilder setTRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS(Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> m) {
            this.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS = m;
            return this;
        }

        public MulibConfigBuilder addTRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS(Class<?> clazz, BiFunction<MulibValueTransformer, Object, Object> transformation) {
            this.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS.put(clazz, transformation);
            return this;
        }

        public MulibConfigBuilder setSYMSINT_DOMAIN(int SYMSINT_LB, int SYMSINT_UB) {
            if (SYMSINT_LB > SYMSINT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSINT_LB = Optional.of(SYMSINT_LB);
            this.SYMSINT_UB = Optional.of(SYMSINT_UB);
            return this;
        }

        public MulibConfigBuilder setSYMSLONG_DOMAIN(long SYMSLONG_LB, long SYMSLONG_UB) {
            if (SYMSLONG_LB > SYMSLONG_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSLONG_LB = Optional.of(SYMSLONG_LB);
            this.SYMSLONG_UB = Optional.of(SYMSLONG_UB);
            return this;
        }

        public MulibConfigBuilder setSYMSDOUBLE_DOMAIN(double SYMSDOUBLE_LB, double SYMSDOUBLE_UB) {
            if (SYMSDOUBLE_LB > SYMSDOUBLE_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSDOUBLE_UB = Optional.of(SYMSDOUBLE_UB);
            this.SYMSDOUBLE_LB = Optional.of(SYMSDOUBLE_LB);
            return this;
        }

        public MulibConfigBuilder setSYMSFLOAT_DOMAIN(float SYMSFLOAT_LB, float SYMSFLOAT_UB) {
            if (SYMSFLOAT_LB > SYMSFLOAT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSFLOAT_UB = Optional.of(SYMSFLOAT_UB);
            this.SYMSFLOAT_LB = Optional.of(SYMSFLOAT_LB);
            return this;
        }

        public MulibConfigBuilder setSYMSSHORT_DOMAIN(short SYMSSHORT_LB, short SYMSSHORT_UB) {
            if (SYMSSHORT_LB > SYMSSHORT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSSHORT_UB = Optional.of(SYMSSHORT_UB);
            this.SYMSSHORT_LB = Optional.of(SYMSSHORT_LB);
            return this;
        }

        public MulibConfigBuilder setSYMSBYTE_DOMAIN(byte SYMSBYTE_LB, byte SYMSBYTE_UB) {
            if (SYMSBYTE_LB > SYMSBYTE_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.SYMSBYTE_UB = Optional.of(SYMSBYTE_UB);
            this.SYMSBYTE_LB = Optional.of(SYMSBYTE_LB);
            return this;
        }

        public MulibConfigBuilder setTREAT_BOOLEANS_AS_INTS(boolean TREAT_BOOLEANS_AS_INTS) {
            this.TREAT_BOOLEANS_AS_INTS = TREAT_BOOLEANS_AS_INTS;
            return this;
        }

        public MulibConfigBuilder assumeMulibDefaultValueRanges() {
            this.SYMSINT_LB =    Optional.of(Integer.MIN_VALUE);
            this.SYMSINT_UB =    Optional.of(Integer.MAX_VALUE);
            this.SYMSDOUBLE_LB = Optional.of(-100_000_000_000_000_000D);
            this.SYMSDOUBLE_UB = Optional.of(100_000_000_000_000_000D);
            this.SYMSFLOAT_LB =  Optional.of(-100_000_000_000_000_000F);
            this.SYMSFLOAT_UB =  Optional.of(100_000_000_000_000_000F);
            this.SYMSLONG_LB =   Optional.of(-100_000_000_000_000_000L);
            this.SYMSLONG_UB =   Optional.of(100_000_000_000_000_000L);
            this.SYMSSHORT_LB =  Optional.of(Short.MIN_VALUE);
            this.SYMSSHORT_UB =  Optional.of(Short.MAX_VALUE);
            this.SYMSBYTE_LB =   Optional.of(Byte.MIN_VALUE);
            this.SYMSBYTE_UB =   Optional.of(Byte.MAX_VALUE);
            return this;
        }

        public MulibConfigBuilder setCONCOLIC(boolean CONCOLIC) {
            this.CONCOLIC = CONCOLIC;
            return this;
        }

        public MulibConfig build() {

            if (TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER && (!TRANSF_INCLUDE_PACKAGE_NAME || !TRANSF_WRITE_TO_FILE)) {
                throw new MisconfigurationException("When choosing to load with the SystemClassLoader, the files" +
                        " must be generated under the same path as the original classes. Set TRANSF_GENERATED_CLASSES_PATH" +
                        " for this and set TRANSF_WRITE_TO_FILE to true."
                );
            }


            if (INCR_ACTUAL_CP_BUDGET != 0) {
                if (GLOBAL_SEARCH_STRATEGY != SearchStrategy.IDDFS
                        && !ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.contains(SearchStrategy.IDDFS)) {
                    throw new MisconfigurationException("When choosing an incremental budget, IDDFS must be used" +
                            " as the search strategy. Currently, " + GLOBAL_SEARCH_STRATEGY + " is used as the global" +
                            " search strategy and " + ADDITIONAL_PARALLEL_SEARCH_STRATEGIES + " are used as additional" +
                            " search strategies.");
                }
            }

            if (INCR_ACTUAL_CP_BUDGET < 1
                    && (GLOBAL_SEARCH_STRATEGY == SearchStrategy.IDDFS
                        || ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.contains(SearchStrategy.IDDFS))) {
                throw new MisconfigurationException("When choosing IDDFS, an incremental budget must be specified.");
            }

            return new MulibConfig(
                    LABEL_RESULT_VALUE,
                    GLOBAL_AVOID_SAT_CHECKS,
                    ENLIST_LEAVES,
                    CONCRETIZE_IF_NEEDED,
                    TREE_INDENTATION,
                    GLOBAL_SEARCH_STRATEGY,
                    ADDITIONAL_PARALLEL_SEARCH_STRATEGIES,
                    PARALLEL_TIMEOUT_IN_MS,
                    GLOBAL_SOLVER_TYPE,
                    FIXED_POSSIBLE_CP_BUDGET,
                    FIXED_ACTUAL_CP_BUDGET,
                    INCR_ACTUAL_CP_BUDGET,
                    SECONDS_PER_INVOCATION,
                    MAX_FAILS,
                    MAX_PATH_SOLUTIONS,
                    MAX_EXCEEDED_BUDGETS,
                    TRANSF_IGNORE_FROM_PACKAGES,
                    TRANSF_IGNORE_CLASSES,
                    TRANSF_IGNORE_SUBCLASSES_OF,
                    TRANSF_REGARD_SPECIAL_CASE,
                    TRANSF_WRITE_TO_FILE,
                    TRANSF_GENERATED_CLASSES_PATH,
                    TRANSF_INCLUDE_PACKAGE_NAME,
                    TRANSF_VALIDATE_TRANSFORMATION,
                    TRANSF_CONCRETIZE_FOR,
                    TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR,
                    TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS,
                    TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS,
                    TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS,
                    TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER,
                    CHOICE_OPTION_DEQUE_TYPE,
                    ACTIVATE_PARALLEL_FOR,
                    SYMSINT_LB,
                    SYMSINT_UB,
                    SYMSDOUBLE_LB,
                    SYMSDOUBLE_UB,
                    SYMSFLOAT_LB,
                    SYMSFLOAT_UB,
                    SYMSLONG_LB,
                    SYMSLONG_UB,
                    SYMSSHORT_LB,
                    SYMSSHORT_UB,
                    SYMSBYTE_LB,
                    SYMSBYTE_UB,
                    TREAT_BOOLEANS_AS_INTS,
                    CONCOLIC
            );
        }
    }
    private MulibConfig(boolean LABEL_RESULT_VALUE,
                        boolean GLOBAL_AVOID_SAT_CHECKS,
                        boolean ENLIST_LEAVES,
                        boolean CONCRETIZE_IF_NEEDED,
                        String TREE_INDENTATION,
                        SearchStrategy GLOBAL_SEARCH_STRATEGY,
                        List<SearchStrategy> ADDITIONAL_PARALLEL_SEARCH_STRATEGIES,
                        long PARALLEL_TIMEOUT_IN_MS,
                        Solvers GLOBAL_SOLVER_TYPE,
                        long FIXED_POSSIBLE_CP_BUDGET,
                        long FIXED_ACTUAL_CP_BUDGET,
                        long INCR_ACTUAL_CP_BUDGET,
                        long SECONDS_PER_INVOCATION,
                        long MAX_FAILS,
                        long MAX_PATH_SOLUTIONS,
                        long MAX_EXCEEDED_BUDGETS,
                        List<String> TRANSF_IGNORE_FROM_PACKAGES,
                        List<Class<?>> TRANSF_IGNORE_CLASSES,
                        List<Class<?>> TRANSF_IGNORE_SUBCLASSES_OF,
                        List<Class<?>> TRANSF_REGARD_SPECIAL_CASE,
                        boolean TRANSF_WRITE_TO_FILE,
                        String TRANSF_GENERATED_CLASSES_PATH,
                        boolean TRANSF_INCLUDE_PACKAGE_NAME,
                        boolean TRANSF_VALIDATE_TRANSFORMATION,
                        List<Class<?>> TRANSF_CONCRETIZE_FOR,
                        List<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR,
                        Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS,
                        Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS,
                        Map<Class<?>, BiFunction<MulibValueTransformer, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS,
                        boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER,
                        ChoiceOptionDeques CHOICE_OPTION_DEQUE_TYPE,
                        long ACTIVATE_PARALLEL_FOR,
                        Optional<Integer> SYMSINT_LB,
                        Optional<Integer> SYMSINT_UB,
                        Optional<Double> SYMSDOUBLE_LB,
                        Optional<Double> SYMSDOUBLE_UB,
                        Optional<Float> SYMSFLOAT_LB,
                        Optional<Float> SYMSFLOAT_UB,
                        Optional<Long> SYMSLONG_LB,
                        Optional<Long> SYMSLONG_UB,
                        Optional<Short> SYMSSHORT_LB,
                        Optional<Short> SYMSSHORT_UB,
                        Optional<Byte> SYMSBYTE_LB,
                        Optional<Byte> SYMSBYTE_UB,
                        boolean TREAT_BOOLEANS_AS_INTS,
                        boolean CONCOLIC
    ) {
        this.LABEL_RESULT_VALUE = LABEL_RESULT_VALUE;
        this.GLOBAL_AVOID_SAT_CHECKS = GLOBAL_AVOID_SAT_CHECKS;
        this.ENLIST_LEAVES = ENLIST_LEAVES;
        this.TREE_INDENTATION = TREE_INDENTATION;
        this.GLOBAL_SEARCH_STRATEGY = GLOBAL_SEARCH_STRATEGY;
        this.ADDITIONAL_PARALLEL_SEARCH_STRATEGIES = List.copyOf(ADDITIONAL_PARALLEL_SEARCH_STRATEGIES);
        this.GLOBAL_SOLVER_TYPE = GLOBAL_SOLVER_TYPE;
        this.FIXED_POSSIBLE_CP_BUDGET = 0 != FIXED_POSSIBLE_CP_BUDGET   ? Optional.of(FIXED_POSSIBLE_CP_BUDGET) : Optional.empty();
        this.FIXED_ACTUAL_CP_BUDGET =   0 != FIXED_ACTUAL_CP_BUDGET     ? Optional.of(FIXED_ACTUAL_CP_BUDGET)   : Optional.empty();
        this.INCR_ACTUAL_CP_BUDGET =    0 != INCR_ACTUAL_CP_BUDGET      ? Optional.of(INCR_ACTUAL_CP_BUDGET)    : Optional.empty();
        this.NANOSECONDS_PER_INVOCATION =   0 != SECONDS_PER_INVOCATION     ? Optional.of(SECONDS_PER_INVOCATION * 1_000_000_000) : Optional.empty();
        this.MAX_FAILS =                0 != MAX_FAILS                  ? Optional.of(MAX_FAILS) : Optional.empty();
        this.MAX_PATH_SOLUTIONS =       0 != MAX_PATH_SOLUTIONS         ? Optional.of(MAX_PATH_SOLUTIONS) : Optional.empty();
        this.MAX_EXCEEDED_BUDGETS =     0 != MAX_EXCEEDED_BUDGETS       ? Optional.of(MAX_EXCEEDED_BUDGETS) : Optional.empty();
        this.TRANSF_IGNORE_FROM_PACKAGES = List.copyOf(TRANSF_IGNORE_FROM_PACKAGES);
        this.TRANSF_IGNORE_CLASSES = List.copyOf(TRANSF_IGNORE_CLASSES);
        this.TRANSF_IGNORE_SUBCLASSES_OF = List.copyOf(TRANSF_IGNORE_SUBCLASSES_OF);
        this.TRANSF_REGARD_SPECIAL_CASE = List.copyOf(TRANSF_REGARD_SPECIAL_CASE);
        this.TRANSF_WRITE_TO_FILE = TRANSF_WRITE_TO_FILE;
        this.TRANSF_GENERATED_CLASSES_PATH = TRANSF_GENERATED_CLASSES_PATH;
        this.TRANSF_INCLUDE_PACKAGE_NAME = TRANSF_INCLUDE_PACKAGE_NAME;
        this.TRANSF_VALIDATE_TRANSFORMATION = TRANSF_VALIDATE_TRANSFORMATION;
        this.TRANSF_CONCRETIZE_FOR = TRANSF_CONCRETIZE_FOR;
        this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        this.TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS = Map.copyOf(TRANSF_IGNORED_CLASSES_TO_COPY_FUNCTIONS);
        this.TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS = Map.copyOf(TRANSF_IGNORED_CLASSES_TO_TRANSFORM_FUNCTIONS);
        this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS = Map.copyOf(TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS);
        this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        this.PARALLEL_TIMEOUT_IN_MS = PARALLEL_TIMEOUT_IN_MS;
        this.CHOICE_OPTION_DEQUE_TYPE = CHOICE_OPTION_DEQUE_TYPE;
        this.ACTIVATE_PARALLEL_FOR = ACTIVATE_PARALLEL_FOR < 1 ? Optional.empty() : Optional.of(ACTIVATE_PARALLEL_FOR);
        this.SYMSINT_LB =    SYMSINT_LB.isEmpty() ? Optional.empty() :    Optional.of(Sint.concSint(SYMSINT_LB.get()));
        this.SYMSINT_UB =    SYMSINT_UB.isEmpty() ? Optional.empty() :    Optional.of(Sint.concSint(SYMSINT_UB.get()));
        this.SYMSDOUBLE_LB = SYMSDOUBLE_LB.isEmpty() ? Optional.empty() : Optional.of(Sdouble.concSdouble(SYMSDOUBLE_LB.get()));
        this.SYMSDOUBLE_UB = SYMSDOUBLE_UB.isEmpty() ? Optional.empty() : Optional.of(Sdouble.concSdouble(SYMSDOUBLE_UB.get()));
        this.SYMSFLOAT_LB =  SYMSFLOAT_LB.isEmpty() ? Optional.empty() :  Optional.of(Sfloat.concSfloat(SYMSFLOAT_LB.get()));
        this.SYMSFLOAT_UB =  SYMSFLOAT_UB.isEmpty() ? Optional.empty() :  Optional.of(Sfloat.concSfloat(SYMSFLOAT_UB.get()));
        this.SYMSLONG_LB =   SYMSLONG_LB.isEmpty() ? Optional.empty() :   Optional.of(Slong.concSlong(SYMSLONG_LB.get()));
        this.SYMSLONG_UB =   SYMSLONG_UB.isEmpty() ? Optional.empty() :   Optional.of(Slong.concSlong(SYMSLONG_UB.get()));
        this.SYMSSHORT_LB =  SYMSSHORT_LB.isEmpty() ? Optional.empty() :  Optional.of(Sshort.concSshort(SYMSSHORT_LB.get()));
        this.SYMSSHORT_UB =  SYMSSHORT_UB.isEmpty() ? Optional.empty() :  Optional.of(Sshort.concSshort(SYMSSHORT_UB.get()));
        this.SYMSBYTE_LB =   SYMSBYTE_LB.isEmpty() ? Optional.empty() :   Optional.of(Sbyte.concSbyte(SYMSBYTE_LB.get()));
        this.SYMSBYTE_UB =   SYMSBYTE_UB.isEmpty() ? Optional.empty() :   Optional.of(Sbyte.concSbyte(SYMSBYTE_UB.get()));
        this.TREAT_BOOLEANS_AS_INTS = TREAT_BOOLEANS_AS_INTS;
        this.CONCOLIC = CONCOLIC;
    }

    @Override
    public String toString() {
        return "MulibConfig{"
                + "GLOBAL_SEARCH_STRATEGY=" + GLOBAL_SEARCH_STRATEGY
                + (ADDITIONAL_PARALLEL_SEARCH_STRATEGIES.isEmpty() ? "" : ", ADDITIONAL_PARALLEL_SEARCH_STRATEGIES=" + ADDITIONAL_PARALLEL_SEARCH_STRATEGIES)
                + ", GLOBAL_SOLVER_TYPE=" + GLOBAL_SOLVER_TYPE
                + ", CONCOLIC=" + CONCOLIC
                + (GLOBAL_AVOID_SAT_CHECKS ? ", GLOBAL_AVOID_SAT_CHECKS=" + GLOBAL_AVOID_SAT_CHECKS : "")
                + (ENLIST_LEAVES ? ", ENLIST_LEAVES=" + true : "")
                + FIXED_ACTUAL_CP_BUDGET.map(v -> ", FIXED_ACTUAL_CP_BUDGET=" + v).orElse("")
                + FIXED_POSSIBLE_CP_BUDGET.map(v -> ", FIXED_POSSIBLE_CP_BUDGET=" + v).orElse("")
                + INCR_ACTUAL_CP_BUDGET.map(v -> ", INCR_ACTUAL_CP_BUDGET=" + v).orElse("")
                + NANOSECONDS_PER_INVOCATION.map(v -> ", SECONDS_PER_INVOCATION=" + v).orElse("")
                + MAX_FAILS.map(v -> ", MAX_FAILS=" + v).orElse("")
                + MAX_PATH_SOLUTIONS.map(v -> ", MAX_PATH_SOLUTIONS=" + v).orElse("")
                + MAX_EXCEEDED_BUDGETS.map(v -> ", MAX_EXCEEDED_BUDGETS=" + v).orElse("")
                + "}";
    }
}
