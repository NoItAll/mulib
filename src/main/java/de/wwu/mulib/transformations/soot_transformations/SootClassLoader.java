package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.transformations.AbstractMulibTransformer;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import de.wwu.mulib.transformations.MulibClassLoader;
import soot.SootClass;

public final class SootClassLoader extends MulibClassLoader<SootClass> {

    SootClassLoader(AbstractMulibTransformer<SootClass> transformer) {
        super(transformer);
    }

    @Override
    protected Class<?> getPartnerClassForOriginal(String original) {
        SootClass classNode = transformer.getTransformedClassNode(original);
        MulibClassFileWriter<SootClass> classWriter = transformer.generateMulibClassFileWriter();
        return defineClass(original, classNode.getName().replace("/", "."), classWriter.toByteArray(classNode));
    }
}
