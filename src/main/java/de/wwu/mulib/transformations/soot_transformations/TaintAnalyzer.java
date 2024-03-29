package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.throwables.NotYetImplementedException;
import soot.*;
import soot.jimple.*;

import java.lang.invoke.StringConcatFactory;
import java.util.*;
import java.util.stream.Collectors;

public class TaintAnalyzer {

    private final MulibConfig config;
    private final SootMulibTransformer mulibTransformer;
    private final SootClass owner;
    private final SootMethod originalSootMethod;
    private final JimpleBody originalJimpleBody;
    private final MethodSource originalMethodSource;
    private final UnitPatchingChain upc;
    private final Set<Value> values;

    private final Set<Stmt> tainted = new HashSet<>();
    private final Set<Stmt> toWrap = new HashSet<>();

    private final Set<Value> taintedValues = new HashSet<>();

    private final Set<Stmt> concretizeInputs = new HashSet<>();
    private final Set<Stmt> generalizeSignature = new HashSet<>();

    // Store the type of a class constants of a local so that we can potentially distinguish what they were assigned to
    // FieldRef, ArrayRef or Local -> Type
    private final Map<Value, Type> valueHolderToClassConstantType = new HashMap<>();
    private final SootMulibClassesAndMethods sootMulibClassesAndMethods;

    public TaintAnalyzer(
            MulibConfig config,
            SootMulibTransformer mulibTransformer,
            SootMethod originalSootMethod,
            SootMulibClassesAndMethods sootMulibClassesAndMethods) {
        this.config = config;
        this.mulibTransformer = mulibTransformer;
        this.originalSootMethod = originalSootMethod;
        this.originalMethodSource = originalSootMethod.getSource();
        this.owner = originalSootMethod.getDeclaringClass();
        this.originalJimpleBody = (JimpleBody) originalSootMethod.retrieveActiveBody();
        this.upc = originalJimpleBody.getUnits();
        this.values = new HashSet<>();
        // Gather all values
        for (Unit u : upc) {
            values.addAll(getValuesOfBoxes(u.getUseAndDefBoxes()));
            if (u instanceof AssignStmt) {
                Value assignedTo = ((AssignStmt) u).getLeftOp();
                Value rhs = ((AssignStmt) u).getRightOp();
                if ((assignedTo instanceof Local || assignedTo instanceof FieldRef || assignedTo instanceof ArrayRef)
                    && rhs instanceof ClassConstant) {
                    valueHolderToClassConstantType.put(assignedTo, ((ClassConstant) rhs).toSootType());
                }
            }
        }
        this.sootMulibClassesAndMethods = sootMulibClassesAndMethods;
    }

