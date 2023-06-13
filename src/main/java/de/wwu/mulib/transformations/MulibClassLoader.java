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
        String classNameWithoutPackage = name.substring(name.lastIndexOf('.') + 1);
        if (!classNameWithoutPackage.startsWith(_TRANSFORMATION_PREFIX)) {
            try {
                return super.loadClass(name, true);
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException(e);
            }
        }
        String withoutPrefix = name.replace(_TRANSFORMATION_PREFIX, "");
        Class<?> result = transformer.getTransformedClassForOriginalClassName(withoutPrefix);
        if (result != null) {
            return result;
        }
        result = getPartnerClassForOriginal(withoutPrefix);
        return result;
    }

    protected abstract Class<?> getPartnerClassForOriginal(String original);
}