package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ConcolicConstraintContainer;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.Not;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A thread-safe {@link ChoicePointFactory} for concolic execution.
 * Overrides {@link SymbolicChoicePointFactory#determineBooleanWithNewBinaryChoice(SymbolicExecution, Constraint, Choice.ChoiceOption)}
 * to not let the {@link de.wwu.mulib.search.executors.MulibExecutor} decide on the next choice option, but rather
 * let the already labeled symbolic values guide the execution.
 * It can happen that during concolic execution, some labels become stale because a constraint has been added that violates
 * them and that does not lead to a choice point (it might, for instance, be assumed that an index is contained in an array,
 * invalidating the actual given label).
 * In this case, we finish the evaluation and check for satisfiability at the very end in {@link de.wwu.mulib.search.executors.MulibExecutor}.
 */
public class ConcolicChoicePointFactory extends SymbolicChoicePointFactory {

    ConcolicChoicePointFactory(MulibConfig config, CoverageCfg coverageCfg) {
        // No guidance via a Cfg for ConcolicChoicePointFactory
        super(config, coverageCfg);
    }

    /**
     * Constructs a new instance
     * @param config The configuration
     * @param coverageCfg The control-flow graph used for, e.g. calculating coverage. Can be null.
     * @return A new instance of ConcolicChoicePointFactory
     */
    public static ConcolicChoicePointFactory getInstance(MulibConfig config, CoverageCfg coverageCfg) {
        return new ConcolicChoicePointFactory(config, coverageCfg);
    }


    @Override
    public boolean ltChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        // Here, we use se.lt instead of Lt.newInstance directly. This is because in concolic execution
        // we use the CalculationFactory to unwrap the concolic containers
        return threeCaseDistinctionTemplate(se, se.lt(lhs, rhs));
    }

    @Override
    public boolean gtChoice(SymbolicExecution se, Sint lhs, Sint rhs) {
        return threeCaseDistinctionTemplate(se, se.lt(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lte(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lt(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lte(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lt(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lte(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lt(rhs, lhs));
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
        return threeCaseDistinctionTemplate(se, se.lte(rhs, lhs));
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
    public boolean boolChoice(final SymbolicExecution se, final Sbool b) {
        return threeCaseDistinctionTemplate(se, b);
    }
    
    @Override
    protected boolean determineBooleanWithNewBinaryChoice(
            SymbolicExecution se,
            Constraint constraint,
            Choice.ChoiceOption currentChoiceOption) {
        Constraint innerConstraint = ((Sbool.SymSbool) constraint).getRepresentedConstraint();
        ConcolicConstraintContainer container = (ConcolicConstraintContainer) innerConstraint;

        // Find out which choice option is pre-chosen by concolic values
        boolean firstIsChosen = container.getConc().isTrue();
        Constraint actualConstraint = container.getSym().getRepresentedConstraint();

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
            se.notifyNewChoice(newChoice.depth, newChoice.getChoiceOptions());
            throw Backtrack.getInstance();
        }
        assert chosen.get() == chosenChoiceOption;
        // Then, add the new ChoiceOptions to the ExecutionManager's deque.
        // This depends on the chosen ChoiceOption and whether the incremental budget is exceeded.
        List<Choice.ChoiceOption> notChosenOptions = Collections.singletonList(
                firstIsChosen ? newChoice.getOption(1) : newChoice.getOption(0)
        );
        se.notifyNewChoice(newChoice.depth, notChosenOptions);
        Constraint newCpConstraint = chosenChoiceOption.getOptionConstraint();
        return newCpConstraint == actualConstraint;
    }
}
