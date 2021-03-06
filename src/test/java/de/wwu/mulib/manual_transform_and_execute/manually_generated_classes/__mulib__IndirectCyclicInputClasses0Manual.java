package de.wwu.mulib.manual_transform_and_execute.manually_generated_classes;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashSet;
import java.util.Set;

public class __mulib__IndirectCyclicInputClasses0Manual implements PartnerClass {
    public __mulib__IndirectCyclicInputClasses1Manual c;

    public __mulib__IndirectCyclicInputClasses0Manual(SymbolicExecution var1) {
        if (var1 != null) {
            this.c = null;
        }

    }

    public __mulib__IndirectCyclicInputClasses0Manual(IndirectCyclicInputClasses0Manual __mulib__originalObject, MulibValueTransformer var2) {
        var2.registerCopy(__mulib__originalObject, this);
        this.c = __mulib__originalObject.c != null ? (!var2.alreadyCreated(__mulib__originalObject.c) ? new __mulib__IndirectCyclicInputClasses1Manual(__mulib__originalObject.c, var2) : (__mulib__IndirectCyclicInputClasses1Manual)var2.getCopy(__mulib__originalObject)) : null;
    }

    public __mulib__IndirectCyclicInputClasses0Manual(__mulib__IndirectCyclicInputClasses0Manual __mulib__toCopy, MulibValueTransformer __mulib__valueTransformer) {
        __mulib__valueTransformer.registerCopy(__mulib__toCopy, this);
        this.c = __mulib__toCopy.c != null ? (!__mulib__valueTransformer.alreadyCreated(__mulib__toCopy.c) ? new __mulib__IndirectCyclicInputClasses1Manual(__mulib__toCopy.c, __mulib__valueTransformer) : (__mulib__IndirectCyclicInputClasses1Manual)__mulib__valueTransformer.getCopy(__mulib__toCopy)) : null;
    }

    public Object copy(MulibValueTransformer var1) {
        return new __mulib__IndirectCyclicInputClasses0Manual(this, var1);
    }

    public Object label(Object var1, MulibValueTransformer __mulib__valueTransformer, SolverManager var3) {
        IndirectCyclicInputClasses0Manual __mulib__originalObject = (IndirectCyclicInputClasses0Manual)var1;
        __mulib__originalObject.c = (IndirectCyclicInputClasses1Manual)(__mulib__valueTransformer.labelValue(this.c, var3));
        return __mulib__originalObject;
    }

    public Class getOriginalClass() {
        return IndirectCyclicInputClasses0Manual.class;
    }

    public __mulib__IndirectCyclicInputClasses0Manual() {
        super();
        SymbolicExecution se = SymbolicExecution.get();
        this.c = new __mulib__IndirectCyclicInputClasses1Manual(this);
    }

    public static Sint calc0(__mulib__IndirectCyclicInputClasses0Manual cycle) {
        SymbolicExecution se = SymbolicExecution.get();
        Set<Object> alreadySeen = new HashSet();
        Object current = cycle;

        while(!alreadySeen.contains(current)) {
            alreadySeen.add(current);
            if (SymbolicExecution.evalInstanceof((PartnerClass)current, __mulib__IndirectCyclicInputClasses0Manual.class, se).boolChoice(se)) {
                current = ((__mulib__IndirectCyclicInputClasses0Manual)current).c;
            } else {
                if (!SymbolicExecution.evalInstanceof((PartnerClass)current, __mulib__IndirectCyclicInputClasses1Manual.class, se).boolChoice(se)) {
                    throw new IllegalStateException("");
                }

                current = ((__mulib__IndirectCyclicInputClasses1Manual)current).c;
            }
        }

        return SymbolicExecution.concSint(alreadySeen.size(), se);
    }

    public static Sint calc1() {
        SymbolicExecution se = SymbolicExecution.get();
        __mulib__IndirectCyclicInputClasses0Manual cycle = new __mulib__IndirectCyclicInputClasses0Manual();
        return calc0(cycle);
    }
}