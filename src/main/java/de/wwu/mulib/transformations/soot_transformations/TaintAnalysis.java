package de.wwu.mulib.transformations.soot_transformations;

import soot.MethodSource;
import soot.Unit;
import soot.Value;
import soot.jimple.JimpleBody;

import java.util.Set;

public class TaintAnalysis {

    public final JimpleBody analyzedBody;
    public final MethodSource originalMethodSource;
    public final Set<Value> taintedValues;
    public final Set<Unit> tainted;
    public final Set<Unit> unchangedUnits;
    public final Set<Unit> toWrap;
    public final Set<Unit> concretizeInputs;
    public final Set<Unit> generalizeSignature;

    public TaintAnalysis(
            JimpleBody analyzedBody,
            MethodSource methodSource,
            Set<Value> taintedValues,
            Set<Unit> tainted,
            Set<Unit> toWrap,
            Set<Unit> unchangedUnits,
            Set<Unit> concretizeInputs,
            Set<Unit> generalizeSignature) {
        this.analyzedBody = analyzedBody;
        this.originalMethodSource = methodSource;
        this.taintedValues = taintedValues;
        this.tainted = tainted;
        this.toWrap = toWrap;
        this.unchangedUnits = unchangedUnits;
        this.concretizeInputs = concretizeInputs;
        this.generalizeSignature = generalizeSignature;
    }
}
