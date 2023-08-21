package de.wwu.mulib.transformations;

import de.wwu.mulib.throwables.MulibRuntimeException;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_INDICATOR;

/**
 * Loads classes without writing them to disk
 * @param <T>
 */
public abstract class MulibClassLoader<T> extends ClassLoader {

    /**
     * The transformer using this class loader
     */
    protected final AbstractMulibTransformer<T> transformer;

    /**
     * @param transformer The transformer using this class loader
     */
    protected MulibClassLoader(AbstractMulibTransformer<T> transformer) {
        super(ClassLoader.getSystemClassLoader());
        this.transformer = transformer;
    }

    /**
     * Defines a class using the byte[] representation and adds it as a transformed class via
     * {@link AbstractMulibTransformer#addTransformedClass(String, Class)}
     * @param originalName The original name of the class
     * @param name The transformed name of the class
     * @param classFileBytes The class represented as bytes
     * @return The class
     */
    protected final Class<?> defineClass(String originalName, String name, byte[] classFileBytes) {
        Class<?> result = defineClass(name, classFileBytes, 0, classFileBytes.length);
        transformer.addTransformedClass(originalName, result);
        return result;
    }

    @Override
    public final Class<?> loadClass(String name) {
        if (!name.contains(_TRANSFORMATION_INDICATOR)) {
            try {
                return super.loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException(e);
            }
        }
        String adjusted = transformer.getSpecializedArrayTypeNameToOriginalTypeName().get(name);
        if (adjusted == null) {
            adjusted = name.replace(_TRANSFORMATION_INDICATOR, "");
        }
        Class<?> result = transformer.getTransformedClassForOriginalClassName(adjusted);
        if (result != null) {
            return result;
        }
        result = getPartnerClassForOriginal(adjusted);
        return result;
    }

    /**
     * Gets the original class node and transforms it into a byte array using {@link MulibClassFileWriter}.
     * Then calls {@link #defineClass(String, String, byte[])}
     * @param original The original class name
     * @return The partner class
     */
    protected abstract Class<?> getPartnerClassForOriginal(String original);
}