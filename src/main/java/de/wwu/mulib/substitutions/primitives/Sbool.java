package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.constraints.Constraint;
import de.wwu.mulib.expressions.NumericalExpression;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.ValueFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents boolean values
 */
public abstract class Sbool extends Sint implements Sprimitive, Constraint {

    private Sbool() {}

    /**
     * @param b A boolean to wrap
     * @return {@link ConcSbool#TRUE} if b is true, else {@link ConcSbool#FALSE}
     */
    public static Sbool.ConcSbool concSbool(boolean b) {
        return b ? ConcSbool.TRUE : ConcSbool.FALSE;
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @return A new leaf
     */
    public static Sbool.SymSbool newInputSymbolicSbool() {
        return new SymSboolLeaf();
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param id The identifier of the leaf
     * @return A new leaf
     */
    public static SymSbool newInputSymbolicSbool(long id) {
        return new SymSboolLeaf(id);
    }

    /**
     * Should never be used in the search region directly. Should either be called by the
     * {@link de.wwu.mulib.solving.solvers.SolverManager}-backend, or a {@link ValueFactory}
     * @param c The constraint to wrap
     * @return A symbolic value wrapping a constraint
     */
    public static Sbool.SymSbool newConstraintSbool(Constraint c) {
        assert !(c instanceof SymSbool || c instanceof ConcSbool);
        return new SymSbool(c);
    }

    /**
     * @param rhs The other bool
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The wrapped result of this ^ rhs
     */
    public final Sbool xor(Sbool rhs, SymbolicExecution se) {
        return se.xor(this, rhs);
    }

    /**
     * @param rhs The other bool
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The wrapped result of this && rhs
     */
    public final Sbool and(Sbool rhs, SymbolicExecution se) {
       return se.and(this, rhs);
    }

    /**
     * @param rhs The other bool
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The wrapped result of this || rhs
     */
    public final Sbool or(Sbool rhs, SymbolicExecution se) {
        return se.or(this, rhs);
    }

    @Override
    public Sint ixor(Sint i, SymbolicExecution se) {
        if (i instanceof Sbool) {
            return se.xor(this, (Sbool) i);
        }
        return se.ixor(this, i);
    }

    @Override
    public Sint ior(Sint i, SymbolicExecution se) {
        if (i instanceof Sbool) {
            return se.or(this, (Sbool) i);
        }
        return se.ior(this, i);
    }

    @Override
    public Sint iand(Sint i, SymbolicExecution se) {
        if (i instanceof Sbool) {
            return se.and(this, (Sbool) i);
        }
        return se.iand(this, i);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The wrapped result of !this
     */
    public final Sbool not(SymbolicExecution se) {
        return se.not(this);
    }

    /**
     * @param rhs The other bool
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return The wrapped result of this == rhs
     */
    public final Sbool isEqualTo(Sbool rhs, SymbolicExecution se) {
        return se.or(se.and(this, rhs), se.and(se.not(this), se.not(rhs)));
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true if we assume 'this' to be true in the following, else false
     */
    public final boolean boolChoice(SymbolicExecution se) {
        return se.boolChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true if we assume 'this' to be false in the following, else false
     */
    public final boolean negatedBoolChoice(SymbolicExecution se) {
        return se.negatedBoolChoice(this);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true if we assume 'this == other' to be true in the following, else false
     */
    public final boolean boolChoice(Sbool other, SymbolicExecution se) {
        return se.boolChoice(isEqualTo(other, se));
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @return true if we assume 'this != other' to be true in the following, else false
     */
    public final boolean negatedBoolChoice(Sbool other, SymbolicExecution se) {
        return se.boolChoice(se.or(se.and(se.not(this), other), se.and(this, se.not(other))));
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to find this choice point in the control flow graph
     * @return true if we assume 'this' to be true in the following, else false
     */
    public final boolean boolChoice(SymbolicExecution se, long id) {
        return se.boolChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to find this choice point in the control flow graph
     * @return true if we assume 'this' to be false in the following, else false
     */
    public final boolean negatedBoolChoice(SymbolicExecution se, long id) {
        return se.negatedBoolChoice(this, id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to find this choice point in the control flow graph
     * @return true if we assume 'this == other' to be true in the following, else false
     */
    public final boolean boolChoice(Sbool other, SymbolicExecution se, long id) {
        return se.boolChoice(isEqualTo(other, se), id);
    }

    /**
     * @param se The current instance of {@link SymbolicExecution} for this run
     * @param id The identifier to find this choice point in the control flow graph
     * @return true if we assume 'this != other' to be true in the following, else false
     */
    public final boolean negatedBoolChoice(Sbool other, SymbolicExecution se, long id) {
        return se.boolChoice(se.or(se.and(se.not(this), other), se.and(this, se.not(other))), id);
    }

    /**
     * Class representing a concrete Sbool
     */
    public static final class ConcSbool extends Sbool implements ConcSnumber {
        /**
         * True
         */
        public final static ConcSbool TRUE = new ConcSbool(true);
        /**
         * False
         */
        public final static ConcSbool FALSE = new ConcSbool(false);
        private final boolean value;
        ConcSbool(final boolean value) {
            this.value = value;
        }

        /**
         * @return true, if a true value is represented, else false
         */
        public boolean isTrue() {
            return value;
        }

        /**
         * @return false, if a true value is represented, else true
         */
        public boolean isFalse() {
            return !value;
        }

        /**
         * @return The negated ConcSbool
         */
        public Sbool negate() {
            return isTrue() ? FALSE : TRUE;
        }

        @Override
        public int hashCode() {
            return intVal();
        }

        @Override
        public int intVal() {
            return value ? 1 : 0;
        }

        @Override
        public double doubleVal() {
            return intVal();
        }

        @Override
        public float floatVal() {
            return intVal();
        }

        @Override
        public long longVal() {
            return intVal();
        }

        @Override
        public short shortVal() {
            return (short) intVal();
        }

        @Override
        public byte byteVal() {
            return (byte) intVal();
        }

        @Override
        public char charVal() {
            return (char) intVal();
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Class for wrapping constraints
     */
    public static class SymSbool extends Sbool implements SymSprimitive, SymSnumber {
        private final Constraint representedConstraint;

        private SymSbool() {
            this.representedConstraint = this;
        }

        private SymSbool(Constraint representedConstraint) {
            this.representedConstraint = representedConstraint;
        }

        /**
         * @return The represented constraint
         */
        public final Constraint getRepresentedConstraint() {
            return representedConstraint;
        }

        @Override
        public NumericalExpression getRepresentedExpression() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != getClass()) {
                return false;
            }
            return representedConstraint.equals(((SymSbool) o).representedConstraint);
        }

        @Override
        public int hashCode() {
            return representedConstraint.hashCode();
        }

        @Override
        public String toString() {
            return "SymSbool{" + representedConstraint.toString() + "}";
        }
    }

    /**
     * Class for representing simple symbolic booleans; - there are no composite constrains here
     */
    public static class SymSboolLeaf extends SymSbool implements SymSprimitiveLeaf {
        private static final AtomicLong nextId = new AtomicLong(0);
        private final String id;

        SymSboolLeaf() {
            id = "Sbool" + nextId.incrementAndGet();
        }

        SymSboolLeaf(long nextId) {
            id = "Sbool" + nextId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != getClass()) {
                return false;
            }
            return id.equals(((SymSprimitiveLeaf) o).getId());
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
