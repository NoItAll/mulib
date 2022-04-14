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

    private final Map<AbstractInsnNode, String> concretizeForMethodCall = new HashMap<>();
    private final Set<MethodInsnNode> tryToGeneralize = new HashSet<>();

    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereProduced = new HashMap<>();
    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereUsed = new HashMap<>();
    // Key is AbstractInsnNode to allow for IincInsnNode
    private final Map<AbstractInsnNode, LocalVariableNode> varInsnNodesToReferencedLocalVar = new HashMap<>(); /// TODO Perhaps create artificial LVNs for anonymous local vars?

    // LocalVariableNode -> new index
    private final Map<LocalVariableNode, Integer> newLvnIndices = new HashMap<>();

    // VarInsnNode or IincInsnNode --> new index
    private final Map<AbstractInsnNode, Integer> newIndexInsnIndices = new HashMap<>();

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
                    assert (f.getStack(i).instrsWhereUsed.stream().filter(insn -> insn.getOpcode() != DUP).count() <= 1) : "This can occur using JMPs, but it is not yet dedicatedly checked";
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
            Frame<TaintValue> firstFrame = frames[0];
            for (Frame<TaintValue> f : frames) {
                for (int j = 0; j < f.getLocals(); j++) {
                    TaintValue localVal = f.getLocal(j);
                    localVal.index = j;
                    if (localVal.size == 2) {
                        if (f == firstFrame && numberInputs + numberOfInputsSized2 - j > 0) {
                            // We only need to determine the number of inputs sized 2 for the first frame
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

    public TaintAnalysis analyze() {
        /*
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES FOR PRIMITIVES
        OR A PARTNER CLASS MUST BE USED.
        */
        prepareLocalVariablesToUsingInstructionsMapping();
        gatherInitialTaintedAndWrappedInstructions();
        calculateTaintFixedPoint();

        /* Add those instructions that must be concretized, since they are used in a non-transformed method, yet, are tainted. */
        determineSpecialMethodTreatment();

        // Determine new indices of explicit local variables
        newLocalVariableIndices();
        // Gather VarInsnNodes and IincInsnNodes
        // Determine new indices of indexInsns (including those using anonymous local variables)
        newLocalVariableIndexInsns();

        // Get the maximal variable index, this is later used to determine the index of SymbolicExecution.
        int maxVarIndexInsn = newIndexInsnIndices.values().stream()
                .max(Integer::compareTo).orElse(-1);

        // Used to decide on, e.g., which type is loaded from SarraySarray or PartnerClassSarray (for casting) etc.
        SarraySarrayPartnerClassSarrayDistinguisher sarrayDistinguisher =
                new SarraySarrayPartnerClassSarrayDistinguisher(
                        mn, taintedInstructions,
                        instructionsToWrap, frames,
                        varInsnNodesToReferencedLocalVar
                );

        // Necessary since, e.g., JVM bytecode oftentimes does not differentiate between bytes and booleans and ints
        BooleanByteShortAndCharDistinguisher primitiveDistinguisher =
                new BooleanByteShortAndCharDistinguisher(
                        mn, taintedInstructions,
                        instructionsToWrap, frames,
                        varInsnNodesToReferencedLocalVar
                );

        // Gather all results in container
        return new TaintAnalysis(
                taintedLocalVariables, taintedInstructions,
                instructionsToWrap, frames,
                primitiveDistinguisher.getTaintedBoolInsns(), primitiveDistinguisher.getToWrapSinceUsedByBoolInsns(),
                primitiveDistinguisher.getTaintedByteInsns(), primitiveDistinguisher.getToWrapSinceUsedByByteInsns(),
                primitiveDistinguisher.getTaintedShortInsns(), primitiveDistinguisher.getToWrapSinceUsedByShortInsns(),
                primitiveDistinguisher.returnsBoolean(), primitiveDistinguisher.returnsByte(), primitiveDistinguisher.returnsShort(),
                concretizeForMethodCall,
                tryToGeneralize,
                newLvnIndices,
                newIndexInsnIndices,
                maxVarIndexInsn,
                sarrayDistinguisher.getTaintedNewObjectArrayInsns(),
                sarrayDistinguisher.getTaintedNewArrayArrayInsns(),
                sarrayDistinguisher.getSelectedTypeFromSarray()
        );
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
                addTainted(ain);
                if (ain.getOpcode() == PUTFIELD || ain.getOpcode() == PUTSTATIC) {
                    // PUTFIELD is defined to manipulate the operand stack as ..., objectref, value -> ...
                    // We thus check the top of the stack
                    TaintValue value = getFromTopOfStack(frameOfInsn);
                    addWrapped(value.instrsWhereProduced);
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
                }
                if (specialArrayInitializationMethods.contains(m.name)) {
                    taintMethodInsnAndParameters(m);
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
                }
                taintMethodInsnAndParameters(min);
            } else if (ain.getOpcode() == NEW || ain.getOpcode() == ANEWARRAY) {
                TypeInsnNode n = (TypeInsnNode) ain;
                if (mulibTransformer.shouldBeTransformed(n.desc)) {
                    addTainted(n);
                }
            } else if (ain.getOpcode() >= IRETURN && ain.getOpcode() <= ARETURN) {
                addTainted(ain);
                InsnNode insn = (InsnNode) ain;
                Frame<TaintValue> f = frames[mn.instructions.indexOf(insn)];
                TaintValue tv = getFromTopOfStack(f);
                addWrapped(tv.instrsWhereProduced);
            }

            // Also taint the local variables if they are part of the input
            for (int i = 0; i < numberInputs + numberOfInputsSized2; i++) {
                addToTaintedInstructions(frameOfInsn, i);
            }
        }
    }

    private void taintMethodInsnAndParameters(MethodInsnNode min) {
        taintedInstructions.add(min);
        int ainIndex = mn.instructions.indexOf(min);
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
            addWrapped(fromTopOfStack.instrsWhereProduced);
            currentParamNumber++;
        }

        // Add all instructions using the non-void return value to the set of tainted instructions
        if (methodDescSplit[1].equals("V")) {
            return;
        }
        int indexOfMethodInsnReturnValue = mn.instructions.indexOf(min.getNext());
        Frame<TaintValue> frameOfMethodInsnReturnValue = frames[indexOfMethodInsnReturnValue];
        TaintValue methodInsnReturnValue = getFromTopOfStack(frameOfMethodInsnReturnValue);
        addTainted(methodInsnReturnValue.instrsWhereUsed);
    }

    private boolean addTainted(Collection<AbstractInsnNode> toAdd) {
        List<Boolean> changed = toAdd.stream()
                .map(this::addTainted)
                .collect(Collectors.toList());
        return changed.stream().anyMatch(Boolean::booleanValue);
    }

    private boolean canBeAddedToTainted(AbstractInsnNode toAdd) {
        return !(toAdd instanceof MethodInsnNode);
    }

    private boolean addTainted(AbstractInsnNode toAdd) {
        if (!canBeAddedToTainted(toAdd)) {
            return false;
        }
        boolean changed = false;
        if (toAdd instanceof VarInsnNode) {
            LocalVariableNode lvn = this.varInsnNodesToReferencedLocalVar.get(toAdd);
            if (lvn == null) {
                // Is anonymous variable that must be tainted; for this, we taint the respective
                // instructions using it
                Collection<AbstractInsnNode> insnUsingLocalVariable =
                    getInsnsUsingAnonymousLocalVariable(toAdd);
                changed = insnUsingLocalVariable.stream()
                        .filter(this::canBeAddedToTainted)
                        .map(insn ->
                                taintedInstructions.add(insn))
                        .anyMatch(Boolean::booleanValue);
            } else {
                // Taint those instructions using the local variable
                Set<AbstractInsnNode> producedBy = localVarToInstrsWhereProduced.get(lvn);
                Set<AbstractInsnNode> usedBy = localVarToInstrsWhereUsed.get(lvn);
                if (producedBy != null) {
                    localVarToInstrsWhereProduced.remove(lvn);
                    localVarToInstrsWhereUsed.remove(lvn);
                    taintedLocalVariables.add(lvn);
                    assert usedBy != null;
                    changed = true;
                    if (lvn.desc.startsWith("[") || lvn.desc.startsWith("L")) {
                        // Objects and arrays are tainted, not wrapped.
                        addTainted(producedBy);
                    } else {
                        addWrapped(producedBy);
                    }
                    addTainted(usedBy);
                }
            }
        }
        return taintedInstructions.add(toAdd) || changed;
    }

    private boolean addWrapped(Collection<AbstractInsnNode> toAdd) {
        List<Boolean> changed = toAdd.stream().map(this::addWrapped).collect(Collectors.toList());
        return changed.stream().anyMatch(Boolean::booleanValue);
    }

    private boolean addWrapped(AbstractInsnNode toAdd) {
        if (toAdd instanceof MethodInsnNode) {
            String[] descSplit = splitMethodDesc(((MethodInsnNode) toAdd).desc);
            if (descSplit[1].length() == 1 && primitiveTypes.contains(descSplit[1])) {
                return instructionsToWrap.add(toAdd);
            } else {
                // Is either already tainted or listed as to-be-ignored so it must not be wrapped
                return false;
            }
        }
        boolean isArrayInsnOrLoad = toAdd.getOpcode() == AALOAD
                || (toAdd.getOpcode() >= IASTORE && toAdd.getOpcode() <= SASTORE)
                || toAdd.getOpcode() == ANEWARRAY || toAdd.getOpcode() == NEWARRAY
                || toAdd.getOpcode() == MULTIANEWARRAY
                || toAdd.getOpcode() == ALOAD;
        if (isArrayInsnOrLoad) {
            return addTainted(toAdd);
        } else {
            return instructionsToWrap.add(toAdd);
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

    public static TaintValue getFromTopOfStack(Frame<TaintValue> frame, int offset) {
        return frame.getStack(frame.getStackSize() - (offset + 1));
    }

    public static TaintValue getFromTopOfStack(Frame<TaintValue> frame) {
        return getFromTopOfStack(frame, 0);
    }

    private void prepareLocalVariablesToUsingInstructionsMapping() {
        // Mapping from LocalVariableNode to instructions where produced/used. The LocalVariableNode is not necessarily
        // tainted. These mappings are used to check whether one of the produced/used instructions was tainted.
        // In this case, we add all other produced/used instructions to the set of tainted instructions.
        for (LocalVariableNode lvn : localVariables) {
            if (lvn.index < numberInputs + numberOfInputsSized2 - 1) {
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
                // Regard those frames in which the LocalVariableNode is valid
                if (lvnStartIndex > indexOfInstruction
                        || lvnEndIndex < indexOfInstruction) {
                    continue;
                }
                TaintValue current = f.getLocal(lvn.index);
                currentProduced.addAll(current.instrsWhereProduced);
                currentUsed.addAll(current.instrsWhereUsed);
                // Special treatment for arrays. Different from objects that are always
                // transformed to be of the same type as the partner class, we do not immediately assume that
                // arrays should be Sarrays. Instead, this is only the case if (an input of) XALOAD or XASTORE
                // instructions are tainted, or if ASTORE is tainted for this array
                // To associate the array local variable with its creation, we add the source of ASTORE
                if (lvn.desc.startsWith("[")) {
                    // ASTORE
                    Set<AbstractInsnNode> creationsOfAstoreValues = currentProduced.stream()
                            .filter(insn -> insn.getOpcode() == ASTORE)
                            .flatMap(astore -> {
                                Frame<TaintValue> frameOfAstore = frames[mn.instructions.indexOf(astore)];
                                return getFromTopOfStack(frameOfAstore).instrsWhereProduced.stream();
                            }).collect(Collectors.toSet());
                    currentProduced.addAll(creationsOfAstoreValues);
                }
            }
        }
        for (AbstractInsnNode ain : mn.instructions) {
            LocalVariableNode lvn = null;
            if (ain instanceof VarInsnNode) {
                lvn = getLocalVariableInScopeOfInsn(ain);
            } else if (ain instanceof IincInsnNode) {
                lvn = getLocalVariableInScopeOfInsn(ain);
            }
            if (lvn != null) {
                varInsnNodesToReferencedLocalVar.put(ain, lvn);
            }
        }
    }

    private void calculateTaintFixedPoint() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int numberOfFrame = 0; numberOfFrame < frames.length; numberOfFrame++) {
                Frame<TaintValue> f = frames[numberOfFrame];
                // Taint stack values
                for (int i = 0; i < f.getStackSize(); i++) {
                    TaintValue val = f.getStack(i);
                    // Check if the value is produced by a wrapped or a tainted instruction
                    if (val.instrsWhereProduced.stream().anyMatch(insn ->
                            taintedInstructions.contains(insn) || instructionsToWrap.contains(insn))) {
                        // If any of the instructions where the current stack value was produced is tainted or wrapped,
                        // we must continue the taint.
                        // This value now is tainted.
                        // Those instructions that yield the given value, if not already tainted, must be wrapped.
                        changed = addWrapped(val.instrsWhereProduced) || changed;
                        // A tainted value must only be used by another tainted instruction.
                        changed = addTainted(val.instrsWhereUsed) || changed;
                    }

                    // If the value is used by a tainted instruction...
                    if (val.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains)) {
                        // ...and the instructions producing this value are not already tainted, they must be wrapped.
                        changed = addWrapped(val.instrsWhereProduced) || changed;
                    }
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
                TaintValue topOfStack = getFromTopOfStack(f);
                if (topOfStack.size == 2 && topOfStack.instrsWhereProduced.stream().anyMatch(taintedInstructions::contains)) {
                    addTainted(ain);
                }
            }
        }
    }

    private void determineSpecialMethodTreatment() {
        for (MethodInsnNode min : methodsToPreserve) {
            assert !taintedInstructions.contains(min) : "Should not occur";
            if (!splitMethodDesc(min.desc)[1].equals("V")) {
                // Decide on whether to wrap output
                Frame<TaintValue> frameWithStackValueOfMethodOutput = frames[mn.instructions.indexOf(min.getNext())];
                TaintValue tv = getFromTopOfStack(frameWithStackValueOfMethodOutput);
                boolean anyTainted = tv.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains);
                if (anyTainted) {
                    // Sanity check
                    assert taintedInstructions.containsAll(tv.instrsWhereUsed);
                    addWrapped(min);
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

    private LocalVariableNode getLocalVariableInScopeOfInsn(
            AbstractInsnNode insn) {
        if (insn instanceof VarInsnNode) {
            return getLocalVariableInScopeOfInsn(
                    mn.instructions.indexOf(insn),
                    ((VarInsnNode) insn).var,
                    insn.getOpcode(),
                    mn.instructions,
                    localVariables
            );
        } else if (insn instanceof IincInsnNode) {
            return getLocalVariableInScopeOfInsn(
                    mn.instructions.indexOf(insn),
                    ((IincInsnNode) insn).var,
                    insn.getOpcode(),
                    mn.instructions,
                    localVariables
            );
        } else {
            throw new MulibRuntimeException("Expecting either VarInsnNode or IincInsnNode.");
        }
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

    private Collection<AbstractInsnNode> getInsnsUsingAnonymousLocalVariable(AbstractInsnNode ain) {
        if (ain instanceof VarInsnNode) {
            return getInsnsUsingAnonymousLocalVariable(((VarInsnNode) ain).var);
        } else if (ain instanceof IincInsnNode) {
            assert ain.getOpcode() == IINC;
            return getInsnsUsingAnonymousLocalVariable(((IincInsnNode) ain).var);
        } else {
            throw new MulibRuntimeException("We expect a VarInsnNode or an IincInsnNode here");
        }
    }

    private Collection<AbstractInsnNode> getInsnsUsingAnonymousLocalVariable(int insnVar) {
        Collection<AbstractInsnNode> result = new ArrayList<>();
        InsnList insns = mn.instructions;
        List<LocalVariableNode> lvns = mn.localVariables;
        List<int[]> reservedIntervals = new ArrayList<>();
        for (LocalVariableNode lvn : lvns) {
            // Check if there are other local variable nodes that have this index
            if (lvn.index == insnVar) {
                int s = insns.indexOf(lvn.start);
                int e = insns.indexOf(lvn.end);
                reservedIntervals.add(new int[] {s, e});
            }
        }

        // Add those instructions using the variable where not in reservedInterval
        int var;
        int ainPos;
        nextAin:
        for (AbstractInsnNode ain : insns) {
            if (ain instanceof VarInsnNode) {
                var = ((VarInsnNode) ain).var;
            } else if (ain instanceof IincInsnNode) {
                var = ((IincInsnNode) ain).var;
            } else {
                continue;
            }
            // If not the same position in variable table is regarded: continue
            if (var != insnVar) {
                continue;
            }
            ainPos = insns.indexOf(ain);
            // Check all intervals
            for (int[] ri : reservedIntervals) {
                // If is in interval, move on to next ain
                if (ri[0] <= ainPos && ri[1] >= ainPos) {
                    continue nextAin;
                }
            }
            // Otherwise, add to result
            result.add(ain);
        }

        return result;
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

    private void newLocalVariableIndexInsns() {
        List<AbstractInsnNode> indexInsns = getIndexInsns(mn.instructions);

        for (AbstractInsnNode indexInsn : indexInsns) {
            LocalVariableNode lvn;
            int indexInsnVarIndex;
            if (indexInsn instanceof VarInsnNode) {
                indexInsnVarIndex = ((VarInsnNode) indexInsn).var;
                lvn = varInsnNodesToReferencedLocalVar.get(indexInsn);
            } else if (indexInsn instanceof IincInsnNode) {
                indexInsnVarIndex = ((IincInsnNode) indexInsn).var;
                lvn = varInsnNodesToReferencedLocalVar.get(indexInsn);
            } else {
                throw new MulibRuntimeException("Only VarInsnNode and IincInsnNode should be used here");
            }
            if (lvn == null) {
                // In this case, we have a index instruction that uses an anonymous local variable
                int number = getNumberTaintedLocalVariablesWithTwoSlotsInScopeOfInsn(
                        mn.instructions.indexOf(indexInsn),
                        indexInsnVarIndex,
                        indexInsn.getOpcode(),
                        mn.instructions,
                        taintedLocalVariables
                );
                newIndexInsnIndices.put(indexInsn, indexInsnVarIndex - number);
            } else {
                // We have a local variable node within the scope, we can simply use its new index
                Integer newIndex = newLvnIndices.get(lvn);
                assert newIndex != null;
                newIndexInsnIndices.put(indexInsn, newIndex);
            }
        }

    }

    private static List<AbstractInsnNode> getIndexInsns(InsnList insns) {
        return Arrays.stream(insns.toArray())
                .filter(insn -> insn instanceof VarInsnNode || insn instanceof IincInsnNode)
                .collect(Collectors.toList());
    }

    private void newLocalVariableIndices() {
        InsnList insns = mn.instructions;
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
        for (List<LocalVariableNode> affectedLocalVariables : taintedDoubleSlotsToAffectedLocalVariables.values()) {
            for (LocalVariableNode affected : affectedLocalVariables) {
                newLvnIndices.putIfAbsent(affected, affected.index);
                newLvnIndices.put(affected, newLvnIndices.get(affected) - 1);
            }
        }
        // Add the unaffected local variable nodes as well
        for (LocalVariableNode lvn : localVariables) {
            newLvnIndices.putIfAbsent(lvn, lvn.index);
        }
    }

    private void addToTaintedInstructions(
            Frame<TaintValue> f,
            int toAddForIndex) {
        for (int i = 0; i < f.getLocals(); i++) {
            TaintValue localInputVarTaintValue = f.getLocal(i);
            if (localInputVarTaintValue.index != toAddForIndex) continue;
            addTainted(localInputVarTaintValue.instrsWhereProduced);
            addTainted(localInputVarTaintValue.instrsWhereUsed);
        }
    }
}
