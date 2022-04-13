package de.wwu.mulib.transformations;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaintAnalysis {
    /**
     * Local variables that must be replaced by Mulib's set of classes (e.g. Sint) since
     * they are written into by a tainted instruction. Input variables for a method are always
     * tainted and are used to spread the taint.
     */
    public final Set<LocalVariableNode> taintedLocalVariables;
    /**
     * Instructions that must be replaced by a corresponding instruction accounting for tainted
     * types (e.g. int -> Sint implies that ILOAD -> ALOAD).
     */
    public final Set<AbstractInsnNode> taintedInstructions;
    /**
     * Instructions which must be wrapped since they are used in a tainted calculation, yet, are
     * not tainted themselves. This can happen if a non-tainted value (which is not written into by a
     * tainted instruction) is used as input for a tainted variable.
     */
    public final Set<AbstractInsnNode> instructionsToWrap;

    /**
     * Some bytecode instructions, such as checking whether a boolean is true, are executed on int-bytecode-instructions.
     * To differentiate in the transformation to Sbools and Sints, we add the affected instructions to the given list.
     */
    public final Set<AbstractInsnNode> taintedBooleanInsns;

    public final Set<AbstractInsnNode> taintedByteInsns;

    public final Set<AbstractInsnNode> taintedShortInsns;

    /**
     * If some instructions are not directly tainted but used by a boolean instruction, those instructions should be
     * wrapped.
     */
    public final Set<AbstractInsnNode> instructionsToWrapSinceUsedByBoolInsns;

    public final Set<AbstractInsnNode> instructionsToWrapSinceUsedByByteInsns;

    public final Set<AbstractInsnNode> instructionsToWrapSinceUsedByShortInsns;

    /**
     * Flag to determine how to treat return value.
     */
    public final boolean returnsBoolean;

    public final boolean returnsByte;

    public final boolean returnsShort;


    public final List<Frame<TaintValue>> frames;

    public final Map<AbstractInsnNode, String> concretizeForMethodCall;

    public final Set<MethodInsnNode> tryToGeneralize;

    public final int maxVarIndexInsn;


    public final Set<AbstractInsnNode> taintedNewObjectArrayInsns;

    public final Set<AbstractInsnNode> taintedNewArrayArrayInsns;

    /**
     * Since local variables that are of type double or long are replaced by objects, the index must be adjusted.
     * Double and float local variables are of size 2 while object references are of size 1. Hence, the index of
     * in the transformation "f(double d, int i) --> __mulib__f(Snumber d, Snumber i)" is 1 after the transformation,
     * not 2.
     * This array represents the mapping oldIndex -> newIndex.
     */
    public final Map<LocalVariableNode, Integer> newLvnIndices;

    /**
     * Contains a mapping from the instructions accessing a local variable's index to the new index.
     */
    public final Map<AbstractInsnNode, Integer> newIndexInsnIndices;

    public final Map<AbstractInsnNode, String> selectedTypeFromSarray;

    public TaintAnalysis(
            Set<LocalVariableNode> taintedLocalVariables,
            Set<AbstractInsnNode> taintedInstructions,
            Set<AbstractInsnNode> instructionsToWrap,
            Frame<TaintValue>[] frames,
            Set<AbstractInsnNode> taintedBooleanInsns, Set<AbstractInsnNode> instructionsToWrapSinceUsedByBoolInsns,
            Set<AbstractInsnNode> taintedByteInsns, Set<AbstractInsnNode> instructionsToWrapSinceUsedByByteInsns,
            Set<AbstractInsnNode> taintedShortInsns, Set<AbstractInsnNode> instructionsToWrapSinceUsedByShortInsns,
            boolean returnsBoolean, boolean returnsByte, boolean returnsShort,
            Map<AbstractInsnNode, String> concretizeForMethodCall,
            Set<MethodInsnNode> tryToGeneralize,
            Map<LocalVariableNode, Integer> newLvnIndices,
            Map<AbstractInsnNode, Integer> newIndexInsnIndices,
            int maxVarIndexInsn,
            Set<AbstractInsnNode> taintedNewObjectArrayInsns,
            Set<AbstractInsnNode> taintedNewArrayArrayInsns,
            Map<AbstractInsnNode, String> selectedTypeFromSarray) {
        this.taintedLocalVariables = taintedLocalVariables;
        this.taintedInstructions = taintedInstructions;
        this.instructionsToWrap = instructionsToWrap;
        this.frames = frames != null ? List.of(frames) : Collections.emptyList();
        this.taintedBooleanInsns = Collections.unmodifiableSet(taintedBooleanInsns);
        this.instructionsToWrapSinceUsedByBoolInsns = Collections.unmodifiableSet(instructionsToWrapSinceUsedByBoolInsns);
        this.taintedByteInsns = taintedByteInsns;
        this.instructionsToWrapSinceUsedByByteInsns = instructionsToWrapSinceUsedByByteInsns;
        this.taintedShortInsns = taintedShortInsns;
        this.instructionsToWrapSinceUsedByShortInsns = instructionsToWrapSinceUsedByShortInsns;
        this.returnsBoolean = returnsBoolean;
        this.returnsByte = returnsByte;
        this.returnsShort = returnsShort;
        this.concretizeForMethodCall = Collections.unmodifiableMap(concretizeForMethodCall);
        this.tryToGeneralize = Collections.unmodifiableSet(tryToGeneralize);
        this.newLvnIndices = Collections.unmodifiableMap(newLvnIndices);
        this.newIndexInsnIndices = Collections.unmodifiableMap(newIndexInsnIndices);
        this.maxVarIndexInsn = maxVarIndexInsn;
        this.taintedNewObjectArrayInsns = Collections.unmodifiableSet(taintedNewObjectArrayInsns);
        this.taintedNewArrayArrayInsns = Collections.unmodifiableSet(taintedNewArrayArrayInsns);
        this.selectedTypeFromSarray = Collections.unmodifiableMap(selectedTypeFromSarray);
    }
}
