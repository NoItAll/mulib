package de.wwu.mulib.search.trees;

import java.util.BitSet;

/**
 * Indicator interface for {@link PathSolution}s that have a cover
 */
public interface IPathSolutionWithBitSetCover {

    /**
     * @return The cover associated with this path solution
     */
    BitSet getCover();

}
