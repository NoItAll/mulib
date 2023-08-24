package de.wwu.mulib.transformations;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.transformations.soot_transformations.SootMulibTransformer;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Core piece of Mulib. Is used before the exploration of the search region can begin.
 * Configurations for it which implementing classes must allow for are prefixed via "TRANSF_" in {@link MulibConfig}.
 * Transforms classes into their partner classes. Generates specific classes for arrays of arrays and arrays of
 * "usual" classes.
 * Also gathers the static fields of the search region.
 */
public interface MulibTransformer {

    /**
     * @return A map of (accessible static field of partner class, accessible static field of original class)-pairs
     * for static field (implicitly) referenced in the search region
     */
    Map<Field, Field> getAccessibleStaticFieldsOfTransformedClassesToOriginalClasses();

    /**
     * Transforms the specified classes so that {@link MulibTransformer#getTransformedClass(Class)} can return the
     * partner class.
     * Even if the passed classes are ignored according to the configuration,
     * they will be transformed as they have been explicitly stated to be transformed.
     * These classes that are dependencies for the specified classes are transformed according to the {@link MulibConfig}.
     * The classes are first transformed, alongside with all their referenced classes (if not set to be ignored).
     * Then, we replace direct accesses to fields with synthesized method calls. In these method calls
     * techniques like symbolic aliasing, checks for whether this object can in fact be null, etc. are performed.
     * Then, special method treatment according to {@link MulibConfig#TRANSF_TREAT_SPECIAL_METHOD_CALLS} is performed.
     * Thereafter, it is checked whether we should try to load classes using the system class loader and/or write them
     * to disk.
     * Finally, we validate the classes, if configured to do so.
     * @param toTransform Those classes that are transformed, even if they have been set to be ignored.
     */
    void transformAndLoadClasses(Class<?>... toTransform);

    /**
     * Transforms type. Arrays are transformed to their respective subclass of Sarray. Must be called after
     * the transformation has been performed.
     * @param toTransform Type to transform
     * @param sarraysToRealArrayTypes Should, e.g., Sint[].class be returned instead of SintSarray?
     * @return Transformed type
     */
    Class<?> transformType(Class<?> toTransform, boolean sarraysToRealArrayTypes);

    /**
     * @param toTransform The original class
     * @return The search region representation of this class, i.e., a partner class
     */
    default Class<?> transformType(Class<?> toTransform) {
        return transformType(toTransform, false);
    }

    /**
     * Transforms Sprimitives to the respective primitive. Sarrays are transformed to array types.
     * The original classes to partner classes should be easily identifiable by removing the {@link StringConstants#_TRANSFORMATION_INDICATOR}
     * @param toTransform The search region representation
     * @return The original representation
     */
    Class<?> transformMulibTypeBackIfNeeded(Class<?> toTransform);

    /**
     * Returns the partner class for the given class.
     * Throws an exception if the class could not be found
     * @param beforeTransformation The class the partner class of which is to be returned.
     * @return The partner class
     */
    Class<?> getTransformedClass(Class<?> beforeTransformation);

    /**
     * @param beforeTransformation The class before transforming it into the partner class.
     * @return Either the partner class, or, if not available, the original class.
     */
    Class<?> getPossiblyTransformedClass(Class<?> beforeTransformation);

    /**
     * @return A map of (java array types, specialized subtypes of sarray sarray and partner class sarray)-pairs
     * for array types found in the search region
     */
    Map<Class<?>, Class<?>> getArrayTypesToSpecializedSarrayClass();

    /**
     * Explicitly sets the partner class for a given class
     * @param clazz The original class
     * @param partnerClass The search region representation for the original class
     */
    void setPartnerClass(Class<?> clazz, Class<?> partnerClass);

    /**
     * @param classAsPath The class as a path or a class name
     * @return true, if the class should be transformed, else false
     */
    boolean shouldBeTransformed(String classAsPath);

    /**
     * @param config The configuration
     * @return The transformer
     */
    static MulibTransformer get(MulibConfig config) { // TODO Share SootMulibTransformer if configs are compatible?
        return new SootMulibTransformer(config);
    }

    /**
     * Should only be used if {@link MulibConfig#TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID} is set, else throws an exception
     * @return The number of choice points in the search region. This should also be equal to the highest identifier
     * of a choice point+1
     */
    long getNumberNumberedChoicePoints();

}
