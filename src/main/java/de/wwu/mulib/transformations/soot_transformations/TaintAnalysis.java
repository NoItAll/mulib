package de.wwu.mulib.transformations.soot_transformations;

import soot.MethodSource;
import soot.Type;
import soot.Value;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;

import java.util.Map;
import java.util.Set;

public class TaintAnalysis {

    public final JimpleBody analyzedBody;
    public final MethodSource originalMethodSource;
    public final Set<Value> taintedValues;
    public final Set<Stmt> tainted;
    public final Set<Stmt> unchangedUnits;
    public final Set<Stmt> toWrap;
    public final Set<Stmt> concretizeInputs;
    public final Set<Stmt> generalizeSignature;
    public final Map<Value, Type> valueHolderToClassConstantType;

    public TaintAnalysis(
            JimpleBody analyzedBody,
            MethodSource methodSource,
            Set<Value> taintedValues,
            Set<Stmt> tainted,
            Set<Stmt> toWrap,
            Set<Stmt> unchangedUnits,
            Set<Stmt> concretizeInputs,
            Set<Stmt> generalizeSignature,
            Map<Value, Type> valueHolderToClassConstantType) {
        this.analyzedBody = analyzedBody;
        this.originalMethodSource = methodSource;
        this.taintedValues = taintedValues;
        this.tainted = tainted;
        this.toWrap = toWrap;
        this.unchangedUnits = unchangedUnits;
        this.concretizeInputs = concretizeInputs;
        this.generalizeSignature = generalizeSignature;
        this.valueHolderToClassConstantType = valueHolderToClassConstantType;
    }
}
