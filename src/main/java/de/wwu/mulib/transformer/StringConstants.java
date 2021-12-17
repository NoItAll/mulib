package de.wwu.mulib.transformer;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.exceptions.MulibRuntimeException;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.List;

public final class StringConstants {
    private StringConstants() {}

    public static final String clinit = "<clinit>";
    public static final String init = "<init>";
    public static final String main = "main";

    public static final String thisDesc = "this";
    public static final String seName = "se";

    public static final String voidDesc = "V";
    public static final String primitiveTypes = "BCDFIJSZ";

    public static final String integerCp = cpForClass(Integer.class);
    public static final String longCp = cpForClass(Long.class);
    public static final String doubleCp = cpForClass(Double.class);
    public static final String floatCp = cpForClass(Float.class);
    public static final String shortCp = cpForClass(Short.class);
    public static final String byteCp = cpForClass(Byte.class);
    public static final String booleanCp = cpForClass(Boolean.class);

    public static final String objectCn = cnForClass(Object.class);
    public static final String objectCp = cpForClass(Object.class);
    public static final String objectDesc = cdescForClass(Object.class);

    public static final String stringCn = cnForClass(String.class);
    public static final String stringCp = cpForClass(String.class);
    public static final String stringDesc = cdescForClass(String.class);

    public static final String mulibCn = cnForClass(Mulib.class);
    public static final String mulibCp = cpForClass(Mulib.class);
    public static final String mulibDesc = cdescForClass(Mulib.class);

    public static final String classCn = cnForClass(Class.class);
    public static final String classCp = cpForClass(Class.class);
    public static final String classDesc = cdescForClass(Class.class);

    public static final String substitutedVarCn = cnForClass(SubstitutedVar.class);
    public static final String substitutedVarCp = cpForClass(SubstitutedVar.class);
    public static final String substitutedVarDesc = cdescForClass(SubstitutedVar.class);

    public static final String sprimitiveCn = cnForClass(Sprimitive.class);
    public static final String sprimitiveCp = cpForClass(Sprimitive.class);
    public static final String sprimitiveDesc = cdescForClass(Sprimitive.class);

    public static final String snumberCn = cnForClass(Snumber.class);
    public static final String snumberCp = cpForClass(Snumber.class);
    public static final String snumberDesc = cdescForClass(Snumber.class);

    public static final String abstractSnumberCn = cnForClass(AbstractSnumber.class);
    public static final String abstractSnumberCp = cpForClass(AbstractSnumber.class);
    public static final String abstractSnumberDesc = cdescForClass(AbstractSnumber.class);

    public static final String sintegerCn = cnForClass(Sintegernumber.class);
    public static final String sintegerCp = cpForClass(Sintegernumber.class);
    public static final String sintegerDesc = cdescForClass(Sintegernumber.class);

    public static final String sfpCn = cnForClass(Sfpnumber.class);
    public static final String sfpCp = cpForClass(Sfpnumber.class);
    public static final String sfpDesc = cdescForClass(Sfpnumber.class);

    public static final String sintCn = cnForClass(Sint.class);
    public static final String sintCp = cpForClass(Sint.class);
    public static final String sintDesc = cdescForClass(Sint.class);
    public static final String concSintDesc = cdescForClass(Sint.ConcSint.class);
    public static final String symSintDesc = cdescForClass(Sint.SymSint.class);


    public static final String sdoubleCn = cnForClass(Sdouble.class);
    public static final String sdoubleCp = cpForClass(Sdouble.class);
    public static final String sdoubleDesc = cdescForClass(Sdouble.class);
    public static final String symSdoubleDesc = cdescForClass(Sdouble.SymSdouble.class);
    public static final String concSdoubleDesc = cdescForClass(Sdouble.ConcSdouble.class);

    public static final String sfloatCn = cnForClass(Sfloat.class);
    public static final String sfloatCp = cpForClass(Sfloat.class);
    public static final String sfloatDesc = cdescForClass(Sfloat.class);
    public static final String concSfloatDesc = cdescForClass(Sfloat.ConcSfloat.class);
    public static final String symSfloatDesc = cdescForClass(Sfloat.SymSfloat.class);


    public static final String sboolCn = cnForClass(Sbool.class);
    public static final String sboolCp = cpForClass(Sbool.class);
    public static final String sboolDesc = cdescForClass(Sbool.class);
    public static final String concSboolDesc = cdescForClass(Sbool.ConcSbool.class);
    public static final String symSboolDesc = cdescForClass(Sbool.SymSbool.class);

    public static final String sshortCn = cnForClass(Sshort.class);
    public static final String sshortCp = cpForClass(Sshort.class);
    public static final String sshortDesc = cdescForClass(Sshort.class);
    public static final String concSshortDesc = cdescForClass(Sshort.ConcSshort.class);
    public static final String symSshortDesc = cdescForClass(Sshort.SymSshort.class);

    public static final String sbyteCn = cnForClass(Sbyte.class);
    public static final String sbyteCp = cpForClass(Sbyte.class);
    public static final String sbyteDesc = cdescForClass(Sbyte.class);
    public static final String concSbyteDesc = cdescForClass(Sbyte.ConcSbyte.class);
    public static final String symSbyteDesc = cdescForClass(Sbyte.SymSbyte.class);

    public static final String slongCn = cnForClass(Slong.class);
    public static final String slongCp = cpForClass(Slong.class);
    public static final String slongDesc = cdescForClass(Slong.class);
    public static final String concSlongDesc = cdescForClass(Slong.ConcSlong.class);
    public static final String symSlongDesc = cdescForClass(Slong.SymSlong.class);

