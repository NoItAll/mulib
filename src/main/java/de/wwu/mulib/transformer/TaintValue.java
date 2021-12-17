package de.wwu.mulib.transformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaintValue implements Value {

    public final int size;
    public final Set<AbstractInsnNode> instrsWhereProduced;
    public final Set<AbstractInsnNode> instrsWhereUsed;
    public final Set<TaintValue> producedBy;
    public final Set<TaintValue> produces;
    // This will not be set by the TaintInterpreter. Rather, it is set by a subsequent
    // analysis of the dependencies between instructions and TaintValues
    public boolean isActuallyTainted;
    public int frameNumber;
    public boolean isStackVariable = false;
    public int index;

    public TaintValue(int size) {
        this(size, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    public TaintValue(int size, AbstractInsnNode instrsWhereProduced) {
        this(size, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        this.instrsWhereProduced.add(instrsWhereProduced);
    }

    public TaintValue(int size, AbstractInsnNode instrsWhereProduced, TaintValue... producedBy) {
        this(size, instrsWhereProduced);
        List<TaintValue> producedByList = List.of(producedBy);
        this.producedBy.addAll(producedByList);
    }

    protected TaintValue(
            int size,
            Set<AbstractInsnNode> instrsWhereProduced,
            Set<AbstractInsnNode> instrsWhereUsed,
            Set<TaintValue> producedBy,
            Set<TaintValue> produces) {
        this.size = size;
        this.instrsWhereProduced = instrsWhereProduced;
        this.instrsWhereUsed = instrsWhereUsed;
        this.producedBy = producedBy;
        this.produces = produces;
    }

    @Override
    public int getSize() {
        return size;
    }
}
