package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayConstraint;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueCopier;
import de.wwu.mulib.transformations.MulibValueLabeler;

import java.util.*;

@SuppressWarnings({"unused", "rawtypes"})
public final class SymbolicExecution {
    private final static ThreadLocal<SymbolicExecution> se = new ThreadLocal<>();
    private final MulibExecutor mulibExecutor;
    private final ChoicePointFactory choicePointFactory;
    private final ValueFactory valueFactory;
    private final CalculationFactory calculationFactory;
    // When on a known path, the upmost ChoiceOption corresponds to SymbolicExecution.currentChoiceOption
    private final ArrayDeque<Choice.ChoiceOption> predeterminedPath;

    // The current choice option. This will also be set to choice options on the known path.
    private Choice.ChoiceOption currentChoiceOption;
    private final ExecutionBudgetManager executionBudgetManager;
    private final MulibValueCopier mulibValueCopier;
    private final MulibValueLabeler mulibValueLabeler;
    private final Map<String, SubstitutedVar> namedVariables = new LinkedHashMap<>();
    private int nextNumberInitializedAtomicSymSints = 0;
    private int nextNumberInitializedAtomicSymSdoubles = 0;
    private int nextNumberInitializedAtomicSymSfloats = 0;
    private int nextNumberInitializedAtomicSymSbools = 0;
    private int nextNumberInitializedAtomicSymSlongs = 0;
    private int nextNumberInitializedAtomicSymSshorts = 0;
    private int nextNumberInitializedAtomicSymSbytes = 0;
    private long nextSarrayId;

    public SymbolicExecution(
            MulibExecutor mulibExecutor,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            Choice.ChoiceOption navigateTo,
            ExecutionBudgetManager executionBudgetManager,
            long nextSarrayId,
            boolean transformationRequired,
            MulibConfig config) {
        this.mulibExecutor = mulibExecutor;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.calculationFactory = calculationFactory;
        this.predeterminedPath = SearchTree.getPathTo(navigateTo);
        this.currentChoiceOption = predeterminedPath.peek();
        assert currentChoiceOption != null;
        this.executionBudgetManager = executionBudgetManager.copyFromPrototype();
        this.mulibValueCopier = new MulibValueCopier(this, config);
        this.mulibValueLabeler = new MulibValueLabeler(config, transformationRequired);
        set();
    }

    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    public MulibValueCopier getMulibValueCopier() {
        return mulibValueCopier;
    }

    public MulibValueLabeler getMulibValueLabeler() {
        return mulibValueLabeler;
    }

    public int getNextNumberInitializedAtomicSymSints() {
        return nextNumberInitializedAtomicSymSints++;
    }

    public int getNextNumberInitializedAtomicSymSdoubles() {
        return nextNumberInitializedAtomicSymSdoubles++;
    }

    public int getNextNumberInitializedAtomicSymSfloats() {
        return nextNumberInitializedAtomicSymSfloats++;
    }

    public int getNextNumberInitializedAtomicSymSbools() {
        return nextNumberInitializedAtomicSymSbools++;
    }

    public int getNextNumberInitializedAtomicSymSlongs() {
        return nextNumberInitializedAtomicSymSlongs++;
    }

    public int getNextNumberInitializedAtomicSymSbytes() {
        return nextNumberInitializedAtomicSymSbytes++;
    }

    public int getNextNumberInitializedAtomicSymSshorts() {
        return nextNumberInitializedAtomicSymSshorts++;
    }

    public long getNextNumberInitializedSarray() { return nextSarrayId++; }

    private void set() {
        se.set(this);
    }

    public static SymbolicExecution get() {
        return se.get();
    }

    // If a ChoiceOption exists on the predeterminedPath, pop it and add its constraints to existingConstraints
    // These are then orderly polled during the execution.
    public boolean transitionToNextChoiceOptionAndCheckIfOnKnownPath() {
        if (predeterminedPath.size() > 0) {
            predeterminedPath.pop();
        }

        // This case indicates that we are not on the predetermined path anymore.
        if (predeterminedPath.isEmpty()) {
            currentChoiceOption = null;
            return false;
        }

        currentChoiceOption = predeterminedPath.peek();
        assert currentChoiceOption != null;
        return true;
    }

    public boolean isOnKnownPath() {
        return !predeterminedPath.isEmpty();
    }

