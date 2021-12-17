package de.wwu.mulib.solving.solvers;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Z3MulibAdapter {
    private final Context ctx;
    private final Map<Sprimitive, Expr> primitiveStore = new WeakHashMap<>();
    private final Map<NumericExpression, Expr> numericExpressionsCache = new WeakHashMap<>();
    private final Map<Constraint, BoolExpr> boolExprCache = new WeakHashMap<>();

    Z3MulibAdapter(Context ctx) {
        this.ctx = ctx;
    }

    Expr getExprForPrimitive(Sprimitive sprimitive) {
        return primitiveStore.get(sprimitive);
    }

    Map<Sprimitive, Expr> getPrimitiveStore() {
        return Collections.unmodifiableMap(primitiveStore);
    }

    Map<NumericExpression, Expr> getNumericExpressionsCache() {
        return Collections.unmodifiableMap(numericExpressionsCache);
    }

    Map<Constraint, BoolExpr> getBoolExprCache() {
        return Collections.unmodifiableMap(boolExprCache);
    }

    BoolExpr transformConstraint(Constraint c) {
        BoolExpr result = boolExprCache.get(c);
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
        boolExprCache.put(c, result);
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
            result = numericExpressionsCache.get(n);
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
            numericExpressionsCache.put(n, result);
        } else if (n instanceof AbstractExpressionWrappingExpression) {
            if (n instanceof Neg) {
                result = ctx.mkMul((ArithExpr) transformNumericExpr(((Neg) n).getWrapped()), ctx.mkInt(-1));
            } else {
                throw new NotYetImplementedException();
            }
        } else if (n instanceof Snumber) {
            result = primitiveStore.get(n);
            if (result != null) {
                return result;
            }

            if (n instanceof Sintegernumber) {
                result = transformSintegerNumber((Sintegernumber) n);
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
            primitiveStore.put((Sprimitive) n, result);
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

    private Expr transformSintegerNumber(Sintegernumber i) {
        if (i instanceof Sint) {
            return _transformSnumber(
                    i,
                    () -> i instanceof Sint.ConcSint,
                    () -> i instanceof Sint.SymSint,
                    () -> ctx.mkInt(((Sint.ConcSint) i).intVal()),
                    () -> ctx.mkIntConst(i.getInternalName())
            );
        } else if (i instanceof Sshort) {
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
        }
        throw new NotYetImplementedException();
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
        BoolExpr result = (BoolExpr) primitiveStore.get(b);
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
        primitiveStore.put(b, result);
        return result;
    }
}
