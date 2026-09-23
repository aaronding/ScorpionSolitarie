/*
 * Column.java
 *
 * Created on December 3, 2006, 7:29 PM
 */

package com.family.solitaire.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the seven piles on the board, top card first.
 *
 * @author Aaron
 */
public class Column {

    Column(int index) {
        this.index = index;
    }

    public int getIndex() { return index; }
    public int getSize() { return cards.size(); }
    public boolean isEmpty() { return cards.isEmpty(); }
    public Card getCard(int row) { return cards.get(row); }

    public Card getLastCard() {
        return isEmpty() ? null : cards.get(cards.size()-1);
    }

    /** Number of face-down cards at the top of the column. */
    public int getFaceDownCount() {
        int count = 0;
        while (count < cards.size() && cards.get(count).isFacedDown())
            count++;
        return count;
    }

    public int findCard(Card card) {
        return cards.indexOf(card);
    }

    void add(Card card) {
        cards.add(card);
    }

    Card removeLast() {
        return cards.remove(cards.size()-1);
    }

    /** Removes and returns the cards from {@code row} to the end. */
    List<Card> cut(int row) {
        List<Card> tail = cards.subList(row, cards.size());
        List<Card> ret = new ArrayList<Card>(tail);
        tail.clear();
        return ret;
    }

    void addAll(List<Card> more) {
        cards.addAll(more);
    }

    void clear() {
        cards.clear();
    }

    private final int index;
    private final List<Card> cards = new ArrayList<Card>();
}
