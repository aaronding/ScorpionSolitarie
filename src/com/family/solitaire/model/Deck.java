/*
 * Deck.java
 *
 * Created on November 29, 2006, 4:00 PM
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCARDS;

import java.util.Random;

import com.family.solitaire.model.Card.Rank;
import com.family.solitaire.model.Card.Suit;

/**
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

    /** Fisher-Yates shuffle; the same random source always gives the same order. */
    public void shuffle(Random random) {
        for (int i=NCARDS-1; i>0; i--) {
            int pos = random.nextInt(i+1);
            Card tmp = deck[i];
            deck[i] = deck[pos];
            deck[pos] = tmp;
        }
    }

    private final Card[] deck = new Card[NCARDS];
}
