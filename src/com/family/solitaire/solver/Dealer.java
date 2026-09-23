package com.family.solitaire.solver;

import java.util.List;
import java.util.Random;

import com.family.solitaire.model.Game;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Rule;
import com.family.solitaire.model.SaveFile;

/**
 * Picks deals that can be won, and works out how to finish a game the player
 * gives up on.
 *
 * @author Aaron Ding
 */
public final class Dealer {

    /** Deal numbers run from 1 to this. */
    public static final int MAX_DEAL = 999999;

    /** Positions to search before calling a deal too hard to check. */
    static final int LIMIT = 300000;

    private Dealer() { }

    /** A random deal number that the solver can win at this level. */
    public static int winnableDeal(Rule rule, Random random) {
        Solver solver = new Solver(rule, LIMIT);
        int number = 0;
        for (int attempt = 0; attempt < 1000; attempt++) {
            number = 1 + random.nextInt(MAX_DEAL);
            Game game = new Game();
            game.deal(rule, number);
            if (solver.solve(game.getBoard()).outcome == Solver.Outcome.SOLVED)
                return number;
        }
        return number;
    }

    public static Solver.Outcome check(Rule rule, int dealNumber) {
        Game game = new Game();
        game.deal(rule, dealNumber);
        return new Solver(rule, LIMIT).solve(game.getBoard()).outcome;
    }

    /** How to finish a game: take back {@code undo} steps, then play {@code moves}. */
    public static final class Plan {
        Plan(int undo, List<Move> moves) {
            this.undo = undo;
            this.moves = moves;
        }
        public final int undo;
        /** Moves to win; {@link Solver#DEAL_RESERVE} deals the reserve. */
        public final List<Move> moves;
    }

    /**
     * Finds the latest point in the game's history from which it can still be
     * won, trying the current position, then 1, 2, 4, 8... steps back, and
     * finally the start. Returns null if even the start cannot be won.
     */
    public static Plan finish(Game game) {
        String saved = SaveFile.toText(game);
        int steps = game.getMoveCount();
        Solver solver = new Solver(game.getRule(), LIMIT);
        for (int back = 0; ; back = back == 0 ? 1 : Math.min(back * 2, steps)) {
            Game copy;
            try {
                copy = SaveFile.fromText(saved);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            for (int i = 0; i < back; i++)
                copy.forceUndo();
            Solver.Result result = solver.solve(copy.getBoard());
            if (result.outcome == Solver.Outcome.SOLVED)
                return new Plan(back, result.solution);
            if (back == steps)
                return null;
        }
    }
}
