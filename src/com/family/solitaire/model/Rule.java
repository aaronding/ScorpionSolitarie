package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.CARDS_IN_SUIT;
import static com.family.solitaire.model.CardConstants.NCOLS;

import java.util.ArrayList;
import java.util.List;

import com.family.solitaire.model.Card.Rank;

/**
 * The rules for each difficulty level. The levels differ in which card may be
 * placed on which, and in how far the player may undo; everything else is shared.
 *
 * @author Aaron Ding
 */
public enum Rule {

    /** Build down in any suit. Every step can be undone. */
    EASY("easy") {
        public boolean canStack(Card card, Card onto) {
            return oneRankBelow(card, onto);
        }
        public boolean mayUndo(Step step) {
            return true;
        }
    },

    /** Build down in the same color. Undo reaches back to, but not past, the reserve deal. */
    MEDIUM("medium") {
        public boolean canStack(Card card, Card onto) {
            return card.color() == onto.color() && oneRankBelow(card, onto);
        }
        public boolean mayUndo(Step step) {
            return !step.isReserveDeal();
        }
    },

    /** Build down in the same suit. No undo. */
    DIFFICULT("difficult") {
        public boolean canStack(Card card, Card onto) {
            return card.suit() == onto.suit() && oneRankBelow(card, onto);
        }
        public boolean mayUndo(Step step) {
            return false;
        }
    };

    Rule(String level) {
        this.level = level;
    }

    /** True if {@code card} (with anything below it) may be placed on {@code onto}. */
    public abstract boolean canStack(Card card, Card onto);

    /** True if the player may take back {@code step}, the most recent one. */
    public abstract boolean mayUndo(Step step);

    /** The name used in save files. */
    public String getLevel() {
        return level;
    }

    public static Rule forLevel(String level) {
        for (Rule rule : values()) {
            if (rule.level.equalsIgnoreCase(level.trim()))
                return rule;
        }
        throw new IllegalArgumentException("invalid level:" + level);
    }

    public boolean isValidMove(Move move, Board board) {
        if (move.toColumn < 0 || move.toColumn >= NCOLS || move.fromColumn == move.toColumn)
            return false;
        Column from = board.getColumn(move.fromColumn);
        if (move.fromRow < 0 || move.fromRow >= from.getSize())
            return false;
        Card card = from.getCard(move.fromRow);
        if (card.isFacedDown())
            return false;

        Column to = board.getColumn(move.toColumn);
        if (to.isEmpty())
            return card.rank() == Rank.KING;
        return canStack(card, to.getLastCard());
    }

    /** Won when every column is empty or runs King down to Ace in one suit. */
    public boolean checkResult(Board board) {
        for (int j=0; j<NCOLS; j++) {
            Column column = board.getColumn(j);
            if (column.isEmpty())
                continue;
            if (column.getSize() != CARDS_IN_SUIT || column.getCard(0).rank() != Rank.KING)
                return false;
            for (int i=1; i<CARDS_IN_SUIT; i++) {
                if (column.getCard(i-1).value() - column.getCard(i).value() != 1)
                    return false;
            }
        }
        return true;
    }

    /** Moves worth suggesting as hints; empty if there are none. */
    public List<Move> getAvailableMoves(Board board) {
        List<Move> ret = new ArrayList<Move>();
        for (int j=0; j<NCOLS; j++) {
            Card target = board.getColumn(j).getLastCard();
            for (Card card : candidatesFor(target)) {
                Position pos = board.findFaceUpCard(card);
                if (pos == null || pos.column == j)
                    continue;
                if (card.rank() == Rank.KING && pos.row == 0)
                    continue;
                if (pos.row != 0 && target != null) {
                    // Skip a card already on the next card up in its own suit:
                    // moving it off never helps.
                    Card above = board.getColumn(pos.column).getCard(pos.row-1);
                    if (above.isFacedUp() && above.suit() == card.suit()
                            && above.rank().ordinal() - card.rank().ordinal() == 1)
                        continue;
                }
                ret.add(new Move(pos.column, pos.row, j));
            }
        }
        return ret;
    }

    // Cards that may go on target: Kings for an empty column, same suit first.
    private List<Card> candidatesFor(Card target) {
        List<Card> ret = new ArrayList<Card>(4);
        if (target == null) {
            for (Card.Suit suit : Card.Suit.values())
                ret.add(new Card(Rank.KING, suit));
            return ret;
        }
        if (target.rank() == Rank.ACE)
            return ret;
        Rank below = Rank.values()[target.rank().ordinal()-1];
        ret.add(new Card(below, target.suit()));
        for (Card.Suit suit : Card.Suit.values()) {
            Card card = new Card(below, suit);
            if (suit != target.suit() && canStack(card, target))
                ret.add(card);
        }
        return ret;
    }

    private static boolean oneRankBelow(Card card, Card onto) {
        return onto.rank().ordinal() - card.rank().ordinal() == 1;
    }

    private final String level;
}
