/*
 * BaseRule.java
 *
 * Created on December 8, 2006, 4:52 PM
 *
 */

package com.family.solitaire.model.rule;

import static com.family.solitaire.model.CardConstants.CARDS_IN_SUIT;
import static com.family.solitaire.model.CardConstants.NCOLS;

import java.util.ArrayList;

import com.family.solitaire.model.Board;
import com.family.solitaire.model.Card;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Position;
import com.family.solitaire.model.Card.Rank;

/**
 *
 * @author Aaron Ding
 */
public abstract class BaseRule implements Rule {
    
    public BaseRule() {
    }
    
    public boolean isValidFrom(Position position, Board board) {
        return board.getCard(position).isFacedUp();
    }
    
    public boolean checkResult(Board board) {
        for (int j=0; j<NCOLS; j++) {
            if (board.getColumn(j).isEmpty()) {
                continue;
            }
            
            if (board.getColumn(j).getSize() != CARDS_IN_SUIT)
                return false;
            
            if (board.getCard(0,j).rank() != Rank.KING) {
                return false;
            }
            for (int i=1; i<CARDS_IN_SUIT; i++) {
                if (board.getCard(i-1,j).value() - board.getCard(i,j).value() != 1) {
                    return false;
                }
            }
            if (board.getCard(CARDS_IN_SUIT, j) != null)
                return false;
        }
        
        return true;
    }
    
    // return null if you lose the game
    public Move[] getAvailableMoves(Board board) {
        ArrayList<Move> ret = new ArrayList<Move>();
        for (int j=0; j<NCOLS; j++) {
            Card target = board.getColumn(j).getLastCard();
            Card[] aCards = getFollowingCards(target);
            if (aCards == null) {
                continue;
            }
            for (Card card: aCards) {
                Position pos = board.findFaceupCard(card);
                if (pos == null) {
                    continue;
                }
                if (card.rank() == Rank.KING && pos.row == 0) {
                    continue;
                }
                if (pos.column ==j) {
                    continue;
                }

                if (pos.row != 0 && target != null) {
                    Card hCard = board.getCard(new Position(pos.row-1, pos.column));
                    if (hCard.isFacedDown()) {
                        continue;
                    }
                    if (hCard.suit() == card.suit()) {
                        continue;
                    }
                }
                ret.add(new Move(pos, new Position(board.getColumn(j).getSize()-1, j)));
            }
        }

        if (ret.size() == 0)
            return null;
        else
            return ret.toArray(new Move[0]);
    }
    
    abstract protected Card[] getFollowingCards(Card target);
}
