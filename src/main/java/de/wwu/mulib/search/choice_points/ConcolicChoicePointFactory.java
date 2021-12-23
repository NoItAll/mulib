package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.substitutions.primitives.Sbool;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ConcolicChoicePointFactory extends SymbolicChoicePointFactory {

    ConcolicChoicePointFactory(MulibConfig config) {
        super(config);
    }

    public static ConcolicChoicePointFactory getInstance(MulibConfig config) {
        return new ConcolicChoicePointFactory(config);
    }

    protected boolean determineBooleanWithNewBinaryChoice(
            SymbolicExecution se,
            Constraint constraint,
            Choice.ChoiceOption currentChoiceOption) {
        Constraint innerConstraint = ((Sbool.SymSbool) constraint).getRepresentedConstraint();
        ConcolicConstraintContainer container = (ConcolicConstraintContainer) innerConstraint;

        // Find out which choice option is pre-chosen by concolic values
        boolean firstIsChosen = container.getConc().isTrue();
        Constraint actualConstraint = container.getSym();

        // Create Choice with ChoiceOptions (true false)
        Choice newChoice = new Choice(currentChoiceOption, actualConstraint, Not.newInstance(actualConstraint));
        // First, let the Executor of the current SymbolicExecution decide which choice is to be.
        // This also adds the constraint to the SolverManager's stack
        Choice.ChoiceOption chosenChoiceOption = firstIsChosen ?
                newChoice.getOption(0)
                :
                newChoice.getOption(1);

        chosenChoiceOption.setSatisfiable(); // Determined by concrete values

        // Only forward the predetermined choice option. This will trigger the necessary side-effects in GenericExecutor.
        Optional<Choice.ChoiceOption> chosen =
                se.decideOnNextChoiceOptionDuringExecution(Collections.singletonList(chosenChoiceOption));
        if (chosen.isEmpty()) { // Incremental budget exceeded.
            throw new Backtrack();
        }
        assert chosen.get() == chosenChoiceOption;
        // Then, add the new ChoiceOptions to the ExecutionManager's deque.
        // This depends on the chosen ChoiceOption and whether the incremental budget is exceeded.
        List<Choice.ChoiceOption> notChosenOptions;
        notChosenOptions = Collections.singletonList(
                firstIsChosen ? newChoice.getOption(1) : newChoice.getOption(0)
        );
        se.notifyNewChoice(newChoice.depth, notChosenOptions);
        Constraint newCpConstraint = chosenChoiceOption.getOptionConstraint();
        return newCpConstraint == actualConstraint;
    }
}
