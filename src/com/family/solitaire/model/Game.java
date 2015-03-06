/*
 * Game.java
 *
 * Created on November 29, 2006, 5:09 PM
 *
 */

package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCARDS;
import static com.family.solitaire.model.CardConstants.NCOLS;
import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

import com.family.solitaire.model.rule.Rule;
import com.family.solitaire.model.rule.RuleFactory;

/**
 *
 * @author Aaron Ding
 */
public class Game {
    
    public Game() {
        deck = new Deck();
        board = new Board();
        history = new MoveHistory();
    }
    
    public void init(String level) {
        rule = RuleFactory.instance().createRule(level);
        deck.shuffle();
        board.clear();
        deal();
        history.clear();
    }
    
    private void deal() {
        for (int j=0; j<NCOLS; j++) {
            for (int i=0; i<7; i++) {
                board.setCard(i, j, deck.getCard(j*7+i));
                if (j<RESERVE_SIZE && i<4)
                    board.getCard(i, j).faceDown();
                else
                    board.getCard(i, j).faceUp();
            }
            for (int i=7; i<NCARDS; i++) {
                board.setCard(i,j, null);
            }
            board.getColumn(j).repaintUI();
        }

        
        Card[] t = new Card[RESERVE_SIZE];
        for (int k=0; k<3; k++) {
            t[k] = deck.getCard(k+49);
            t[k].faceDown();
        }
        board.getReserve().putCards(t);
    }
    
    public void load(String fileName) throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            
            String line = reader.readLine();
            rule = RuleFactory.instance().createRule(line);

            board.clear();

            line = reader.readLine();
            String[] fields = line.split("\t");
            Card[] t = new Card[RESERVE_SIZE];
            for (int i=0; i<RESERVE_SIZE; i++) {
                t[i] = Card.valueOf(Integer.parseInt(fields[i]));
            }
            board.getReserve().putCards(t);
            

            for (int c=0; c<NCOLS; c++) {
                line = reader.readLine();
                fields = line.split("\t");

                for (int row=0; row<fields.length; row++) {
                    board.setCard(row,c,Card.valueOf(Integer.parseInt(fields[row])));
                }
                board.getColumn(c).repaintUI();
            }
            history.clear();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try { reader.close(); } catch(Exception e) { }
        }
    }
    
    public void save(String fileName) throws Exception {

        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            writer.write(rule.getLevel());
            writer.write("\n");
            Card[] r = board.getReserve().getCards();
            for (int i=0; i<RESERVE_SIZE; i++) {
                if (r[i] == null)
                    writer.write("99\t");
                else
                    writer.write(r[i].toValue() + "\t");
            }
            writer.write("\n");
            
            for (int c=0; c<NCOLS; c++) {
                Column col = board.getColumn(c);
                int size = col.getSize();
                for (int row=0; row<size; row++) {
                    writer.write(col.getCard(row).toValue() + "\t");
                }
                writer.write("\n");
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
            throw ioe;
        } finally {
            try { writer.close(); } catch(Exception e) { }
        }
    }
    
    public boolean move(Move move) {
        if (rule.isValidMove(move, board)) {
            if (board.moveCards(move)) {
                history.clear();
            } else {
                history.save(move);
            }
            return true;
        }
        return false;
    }
    
    public Move move(Move[] moves) {
        for(Move move : moves) {
            if (move(move)) {
                return move;
            }
        }
        return null;
    }

    public boolean useReserve() {
        if (isReserveUsed())
            return false;
        
        Card[] reserve = board.getReserve().pickCards();
        for (int i=0; i<reserve.length; i++) {
            int len = board.getColumn(i).getSize();
            reserve[i].faceUp();
            board.setCard(len, i, reserve[i]);
            board.getColumn(i).repaintUI();
        }
        history.clear();
        return true;
    }
    
    public void undo() {
        Move move = history.previous();
        if (move != null) {
            board.moveCards(new Move(move.getTo(), move.getFrom()));
        }
    }
    
    public void redo() {
        Move move = history.next();
        if (move != null) {
            board.moveCards(move);
        }
    }

    // if win return true, otherwise return false
    public boolean checkResult() {
        return rule.checkResult(board);
    }
    
    // return null if you lose the game
    public Move[] getAvailableMove() {
        Move[] ret = rule.getAvailableMoves(board);
        if (ret != null) {
            return ret;
        } else {
            if (!isReserveUsed()) {
                return new Move[] {};
            }
        }
        return null;
    }
    
    public boolean isValidFrom(Position p) {
        return rule.isValidFrom(p, board);
    }
    
    public Card[] getCards(Position p) {
        int size = board.getColumn(p.column).getSize()- p.row;

        Card[] ret = new Card[size];
        for (int i=0; i<size; i++) {
            ret[i] = board.getColumn(p.column).getCard(p.row+i);
        }
        return ret;
    }
    
    private boolean isReserveUsed() { return board.getReserve().used(); }
    
    public Column getColumn(int i) { return board.getColumn(i); }
    public Reserve getReserve() { return board.getReserve(); }

    private Rule rule;
    private Deck deck;
    private Board board;
    
    private MoveHistory history;
}
