package de.wwu.mulib.expressions;

import de.wwu.mulib.substitutions.primitives.ConcSnumber;
import de.wwu.mulib.substitutions.primitives.Snumber;
import de.wwu.mulib.substitutions.primitives.SymNumericExpressionSprimitive;

public final class ConcolicNumericContainer implements NumericExpression {

    private final SymNumericExpressionSprimitive sym;
    private ConcSnumber conc;

    public ConcolicNumericContainer(SymNumericExpressionSprimitive sym, ConcSnumber conc) {
        this.sym = sym;
        this.conc = conc;
    }

    @Override
    public boolean isFp() {
        return sym.isFp();
    }

    public SymNumericExpressionSprimitive getSym() {
        return sym;
    }

    public ConcSnumber getConc() {
        return conc;
    }

    public void labelAnew(ConcSnumber conc) {
        this.conc = conc;
    }

    public static Snumber tryGetSymFromConcolic(Snumber ne) {
        if (ne instanceof SymNumericExpressionSprimitive) {
            SymNumericExpressionSprimitive sym = (SymNumericExpressionSprimitive) ne;
            if (sym.getRepresentedExpression() instanceof ConcolicNumericContainer) {
                return ((ConcolicNumericContainer) sym.getRepresentedExpression()).getSym();
            }
        }
        return ne;
    }

    public static ConcSnumber getConcNumericFromConcolic(NumericExpression ne) {
        return ne instanceof SymNumericExpressionSprimitive ?
                ((ConcolicNumericContainer) ((SymNumericExpressionSprimitive) ne).getRepresentedExpression()).getConc()
                :
                (ConcSnumber) ne;
    }

    @Override
    public String toString() {
        return "ConcolicNumericContainer{conc=" + conc + ",sym=" + sym + "}";
    }

    @Override
    public int hashCode() {
        return sym.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConcolicNumericContainer && ((ConcolicNumericContainer) o).getSym() == sym;
    }
}
