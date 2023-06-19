package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;

public abstract class MulibClassLoader<T> extends ClassLoader {

    protected final AbstractMulibTransformer<T> transformer;

    protected MulibClassLoader(AbstractMulibTransformer<T> transformer) {
        super(ClassLoader.getSystemClassLoader());
        this.transformer = transformer;
    }

    protected final Class<?> defineClass(String originalName, String name, byte[] classFileBytes) {
        Class<?> result = defineClass(name, classFileBytes, 0, classFileBytes.length);
        transformer.addTransformedClass(originalName, result);
        return result;
    }

    @Override
    public final Class<?> loadClass(String name) {
        if (!name.contains(_TRANSFORMATION_PREFIX)) {
            try {
                return super.loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException(e);
            }
        }
        String adjusted = transformer.getSpecializedArrayTypeNameToOriginalTypeName().get(name);
        if (adjusted == null) {
            adjusted = name.replace(_TRANSFORMATION_PREFIX, "");
        }
        Class<?> result = transformer.getTransformedClassForOriginalClassName(adjusted);
        if (result != null) {
            return result;
        }
        result = getPartnerClassForOriginal(adjusted);
        return result;
    }

    protected abstract Class<?> getPartnerClassForOriginal(String original);
}