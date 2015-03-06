/*
 * Card.java
 *
 * Created on November 29, 2006, 3:43 PM
 *
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.CARDS_IN_SUIT;
import static com.family.solitaire.model.CardConstants.NCARDS;

/**
 *
 * @author Aaron Ding
 */
public class Card {
    public enum Suit {  SPADES, HEARTS, CLUBS, DIAMONDS; }
    
    public enum Rank {  ACE, DEUCE, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING; }
    
    public enum Color { BLACK, RED }
    
    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.color = Color.values()[suit.ordinal() % 2];
        this.value = suit.ordinal() * CARDS_IN_SUIT + rank.ordinal();
    }
    
    private Card(int value) {
        this.value = value;
        rank = Rank.values()[(value) % CARDS_IN_SUIT];
        suit = Suit.values()[(value) / CARDS_IN_SUIT];
        color = Color.values()[suit.ordinal() % 2];
    }
    
    public static Card valueOf(int value) {
        if (value>=NCARDS || value <-NCARDS)
            return null;
        
        Card card;
        if (value < 0) {
            value = 0-(value + 1);
            card = new Card(value);
            card.faceDown();
        } else {
            card = new Card(value);
            card.faceUp();
        }
        return card;
    }
    
    public int toValue() {
        return isFacedUp() ? value : 0-value-1;
    }

    public Rank rank() { return rank; }
    public Suit suit() { return suit; }
    public Color color() { return color; }
    public int value() { return value; }
    public void faceUp()   { faceUp = true;  }
    public void faceDown() { faceUp = false;  }
    public boolean isFacedUp() { return faceUp; }
    public boolean isFacedDown() { return !faceUp; }
    
    @Override
    public String toString() { return (rank.ordinal()+1) + " of " + suit; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        
        if (o instanceof Card) {
            return ((Card)o).value == this.value;
        }
        return false;
    }
    
    private final Rank rank;
    private final Suit suit;
    private final Color color;
    private final int value; // [0,1,2,3...51]
    private boolean faceUp = true;
}

