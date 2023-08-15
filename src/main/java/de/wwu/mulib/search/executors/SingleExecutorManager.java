package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.choice_points.CoverageCfg;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

/**
 * Executor manager for a single thread
 */
public class SingleExecutorManager extends MulibExecutorManager {


    /**
     * @param config The config
     * @param observedTree The search tree representing the evaluation of the search region
     * @param choicePointFactory The choice point factory; - must be compatible with the value and calculation factory
     * @param valueFactory The value factory; - must be compatible with the choice point and calculation factory
     * @param calculationFactory The calculation factory; - must be compatible with the value and choice point factory
     * @param mulibValueTransformer The mulib value transformed used to transform the initial arguments into search region types
     * @param searchRegionMethod The method handle used to invoke the search region
     * @param staticVariables A prototype of the manager of static variables
     * @param searchRegionArgs The transformed search region arguments
     * @param coverageCfg Can be null: The coverage control flow graph.
     */
    public SingleExecutorManager(
            MulibConfig config,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer,
            MethodHandle searchRegionMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs,
            CoverageCfg coverageCfg) {
        super(
                config,
                new ArrayList<>(),
                observedTree,
                choicePointFactory,
                valueFactory,
                calculationFactory,
                mulibValueTransformer,
                searchRegionMethod,
                staticVariables,
                searchRegionArgs,
                coverageCfg
        );
    }

    @Override
    protected void checkForFailure() { }
}
