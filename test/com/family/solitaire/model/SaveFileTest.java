package com.family.solitaire.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SaveFileTest {

    @Test
    void roundTripKeepsBoardLevelDealTimeAndHistory() throws Exception {
        Game g = new Game();
        g.deal(Rule.MEDIUM, 99);
        GameTest.playRandomly(g, 25, new Random(8));
        g.useReserve();
        GameTest.playRandomly(g, 5, new Random(9));
        g.setElapsedMillis(123456);

        Game copy = SaveFile.fromText(SaveFile.toText(g));
        assertEquals(Rule.MEDIUM, copy.getRule());
        assertEquals(99, copy.getDealNumber());
        assertEquals(123456, copy.getElapsedMillis());
        assertEquals(g.getMoveCount(), copy.getMoveCount());
        assertEquals(RuleGoldenTest.state(g.getBoard()), RuleGoldenTest.state(copy.getBoard()));

        // The restored history can be undone just like the original.
        while (g.forceUndo() != null) { }
        while (copy.forceUndo() != null) { }
        assertEquals(RuleGoldenTest.state(g.getBoard()), RuleGoldenTest.state(copy.getBoard()));
    }

    @Test
    void emptyColumnsSurvive() throws Exception {
        Game g = new Game();
        g.deal(Rule.EASY, 5);
        String text = SaveFile.toText(g);
        // Empty the last column by hand: drop its line's contents.
        String[] lines = text.split("\n", -1);
        lines[8] = "";
        Game copy = SaveFile.fromText(String.join("\n", lines));
        assertTrue(copy.getColumn(6).isEmpty());
        assertEquals(7, copy.getColumn(5).getSize());
    }

    @Test
    void readsFilesFromEarlierVersions() throws Exception {
        String old = "easy\n-1\t-2\t-3\t\n"
                + "-4\t-5\t-6\t7\t\n8\t\n9\t\n10\t\n11\t\n12\t\n\n";
        Game g = SaveFile.fromText(old);
        assertEquals(Rule.EASY, g.getRule());
        assertEquals(0, g.getDealNumber());
        assertEquals(0, g.getMoveCount());
        assertEquals(3, g.getColumn(0).getFaceDownCount());
        assertTrue(g.getColumn(6).isEmpty());
    }

    @Test
    void usedReserveIsSavedAs99() throws Exception {
        Game g = new Game();
        g.deal(Rule.EASY, 6);
        g.useReserve();
        assertTrue(SaveFile.toText(g).split("\n")[1].startsWith("99\t99\t99"));
        assertTrue(SaveFile.fromText(SaveFile.toText(g)).isReserveUsed());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IOException.class, () -> SaveFile.fromText("hello\n"));
    }
}
