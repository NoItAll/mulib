package de.wwu.mulib.search.executors;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.ConcolicMathematicalContainer;
import de.wwu.mulib.search.budget.ExecutionBudgetManager;
import de.wwu.mulib.search.choice_points.ChoicePointFactory;
import de.wwu.mulib.search.trees.Choice;
import de.wwu.mulib.search.trees.SearchTree;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.solving.object_representations.AbstractPartnerClassObjectSolverRepresentation;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.ValueFactory;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.throwables.MulibIllegalStateException;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implements the facade pattern.
 * For each exploration of the search region, a new instance of SymbolicExecution is created and stored in a {@link ThreadLocal}.
 * This instance of symbolic execution contains all trails relevant for validly exploring the search region.
 * It stores a trail of predetermined choice options so that we can easily navigate to the unexplored choice option
 * that should be explored next.
 * It furthermore contains identifier-counter for ensuring that symbolic values that are already involved in constraints stored
 * in the constraint solver, are recreated so that the constraint stack is valid with regards to the current state of the execution.
 * In other words: If on the constraint solver there is a {@link de.wwu.mulib.substitutions.primitives.Sint.SymSintLeaf) with
 * the id {@link Sint.SymSintLeaf#getId()} "Sint1", {@link SymbolicExecution} will contact the {@link ValueFactory} to
 * create such a {@link de.wwu.mulib.substitutions.primitives.Sint.SymSintLeaf} at the correct location during the execution.
 * Similarly, for all other {@link Sprimitive}s, numbers are maintained and so are concrete identifiers of objects that
 * will be represented for the constraint solver.
 * It mostly delegates all other business logic to {@link MulibExecutor}, {@link ChoicePointFactory}, {@link ValueFactory},
 * and {@link CalculationFactory}.
 */
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
    private int nextNumberSymSintLeaf = 0, nextNumberSymSdoubleLeaf = 0,
            nextNumberSymSfloatLeaf = 0, nextNumberSymSboolLeaf = 0,
            nextNumberSymSlongLeaf = 0, nextNumberSymSshortLeaf = 0,
            nextNumberSymSbyteLeaf = 0, nextNumberSymScharLeaf = 0,
            nextIdentitiyHavingObjectNr;

    /**
     * @param mulibExecutor The executor that constructed this instance
     * @param choicePointFactory The choice point factory
     * @param valueFactory The value factory
     * @param calculationFactory The calculation factory
     * @param navigateTo The {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} to which we try to navigate.
     *                   {@link SymbolicExecution} will create a trail of options, predetermining the choices made in the
     *                   {@link ChoicePointFactory} to reach this new option.
     * @param executionBudgetManager The budget manager for this execution
     * @param nextIdentitiyHavingObjectNr The starting identifier for objects that are represented in the constraint solver
     * @param config The configuration
     */
    public SymbolicExecution(
            MulibExecutor mulibExecutor,
            ChoicePointFactory choicePointFactory,
            ValueFactory valueFactory,
            CalculationFactory calculationFactory,
            Choice.ChoiceOption navigateTo,
            ExecutionBudgetManager executionBudgetManager,
            int nextIdentitiyHavingObjectNr,
            MulibConfig config) {
        this.nextIdentitiyHavingObjectNr = nextIdentitiyHavingObjectNr;
        this.mulibExecutor = mulibExecutor;
        this.choicePointFactory = choicePointFactory;
        this.valueFactory = valueFactory;
        this.calculationFactory = calculationFactory;
        this.predeterminedPath = SearchTree.getPathTo(navigateTo);
        this.currentChoiceOption = predeterminedPath.peek();
        assert currentChoiceOption != null;
        this.executionBudgetManager = executionBudgetManager.copyFromPrototype();
        set();
    }
    
    public ValueFactory getValueFactory() {
        return valueFactory;
    }

    public CalculationFactory getCalculationFactory() {
        return calculationFactory;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sint.SymSintLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSintLeaf() {
        return nextNumberSymSintLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sdouble.SymSdoubleLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSdoubleLeaf() {
        return nextNumberSymSdoubleLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sfloat.SymSfloatLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSfloatLeaf() {
        return nextNumberSymSfloatLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sbool.SymSboolLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSboolLeaf() {
        return nextNumberSymSboolLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Slong.SymSlongLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSlongLeaf() {
        return nextNumberSymSlongLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sbyte.SymSbyteLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSbyteLeaf() {
        return nextNumberSymSbyteLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Sshort.SymSshortLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymSshortLeaf() {
        return nextNumberSymSshortLeaf++;
    }

    /**
     * @return A number identifying which {@link de.wwu.mulib.substitutions.primitives.Schar.SymScharLeaf}
     * (or an equivalent object) should be spawned next
     */
    public int getNextNumberSymScharLeaf() {
        return nextNumberSymScharLeaf++;
    }

    /**
     * @return The next number of an object that is represented for/in the solver
     * @see AbstractPartnerClassObjectSolverRepresentation#getNextUntrackedReservedId()
     */
    public int getNextNumberInitializedSymObject() {
        int result = nextIdentitiyHavingObjectNr++;
        if (result < 0) {
            // See AbstractPartnerClassObjectSolverRepresentation.getNextUntrackedReservedId
            throw new MulibIllegalStateException("Currently, negative integers are reserved for lazy initialization");
        }
        if (result == -1) { // Reserved for null-representation
            result = nextIdentitiyHavingObjectNr++;
        }
        return result;
    }

    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(Sint id, String field) {
        return mulibExecutor.getAvailableInformationOnPartnerClassObject(id, field, currentChoiceOption.getDepth());
    }

    ArrayInformation getAvailableInformationOnArray(Sint id) {
        return mulibExecutor.getAvailableInformationOnArray(id, currentChoiceOption.getDepth());
    }

    private void set() {
        se.set(this);
        AliasingInformation.resetAliasingTargets();
    }

    public static SymbolicExecution get() {
        return se.get();
    }

    public static void remove() {
        se.remove();
    }

    /**
     * If a ChoiceOption exists on the predeterminedPath, pop it and take the next choice option from the trail
     * @return true, if there is such a new choice option, else false. Returning true means that we are still on a
     * predetermined path for navigating to a new choice option. Returning false means that a {@link MulibExecutor}
     * must decide on the next choice option
     */
    public boolean transitionToNextChoiceOptionAndCheckIfOnKnownPath() {
        if (!predeterminedPath.isEmpty()) {
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

    /**
     * @return true if there is a next predetermined {@link de.wwu.mulib.search.trees.Choice.ChoiceOption} on the current path.
     * Returning true means that we should not modify any option since we already evaluated this choice option.
     * Returning false means that we are currently exploring a new choice option (for which, e.g., constraints should be
     * added that are found)
     */
    public boolean nextIsOnKnownPath() {
        return predeterminedPath.size() > 1;
    }

    /**
     * @return The current choice option. Might be a choice option that was already evaluated.
     */
    public Choice.ChoiceOption getCurrentChoiceOption() {
        return currentChoiceOption;
    }

    private void addNamedVariable(String key, Substituted value) {
        calculationFactory.remember(this, key, value);
        if (value instanceof Sprimitive) {
            mulibExecutor.rememberSprimitive(key, (Sprimitive) value);
        }
    }

    /**
     * Delegates the decision on determining which of the options to evaluate next to
     * {@link MulibExecutor#decideOnNextChoiceOptionDuringExecution(List)}.
     * @param chooseFrom The choice options to choose from
     * @return The {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}, if the execution should be returned.
     * {@link Optional#empty()} if the execution should be aborted
     */
    public Optional<Choice.ChoiceOption> decideOnNextChoiceOptionDuringExecution(List<Choice.ChoiceOption> chooseFrom) {
        assert predeterminedPath.isEmpty() : "Should not occur";
        Optional<Choice.ChoiceOption> result = mulibExecutor.decideOnNextChoiceOptionDuringExecution(chooseFrom);
        result.ifPresent(choiceOption -> this.currentChoiceOption = choiceOption);
        return result;
    }

    /**
     * Adds a constraint via {@link MulibExecutor#addNewConstraint(Constraint)} to the constraint solver and the
     * current {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * @param c The constraint
     */
    public void addNewConstraint(Constraint c) {
        assert !nextIsOnKnownPath();
        mulibExecutor.addNewConstraint(c);
    }

    /**
     * Notifies new choice options to the executor via {@link MulibExecutor#notifyNewChoice(int, List)}. These
     * choice options are NOT treated by this instance of symbolic execution.
     * @param depth The depth of the choice options
     * @param choiceOptions The choice options
     */
    public void notifyNewChoice(int depth, List<Choice.ChoiceOption> choiceOptions) {
        mulibExecutor.notifyNewChoice(depth, choiceOptions);
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

    /**
     * Adds a constraint via {@link MulibExecutor#addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint)}
     * to the constraint solver and the current {@link de.wwu.mulib.search.trees.Choice.ChoiceOption}.
     * @param pc The constraint
     */
    public void addNewPartnerClassObjectConstraint(PartnerClassObjectConstraint pc) {
        assert !nextIsOnKnownPath();
        mulibExecutor.addNewPartnerClassObjectConstraint(pc);
    }

    public Sbool.ConcSbool check(Sbool s) {
        return Sbool.concSbool(mulibExecutor.checkWithNewConstraint(ConcolicConstraintContainer.tryGetSymFromConcolic(s)));
    }

    public void assume(Sbool s) {
        if (!nextIsOnKnownPath()) {
            if (s instanceof Sbool.ConcSbool) {
                if (((Sbool.ConcSbool) s).isFalse()) {
                    throw Mulib.fail();
                }
            } else {
                addNewConstraint(ConcolicConstraintContainer.tryGetSymFromConcolic(s));
            }
        }
    }

    public Sbool checkAssume(Sbool s) {
        if (check(s).isTrue()) {
            assume(s);
            return Sbool.ConcSbool.TRUE;
        }
        return Sbool.ConcSbool.FALSE;
    }


    /* FREE ARRAY OPERATIONS */

    public Sint select(Sarray.SintSarray sarray, Sint index) {
        return (Sint) calculationFactory.select(this, sarray, index);
    }

    public Sint store(Sarray.SintSarray sarray, Sint index, Sint value) {
        return (Sint) calculationFactory.store(this, sarray, index, value);
    }

    public Sdouble select(Sarray.SdoubleSarray sarray, Sint index) {
        return (Sdouble) calculationFactory.select(this, sarray, index);
    }

    public Sdouble store(Sarray.SdoubleSarray sarray, Sint index, Sdouble value) {
        return (Sdouble) calculationFactory.store(this, sarray, index, value);
    }

    public Sfloat select(Sarray.SfloatSarray sarray, Sint index) {
        return (Sfloat) calculationFactory.select(this, sarray, index);
    }

    public Sfloat store(Sarray.SfloatSarray sarray, Sint index, Sfloat value) {
        return (Sfloat) calculationFactory.store(this, sarray, index, value);
    }

    public Slong select(Sarray.SlongSarray sarray, Sint index) {
        return (Slong) calculationFactory.select(this, sarray, index);
    }

    public Slong store(Sarray.SlongSarray sarray, Sint index, Slong value) {
        return (Slong) calculationFactory.store(this, sarray, index, value);
    }

    public Sbyte select(Sarray.SbyteSarray sarray, Sint index) {
        return (Sbyte) calculationFactory.select(this, sarray, index);
    }

    public Sbyte store(Sarray.SbyteSarray sarray, Sint index, Sbyte value) {
        return (Sbyte) calculationFactory.store(this, sarray, index, value);
    }

    public Sshort select(Sarray.SshortSarray sarray, Sint index) {
        return (Sshort) calculationFactory.select(this, sarray, index);
    }

    public Schar select(Sarray.ScharSarray sarray, Sint index) {
        return (Schar) calculationFactory.select(this, sarray, index);
    }

    public Sshort store(Sarray.SshortSarray sarray, Sint index, Sshort value) {
        return (Sshort) calculationFactory.store(this, sarray, index, value);
    }

    public Schar store(Sarray.ScharSarray sarray, Sint index, Schar value) {
        return (Schar) calculationFactory.store(this, sarray, index, value);
    }

    public Sbool select(Sarray.SboolSarray sarray, Sint index) {
        return (Sbool) calculationFactory.select(this, sarray, index);
    }

    public Sbool store(Sarray.SboolSarray sarray, Sint index, Sbool value) {
        return (Sbool) calculationFactory.store(this, sarray, index, value);
    }

    public PartnerClass select(Sarray.PartnerClassSarray sarray, Sint index) {
        return calculationFactory.select(this, sarray, index);
    }

    public PartnerClass store(Sarray.PartnerClassSarray sarray, Sint index, PartnerClass value) {
        return calculationFactory.store(this, sarray, index, value);
    }

    public Sarray select(Sarray.SarraySarray sarray, Sint index) {
        return calculationFactory.select(this, sarray, index);
    }

    public Sarray store(Sarray.SarraySarray sarray, Sint index, Sarray value) {
        return calculationFactory.store(this, sarray, index, value);
    }

    public Substituted getField(PartnerClass partnerClassObject, String field, Class<?> typeOfField) {
        return calculationFactory.getField(this, partnerClassObject, field, typeOfField);
    }

    public void putField(PartnerClass partnerClassObject, String field, Substituted value) {
        calculationFactory.putField(this, partnerClassObject, field, value);
    }

    /* SYMBOLIC VARIABLE CREATION */

    public Sint namedSymSint(String identifier) {
        Sint result = symSint();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sshort namedSymSshort(String identifier) {
        Sshort result = symSshort();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sbyte namedSymSbyte(String identifier) {
        Sbyte result = symSbyte();
        addNamedVariable(identifier, result);
        return result;
    }

    public Slong namedSymSlong(String identifier) {
        Slong result = symSlong();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sdouble namedSymSdouble(String identifier) {
        Sdouble result = symSdouble();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sfloat namedSymSfloat(String identifier) {
        Sfloat result = symSfloat();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sbool namedSymSbool(String identifier) {
        Sbool result = symSbool();
        addNamedVariable(identifier, result);
        return result;
    }

    public Schar namedSymSchar(String identifier) {
        Schar result = symSchar();
        addNamedVariable(identifier, result);
        return result;
    }

    public Sint namedSymSint(String identifier, Sint lb, Sint ub) {
        Sint result = symSint(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sshort namedSymSshort(String identifier, Sshort lb, Sshort ub) {
        Sshort result = symSshort(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sbyte namedSymSbyte(String identifier, Sbyte lb, Sbyte ub) {
        Sbyte result = symSbyte(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Slong namedSymSlong(String identifier, Slong lb, Slong ub) {
        Slong result = symSlong(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sdouble namedSymSdouble(String identifier, Sdouble lb, Sdouble ub) {
        Sdouble result = symSdouble(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sfloat namedSymSfloat(String identifier, Sfloat lb, Sfloat ub) {
        Sfloat result = symSfloat(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Schar namedSymSchar(String identifier, Schar lb, Schar ub) {
        Schar result = symSchar(lb, ub);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SintSarray namedSintSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SintSarray result = valueFactory.sintSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SdoubleSarray namedSdoubleSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SdoubleSarray result = valueFactory.sdoubleSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SfloatSarray namedSfloatSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SfloatSarray result = valueFactory.sfloatSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SlongSarray namedSlongSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SlongSarray result = valueFactory.slongSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SshortSarray namedSshortSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SshortSarray result = valueFactory.sshortSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SbyteSarray namedSbyteSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SbyteSarray result = valueFactory.sbyteSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SboolSarray namedSboolSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.SboolSarray result = valueFactory.sboolSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.ScharSarray namedScharSarray(String identifier, Sint len, boolean defaultIsSymbolic) {
        Sarray.ScharSarray result = valueFactory.scharSarray(this, len, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.PartnerClassSarray namedPartnerClassSarray(
            String identifier, Sint len, Class<? extends PartnerClass> clazz, boolean defaultIsSymbolic) {
        Sarray.PartnerClassSarray result = valueFactory.partnerClassSarray(this, len, clazz, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public Sarray.SarraySarray namedSarraySarray(
            String identifier, Sint len, Class<? extends Substituted> clazz, boolean defaultIsSymbolic) {
        Sarray.SarraySarray result = valueFactory.sarraySarray(this, len, clazz, defaultIsSymbolic);
        addNamedVariable(identifier, result);
        return result;
    }

    public PartnerClass symObject(Class<? extends PartnerClass> clazz) {
        return valueFactory.symObject(this, clazz);
    }

    public Sint aliasOf(Sint... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SintSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sdouble aliasOf(Sdouble... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SdoubleSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sfloat aliasOf(Sfloat... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SfloatSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Slong aliasOf(Slong... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SlongSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sshort aliasOf(Sshort... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SshortSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sbyte aliasOf(Sbyte... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SbyteSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sbool aliasOf(Sbool... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.SboolSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Schar aliasOf(Schar... aliasingTargets) {
        return select(aliasOf(
                aliasingTargets,
                () -> new Sarray.ScharSarray(concSint(aliasingTargets.length), this, false, Sbool.ConcSbool.FALSE)
        ), symSint());
    }

    public Sint aliasOf(Sarray.SintSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Sdouble aliasOf(Sarray.SdoubleSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Sfloat aliasOf(Sarray.SfloatSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Slong aliasOf(Sarray.SlongSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Sshort aliasOf(Sarray.SshortSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Sbyte aliasOf(Sarray.SbyteSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Sbool aliasOf(Sarray.SboolSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public Schar aliasOf(Sarray.ScharSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    private <T extends Sarray<U>, U extends Sprimitive> T aliasOf(U[] aliasingTargets, Supplier<T> init) {
        T res = init.get();
        for (int i = 0; i < aliasingTargets.length; i++) {
            res.store(concSint(i), aliasingTargets[i], this);
        }
        return res;
    }

    public PartnerClass aliasOf(PartnerClass... aliasingTargets) {
        // TODO There certainly are more efficient implementations for this without having to create a partner class sarray
        //  Possibly, ValueFactory and CalculationFactory should reuse functionality for representing objects and arrays for the solver
        if (aliasingTargets == null || aliasingTargets.length == 0) {
            throw Mulib.fail();
        }
        PartnerClass[] pcs = new PartnerClass[aliasingTargets.length];
        Class mostGeneralClass = null;
        for (int i = 0; i < aliasingTargets.length; i++) {
            pcs[i] = aliasingTargets[i];
            if (aliasingTargets[i] != null && (mostGeneralClass == null || !aliasingTargets[i].getClass().isAssignableFrom(mostGeneralClass))) {
                mostGeneralClass = aliasingTargets[i].getClass();
            }
        }
        mostGeneralClass = mostGeneralClass == null ? PartnerClass.class : mostGeneralClass;
        Sarray.PartnerClassSarray temp = Sarray.class.isAssignableFrom(mostGeneralClass) ?
                new Sarray.SarraySarray(concSint(pcs.length), this, false, mostGeneralClass, Sbool.ConcSbool.FALSE)
                :
                new Sarray.PartnerClassSarray(mostGeneralClass, concSint(pcs.length), this, false, Sbool.ConcSbool.FALSE);
        for (int i = 0; i < pcs.length; i++) {
            store(temp, concSint(i), pcs[i]);
        }
        return aliasOf(temp);
    }

    public PartnerClass aliasOf(Sarray.PartnerClassSarray aliasingTargets) {
        return select(aliasingTargets, symSint());
    }

    public void initializeLazyFields(PartnerClass partnerClass) {
        calculationFactory.initializeLazyFields(this, partnerClass);
    }

    public PartnerClass namedSymObject(String identifier, Class<? extends PartnerClass> clazz) {
        PartnerClass symObject = symObject(clazz);
        addNamedVariable(identifier, symObject);
        return symObject;
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

    public Schar symSchar() {
        return valueFactory.symSchar(this);
    }

    public Sshort symSshort(Sshort lb, Sshort ub) {
        return valueFactory.symSshort(this, lb, ub);
    }

    public Slong symSlong(Slong lb, Slong ub) {
        return valueFactory.symSlong(this, lb, ub);
    }

    public Sbyte symSbyte(Sbyte lb, Sbyte ub) {
        return valueFactory.symSbyte(this, lb, ub);
    }

    public Sint symSint(Sint lb, Sint ub) {
        return valueFactory.symSint(this, lb, ub);
    }

    public Sdouble symSdouble(Sdouble lb, Sdouble ub) {
        return valueFactory.symSdouble(this, lb, ub);
    }

    public Sfloat symSfloat(Sfloat lb, Sfloat ub) {
        return valueFactory.symSfloat(this, lb, ub);
    }

    public Schar symSchar(Schar lb, Schar ub) {
        return valueFactory.symSchar(this, lb, ub);
    }

    public Sint concSint(int i) {
        return Sint.concSint(i);
    }

    public Slong concSlong(long l) {
        return Slong.concSlong(l);
    }

    public Sbyte concSbyte(byte b) {
        return Sbyte.concSbyte(b);
    }

    public Sshort concSshort(short s) {
        return Sshort.concSshort(s);
    }

    public Sdouble concSdouble(double d) {
        return Sdouble.concSdouble(d);
    }

    public Sfloat concSfloat(float f) {
        return Sfloat.concSfloat(f);
    }

    public Sbool concSbool(boolean b) {
        return Sbool.concSbool(b);
    }

    public Schar concSchar(char c) {
        return Schar.concSchar(c);
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

    public Sarray.ScharSarray scharSarray(Sint len, boolean defaultIsSymbolic) {
        return valueFactory.scharSarray(this, len, defaultIsSymbolic);
    }

    public Sarray.PartnerClassSarray partnerClassSarray(Sint len, Class<? extends PartnerClass> innerElementClass, boolean defaultIsSymbolic) {
        return valueFactory.partnerClassSarray(this, len, innerElementClass, defaultIsSymbolic);
    }

    public Sarray.SarraySarray sarraySarray(Sint len, Class<?> innerElementClass, boolean defaultIsSymbolic) {
        return valueFactory.sarraySarray(this, len, innerElementClass, defaultIsSymbolic);
    }

    public Sarray.SintSarray sintSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sintSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SdoubleSarray sdoubleSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sdoubleSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SfloatSarray sfloatSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sfloatSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SlongSarray slongSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.slongSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SshortSarray sshortSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sshortSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SbyteSarray sbyteSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sbyteSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SboolSarray sboolSarray(Sint len, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sboolSarray(this, len, defaultIsSymbolic, canBeNull);
    }

    public Sarray.PartnerClassSarray partnerClassSarray(Sint len, Class<? extends PartnerClass> innerElementClass, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.partnerClassSarray(this, len, innerElementClass, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SarraySarray sarraySarray(Sint len, Class<?> innerElementClass, boolean defaultIsSymbolic, boolean canBeNull) {
        return valueFactory.sarraySarray(this, len, innerElementClass, defaultIsSymbolic, canBeNull);
    }

    public Sarray.SarraySarray sarraySarray(Sint[] lengths, Class<?> innerElementClass) {
        return valueFactory.sarrarySarray(this, lengths, innerElementClass);
    }

    public void nameSubstitutedVar(Substituted sv, String name) {
        addNamedVariable(name, sv);
    }


    /* CONCRETIZE */

    public Object concretize(Object var) {
        return mulibExecutor.concretize(var);
    }

    public Object label(Object var) {
        return mulibExecutor.label(var);
    }

    public static Sbool evalInstanceof(PartnerClass partnerClass, Class<?> c, SymbolicExecution se) {
        return se.evalInstanceof(partnerClass, c);
    }
    /* NUMBER OPERATIONS */

    public Sint add(Sint lhs, Sint rhs) {
        return calculationFactory.add(this, lhs, rhs);
    }

    public Sint sub(Sint lhs, Sint rhs) {
        return calculationFactory.sub(this, lhs, rhs);
    }

    public Sint div(Sint lhs, Sint rhs) {
        return calculationFactory.div(this, lhs, rhs);
    }

    public Sint mul(Sint lhs, Sint rhs) {
        return calculationFactory.mul(this, lhs, rhs);
    }

    public Sint mod(Sint lhs,  Sint rhs) {
        return calculationFactory.mod(this, lhs, rhs);
    }

    public Sint neg(Sint i) {
        return calculationFactory.neg(this, i);
    }

    public Sdouble add(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.add(this, lhs, rhs);
    }

    public Sdouble sub(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.sub(this, lhs, rhs);
    }

    public Sdouble div(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.div(this, lhs, rhs);
    }

    public Sdouble mul(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.mul(this, lhs, rhs);
    }

    public Sdouble mod(Sdouble lhs,  Sdouble rhs) {
        return calculationFactory.mod(this, lhs, rhs);
    }

    public Sdouble neg(Sdouble i) {
        return calculationFactory.neg(this, i);
    }

    public Slong add(Slong lhs, Slong rhs) {
        return calculationFactory.add(this, lhs, rhs);
    }

    public Slong sub(Slong lhs, Slong rhs) {
        return calculationFactory.sub(this, lhs, rhs);
    }

    public Slong div(Slong lhs, Slong rhs) {
        return calculationFactory.div(this, lhs, rhs);
    }

    public Slong mul(Slong lhs, Slong rhs) {
        return calculationFactory.mul(this, lhs, rhs);
    }

    public Slong mod(Slong lhs,  Slong rhs) {
        return calculationFactory.mod(this, lhs, rhs);
    }

    public Slong neg(Slong i) {
        return calculationFactory.neg(this, i);
    }

    public Sfloat add(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.add(this, lhs, rhs);
    }

    public Sfloat sub(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.sub(this, lhs, rhs);
    }

    public Sfloat div(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.div(this, lhs, rhs);
    }

    public Sfloat mul(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.mul(this, lhs, rhs);
    }

    public Sfloat mod(Sfloat lhs,  Sfloat rhs) {
        return calculationFactory.mod(this, lhs, rhs);
    }

    public Sfloat neg(Sfloat i) {
        return calculationFactory.neg(this, i);
    }

    public Sbool gt(Sint lhs, Sint rhs) {
        return calculationFactory.lt(this, rhs, lhs);
    }

    public Sbool gt(Slong lhs, Slong rhs) {
        return calculationFactory.lt(this, rhs, lhs);
    }

    public Sbool gt(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lt(this, rhs, lhs);
    }

    public Sbool gt(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lt(this, rhs, lhs);
    }

    public Sbool lt(Sint lhs, Sint rhs) {
        return calculationFactory.lt(this, lhs, rhs);
    }

    public Sbool lt(Slong lhs, Slong rhs) {
        return calculationFactory.lt(this, lhs, rhs);
    }

    public Sbool lt(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lt(this, lhs, rhs);
    }

    public Sbool lt(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lt(this, lhs, rhs);
    }

    public Sbool gte(Sint lhs, Sint rhs) {
        return calculationFactory.lte(this, rhs, lhs);
    }

    public Sbool gte(Slong lhs, Slong rhs) {
        return calculationFactory.lte(this, rhs, lhs);
    }

    public Sbool gte(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lte(this, rhs, lhs);
    }

    public Sbool gte(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lte(this, rhs, lhs);
    }

    public Sbool lte(Sint lhs, Sint rhs) {
        return calculationFactory.lte(this, lhs, rhs);
    }

    public Sbool lte(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.lte(this, lhs, rhs);
    }

    public Sbool lte(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.lte(this, lhs, rhs);
    }

    public Sbool lte(Slong lhs, Slong rhs) {
        return calculationFactory.lte(this, lhs, rhs);
    }

    public Sbool eq(Sint lhs, Sint rhs) {
        return calculationFactory.eq(this, lhs, rhs);
    }

    public Sbool eq(Slong lhs, Slong rhs) {
        return calculationFactory.eq(this, lhs, rhs);
    }

    public Sbool eq(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.eq(this, lhs, rhs);
    }

    public Sbool eq(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.eq(this, lhs, rhs);
    }

    public Sint cmp(Slong lhs, Slong rhs) {
        return calculationFactory.cmp(this, lhs, rhs);
    }

    public Sint cmp(Sdouble lhs, Sdouble rhs) {
        return calculationFactory.cmp(this, lhs, rhs);
    }

    public Sint cmp(Sfloat lhs, Sfloat rhs) {
        return calculationFactory.cmp(this, lhs, rhs);
    }

    public Slong i2l(Sint i) {
        return calculationFactory.i2l(this, i);
    }

    public Sfloat i2f(Sint i) {
        return calculationFactory.i2f(this, i);
    }

    public Sdouble i2d(Sint i) {
        return calculationFactory.i2d(this, i);
    }

    public Schar i2c(Sint i) {
        return calculationFactory.i2c(this, i);
    }

    public Sint l2i(Slong l) {
        return calculationFactory.l2i(this, l);
    }

    public Sfloat l2f(Slong l) {
        return calculationFactory.l2f(this, l);
    }

    public Sdouble l2d(Slong l) {
        return calculationFactory.l2d(this, l);
    }

    public Sint f2i(Sfloat f) {
        return calculationFactory.f2i(this, f);
    }

    public Slong f2l(Sfloat f) {
        return calculationFactory.f2l(this, f);
    }

    public Sdouble f2d(Sfloat f) {
        return calculationFactory.f2d(this, f);
    }

    public Sint d2i(Sdouble d) {
        return calculationFactory.d2i(this, d);
    }

    public Slong d2l(Sdouble d) {
        return calculationFactory.d2l(this, d);
    }

    public Sfloat d2f(Sdouble d) {
        return calculationFactory.d2f(this, d);
    }

    public Sbyte i2b(Sint i) {
        return calculationFactory.i2b(this, i);
    }

    public Sshort i2s(Sint i) {
        return calculationFactory.i2s(this, i);
    }

    public Sint ishl(Sint i0, Sint i1) {
        return calculationFactory.ishl(this, i0, i1);
    }

    public Sint ishr(Sint i0, Sint i1) {
        return calculationFactory.ishr(this, i0, i1);
    }

    public Sint ixor(Sint i0, Sint i1) {
        return calculationFactory.ixor(this, i0, i1);
    }

    public Sint ior(Sint i0, Sint i1) {
        return calculationFactory.ior(this, i0, i1);
    }

    public Sint iushr(Sint i0, Sint i1) {
        return calculationFactory.iushr(this, i0, i1);
    }

    public Sint iand(Sint i0, Sint i1) {
        return calculationFactory.iand(this, i0, i1);
    }

    public Slong lshl(Slong l0, Sint l1) {
        return calculationFactory.lshl(this, l0, l1);
    }

    public Slong lshr(Slong l0, Sint l1) {
        return calculationFactory.lshr(this, l0, l1);
    }

    public Slong lxor(Slong l0, Slong l1) {
        return calculationFactory.lxor(this, l0, l1);
    }

    public Slong lor(Slong l0, Slong l1) {
        return calculationFactory.lor(this, l0, l1);
    }

    public Slong lushr(Slong l0, Sint l1) {
        return calculationFactory.lushr(this, l0, l1);
    }

    public Slong land(Slong l0, Slong l1) {
        return calculationFactory.land(this, l0, l1);
    }

    public Sbool evalInstanceof(Object partnerClass, Class<?> c) {
        /// TODO To CalculationFactory
        if (partnerClass == null) {
            return Sbool.ConcSbool.FALSE;
        }
        return Sbool.concSbool(c.isInstance(partnerClass));
    }

    public Object castTo(Object sarrayOrPartnerClassObject, Class<?> castTo) {
        /// TODO To CalculationFactory
        if (sarrayOrPartnerClassObject == null) {
            return null;
        }
        if (sarrayOrPartnerClassObject.getClass() == castTo) {
            return sarrayOrPartnerClassObject;
        }
        if (sarrayOrPartnerClassObject instanceof Sbool) {
            if (castTo != Sint.class) {
                throw new MulibIllegalStateException(castTo.getName());
            }
            return sarrayOrPartnerClassObject;
        } else if (sarrayOrPartnerClassObject instanceof Sint) {
            if (castTo != Sbool.class) {
                if (castTo.isAssignableFrom(sarrayOrPartnerClassObject.getClass())) {
                    return sarrayOrPartnerClassObject;
                }
                throw new MulibIllegalStateException(castTo.getName());
            }
            Sint i = (Sint) sarrayOrPartnerClassObject;
            if (i instanceof ConcSnumber) {
                if ((((ConcSnumber) i).intVal())%2 == 0) {
                    return Sbool.ConcSbool.FALSE;
                } else {
                    return Sbool.ConcSbool.TRUE;
                }
            }
            i = (Sint) ConcolicMathematicalContainer.tryGetSymFromConcolic(i);
            Sbool representingSymSbool = symSbool();
            if (!nextIsOnKnownPath()) {
                // TODO Usually, we should also employ modulo...however, this would be rather expensive, so for now
                // we stick with the case that actually occurs
                addNewConstraint(BoolIte.newInstance(
                        Eq.newInstance(i, Sint.ConcSint.ZERO),
                        Not.newInstance(representingSymSbool),
                        representingSymSbool
                ));
            }
            return representingSymSbool;
        }
        return castTo.cast(sarrayOrPartnerClassObject);
    }

    public Sbool evalReferencesEq(Object o0, Object o1) {
        // TODO TO Calculation Factory
        if (o0 instanceof PartnerClass) {
            if (o1 instanceof PartnerClass) {
                return ((PartnerClass) o0).__mulib__getId().eq(((PartnerClass) o1).__mulib__getId(), this);
            } else if (o1 == null) {
                return ((PartnerClass) o0).__mulib__isNull();
            }
        } else if (o1 instanceof PartnerClass) {
            if (o0 == null) {
                return ((PartnerClass) o1).__mulib__isNull();
            }
        }
        return concSbool(o0 == o1);
    }

    /* BOOLEAN OPERATIONS */

    public Sbool and(final Sbool lhs, final Sbool rhs) {
        return calculationFactory.and(this, lhs, rhs);
    }

    public Sbool or(final Sbool lhs, final Sbool rhs) {
        return calculationFactory.or(this, lhs, rhs);
    }

    public Sbool not(final Sbool b) {
        return calculationFactory.not(this, b);
    }

    public Sbool xor(final Sbool lhs, final Sbool rhs) {
        return calculationFactory.xor(this, lhs, rhs);
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

    public boolean boolChoice(final Sbool b) {
        return choicePointFactory.boolChoice(this, b);
    }

    public boolean negatedBoolChoice(final Sbool b) {
        return choicePointFactory.negatedBoolChoice(this, b);
    }

    public boolean ltChoice(final Sint compareToZero, long id) {
        return ltChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean gtChoice(final Sint compareToZero, long id) {
        return gtChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean eqChoice(final Sint compareToZero, long id) {
        return eqChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean notEqChoice(final Sint compareToZero, long id) {
        return notEqChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean gteChoice(final Sint compareToZero, long id) {
        return gteChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean lteChoice(final Sint compareToZero, long id) {
        return lteChoice(compareToZero, Sint.ConcSint.ZERO, id);
    }

    public boolean ltChoice(final Slong compareToZero, long id) {
        return ltChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean gtChoice(final Slong compareToZero, long id) {
        return gtChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean eqChoice(final Slong compareToZero, long id) {
        return eqChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean notEqChoice(final Slong compareToZero, long id) {
        return notEqChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean gteChoice(final Slong compareToZero, long id) {
        return gteChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean lteChoice(final Slong compareToZero, long id) {
        return lteChoice(compareToZero, Slong.ConcSlong.ZERO, id);
    }

    public boolean ltChoice(final Sdouble compareToZero, long id) {
        return ltChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean gtChoice(final Sdouble compareToZero, long id) {
        return gtChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean eqChoice(final Sdouble compareToZero, long id) {
        return eqChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean notEqChoice(final Sdouble compareToZero, long id) {
        return notEqChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean gteChoice(final Sdouble compareToZero, long id) {
        return gteChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean lteChoice(final Sdouble compareToZero, long id) {
        return lteChoice(compareToZero, Sdouble.ConcSdouble.ZERO, id);
    }

    public boolean ltChoice(final Sfloat compareToZero, long id) {
        return ltChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean gtChoice(final Sfloat compareToZero, long id) {
        return gtChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean eqChoice(final Sfloat compareToZero, long id) {
        return eqChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean notEqChoice(final Sfloat compareToZero, long id) {
        return notEqChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean gteChoice(final Sfloat compareToZero, long id) {
        return gteChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean lteChoice(final Sfloat compareToZero, long id) {
        return lteChoice(compareToZero, Sfloat.ConcSfloat.ZERO, id);
    }

    public boolean ltChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.ltChoice(this, id, lhs, rhs);
    }

    public boolean gtChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.gtChoice(this, id, lhs, rhs);
    }

    public boolean eqChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.eqChoice(this, id, lhs, rhs);
    }

    public boolean notEqChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.notEqChoice(this, id, lhs, rhs);
    }

    public boolean gteChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.gteChoice(this, id, lhs, rhs);
    }

    public boolean lteChoice(final Sint lhs, final Sint rhs, long id) {
        return choicePointFactory.lteChoice(this, id, lhs, rhs);
    }

    public boolean ltChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.ltChoice(this, id, lhs, rhs);
    }

    public boolean gtChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.gtChoice(this, id, lhs, rhs);
    }

    public boolean eqChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.eqChoice(this, id, lhs, rhs);
    }

    public boolean notEqChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.notEqChoice(this, id, lhs, rhs);
    }

    public boolean gteChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.gteChoice(this, id, lhs, rhs);
    }

    public boolean lteChoice(final Sdouble lhs, final Sdouble rhs, long id) {
        return choicePointFactory.lteChoice(this, id, lhs, rhs);
    }

    public boolean ltChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.ltChoice(this, id, lhs, rhs);
    }

    public boolean gtChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.gtChoice(this, id, lhs, rhs);
    }

    public boolean eqChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.eqChoice(this, id, lhs, rhs);
    }

    public boolean notEqChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.notEqChoice(this, id, lhs, rhs);
    }

    public boolean gteChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.gteChoice(this, id, lhs, rhs);
    }

    public boolean lteChoice(final Sfloat lhs, final Sfloat rhs, long id) {
        return choicePointFactory.lteChoice(this, id, lhs, rhs);
    }

    public boolean ltChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.ltChoice(this, id, lhs, rhs);
    }

    public boolean gtChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.gtChoice(this, id, lhs, rhs);
    }

    public boolean eqChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.eqChoice(this, id, lhs, rhs);
    }

    public boolean notEqChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.notEqChoice(this, id, lhs, rhs);
    }

    public boolean gteChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.gteChoice(this, id, lhs, rhs);
    }

    public boolean lteChoice(final Slong lhs, final Slong rhs, long id) {
        return choicePointFactory.lteChoice(this, id, lhs, rhs);
    }

    public boolean boolChoice(final Sbool b, long id) {
        return choicePointFactory.boolChoice(this, id, b);
    }

    public boolean negatedBoolChoice(final Sbool b, long id) {
        return choicePointFactory.negatedBoolChoice(this, id, b);
    }

    public void setStaticField(String fieldName, Object value) {
        mulibExecutor.setStaticField(fieldName, value);
    }

    public Object getStaticField(String fieldName) {
        return mulibExecutor.getStaticField(fieldName);
    }

    public Sbool isInSearch() {
        return Sbool.concSbool(get() == this);
    }
}