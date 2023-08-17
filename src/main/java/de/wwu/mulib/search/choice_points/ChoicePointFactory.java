package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

/**
 * Interface for all factories generating choice points
 */
public interface ChoicePointFactory {

    /**
     * Constructs a new ChoicePointFactory, based on the configuration
     * @param config The configuration
     * @param coverageCfg The control-flow graph, e.g., used for calculating the cover. Can be null.
     * @return The new ChoicePointFactory
     */
    static ChoicePointFactory getInstance(MulibConfig config, CoverageCfg coverageCfg) {
        if (config.SEARCH_CONCOLIC) {
            return ConcolicChoicePointFactory.getInstance(config, coverageCfg);
        } else {
            return SymbolicChoicePointFactory.getInstance(config, coverageCfg);
        }
    }

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs < rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean ltChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs > rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean gtChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs == rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean eqChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs != rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean notEqChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs >= rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean gteChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * One of three cases:
     * 1. If the result can be determined without invoking the constraint solver, the result is returned immediately
     * 2. Else, if the current instance of {@link SymbolicExecution} is on a known path to a new choice option,
     * follows this path. This is done by returning true if this leads to the targeted choice option, else false.
     * 3. Else, constructs a new {@link de.wwu.mulib.search.trees.Choice} with two options, evaluating lhs <= rhs or its negation.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side number
     * @param rhs The right-hand side number
     * @return true, if the constraint of the encountered choice point evaluates to true, either based on either
     * (1) a concrete evaluation,
     * (2) a predetermined path through the search tree, or
     * (3) a constraint solver check
     */
    boolean lteChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    /**
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    boolean ltChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#gtChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gtChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#eqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean eqChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#notEqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean notEqChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#gteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gteChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#lteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean lteChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    /**
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    boolean ltChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#gtChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gtChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#eqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean eqChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#notEqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean notEqChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#gteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gteChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#lteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean lteChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    /**
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    boolean ltChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * @see ChoicePointFactory#gtChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gtChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * @see ChoicePointFactory#eqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean eqChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * @see ChoicePointFactory#notEqChoice(SymbolicExecution, Sint, Sint)
     */
    boolean notEqChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * @see ChoicePointFactory#gteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean gteChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * @see ChoicePointFactory#lteChoice(SymbolicExecution, Sint, Sint)
     */
    boolean lteChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    /**
     * Similar to methods comparing numbers, only that here, the constraint is directly given via a {@link Sbool}.
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    boolean boolChoice(SymbolicExecution se, final Sbool c);

    /**
     * Similar to methods comparing numbers, only that here, the constraint is directly given via a {@link Sbool}.
     * @see ChoicePointFactory#ltChoice(SymbolicExecution, Sint, Sint)
     */
    boolean negatedBoolChoice(SymbolicExecution se, final Sbool b);


    /* For identification of choice point to ensure coverage */

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean ltChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gtChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean eqChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean notEqChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gteChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean lteChoice(SymbolicExecution se, long id, final Sint lhs, final Sint rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean ltChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gtChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean eqChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean notEqChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gteChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean lteChoice(SymbolicExecution se, long id, final Sdouble lhs, final Sdouble rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean ltChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gtChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean eqChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean notEqChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gteChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean lteChoice(SymbolicExecution se, long id, final Sfloat lhs, final Sfloat rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean ltChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gtChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean eqChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean notEqChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean gteChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean lteChoice(SymbolicExecution se, long id, final Slong lhs, final Slong rhs);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean boolChoice(SymbolicExecution se, long id, final Sbool c);

    /**
     * Same as the method without the id-parameter, only that we also add an ID to identify the choice point's
     * position in the {@link CoverageCfg}. This is a transformation-time decision based on
     * {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID}.
     */
    boolean negatedBoolChoice(SymbolicExecution se, long id, final Sbool b);
}
