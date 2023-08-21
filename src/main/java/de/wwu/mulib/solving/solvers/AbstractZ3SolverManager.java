package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.throwables.MisconfigurationException;
import de.wwu.mulib.throwables.MulibRuntimeException;
import de.wwu.mulib.throwables.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Supertype for constraint solver adapters using JNI and the Z3 constraint solver.
 */
public abstract class AbstractZ3SolverManager extends AbstractIncrementalEnabledSolverManager<Model, BoolExpr, ArrayExpr, DatatypeExpr /* TODO Validate*/> {

    private static final Object syncObject = new Object();
    /**
     * The solver
     */
    protected final Solver solver;
    /**
     * The adapter with which to transform constraints
     */
    protected final Z3MulibAdapter adapter;

    /**
     * @param config The configuration
     */
    protected AbstractZ3SolverManager(MulibConfig config) {
        super(config);
        synchronized (syncObject) {
            Context context = new Context();
            if (config.SOLVER_ARGS.isEmpty()) {
                solver = context.mkSolver();
            } else {
                Object arg = config.SOLVER_ARGS.get("z3.config");
                if (arg instanceof String && ((String) arg).equalsIgnoreCase("default")) {
                    // Default tactic from https://github.com/Z3Prover/z3/blob/21e59f7c6e5033006265fc6bc16e2c9f023db0e8/src/tactic/portfolio/default_tactic.cpp
                    // Solvers constructed from tactics are always non-incremental
                    Params contextSimpP = context.mkParams();
                    contextSimpP.add("max_depth", 30);
                    contextSimpP.add("max-steps", 5000000);
                    Params pullIteP = context.mkParams();
                    pullIteP.add("pull_cheap_ite", true);
                    pullIteP.add("push_ite_arith", false);
                    pullIteP.add("local_ctx", true);
                    pullIteP.add("local_ctx_limit", 10000000);
                    pullIteP.add("hoist_ite", true);
                    solver = context.mkSolver(
                            context.usingParams(
                                    context.andThen(
                                            context.mkTactic("simplify"),
                                            context.cond(context.and(context.mkProbe("is-propositional"), context.not(context.mkProbe("produce-proofs"))), context.mkTactic("smtfd"),
                                                    context.cond(context.mkProbe("is-qfbv"), context.mkTactic("qfbv"),
                                                            context.cond(context.mkProbe("is-qfaufbv"), context.mkTactic("qfaufbv"),
                                                                    context.cond(context.mkProbe("is-qflia"), context.mkTactic("qflia"),
                                                                            context.cond(context.mkProbe("is-qfauflia"), context.mkTactic("qfauflia"),
                                                                                    context.cond(context.mkProbe("is-qflra"), context.mkTactic("qflra"),
                                                                                            context.cond(context.mkProbe("is-qfnra"), context.mkTactic("qfnra"),
                                                                                                    context.cond(context.mkProbe("is-qfnia"), context.mkTactic("qfnia"),
                                                                                                            context.cond(context.mkProbe("is-lira"), context.mkTactic("lira"),
                                                                                                                    context.cond(context.mkProbe("is-nra"), context.mkTactic("nra"),
                                                                                                                            context.cond(context.mkProbe("is-qffp"), context.mkTactic("qffp"),
                                                                                                                                    context.cond(context.mkProbe("is-qffplra"), context.mkTactic("qffplra"),
                                                                                                                                            context.andThen(
                                                                                                                                                    context.mkTactic("propagate-values"),
                                                                                                                                                    context.usingParams(context.mkTactic("ctx-simplify"), contextSimpP),
                                                                                                                                                    context.usingParams(context.mkTactic("simplify"), pullIteP),
                                                                                                                                                    context.mkTactic("solve-eqs"),
                                                                                                                                                    context.mkTactic("elim-uncnstr"),
                                                                                                                                                    context.mkTactic("smt"))))))))))))))),
                                    context.mkParams()
                            )
                    );
                } else {
                    try {
                        Function<Context, Tactic> contextFunction = (Function<Context, Tactic>) arg;
                        try {
                            Tactic initTactic = contextFunction.apply(context);
                            solver = context.mkSolver(initTactic);
                        } catch (ClassCastException e) {
                            throw new MisconfigurationException("The configuration should be an instance of Function<Context, Tactic>!");
                        }
                    } catch (Throwable t) {
                        throw new MisconfigurationException("Illegal solver configuration detected: ", t);
                    }
                }
            }
            adapter = new Z3MulibAdapter(config, context);
        }
    }

