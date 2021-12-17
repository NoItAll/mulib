package de.wwu.mulib.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class TransformationUtility {

    public static int getNumInputs(MethodNode mn) {
        return getNumInputs(mn.desc, (mn.access & Opcodes.ACC_STATIC) != 0 );
    }

    public static int getNumInputs(String methodDesc, boolean isStatic) {
        return org.objectweb.asm.Type.getArgumentTypes(methodDesc).length
                + (isStatic ? 0 : 1); // Static vs not static.
    }
}
