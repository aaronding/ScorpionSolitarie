/*
 * Board.java
 *
 * Created on December 3, 2006, 7:26 PM
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCOLS;
import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

import java.util.List;

/**
 * The seven columns and the reserve. Applies and reverses steps without
 * checking the rules; {@link Game} does that.
 *
 * @author Aaron
 */
public class Board {

    Board() {
        columns = new Column[NCOLS];
        for (int i=0; i<NCOLS; i++)
            columns[i] = new Column(i);
        reserve = new Reserve();
    }

    public Column getColumn(int index) { return columns[index]; }
    public Reserve getReserve() { return reserve; }

    public Card getCard(int row, int column) {
        return columns[column].getCard(row);
    }

    public Position findFaceUpCard(Card card) {
        for (int i=0; i<NCOLS; i++) {
            int row = columns[i].findCard(card);
            if (row != -1 && columns[i].getCard(row).isFacedUp())
                return new Position(row, i);
        }
        return null;
    }

    void clear() {
        for (Column column : columns)
            column.clear();
        reserve.clear();
    }

    /** Seven cards to each column, then three to the reserve. */
    void deal(Deck deck) {
        clear();
        for (int j=0; j<NCOLS; j++) {
            for (int i=0; i<7; i++) {
                Card card = deck.getCard(j*7+i);
                if (j<4 && i<3)
                    card.faceDown();
                else
                    card.faceUp();
                columns[j].add(card);
            }
        }
        Card[] t = new Card[RESERVE_SIZE];
        for (int k=0; k<RESERVE_SIZE; k++) {
            t[k] = deck.getCard(k+49);
            t[k].faceDown();
        }
        reserve.putCards(t);
    }

    void addCard(int column, Card card) {
        columns[column].add(card);
    }

    void setReserve(Card[] cards) {
        reserve.putCards(cards);
    }

    Step moveCards(Move move) {
        Column from = columns[move.fromColumn];
        List<Card> cards = from.cut(move.fromRow);
        columns[move.toColumn].addAll(cards);

        boolean flipped = false;
        if (!from.isEmpty() && from.getLastCard().isFacedDown()) {
            from.getLastCard().faceUp();
            flipped = true;
        }
        return Step.move(move, cards.size(), flipped);
    }

    void unmoveCards(Step step) {
        Move move = step.getMove();
        Column from = columns[move.fromColumn];
        if (step.flipped())
            from.getLastCard().faceDown();
        Column to = columns[move.toColumn];
        from.addAll(to.cut(to.getSize() - step.getCount()));
    }

    /** Turns the reserve cards up onto the first three columns. */
    Step dealReserve() {
        Card[] cards = reserve.pickCards();
        for (int i=0; i<cards.length; i++) {
            cards[i].faceUp();
            columns[i].add(cards[i]);
        }
        return Step.reserveDeal();
    }

    void undealReserve() {
        Card[] cards = new Card[RESERVE_SIZE];
        for (int i=0; i<RESERVE_SIZE; i++) {
            cards[i] = columns[i].removeLast();
            cards[i].faceDown();
        }
        reserve.putCards(cards);
    }

    private final Column[] columns;
    private final Reserve reserve;
}
