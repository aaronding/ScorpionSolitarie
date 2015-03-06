/*
 * MediumRule.java
 *
 * Created on December 8, 2006, 4:18 PM
 *
 */

package com.family.solitaire.model.rule;

import static com.family.solitaire.model.CardConstants.NCOLS;

import com.family.solitaire.model.Board;
import com.family.solitaire.model.Card;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Card.Rank;
import com.family.solitaire.model.Card.Suit;

/**
 *
 * @author Aaron Ding
 */
public class MediumRule extends BaseRule {
    
    MediumRule() {
    }
 
    public boolean isValidMove(Move move, Board board) {
        int toCol = move.getTo().column;
        if (toCol<0 || toCol>=NCOLS) return false;

        int fromCol = move.getFrom().column;
        if (fromCol == toCol)
            return false;
        
        int fromRow = move.getFrom().row;
        Card fromCard = board.getColumn(fromCol).getCard(fromRow+1);
        if (board.getColumn(toCol).isEmpty()) {
            return fromCard.rank() == Rank.KING;
        }
        
        Card toCard = board.getColumn(move.getTo().column).getCard(move.getTo().row);
        return toCard.rank() != Rank.ACE && toCard.color() == fromCard.color() 
            && toCard.rank().ordinal() - fromCard.rank().ordinal() == 1;
    }
    
    protected Card[] getFollowingCards(Card target) {
        if (target == null) {
            return new Card[] { 
                new Card(Rank.KING, Suit.SPADES),
                new Card(Rank.KING, Suit.HEARTS), 
                new Card(Rank.KING, Suit.CLUBS), 
                new Card(Rank.KING, Suit.DIAMONDS)};
        }
        
        if (target.rank() == Rank.ACE) {
            return null;
        }
        
        return new Card[] { 
            new Card(Rank.values()[target.rank().ordinal()-1], target.suit()), // same suit
            new Card(Rank.values()[target.rank().ordinal()-1], 
                     Suit.values()[(target.suit().ordinal()+2)%4])};  // same color
    }
    
    public String getLevel() {
        return "difficult";
    }
}
