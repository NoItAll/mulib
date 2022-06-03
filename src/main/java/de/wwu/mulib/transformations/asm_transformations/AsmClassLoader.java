package de.wwu.mulib.transformations.asm_transformations;

import de.wwu.mulib.transformations.AbstractMulibTransformer;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import de.wwu.mulib.transformations.MulibClassLoader;
import org.objectweb.asm.tree.ClassNode;

public final class AsmClassLoader extends MulibClassLoader<ClassNode> {

    AsmClassLoader(AbstractMulibTransformer<ClassNode> transformer) {
        super(transformer);
    }
    @Override
    protected Class<?> getPartnerClassForOriginal(String original) {
        ClassNode classNode = transformer.getTransformedClassNode(original);
        MulibClassFileWriter<ClassNode> classWriter = transformer.generateMulibClassFileWriter();
        return defineClass(original, classNode.name.replace("/", "."), classWriter.toByteArray(classNode));
    }
}
