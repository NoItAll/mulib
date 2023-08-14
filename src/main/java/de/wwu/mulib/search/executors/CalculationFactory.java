package de.wwu.mulib.search.executors;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.constraints.ArrayAccessConstraint;
import de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint;
import de.wwu.mulib.solving.ArrayInformation;
import de.wwu.mulib.solving.PartnerClassObjectInformation;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.SubstitutedVar;
import de.wwu.mulib.substitutions.primitives.*;

/**
 * Factory for performing calculations and more complex operations with regards to objects in the search region.
 * Aside from arithmetic operations, array operation such as selecting or storing in an element and potentially representing
 * an object for the constraint solver are dealt with here. Furthermore, if needed, the fields of a lazily initialized objects
 * are initialized here.
 * For these calculations, either a concrete value, a symbolic value wrapping a {@link de.wwu.mulib.constraints.Constraint}
 * or {@link de.wwu.mulib.expressions.NumericExpression}, a {@link PartnerClass}, or a {@link Sarray} with
 * {@link PartnerClass#__mulib__defaultIsSymbolic()} == true is returned.
 * @see de.wwu.mulib.expressions.Neg
 * @see de.wwu.mulib.expressions.AbstractOperatorNumericExpression
 * @see de.wwu.mulib.constraints.AbstractTwoSidedConstraint
 * @see de.wwu.mulib.constraints.AbstractTwoSidedNumericConstraint
 * @see de.wwu.mulib.constraints.ArrayInitializationConstraint
 * @see ArrayAccessConstraint
 * @see de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint
 * @see PartnerClassObjectFieldConstraint
 * @see de.wwu.mulib.constraints.PartnerClassObjectRememberConstraint
 */
public interface CalculationFactory {

    /**
     * Creates a new instance of the calculation factory
     * @param config The configuration according to which a new instance shall be constructed
     * @param vf The value factory which is called to construct new values
     * @return The new calculation factory
     */
    static CalculationFactory getInstance(MulibConfig config, ValueFactory vf) {
        if (config.CONCOLIC) {
            return ConcolicCalculationFactory.getInstance(config, vf);
        } else {
            return SymbolicCalculationFactory.getInstance(config, vf);
        }
    }

    /**
     * Calculates the implication
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The antecedent
     * @param rhs The consequent
     * @return A Sbool representing the implication
     */
    Sbool implies(SymbolicExecution se, Sbool lhs, Sbool rhs);

