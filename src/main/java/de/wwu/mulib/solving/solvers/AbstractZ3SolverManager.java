package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MisconfigurationException;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractZ3SolverManager extends AbstractIncrementalEnabledSolverManager<Model, BoolExpr, ArrayExpr> {

    private static final Object syncObject = new Object();
    protected final Solver solver;
    protected final Z3MulibAdapter adapter;

    public AbstractZ3SolverManager(MulibConfig config) {
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
                    assert arg instanceof Function;
                    try {
                        Function<Context, Tactic> contextFunction = (Function<Context, Tactic>) arg;
                        Tactic initTactic = contextFunction.apply(context);
                        solver = context.mkSolver(initTactic);
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
    protected final ArrayExpr createCompletelyNewArrayRepresentation(ArrayConstraint ac) {
        return adapter.newArrayExprFromValue(ac.getArrayId(), ac.getValue());
    }

    @Override
    protected final ArrayExpr createNewArrayRepresentationForStore(ArrayConstraint ac, ArrayExpr oldRepresentation) {
        assert ac.getType() == ArrayConstraint.Type.STORE;
        // We do not need to add anything to the constraint store. This constraint will "be made true" via array-nesting.
        return adapter.newArrayExprFromStore(
                oldRepresentation,
                ac.getIndex(),
                ac.getValue()
        );
    }

    @Override
    protected final void addArraySelectConstraint(ArrayExpr arrayRepresentation, Sint index, SubstitutedVar value) {
        BoolExpr selectConstraint = newArraySelectConstraint(arrayRepresentation, index, value);
        solver.add(selectConstraint);
    }

    protected BoolExpr newArraySelectConstraint(ArrayExpr arrayRepresentation, Sint index, SubstitutedVar value) {
        return adapter.transformSelectConstraint(arrayRepresentation, index, value);
    }

    @Override
    protected BoolExpr transformConstraint(Constraint c) {
        return adapter.transformConstraint(c);
    }

    @Override
    public Object getLabel(Sprimitive var) {
        if (!isSatisfiable()) {
            throw new MulibRuntimeException("Must be satisfiable.");
        }
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

    protected static final class Z3MulibAdapter {
        private final Context ctx;
        private final Map<NumericExpression, Expr> numericExpressionsStore = new WeakHashMap<>();
        // Constraint --> BoolExpr; if booleans are used in {0,1}-encoding (due to them appearing in arithmetic operations)
        // it can also be Expr --> BoolExpr, where Expr is the 0,1-encoding-integer.
        private final Map<Object, BoolExpr> boolExprStore = new WeakHashMap<>();
        private final boolean treatSboolsAsInts;

        Z3MulibAdapter(MulibConfig config, Context ctx) {
            this.treatSboolsAsInts = config.TREAT_BOOLEANS_AS_INTS;
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
            if (c instanceof And) {
                return ctx.mkAnd(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
            } else if (c instanceof Or) {
                return ctx.mkOr(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
            } else if (c instanceof Xor) {
                return ctx.mkXor(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
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
                } else {
                    throw new NotYetImplementedException();
                }
                numericExpressionsStore.put(n, result);
            } else if (n instanceof AbstractExpressionWrappingExpression) {
                if (n instanceof Neg) {
                    result = ctx.mkMul((ArithExpr) transformNumericExpr(((Neg) n).getWrapped()), ctx.mkInt(-1));
                } else {
                    throw new NotYetImplementedException();
                }
            }
            else if (n instanceof Snumber) {
                result = transformSnumber((Snumber) n);
            } else {
                throw new NotYetImplementedException();
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
                        () -> ctx.mkIntConst((n).getInternalName())
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
                if (_i.getRepresentedExpression() == _i) {
                    return makeSym.get();
                } else {
                    return transformNumericExpr(_i.getRepresentedExpression());
                }
            } else {
                throw new NotYetImplementedException();
            }
        }

        private Expr transformSintegerNumber(Sint i) {
            if (i instanceof Sshort) {
                return _transformSnumber(
                        i,
                        () -> i instanceof Sshort.ConcSshort,
                        () -> i instanceof Sshort.SymSshort,
                        () -> ctx.mkInt(((Sshort.ConcSshort) i).shortVal()),
                        () -> ctx.mkIntConst(i.getInternalName())
                );
            } else if (i instanceof Sbyte) {
                return _transformSnumber(
                        i,
                        () -> i instanceof Sbyte.ConcSbyte,
                        () -> i instanceof Sbyte.SymSbyte,
                        () -> ctx.mkInt(((Sbyte.ConcSbyte) i).byteVal()),
                        () -> ctx.mkIntConst(i.getInternalName())
                );
            } else if (i instanceof Sbool) {
                if (!treatSboolsAsInts) {
                    throw new MulibRuntimeException("Must not occur.");
                }
                return _transformSnumber(
                        i,
                        () -> i instanceof Sbool.ConcSbool,
                        () -> i instanceof Sbool.SymSbool,
                        () -> ctx.mkInt(((Sbool.ConcSbool) i).intVal()),
                        () -> ctx.mkIntConst(i.getInternalName())
                );
            } else {
                return _transformSnumber(
                        i,
                        () -> i instanceof Sint.ConcSint,
                        () -> i instanceof Sint.SymSint,
                        () -> ctx.mkInt(((Sint.ConcSint) i).intVal()),
                        () -> ctx.mkIntConst(i.getInternalName())
                );
            }
        }


        private Expr transformSfpnumber(Sfpnumber f) {
            if (f instanceof Sdouble) {
                return _transformSnumber(
                        f,
                        () -> f instanceof Sdouble.ConcSdouble,
                        () -> f instanceof Sdouble.SymSdouble,
                        () -> ctx.mkReal(String.valueOf(((Sdouble.ConcSdouble) f).doubleVal())),
                        () -> ctx.mkRealConst(f.getInternalName())
                );
            } else if (f instanceof Sfloat) {
                return _transformSnumber(
                        f,
                        () -> f instanceof Sfloat.ConcSfloat,
                        () -> f instanceof Sfloat.SymSfloat,
                        () -> ctx.mkReal(String.valueOf(((Sfloat.ConcSfloat) f).doubleVal())),
                        () -> ctx.mkRealConst(f.getInternalName())
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
                if (representedConstraint == b) { // Only represents itself
                    result = ctx.mkBoolConst(b.getInternalName());
                } else {
                    result = transformConstraint(representedConstraint);
                }
            }
            boolExprStore.put(b, result);
            return result;
        }

        public ArrayExpr newArrayExprFromValue(long arrayId, SubstitutedVar value) {
            Sort arraySort;
            if (value instanceof Sprimitive) {
                if (value instanceof Sbool) {
                    arraySort = ctx.mkBoolSort();
                } else if (value instanceof Sint || value instanceof Slong) {
                    arraySort = ctx.mkIntSort();
                } else if (value instanceof Sfpnumber) {
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

        public ArrayExpr newArrayExprFromStore(ArrayExpr oldRepresentation, Sint index, SubstitutedVar value) {
            Expr val = transformSubstitutedVar(value);
            Expr i = transformSintegerNumber(index);
            return ctx.mkStore(oldRepresentation, i, val);
        }

        public BoolExpr transformSelectConstraint(ArrayExpr arrayExpr, Sint index, SubstitutedVar value) {
            Expr selectExpr = ctx.mkSelect(arrayExpr, transformSintegerNumber(index));
            return ctx.mkEq(selectExpr, transformSubstitutedVar(value));
        }
    }
}
