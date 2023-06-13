package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.transformations.soot_transformations.SootMulibTransformer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public interface MulibTransformer {

    Map<Field, Field> getAccessibleStaticFieldsOfTransformedClassesToOriginalClasses();

    Collection<Field> getAccessibleStaticFieldsOfTransformedClasses();
    Collection<Field> getAccessibleStaticFieldsAndConnectedAccessibleStaticFields(Class<?> c);

    void transformAndLoadClasses(Class<?>... toTransform);

    Class<?> transformType(Class<?> toTransform, boolean sarrayToRealArrayTypes);

    default Class<?> transformType(Class<?> toTransform) {
        return transformType(toTransform, false);
    }

    Class<?> transformMulibTypeBack(Class<?> toTransform);

    Class<?> getTransformedClass(Class<?> beforeTransformation);

    Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation);

    void setPartnerClass(Class<?> clazz, Class<?> partnerClass);

    boolean shouldBeTransformed(String classAsPath);

    static MulibTransformer get(MulibConfig config) { // TODO Share SootMulibTransformer if configs are compatible?
        return new SootMulibTransformer(config);
    }

}
