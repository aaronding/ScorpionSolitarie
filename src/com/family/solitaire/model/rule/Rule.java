/*
 * Rule.java
 *
 * Created on December 6, 2006, 4:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.model.rule;

import com.family.solitaire.model.Board;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Position;

/**
 *
 * @author ading
 */
public interface Rule {
    boolean isValidMove(Move move, Board board);
    boolean isValidFrom(Position position, Board board);
    boolean checkResult(Board board);
    Move[] getAvailableMoves(Board board);
    
    String getLevel();
}
