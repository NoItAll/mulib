package de.wwu.mulib.transformations.asm_transformations;

import de.wwu.mulib.transformations.AbstractMulibTransformer;
import de.wwu.mulib.transformations.MulibClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class AsmMulibClassLoader extends MulibClassLoader<ClassNode> {

    AsmMulibClassLoader(AbstractMulibTransformer<ClassNode> transformer) {
        super(transformer);
    }
    @Override
    protected Class<?> getByteArrayOfPartnerClassFor(String original) {
        ClassNode classNode = transformer.getTransformedClassNode(original);
        ClassWriter classWriter = transformer.generateMulibClassWriter();
        classNode.accept(classWriter);
        return defineClass(original, classNode.name.replace("/", "."), classWriter.toByteArray());
    }
}
