package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.TaintAnalyzer.getFromTopOfStack;
import static de.wwu.mulib.transformations.TransformationUtility.getSingleDescsFromMethodParams;
import static de.wwu.mulib.transformations.TransformationUtility.splitMethodDesc;
import static org.objectweb.asm.Opcodes.*;

public class BooleanByteShortAndCharDistinguisher {
    /* INPUT */
    private final MethodNode oldMethodNode;
    private final Set<AbstractInsnNode> taintedInstructions;
    private final Set<AbstractInsnNode> instructionsToWrap;
    private final Frame<TaintValue>[] frames;
    private final Map<AbstractInsnNode, LocalVariableNode> varInsnNodesToReferencedLocalVar;

    /* OUTPUT */
    private boolean returnsBoolean;
    private boolean returnsByte;
    private boolean returnsShort;
    private final Set<AbstractInsnNode> taintedBoolInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByBoolInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedByteInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByByteInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedShortInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByShortInsns = new HashSet<>();

    public BooleanByteShortAndCharDistinguisher(
            MethodNode oldMethodNode,
            Set<AbstractInsnNode> taintedInstructions,
            Set<AbstractInsnNode> instructionsToWrap,
            Frame<TaintValue>[] frames,
            Map<AbstractInsnNode, LocalVariableNode> varInsnNodesToReferencedLocalVar) {
        this.oldMethodNode = oldMethodNode;
        this.taintedInstructions = taintedInstructions;
        this.instructionsToWrap = instructionsToWrap;
        this.frames = frames;
        this.varInsnNodesToReferencedLocalVar = varInsnNodesToReferencedLocalVar;
        // From the tainted instructions, determine those instructions that use boolean, byte, or short values. This is necessary
        // to create Sbools, Sbytes, and Sshorts since Java bytecode does not differentiate between booleans, bytes, shorts, and ints.
        // If the return type is boolean, we add this instruction to the tainted boolean instructions as well.
        determineTaintedBoolShortByteInsns();
    }

