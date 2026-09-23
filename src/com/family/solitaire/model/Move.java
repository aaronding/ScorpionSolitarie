/*
 * Move.java
 */

package com.family.solitaire.model;

/**
 * Moves the card at {@code fromRow} of {@code fromColumn}, and every card below
 * it, onto the end of {@code toColumn}.
 *
 * @author Aaron Ding
 */
public final class Move {

    public Move(int fromColumn, int fromRow, int toColumn) {
        this.fromColumn = fromColumn;
        this.fromRow = fromRow;
        this.toColumn = toColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Move))
            return false;
        Move m = (Move)o;
        return m.fromColumn == fromColumn && m.fromRow == fromRow && m.toColumn == toColumn;
    }

    @Override
    public int hashCode() {
        return (fromColumn * 64 + fromRow) * 8 + toColumn;
    }

    @Override
    public String toString() {
        return fromColumn + "." + fromRow + " -> " + toColumn;
    }

    public final int fromColumn;
    public final int fromRow;
    public final int toColumn;
}
