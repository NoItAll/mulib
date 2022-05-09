package de.wwu.mulib.transformations;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.solving.solvers.SolverManager;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.List;

import static de.wwu.mulib.transformations.TransformationUtility.*;

public final class StringConstants {
    private StringConstants() {}

    public static final String _TRANSFORMATION_PREFIX = "__mulib__";
    public static final String clinit = "<clinit>";
    public static final String init = "<init>";
    public static final String main = "main";

    public static final String thisDesc = "this";
    public static final String seName = "se";

    public static final String primitiveTypes = "BCDFIJSZ";

    public static final String integerCp = cpForClass(Integer.class);
    public static final String longCp = cpForClass(Long.class);
    public static final String doubleCp = cpForClass(Double.class);
    public static final String floatCp = cpForClass(Float.class);
    public static final String shortCp = cpForClass(Short.class);
    public static final String byteCp = cpForClass(Byte.class);
    public static final String booleanCp = cpForClass(Boolean.class);

    public static final String objectCp = cpForClass(Object.class);
    public static final String objectDesc = cdescForClass(Object.class);

    public static final String stringCp = cpForClass(String.class);
    public static final String stringDesc = cdescForClass(String.class);

    public static final String mulibCp = cpForClass(Mulib.class);
    public static final String mulibDesc = cdescForClass(Mulib.class);

    public static final String classCp = cpForClass(Class.class);
    public static final String classDesc = cdescForClass(Class.class);

    public static final String partnerClassCp = cpForClass(PartnerClass.class);
    public static final String partnerClassDesc = cdescForClass(PartnerClass.class);

    public static final String sarrayCp = cpForClass(Sarray.class);
    public static final String sarrayDesc = cdescForClass(Sarray.class);

    public static final String substitutedVarCp = cpForClass(SubstitutedVar.class);
    public static final String substitutedVarDesc = cdescForClass(SubstitutedVar.class);

    public static final String sprimitiveCp = cpForClass(Sprimitive.class);
    public static final String sprimitiveDesc = cdescForClass(Sprimitive.class);
    public static final String symSprimitiveCp = cpForClass(SymSprimitive.class);

    public static final String sintCp = cpForClass(Sint.class);
    public static final String sintDesc = cdescForClass(Sint.class);
    public static final String concSintCp = cpForClass(Sint.ConcSint.class);

    public static final String sdoubleCp = cpForClass(Sdouble.class);
    public static final String sdoubleDesc = cdescForClass(Sdouble.class);
    public static final String concSdoubleCp = cpForClass(Sdouble.ConcSdouble.class);

    public static final String sfloatCp = cpForClass(Sfloat.class);
    public static final String sfloatDesc = cdescForClass(Sfloat.class);
    public static final String concSFloatCp = cpForClass(Sfloat.ConcSfloat.class);


    public static final String sboolCp = cpForClass(Sbool.class);
    public static final String sboolDesc = cdescForClass(Sbool.class);
    public static final String concSboolCp = cpForClass(Sbool.ConcSbool.class);

    public static final String sshortCp = cpForClass(Sshort.class);
    public static final String sshortDesc = cdescForClass(Sshort.class);
    public static final String concSshortCp = cpForClass(Sshort.ConcSshort.class);

    public static final String sbyteCp = cpForClass(Sbyte.class);
    public static final String sbyteDesc = cdescForClass(Sbyte.class);
    public static final String concSbyteCp = cpForClass(Sbyte.ConcSbyte.class);

    public static final String slongCp = cpForClass(Slong.class);
    public static final String slongDesc = cdescForClass(Slong.class);
    public static final String concSlongCp = cpForClass(Slong.ConcSlong.class);

