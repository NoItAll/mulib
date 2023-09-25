package de.wwu.mulib.examples.efficient_bytecode_folding;

import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClassObject;
import de.wwu.mulib.substitutions.Sarray;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;

public class NQueensEfficient {

    static class Queen extends PartnerClassObject {
        Sint x, y;
        Queen(Sint x, Sint y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Board extends PartnerClassObject {
        final Sint dimension;
        Board(Sint dimension) {
            this.dimension = dimension;
        }
        public Sbool isOnBoardEfficient(Queen q) {
            SymbolicExecution se = SymbolicExecution.get();
            return se.and(
                    se.gte(q.x, se.concSint(0)),
                    se.and(se.lte(q.x, dimension.sub(se.concSint(1), se)),
                            se.and(se.gte(q.y, se.concSint(0)),
                                    se.lte(q.y, dimension.sub(se.concSint(1), se))))
            );
        }

        public Sbool notThreatensEfficient(Queen p, Queen q) {
            SymbolicExecution se = SymbolicExecution.get();
            Sbool b0 = se.eq(p.x, q.x);
            Sbool b1 = se.eq(p.y, q.y);
            Sbool b2 = se.eq(p.x.sub(p.y, se), q.x.sub(q.y, se));
            Sbool b3 = se.eq(p.x.add(p.y, se), q.x.add(q.y, se));
            return se.not(b0.or(b1, se).or(b2, se).or(b3, se));
        }
    }

    public static Sarray.PartnerClassSarray<Queen> solveEfficient(Sint dimensionality) {
        SymbolicExecution se = SymbolicExecution.get();
        Board board = new Board(dimensionality);
        Sarray.PartnerClassSarray<Queen> qs = se.partnerClassSarray(dimensionality, Queen.class, false);
        for (int i = 0; Sint.concSint(i).ltChoice(dimensionality, se); i++) {
            qs.store(Sint.concSint(i), new Queen(se.concSint(i), se.namedSymSint("y" + i)), se);
        }
        for (int i = 0; Sint.concSint(i).ltChoice(dimensionality, se); i++) {
            se.assume(board.isOnBoardEfficient(qs.select(Sint.concSint(i), se)));
            for (int j = i+1; Sint.concSint(j).ltChoice(dimensionality, se); j++) {
                Sbool notThreatens = board.notThreatensEfficient(qs.select(Sint.concSint(i), se), qs.select(Sint.concSint(j), se));
                se.assume(notThreatens);
            }
        }
        return qs;
    }
}