    // Determine the subset of tainted instructions and instructions to wrap which are boolean instructions.
    private void determineTaintedBoolShortByteInsns() { /// TODO Refactor!
        returnsBoolean = oldMethodNode.desc.substring(oldMethodNode.desc.lastIndexOf(')') + 1).equals("Z");
        returnsByte = oldMethodNode.desc.substring(oldMethodNode.desc.lastIndexOf(')') + 1).equals("B");
        returnsShort = oldMethodNode.desc.substring(oldMethodNode.desc.lastIndexOf(')') + 1).equals("S");
        // Loop to determine those instructions that are to be tainted:
        for (AbstractInsnNode ain : taintedInstructions) {
            // These instructions should not be tainted as they do not have any input value
            assert ain.getOpcode() != ICONST_0 && ain.getOpcode() != ICONST_1;
            if (ain.getOpcode() == INSTANCEOF) {
                int indexOfStackValue = oldMethodNode.instructions.indexOf(ain.getNext());
                Frame<TaintValue> frameOfInstanceofResult = frames[indexOfStackValue];
                TaintValue instanceofResult = getFromTopOfStack(frameOfInstanceofResult);
                assert taintedInstructions.containsAll(instanceofResult.instrsWhereUsed);
                taintedBoolInsns.addAll(instanceofResult.instrsWhereUsed);
            } else if (ain.getOpcode() == ILOAD
                    || ain.getOpcode() == ISTORE
                    || ain instanceof FieldInsnNode) {
                // We start by looking for boolean, byte, and short local variable nodes
                // Find the type of the local or field variable. If the local or field variable is not of types
                // boolean, byte, or short, continue with the next AbstractInsnNode
                String desc;
                if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                    // Get local variables in scope with the index
                    LocalVariableNode lvn =
                            varInsnNodesToReferencedLocalVar.get(ain);
                    // Check if the variable is boolean, byte, or short
                    if (lvn == null) {
                        // Anonymous variable
                        continue;
                    }
                    desc = lvn.desc;

                } else if (ain instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    desc = fin.desc;
                } else {
                    throw new MulibRuntimeException("Should not occur.");
                }
                byte type; // 0 = boolean, 1 = byte, 2 = short
                type = getTypeForDesc(desc);
                if (type == -1) {
                    continue;
                }
                // Get those values that were computes using the now-loaded value. Then, get those values that
                // use the respective computed values (if the computations are boolean comparisons).
                int indexOfLoaded = oldMethodNode.instructions.indexOf(
                        ain.getOpcode() == ILOAD || ain.getOpcode() == GETFIELD || ain.getOpcode() == GETSTATIC ?
                                // In this case, the next frame has the value loaded on the top of the stack
                                ain.getNext()
                                :
                                // Else is ISTORE, PUTFIELD, or PUTSTATIC. The value is then already on the top of the stack
                                ain
                );
                Frame<TaintValue> frameOfInstruction = frames[indexOfLoaded];
                // Now we mark those instructions as boolean, short, or byte instructions that use the instruction to
                // produce new booleans, shorts, or bytes.
                // Using these outputs are the end points of this inner analysis.
                TaintValue stackValueOfLoadingSpecialTypeVar = getFromTopOfStack(frameOfInstruction);

                Set<AbstractInsnNode> toAdd = new HashSet<>(stackValueOfLoadingSpecialTypeVar.instrsWhereUsed);
                toAdd.addAll(stackValueOfLoadingSpecialTypeVar.instrsWhereProduced);
                // Determine the set of instructions where this is added to:
                Set<AbstractInsnNode> taintedSpecialInsns;
                Set<AbstractInsnNode> toWrapSpecialInsns;
                if (type == 0) {
                    taintedSpecialInsns = taintedBoolInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByBoolInsns;
                } else if (type == 1) {
                    taintedSpecialInsns = taintedByteInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByByteInsns;
                } else if (type == 2) {
                    taintedSpecialInsns = taintedShortInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByShortInsns;
                } else {
                    throw new NotYetImplementedException();
                }
                decideOnTaintedOrWrappedForSpecialTypeInsn( //// TODO Refactor, let decideOnTaintedOrWrappedForSpecialTypeInsn decide on which set to use
                        //// TODO What does it mean for AALOAD to be contained here?
                        taintedSpecialInsns,
                        toWrapSpecialInsns,
                        toAdd
                );
            } else if (ain.getOpcode() == IRETURN && (returnsBoolean || returnsByte || returnsShort)) {
                int indexOfReturn = oldMethodNode.instructions.indexOf(ain);
                Frame<TaintValue> frameOfInstruction = frames[indexOfReturn];
                TaintValue stackValueReturned = getFromTopOfStack(frameOfInstruction);
                // Determine the set of instructions where this is added to:
                Set<AbstractInsnNode> taintedSpecialInsns;
                Set<AbstractInsnNode> toWrapSpecialInsns;
                if (returnsBoolean) {
                    taintedSpecialInsns = taintedBoolInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByBoolInsns;
                } else if (returnsByte) {
                    taintedSpecialInsns = taintedByteInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByByteInsns;
                } else {
                    assert returnsShort;
                    taintedSpecialInsns = taintedShortInsns;
                    toWrapSpecialInsns = toWrapSinceUsedByShortInsns;
                }
                Set<AbstractInsnNode> toAdd = new HashSet<>(stackValueReturned.instrsWhereUsed);
                toAdd.addAll(stackValueReturned.instrsWhereProduced);
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        taintedSpecialInsns,
                        toWrapSpecialInsns,
                        toAdd
                );
            } else if (ain instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) ain;
                String returnPart = splitMethodDesc(min.desc)[1];
                if (!"ZBS".contains(returnPart)) {
                    continue;
                }
                int indexOfValue = oldMethodNode.instructions.indexOf(min.getNext());
                Frame<TaintValue> frameOfPushedValue = frames[indexOfValue];
                TaintValue topOfStackOfValue = getFromTopOfStack(frameOfPushedValue);
                if (returnPart.equals("Z")) {
                    taintedBoolInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                } else if (returnPart.equals("B")) {
                    taintedByteInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                } else {
                    assert returnPart.equals("S");
                    taintedShortInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                }
            } else if (ain.getOpcode() == BASTORE || ain.getOpcode() == BALOAD) {
                // Differentiate BASTORE and BALOAD
                Frame<TaintValue> frameOfArrayOp = frames[oldMethodNode.instructions.indexOf(ain)];
                int offset = ain.getOpcode() == BASTORE ? 2 : 1;
                TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfArrayOp, offset);
                ArrayDeque<TaintValue> checkForSource = new ArrayDeque<>();
                checkForSource.add(arrayrefTaintValue);
                boolean added = false;
                String desc = null;
                outerLoop:
                while (!checkForSource.isEmpty()) {
                    TaintValue check = checkForSource.pop();
                    for (AbstractInsnNode potentialArrayInitializer : check.instrsWhereProduced) {
                        if (potentialArrayInitializer instanceof MethodInsnNode) {
                            String mdesc = ((MethodInsnNode) potentialArrayInitializer).desc;
                            desc = TransformationUtility.splitMethodDesc(mdesc)[1];
                        } else if (potentialArrayInitializer.getOpcode() == NEWARRAY) {
                            TypeInsnNode tin = (TypeInsnNode) potentialArrayInitializer;
                            desc = tin.desc;
                        } else if (potentialArrayInitializer.getOpcode() == MULTIANEWARRAY) {
                            MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) potentialArrayInitializer;
                            desc = mana.desc;
                        } else if (potentialArrayInitializer.getOpcode() == ALOAD) {
                            LocalVariableNode lvn = varInsnNodesToReferencedLocalVar.get(potentialArrayInitializer);
                            if (lvn == null) {
                                throw new NotYetImplementedException();
                            }
                            desc = lvn.desc;
                        } else if (potentialArrayInitializer instanceof FieldInsnNode) {
                            desc = ((FieldInsnNode) potentialArrayInitializer).desc;
                        }

                        if (desc != null) {
                            String primitiveDesc = desc.replace("[", "");
                            if (primitiveDesc.equals("Z")) {
                                this.taintedBoolInsns.add(ain);
                            } else if (primitiveDesc.equals("B")) {
                                this.taintedByteInsns.add(ain);
                            } else {
                                throw new NotYetImplementedException(primitiveDesc);
                            }
                            added = true;
                            break outerLoop;
                        }
                    }
                    // We did not break the outer loop
                    checkForSource.addAll(check.producedBy);
                }
                if (!added) {
                    throw new MulibRuntimeException("Could not decide to which type of array the BA{STORE,LOAD} belongs to.");
                }
            }
        }
        // Loop to determine those instructions that are to be wrapped
        for (AbstractInsnNode ain : instructionsToWrap) {
            byte type;
            if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                LocalVariableNode lvn = varInsnNodesToReferencedLocalVar.get(ain);
                // Check if the variable is boolean
                if (lvn == null) {
                    continue; // Not of type boolean, byte, or short --> not relevant here.
                }
                type = getTypeForDesc(lvn.desc);
                if (type == -1) {
                    continue;
                }
                if (type == 0) {
                    toWrapSinceUsedByBoolInsns.add(ain);
                } else if (type == 1) {
                    toWrapSinceUsedByByteInsns.add(ain);
                } else if (type == 2) {
                    toWrapSinceUsedByShortInsns.add(ain);
                } else {
                    throw new NotYetImplementedException();
                }
            } else if (ain instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) ain;
                type = getTypeForDesc(fin.desc);

                if (type == -1) {
                    continue;
                }
                if (type == 0) {
                    toWrapSinceUsedByBoolInsns.add(ain);
                } else if (type == 1) {
                    toWrapSinceUsedByByteInsns.add(ain);
                } else if (type == 2) {
                    toWrapSinceUsedByShortInsns.add(ain);
                } else {
                    throw new NotYetImplementedException();
                }
            } else if (List.of(ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, BIPUSH, SIPUSH)
                    .contains(ain.getOpcode())) {
                // Get taint value pushed on stack for the constant-push
                int index = oldMethodNode.instructions.indexOf(ain.getNext());
                TaintValue tvOfIconst = getFromTopOfStack(frames[index]);
                // Case 1: value is used as input of method
                List<AbstractInsnNode> relevantMethodInsnNodes =
                        tvOfIconst.instrsWhereUsed.stream()
                                .filter(insn ->
                                        insn instanceof MethodInsnNode)
                                .collect(Collectors.toList());

                // Check whether, and if, where used as input for MethodInsnNodes. It is sufficient to fint the first
                // method.
                Optional<AbstractInsnNode> optionalMethodInsnNode =
                        relevantMethodInsnNodes.stream()
                                .findFirst();
                if (optionalMethodInsnNode.isPresent()) {
                    MethodInsnNode anyMethodInsn = (MethodInsnNode) optionalMethodInsnNode.get();
                    // Check signature to see if it should be a boolean value
                    String[] paramAndReturn = splitMethodDesc(anyMethodInsn.desc);
                    String[] paramsDescSplit = getSingleDescsFromMethodParams(paramAndReturn[0]);
                    // Get frame of method insn
                    Frame<TaintValue> frameOfMethodInsn = frames[oldMethodNode.instructions.indexOf(anyMethodInsn)];
                    // Check stack at this position. At which position is tvOfIconst?
                    int posInParams = paramsDescSplit.length;
                    for (int i = frameOfMethodInsn.getStackSize() - 1; i >= 0; i--) {
                        posInParams--;
                        TaintValue tv = frameOfMethodInsn.getStack(i);
                        if (tv == tvOfIconst) {
                            break;
                        }
                    }
                    // Compare the position of constant-pushed value with desc
                    String desc = paramsDescSplit[posInParams];
                    if (desc.equals("Z")) {
                        toWrapSinceUsedByBoolInsns.add(ain);
                    } else if (desc.equals("B")) {
                        toWrapSinceUsedByByteInsns.add(ain);
                    } else if (desc.equals("S")) {
                        toWrapSinceUsedByShortInsns.add(ain);
                    }
                    continue; // We do not even need to check XASTORES, it is clear that this has to be wrapped.
                }

                // Case 2: Value used in SASTORE or BASTORE
                List<AbstractInsnNode> relevantXASTORENodes = tvOfIconst.instrsWhereUsed.stream()
                        .filter(insn -> insn.getOpcode() == SASTORE || insn.getOpcode() == BASTORE)
                        .collect(Collectors.toList());
                Optional<AbstractInsnNode> optionalXASTORENode =
                        relevantXASTORENodes.stream()
                                .findFirst();
                if (optionalXASTORENode.isEmpty()) {
                    continue;
                }
                AbstractInsnNode store = optionalXASTORENode.get();
                // Determine type of array this store belongs to
                Frame<TaintValue> frameOfStore = frames[oldMethodNode.instructions.indexOf(store)];
                // Make sure that we are talking about the value, and not the index here
                if (getFromTopOfStack(frameOfStore) != tvOfIconst) {
                    continue;
                }
                TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfStore, 2);
                ArrayDeque<TaintValue> checkForSource = new ArrayDeque<>();
                checkForSource.add(arrayrefTaintValue);
                boolean added = false;
                String desc = null;
                outerLoop:
                while (!checkForSource.isEmpty()) {
                    TaintValue check = checkForSource.pop();
                    for (AbstractInsnNode potentialArrayInitializer : check.instrsWhereProduced) {
                        if (potentialArrayInitializer instanceof MethodInsnNode) {
                            String mdesc = ((MethodInsnNode) potentialArrayInitializer).desc;
                            desc = TransformationUtility.splitMethodDesc(mdesc)[1];
                        } else if (potentialArrayInitializer.getOpcode() == NEWARRAY) {
                            TypeInsnNode tin = (TypeInsnNode) potentialArrayInitializer;
                            desc = tin.desc;
                        } else if (potentialArrayInitializer.getOpcode() == MULTIANEWARRAY) {
                            MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) potentialArrayInitializer;
                            desc = mana.desc;
                        }

                        if (desc != null) {
                            String primitiveDesc = desc.replace("[", "");
                            if (primitiveDesc.equals("Z")) {
                                this.toWrapSinceUsedByBoolInsns.add(ain);
                            } else if (primitiveDesc.equals("B")) {
                                this.toWrapSinceUsedByByteInsns.add(ain);
                            } else {
                                throw new NotYetImplementedException(primitiveDesc);
                            }
                            added = true;
                            break outerLoop;
                        }
                    }
                    // We did not break the outer loop
                    checkForSource.addAll(check.producedBy);
                }
                if (!added) {
                    throw new MulibRuntimeException("Could not decide to which type of array the BASTORE belongs to.");
                }
            } else if (ain.getOpcode() == BALOAD || ain.getOpcode() == BASTORE) {
                // Differentiate BASTORE and BALOAD
                Frame<TaintValue> frameOfArrayOp = frames[oldMethodNode.instructions.indexOf(ain)];
                int offset = ain.getOpcode() == BASTORE ? 2 : 1;
                TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfArrayOp, offset);
                ArrayDeque<TaintValue> checkForSource = new ArrayDeque<>();
                checkForSource.add(arrayrefTaintValue);
                boolean added = false;
                String desc = null;
                outerLoop:
                while (!checkForSource.isEmpty()) {
                    TaintValue check = checkForSource.pop();
                    for (AbstractInsnNode potentialArrayInitializer : check.instrsWhereProduced) {
                        if (potentialArrayInitializer instanceof MethodInsnNode) {
                            String mdesc = ((MethodInsnNode) potentialArrayInitializer).desc;
                            desc = TransformationUtility.splitMethodDesc(mdesc)[1];
                        } else if (potentialArrayInitializer.getOpcode() == NEWARRAY) {
                            TypeInsnNode tin = (TypeInsnNode) potentialArrayInitializer;
                            desc = tin.desc;
                        } else if (potentialArrayInitializer.getOpcode() == MULTIANEWARRAY) {
                            MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) potentialArrayInitializer;
                            desc = mana.desc;
                        } else if (potentialArrayInitializer.getOpcode() == ALOAD) {
                            LocalVariableNode lvn = varInsnNodesToReferencedLocalVar.get(potentialArrayInitializer);
                            if (lvn == null) {
                                throw new NotYetImplementedException();
                            }
                            desc = lvn.desc;
                        }

                        if (desc != null) {
                            String primitiveDesc = desc.replace("[", "");
                            if (primitiveDesc.equals("Z")) {
                                this.toWrapSinceUsedByBoolInsns.add(ain);
                            } else if (primitiveDesc.equals("B")) {
                                this.toWrapSinceUsedByByteInsns.add(ain);
                            } else {
                                throw new NotYetImplementedException(primitiveDesc);
                            }
                            added = true;
                            break outerLoop;
                        }
                    }
                    // We did not break the outer loop
                    checkForSource.addAll(check.producedBy);
                }
                assert added : "Could not decide to which type of array the BA{STORE,LOAD} belongs to.";
            }
        }

        // We determine which JumpInstructions are using purely boolean values
        Set<AbstractInsnNode> jumpInsns = taintedInstructions.stream()
                .filter(insn -> insn instanceof JumpInsnNode)
                .collect(Collectors.toSet());
        assert instructionsToWrap.stream().noneMatch(insn -> insn instanceof JumpInsnNode);
        for (AbstractInsnNode ain : jumpInsns) {
            int index = oldMethodNode.instructions.indexOf(ain);
            if (ain.getOpcode() == IFEQ || ain.getOpcode() == IFNE) {
                // The int is on top of the stack
                TaintValue topOfStack = getFromTopOfStack(frames[index]);
                if (jumpIsBoolean(topOfStack)) {
                    decideOnTaintedOrWrappedForSpecialTypeInsn(
                            taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                            topOfStack.instrsWhereProduced);
                }
            } else if (ain.getOpcode() == IF_ICMPEQ || ain.getOpcode() == IF_ICMPNE) {
                // Get the two values from the stack used here
                TaintValue topOfStack = getFromTopOfStack(frames[index]);
                TaintValue secondTopOfStack = getFromTopOfStack(frames[index], 1);
                // Check if either value is boolean
                boolean topOfStackIsBoolean = jumpIsBoolean(topOfStack);
                boolean secondTopOfStackIsBoolean = jumpIsBoolean(secondTopOfStack);
                boolean onlyOneIsBoolean = (!topOfStackIsBoolean && secondTopOfStackIsBoolean)
                        || (topOfStackIsBoolean && !secondTopOfStackIsBoolean);
                if (onlyOneIsBoolean) {
                    Set<AbstractInsnNode> toRemove;
                    if (topOfStackIsBoolean) {
                        toRemove = topOfStack.instrsWhereProduced;
                    } else {
                        toRemove = secondTopOfStack.instrsWhereProduced;
                    }
                    toRemove.forEach(insn -> {
                        taintedBoolInsns.remove(insn);
                        toWrapSinceUsedByBoolInsns.remove(insn);
                    });
                    continue;
                }
                if (topOfStackIsBoolean) {
                    decideOnTaintedOrWrappedForSpecialTypeInsn(
                            taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                            topOfStack.instrsWhereProduced
                    );
                }
                if (secondTopOfStackIsBoolean) {
                    decideOnTaintedOrWrappedForSpecialTypeInsn(
                            taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                            secondTopOfStack.instrsWhereProduced
                    );
                }
            }
        }
    }

    private boolean jumpIsBoolean(TaintValue usedByJump) {
        if (usedByJump.instrsWhereUsed.size() == 1
                && usedByJump.instrsWhereProduced.stream()
                .allMatch(insn -> insn.getOpcode() == ICONST_0 || insn.getOpcode() == ICONST_1)) {
            return true;
        } else if (usedByJump.instrsWhereProduced.stream()
                .anyMatch(insn -> taintedBoolInsns.contains(insn) || toWrapSinceUsedByBoolInsns.contains(insn))) {
            assert usedByJump.instrsWhereProduced.stream()
                    .anyMatch(insn -> taintedBoolInsns.contains(insn) || toWrapSinceUsedByBoolInsns.contains(insn));
            return true;
        }
        return false;
    }

    private void decideOnTaintedOrWrappedForSpecialTypeInsn(
            Set<AbstractInsnNode> taintedSpecialTypeInsns,
            Set<AbstractInsnNode> toWrapSinceUsedBySpecialTypeInsns,
            Set<AbstractInsnNode> specialTypeInsnsToAdd) {

        specialTypeInsnsToAdd.stream()
                .filter(insn -> !taintedSpecialTypeInsns.contains(insn)
                        && !toWrapSinceUsedBySpecialTypeInsns.contains(insn))
                .forEach(insn -> {
                    if (instructionsToWrap.contains(insn)) {
                        toWrapSinceUsedBySpecialTypeInsns.add(insn);
                    } else if (taintedInstructions.contains(insn)) {
                        taintedSpecialTypeInsns.add(insn);
                    }
                });
    }

    private static byte getTypeForDesc(String desc) {
        desc = desc.replace("[", ""); //// TODO
        // Check if the variable is boolean, byte, or short
        if ((!desc.equals("Z") && !desc.equals("B") && !desc.equals("S"))) {
            return -1; // Not of type boolean, byte, or short --> not relevant here.
        }
        return (byte) (
                desc.equals("Z") ? 0
                        : desc.equals("B") ? 1 : 2);
    }

    public boolean returnsBoolean() {
        return returnsBoolean;
    }

    public boolean returnsByte() {
        return returnsByte;
    }

    public boolean returnsShort() {
        return returnsShort;
    }

    public Set<AbstractInsnNode> getTaintedBoolInsns() {
        return taintedBoolInsns;
    }

    public Set<AbstractInsnNode> getToWrapSinceUsedByBoolInsns() {
        return toWrapSinceUsedByBoolInsns;
    }

    public Set<AbstractInsnNode> getTaintedByteInsns() {
        return taintedByteInsns;
    }

    public Set<AbstractInsnNode> getToWrapSinceUsedByByteInsns() {
        return toWrapSinceUsedByByteInsns;
    }

    public Set<AbstractInsnNode> getTaintedShortInsns() {
        return taintedShortInsns;
    }

    public Set<AbstractInsnNode> getToWrapSinceUsedByShortInsns() {
        return toWrapSinceUsedByShortInsns;
    }
}
