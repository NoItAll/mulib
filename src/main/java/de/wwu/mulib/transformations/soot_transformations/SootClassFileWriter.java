package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.transformations.MulibClassFileWriter;
import org.apache.commons.io.FileUtils;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.validation.ValidationException;

import java.io.File;
import java.io.OutputStream;

public final class SootClassFileWriter implements MulibClassFileWriter<SootClass> {

    /* IMPLEMENTATION OF MULIBCLASSFILEWRITER-INTERFACE */
    @Override
    public void validateClassNode(SootClass classNode) {
        Body b = null;
        try {
            // Validate class structure:
            classNode.validate();
            for (SootMethod m : classNode.getMethods()) {
                if (!m.isAbstract()) {
                    // Validate aspects of the class
                    b = m.retrieveActiveBody();
                    b.validate();
                    b.validateLocals();
                    b.validateTraps();
                    b.validateUnitBoxes();
                    b.validateUses();
                    b.validateValueBoxes();
                }
            }
        } catch (ValidationException e) {
            throw new MulibRuntimeException("In class: " + classNode.getName() + "\r\n\r\n" + e.getMessage()
                    + ": " + e.getConcerned() + "\r\n\r\n" + b);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MulibRuntimeException(b == null ? "no body" : b.toString(), e);
        }
    }

    @Override
    public void writeClassToFile(String generatedClassesPathPattern, boolean includePackageName, SootClass classNode) {
        String className = classNode.getName();
        if (!includePackageName) {
            className = className.substring(classNode.getName().lastIndexOf('.') + 1);
        }
        MulibBafASMBackend backend = new MulibBafASMBackend(classNode);
        try {
            File f = new File(String.format(generatedClassesPathPattern, className.replace(".", "/")));
            OutputStream os = FileUtils.openOutputStream(f);
            backend.generateClassFile(os);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MulibRuntimeException("Class file could not be written to file. Class name: " + className);
        }
    }

    @Override
    public byte[] toByteArray(SootClass classNode) {
        MulibBafASMBackend backend = new MulibBafASMBackend(classNode);
        return backend.getByteCode();
    }
}
