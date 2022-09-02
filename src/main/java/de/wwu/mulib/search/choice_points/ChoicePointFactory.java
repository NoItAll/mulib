package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

public interface ChoicePointFactory {

    static ChoicePointFactory getInstance(MulibConfig config) {
        if (config.CONCOLIC) {
            return ConcolicChoicePointFactory.getInstance(config);
        } else {
            return SymbolicChoicePointFactory.getInstance(config);
        }
    }

    boolean ltChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean gtChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean eqChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean notEqChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean gteChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean lteChoice(SymbolicExecution se, final Sint lhs, final Sint rhs);

    boolean ltChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean gtChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean eqChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean notEqChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean gteChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean lteChoice(SymbolicExecution se, final Sdouble lhs, final Sdouble rhs);

    boolean ltChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean gtChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean eqChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean notEqChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean gteChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean lteChoice(SymbolicExecution se, final Sfloat lhs, final Sfloat rhs);

    boolean ltChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean gtChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean eqChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean notEqChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean gteChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean lteChoice(SymbolicExecution se, final Slong lhs, final Slong rhs);

    boolean boolChoice(SymbolicExecution se, final Sbool c);

    boolean negatedBoolChoice(SymbolicExecution se, final Sbool b);
}
