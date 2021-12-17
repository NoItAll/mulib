package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;

public class IndirectCyclicInputClasses1 {

    protected IndirectCyclicInputClasses2 c;

    public IndirectCyclicInputClasses1(IndirectCyclicInputClasses0 c0) {
        c = new IndirectCyclicInputClasses2(c0);
    }
}
