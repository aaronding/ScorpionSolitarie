/*
 * Board.java
 *
 * Created on December 3, 2006, 7:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCOLS;

/**
 *
 * @author Aaron
 */
public class Board {
    
    /** Creates a new instance of Board */
    Board() {
        columns = new Column[NCOLS];
        for(int i=0; i<NCOLS; i++) {
            columns[i] = new Column(i);
        }
        reserve = new Reserve();
    }
    
    public void clear() {
        for(int i=0; i<NCOLS; i++) {
            columns[i].clear();
        }
        reserve.clear();
    }
    
    public Reserve getReserve() {
        return reserve;
    }
    
    public Column getColumn(int index) {
        return columns[index];
    }
    
    public boolean moveCards(Move move) {
        Column from = columns[move.getFrom().column];
        Column to = columns[move.getTo().column];
        int iFrom = move.getFrom().row+1;
        int iTo = move.getTo().row+1;
        
        int size = from.getSize();
        for (; iFrom<size; iFrom++,iTo++) {
            to.setCard(iTo, from.getCard(iFrom));
            from.setCard(iFrom, null);
        }
        
        boolean changed = false;
        if (!from.isEmpty() && from.getLastCard().isFacedDown()) {
            from.getLastCard().faceUp();
            changed = true;
        }
        from.repaintUI();
        to.repaintUI();
        return changed;
    }
    
    public Position findFaceupCard(Card card) {
        for (int i=0; i<NCOLS; i++) {
            int row = getColumn(i).findCard(card);
            if (row != -1 && getColumn(i).getCard(row).isFacedUp()) {
                return new Position(row, i);
            }
        }
        return null;
    }
    
    public Card getCard(Position p) {
        return columns[p.column].getCard(p.row);
    }
    
    public Card getCard(int row, int column) {
        return columns[column].getCard(row);
    }
    
    void setCard(int row, int column, Card card) {
        columns[column].setCard(row, card);
    }
    
    private Column[] columns; 
    private Reserve reserve;
}
