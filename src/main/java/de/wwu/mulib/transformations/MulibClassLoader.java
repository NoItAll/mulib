package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import static de.wwu.mulib.transformations.StringConstants._TRANSFORMATION_PREFIX;

public class MulibClassLoader extends ClassLoader {

    private final MulibTransformer transformer;

    MulibClassLoader(MulibTransformer transformer) {
        super(ClassLoader.getSystemClassLoader());
        this.transformer = transformer;
    }

    private Class<?> defineClass(String originalName, String name, byte[] classFileBytes) {
        Class<?> result = defineClass(name, classFileBytes, 0, classFileBytes.length);
        transformer.addTransformedClass(originalName, result);
        return result;
    }

    @Override
    public Class<?> loadClass(String name) {
        String classNameWithoutPackage = name.substring(name.lastIndexOf('.') + 1);
        if (!classNameWithoutPackage.startsWith(_TRANSFORMATION_PREFIX)) {
            try {
                return super.loadClass(name, true);
            } catch (ClassNotFoundException e) {
                throw new MulibRuntimeException(e);
            }
        }
        String withoutPrefix = name.replace(_TRANSFORMATION_PREFIX, "");
        Class<?> result = transformer.getTransformedClass(withoutPrefix);
        if (result != null) {
            return result;
        }
        ClassNode classNode = transformer.getTransformedClassNode(withoutPrefix);
        ClassWriter classWriter = new MulibClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        result = defineClass(withoutPrefix, classNode.name.replace("/", "."), classWriter.toByteArray());
        return result;
    }
}