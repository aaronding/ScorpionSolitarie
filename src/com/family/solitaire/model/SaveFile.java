package com.family.solitaire.model;

import static com.family.solitaire.model.CardConstants.NCOLS;
import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes games as text.
 * <pre>
 * level
 * reserve cards, or 99 once dealt
 * one line per column, top card first
 * deal    number          (optional, added in 1.4)
 * time    milliseconds    (optional)
 * steps   history         (optional)
 * </pre>
 * Card values are 0-51, negative (-value-1) when face down; fields are
 * tab-separated. Files from older versions have only the first nine lines.
 *
 * @author Aaron Ding
 */
public final class SaveFile {

    private SaveFile() { }

    public static Game load(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return read(reader);
        }
    }

    /** Writes to a temporary file first so a crash never leaves half a save. */
    public static void save(Game game, File file) throws IOException {
        File tmp = new File(file.getPath() + ".tmp");
        try (Writer writer = new FileWriter(tmp)) {
            write(game, writer);
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String toText(Game game) {
        StringWriter writer = new StringWriter();
        try {
            write(game, writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    public static Game fromText(String text) throws IOException {
        return read(new StringReader(text));
    }

    public static void write(Game game, Writer writer) throws IOException {
        Board board = game.getBoard();
        writer.write(game.getLevel());
        writer.write("\n");
        for (Card card : board.getReserve().getCards())
            writer.write((card == null ? "99" : String.valueOf(card.toValue())) + "\t");
        writer.write("\n");
        for (int c=0; c<NCOLS; c++) {
            Column col = board.getColumn(c);
            for (int row=0; row<col.getSize(); row++)
                writer.write(col.getCard(row).toValue() + "\t");
            writer.write("\n");
        }
        writer.write("deal\t" + game.getDealNumber() + "\n");
        writer.write("time\t" + game.getElapsedMillis() + "\n");
        writer.write("steps");
        for (Step step : game.getHistory()) {
            writer.write("\t");
            if (step.isReserveDeal()) {
                writer.write("r");
            } else {
                Move m = step.getMove();
                writer.write("m," + m.fromColumn + "," + m.fromRow + "," + m.toColumn
                        + "," + step.getCount() + "," + (step.flipped() ? 1 : 0));
            }
        }
        writer.write("\n");
    }

    public static Game read(Reader in) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        Game game = new Game();
        Board board = game.getBoard();
        board.clear();
        try {
            Rule rule = Rule.forLevel(reader.readLine());

            String[] fields = reader.readLine().split("\t");
            Card[] reserve = new Card[RESERVE_SIZE];
            for (int i=0; i<RESERVE_SIZE; i++)
                reserve[i] = Card.valueOf(Integer.parseInt(fields[i]));
            board.setReserve(reserve);

            for (int c=0; c<NCOLS; c++) {
                for (String field : reader.readLine().split("\t")) {
                    if (field.length() > 0)
                        board.addCard(c, Card.valueOf(Integer.parseInt(field)));
                }
            }

            int deal = 0;
            long time = 0;
            List<Step> steps = new ArrayList<Step>();
            String line;
            while ((line = reader.readLine()) != null) {
                fields = line.split("\t");
                if (fields[0].equals("deal")) {
                    deal = Integer.parseInt(fields[1]);
                } else if (fields[0].equals("time")) {
                    time = Long.parseLong(fields[1]);
                } else if (fields[0].equals("steps")) {
                    for (int i=1; i<fields.length; i++)
                        steps.add(readStep(fields[i]));
                }
            }
            game.start(rule, deal);
            game.setElapsedMillis(time);
            game.restoreHistory(steps);
            return game;
        } catch (RuntimeException e) {
            throw new IOException("Not a saved game", e);
        }
    }

    private static Step readStep(String field) {
        if (field.equals("r"))
            return Step.reserveDeal();
        String[] p = field.split(",");
        Move move = new Move(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        return Step.move(move, Integer.parseInt(p[4]), p[5].equals("1"));
    }
}
