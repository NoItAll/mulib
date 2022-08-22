package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SymbolicChoicePointFactory implements ChoicePointFactory {

    private final MulibConfig config;
    SymbolicChoicePointFactory(MulibConfig config) {
        this.config = config;
    }

    public static SymbolicChoicePointFactory getInstance(MulibConfig config) {
        return new SymbolicChoicePointFactory(config);
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.gt(lhs, rhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.eq(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.not(se.eq(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.gte(lhs, rhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.lte(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.gt(lhs, rhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.eq(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.not(se.eq(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.gte(lhs, rhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, se.lte(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.gt(lhs, rhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.eq(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.not(se.eq(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.gte(lhs, rhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, se.lte(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.gt(lhs, rhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.eq(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.not(se.eq(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.gte(lhs, rhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, se.lte(lhs, rhs));
    }

    @Override
    public boolean negatedBoolChoice(SymbolicExecution se, Sbool b) {
        return threeCaseDistinctionTemplate(se, se.not(b));
    }

    @Override
    public boolean boolChoice(final SymbolicExecution se, final Constraint b) {
        return threeCaseDistinctionTemplate(se, b);
    }
    
    private boolean threeCaseDistinctionTemplate(
            SymbolicExecution se,
            Constraint b) {
        // Case 1: No actual choice, only concrete values
        if (b instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) b).isTrue();
        }

        // This choice option must be stored either way to be set as a parent later on
        Choice.ChoiceOption currentChoiceOption = se.getCurrentChoiceOption();
        // Case 2: We are still on the known path
        // We encounter a new ChoiceOption. We check if a next ChoiceOption is present and which option is chosen.
        Optional<Boolean> possibleResult = checkIfStillOnKnownPath(se);
        if (possibleResult.isPresent()) {
            assert !config.CONCOLIC || (!se.nextIsOnKnownPath() || (ConcolicConstraintContainer.getConcSboolFromConcolic(b).isTrue() == possibleResult.get())) : config;
            return possibleResult.get();
        }

        assert !currentChoiceOption.isEvaluated() : "Should not occur";

        // Case 3: We are not on the known path. A new Choice is added. Potentially, we backtrack
        return determineBooleanWithNewBinaryChoice(
                se,
                b,
                currentChoiceOption
        );
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

    protected boolean determineBooleanWithNewBinaryChoice(
            SymbolicExecution se,
            Constraint constraint,
            Choice.ChoiceOption currentChoiceOption) {
        if (constraint instanceof Sbool.SymSbool) {
            constraint = ((Sbool.SymSbool) constraint).getRepresentedConstraint();
        }
        // Create Choice with ChoiceOptions (true false)
        Choice newChoice = new Choice(currentChoiceOption, constraint, Not.newInstance(constraint));
        // First, let the Executor of the current SymbolicExecution decide which choice is to be.
        // This also adds the constraint to the SolverManager's stack
        Optional<Choice.ChoiceOption> possibleNextChoiceOption =
                se.decideOnNextChoiceOptionDuringExecution(newChoice.getChoiceOptions());

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
            Constraint newCpConstraint = possibleNextChoiceOption.get().getOptionConstraint();
            return newCpConstraint == constraint;
        }
    }

}