    public static final String seCn = cnForClass(SymbolicExecution.class);
    public static final String seCp = cpForClass(SymbolicExecution.class);
    public static final String seDesc = cdescForClass(SymbolicExecution.class);

    public static final String castTo = "castTo";
    public static final String add = "add";
    public static final String sub = "sub";
    public static final String mul = "mul";
    public static final String rem = "mod";
    public static final String neg = "neg";
    public static final String div = "div";

    public static final String lt = "lt";
    public static final String lte = "lte";
    public static final String eq = "eq";
    public static final String cmp = "cmp";
    public static final String gt = "gt";
    public static final String gte = "gte";

    public static final String ltChoice = "ltChoice";
    public static final String lteChoice = "lteChoice";
    public static final String eqChoice = "eqChoice";
    public static final String notEqChoice = "notEqChoice";
    public static final String gtChoice = "gtChoice";
    public static final String gteChoice = "gteChoice";

    public static final String boolChoice = "boolChoice";
    public static final String negatedBoolChoice = "negatedBoolChoice";

    public static final String and = "and";
    public static final String or = "or";
    public static final String not = "not";

    public static final String concSdouble = "concSdouble";
    public static final String concSint = "concSint";
    public static final String concSfloat = "concSfloat";
    public static final String concSbool = "concSbool";
    public static final String concSshort = "concSshort";
    public static final String concSbyte = "concSbyte";
    public static final String concSlong = "concSlong";

    public static final String symSdouble = "symSdouble";
    public static final String symSint = "symSint";
    public static final String symSfloat = "symSfloat";
    public static final String symSbool = "symSbool";
    public static final String symSshort = "symSshort";
    public static final String symSbyte = "symSbyte";
    public static final String symSlong = "symSlong";

    public static final String trackedSymSdouble = "trackedSymSdouble";
    public static final String trackedSymSint = "trackedSymSint";
    public static final String trackedSymSfloat = "trackedSymSfloat";
    public static final String trackedSymSbool = "trackedSymSbool";
    public static final String trackedSymSshort = "trackedSymSshort";
    public static final String trackedSymSbyte = "trackedSymSbyte";
    public static final String trackedSymSlong = "trackedSymSlong";

    public static final String concretize = "concretize";
    public static final String concretizeDesc = "(" + objectDesc + seDesc + ")" + objectDesc;

    public static final String sintArithDesc = "(" + sintegerDesc + seDesc + ")" + sintDesc;
    public static final String slongArithDesc = "(" + slongDesc + seDesc + ")" + slongDesc;
    public static final String sdoubleArithDesc = "(" + sdoubleDesc + seDesc + ")" + sdoubleDesc;
    public static final String sfloatArithDesc = "(" + sfloatDesc + seDesc + ")" + sfloatDesc;
    public static final String singlesintDesc = "(" + seDesc + ")" + sintDesc;
    public static final String singlesfloatDesc = "(" + seDesc + ")" + sfloatDesc;
    public static final String singlesdoubleDesc = "(" + seDesc + ")" + sdoubleDesc;

    public static final String ltDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String lteDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String eqDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String gtDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String gteDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String cmpDesc = "(" + snumberDesc + seDesc + ")" + sboolDesc;
    public static final String andDesc = "(" + sboolDesc + seDesc + ")" + sboolDesc;
    public static final String orDesc = "(" + sboolDesc + seDesc + ")" + sboolDesc;
    public static final String notDesc = "(" + seDesc + ")" + sboolDesc;

    public static final String singleVarChoiceDesc = "(" + seDesc + ")Z";
    public static final String twoNumbersChoiceDesc = "(" + snumberDesc + seDesc + ")Z";

    /* SPECIAL METHODS TO SUBSTITUTE */
    public static final String freeInt = "freeInt";
    public static final String freeLong = "freeLong";
    public static final String freeDouble = "freeDouble";
    public static final String freeFloat = "freeFloat";
    public static final String freeShort = "freeShort";
    public static final String freeByte = "freeByte";
    public static final String freeBoolean = "freeBoolean";
    public static final String freeChar = "freeChar";
    public static final String freeObject = "freeObject";
    public static final String freeArray = "freeArray";

    public static final String trackedFreeInt = "trackedFreeInt";
    public static final String trackedFreeLong = "trackedFreeLong";
    public static final String trackedFreeDouble = "trackedFreeDouble";
    public static final String trackedFreeFloat = "trackedFreeFloat";
    public static final String trackedFreeShort = "trackedFreeShort";
    public static final String trackedFreeByte = "trackedFreeByte";
    public static final String trackedFreeBoolean = "trackedFreeBoolean";
    public static final String trackedFreeChar = "trackedFreeChar";
    public static final String trackedFreeObject = "trackedFreeObject";
    public static final String trackedFreeArray = "trackedFreeArray";

    public static final List<String> methodsIntroducingTaint = List.of(
            freeInt, freeLong, freeDouble, freeFloat, freeShort, freeByte, freeChar, freeBoolean,
            trackedFreeByte, trackedFreeDouble, trackedFreeChar, trackedFreeDouble, trackedFreeFloat,
            trackedFreeInt, trackedFreeLong, trackedFreeShort
    );


    public static String toMethodDesc(String inParameters, String returnType) {
        return "(" + inParameters + ")" + returnType;
    }

    public static String toObjDesc(String typeName) {
        return "L" + typeName + ";";
    }

    private static String cnForClass(Class<?> c) {
        return c.getName();
    }

    private static String cpForClass(Class<?> c) {
        return c.getName().replaceAll("\\.", "/");
    }

    private static String cdescForClass(Class<?> c) {
        if (c.isPrimitive()) {
            throw new MulibRuntimeException("Should not occur.");
        } else {
            return "L" + cpForClass(c) + ";";
        }
    }
}