    public TaintAnalysis analyze() {
        long startTime = System.nanoTime();
        /* ADD INITIAL TAINT */
        // Get parameter values (does NOT contain 'this' of the method)
        List<Local> parameterLocals = originalJimpleBody.getParameterLocals();
        addValuesToTainted(parameterLocals);
        assert parameterLocals.stream().allMatch(p -> p.getUseBoxes().isEmpty());
        // Add this
        if (!originalSootMethod.isStatic()) {
            addValueToTainted(originalJimpleBody.getThisLocal());
            addValuesOfBoxesToTainted(originalJimpleBody.getThisLocal().getUseBoxes());
        }
        // Add Stmts returning a value; - we assume that the return value is always tainted
        List<Stmt> returnStmts = upc.stream().filter(u -> u instanceof ReturnStmt).map(Stmt.class::cast).collect(Collectors.toList());
        for (Stmt u : returnStmts) {
            ReturnStmt returnStmt = (ReturnStmt) u;
            if (returnStmt.getOp().getType() instanceof RefType) {
                // Objects do not have to be wrapped
                continue;
            }
            if (returnStmt.getOp().getType() instanceof ArrayType) {
                addTainted(u);
                continue;
            }
            // Otherwise, add to wrap (at least)
            addToWrap(u);
        }
        // Add all fieldrefs to the set of tainted values
        for (Value v : values) {
            if (v instanceof FieldRef) {
                if (!mulibTransformer.shouldBeTransformed(((FieldRef) v).getFieldRef().declaringClass().getName())) {
                    continue;
                }
                Type t = ((FieldRef) v).getField().getType();
                if (!(t instanceof RefType)) {
                    // Ref types can be statically replaced
                    taintedValues.add(v);
                }
            } else if (v instanceof MethodHandle || v instanceof MethodType) {
                throw new NotYetImplementedException();
            }
        }

        if (config.TRANSF_CFG_GENERATE_CHOICE_POINTS_WITH_ID) {
            // If we use the CFG graph, each if-stmt must be transformed
            for (Unit u : upc) {
                Stmt s = (Stmt) u;
                if (s instanceof IfStmt) {
                    addTainted(s);
                }
            }
        }

        // Taint methods statically (i.e. methods won't be tainted in the subsequent main loop)
        List<Stmt> methodCallsToPotentiallyTaint = upc.stream().filter(u -> this.isMethodCallStmt((Stmt) u)).map(Stmt.class::cast).collect(Collectors.toList());
        // Add taint of special Mulib-indicator methods
        for (Stmt s : methodCallsToPotentiallyTaint) {
            if (isSpecialMulibIndicatorMethod(s)) {
                addTainted(s);
            } else if (isToTransformMethodCallStmt(s)) {
                InvokeExpr invokeExpr;
                if (s instanceof AssignStmt) {
                    invokeExpr = s.getInvokeExpr();
                } else {
                    assert s instanceof InvokeStmt;
                    invokeExpr = s.getInvokeExpr();
                }
                // We must taint inputs that are arrays. These might be altered in the method. Hence, we cannot wrap arrays
                // that are used as inputs for tainted methods
                addValuesToTainted(invokeExpr.getArgs().stream().filter(arg -> arg.getType() instanceof ArrayType).collect(Collectors.toSet()));
                taintedValues.add(invokeExpr);
                addTainted(s);
            } else {
                String declaringClassName = s.getInvokeExpr().getMethodRef().getDeclaringClass().getName();
                if (config.TRANSF_CONCRETIZE_FOR.stream()
                        .anyMatch(c -> c.getName().equals(declaringClassName))) {
                    concretizeInputs.add(s);
                } else if (config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR.stream()
                        .anyMatch(c -> c.getName().equals(declaringClassName))) {
                    generalizeSignature.add(s);
                } else if (!declaringClassName.equals(Object.class.getName())
                        && !declaringClassName.equals(Exception.class.getName())
                        && !declaringClassName.equals(RuntimeException.class.getName())
                        && !declaringClassName.startsWith("de.wwu.mulib")
                        && !declaringClassName.equals(String.class.getName())
                        && !declaringClassName.equals(StringConcatFactory.class.getName())
                        && s.getInvokeExpr().getMethodRef().getParameterTypes().size() > 0) { // TODO Free Strings
                    Mulib.log.info("Behavior for treating untransformed method in Stmt " + s + " is not " +
                            "defined. The inputs for this method will be concretized as a default, if any " +
                            "tainted inputs are involved");
                    concretizeInputs.add(s);
                }
            }
        }
        // Main loop: Loop until no Stmt need to be tainted anymore
        int iterations = 0;
        boolean changed = true;
        while (changed) {
            iterations++;
            changed = false;
            for (Unit u : upc) {
                Stmt stmt = (Stmt) u;
                changed = decideOnWrapStmt(stmt) || changed;
                changed = decideOnTaintStmt(stmt) || changed;
            }
            // Regard values consisting of other values
            for (Value v : values) {
                if (containsTaintedValueBoxes(v.getUseBoxes())) {
                    changed = addValueToTainted(v) || changed;
                }
            }
        }

        // To avoid ambiguities, keep toWrap and tainted disjoint. Furthermore, explicitly add those Stmts that are
        // neither tainted nor to-be-wrapped.
        toWrap.removeAll(tainted);
        @SuppressWarnings("unchecked")
        Set<Stmt> unchangedStmts = (Set<Stmt>) (Object) new HashSet<>(upc);
        unchangedStmts.removeAll(tainted);
        unchangedStmts.removeAll(toWrap);
        // Assert consistent TaintAnalysis results
        assert concretizeInputs.stream().allMatch(c -> unchangedStmts.contains(c) || toWrap.contains(c));
        assert generalizeSignature.stream().allMatch(c -> unchangedStmts.contains(c) || toWrap.contains(c));
        assert toWrap.stream().allMatch(tw -> taintedValues.containsAll(getValuesOfBoxes(tw.getDefBoxes())));
        assert tainted.stream().allMatch(t -> {
            return getValuesOfBoxes(t.getDefBoxes()).stream().allMatch(
                    v -> taintedValues.contains(v)
                            || v.getType() instanceof RefType);
        });
        // RefTypes can also be tainted. Arrays in Soot are not RefTypes but RefLikeTypes, however, to carry
        // the taint of, e.g., (int[]) Mulib.freeObject(int[].class) to the array, the result of
        // Mulib.freeObject(int[].class) must be tainted as well

        // If there is one tainted value defined by u in UPC, all values defined by u must be tainted:
        assert upc.stream().allMatch(u -> {
            Collection<Value> valuesOfStmt = getValuesOfBoxes(u.getDefBoxes());
            if (taintedValues.stream().anyMatch(valuesOfStmt::contains)) {
                return taintedValues.containsAll(valuesOfStmt);
            } else {
                return true;
            }
        }) : "Erroneous tainting for: " + originalSootMethod.getName() + ". All values defined by the tainted instruction should be tainted";
        assert taintedValues.stream()
                .filter(tv -> tv instanceof ArrayRef).map(ArrayRef.class::cast)
                .noneMatch(ar -> ar.getType() instanceof RefType && mulibTransformer.shouldBeTransformed(((RefType) ar.getType()).getClassName()));
        long endTime = System.nanoTime();
        Mulib.log.finest("Duration of taint analysis: " + ((endTime - startTime) / 1e6) + "ms with " + iterations + " iterations");
        return new TaintAnalysis(originalJimpleBody, originalMethodSource, taintedValues,
                tainted, toWrap, unchangedStmts, concretizeInputs, generalizeSignature, valueHolderToClassConstantType);
    }
    
