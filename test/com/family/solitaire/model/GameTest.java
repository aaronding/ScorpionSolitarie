package com.family.solitaire.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void dealsThreeFaceDownCardsInEachOfTheFirstFourColumns() {
        Game g = new Game();
        g.deal(Rule.EASY, 42);
        int[] expected = {3, 3, 3, 3, 0, 0, 0};
        for (int c = 0; c < 7; c++) {
            assertEquals(7, g.getColumn(c).getSize());
            assertEquals(expected[c], g.getColumn(c).getFaceDownCount());
        }
        assertFalse(g.isReserveUsed());
    }

    @Test
    void sameDealNumberGivesSameCards() {
        Game a = new Game();
        Game b = new Game();
        a.deal(Rule.MEDIUM, 1234);
        b.deal(Rule.DIFFICULT, 1234);
        assertEquals(RuleGoldenTest.state(a.getBoard()), RuleGoldenTest.state(b.getBoard()));
        b.deal(Rule.DIFFICULT, 1235);
        assertFalse(RuleGoldenTest.state(a.getBoard()).equals(RuleGoldenTest.state(b.getBoard())));
    }

    @Test
    void easyUndoesEverythingBackToTheDeal() {
        Game g = new Game();
        g.deal(Rule.EASY, 7);
        String start = RuleGoldenTest.state(g.getBoard());
        playRandomly(g, 60, new Random(1));
        assertTrue(g.getMoveCount() > 0);
        while (g.canUndo())
            assertNotNull(g.undo());
        assertEquals(0, g.getMoveCount());
        assertEquals(start, RuleGoldenTest.state(g.getBoard()));
    }

    @Test
    void redoReplaysUndoneSteps() {
        Game g = new Game();
        g.deal(Rule.EASY, 8);
        playRandomly(g, 40, new Random(2));
        String end = RuleGoldenTest.state(g.getBoard());
        int moves = g.getMoveCount();
        for (int i = 0; i < 10; i++)
            g.undo();
        while (g.canRedo())
            g.redo();
        assertEquals(moves, g.getMoveCount());
        assertEquals(end, RuleGoldenTest.state(g.getBoard()));
    }

    @Test
    void undoTurnsTheUncoveredCardBackDown() {
        Game g = new Game();
        g.deal(Rule.EASY, 3);
        Random r = new Random(5);
        while (g.getHistory().isEmpty() || !lastStepFlipped(g))
            if (!playOne(g, r))
                return;   // no flip in this game; nothing to check
        Step last = g.getHistory().get(g.getHistory().size() - 1);
        Column from = g.getColumn(last.getMove().fromColumn);
        int downBefore = from.getFaceDownCount();
        g.undo();
        assertEquals(downBefore + 1, from.getFaceDownCount());
    }

    @Test
    void mediumUndoStopsAtTheReserveDeal() {
        Game g = new Game();
        g.deal(Rule.MEDIUM, 11);
        playRandomly(g, 5, new Random(3));
        assertTrue(g.useReserve());
        assertFalse(g.canUndo());
        playRandomly(g, 5, new Random(4));
        while (g.canUndo())
            g.undo();
        Step last = g.getHistory().get(g.getHistory().size() - 1);
        assertTrue(last.isReserveDeal());
        assertTrue(g.isReserveUsed());
    }

    @Test
    void difficultNeverUndoes() {
        Game g = new Game();
        g.deal(Rule.DIFFICULT, 12);
        playRandomly(g, 10, new Random(6));
        assertFalse(g.canUndo());
        assertNull(g.undo());
    }

    @Test
    void forceUndoIgnoresTheRule() {
        Game g = new Game();
        g.deal(Rule.DIFFICULT, 13);
        String start = RuleGoldenTest.state(g.getBoard());
        playRandomly(g, 30, new Random(7));
        g.useReserve();
        while (g.forceUndo() != null) { }
        assertEquals(start, RuleGoldenTest.state(g.getBoard()));
    }

    @Test
    void invalidMovesAreRejected() {
        Game g = new Game();
        g.deal(Rule.DIFFICULT, 14);
        assertFalse(g.move(new Move(0, 0, 1)));   // face down
        assertFalse(g.move(new Move(4, 6, 4)));   // same column
        assertEquals(0, g.getMoveCount());
    }

    static void playRandomly(Game g, int steps, Random r) {
        for (int i = 0; i < steps; i++)
            if (!playOne(g, r))
                return;
    }

    static boolean playOne(Game g, Random r) {
        java.util.List<Move> moves = new java.util.ArrayList<>();
        for (int c = 0; c < 7; c++)
            for (int row = 0; row < g.getColumn(c).getSize(); row++)
                for (int t = 0; t < 7; t++) {
                    Move m = new Move(c, row, t);
                    if (g.getRule().isValidMove(m, g.getBoard()))
                        moves.add(m);
                }
        if (moves.isEmpty())
            return false;
        return g.move(moves.get(r.nextInt(moves.size())));
    }

    private static boolean lastStepFlipped(Game g) {
        Step s = g.getHistory().get(g.getHistory().size() - 1);
        return !s.isReserveDeal() && s.flipped();
    }
}
