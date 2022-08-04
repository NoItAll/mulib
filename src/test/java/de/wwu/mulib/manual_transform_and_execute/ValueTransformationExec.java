package de.wwu.mulib.manual_transform_and_execute;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.IndirectCyclicInputClasses0Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.IndirectCyclicInputClasses1Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.__mulib__IndirectCyclicInputClasses0Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.__mulib__IndirectCyclicInputClasses1Manual;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.solving.solvers.Z3IncrementalSolverManager;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueLabeler;
import de.wwu.mulib.transformations.MulibValueTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ValueTransformationExec {
    private MulibConfig mc;
    MulibTransformer mt;
    MulibValueCopier mvc;
    MulibValueTransformer mvt;
    MulibValueLabeler mvl;
    SolverManager sm;


    @BeforeEach
    public void setup() {
        mc = MulibConfig.builder().build();
        mt = MulibTransformer.get(mc);
        mt.setPartnerClass(IndirectCyclicInputClasses0Manual.class, __mulib__IndirectCyclicInputClasses0Manual.class);
        mt.setPartnerClass(IndirectCyclicInputClasses1Manual.class, __mulib__IndirectCyclicInputClasses1Manual.class);
        mvt = new MulibValueTransformer(mc, mt);
        mvc = new MulibValueCopier(null, mc);
        mvl = new MulibValueLabeler(mc, true);
        sm = new Z3IncrementalSolverManager(mc);
    }

    @Test
    public void testIndirectCyclicInputClassesTransform() {
        Object value = mvt.transform(new IndirectCyclicInputClasses0Manual());
        assertInstanceOf(__mulib__IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                __mulib__IndirectCyclicInputClasses1Manual.class,
                ((__mulib__IndirectCyclicInputClasses0Manual) value).c
        );
    }

    @Test
    public void testIndirectCyclicInputClassesCopy() {
        Object value = mvc.copyNonSprimitive(new __mulib__IndirectCyclicInputClasses0Manual());
        assertInstanceOf(__mulib__IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                __mulib__IndirectCyclicInputClasses1Manual.class,
                ((__mulib__IndirectCyclicInputClasses0Manual) value).c
        );
    }

    @Test
    public void testIndirectCyclicInputClassesLabel() {
        Object value = mvl.label(new __mulib__IndirectCyclicInputClasses0Manual(), sm);
        assertInstanceOf(IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                IndirectCyclicInputClasses1Manual.class,
                ((IndirectCyclicInputClasses0Manual) value).c
        );
    }
}
