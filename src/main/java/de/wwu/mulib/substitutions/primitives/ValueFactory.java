package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

public interface ValueFactory {

    static ValueFactory getInstance(MulibConfig config) {
        if (config.CONCOLIC) {
            return ConcolicValueFactory.getInstance(config);
        } else {
            return SymbolicValueFactory.getInstance(config);
        }
    }

    Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements);

    Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements);

    Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz);

    Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements, boolean canBeNull);

    Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements, boolean canBeNull);

    <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf);

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

    Sint.SymSint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1);

    Sint.ConcSint concSint(int i);

    Sdouble.ConcSdouble concSdouble(double d);

    Sfloat.ConcSfloat concSfloat(float f);

    Sbool.ConcSbool concSbool(boolean b);

    Slong.ConcSlong concSlong(long l);

    Sshort.ConcSshort concSshort(short s);

    Sbyte.ConcSbyte concSbyte(byte b);
}
