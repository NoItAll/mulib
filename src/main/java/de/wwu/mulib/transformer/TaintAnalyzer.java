package de.wwu.mulib.transformer;

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

import static de.wwu.mulib.transformer.MulibTransformer.*;
import static org.objectweb.asm.Opcodes.*;
import static de.wwu.mulib.transformer.StringConstants.*;

public final class TaintAnalyzer {
    private final MulibTransformer mulibTransformer;
    private final MethodNode mn;
    private final int numberInputs;
    private int numberOfInputsSized2;
    private final List<LocalVariableNode> localVariables;
    private final Frame<TaintValue>[] frames;

    @SuppressWarnings("unchecked")
    public TaintAnalyzer(
            MulibTransformer mulibTransformer,
            MethodNode mn,
            String owner) {
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
            for (int i = 0; i < frames.length; i++) {
                Frame<TaintValue> f = frames[i];
                numberOfInputsSized2 = 0;
                for (int j = 0; j < f.getLocals(); j++) {
                    TaintValue localVal = f.getLocal(j);
                    localVal.frameNumber = i;
                    localVal.index = j;
                    if (localVal.size == 2) {
                        if (numberInputs + numberOfInputsSized2 - j > 0) {
                            numberOfInputsSized2++;
                        }
                        localVal = f.getLocal(j+1);
                        localVal.frameNumber = i;
                        localVal.index = j;
                        j++;
                    }

                }
                for (int j = 0; j < f.getStackSize(); j++) {
                    TaintValue stackVal = f.getStack(j);
                    stackVal.frameNumber = i;
                    stackVal.index = j;
                    stackVal.isStackVariable = true;
                    if (stackVal.size == 2 && f.getStackSize() > j+1) {
                        stackVal = f.getStack(j+1);
                        if (stackVal != null) {
                            stackVal.frameNumber = i;
                            stackVal.index = j;
                            stackVal.isStackVariable = true;
                            j++;
                        }
                    }
                }
            }
        } catch (AnalyzerException e) {
            throw new MulibRuntimeException("Analyzer failed!" , e);
        }
    }

    public TaintAnalysis analyze() {
        /* ----------------------------------------------------------------------
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES AND TAINTED INSTRUCTIONS.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES MUST BE USED.
        */
        Set<LocalVariableNode> taintedLocalVariables = new HashSet<>();
        for (int i = 0; i < numberInputs + numberOfInputsSized2; i++) {
            addToTaintedLocalVariables(localVariables, taintedLocalVariables, i);
        }

        if (mn.localVariables != null) {
            // We taint all local object variables that are of types that should be transformed
            for (LocalVariableNode lvn : mn.localVariables) {
                if (lvn.desc.length() <= 1 || (lvn.desc.charAt(0) == '[' && lvn.desc.charAt(1) != 'L')) {
                    continue;
                }
                int start = lvn.desc.charAt(0) == '[' ? 2 : 1;
                if (mulibTransformer.shouldBeTransformed(lvn.desc.substring(start, lvn.desc.length() - 1))) {
                    taintedLocalVariables.add(lvn);
                }
            }
        }
        // Treat all method-input variables as taintedValues.
        // The identity of local variables might change.
        // For instance for "result += i2;" result.add(i2, se); a different value would now be stored.
        // Therefore, we must regard the input variables of each frame.
        Set<AbstractInsnNode> taintedInstructions = new HashSet<>();

        Set<AbstractInsnNode> instructionsToWrap = new HashSet<>();

        Set<MethodInsnNode> methodsToPreserve = new HashSet<>();
        // All GETFIELDs of a transformed class are tainted.
        for (AbstractInsnNode ain : mn.instructions) {
            // Additionally, currently, we will assume that all XFIELD instructions are tainted and must be replaced.
            if (ain.getOpcode() >= GETSTATIC && ain.getOpcode() <= PUTFIELD) {
                taintedInstructions.add(ain);
            } else if (ain.getOpcode() == INVOKESTATIC && ((MethodInsnNode) ain).owner.equals(mulibCp)) {
                // Regard special Mulib methods for introducing symbolic/free variables
                MethodInsnNode m = (MethodInsnNode) ain;
                switch (m.name) {
                    case freeInt:
                    case freeDouble:
                    case freeFloat:
                    case freeBoolean:
                    case trackedFreeInt:
                    case trackedFreeDouble:
                    case trackedFreeFloat:
                    case trackedFreeBoolean:
                    case freeLong:
                    case freeShort:
                    case freeByte:
                    case trackedFreeLong:
                    case trackedFreeShort:
                    case trackedFreeByte:
                        taintedInstructions.add(ain);
                        break;
                    case freeChar:
                    case trackedFreeChar:
                        throw new NotYetImplementedException();
                    default:
                }
            } else if (ain.getOpcode() == INVOKESTATIC
                    || ain.getOpcode() == INVOKEVIRTUAL
                    || ain.getOpcode() == INVOKEINTERFACE) {
                // Taint those instructions that receive their value(s) from a method call from a class that is
                // to be replaced.
                MethodInsnNode min = (MethodInsnNode) ain;
                /// TODO Check supertypes: If the method is already defined there with the same signature, it should not be overriden; still, a warning should be raised in the logger
                if (!mulibTransformer.shouldBeTransformed(min.owner)) {
                    methodsToPreserve.add(min);
                } else {
                    taintedInstructions.add(min);
                }
            } else if (ain.getOpcode() == NEW || ain.getOpcode() == ANEWARRAY) {
                TypeInsnNode n = (TypeInsnNode) ain;
                if (mulibTransformer.shouldBeTransformed(n.desc)) {
                    taintedInstructions.add(n);
                }
            }/*else if (ain.getOpcode() == NEWARRAY) {
            } else if (ain.getOpcode() == MULTIANEWARRAY) {
                throw new NotYetImplementedException();
            }*/
        }
        for (Frame<TaintValue> f : frames) {
            for (int i = 0; i < numberInputs + numberOfInputsSized2; i++) {
                addToTaintedInstructions(f, taintedInstructions, instructionsToWrap, i);
            }
        }

        // Furthermore, we taint all those instructions, that are non-void return instructions
        AbstractInsnNode[] ains = mn.instructions.toArray();
        for (AbstractInsnNode ain : ains) {
            if (ain.getOpcode() >= IRETURN && ain.getOpcode() <= ARETURN) {
                taintedInstructions.add(ain);
            }
        }

        // TODO For both should be local variable, not integer!
        Map<Integer, Set<AbstractInsnNode>> localVarIndexToInstrsWhereProduced = new HashMap<>();
        Map<Integer, Set<AbstractInsnNode>> localVarIndexToInstrsWhereUsed = new HashMap<>();

        for (int i = 0; i < mn.maxLocals; i++) {
            localVarIndexToInstrsWhereProduced.putIfAbsent(i, new HashSet<>());
            localVarIndexToInstrsWhereUsed.putIfAbsent(i, new HashSet<>());
            Set<AbstractInsnNode> currentProduced = localVarIndexToInstrsWhereProduced.get(i);
            Set<AbstractInsnNode> currentUsed = localVarIndexToInstrsWhereUsed.get(i);
            for (Frame<TaintValue> f : frames) {
                TaintValue current = f.getLocal(i);
                currentProduced.addAll(current.instrsWhereProduced);
                currentUsed.addAll(current.instrsWhereUsed);
            }
        }


        // We must at least wrap the respective instructions for the constructors statements // TODO Refactor with method treatment above?
        for (AbstractInsnNode ain : mn.instructions) {
            if (ain.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode min = (MethodInsnNode) ain;
                if (init.equals(min.name) && mulibTransformer.shouldBeTransformed(min.owner)) {
                    int indexOfNewObject = mn.instructions.indexOf(ain);
                    Frame<TaintValue> frame = frames[indexOfNewObject];
                    for (int i = 0; i < frame.getStackSize(); i++) {
                        TaintValue tv = frame.getStack(i);
                        if (tv.instrsWhereUsed.contains(min)) {
                            instructionsToWrap.addAll(tv.instrsWhereProduced);
                        }
                    }
                }
            }
        }


        boolean changed = true;
        while (changed) {
            changed = false;
            for (Frame<TaintValue> f : frames) {
                // We spread taint to non-input local variables by regarding currently tainted values.

                // Those local variables that are written into by an operation where a tainted variable is used
                // are also tainted.
                for (int i = numberInputs; i < localVariables.size(); i++) {
                    if (i >= f.getLocals()) continue;
                    TaintValue localInputVarTaintValue = f.getLocal(i);
                    if (localInputVarTaintValue.instrsWhereProduced.stream().anyMatch(insn -> taintedInstructions.contains(insn) || instructionsToWrap.contains(insn))) {
                        addToTaintedLocalVariables(localVariables, taintedLocalVariables, i);
                        instructionsToWrap.addAll(localInputVarTaintValue.instrsWhereProduced);
                        taintedInstructions.addAll(filterToAddToTaintedInstructions(localInputVarTaintValue.instrsWhereUsed));
                        if (!localInputVarTaintValue.isActuallyTainted) {
                            changed = true;
                        }
                        localInputVarTaintValue.isActuallyTainted = true;
                    }
                }

                // Taint stack values
                for (int i = 0; i < f.getStackSize(); i++) {
                    TaintValue val = f.getStack(i);
                    if (val.instrsWhereProduced.stream().anyMatch(insn ->
                            taintedInstructions.contains(insn) || instructionsToWrap.contains(insn))) {
                        if (!val.isActuallyTainted) {
                            changed = true;
                        }
                        // If any of the instructions where the current stack value was produced is tainted or wrapped,
                        // we must continue the taint.
                        // This value now is tainted.
                        val.isActuallyTainted = true;
                        // Those instructions that yield the given value, if not already tainted, must be wrapped.
                        val.instrsWhereProduced
                                .stream()
                                .filter(insn -> !taintedInstructions.contains(insn))
                                .forEach(instructionsToWrap::add);
                        // A tainted value must only be used by another tainted instruction.
                        taintedInstructions.addAll(filterToAddToTaintedInstructions(val.instrsWhereUsed));
                        val.instrsWhereUsed.stream()
                                .filter(insn -> insn instanceof VarInsnNode)
                                .forEach(vin -> addToTaintedLocalVariables(localVariables, taintedLocalVariables, ((VarInsnNode) vin).var));
                    }

                    // If the value is used by a tainted instruction...
                    if (val.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains)) {
                        if (!val.isActuallyTainted) {
                            changed = true;
                        }
                        val.isActuallyTainted = true;
                        // ...and the instructions producing this value are not already tainted, they must be wrapped.
                        val.instrsWhereProduced
                                .stream()
                                .filter(insn -> !taintedInstructions.contains(insn))
                                .forEach(instructionsToWrap::add);
                    }
                }
            }
            for (int i = 0; i < localVarIndexToInstrsWhereProduced.size(); i++) {
                Set<AbstractInsnNode> currentProduced = localVarIndexToInstrsWhereProduced.get(i);
                Set<AbstractInsnNode> currentUsed = localVarIndexToInstrsWhereUsed.get(i);
                if (currentProduced.stream().anyMatch(taintedInstructions::contains)) {
                    instructionsToWrap.addAll(currentProduced);
                    taintedInstructions.addAll(filterToAddToTaintedInstructions(currentUsed));
                    addToTaintedLocalVariables(localVariables, taintedLocalVariables, i);
                }
            }
        }

        // Wrap those PUTFIELDs where the output of a non-tainted instruction is consumed
        for (int i = 0; i < mn.instructions.size(); i++) {
            AbstractInsnNode ain = mn.instructions.get(i);
            if (ain.getOpcode() != PUTFIELD && ain.getOpcode() != PUTSTATIC) {
                continue;
            }
            Frame<TaintValue> frameForPutfield = frames[i];
            // PUTFIELD is defined to manipulate the operand stack as ..., objectref, value -> ...
            // We thus check the top of the stack
            TaintValue value = frameForPutfield.getStack(frameForPutfield.getStackSize() - 1);
            if (!value.isActuallyTainted) {
                instructionsToWrap.add(ain);
            }
        }

        int[] newLocalVariablesIndices = newLocalVariablesIndices(mn.maxLocals, taintedLocalVariables, localVariables);

        // From the tainted instructions, determine those instructions that use boolean values. This is necessary
        // to create Sbools, Sbytes, and Sshorts since Java bytecode does not differentiate between booleans, bytes, shorts, and ints.
        Set<AbstractInsnNode> taintedBoolInsns = new HashSet<>();
        Set<AbstractInsnNode> toWrapSinceUsedByBoolInsns = new HashSet<>();
        Set<AbstractInsnNode> taintedByteInsns = new HashSet<>();
        Set<AbstractInsnNode> toWrapSinceUsedByByteInsns = new HashSet<>();
        Set<AbstractInsnNode> taintedShortInsns = new HashSet<>();
        Set<AbstractInsnNode> toWrapSinceUsedByShortInsns = new HashSet<>();
        // If the return type is boolean, we add this instruction to the tainted boolean instructions as well.
        boolean returnsBoolean = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("Z");
        boolean returnsByte = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("B");
        boolean returnsShort = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("S");
        determineTaintedBoolShortBytInsns(
                returnsBoolean, returnsByte, returnsShort,
                taintedInstructions, instructionsToWrap,
                mn.instructions, mn.localVariables, frames,
                taintedBoolInsns, taintedByteInsns, taintedShortInsns,
                toWrapSinceUsedByBoolInsns, toWrapSinceUsedByByteInsns, toWrapSinceUsedByShortInsns
        );


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
                TaintValue upperStack = frame.getStack(frame.getStackSize() - 1);
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
                            TaintValue iastoreUpperStack = iastoreFrame.getStack(iastoreFrame.getStackSize() - 2);
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
        Map<AbstractInsnNode, String> concretizeForMethodCall = new HashMap<>();
        Set<MethodInsnNode> tryToGeneralize = new HashSet<>();
        for (MethodInsnNode min : methodsToPreserve) {
            assert !taintedInstructions.contains(min) : "Should not occur";
            if (!splitMethodDesc(min.desc)[1].equals("V")) {
                // Decide on whether to wrap output
                Frame<TaintValue> frameWithStackValueOfMethodOutput = frames[mn.instructions.indexOf(min.getNext())];
                TaintValue tv = frameWithStackValueOfMethodOutput.getStack(frameWithStackValueOfMethodOutput.getStackSize() - 1);
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
//                    numParams = min.getOpcode() == INVOKESTATIC ? numParams : numParams + 1;
                    int currentParam = 0;
                    methodSearch:
                    for (int i = frameOfMethodCall.getStackSize() - numParams; i < frameOfMethodCall.getStackSize(); i++) {
                        TaintValue tv = frameOfMethodCall.getStack(i);
                        assert tv.instrsWhereProduced.size() <= 1 : "Not yet regarded";
                        for (AbstractInsnNode prod : tv.instrsWhereProduced) {
                            assert !instructionsToWrap.contains(prod);
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

        for (int i = 0; i < ains.length; i++) {
            AbstractInsnNode ain = ains[i];
            // If 2 are popped because one of them is a long/double, and the long/double is tainted, we must taint POP2
            // so that it is replaced by POP. This is because after substituting, only one slot is taken opposed to
            // two slots (which is the case for longs and doubles)
            if (ain.getOpcode() == POP2) {
                Frame<TaintValue> f = frames[i];
                TaintValue topOfStack = f.getStack(f.getStackSize() - 1);
                if (topOfStack.size == 2 && topOfStack.instrsWhereProduced.stream().anyMatch(taintedInstructions::contains)) {
                    taintedInstructions.add(ain);
                }
            }
        }

        return new TaintAnalysis(
                taintedLocalVariables, taintedInstructions,
                instructionsToWrap, frames,
                newLocalVariablesIndices,
                taintedBoolInsns, toWrapSinceUsedByBoolInsns,
                taintedByteInsns, toWrapSinceUsedByByteInsns,
                taintedShortInsns, toWrapSinceUsedByShortInsns,
                returnsBoolean, returnsByte, returnsShort,
                concretizeForMethodCall,
                tryToGeneralize
        );
    }

    private Collection<AbstractInsnNode> filterToAddToTaintedInstructions(Set<AbstractInsnNode> ains) {
        return ains.stream()
                .filter(insn ->
                        (!(insn instanceof MethodInsnNode)
                                || mulibTransformer.shouldBeTransformed(((MethodInsnNode) insn).owner)))
                .collect(Collectors.toList());
    }


    // Determine the subset of tainted instructions and instructions to wrap which are boolean instructions.
    // TODO Possibly tainted boolean instructions are not necessarily subset of tainted, since the JMP to initialize booleans "cut-off" taint flow; treat this in the TaintInterpreter
    private static void determineTaintedBoolShortBytInsns(
            boolean returnsBoolean, boolean returnsByte, boolean returnsShort,
            Set<AbstractInsnNode> taintedInstructions, Set<AbstractInsnNode> instructionsToWrap,
            InsnList oldInstructionList, List<LocalVariableNode> oldLocalVariables, Frame<TaintValue>[] frames,
            Set<AbstractInsnNode> taintedBoolInsns, Set<AbstractInsnNode> taintedByteInsns, Set<AbstractInsnNode> taintedShortInsns,
            Set<AbstractInsnNode> toWrapSinceUsedByBoolInsns, Set<AbstractInsnNode> toWrapSinceUsedByByteInsns, Set<AbstractInsnNode> toWrapSinceUsedByShortInsns) {

        for (AbstractInsnNode ain : taintedInstructions) {
            // We start by looking for boolean, byte, and short local variable nodes
            if (ain.getOpcode() == ILOAD
                    || ain.getOpcode() == ISTORE
                    || ain instanceof  FieldInsnNode) {
                byte type = -1; // 0 = boolean, 1 = byte, 2 = short
                if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                    // Get local variables in scope with the index
                    LocalVariableNode lvn =
                            getLocalVariableInScopeOfInsn((VarInsnNode) ain, oldInstructionList, oldLocalVariables);
                    // Check if the variable is boolean
                    if (!lvn.desc.equals("Z") && !lvn.desc.equals("B") && !lvn.desc.equals("S")) {
                        continue; // Not of type boolean, byte, or short --> not relevant here.
                    }
                    type = (byte) (
                            lvn.desc.equals("Z") ? 0
                            : lvn.desc.equals("B") ? 1 : 2);
                } else if (ain instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ain;
                    String desc = fin.desc;
                    if (!desc.equals("Z")
                            && !desc.equals("B")
                            && !desc.equals("S")) {
                        continue; // Not of type boolean, byte, or short --> not relevant here.
                    }
                    type = (byte) (
                            desc.equals("Z") ? 0
                            : desc.equals("B") ? 1 : 2);
                }
                // Get those values that were computes using the now-loaded value. Then, get those values that
                // use the respective computed values (if the computations are boolean comparisons).
                int indexOfLoaded = oldInstructionList.indexOf(ain.getOpcode() == ILOAD || ain.getOpcode() == GETFIELD || ain.getOpcode() == GETSTATIC ? ain.getNext() : ain);
                Frame<TaintValue> frameOfInstruction = frames[indexOfLoaded];
                // Now we mark those instructions as boolean instructions that use the instruction to produce new booleans.
                // Consuming instructions of booleans (e.g. JumpInsnNodes) are the end points of this analysis.
                TaintValue stackValueOfLoadingSpecialTypeVar = frameOfInstruction.getStack(frameOfInstruction.getStackSize() - 1);

                Set<AbstractInsnNode> toAdd = new HashSet<>(stackValueOfLoadingSpecialTypeVar.instrsWhereUsed);
                toAdd.addAll(stackValueOfLoadingSpecialTypeVar.instrsWhereProduced);
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        instructionsToWrap,
                        taintedInstructions,
                        type == 0 ? taintedBoolInsns : type == 1 ? taintedByteInsns : taintedShortInsns,
                        type == 0 ? toWrapSinceUsedByBoolInsns : type == 1 ? toWrapSinceUsedByByteInsns : toWrapSinceUsedByShortInsns,
                        toAdd
                );
            } else if (ain.getOpcode() == IRETURN && (returnsBoolean || returnsByte || returnsShort)) {
                int indexOfReturn = oldInstructionList.indexOf(ain);
                Frame<TaintValue> frameOfInstruction = frames[indexOfReturn];
                TaintValue stackValueReturned = frameOfInstruction.getStack(frameOfInstruction.getStackSize() - 1);
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        instructionsToWrap,
                        taintedInstructions,
                        returnsBoolean ? taintedBoolInsns : returnsByte ? taintedByteInsns : taintedShortInsns,
                        returnsBoolean ? toWrapSinceUsedByBoolInsns : returnsByte ? toWrapSinceUsedByByteInsns : toWrapSinceUsedByShortInsns,
                        stackValueReturned.instrsWhereUsed
                );
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        instructionsToWrap,
                        taintedInstructions,
                        taintedBoolInsns,
                        toWrapSinceUsedByBoolInsns,
                        stackValueReturned.instrsWhereProduced
                );
            } else if (ain.getOpcode() == ICONST_0 || ain.getOpcode() == ICONST_1) { throw new NotYetImplementedException(); }
        }

        for (AbstractInsnNode ain : instructionsToWrap) {
            byte type = -1;
            if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                LocalVariableNode lvn =
                        getLocalVariableInScopeOfInsn((VarInsnNode) ain, oldInstructionList, oldLocalVariables);
                // Check if the variable is boolean
                if (!lvn.desc.equals("Z") && !lvn.desc.equals("B") && !lvn.desc.equals("S")) {
                    continue; // Not of type boolean, byte, or short --> not relevant here.
                }
                type = (byte) (
                        lvn.desc.equals("Z") ? 0
                                : lvn.desc.equals("B") ? 1 : 2);
                if (type == 0)
                    toWrapSinceUsedByBoolInsns.add(ain);
                else if (type == 1)
                    toWrapSinceUsedByByteInsns.add(ain);
                else
                    toWrapSinceUsedByShortInsns.add(ain);
            } else if (ain instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) ain;
                String desc = fin.desc;
                if (!desc.equals("Z")
                        && !desc.equals("B")
                        && !desc.equals("S")) {
                    continue; // Not of type boolean, byte, or short --> not relevant here.
                }
                type = (byte) (
                        desc.equals("Z") ? 0
                                : desc.equals("B") ? 1 : 2);
                if (type == 0)
                    toWrapSinceUsedByBoolInsns.add(ain);
                else if (type == 1)
                    toWrapSinceUsedByByteInsns.add(ain);
                else
                    toWrapSinceUsedByShortInsns.add(ain);
            } else if (ain.getOpcode() == ICONST_0 || ain.getOpcode() == ICONST_1) {
                int index = oldInstructionList.indexOf(ain.getNext());
                // Get taint value pushed on stack for ICONST_0 or ICONST_1
                TaintValue tvOfIconst = frames[index].getStack(frames[index].getStackSize() - 1);
                List<AbstractInsnNode> methodInsnNodes =
                        tvOfIconst.instrsWhereUsed.stream()
                                .filter(insns -> insns instanceof MethodInsnNode)
                                .collect(Collectors.toList());
                if (methodInsnNodes.isEmpty()) {
                    continue;
                }
                MethodInsnNode anyMethodInsn = (MethodInsnNode) methodInsnNodes.get(0);
                // Check signature to see if it should be a boolean value
                String[] paramsDescSplit = splitMethodDesc(anyMethodInsn.desc)[0].split(";");
                // Get frame of method insn
                Frame<TaintValue> frameOfMethodInsn = frames[oldInstructionList.indexOf(anyMethodInsn)];
                // Check stack at this position. At which position is tvOfIconst?
                int posInParams = -1;
                for (int i = frameOfMethodInsn.getStackSize() - 1; i >= 0; i--) {
                    posInParams++;
                    TaintValue tv = frameOfMethodInsn.getStack(i);
                    if (tv == tvOfIconst) {
                        break;
                    }
                }
                // Compare the position of ICONST_{0,1}-generated value with desc
                String[] descs = getSingleDescsFromMethodParams(paramsDescSplit[0]);
                String desc = descs[posInParams];
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

    private static LocalVariableNode getLocalVariableInScopeOfInsn(
            VarInsnNode insn,
            InsnList l,
            List<LocalVariableNode> lvns) {
        int indexOfInsn = l.indexOf(insn);
        for (LocalVariableNode lvn : lvns) {
            if (lvn.index == insn.var) {
                int indexStartLvn = l.indexOf(lvn.start);
                if (insn.getOpcode() >= ISTORE && insn.getOpcode() <= ASTORE) {
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
        throw new MulibRuntimeException("Should not occur!");
    }
    private static void decideOnTaintedOrWrappedForSpecialTypeInsn(
            Set<AbstractInsnNode> instructionsToWrap,
            Set<AbstractInsnNode> taintedInstructions,
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
                    } else { // Boolean instructions should be subset
                        assert false;
                    }
                });
    }

    private static int[] newLocalVariablesIndices(int maxLocals, Set<LocalVariableNode> taintedLocalVariables, List<LocalVariableNode> localVariables) {
        int[] newLocalVariablesIndices = new int[maxLocals];
        Arrays.fill(newLocalVariablesIndices, -1);
        int numberTaintedLocalInputVariablesOfSize2 = 0;
        for (int i = 0; i < maxLocals; i++) {
            boolean taintedSized2 = false;
            Set<LocalVariableNode> lvnsOfIndex = getLocalVariableNodesWithIndex(i, localVariables);
            for (LocalVariableNode lvn : lvnsOfIndex) {
                assert newLocalVariablesIndices[lvn.index] == -1
                        || newLocalVariablesIndices[lvn.index] == lvn.index - numberTaintedLocalInputVariablesOfSize2 : "Unexpected";
                newLocalVariablesIndices[lvn.index] = lvn.index - numberTaintedLocalInputVariablesOfSize2;
                if ((lvn.desc.equals("D") || lvn.desc.equals("J")) && taintedLocalVariables.contains(lvn)) {
                    taintedSized2 = true;
                }
            }
            if (taintedSized2) {
                numberTaintedLocalInputVariablesOfSize2++;
            }
        }
        return newLocalVariablesIndices;
    }

    private static Set<LocalVariableNode> getLocalVariableNodesWithIndex(int index, List<LocalVariableNode> lvns) {
        Set<LocalVariableNode> result = new HashSet<>();
        String descriptor = null;
        for (LocalVariableNode lvn : lvns) {
            if (lvn.index == index) {
                result.add(lvn);
                if (descriptor == null) {
                    descriptor = lvn.desc;
                }
            }
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
            localInputVarTaintValue.isActuallyTainted = true;
        }
    }

    private static void addToTaintedLocalVariables(
            List<LocalVariableNode> localVariableNodes,
            Set<LocalVariableNode> taintedLocalVariables,
            int toAddIndex) {
        for (LocalVariableNode lvn : localVariableNodes) { /// TODO Better: directly retrieve suiting local variable. in case local variable indexes are used multiple times, we do not always need to taint them all.
            if (lvn.index == toAddIndex) {
                taintedLocalVariables.add(lvn);
            }
        }
    }
}
