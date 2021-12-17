package de.wwu.mulib.solving.solvers;

import de.wwu.mulib.constraints.*;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.expressions.*;
import de.wwu.mulib.substitutions.primitives.*;
import org.sosy_lab.java_smt.api.*;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class JavaSMTMulibAdapter {
    private final Map<Sprimitive, Formula> primitiveStore = new WeakHashMap<>();
    private final BooleanFormulaManager booleanFormulaManager;
    private final IntegerFormulaManager integerFormulaManager;
    private final RationalFormulaManager rationalFormulaManager;

    JavaSMTMulibAdapter(SolverContext context) {
        FormulaManager formulaManager = context.getFormulaManager();
        booleanFormulaManager = formulaManager.getBooleanFormulaManager();
        integerFormulaManager = formulaManager.getIntegerFormulaManager();
        rationalFormulaManager = formulaManager.getRationalFormulaManager();
    }

    Formula getFormulaForPrimitive(Sprimitive sprimitive) {
        return primitiveStore.get(sprimitive);
    }

    Map<Sprimitive, Formula> getPrimitiveStore() {
        return Collections.unmodifiableMap(primitiveStore);
    }

    BooleanFormula transformConstraint(Constraint c) {
        BooleanFormula result;
        if (c instanceof Sbool) {
            result = transformSbool((Sbool) c);
        } else if (c instanceof Not) {
            result = booleanFormulaManager.not(transformConstraint(((Not) c).getConstraint()));
        } else if (c instanceof AbstractTwoSidedNumericConstraint) {
            result = transformAbstractNumericTwoSidedConstraint((AbstractTwoSidedNumericConstraint) c);
        } else if (c instanceof AbstractTwoSidedConstraint) {
            result = transformAbstractTwoSidedConstraint((AbstractTwoSidedConstraint) c);
        } else {
            throw new NotYetImplementedException();
        }
        return result;
    }

    private BooleanFormula transformAbstractTwoSidedConstraint(AbstractTwoSidedConstraint c) {
        if (c instanceof And) {
            return booleanFormulaManager.and(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
        } else if (c instanceof Or) {
            return booleanFormulaManager.or(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
        } else if (c instanceof Xor) {
            return booleanFormulaManager.xor(transformConstraint(c.getLhs()), transformConstraint(c.getRhs()));
        } else {
            throw new NotYetImplementedException();
        }
    }

    private BooleanFormula transformAbstractNumericTwoSidedConstraint(AbstractTwoSidedNumericConstraint a) {
        BooleanFormula result;
        NumeralFormula lhs = transformNumeral(a.getLhs());
        NumeralFormula rhs = transformNumeral(a.getRhs());

        BiFunction<NumeralFormula, NumeralFormula, BooleanFormula> fpCase;
        BiFunction<NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula, BooleanFormula> integerCase;
        if (a instanceof Gt) {
            fpCase = rationalFormulaManager::greaterThan;
            integerCase = integerFormulaManager::greaterThan;
        } else if (a instanceof Eq) {
            fpCase = rationalFormulaManager::equal;
            integerCase = integerFormulaManager::equal;
        } else if (a instanceof Lt) {
            fpCase = rationalFormulaManager::lessThan;
            integerCase = integerFormulaManager::lessThan;
        } else if (a instanceof Gte) {
            fpCase = rationalFormulaManager::greaterOrEquals;
            integerCase = integerFormulaManager::greaterOrEquals;
        } else if (a instanceof Lte) {
            fpCase = rationalFormulaManager::lessOrEquals;
            integerCase = integerFormulaManager::lessOrEquals;
        } else {
            throw new NotYetImplementedException();
        }
        if (a.getLhs().isFp() || a.getRhs().isFp()) {
            result = fpCase.apply(lhs, rhs);
        } else {
            result = integerCase.apply((NumeralFormula.IntegerFormula) lhs, (NumeralFormula.IntegerFormula) rhs);
        }

        return result;
    }

    public NumeralFormula transformNumeral(NumericExpression n) {
        NumeralFormula result;

        if (n instanceof AbstractOperatorNumericExpression) {
            AbstractOperatorNumericExpression o = (AbstractOperatorNumericExpression) n;
            NumericExpression lhs = o.getExpr0();
            NumericExpression rhs = o.getExpr1();
            NumeralFormula elhs = transformNumeral(lhs);
            NumeralFormula erhs = transformNumeral(rhs);
            BiFunction<NumeralFormula, NumeralFormula, NumeralFormula> fpCase;
            BiFunction<NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula, NumeralFormula> integerCase;
            if (n instanceof Sum) {
                fpCase = rationalFormulaManager::add;
                integerCase = integerFormulaManager::add;
            } else if (n instanceof Mul) {
                fpCase = rationalFormulaManager::multiply;
                integerCase = integerFormulaManager::multiply;
            } else if (n instanceof Sub) {
                fpCase = rationalFormulaManager::subtract;
                integerCase = integerFormulaManager::subtract;
            } else {
                throw new NotYetImplementedException();
            }
            if (n.isFp()) {
                result = fpCase.apply(elhs, erhs);
            } else {
                result = integerCase.apply((NumeralFormula.IntegerFormula) elhs, (NumeralFormula.IntegerFormula) erhs);
            }
        } else if (n instanceof AbstractExpressionWrappingExpression) {
            if (n instanceof Neg) {
                if (n.isFp()) {
                    result = rationalFormulaManager.negate(transformNumeral(((Neg) n).getWrapped()));
                } else {
                    result = integerFormulaManager.negate((NumeralFormula.IntegerFormula) transformNumeral(((Neg) n).getWrapped()));
                }
            } else {
                throw new NotYetImplementedException();
            }
        } else {

            if (n instanceof Sintegernumber) {
                result = transformSintegerNumber((Sintegernumber) n);
            } else if (n instanceof Sfpnumber) {
                result = transformSfpnumber((Sfpnumber) n);
            } else if (n instanceof Slong) {
            result = _transformSnumber(
                    (Slong) n,
                    () -> n instanceof Slong.ConcSlong,
                    () -> n instanceof Slong.SymSlong,
                    () -> integerFormulaManager.makeNumber(((Slong.ConcSlong) n).longVal()),
                    () -> integerFormulaManager.makeVariable(((Slong.SymSlong) n).getInternalName())
            );
        } else {
                throw new NotYetImplementedException();
            }
        }

        return result;
    }

    private NumeralFormula _transformSnumber(
            Snumber n,
            Supplier<Boolean> isConc,
            Supplier<Boolean> isSym,
            Supplier<NumeralFormula> makeConc,
            Supplier<NumeralFormula> makeSym) {
        if (isConc.get()) {
            return makeConc.get();
        } else if (isSym.get()) {
            SymNumericExpressionSprimitive _i = (SymNumericExpressionSprimitive) n;
            if (_i.getRepresentedExpression() == _i) {
                return makeSym.get();
            } else {
                return transformNumeral(_i.getRepresentedExpression());
            }
        } else {
            throw new NotYetImplementedException();
        }
    }

    private NumeralFormula transformSfpnumber(Sfpnumber f) {
        NumeralFormula result = (NumeralFormula) primitiveStore.get(f);
        if (result != null) {
            return result;
        }
        if (f instanceof Sdouble) {
            result = _transformSnumber(
                    f,
                    () -> f instanceof Sdouble.ConcSdouble,
                    () -> f instanceof Sdouble.SymSdouble,
                    () -> rationalFormulaManager.makeNumber(((Sdouble.ConcSdouble) f).doubleVal()),
                    () -> rationalFormulaManager.makeVariable(f.getInternalName())
            );
        } else if (f instanceof Sfloat) {
            result = _transformSnumber(
                    f,
                    () -> f instanceof Sfloat.ConcSfloat,
                    () -> f instanceof Sfloat.SymSfloat,
                    () -> rationalFormulaManager.makeNumber(((Sfloat.ConcSfloat) f).floatVal()),
                    () -> rationalFormulaManager.makeVariable(f.getInternalName())
            );
        }
        primitiveStore.put(f, result);
        return result;
    }

    private NumeralFormula transformSintegerNumber(Sintegernumber i) {
        NumeralFormula result = (NumeralFormula.IntegerFormula) primitiveStore.get(i);
        if (result != null) {
            return result;
        }
        if (i instanceof Sint) {
            result = _transformSnumber(
                    i,
                    () -> i instanceof Sint.ConcSint,
                    () -> i instanceof Sint.SymSint,
                    () -> integerFormulaManager.makeNumber(((Sint.ConcSint) i).intVal()),
                    () -> integerFormulaManager.makeVariable(i.getInternalName())
            );
        } else if (i instanceof Sshort) {
            result = _transformSnumber(
                    i,
                    () -> i instanceof Sshort.ConcSshort,
                    () -> i instanceof Sshort.SymSshort,
                    () -> integerFormulaManager.makeNumber(((Sshort.ConcSshort) i).shortVal()),
                    () -> integerFormulaManager.makeVariable(i.getInternalName())
            );
        } else if (i instanceof Sbyte) {
            result = _transformSnumber(
                    i,
                    () -> i instanceof Sbyte.ConcSbyte,
                    () -> i instanceof Sbyte.SymSbyte,
                    () -> integerFormulaManager.makeNumber(((Sbyte.ConcSbyte) i).intVal()),
                    () -> integerFormulaManager.makeVariable(i.getInternalName())
            );
        } else {
            throw new NotYetImplementedException();
        }
        primitiveStore.put(i, result);
        return result;
    }

    private BooleanFormula transformSbool(Sbool b) {
        BooleanFormula result = (BooleanFormula) primitiveStore.get(b);
        if (result != null) {
            return result;
        }
        if (b instanceof Sbool.ConcSbool) {
            result = booleanFormulaManager.makeBoolean(((Sbool.ConcSbool) b).isTrue());
        } else if (b instanceof Sbool.SymSbool) {
            Constraint representedConstraint = ((Sbool.SymSbool) b).getRepresentedConstraint();
            if (representedConstraint == b) {
                result = booleanFormulaManager.makeVariable(b.getInternalName());
            } else {
                result = transformConstraint(representedConstraint);
            }
        } else {
            throw new NotYetImplementedException();
        }
        primitiveStore.put(b, result);
        return result;
    }
}