    public static final String sintSarrayCp = cpForClass(Sarray.SintSarray.class);
    public static final String sintSarrayDesc = cdescForClass(Sarray.SintSarray.class);
    public static final String slongSarrayCp = cpForClass(Sarray.SlongSarray.class);
    public static final String slongSarrayDesc = cdescForClass(Sarray.SlongSarray.class);
    public static final String sdoubleSarrayCp = cpForClass(Sarray.SdoubleSarray.class);
    public static final String sdoubleSarrayDesc = cdescForClass(Sarray.SdoubleSarray.class);
    public static final String sfloatSarrayCp = cpForClass(Sarray.SfloatSarray.class);
    public static final String sfloatSarrayDesc = cdescForClass(Sarray.SfloatSarray.class);
    public static final String sshortSarrayCp = cpForClass(Sarray.SshortSarray.class);
    public static final String sshortSarrayDesc = cdescForClass(Sarray.SshortSarray.class);
    public static final String sbyteSarrayCp = cpForClass(Sarray.SbyteSarray.class);
    public static final String sbyteSarrayDesc = cdescForClass(Sarray.SbyteSarray.class);
    public static final String sboolSarrayCp = cpForClass(Sarray.SboolSarray.class);
    public static final String sboolSarrayDesc = cdescForClass(Sarray.SboolSarray.class);
    public static final String sarraySarrayCp = cpForClass(Sarray.SarraySarray.class);
    public static final String sarraySarrayDesc = cdescForClass(Sarray.SarraySarray.class);
    public static final String partnerClassSarrayCp = cpForClass(Sarray.PartnerClassSarray.class);
    public static final String partnerClassSarrayDesc = cdescForClass(Sarray.PartnerClassSarray.class);


    public static final String seCn = cnForClass(SymbolicExecution.class);
    public static final String seCp = cpForClass(SymbolicExecution.class);
    public static final String seDesc = cdescForClass(SymbolicExecution.class);

    public static final String mulibValueTransformerCp = cpForClass(MulibValueTransformer.class);
    public static final String mulibValueTransformerDesc = cdescForClass(MulibValueTransformer.class);

    public static final String solverManagerCp = cpForClass(SolverManager.class);
    public static final String solverManagerDesc = cdescForClass(SolverManager.class);

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

    public static final String sdoubleSarray = "sdoubleSarray";
    public static final String sintSarray = "sintSarray";
    public static final String sfloatSarray = "sfloatSarray";
    public static final String sboolSarray = "sboolSarray";
    public static final String sshortSarray = "sshortSarray";
    public static final String sbyteSarray = "sbyteSarray";
    public static final String slongSarray = "slongSarray";
    public static final String sarraySarray = "sarraySarray";
    public static final String partnerClassSarray = "partnerClassSarray";

    public static final String namedSymSdouble = "namedSymSdouble";
    public static final String namedSymSint = "namedSymSint";
    public static final String namedSymSfloat = "namedSymSfloat";
    public static final String namedSymSbool = "namedSymSbool";
    public static final String namedSymSshort = "namedSymSshort";
    public static final String namedSymSbyte = "namedSymSbyte";
    public static final String namedSymSlong = "namedSymSlong";

    public static final String namedSdoubleSarray = "namedSdoubleSarray";
    public static final String namedSintSarray = "namedSintSarray";
    public static final String namedSfloatSarray = "namedSfloatSarray";
    public static final String namedSboolSarray = "namedSboolSarray";
    public static final String namedSshortSarray = "namedSshortSarray";
    public static final String namedSbyteSarray = "namedSbyteSarray";
    public static final String namedSlongSarray = "namedSlongSarray";
    public static final String namedSarraySarray = "namedSarraySarray";
    public static final String namedPartnerClassSarray = "namedPartnerClassSarray";

    public static final String concretize = "concretize";
    public static final String concretizeDesc = "(" + objectDesc + seDesc + ")" + objectDesc;

    public static final String sintArithDesc = "(" + sintDesc + seDesc + ")" + sintDesc;
    public static final String slongArithDesc = "(" + slongDesc + seDesc + ")" + slongDesc;
    public static final String sdoubleArithDesc = "(" + sdoubleDesc + seDesc + ")" + sdoubleDesc;
    public static final String sfloatArithDesc = "(" + sfloatDesc + seDesc + ")" + sfloatDesc;
    public static final String singlesintDesc = "(" + seDesc + ")" + sintDesc;
    public static final String singleslongDesc = "(" + seDesc + ")" + slongDesc;
    public static final String singlesfloatDesc = "(" + seDesc + ")" + sfloatDesc;
    public static final String singlesdoubleDesc = "(" + seDesc + ")" + sdoubleDesc;

    public static final String lltDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;
    public static final String llteDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;
    public static final String leqDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;
    public static final String lgtDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;
    public static final String lgteDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;
    public static final String lcmpDesc = "(" + slongDesc + seDesc + ")" + sboolDesc;

    public static final String dltDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;
    public static final String dlteDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;
    public static final String deqDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;
    public static final String dgtDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;
    public static final String dgteDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;
    public static final String dcmpDesc = "(" + sdoubleDesc + seDesc + ")" + sboolDesc;