    @Override
    protected Model calculateCurrentModel() {
        try {
            return solver.getModel();
        } catch (Exception e) {
            throw new MulibRuntimeException(e);
        }
    }

    @Override
    protected final ArrayExpr createCompletelyNewArrayRepresentation(ArrayInitializationConstraint ac) {
        return adapter.newArrayExprFromType(ac.getPartnerClassObjectId(), ac.getValueType());
    }

    @Override
    protected final ArrayExpr createNewArrayRepresentationForStore(ArrayAccessConstraint ac, ArrayExpr oldRepresentation) {
        assert ac.getType() == ArrayAccessConstraint.Type.STORE;
        // We do not need to add anything to the constraint store. This constraint will "be made true" via array-nesting.
        return adapter.newArrayExprFromStore(
                oldRepresentation,
                ac.getIndex(),
                ac.getValue()
        );
    }

    @Override
    protected BoolExpr newArraySelectConstraint(ArrayExpr arrayRepresentation, Sint index, SubstitutedVar value) {
        return adapter.transformSelectConstraint(arrayRepresentation, index, value);
    }

    @Override
    protected BoolExpr transformConstraint(Constraint c) {
        return adapter.transformConstraint(c);
    }

    @Override
    public Object labelSymSprimitive(SymSprimitive var) {
        // This order is important since Sbool is also a NumericExpression (in analogy to Java's bytecode):
        Expr expr = var instanceof Constraint ?
                adapter.getBoolExprForConstraint((Constraint) var)
                :
                adapter.getExprForNumericExpression((NumericExpression) var);
        if (expr == null) {
            if (var instanceof Constraint) {
                expr = adapter.transformConstraint((Constraint) var);
            } else {
                expr = adapter.transformNumericExpr((NumericExpression) var);
            }
        }
        assert expr != null;
        Object result = toPrimitiveOrString(var, getCurrentModel().eval(expr, true));
        assert result != null;
        return result;
    }

    private static Object toPrimitiveOrString(Sprimitive p, Expr e) {
        if (e.isIntNum()) {
            BigInteger bi = ((IntNum) e).getBigInteger();
            if (p instanceof Sint) {
                if (p instanceof Sshort) {
                    return bi.shortValue();
                } else if (p instanceof Sbyte) {
                    return bi.byteValue();
                } else if (p instanceof Schar) {
                    return (char) bi.intValue();
                } else {
                    return bi.intValue();
                }
            } else if (p instanceof Slong) {
                return bi.longValue();
            } else {
                throw new NotYetImplementedException();
            }
        } else if (e.isRatNum()) {
            RatNum ratNum = (RatNum) e;
            if (p instanceof Sdouble) {
                if (ratNum.getNumerator().getBigInteger().doubleValue() == 0.0) {
                    return 0d;
                }
                return ratNum.getNumerator().getBigInteger().doubleValue() / ratNum.getDenominator().getBigInteger().doubleValue();
            } else {
                if (ratNum.getNumerator().getBigInteger().doubleValue() == 0.0) {
                    return 0f;
                }
                return (float) (ratNum.getNumerator().getBigInteger().doubleValue() / ratNum.getDenominator().getBigInteger().doubleValue());
            }
        } else if (e.isBool()) {
            return e.isTrue();
        } else {
            throw new NotYetImplementedException();
        }
    }

    /**
     * An adapter to transform Mulib's types to Z3's types
     */
    protected static final class Z3MulibAdapter {
        final Context ctx;
        private final Map<NumericExpression, Expr> numericExpressionsStore = new HashMap<>();
        // Constraint --> BoolExpr; if booleans are used in {0,1}-encoding (due to them appearing in arithmetic operations)
        // it can also be Expr --> BoolExpr, where Expr is the 0,1-encoding-integer.
        private final Map<Object, BoolExpr> boolExprStore = new HashMap<>();
        private final boolean treatSboolsAsInts;

        Z3MulibAdapter(MulibConfig config, Context ctx) {
            this.treatSboolsAsInts = config.VALS_TREAT_BOOLEANS_AS_INTS;
            this.ctx = ctx;
        }

        Expr getExprForNumericExpression(NumericExpression ne) {
            return numericExpressionsStore.get(ne);
        }

        BoolExpr getBoolExprForConstraint(Constraint c) {
            return boolExprStore.get(c);
        }

