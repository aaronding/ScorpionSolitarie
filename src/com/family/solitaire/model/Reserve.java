/*
 * Reserve.java
 *
 * Created on December 9, 2006, 10:45 PM
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

/**
 * The three face-down cards set aside at the deal, played once onto the first
 * three columns.
 *
 * @author Aaron
 */
public class Reserve {

    public Card[] getCards() {
        return reserve.clone();
    }

    public boolean used() {
        return reserve[0] == null;
    }

    void putCards(Card[] cards) {
        System.arraycopy(cards, 0, reserve, 0, RESERVE_SIZE);
    }

    Card[] pickCards() {
        Card[] ret = getCards();
        clear();
        return ret;
    }

    void clear() {
        for (int i=0; i<reserve.length; i++)
            reserve[i] = null;
    }

    private final Card[] reserve = new Card[RESERVE_SIZE];
}
