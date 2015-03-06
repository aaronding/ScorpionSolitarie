/*
 * Column.java
 *
 * Created on December 3, 2006, 7:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCARDS;

/**
 *
 * @author Aaron
 */
public class Column {
    
    /** Creates a new instance of Column */
    Column(int index) {
        size = 0;
        cards = new Card[NCARDS];
        this.index = index;
    }
    
    public void registerUI(ColumnUI ui) {
        this.ui = ui;
    }
    
    public void clear() {
        for (int i=0; i<cards.length; i++) {
            setCard(i, null);
        }
    }
    
    public int getSize() {
        return size;
    }
    
    public int getFacedOffCardsCount() {
        int count = -1;
        while (++count != NCARDS && getCard(count)!=null && 
                getCard(count).isFacedDown()) {}
        
        return count;
    }
    
    public Card getLastCard() {
        if (isEmpty())
            return null;
        
        return getCard(getSize()-1);
    }

    public Card getCard(int row) {
        return cards[row];
    }
    
    public int findCard(Card card) {
        for (int i=0; i<size; i++) {
            if (card.equals(cards[i])) {
                return i;
            }
        }
        
        return -1;
    }
    
    void setCard(int row, Card card) {
        if (cards[row] != null && card != null)
            throw new java.lang.IllegalArgumentException(
                "This position already has a card.");
        
        if (cards[row] == null && card == null)
            return;
        
        if (cards[row] != null && card == null) {
            size--;
        } else if (cards[row] == null && card != null) {
            size++;
        }
        cards[row] = card;
    }
    
    public void repaintUI() {
        if (ui != null) {
            ui.repaintUI();
        }
    }
    
    public boolean isEmpty() {
        return size==0;
    }
    
    public int getIndex() {
        return index;
    }
    
    private int index;
    private int size;
    private Card[] cards;
    
    private ColumnUI ui;
}