        BoolExpr transformConstraint(Constraint c) {
            BoolExpr result = boolExprStore.get(c);
            if (result != null) {
                return result;
            }
            if (c instanceof Sbool) {
                result = transformSbool((Sbool) c);
            } else if (c instanceof Not) {
                result = ctx.mkNot(transformConstraint(((Not) c).getConstraint()));
            } else if (c instanceof AbstractTwoSidedNumericConstraint) {
                result = transformAbstractNumericTwoSidedConstraint((AbstractTwoSidedNumericConstraint) c);
            } else if (c instanceof AbstractTwoSidedConstraint) {
                result = transformAbstractTwoSidedConstraint((AbstractTwoSidedConstraint) c);
            } else if (c instanceof BoolIte) {
                BoolIte ite = (BoolIte) c;
                result = (BoolExpr) ctx.mkITE(
                        transformConstraint(ite.getCondition()),
                        transformConstraint(ite.getIfCase()),
                        transformConstraint(ite.getElseCase())
                );
            } else if (c instanceof In) {
                In in = (In) c;
                Expr eExpr = transformSubstitutedVar(in.getElement());
                result =
                        (BoolExpr) Arrays.stream(in.getSet())
                                .map(this::transformSubstitutedVar)
                                .reduce(ctx.mkBool(false), (d, e) -> ctx.mkOr((BoolExpr) d, ctx.mkEq(eExpr, e)));
            } else {
                throw new NotYetImplementedException(c.toString());
            }
            boolExprStore.put(c, result);
            return result;
        }

        private Expr transformSubstitutedVar(SubstitutedVar sv) {
            if (sv instanceof Sbool) {
                return transformSbool((Sbool) sv);
            } else if (sv instanceof Snumber) {
                return transformSnumber((Snumber) sv);
            } else {
                throw new NotYetImplementedException();
            }
        }

        private BoolExpr transformAbstractTwoSidedConstraint(AbstractTwoSidedConstraint c) {
            BoolExpr lhs = transformConstraint(c.getLhs());
            BoolExpr rhs = transformConstraint(c.getRhs());
            if (c instanceof And) {
                return ctx.mkAnd(lhs, rhs);
            } else if (c instanceof Or) {
                return ctx.mkOr(lhs, rhs);
            } else if (c instanceof Xor) {
                return ctx.mkXor(lhs, rhs);
            } else if (c instanceof Implication) {
                return ctx.mkImplies(lhs, rhs);
            } else if (c instanceof Equivalence) {
                return ctx.mkIff(lhs, rhs);
            } else {
                throw new NotYetImplementedException();
            }
        }

        private BoolExpr transformAbstractNumericTwoSidedConstraint(AbstractTwoSidedNumericConstraint a) {
            NumericExpression lhs = a.getLhs();
            NumericExpression rhs = a.getRhs();
            Expr elhs = transformNumericExpr(lhs);
            Expr erhs = transformNumericExpr(rhs);
            BoolExpr result;
            if (a instanceof Eq) {
                result = ctx.mkEq(elhs, erhs);
            } else if (a instanceof Lt) {
                result = ctx.mkLt((ArithExpr) elhs, (ArithExpr) erhs);
            } else if (a instanceof Lte) {
                result = ctx.mkLe((ArithExpr) elhs, (ArithExpr) erhs);
            } else {
                throw new NotYetImplementedException();
            }
            return result;
        }

