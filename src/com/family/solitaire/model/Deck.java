/*
 * Deck.java
 *
 * Created on November 29, 2006, 4:00 PM
 *
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCARDS;

import com.family.solitaire.model.Card.Rank;
import com.family.solitaire.model.Card.Suit;

/**
 *
 * @author Aaron Ding
 */
public class Deck {
    
    Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                Card card = new Card(rank, suit);
                deck[card.value()] = card;
            }
        }
    }
    
    public Card getCard(int index) {
        return deck[index];
    }
    
    private final Card[] deck = new Card[52];

    public Card[] deck() {
        return deck;
    }
    
    public void shuffle() {
        for (int i=0; i<NCARDS; i++) {
            int pos = (int)(NCARDS * Math.random());
            Card tmp = deck[i];
            deck[i] = deck[pos];
            deck[pos] = tmp;
        }
    }
}
