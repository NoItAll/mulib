package de.wwu.mulib.expressions;

public class Neg extends AbstractExpressionWrappingExpression {

    private Neg(NumericExpression wrapped) {
        super(wrapped);
    }

    public static NumericExpression neg(NumericExpression wrapped) {
        return wrapped instanceof Neg ?
                ((Neg) wrapped).getWrapped()
                :
                new Neg(wrapped);
    }

    @Override
    public String toString() {
        return "-(" + wrapped + ")";
    }
}
