/*
 * Move.java
 *
 * Created on November 29, 2006, 6:24 PM
 *
 */

package com.family.solitaire.model;

/**
 *
 * @author Aaron Ding
 */
public class Move {
    
    public Move() { }
    
    public String toString() {
        return from.column + "." + from.row + " -> " + to.column + "." + to.row;
    }
    
    public Move(Position from, Position to) {
        this.from = from;
        this.to = to;
    }
    
    public Position getFrom() { return from; }
    
    public Position getTo() { return to; }
    
    public boolean isUseReserve() { return from==null; }
    
    private Position from;
    private Position to;
}
