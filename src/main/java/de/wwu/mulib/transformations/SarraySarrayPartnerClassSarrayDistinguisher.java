package de.wwu.mulib.transformations;

import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

import static de.wwu.mulib.transformations.StringConstants.*;
import static de.wwu.mulib.transformations.TaintAnalyzer.getFromTopOfStack;
import static de.wwu.mulib.transformations.TransformationUtility.getNumInputs;
import static de.wwu.mulib.transformations.TransformationUtility.splitMethodDesc;
import static org.objectweb.asm.Opcodes.*;

// Needed to distinguish between (AALOAD, AASTORE) --> ((SarraySarray.select, PartnerClassSarray.select), (SarraySarray.store, PartnerClassSarray.store))
// and which class to initialize
public class SarraySarrayPartnerClassSarrayDistinguisher {
    /* INPUT */
    private final MethodNode oldMethodNode;
    private final Set<AbstractInsnNode> taintedInstructions;
    private final Set<AbstractInsnNode> instructionsToWrap;
    private final Frame<TaintValue>[] frames;
    private final Map<AbstractInsnNode, LocalVariableNode> varInsnNodesToReferencedLocalVar;

    /* OUTPUT */
    private final Set<AbstractInsnNode> taintedNewObjectArrayInsns = new HashSet<>();
    private final Set<AbstractInsnNode> taintedNewArrayArrayInsns = new HashSet<>();
    // For PartnerClassSarrays and SarraySarrays we need to determine the selected type so that we can cast to it
    private final Map<AbstractInsnNode, String> selectedTypeFromSarray = new HashMap<>();

    public SarraySarrayPartnerClassSarrayDistinguisher(
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
        // Add special method-instruction, Mulib.freeArray etc., that initialize an object array or an array array to a designated map
        // This is done to differentiate at transformation time since both types of arrays are indicated by the same method
        determineNewObjectArrayOrArrayArrayInsnsAndDimensionalityOfSarraySarrays();
        // Differentiate Sarray.PartnerClassSarray and Sarray.SarraySarray for XASELECT and XASTORE
        determineWhetherTaintedPartnerClassSarrayOrSarraySarrayInsnAndSelectedType();
    }

    private void determineNewObjectArrayOrArrayArrayInsnsAndDimensionalityOfSarraySarrays() {
        for (AbstractInsnNode ain : taintedInstructions) {
            if (ain instanceof MethodInsnNode
                    && List.of(freeObject, namedFreeObject).contains(((MethodInsnNode) ain).name)) {
                int arInitMethodIndex = oldMethodNode.instructions.indexOf(ain);
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

    private static class ProductionPathNode {
        int numberAALOADsOnPath;
        final AbstractInsnNode lastSeenProducingInsn;
        final TaintValue lastSeenProducedTv;
        final ProductionPathNode previous;
        private ProductionPathNode(int numberAALOADsOnPath,
                                   AbstractInsnNode lastSeenProducingInsn,
                                   TaintValue lastSeenProducedTv,
                                   ProductionPathNode previous) {
            this.numberAALOADsOnPath = numberAALOADsOnPath;
            this.lastSeenProducingInsn = lastSeenProducingInsn;
            this.lastSeenProducedTv = lastSeenProducedTv;
            this.previous = previous;
        }

        @Override
        public String toString() {
            Stack<AbstractInsnNode> path = new Stack<>();
            ProductionPathNode current = this;
            while (current != null) {
                path.push(current.lastSeenProducingInsn);
                current = current.previous;
            }
            return TransformationUtility.getBytecode(path);
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
            Frame<TaintValue> frameOfInsn = frames[oldMethodNode.instructions.indexOf(in)];
            TaintValue arrayrefTaintValue = getFromTopOfStack(frameOfInsn, stackOffsetToArrayref);


            List<ProductionPathNode> checkForSource = new ArrayList<>();
            for (AbstractInsnNode insn : arrayrefTaintValue.instrsWhereProduced) {
                checkForSource.add(new ProductionPathNode(0, insn, arrayrefTaintValue, null));
            }
            // This way we can determine the desc of the array which is to be returned
            String descOfRootArrayTargetedByStoreOrLoad = null;
            while (!checkForSource.isEmpty()) { //// TODO ASTORE
                ProductionPathNode check = checkForSource.remove(0);
                AbstractInsnNode potentialArrayInitializer = check.lastSeenProducingInsn;
                if (potentialArrayInitializer instanceof MethodInsnNode) {
                    /// TODO GETFIELD, GETSTATIC?
                    String mdesc = ((MethodInsnNode) potentialArrayInitializer).desc;
                    // Check if result is casted
                    Frame<TaintValue> frameOfResultValueOfPotentialArrayInitializer
                            = frames[oldMethodNode.instructions.indexOf(potentialArrayInitializer.getNext())];
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
                } else if (potentialArrayInitializer.getOpcode() == ALOAD) {
                    LocalVariableNode lvn = this.varInsnNodesToReferencedLocalVar.get(potentialArrayInitializer);
                    if (lvn != null) {
                        descOfRootArrayTargetedByStoreOrLoad = lvn.desc;
                    }
                } else if (potentialArrayInitializer.getOpcode() == AALOAD) {
                    check.numberAALOADsOnPath++;
                }

                if (descOfRootArrayTargetedByStoreOrLoad != null) {
                    addToArrayArrayOrObjectArrayInsnsDependingOnDesc(ain, descOfRootArrayTargetedByStoreOrLoad, check.numberAALOADsOnPath);
                    break;
                }
                for (TaintValue producingTv : check.lastSeenProducedTv.producedBy) {
                    for (AbstractInsnNode insn : producingTv.instrsWhereProduced) {
                        // If we were not able to find a desc which we can use to determine the type of operation, do this here
                        checkForSource.add(new ProductionPathNode(check.numberAALOADsOnPath, insn, producingTv, check));
                    }
                }
            }

            if (descOfRootArrayTargetedByStoreOrLoad == null) {
                throw new MulibRuntimeException("Could not decide whether AA{STORE, LOAD} targets SarraySarray or PartnerClassSarray.");
            }
        }

    }

    private void addToArrayArrayOrObjectArrayInsnsDependingOnDesc(AbstractInsnNode selectOrStore, String desc, int additionalAALOADs) {
        assert taintedInstructions.contains(selectOrStore);
        String adjustedDesc = desc.substring(additionalAALOADs);
        if (adjustedDesc.startsWith("[[")) {
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

    public Set<AbstractInsnNode> getTaintedNewObjectArrayInsns() {
        return taintedNewObjectArrayInsns;
    }

    public Set<AbstractInsnNode> getTaintedNewArrayArrayInsns() {
        return taintedNewArrayArrayInsns;
    }

    public Map<AbstractInsnNode, String> getSelectedTypeFromSarray() {
        return selectedTypeFromSarray;
    }
}