    /**
     * Calculates the addition of two integers
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sint representing the addition
     */
    Sint add(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the subtraction of two integers
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sint representing the subtraction
     */
    Sint sub(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the multiplication of two integers
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sint representing the multiplication
     */
    Sint mul(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the division of two integers
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sint representing the divison
     */
    Sint div(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the modulo operation of two integers
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sint representing the modulo
     */
    Sint mod(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the negation of a number
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The number
     * @return A Sint representing the negation of i
     */
    Sint neg(SymbolicExecution se, Sint i);

    /**
     * Calculates the addition of two doubles
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sdouble representing the addition
     */
    Sdouble add(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the subtraction of two doubles
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sdouble representing the subtraction
     */
    Sdouble sub(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the multiplication of two doubles
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sdouble representing the multiplication
     */
    Sdouble mul(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the division of two doubles
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sdouble representing the division
     */
    Sdouble div(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the modulo operation of two doubles
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sdouble representing the modulo
     */
    Sdouble mod(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the negation of a double
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param d The double
     * @return A Sdouble representing the negation of d
     */
    Sdouble neg(SymbolicExecution se, Sdouble d);

    /**
     * Calculates the addition of two longs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Slong representing the addition
     */
    Slong add(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the subtraction of two longs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Slong representing the subtraction
     */
    Slong sub(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the multiplication of two longs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Slong representing the multiplication
     */
    Slong mul(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the division of two longs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Slong representing the division
     */
    Slong div(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the modulo operation of two longs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Slong representing the modulo
     */
    Slong mod(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the negation of a long
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The long
     * @return A Slong representing the negation of l
     */
    Slong neg(SymbolicExecution se, Slong l);

    /**
     * Calculates the addition of two floats
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sfloat representing the addition
     */
    Sfloat add(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the subtraction of two floats
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sfloat representing the subtraction
     */
    Sfloat sub(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the multiplication of two floats
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sfloat representing the multiplication
     */
    Sfloat mul(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the division of two floats
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sfloat representing the division
     */
    Sfloat div(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the modulo operation of two floats
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sfloat representing the modulo
     */
    Sfloat mod(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the negation of a float
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param f The float
     * @return A Sfloat representing the negation of f
     */
    Sfloat neg(SymbolicExecution se, Sfloat f);

    /**
     * Calculates the logical and (&&) of two booleans
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the AND
     */
    Sbool and(SymbolicExecution se, Sbool lhs, Sbool rhs);

    /**
     * Calculates the logical or ||) of two booleans
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the OR
     */
    Sbool or(SymbolicExecution se, Sbool lhs, Sbool rhs);

    /**
     * Calculates the logical negation (!) of a boolean
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param b The Sbool
     * @return A Sbool representing the NOT
     */
    Sbool not(SymbolicExecution se, Sbool b);

    /**
     * Calculates the logical XOR (^) of two booleans
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the XOR
     */
    Sbool xor(SymbolicExecution se, Sbool lhs, Sbool rhs);

    /**
     * Calculates the result of lhs < rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lt(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the result of lhs < rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lt(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the result of lhs < rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lt(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the result of lhs < rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lt(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the result of lhs <= rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lte(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the result of lhs <= rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lte(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the result of lhs <= rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lte(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the result of lhs <= rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool lte(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the result of lhs == rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool eq(SymbolicExecution se, Sint lhs, Sint rhs);

    /**
     * Calculates the result of lhs == rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool eq(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the result of lhs == rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool eq(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the result of lhs == rhs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sbool eq(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Calculates the comparison of two Slongs according to the LCMP bytecode instruction
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sint cmp(SymbolicExecution se, Slong lhs, Slong rhs);

    /**
     * Calculates the comparison of two Sdoubles according to the DCMP bytecode instruction
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sint cmp(SymbolicExecution se, Sdouble lhs, Sdouble rhs);

    /**
     * Calculates the comparison of two Sfloats according to the FCMP bytecode instruction
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param lhs The left-hand side
     * @param rhs The right-hand side
     * @return A Sbool representing the result of the operation
     */
    Sint cmp(SymbolicExecution se, Sfloat lhs, Sfloat rhs);

    /**
     * Converts an Sint to a Slong
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return i converted to a Slong
     */
    Slong i2l(SymbolicExecution se, Sint i);

    /**
     * Converts an Sint to a Sfloat
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return i converted to a Sfloat
     */
    Sfloat i2f(SymbolicExecution se, Sint i);

    /**
     * Converts an Sint to a Sdouble
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return i converted to a Sdouble
     */
    Sdouble i2d(SymbolicExecution se, Sint i);

    /**
     * Converts an Sint to a Schar
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return i converted to a Schar
     */
    Schar i2c(SymbolicExecution se, Sint i);

    /**
     * Converts an Slong to a Sint
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong
     * @return l converted to a Sint
     */
    Sint l2i(SymbolicExecution se, Slong l);

    /**
     * Converts an Slong to a Sfloat
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong
     * @return l converted to a Sfloat
     */
    Sfloat l2f(SymbolicExecution se, Slong l);

    /**
     * Converts an Slong to a Sdouble
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong
     * @return l converted to a Sdouble
     */
    Sdouble l2d(SymbolicExecution se, Slong l);

    /**
     * Converts an Sfloat to a Sint
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param f The Sfloat
     * @return f converted to a Sint
     */
    Sint f2i(SymbolicExecution se, Sfloat f);

    /**
     * Converts an Sfloat to a Slong
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param f The Sfloat
     * @return f converted to a Slong
     */
    Slong f2l(SymbolicExecution se, Sfloat f);

    /**
     * Converts an Sfloat to a Sdouble
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param f The Sfloat
     * @return f converted to a Sdouble
     */
    Sdouble f2d(SymbolicExecution se, Sfloat f);

    /**
     * Converts an Sdouble to a Sint
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param d The Sdouble
     * @return d converted to a Sint
     */
    Sint d2i(SymbolicExecution se, Sdouble d);

    /**
     * Converts an Sdouble to a Slong
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param d The Sdouble
     * @return d converted to a Slong
     */
    Slong d2l(SymbolicExecution se, Sdouble d);

    /**
     * Converts an Sdouble to a Sfloat
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param d The Sdouble
     * @return d converted to a Sfloat
     */
    Sfloat d2f(SymbolicExecution se, Sdouble d);

    /**
     * Converts an Sint to a Sbyte
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return d converted to a Sbyte
     */
    Sbyte i2b(SymbolicExecution se, Sint i);

    /**
     * Converts an Sint to a Sshort
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i The Sint
     * @return d converted to a Sshort
     */
    Sshort i2s(SymbolicExecution se, Sint i);

    /**
     * Shift a Sint left
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The Sint to-be-shifted
     * @param i1 The Sint to-shift-by
     * @return i0 << i1
     */
    Sint ishl(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * Shift a Sint right
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The Sint to-be-shifted
     * @param i1 The Sint to-shift-by
     * @return i0 >> i1
     */
    Sint ishr(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * XOR the Sints
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The first Sint
     * @param i1 The second Sint
     * @return i0 ^ i1
     */
    Sint ixor(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * OR the Sints
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The first Sint
     * @param i1 The second Sint
     * @return i0 | i1
     */
    Sint ior(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * Logically shift a Sint right
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The Sint to-be-shifted
     * @param i1 The Sint to-shift-by
     * @return i0 >>> i1
     */
    Sint iushr(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * AND the Sints
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param i0 The first Sint
     * @param i1 The second Sint
     * @return i0 & i1
     */
    Sint iand(SymbolicExecution se, Sint i0, Sint i1);

    /**
     * Shift a Slong left
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong to-be-shifted
     * @param i The Sint to-shift-by
     * @return l << i
     */
    Slong lshl(SymbolicExecution se, Slong l, Sint i);

    /**
     * Shift a Slong right
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong to-be-shifted
     * @param i The Sint to-shift-by
     * @return l >> i
     */
    Slong lshr(SymbolicExecution se, Slong l, Sint i);

    /**
     * XOR the Slongs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l0 The first Slong
     * @param l1 The second Slong
     * @return l0 ^ l1
     */
    Slong lxor(SymbolicExecution se, Slong l0, Slong l1);

    /**
     * OR the Slongs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l0 The first Slong
     * @param l1 The second Slong
     * @return l0 | l1
     */
    Slong lor(SymbolicExecution se, Slong l0, Slong l1);

    /**
     * Logically shift a Slong left
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l The Slong to-be-shifted
     * @param i The Sint to-shift-by
     * @return l >>> i
     */
    Slong lushr(SymbolicExecution se, Slong l, Sint i);

    /**
     * AND the Slongs
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param l0 The first Slong
     * @param l1 The second Slong
     * @return l0 & l1
     */
    Slong land(SymbolicExecution se, Slong l0, Slong l1);

    /**
     * This method should only be called if the object for which a field is accessed is represented for or in the constraint
     * solver. The {@link PartnerClass} must not be a {@link Sarray}.
     * A respective {@link de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint} with
     * {@link PartnerClassObjectFieldConstraint#getType()} == {@link de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint.Type#GETFIELD}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param pco The object containing the field
     * @param field The field name. It should be given in the format packageName.className.fieldName
     * @param fieldClass The type of the field
     * @return A {@link SubstitutedVar} representing the content of the object at a specified field with a specified type.
     * If the returned value is an instance of {@link PartnerClass}, {@link PartnerClass#__mulib__getId()} probably
     * returns a symbolic value that indicates that this object is a symbolic alias of a set of objects.
     */
    SubstitutedVar getField(SymbolicExecution se, PartnerClass pco, String field, Class<?> fieldClass);

    /**
     * This method should only be called if the object for which a field is accessed is represented for or in the constraint
     * solver. The {@link PartnerClass} must not be a {@link Sarray}.
     * If the value stored by this method is an instance of {@link PartnerClass}, it will be represented for the solver and a
     * respective {@link de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint} with
     * {@link PartnerClassObjectFieldConstraint#getType()} == {@link de.wwu.mulib.constraints.PartnerClassObjectFieldConstraint.Type#PUTFIELD}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param pco The object containing the field
     * @param field The field name. It should be given in the format packageName.className.fieldName
     * @param value A {@link SubstitutedVar} representing the content that is stored into a specified field.
     */
    void putField(SymbolicExecution se, PartnerClass pco, String field, SubstitutedVar value);

    /**
     * This method is called to retrieve the content of a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint} is pushed
     * to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#SELECT}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param sarray The array from which we read
     * @param index The index with which we read from the array
     * @return A {@link Sprimitive} representing the content of the object at the specified index with a specified type.
     */
    Sprimitive select(SymbolicExecution se, Sarray sarray, Sint index);

    /**
     * This method is called to store content into a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint} is pushed
     * to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#STORE}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param sarray The array into which we store
     * @param index The index with which we store into the array
     * @param value The value that we store into the Sarray
     * @return A {@link Sarray} representing the content of the object at the specified index with a specified type.
     */
    Sprimitive store(SymbolicExecution se, Sarray sarray, Sint index, Sprimitive value);

    /**
     * This method is called to retrieve the content of a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint} is pushed
     * to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#SELECT}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param sarraySarray The array from which we read
     * @param index The index with which we read from the array
     * @return A {@link Sarray} representing the content of the object at the specified index with a specified type.
     * {@link PartnerClass#__mulib__getId()} probably returns a symbolic value that indicates that this object is a
     * symbolic alias of a set of objects.
     */
    Sarray<?> select(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index);

    /**
     * This method is called to store content into a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. If the sarray is represented for the solver, the representation for the solver is also
     * triggered for the value that is stored into it.
     * In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint} is pushed
     * to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#STORE}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param sarraySarray The array into which we store
     * @param index The index with which we store into the array
     * @param value The value that we store into the Sarray
     * @return A {@link Sarray} representing the content of the object at the specified index with a specified type.
     */
    Sarray<?> store(SymbolicExecution se, Sarray.SarraySarray sarraySarray, Sint index, SubstitutedVar value);

    /**
     * This method is called to retrieve the content of a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint} is pushed
     * to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#SELECT}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param partnerClassSarray The array from which we read
     * @param index The index with which we read from the array
     * @return A {@link PartnerClass} representing the content of the object at the specified index with a specified type.
     * {@link PartnerClass#__mulib__getId()} probably returns a symbolic value that indicates that this object is a
     * symbolic alias of a set of objects.
     */
    PartnerClass select(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index);

    /**
     * This method is called to store content into a Sarray.
     * This method can trigger the representation of an array for/in the constraint solver if the array is accessed
     * with a symbolic index. If the sarray is represented for the solver, the representation for the solver is also
     * triggered for the value that is stored into it.
     * In this case, a {@link de.wwu.mulib.constraints.ArrayInitializationConstraint}/a {@link de.wwu.mulib.constraints.PartnerClassObjectInitializationConstraint}
     * is pushed to the constraint solver.
     * If the array is already represented for/in the solver, a {@link de.wwu.mulib.constraints.ArrayAccessConstraint}
     * with {@link ArrayAccessConstraint#getType()} == {@link de.wwu.mulib.constraints.ArrayAccessConstraint.Type#STORE}
     * is added to the constraint solver.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param partnerClassSarray The array into which we store
     * @param index The index with which we store into the array
     * @param value The value that we store into the Sarray
     * @return A {@link Sarray} representing the content of the object at the specified index with a specified type.
     */
    PartnerClass store(SymbolicExecution se, Sarray.PartnerClassSarray<?> partnerClassSarray, Sint index, SubstitutedVar value);

    /**
     * Represents the partner class object, i.e., either a {@link Sarray} or a other {@link PartnerClass} for/in the constraint solver.
     * This method accounts for the context of initialization via its parameters where we potentially can represent an object
     * in a field, in a Sarray-index, or without any further information.
     * To be represented the object's {@link PartnerClass#__mulib__shouldBeRepresentedInSolver()} must return true.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param toPotentiallyRepresent The object that is potentially represented.
     * @param idOfContainingPartnerClassObject The identifier, if any, of the object in which the new object is
     *                                         initialized for/in the solver. Can be null. If this is not null, either
     *                                         fieldName OR index must be set.
     * @param fieldName The name of the field that contains the object that is about to be represented.
     *                  This name must follow the pattern packageName.className.fieldName.
     *                  Can be null.
     *                  Must be null if idOfContainingPartnerClassObject is null.
     * @param index The index of the Sarray containing this object. Can be null. Must be null if idOfContainingPartnerClassObject is null.
     */
    void representPartnerClassObjectIfNeeded(SymbolicExecution se, PartnerClass toPotentiallyRepresent, Sint idOfContainingPartnerClassObject, String fieldName, Sint index);

    /**
     * Initializes the fields of an object that is to be lazily initialized.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param partnerClassObject The partner class object the fields of which should be lazily initialized.
     *                           Must not be a Sarray.
     *                           {@link PartnerClass#__mulib__defaultIsSymbolic()} must evaluate to true for this object,
     *                           i.e., the object must be created symbolically.
     */
    void initializeLazyFields(SymbolicExecution se, PartnerClass partnerClassObject);

    /**
     * Is used for efficient evaluation of an instance of {@link PartnerClass}'s representation for/in the constraint solver.
     * For instance, we can check whether the content of a field can potentially be null due to symbolic aliasing or
     * an according configuration. By doing so, we avoid conservatively initializing symbolic values for
     * {@link PartnerClass#__mulib__isNull()}.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param containingObject The object containing the field. Must not be a Sarray.
     *                         Must be represented for/in the constraint, i.e. {@link PartnerClass#__mulib__isRepresentedInSolver()}
     *                         must evaluate to true.
     * @param field The field on which further information is requested for
     * @return An object containing various metainformation on the containingObject and its field.
     */
    PartnerClassObjectInformation getAvailableInformationOnPartnerClassObject(SymbolicExecution se, PartnerClass containingObject, String field);

    /**
     * Is used for efficient evaluation of an instance of {@link Sarray}'s representation for/in the constraint solver.
     * For instance, we can check whether the content of a field can potentially be null due to symbolic aliasing or
     * an according configuration. By doing so, we avoid conservatively initializing symbolic values for
     * {@link PartnerClass#__mulib__isNull()}.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param containingObject The object containing the field.
     *                         Must be represented for/in the constraint, i.e. {@link PartnerClass#__mulib__isRepresentedInSolver()}
     *                         must evaluate to true.
     * @return An object containing various metainformation on the containingObject and its field.
     */
    ArrayInformation getAvailableInformationOnArray(SymbolicExecution se, Sarray<?> containingObject);

    /**
     * Remembers a value. If the value is a {@link Sarray} or another {@link PartnerClass} object a special trail or a deep-copy
     * is constructed.
     * @param se The instance of {@link SymbolicExecution} used for this execution run
     * @param name The name to remember the value by
     * @param toRemember The value
     */
    void remember(SymbolicExecution se, String name, SubstitutedVar toRemember);

}
