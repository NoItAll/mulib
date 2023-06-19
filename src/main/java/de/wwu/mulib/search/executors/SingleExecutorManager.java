package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

public class SingleExecutorManager extends MulibExecutorManager {


    public SingleExecutorManager(
            MulibConfig config,
            SearchTree observedTree,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            MulibValueTransformer mulibValueTransformer,
            MethodHandle representedMethod,
            StaticVariables staticVariables,
            Object[] searchRegionArgs) {
        super(
                config,
                new ArrayList<>(),
                observedTree,
                choicePointFactory,
                valueFactory,
                calculationFactory,
                mulibValueTransformer,
                representedMethod,
                staticVariables,
                searchRegionArgs
        );
    }

    @Override
    public void checkForFailure() { }
}