    public static final String fltDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;
    public static final String flteDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;
    public static final String feqDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;
    public static final String fgtDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;
    public static final String fgteDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;
    public static final String fcmpDesc = "(" + sfloatDesc + seDesc + ")" + sboolDesc;

    public static final String iltDesc = "(" + sintDesc + seDesc + ")" + sboolDesc;
    public static final String ilteDesc = "(" + sintDesc + seDesc + ")" + sboolDesc;
    public static final String ieqDesc = "(" + sintDesc + seDesc + ")" + sboolDesc;
    public static final String igtDesc = "(" + sintDesc + seDesc + ")" + sboolDesc;
    public static final String igteDesc = "(" + sintDesc + seDesc + ")" + sboolDesc;

    public static final String andDesc = "(" + sboolDesc + seDesc + ")" + sboolDesc;
    public static final String orDesc = "(" + sboolDesc + seDesc + ")" + sboolDesc;
    public static final String notDesc = "(" + seDesc + ")" + sboolDesc;

    public static final String singleVarChoiceDesc = "(" + seDesc + ")Z";
    public static final String itwoNumbersChoiceDesc = "(" + sintDesc + seDesc + ")Z";

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
    public static final String freeIntArray = "freeIntArray";
    public static final String freeLongArray = "freeLongArray";
    public static final String freeDoubleArray = "freeDoubleArray";
    public static final String freeFloatArray = "freeFloatArray";
    public static final String freeShortArray = "freeShortArray";
    public static final String freeByteArray = "freeByteArray";
    public static final String freeBooleanArray = "freeBooleanArray";
    public static final String freeCharArray = "freeCharArray";

    public static final String namedFreeInt = "namedFreeInt";
    public static final String namedFreeLong = "namedFreeLong";
    public static final String namedFreeDouble = "namedFreeDouble";
    public static final String namedFreeFloat = "namedFreeFloat";
    public static final String namedFreeShort = "namedFreeShort";
    public static final String namedFreeByte = "namedFreeByte";
    public static final String namedFreeBoolean = "namedFreeBoolean";
    public static final String namedFreeChar = "namedFreeChar";
    public static final String namedFreeObject = "namedFreeObject";
    public static final String namedFreeIntArray = "namedFreeIntArray";
    public static final String namedFreeLongArray = "namedFreeLongArray";
    public static final String namedFreeDoubleArray = "namedFreeDoubleArray";
    public static final String namedFreeFloatArray = "namedFreeFloatArray";
    public static final String namedFreeShortArray = "namedFreeShortArray";
    public static final String namedFreeByteArray = "namedFreeByteArray";
    public static final String namedFreeCharArray = "namedFreeCharArray";
    public static final String namedFreeBooleanArray = "namedFreeBooleanArray";

    private static final String newArrayParamsDesc = "(" + sintDesc + "Z" + seDesc + ")";
    private static final String newClassBasedArrayParamsDesc = "(" + classDesc + sintDesc + "Z" + seDesc + ")";
    public static final String newSintSarrayDesc = newArrayParamsDesc + sintSarrayDesc;
    public static final String newSlongSarrayDesc = newArrayParamsDesc + slongSarrayDesc;
    public static final String newSdoubleSarrayDesc = newArrayParamsDesc + sdoubleSarrayDesc;
    public static final String newSfloatSarrayDesc = newArrayParamsDesc + sfloatSarrayDesc;
    public static final String newSshortSarrayDesc = newArrayParamsDesc + sshortSarrayDesc;
    public static final String newSbyteSarrayDesc = newArrayParamsDesc + sbyteSarrayDesc;
    public static final String newSboolSarrayDesc = newArrayParamsDesc + sboolSarrayDesc;
    public static final String newSarraySarrayDesc = newClassBasedArrayParamsDesc + sarraySarrayDesc;
    public static final String newSarraySarrayWithFixedDimensionsDesc = "(" +  sintDesc + "[" + sintDesc + classDesc + seDesc + ")" + sarraySarrayDesc;
    public static final String newPartnerClassSarrayDesc = newClassBasedArrayParamsDesc + partnerClassSarrayDesc;