    private List<Value> getValuesOfBoxes(List<? extends ValueBox> vbs) {
        return vbs.stream().map(ValueBox::getValue).collect(Collectors.toList());
    }

    private boolean containsTaintedValueBoxes(List<ValueBox> vbs) {
        return getValuesOfBoxes(vbs).stream().anyMatch(taintedValues::contains);
    }

    private boolean stmtShouldBeTainted(Stmt s) {
        if (stmtIsTainted(s) || isMethodCallStmt(s)) {
            return false;
        }
        if (isFieldStmtWithArrayType(s)) {
            return true;
        }
        if (s instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) s;
            if (a.getLeftOp().getType() instanceof ArrayType) {
                return containsTaintedValueBoxes(a.getUseAndDefBoxes());
            }
        }
        return containsTaintedValueBoxes(s.getUseBoxes());
    }

    private boolean stmtIsTainted(Stmt u) {
        return tainted.contains(u);
    }

    private boolean stmtIsWrapped(Stmt s) {
        return toWrap.contains(s);
    }

    private boolean decideOnTaintStmt(Stmt s) {
        if (stmtShouldBeTainted(s)) {
            return addTainted(s);
        }
        return false;
    }

    private boolean stmtShouldBeWrapped(Stmt s) {
        return !stmtIsTainted(s)
                && !stmtIsWrapped(s)
                // If the statement is an InvokeStmt that should be generalized or concretized, it can still be wrapped.
                // If the statement uses inputs that are tainted, it should be tainted instead
                && (generalizeSignature.contains(s) || concretizeInputs.contains(s)
                    || !containsTaintedValueBoxes(s.getUseBoxes()))
                && containsTaintedValueBoxes(s.getDefBoxes())
                // Regard array cases:
                // If an array is loaded from a field or stored in a field, it must not be wrapped
                && !isFieldStmtWithArrayType(s)
                && s instanceof AssignStmt
                // If AssignStmt does not define an array, it can be wrapped
                && s.getDefBoxes().stream().noneMatch(vb -> vb.getValue().getType() instanceof ArrayType)
                ;
    }

    private boolean isFieldStmtWithArrayType(Stmt s) {
        return s.containsFieldRef()
                && mulibTransformer.shouldBeTransformed(s.getFieldRef().getFieldRef().declaringClass().getName())
                && s.getFieldRef().getType() instanceof ArrayType;
    }

    private boolean decideOnWrapStmt(Stmt s) {
        if (stmtShouldBeWrapped(s)) {
            return addToWrap(s);
        }
        return false;
    }

    private boolean addValuesOfBoxesToTainted(List<? extends ValueBox> vbs) {
        return addValuesToTainted(getValuesOfBoxes(vbs));
    }

    private boolean addValuesToTainted(Collection<? extends Value> vs) {
        return vs.stream().map(this::addValueToTainted).reduce(false, (b0, b1) -> b0 || b1);
    }

    private boolean addValueToTainted(Value v) {
        if (v instanceof ArrayRef) {
            boolean isTainted = false;
            if (taintedValues.contains(((ArrayRef) v).getIndex())) {
                isTainted = taintedValues.add(((ArrayRef) v).getBase());
            }
            if (!(v.getType() instanceof RefType)) {
                isTainted = taintedValues.add(v) || isTainted;
            }
            return isTainted;
        }
        if (v.getType() instanceof RefType) {
            return false;
        }
        if (v instanceof InvokeExpr) {
            InvokeExpr invokeExpr = (InvokeExpr) v;
            SootClass declaringClass = invokeExpr.getMethod().getDeclaringClass();
            if (!mulibTransformer.shouldBeTransformed(declaringClass.getName())) {
                return false;
            }
        }
        return taintedValues.add(v);
    }

    private boolean isRhsArrayTypeStmt(AssignStmt u) {
        if (u.getRightOp().getType() instanceof ArrayType) {
            return true;
        }
        if (u.getRightOp() instanceof InvokeExpr) {
            InvokeExpr ie = (InvokeExpr) u.getRightOp();
            if (!ie.getMethodRef().equals(sootMulibClassesAndMethods.SM_MULIB_FREE_OBJECT.makeRef())
                    && !ie.getMethodRef().equals(sootMulibClassesAndMethods.SM_MULIB_NAMED_FREE_OBJECT.makeRef())) {
                return false;
            }
            Value potentiallyUsedClassConstant;
            if (SootMulibClassesAndMethods.methodNameImpliesRememberedInitialization(ie.getMethodRef().getName())) {
                potentiallyUsedClassConstant = ie.getArg(1);
            } else {
                potentiallyUsedClassConstant = ie.getArg(0);
            }
            Type typeOfClass = potentiallyUsedClassConstant instanceof ClassConstant ?
                    ((ClassConstant) potentiallyUsedClassConstant).toSootType()
                    :
                    valueHolderToClassConstantType.get(potentiallyUsedClassConstant);
            return typeOfClass instanceof ArrayType;
        }
        return false;
    }

    private boolean addTainted(Stmt u) {
        if (u instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) u;
            if (isRhsArrayTypeStmt(a)) {
                // If the assign statement is tainted, then the array must be tainted as well
                taintedValues.add(a.getRightOp());
                taintedValues.add(a.getLeftOp());
                // TODO We usually want to avoid directly adding to the taintedValues field
                if (a.getRightOp() instanceof ArrayRef) {
                    addValueToTainted(((ArrayRef) a.getRightOp()).getBase());
                }
            }
            if (a.getLeftOp() instanceof ArrayRef) {
                addValueToTainted(((ArrayRef) a.getLeftOp()).getBase());
            }
        } else if (u instanceof ReturnStmt) {
            if (((ReturnStmt) u).getOp().getType() instanceof ArrayType) {
                addValueToTainted(((ReturnStmt) u).getOp());
            }
        }
        // All result values must be tainted!
        addValuesOfBoxesToTainted(u.getDefBoxes());
        return tainted.add(u);
    }

    private boolean addToWrap(Stmt u) {
        // All result values must be tainted!
        addValuesOfBoxesToTainted(u.getDefBoxes());
        if (u instanceof IdentityStmt) {
            // IdentityStmts that are wrapped must also account for used variables
            addValuesOfBoxesToTainted(u.getUseBoxes());
        }
        return toWrap.add(u);
    }

    private boolean valueIsOfArrays(Value v) {
        return v instanceof NewArrayExpr
                || v instanceof NewMultiArrayExpr
                || v instanceof ArrayRef;
    }

    private boolean isMethodCallStmt(Stmt u) {
        return ((u instanceof InvokeStmt)
                || (u instanceof AssignStmt && ((AssignStmt) u).containsInvokeExpr()));
    }

    private boolean isToTransformMethodCallStmt(Stmt u) { // TODO Invokedynamic and static bootstrap methods
        return ((u instanceof InvokeStmt && mulibTransformer.shouldBeTransformed(u.getInvokeExpr().getMethodRef().getDeclaringClass().getName()))
                || (u instanceof AssignStmt && u.containsInvokeExpr()
                    && ((u.getInvokeExpr() instanceof DynamicInvokeExpr && mulibTransformer.shouldBeTransformed(((DynamicInvokeExpr) u.getInvokeExpr()).getBootstrapMethodRef().getDeclaringClass().getName()))
                        || (!(u.getInvokeExpr() instanceof DynamicInvokeExpr) && mulibTransformer.shouldBeTransformed(u.getInvokeExpr().getMethodRef().getDeclaringClass().getName())))));
    }

    private boolean isSpecialMulibIndicatorMethod(Stmt s) {
        String declaringClassName = s.getInvokeExpr().getMethodRef().getDeclaringClass().getName();
        if (!declaringClassName.equals(Mulib.class.getName())) {
            return false;
        }
        String methodName = s.getInvokeExpr().getMethod().getName();
        return sootMulibClassesAndMethods.isIndicatorMethodName(methodName);
    }
}
