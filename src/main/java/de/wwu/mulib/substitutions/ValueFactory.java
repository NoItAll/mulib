package de.wwu.mulib.substitutions;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.primitives.*;

import java.util.Map;

/**
 * Interface for all value factories. Value factories should be thread-safe.
 * Constructs instances of sarrays and specialized {@link de.wwu.mulib.substitutions.Sarray.PartnerClassSarray}s and
 * {@link de.wwu.mulib.substitutions.Sarray.SarraySarray}s.
 * Might cache values, yet, has to assure that a trail of values is created that equals to the one provided to the
 * {@link de.wwu.mulib.solving.solvers.SolverManager}.
 * For this, symbolic values must be provided that can be mapped to the symbolic values that are present in the
 * constraint solver. This is possible by regarding {@link SymbolicExecution}s counters.
 * @see SymbolicExecution#getNextNumberInitializedSymObject()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSints()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSlongs()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSshorts()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSbytes()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSchars()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSbools()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSdoubles()
 * @see SymbolicExecution#getNextNumberInitializedAtomicSymSfloats()
 */
public interface ValueFactory {

    static ValueFactory getInstance(MulibConfig config, Map<Class<?>, Class<?>> arrayTypesToSpecializedSarrayClass) {
        if (config.CONCOLIC) {
            return ConcolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
        } else {
            return SymbolicValueFactory.getInstance(config, arrayTypesToSpecializedSarrayClass);
        }
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new int-array
     */
    Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new double-array
     */
    Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new float-array
     */
    Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new long-array
     */
    Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new sshort-array
     */
    Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new byte-array
     */
    Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new boolean-array
     */
    Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @return A new char-array
     */
    Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param clazz The component type
     * @return A new partner class array. Should be a specific subclass of {@link de.wwu.mulib.substitutions.Sarray.PartnerClassSarray}.
     */
    Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param clazz The component type
     * @return A new array of array. Should be a specific subclass of {@link de.wwu.mulib.substitutions.Sarray.SarraySarray}.
     */
    Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lengths The lengths of the different dimensions
     * @param clazz The component type
     * @return A new array of array. Should be a specific subclass of {@link de.wwu.mulib.substitutions.Sarray.SarraySarray}.
     */
    Sarray.SarraySarray sarrarySarray(SymbolicExecution se, Sint[] lengths, Class<?> clazz);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new int-array
     */
    Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new double-array
     */
    Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new float-array
     */
    Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new long-array
     */
    Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new short-array
     */
    Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new byte-array
     */
    Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new boolean-array
     */
    Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param canBeNull Whether the array might be null
     * @return A new char-array
     */
    Sarray.ScharSarray scharSarray(SymbolicExecution se, Sint len, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param clazz The component type
     * @param canBeNull Whether the array might be null
     * @return A new partner class array. Should be a specific subclass of {@link de.wwu.mulib.substitutions.Sarray.PartnerClassSarray}.
     */
    Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<? extends PartnerClass> clazz, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param len The length
     * @param freeElements Should unknown elements be symbolic by default?
     * @param clazz The component type
     * @param canBeNull Whether the array might be null
     * @return A new array of array. Should be a specific subclass of {@link de.wwu.mulib.substitutions.Sarray.SarraySarray}.
     */
    Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<?> clazz, boolean freeElements, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param toGetInstanceOf The class of the object that is to be lazily initialized
     * @return A symbolic object that is to be lazily initialized
     * @param <T> The type of the class
     */
    <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param toGetInstanceOf The class of the object that is to be lazily initialized
     * @param canBeNull Whether the instance can be null or not
     * @return A symbolic object that is to be lazily initialized
     * @param <T> The type of the class
     */
    <T extends PartnerClass> T symObject(SymbolicExecution se, Class<T> toGetInstanceOf, boolean canBeNull);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic int that, for each run, is equal to other symbolic ints generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSints()}
     */
    Sint symSint(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic double that, for each run, is equal to other symbolic double generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSdoubles()}
     */
    Sdouble symSdouble(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic float that, for each run, is equal to other symbolic float generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSfloats()}
     */
    Sfloat symSfloat(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic boolean that, for each run, is equal to other symbolic booleans generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSbools()}
     */
    Sbool symSbool(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic long that, for each run, is equal to other symbolic longs generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSlongs()}
     */
    Slong symSlong(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic short that, for each run, is equal to other symbolic shorts generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSshorts()}
     */
    Sshort symSshort(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic short that, for each run, is equal to other symbolic shorts generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSshorts()}
     */
    Sbyte symSbyte(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return A symbolic char that, for each run, is equal to other symbolic chars generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSchars()}
     */
    Schar symSchar(SymbolicExecution se);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic int that, for each run, is equal to other symbolic ints generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSints()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Sint symSint(SymbolicExecution se, Sint lb, Sint ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic double that, for each run, is equal to other symbolic doubles generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSdoubles()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Sdouble symSdouble(SymbolicExecution se, Sdouble lb, Sdouble ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic float that, for each run, is equal to other symbolic floats generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSfloats()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Sfloat symSfloat(SymbolicExecution se, Sfloat lb, Sfloat ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic long that, for each run, is equal to other symbolic longs generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSlongs()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Slong symSlong(SymbolicExecution se, Slong lb, Slong ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic short that, for each run, is equal to other symbolic shorts generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSshorts()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Sshort symSshort(SymbolicExecution se, Sshort lb, Sshort ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic byte that, for each run, is equal to other symbolic bytes generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSbytes()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Sbyte symSbyte(SymbolicExecution se, Sbyte lb, Sbyte ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param lb The lower bound
     * @param ub The upper bound
     * @return A symbolic char that, for each run, is equal to other symbolic chars generated for the same value
     * retrieved from {@link SymbolicExecution#getNextNumberInitializedAtomicSymSchars()}. The lower and upper bounds
     * are constrained to be in [lb, ub]
     */
    Schar symSchar(SymbolicExecution se, Schar lb, Schar ub);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Sint representing a composed numeric expression
     */
    Sint wrappingSymSint(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Sdouble representing a composed numeric expression
     */
    Sdouble wrappingSymSdouble(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Sfloat representing a composed numeric expression
     */
    Sfloat wrappingSymSfloat(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Slong representing a composed numeric expression
     */
    Slong wrappingSymSlong(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Sshort representing a composed numeric expression
     */
    Sshort wrappingSymSshort(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Sbyte representing a composed numeric expression
     */
    Sbyte wrappingSymSbyte(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param numericExpression The wrapped expression
     * @return A Schar representing a composed numeric expression
     */
    Schar wrappingSymSchar(SymbolicExecution se, NumericExpression numericExpression);

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param constraint The wrapped constraint
     * @return A Sbool representing a composed constraint
     */
    Sbool wrappingSymSbool(SymbolicExecution se, Constraint constraint);

    /**
     * Compares two numeric expressions (should be Sfloat, Sdouble, or Slong) according to Java's fcmp, dcmp, or lcmp
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param n0 The first numeric expression
     * @param n1 The second numeric expression
     * @return A Sint that can either be -1, 0, or 1
     */
    Sint cmp(SymbolicExecution se, NumericExpression n0, NumericExpression n1);
}
