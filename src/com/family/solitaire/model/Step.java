package com.family.solitaire.model;

/**
 * One entry in a game's history: either a move, or dealing the reserve.
 * Records what is needed to take it back.
 *
 * @author Aaron Ding
 */
public final class Step {

    static Step move(Move move, int count, boolean flipped) {
        return new Step(move, count, flipped);
    }

    static Step reserveDeal() {
        return new Step(null, 0, false);
    }

    private Step(Move move, int count, boolean flipped) {
        this.move = move;
        this.count = count;
        this.flipped = flipped;
    }

    public boolean isReserveDeal() { return move == null; }

    /** The move, or null for a reserve deal. */
    public Move getMove() { return move; }

    /** How many cards the move carried. */
    public int getCount() { return count; }

    /** True if the move turned over the card it uncovered. */
    public boolean flipped() { return flipped; }

    private final Move move;
    private final int count;
    private final boolean flipped;
}
