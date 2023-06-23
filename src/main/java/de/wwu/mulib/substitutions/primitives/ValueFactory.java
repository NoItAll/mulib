package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

import java.util.Map;

public interface ValueFactory {

    static ValueFactory getInstance(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        if (config.CONCOLIC) {
            return ConcolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
        } else {
            return SymbolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
        }
    }

    Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements);

    Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean freeElements);

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

    Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements, boolean canBeNull);

    Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements, boolean canBeNull);

    <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf);

    <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf, boolean canBeNull);

    Sint symSint(SymbolicExecution se);

    Sdouble symSdouble(SymbolicExecution se);

    Sfloat symSfloat(SymbolicExecution se);

    Sbool symSbool(SymbolicExecution se);

    Slong symSlong(SymbolicExecution se);

    Sshort symSshort(SymbolicExecution se);

    Sbyte symSbyte(SymbolicExecution se);

    Schar symSchar(SymbolicExecution se);

    Sint symSint(SymbolicExecution se, Sint lb, Sint ub);

    Sdouble symSdouble(SymbolicExecution se, Sdouble lb, Sdouble ub);

    Sfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub);

    Slong symSlong(SymbolicExecution se, Slong lb, Slong ub);

    Sshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub);

    Sbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub);

    Schar symSchar(SymbolicExecution se, Schar lb, Schar ub);

    Sint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression);

    Sdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression);

    Sfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression);

    Slong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression);

    Sshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression);

    Sbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression);

    Schar wrappingSymSchar(SymbolicExecution se, NumericExpression numericExpression);

    Sbool wrappingSymSbool(SymbolicExecution se, Constraint constraint);

    Sint.SymSint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1);
}
