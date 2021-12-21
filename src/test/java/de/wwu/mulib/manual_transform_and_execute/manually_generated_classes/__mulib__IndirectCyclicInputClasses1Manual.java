package de.wwu.mulib.manual_transform_and_execute.manually_generated_classes;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.transformations.MulibValueTransformer;

public class __mulib__IndirectCyclicInputClasses1Manual implements PartnerClass {
    public __mulib__IndirectCyclicInputClasses0Manual c;

    public __mulib__IndirectCyclicInputClasses1Manual(SymbolicExecution var1) {
        if (var1 != null) {
            this.c = null;
        }

    }

    public __mulib__IndirectCyclicInputClasses1Manual(IndirectCyclicInputClasses1Manual __mulib__originalObject, MulibValueTransformer var2) {
        var2.registerCopy(__mulib__originalObject, this);
        this.c = __mulib__originalObject.c != null ? (!var2.alreadyCreated(__mulib__originalObject.c) ? new __mulib__IndirectCyclicInputClasses0Manual(__mulib__originalObject.c, var2) : (__mulib__IndirectCyclicInputClasses0Manual)var2.getCopy(__mulib__originalObject.c)) : null;
    }

    public __mulib__IndirectCyclicInputClasses1Manual(__mulib__IndirectCyclicInputClasses1Manual __mulib__toCopy, MulibValueTransformer __mulib__valueTransformer) {
        __mulib__valueTransformer.registerCopy(__mulib__toCopy, this);
        this.c = __mulib__toCopy.c != null ? (!__mulib__valueTransformer.alreadyCreated(__mulib__toCopy.c) ? new __mulib__IndirectCyclicInputClasses0Manual(__mulib__toCopy.c, __mulib__valueTransformer) : (__mulib__IndirectCyclicInputClasses0Manual)__mulib__valueTransformer.getCopy(__mulib__toCopy.c)) : null;
    }

    public Object copy(MulibValueTransformer var1) {
        return new __mulib__IndirectCyclicInputClasses1Manual(this, var1);
    }

    public Object label(Object var1, MulibValueTransformer __mulib__valueTransformer, SolverManager var3) {
        IndirectCyclicInputClasses1Manual __mulib__originalObject = (IndirectCyclicInputClasses1Manual)var1;
        __mulib__originalObject.c = (IndirectCyclicInputClasses0Manual)(__mulib__valueTransformer.labelValue(this.c, var3));
        return __mulib__originalObject;
    }

    public Class getOriginalClass() {
        return IndirectCyclicInputClasses1Manual.class;
    }

    public __mulib__IndirectCyclicInputClasses1Manual(__mulib__IndirectCyclicInputClasses0Manual c0) {
        this.c = c0;
    }
}
