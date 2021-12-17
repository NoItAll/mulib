package de.wwu.mulib.transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TransformationUtility.getSingleDescsFromMethodParams;
import static de.wwu.mulib.transformations.TransformationUtility.splitMethodDesc;
import static org.objectweb.asm.Opcodes.*;

public final class TaintAnalyzer {
    private final MulibTransformer mulibTransformer;
    private final MethodNode mn;
    private final int numberInputs;
    private final int numberOfInputsSized2;
    private final List<LocalVariableNode> localVariables;
    private final Frame<TaintValue>[] frames;

    private final Set<LocalVariableNode> taintedLocalVariables = new HashSet<>();
    private final Set<AbstractInsnNode> taintedInstructions = new HashSet<>();
    private final Set<AbstractInsnNode> instructionsToWrap = new HashSet<>();
    private final Set<MethodInsnNode> methodsToPreserve = new HashSet<>();
    private final AbstractInsnNode[] ains;

    private final Set<AbstractInsnNode> taintedBoolInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByBoolInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedByteInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByByteInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedShortInsns = new HashSet<>();
    private final Set<AbstractInsnNode> toWrapSinceUsedByShortInsns = new HashSet<>();

    private boolean returnsBoolean;
    private boolean returnsByte;
    private boolean returnsShort;

    private final Map<AbstractInsnNode, String> concretizeForMethodCall = new HashMap<>();
    private final Set<MethodInsnNode> tryToGeneralize = new HashSet<>();

    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereProduced = new HashMap<>();
    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereUsed = new HashMap<>();
    @SuppressWarnings("unchecked")
    public TaintAnalyzer(
            MulibTransformer mulibTransformer,
            MethodNode mn,
            String owner) {
        this.ains = mn.instructions.toArray();
        this.mulibTransformer = mulibTransformer;
        this.mn = mn;
        this.numberInputs = TransformationUtility.getNumInputs(mn);
        this.localVariables = new ArrayList<>(mn.localVariables != null ? mn.localVariables : new ArrayList<>());
        try {
            Analyzer<TaintValue> a = new Analyzer<>(new TaintInterpreter());
            a.analyzeAndComputeMaxs(owner, mn);
            Frame<TaintValue>[] allFrames = a.getFrames(); // get frames
            for (Frame<TaintValue> f : allFrames) {
                if (f == null) continue;
                // Sanity check
                for (int i = 0; i < f.getStackSize(); i++) {
                    assert (f.getStack(i).instrsWhereUsed.stream().filter(insn -> insn.getOpcode() != DUP).count() <= 1) : "This can occur using JMPs, but it is not yet dealt with";
                }
            }
            if (allFrames.length == 0) {
                this.frames = new Frame[0];
                this.numberOfInputsSized2 = 0;
                return;
            }
            if (allFrames[allFrames.length - 1] == null) {
                this.frames = new Frame[allFrames.length - 1];
                System.arraycopy(allFrames, 0, frames, 0, frames.length);
            } else {
                this.frames = allFrames;
            }
            // Determine the number of local variables of size 2,
            // set the index of TaintValues as well as the frame number.
            int numberOfInputsSized2 = 0;
            for (Frame<TaintValue> f : frames) {
                for (int j = 0; j < f.getLocals(); j++) {
                    TaintValue localVal = f.getLocal(j);
                    localVal.index = j;
                    if (localVal.size == 2) {
                        if (numberInputs + numberOfInputsSized2 - j > 0) {
                            numberOfInputsSized2++;
                        }
                        localVal = f.getLocal(j + 1);
                        localVal.index = j;
                        j++;
                    }
                }
                for (int j = 0; j < f.getStackSize(); j++) {
                    TaintValue stackVal = f.getStack(j);
                    stackVal.index = j;
                    stackVal.isStackVariable = true;
                    if (stackVal.size == 2 && f.getStackSize() > j + 1) {
                        stackVal = f.getStack(j + 1);
                        if (stackVal != null) {
                            stackVal.index = j;
                            stackVal.isStackVariable = true;
                            j++;
                        }
                    }
                }
            }
            this.numberOfInputsSized2 = numberOfInputsSized2;
        } catch (AnalyzerException e) {
            throw new MulibRuntimeException("Analyzer failed!" , e);
        }
    }

    private void gatherInitialTaintedAndWrappedInstructions() {
        // 1. Taint local variables if they are in the parameter list (and not ignored) or if they come
        // from a type that is to be replaced
        gatherInitialTaintedLocalVariables();
        // 2. Taint instructions if they receive their values from a special type of instruction
        gatherInitialTaintedAndWrappedAndPreservedMethodCallInstructions();
    }

