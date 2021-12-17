package de.wwu.mulib.search.choice_points;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Snumber;

public interface ChoicePointFactory {

    // Can later be used to, e.g., decide on whether or not aliasing is used in FreeObjects etc.
    boolean ltChoice(SymbolicExecution se, final Snumber lhs, final Snumber rhs);

    boolean gtChoice(SymbolicExecution se, final Snumber lhs, final Snumber rhs);

    boolean eqChoice(SymbolicExecution se, final Snumber lhs, final Snumber rhs);

    boolean gteChoice(SymbolicExecution se, final Snumber lhs, final Snumber rhs);

    boolean lteChoice(SymbolicExecution se, final Snumber lhs, final Snumber rhs);

    boolean boolChoice(SymbolicExecution se, final Sbool b);

//    void assume(SymbolicExecution se, final Sbool b); /// TODO
}
