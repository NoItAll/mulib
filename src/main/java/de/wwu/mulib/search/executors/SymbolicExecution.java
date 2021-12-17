package de.wwu.mulib.search.executors;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.search.NumberUtil;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.search.values.ValueFactory;
import de.wwu.mulib.substitutions.Conc;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.Sym;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static de.wwu.mulib.search.NumberUtil.newWrappingSnumberDependingOnSnumber;

@SuppressWarnings("unused")
public final class SymbolicExecution {
    private final static ThreadLocal<SymbolicExecution> se = new ThreadLocal<>();
    private final MulibExecutor mulibExecutor;
    private final ChoicePointFactory choicePointFactory;
    private final ValueFactory valueFactory;
    // When on a known path, the upmost ChoiceOption corresponds to SymbolicExecution.currentChoiceOption
    private final ArrayDeque<Choice.ChoiceOption> predeterminedPath;

    private Choice.ChoiceOption currentChoiceOption;

    private final ExecutionBudgetManager executionBudgetManager;

    private final Map<String, Object> trackedVariables = new LinkedHashMap<>();
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
            Choice.ChoiceOption navigateTo,
            ExecutionBudgetManager executionBudgetManager) {
        this.mulibExecutor = mulibExecutor;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.predeterminedPath = SearchTree.getPathTo(navigateTo);
        this.currentChoiceOption = predeterminedPath.peek();
        assert currentChoiceOption != null;
        this.executionBudgetManager = executionBudgetManager.copyFromPrototype();
        set();
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
        return true;
    }

    public boolean isOnKnownPath() {
        return !predeterminedPath.isEmpty();
    }

    public ChoicePointFactory getCpFactory() {
        return choicePointFactory;
    }

    public Choice.ChoiceOption getCurrentChoiceOption() {
        return currentChoiceOption;
    }

    public Map<String, Object> getTrackedVariables() {
        return Collections.unmodifiableMap(trackedVariables);
    }

    public void addTrackedVariable(String key, Object value) {
        if (trackedVariables.containsKey(key)) {
            throw new MulibRuntimeException("Must not override tracked variable.");
        }

        trackedVariables.put(key, value);
    }

    public Optional<Choice.ChoiceOption> decideOnNextChoiceOptionDuringExecution(Choice chooseFrom) {
        assert !isOnKnownPath() : "Should not occur"; // Sanity check
        Optional<Choice.ChoiceOption> result = mulibExecutor.chooseNextChoiceOption(chooseFrom);
        result.ifPresent(choiceOption -> this.currentChoiceOption = choiceOption);
        return result;
    }

    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        mulibExecutor.getExecutorManager().notifyNewChoice(depth, choiceOptions);
    }

    public ExecutionBudgetManager getExecutionBudgetManager() {
        return executionBudgetManager;
    }

    /* SYMBOLIC VARIABLE CREATION */

    public Sint.SymSint trackedSymSint(String identifier) {
        Sint.SymSint result = symSint();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sshort.SymSshort trackedSymSshort(String identifier) {
        Sshort.SymSshort result = symSshort();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sbyte.SymSbyte trackedSymSbyte(String identifier) {
        Sbyte.SymSbyte result = symSbyte();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Slong.SymSlong trackedSymSlong(String identifier) {
        Slong.SymSlong result = symSlong();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sdouble.SymSdouble trackedSymSdouble(String identifier) {
        Sdouble.SymSdouble result = symSdouble();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sfloat.SymSfloat trackedSymSfloat(String identifier) {
        Sfloat.SymSfloat result = symSfloat();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sbool.SymSbool trackedSymSbool(String identifier) {
        Sbool.SymSbool result = symSbool();
        trackedVariables.put(identifier, result);
        return result;
    }

    public Sshort.SymSshort symSshort() {
        return valueFactory.symSshort(this);
    }

    public Slong.SymSlong symSlong() {
        return valueFactory.symSlong(this);
    }

    public Sbyte.SymSbyte symSbyte() {
        return valueFactory.symSbyte(this);
    }

    public Sint.SymSint symSint() {
        return valueFactory.symSint(this);
    }

    public Sdouble.SymSdouble symSdouble() {
        return valueFactory.symSdouble(this);
    }

    public Sfloat.SymSfloat symSfloat() {
        return valueFactory.symSfloat(this);
    }

    public Sbool.SymSbool symSbool() {
        return valueFactory.symSbool(this);
    }

    public Sint.ConcSint concSint(int i) {
        return valueFactory.concSint(i);
    }

    public Slong.ConcSlong concSlong(long l) {
        return valueFactory.concSlong(l);
    }

    public Sbyte.ConcSbyte concSbyte(byte b) {
        return valueFactory.concSbyte(b);
    }

    public Sshort.ConcSshort concSshort(short s) {
        return valueFactory.concSshort(s);
    }

    public Sdouble.ConcSdouble concSdouble(double d) {
        return valueFactory.concSdouble(d);
    }

    public Sfloat.ConcSfloat concSfloat(float f) {
        return valueFactory.concSfloat(f);
    }

    public Sbool.ConcSbool concSbool(boolean b) {
        return valueFactory.concSbool(b);
    }

    /* CONCRETIZE */

    public Object concretize(SubstitutedVar var) {
        if (var instanceof Conc) {
            if (var instanceof Sint.ConcSint) {
                return ((Sint.ConcSint) var).intVal();
            } else if (var instanceof Sdouble.ConcSdouble) {
                return ((Sdouble.ConcSdouble) var).doubleVal();
            } else if (var instanceof Sfloat.ConcSfloat) {
                return ((Sfloat.ConcSfloat) var).floatVal();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (var instanceof Sym) {
            throw new NotYetImplementedException();
//            return mulibExecutor.concretize(var);
        } else {
            throw new NotYetImplementedException();
        }
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

    public static Sint.ConcSint concSint(int i, SymbolicExecution se) {
        return se.concSint(i);
    }

    public static Sfloat.ConcSfloat concSfloat(float f, SymbolicExecution se) {
        return se.concSfloat(f);
    }

    public static Sdouble.ConcSdouble concSdouble(double d, SymbolicExecution se) {
        return se.concSdouble(d);
    }

    public static Sbool.ConcSbool concSbool(boolean b, SymbolicExecution se) {
        return se.concSbool(b);
    }

    public static Slong.ConcSlong concSlong(long l, SymbolicExecution se) {
        return se.concSlong(l);
    }

    public static Sbyte.ConcSbyte concSbyte(byte b, SymbolicExecution se) {
        return se.concSbyte(b);
    }

    public static Sshort.ConcSshort concSshort(short s, SymbolicExecution se) {
        return se.concSshort(s);
    }

    public static Sint.SymSint symSint(SymbolicExecution se) {
        return se.symSint();
    }

    public static Sint.SymSint trackedSymSint(String identifier, SymbolicExecution se) {
        return se.trackedSymSint(identifier);
    }

    public static Sfloat.SymSfloat symSfloat(SymbolicExecution se) {
        return se.symSfloat();
    }

    public static Sfloat.SymSfloat trackedSymSfloat(String identifier, SymbolicExecution se) {
        return se.trackedSymSfloat(identifier);
    }

    public static Sdouble.SymSdouble symSdouble(SymbolicExecution se) {
        return se.symSdouble();
    }

    public static Sdouble.SymSdouble trackedSymSdouble(String identifier, SymbolicExecution se) {
        return se.trackedSymSdouble(identifier);
    }

    public static Sbool.SymSbool symSbool(SymbolicExecution se) {
        return se.symSbool();
    }

    public static Sbool.SymSbool trackedSymSbool(String identifier, SymbolicExecution se) {
        return se.trackedSymSbool(identifier);
    }

    public static Slong.SymSlong symSlong(SymbolicExecution se) {
        return se.symSlong();
    }

    public static Slong.SymSlong trackedSymSlong(String identifier, SymbolicExecution se) {
        return se.trackedSymSlong(identifier);
    }

    public static Sbyte.SymSbyte symSbyte(SymbolicExecution se) {
        return se.symSbyte();
    }

    public static Sbyte.SymSbyte symSbyte(String identifier, SymbolicExecution se) {
        return se.trackedSymSbyte(identifier);
    }

    public static Sshort.SymSshort symSshort(SymbolicExecution se) {
        return se.symSshort();
    }

    public static Sshort.SymSshort symSshort(String identifier, SymbolicExecution se) {
        return se.trackedSymSshort(identifier);
    }
    /* NUMBER OPERATIONS */

    public <T extends Snumber> T add(final Snumber lhs, final Snumber rhs, Class<T> returnType) {
        return castTo(arithmeticOperatorTemplate(
                lhs, rhs,
                NumberUtil::addConcSnumber,
                Sum::sum
        ), returnType);
    }


    public <T extends Snumber> T sub(final Snumber lhs, final Snumber rhs, Class<T> returnType) {
        return castTo(arithmeticOperatorTemplate(
                lhs, rhs,
                NumberUtil::subConcSnumber,
                Sub::sub
        ), returnType);
    }

    public <T extends Snumber> T div(final Snumber lhs, final Snumber rhs, Class<T> returnType) {
        return castTo(arithmeticOperatorTemplate(
                lhs, rhs,
                NumberUtil::divConcSnumber,
                Div::div
        ), returnType);
    }

    public <T extends Snumber> T mul(final Snumber lhs, final Snumber rhs, Class<T> returnType) {
        return castTo(arithmeticOperatorTemplate(
                lhs, rhs,
                NumberUtil::mulConcSnumber,
                Mul::mul
        ), returnType);
    }

    public <T extends Snumber> T mod(final Snumber lhs, final Snumber rhs, Class<T> returnType) {
        return castTo(arithmeticOperatorTemplate(
                lhs, rhs,
                NumberUtil::modConcSnumber,
                Mod::mod
        ), returnType);
    }

    public <T extends Snumber> T neg(final Snumber number, Class<T> returnType) {
        return castTo(singleNumberOperatorTemplate(
                number,
                NumberUtil::neg,
                Neg::neg
        ), returnType);
    }

    public Sbool gt(Snumber lhs, Snumber rhs) {
        return numberComparisonTemplate(
                lhs, rhs,
                NumberUtil::gt,
                Gt::newInstance
        );
    }

    public Sbool lt(Snumber lhs, Snumber rhs) {
        return numberComparisonTemplate(
                lhs, rhs,
                NumberUtil::lt,
                Lt::newInstance
        );
    }

    public Sbool gte(Snumber lhs, Snumber rhs) {
        return numberComparisonTemplate(
                lhs, rhs,
                NumberUtil::gte,
                Gte::newInstance
        );
    }

    public Sbool lte(Snumber lhs, Snumber rhs) {
        return numberComparisonTemplate(
                lhs, rhs,
                NumberUtil::lte,
                Lte::newInstance
        );
    }

    public Sbool eq(Snumber lhs, Snumber rhs) {
        return numberComparisonTemplate(
                lhs, rhs,
                NumberUtil::eq,
                Eq::newInstance
        );
    }

    public Sint cmp(Snumber lhs, Snumber rhs) {
        return twoParameterOperatorTwoCaseDistinctionTemplate(
                lhs, rhs,
                Cmp::newInstance,
                Cmp::newInstance,
                cmp -> (Sint.ConcSint) cmp,
                valueFactory::wrappingSymSint
        );
    }

    @SuppressWarnings("unchecked")
    public <T extends Sprimitive> T castTo(Sprimitive sprimitive, final Class<T> castTo) {
        if (castTo.isAssignableFrom(sprimitive.getClass())) {
            return (T) sprimitive;
        }

        if (sprimitive instanceof Sintegernumber) {
            throw new NotYetImplementedException();
        } else if (sprimitive instanceof Sfpnumber) {
            throw new NotYetImplementedException();
        } else {
            // TODO Regard other cases of Sprimitives.
            return (T) sprimitive;
        }
    }

    /* BOOLEAN OPERATIONS */

    public Sbool and(final Sbool lhs, final Sbool rhs) {
        return booleanOperatorTemplate(
                lhs, rhs,
                (clhs, crhs) -> clhs.isTrue() && crhs.isTrue(),
                And::newInstance
        );
    }

    public Sbool or(final Sbool lhs, final Sbool rhs) {
        return booleanOperatorTemplate(
                lhs, rhs,
                (clhs, crhs) -> clhs.isTrue() || crhs.isTrue(),
                Or::newInstance
        );
    }

    public Sbool not(final Sbool b) {
        return singleBooleanOperatorTemplate(
                b,
                cb -> !cb.isTrue(),
                Not::newInstance
        );
    }

    /* CHOICEPOINTFACTORY FACADE */

    public boolean ltChoice(final Snumber compareToZero) {
        return choicePointFactory.ltChoice(this, compareToZero, concSint(0));
    }

    public boolean gtChoice(final Snumber compareToZero) {
        return choicePointFactory.gtChoice(this, compareToZero, concSint(0));
    }

    public boolean eqChoice(final Snumber compareToZero) {
        return choicePointFactory.eqChoice(this, compareToZero, concSint(0));
    }

    public boolean notEqChoice(final Snumber compareToZero) {
        return notEqChoice(compareToZero, concSint(0));
    }

    public boolean gteChoice(final Snumber compareToZero) {
        return choicePointFactory.gteChoice(this, compareToZero, concSint(0));
    }

    public boolean lteChoice(final Snumber compareToZero) {
        return choicePointFactory.lteChoice(this, compareToZero, concSint(0));
    }

    public boolean ltChoice(final Snumber lhs, final Snumber rhs) {
        return choicePointFactory.ltChoice(this, lhs, rhs);
    }

    public boolean gtChoice(final Snumber lhs, final Snumber rhs) {
        return choicePointFactory.gtChoice(this, lhs, rhs);
    }

    public boolean eqChoice(final Snumber lhs, final Snumber rhs) {
        return choicePointFactory.eqChoice(this, lhs, rhs);
    }

    public boolean notEqChoice(final Snumber lhs, final Snumber rhs) {
        Constraint constraint = eq(lhs, rhs);
        return negatedBoolChoice(Sbool.newConstraintSbool(constraint));
    }

    public boolean gteChoice(final Snumber lhs, final Snumber rhs) {
        return choicePointFactory.gteChoice(this, lhs, rhs);
    }

    public boolean lteChoice(final Snumber lhs, final Snumber rhs) {
        return choicePointFactory.lteChoice(this, lhs, rhs);
    }

    public boolean boolChoice(final Sbool b) {
        return choicePointFactory.boolChoice(this, b);
    }

    public boolean negatedBoolChoice(final Sbool b) {
        return choicePointFactory.boolChoice(this, Sbool.newConstraintSbool(Not.newInstance(b)));
    }


    /* TEMPLATES */

    @SuppressWarnings("unchecked")
    private <R, P, C, I, S, SI> R operatorTwoCaseDistinctionTemplate(
            P p,
            boolean isConcrete,
            Function<C, I> concreteToIntermediate,
            Function<S, SI> symbolicToIntermediate,
            Function<I, R> resultWrapperConcFunction,
            Function<SI, R> resultWrapperSymFunction) {
        // Case 1: Concrete calculation
        if (isConcrete) {
            I intermediate = concreteToIntermediate.apply((C) p);
            return resultWrapperConcFunction.apply(intermediate);
        }

        // Case 2: Use factory to either retrieve or get cached result of symbolic calculation
        SI intermediate = symbolicToIntermediate.apply((S) p);

        return resultWrapperSymFunction.apply(intermediate);
    }

    private <R, C, I, S> R singleParameterOperatorTwoCaseDistinctionTemplate(
            R p,
            Function<C, I> concreteToIntermediate,
            Function<S, S> symbolicToIntermediate,
            Function<I, R> resultWrapperConcFunction,
            Function<S, R> resultWrapperSymFunction) {
        return operatorTwoCaseDistinctionTemplate(
                p,
                p instanceof ConcSprimitive,
                concreteToIntermediate,
                symbolicToIntermediate,
                resultWrapperConcFunction,
                resultWrapperSymFunction
        );
    }

    @SuppressWarnings("unchecked")
    private <R, P, C, I, S, SI> R twoParameterOperatorTwoCaseDistinctionTemplate(
            P lhs,
            P rhs,
            BiFunction<C, C, I> concreteToIntermediate,
            BiFunction<S, S, SI> symbolicToIntermediate,
            Function<I, R> resultWrapperConcFunction,
            Function<SI, R> resultWrapperSymFunction) {
        return operatorTwoCaseDistinctionTemplate(
                rhs,
                lhs instanceof ConcSprimitive && rhs instanceof ConcSprimitive,
                crhs -> concreteToIntermediate.apply((C) lhs, (C) crhs),
                srhs -> symbolicToIntermediate.apply((S) lhs, (S) srhs),
                resultWrapperConcFunction,
                resultWrapperSymFunction
        );
    }

    private Sbool singleBooleanOperatorTemplate(
            Sbool b,
            Function<Sbool.ConcSbool, Boolean> concreteCaseFunction,
            Function<Constraint, Constraint> symbolicCaseFunction) {
        return singleParameterOperatorTwoCaseDistinctionTemplate(
                b,
                concreteCaseFunction,
                symbolicCaseFunction,
                valueFactory::concSbool,
                valueFactory::wrappingSymSbool
        );
    }

    private Snumber singleNumberOperatorTemplate(
            Snumber n,
            Function<ConcSnumber, Snumber> concreteCaseFunction,
            Function<NumericExpression, NumericExpression> operatorFunction ) {
        return singleParameterOperatorTwoCaseDistinctionTemplate(
                n,
                concreteCaseFunction,
                operatorFunction,
                (snumber -> snumber),
                expressionToWrap -> newWrappingSnumberDependingOnSnumber(n, expressionToWrap, valueFactory)
        );
    }

    private Sbool booleanOperatorTemplate(
            Sbool lhs,
            Sbool rhs,
            BiFunction<Sbool.ConcSbool, Sbool.ConcSbool, Boolean> concreteCaseFunction,
            BiFunction<Sbool, Sbool, Constraint> symbolicCaseFunction) {
        return twoParameterOperatorTwoCaseDistinctionTemplate(
                lhs, rhs,
                concreteCaseFunction,
                symbolicCaseFunction,
                valueFactory::concSbool,
                valueFactory::wrappingSymSbool
        );
    }

    private Sbool numberComparisonTemplate(
            Snumber lhs,
            Snumber rhs,
            BiFunction<ConcSnumber, ConcSnumber, Boolean> concreteCase,
            BiFunction<Snumber, Snumber, Constraint> symbolicConstraintCase) {
        return twoParameterOperatorTwoCaseDistinctionTemplate(
                lhs, rhs,
                concreteCase,
                symbolicConstraintCase,
                valueFactory::concSbool,
                valueFactory::wrappingSymSbool
        );
    }

    private Snumber arithmeticOperatorTemplate(
            Snumber lhs,
            Snumber rhs,
            BiFunction<ConcSnumber, ConcSnumber, Snumber> concreteCaseFunction,
            BiFunction<NumericExpression, NumericExpression, NumericExpression> operatorFunction) {
        return twoParameterOperatorTwoCaseDistinctionTemplate(
                lhs, rhs,
                concreteCaseFunction,
                operatorFunction,
                snumber -> snumber,
                (numericExpression -> NumberUtil.newWrappingSnumberDependingOnLhsAndRhs(lhs, rhs, numericExpression, valueFactory))
        );
    }

    private boolean isOnExploredPath() {
        return currentChoiceOption.isEvaluated();
    }
}
