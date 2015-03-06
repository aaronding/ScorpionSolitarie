/*
 * Reserve.java
 *
 * Created on December 9, 2006, 10:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

/**
 *
 * @author Aaron
 */
public class Reserve {
    
    /** Creates a new instance of Reserve */
    public Reserve() {
        reserve = new Card[RESERVE_SIZE];
    }
    
    public void clear() {
        for (int i=0; i<reserve.length; i++) {
            reserve[i] = null;
        }
    }
    
    public Card[] getCards() {
        Card[] ret = new Card[reserve.length];
        System.arraycopy(reserve, 0, ret, 0, reserve.length);
        
        return ret;
    }
    
    public void putCards(Card[] cards) {
        System.arraycopy(cards, 0,  reserve, 0, RESERVE_SIZE);
        
        if (ui != null) {
            ui.repaintUI();
        }
    }
    
    public Card[] pickCards() {
        Card[] ret = getCards();
        
        for (int i=0; i<reserve.length; i++) {
            reserve[i] = null;
        }
        
        if (ui != null) {
            ui.repaintUI();
        }
        return ret;
    }
    
    public boolean used() {
        return reserve[0] == null;
    }
    
    public void register(ColumnUI ui) {
        this.ui = ui;
    }
    
    private Card[] reserve;
    private ColumnUI ui;
}
