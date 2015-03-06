/*
 * MoveHistory.java
 *
 * Created on December 8, 2006, 11:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.model;

import java.util.Vector;

/**
 *
 * @author Aaron
 */
public class MoveHistory {
    
    /** Creates a new instance of MoveHistory */
    public MoveHistory() {
        history = new Vector<Move>(1000);
        current = 0;
    }
    
    public void save(Move move) {
        if (current != history.size()) {
            history.setSize(current);
        }
        history.add(move);
        current++;
    }
    
    public void clear() {
        current = 0;
        history.clear();
    }
    
    public boolean canUndo() {
        return current == 0;
    }
    
    public boolean canRedo() {
        return current == history.size();
    }
    
    public Move next() {
        if (current == history.size())
            return null;
        else {
            Move move = history.get(current);
            current++;
            return move;
        }
    }
    
    public Move previous() {
        if (current==0)
            return null;
        current--;
        return history.get(current);
    }
    
    private Vector<Move> history;
    
    private int current;
}
