package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.transformations.soot_transformations.SootMulibTransformer;

public interface MulibTransformer {

    void transformAndLoadClasses(Class<?>... toTransform);

    Class<?> transformType(Class<?> toTransform, boolean sarrayToRealArrayTypes);

    default Class<?> transformType(Class<?> toTransform) {
        return transformType(toTransform, false);
    }

    Class<?> getTransformedClass(Class<?> beforeTransformation);

    Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation);

    void setPartnerClass(Class<?> clazz, Class<?> partnerClass);

    boolean shouldBeTransformed(String classAsPath);

    static MulibTransformer get(MulibConfig config) { // TODO Share SootMulibTransformer if configs are compatible?
        return new SootMulibTransformer(config);
    }
}
