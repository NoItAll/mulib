package de.wwu.mulib.transformations.asm_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import static de.wwu.mulib.transformations.asm_transformations.AsmTransformationUtility.getBytecodeForClassNodeMethods;

public final class AsmClassFileWriter implements MulibClassFileWriter<ClassNode> {

    private static final int FLAGS = ClassWriter.COMPUTE_FRAMES;

    @Override
    public void validateClassNode(ClassNode classNode) {
        MulibAsmClassWriter mulibAsmClassWriter = new MulibAsmClassWriter(FLAGS);
        classNode.accept(mulibAsmClassWriter);
        byte[] bytes = mulibAsmClassWriter.toByteArray();
        ClassReader classReader = new ClassReader(bytes);
        ClassVisitor classVisitor = new CheckClassAdapter(mulibAsmClassWriter, true);
        Mulib.log.log(Level.INFO, "Validating ClassNode for " + classNode.name);
        classReader.accept(classVisitor, 0);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        CheckClassAdapter.verify(new ClassReader(bytes), true, printWriter);
    }

    @Override
    public void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, ClassNode classNode) {
        MulibAsmClassWriter mulibAsmClassWriter = new MulibAsmClassWriter(FLAGS);
        String className;
        OutputStream os;
        try {
            className = classNode.name;
            if (!includePackageName) {
                className = className.substring(classNode.name.lastIndexOf('/') + 1);
            }
            classNode.accept(mulibAsmClassWriter);
            os = new FileOutputStream(String.format(generatedClassesPathPattern, className));
            os.write(mulibAsmClassWriter.toByteArray());
            os.flush();
            os.close();
        } catch (Exception e) {
            throw new MulibRuntimeException("Class file could not be written to file. Bytecode:\r\n" +
                    getBytecodeForClassNodeMethods(classNode), e);
        }
    }

    @Override
    public byte[] toByteArray(ClassNode classNode) {
        MulibAsmClassWriter mulibAsmClassWriter = new MulibAsmClassWriter(FLAGS);
        classNode.accept(mulibAsmClassWriter);
        return mulibAsmClassWriter.toByteArray();
    }
}
