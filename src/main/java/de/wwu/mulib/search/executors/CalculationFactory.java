package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

public interface CalculationFactory {

    static CalculationFactory getInstance(MulibConfig config, ValueFactory vf) {
        if (config.CONCOLIC) {
            return ConcolicCalculationFactory.getInstance(config, vf);
        } else {
            return SymbolicCalculationFactory.getInstance(config, vf);
        }
    }

    Sint add(SymbolicExecution se, Sint lhs, Sint rhs);
    
    Sint sub(SymbolicExecution se, Sint lhs, Sint rhs);
    
    Sint mul(SymbolicExecution se, Sint lhs, Sint rhs);
    
    Sint div(SymbolicExecution se, Sint lhs, Sint rhs);

    Sint mod(SymbolicExecution se, Sint lhs, Sint rhs);
    
    Sint neg(SymbolicExecution se, Sint i);

    Sdouble add(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sdouble sub(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sdouble mul(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sdouble div(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sdouble mod(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sdouble neg(SymbolicExecution se, Sdouble d);

    Slong add(SymbolicExecution se, Slong lhs, Slong rhs);

    Slong sub(SymbolicExecution se, Slong lhs, Slong rhs);

    Slong mul(SymbolicExecution se, Slong lhs, Slong rhs);

    Slong div(SymbolicExecution se, Slong lhs, Slong rhs);

    Slong mod(SymbolicExecution se, Slong lhs, Slong rhs);

    Slong neg(SymbolicExecution se, Slong l);

    Sfloat add(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sfloat sub(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sfloat mul(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sfloat div(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sfloat mod(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sfloat neg(SymbolicExecution se, Sfloat f);

    Sbool and(SymbolicExecution se, Sbool lhs, Sbool rhs);

    Sbool or(SymbolicExecution se, Sbool lhs, Sbool rhs);

    Sbool not(SymbolicExecution se, Sbool b);

    Sbool lt(SymbolicExecution se, Sint lhs, Sint rhs);

    Sbool lt(SymbolicExecution se, Slong lhs, Slong rhs);

    Sbool lt(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sbool lt(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sbool lte(SymbolicExecution se, Sint lhs, Sint rhs);

    Sbool lte(SymbolicExecution se, Slong lhs, Slong rhs);

    Sbool lte(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sbool lte(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sbool eq(SymbolicExecution se, Sint lhs, Sint rhs);

    Sbool eq(SymbolicExecution se, Slong lhs, Slong rhs);

    Sbool eq(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sbool eq(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Sint cmp(SymbolicExecution se, Slong lhs, Slong rhs);

    Sint cmp(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    Sint cmp(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    Slong i2l(SymbolicExecution se, Sint i);

    Sfloat i2f(SymbolicExecution se, Sint i);

    Sdouble i2d(SymbolicExecution se, Sint i);

    Sint l2i(SymbolicExecution se, Slong l);

    Sfloat l2f(SymbolicExecution se, Slong l);

    Sdouble l2d(SymbolicExecution se, Slong l);

    Sint f2i(SymbolicExecution se, Sfloat f);

    Slong f2l(SymbolicExecution se, Sfloat f);

    Sdouble f2d(SymbolicExecution se, Sfloat f);

    Sint d2i(SymbolicExecution se, Sdouble d);

    Slong d2l(SymbolicExecution se, Sdouble d);

    Sfloat d2f(SymbolicExecution se, Sdouble d);

    Sbyte i2b(SymbolicExecution se, Sint i);

    Sshort i2s(SymbolicExecution se, Sint i);

    Sprimitive select(SymbolicExecution se, Sarray sarray, Sint index);

    Sprimitive store(SymbolicExecution se, Sarray sarray, Sint index, Sprimitive value);

    Sarray<?> select(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index);

    Sarray<?> store(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index, SubstitutedVar value);

    PartnerClass select(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index);

    PartnerClass store(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index, SubstitutedVar value);

    void representPartnerClassObjectIfNeeded(SymbolicExecution se, PartnerClass ihsr, Sint idOfContainingSarraySarray);

    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(SymbolicExecution se, PartnerClass var);


}
