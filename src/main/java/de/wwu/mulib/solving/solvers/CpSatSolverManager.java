package de.wwu.mulib.solving.solvers;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.substitutions.Substituted;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.throwables.UnknownSolutionException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CpSatSolverManager extends AbstractIncrementalEnabledSolverManager<
        CpSatSolverManager.SolEnumerator,
        com.google.ortools.sat.Constraint,
        Object,
        Object> {
    private final long lb;
    private final long ub;
    private CpModel modelWithConstraints;
    private final CpSolver solver;

    public CpSatSolverManager(MulibConfig mc) {
        super(mc);
        Loader.loadNativeLibraries();
        this.solver = new CpSolver();
        this.solver.getParameters().setAlsoBumpVariablesInConflictReasons(true);
        this.solver.getParameters().setAutoDetectGreaterThanAtLeastOneOf(false);
        this.solver.getParameters().setBinaryMinimizationAlgorithm(SatParameters.BinaryMinizationAlgorithm.BINARY_MINIMIZATION_FIRST); // Default
        this.solver.getParameters().setPreferredVariableOrder(SatParameters.VariableOrder.IN_ORDER); // Default
        this.solver.getParameters().setExploitAllPrecedences(false); // Default
        this.solver.getParameters().setMaxSatStratification(SatParameters.MaxSatStratificationAlgorithm.STRATIFICATION_ASCENT);
        this.solver.getParameters().setGlucoseDecayIncrementPeriod(5000); // Default
        this.solver.getParameters().setGlucoseDecayIncrement(0.01); // Default
        this.solver.getParameters().setGlucoseMaxDecay(0.95); // Default
        this.solver.getParameters().setMaxPresolveIterations(3); // Default
        this.solver.getParameters().setMinimizeCore(true); // Default
        this.solver.getParameters().setPermutePresolveConstraintOrder(false); // Default
        this.solver.getParameters().setPresolveBlockedClause(true); // Default
        this.solver.getParameters().setPresolveExtractIntegerEnforcement(true);
        this.solver.getParameters().setPresolveBvaThreshold(1); // Default
        this.solver.getParameters().setUsePhaseSaving(true); // Default
        this.solver.getParameters().setPresolveUseBva(true); // Default
        this.solver.getParameters().setPresolveBveThreshold(500); // Default
        this.solver.getParameters().setPresolveBveClauseWeight(3); // Default
        this.solver.getParameters().setVariableActivityDecay(0.2);
        this.solver.getParameters().setSymmetryLevel(2); // Default
        this.solver.getParameters().setLinearizationLevel(1); // Default//
        this.solver.getParameters().setSearchBranching(SatParameters.SearchBranching.AUTOMATIC_SEARCH);
//        this.solver.getParameters().setNumWorkers(4);
//        this.solver.getParameters().setLogSearchProgress(true).setLogSubsolverStatistics(false);
//        this.solver.getParameters().setRepairHint(true);
        this.lb = mc.VALS_SYMSINT_LB.map(sint -> ((Sint.ConcSint) sint).intVal()).orElse(Integer.MIN_VALUE);
        this.ub = mc.VALS_SYMSINT_UB.map(sint -> ((Sint.ConcSint) sint).intVal()).orElse(Integer.MAX_VALUE);
    }


    private void newModelWithCurrentConstraints() {
        // We use this method to generate a model after a pop. To this model constraints are then added
        // It is assumed that transformConstraint will modify the current model
        assert this.modelWithConstraints == null;
        this.modelWithConstraints = new CpModel();
        for (Constraint c : _getConstraints()) {
            _transformConstraint(c, false);
        }
        for (PartnerClassObjectConstraint pcoc : _getPartnerClassObjectConstraints()) {
            addPartnerClassObjectConstraint(pcoc);
        }
    }

    private void maybeApplyHints() {
        // TODO
    }

    @Override
    protected void addSolverConstraintRepresentation(com.google.ortools.sat.Constraint c) {
        // Nothing to do here; - the constraint is already added in the transformConstraint-method
    }

    @Override
    protected boolean calculateIsSatisfiable() {
        maybeApplyHints();
        // TODO check if is incremental (i.e. called multiple times; is the case if model is not null and solver has solution), apply hints in that case (optional)
        // TODO Potentially, model can add all constraints with "onlyEnforceIf" --> check!
        CpSolverStatus status = solver.solve(getModelWithConstraints());
        if (status == CpSolverStatus.UNKNOWN) {
            throw new UnknownSolutionException("Some limit has been reached");
        }
        return status == CpSolverStatus.FEASIBLE || status == CpSolverStatus.OPTIMAL;
    }

    @Override
    protected Object createCompletelyNewArrayRepresentation(ArrayInitializationConstraint ac) {
        throw new NotYetImplementedException("No specific array representation has been implemented");
    }

    @Override
    protected Object createNewArrayRepresentationForStore(ArrayAccessConstraint ac, Object oldRepresentation) {
        throw new NotYetImplementedException("No specific array representation has been implemented");
    }

    @Override
    protected void solverSpecificBacktrackingPoint() {
        // Nothing to do here
    }

    @Override
    protected void solverSpecificBacktrackOnce() {
        resetModelWithCurrentConstraints();
    }

    @Override
    protected void solverSpecificBacktrack(int toBacktrack) {
        if (toBacktrack > 0) {
            solverSpecificBacktrackOnce();
        }
    }

    @Override
    protected boolean calculateSatisfiabilityWithSolverBoolRepresentation(com.google.ortools.sat.Constraint constraint) {
        BoolVar boolVar = spawnRepresentationBoolVar();
        constraint.onlyEnforceIf(boolVar);
        getModelWithConstraints().addAssumption(boolVar);
        boolean isSatisfiable = calculateIsSatisfiable();
        // TODO If we use assumptions anywhere else, we need to reevaluate this approach:
        getModelWithConstraints().clearAssumptions();
        return isSatisfiable;
    }

    @Override
    protected com.google.ortools.sat.Constraint newArraySelectConstraint(Object arrayRepresentation, Sint indexInArray, Substituted arrayValue) {
        throw new NotYetImplementedException("No specific array representation has been implemented");
    }

    private BoolVar spawnRepresentationBoolVar() {
        return getModelWithConstraints().newBoolVar("_r" + currentVarId++);
    }

    @Override
    protected com.google.ortools.sat.Constraint transformConstraint(Constraint c) {
        getModelWithConstraints();
        com.google.ortools.sat.Constraint transformed = _transformConstraint(c, false);
        return transformed;
    }

    @Override
    protected void solverSpecificShutdown() {

    }

    private com.google.ortools.sat.Constraint enforceLiteralToBe(Literal l, boolean t) {
        if (t) {
            return getModelWithConstraints().addEquality(l, getModelWithConstraints().trueLiteral());
        } else {
            return getModelWithConstraints().addEquality(l, getModelWithConstraints().falseLiteral());
        }
    }

    private CpModel getModelWithConstraints() {
        if (modelWithConstraints == null) {
            assert enumerator == null;
            newModelWithCurrentConstraints();
        }
        return modelWithConstraints;
    }

    private com.google.ortools.sat.Constraint _transformConstraint(Constraint c, boolean andIsPotentiallyReified) {
        if (c instanceof Sbool.SymSbool) {
            c = ((Sbool.SymSbool) c).getRepresentedConstraint();
        }
        Literal l;
        if (c instanceof Sbool) {
            l = getLiteralRepresentingConstraintIfNeeded(c);
            return enforceLiteralToBe(l, true);
        } else if (c instanceof Not) {
            Not n = (Not) c;
            if (n.getConstraint() instanceof Eq) {
                Eq eq = (Eq) n.getConstraint();
                LinearArgument left = transformNumericalExpression(eq.getLhs());
                LinearArgument right = transformNumericalExpression(eq.getRhs());
                return getModelWithConstraints().addDifferent(left, right);
            } else if (n.getConstraint() instanceof Sbool) {
                Sbool b = (Sbool) n.getConstraint();
                l = getLiteralRepresentingConstraintIfNeeded(b);
                return enforceLiteralToBe(l, false);
            } else if (n.getConstraint() instanceof In) {
                In in = (In) n.getConstraint();
                LinearArgument element = transformNumericalExpression((Snumber) in.getElement());
                LinearArgument[] vals = Arrays.stream(in.getSet())
                        .map(this::transformNumericalExpression)
                        .toArray(LinearArgument[]::new);
                throw new NotYetImplementedException(); // TODO
            } else {
                // We use Not in getLiteralRepresentingConstraintIfNeeded, thus, we cannot simply pass down the content
                // of Not
//                Constraint pushedDown = n.pushDown();
//                l = getLiteralRepresentingConstraintIfNeeded(pushedDown);
//                return enforceLiteralToBe(l, true);
                l = getLiteralRepresentingConstraintIfNeeded(n.getConstraint());
                return enforceLiteralToBe(l, false);
            }
        } else if (c instanceof TwoSidedConstraint) {
            return transformTwoSidedConstraint((TwoSidedConstraint) c, andIsPotentiallyReified);
        } else if (c instanceof TwoSidedExpressionConstraint) {
            return transformTwoSidedExpressionConstraint((TwoSidedExpressionConstraint) c);
        } else if (c instanceof In) {
            return transformIn((In) c);
        } else if (c instanceof BoolIte) {
            return transformBoolIte((BoolIte) c);
        } else {
            throw new NotYetImplementedException(c.toString());
        }
    }

    private final Map<Constraint, Literal> constraintToReification = new HashMap<>();
    private Literal getLiteralRepresentingConstraintIfNeeded(Constraint c) {
        Literal l = constraintToReification.get(c);
        if (l != null) {
            return l;
        }
        if (c instanceof Sbool) {
            l = (Literal) getVarForSprimitive((Sbool) c);
            if (l == null) {
                l = (Literal) transformNumericalExpression((Sbool) c);
            }
            return l;
        } else if (c instanceof Not && ((Not) c).getConstraint() instanceof Sbool) {
            Sbool s = (Sbool) ((Not) c).getConstraint();
            l = (Literal) getVarForSprimitive(s);
            if (l == null) {
                l = (Literal) transformNumericalExpression(s);
            }
            constraintToReification.put(c, l.not());
            return l.not();
        } else {
            l = transformConstraintRepresentedByBool(c);
            return l;
        }
    }

    protected BoolVar transformConstraintRepresentedByBool(Constraint c) {
        // Following the documentation of ConstraintProto.addEnforcementLiteral(...), we
        // want to have full reification.
        BoolVar result = spawnRepresentationBoolVar();
        constraintToReification.put(c, result);
        com.google.ortools.sat.Constraint representedConstraint = _transformConstraint(c, true);
        Constraint pushedDown = Not.newInstance(c); // TODO Check if we can avoid that
        constraintToReification.put(pushedDown, result.not());
        if (pushedDown instanceof Not) {
            pushedDown = ((Not) pushedDown).tryPushDown();
        }
        constraintToReification.put(pushedDown, result.not());
        com.google.ortools.sat.Constraint negatedRepresentedConstraint = _transformConstraint(pushedDown, true);
        representedConstraint.onlyEnforceIf(result);
        negatedRepresentedConstraint.onlyEnforceIf(result.not());
        return result;
    }

    private com.google.ortools.sat.Constraint transformTwoSidedConstraint(
            TwoSidedConstraint c,
            boolean andIsPotentiallyReified) {
        if (c instanceof Implication) {
            Literal blhs = getLiteralRepresentingConstraintIfNeeded(c.getLhs());
            Literal brhs = getLiteralRepresentingConstraintIfNeeded(c.getRhs());
            return getModelWithConstraints().addImplication(blhs, brhs);
        } else if (c instanceof Equivalence) {
            Literal blhs = getLiteralRepresentingConstraintIfNeeded(c.getLhs());
            Literal brhs = getLiteralRepresentingConstraintIfNeeded(c.getRhs());
            return getModelWithConstraints().addEquality(blhs, brhs);
        }

        List<Constraint> unrolled = c.unrollSameType();
        if (!andIsPotentiallyReified && c instanceof And) { // TODO Check if even is worth it
            // And must be reified if it is not a top-layer AND, i.e., if it only must be true under some circumstances (e.g. an implication)
            for (Constraint u : unrolled) {
                // Also adds constraint to model:
                _transformConstraint(u, false);
            }
            return null; // TODO Verify Should be unproblematic here!
        }
        List<Literal> literals = new ArrayList<>();
        for (Constraint u : unrolled) {
            Literal l = getLiteralRepresentingConstraintIfNeeded(u);
            literals.add(l);
        }
        if (c instanceof And) {
            return getModelWithConstraints().addBoolAnd(literals); // TODO Potentially we do not need to even account for the NOT-case here!
        } else if (c instanceof Or) {
            return getModelWithConstraints().addBoolOr(literals);
        } else if (c instanceof Xor) {
            return getModelWithConstraints().addBoolXor(literals);
        } else {
            throw new NotYetImplementedException(c.toString());
        }
    }

    private com.google.ortools.sat.Constraint transformTwoSidedExpressionConstraint(TwoSidedExpressionConstraint c) {
        LinearArgument lhs = transformNumericalExpression(c.getLhs());
        LinearArgument rhs = transformNumericalExpression(c.getRhs());
        if (c instanceof Eq) {
            return getModelWithConstraints().addEquality(lhs, rhs);
        } else if (c instanceof Lt) {
            return getModelWithConstraints().addLessThan(lhs, rhs);
        } else if (c instanceof Lte) {
            return getModelWithConstraints().addLessOrEqual(lhs, rhs);
        } else {
            throw new NotYetImplementedException(c.toString());
        }
    }

    private com.google.ortools.sat.Constraint transformIn(In in) {
        LinearArgument element = transformNumericalExpression(in.getElement());
        LinearArgument[] vals = Arrays.stream(in.getSet())
                .map(this::transformNumericalExpression)
                .toArray(LinearArgument[]::new);
        throw new NotYetImplementedException(); // TODO
    }

    private com.google.ortools.sat.Constraint transformBoolIte(BoolIte c) {
        return _transformConstraint(
                And.newInstance(
                        Implication.newInstance(c.getCondition(), c.getIfCase()),
                        Implication.newInstance(Not.newInstance(c.getCondition()), c.getElseCase())
                ),
                true
        );
        // TODO Alternative:
//        Literal cond = getLiteralRepresentingConstraintIfNeeded(c.getCondition());
//        Literal ifc = getLiteralRepresentingConstraintIfNeeded(c.getIfCase());
//        Literal elsec = getLiteralRepresentingConstraintIfNeeded(c.getElseCase());
//        Literal b0 = spawnRepresentationBoolVar();
//        Literal b1 = spawnRepresentationBoolVar();
//        getModelWithConstraints().addImplication(cond, ifc).onlyEnforceIf(b0);
//        getModelWithConstraints().addImplication(cond.not(), elsec).onlyEnforceIf(b1);
//        return getModelWithConstraints().addBoolAnd(new Literal[] {b0, b1});
        // TODO Alternative: Seems bad
//        Literal cond = getLiteralRepresentingConstraintIfNeeded(c.getCondition());
//        com.google.ortools.sat.Constraint ifc = _transformConstraint(c.getIfCase(), true);
//        com.google.ortools.sat.Constraint elsec = _transformConstraint(c.getElseCase(), true);
//        ifc.onlyEnforceIf(cond);
//        elsec.onlyEnforceIf(cond.not());
//        return getModelWithConstraints().addBoolOr(new Literal[] {cond, cond.not()});
    }

    private long currentVarId = 0;
    private LinearArgument transformNumericalExpression(NumericalExpression s) {
        if (s instanceof SymSnumber) {
            s = ((SymSnumber) s).getRepresentedExpression();
        }
        if (s instanceof Sbool) {
            Literal result;
            if (s instanceof Sbool.ConcSbool) {
                result = ((Sbool.ConcSbool) s).isTrue() ?
                        getModelWithConstraints().trueLiteral()
                        :
                        getModelWithConstraints().falseLiteral();
            } else {
                assert s instanceof Sbool.SymSboolLeaf;
                result = getModelWithConstraints().newBoolVar(((Sbool.SymSboolLeaf) s).getId());
            }
            constraintToReification.put((Sbool) s, result);
            return result;
        }
        if (s instanceof Sprimitive) {
            LinearArgument result = sprimitiveToVars.get(s);
            if (result != null) {
                return result;
            }

            if (s instanceof ConcSnumber) {
                long val = ((ConcSnumber) s).longVal();
                result = getModelWithConstraints().newConstant(val);
            } else {
                assert s instanceof SymSprimitiveLeaf;
                result = getModelWithConstraints().newIntVar(lb, ub, ((SymSprimitiveLeaf) s).getId());
            }
            sprimitiveToVars.put((Sprimitive) s, result);
            return result;
        } else if (s instanceof Neg) {
            LinearArgument la = transformNumericalExpression(((Neg) s).getWrapped());
            IntVar i = getModelWithConstraints().newIntVar(lb, ub, "neg_"+currentVarId++);
            getModelWithConstraints().addMultiplicationEquality(i, getModelWithConstraints().newConstant(-1), la);
            return i;
        } else if (s instanceof AbstractOperatorNumericalExpression) {
            AbstractOperatorNumericalExpression a = (AbstractOperatorNumericalExpression) s;
            Supplier<IntVar> intVarSupplier = () -> getModelWithConstraints().newIntVar(lb, ub, "i_"+currentVarId++);
            IntVar intVar;
            if (s instanceof Mod) {
                intVar = intVarSupplier.get();
                LinearArgument lhs = transformNumericalExpression(((Mod) s).getExpr0());
                LinearArgument rhs = transformNumericalExpression(((Mod) s).getExpr1());
                getModelWithConstraints().addModuloEquality(intVar, lhs, rhs);
                return intVar;
            } else if (s instanceof Div) {
                intVar = intVarSupplier.get();
                LinearArgument lhs = transformNumericalExpression(((Div) s).getExpr0());
                LinearArgument rhs = transformNumericalExpression(((Div) s).getExpr1());
                getModelWithConstraints().addDivisionEquality(intVar, lhs, rhs);
                return intVar;
            }
            List<NumericalExpression> unrolled = a.unrollSameType();
            LinearArgument[] u = unrolled.stream().map(this::transformNumericalExpression).toArray(LinearArgument[]::new);
            LinearExprBuilder le = LinearExpr.newBuilder();
            if (s instanceof Sum) {
                return le.addSum(u).build();
            } else if (s instanceof Sub) {
                int[] coeffs = new int[u.length];
                coeffs[0] = 1;
                for (int i = 1; i < coeffs.length; i++) {
                    coeffs[i] = -1;
                }
                return le.addWeightedSum(u, coeffs);
            } else if (s instanceof Mul) {
                intVar = intVarSupplier.get();
                getModelWithConstraints().addMultiplicationEquality(intVar, u);
            } else {
                throw new NotYetImplementedException(s.toString());
            }
            return intVar;
        } else if (s instanceof NumericalIte) {
            IntVar intVar = getModelWithConstraints().newIntVar(lb, ub, "i_"+currentVarId++);
            LinearArgument ifc = transformNumericalExpression(((NumericalIte) s).getIfCase());
            LinearArgument elsec = transformNumericalExpression(((NumericalIte) s).getElseCase());
            Literal l = getLiteralRepresentingConstraintIfNeeded(((NumericalIte) s).getCondition());
            com.google.ortools.sat.Constraint ifconEq = getModelWithConstraints().addEquality(intVar, ifc);
            com.google.ortools.sat.Constraint elseconEq = getModelWithConstraints().addEquality(intVar, elsec);
            Literal enforceIf = spawnRepresentationBoolVar();
            Literal enforceElse = spawnRepresentationBoolVar();
            ifconEq.onlyEnforceIf(enforceIf);
            elseconEq.onlyEnforceIf(enforceElse);
            getModelWithConstraints().addImplication(l, enforceIf);
            getModelWithConstraints().addImplication(l.not(), enforceElse);
            return intVar;
        }
        throw new NotYetImplementedException(s.toString());
    }


    SolEnumerator enumerator = null;
    class SolEnumerator extends CpSolverSolutionCallback {
        private List<Map.Entry<String, Substituted>> namedVars;
        private final List<Solution> solutionLabels;
        private final AtomicInteger maxNumberSolutions;

        SolEnumerator(
                List<Map.Entry<String, Substituted>> namedVariables,
                AtomicInteger maxNumberSolutions) {
            this.namedVars = namedVariables;
            this.solutionLabels = new ArrayList<>();
            this.maxNumberSolutions = maxNumberSolutions;
        }

        @Override
        public void onSolutionCallback() {/// TODO Potentially always use SolEnumerator
            Object returnValue = null;
            Map<String, Sprimitive> primitives = new HashMap<>();
            for (Map.Entry<String, Substituted> e : namedVars) {
                if (e.getKey().equals("return")) {
                    returnValue = e.getValue();
                    continue;
                }
                primitives.put(e.getKey(), (Sprimitive) e.getValue());
            }
            Solution newSolution = labelSolution(returnValue, primitives);
            solutionLabels.add(newSolution);
            if (maxNumberSolutions.decrementAndGet() <= 0) {
                stopSearch();
            }
        }
    }

    @Override
    public List<Solution> getUpToNSolutions(final Solution initialSolution, AtomicInteger N, boolean backtrackAfter) {
        if (!backtrackAfter) {
            throw new NotYetImplementedException();
        }
        if (N.get() == 1) {
            N.decrementAndGet();
            return List.of(initialSolution);
        }
        List<Map.Entry<String, Substituted>> namedSprimitives =
                initialSolution.labels.getIdToNamedVar().entrySet().stream()
                        .filter(e -> e.getValue() instanceof Sprimitive || e.getKey().contains("return"))
                        .collect(Collectors.toList());

        // TODO in general, a static (one model) bool-propagation based strategies avoids potential complications, recurrent preprocessing overhead
//        resetModelWithCurrentConstraints();
//        newModelWithCurrentConstraints();
        solver.getParameters().setEnumerateAllSolutions(true);
        enumerator = new SolEnumerator(namedSprimitives, N);
        // TODO Set hints!
        solver.solve(modelWithConstraints, enumerator);
        solver.getParameters().setEnumerateAllSolutions(false);
        List<Solution> solutions = enumerator.solutionLabels;
        enumerator = null;
        return solutions;
    }

    private final Map<Sprimitive, LinearArgument> sprimitiveToVars = new HashMap<>();

    private void resetModelWithCurrentConstraints() {
        modelWithConstraints = null;
        resetLabels();
        // We must clear this since we created variables specific to a CpModel
        sprimitiveToVars.clear();
        constraintToReification.clear();
    }

    private LinearArgument getVarForSprimitive(Sprimitive sprimitive) {
        LinearArgument result;
        if (sprimitive instanceof Sbool) {
            result = constraintToReification.get(sprimitive);
        } else {
            result = sprimitiveToVars.get(sprimitive);
        }
        if (result == null) {
            result = transformNumericalExpression((Snumber) sprimitive);
        }
        return result;
    }

    @Override
    protected SolEnumerator calculateCurrentModel() {
        throw new NotYetImplementedException(); // TODO Not needed here
    }

    @Override
    protected Object labelSymSprimitive(SymSprimitive symSprimitive) {
        getModelWithConstraints();
        LinearArgument var = getVarForSprimitive(symSprimitive);
        if (symSprimitive instanceof Sbool) {
            assert var instanceof Literal;
            return enumerator != null ? enumerator.booleanValue((Literal) var) : solver.booleanValue((Literal) var);
        } else {
            long val = enumerator != null ? enumerator.value(var) : solver.value(var);
            if (symSprimitive instanceof Sint) {
                if (symSprimitive instanceof Sbyte) {
                    return (byte) val;
                } else if (symSprimitive instanceof Sshort) {
                    return (short) val;
                } else {
                    return (int) val;
                }
            } else if (symSprimitive instanceof Sdouble) {
                return (double) val;
            } else if (symSprimitive instanceof Sfloat) {
                return (float) val;
            } else if (symSprimitive instanceof Slong) {
                return val;
            }
        }
        throw new NotYetImplementedException(symSprimitive.toString());
    }

}