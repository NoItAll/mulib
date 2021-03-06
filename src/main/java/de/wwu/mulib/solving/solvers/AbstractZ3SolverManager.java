package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.*;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.primitives.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public abstract class AbstractZ3SolverManager extends AbstractIncrementalEnabledSolverManager<Model> {

    private static final Object syncObject = new Object();
    protected final Solver solver;
    protected final Z3MulibAdapter adapter;

    public AbstractZ3SolverManager(MulibConfig config) {
        super(config);
        synchronized (syncObject) {
            Context context = new Context();
            solver = context.mkSolver();
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
                throw new NotYetImplementedException();
            }
            boolExprStore.put(c, result);
            return result;
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
            if (a instanceof Gt) {
                result = ctx.mkGt((ArithExpr) elhs, (ArithExpr) erhs);
            } else if (a instanceof Eq) {
                result = ctx.mkEq(elhs, erhs);
            } else if (a instanceof Lt) {
                result = ctx.mkLt((ArithExpr) elhs, (ArithExpr) erhs);
            } else if (a instanceof Gte) {
                result = ctx.mkGe((ArithExpr) elhs, (ArithExpr) erhs);
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
                result = numericExpressionsStore.get(n);
                if (result != null) {
                    return result;
                }
                if (n instanceof Sint) {
                    result = transformSintegerNumber((Sint) n);
                } else if (n instanceof Sfpnumber) {
                    result = transformSfpnumber((Sfpnumber) n);
                } else if (n instanceof Slong) {
                    result = _transformSnumber(
                            (Slong) n,
                            () -> n instanceof Slong.ConcSlong,
                            () -> n instanceof Slong.SymSlong,
                            () -> ctx.mkInt(((Slong.ConcSlong) n).longVal()),
                            () -> ctx.mkIntConst(((Slong) n).getInternalName())
                    );
                } else {
                    throw new NotYetImplementedException();
                }
                numericExpressionsStore.put(n, result);
            } else {
                throw new NotYetImplementedException();
            }
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
    }
}
