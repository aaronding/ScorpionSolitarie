/*
 * DifficultRule.java
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
public class DifficultRule extends BaseRule {
    
    DifficultRule() {
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
        return toCard.suit() == fromCard.suit() 
            && toCard.value() - fromCard.value() == 1;
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
        
        return new Card[] { Card.valueOf(target.value() - 1) };
    }
    
    public String getLevel() {
        return "difficult";
    }
}
