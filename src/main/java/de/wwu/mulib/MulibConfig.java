package de.wwu.mulib;

import de.wwu.mulib.model.classes.java.lang.IntegerReplacement;
import de.wwu.mulib.model.classes.java.lang.NumberReplacement;
import de.wwu.mulib.search.choice_points.Backtrack;
import de.wwu.mulib.search.executors.MulibExecutor;
import de.wwu.mulib.search.executors.SearchStrategy;
import de.wwu.mulib.search.trees.ChoiceOptionDeques;
import de.wwu.mulib.search.trees.ExceededBudget;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.solving.Solvers;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.throwables.MisconfigurationException;
import de.wwu.mulib.util.TriConsumer;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Comprises configuration options for executing Mulib.
 * Is created using the builder pattern.
 * The prefix 'SOLVER_' is used for configurations involving the solver.
 * The prefix 'SEARCH_' is used for configurations involving the behavior within the search, such as search strategies,
 * whether to use concolic execution, etc.
 * The prefix 'BUDGETS_' is used to specify budgets.
 * The prefix 'ARRAYS_' is used to specify the procedures for how arrays are treated, e.g., whether to check if
 * valid accesses should be checked, etc.
 * The prefix 'TRANSF_' is used to define how the program transformation should behave.
 * The prefix 'CFG_' is used to define the behavior of the control-flow graph, if any is specified via
 * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
 * The prefix 'VALS_' is used to globally restrict the valid domains of values.
 * The prefix 'LOG_' is for logging.
 * The prefix 'TREE_' is for debug configs for displaying the tree.
 *
 * @see MulibConfigBuilder
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class MulibConfig {

    /* Tree */
    /**
     * For representing this tree as a String: how much to indent between the different depths?
     */
    public final String TREE_INDENTATION;
    /**
     * Whether we should enlist all leaves of the search tree, is useful for debugging
     */
    public final boolean TREE_ENLIST_LEAVES;
    /* Search */
    /**
     * The search strategy. If {@link MulibConfig#SEARCH_ADDITIONAL_PARALLEL_STRATEGIES} are used, the seed strategy
     */
    public final SearchStrategy SEARCH_MAIN_STRATEGY;
    /**
     * Additional search strategies that will be "activated" in-order
     */
    public final List<SearchStrategy> SEARCH_ADDITIONAL_PARALLEL_STRATEGIES;
    /**
     * The type of {@link de.wwu.mulib.search.trees.ChoiceOptionDeque} used
     */
    public final ChoiceOptionDeques SEARCH_CHOICE_OPTION_DEQUE_TYPE;
    /**
     * How many choice options need to be retrieved until we activate one of the search strategies from
     * {@link MulibConfig#SEARCH_ADDITIONAL_PARALLEL_STRATEGIES}?
     */
    public final Optional<Long> SEARCH_ACTIVATE_PARALLEL_FOR;
    /**
     * True, if we want to execute the search region in concolc mode, false leads to a purely symbolic search
     */
    public final boolean SEARCH_CONCOLIC;
    /**
     * Whether we throw an exception once we find an {@link Throwable} in the search region, or not
     */
    public final boolean SEARCH_ALLOW_EXCEPTIONS;
    /**
     * Useful for debugging: Should the result be labeled?
     */
    public final boolean SEARCH_LABEL_RESULT_VALUE;
    /* Shutdown */
    /**
     * Timeout for shutting down the executors
     */
    public final long SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS;
    /* Logging */
    /**
     * Log the time for finding each path solution
     */
    public final boolean LOG_TIME_FOR_EACH_PATH_SOLUTION;
    /**
     * Log the time for finding only the first path solution
     */
    public final boolean LOG_TIME_FOR_FIRST_PATH_SOLUTION;
    /* Callbacks */
    /**
     * Callback that is invoked after finding each path solution
     * The arguments are the executor finding this path solution, the path solution, and the solver manager of the
     * executor
     */
    public final TriConsumer<MulibExecutor, PathSolution, SolverManager> CALLBACK_PATH_SOLUTION;
    /**
     * Callback that is invoked after backtracking
     * The arguments are the executor that is backtracking, the backtrack object, and the solver manager of the
     * executor
     */
    public final TriConsumer<MulibExecutor, Backtrack, SolverManager> CALLBACK_BACKTRACK;
    /**
     * Callback that is invoked after an explicit {@link Fail} has been thrown
     * The arguments are the executor finding this fail, the fail node in the search tree, and the solver manager of the
     * executor
     */
    public final TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.Fail, SolverManager> CALLBACK_FAIL;
    /**
     * Callback that is invoked after exceeding a budget
     * The arguments are the executor finding exceeded budget, the exceeded budget node in the search tree,
     * and the solver manager of the executor
     */
    public final TriConsumer<MulibExecutor, ExceededBudget, SolverManager> CALLBACK_EXCEEDED_BUDGET;
    /* Values */
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sint> VALS_SYMSINT_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sint> VALS_SYMSINT_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Slong> VALS_SYMSLONG_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Slong> VALS_SYMSLONG_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sdouble> VALS_SYMSDOUBLE_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sdouble> VALS_SYMSDOUBLE_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sfloat> VALS_SYMSFLOAT_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sfloat> VALS_SYMSFLOAT_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sshort> VALS_SYMSSHORT_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sshort> VALS_SYMSSHORT_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sbyte> VALS_SYMSBYTE_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Sbyte> VALS_SYMSBYTE_UB;
    /**
     * Lower bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Schar> VALS_SYMSCHAR_LB;
    /**
     * Upper bound. Can be overridden by providing an explicit lower and upper bound for a value.
     */
    public final Optional<Schar> VALS_SYMSCHAR_UB;
    /**
     * Usually, this is not possible in Java, but the bytecode allows for adding and subtracting from and using
     * booleans. The current implementation deals with this case rather inefficiently, and thus, it must be
     * explicitly activated
     */
    public final boolean VALS_TREAT_BOOLEANS_AS_INTS;

    /* Free Arrays */
    /**
     * Should eager indexes be used for dealing with arrays containing objects?
     */
    public final boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
    /**
     * Should eager indexes be used for dealing with arrays containing primitives?
     * If this is true, {@link MulibConfig#ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS} must be true as well.
     */
    public final boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS;
    /**
     * Should the symbolic execution check for out-of-bounds exceptions that are possible in arrays?
     */
    public final boolean ARRAYS_THROW_EXCEPTION_ON_OOB;
    /* Solver */
    /**
     * The solver that is used for the search strategy/search strategies
     */
    public final Solvers SOLVER_GLOBAL_TYPE;
    /**
     * Arguments that should be passed to the solver
     */
    public final Map<String, Object> SOLVER_ARGS;
    /**
     * Whether we should enable the solver-independent approach for dealing with symbolic arrays and symbolic objects
     */
    public final boolean SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH;

    /**
     * Whether the {@link de.wwu.mulib.solving.solvers.AbstractIncrementalEnabledSolverManager} should additionally
     * keep track of constraints in Mulib's {@link de.wwu.mulib.constraints.Constraint} representation.
     * This can be useful for debugging.
     */
    public final boolean SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS;

    /* Budget */
    /**
     * The maximal depth in the search tree
     */
    public final Optional<Long> BUDGETS_FIXED_ACTUAL_CP;
    /**
     * The incremental budget. Used by {@link SearchStrategy#IDDFS} and {@link SearchStrategy#IDDSAS}
     */
    public final Optional<Long> BUDGETS_INCR_ACTUAL_CP;
    /**
     * The time allowed for executing the search region in nano seconds
     */
    public final Optional<Long> BUDGETS_GLOBAL_TIME_IN_NANOSECONDS;
    /**
     * The maximal number of fails encountered during search
     */
    public final Optional<Long> BUDGETS_MAX_FAILS;
    /**
     * The maximal number of path solutions encountered during search
     */
    public final Optional<Long> BUDGETS_MAX_PATH_SOLUTIONS;
    /**
     * The maximal number of exceeded budgets encountered during search
     */
    public final Optional<Long> BUDGETS_MAX_EXCEEDED_BUDGET;

    /* Free Initialization */
    /**
     * Should we allow arrays initialized via {@link Mulib#freeObject(Class)} etc. to be null?
     */
    public final boolean FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
    /**
     * Should we allow objects initialized via {@link Mulib#freeObject(Class)} etc. to be null?
     */
    public final boolean FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
    /**
     * Should we initialize objects via {@link Mulib#freeObject(Class)} etc. to be potential targets for aliasing
     * of subsequently initialized objects?
     */
    public final boolean FREE_INIT_ALIASING_FOR_FREE_OBJECTS;

    /* Transformation */
    private final boolean TRANSF_USE_DEFAULT_MODEL_CLASSES;
    /**
     * A map of (original class, replacement of some class that will be transformed instead)-pairs in the search region.
     * The replacement class will be transformed and all references of the original class will be removed.
     * Used for custom model classes such as {@link IntegerReplacement}.
     */
    public final Map<Class<?>, Class<?>> TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS;
    /**
     * Should the methods specified in {@link de.wwu.mulib.model.ModelMethods} be used in place of the
     * replaced methods?
     */
    public final boolean TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH;
    /**
     * A map of (original method, replacement method)-pairs. In the search region, all occurrences of calls to
     * 'original method' will be replaced by calls to 'replacement method'.
     * Should be done for native methods if not the whole class is substituted via
     * {@link #TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS}.
     */
    public final Map<Method, Method> TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD;
    /**
     * Classes from the specified packages will not be transformed
     * Should be used with care so that class hierachies are not broken
     */
    public final Set<String> TRANSF_IGNORE_FROM_PACKAGES;
    /**
     * Classes in the set will not be transformed
     * Should be used with care so that class hierachies are not broken
     */
    public final Set<Class<?>> TRANSF_IGNORE_CLASSES;
    /**
     * Used to mark classes that usually are ignored but should be transformed nonetheless
     * Should be used carefully, is useful for writing test cases for Mulib
     */
    @Deprecated
    public final Set<Class<?>> TRANSF_REGARD_SPECIAL_CASE;
    /**
     * The set of classes is ignored.
     * If a method of this class is called, the inputs will be concretized.
     * Should be used with care so that class hierachies are not broken
     */
    public final Set<Class<?>> TRANSF_CONCRETIZE_FOR;
    /**
     * The set of classes is ignored
     * If a method of this class is called, it is tried to generalize the input types so a {@link Object} parameter
     * is used instead
     * Should be used with care so that class hierachies are not broken
     */
    @Deprecated
    public final Set<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
    /**
     * If there are ignored classes that still are generated in the search region, and they are part of the final
     * {@link de.wwu.mulib.solving.Solution} object, they must be labeled. To do so, such a method must be supplied
     * here.
     */
    public final Map<Class<?>, BiFunction<SolverManager, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
    /**
     * If {@link MulibConfig#TRANSF_WRITE_TO_FILE} is true, should already existing files be overwritten?
     */
    public final boolean TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
    /**
     * Should we write the transformed partner classes onto disk?
     */
    public final boolean TRANSF_WRITE_TO_FILE;
    /**
     * Where should we try to write the generated partner classes?
     */
    public final String TRANSF_GENERATED_CLASSES_PATH;
    /**
     * Should the partner class be validated after generation? This is useful for debugging
     */
    public final boolean TRANSF_VALIDATE_TRANSFORMATION;
    /**
     * For debugging and testing, it sometimes is helpful to directly write library-code without a transformation.
     * If this is false, the {@link de.wwu.mulib.transformations.MulibTransformer} will not try to
     * transform the search region
     */
    public final boolean TRANSF_TRANSFORMATION_REQUIRED;
    /**
     * For the system classloader to work, it is required that {@link #TRANSF_GENERATED_CLASSES_PATH} is set to the same root
     * folder of the usual system classes
     */
    public final boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
    /**
     * If true, the generated file will be in a folder structure according to its package name
     */
    public final boolean TRANSF_INCLUDE_PACKAGE_NAME;
    /**
     * Currently only done for the integration with Dacite: Some code is treated as a special case
     */
    public final boolean TRANSF_TREAT_SPECIAL_METHOD_CALLS;
    /**
     * Should the choice points be generated with an identifier for using {@link de.wwu.mulib.search.choice_points.CoverageCfg}?
     */
    public final boolean TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID;
    /* CFG */
    /**
     * Should the symbolic execution consult the {@link de.wwu.mulib.search.choice_points.CoverageCfg} to determine
     * which of the two choice options should be used? If this is the case, {@link de.wwu.mulib.search.choice_points.CoverageCfg}
     * will check if either of these choices has not yet been executed
     */
    public final boolean CFG_USE_GUIDANCE_DURING_EXECUTION;
    /**
     * If we can prove that all nodes in the control flow graph have been visited, should we terminate early?
     */
    public final boolean CFG_TERMINATE_EARLY_ON_FULL_COVERAGE;
    /**
     * Should a search strategy prefer choice options with uncovered nodes on the control flow graph during execution?
     */
    public final boolean CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE;

    public static MulibConfigBuilder builder() {
        return new MulibConfigBuilder();
    }

    public static MulibConfig get() {
        return builder().build();
    }

    public final static class MulibConfigBuilder {
        private boolean LOG_TIME_FOR_EACH_PATH_SOLUTION;
        private boolean FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        private boolean FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
        private boolean FREE_INIT_ALIASING_FOR_FREE_OBJECTS;
        private String TREE_INDENTATION;
        private boolean SEARCH_LABEL_RESULT_VALUE;
        private boolean TREE_ENLIST_LEAVES;
        private Set<String> TRANSF_IGNORE_FROM_PACKAGES;
        private Set<Class<?>> TRANSF_IGNORE_CLASSES;
        private Set<Class<?>> TRANSF_REGARD_SPECIAL_CASE;
        private Set<Class<?>> TRANSF_CONCRETIZE_FOR;
        private Set<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        private Map<Class<?>, BiFunction<SolverManager, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS;
        private boolean TRANSF_USE_DEFAULT_MODEL_CLASSES;
        private Map<Class<?>, Class<?>> TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS;
        private boolean TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH;
        private Map<Method, Method> TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD;
        private SearchStrategy SEARCH_MAIN_STRATEGY;
        private boolean SEARCH_CONCOLIC;
        private boolean SEARCH_ALLOW_EXCEPTIONS;
        private List<SearchStrategy> SEARCH_ADDITIONAL_PARALLEL_STRATEGIES;
        private ChoiceOptionDeques SEARCH_CHOICE_OPTION_DEQUE_TYPE;
        private long SEARCH_ACTIVATE_PARALLEL_FOR;
        private Solvers SOLVER_GLOBAL_TYPE;
        private boolean SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS;
        private long BUDGET_FIXED_ACTUAL_CP;
        private long BUDGET_INCR_ACTUAL_CP;
        private long BUDGET_GLOBAL_TIME_IN_SECONDS;
        private long BUDGET_MAX_FAILS;
        private long BUDGET_MAX_PATH_SOLUTIONS;
        private long BUDGET_MAX_EXCEEDED;
        private boolean TRANSF_WRITE_TO_FILE;
        private String TRANSF_GENERATED_CLASSES_PATH;
        private boolean TRANSF_VALIDATE_TRANSFORMATION;
        private boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        private boolean TRANSF_INCLUDE_PACKAGE_NAME;
        private boolean TRANSF_TREAT_SPECIAL_METHOD_CALLS;
        private boolean TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID;
        private boolean CFG_USE_GUIDANCE_DURING_EXECUTION;
        private boolean CFG_TERMINATE_EARLY_ON_FULL_COVERAGE;
        private boolean CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE;
        private boolean TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
        private boolean TRANSF_TRANSFORMATION_REQUIRED;
        private long SHUTDOWN_PARALLEL_TIMEOUT_IN_MS;
        private Optional<Integer> VALS_SYMSINT_LB;
        private Optional<Integer> VALS_SYMSINT_UB;
        private Optional<Long> VALS_SYMSLONG_LB;
        private Optional<Long> VALS_SYMSLONG_UB;
        private Optional<Double> VALS_SYMSDOUBLE_LB;
        private Optional<Double> VALS_SYMSDOUBLE_UB;
        private Optional<Float> VALS_SYMSFLOAT_LB;
        private Optional<Float> VALS_SYMSFLOAT_UB;
        private Optional<Short> VALS_SYMSSHORT_LB;
        private Optional<Short> VALS_SYMSSHORT_UB;
        private Optional<Byte> VALS_SYMSBYTE_LB;
        private Optional<Byte> VALS_SYMSBYTE_UB;
        private Optional<Character> VALS_SYMSCHAR_LB;
        private Optional<Character> VALS_SYMSCHAR_UB;
        private boolean VALS_TREAT_BOOLEANS_AS_INTS;
        private boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS;
        private boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
        private boolean ARRAYS_THROW_EXCEPTION_ON_OOB;
        private boolean SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH;
        private LinkedHashMap<String, Object> SOLVER_ARGS;
        private boolean LOG_TIME_FOR_FIRST_PATH_SOLUTION;

        private TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.Fail, SolverManager> CALLBACK_FAIL;
        private TriConsumer<MulibExecutor, Backtrack, SolverManager> CALLBACK_BACKTRACK;
        private TriConsumer<MulibExecutor, ExceededBudget, SolverManager> CALLBACK_EXCEEDED_BUDGET;
        private TriConsumer<MulibExecutor, PathSolution, SolverManager> CALLBACK_PATH_SOLUTION;

        private MulibConfigBuilder() {
            // Defaults
            this.LOG_TIME_FOR_EACH_PATH_SOLUTION = false;
            this.FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL = false;
            this.FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL = false;
            this.FREE_INIT_ALIASING_FOR_FREE_OBJECTS = false;
            this.SEARCH_LABEL_RESULT_VALUE = true;
            this.TREE_ENLIST_LEAVES = false;
            this.SEARCH_CONCOLIC = false;
            this.SEARCH_ALLOW_EXCEPTIONS = false;
            this.TREE_INDENTATION = "    ";
            this.SEARCH_MAIN_STRATEGY = SearchStrategy.DFS;
            this.SOLVER_GLOBAL_TYPE = Solvers.Z3_INCREMENTAL;
            this.SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS = false;
            this.BUDGET_FIXED_ACTUAL_CP =   0;
            this.BUDGET_INCR_ACTUAL_CP =    0;
            this.BUDGET_GLOBAL_TIME_IN_SECONDS =   0;
            this.BUDGET_MAX_FAILS =                0;
            this.BUDGET_MAX_PATH_SOLUTIONS =       0;
            this.BUDGET_MAX_EXCEEDED =     0;
            this.SEARCH_ACTIVATE_PARALLEL_FOR =    2;
            this.SEARCH_CHOICE_OPTION_DEQUE_TYPE = ChoiceOptionDeques.SIMPLE;
            this.TRANSF_IGNORE_CLASSES = Set.of(
                    Mulib.class, Fail.class
            );
            this.TRANSF_IGNORE_FROM_PACKAGES = Set.of(
                    "de.wwu.mulib.substitutions", "de.wwu.mulib.transformations", "de.wwu.mulib.exceptions",
                    "de.wwu.mulib.expressions", "de.wwu.mulib.search", "de.wwu.mulib.solving",
                    "java"
            );
            this.TRANSF_CONCRETIZE_FOR = Set.of(
                    String.class
            );
            this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = Set.of(
                    PrintStream.class
            );
            this.TRANSF_REGARD_SPECIAL_CASE = new HashSet<>();
            this.TRANSF_USE_DEFAULT_MODEL_CLASSES = true;
            this.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS = new HashMap<>();
            this.TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH = false;
            this.TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD = new HashMap<>();
            this.TRANSF_WRITE_TO_FILE = true;
            this.TRANSF_GENERATED_CLASSES_PATH = "build/classes/java/";
            this.TRANSF_INCLUDE_PACKAGE_NAME = false;
            this.TRANSF_TREAT_SPECIAL_METHOD_CALLS = false;
            this.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID = false;
            this.CFG_USE_GUIDANCE_DURING_EXECUTION = false;
            this.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE = false;
            this.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE = false;
            this.TRANSF_VALIDATE_TRANSFORMATION = false;
            this.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER = false;
            this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS = new HashMap<>();
            this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS.put(
                    ArrayList.class,
                    (sm, toLabel) -> {
                        ArrayList<Object> result = new ArrayList<>();
                        ArrayList<?> arrayList = (ArrayList<?>) toLabel;
                        sm.registerLabelPair(toLabel, result);
                        for (Object o : arrayList) {
                            result.add(sm.getLabel(o));
                        }
                        return result;
                    }
            );
            this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = false;
            this.TRANSF_TRANSFORMATION_REQUIRED = true;
            this.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES = Collections.emptyList();
            this.SHUTDOWN_PARALLEL_TIMEOUT_IN_MS = 5000;
            this.VALS_SYMSINT_LB =    Optional.empty();
            this.VALS_SYMSINT_UB =    Optional.empty();
            this.VALS_SYMSDOUBLE_LB = Optional.empty();
            this.VALS_SYMSDOUBLE_UB = Optional.empty();
            this.VALS_SYMSFLOAT_LB =  Optional.empty();
            this.VALS_SYMSFLOAT_UB =  Optional.empty();
            this.VALS_SYMSLONG_LB =   Optional.empty();
            this.VALS_SYMSLONG_UB =   Optional.empty();
            this.VALS_SYMSSHORT_LB =  Optional.empty();
            this.VALS_SYMSSHORT_UB =  Optional.empty();
            this.VALS_SYMSBYTE_LB =   Optional.empty();
            this.VALS_SYMSBYTE_UB =   Optional.empty();
            this.VALS_SYMSCHAR_LB =   Optional.empty();
            this.VALS_SYMSCHAR_UB =   Optional.empty();
            this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS = false;
            this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS = false;
            this.VALS_TREAT_BOOLEANS_AS_INTS = false;
            this.ARRAYS_THROW_EXCEPTION_ON_OOB = false;
            this.SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH = false;
            this.SOLVER_ARGS = new LinkedHashMap<>();
            this.LOG_TIME_FOR_FIRST_PATH_SOLUTION = false;
            this.CALLBACK_FAIL = (me, f, sm) -> {};
            this.CALLBACK_BACKTRACK = (me, b, sm) -> {};
            this.CALLBACK_EXCEEDED_BUDGET = (me, b, sm) -> {};
            this.CALLBACK_PATH_SOLUTION = (me, ps, sm) -> {};
        }

        /**
         * @see MulibConfig#TREE_ENLIST_LEAVES
         */
        public MulibConfigBuilder setTREE_ENLIST_LEAVES(boolean TREE_ENLIST_LEAVES) {
            this.TREE_ENLIST_LEAVES = TREE_ENLIST_LEAVES;
            return this;
        }

        /**
         * @see MulibConfig#TREE_INDENTATION
         */
        public MulibConfigBuilder setTREE_INDENTATION(String TREE_INDENTATION) {
            this.TREE_INDENTATION = TREE_INDENTATION;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_IGNORE_FROM_PACKAGES
         */
        public MulibConfigBuilder setTRANSF_IGNORE_FROM_PACKAGES(Collection<String> TRANSF_IGNORE_FROM_PACKAGES) {
            this.TRANSF_IGNORE_FROM_PACKAGES = new HashSet<>(TRANSF_IGNORE_FROM_PACKAGES);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_IGNORE_CLASSES
         */
        public MulibConfigBuilder setTRANSF_IGNORE_CLASSES(Collection<Class<?>> TRANSF_IGNORE_CLASSES) {
            Set<Class<?>> temp = new HashSet<>(TRANSF_IGNORE_CLASSES);
            temp.add(Mulib.class);
            temp.add(Fail.class);
            this.TRANSF_IGNORE_CLASSES = Collections.unmodifiableSet(temp);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_REGARD_SPECIAL_CASE
         */
        public MulibConfigBuilder setTRANSF_REGARD_SPECIAL_CASE(Collection<Class<?>> TRANSF_REGARD_SPECIAL_CASE) {
            this.TRANSF_REGARD_SPECIAL_CASE = new HashSet<>(TRANSF_REGARD_SPECIAL_CASE);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_CONCRETIZE_FOR
         */
        public MulibConfigBuilder setTRANSF_CONCRETIZE_FOR(Collection<Class<?>> TRANSF_CONCRETIZE_FOR) {
            this.TRANSF_CONCRETIZE_FOR = new HashSet<>(TRANSF_CONCRETIZE_FOR);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR
         */
        public MulibConfigBuilder setTRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR(Collection<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR) {
            this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = new HashSet<>(TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR);
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_MAIN_STRATEGY
         */
        public MulibConfigBuilder setSEARCH_MAIN_STRATEGY(SearchStrategy SEARCH_MAIN_STRATEGY) {
            this.SEARCH_MAIN_STRATEGY = SEARCH_MAIN_STRATEGY;
            return this;
        }

        /**
         * @see MulibConfig#SOLVER_GLOBAL_TYPE
         */
        public MulibConfigBuilder setSOLVER_GLOBAL_TYPE(Solvers SOLVER_GLOBAL_TYPE) {
            this.SOLVER_GLOBAL_TYPE = SOLVER_GLOBAL_TYPE;
            return this;
        }

        /**
         * @see MulibConfig#BUDGETS_FIXED_ACTUAL_CP
         */
        public MulibConfigBuilder setBUDGET_FIXED_ACTUAL_CP(long BUDGET_FIXED_ACTUAL_CP) {
            this.BUDGET_FIXED_ACTUAL_CP = BUDGET_FIXED_ACTUAL_CP;
            return this;
        }

        /**
         * @see MulibConfig#BUDGETS_INCR_ACTUAL_CP
         */
        public MulibConfigBuilder setBUDGET_INCR_ACTUAL_CP(long BUDGET_INCR_ACTUAL_CP) {
            this.BUDGET_INCR_ACTUAL_CP = BUDGET_INCR_ACTUAL_CP;
            return this;
        }
        /**
         * @see MulibConfig#TRANSF_WRITE_TO_FILE
         */
        public MulibConfigBuilder setTRANSF_WRITE_TO_FILE(boolean TRANSF_WRITE_TO_FILE) {
            this.TRANSF_WRITE_TO_FILE = TRANSF_WRITE_TO_FILE;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_GENERATED_CLASSES_PATH
         */
        public MulibConfigBuilder setTRANSF_GENERATED_CLASSES_PATH(String TRANSF_GENERATED_CLASSES_PATH) {
            this.TRANSF_GENERATED_CLASSES_PATH = TRANSF_GENERATED_CLASSES_PATH;
            this.TRANSF_INCLUDE_PACKAGE_NAME = true;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_VALIDATE_TRANSFORMATION
         */
        public MulibConfigBuilder setTRANSF_VALIDATE_TRANSFORMATION(boolean TRANSF_VALIDATE_TRANSFORMATION) {
            this.TRANSF_VALIDATE_TRANSFORMATION = TRANSF_VALIDATE_TRANSFORMATION;
            return this;
        }

        /**
         * @param BUDGET_GLOBAL_TIME_IN_SECONDS The time budget in seconds
         * @see MulibConfig#BUDGETS_GLOBAL_TIME_IN_NANOSECONDS
         */
        public MulibConfigBuilder setBUDGET_GLOBAL_TIME_IN_SECONDS(long BUDGET_GLOBAL_TIME_IN_SECONDS) {
            this.BUDGET_GLOBAL_TIME_IN_SECONDS = BUDGET_GLOBAL_TIME_IN_SECONDS;
            return this;
        }

        /**
         * @see MulibConfig#BUDGETS_MAX_FAILS
         */
        public MulibConfigBuilder setBUDGET_MAX_FAILS(long BUDGET_MAX_FAILS) {
            this.BUDGET_MAX_FAILS = BUDGET_MAX_FAILS;
            return this;
        }

        /**
         * @see MulibConfig#BUDGETS_MAX_PATH_SOLUTIONS
         */
        public MulibConfigBuilder setBUDGET_MAX_PATH_SOLUTIONS(long BUDGET_MAX_PATH_SOLUTIONS) {
            this.BUDGET_MAX_PATH_SOLUTIONS = BUDGET_MAX_PATH_SOLUTIONS;
            return this;
        }

        /**
         * @see MulibConfig#BUDGETS_MAX_EXCEEDED_BUDGET
         */
        public MulibConfigBuilder setBUDGET_MAX_EXCEEDED(long BUDGET_MAX_EXCEEDED) {
            this.BUDGET_MAX_EXCEEDED = BUDGET_MAX_EXCEEDED;
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_ADDITIONAL_PARALLEL_STRATEGIES
         */
        public MulibConfigBuilder setSEARCH_ADDITIONAL_PARALLEL_STRATEGIES(SearchStrategy... searchStrategies) {
            this.setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(Arrays.asList(searchStrategies));
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_ADDITIONAL_PARALLEL_STRATEGIES
         */
        public MulibConfigBuilder setADDITIONAL_PARALLEL_SEARCH_STRATEGIES(List<SearchStrategy> searchStrategies) {
            this.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES = searchStrategies;
            return this;
        }

        /**
         * @see MulibConfig#SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS
         */
        public MulibConfigBuilder setSHUTDOWN_PARALLEL_TIMEOUT_IN_MS(long ms) {
            this.SHUTDOWN_PARALLEL_TIMEOUT_IN_MS = ms;
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_CHOICE_OPTION_DEQUE_TYPE
         */
        public MulibConfigBuilder setSEARCH_CHOICE_OPTION_DEQUE_TYPE(ChoiceOptionDeques choiceOptionDequeType) {
            this.SEARCH_CHOICE_OPTION_DEQUE_TYPE = choiceOptionDequeType;
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_ACTIVATE_PARALLEL_FOR
         */
        public MulibConfigBuilder setSEARCH_ACTIVATE_PARALLEL_FOR(long setFor) {
            this.SEARCH_ACTIVATE_PARALLEL_FOR = setFor;
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_LABEL_RESULT_VALUE
         */
        public MulibConfigBuilder setSEARCH_LABEL_RESULT_VALUE(boolean b) {
            this.SEARCH_LABEL_RESULT_VALUE = b;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS
         */
        public MulibConfigBuilder addTRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS(Class<?> clazz, BiFunction<SolverManager, Object, Object> transformation) {
            this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS.put(clazz, transformation);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER
         */
        public MulibConfigBuilder setTRANSF_LOAD_WITH_SYSTEM_CLASSLOADER(boolean b) {
            this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = b;
            return this;
        }

        /**
         * @param SYMSINT_LB The lower bound
         * @param SYMSINT_UB The upper bound
         * @see MulibConfig#VALS_SYMSINT_LB
         * @see MulibConfig#VALS_SYMSINT_UB
         */
        public MulibConfigBuilder setVALS_SYMSINT_DOMAIN(int SYMSINT_LB, int SYMSINT_UB) {
            if (SYMSINT_LB > SYMSINT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSINT_LB = Optional.of(SYMSINT_LB);
            this.VALS_SYMSINT_UB = Optional.of(SYMSINT_UB);
            return this;
        }

        /**
         * @param SYMSLONG_LB The lower bound
         * @param SYMSLONG_UB The upper bound
         * @see MulibConfig#VALS_SYMSLONG_LB
         * @see MulibConfig#VALS_SYMSLONG_UB
         */
        public MulibConfigBuilder setVALS_SYMSLONG_DOMAIN(long SYMSLONG_LB, long SYMSLONG_UB) {
            if (SYMSLONG_LB > SYMSLONG_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSLONG_LB = Optional.of(SYMSLONG_LB);
            this.VALS_SYMSLONG_UB = Optional.of(SYMSLONG_UB);
            return this;
        }

        /**
         * @param SYMSDOUBLE_LB The lower bound
         * @param SYMSDOUBLE_UB The upper bound
         * @see MulibConfig#VALS_SYMSDOUBLE_LB
         * @see MulibConfig#VALS_SYMSDOUBLE_UB
         */
        public MulibConfigBuilder setVALS_SYMSDOUBLE_DOMAIN(double SYMSDOUBLE_LB, double SYMSDOUBLE_UB) {
            if (SYMSDOUBLE_LB > SYMSDOUBLE_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSDOUBLE_UB = Optional.of(SYMSDOUBLE_UB);
            this.VALS_SYMSDOUBLE_LB = Optional.of(SYMSDOUBLE_LB);
            return this;
        }

        /**
         * @param SYMSFLOAT_LB The lower bound
         * @param SYMSFLOAT_UB The upper bound
         * @see MulibConfig#VALS_SYMSFLOAT_LB
         * @see MulibConfig#VALS_SYMSFLOAT_UB
         */
        public MulibConfigBuilder setVALS_SYMSFLOAT_DOMAIN(float SYMSFLOAT_LB, float SYMSFLOAT_UB) {
            if (SYMSFLOAT_LB > SYMSFLOAT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSFLOAT_UB = Optional.of(SYMSFLOAT_UB);
            this.VALS_SYMSFLOAT_LB = Optional.of(SYMSFLOAT_LB);
            return this;
        }

        /**
         * @param SYMSSHORT_LB The lower bound
         * @param SYMSSHORT_UB The upper bound
         * @see MulibConfig#VALS_SYMSSHORT_LB
         * @see MulibConfig#VALS_SYMSSHORT_UB
         */
        public MulibConfigBuilder setVALS_SYMSSHORT_DOMAIN(short SYMSSHORT_LB, short SYMSSHORT_UB) {
            if (SYMSSHORT_LB > SYMSSHORT_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSSHORT_UB = Optional.of(SYMSSHORT_UB);
            this.VALS_SYMSSHORT_LB = Optional.of(SYMSSHORT_LB);
            return this;
        }

        /**
         * @param SYMSBYTE_LB The lower bound
         * @param SYMSBYTE_UB The upper bound
         * @see MulibConfig#VALS_SYMSBYTE_LB
         * @see MulibConfig#VALS_SYMSBYTE_UB
         */
        public MulibConfigBuilder setVALS_SYMSBYTE_DOMAIN(byte SYMSBYTE_LB, byte SYMSBYTE_UB) {
            if (SYMSBYTE_LB > SYMSBYTE_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSBYTE_UB = Optional.of(SYMSBYTE_UB);
            this.VALS_SYMSBYTE_LB = Optional.of(SYMSBYTE_LB);
            return this;
        }

        /**
         * @param SYMSCHAR_LB The lower bound
         * @param SYMSCHAR_UB The upper bound
         * @see MulibConfig#VALS_SYMSCHAR_LB
         * @see MulibConfig#VALS_SYMSCHAR_UB
         */
        public MulibConfigBuilder setVALS_SYMSCHAR_DOMAIN(char SYMSCHAR_LB, char SYMSCHAR_UB) {
            if (SYMSCHAR_LB > SYMSCHAR_UB) {
                throw new MisconfigurationException("Upper bound must be larger or equal to lower bound.");
            }
            this.VALS_SYMSCHAR_UB = Optional.of(SYMSCHAR_UB);
            this.VALS_SYMSCHAR_LB = Optional.of(SYMSCHAR_LB);
            return this;
        }

        /**
         * @see MulibConfig#VALS_TREAT_BOOLEANS_AS_INTS
         */
        public MulibConfigBuilder setVALS_TREAT_BOOLEANS_AS_INTS(boolean VALS_TREAT_BOOLEANS_AS_INTS) {
            this.VALS_TREAT_BOOLEANS_AS_INTS = VALS_TREAT_BOOLEANS_AS_INTS;
            return this;
        }

        /**
         * @see MulibConfig#SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH
         */
        public MulibConfigBuilder setSOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH(boolean SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH) {
            this.SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH = SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH;
            return this;
        }

        /**
         * Assumes some default value ranges that are "high enough"
         */
        public MulibConfigBuilder assumeMulibDefaultValueRanges() {
            this.VALS_SYMSINT_LB =    Optional.of(Integer.MIN_VALUE);
            this.VALS_SYMSINT_UB =    Optional.of(Integer.MAX_VALUE);
            this.VALS_SYMSDOUBLE_LB = Optional.of(-100_000_000_000_000_000D);
            this.VALS_SYMSDOUBLE_UB = Optional.of(100_000_000_000_000_000D);
            this.VALS_SYMSFLOAT_LB =  Optional.of(-100_000_000_000_000_000F);
            this.VALS_SYMSFLOAT_UB =  Optional.of(100_000_000_000_000_000F);
            this.VALS_SYMSLONG_LB =   Optional.of(-100_000_000_000_000_000L);
            this.VALS_SYMSLONG_UB =   Optional.of(100_000_000_000_000_000L);
            this.VALS_SYMSSHORT_LB =  Optional.of(Short.MIN_VALUE);
            this.VALS_SYMSSHORT_UB =  Optional.of(Short.MAX_VALUE);
            this.VALS_SYMSBYTE_LB =   Optional.of(Byte.MIN_VALUE);
            this.VALS_SYMSBYTE_UB =   Optional.of(Byte.MAX_VALUE);
            this.VALS_SYMSCHAR_LB =   Optional.of(Character.MIN_VALUE);
            this.VALS_SYMSCHAR_UB =   Optional.of(Character.MAX_VALUE);
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_ALLOW_EXCEPTIONS
         */
        public MulibConfigBuilder setSEARCH_ALLOW_EXCEPTIONS(boolean SEARCH_ALLOW_EXCEPTIONS) {
            this.SEARCH_ALLOW_EXCEPTIONS = SEARCH_ALLOW_EXCEPTIONS;
            return this;
        }

        /**
         * @see MulibConfig#ARRAYS_THROW_EXCEPTION_ON_OOB
         */
        public MulibConfigBuilder setARRAYS_THROW_EXCEPTION_ON_OOB(boolean ARRAYS_THROW_EXCEPTION_ON_OOB) {
            this.ARRAYS_THROW_EXCEPTION_ON_OOB = ARRAYS_THROW_EXCEPTION_ON_OOB;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER
         */
        public MulibConfigBuilder setTRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER(boolean TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER) {
            this.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER = TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
            return this;
        }

        /**
         * @see MulibConfig#SEARCH_CONCOLIC
         */
        public MulibConfigBuilder setSEARCH_CONCOLIC(boolean SEARCH_CONCOLIC) {
            this.SEARCH_CONCOLIC = SEARCH_CONCOLIC;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_TRANSFORMATION_REQUIRED
         */
        public MulibConfigBuilder setTRANSF_TRANSFORMATION_REQUIRED(boolean TRANSF_TRANSFORMATION_REQUIRED) {
            this.TRANSF_TRANSFORMATION_REQUIRED = TRANSF_TRANSFORMATION_REQUIRED;
            return this;
        }

        /**
         * @see MulibConfig#SOLVER_ARGS
         */
        public MulibConfigBuilder putSOLVER_ARGS(String key, String val) {
            SOLVER_ARGS.put(key, val);
            return this;
        }

        /**
         * @see MulibConfig#SOLVER_ARGS
         */
        public MulibConfigBuilder putSOLVER_ARGS(String key, Function<?, ?> val) {
            SOLVER_ARGS.put(key, val);
            return this;
        }

        /**
         * @see MulibConfig#ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS
         */
        public MulibConfigBuilder setARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS(boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
            this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS = ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
            return this;
        }

        /**
         * @see MulibConfig#ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS
         */
        public MulibConfigBuilder setARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS(boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS) {
            this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS = ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS;
            return this;
        }

        /**
         * @see MulibConfig#FREE_INIT_ALIASING_FOR_FREE_OBJECTS
         */
        public MulibConfigBuilder setFREE_INIT_ALIASING_FOR_FREE_OBJECTS(boolean FREE_INIT_ALIASING_FOR_FREE_OBJECTS) {
            this.FREE_INIT_ALIASING_FOR_FREE_OBJECTS = FREE_INIT_ALIASING_FOR_FREE_OBJECTS;
            return this;
        }

        /**
         * @see MulibConfig#FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL
         */
        public MulibConfigBuilder setFREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL(boolean FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL) {
            this.FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL = FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
            return this;
        }

        /**
         * @see MulibConfig#FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL
         */
        public MulibConfigBuilder setFREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL(boolean FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL) {
            this.FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL = FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
            return this;
        }

        /**
         * @see MulibConfig#LOG_TIME_FOR_EACH_PATH_SOLUTION
         */
        public MulibConfigBuilder setLOG_TIME_FOR_EACH_PATH_SOLUTION(boolean LOG_TIME_FOR_EACH_PATH_SOLUTION) {
            this.LOG_TIME_FOR_EACH_PATH_SOLUTION = LOG_TIME_FOR_EACH_PATH_SOLUTION;
            return this;
        }

        /**
         * @see MulibConfig#LOG_TIME_FOR_FIRST_PATH_SOLUTION
         */
        public MulibConfigBuilder setLOG_TIME_FOR_FIRST_PATH_SOLUTION(boolean LOG_TIME_FOR_FIRST_PATH_SOLUTION) {
            this.LOG_TIME_FOR_FIRST_PATH_SOLUTION = LOG_TIME_FOR_FIRST_PATH_SOLUTION;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD
         */
        public MulibConfigBuilder setTRANSF_REPLACE_METHOD_WITH_OTHER_METHOD(Map<Method, Method> TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD) {
            this.TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD = TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD
         */
        public MulibConfigBuilder addREPLACE_METHOD_CALL_OF_NON_SUBSTITUTED_CLASS_WITH(Method toSubstitute, Method substituteBy) {
            this.TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD.put(toSubstitute, substituteBy);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH
         */
        public MulibConfigBuilder setTRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH(boolean TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH) {
            this.TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH = TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS
         * @see MulibConfig#TRANSF_REGARD_SPECIAL_CASE
         */
        public MulibConfigBuilder addModelClass(Class<?> toBeModelled, Class<?> modelClass) {
            this.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS.put(toBeModelled, modelClass);
            this.TRANSF_REGARD_SPECIAL_CASE.add(toBeModelled);
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_USE_DEFAULT_MODEL_CLASSES
         */
        public MulibConfigBuilder setTRANSF_USE_DEFAULT_MODEL_CLASSES(boolean val) {
            this.TRANSF_USE_DEFAULT_MODEL_CLASSES = val;
            return this;
        }

        /**
         * @see MulibConfig#CALLBACK_PATH_SOLUTION
         */
        public MulibConfigBuilder setCALLBACK_PATH_SOLUTION(TriConsumer<MulibExecutor, PathSolution, SolverManager> callback) {
            this.CALLBACK_PATH_SOLUTION = callback;
            return this;
        }

        /**
         * @see MulibConfig#CALLBACK_FAIL
         */
        public MulibConfigBuilder setCALLBACK_FAIL(TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.Fail, SolverManager> CALLBACK_FAIL) {
            this.CALLBACK_FAIL = CALLBACK_FAIL;
            return this;
        }

        /**
         * @see MulibConfig#CALLBACK_BACKTRACK
         */
        public MulibConfigBuilder setCALLBACK_BACKTRACK(TriConsumer<MulibExecutor, Backtrack, SolverManager> CALLBACK_BACKTRACK) {
            this.CALLBACK_BACKTRACK = CALLBACK_BACKTRACK;
            return this;
        }

        /**
         * @see MulibConfig#CALLBACK_EXCEEDED_BUDGET
         */
        public MulibConfigBuilder setCALLBACK_EXCEEDED_BUDGET(TriConsumer<MulibExecutor, ExceededBudget, SolverManager> CALLBACK_EXCEEDED_BUDGET) {
            this.CALLBACK_EXCEEDED_BUDGET = CALLBACK_EXCEEDED_BUDGET;
            return this;
        }

        private void addDefaultModelClasses() {
            addModelClass(Number.class, NumberReplacement.class);
            addModelClass(Integer.class, IntegerReplacement.class);
        }

        /**
         * @see MulibConfig#TRANSF_TREAT_SPECIAL_METHOD_CALLS
         */
        public MulibConfigBuilder setTRANSF_TREAT_SPECIAL_METHOD_CALLS(boolean TRANSF_TREAT_SPECIAL_METHOD_CALLS) {
            this.TRANSF_TREAT_SPECIAL_METHOD_CALLS = TRANSF_TREAT_SPECIAL_METHOD_CALLS;
            return this;
        }

        /**
         * @see MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID
         * @see MulibConfig#CFG_USE_GUIDANCE_DURING_EXECUTION
         * @see MulibConfig#CFG_TERMINATE_EARLY_ON_FULL_COVERAGE
         * @see MulibConfig#CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE
         */
        public MulibConfigBuilder setTRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID(
                boolean CFG_USE_HINTS_DURING_EXECUTION,
                boolean CFG_TERMINATE_EARLY_ON_FULL_COVERAGE,
                boolean CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE) {
            this.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID = true;
            this.CFG_USE_GUIDANCE_DURING_EXECUTION = CFG_USE_HINTS_DURING_EXECUTION;
            this.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE = CFG_TERMINATE_EARLY_ON_FULL_COVERAGE;
            this.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE = CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE;
            return this;
        }

        public MulibConfigBuilder setSOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS(boolean SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS) {
            this.SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS = SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS;
            return this;
        }

        /**
         * @return true if the config builder is set to work with concolic execution
         */
        public boolean isConcolic() {
            return SEARCH_CONCOLIC;
        }

        /**
         * @return A built MulibConfig
         */
        public MulibConfig build() {

            if (TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER && (!TRANSF_INCLUDE_PACKAGE_NAME || !TRANSF_WRITE_TO_FILE)) {
                throw new MisconfigurationException("When choosing to load with the SystemClassLoader, the files" +
                        " must be generated under the same path as the original classes. Set TRANSF_GENERATED_CLASSES_PATH" +
                        " for this and set TRANSF_WRITE_TO_FILE to true."
                );
            }


            if (BUDGET_INCR_ACTUAL_CP != 0) {
                if ((SEARCH_MAIN_STRATEGY != SearchStrategy.IDDFS
                        && !SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.contains(SearchStrategy.IDDFS))
                    && (SEARCH_MAIN_STRATEGY != SearchStrategy.IDDSAS && !SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.contains(SearchStrategy.IDDSAS))) {
                    throw new MisconfigurationException("When choosing an incremental budget, an IDDFS-based search strategy" +
                            " must be used. Currently, " + SEARCH_MAIN_STRATEGY + " is used as the global" +
                            " search strategy and " + SEARCH_ADDITIONAL_PARALLEL_STRATEGIES + " are used as additional" +
                            " search strategies.");
                }
            }

            if (BUDGET_INCR_ACTUAL_CP < 1
                    && ((SEARCH_MAIN_STRATEGY == SearchStrategy.IDDFS
                        || SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.contains(SearchStrategy.IDDFS))
                        || SEARCH_MAIN_STRATEGY == SearchStrategy.IDDSAS
                        || SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.contains(SearchStrategy.IDDSAS))) {
                throw new MisconfigurationException("When choosing IDDFS, an incremental budget must be specified.");
            }

            if (ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS && !ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS) {
                throw new MisconfigurationException("Since our way of representing free arrays of arrays or free arrays of objects " +
                        "is based on the assumption that we represent the contained arrays in the constraint solver, we cannot " +
                        "use eager indices for primitive elements but not for object elements.");
            }

            if (SEARCH_CONCOLIC && CFG_USE_GUIDANCE_DURING_EXECUTION) {
                throw new MisconfigurationException("Concolic execution cannot be guided by the CFG; - the concrete values guide the" +
                        " execution. Deactivate either the concolic mode or the hints using the CFG."
                );
            }

            if (TRANSF_USE_DEFAULT_MODEL_CLASSES) {
                addDefaultModelClasses();
                TRANSF_REGARD_SPECIAL_CASE.add(Boolean.class);
            }

            return new MulibConfig(
                    SEARCH_LABEL_RESULT_VALUE,
                    TREE_ENLIST_LEAVES,
                    TREE_INDENTATION,
                    SEARCH_MAIN_STRATEGY,
                    SEARCH_ADDITIONAL_PARALLEL_STRATEGIES,
                    SHUTDOWN_PARALLEL_TIMEOUT_IN_MS,
                    SOLVER_GLOBAL_TYPE,
                    SOLVER_ARGS,
                    BUDGET_FIXED_ACTUAL_CP,
                    BUDGET_INCR_ACTUAL_CP,
                    BUDGET_GLOBAL_TIME_IN_SECONDS,
                    BUDGET_MAX_FAILS,
                    BUDGET_MAX_PATH_SOLUTIONS,
                    BUDGET_MAX_EXCEEDED,
                    TRANSF_IGNORE_FROM_PACKAGES,
                    TRANSF_IGNORE_CLASSES,
                    TRANSF_REGARD_SPECIAL_CASE,
                    TRANSF_WRITE_TO_FILE,
                    TRANSF_GENERATED_CLASSES_PATH,
                    TRANSF_INCLUDE_PACKAGE_NAME,
                    TRANSF_VALIDATE_TRANSFORMATION,
                    TRANSF_CONCRETIZE_FOR,
                    TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR,
                    TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS,
                    TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER,
                    TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER,
                    TRANSF_TRANSFORMATION_REQUIRED,
                    SEARCH_CHOICE_OPTION_DEQUE_TYPE,
                    SEARCH_ACTIVATE_PARALLEL_FOR,
                    VALS_SYMSINT_LB,
                    VALS_SYMSINT_UB,
                    VALS_SYMSDOUBLE_LB,
                    VALS_SYMSDOUBLE_UB,
                    VALS_SYMSFLOAT_LB,
                    VALS_SYMSFLOAT_UB,
                    VALS_SYMSLONG_LB,
                    VALS_SYMSLONG_UB,
                    VALS_SYMSSHORT_LB,
                    VALS_SYMSSHORT_UB,
                    VALS_SYMSBYTE_LB,
                    VALS_SYMSBYTE_UB,
                    VALS_SYMSCHAR_LB,
                    VALS_SYMSCHAR_UB,
                    VALS_TREAT_BOOLEANS_AS_INTS,
                    ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS,
                    ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS,
                    ARRAYS_THROW_EXCEPTION_ON_OOB,
                    SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH,
                    SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS,
                    SEARCH_CONCOLIC,
                    SEARCH_ALLOW_EXCEPTIONS,
                    FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL,
                    FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL,
                    FREE_INIT_ALIASING_FOR_FREE_OBJECTS,
                    LOG_TIME_FOR_EACH_PATH_SOLUTION,
                    LOG_TIME_FOR_FIRST_PATH_SOLUTION,
                    TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD,
                    TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH,
                    TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS,
                    TRANSF_USE_DEFAULT_MODEL_CLASSES,
                    CALLBACK_PATH_SOLUTION,
                    CALLBACK_BACKTRACK,
                    CALLBACK_FAIL,
                    CALLBACK_EXCEEDED_BUDGET,
                    TRANSF_TREAT_SPECIAL_METHOD_CALLS,
                    TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID,
                    CFG_USE_GUIDANCE_DURING_EXECUTION,
                    CFG_TERMINATE_EARLY_ON_FULL_COVERAGE,
                    CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE
            );
        }
    }
    private MulibConfig(boolean SEARCH_LABEL_RESULT_VALUE,
                        boolean TREE_ENLIST_LEAVES,
                        String TREE_INDENTATION,
                        SearchStrategy SEARCH_MAIN_STRATEGY,
                        List<SearchStrategy> SEARCH_ADDITIONAL_PARALLEL_STRATEGIES,
                        long SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS,
                        Solvers SOLVER_GLOBAL_TYPE,
                        LinkedHashMap<String, Object> SOLVER_ARGS,
                        long BUDGETS_FIXED_ACTUAL_CP,
                        long BUDGETS_INCR_ACTUAL_CP,
                        long SECONDS_PER_INVOCATION,
                        long BUDGETS_MAX_FAILS,
                        long BUDGETS_MAX_PATH_SOLUTIONS,
                        long BUDGETS_MAX_EXCEEDED_BUDGET,
                        Set<String> TRANSF_IGNORE_FROM_PACKAGES,
                        Set<Class<?>> TRANSF_IGNORE_CLASSES,
                        Set<Class<?>> TRANSF_REGARD_SPECIAL_CASE,
                        boolean TRANSF_WRITE_TO_FILE,
                        String TRANSF_GENERATED_CLASSES_PATH,
                        boolean TRANSF_INCLUDE_PACKAGE_NAME,
                        boolean TRANSF_VALIDATE_TRANSFORMATION,
                        Set<Class<?>> TRANSF_CONCRETIZE_FOR,
                        Set<Class<?>> TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR,
                        Map<Class<?>, BiFunction<SolverManager, Object, Object>> TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS,
                        boolean TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER,
                        boolean TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER,
                        boolean TRANSF_TRANSFORMATION_REQUIRED,
                        ChoiceOptionDeques SEARCH_CHOICE_OPTION_DEQUE_TYPE,
                        long SEARCH_ACTIVATE_PARALLEL_FOR,
                        Optional<Integer> VALS_SYMSINT_LB,
                        Optional<Integer> VALS_SYMSINT_UB,
                        Optional<Double> VALS_SYMSDOUBLE_LB,
                        Optional<Double> VALS_SYMSDOUBLE_UB,
                        Optional<Float> VALS_SYMSFLOAT_LB,
                        Optional<Float> VALS_SYMSFLOAT_UB,
                        Optional<Long> VALS_SYMSLONG_LB,
                        Optional<Long> VALS_SYMSLONG_UB,
                        Optional<Short> VALS_SYMSSHORT_LB,
                        Optional<Short> VALS_SYMSSHORT_UB,
                        Optional<Byte> VALS_SYMSBYTE_LB,
                        Optional<Byte> VALS_SYMSBYTE_UB,
                        Optional<Character> VALS_SYMSCHAR_LB,
                        Optional<Character> VALS_SYMSCHAR_UB,
                        boolean VALS_TREAT_BOOLEANS_AS_INTS,
                        boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS,
                        boolean ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS,
                        boolean ARRAYS_THROW_EXCEPTION_ON_OOB,
                        boolean SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH,
                        boolean SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS,
                        boolean SEARCH_CONCOLIC,
                        boolean SEARCH_ALLOW_EXCEPTIONS,
                        boolean FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL,
                        boolean FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL,
                        boolean FREE_INIT_ALIASING_FOR_FREE_OBJECTS,
                        boolean LOG_TIME_FOR_EACH_PATH_SOLUTION,
                        boolean LOG_TIME_FOR_FIRST_PATH_SOLUTION,
                        Map<Method, Method> TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD,
                        boolean TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH,
                        Map<Class<?>, Class<?>> TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS,
                        boolean TRANSF_USE_DEFAULT_MODEL_CLASSES,
                        TriConsumer<MulibExecutor, PathSolution, SolverManager> CALLBACK_PATH_SOLUTION,
                        TriConsumer<MulibExecutor, Backtrack, SolverManager> CALLBACK_BACKTRACK,
                        TriConsumer<MulibExecutor, de.wwu.mulib.search.trees.Fail, SolverManager> CALLBACK_FAIL,
                        TriConsumer<MulibExecutor, ExceededBudget, SolverManager> CALLBACK_EXCEEDED_BUDGET,
                        boolean TRANSF_TREAT_SPECIAL_METHOD_CALLS,
                        boolean TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID,
                        boolean CFG_USE_GUIDANCE_DURING_EXECUTION,
                        boolean CFG_TERMINATE_EARLY_ON_FULL_COVERAGE,
                        boolean CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE
    ) {
        this.SEARCH_LABEL_RESULT_VALUE = SEARCH_LABEL_RESULT_VALUE;
        this.TREE_ENLIST_LEAVES = TREE_ENLIST_LEAVES;
        this.TREE_INDENTATION = TREE_INDENTATION;
        this.SEARCH_MAIN_STRATEGY = SEARCH_MAIN_STRATEGY;
        this.SEARCH_ADDITIONAL_PARALLEL_STRATEGIES = List.copyOf(SEARCH_ADDITIONAL_PARALLEL_STRATEGIES);
        this.SOLVER_GLOBAL_TYPE = SOLVER_GLOBAL_TYPE;
        this.SOLVER_ARGS = Collections.unmodifiableMap(new LinkedHashMap<>(SOLVER_ARGS));
        this.BUDGETS_FIXED_ACTUAL_CP =   0 != BUDGETS_FIXED_ACTUAL_CP ? Optional.of(BUDGETS_FIXED_ACTUAL_CP)   : Optional.empty();
        this.BUDGETS_INCR_ACTUAL_CP =    0 != BUDGETS_INCR_ACTUAL_CP ? Optional.of(BUDGETS_INCR_ACTUAL_CP)    : Optional.empty();
        this.BUDGETS_GLOBAL_TIME_IN_NANOSECONDS =   0 != SECONDS_PER_INVOCATION     ? Optional.of(SECONDS_PER_INVOCATION * 1_000_000_000) : Optional.empty();
        this.BUDGETS_MAX_FAILS =                0 != BUDGETS_MAX_FAILS ? Optional.of(BUDGETS_MAX_FAILS) : Optional.empty();
        this.BUDGETS_MAX_PATH_SOLUTIONS =       0 != BUDGETS_MAX_PATH_SOLUTIONS ? Optional.of(BUDGETS_MAX_PATH_SOLUTIONS) : Optional.empty();
        this.BUDGETS_MAX_EXCEEDED_BUDGET =     0 != BUDGETS_MAX_EXCEEDED_BUDGET ? Optional.of(BUDGETS_MAX_EXCEEDED_BUDGET) : Optional.empty();
        this.TRANSF_IGNORE_FROM_PACKAGES = Set.copyOf(TRANSF_IGNORE_FROM_PACKAGES);
        this.TRANSF_IGNORE_CLASSES = Set.copyOf(TRANSF_IGNORE_CLASSES);
        this.TRANSF_REGARD_SPECIAL_CASE = Set.copyOf(TRANSF_REGARD_SPECIAL_CASE);
        this.TRANSF_WRITE_TO_FILE = TRANSF_WRITE_TO_FILE;
        this.TRANSF_GENERATED_CLASSES_PATH = TRANSF_GENERATED_CLASSES_PATH;
        this.TRANSF_INCLUDE_PACKAGE_NAME = TRANSF_INCLUDE_PACKAGE_NAME;
        this.TRANSF_VALIDATE_TRANSFORMATION = TRANSF_VALIDATE_TRANSFORMATION;
        this.TRANSF_CONCRETIZE_FOR = TRANSF_CONCRETIZE_FOR;
        this.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR = TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR;
        this.TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS = Map.copyOf(TRANSF_IGNORED_CLASSES_TO_LABEL_FUNCTIONS);
        this.TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER = TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER;
        this.TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER = TRANSF_OVERWRITE_FILE_FOR_SYSTEM_CLASSLOADER;
        this.TRANSF_TRANSFORMATION_REQUIRED = TRANSF_TRANSFORMATION_REQUIRED;
        this.TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD = Map.copyOf(TRANSF_REPLACE_METHOD_WITH_OTHER_METHOD);
        this.TRANSF_USE_DEFAULT_MODEL_CLASSES = TRANSF_USE_DEFAULT_MODEL_CLASSES;
        this.TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS = Map.copyOf(TRANSF_REPLACE_TO_BE_TRANSFORMED_CLASS_WITH_SPECIFIED_CLASS);
        this.TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH = TRANSF_USE_DEFAULT_METHODS_TO_REPLACE_METHOD_CALLS_OF_NON_SUBSTITUTED_CLASS_WITH;
        this.SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS = SHUTDOWN_PARALLEL_TIMEOUT_ON_SHUTDOWN_IN_MS;
        this.SEARCH_CHOICE_OPTION_DEQUE_TYPE = SEARCH_CHOICE_OPTION_DEQUE_TYPE;
        this.SEARCH_ACTIVATE_PARALLEL_FOR = SEARCH_ACTIVATE_PARALLEL_FOR < 1 ? Optional.empty() : Optional.of(SEARCH_ACTIVATE_PARALLEL_FOR);
        this.VALS_SYMSINT_LB =    VALS_SYMSINT_LB.isEmpty() ? Optional.empty() :    Optional.of(Sint.concSint(VALS_SYMSINT_LB.get()));
        this.VALS_SYMSINT_UB =    VALS_SYMSINT_UB.isEmpty() ? Optional.empty() :    Optional.of(Sint.concSint(VALS_SYMSINT_UB.get()));
        this.VALS_SYMSDOUBLE_LB = VALS_SYMSDOUBLE_LB.isEmpty() ? Optional.empty() : Optional.of(Sdouble.concSdouble(VALS_SYMSDOUBLE_LB.get()));
        this.VALS_SYMSDOUBLE_UB = VALS_SYMSDOUBLE_UB.isEmpty() ? Optional.empty() : Optional.of(Sdouble.concSdouble(VALS_SYMSDOUBLE_UB.get()));
        this.VALS_SYMSFLOAT_LB =  VALS_SYMSFLOAT_LB.isEmpty() ? Optional.empty() :  Optional.of(Sfloat.concSfloat(VALS_SYMSFLOAT_LB.get()));
        this.VALS_SYMSFLOAT_UB =  VALS_SYMSFLOAT_UB.isEmpty() ? Optional.empty() :  Optional.of(Sfloat.concSfloat(VALS_SYMSFLOAT_UB.get()));
        this.VALS_SYMSLONG_LB =   VALS_SYMSLONG_LB.isEmpty() ? Optional.empty() :   Optional.of(Slong.concSlong(VALS_SYMSLONG_LB.get()));
        this.VALS_SYMSLONG_UB =   VALS_SYMSLONG_UB.isEmpty() ? Optional.empty() :   Optional.of(Slong.concSlong(VALS_SYMSLONG_UB.get()));
        this.VALS_SYMSSHORT_LB =  VALS_SYMSSHORT_LB.isEmpty() ? Optional.empty() :  Optional.of(Sshort.concSshort(VALS_SYMSSHORT_LB.get()));
        this.VALS_SYMSSHORT_UB =  VALS_SYMSSHORT_UB.isEmpty() ? Optional.empty() :  Optional.of(Sshort.concSshort(VALS_SYMSSHORT_UB.get()));
        this.VALS_SYMSBYTE_LB =   VALS_SYMSBYTE_LB.isEmpty() ? Optional.empty() :   Optional.of(Sbyte.concSbyte(VALS_SYMSBYTE_LB.get()));
        this.VALS_SYMSBYTE_UB =   VALS_SYMSBYTE_UB.isEmpty() ? Optional.empty() :   Optional.of(Sbyte.concSbyte(VALS_SYMSBYTE_UB.get()));
        this.VALS_SYMSCHAR_LB =   VALS_SYMSCHAR_LB.isEmpty() ? Optional.empty() :   Optional.of(Schar.concSchar(VALS_SYMSCHAR_LB.get()));
        this.VALS_SYMSCHAR_UB =   VALS_SYMSCHAR_UB.isEmpty() ? Optional.empty() :   Optional.of(Schar.concSchar(VALS_SYMSCHAR_UB.get()));

        this.VALS_TREAT_BOOLEANS_AS_INTS = VALS_TREAT_BOOLEANS_AS_INTS;
        this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS = ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS;
        this.ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS = ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS;
        this.ARRAYS_THROW_EXCEPTION_ON_OOB = ARRAYS_THROW_EXCEPTION_ON_OOB;
        this.SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH = SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH;
        this.SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS = SOLVER_KEEP_TRACK_OF_ORIGINAL_CONSTRAINTS;
        this.SEARCH_CONCOLIC = SEARCH_CONCOLIC;
        this.SEARCH_ALLOW_EXCEPTIONS = SEARCH_ALLOW_EXCEPTIONS;
        this.FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL = FREE_INIT_ENABLE_INITIALIZE_FREE_ARRAYS_WITH_NULL;
        this.FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL = FREE_INIT_ENABLE_INITIALIZE_FREE_OBJECTS_WITH_NULL;
        this.FREE_INIT_ALIASING_FOR_FREE_OBJECTS = FREE_INIT_ALIASING_FOR_FREE_OBJECTS;
        this.LOG_TIME_FOR_EACH_PATH_SOLUTION = LOG_TIME_FOR_EACH_PATH_SOLUTION;
        this.LOG_TIME_FOR_FIRST_PATH_SOLUTION = LOG_TIME_FOR_FIRST_PATH_SOLUTION;
        this.CALLBACK_PATH_SOLUTION = CALLBACK_PATH_SOLUTION;
        this.CALLBACK_BACKTRACK = CALLBACK_BACKTRACK;
        this.CALLBACK_FAIL = CALLBACK_FAIL;
        this.CALLBACK_EXCEEDED_BUDGET = CALLBACK_EXCEEDED_BUDGET;
        this.TRANSF_TREAT_SPECIAL_METHOD_CALLS = TRANSF_TREAT_SPECIAL_METHOD_CALLS;
        this.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID = TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID;
        this.CFG_USE_GUIDANCE_DURING_EXECUTION = CFG_USE_GUIDANCE_DURING_EXECUTION;
        this.CFG_TERMINATE_EARLY_ON_FULL_COVERAGE = CFG_TERMINATE_EARLY_ON_FULL_COVERAGE;
        this.CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE = CFG_CREATE_NEXT_EXECUTION_BASED_ON_COVERAGE;
    }

    @Override
    public String toString() {
        return "MulibConfig{"
                + "GLOBAL_SEARCH_STRATEGY=" + SEARCH_MAIN_STRATEGY
                + (SEARCH_ADDITIONAL_PARALLEL_STRATEGIES.isEmpty() ? "" : ",ADDITIONAL_PARALLEL_SEARCH_STRATEGIES=" + SEARCH_ADDITIONAL_PARALLEL_STRATEGIES)
                + ",GLOBAL_SOLVER_TYPE=" + SOLVER_GLOBAL_TYPE
                + ",HIGH_LEVEL_FREE_ARRAY_THEORY=" + SOLVER_HIGH_LEVEL_SYMBOLIC_OBJECT_APPROACH
                + ",USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS=" + ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_OBJECT_ELEMENTS
                + ",USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS=" + ARRAYS_USE_EAGER_INDEXES_FOR_FREE_ARRAY_PRIMITIVE_ELEMENTS
                + (!SOLVER_ARGS.isEmpty() ? "SOLVER_ARGS=" + SOLVER_ARGS : "")
                + ",CONCOLIC=" + SEARCH_CONCOLIC
                + (TREE_ENLIST_LEAVES ? ",ENLIST_LEAVES=" + true : "")
                + BUDGETS_FIXED_ACTUAL_CP.map(v -> ",FIXED_ACTUAL_CP_BUDGET=" + v).orElse("")
                + BUDGETS_INCR_ACTUAL_CP.map(v -> ",INCR_ACTUAL_CP_BUDGET=" + v).orElse("")
                + BUDGETS_GLOBAL_TIME_IN_NANOSECONDS.map(v -> ",NANOSECONDS_PER_INVOCATION=" + v).orElse("")
                + BUDGETS_MAX_FAILS.map(v -> ",MAX_FAILS=" + v).orElse("")
                + BUDGETS_MAX_PATH_SOLUTIONS.map(v -> ",MAX_PATH_SOLUTIONS=" + v).orElse("")
                + BUDGETS_MAX_EXCEEDED_BUDGET.map(v -> ",MAX_EXCEEDED_BUDGETS=" + v).orElse("")
                + ",TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER=" + TRANSF_LOAD_WITH_SYSTEM_CLASSLOADER
                + "}";
    }
}
