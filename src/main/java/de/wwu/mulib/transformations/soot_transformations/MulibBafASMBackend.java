package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import org.objectweb.asm.ClassWriter;
import soot.SootClass;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Custom BafASMBackend to allow for using MulibSootClassWriter
 */
public class MulibBafASMBackend extends BafASMBackend {


    /**
     * Creates a new BafASMBackend with a given enforced java version
     *
     * @param sc          The SootClass the bytecode is to be generated for
     */
    public MulibBafASMBackend(SootClass sc) {
        // Bug if 0 is used
        super(sc, Options.java_version_11 /* 0 for automatic detection of Java version */);
    }

    @Override
    public void generateClassFile(OutputStream os) {
        if (cv != null) {
            throw new MulibRuntimeException("Must not reuse this object");
        }
        ClassWriter cw = new MulibSootClassWriter(ClassWriter.COMPUTE_FRAMES);
        cv = cw;
        generateByteCode();
        try {
            os.write(cw.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Could not write class file in the ASM-backend!", e);
        }
    }

    public byte[] getByteCode() {
        if (cv != null) {
            throw new MulibRuntimeException("Must not reuse this object");
        }
        ClassWriter cw = new MulibSootClassWriter(ClassWriter.COMPUTE_FRAMES);
        cv = cw;
        generateByteCode();
        return cw.toByteArray();
    }
}