    private void gatherInitialTaintedAndWrappedAndPreservedMethodCallInstructions() {
        /* DETERMINE INITIAL TAINTED INSTRUCTIONS */
        for (AbstractInsnNode ain : ains) {
            if (ain.getOpcode() == -1) {
                continue;
            }
            int ainIndex = mn.instructions.indexOf(ain);
            Frame<TaintValue> frameOfInsn = frames[ainIndex];
            // Currently, we will assume that all XFIELD instructions are tainted and must be replaced.
            if (ain.getOpcode() >= GETSTATIC && ain.getOpcode() <= PUTFIELD) { // All field insns
                if (!mulibTransformer.shouldBeTransformed(((FieldInsnNode) ain).owner)) {
                    continue;
                }
                taintedInstructions.add(ain);
                if (ain.getOpcode() == PUTFIELD || ain.getOpcode() == PUTSTATIC) {
                    // PUTFIELD is defined to manipulate the operand stack as ..., objectref, value -> ...
                    // We thus check the top of the stack
                    TaintValue value = getFromTopOfStack(frameOfInsn, 0);
                    instructionsToWrap.addAll(value.instrsWhereProduced);
                }
            } else if (ain.getOpcode() == INVOKESTATIC && ((MethodInsnNode) ain).owner.equals(mulibCp)) {
                // Regard special Mulib methods for introducing symbolic/free variables
                MethodInsnNode m = (MethodInsnNode) ain;
                switch (m.name) {
                    case freeInt:
                    case freeDouble:
                    case freeFloat:
                    case freeBoolean:
                    case namedFreeInt:
                    case namedFreeDouble:
                    case namedFreeFloat:
                    case namedFreeBoolean:
                    case freeLong:
                    case freeShort:
                    case freeByte:
                    case namedFreeLong:
                    case namedFreeShort:
                    case namedFreeByte:
                        taintedInstructions.add(ain);
                        break;
                    case freeChar:
                    case namedFreeChar:
                        throw new NotYetImplementedException();
                    default:
                }
            } else if (ain.getOpcode() == INVOKESTATIC
                    || ain.getOpcode() == INVOKEVIRTUAL
                    || ain.getOpcode() == INVOKEINTERFACE
                    || ain.getOpcode() == INVOKESPECIAL) {
                // Taint those instructions that receive their value(s) from a method call from a class that is
                // to be replaced.
                MethodInsnNode min = (MethodInsnNode) ain;
                if (!mulibTransformer.shouldBeTransformed(min.owner)) {
                    methodsToPreserve.add(min);
                    continue;
                } else {
                    taintedInstructions.add(min);
                }
                // Add all parameters to the set of wrapped instructions, potentially, they might become tainted later on
                Frame<TaintValue> frameOfMethodInsn = frames[ainIndex];
                String[] methodDescSplit = splitMethodDesc(min.desc);
                String[] parameterPart = getSingleDescsFromMethodParams(methodDescSplit[0]);
                int numberDoubleSlot = (int) Arrays.stream(parameterPart).filter(s -> s.equals("D") || s.equals("J")).count();
                for (int i = 0, currentParamNumber = 0; i < parameterPart.length + numberDoubleSlot; i++) {
                    if (parameterPart[currentParamNumber].equals("D") || parameterPart[currentParamNumber].equals("J")) {
                        i++;
                    }
                    TaintValue fromTopOfStack = getFromTopOfStack(frameOfMethodInsn, i);
                    instructionsToWrap.addAll(fromTopOfStack.instrsWhereProduced);
                    currentParamNumber++;
                }

                // Add all instructions using the non-void return value to the set of tainted instructions
                if (methodDescSplit[1].equals("V")) {
                    continue;
                }
                int indexOfMethodInsnReturnValue = mn.instructions.indexOf(ain.getNext());
                Frame<TaintValue> frameOfMethodInsnReturnValue = frames[indexOfMethodInsnReturnValue];
                TaintValue methodInsnReturnValue = getFromTopOfStack(frameOfMethodInsnReturnValue, 0);
                taintedInstructions.addAll(filterToAddToTaintedInstructions(methodInsnReturnValue.instrsWhereUsed));
            } else if (ain.getOpcode() == NEW || ain.getOpcode() == ANEWARRAY) {
                TypeInsnNode n = (TypeInsnNode) ain;
                if (mulibTransformer.shouldBeTransformed(n.desc)) {
                    taintedInstructions.add(n);
                }
            } else if (ain.getOpcode() >= IRETURN && ain.getOpcode() <= ARETURN) {
                taintedInstructions.add(ain);
            }

            // Also taint the local variables if they are part of the input
            for (int i = 0; i < numberInputs + numberOfInputsSized2; i++) {
                addToTaintedInstructions(frameOfInsn, taintedInstructions, instructionsToWrap, i);
            }
        }
    }

    private void gatherInitialTaintedLocalVariables() {
        for (LocalVariableNode lvn : localVariables) {
            // Directly add all those local variables to the set of tainted local variables that
            // are local input variables.
            if (lvn.index >= numberInputs + numberOfInputsSized2) {
                continue;
            }
            if (!primitiveTypes.contains(lvn.desc) && !mulibTransformer.shouldBeTransformedFromDesc(lvn.desc)) {
                continue;
            }
            taintedLocalVariables.add(lvn);
        }

        // We taint all local object variables that are of types that should be transformed
        for (LocalVariableNode lvn : localVariables) {
            if (lvn.desc.length() <= 1 || (lvn.desc.charAt(0) == '[' && lvn.desc.charAt(1) != 'L')) {
                continue;
            }
            if (mulibTransformer.shouldBeTransformedFromDesc(lvn.desc)) {
                taintedLocalVariables.add(lvn);
            }
        }
    }

