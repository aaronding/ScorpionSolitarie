package com.family.solitaire.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

/**
 * Plays 30 random games and compares every board, legal move, hint and win
 * check against rules-golden.txt.gz, recorded from the original 2006 rules.
 */
class RuleGoldenTest {

    @Test
    void matchesRecordedGames() throws Exception {
        List<String> expected = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                getClass().getResourceAsStream("rules-golden.txt.gz")), "UTF-8"))) {
            String line;
            while ((line = in.readLine()) != null)
                expected.add(line);
        }
        List<String> actual = play(30);
        for (int i = 0; i < expected.size(); i++)
            assertEquals(expected.get(i), actual.get(i), "line " + (i + 1));
        assertEquals(expected.size(), actual.size());
    }

    static List<String> play(int games) {
        List<String> out = new ArrayList<>();
        String[] levels = {"easy", "medium", "difficult"};
        for (int seed = 1; seed <= games; seed++) {
            String level = levels[seed % 3];
            Random r = new Random(seed);
            for (int i = 51; i > 0; i--)
                r.nextInt(i + 1);   // the recording drew its shuffle from this generator
            Game g = new Game();
            g.deal(Rule.forLevel(level), seed);
            Board b = g.getBoard();
            out.add("GAME " + seed + " " + level);
            for (int step = 0; step < 400; step++) {
                out.add("S " + state(b));
                List<String> valid = new ArrayList<>();
                List<Move> moves = new ArrayList<>();
                for (int c = 0; c < 7; c++)
                    for (int row = 0; row < b.getColumn(c).getSize(); row++)
                        for (int t = 0; t < 7; t++) {
                            Move m = new Move(c, row, t);
                            if (g.getRule().isValidMove(m, b)) {
                                valid.add(c + "." + row + ">" + t);
                                moves.add(m);
                            }
                        }
                out.add("V " + String.join(" ", valid));
                List<String> hints = new ArrayList<>();
                for (Move m : g.getAvailableMoves())
                    hints.add(m.fromColumn + "." + m.fromRow + ">" + m.toColumn);
                Collections.sort(hints);
                out.add("H " + (hints.isEmpty() ? "null" : String.join(" ", hints)));
                out.add("W " + g.checkResult());
                boolean reserveLeft = !g.isReserveUsed();
                if (moves.isEmpty() && !reserveLeft)
                    break;
                if (reserveLeft && (moves.isEmpty() || r.nextInt(25) == 0)) {
                    g.useReserve();
                    out.add("R");
                    continue;
                }
                Move m = moves.get(r.nextInt(moves.size()));
                g.move(m);
                out.add("M " + m.fromColumn + "." + m.fromRow + ">" + m.toColumn);
            }
        }
        return out;
    }

    static String state(Board b) {
        StringBuilder s = new StringBuilder();
        for (Card c : b.getReserve().getCards())
            s.append(c == null ? "_" : String.valueOf(c.toValue())).append(',');
        for (int c = 0; c < 7; c++) {
            s.append('|');
            Column col = b.getColumn(c);
            for (int i = 0; i < col.getSize(); i++)
                s.append(col.getCard(i).toValue()).append(',');
        }
        return s.toString();
    }
}
