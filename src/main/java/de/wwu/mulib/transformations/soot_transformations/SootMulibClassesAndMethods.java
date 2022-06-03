package de.wwu.mulib.transformations.soot_transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.NotYetImplementedException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.primitives.*;
import de.wwu.mulib.transformations.MulibValueTransformer;
import soot.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class SootMulibClassesAndMethods {
    /* SPECIAL CLASSES */
    public final SootClass SC_EXCEPTION;
    public final SootClass SC_MULIB;
    public final SootClass SC_SINT;
    public final SootClass SC_SLONG;
    public final SootClass SC_SDOUBLE;
    public final SootClass SC_SFLOAT;
    public final SootClass SC_SSHORT;
    public final SootClass SC_SBYTE;
    public final SootClass SC_SBOOL;
    public final SootClass SC_PARTNERCLASS;
    public final List<SootClass> SC_S;
    public final SootClass SC_SE;

    /* SPECIAL TYPES */
    public final RefType TYPE_SINT;
    public final RefType TYPE_SLONG;
    public final RefType TYPE_SDOUBLE;
    public final RefType TYPE_SFLOAT;
    public final RefType TYPE_SSHORT;
    public final RefType TYPE_SBYTE;
    public final RefType TYPE_SBOOL;
    public final RefType TYPE_PARTNERCLASS;
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
    /* SPECIAL METHODS */
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

    public final SootMethod SM_SBOOL_BOOL_CHOICE_S;
    public final SootMethod SM_SBOOL_NEGATED_BOOL_CHOICE_S;
    public final SootMethod SM_SBOOL_BOOL_CHOICE;
    public final SootMethod SM_SBOOL_NEGATED_BOOL_CHOICE;

    /* SPECIAL METHODREFS */
    public final SootMethodRef SMR_SINT_ADD;
    public final SootMethodRef SMR_SINT_SUB;
    public final SootMethodRef SMR_SINT_DIV;
    public final SootMethodRef SMR_SINT_MUL;
    public final SootMethodRef SMR_SINT_MOD;
    public final SootMethodRef SMR_SINT_NEG;
    public final SootMethodRef SMR_SINT_LT;
    public final SootMethodRef SMR_SINT_LTE;
    public final SootMethodRef SMR_SINT_GT;
    public final SootMethodRef SMR_SINT_GTE;
    public final SootMethodRef SMR_SINT_EQ;
    public final SootMethodRef SMR_SINT_LT_CHOICE_S;
    public final SootMethodRef SMR_SINT_LTE_CHOICE_S;
    public final SootMethodRef SMR_SINT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SINT_NOT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SINT_GT_CHOICE_S;
    public final SootMethodRef SMR_SINT_GTE_CHOICE_S;
    public final SootMethodRef SMR_SINT_LT_CHOICE;
    public final SootMethodRef SMR_SINT_LTE_CHOICE;
    public final SootMethodRef SMR_SINT_EQ_CHOICE;
    public final SootMethodRef SMR_SINT_NOT_EQ_CHOICE;
    public final SootMethodRef SMR_SINT_GT_CHOICE;
    public final SootMethodRef SMR_SINT_GTE_CHOICE;
    public final SootMethodRef SMR_SINT_I2D;
    public final SootMethodRef SMR_SINT_I2F;
    public final SootMethodRef SMR_SINT_I2L;
    public final SootMethodRef SMR_SINT_I2B;
    public final SootMethodRef SMR_SINT_I2S;
    public final SootMethodRef SMR_SDOUBLE_ADD;
    public final SootMethodRef SMR_SDOUBLE_SUB;
    public final SootMethodRef SMR_SDOUBLE_DIV;
    public final SootMethodRef SMR_SDOUBLE_MUL;
    public final SootMethodRef SMR_SDOUBLE_MOD;
    public final SootMethodRef SMR_SDOUBLE_NEG;
    public final SootMethodRef SMR_SDOUBLE_LT;
    public final SootMethodRef SMR_SDOUBLE_LTE;
    public final SootMethodRef SMR_SDOUBLE_GT;
    public final SootMethodRef SMR_SDOUBLE_GTE;
    public final SootMethodRef SMR_SDOUBLE_EQ;
    public final SootMethodRef SMR_SDOUBLE_LT_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_LTE_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_EQ_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_NOT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_GT_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_GTE_CHOICE_S;
    public final SootMethodRef SMR_SDOUBLE_LT_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_LTE_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_EQ_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_NOT_EQ_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_GT_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_GTE_CHOICE;
    public final SootMethodRef SMR_SDOUBLE_D2F;
    public final SootMethodRef SMR_SDOUBLE_D2L;
    public final SootMethodRef SMR_SDOUBLE_D2I;
    public final SootMethodRef SMR_SDOUBLE_CMP;
    public final SootMethodRef SMR_SFLOAT_ADD;
    public final SootMethodRef SMR_SFLOAT_SUB;
    public final SootMethodRef SMR_SFLOAT_DIV;
    public final SootMethodRef SMR_SFLOAT_MUL;
    public final SootMethodRef SMR_SFLOAT_MOD;
    public final SootMethodRef SMR_SFLOAT_NEG;
    public final SootMethodRef SMR_SFLOAT_LT;
    public final SootMethodRef SMR_SFLOAT_LTE;
    public final SootMethodRef SMR_SFLOAT_GT;
    public final SootMethodRef SMR_SFLOAT_GTE;
    public final SootMethodRef SMR_SFLOAT_EQ;
    public final SootMethodRef SMR_SFLOAT_LT_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_LTE_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_NOT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_GT_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_GTE_CHOICE_S;
    public final SootMethodRef SMR_SFLOAT_LT_CHOICE;
    public final SootMethodRef SMR_SFLOAT_LTE_CHOICE;
    public final SootMethodRef SMR_SFLOAT_EQ_CHOICE;
    public final SootMethodRef SMR_SFLOAT_NOT_EQ_CHOICE;
    public final SootMethodRef SMR_SFLOAT_GT_CHOICE;
    public final SootMethodRef SMR_SFLOAT_GTE_CHOICE;
    public final SootMethodRef SMR_SFLOAT_F2D;
    public final SootMethodRef SMR_SFLOAT_F2L;
    public final SootMethodRef SMR_SFLOAT_F2I;
    public final SootMethodRef SMR_SFLOAT_CMP;
    public final SootMethodRef SMR_SLONG_ADD;
    public final SootMethodRef SMR_SLONG_SUB;
    public final SootMethodRef SMR_SLONG_DIV;
    public final SootMethodRef SMR_SLONG_MUL;
    public final SootMethodRef SMR_SLONG_MOD;
    public final SootMethodRef SMR_SLONG_NEG;
    public final SootMethodRef SMR_SLONG_LT;
    public final SootMethodRef SMR_SLONG_LTE;
    public final SootMethodRef SMR_SLONG_GT;
    public final SootMethodRef SMR_SLONG_GTE;
    public final SootMethodRef SMR_SLONG_EQ;
    public final SootMethodRef SMR_SLONG_LT_CHOICE_S;
    public final SootMethodRef SMR_SLONG_LTE_CHOICE_S;
    public final SootMethodRef SMR_SLONG_EQ_CHOICE_S;
    public final SootMethodRef SMR_SLONG_NOT_EQ_CHOICE_S;
    public final SootMethodRef SMR_SLONG_GT_CHOICE_S;
    public final SootMethodRef SMR_SLONG_GTE_CHOICE_S;
    public final SootMethodRef SMR_SLONG_LT_CHOICE;
    public final SootMethodRef SMR_SLONG_LTE_CHOICE;
    public final SootMethodRef SMR_SLONG_EQ_CHOICE;
    public final SootMethodRef SMR_SLONG_NOT_EQ_CHOICE;
    public final SootMethodRef SMR_SLONG_GT_CHOICE;
    public final SootMethodRef SMR_SLONG_GTE_CHOICE;
    public final SootMethodRef SMR_SLONG_L2D;
    public final SootMethodRef SMR_SLONG_L2F;
    public final SootMethodRef SMR_SLONG_L2I;
    public final SootMethodRef SMR_SLONG_CMP;
    public final SootMethodRef SMR_SE_CONCSINT;
    public final SootMethodRef SMR_SE_CONCSDOUBLE;
    public final SootMethodRef SMR_SE_CONCSLONG;
    public final SootMethodRef SMR_SE_CONCSFLOAT;
    public final SootMethodRef SMR_SE_CONCSSHORT;
    public final SootMethodRef SMR_SE_CONCSBYTE;
    public final SootMethodRef SMR_SE_CONCSBOOL;
    public final SootMethodRef SMR_SE_GET;
    public final SootMethodRef SMR_SE_INSTANCEOF;
    public final SootMethodRef SMR_SE_CAST_TO;

    public final SootMethodRef SMR_SBOOL_BOOL_CHOICE_S;
    public final SootMethodRef SMR_SBOOL_NEGATED_BOOL_CHOICE_S;
    public final SootMethodRef SMR_SBOOL_BOOL_CHOICE;
    public final SootMethodRef SMR_SBOOL_NEGATED_BOOL_CHOICE;

    public SootMulibClassesAndMethods() {
        SC_EXCEPTION = Scene.v().forceResolve(Exception.class.getName(), SootClass.SIGNATURES);
        SC_MULIB = Scene.v().forceResolve(Mulib.class.getName(), SootClass.SIGNATURES);
        SC_SINT = Scene.v().forceResolve(Sint.class.getName(), SootClass.SIGNATURES);
        SC_SLONG = Scene.v().forceResolve(Slong.class.getName(), SootClass.SIGNATURES);
        SC_SDOUBLE = Scene.v().forceResolve(Sdouble.class.getName(), SootClass.SIGNATURES);
        SC_SFLOAT = Scene.v().forceResolve(Sfloat.class.getName(), SootClass.SIGNATURES);
        SC_SSHORT = Scene.v().forceResolve(Sshort.class.getName(), SootClass.SIGNATURES);
        SC_SBYTE = Scene.v().forceResolve(Sbyte.class.getName(), SootClass.SIGNATURES);
        SC_SBOOL = Scene.v().forceResolve(Sbool.class.getName(), SootClass.SIGNATURES);
        SC_PARTNERCLASS = Scene.v().forceResolve(PartnerClass.class.getName(), SootClass.SIGNATURES);
        SC_SE = Scene.v().forceResolve(SymbolicExecution.class.getName(), SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
        SC_S = List.of(SC_SINT, SC_SLONG, SC_SDOUBLE, SC_SFLOAT,
                SC_SSHORT, SC_SBYTE, SC_SBOOL, SC_PARTNERCLASS);
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

        SM_SE_CONCSINT = SC_SE.getMethod("concSint", List.of(TYPE_INT, TYPE_SE), TYPE_SINT);
        SM_SE_CONCSDOUBLE = SC_SE.getMethod("concSdouble", List.of(TYPE_DOUBLE, TYPE_SE), TYPE_SDOUBLE);
        SM_SE_CONCSLONG = SC_SE.getMethod("concSlong", List.of(TYPE_LONG, TYPE_SE), TYPE_SLONG);
        SM_SE_CONCSFLOAT = SC_SE.getMethod("concSfloat", List.of(TYPE_FLOAT, TYPE_SE), TYPE_SFLOAT);
        SM_SE_CONCSSHORT = SC_SE.getMethod("concSshort", List.of(TYPE_SHORT, TYPE_SE), TYPE_SSHORT);
        SM_SE_CONCSBYTE = SC_SE.getMethod("concSbyte", List.of(TYPE_BYTE, TYPE_SE), TYPE_SBYTE);
        SM_SE_CONCSBOOL = SC_SE.getMethod("concSbool", List.of(TYPE_BOOL, TYPE_SE), TYPE_SBOOL);
        SM_SE_GET = SC_SE.getMethod("get", Collections.emptyList(), TYPE_SE);
        SM_SE_INSTANCEOF = SC_SE.getMethod("evalInstanceof", List.of(TYPE_PARTNERCLASS, TYPE_CLASS), TYPE_SBOOL);
        SM_SE_CAST_TO = SC_SE.getMethod("castTo", List.of(TYPE_OBJECT, TYPE_CLASS), TYPE_OBJECT);

        SMR_SE_CONCSINT = getMethodRefForMethod(SM_SE_CONCSINT);
        SMR_SE_CONCSDOUBLE = getMethodRefForMethod(SM_SE_CONCSDOUBLE);
        SMR_SE_CONCSLONG = getMethodRefForMethod(SM_SE_CONCSLONG);
        SMR_SE_CONCSFLOAT = getMethodRefForMethod(SM_SE_CONCSFLOAT);
        SMR_SE_CONCSSHORT = getMethodRefForMethod(SM_SE_CONCSSHORT);
        SMR_SE_CONCSBYTE = getMethodRefForMethod(SM_SE_CONCSBYTE);
        SMR_SE_CONCSBOOL = getMethodRefForMethod(SM_SE_CONCSBOOL);
        SMR_SE_GET = getMethodRefForMethod(SM_SE_GET);
        SMR_SE_INSTANCEOF = getMethodRefForMethod(SM_SE_INSTANCEOF);
        SMR_SE_CAST_TO = getMethodRefForMethod(SM_SE_CAST_TO);

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

        SM_SBOOL_BOOL_CHOICE = SC_SBOOL.getMethod("boolChoice",          List.of(TYPE_SBOOL, TYPE_SE),          TYPE_BOOL);
        SM_SBOOL_NEGATED_BOOL_CHOICE = SC_SBOOL.getMethod("negatedBoolChoice",   List.of(TYPE_SBOOL, TYPE_SE),  TYPE_BOOL);
        SM_SBOOL_BOOL_CHOICE_S = SC_SBOOL.getMethod("boolChoice",          List.of(TYPE_SE),                    TYPE_BOOL);
        SM_SBOOL_NEGATED_BOOL_CHOICE_S = SC_SBOOL.getMethod("negatedBoolChoice",   List.of(TYPE_SE),            TYPE_BOOL);



        SMR_SINT_ADD                = getMethodRefForMethod(SM_SINT_ADD            );
        SMR_SINT_SUB                = getMethodRefForMethod(SM_SINT_SUB            );
        SMR_SINT_DIV                = getMethodRefForMethod(SM_SINT_DIV            );
        SMR_SINT_MUL                = getMethodRefForMethod(SM_SINT_MUL            );
        SMR_SINT_MOD                = getMethodRefForMethod(SM_SINT_MOD            );
        SMR_SINT_NEG                = getMethodRefForMethod(SM_SINT_NEG            );
        SMR_SINT_LT                 = getMethodRefForMethod(SM_SINT_LT             );
        SMR_SINT_LTE                = getMethodRefForMethod(SM_SINT_LTE            );
        SMR_SINT_GT                 = getMethodRefForMethod(SM_SINT_GT             );
        SMR_SINT_GTE                = getMethodRefForMethod(SM_SINT_GTE            );
        SMR_SINT_EQ                 = getMethodRefForMethod(SM_SINT_EQ             );
        SMR_SINT_LT_CHOICE_S        = getMethodRefForMethod(SM_SINT_LT_CHOICE_S    );
        SMR_SINT_LTE_CHOICE_S       = getMethodRefForMethod(SM_SINT_LTE_CHOICE_S   );
        SMR_SINT_EQ_CHOICE_S        = getMethodRefForMethod(SM_SINT_EQ_CHOICE_S    );
        SMR_SINT_NOT_EQ_CHOICE_S    = getMethodRefForMethod(SM_SINT_NOT_EQ_CHOICE_S);
        SMR_SINT_GT_CHOICE_S        = getMethodRefForMethod(SM_SINT_GT_CHOICE_S    );
        SMR_SINT_GTE_CHOICE_S       = getMethodRefForMethod(SM_SINT_GTE_CHOICE_S   );
        SMR_SINT_LT_CHOICE          = getMethodRefForMethod(SM_SINT_LT_CHOICE      );
        SMR_SINT_LTE_CHOICE         = getMethodRefForMethod(SM_SINT_LTE_CHOICE     );
        SMR_SINT_EQ_CHOICE          = getMethodRefForMethod(SM_SINT_EQ_CHOICE      );
        SMR_SINT_NOT_EQ_CHOICE      = getMethodRefForMethod(SM_SINT_NOT_EQ_CHOICE  );
        SMR_SINT_GT_CHOICE          = getMethodRefForMethod(SM_SINT_GT_CHOICE      );
        SMR_SINT_GTE_CHOICE         = getMethodRefForMethod(SM_SINT_GTE_CHOICE     );
        SMR_SINT_I2D                = getMethodRefForMethod(SM_SINT_I2D            );
        SMR_SINT_I2F                = getMethodRefForMethod(SM_SINT_I2F            );
        SMR_SINT_I2L                = getMethodRefForMethod(SM_SINT_I2L            );
        SMR_SINT_I2B                = getMethodRefForMethod(SM_SINT_I2B            );
        SMR_SINT_I2S                = getMethodRefForMethod(SM_SINT_I2S            );

        SMR_SDOUBLE_ADD             = getMethodRefForMethod(SM_SDOUBLE_ADD            );
        SMR_SDOUBLE_SUB             = getMethodRefForMethod(SM_SDOUBLE_SUB            );
        SMR_SDOUBLE_DIV             = getMethodRefForMethod(SM_SDOUBLE_DIV            );
        SMR_SDOUBLE_MUL             = getMethodRefForMethod(SM_SDOUBLE_MUL            );
        SMR_SDOUBLE_MOD             = getMethodRefForMethod(SM_SDOUBLE_MOD            );
        SMR_SDOUBLE_NEG             = getMethodRefForMethod(SM_SDOUBLE_NEG            );
        SMR_SDOUBLE_LT              = getMethodRefForMethod(SM_SDOUBLE_LT             );
        SMR_SDOUBLE_LTE             = getMethodRefForMethod(SM_SDOUBLE_LTE            );
        SMR_SDOUBLE_GT              = getMethodRefForMethod(SM_SDOUBLE_GT             );
        SMR_SDOUBLE_GTE             = getMethodRefForMethod(SM_SDOUBLE_GTE            );
        SMR_SDOUBLE_EQ              = getMethodRefForMethod(SM_SDOUBLE_EQ             );
        SMR_SDOUBLE_LT_CHOICE_S     = getMethodRefForMethod(SM_SDOUBLE_LT_CHOICE_S    );
        SMR_SDOUBLE_LTE_CHOICE_S    = getMethodRefForMethod(SM_SDOUBLE_LTE_CHOICE_S   );
        SMR_SDOUBLE_EQ_CHOICE_S     = getMethodRefForMethod(SM_SDOUBLE_EQ_CHOICE_S    );
        SMR_SDOUBLE_NOT_EQ_CHOICE_S = getMethodRefForMethod(SM_SDOUBLE_NOT_EQ_CHOICE_S);
        SMR_SDOUBLE_GT_CHOICE_S     = getMethodRefForMethod(SM_SDOUBLE_GT_CHOICE_S    );
        SMR_SDOUBLE_GTE_CHOICE_S    = getMethodRefForMethod(SM_SDOUBLE_GTE_CHOICE_S   );
        SMR_SDOUBLE_LT_CHOICE       = getMethodRefForMethod(SM_SDOUBLE_LT_CHOICE      );
        SMR_SDOUBLE_LTE_CHOICE      = getMethodRefForMethod(SM_SDOUBLE_LTE_CHOICE     );
        SMR_SDOUBLE_EQ_CHOICE       = getMethodRefForMethod(SM_SDOUBLE_EQ_CHOICE      );
        SMR_SDOUBLE_NOT_EQ_CHOICE   = getMethodRefForMethod(SM_SDOUBLE_NOT_EQ_CHOICE  );
        SMR_SDOUBLE_GT_CHOICE       = getMethodRefForMethod(SM_SDOUBLE_GT_CHOICE      );
        SMR_SDOUBLE_GTE_CHOICE      = getMethodRefForMethod(SM_SDOUBLE_GTE_CHOICE     );
        SMR_SDOUBLE_D2F             = getMethodRefForMethod(SM_SDOUBLE_D2F            );
        SMR_SDOUBLE_D2L             = getMethodRefForMethod(SM_SDOUBLE_D2L            );
        SMR_SDOUBLE_D2I             = getMethodRefForMethod(SM_SDOUBLE_D2I            );
        SMR_SDOUBLE_CMP             = getMethodRefForMethod(SM_SDOUBLE_CMP            );

        SMR_SFLOAT_ADD              = getMethodRefForMethod(SM_SFLOAT_ADD            );
        SMR_SFLOAT_SUB              = getMethodRefForMethod(SM_SFLOAT_SUB            );
        SMR_SFLOAT_DIV              = getMethodRefForMethod(SM_SFLOAT_DIV            );
        SMR_SFLOAT_MUL              = getMethodRefForMethod(SM_SFLOAT_MUL            );
        SMR_SFLOAT_MOD              = getMethodRefForMethod(SM_SFLOAT_MOD            );
        SMR_SFLOAT_NEG              = getMethodRefForMethod(SM_SFLOAT_NEG            );
        SMR_SFLOAT_LT               = getMethodRefForMethod(SM_SFLOAT_LT             );
        SMR_SFLOAT_LTE              = getMethodRefForMethod(SM_SFLOAT_LTE            );
        SMR_SFLOAT_GT               = getMethodRefForMethod(SM_SFLOAT_GT             );
        SMR_SFLOAT_GTE              = getMethodRefForMethod(SM_SFLOAT_GTE            );
        SMR_SFLOAT_EQ               = getMethodRefForMethod(SM_SFLOAT_EQ             );
        SMR_SFLOAT_LT_CHOICE_S      = getMethodRefForMethod(SM_SFLOAT_LT_CHOICE_S    );
        SMR_SFLOAT_LTE_CHOICE_S     = getMethodRefForMethod(SM_SFLOAT_LTE_CHOICE_S   );
        SMR_SFLOAT_EQ_CHOICE_S      = getMethodRefForMethod(SM_SFLOAT_EQ_CHOICE_S    );
        SMR_SFLOAT_NOT_EQ_CHOICE_S  = getMethodRefForMethod(SM_SFLOAT_NOT_EQ_CHOICE_S);
        SMR_SFLOAT_GT_CHOICE_S      = getMethodRefForMethod(SM_SFLOAT_GT_CHOICE_S    );
        SMR_SFLOAT_GTE_CHOICE_S     = getMethodRefForMethod(SM_SFLOAT_GTE_CHOICE_S   );
        SMR_SFLOAT_LT_CHOICE        = getMethodRefForMethod(SM_SFLOAT_LT_CHOICE      );
        SMR_SFLOAT_LTE_CHOICE       = getMethodRefForMethod(SM_SFLOAT_LTE_CHOICE     );
        SMR_SFLOAT_EQ_CHOICE        = getMethodRefForMethod(SM_SFLOAT_EQ_CHOICE      );
        SMR_SFLOAT_NOT_EQ_CHOICE    = getMethodRefForMethod(SM_SFLOAT_NOT_EQ_CHOICE  );
        SMR_SFLOAT_GT_CHOICE        = getMethodRefForMethod(SM_SFLOAT_GT_CHOICE      );
        SMR_SFLOAT_GTE_CHOICE       = getMethodRefForMethod(SM_SFLOAT_GTE_CHOICE     );
        SMR_SFLOAT_F2D              = getMethodRefForMethod(SM_SFLOAT_F2D            );
        SMR_SFLOAT_F2L              = getMethodRefForMethod(SM_SFLOAT_F2L            );
        SMR_SFLOAT_F2I              = getMethodRefForMethod(SM_SFLOAT_F2I            );
        SMR_SFLOAT_CMP              = getMethodRefForMethod(SM_SFLOAT_CMP            );

        SMR_SLONG_ADD               = getMethodRefForMethod(SM_SLONG_ADD            );
        SMR_SLONG_SUB               = getMethodRefForMethod(SM_SLONG_SUB            );
        SMR_SLONG_DIV               = getMethodRefForMethod(SM_SLONG_DIV            );
        SMR_SLONG_MUL               = getMethodRefForMethod(SM_SLONG_MUL            );
        SMR_SLONG_MOD               = getMethodRefForMethod(SM_SLONG_MOD            );
        SMR_SLONG_NEG               = getMethodRefForMethod(SM_SLONG_NEG            );
        SMR_SLONG_LT                = getMethodRefForMethod(SM_SLONG_LT             );
        SMR_SLONG_LTE               = getMethodRefForMethod(SM_SLONG_LTE            );
        SMR_SLONG_GT                = getMethodRefForMethod(SM_SLONG_GT             );
        SMR_SLONG_GTE               = getMethodRefForMethod(SM_SLONG_GTE            );
        SMR_SLONG_EQ                = getMethodRefForMethod(SM_SLONG_EQ             );
        SMR_SLONG_LT_CHOICE_S       = getMethodRefForMethod(SM_SLONG_LT_CHOICE_S    );
        SMR_SLONG_LTE_CHOICE_S      = getMethodRefForMethod(SM_SLONG_LTE_CHOICE_S   );
        SMR_SLONG_EQ_CHOICE_S       = getMethodRefForMethod(SM_SLONG_EQ_CHOICE_S    );
        SMR_SLONG_NOT_EQ_CHOICE_S   = getMethodRefForMethod(SM_SLONG_NOT_EQ_CHOICE_S);
        SMR_SLONG_GT_CHOICE_S       = getMethodRefForMethod(SM_SLONG_GT_CHOICE_S    );
        SMR_SLONG_GTE_CHOICE_S      = getMethodRefForMethod(SM_SLONG_GTE_CHOICE_S   );
        SMR_SLONG_LT_CHOICE         = getMethodRefForMethod(SM_SLONG_LT_CHOICE      );
        SMR_SLONG_LTE_CHOICE        = getMethodRefForMethod(SM_SLONG_LTE_CHOICE     );
        SMR_SLONG_EQ_CHOICE         = getMethodRefForMethod(SM_SLONG_EQ_CHOICE      );
        SMR_SLONG_NOT_EQ_CHOICE     = getMethodRefForMethod(SM_SLONG_NOT_EQ_CHOICE  );
        SMR_SLONG_GT_CHOICE         = getMethodRefForMethod(SM_SLONG_GT_CHOICE      );
        SMR_SLONG_GTE_CHOICE        = getMethodRefForMethod(SM_SLONG_GTE_CHOICE     );
        SMR_SLONG_L2D               = getMethodRefForMethod(SM_SLONG_L2D            );
        SMR_SLONG_L2F               = getMethodRefForMethod(SM_SLONG_L2F            );
        SMR_SLONG_L2I               = getMethodRefForMethod(SM_SLONG_L2I            );
        SMR_SLONG_CMP               = getMethodRefForMethod(SM_SLONG_CMP            );

        SMR_SBOOL_BOOL_CHOICE = getMethodRefForMethod(SM_SBOOL_BOOL_CHOICE);
        SMR_SBOOL_NEGATED_BOOL_CHOICE = getMethodRefForMethod(SM_SBOOL_NEGATED_BOOL_CHOICE);
        SMR_SBOOL_BOOL_CHOICE_S = getMethodRefForMethod(SM_SBOOL_BOOL_CHOICE_S);
        SMR_SBOOL_NEGATED_BOOL_CHOICE_S = getMethodRefForMethod(SM_SBOOL_NEGATED_BOOL_CHOICE_S);
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