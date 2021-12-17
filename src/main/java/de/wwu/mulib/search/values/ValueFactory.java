package de.wwu.mulib.search.values;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

public interface ValueFactory {

    Sint.SymSint symSint(SymbolicExecution se);

    Sdouble.SymSdouble symSdouble(SymbolicExecution se);

    Sfloat.SymSfloat symSfloat(SymbolicExecution se);

    Sbool.SymSbool symSbool(SymbolicExecution se);

    Slong.SymSlong symSlong(SymbolicExecution se);

    Sshort.SymSshort symSshort(SymbolicExecution se);

    Sbyte.SymSbyte symSbyte(SymbolicExecution se);

    Sint.SymSint wrappingSymSint(NumericExpression numericExpression);

    Sdouble.SymSdouble wrappingSymSdouble(NumericExpression numericExpression);

    Sfloat.SymSfloat wrappingSymSfloat(NumericExpression numericExpression);

    Slong.SymSlong wrappingSymSlong(NumericExpression numericExpression);

    Sshort.SymSshort wrappingSymSshort(NumericExpression numericExpression);

    Sbyte.SymSbyte wrappingSymSbyte(NumericExpression numericExpression);

    Sbool wrappingSymSbool(Constraint constraint);

    Sint.ConcSint concSint(int i);

    Sdouble.ConcSdouble concSdouble(double d);

    Sfloat.ConcSfloat concSfloat(float f);

    Sbool.ConcSbool concSbool(boolean b);

    Slong.ConcSlong concSlong(long l);

    Sshort.ConcSshort concSshort(short s);

    Sbyte.ConcSbyte concSbyte(byte b);
}
