package com.family.solitaire.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.family.solitaire.model.Game;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Rule;
import com.family.solitaire.model.SaveFile;

class SolverTest {

    @Test
    void solutionsReallyWin() {
        for (Rule rule : Rule.values()) {
            Solver solver = new Solver(rule, 200000);
            int solved = 0;
            for (int deal = 1; deal <= 15; deal++) {
                Game game = new Game();
                game.deal(rule, deal);
                Solver.Result result = solver.solve(game.getBoard());
                if (result.outcome != Solver.Outcome.SOLVED)
                    continue;
                solved++;
                play(game, result.solution);
                assertTrue(game.checkResult(), rule + " deal " + deal);
            }
            assertTrue(solved > 0, "no " + rule + " deal solved");
        }
    }

    @Test
    void dealerOnlyDealsWinnableGames() {
        Random random = new Random(3);
        for (Rule rule : Rule.values()) {
            int number = Dealer.winnableDeal(rule, random);
            assertEquals(Solver.Outcome.SOLVED, Dealer.check(rule, number));
        }
    }

    @Test
    void finishingAGameWinsItFromWhereverThePlayerLeftOff() throws Exception {
        Random random = new Random(11);
        for (Rule rule : Rule.values()) {
            int number = Dealer.winnableDeal(rule, random);
            Game game = new Game();
            game.deal(rule, number);
            playRandomly(game, 40, random);

            Dealer.Plan plan = Dealer.finish(game);
            assertNotNull(plan, rule + " game " + number);
            for (int i = 0; i < plan.undo; i++)
                assertNotNull(game.forceUndo());
            play(game, plan.moves);
            assertTrue(game.checkResult(), rule + " game " + number);
        }
    }

    @Test
    void provesADeadEnd() throws Exception {
        // Reserve gone; the Ace of hearts is stuck between the 4 and the 3.
        String text = "difficult\n99\t99\t99\t\n"
            + "12\t11\t10\t9\t8\t7\t6\t5\t4\t3\t2\t1\t0\t\n"
            + "25\t24\t23\t22\t21\t20\t19\t18\t17\t16\t13\t15\t14\t\n"
            + "38\t37\t36\t35\t34\t33\t32\t31\t30\t29\t28\t27\t26\t\n"
            + "51\t50\t49\t48\t47\t46\t45\t44\t43\t42\t41\t40\t39\t\n\n\n\n";
        Game game = SaveFile.fromText(text);
        Solver.Result result = new Solver(Rule.DIFFICULT, 1000).solve(game.getBoard());
        assertEquals(Solver.Outcome.UNSOLVABLE, result.outcome);
    }

    private static void play(Game game, List<Move> moves) {
        for (Move m : moves) {
            boolean ok = m == Solver.DEAL_RESERVE ? game.useReserve() : game.move(m);
            assertTrue(ok, "illegal move " + m);
        }
    }

    private static void playRandomly(Game game, int steps, Random random) {
        for (int i = 0; i < steps; i++) {
            List<Move> moves = new ArrayList<>();
            for (int c = 0; c < 7; c++)
                for (int row = 0; row < game.getColumn(c).getSize(); row++)
                    for (int t = 0; t < 7; t++) {
                        Move m = new Move(c, row, t);
                        if (game.getRule().isValidMove(m, game.getBoard()))
                            moves.add(m);
                    }
            if (moves.isEmpty())
                return;
            game.move(moves.get(random.nextInt(moves.size())));
        }
    }
}