        /**
         * Transforms a numeric expression into an Expr object of Z3
         * @param n The numeric expression
         * @return The Z3 representation of n
         */
        public Expr transformNumericExpr(NumericExpression n) {
            Expr result;
            if (n instanceof AbstractOperatorNumericExpression) {
                result = numericExpressionsStore.get(n);
                if (result != null) {
                    return result;
                }

                AbstractOperatorNumericExpression o = (AbstractOperatorNumericExpression) n;
                NumericExpression lhs = o.getExpr0();
                NumericExpression rhs = o.getExpr1();
                Expr elhs = transformNumericExpr(lhs);
                Expr erhs = transformNumericExpr(rhs);
                if (n instanceof Sum) {
                    result = ctx.mkAdd((ArithExpr) elhs, (ArithExpr) erhs);
                } else if (n instanceof Mul) {
                    result = ctx.mkMul((ArithExpr) elhs, (ArithExpr) erhs);
                } else if (n instanceof Sub) {
                    result = ctx.mkSub((ArithExpr) elhs, (ArithExpr) erhs);
                } else if (n instanceof Div) {
                    result = ctx.mkDiv((ArithExpr) elhs, (ArithExpr) erhs);
                } else if (n instanceof Mod) {
                    result = ctx.mkMod((IntExpr) elhs, (IntExpr) erhs);
                } else {
                    // Bit-wise operations
                    IntExpr ilhs = (IntExpr) elhs;
                    IntExpr irhs = (IntExpr) erhs;
                    assert lhs instanceof Slong || lhs instanceof Sint;
                    assert rhs instanceof Slong || rhs instanceof Sint;
                    boolean lhsIsLong = lhs instanceof Slong;
                    boolean rhsIsLong = rhs instanceof Slong;
                    assert lhsIsLong == rhsIsLong;
                    BitVecExpr bvlhs = ctx.mkInt2BV(lhsIsLong ? 64 : 32, ilhs);
                    BitVecExpr bvrhs = ctx.mkInt2BV(rhsIsLong ? 64 : 32, irhs);
                    if (n instanceof NumericAnd) {
                        result = ctx.mkBVAND(bvlhs, bvrhs);
                    } else if (n instanceof NumericOr) {
                        result = ctx.mkBVOR(bvlhs, bvrhs);
                    } else if (n instanceof NumericXor) {
                        result = ctx.mkBVXOR(bvlhs, bvrhs);
                    } else if (n instanceof ShiftLeft) {
                        result = ctx.mkBVSHL(bvlhs, bvrhs);
                    } else if (n instanceof ShiftRight) {
                        result = ctx.mkBVASHR(bvlhs, bvrhs);
                    } else if (n instanceof LogicalShiftRight) {
                        result = ctx.mkBVLSHR(bvlhs, bvrhs);
                    } else {
                        throw new NotYetImplementedException();
                    }
                    result = ctx.mkBV2Int((BitVecExpr) result, true);
                }
                numericExpressionsStore.put(n, result);
            } else if (n instanceof Neg) {
                result = ctx.mkUnaryMinus((ArithExpr) transformNumericExpr(((Neg) n).getWrapped()));
                numericExpressionsStore.put(n, result);
            } else if (n instanceof NumericIte) {
                NumericIte ite = (NumericIte) n;
                result = ctx.mkITE(
                        transformConstraint(ite.getCondition()),
                        transformNumericExpr(ite.getIfCase()),
                        transformNumericExpr(ite.getElseCase())
                );
                numericExpressionsStore.put(n, result);
            } else if (n instanceof Snumber) {
                result = transformSnumber((Snumber) n);
            } else {
                throw new NotYetImplementedException(String.valueOf(n));
            }
            return result;
        }

        private Expr transformSnumber(Snumber n) {
            Expr result = numericExpressionsStore.get(n);
            if (result != null) {
                return result;
            }
            if (n instanceof Sint) {
                result = transformSintegerNumber((Sint) n);
            } else if (n instanceof Sfpnumber) {
                result = transformSfpnumber((Sfpnumber) n);
            } else if (n instanceof Slong) {
                result = _transformSnumber(
                        n,
                        () -> n instanceof Slong.ConcSlong,
                        () -> n instanceof Slong.SymSlong,
                        () -> ctx.mkInt(((Slong.ConcSlong) n).longVal()),
                        () -> ctx.mkIntConst(((Slong.SymSlongLeaf) n).getId())
                );
            } else {
                throw new NotYetImplementedException();
            }
            numericExpressionsStore.put(n, result);
            return result;
        }

        private Expr _transformSnumber(
                Snumber n,
                Supplier<Boolean> isConc,
                Supplier<Boolean> isSym,
                Supplier<Expr> makeConc,
                Supplier<Expr> makeSym) {
            if (isConc.get()) {
                return makeConc.get();
            } else if (isSym.get()) {
                SymNumericExpressionSprimitive _i = (SymNumericExpressionSprimitive) n;
                if (_i instanceof SymSprimitiveLeaf) {
                    return makeSym.get();
                } else {
                    assert _i.getRepresentedExpression() != _i;
                    return transformNumericExpr(_i.getRepresentedExpression());
                }
            } else {
                throw new NotYetImplementedException();
            }
        }

        private Expr transformSintegerNumber(Sint i) {
            Supplier<Expr> makeSym;
            if (i instanceof Sbool) {
                if (!treatSboolsAsInts) {
                    throw new MulibRuntimeException("Must not occur");
                }
                makeSym = () -> ctx.mkIntConst(((SymSprimitiveLeaf) i).getId() + "_int");
            } else {
                makeSym = () -> ctx.mkIntConst(((SymSprimitiveLeaf) i).getId());
            }
            return _transformSnumber(
                    i,
                    () -> i instanceof ConcSnumber,
                    () -> i instanceof SymSprimitive,
                    () -> ctx.mkInt(((ConcSnumber) i).intVal()),
                    makeSym
            );
        }


