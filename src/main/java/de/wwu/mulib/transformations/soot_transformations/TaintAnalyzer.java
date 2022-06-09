package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.transformations.AbstractMulibTransformer;
import soot.*;
import soot.jimple.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TaintAnalyzer {

    private final MulibConfig config;
    private final AbstractMulibTransformer<SootClass> mulibTransformer;
    private final SootClass owner;
    private final SootMethod originalSootMethod;
    private final JimpleBody originalJimpleBody;
    private final MethodSource originalMethodSource;
    private final UnitPatchingChain upc;
    private final Set<Value> values;

    private final Set<Unit> tainted = new HashSet<>();
    private final Set<Unit> toWrap = new HashSet<>();

    private final Set<Value> taintedValues = new HashSet<>();

    private final Set<Unit> concretizeInputs = new HashSet<>();
    private final Set<Unit> generalizeSignature = new HashSet<>();

    private final SootMulibClassesAndMethods sootMulibClassesAndMethods;

    public TaintAnalyzer(
            MulibConfig config,
            AbstractMulibTransformer<SootClass> mulibTransformer,
            SootMethod originalSootMethod,
            SootMulibClassesAndMethods sootMulibClassesAndMethods) {
        this.config = config;
        this.mulibTransformer = mulibTransformer;
        this.originalSootMethod = originalSootMethod;
        this.originalMethodSource = originalSootMethod.getSource();
        this.owner = originalSootMethod.getDeclaringClass();
        this.originalJimpleBody = (JimpleBody) originalSootMethod.retrieveActiveBody();
        this.originalSootMethod.setActiveBody(originalJimpleBody);
        this.upc = originalJimpleBody.getUnits();
        this.values = new HashSet<>();
        for (Unit u : upc) {
            values.addAll(getValuesOfBoxes(u.getUseAndDefBoxes()));
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
        // Add units returning a value; - we assume that the return value is always tainted
        List<Unit> returnUnits = upc.stream().filter(u -> u instanceof ReturnStmt).collect(Collectors.toList());
        for (Unit u : returnUnits) {
            ReturnStmt returnStmt = (ReturnStmt) u;
            if (!(returnStmt.getOp().getType() instanceof RefType)) {
                // Objects do not have to be wrapped
                addToWrap(u);
            }
        }
        // Add all fieldrefs to the set of tainted values. Furthermore, if a class in, e.g., a cast is to be replaced,
        // it should also be tainted here.
        for (Value v : values) {
            if (v instanceof FieldRef) {
                if (mulibTransformer.shouldBeTransformed(((FieldRef) v).getField().getDeclaringClass().getName().replace(".", "/"))) {
                    taintedValues.add(v);
                }
            } else if (v instanceof CastExpr) {
                Type castToType = v.getType();
                if (castToType instanceof RefType
                        && mulibTransformer.shouldBeTransformed(((RefType) castToType).getClassName())) {
                    taintedValues.add(v);
                }
            } else if (v instanceof ClassConstant) {
                ClassConstant classConstant = ((ClassConstant) v);
                if (classConstant.getType() instanceof RefType
                        && mulibTransformer.shouldBeTransformed(((RefType) classConstant.getType()).getClassName())) {
                    taintedValues.add(v);
                }
            } else if (v instanceof NewExpr) {
                NewExpr newExpr = (NewExpr) v;
                if (mulibTransformer.shouldBeTransformed(newExpr.getBaseType().getClassName())) {
                    taintedValues.add(v);
                }
            } else if (v instanceof MethodHandle || v instanceof MethodType) {
                throw new NotYetImplementedException();
            }
        }


        // Taint methods statically (i.e. methods won't be tainted in the subsequent main loop)
        List<Unit> methodCallsToPotentiallyTaint = upc.stream().filter(this::isMethodCallUnit).collect(Collectors.toList());
        // Add taint of special Mulib-indicator methods
        for (Unit u : methodCallsToPotentiallyTaint) {
            if (isSpecialMulibIndicatorMethod(u)) {
                addTainted(u);
            } else if (isToTransformMethodCallUnit(u)) {
                InvokeExpr invokeExpr;
                if (u instanceof AssignStmt) {
                    invokeExpr = ((AssignStmt) u).getInvokeExpr();
                } else {
                    assert u instanceof InvokeStmt;
                    invokeExpr = ((InvokeStmt) u).getInvokeExpr();
                }
                taintedValues.add(invokeExpr);
                addTainted(u);
            } else {
                String declaringClassName = getNameOfDeclaringClassOfMethodCallUnit(u);
                if (config.TRANSF_CONCRETIZE_FOR.stream()
                        .anyMatch(c -> c.getName().equals(declaringClassName))) {
                    concretizeInputs.add(u);
                } else if (config.TRANSF_TRY_USE_MORE_GENERAL_METHOD_FOR.stream()
                        .anyMatch(c -> c.getName().equals(declaringClassName))) {
                    generalizeSignature.add(u);
                } else if (!declaringClassName.equals(Object.class.getName())
                        && !declaringClassName.equals(Exception.class.getName())
                        && !declaringClassName.equals(RuntimeException.class.getName())
                        && !declaringClassName.equals(Mulib.class.getName())
                        && !declaringClassName.equals(String.class.getName())) { // TODO Free Strings
                    Mulib.log.log(Level.WARNING, "Behavior for treating untransformed method in unit " + u + " is not " +
                            "defined. The generalized signature is used as a default");
                    generalizeSignature.add(u);
                }
            }
        }

        // Main loop: Loop until no unit need to be tainted anymore
        int iterations = 0;
        boolean changed = true;
        while (changed) {
            iterations++;
            changed = false;
            for (Unit u : upc) {
                changed = decideOnWrapUnit(u) || changed;
                changed = decideOnTaintUnit(u) || changed;
            }
            // Regard values consisting of other values
            for (Value v : values) {
                if (containsTaintedValueBoxes(v.getUseBoxes())) {
                    changed = addValueToTainted(v) || changed;
                }
            }
        }

        // To avoid ambiguities, keep toWrap and tainted disjoint. Furthermore, explicitly add those units that are
        // neither tainted nor to-be-wrapped.
        toWrap.removeAll(tainted);
        Set<Unit> unchangedUnits = new HashSet<>(upc);
        unchangedUnits.removeAll(tainted);
        unchangedUnits.removeAll(toWrap);
        // Assert consistent TaintAnalysis results
        assert concretizeInputs.stream().allMatch(c -> unchangedUnits.contains(c) || toWrap.contains(c));
        assert generalizeSignature.stream().allMatch(c -> unchangedUnits.contains(c) || toWrap.contains(c));
        assert toWrap.stream().allMatch(tw -> taintedValues.containsAll(getValuesOfBoxes(tw.getDefBoxes())));
        assert tainted.stream().allMatch(t -> taintedValues.containsAll(getValuesOfBoxes(t.getDefBoxes())));
        // If there is one tainted value defined by u in UPC, all values defined by u must be tainted:
        assert upc.stream().allMatch(u -> {
            Collection<Value> valuesOfUnit = getValuesOfBoxes(u.getDefBoxes());
            if (taintedValues.stream().anyMatch(valuesOfUnit::contains)) {
                return taintedValues.containsAll(valuesOfUnit);
            } else {
                return true;
            }
        }) : "Erroneous tainting for: " + originalSootMethod.getName() + ". All values defined by the tainted instruction should be tainted";
        long endTime = System.nanoTime();
        Mulib.log.log(Level.INFO, "Duration of taint analysis: " + ((endTime - startTime) / 1e6) + "ms with " + iterations + " iterations");
        return new TaintAnalysis(originalJimpleBody, originalMethodSource, taintedValues, tainted, toWrap, unchangedUnits,
                concretizeInputs, generalizeSignature);
    }
    
    private Collection<Value> getValuesOfBoxes(List<? extends ValueBox> vbs) {
        return vbs.stream().map(ValueBox::getValue).collect(Collectors.toList());
    }

    private boolean containsTaintedValueBoxes(List<ValueBox> vbs) {
        return getValuesOfBoxes(vbs).stream().anyMatch(taintedValues::contains);
    }

    private boolean unitShouldBeTainted(Unit u) {
        return !unitIsTainted(u)
                && containsTaintedValueBoxes(u.getUseBoxes())
                && !isMethodCallUnit(u);
    }

    private boolean unitIsTainted(Unit u) {
        return tainted.contains(u);
    }

    private boolean unitIsWrapped(Unit u) {
        return toWrap.contains(u);
    }

    private boolean decideOnTaintUnit(Unit u) {
        if (unitShouldBeTainted(u)) {
            return addTainted(u);
        }
        return false;
    }

    private boolean unitShouldBeWrapped(Unit u) {
        return !unitIsTainted(u)
                && !unitIsWrapped(u)
                && !containsTaintedValueBoxes(u.getUseBoxes()) // Should then be tainted instead
                && containsTaintedValueBoxes(u.getDefBoxes());
    }

    private boolean decideOnWrapUnit(Unit u) {
        if (unitShouldBeWrapped(u)) {
            return addToWrap(u);
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
        if (v instanceof InvokeExpr) {
            InvokeExpr invokeExpr = (InvokeExpr) v;
            SootClass declaringClass = invokeExpr.getMethod().getDeclaringClass();
            if (!mulibTransformer.shouldBeTransformed(declaringClass.getName())) {
                return false;
            }
        }
        return taintedValues.add(v);
    }

    private boolean addTainted(Unit u) {
        // All result values must be tainted!
        addValuesOfBoxesToTainted(u.getDefBoxes());
        return tainted.add(u);
    }

    private boolean addToWrap(Unit u) {
        if (unitIsTainted(u)) {
            return false;
        }
        // All result values must be tainted!
        addValuesOfBoxesToTainted(u.getDefBoxes());
        if (u instanceof IdentityStmt) {
            // IdentityStmts that are wrapped must also account for used variables
            addValuesOfBoxesToTainted(u.getUseBoxes());
        }
        return toWrap.add(u);
    }

    private boolean isMethodCallUnit(Unit u) {
        return ((u instanceof InvokeStmt)
                || (u instanceof AssignStmt && ((AssignStmt) u).containsInvokeExpr()));
    }

    private boolean isToTransformMethodCallUnit(Unit u) {
        return ((u instanceof InvokeStmt && mulibTransformer.shouldBeTransformed(((InvokeStmt) u).getInvokeExpr().getMethodRef().getDeclaringClass().getName()))
                || (u instanceof AssignStmt && ((AssignStmt) u).containsInvokeExpr()
                && mulibTransformer.shouldBeTransformed(((AssignStmt) u).getInvokeExpr().getMethodRef().getDeclaringClass().getName())));
    }

    private boolean isSpecialMulibIndicatorMethod(Unit u) {
        String declaringClassName = getNameOfDeclaringClassOfMethodCallUnit(u);
        if (!declaringClassName.equals(Mulib.class.getName())) {
            return false;
        }
        String methodName;
        if (u instanceof AssignStmt) {
            methodName = ((AssignStmt) u).getInvokeExpr().getMethod().getName();
        } else if (u instanceof InvokeStmt) {
            methodName = ((InvokeStmt) u).getInvokeExpr().getMethodRef().getName();
        } else {
            throw new NotYetImplementedException(u.toString());
        }

        return sootMulibClassesAndMethods.isIndicatorMethodName(methodName);
    }

    private String getNameOfDeclaringClassOfMethodCallUnit(Unit u) {
        if (u instanceof InvokeStmt) {
            return ((InvokeStmt) u).getInvokeExpr().getMethodRef().getDeclaringClass().getName();
        } else if (u instanceof AssignStmt && ((AssignStmt) u).containsInvokeExpr()) {
            return ((AssignStmt) u).getInvokeExpr().getMethodRef().getDeclaringClass().getName();
        } else {
            throw new NotYetImplementedException(u.toString());
        }
    }
}