    private static TaintValue getFromTopOfStack(Frame<TaintValue> frame, int offset) {
        return frame.getStack(frame.getStackSize() - (offset + 1));
    }

    private void prepareLocalVariablesToUsingInstructionsMapping() {
        // Mapping from LocalVariableNode to instructions where produced/used. The LocalVariableNode is not necessarily
        // tainted. These mappings are used to check whether one of the produced/used instructions was tainted.
        // In this case, we add all other produced/used instructions to the set of tainted instructions.
        for (LocalVariableNode lvn : localVariables) {
            if (lvn.index < numberInputs + numberOfInputsSized2) {
                continue;
            }
            localVarToInstrsWhereProduced.put(lvn, new HashSet<>());
            localVarToInstrsWhereUsed.put(lvn, new HashSet<>());
            Set<AbstractInsnNode> currentProduced = localVarToInstrsWhereProduced.get(lvn);
            Set<AbstractInsnNode> currentUsed = localVarToInstrsWhereUsed.get(lvn);
            int lvnStartIndex = mn.instructions.indexOf(lvn.start);
            int lvnEndIndex = mn.instructions.indexOf(lvn.end);
            int indexOfInstruction = -1;
            for (Frame<TaintValue> f : frames) {
                indexOfInstruction++;
                if (lvnStartIndex> indexOfInstruction
                        || lvnEndIndex < indexOfInstruction) {
                    continue;
                }
                TaintValue current = f.getLocal(lvn.index);
                currentProduced.addAll(current.instrsWhereProduced);
                currentUsed.addAll(current.instrsWhereUsed);
            }
        }
    }

