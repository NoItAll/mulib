package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibIllegalStateException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A thread-safe {@link ChoicePointFactory} for pure symbolic execution.
 * Implements the case distinction mentioned in, e.g., {@link ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)}
 * in the method {@link SymbolicChoicePointFactory#threeCaseDistinctionTemplate(SymbolicExecution, Constraint)}.
 * Offers the method {@link SymbolicChoicePointFactory#determineBooleanWithNewBinaryChoice(SymbolicExecution, Constraint, Choice.ChoiceOption)}
 * to allow for overriding how the third case (a new choice point) should be dealt with.
 */
public class SymbolicChoicePointFactory implements ChoicePointFactory {

    private final MulibConfig config;
    private final CoverageCfg cfg;
    SymbolicChoicePointFactory(MulibConfig config, CoverageCfg cfg) {
        this.config = config;
        if ((cfg == null && config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID)
                || (cfg != null && !config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID)) {
            throw new MulibIllegalStateException("Must not set CFG if specified by configuration options");
        }
        this.cfg = cfg;
    }

    public static SymbolicChoicePointFactory getInstance(MulibConfig config, CoverageCfg cfg) {
        return new SymbolicChoicePointFactory(config, cfg);
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(rhs, lhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Eq.newInstance(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Not.newInstance(Eq.newInstance(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(rhs, lhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(rhs, lhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Eq.newInstance(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Not.newInstance(Eq.newInstance(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(rhs, lhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sdouble lhs, Sdouble rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(rhs, lhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Eq.newInstance(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Not.newInstance(Eq.newInstance(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(rhs, lhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Sfloat lhs, Sfloat rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(lhs, rhs));
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Lt.newInstance(rhs, lhs));
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Eq.newInstance(lhs, rhs));
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Not.newInstance(Eq.newInstance(lhs, rhs)));
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(rhs, lhs));
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, Slong lhs, Slong rhs) {
        return threeCaseDistinctionTemplate(se, Lte.newInstance(lhs, rhs));
    }

    @Override
    public boolean negatedBoolChoice(SymbolicExecution se, Sbool b) {
        return threeCaseDistinctionTemplate(se, Not.newInstance(b));
    }

    @Override
    public boolean boolChoice(final SymbolicExecution se, final Sbool b) {
        return threeCaseDistinctionTemplate(se, b);
    }

    private boolean choiceTemplateWithId(SymbolicExecution se, Supplier<Boolean> booleanSupplier, long id) {
        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            cfg.setCurrentCfgNodeIfNecessary(id);
            boolean result = booleanSupplier.get();
            cfg.traverseCurrentNodeWithDecision(id, result);
            cfg.addChoiceForCfgNode(se.getCurrentChoiceOption().getChoice(), id);
            return result;
        } else {
            // Ignore CoverageCfg here
            return booleanSupplier.get();
        }
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> ltChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> gtChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> eqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> notEqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> gteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, long id, Sint lhs, Sint rhs) {
        return choiceTemplateWithId(se, () -> lteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> ltChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> gtChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> eqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> notEqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> gteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, long id, Sdouble lhs, Sdouble rhs) {
        return choiceTemplateWithId(se, () -> lteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> ltChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> gtChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> eqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> notEqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> gteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, long id, Sfloat lhs, Sfloat rhs) {
        return choiceTemplateWithId(se, () -> lteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean ltChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> ltChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> gtChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean eqChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> eqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean notEqChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> notEqChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean gteChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> gteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean lteChoice(SymbolicExecution se, long id, Slong lhs, Slong rhs) {
        return choiceTemplateWithId(se, () -> lteChoice(se, lhs, rhs), id);
    }

    @Override
    public boolean boolChoice(SymbolicExecution se, long id, Sbool c) {
        return choiceTemplateWithId(se, () -> boolChoice(se, c), id);
    }

    @Override
    public boolean negatedBoolChoice(SymbolicExecution se, long id, Sbool b) {
        return choiceTemplateWithId(se, () -> negatedBoolChoice(se, b), id);
    }

    /**
     * All XYZChoice methods delegate their execution to this method
     * @param se The instance of symbolic execution used in this run
     * @param c The constraint the decision is based on
     * @return true, if the decision should evaluate to true, else false
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    protected boolean threeCaseDistinctionTemplate(
            SymbolicExecution se,
            Constraint c) {
        // Case 1: No actual choice, only concrete values
        if (c instanceof Sbool.ConcSbool) {
            return ((Sbool.ConcSbool) c).isTrue();
        }

        // This choice option must be stored either way to be set as a parent later on
        Choice.ChoiceOption currentChoiceOption = se.getCurrentChoiceOption();
        // Case 2: We are still on the known path
        // We encounter a new ChoiceOption. We check if a next ChoiceOption is present and which option is chosen.
        Optional<Boolean> possibleResult = checkIfStillOnKnownPath(se);
        if (possibleResult.isPresent()) {
            assert !config.CONCOLIC || (!se.nextIsOnKnownPath() || (ConcolicConstraintContainer.getConcSboolFromConcolic(c).isTrue() == possibleResult.get())) : config;
            return possibleResult.get();
        }

        assert !currentChoiceOption.isEvaluated() : "Should not occur";

        // Case 3: We are not on the known path. A new Choice is added. Potentially, we backtrack
        return determineBooleanWithNewBinaryChoice(
                se,
                c,
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

    /**
     * Determines how to proceed with newly encountered choices, i.e., the third case.
     * Delegates the decision on which of the two choice options of the new choice to evaluate first to
     * the {@link de.wwu.mulib.search.executors.MulibExecutor} of the current symbolic execution instance.
     * @param se The instance of symbolic execution used in the current execution run
     * @param constraint The base constraint of the new choice
     * @param currentChoiceOption The choice option that will be the parent of the new choice
     * @return true, if the constraint shall evaluate to true, else false; - might also throw {@link Backtrack},
     * depending on the decision of {@link de.wwu.mulib.search.executors.MulibExecutor}
     * @see SymbolicChoicePointFactory#threeCaseDistinctionTemplate(SymbolicExecution, Constraint)
     */
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
        Optional<Choice.ChoiceOption> possibleNextChoiceOption;
        if (config.CFG_USE_GUIDANCE_DURING_EXECUTION) {
            possibleNextChoiceOption = decisionBasedOnCfg(se, newChoice, cfg);
        } else {
            possibleNextChoiceOption = decisionNotBasedOnCfg(se, newChoice);
        }

        if (possibleNextChoiceOption.isEmpty()) {
            throw new Backtrack();
        } else {
            Choice.ChoiceOption newCo = possibleNextChoiceOption.get();
            return newCo.choiceOptionNumber == 0;
        }
    }

    private static Optional<Choice.ChoiceOption> decisionBasedOnCfg(SymbolicExecution se, Choice newChoice, CoverageCfg cfg) {
        CoverageCfg.CoverageInformation coverageInformation = cfg.getCoverageInformationForCurrentNode();
        if (coverageInformation == CoverageCfg.CoverageInformation.ALL_COVERED
                || coverageInformation == CoverageCfg.CoverageInformation.BOTH_NOT_COVERED
                || coverageInformation == CoverageCfg.CoverageInformation.NO_INFORMATION) {
            return decisionNotBasedOnCfg(se, newChoice);
        }
        final List<Choice.ChoiceOption> allOptions = newChoice.getChoiceOptions();
        Choice.ChoiceOption chosen;
        Choice.ChoiceOption other;
        if (coverageInformation == CoverageCfg.CoverageInformation.TRUE_BRANCH_NOT_COVERED) {
            chosen = allOptions.get(0);
            other = allOptions.get(1);
        } else {
            assert coverageInformation == CoverageCfg.CoverageInformation.FALSE_BRANCH_NOT_COVERED;
            chosen = allOptions.get(1);
            other = allOptions.get(0);
        }
        // We will simply reorder the list. The first choice option that is acceptable should be used
        List<Choice.ChoiceOption> reordered = List.of(chosen, other);
        Optional<Choice.ChoiceOption> possibleNewChoiceOption = se.decideOnNextChoiceOptionDuringExecution(reordered);
        se.notifyNewChoice(newChoice.depth, possibleNewChoiceOption.map(chosenOption -> chosenOption == chosen ? List.of(other) : List.of(chosen)).orElse(reordered));
        return possibleNewChoiceOption;
    }

    private static Optional<Choice.ChoiceOption> decisionNotBasedOnCfg(SymbolicExecution se, Choice newChoice) {
        List<Choice.ChoiceOption> potentiallySublist = newChoice.getChoiceOptions();
        Optional<Choice.ChoiceOption> possibleNextChoiceOption =
                se.decideOnNextChoiceOptionDuringExecution(potentiallySublist);

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
        return possibleNextChoiceOption;
    }

}
