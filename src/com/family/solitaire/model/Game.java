/*
 * Game.java
 *
 * Created on November 29, 2006, 5:09 PM
 */

package com.family.solitaire.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A game in progress: the board, the rule it is played under, and its history.
 * Every change goes through here so it can be checked and undone.
 *
 * @author Aaron Ding
 */
public class Game {

    public Game() {
        board = new Board();
        rule = Rule.EASY;
    }

    /** Deals game number {@code dealNumber}; the same number always deals the same cards. */
    public void deal(Rule rule, int dealNumber) {
        Deck deck = new Deck();
        deck.shuffle(new Random(dealNumber));
        board.deal(deck);
        start(rule, dealNumber);
    }

    void start(Rule rule, int dealNumber) {
        this.rule = rule;
        this.dealNumber = dealNumber;
        done.clear();
        undone.clear();
        elapsedMillis = 0;
    }

    public boolean move(Move move) {
        if (!rule.isValidMove(move, board))
            return false;
        done.add(board.moveCards(move));
        undone.clear();
        return true;
    }

    public boolean useReserve() {
        if (isReserveUsed())
            return false;
        done.add(board.dealReserve());
        undone.clear();
        return true;
    }

    public boolean canUndo() {
        return !done.isEmpty() && rule.mayUndo(done.get(done.size()-1));
    }

    /** Takes back the last step if the rule allows it; returns it, or null. */
    public Step undo() {
        return canUndo() ? undoLast() : null;
    }

    /** Takes back the last step whatever the rule says, for finishing a game the player gave up. */
    public Step forceUndo() {
        return done.isEmpty() ? null : undoLast();
    }

    private Step undoLast() {
        Step step = done.remove(done.size()-1);
        if (step.isReserveDeal())
            board.undealReserve();
        else
            board.unmoveCards(step);
        undone.add(step);
        return step;
    }

    public boolean canRedo() {
        return !undone.isEmpty();
    }

    /** Replays the last undone step; returns it, or null. */
    public Step redo() {
        if (undone.isEmpty())
            return null;
        Step step = undone.remove(undone.size()-1);
        Step again = step.isReserveDeal() ? board.dealReserve() : board.moveCards(step.getMove());
        done.add(again);
        return again;
    }

    // if win return true, otherwise return false
    public boolean checkResult() {
        return rule.checkResult(board);
    }

    public List<Move> getAvailableMoves() {
        return rule.getAvailableMoves(board);
    }

    /** No hint left and the reserve is gone: the original "You Lose!" test. */
    public boolean noMovesLeft() {
        return isReserveUsed() && getAvailableMoves().isEmpty();
    }

    public boolean isReserveUsed() { return board.getReserve().used(); }

    public Board getBoard() { return board; }
    public Column getColumn(int i) { return board.getColumn(i); }
    public Reserve getReserve() { return board.getReserve(); }
    public Rule getRule() { return rule; }
    public String getLevel() { return rule.getLevel(); }

    /** The deal number, or 0 if unknown (a game saved by an older version). */
    public int getDealNumber() { return dealNumber; }

    public List<Step> getHistory() { return Collections.unmodifiableList(done); }
    public int getMoveCount() { return done.size(); }

    public long getElapsedMillis() { return elapsedMillis; }
    public void setElapsedMillis(long millis) { elapsedMillis = millis; }

    void restoreHistory(List<Step> steps) {
        done.clear();
        done.addAll(steps);
        undone.clear();
    }

    private final Board board;
    private Rule rule;
    private int dealNumber;
    private long elapsedMillis;

    private final List<Step> done = new ArrayList<Step>();
    private final List<Step> undone = new ArrayList<Step>();
}
