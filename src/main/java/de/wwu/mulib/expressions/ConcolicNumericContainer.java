package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.primitives.ConcSnumber;

public final class ConcolicNumericContainer implements NumericExpression {

    private final NumericExpression sym;
    private ConcSnumber conc;

    public ConcolicNumericContainer(NumericExpression sym, ConcSnumber conc) {
        assert !(sym instanceof ConcolicNumericContainer);
        this.sym = sym;
        this.conc = conc;
    }

    @Override
    public boolean isFp() {
        return sym.isFp();
    }

    public NumericExpression getSym() {
        return sym;
    }

    public ConcSnumber getConc() {
        return conc;
    }

    public void labelAnew(ConcSnumber conc) {
        this.conc = conc;
    }
}
