package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;

public interface CalculationFactory {
    // TODO We also represent bytecode instructions that cannot be executed when having a primitive mapping from bytecode
    //  to library classes. For instance: lt(SE, VF, Sdouble, Sdouble). We might later want to collapse, e.g., the DCMP and
    //  lt(SE, VF, Sint, Sint) to these instructions.

    static CalculationFactory getInstance(MulibConfig config) {
        if (config.CONCOLIC) {
            return ConcolicCalculationFactory.getInstance(config);
        } else {
            return SymbolicCalculationFactory.getInstance(config);
        }
    }

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

    Slong i2l(SymbolicExecution se, ValueFactory vf, Sint i);

    Sfloat i2f(SymbolicExecution se, ValueFactory vf, Sint i);

    Sdouble i2d(SymbolicExecution se, ValueFactory vf, Sint i);

    Sint l2i(SymbolicExecution se, ValueFactory vf, Slong l);

    Sfloat l2f(SymbolicExecution se, ValueFactory vf, Slong l);

    Sdouble l2d(SymbolicExecution se, ValueFactory vf, Slong l);

    Sint f2i(SymbolicExecution se, ValueFactory vf, Sfloat f);

    Slong f2l(SymbolicExecution se, ValueFactory vf, Sfloat f);

    Sdouble f2d(SymbolicExecution se, ValueFactory vf, Sfloat f);

    Sint d2i(SymbolicExecution se, ValueFactory vf, Sdouble d);

    Slong d2l(SymbolicExecution se, ValueFactory vf, Sdouble d);

    Sfloat d2f(SymbolicExecution se, ValueFactory vf, Sdouble d);

    Sbyte i2b(SymbolicExecution se, ValueFactory vf, Sint i);

    Sshort i2s(SymbolicExecution se, ValueFactory vf, Sint i);

    SubstitutedVar select(SymbolicExecution se, ValueFactory vf, Sarray sarray, Sint index);

    SubstitutedVar store(SymbolicExecution se, ValueFactory vf, Sarray sarray, Sint index, SubstitutedVar value);


}
