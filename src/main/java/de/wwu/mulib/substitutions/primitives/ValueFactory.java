package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;

public interface ValueFactory {

    Sint symSint(SymbolicExecution se);

    Sdouble symSdouble(SymbolicExecution se);

    Sfloat symSfloat(SymbolicExecution se);

    Sbool symSbool(SymbolicExecution se);

    Slong symSlong(SymbolicExecution se);

    Sshort symSshort(SymbolicExecution se);

    Sbyte symSbyte(SymbolicExecution se);

    Sint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression);

    Sdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression);

    Sfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression);

    Slong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression);

    Sshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression);

    Sbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression);

    Sbool wrappingSymSbool(SymbolicExecution se, Constraint constraint);

    Sint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1);

    Sint concSint(int i);

    Sdouble.ConcSdouble concSdouble(double d);

    Sfloat.ConcSfloat concSfloat(float f);

    Sbool.ConcSbool concSbool(boolean b);

    Slong.ConcSlong concSlong(long l);

    Sshort.ConcSshort concSshort(short s);

    Sbyte.ConcSbyte concSbyte(byte b);
}