    private static final String newNamedArrayParamsDesc = "(" + stringDesc + sintDesc + "Z" + seDesc + ")";
    private static final String newNamedClassBasedArrayParamsDesc = "(" + stringDesc + classDesc + sintDesc + "Z" + seDesc + ")";
    public static final String newNamedSintSarrayDesc = newNamedArrayParamsDesc + sintSarrayDesc;
    public static final String newNamedSlongSarrayDesc = newNamedArrayParamsDesc + slongSarrayDesc;
    public static final String newNamedSdoubleSarrayDesc = newNamedArrayParamsDesc + sdoubleSarrayDesc;
    public static final String newNamedSfloatSarrayDesc = newNamedArrayParamsDesc + sfloatSarrayDesc;
    public static final String newNamedSshortSarrayDesc = newNamedArrayParamsDesc + sshortSarrayDesc;
    public static final String newNamedSbyteSarrayDesc = newNamedArrayParamsDesc + sbyteSarrayDesc;
    public static final String newNamedSboolSarrayDesc = newNamedArrayParamsDesc + sboolSarrayDesc;
    public static final String newNamedSarraySarrayDesc = newNamedClassBasedArrayParamsDesc + sarraySarrayDesc;
    public static final String newNamedPartnerClassSarrayDesc = newNamedClassBasedArrayParamsDesc + partnerClassSarrayDesc;

    /* SARRAY SELECTS */
    private static final String sarraySelectDescTemplate = "(" + sintDesc + seDesc + ")%s";
    public static final String sintSarraySelectDesc = String.format(sarraySelectDescTemplate, sintDesc);
    public static final String sdoubleSarraySelectDesc = String.format(sarraySelectDescTemplate, sdoubleDesc);
    public static final String sfloatSarraySelectDesc = String.format(sarraySelectDescTemplate, sfloatDesc);
    public static final String slongSarraySelectDesc = String.format(sarraySelectDescTemplate, slongDesc);
    public static final String sshortSarraySelectDesc = String.format(sarraySelectDescTemplate, sshortDesc);
    public static final String sbyteSarraySelectDesc = String.format(sarraySelectDescTemplate, sbyteDesc);
    public static final String sboolSarraySelectDesc = String.format(sarraySelectDescTemplate, sboolDesc);
    public static final String partnerClassSarraySelectDesc = String.format(sarraySelectDescTemplate, partnerClassDesc);
    public static final String sarraySarraySelectDesc = String.format(sarraySelectDescTemplate, sarrayDesc);


    /* SARRAY STORES */
    private static final String sarrayStoreDescTemplate = "(" + sintDesc + "%s" + seDesc + ")V";
    public static final String sintSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sintDesc);
    public static final String sdoubleSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sdoubleDesc);
    public static final String sfloatSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sfloatDesc);
    public static final String slongSarrayStoreDesc = String.format(sarrayStoreDescTemplate, slongDesc);
    public static final String sshortSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sshortDesc);
    public static final String sbyteSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sbyteDesc);
    public static final String sboolSarrayStoreDesc = String.format(sarrayStoreDescTemplate, sboolDesc);
    public static final String partnerClassSarrayStoreDesc = String.format(sarrayStoreDescTemplate, partnerClassDesc);
    public static final String sarraySarrayStoreDesc = String.format(sarrayStoreDescTemplate, sarrayDesc);

    /* SARRAY LENGTH */
    public static final String sarrayLength = "length";
    public static final String sarrayLengthDesc = "()" + sintDesc;

    public static final List<String> getMulibPrimitiveWrapperDescs = List.of(
            sintDesc, sdoubleDesc, sfloatDesc, sboolDesc, slongDesc, sshortDesc, sbyteDesc);

    public static final List<String> specialInitializationMethodsWithoutArrays = List.of(
            freeInt, namedFreeInt, freeLong, namedFreeLong, freeDouble, namedFreeDouble,
            freeFloat, namedFreeFloat, freeShort, namedFreeShort, freeByte, namedFreeByte,
            freeBoolean, namedFreeBoolean, freeChar, namedFreeChar, freeObject, namedFreeObject
    );

    public static final List<String> specialArrayInitializationMethods = List.of(
            freeObject, namedFreeObject, freeIntArray, namedFreeIntArray, freeLongArray, namedFreeLongArray,
            freeDoubleArray, namedFreeDoubleArray, freeFloatArray, namedFreeFloatArray,
            freeShortArray, namedFreeShortArray, freeByteArray, namedFreeByteArray,
            freeBooleanArray, namedFreeBooleanArray, freeCharArray, namedFreeCharArray
    );

    public static final List<String> freeArrayDescs = List.of(
            partnerClassSarrayDesc, sarraySarrayDesc, sintSarrayDesc, slongSarrayDesc, sdoubleSarrayDesc,
            sfloatSarrayDesc, sshortSarrayDesc, sbyteSarrayDesc, sboolSarrayDesc
    );
}
