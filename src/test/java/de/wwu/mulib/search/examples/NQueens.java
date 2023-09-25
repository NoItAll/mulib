package de.wwu.mulib.search.examples;

import de.wwu.mulib.Mulib;
import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.search.trees.ThrowablePathSolution;
import de.wwu.mulib.solving.Solution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import de.wwu.mulib.throwables.MulibRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class NQueens {
    private final static int dimension = 8;
    private final static int higherDimension = 12;

    @Test
    public void checkExecute() {
        TestUtility.getAllSolutions(this::_checkExecute, "solve");
    }

    private List<PathSolution> _checkExecute(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "solve",
                NQueens.class,
                mb,
                false
        );
        assertEquals(1, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ThrowablePathSolution));
        return result;
    }

    @Test
    public void checkExecuteAlt() {
        TestUtility.getAllSolutions(this::_checkExecuteAlt, "solveAlt");
    }

    @Test
    public void checkExecuteEfficient() {
        TestUtility.getAllSolutions(this::_checkExecuteEfficient, "solveEfficient");
    }

    private List<Solution> _checkExecuteAlt(MulibConfig.MulibConfigBuilder mb) {
        List<Solution> result = TestUtility.getUpToNSolutions(
                95, // Only 92 solutions are possible
                "solveAlt",
                NQueens.class,
                mb,
                false,
                new Class[0],
                new Object[0]
        );
        assertEquals(92, result.size());
        return result;
    }

    public void checkEfficient() {
        TestUtility.getSolution(this::_checkExecuteEfficient);
    }

    private Optional<PathSolution> _checkExecuteEfficient(MulibConfig.MulibConfigBuilder config) {
        Optional<PathSolution> result = TestUtility.executeMulibForOne(
                "solveEfficient",
                NQueens.class,
                config,
                false
        );
        assertFalse(result.isEmpty());
        return result;
    }

    static class Queen {
        Sint x, y;
        Queen(Sint x, Sint y) {
            this.x = x;
            this.y = y;
        }
    }

    static class Board {
        final int dimension;
        Board(int dimension) {
            this.dimension = dimension;
        }

        public boolean isOnBoard(Queen q) {
            SymbolicExecution se = SymbolicExecution.get();
            if (se.ltChoice(q.x, se.concSint(0)))
                return false;
            if (se.gtChoice(q.x, se.concSint(dimension).sub(se.concSint(1), se)))
                return false;
            if (se.ltChoice(q.y, se.concSint(0)))
                return false;
            if (se.gtChoice(q.y, se.concSint(dimension).sub(se.concSint(1), se)))
                return false;
            return true;
        }

        public Sbool isOnBoardEfficient(Queen q) {
            SymbolicExecution se = SymbolicExecution.get();
            return se.and(
                            se.gte(q.x, se.concSint(0)),
                            se.and(se.lte(q.x, se.concSint(dimension).sub(se.concSint(1), se)),
                            se.and(se.gte(q.y, se.concSint(0)),
                            se.lte(q.y, se.concSint(dimension).sub(se.concSint(1), se))))
            );
        }

        public boolean threatens(Queen p, Queen q) {
            SymbolicExecution se = SymbolicExecution.get();
            if (se.eqChoice(p.x, q.x))
                return true;
            if (se.eqChoice(p.y, q.y))
                return true;
            if (se.eqChoice(p.x.sub(p.y,se ), q.x.sub(q.y, se)))
                return true;
            if (se.eqChoice(p.x.add(p.y, se), q.x.add(q.y, se)))
                return true;
            return false;
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

    public static Queen[] solve() {
        SymbolicExecution se = SymbolicExecution.get();
        Board board = new Board(dimension);
        Queen[] qs = new Queen[dimension];
        for (int i = 0; i < dimension; i++) {
            qs[i] = new Queen(se.namedSymSint("x" + i), se.namedSymSint("y" + i));
        }
        for (int i = 0; i < dimension; i++) {
            boolean valid = board.isOnBoard(qs[i]);
            if (!valid) {
                throw Mulib.fail();
            }

            for (int j = i+1; j < dimension; j++) {
                boolean threatens = board.threatens(qs[i], qs[j]);
                if (threatens) {
                    throw Mulib.fail();
                }
            }
        }
        return qs;
    }

    public static Queen[] solveAlt() {
        SymbolicExecution se = SymbolicExecution.get();
        Board board = new Board(dimension);
        Queen[] qs = new Queen[dimension];
        for (int i = 0; i < dimension; i++) {
            qs[i] = new Queen(se.concSint(i), se.namedSymSint("y" + i));
        }
        for (int i = 0; i < dimension; i++) {
            boolean valid = board.isOnBoard(qs[i]);
            if (!valid) {
                throw Mulib.fail();
            }

            Sbool b0 = se.symSbool();
            if (se.boolChoice(b0)) {
                throw Mulib.fail();
            }

            if (!board.isOnBoard(qs[i])) {
                throw new MulibRuntimeException("Should not be possible");
            }

            for (int j = i+1; j < dimension; j++) {
                boolean threatens = board.threatens(qs[i], qs[j]);
                if (threatens) {
                    Sbool b1 = se.namedSymSbool("notImportant");
                    if (se.boolChoice(b1)) {
                        throw Mulib.fail();
                    }
                    throw Mulib.fail();
                }
                if (board.threatens(qs[i], qs[j])) {
                    throw new MulibRuntimeException("Should not be possible");
                }
            }
        }
        return qs;
    }

    public static Queen[] solveEfficient() {
        SymbolicExecution se = SymbolicExecution.get();
        Board board = new Board(higherDimension);
        Queen[] qs = new Queen[higherDimension];
        for (int i = 0; i < higherDimension; i++) {
            qs[i] = new Queen(se.concSint(i), se.namedSymSint("y" + i));
        }
        for (int i = 0; i < higherDimension; i++) {
            se.assume(board.isOnBoardEfficient(qs[i]));

            for (int j = i+1; j < higherDimension; j++) {
                Sbool notThreatens = board.notThreatensEfficient(qs[i], qs[j]);
                se.assume(notThreatens);
            }
        }
        return qs;
    }
}