    private void calculateTaintFixedPoint() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Frame<TaintValue> f : frames) {
                // Taint stack values
                for (int i = 0; i < f.getStackSize(); i++) {
                    TaintValue val = f.getStack(i);
                    if (val.instrsWhereProduced.stream().anyMatch(insn ->
                            taintedInstructions.contains(insn) || instructionsToWrap.contains(insn))) {
                        if (val.instrsWhereUsed.stream().anyMatch(insn -> !(insn instanceof MethodInsnNode)
                                && (!taintedInstructions.contains(insn) && !instructionsToWrap.contains(insn)))) {
                            changed = true;
                        }
                        // If any of the instructions where the current stack value was produced is tainted or wrapped,
                        // we must continue the taint.
                        // This value now is tainted.
                        // Those instructions that yield the given value, if not already tainted, must be wrapped.
                        instructionsToWrap.addAll(val.instrsWhereProduced);
                        // A tainted value must only be used by another tainted instruction.
                        taintedInstructions.addAll(filterToAddToTaintedInstructions(val.instrsWhereUsed));
                        val.instrsWhereUsed.stream()
                                .filter(insn -> insn instanceof VarInsnNode
                                        // Only add to tainted local variables, if it is not a benign LOAD instruction
                                        && !(insn.getOpcode() >= ILOAD && insn.getOpcode() <= ALOAD))
                                .forEach(insn -> {
                                    VarInsnNode vin = (VarInsnNode) insn;
                                    LocalVariableNode lvn = getLocalVariableInScopeOfInsn(vin, mn.instructions, localVariables);
                                    if (lvn != null) {
                                        taintedLocalVariables.add(lvn);
                                    }
                                });
                    }

                    // If the value is used by a tainted instruction...
                    if (val.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains)) {
                        if (!instructionsToWrap.containsAll(val.instrsWhereProduced)) {
                            changed = true;
                        }
                        // ...and the instructions producing this value are not already tainted, they must be wrapped.
                        instructionsToWrap.addAll(val.instrsWhereProduced);
                    }
                }
            }
            // Now taint the local variables and related instructions
            Set<LocalVariableNode> remove = new HashSet<>();
            for (Map.Entry<LocalVariableNode, Set<AbstractInsnNode>> entry : localVarToInstrsWhereProduced.entrySet()) {
                Set<AbstractInsnNode> currentProduced = entry.getValue();
                if (currentProduced.stream().anyMatch(insn ->
                        (taintedInstructions.contains(insn) || instructionsToWrap.contains(insn)))) {
                    Set<AbstractInsnNode> currentUsed = localVarToInstrsWhereUsed.get(entry.getKey());
                    assert currentUsed != null;
                    instructionsToWrap.addAll(currentProduced);
                    taintedInstructions.addAll(filterToAddToTaintedInstructions(currentUsed));
                    taintedLocalVariables.add(entry.getKey());
                    remove.add(entry.getKey());
                }
            }
            // If a local variable has been tainted, it does not need to be tainted again, therefore we can remove
            // the respective variable from the mapping
            for (LocalVariableNode r : remove) {
                localVarToInstrsWhereUsed.remove(r);
                localVarToInstrsWhereProduced.remove(r);
            }
        }

        for (int i = 0; i < ains.length; i++) {
            AbstractInsnNode ain = ains[i];
            // If 2 are popped because one of them is a long/double, and the long/double is tainted, we must taint POP2
            // so that it is replaced by POP. This is because after substituting, only one slot is taken opposed to
            // two slots (which is the case for longs and doubles)
            if (ain.getOpcode() == POP2) {
                Frame<TaintValue> f = frames[i];
                TaintValue topOfStack = getFromTopOfStack(f, 0);
                if (topOfStack.size == 2 && topOfStack.instrsWhereProduced.stream().anyMatch(taintedInstructions::contains)) {
                    taintedInstructions.add(ain);
                }
            }
        }
    }

    public TaintAnalysis analyze() {
        /*
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES FOR PRIMITIVES
        OR A PARTNER CLASS MUST BE USED.
        */
        gatherInitialTaintedAndWrappedInstructions();
        prepareLocalVariablesToUsingInstructionsMapping();
        calculateTaintFixedPoint();

        // From the tainted instructions, determine those instructions that use boolean, byte, or short values. This is necessary
        // to create Sbools, Sbytes, and Sshorts since Java bytecode does not differentiate between booleans, bytes, shorts, and ints.
        // If the return type is boolean, we add this instruction to the tainted boolean instructions as well.
        determineTaintedBoolShortByteInsns();

        // There are four kinds of instructions to regard while treating arrays:
        // 1. {NEWARRAY, ANEWARRAY, MULTIANEWARRAY}
        // 2. {IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD}
        // 3. {IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE}
        // 4. ILOAD
        // 5. ARRAYLENGTH
        // TODO For now: avoid tainting array creations, loads and stores! for this, we remove the ILOAD and the ARRAYLENGTH from tainted
        Set<AbstractInsnNode> removeFromWrapping = new HashSet<>();
        for (AbstractInsnNode ain : instructionsToWrap) {
            if (ain.getOpcode() == ILOAD || ain.getOpcode() == ICONST_M1 || ain.getOpcode() == ICONST_0
                    || ain.getOpcode() == ICONST_1 || ain.getOpcode() == ICONST_2 || ain.getOpcode() == ICONST_3
                    || ain.getOpcode() == ICONST_4 || ain.getOpcode() == ICONST_5 || ain.getOpcode() == BIPUSH
                    || ain.getOpcode() == SIPUSH
                    || (ain.getOpcode() == GETFIELD && ((FieldInsnNode) ain).desc.equals("I")) // TODO Hardcoded for proof-of-concept
            ) {
                Frame<TaintValue> frame = frames[mn.instructions.indexOf(ain.getNext())];
                TaintValue upperStack = getFromTopOfStack(frame, 0);
                boolean iloadUsedInArrayLoadOrStore =
                        upperStack.instrsWhereUsed
                                .stream()
                                .anyMatch(insn -> (insn.getOpcode() >= IALOAD && insn.getOpcode() <= SALOAD)
                                        || (insn.getOpcode() >= IASTORE && insn.getOpcode() <= SASTORE)
                                        || insn.getOpcode() == ANEWARRAY
                                        || insn.getOpcode() == NEWARRAY
                                        || insn.getOpcode() == MULTIANEWARRAY);
                if (iloadUsedInArrayLoadOrStore) {
                    Set<AbstractInsnNode> used = upperStack.instrsWhereUsed;
                    for (AbstractInsnNode usedByAin : used) {
                        if (usedByAin.getOpcode() == IASTORE) { // TODO Hardcoded for proof-of-concept
                            // Only remove if it is the index which is to be wrapped!
                            // This is the frame of the value produced by ILOAD
                            Frame<TaintValue> iastoreFrame = frames[mn.instructions.indexOf(usedByAin)];
                            // Only remove the index-load from those instructions which are to be wrapped.
                            // The index load is on the second-most position on the stack.
                            TaintValue iastoreUpperStack = getFromTopOfStack(iastoreFrame, 1);
                            assert iastoreUpperStack.producedBy.size() == 1 || iastoreUpperStack.producedBy.size() == 0;
                            removeFromWrapping.addAll(iastoreUpperStack.instrsWhereProduced);
                        } else {
                            removeFromWrapping.addAll(upperStack.instrsWhereProduced);
                            break;
                        }
                    }

                }
            }
        }
        instructionsToWrap.removeAll(removeFromWrapping);

        /* Add those instructions that must be concretized, since they are used in a non-transformed method, yet, are tainted. */
        determineSpecialMethodTreatment();

        // Determine new indices of explicit local variables
        Map<LocalVariableNode, Integer> newLvnIndices =
                newLocalVariableIndices(mn.instructions, taintedLocalVariables, localVariables);
        // Gather VarInsnNodes and IincInsnNodes
        // Determine new indices of indexInsns (including those using anonymous local variables)
        Map<AbstractInsnNode, Integer> newIndexInsnIndices =
                newLocalVariableIndexInsns(mn.instructions, taintedLocalVariables, newLvnIndices);

        // Get the maximal variable index, this is later used to determine the index of SymbolicExecution.
        int maxVarIndexInsn = newIndexInsnIndices.values().stream()
                .max(Integer::compareTo).orElse(-1);

        return new TaintAnalysis(
                taintedLocalVariables, taintedInstructions,
                instructionsToWrap, frames,
                taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                taintedByteInsns, toWrapSinceUsedByByteInsns,
                taintedShortInsns, toWrapSinceUsedByShortInsns,
                returnsBoolean, returnsByte, returnsShort,
                concretizeForMethodCall,
                tryToGeneralize,
                newLvnIndices,
                newIndexInsnIndices,
                maxVarIndexInsn
        );
    }

    private void determineSpecialMethodTreatment() {
        for (MethodInsnNode min : methodsToPreserve) {
            assert !taintedInstructions.contains(min) : "Should not occur";
            if (!splitMethodDesc(min.desc)[1].equals("V")) {
                // Decide on whether to wrap output
                Frame<TaintValue> frameWithStackValueOfMethodOutput = frames[mn.instructions.indexOf(min.getNext())];
                TaintValue tv = getFromTopOfStack(frameWithStackValueOfMethodOutput, 0);
                boolean anyTainted = tv.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains);
                if (anyTainted) {
                    // Sanity check
                    assert taintedInstructions.containsAll(tv.instrsWhereUsed);
                    instructionsToWrap.add(min);
                }
            }

            // Treat arguments of method-call
            boolean concretize = mulibTransformer.shouldBeConcretizedFor(min.owner);
            boolean generalize = mulibTransformer.shouldTryToUseGeneralizedMethodCall(min.owner);
            if (concretize && generalize) {
                throw new MisconfigurationException("It should be unambiguously specified whether " +
                        "the method " + min.name + " should be generalized or concretized.");
            }
            if (concretize || generalize) {
                int numParams = TransformationUtility.getNumInputs(
                        min.desc,
                        /* We never count the object itself here, since it is not replaced. Only
                         * objects that are not replaced can have ignored methods.*/
                        true
                );
                if (numParams > 0) {
                    int mindex = mn.instructions.indexOf(min);
                    Frame<TaintValue> frameOfMethodCall = frames[mindex];
                    String[] paramsAndReturn = splitMethodDesc(min.desc);
                    String[] paramDescs = getSingleDescsFromMethodParams(paramsAndReturn[0]);
                    int currentParam = 0;
                    methodSearch:
                    for (int i = frameOfMethodCall.getStackSize() - numParams; i < frameOfMethodCall.getStackSize(); i++) {
                        TaintValue tv = frameOfMethodCall.getStack(i);
                        assert tv.instrsWhereProduced.size() <= 1 : "Not yet regarded";
                        for (AbstractInsnNode prod : tv.instrsWhereProduced) {
                            if (taintedInstructions.contains(prod)) {
                                if (concretize) {
                                    String desc = paramDescs[currentParam];
                                    concretizeForMethodCall.put(prod, desc);
                                }
                                if (generalize) {
                                    // Only add to methods which must be generalized, if, in fact, the value is tainted and thus the type has changed.
                                    if (taintedInstructions.contains(prod)) {
                                        tryToGeneralize.add(min);
                                        break methodSearch;
                                    }
                                }
                            }
                        }
                        currentParam++;
                    }
                }
            } else {
                String[] descSplit = splitMethodDesc(min.desc);
                String[] params = getSingleDescsFromMethodParams(descSplit[0]);
                if (Arrays.stream(params).anyMatch(p -> !p.equals(objectDesc)) || (!descSplit[1].equals(objectDesc) && !descSplit[1].equals("V"))) {
                    Mulib.log.log(Level.WARNING, "The method " + min.name + " is to be kept, " +
                            "yet is neither concretized nor generalized.");
                }
            }
        }
    }

    // We do not add any more method nodes during the taint analysis since all those MethodInsnNodes that should be transformed
    // are already added at the initialization. This way we avoid accidentally tainting methods that are to be preserved.
    private static Collection<AbstractInsnNode> filterToAddToTaintedInstructions(Set<AbstractInsnNode> ains) {
        return ains.stream()
                .filter(insn ->
                        (!(insn instanceof MethodInsnNode)))
                .collect(Collectors.toList());
    }

    private static byte getTypeForDesc(String desc) {
        // Check if the variable is boolean, byte, or short
        if ((!desc.equals("Z") && !desc.equals("B") && !desc.equals("S"))) {
            return -1; // Not of type boolean, byte, or short --> not relevant here.
        }
        return (byte) (
                desc.equals("Z") ? 0
                        : desc.equals("B") ? 1 : 2);
    }


    // Determine the subset of tainted instructions and instructions to wrap which are boolean instructions.
    private void determineTaintedBoolShortByteInsns() {
        InsnList oldInstructionList = mn.instructions;
        List<LocalVariableNode> oldLocalVariables = mn.localVariables;
        returnsBoolean = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("Z");
        returnsByte = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("B");
        returnsShort = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("S");
        // Loop to determine those instructions that are to be tainted:
        for (AbstractInsnNode ain : taintedInstructions) {
            if (ain.getOpcode() == INSTANCEOF) {
                int indexOfStackValue = oldInstructionList.indexOf(ain.getNext());
                Frame<TaintValue> frameOfInstanceofResult = frames[indexOfStackValue];
                TaintValue instanceofResult = getFromTopOfStack(frameOfInstanceofResult, 0);
                assert taintedInstructions.containsAll(instanceofResult.instrsWhereUsed);
                taintedBoolInsns.addAll(instanceofResult.instrsWhereUsed);
                continue;
            }
            // We start by looking for boolean, byte, and short local variable nodes
            if (ain.getOpcode() == ILOAD
                    || ain.getOpcode() == ISTORE
                    || ain instanceof  FieldInsnNode) {
                // Find the type of the local or field variable. If the local or field variable is not of types
                // boolean, byte, or short, continue with the next AbstractInsnNode
                byte type; // 0 = boolean, 1 = byte, 2 = short
                if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                    // Get local variables in scope with the index
                    LocalVariableNode lvn =
                            getLocalVariableInScopeOfInsn((VarInsnNode) ain, oldInstructionList, oldLocalVariables);
                    // Check if the variable is boolean, byte, or short
                    if (lvn == null) {
                        continue;
                    }
                    type = getTypeForDesc(lvn.desc);
                    if (type == -1) {
                        continue;
                    }
                } else if (ain instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    type = getTypeForDesc(fin.desc);

                    if (type == -1) {
                        continue;
                    }
                } else {
                    throw new MulibRuntimeException("Should not occur.");
                }
                // Get those values that were computes using the now-loaded value. Then, get those values that
                // use the respective computed values (if the computations are boolean comparisons).
                int indexOfLoaded = oldInstructionList.indexOf(
                        ain.getOpcode() == ILOAD || ain.getOpcode() == GETFIELD || ain.getOpcode() == GETSTATIC ?
                                ain.getNext()
                                :
                                ain
                );
                Frame<TaintValue> frameOfInstruction = frames[indexOfLoaded];
                // Now we mark those instructions as boolean, short, or byte instructions that use the instruction to
                // produce new booleans, shorts, or bytes.
                // Using these outputs are the end points of this inner analysis.
                TaintValue stackValueOfLoadingSpecialTypeVar = getFromTopOfStack(frameOfInstruction, 0);

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
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        taintedSpecialInsns,
                        toWrapSpecialInsns,
                        toAdd
                );
            } else if (ain.getOpcode() == IRETURN && (returnsBoolean || returnsByte || returnsShort)) {
                int indexOfReturn = oldInstructionList.indexOf(ain);
                Frame<TaintValue> frameOfInstruction = frames[indexOfReturn];
                TaintValue stackValueReturned = getFromTopOfStack(frameOfInstruction, 0);
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
                int indexOfValue = oldInstructionList.indexOf(min.getNext());
                Frame<TaintValue> frameOfPushedValue = frames[indexOfValue];
                TaintValue topOfStackOfValue = getFromTopOfStack(frameOfPushedValue, 0);
                if (returnPart.equals("Z")) {
                    taintedBoolInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                } else if (returnPart.equals("B")) {
                    taintedByteInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                } else {
                    assert returnPart.equals("S");
                    taintedShortInsns.addAll(topOfStackOfValue.instrsWhereUsed);
                }
            } else if (ain.getOpcode() == ICONST_0 || ain.getOpcode() == ICONST_1) {
                // These instructions should not be tainted as they do not have any input value
                throw new NotYetImplementedException();
            }
        }
        // Loop to determine those instructions that are to be wrapped
        for (AbstractInsnNode ain : instructionsToWrap) {
            byte type;
            if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                LocalVariableNode lvn =
                        getLocalVariableInScopeOfInsn((VarInsnNode) ain, oldInstructionList, oldLocalVariables);
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
                int index = oldInstructionList.indexOf(ain.getNext());
                TaintValue tvOfIconst = getFromTopOfStack(frames[index], 0);

                List<AbstractInsnNode> relevantInsnNodes =
                        tvOfIconst.instrsWhereUsed.stream()
                                .filter(insn ->
                                        insn instanceof MethodInsnNode)
                                .toList();

                if (relevantInsnNodes.isEmpty()) {
                    continue;
                }

                // Check whether, and if, where used as input for MethodInsnNodes:
                Optional<AbstractInsnNode> optionalMethodInsnNode =
                        relevantInsnNodes.stream()
                                .filter(insn -> insn instanceof MethodInsnNode)
                                .findFirst();
                if (optionalMethodInsnNode.isPresent()) {
                    MethodInsnNode anyMethodInsn = (MethodInsnNode) optionalMethodInsnNode.get();
                    // Check signature to see if it should be a boolean value
                    String[] paramAndReturn = splitMethodDesc(anyMethodInsn.desc);
                    String[] paramsDescSplit = getSingleDescsFromMethodParams(paramAndReturn[0]);
                    // Get frame of method insn
                    Frame<TaintValue> frameOfMethodInsn = frames[oldInstructionList.indexOf(anyMethodInsn)];
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
                }
            }
        }
        // We determine which JumpInstructions are using purely boolean values
        Set<AbstractInsnNode> jumpInsns = taintedInstructions.stream()
                .filter(insn -> insn instanceof JumpInsnNode)
                .collect(Collectors.toSet());
        assert instructionsToWrap.stream().noneMatch(insn -> insn instanceof JumpInsnNode);
        for (AbstractInsnNode ain : jumpInsns) {
            int index = oldInstructionList.indexOf(ain);
            if (ain.getOpcode() == IFEQ || ain.getOpcode() == IFNE) {
                // The int is on top of the stack
                TaintValue topOfStack = getFromTopOfStack(frames[index], 0);
                if (jumpIsBoolean(topOfStack)) {
                    decideOnTaintedOrWrappedForSpecialTypeInsn(
                            taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                            topOfStack.instrsWhereProduced);
                }
            } else if (ain.getOpcode() == IF_ICMPEQ || ain.getOpcode() == IF_ICMPNE) {
                // Get the two values from the stack used here
                TaintValue topOfStack = getFromTopOfStack(frames[index], 0);
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

    private static LocalVariableNode getLocalVariableInScopeOfInsn(
            VarInsnNode insn,
            InsnList l,
            Collection<LocalVariableNode> lvns) {
        return getLocalVariableInScopeOfInsn(l.indexOf(insn), insn.var, insn.getOpcode(), l, lvns);
    }

    private static LocalVariableNode getLocalVariableInScopeOfInsn(
            IincInsnNode insn,
            InsnList l,
            Collection<LocalVariableNode> lvns) {
        return getLocalVariableInScopeOfInsn(l.indexOf(insn), insn.var, insn.getOpcode(), l, lvns);
    }

    private static LocalVariableNode getLocalVariableInScopeOfInsn(
            int indexOfInsn,
            int varIndex,
            int op,
            InsnList l,
            Collection<LocalVariableNode> lvns) {
        for (LocalVariableNode lvn : lvns) {
            if (varIndex == lvn.index) {
                int indexStartLvn = l.indexOf(lvn.start);
                if ((op >= ISTORE && op <= ASTORE) || op == IINC) {
                    indexOfInsn++; // XSTORE instructions might "enable" the variable.
                }
                if (indexOfInsn < indexStartLvn) {
                    continue;
                }
                int indexEndLvn = l.indexOf(lvn.end);
                if (indexOfInsn <= indexEndLvn) {
                    return lvn;
                }
            }
        }
        // Can occur if variable is not granted a dedicated local variable place (e.g. because it is not used, or is anonymous)
        return null;
    }

    private static int getNumberTaintedLocalVariablesWithTwoSlotsInScopeOfInsn(
            int indexOfInsn,
            int varIndex,
            int op,
            InsnList l,
            Set<LocalVariableNode> taintedLvns) {
        int result = 0;
        for (LocalVariableNode lvn : taintedLvns) {
            if (!(lvn.desc.equals("D") || lvn.desc.equals("J"))) {
                continue;
            }
            if (varIndex > lvn.index) {
                int indexStartLvn = l.indexOf(lvn.start);
                if ((op >= ISTORE && op <= ASTORE) || op == IINC) {
                    indexOfInsn++; // XSTORE instructions might "enable" the variable.
                }
                if (indexOfInsn < indexStartLvn) {
                    continue;
                }
                int indexEndLvn = l.indexOf(lvn.end);
                if (indexOfInsn <= indexEndLvn) {
                    result++;
                }
            }
        }
        return result;
    }

    private void decideOnTaintedOrWrappedForSpecialTypeInsn(
            Set<AbstractInsnNode> taintedSpecialTypeInsns,
            Set<AbstractInsnNode> toWrapSinceUsedBySpecialTypeInsns,
            Set<AbstractInsnNode> specialTypeInsnsToAdd) {

        specialTypeInsnsToAdd.stream()
                .filter(insn -> !taintedSpecialTypeInsns.contains(insn) && !toWrapSinceUsedBySpecialTypeInsns.contains(insn))
                .forEach(insn -> {
                    if (instructionsToWrap.contains(insn)) {
                        toWrapSinceUsedBySpecialTypeInsns.add(insn);
                    } else if (taintedInstructions.contains(insn)) {
                        taintedSpecialTypeInsns.add(insn);
                    }
                });
    }

    private static Map<AbstractInsnNode, Integer> newLocalVariableIndexInsns(
            InsnList insns,
            Set<LocalVariableNode> taintedLocalVariables,
            Map<LocalVariableNode, Integer> newLocalVariableIndices) {
        List<AbstractInsnNode> indexInsns = getIndexInsns(insns);

        // VarInsnNode or IincInsnNode --> new index
        Map<AbstractInsnNode, Integer> result = new HashMap<>();
        for (AbstractInsnNode indexInsn : indexInsns) {
            LocalVariableNode lvn;
            int indexInsnVarIndex;
            if (indexInsn instanceof VarInsnNode) {
                indexInsnVarIndex = ((VarInsnNode) indexInsn).var;
                lvn = getLocalVariableInScopeOfInsn((VarInsnNode) indexInsn, insns, newLocalVariableIndices.keySet());
            } else if (indexInsn instanceof IincInsnNode) {
                indexInsnVarIndex = ((IincInsnNode) indexInsn).var;
                lvn = getLocalVariableInScopeOfInsn((IincInsnNode) indexInsn, insns, newLocalVariableIndices.keySet());
            } else {
                throw new MulibRuntimeException("Only VarInsnNode and IincInsnNode should be used here");
            }
            if (lvn == null) {

                // In this case, we have a index instruction that uses an anonymous local variable
                int number = getNumberTaintedLocalVariablesWithTwoSlotsInScopeOfInsn(
                        insns.indexOf(indexInsn),
                        indexInsnVarIndex,
                        indexInsn.getOpcode(),
                        insns,
                        taintedLocalVariables
                );
                result.put(indexInsn, indexInsnVarIndex - number);
            } else {
                // We have a local variable node within the scope, we can simply use its new index
                Integer newIndex = newLocalVariableIndices.get(lvn);
                assert newIndex != null;
                result.put(indexInsn, newIndex);
            }
        }

        return result;
    }

    private static List<AbstractInsnNode> getIndexInsns(InsnList insns) {
        return Arrays.stream(insns.toArray())
                .filter(insn -> insn instanceof VarInsnNode || insn instanceof IincInsnNode)
                .collect(Collectors.toList());
    }

    private static Map<LocalVariableNode, Integer> newLocalVariableIndices(
            InsnList insns,
            Set<LocalVariableNode> taintedLocalVariables,
            List<LocalVariableNode> localVariables) {
        Map<LocalVariableNode, List<LocalVariableNode>> taintedDoubleSlotsToAffectedLocalVariables = new HashMap<>();
        for (LocalVariableNode taintedLvn : taintedLocalVariables) {
            // We only have to regard index-shifts for those tainted variables that are double-slots, i.e., those
            // that are of type double or long
            if (!(taintedLvn.desc.equals("J") || taintedLvn.desc.equals("D"))) {
                continue;
            }
            List<LocalVariableNode> affectedVariablesInScope = new ArrayList<>();
            int taintedStartIndex = insns.indexOf(taintedLvn.start);
            int taintedEndIndex = insns.indexOf(taintedLvn.end);
            for (LocalVariableNode lvn : localVariables) {
                // If the index of the tainted instruction is larger, we do not need to shift the index of lvn,
                // the same index can only be used for either a variable in another scope (irrelevant in the following)
                // or for the tainted variable itself (also irrelevant to shift).
                if (taintedLvn.index >= lvn.index) {
                    continue;
                }
                // Check for whether the tainted local variable is defined in the same scope and before the other
                // local variable is defined.
                // If the tainted local variable is defined after the local variable, we do not need to shift lvn
                int toCheckStartIndex = insns.indexOf(lvn.start);
                if (toCheckStartIndex < taintedStartIndex) {
                    continue;
                }
                // If the tainted local variable is not in scope for the newly defined index, we do not need to shift lvn
                if (toCheckStartIndex > taintedEndIndex) {
                    continue;
                }
                affectedVariablesInScope.add(lvn);

            }
            taintedDoubleSlotsToAffectedLocalVariables.put(taintedLvn, affectedVariablesInScope);
        }
        // We now calculate new indices for all local variables which must be index-shifted since a double-slot
        // local variable is to be substituted.
        Map<LocalVariableNode, Integer> result = new HashMap<>();
        for (List<LocalVariableNode> affectedLocalVariables : taintedDoubleSlotsToAffectedLocalVariables.values()) {
            for (LocalVariableNode affected : affectedLocalVariables) {
                result.putIfAbsent(affected, affected.index);
                result.put(affected, result.get(affected) - 1);
            }
        }
        // Add the unaffected local variable nodes as well
        for (LocalVariableNode lvn : localVariables) {
            result.putIfAbsent(lvn, lvn.index);
        }

        return result;
    }

    private void addToTaintedInstructions(
            Frame<TaintValue> f,
            Set<AbstractInsnNode> taintedInstructions,
            Set<AbstractInsnNode> instructionsToWrap,
            int toAddForIndex) {
        for (int i = 0; i < f.getLocals(); i++) {
            TaintValue localInputVarTaintValue = f.getLocal(i);
            if (localInputVarTaintValue.index != toAddForIndex) continue;
            instructionsToWrap.addAll(localInputVarTaintValue.instrsWhereProduced);
            taintedInstructions.addAll(filterToAddToTaintedInstructions(localInputVarTaintValue.instrsWhereUsed));
        }
    }
}
