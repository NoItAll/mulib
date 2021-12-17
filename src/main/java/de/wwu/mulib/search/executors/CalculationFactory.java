package de.wwu.mulib.search.executors;

import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

public interface CalculationFactory {
    // TODO We also represent bytecode instructions that cannot be executed when having a primitive mapping from bytecode
    //  to library classes. For instance: lt(SE, VF, Sdouble, Sdouble). We might later want to collapse, e.g., the DCMP and
    //  lt(SE, VF, Sint, Sint) to these instructions.
    Sint add(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);
    
    Sint sub(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);
    
    Sint mul(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);
    
    Sint div(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);

    Sint mod(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);
    
    Sint neg(SymbolicExecution se, ValueFactory vf, Sint i);

    Sdouble add(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sdouble sub(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sdouble mul(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sdouble div(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sdouble mod(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sdouble neg(SymbolicExecution se, ValueFactory vf, Sdouble d);

    Slong add(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Slong sub(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Slong mul(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Slong div(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Slong mod(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Slong neg(SymbolicExecution se, ValueFactory vf, Slong l);

    Sfloat add(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sfloat sub(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sfloat mul(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sfloat div(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sfloat mod(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sfloat neg(SymbolicExecution se, ValueFactory vf, Sfloat f);

    Sbool and(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs);

    Sbool or(SymbolicExecution se, ValueFactory vf, Sbool lhs, Sbool rhs);

    Sbool not(SymbolicExecution se, ValueFactory vf, Sbool b);

    Sbool lt(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);

    Sbool lt(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Sbool lt(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sbool lt(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sbool lte(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);

    Sbool lte(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Sbool lte(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sbool lte(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sbool eq(SymbolicExecution se, ValueFactory vf, Sint lhs, Sint rhs);

    Sbool eq(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Sbool eq(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sbool eq(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

    Sint cmp(SymbolicExecution se, ValueFactory vf, Slong lhs, Slong rhs);

    Sint cmp(SymbolicExecution se, ValueFactory vf, Sdouble lhs, Sdouble rhs);

    Sint cmp(SymbolicExecution se, ValueFactory vf, Sfloat lhs, Sfloat rhs);

}
