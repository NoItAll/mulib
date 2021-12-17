package de.wwu.mulib.manual_transform_and_execute;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.IndirectCyclicInputClasses0Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.IndirectCyclicInputClasses1Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.__mulib__IndirectCyclicInputClasses0Manual;
import de.wwu.mulib.manual_transform_and_execute.manually_generated_classes.__mulib__IndirectCyclicInputClasses1Manual;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.solving.solvers.Z3IncrementalSolverManager;
import de.wwu.mulib.transformations.MulibTransformer;
import de.wwu.mulib.transformations.MulibValueTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValueTransformationExec {
    private MulibConfig mc;
    MulibTransformer mt;
    MulibValueTransformer mvt;
    SolverManager sm;


    @BeforeEach
    public void setup() {
        mc = MulibConfig.builder()
                .build();
        mt = new MulibTransformer(mc);
        mt.setPartnerClass(IndirectCyclicInputClasses0Manual.class, __mulib__IndirectCyclicInputClasses0Manual.class);
        mt.setPartnerClass(IndirectCyclicInputClasses1Manual.class, __mulib__IndirectCyclicInputClasses1Manual.class);
        mvt = new MulibValueTransformer(mc, mt, true);
        sm = new Z3IncrementalSolverManager(mc);
    }

    @Test
    public void testIndirectCyclicInputClassesTransform() {
        Object value = mvt.transformValue(new IndirectCyclicInputClasses0Manual());
        assertInstanceOf(__mulib__IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                __mulib__IndirectCyclicInputClasses1Manual.class,
                ((__mulib__IndirectCyclicInputClasses0Manual) value).c
        );
    }

    @Test
    public void testIndirectCyclicInputClassesCopy() {
        Object value = mvt.copySearchRegionRepresentation(new __mulib__IndirectCyclicInputClasses0Manual());
        assertInstanceOf(__mulib__IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                __mulib__IndirectCyclicInputClasses1Manual.class,
                ((__mulib__IndirectCyclicInputClasses0Manual) value).c
        );
    }

    @Test
    public void testIndirectCyclicInputClassesLabel() {
        Object value = mvt.labelValue(new __mulib__IndirectCyclicInputClasses0Manual(), sm);
        assertInstanceOf(IndirectCyclicInputClasses0Manual.class, value);
        assertInstanceOf(
                IndirectCyclicInputClasses1Manual.class,
                ((IndirectCyclicInputClasses0Manual) value).c
        );
    }
}