        private Expr transformSfpnumber(Sfpnumber f) {
            if (f instanceof Sdouble) {
                return _transformSnumber(
                        f,
                        () -> f instanceof Sdouble.ConcSdouble,
                        () -> f instanceof Sdouble.SymSdouble,
                        () -> ctx.mkReal(String.valueOf(((Sdouble.ConcSdouble) f).doubleVal())),
                        () -> ctx.mkRealConst(((Sdouble.SymSdoubleLeaf) f).getId())
                );
            } else if (f instanceof Sfloat) {
                return _transformSnumber(
                        f,
                        () -> f instanceof Sfloat.ConcSfloat,
                        () -> f instanceof Sfloat.SymSfloat,
                        () -> ctx.mkReal(String.valueOf(((Sfloat.ConcSfloat) f).doubleVal())),
                        () -> ctx.mkRealConst(((Sfloat.SymSfloatLeaf) f).getId())
                );
            }
            throw new NotYetImplementedException();
        }

        private BoolExpr transformSbool(Sbool b) {
            BoolExpr result = boolExprStore.get(b);
            if (result != null) {
                return result;
            }
            if (b instanceof Sbool.ConcSbool) {
                result = ctx.mkBool(((Sbool.ConcSbool) b).isTrue());
            } else {
                Constraint representedConstraint = ((Sbool.SymSbool) b).getRepresentedConstraint();
                if (representedConstraint instanceof Sbool.SymSboolLeaf) { // Only represents itself
                    result = ctx.mkBoolConst(((Sbool.SymSboolLeaf) b).getId());
                } else {
                    result = transformConstraint(representedConstraint);
                }
            }
            boolExprStore.put(b, result);
            return result;
        }

        /**
         * Creates a new Z3 representation of the array with a given identifier and a type
         * @param arrayId The identifier
         * @param type The type
         * @return The Z3 representation of the array
         */
        public ArrayExpr newArrayExprFromType(Sint arrayId, Class<?> type) {
            Sort arraySort;
            if (Sprimitive.class.isAssignableFrom(type)) {
                if (Sbool.class.isAssignableFrom(type)) {
                    arraySort = ctx.mkBoolSort();
                } else if (Sint.class.isAssignableFrom(type) || Slong.class.isAssignableFrom(type)) {
                    arraySort = ctx.mkIntSort();
                } else if (Sfpnumber.class.isAssignableFrom(type)) {
                    arraySort = ctx.mkRealSort();
                } else {
                    throw new NotYetImplementedException();
                }
            } else {
                throw new NotYetImplementedException();
            }
            try {
                return ctx.mkArrayConst(
                        // In the mutable case, multiple array expressions might represent the array
                        "Sarray" + arrayId,
                        // The array is accessed via an int
                        ctx.mkIntSort(),
                        arraySort
                );
            } catch (Throwable t) {
                throw new MulibRuntimeException(t);
            }
        }

        /**
         * Constructs a new array expression nesting the old expression
         * @param oldRepresentation The old representation
         * @param index The index
         * @param value The value to store
         * @return The new representation also expressing the store
         */
        public ArrayExpr newArrayExprFromStore(ArrayExpr oldRepresentation, Sint index, SubstitutedVar value) {
            Expr val = transformSubstitutedVar(value);
            Expr i = transformSintegerNumber(index);
            return ctx.mkStore(oldRepresentation, i, val);
        }

        /**
         * Creates a constraint ensuring that the value is selected from the array at position index
         * @param arrayExpr The array representation
         * @param index The index
         * @param value The value
         * @return Z3's representation of the constraint
         */
        public BoolExpr transformSelectConstraint(ArrayExpr arrayExpr, Sint index, SubstitutedVar value) {
            Expr selectExpr = ctx.mkSelect(arrayExpr, transformSintegerNumber(index));
            return ctx.mkEq(selectExpr, transformSubstitutedVar(value));
        }
    }

    @Override
    protected void solverSpecificShutdown() {
        adapter.boolExprStore.clear();
        adapter.numericExpressionsStore.clear();
        try {
            adapter.ctx.close();
        } catch (Z3Exception e) {}
    }
}