    public boolean nextIsOnKnownPath() {
        return predeterminedPath.size() > 1;
    }

    public Choice.ChoiceOption getCurrentChoiceOption() {
        return currentChoiceOption;
    }

    public Map<String, SubstitutedVar> getNamedVariables() {
        return Collections.unmodifiableMap(namedVariables);
    }

    public void addNamedVariable(String key, SubstitutedVar value) {
        if (namedVariables.containsKey(key)) {
            throw new MulibRuntimeException("Must not overwrite named variable.");
        }

        namedVariables.put(key, value);
    }

    public Optional<Choice.ChoiceOption> decideOnNextChoiceOptionDuringExecution(List<Choice.ChoiceOption> chooseFrom) {
        assert !isOnKnownPath() : "Should not occur";
        Optional<Choice.ChoiceOption> result = mulibExecutor.chooseNextChoiceOption(chooseFrom);
        result.ifPresent(choiceOption -> this.currentChoiceOption = choiceOption);
        return result;
    }

    public void addNewConstraint(Constraint c) {
        assert !nextIsOnKnownPath();
        mulibExecutor.addNewConstraint(c);
    }

    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        mulibExecutor.getExecutorManager().notifyNewChoice(depth, choiceOptions);
    }

    public ExecutionBudgetManager getExecutionBudgetManager() {
        return executionBudgetManager;
    }

    public boolean isSatisfiable() {
        return mulibExecutor.isSatisfiable();
    }

    public boolean checkWithNewConstraint(Constraint c) {
        return mulibExecutor.checkWithNewConstraint(c);
    }

    public void addNewArrayConstraint(ArrayConstraint ac) {
        assert !nextIsOnKnownPath();
        mulibExecutor.addNewArrayConstraint(ac);
    }

    public boolean checkWithNewArrayConstraint(ArrayConstraint ac) {
        return mulibExecutor.checkWithNewArrayConstraint(ac);
    }

    /* FREE ARRAY OPERATIONS */

    public Sint select(Sarray.SintSarray sarray, Sint index) {
        return (Sint) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sint store(Sarray.SintSarray sarray, Sint index, Sint value) {
        return (Sint) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sdouble select(Sarray.SdoubleSarray sarray, Sint index) {
        return (Sdouble) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sdouble store(Sarray.SdoubleSarray sarray, Sint index, Sdouble value) {
        return (Sdouble) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sfloat select(Sarray.SfloatSarray sarray, Sint index) {
        return (Sfloat) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sfloat store(Sarray.SfloatSarray sarray, Sint index, Sfloat value) {
        return (Sfloat) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Slong select(Sarray.SlongSarray sarray, Sint index) {
        return (Slong) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Slong store(Sarray.SlongSarray sarray, Sint index, SubstitutedVar value) {
        return (Slong) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sbyte select(Sarray.SbyteSarray sarray, Sint index) {
        return (Sbyte) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sbyte store(Sarray.SbyteSarray sarray, Sint index, SubstitutedVar value) {
        return (Sbyte) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sshort select(Sarray.SshortSarray sarray, Sint index) {
        return (Sshort) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sshort store(Sarray.SshortSarray sarray, Sint index, SubstitutedVar value) {
        return (Sshort) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sbool select(Sarray.SboolSarray sarray, Sint index) {
        return (Sbool) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sbool store(Sarray.SboolSarray sarray, Sint index, Sbool value) {
        return (Sbool) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public PartnerClass select(Sarray.PartnerClassSarray sarray, Sint index) {
        return (PartnerClass) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public PartnerClass store(Sarray.PartnerClassSarray sarray, Sint index, PartnerClass value) {
        return (PartnerClass) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    public Sarray select(Sarray.SarraySarray sarray, Sint index) {
        return (Sarray) calculationFactory.select(this, valueFactory, sarray, index);
    }

    public Sarray store(Sarray.SarraySarray sarray, Sint index, Sarray value) {
        return (Sarray) calculationFactory.store(this, valueFactory, sarray, index, value);
    }

    /* SYMBOLIC VARIABLE CREATION */

    public Sint namedSymSint(String identifier) {
        Sint result = symSint();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sshort namedSymSshort(String identifier) {
        Sshort result = symSshort();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sbyte namedSymSbyte(String identifier) {
        Sbyte result = symSbyte();
        namedVariables.put(identifier, result);
        return result;
    }

    public Slong namedSymSlong(String identifier) {
        Slong result = symSlong();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sdouble namedSymSdouble(String identifier) {
        Sdouble result = symSdouble();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sfloat namedSymSfloat(String identifier) {
        Sfloat result = symSfloat();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sbool namedSymSbool(String identifier) {
        Sbool result = symSbool();
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SintSarray namedSintSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SintSarray result = valueFactory.sintSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SdoubleSarray namedSdoubleSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SdoubleSarray result = valueFactory.sdoubleSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SfloatSarray namedSfloatSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SfloatSarray result = valueFactory.sfloatSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SlongSarray namedSlongSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SlongSarray result = valueFactory.slongSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SshortSarray namedSshortSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SshortSarray result = valueFactory.sshortSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SbyteSarray namedSbyteSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SbyteSarray result = valueFactory.sbyteSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SboolSarray namedSboolSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SboolSarray result = valueFactory.sboolSarray(this, len, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.PartnerClassSarray namedPartnerClassSarray(
            String identifier, Sint len, Class<? extends PartnerClass> clazz, boolean defaultIsSymbolic) {
        Sarray.PartnerClassSarray result = valueFactory.partnerClassSarray(this, len, clazz, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public Sarray.SarraySarray namedSarraySarray(
            String identifier, Sint len, Class<? extends SubstitutedVar> clazz, boolean defaultIsSymbolic) {
        Sarray.SarraySarray result = valueFactory.sarraySarray(this, len, clazz, defaultIsSymbolic);
        namedVariables.put(identifier, result);
        return result;
    }

    public PartnerClass symObject(Class<?> clazz) {
        throw new NotYetImplementedException();
    }

    public PartnerClass namedSymObject(String identifier, Class<?> clazz) {
        throw new NotYetImplementedException();
    }

    public Sshort symSshort() {
        return valueFactory.symSshort(this);
    }

    public Slong symSlong() {
        return valueFactory.symSlong(this);
    }

    public Sbyte symSbyte() {
        return valueFactory.symSbyte(this);
    }

    public Sint symSint() {
        return valueFactory.symSint(this);
    }

    public Sdouble symSdouble() {
        return valueFactory.symSdouble(this);
    }

    public Sfloat symSfloat() {
        return valueFactory.symSfloat(this);
    }

    public Sbool symSbool() {
        return valueFactory.symSbool(this);
    }

    public Sint concSint(int i) {
        return valueFactory.concSint(i);
    }

    public Slong concSlong(long l) {
        return valueFactory.concSlong(l);
    }

    public Sbyte concSbyte(byte b) {
        return valueFactory.concSbyte(b);
    }

    public Sshort concSshort(short s) {
        return valueFactory.concSshort(s);
    }

    public Sdouble concSdouble(double d) {
        return valueFactory.concSdouble(d);
    }

    public Sfloat concSfloat(float f) {
        return valueFactory.concSfloat(f);
    }

    public Sbool concSbool(boolean b) {
        return valueFactory.concSbool(b);
    }

    public Sarray.SintSarray sintSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sintSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SdoubleSarray sdoubleSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sdoubleSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SfloatSarray sfloatSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sfloatSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SlongSarray slongSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.slongSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SshortSarray sshortSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sshortSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SbyteSarray sbyteSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sbyteSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.SboolSarray sboolSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.sboolSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.PartnerClassSarray partnerClassSarray(Sint len, Class<? extends PartnerClass> innerElementClass, boolean defaultIsSymbolic) {
        return valueFactory.partnerClassSarray(this, len, innerElementClass, defaultIsSymbolic);
    }

    public Sarray.SarraySarray sarraySarray(Sint len, Class<?> innerElementClass, boolean defaultIsSymbolic) {
        return valueFactory.sarraySarray(this, len, innerElementClass, defaultIsSymbolic);
    }

    public Sarray.SarraySarray sarraySarray(Sint[] lengths, Class<?> innerElementClass) {
        return valueFactory.sarrarSarray(this, lengths, innerElementClass);
    }


    /* CONCRETIZE */

    public Object concretize(SubstitutedVar var) {
        return mulibExecutor.concretize(var);
    }

    public Object label(Sprimitive var) {
        return mulibExecutor.label(var);
    }

    /*
     * Convenience methods for bytecode-transformation. It is easier to just push a SymbolicExecution instance
     * to the stack and then execute a static method than to determine the code position where to push the
     * SymbolicExecution to.
     */

    public static Object concretize(Object var, SymbolicExecution se) {
        return se.concretize((SubstitutedVar) var);
    }

    public static Sint concSint(int i, SymbolicExecution se) {
        return se.concSint(i);
    }

    public static Sfloat concSfloat(float f, SymbolicExecution se) {
        return se.concSfloat(f);
    }

    public static Sdouble concSdouble(double d, SymbolicExecution se) {
        return se.concSdouble(d);
    }

    public static Sbool concSbool(boolean b, SymbolicExecution se) {
        return se.concSbool(b);
    }

    public static Slong concSlong(long l, SymbolicExecution se) {
        return se.concSlong(l);
    }

    public static Sbyte concSbyte(byte b, SymbolicExecution se) {
        return se.concSbyte(b);
    }

    public static Sshort concSshort(short s, SymbolicExecution se) {
        return se.concSshort(s);
    }

    public static Sint symSint(SymbolicExecution se) {
        return se.symSint();
    }

    public static Sint namedSymSint(String identifier, SymbolicExecution se) {
        return se.namedSymSint(identifier);
    }

    public static Sfloat symSfloat(SymbolicExecution se) {
        return se.symSfloat();
    }

    public static Sfloat namedSymSfloat(String identifier, SymbolicExecution se) {
        return se.namedSymSfloat(identifier);
    }

    public static Sdouble symSdouble(SymbolicExecution se) {
        return se.symSdouble();
    }

    public static Sdouble namedSymSdouble(String identifier, SymbolicExecution se) {
        return se.namedSymSdouble(identifier);
    }

    public static Sbool symSbool(SymbolicExecution se) {
        return se.symSbool();
    }

    public static Sbool namedSymSbool(String identifier, SymbolicExecution se) {
        return se.namedSymSbool(identifier);
    }

    public static Slong symSlong(SymbolicExecution se) {
        return se.symSlong();
    }

    public static Slong namedSymSlong(String identifier, SymbolicExecution se) {
        return se.namedSymSlong(identifier);
    }

    public static Sbyte symSbyte(SymbolicExecution se) {
        return se.symSbyte();
    }

    public static Sbyte namedSymSbyte(String identifier, SymbolicExecution se) {
        return se.namedSymSbyte(identifier);
    }

    public static Sshort symSshort(SymbolicExecution se) {
        return se.symSshort();
    }

    public static Sshort namedSymSshort(String identifier, SymbolicExecution se) {
        return se.namedSymSshort(identifier);
    }

    public static Sarray.SintSarray sintSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sintSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SdoubleSarray sdoubleSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sdoubleSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SfloatSarray sfloatSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sfloatSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SlongSarray slongSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.slongSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SshortSarray sshortSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sshortSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SbyteSarray sbyteSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sbyteSarray(len, defaultIsSymbolic);
    }

    public static Sarray.SboolSarray sboolSarray(Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sboolSarray(len, defaultIsSymbolic);
    }

    public static Sarray.PartnerClassSarray partnerClassSarray(Class<? extends PartnerClass> innerElementClass, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.partnerClassSarray(len, innerElementClass, defaultIsSymbolic);
    }

    public static Sarray.SarraySarray sarraySarray(Class<?> innerElementClass, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.sarraySarray(len, innerElementClass, defaultIsSymbolic);
    }

    public static Sarray.SarraySarray sarraySarray(Sint[] lengths, Class<? extends SubstitutedVar> innerElementClass, SymbolicExecution se) {
        return se.sarraySarray(lengths, innerElementClass);
    }

    public static Sarray.SintSarray namedSintSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSintSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SdoubleSarray namedSdoubleSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSdoubleSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SfloatSarray namedSfloatSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSfloatSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SlongSarray namedSlongSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSlongSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SshortSarray namedSshortSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSshortSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SbyteSarray namedSbyteSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSbyteSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.SboolSarray namedSboolSarray(String name, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSboolSarray(name, len, defaultIsSymbolic);
    }

    public static Sarray.PartnerClassSarray namedPartnerClassSarray(String name, Class<? extends PartnerClass> clazz, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedPartnerClassSarray(name, len, clazz, defaultIsSymbolic);
    }

    public static Sarray.SarraySarray namedSarraySarray(String name, Class<? extends SubstitutedVar> clazz, Sint len, boolean defaultIsSymbolic, SymbolicExecution se) {
        return se.namedSarraySarray(name, len, clazz, defaultIsSymbolic);
    }

    public static Sbool evalInstanceof(PartnerClass partnerClass, Class<?> c, SymbolicExecution se) {
        return se.evalInstanceof(partnerClass, c);
    }
    /* NUMBER OPERATIONS */

    public Sint add(Sint lhs, Sint rhs) {
        return calculationFactory.add(this, valueFactory, lhs, rhs);
    }

    public Sint sub(Sint lhs, Sint rhs) {
        return calculationFactory.sub(this, valueFactory, lhs, rhs);
    }

    public Sint div(Sint lhs, Sint rhs) {
        return calculationFactory.div(this, valueFactory, lhs, rhs);
    }

    public Sint mul(Sint lhs, Sint rhs) {
        return calculationFactory.mul(this, valueFactory, lhs, rhs);
    }

    public Sint mod(Sint lhs,  Sint rhs) {
        return calculationFactory.mod(this, valueFactory, lhs, rhs);
    }

    public Sint neg(Sint i) {
        return calculationFactory.neg(this, valueFactory, i);
    }

    public Sdouble add(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.add(this, valueFactory, lhs, rhs);
    }

    public Sdouble sub(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.sub(this, valueFactory, lhs, rhs);
    }

    public Sdouble div(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.div(this, valueFactory, lhs, rhs);
    }

    public Sdouble mul(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.mul(this, valueFactory, lhs, rhs);
    }

    public Sdouble mod(Sdouble lhs,  Sdouble rhs) {
        return calculationFactory.mod(this, valueFactory, lhs, rhs);
    }

    public Sdouble neg(Sdouble i) {
        return calculationFactory.neg(this, valueFactory, i);
    }

    public Slong add(Slong lhs, Slong rhs) {
        return calculationFactory.add(this, valueFactory, lhs, rhs);
    }

    public Slong sub(Slong lhs, Slong rhs) {
        return calculationFactory.sub(this, valueFactory, lhs, rhs);
    }

    public Slong div(Slong lhs, Slong rhs) {
        return calculationFactory.div(this, valueFactory, lhs, rhs);
    }

    public Slong mul(Slong lhs, Slong rhs) {
        return calculationFactory.mul(this, valueFactory, lhs, rhs);
    }

    public Slong mod(Slong lhs,  Slong rhs) {
        return calculationFactory.mod(this, valueFactory, lhs, rhs);
    }

    public Slong neg(Slong i) {
        return calculationFactory.neg(this, valueFactory, i);
    }

    public Sfloat add(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.add(this, valueFactory, lhs, rhs);
    }

    public Sfloat sub(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.sub(this, valueFactory, lhs, rhs);
    }

    public Sfloat div(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.div(this, valueFactory, lhs, rhs);
    }

    public Sfloat mul(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.mul(this, valueFactory, lhs, rhs);
    }

    public Sfloat mod(Sfloat lhs,  Sfloat rhs) {
        return calculationFactory.mod(this, valueFactory, lhs, rhs);
    }

    public Sfloat neg(Sfloat i) {
        return calculationFactory.neg(this, valueFactory, i);
    }

    public Sbool gt(Sint lhs, Sint rhs) {
        return calculationFactory.lt(this, valueFactory, rhs, lhs);
    }

    public Sbool gt(Slong lhs, Slong rhs) {
        return calculationFactory.lt(this, valueFactory, rhs, lhs);
    }

    public Sbool gt(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lt(this, valueFactory, rhs, lhs);
    }

    public Sbool gt(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lt(this, valueFactory, rhs, lhs);
    }

    public Sbool lt(Sint lhs, Sint rhs) {
        return calculationFactory.lt(this, valueFactory, lhs, rhs);
    }

    public Sbool lt(Slong lhs, Slong rhs) {
        return calculationFactory.lt(this, valueFactory, lhs, rhs);
    }

    public Sbool lt(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lt(this, valueFactory, lhs, rhs);
    }

    public Sbool lt(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lt(this, valueFactory, lhs, rhs);
    }

    public Sbool gte(Sint lhs, Sint rhs) {
        return calculationFactory.lte(this, valueFactory, rhs, lhs);
    }

    public Sbool gte(Slong lhs, Slong rhs) {
        return calculationFactory.lte(this, valueFactory, rhs, lhs);
    }

    public Sbool gte(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lte(this, valueFactory, rhs, lhs);
    }

    public Sbool gte(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lte(this, valueFactory, rhs, lhs);
    }

    public Sbool lte(Sint lhs, Sint rhs) {
        return calculationFactory.lte(this, valueFactory, lhs, rhs);
    }

    public Sbool lte(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lte(this, valueFactory, lhs, rhs);
    }

    public Sbool lte(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lte(this, valueFactory, lhs, rhs);
    }

    public Sbool lte(Slong lhs, Slong rhs) {
        return calculationFactory.lte(this, valueFactory, lhs, rhs);
    }

    public Sbool eq(Sint lhs, Sint rhs) {
        return calculationFactory.eq(this, valueFactory, lhs, rhs);
    }

    public Sbool eq(Slong lhs, Slong rhs) {
        return calculationFactory.eq(this, valueFactory, lhs, rhs);
    }

    public Sbool eq(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.eq(this, valueFactory, lhs, rhs);
    }

    public Sbool eq(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.eq(this, valueFactory, lhs, rhs);
    }

    public Sint cmp(Slong lhs, Slong rhs) {
        return calculationFactory.cmp(this, valueFactory, lhs, rhs);
    }

    public Sint cmp(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.cmp(this, valueFactory, lhs, rhs);
    }

    public Sint cmp(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.cmp(this, valueFactory, lhs, rhs);
    }

    public Slong i2l(Sint i) {
        return calculationFactory.i2l(this, valueFactory, i);
    }

    public Sfloat i2f(Sint i) {
        return calculationFactory.i2f(this, valueFactory, i);
    }

    public Sdouble i2d(Sint i) {
        return calculationFactory.i2d(this, valueFactory, i);
    }

    public Sint l2i(Slong l) {
        return calculationFactory.l2i(this, valueFactory, l);
    }

    public Sfloat l2f(Slong l) {
        return calculationFactory.l2f(this, valueFactory, l);
    }

    public Sdouble l2d(Slong l) {
        return calculationFactory.l2d(this, valueFactory, l);
    }

    public Sint f2i(Sfloat f) {
        return calculationFactory.f2i(this, valueFactory, f);
    }

    public Slong f2l(Sfloat f) {
        return calculationFactory.f2l(this, valueFactory, f);
    }

    public Sdouble f2d(Sfloat f) {
        return calculationFactory.f2d(this, valueFactory, f);
    }

    public Sint d2i(Sdouble d) {
        return calculationFactory.d2i(this, valueFactory, d);
    }

    public Slong d2l(Sdouble d) {
        return calculationFactory.d2l(this, valueFactory, d);
    }

    public Sfloat d2f(Sdouble d) {
        return calculationFactory.d2f(this, valueFactory, d);
    }

    public Sbyte i2b(Sint i) {
        return calculationFactory.i2b(this, valueFactory, i);
    }

    public Sshort i2s(Sint i) {
        return calculationFactory.i2s(this, valueFactory, i);
    }

    public Sbool evalInstanceof(PartnerClass partnerClass, Class<?> c) {
        /// TODO To CalculationFactory
        if (partnerClass == null) {
            return Sbool.ConcSbool.FALSE;
        }
        return Sbool.concSbool(c.isInstance(partnerClass));
    }

    public Object castTo(Object sarrayOrPartnerClassObject, Class<?> castTo) {
        /// TODO To CalculationFactory
        if (sarrayOrPartnerClassObject == null) {
            throw new ClassCastException();
        }
        return castTo.cast(sarrayOrPartnerClassObject);
    }

    /* BOOLEAN OPERATIONS */

    public Sbool and(final Sbool lhs, final Sbool rhs) {
        return calculationFactory.and(this, valueFactory, lhs, rhs);
    }

    public Sbool or(final Sbool lhs, final Sbool rhs) {
        return calculationFactory.or(this, valueFactory, lhs, rhs);
    }

    public Sbool not(final Sbool b) {
        return calculationFactory.not(this, valueFactory, b);
    }

    /* CHOICEPOINTFACTORY FACADE */

    public boolean ltChoice(final Sint compareToZero) {
        return ltChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean gtChoice(final Sint compareToZero) {
        return gtChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean eqChoice(final Sint compareToZero) {
        return eqChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean notEqChoice(final Sint compareToZero) {
        return notEqChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean gteChoice(final Sint compareToZero) {
        return gteChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean lteChoice(final Sint compareToZero) {
        return lteChoice(compareToZero, Sint.ConcSint.ZERO);
    }

    public boolean ltChoice(final Slong compareToZero) {
        return ltChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean gtChoice(final Slong compareToZero) {
        return gtChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean eqChoice(final Slong compareToZero) {
        return eqChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean notEqChoice(final Slong compareToZero) {
        return notEqChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean gteChoice(final Slong compareToZero) {
        return gteChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean lteChoice(final Slong compareToZero) {
        return lteChoice(compareToZero, Slong.ConcSlong.ZERO);
    }

    public boolean ltChoice(final Sdouble compareToZero) {
        return ltChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean gtChoice(final Sdouble compareToZero) {
        return gtChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean eqChoice(final Sdouble compareToZero) {
        return eqChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean notEqChoice(final Sdouble compareToZero) {
        return notEqChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean gteChoice(final Sdouble compareToZero) {
        return gteChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean lteChoice(final Sdouble compareToZero) {
        return lteChoice(compareToZero, Sdouble.ConcSdouble.ZERO);
    }

    public boolean ltChoice(final Sfloat compareToZero) {
        return ltChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean gtChoice(final Sfloat compareToZero) {
        return gtChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean eqChoice(final Sfloat compareToZero) {
        return eqChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean notEqChoice(final Sfloat compareToZero) {
        return notEqChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean gteChoice(final Sfloat compareToZero) {
        return gteChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean lteChoice(final Sfloat compareToZero) {
        return lteChoice(compareToZero, Sfloat.ConcSfloat.ZERO);
    }

    public boolean ltChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.ltChoice(this, lhs, rhs);
    }

    public boolean gtChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.gtChoice(this, lhs, rhs);
    }

    public boolean eqChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.eqChoice(this, lhs, rhs);
    }

    public boolean notEqChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.notEqChoice(this, lhs, rhs);
    }

    public boolean gteChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.gteChoice(this, lhs, rhs);
    }

    public boolean lteChoice(final Sint lhs, final Sint rhs) {
        return choicePointFactory.lteChoice(this, lhs, rhs);
    }

    public boolean ltChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.ltChoice(this, lhs, rhs);
    }

    public boolean gtChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.gtChoice(this, lhs, rhs);
    }

    public boolean eqChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.eqChoice(this, lhs, rhs);
    }

    public boolean notEqChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.notEqChoice(this, lhs, rhs);
    }

    public boolean gteChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.gteChoice(this, lhs, rhs);
    }

    public boolean lteChoice(final Sdouble lhs, final Sdouble rhs) {
        return choicePointFactory.lteChoice(this, lhs, rhs);
    }

    public boolean ltChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.ltChoice(this, lhs, rhs);
    }

    public boolean gtChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.gtChoice(this, lhs, rhs);
    }

    public boolean eqChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.eqChoice(this, lhs, rhs);
    }

    public boolean notEqChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.notEqChoice(this, lhs, rhs);
    }

    public boolean gteChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.gteChoice(this, lhs, rhs);
    }

    public boolean lteChoice(final Sfloat lhs, final Sfloat rhs) {
        return choicePointFactory.lteChoice(this, lhs, rhs);
    }

    public boolean ltChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.ltChoice(this, lhs, rhs);
    }

    public boolean gtChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.gtChoice(this, lhs, rhs);
    }

    public boolean eqChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.eqChoice(this, lhs, rhs);
    }

    public boolean notEqChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.notEqChoice(this, lhs, rhs);
    }

    public boolean gteChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.gteChoice(this, lhs, rhs);
    }

    public boolean lteChoice(final Slong lhs, final Slong rhs) {
        return choicePointFactory.lteChoice(this, lhs, rhs);
    }

    public boolean boolChoice(final Constraint b) {
        return choicePointFactory.boolChoice(this, b);
    }

    public boolean negatedBoolChoice(final Sbool b) {
        return choicePointFactory.negatedBoolChoice(this, b);
    }
}

