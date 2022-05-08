package de.wwu.mulib.transformations.asm_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import static de.wwu.mulib.transformations.asm_transformations.TransformationUtility.getBytecodeForClassNodeMethods;

public class AsmClassWriter extends MulibClassWriter<ClassNode> {

    public AsmClassWriter(int flags) {
        super(flags);
    }

    @Override
    public void validateClassNode(ClassNode classNode) {
        classNode.accept(this);
        byte[] bytes = this.toByteArray();
        ClassReader classReader = new ClassReader(bytes);
        ClassVisitor classVisitor = new CheckClassAdapter(this, true);
        Mulib.log.log(Level.INFO, "Validating ClassNode for " + classNode.name);
        classReader.accept(classVisitor, 0);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        CheckClassAdapter.verify(new ClassReader(bytes), true, printWriter);
    }

    @Override
    public void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, ClassNode classNode) {
        String className;
        OutputStream os;
        try {
            className = classNode.name;
            if (!includePackageName) {
                className = className.substring(classNode.name.lastIndexOf('/') + 1);
            }
            classNode.accept(this);
            os = new FileOutputStream(String.format(generatedClassesPathPattern, className));
            os.write(this.toByteArray());
            os.flush();
            os.close();
            this.visitSource(String.format(generatedClassesPathPattern, className), null); // TODO connect to debugger
        } catch (Exception e) {
            throw new MulibRuntimeException("Class file could not be written to file. Bytecode:\r\n" +
                    getBytecodeForClassNodeMethods(classNode), e);
        }
    }
}
