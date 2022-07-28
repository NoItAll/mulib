package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueTransformer;
import soot.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class SootMulibClassesAndMethods {
    /* SPECIAL CLASSES */
    public final SootClass SC_CLASS;
    public final SootClass SC_INTEGER;
    public final SootClass SC_LONG;
    public final SootClass SC_DOUBLE;
    public final SootClass SC_FLOAT;
    public final SootClass SC_SHORT;
    public final SootClass SC_BYTE;
    public final SootClass SC_BOOLEAN;
    public final SootClass SC_FIELD;
    public final SootClass SC_EXCEPTION;
    public final SootClass SC_MULIB;
    public final SootClass SC_MULIB_RUNTIME_EXCEPTION;
    public final SootClass SC_SINT;
    public final SootClass SC_SLONG;
    public final SootClass SC_SDOUBLE;
    public final SootClass SC_SFLOAT;
    public final SootClass SC_SSHORT;
    public final SootClass SC_SBYTE;
    public final SootClass SC_SBOOL;
    public final SootClass SC_CONCSINT;
    public final SootClass SC_CONCSLONG;
    public final SootClass SC_CONCSDOUBLE;
    public final SootClass SC_CONCSFLOAT;
    public final SootClass SC_CONCSSHORT;
    public final SootClass SC_CONCSBYTE;
    public final SootClass SC_CONCSBOOL;
    public final SootClass SC_PARTNERCLASS;
    public final SootClass SC_SINTSARRAY;
    public final SootClass SC_SDOUBLESARRAY;
    public final SootClass SC_SFLOATSARRAY;
    public final SootClass SC_SLONGSARRAY;
    public final SootClass SC_SSHORTSARRAY;
    public final SootClass SC_SBYTESARRAY;
    public final SootClass SC_SBOOLSARRAY;
    public final SootClass SC_SARRAYSARRAY;
    public final SootClass SC_SARRAY;
    public final SootClass SC_PARTNERCLASSSARRAY;
    public final SootClass SC_SE;
    public final SootClass SC_MULIB_VALUE_TRANSFORMER;
    public final SootClass SC_SOLVER_MANAGER;
    public final SootClass SC_SPRIMITIVE;
    public final SootClass SC_SYM_SPRIMITIVE;
    public final SootClass SC_SUBSTITUTED_VAR;

    /* SPECIAL TYPES */
    public final RefType TYPE_MULIB_RUNTIME_EXCEPTION;
    public final RefType TYPE_SINT;
    public final RefType TYPE_SLONG;
    public final RefType TYPE_SDOUBLE;
    public final RefType TYPE_SFLOAT;
    public final RefType TYPE_SSHORT;
    public final RefType TYPE_SBYTE;
    public final RefType TYPE_SBOOL;
    public final RefType TYPE_CONCSINT;
    public final RefType TYPE_CONCSLONG;
    public final RefType TYPE_CONCSDOUBLE;
    public final RefType TYPE_CONCSFLOAT;
    public final RefType TYPE_CONCSSHORT;
    public final RefType TYPE_CONCSBYTE;
    public final RefType TYPE_CONCSBOOL;
    public final RefType TYPE_PARTNERCLASS;
    public final RefType TYPE_SARRAYSARRAY;
    public final RefType TYPE_SINTSARRAY;
    public final RefType TYPE_SDOUBLESARRAY;
    public final RefType TYPE_SFLOATSARRAY;
    public final RefType TYPE_SLONGSARRAY;
    public final RefType TYPE_SSHORTSARRAY;
    public final RefType TYPE_SBYTESARRAY;
    public final RefType TYPE_SBOOLSARRAY;
    public final RefType TYPE_PARTNERCLASSSARRAY;
    public final RefType TYPE_SE;
    public final RefType TYPE_MULIB_VALUE_TRANSFORMER;
    public final Type TYPE_INT;
    public final Type TYPE_LONG;
    public final Type TYPE_DOUBLE;
    public final Type TYPE_FLOAT;
    public final Type TYPE_SHORT;
    public final Type TYPE_BYTE;
    public final Type TYPE_BOOL;
    public final Type TYPE_CLASS;
    public final Type TYPE_FIELD;
    public final Type TYPE_EXCEPTION;
    public final Type TYPE_OBJECT;
    public final Type TYPE_STRING;
    public final Type TYPE_VOID;
    public final Type TYPE_SOLVER_MANAGER;
    public final Type TYPE_SYM_SPRIMITIVE;
    public final Type TYPE_SPRIMITIVE;
    public final Type TYPE_SUBSTITUTED_VAR;

    /* FIELDS */
    public final SootField SF_SINT_NEUTRAL;
    public final SootField SF_SLONG_NEUTRAL;
    public final SootField SF_SDOUBLE_NEUTRAL;
    public final SootField SF_SFLOAT_NEUTRAL;
    public final SootField SF_SSHORT_NEUTRAL;
    public final SootField SF_SBYTE_NEUTRAL;
    public final SootField SF_SBOOL_NEUTRAL;
    /* SPECIAL METHODS */
    // Unwrap methods
    public final SootMethod SM_INTEGER_GETVAL;
    public final SootMethod SM_LONG_GETVAL;
    public final SootMethod SM_DOUBLE_GETVAL;
    public final SootMethod SM_FLOAT_GETVAL;
    public final SootMethod SM_SHORT_GETVAL;
    public final SootMethod SM_BYTE_GETVAL;
    public final SootMethod SM_BOOLEAN_GETVAL;
    // Indicator methods
    public final SootMethod SM_MULIB_FREE_INT;
    public final SootMethod SM_MULIB_FREE_LONG;
    public final SootMethod SM_MULIB_FREE_DOUBLE;
    public final SootMethod SM_MULIB_FREE_FLOAT;
    public final SootMethod SM_MULIB_FREE_SHORT;
    public final SootMethod SM_MULIB_FREE_BYTE;
    public final SootMethod SM_MULIB_FREE_BOOL;
    public final SootMethod SM_MULIB_NAMED_FREE_INT;
    public final SootMethod SM_MULIB_NAMED_FREE_LONG;
    public final SootMethod SM_MULIB_NAMED_FREE_DOUBLE;
    public final SootMethod SM_MULIB_NAMED_FREE_FLOAT;
    public final SootMethod SM_MULIB_NAMED_FREE_SHORT;
    public final SootMethod SM_MULIB_NAMED_FREE_BYTE;
    public final SootMethod SM_MULIB_NAMED_FREE_BOOL;

    // Framework methods
    public final SootMethod SM_SE_FREE_SINT;
    public final SootMethod SM_SE_FREE_SLONG;
    public final SootMethod SM_SE_FREE_SDOUBLE;
    public final SootMethod SM_SE_FREE_SFLOAT;
    public final SootMethod SM_SE_FREE_SSHORT;
    public final SootMethod SM_SE_FREE_SBYTE;
    public final SootMethod SM_SE_FREE_SBOOL;
    public final SootMethod SM_SE_NAMED_FREE_SINT;
    public final SootMethod SM_SE_NAMED_FREE_SLONG;
    public final SootMethod SM_SE_NAMED_FREE_SDOUBLE;
    public final SootMethod SM_SE_NAMED_FREE_SFLOAT;
    public final SootMethod SM_SE_NAMED_FREE_SSHORT;
    public final SootMethod SM_SE_NAMED_FREE_SBYTE;
    public final SootMethod SM_SE_NAMED_FREE_SBOOL;

    public final SootMethod SM_SE_CONCSINT;
    public final SootMethod SM_SE_CONCSDOUBLE;
    public final SootMethod SM_SE_CONCSLONG;
    public final SootMethod SM_SE_CONCSFLOAT;
    public final SootMethod SM_SE_CONCSSHORT;
    public final SootMethod SM_SE_CONCSBYTE;
    public final SootMethod SM_SE_CONCSBOOL;
    public final SootMethod SM_SE_GET;
    public final SootMethod SM_SE_INSTANCEOF;
    public final SootMethod SM_SE_CAST_TO;
    public final SootMethod SM_SE_CONCRETIZE;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_ALREADY_CREATED;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_REGISTER_COPY;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_GET_COPY;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_COPY_SEARCH_REGION_REPRESENTATION;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_TRANSFORM_VALUE;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_TRANSFORM_TYPE;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_LABEL_PRIMITIVE_VALUE;
    public final SootMethod SM_MULIB_VALUE_TRANSFORMER_LABEL_VALUE;
    public final SootMethod SM_SINT_ADD;
    public final SootMethod SM_SINT_SUB;
    public final SootMethod SM_SINT_DIV;
    public final SootMethod SM_SINT_MUL;
    public final SootMethod SM_SINT_MOD;
    public final SootMethod SM_SINT_NEG;
    public final SootMethod SM_SINT_LT;
    public final SootMethod SM_SINT_LTE;
    public final SootMethod SM_SINT_GT;
    public final SootMethod SM_SINT_GTE;
    public final SootMethod SM_SINT_EQ;
    public final SootMethod SM_SINT_LT_CHOICE_S;
    public final SootMethod SM_SINT_LTE_CHOICE_S;
    public final SootMethod SM_SINT_EQ_CHOICE_S;
    public final SootMethod SM_SINT_NOT_EQ_CHOICE_S;
    public final SootMethod SM_SINT_GT_CHOICE_S;
    public final SootMethod SM_SINT_GTE_CHOICE_S;
    public final SootMethod SM_SINT_LT_CHOICE;
    public final SootMethod SM_SINT_LTE_CHOICE;
    public final SootMethod SM_SINT_EQ_CHOICE;
    public final SootMethod SM_SINT_NOT_EQ_CHOICE;
    public final SootMethod SM_SINT_GT_CHOICE;
    public final SootMethod SM_SINT_GTE_CHOICE;
    public final SootMethod SM_SINT_I2D;
    public final SootMethod SM_SINT_I2F;
    public final SootMethod SM_SINT_I2L;
    public final SootMethod SM_SINT_I2B;
    public final SootMethod SM_SINT_I2S;
    public final SootMethod SM_SLONG_ADD;
    public final SootMethod SM_SLONG_SUB;
    public final SootMethod SM_SLONG_DIV;
    public final SootMethod SM_SLONG_MUL;
    public final SootMethod SM_SLONG_MOD;
    public final SootMethod SM_SLONG_NEG;
    public final SootMethod SM_SLONG_LT;
    public final SootMethod SM_SLONG_LTE;
    public final SootMethod SM_SLONG_GT;
    public final SootMethod SM_SLONG_GTE;
    public final SootMethod SM_SLONG_EQ;
    public final SootMethod SM_SLONG_LT_CHOICE_S;
    public final SootMethod SM_SLONG_LTE_CHOICE_S;
    public final SootMethod SM_SLONG_EQ_CHOICE_S;
    public final SootMethod SM_SLONG_NOT_EQ_CHOICE_S;
    public final SootMethod SM_SLONG_GT_CHOICE_S;
    public final SootMethod SM_SLONG_GTE_CHOICE_S;
    public final SootMethod SM_SLONG_LT_CHOICE;
    public final SootMethod SM_SLONG_LTE_CHOICE;
    public final SootMethod SM_SLONG_EQ_CHOICE;
    public final SootMethod SM_SLONG_NOT_EQ_CHOICE;
    public final SootMethod SM_SLONG_GT_CHOICE;
    public final SootMethod SM_SLONG_GTE_CHOICE;
    public final SootMethod SM_SLONG_L2D;
    public final SootMethod SM_SLONG_L2F;
    public final SootMethod SM_SLONG_L2I;
    public final SootMethod SM_SLONG_CMP;
    public final SootMethod SM_SDOUBLE_ADD;
    public final SootMethod SM_SDOUBLE_SUB;
    public final SootMethod SM_SDOUBLE_DIV;
    public final SootMethod SM_SDOUBLE_MUL;
    public final SootMethod SM_SDOUBLE_MOD;
    public final SootMethod SM_SDOUBLE_NEG;
    public final SootMethod SM_SDOUBLE_LT;
    public final SootMethod SM_SDOUBLE_LTE;
    public final SootMethod SM_SDOUBLE_GT;
    public final SootMethod SM_SDOUBLE_GTE;
    public final SootMethod SM_SDOUBLE_EQ;
    public final SootMethod SM_SDOUBLE_LT_CHOICE_S;
    public final SootMethod SM_SDOUBLE_LTE_CHOICE_S;
    public final SootMethod SM_SDOUBLE_EQ_CHOICE_S;
    public final SootMethod SM_SDOUBLE_NOT_EQ_CHOICE_S;
    public final SootMethod SM_SDOUBLE_GT_CHOICE_S;
    public final SootMethod SM_SDOUBLE_GTE_CHOICE_S;
    public final SootMethod SM_SDOUBLE_LT_CHOICE;
    public final SootMethod SM_SDOUBLE_LTE_CHOICE;
    public final SootMethod SM_SDOUBLE_EQ_CHOICE;
    public final SootMethod SM_SDOUBLE_NOT_EQ_CHOICE;
    public final SootMethod SM_SDOUBLE_GT_CHOICE;
    public final SootMethod SM_SDOUBLE_GTE_CHOICE;
    public final SootMethod SM_SDOUBLE_D2F;
    public final SootMethod SM_SDOUBLE_D2L;
    public final SootMethod SM_SDOUBLE_D2I;
    public final SootMethod SM_SDOUBLE_CMP;
    public final SootMethod SM_SFLOAT_ADD;
    public final SootMethod SM_SFLOAT_SUB;
    public final SootMethod SM_SFLOAT_DIV;
    public final SootMethod SM_SFLOAT_MUL;
    public final SootMethod SM_SFLOAT_MOD;
    public final SootMethod SM_SFLOAT_NEG;
    public final SootMethod SM_SFLOAT_LT;
    public final SootMethod SM_SFLOAT_LTE;
    public final SootMethod SM_SFLOAT_GT;
    public final SootMethod SM_SFLOAT_GTE;
    public final SootMethod SM_SFLOAT_EQ;
    public final SootMethod SM_SFLOAT_LT_CHOICE_S;
    public final SootMethod SM_SFLOAT_LTE_CHOICE_S;
    public final SootMethod SM_SFLOAT_EQ_CHOICE_S;
    public final SootMethod SM_SFLOAT_NOT_EQ_CHOICE_S;
    public final SootMethod SM_SFLOAT_GT_CHOICE_S;
    public final SootMethod SM_SFLOAT_GTE_CHOICE_S;
    public final SootMethod SM_SFLOAT_LT_CHOICE;
    public final SootMethod SM_SFLOAT_LTE_CHOICE;
    public final SootMethod SM_SFLOAT_EQ_CHOICE;
    public final SootMethod SM_SFLOAT_NOT_EQ_CHOICE;
    public final SootMethod SM_SFLOAT_GT_CHOICE;
    public final SootMethod SM_SFLOAT_GTE_CHOICE;
    public final SootMethod SM_SFLOAT_F2D;
    public final SootMethod SM_SFLOAT_F2L;
    public final SootMethod SM_SFLOAT_F2I;
    public final SootMethod SM_SFLOAT_CMP;
    public final SootMethod SM_SARRAY_SELECT;
    public final SootMethod SM_SARRAY_STORE;
    public final SootMethod SM_SARRAY_LENGTH;

    public final SootMethod SM_SBOOL_BOOL_CHOICE_S;
    public final SootMethod SM_SBOOL_NEGATED_BOOL_CHOICE_S;
    public final SootMethod SM_SBOOL_BOOL_CHOICE;
    public final SootMethod SM_SBOOL_NEGATED_BOOL_CHOICE;

    public final SootMethod SM_CLASS_GET_DECLARED_FIELD;
    public final SootMethod SM_FIELD_SET_ACCESSIBLE;
    public final SootMethod SM_FIELD_GET;
    public final SootMethod SM_FIELD_SET;
    public SootMulibClassesAndMethods() {
        SC_MULIB_RUNTIME_EXCEPTION = Scene.v().forceResolve(MulibRuntimeException.class.getName(), SootClass.SIGNATURES);
        SC_CLASS = Scene.v().forceResolve(Class.class.getName(), SootClass.SIGNATURES);
        SC_INTEGER = Scene.v().forceResolve(Integer.class.getName(), SootClass.SIGNATURES);
        SC_LONG = Scene.v().forceResolve(Long.class.getName(), SootClass.SIGNATURES);
        SC_DOUBLE = Scene.v().forceResolve(Double.class.getName(), SootClass.SIGNATURES);
        SC_FLOAT = Scene.v().forceResolve(Float.class.getName(), SootClass.SIGNATURES);
        SC_SHORT = Scene.v().forceResolve(Short.class.getName(), SootClass.SIGNATURES);
        SC_BYTE = Scene.v().forceResolve(Byte.class.getName(), SootClass.SIGNATURES);
        SC_BOOLEAN = Scene.v().forceResolve(Boolean.class.getName(), SootClass.SIGNATURES);
        SC_FIELD = Scene.v().forceResolve(Field.class.getName(), SootClass.SIGNATURES);
        SC_EXCEPTION = Scene.v().forceResolve(Exception.class.getName(), SootClass.SIGNATURES);
        SC_MULIB = Scene.v().forceResolve(Mulib.class.getName(), SootClass.SIGNATURES);
        SC_SINT = Scene.v().forceResolve(Sint.class.getName(), SootClass.SIGNATURES);
        SC_SLONG = Scene.v().forceResolve(Slong.class.getName(), SootClass.SIGNATURES);
        SC_SDOUBLE = Scene.v().forceResolve(Sdouble.class.getName(), SootClass.SIGNATURES);
        SC_SFLOAT = Scene.v().forceResolve(Sfloat.class.getName(), SootClass.SIGNATURES);
        SC_SSHORT = Scene.v().forceResolve(Sshort.class.getName(), SootClass.SIGNATURES);
        SC_SBYTE = Scene.v().forceResolve(Sbyte.class.getName(), SootClass.SIGNATURES);
        SC_SBOOL = Scene.v().forceResolve(Sbool.class.getName(), SootClass.SIGNATURES);
        SC_CONCSINT     = Scene.v().forceResolve(Sint.ConcSint.class.getName(), SootClass.SIGNATURES);
        SC_CONCSLONG    = Scene.v().forceResolve(Slong.ConcSlong.class.getName(), SootClass.SIGNATURES);
        SC_CONCSDOUBLE  = Scene.v().forceResolve(Sdouble.ConcSdouble.class.getName(), SootClass.SIGNATURES);
        SC_CONCSFLOAT   = Scene.v().forceResolve(Sfloat.ConcSfloat.class.getName(), SootClass.SIGNATURES);
        SC_CONCSSHORT   = Scene.v().forceResolve(Sshort.ConcSshort.class.getName(), SootClass.SIGNATURES);
        SC_CONCSBYTE    = Scene.v().forceResolve(Sbyte.ConcSbyte.class.getName(), SootClass.SIGNATURES);
        SC_CONCSBOOL    = Scene.v().forceResolve(Sbool.ConcSbool.class.getName(), SootClass.SIGNATURES);
        SC_PARTNERCLASS = Scene.v().forceResolve(PartnerClass.class.getName(), SootClass.SIGNATURES);
        SC_PARTNERCLASSSARRAY   = Scene.v().forceResolve(Sarray.PartnerClassSarray.class.getName(), SootClass.SIGNATURES);
        SC_SINTSARRAY           = Scene.v().forceResolve(Sarray.SintSarray.class.getName(), SootClass.SIGNATURES);
        SC_SDOUBLESARRAY        = Scene.v().forceResolve(Sarray.SdoubleSarray.class.getName(), SootClass.SIGNATURES);
        SC_SFLOATSARRAY         = Scene.v().forceResolve(Sarray.SfloatSarray.class.getName(), SootClass.SIGNATURES);
        SC_SLONGSARRAY          = Scene.v().forceResolve(Sarray.SlongSarray.class.getName(), SootClass.SIGNATURES);
        SC_SSHORTSARRAY         = Scene.v().forceResolve(Sarray.SshortSarray.class.getName(), SootClass.SIGNATURES);
        SC_SBYTESARRAY          = Scene.v().forceResolve(Sarray.SbyteSarray.class.getName(), SootClass.SIGNATURES);
        SC_SBOOLSARRAY          = Scene.v().forceResolve(Sarray.SboolSarray.class.getName(), SootClass.SIGNATURES);
        SC_SARRAYSARRAY         = Scene.v().forceResolve(Sarray.SarraySarray.class.getName(), SootClass.SIGNATURES);
        SC_SARRAY               = Scene.v().forceResolve(Sarray.class.getName(), SootClass.SIGNATURES);
        SC_SE = Scene.v().forceResolve(SymbolicExecution.class.getName(), SootClass.SIGNATURES);
        SC_MULIB_VALUE_TRANSFORMER = Scene.v().forceResolve(MulibValueTransformer.class.getName(), SootClass.SIGNATURES);
        SC_SOLVER_MANAGER = Scene.v().forceResolve(SolverManager.class.getName(), SootClass.SIGNATURES);
        SC_SPRIMITIVE = Scene.v().forceResolve(Sprimitive.class.getName(), SootClass.SIGNATURES);
        SC_SYM_SPRIMITIVE = Scene.v().forceResolve(SymSprimitive.class.getName(), SootClass.SIGNATURES);
        SC_SUBSTITUTED_VAR = Scene.v().forceResolve(SubstitutedVar.class.getName(), SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
        TYPE_MULIB_RUNTIME_EXCEPTION = SC_MULIB_RUNTIME_EXCEPTION.getType();
        TYPE_SINT = Scene.v().getRefType(Sint.class.getName());
        TYPE_SLONG = Scene.v().getRefType(Slong.class.getName());
        TYPE_SDOUBLE = Scene.v().getRefType(Sdouble.class.getName());
        TYPE_SFLOAT = Scene.v().getRefType(Sfloat.class.getName());
        TYPE_SSHORT = Scene.v().getRefType(Sshort.class.getName());
        TYPE_SBYTE = Scene.v().getRefType(Sbyte.class.getName());
        TYPE_SBOOL = Scene.v().getRefType(Sbool.class.getName());
        TYPE_PARTNERCLASS = Scene.v().getRefType(PartnerClass.class.getName());
        TYPE_SE = Scene.v().getRefType(SymbolicExecution.class.getName());
        TYPE_MULIB_VALUE_TRANSFORMER = Scene.v().getRefType(MulibValueTransformer.class.getName());
        TYPE_SOLVER_MANAGER = SC_SOLVER_MANAGER.getType();
        TYPE_INT = Scene.v().getType("int");
        TYPE_LONG = Scene.v().getType("long");
        TYPE_DOUBLE = Scene.v().getType("double");
        TYPE_FLOAT = Scene.v().getType("float");
        TYPE_SHORT = Scene.v().getType("short");
        TYPE_BYTE = Scene.v().getType("byte");
        TYPE_BOOL = Scene.v().getType("boolean");
        TYPE_CLASS = Scene.v().getType(Class.class.getName());
        TYPE_FIELD = Scene.v().getType(Field.class.getName());
        TYPE_OBJECT = Scene.v().getType(Object.class.getName());
        TYPE_EXCEPTION = Scene.v().getType(Exception.class.getName());
        TYPE_STRING = Scene.v().getType(String.class.getName());
        TYPE_VOID = Scene.v().getType("void");
        TYPE_SARRAYSARRAY           = SC_SARRAYSARRAY.getType();
        TYPE_SINTSARRAY             = SC_SINTSARRAY.getType();
        TYPE_SDOUBLESARRAY          = SC_SDOUBLESARRAY.getType();
        TYPE_SFLOATSARRAY           = SC_SFLOATSARRAY.getType();
        TYPE_SLONGSARRAY            = SC_SLONGSARRAY.getType();
        TYPE_SSHORTSARRAY           = SC_SSHORTSARRAY.getType();
        TYPE_SBYTESARRAY            = SC_SBYTESARRAY.getType();
        TYPE_SBOOLSARRAY            = SC_SBOOLSARRAY.getType();
        TYPE_PARTNERCLASSSARRAY     = SC_PARTNERCLASSSARRAY.getType();
        TYPE_SPRIMITIVE             = SC_SPRIMITIVE.getType();
        TYPE_SYM_SPRIMITIVE         = SC_SYM_SPRIMITIVE.getType();
        TYPE_CONCSINT       = SC_CONCSINT.getType();
        TYPE_CONCSLONG      = SC_CONCSLONG.getType();
        TYPE_CONCSDOUBLE    = SC_CONCSDOUBLE.getType();
        TYPE_CONCSFLOAT     = SC_CONCSFLOAT.getType();
        TYPE_CONCSSHORT     = SC_CONCSSHORT.getType();
        TYPE_CONCSBYTE      = SC_CONCSBYTE.getType();
        TYPE_CONCSBOOL      = SC_CONCSBOOL.getType();
        TYPE_SUBSTITUTED_VAR = SC_SUBSTITUTED_VAR.getType();

        SF_SINT_NEUTRAL     = SC_SINT.getField("ZERO",      TYPE_CONCSINT);
        SF_SLONG_NEUTRAL    = SC_SLONG.getField("ZERO",     TYPE_CONCSLONG);
        SF_SDOUBLE_NEUTRAL  = SC_SDOUBLE.getField("ZERO",   TYPE_CONCSDOUBLE);
        SF_SFLOAT_NEUTRAL   = SC_SFLOAT.getField("ZERO",    TYPE_CONCSFLOAT);
        SF_SSHORT_NEUTRAL   = SC_SSHORT.getField("ZERO",    TYPE_CONCSSHORT);
        SF_SBYTE_NEUTRAL    = SC_SBYTE.getField("ZERO",     TYPE_CONCSBYTE);
        SF_SBOOL_NEUTRAL    = SC_SBOOL.getField("FALSE",    TYPE_CONCSBOOL);

        SM_INTEGER_GETVAL   = SC_INTEGER.getMethod("intValue", List.of(), TYPE_INT);
        SM_LONG_GETVAL      = SC_LONG.getMethod("longValue", List.of(), TYPE_LONG);
        SM_DOUBLE_GETVAL    = SC_DOUBLE.getMethod("doubleValue", List.of(), TYPE_DOUBLE);
        SM_FLOAT_GETVAL     = SC_FLOAT.getMethod("floatValue", List.of(), TYPE_FLOAT);
        SM_SHORT_GETVAL     = SC_SHORT.getMethod("shortValue", List.of(), TYPE_SHORT);
        SM_BYTE_GETVAL      = SC_BYTE.getMethod("byteValue", List.of(), TYPE_BYTE);
        SM_BOOLEAN_GETVAL   = SC_BOOLEAN.getMethod("booleanValue", List.of(), TYPE_BOOL);

        SM_MULIB_FREE_INT           = SC_MULIB.getMethod("freeInt",         Collections.emptyList(), TYPE_INT);
        SM_MULIB_FREE_LONG          = SC_MULIB.getMethod("freeLong",        Collections.emptyList(), TYPE_LONG);
        SM_MULIB_FREE_DOUBLE        = SC_MULIB.getMethod("freeDouble",      Collections.emptyList(), TYPE_DOUBLE);
        SM_MULIB_FREE_FLOAT         = SC_MULIB.getMethod("freeFloat",       Collections.emptyList(), TYPE_FLOAT);
        SM_MULIB_FREE_SHORT         = SC_MULIB.getMethod("freeShort",       Collections.emptyList(), TYPE_SHORT);
        SM_MULIB_FREE_BYTE          = SC_MULIB.getMethod("freeByte",        Collections.emptyList(), TYPE_BYTE);
        SM_MULIB_FREE_BOOL          = SC_MULIB.getMethod("freeBoolean",     Collections.emptyList(), TYPE_BOOL);
        SM_MULIB_NAMED_FREE_INT     = SC_MULIB.getMethod("namedFreeInt",    List.of(TYPE_STRING),    TYPE_INT);
        SM_MULIB_NAMED_FREE_LONG    = SC_MULIB.getMethod("namedFreeLong",   List.of(TYPE_STRING),    TYPE_LONG);
        SM_MULIB_NAMED_FREE_DOUBLE  = SC_MULIB.getMethod("namedFreeDouble", List.of(TYPE_STRING),    TYPE_DOUBLE);
        SM_MULIB_NAMED_FREE_FLOAT   = SC_MULIB.getMethod("namedFreeFloat",  List.of(TYPE_STRING),    TYPE_FLOAT);
        SM_MULIB_NAMED_FREE_SHORT   = SC_MULIB.getMethod("namedFreeShort",  List.of(TYPE_STRING),    TYPE_SHORT);
        SM_MULIB_NAMED_FREE_BYTE    = SC_MULIB.getMethod("namedFreeByte",   List.of(TYPE_STRING),    TYPE_BYTE);
        SM_MULIB_NAMED_FREE_BOOL    = SC_MULIB.getMethod("namedFreeBoolean",List.of(TYPE_STRING),    TYPE_BOOL);

        SM_SE_FREE_SINT             = SC_SE.getMethod("symSint",            Collections.emptyList(), TYPE_SINT);
        SM_SE_FREE_SLONG            = SC_SE.getMethod("symSlong",           Collections.emptyList(), TYPE_SLONG);
        SM_SE_FREE_SDOUBLE          = SC_SE.getMethod("symSdouble",         Collections.emptyList(), TYPE_SDOUBLE);
        SM_SE_FREE_SFLOAT           = SC_SE.getMethod("symSfloat",          Collections.emptyList(), TYPE_SFLOAT);
        SM_SE_FREE_SSHORT           = SC_SE.getMethod("symSshort",          Collections.emptyList(), TYPE_SSHORT);
        SM_SE_FREE_SBYTE            = SC_SE.getMethod("symSbyte",           Collections.emptyList(), TYPE_SBYTE);
        SM_SE_FREE_SBOOL            = SC_SE.getMethod("symSbool",           Collections.emptyList(), TYPE_SBOOL);
        SM_SE_NAMED_FREE_SINT       = SC_SE.getMethod("namedSymSint",       List.of(TYPE_STRING),    TYPE_SINT);
        SM_SE_NAMED_FREE_SLONG      = SC_SE.getMethod("namedSymSlong",      List.of(TYPE_STRING),    TYPE_SLONG);
        SM_SE_NAMED_FREE_SDOUBLE    = SC_SE.getMethod("namedSymSdouble",    List.of(TYPE_STRING),    TYPE_SDOUBLE);
        SM_SE_NAMED_FREE_SFLOAT     = SC_SE.getMethod("namedSymSfloat",     List.of(TYPE_STRING),    TYPE_SFLOAT);
        SM_SE_NAMED_FREE_SSHORT     = SC_SE.getMethod("namedSymSshort",     List.of(TYPE_STRING),    TYPE_SSHORT);
        SM_SE_NAMED_FREE_SBYTE      = SC_SE.getMethod("namedSymSbyte",      List.of(TYPE_STRING),    TYPE_SBYTE);
        SM_SE_NAMED_FREE_SBOOL      = SC_SE.getMethod("namedSymSbool",      List.of(TYPE_STRING),    TYPE_SBOOL);

        SM_SE_CONCSINT = SC_SE.getMethod("concSint", List.of(TYPE_INT), TYPE_SINT);
        SM_SE_CONCSDOUBLE = SC_SE.getMethod("concSdouble", List.of(TYPE_DOUBLE), TYPE_SDOUBLE);
        SM_SE_CONCSLONG = SC_SE.getMethod("concSlong", List.of(TYPE_LONG), TYPE_SLONG);
        SM_SE_CONCSFLOAT = SC_SE.getMethod("concSfloat", List.of(TYPE_FLOAT), TYPE_SFLOAT);
        SM_SE_CONCSSHORT = SC_SE.getMethod("concSshort", List.of(TYPE_SHORT), TYPE_SSHORT);
        SM_SE_CONCSBYTE = SC_SE.getMethod("concSbyte", List.of(TYPE_BYTE), TYPE_SBYTE);
        SM_SE_CONCSBOOL = SC_SE.getMethod("concSbool", List.of(TYPE_BOOL), TYPE_SBOOL);
        SM_SE_GET = SC_SE.getMethod("get", Collections.emptyList(), TYPE_SE);
        SM_SE_INSTANCEOF = SC_SE.getMethod("evalInstanceof", List.of(TYPE_PARTNERCLASS, TYPE_CLASS), TYPE_SBOOL);
        SM_SE_CAST_TO = SC_SE.getMethod("castTo", List.of(TYPE_OBJECT, TYPE_CLASS), TYPE_OBJECT);
        SM_SE_CONCRETIZE = SC_SE.getMethod("concretize", List.of(TYPE_SUBSTITUTED_VAR), TYPE_OBJECT);

        SM_MULIB_VALUE_TRANSFORMER_ALREADY_CREATED = SC_MULIB_VALUE_TRANSFORMER.getMethod("alreadyCreated",                 List.of(TYPE_OBJECT), TYPE_BOOL);
        SM_MULIB_VALUE_TRANSFORMER_REGISTER_COPY = SC_MULIB_VALUE_TRANSFORMER.getMethod("registerCopy",                   List.of(TYPE_OBJECT, TYPE_OBJECT), TYPE_VOID);
        SM_MULIB_VALUE_TRANSFORMER_GET_COPY = SC_MULIB_VALUE_TRANSFORMER.getMethod("getCopy",                        List.of(TYPE_OBJECT), TYPE_OBJECT);
        SM_MULIB_VALUE_TRANSFORMER_COPY_SEARCH_REGION_REPRESENTATION = SC_MULIB_VALUE_TRANSFORMER.getMethod("copySearchRegionRepresentation", List.of(TYPE_OBJECT), TYPE_OBJECT);
        SM_MULIB_VALUE_TRANSFORMER_TRANSFORM_VALUE = SC_MULIB_VALUE_TRANSFORMER.getMethod("transformValue",                 List.of(TYPE_OBJECT), TYPE_OBJECT);
        SM_MULIB_VALUE_TRANSFORMER_TRANSFORM_TYPE = SC_MULIB_VALUE_TRANSFORMER.getMethod("transformType",                  List.of(TYPE_CLASS), TYPE_CLASS);
        SM_MULIB_VALUE_TRANSFORMER_LABEL_PRIMITIVE_VALUE = SC_MULIB_VALUE_TRANSFORMER.getMethod("labelPrimitiveValue", List.of(TYPE_SPRIMITIVE, TYPE_SOLVER_MANAGER), TYPE_OBJECT);
        SM_MULIB_VALUE_TRANSFORMER_LABEL_VALUE = SC_MULIB_VALUE_TRANSFORMER.getMethod("labelValue", List.of(TYPE_OBJECT, TYPE_SOLVER_MANAGER), TYPE_OBJECT);

        SM_SINT_ADD                 = SC_SINT.getMethod("add",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SINT);
        SM_SINT_SUB                 = SC_SINT.getMethod("sub",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SINT);
        SM_SINT_DIV                 = SC_SINT.getMethod("div",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SINT);
        SM_SINT_MUL                 = SC_SINT.getMethod("mul",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SINT);
        SM_SINT_MOD                 = SC_SINT.getMethod("mod",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SINT);
        SM_SINT_NEG                 = SC_SINT.getMethod("neg",          List.of(TYPE_SE),               TYPE_SINT);
        SM_SINT_LT                  = SC_SINT.getMethod("lt",           List.of(TYPE_SINT, TYPE_SE),    TYPE_SBOOL);
        SM_SINT_LTE                 = SC_SINT.getMethod("lte",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SBOOL);
        SM_SINT_GT                  = SC_SINT.getMethod("gt",           List.of(TYPE_SINT, TYPE_SE),    TYPE_SBOOL);
        SM_SINT_GTE                 = SC_SINT.getMethod("gte",          List.of(TYPE_SINT, TYPE_SE),    TYPE_SBOOL);
        SM_SINT_EQ                  = SC_SINT.getMethod("eq",           List.of(TYPE_SINT, TYPE_SE),    TYPE_SBOOL);
        SM_SINT_LT_CHOICE_S         = SC_SINT.getMethod("ltChoice",     List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_LTE_CHOICE_S        = SC_SINT.getMethod("lteChoice",    List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_EQ_CHOICE_S         = SC_SINT.getMethod("eqChoice",     List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_NOT_EQ_CHOICE_S     = SC_SINT.getMethod("notEqChoice",  List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_GT_CHOICE_S         = SC_SINT.getMethod("gtChoice",     List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_GTE_CHOICE_S        = SC_SINT.getMethod("gteChoice",    List.of(TYPE_SE),               TYPE_BOOL);
        SM_SINT_LT_CHOICE           = SC_SINT.getMethod("ltChoice",     List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_LTE_CHOICE          = SC_SINT.getMethod("lteChoice",    List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_EQ_CHOICE           = SC_SINT.getMethod("eqChoice",     List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_NOT_EQ_CHOICE       = SC_SINT.getMethod("notEqChoice",  List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_GT_CHOICE           = SC_SINT.getMethod("gtChoice",     List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_GTE_CHOICE          = SC_SINT.getMethod("gteChoice",    List.of(TYPE_SINT, TYPE_SE),    TYPE_BOOL);
        SM_SINT_I2D                 = SC_SINT.getMethod("i2d",          List.of(TYPE_SE),               TYPE_SDOUBLE);
        SM_SINT_I2F                 = SC_SINT.getMethod("i2f",          List.of(TYPE_SE),               TYPE_SFLOAT);
        SM_SINT_I2L                 = SC_SINT.getMethod("i2l",          List.of(TYPE_SE),               TYPE_SLONG);
        SM_SINT_I2B                 = SC_SINT.getMethod("i2b",          List.of(TYPE_SE),               TYPE_SBYTE);
        SM_SINT_I2S                 = SC_SINT.getMethod("i2s",          List.of(TYPE_SE),               TYPE_SSHORT);

        SM_SLONG_ADD                = SC_SLONG.getMethod("add",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SLONG);
        SM_SLONG_SUB                = SC_SLONG.getMethod("sub",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SLONG);
        SM_SLONG_DIV                = SC_SLONG.getMethod("div",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SLONG);
        SM_SLONG_MUL                = SC_SLONG.getMethod("mul",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SLONG);
        SM_SLONG_MOD                = SC_SLONG.getMethod("mod",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SLONG);
        SM_SLONG_NEG                = SC_SLONG.getMethod("neg",          List.of(TYPE_SE),              TYPE_SLONG);
        SM_SLONG_LT                 = SC_SLONG.getMethod("lt",           List.of(TYPE_SLONG, TYPE_SE),  TYPE_SBOOL);
        SM_SLONG_LTE                = SC_SLONG.getMethod("lte",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SBOOL);
        SM_SLONG_GT                 = SC_SLONG.getMethod("gt",           List.of(TYPE_SLONG, TYPE_SE),  TYPE_SBOOL);
        SM_SLONG_GTE                = SC_SLONG.getMethod("gte",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SBOOL);
        SM_SLONG_EQ                 = SC_SLONG.getMethod("eq",           List.of(TYPE_SLONG, TYPE_SE),  TYPE_SBOOL);
        SM_SLONG_LT_CHOICE_S        = SC_SLONG.getMethod("ltChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_LTE_CHOICE_S       = SC_SLONG.getMethod("lteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_EQ_CHOICE_S        = SC_SLONG.getMethod("eqChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_NOT_EQ_CHOICE_S    = SC_SLONG.getMethod("notEqChoice",  List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_GT_CHOICE_S        = SC_SLONG.getMethod("gtChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_GTE_CHOICE_S       = SC_SLONG.getMethod("gteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SLONG_LT_CHOICE          = SC_SLONG.getMethod("ltChoice",     List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_LTE_CHOICE         = SC_SLONG.getMethod("lteChoice",    List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_EQ_CHOICE          = SC_SLONG.getMethod("eqChoice",     List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_NOT_EQ_CHOICE      = SC_SLONG.getMethod("notEqChoice",  List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_GT_CHOICE          = SC_SLONG.getMethod("gtChoice",     List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_GTE_CHOICE         = SC_SLONG.getMethod("gteChoice",    List.of(TYPE_SLONG, TYPE_SE),  TYPE_BOOL);
        SM_SLONG_L2D                = SC_SLONG.getMethod("l2d",          List.of(TYPE_SE),              TYPE_SDOUBLE);
        SM_SLONG_L2F                = SC_SLONG.getMethod("l2f",          List.of(TYPE_SE),              TYPE_SFLOAT);
        SM_SLONG_L2I                = SC_SLONG.getMethod("l2i",          List.of(TYPE_SE),              TYPE_SINT);
        SM_SLONG_CMP                = SC_SLONG.getMethod("cmp",          List.of(TYPE_SLONG, TYPE_SE),  TYPE_SINT);

        SM_SDOUBLE_ADD              = SC_SDOUBLE.getMethod("add",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SDOUBLE);
        SM_SDOUBLE_SUB              = SC_SDOUBLE.getMethod("sub",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SDOUBLE);
        SM_SDOUBLE_DIV              = SC_SDOUBLE.getMethod("div",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SDOUBLE);
        SM_SDOUBLE_MUL              = SC_SDOUBLE.getMethod("mul",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SDOUBLE);
        SM_SDOUBLE_MOD              = SC_SDOUBLE.getMethod("mod",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SDOUBLE);
        SM_SDOUBLE_NEG              = SC_SDOUBLE.getMethod("neg",          List.of(TYPE_SE),                TYPE_SDOUBLE);
        SM_SDOUBLE_LT               = SC_SDOUBLE.getMethod("lt",           List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SBOOL);
        SM_SDOUBLE_LTE              = SC_SDOUBLE.getMethod("lte",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SBOOL);
        SM_SDOUBLE_GT               = SC_SDOUBLE.getMethod("gt",           List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SBOOL);
        SM_SDOUBLE_GTE              = SC_SDOUBLE.getMethod("gte",          List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SBOOL);
        SM_SDOUBLE_EQ               = SC_SDOUBLE.getMethod("eq",           List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_SBOOL);
        SM_SDOUBLE_LT_CHOICE_S      = SC_SDOUBLE.getMethod("ltChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_LTE_CHOICE_S     = SC_SDOUBLE.getMethod("lteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_EQ_CHOICE_S      = SC_SDOUBLE.getMethod("eqChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_NOT_EQ_CHOICE_S  = SC_SDOUBLE.getMethod("notEqChoice",  List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_GT_CHOICE_S      = SC_SDOUBLE.getMethod("gtChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_GTE_CHOICE_S     = SC_SDOUBLE.getMethod("gteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SDOUBLE_LT_CHOICE        = SC_SDOUBLE.getMethod("ltChoice",     List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_LTE_CHOICE       = SC_SDOUBLE.getMethod("lteChoice",    List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_EQ_CHOICE        = SC_SDOUBLE.getMethod("eqChoice",     List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_NOT_EQ_CHOICE    = SC_SDOUBLE.getMethod("notEqChoice",  List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_GT_CHOICE        = SC_SDOUBLE.getMethod("gtChoice",     List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_GTE_CHOICE       = SC_SDOUBLE.getMethod("gteChoice",    List.of(TYPE_SDOUBLE, TYPE_SE),  TYPE_BOOL);
        SM_SDOUBLE_D2F              = SC_SDOUBLE.getMethod("d2f",          List.of(TYPE_SE),              TYPE_SFLOAT);
        SM_SDOUBLE_D2L              = SC_SDOUBLE.getMethod("d2l",          List.of(TYPE_SE),              TYPE_SLONG);
        SM_SDOUBLE_D2I              = SC_SDOUBLE.getMethod("d2i",          List.of(TYPE_SE),              TYPE_SINT);
        SM_SDOUBLE_CMP              = SC_SDOUBLE.getMethod("cmp",          List.of(TYPE_SDOUBLE, TYPE_SE),TYPE_SINT);

        SM_SFLOAT_ADD               = SC_SFLOAT.getMethod("add",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SFLOAT);
        SM_SFLOAT_SUB               = SC_SFLOAT.getMethod("sub",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SFLOAT);
        SM_SFLOAT_DIV               = SC_SFLOAT.getMethod("div",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SFLOAT);
        SM_SFLOAT_MUL               = SC_SFLOAT.getMethod("mul",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SFLOAT);
        SM_SFLOAT_MOD               = SC_SFLOAT.getMethod("mod",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SFLOAT);
        SM_SFLOAT_NEG               = SC_SFLOAT.getMethod("neg",          List.of(TYPE_SE),              TYPE_SFLOAT);
        SM_SFLOAT_LT                = SC_SFLOAT.getMethod("lt",           List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SBOOL);
        SM_SFLOAT_LTE               = SC_SFLOAT.getMethod("lte",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SBOOL);
        SM_SFLOAT_GT                = SC_SFLOAT.getMethod("gt",           List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SBOOL);
        SM_SFLOAT_GTE               = SC_SFLOAT.getMethod("gte",          List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_SBOOL);
        SM_SFLOAT_EQ                = SC_SFLOAT.getMethod("eq",           List.of(TYPE_SFLOAT, TYPE_SE), TYPE_SBOOL);
        SM_SFLOAT_LT_CHOICE_S       = SC_SFLOAT.getMethod("ltChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_LTE_CHOICE_S      = SC_SFLOAT.getMethod("lteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_EQ_CHOICE_S       = SC_SFLOAT.getMethod("eqChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_NOT_EQ_CHOICE_S   = SC_SFLOAT.getMethod("notEqChoice",  List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_GT_CHOICE_S       = SC_SFLOAT.getMethod("gtChoice",     List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_GTE_CHOICE_S      = SC_SFLOAT.getMethod("gteChoice",    List.of(TYPE_SE),              TYPE_BOOL);
        SM_SFLOAT_LT_CHOICE         = SC_SFLOAT.getMethod("ltChoice",     List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_LTE_CHOICE        = SC_SFLOAT.getMethod("lteChoice",    List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_EQ_CHOICE         = SC_SFLOAT.getMethod("eqChoice",     List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_NOT_EQ_CHOICE     = SC_SFLOAT.getMethod("notEqChoice",  List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_GT_CHOICE         = SC_SFLOAT.getMethod("gtChoice",     List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_GTE_CHOICE        = SC_SFLOAT.getMethod("gteChoice",    List.of(TYPE_SFLOAT, TYPE_SE),  TYPE_BOOL);
        SM_SFLOAT_F2D               = SC_SFLOAT.getMethod("f2d",          List.of(TYPE_SE),              TYPE_SDOUBLE);
        SM_SFLOAT_F2L               = SC_SFLOAT.getMethod("f2l",          List.of(TYPE_SE),              TYPE_SLONG);
        SM_SFLOAT_F2I               = SC_SFLOAT.getMethod("f2i",          List.of(TYPE_SE),              TYPE_SINT);
        SM_SFLOAT_CMP               = SC_SFLOAT.getMethod("cmp",          List.of(TYPE_SFLOAT, TYPE_SE), TYPE_SINT);
        SM_SARRAY_LENGTH    = SC_SARRAY.getMethod("length", List.of(),                                    TYPE_SINT);
        SM_SARRAY_SELECT    = SC_SARRAY.getMethod("select", List.of(TYPE_SINT, TYPE_SE),                  TYPE_SUBSTITUTED_VAR);
        SM_SARRAY_STORE     = SC_SARRAY.getMethod("store", List.of(TYPE_SINT, TYPE_SUBSTITUTED_VAR, TYPE_SE),     TYPE_VOID);

        SM_SBOOL_BOOL_CHOICE = SC_SBOOL.getMethod("boolChoice",          List.of(TYPE_SBOOL, TYPE_SE),          TYPE_BOOL);
        SM_SBOOL_NEGATED_BOOL_CHOICE = SC_SBOOL.getMethod("negatedBoolChoice",   List.of(TYPE_SBOOL, TYPE_SE),  TYPE_BOOL);
        SM_SBOOL_BOOL_CHOICE_S = SC_SBOOL.getMethod("boolChoice",          List.of(TYPE_SE),                    TYPE_BOOL);
        SM_SBOOL_NEGATED_BOOL_CHOICE_S = SC_SBOOL.getMethod("negatedBoolChoice",   List.of(TYPE_SE),            TYPE_BOOL);

        SM_CLASS_GET_DECLARED_FIELD  = SC_CLASS.getMethod("getDeclaredField", List.of(TYPE_STRING), TYPE_FIELD);
        SM_FIELD_SET_ACCESSIBLE      = SC_FIELD.getMethod("setAccessible", List.of(TYPE_BOOL), TYPE_VOID);
        SM_FIELD_GET = SC_FIELD.getMethod("get", List.of(TYPE_OBJECT), TYPE_OBJECT);
        SM_FIELD_SET = SC_FIELD.getMethod("set", List.of(TYPE_OBJECT, TYPE_OBJECT), TYPE_VOID);
    }

    public static SootMethodRef getMethodRefForMethod(SootMethod sm) { /// TODO not necessary anymore.
        return sm.makeRef();
    }

    public boolean isIndicatorMethodName(String methodName) {
        switch (methodName) {
            case "freeInt":
            case "freeLong":
            case "freeDouble":
            case "freeFloat":
            case "freeShort":
            case "freeByte":
            case "freeBoolean":
            case "namedFreeInt":
            case "namedFreeLong":
            case "namedFreeDouble":
            case "namedFreeFloat":
            case "namedFreeShort":
            case "namedFreeByte":
            case "namedFreeBoolean":
                return true;
            default:
                return false;
        }
    }

    public SootMethod getTransformedMethodForIndicatorMethodName(String indicatorMethodName) {
        switch (indicatorMethodName) {
            case "freeInt":
                return SM_SE_FREE_SINT;
            case "freeLong":
                return SM_SE_FREE_SLONG;
            case "freeDouble":
                return SM_SE_FREE_SDOUBLE;
            case "freeFloat":
                return SM_SE_FREE_SFLOAT;
            case "freeShort":
                return SM_SE_FREE_SSHORT;
            case "freeByte":
                return SM_SE_FREE_SBYTE;
            case "freeBoolean":
                return SM_SE_FREE_SBOOL;
            case "namedFreeInt":
                return SM_SE_NAMED_FREE_SINT;
            case "namedFreeLong":
                return SM_SE_NAMED_FREE_SLONG;
            case "namedFreeDouble":
                return SM_SE_NAMED_FREE_SDOUBLE;
            case "namedFreeFloat":
                return SM_SE_NAMED_FREE_SFLOAT;
            case "namedFreeShort":
                return SM_SE_NAMED_FREE_SSHORT;
            case "namedFreeByte":
                return SM_SE_NAMED_FREE_SBYTE;
            case "namedFreeBoolean":
                return SM_SE_NAMED_FREE_SBOOL;
            default:
                throw new NotYetImplementedException();
        }
    }

}