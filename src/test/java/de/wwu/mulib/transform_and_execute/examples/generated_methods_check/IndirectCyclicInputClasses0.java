package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;

import java.util.HashSet;
import java.util.Set;

public class IndirectCyclicInputClasses0 {

    private final IndirectCyclicInputClasses1 c;

    public IndirectCyclicInputClasses0() {
        c = new IndirectCyclicInputClasses1(this);
    }

    public static int calc0(IndirectCyclicInputClasses0 cycle) {
        Set<Object> alreadySeen = new HashSet<>();
        Object current = cycle;
        while (!alreadySeen.contains(current)) {
            alreadySeen.add(current);
            if (current instanceof IndirectCyclicInputClasses0) {
                current = ((IndirectCyclicInputClasses0) current).c;
            } else if (current instanceof IndirectCyclicInputClasses1) {
                current = ((IndirectCyclicInputClasses1) current).c;
            } else if (current != null) {
                current = ((IndirectCyclicInputClasses2) current).c;
            } else {
                throw new IllegalStateException("");
            }
        }

        return alreadySeen.size();
    }

    public static int calc1() {
        IndirectCyclicInputClasses0 cycle = new IndirectCyclicInputClasses0();
        return calc0(cycle);
    }
}
