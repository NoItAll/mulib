package de.wwu.mulib.expressions;

import de.wwu.mulib.constraints.Constraint;

/**
 * Abstract supertype for conditional values. There always is a condition deciding on which of two
 * values is valid.
 * @param <T> The type of element
 */
public abstract class IfThenElse<T> {
    protected final Constraint condition;
    protected final T ifCase;
    protected final T elseCase;

    protected IfThenElse(Constraint condition, T ifCase, T elseCase) {
        this.condition = condition;
        this.ifCase = ifCase;
        this.elseCase = elseCase;
    }

    /**
     * @return The condition
     */
    public Constraint getCondition() {
        return condition;
    }

    /**
     * @return The value, given {@link IfThenElse#getCondition()} is true
     */
    public T getIfCase() {
        return ifCase;
    }

    /**
     * @return The value, given {@link IfThenElse#getCondition()} is false
     */
    public T getElseCase() {
        return elseCase;
    }

    @Override @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (!(o instanceof IfThenElse)) {
            return false;
        }
        IfThenElse ite = (IfThenElse) o;
        if (ite.hashCode() != hashCode()) {
            return false;
        }
        return condition.equals(ite.condition)
                && ifCase.equals(ite.ifCase)
                && elseCase.equals(ite.elseCase);
    }

    @Override
    public int hashCode() {
        return condition.hashCode();
    }
}
