package de.wwu.mulib.manual_transform_and_execute.manually_generated_classes;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClassObject;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.search.executors.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class __mulib__IndirectCyclicInputClasses0Manual extends PartnerClassObject {
    public __mulib__IndirectCyclicInputClasses1Manual c;

    public __mulib__IndirectCyclicInputClasses0Manual(SymbolicExecution var1) {
        if (var1 != null) {
            this.c = null;
        }

    }
    public __mulib__IndirectCyclicInputClasses0Manual(Object __mulib__originalObjectWithoutCast, MulibValueTransformer var2) {
        IndirectCyclicInputClasses0Manual __mulib__originalObject = (IndirectCyclicInputClasses0Manual) __mulib__originalObjectWithoutCast;
        var2.registerTransformedObject(__mulib__originalObject, this);
        this.c = __mulib__originalObject.c != null ? (!var2.alreadyTransformed(__mulib__originalObject.c) ? new __mulib__IndirectCyclicInputClasses1Manual(__mulib__originalObject.c, var2) : (__mulib__IndirectCyclicInputClasses1Manual)var2.getTransformedObject(__mulib__originalObjectWithoutCast)) : null;
    }

    public __mulib__IndirectCyclicInputClasses0Manual(__mulib__IndirectCyclicInputClasses0Manual __mulib__toCopy, MulibValueCopier __mulib__valueTransformer) {
        __mulib__valueTransformer.registerCopy(__mulib__toCopy, this);
        this.c = __mulib__toCopy.c != null ? (!__mulib__valueTransformer.alreadyCopied(__mulib__toCopy.c) ? new __mulib__IndirectCyclicInputClasses1Manual(__mulib__toCopy.c, __mulib__valueTransformer) : (__mulib__IndirectCyclicInputClasses1Manual)__mulib__valueTransformer.getCopy(__mulib__toCopy)) : null;
    }

    public Object copy(MulibValueCopier var1) {
        return new __mulib__IndirectCyclicInputClasses0Manual(this, var1);
    }

    public Object label(Object var1, SolverManager var3) {
        IndirectCyclicInputClasses0Manual __mulib__originalObject = (IndirectCyclicInputClasses0Manual)var1;
        __mulib__originalObject.c = (IndirectCyclicInputClasses1Manual)(var3.getLabel(this.c));
        return __mulib__originalObject;
    }

    public Class<?> __mulib__getOriginalClass() {
        return IndirectCyclicInputClasses0Manual.class;
    }

    @Override
    public void __mulib__initializeLazyFields(SymbolicExecution se) {

    }

    @Override
    public Map<String, Substituted> __mulib__getFieldNameToSubstituted() {
        return null;
    }

    public __mulib__IndirectCyclicInputClasses0Manual() {
        super();
        SymbolicExecution se = SymbolicExecution.get();
        this.c = new __mulib__IndirectCyclicInputClasses1Manual(this);
    }

    @Override
    protected void __mulib__blockCacheInPartnerClassFields() {

    }

    public static Sint calc0(__mulib__IndirectCyclicInputClasses0Manual cycle) {
        SymbolicExecution se = SymbolicExecution.get();
        Set<Object> alreadySeen = new HashSet<>();
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

        return se.concSint(alreadySeen.size());
    }

    public static Sint calc1() {
        SymbolicExecution se = SymbolicExecution.get();
        __mulib__IndirectCyclicInputClasses0Manual cycle = new __mulib__IndirectCyclicInputClasses0Manual();
        return calc0(cycle);
    }
}