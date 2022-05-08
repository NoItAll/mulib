package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.transformations.asm_transformations.AsmMulibTransformer;

public interface MulibTransformer {

    void transformAndLoadClasses(Class<?>... toTransform);

    Class<?> getTransformedClass(Class<?> beforeTransformation);

    Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation);

    /// TODO Is this here still necessary for a public interface?
    void setPartnerClass(Class<?> clazz, Class<?> partnerClass);

    boolean shouldBeConcretizedFor(String methodOwner);

    boolean shouldBeConcretizedFor(Class<?> methodOwner);

    boolean shouldTryToUseGeneralizedMethodCall(String methodOwner);

    boolean shouldTryToUseGeneralizedMethodCall(Class<?> methodOwner);

    boolean shouldBeTransformed(String classAsPath);

    boolean shouldBeTransformedFromDesc(String desc);

    static MulibTransformer get(MulibConfig config) {
        return new AsmMulibTransformer(config);
    }
}
