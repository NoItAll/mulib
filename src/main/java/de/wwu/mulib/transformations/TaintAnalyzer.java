package de.wwu.mulib.transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TransformationUtility.*;
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

    private final Set<AbstractInsnNode> taintedNewObjectArrayInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedNewArrayArrayInsns = new HashSet<>();
    // For PartnerClassSarrays and SarraySarrays we need to determine the selected type so that we can cast to it
    private final Map<AbstractInsnNode, String> selectedTypeFromSarray = new HashMap<>();

    private boolean returnsBoolean;
    private boolean returnsByte;
    private boolean returnsShort;

    private final Map<AbstractInsnNode, String> concretizeForMethodCall = new HashMap<>();
    private final Set<MethodInsnNode> tryToGeneralize = new HashSet<>();

    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereProduced = new HashMap<>();
    private final Map<LocalVariableNode, Set<AbstractInsnNode>> localVarToInstrsWhereUsed = new HashMap<>();
    // Key is AbstractInsnNode to allow for IincInsnNode
    private final Map<AbstractInsnNode, LocalVariableNode> varInsnNodesToReferencedLocalVar = new HashMap<>();

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

    public TaintAnalysis analyze() {
        /*
        GATHER INFORMATION ON TAINTED LOCAL VARIABLES.
        WE ASSUME THAT EACH INPUT-ARGUMENT CAN BE SYMBOLIC, THUS MULIB'S OWN TYPES FOR PRIMITIVES
        OR A PARTNER CLASS MUST BE USED.
        */
        prepareLocalVariablesToUsingInstructionsMapping();
        gatherInitialTaintedAndWrappedInstructions();
        calculateTaintFixedPoint();

        /* Add special method-instruction, Mulib.freeArray etc., that initialize an object array or an array array to a designated map
         * This is done to differentiate at transformation time since both types of arrays are indicated by the same method
         */
        determineNewObjectArrayOrArrayArrayInsnsAndDimensionalityOfSarraySarrays();
        // Differentiate Sarray.PartnerClassSarray and Sarray.SarraySarray for XASELECT and XASTORE
        determineWhetherTaintedPartnerClassSarrayOrSarraySarrayInsnAndSelectedType();

        // From the tainted instructions, determine those instructions that use boolean, byte, or short values. This is necessary
        // to create Sbools, Sbytes, and Sshorts since Java bytecode does not differentiate between booleans, bytes, shorts, and ints.
        // If the return type is boolean, we add this instruction to the tainted boolean instructions as well.
        determineTaintedBoolShortByteInsns();

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
                maxVarIndexInsn,
                taintedNewObjectArrayInsns,
                taintedNewArrayArrayInsns,
                selectedTypeFromSarray
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
                    addToWrap(value.instrsWhereProduced);
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
                addToWrap(tv.instrsWhereProduced);
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
            addToWrap(fromTopOfStack.instrsWhereProduced);
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
                        .map(taintedInstructions::add)
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
                        addToWrap(producedBy);
                    }
                    addTainted(usedBy);
                }
            }
        }
        return taintedInstructions.add(toAdd) || changed;
    }

    private boolean addToWrap(Collection<AbstractInsnNode> toAdd) {
        List<Boolean> changed = toAdd.stream().map(this::addToWrap).collect(Collectors.toList());
        return changed.stream().anyMatch(Boolean::booleanValue);
    }

    private boolean addToWrap(AbstractInsnNode toAdd) {
        if (toAdd instanceof MethodInsnNode) {
            String[] descSplit = splitMethodDesc(((MethodInsnNode) toAdd).desc);
            if (descSplit[1].length() == 1 && primitiveTypes.contains(descSplit[1])) {
                return instructionsToWrap.add(toAdd);
            } else {
                // Is either already tainted or listed as to-be-ignored so it must not be wrapped
                return false;
            }
        }
        boolean isArrayInsnOrLoad = (toAdd.getOpcode() >= IALOAD && toAdd.getOpcode() <= SALOAD)
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

    private static TaintValue getFromTopOfStack(Frame<TaintValue> frame, int offset) {
        return frame.getStack(frame.getStackSize() - (offset + 1));
    }

    private static TaintValue getFromTopOfStack(Frame<TaintValue> frame) {
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
                        changed = addToWrap(val.instrsWhereProduced) || changed;
                        // A tainted value must only be used by another tainted instruction.
                        changed = addTainted(val.instrsWhereUsed) || changed;
                    }

                    // If the value is used by a tainted instruction...
                    if (val.instrsWhereUsed.stream().anyMatch(taintedInstructions::contains)) {
                        // ...and the instructions producing this value are not already tainted, they must be wrapped.
                        changed = addToWrap(val.instrsWhereProduced) || changed;
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

    private void determineWhetherTaintedPartnerClassSarrayOrSarraySarrayInsnAndSelectedType() {
        for (AbstractInsnNode ain : taintedInstructions) {
            boolean isAALOAD = ain.getOpcode() == AALOAD;
            boolean isAASTORE = ain.getOpcode() == AASTORE;
            if (!isAALOAD && !isAASTORE) {
                continue;
            }
            InsnNode in = (InsnNode) ain;
            // Determine which type of array they belong to
            int stackOffsetToArrayref;
            if (isAALOAD) {
                // stack: ..., arrayref, index
                stackOffsetToArrayref = 1;
            } else { // AASTORE == true
                // stack: ..., arrayref, index, value
                stackOffsetToArrayref = 2;
            }
            Frame<TaintValue> frameOfInsn = frames[mn.instructions.indexOf(in)];
            TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfInsn, stackOffsetToArrayref);
            List<TaintValue> checkForSource = new ArrayList<>(arrayrefTaintValue.producedBy);
            // This way we can determine the desc of the array which is to be returned
            checkForSource.add(null);
            int additionalAALOADs = 0;
            boolean added = false;
            boolean seenAALOAD = false;
            String descOfRootArrayTargetedByStoreOrLoad = null;
            outerLoop:
            while (!checkForSource.isEmpty()) {
                TaintValue check = checkForSource.remove(0);
                if (check == null) {
                    if (seenAALOAD) {
                        seenAALOAD = false;
                        additionalAALOADs++;
                    }
                    continue;
                }
                for (AbstractInsnNode potentialArrayInitializer : check.instrsWhereProduced) {
                    if (potentialArrayInitializer instanceof MethodInsnNode) {
                        /// TODO GETFIELD, GETSTATIC?
                        String mdesc = ((MethodInsnNode) potentialArrayInitializer).desc;
                        // Check if result is casted
                        Frame<TaintValue> frameOfResultValueOfPotentialArrayInitializer
                                = frames[mn.instructions.indexOf(potentialArrayInitializer.getNext())];
                        // Method must have a return value, otherwise it would not be a producer
                        assert splitMethodDesc(mdesc)[1] != null && !splitMethodDesc(mdesc)[1].equals("V");
                        TaintValue returnValueOfMethodCall = getFromTopOfStack(frameOfResultValueOfPotentialArrayInitializer);
                        Optional<AbstractInsnNode> checkcast = returnValueOfMethodCall.instrsWhereUsed.stream()
                                .filter(insn -> insn.getOpcode() == CHECKCAST)
                                .findFirst();
                        if (checkcast.isPresent()) {
                            descOfRootArrayTargetedByStoreOrLoad = ((TypeInsnNode) checkcast.get()).desc;
                        } else {
                            descOfRootArrayTargetedByStoreOrLoad = TransformationUtility.splitMethodDesc(mdesc)[1];
                        }
                        added = true;
                    } else if (potentialArrayInitializer.getOpcode() == MULTIANEWARRAY) {
                        MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) potentialArrayInitializer;
                        // The desc of MultiANewArray is complete to start, we do not need to modify it
                        descOfRootArrayTargetedByStoreOrLoad = mana.desc;
                    } else if (potentialArrayInitializer.getOpcode() == ANEWARRAY) {
                        TypeInsnNode tin = (TypeInsnNode) potentialArrayInitializer;
                        //  ANEWARRAY-desc declares new array with components of tin.desc, hence, add "["
                        if (tin.desc.startsWith("[") || (tin.desc.length() == 1 && primitiveTypes.contains(tin.desc))) {
                            descOfRootArrayTargetedByStoreOrLoad = "[" + tin.desc;
                        } else {
                            descOfRootArrayTargetedByStoreOrLoad = "[L" + tin.desc + ";";
                        }
                        added = true;
                    }
                    if (descOfRootArrayTargetedByStoreOrLoad != null) {
                        added = true;
                        addToArrayArrayOrObjectArrayInsnsDependingOnDesc(ain, descOfRootArrayTargetedByStoreOrLoad, additionalAALOADs);
                        break outerLoop;
                    }
                }
                if (check.instrsWhereUsed.stream().anyMatch(insn -> insn.getOpcode() == AALOAD)) {
                    seenAALOAD = true;
                }
                // We did not break the outer loop, thus, we check the previous elements
                checkForSource.addAll(check.producedBy);
                if (!checkForSource.isEmpty() && checkForSource.get(0) == null) {
                    // Reached next layer
                    checkForSource.add(null);
                }
            }
            if (!added) {
                throw new MulibRuntimeException("Could not decide whether AASTORE targets SarraySarray or PartnerClassSarray.");
            }
        }
    }

    private void addToArrayArrayOrObjectArrayInsnsDependingOnDesc(AbstractInsnNode selectOrStore, String desc, int additionalAALOADs) {
        assert taintedInstructions.contains(selectOrStore);
        String adjustedDesc = desc.substring(additionalAALOADs);
        if (adjustedDesc.startsWith("[[")) {//((isSelect && adjustedDesc.startsWith("[[")) || (!isSelect && adjustedDesc.startsWith("["))) {
            this.taintedNewArrayArrayInsns.add(selectOrStore);
        } else {
            this.taintedNewObjectArrayInsns.add(selectOrStore);
        }
        if (selectOrStore.getOpcode() == AALOAD) {
            String descToLoad;
            if (adjustedDesc.startsWith("[")) {
                descToLoad = adjustedDesc.substring(1); // Remove first [ to get type returned by AALOAD
            } else if (adjustedDesc.startsWith("L") || (adjustedDesc.length() == 1 && primitiveTypes.contains(adjustedDesc))) {
                descToLoad = adjustedDesc;
            } else {
                descToLoad = "L" + adjustedDesc + ";";
            }
            selectedTypeFromSarray.put(
                    selectOrStore,
                    descToLoad
            );
        }
    }

    private void determineNewObjectArrayOrArrayArrayInsnsAndDimensionalityOfSarraySarrays() {
        for (AbstractInsnNode ain : taintedInstructions) {
            if (ain instanceof MethodInsnNode
                    && List.of(freeObject, namedFreeObject).contains(((MethodInsnNode) ain).name)) {
                int arInitMethodIndex = mn.instructions.indexOf(ain);
                Frame<TaintValue> arInitTvFrame = frames[arInitMethodIndex];
                // When executing the method, check how many stack values are popped for it
                // Get the producing instructions of these stack values
                Set<AbstractInsnNode> parameterInsns = new HashSet<>();
                for (int i = 0; i < getNumInputs(((MethodInsnNode) ain).desc, true); i++) {
                    TaintValue arInitTv = getFromTopOfStack(arInitTvFrame, i);
                    parameterInsns.addAll(arInitTv.instrsWhereProduced);
                }
                for (AbstractInsnNode parameterInsn : parameterInsns) {
                    if (parameterInsn instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) parameterInsn;
                        assert ldc.cst instanceof Type || (parameterInsns.size() == 2 && ldc.cst instanceof String);
                        if (ldc.cst instanceof String) {
                            continue;
                        }
                        if (((Type) ldc.cst).getDescriptor().startsWith("[[")) {
                            taintedNewArrayArrayInsns.add(ain);
                        } else {
                            taintedNewObjectArrayInsns.add(ain);
                        }
                    } else if (parameterInsn instanceof VarInsnNode) {
                        assert parameterInsn.getOpcode() == ALOAD : "We expect ALOAD instructions.";
                        LocalVariableNode localVariableNode = varInsnNodesToReferencedLocalVar.get(parameterInsn);
                        if (localVariableNode == null) {
                            throw new NotYetImplementedException();
                        } else if (localVariableNode.desc.equals(stringDesc)) {
                            continue;
                        } else {
                            // The class described by the class must be fully described by parameters
                            // The signature of the LocalVariableNode is like "Ljava/lang/Class<[S>;
                            // Extract the described class.
                            String signature = localVariableNode.signature;
                            String classDesc = signature.substring(17, signature.length() - 2);
                            if (classDesc.startsWith("[")) {
                                taintedNewArrayArrayInsns.add(ain);
                            } else {
                                taintedNewObjectArrayInsns.add(ain);
                            }
                        }
                    } else {
                        throw new NotYetImplementedException();
                    }
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
                    addToWrap(min);
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
        List<LocalVariableNode> oldLocalVariables = mn.localVariables;
        returnsBoolean = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("Z");
        returnsByte = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("B");
        returnsShort = mn.desc.substring(mn.desc.lastIndexOf(')') + 1).equals("S");
        // Loop to determine those instructions that are to be tainted:
        for (AbstractInsnNode ain : taintedInstructions) {
            // These instructions should not be tainted as they do not have any input value
            assert ain.getOpcode() != ICONST_0 && ain.getOpcode() != ICONST_1;
            if (ain.getOpcode() == INSTANCEOF) {
                int indexOfStackValue = mn.instructions.indexOf(ain.getNext());
                Frame<TaintValue> frameOfInstanceofResult = frames[indexOfStackValue];
                TaintValue instanceofResult = getFromTopOfStack(frameOfInstanceofResult);
                assert taintedInstructions.containsAll(instanceofResult.instrsWhereUsed);
                taintedBoolInsns.addAll(instanceofResult.instrsWhereUsed);
            } else if (ain.getOpcode() == ILOAD
                    || ain.getOpcode() == ISTORE
                    || ain instanceof  FieldInsnNode) {
                // We start by looking for boolean, byte, and short local variable nodes
                // Find the type of the local or field variable. If the local or field variable is not of types
                // boolean, byte, or short, continue with the next AbstractInsnNode
                String desc;
                if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                    // Get local variables in scope with the index
                    LocalVariableNode lvn =
                            getLocalVariableInScopeOfInsn(ain);
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
                int indexOfLoaded = mn.instructions.indexOf(
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
                decideOnTaintedOrWrappedForSpecialTypeInsn(
                        taintedSpecialInsns,
                        toWrapSpecialInsns,
                        toAdd
                );
            } else if (ain.getOpcode() == IRETURN && (returnsBoolean || returnsByte || returnsShort)) {
                int indexOfReturn = mn.instructions.indexOf(ain);
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
                int indexOfValue = mn.instructions.indexOf(min.getNext());
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
                Frame<TaintValue> frameOfArrayOp = frames[mn.instructions.indexOf(ain)];
                int offset = ain.getOpcode() == BASTORE ? 2 : 1;
                TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfArrayOp, offset);
                ArrayDeque<TaintValue> checkForSource = new ArrayDeque<>(arrayrefTaintValue.producedBy);
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
                assert added : "Could not decide to which type of array the BASTORE belongs to.";
            }
        }
        // Loop to determine those instructions that are to be wrapped
        for (AbstractInsnNode ain : instructionsToWrap) {
            byte type;
            if (ain.getOpcode() == ILOAD || ain.getOpcode() == ISTORE) {
                LocalVariableNode lvn =
                        getLocalVariableInScopeOfInsn(ain);
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
                int index = mn.instructions.indexOf(ain.getNext());
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
                    Frame<TaintValue> frameOfMethodInsn = frames[mn.instructions.indexOf(anyMethodInsn)];
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
                Frame<TaintValue> frameOfStore = frames[mn.instructions.indexOf(store)];
                // Make sure that we are talking about the value, and not the index here
                if (getFromTopOfStack(frameOfStore) != tvOfIconst) {
                    continue;
                }
                TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfStore, 2);
                ArrayDeque<TaintValue> checkForSource = new ArrayDeque<>(arrayrefTaintValue.producedBy);
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
                assert added : "Could not decide to which type of array the BASTORE belongs to.";
            }
        }

        // We determine which JumpInstructions are using purely boolean values
        Set<AbstractInsnNode> jumpInsns = taintedInstructions.stream()
                .filter(insn -> insn instanceof JumpInsnNode)
                .collect(Collectors.toSet());
        assert instructionsToWrap.stream().noneMatch(insn -> insn instanceof JumpInsnNode);
        for (AbstractInsnNode ain : jumpInsns) {
            int index = mn.instructions.indexOf(ain);
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

    private void newLocalVariableIndexInsns() {
        List<AbstractInsnNode> indexInsns = getIndexInsns(mn.instructions);

        for (AbstractInsnNode indexInsn : indexInsns) {
            LocalVariableNode lvn;
            int indexInsnVarIndex;
            if (indexInsn instanceof VarInsnNode) {
                indexInsnVarIndex = ((VarInsnNode) indexInsn).var;
                lvn = getLocalVariableInScopeOfInsn(indexInsn);
            } else if (indexInsn instanceof IincInsnNode) {
                indexInsnVarIndex = ((IincInsnNode) indexInsn).var;
                lvn = getLocalVariableInScopeOfInsn(indexInsn);
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
