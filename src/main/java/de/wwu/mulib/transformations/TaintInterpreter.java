package de.wwu.mulib.transformations;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


// Strongly inspired from org.objectweb.asm.tree.analysis.SourceInterpreter
public class TaintInterpreter extends Interpreter<TaintValue> implements Opcodes {

    public TaintInterpreter() {
        super(ASM9);
        if (getClass() != TaintInterpreter.class) {
            throw new IllegalStateException();
        }
    }

    protected TaintInterpreter(int api) {
        super(api);
    }

    @Override
    public TaintValue newValue(Type type) {
        if (type == Type.VOID_TYPE) {
            return null;
        }
        return new TaintValue(type == null ? 1 : type.getSize());
    }

    @Override
    public TaintValue newOperation(AbstractInsnNode insn) {
        int size;
        switch (insn.getOpcode()) {
            case LCONST_0:
            case LCONST_1:
            case DCONST_0:
            case DCONST_1:
                size = 2;
                break;
            case LDC:
                Object value = ((LdcInsnNode) insn).cst;
                size = value instanceof Long || value instanceof Double ? 2 : 1;
                break;
            case GETSTATIC:
                size = Type.getType(((FieldInsnNode) insn).desc).getSize();
                break;
            default:
                size = 1;
                break;
        }
        return new TaintValue(size, insn);
    }

    @Override
    public TaintValue copyOperation(AbstractInsnNode insn, TaintValue value) {
        TaintValue copyResult = new TaintValue(value.size, insn, value);
        value.instrsWhereUsed.add(insn);
        value.produces.add(copyResult);
        return copyResult;
    }

    @Override
    public TaintValue unaryOperation(AbstractInsnNode insn, TaintValue value) {
        int size;
        switch (insn.getOpcode()) {
            case LNEG:
            case DNEG:
            case I2L:
            case I2D:
            case L2D:
            case F2L:
            case F2D:
            case D2L:
                size = 2;
                break;
            case GETFIELD:
                size = Type.getType(((FieldInsnNode) insn).desc).getSize();
                break;
            default:
                size = 1;
                break;
        }
        TaintValue result = new TaintValue(size, insn, value);
        value.instrsWhereUsed.add(insn);
        value.produces.add(result);
        return result;
    }

    @Override
    public TaintValue binaryOperation(AbstractInsnNode insn, TaintValue value1, TaintValue value2) {
        int size;
        switch (insn.getOpcode()) {
            case LALOAD:
            case DALOAD:
            case LADD:
            case DADD:
            case LSUB:
            case DSUB:
            case LMUL:
            case DMUL:
            case LDIV:
            case DDIV:
            case LREM:
            case DREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                size = 2;
                break;
            default:
                size = 1;
                break;
        }
        return abstractOperation(size, insn, List.of(value1, value2));
    }

    @Override
    public TaintValue ternaryOperation(AbstractInsnNode insn, TaintValue value1, TaintValue value2, TaintValue value3) {
        return abstractOperation(1, insn, List.of(value1, value2, value3));
    }

    @Override
    public TaintValue naryOperation(AbstractInsnNode insn, List<? extends TaintValue> values) {
        int size;
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            size = 1;
        } else if (opcode == INVOKEDYNAMIC) {
            size = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc).getSize();
        } else {
            size = Type.getReturnType(((MethodInsnNode) insn).desc).getSize();
        }

        return abstractOperation(size, insn, values);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, TaintValue value, TaintValue expected) {
        expected.instrsWhereProduced.add(insn);
        expected.producedBy.add(value);
        value.instrsWhereUsed.add(insn);
        value.produces.add(expected);
    }

    @Override
    public TaintValue merge(TaintValue value1, TaintValue value2) {
        if (allContained(value1, value2)) {
            value2.instrsWhereUsed.addAll(value1.instrsWhereUsed);
            value2.produces.addAll(value1.produces);
            return value1;
        } else if (allContained(value2, value1)) {
            value1.instrsWhereUsed.addAll(value2.instrsWhereUsed);
            value1.produces.addAll(value2.produces);
            return value2;
        }
        int size = Math.max(value1.size, value2.size);
        Set<AbstractInsnNode> prods = new HashSet<>();
        Set<AbstractInsnNode> uses = new HashSet<>();
        Set<TaintValue> producedBy = new HashSet<>();
        Set<TaintValue> produces = new HashSet<>();
        prods.addAll(value1.instrsWhereProduced);
        prods.addAll(value2.instrsWhereProduced);
        uses.addAll(value1.instrsWhereUsed);
        uses.addAll(value2.instrsWhereUsed);
        producedBy.addAll(value1.producedBy);
        producedBy.addAll(value2.producedBy);
        produces.addAll(value1.produces);
        produces.addAll(value2.produces);

        // We also determine indirect uses
        value1.instrsWhereUsed.addAll(value2.instrsWhereUsed);
        value2.instrsWhereUsed.addAll(value1.instrsWhereUsed);
        value1.produces.addAll(value2.produces);
        value2.produces.addAll(value1.produces);
        return new TaintValue(size, prods, uses, producedBy, produces);

    }

    private static boolean allContained(TaintValue v1, TaintValue v2) {
        return v1.instrsWhereUsed.containsAll(v2.instrsWhereUsed) && v1.instrsWhereProduced.containsAll(v2.instrsWhereProduced);
    }


    private TaintValue abstractOperation(int size, AbstractInsnNode insn, List<? extends TaintValue> values) {
        TaintValue result = new TaintValue(size, insn, values.toArray(new TaintValue[0]));
        for (TaintValue value : values) {
            value.instrsWhereUsed.add(insn);
            value.produces.add(result);
        }
        return result;
    }
}
