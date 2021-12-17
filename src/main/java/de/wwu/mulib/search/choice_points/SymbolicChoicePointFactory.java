package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Snumber;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SymbolicChoicePointFactory implements ChoicePointFactory {

    @Override
    public boolean ltChoice(SymbolicExecution se, Snumber lhs, Snumber rhs) {
        return boolChoice(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Snumber lhs, Snumber rhs) {
        return boolChoice(se, se.gt(lhs, rhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Snumber lhs, Snumber rhs) {
        return boolChoice(se, se.eq(lhs, rhs));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Snumber lhs, Snumber rhs) {
        return boolChoice(se, se.gte(lhs, rhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Snumber lhs, Snumber rhs) {
        return boolChoice(se, se.lte(lhs, rhs));
    }

    @Override
    public boolean boolChoice(final SymbolicExecution se, final Sbool b) {
        return threeCaseDistinctionTemplate(
                se,
                b,
                b instanceof Sbool.ConcSbool,
                Sbool.ConcSbool::isTrue,
                sb -> sb
        );
    }

    @SuppressWarnings("unchecked")
    private <P, C> boolean threeCaseDistinctionTemplate(
            SymbolicExecution se,
            P p,
            boolean isConcrete,
            Function<C, Boolean> concreteCase,
            Function<P, Constraint> newConstraintCase) {
        ExecutionBudgetManager ebm = se.getExecutionBudgetManager();
        // Case 1: No actual choice, only concrete values
        if (ebm.fixedPossibleChoicePointBudgetIsExceeded()) {
            throw new ChoicePointExceededBudget(ebm.getFixedPossibleChoicePointBudget());
        }
        if (isConcrete) {
            return concreteCase.apply((C) p);
        }

        // This choice option must be stored either way to be set as a parent later on
        Choice.ChoiceOption currentChoiceOption = se.getCurrentChoiceOption();
        // Case 2: We are still on the known path
        // We encounter a new ChoiceOption. We check if a next ChoiceOption is present and which option is chosen.
        Optional<Boolean> possibleResult = checkIfStillOnKnownPath(se);
        if (possibleResult.isPresent()) {
            return possibleResult.get();
        }

        assert !currentChoiceOption.isEvaluated() : "Should not occur";

        // Case 3: We are not on the known path. A new Choice is added. Potentially, we backtrack
        return determineBooleanWithNewBinaryChoice(se, newConstraintCase.apply(p), currentChoiceOption);
    }

    private Optional<Boolean> checkIfStillOnKnownPath(SymbolicExecution se) {
        ExecutionBudgetManager ebm = se.getExecutionBudgetManager();
        if (ebm.fixedActualChoicePointBudgetIsExceeded()) {
            throw new ChoicePointExceededBudget(ebm.getFixedActualChoicePointBudget());
        }
        if (se.transitionToNextChoiceOptionAndCheckIfOnKnownPath()) {
            Choice.ChoiceOption currentChoiceOption = se.getCurrentChoiceOption();
            assert currentChoiceOption.getChoice().getChoiceOptions().size() == 2 :
                    "For Booleans, there should always be two options.";
            return Optional.of(currentChoiceOption.choiceOptionNumber == 0);
        } else {
            return Optional.empty();
        }
    }

    private boolean determineBooleanWithNewBinaryChoice(
            SymbolicExecution se,
            Constraint constraint,
            Choice.ChoiceOption currentChoiceOption) {
        // Create Choice with ChoiceOptions (true false)
        Choice newChoice = new Choice(currentChoiceOption, constraint, Not.newInstance(constraint));
        // First, let the Executor of the current SymbolicExecution decide which choice is to be.
        // This also adds the constraint to the SolverManager's stack
        Optional<Choice.ChoiceOption> possibleNextChoiceOption = decideOnChoiceOption(se, newChoice);

        // Then, add the new ChoiceOptions to the ExecutionManager's deque.
        // This depends on the chosen ChoiceOption and whether the incremental budget is exceeded.
        List<Choice.ChoiceOption> notChosenOptions;
        if (possibleNextChoiceOption.isEmpty()) {
            notChosenOptions = newChoice.getChoiceOptions();
        } else {
            notChosenOptions = Collections.singletonList(
                    possibleNextChoiceOption.get().choiceOptionNumber == 0 ?
                            newChoice.getOption(1)
                            :
                            newChoice.getOption(0)
            );
        }
        se.notifyNewChoice(newChoice.depth, notChosenOptions);

        if (possibleNextChoiceOption.isEmpty()) {
            throw new Backtrack();
        } else {
            Constraint newCpConstraint = possibleNextChoiceOption.get().optionConstraint;
            return newCpConstraint == constraint;
        }
    }

    protected Optional<Choice.ChoiceOption> decideOnChoiceOption(SymbolicExecution se, Choice newChoice) {
        return se.decideOnNextChoiceOptionDuringExecution(newChoice);
    }
}
