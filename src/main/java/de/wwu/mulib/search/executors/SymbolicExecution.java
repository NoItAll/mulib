package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueTransformer;

import java.util.*;

@SuppressWarnings("unused")
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
    private final MulibValueTransformer mulibValueTransformer;

    private final Map<String, SubstitutedVar> namedVariables = new LinkedHashMap<>();
    private int nextNumberInitializedAtomicSymSints = 0;
    private int nextNumberInitializedAtomicSymSdoubles = 0;
    private int nextNumberInitializedAtomicSymSfloats = 0;
    private int nextNumberInitializedAtomicSymSbools = 0;
    private int nextNumberInitializedAtomicSymSlongs = 0;
    private int nextNumberInitializedAtomicSymSshorts = 0;
    private int nextNumberInitializedAtomicSymSbytes = 0;

    public SymbolicExecution(
            MulibExecutor mulibExecutor,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            Choice.ChoiceOption navigateTo,
            ExecutionBudgetManager executionBudgetManager,
            MulibValueTransformer mulibValueTransformer) {
        this.mulibExecutor = mulibExecutor;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.calculationFactory = calculationFactory;
        this.predeterminedPath = SearchTree.getPathTo(navigateTo);
        this.currentChoiceOption = predeterminedPath.peek();
        assert currentChoiceOption != null;
        this.executionBudgetManager = executionBudgetManager.copyFromPrototype();
        this.mulibValueTransformer = mulibValueTransformer;
        set();
    }

    public MulibValueTransformer getMulibValueTransformer() {
        return mulibValueTransformer;
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

    void set() {
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

    public ChoicePointFactory getCpFactory() {
        return choicePointFactory;
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
        mulibExecutor.addNewConstraint(c);
    }

    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        mulibExecutor.getExecutorManager().notifyNewChoice(depth, choiceOptions);
    }

    public ExecutionBudgetManager getExecutionBudgetManager() {
        return executionBudgetManager;
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
        if (var == null || var.getClass().isArray() || var instanceof String) { // TODO Exemplary special cases...String, Array, Objects...
            return var;
        }
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

    @SuppressWarnings("unchecked")
    public <T extends Sprimitive> T castTo(Sprimitive sprimitive, final Class<T> castTo) {
        /// TODO To CalculationFactory
        if (castTo.isAssignableFrom(sprimitive.getClass())) {
            return (T) sprimitive;
        }

        if (sprimitive instanceof ConcSnumber) {
            ConcSnumber si = (ConcSnumber) sprimitive;
            if (castTo == Sint.class) {
                return (T) concSint(si.intVal());
            } else if (castTo == Slong.class) {
                return (T) concSlong(si.longVal());
            } else if (castTo == Sshort.class) {
                return (T) concSshort(si.shortVal());
            } else if (castTo == Sbyte.class) {
                return (T) concSbyte(si.byteVal());
            } else if (castTo == Sdouble.class) {
                return (T) concSdouble(si.doubleVal());
            } else if (castTo == Sfloat.class) {
                return (T) concSfloat(si.floatVal());
            } else {
                throw new NotYetImplementedException();
            }
        }

        if (sprimitive instanceof Sint) {
            SymNumericExpressionSprimitive sym = (SymNumericExpressionSprimitive) sprimitive;
            if (castTo == Sint.class) {
                return (T) valueFactory.wrappingSymSint(this, sym);
            } else if (castTo == Slong.class) {
                return (T) valueFactory.wrappingSymSlong(this, sym);
            } else if (castTo == Sdouble.class) {
                return (T) valueFactory.wrappingSymSdouble(this, sym);
            } else if (castTo == Sfloat.class) {
                return (T) valueFactory.wrappingSymSfloat(this, sym);
            } else if (castTo == Sshort.class) {
                return (T) valueFactory.wrappingSymSshort(this, sym);
            } else if (castTo == Sbyte.class) {
                return (T) valueFactory.wrappingSymSbyte(this, sym);
            } else {
                throw new MulibRuntimeException("This type should not be castable to: " + castTo + " for " + sprimitive);
            }
        } else if (sprimitive instanceof Sfpnumber || sprimitive instanceof Slong) {
            SymNumericExpressionSprimitive sym = (SymNumericExpressionSprimitive) sprimitive;
            if (castTo == Sint.class) {
                return (T) valueFactory.wrappingSymSint(this, sym);
            } else if (castTo == Sdouble.class) {
                return (T) valueFactory.wrappingSymSdouble(this, sym);
            } else if (castTo == Sfloat.class) {
                return (T) valueFactory.wrappingSymSfloat(this, sym);
            } else if (castTo == Slong.class) {
                return (T) valueFactory.wrappingSymSlong(this, sym);
            } else {
                throw new MulibRuntimeException("This type should not be castable to: " + castTo + " for " + sprimitive);
            }
        } else {
            // TODO Regard other cases of Sprimitives.
            return (T) sprimitive;
        }
    }

    public Sbool evalInstanceof(PartnerClass partnerClass, Class<?> c) {
        /// TODO To CalculationFactory
        if (partnerClass == null) {
            return Sbool.FALSE;
        }
        return Sbool.concSbool(c.isInstance(partnerClass));
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
        return ltChoice(compareToZero, Sint.ZERO);
    }

    public boolean gtChoice(final Sint compareToZero) {
        return gtChoice(compareToZero, Sint.ZERO);
    }

    public boolean eqChoice(final Sint compareToZero) {
        return eqChoice(compareToZero, Sint.ZERO);
    }

    public boolean notEqChoice(final Sint compareToZero) {
        return notEqChoice(compareToZero, Sint.ZERO);
    }

    public boolean gteChoice(final Sint compareToZero) {
        return gteChoice(compareToZero, Sint.ZERO);
    }

    public boolean lteChoice(final Sint compareToZero) {
        return lteChoice(compareToZero, Sint.ZERO);
    }

    public boolean ltChoice(final Slong compareToZero) {
        return ltChoice(compareToZero, Slong.ZERO);
    }

    public boolean gtChoice(final Slong compareToZero) {
        return gtChoice(compareToZero, Slong.ZERO);
    }

    public boolean eqChoice(final Slong compareToZero) {
        return eqChoice(compareToZero, Slong.ZERO);
    }

    public boolean notEqChoice(final Slong compareToZero) {
        return notEqChoice(compareToZero, Slong.ZERO);
    }

    public boolean gteChoice(final Slong compareToZero) {
        return gteChoice(compareToZero, Slong.ZERO);
    }

    public boolean lteChoice(final Slong compareToZero) {
        return lteChoice(compareToZero, Slong.ZERO);
    }

    public boolean ltChoice(final Sdouble compareToZero) {
        return ltChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean gtChoice(final Sdouble compareToZero) {
        return gtChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean eqChoice(final Sdouble compareToZero) {
        return eqChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean notEqChoice(final Sdouble compareToZero) {
        return notEqChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean gteChoice(final Sdouble compareToZero) {
        return gteChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean lteChoice(final Sdouble compareToZero) {
        return lteChoice(compareToZero, Sdouble.ZERO);
    }

    public boolean ltChoice(final Sfloat compareToZero) {
        return ltChoice(compareToZero, Sfloat.ZERO);
    }

    public boolean gtChoice(final Sfloat compareToZero) {
        return gtChoice(compareToZero, Sfloat.ZERO);
    }

    public boolean eqChoice(final Sfloat compareToZero) {
        return eqChoice(compareToZero, Sfloat.ZERO);
    }

    public boolean notEqChoice(final Sfloat compareToZero) {
        return notEqChoice(compareToZero, Sfloat.ZERO);
    }

    public boolean gteChoice(final Sfloat compareToZero) {
        return gteChoice(compareToZero, Sfloat.ZERO);
    }

    public boolean lteChoice(final Sfloat compareToZero) {
        return lteChoice(compareToZero, Sfloat.ZERO);
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

    public boolean boolChoice(final Sbool b) {
        return choicePointFactory.boolChoice(this, b);
    }

    public boolean negatedBoolChoice(final Sbool b) {
        return choicePointFactory.negatedBoolChoice(this, b);
    }
}

