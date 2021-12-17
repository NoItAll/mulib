package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

public interface Snumber extends Sprimitive, NumericExpression {

    Sbool lt(Snumber rhs, SymbolicExecution se);

    Sbool lte(Snumber rhs, SymbolicExecution se);

    Sbool gt(Snumber rhs, SymbolicExecution se);

    Sbool gte(Snumber rhs, SymbolicExecution se);

    Sbool eq(Snumber rhs, SymbolicExecution se);

    Sint cmp(Snumber lhs, SymbolicExecution se);

    boolean ltChoice(SymbolicExecution se);

    boolean lteChoice(SymbolicExecution se);

    boolean eqChoice(SymbolicExecution se);

    boolean notEqChoice(SymbolicExecution se);

    boolean gtChoice(SymbolicExecution se);

    boolean gteChoice(SymbolicExecution se);

    boolean ltChoice(Snumber rhs, SymbolicExecution se);

    boolean lteChoice(Snumber rhs, SymbolicExecution se);

    boolean eqChoice(Snumber rhs, SymbolicExecution se);

    boolean notEqChoice(Snumber rhs, SymbolicExecution se);

    boolean gtChoice(Snumber rhs, SymbolicExecution se);

    boolean gteChoice(Snumber rhs, SymbolicExecution se);

    @Override
    boolean isPrimitive();
}
