/*
 * MainFrame.java
 *
 * Created on November 29, 2006, 4:08 PM
 */

package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.family.about.AboutDialog;
import com.family.help.HelpDialog;
import com.family.solitaire.model.Game;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Rule;
import com.family.solitaire.model.SaveFile;
import com.family.solitaire.solver.Dealer;
import com.family.solitaire.solver.Solver;

/**
 * The window: menus, the table, a message bar above it and a status bar below.
 * Runs the game's life: dealing, the clock, saving, statistics, and the
 * computer finishing a game the player gives up.
 *
 * @author Aaron Ding
 */
public class MainFrame extends JFrame {

    public static final String APP_NAME = "Scorpion Solitaire";

    // Stamped into the jar manifest by build.sh from the git tag
    public static final String APP_VERSION =
        MainFrame.class.getPackage().getImplementationVersion() != null
            ? MainFrame.class.getPackage().getImplementationVersion() : "dev";

    private final Image icon;
    private CardArt art;
    private BoardPanel board;
    private final NoticeBar notice = new NoticeBar();
    private final StatusBar status = new StatusBar();
    private final Clock clock = new Clock();
    private final Random random = new Random();

    private Game game = new Game();
    private boolean over;
    private boolean busy;
    private int version;
    private int hintIndex;
    private Solver deadEndCheck;

    public MainFrame(Image icon) {
        super(APP_NAME);
        this.icon = icon;
        setIconImage(icon);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        art = new CardArt(() -> board.repaint());
        art.load();
        art.setBack(Settings.getCardBack(), Settings.getCustomBackImage());

        board = new BoardPanel(game, art, this::playerMoved);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(notice, BorderLayout.NORTH);
        getContentPane().add(board, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        setJMenuBar(createMenus());
        createToolbar();

        Rectangle screen = getGraphicsConfiguration().getBounds();
        setSize(new Dimension(Math.min(1200, screen.width * 4 / 5), Math.min(860, screen.height * 4 / 5)));
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quit(); }
            @Override public void windowActivated(WindowEvent e) { clock.resume(); }
            @Override public void windowDeactivated(WindowEvent e) { clock.pause(); }
        });
        integrateWithDesktop();
        new Timer(1000, e -> updateStatus()).start();
    }

    /** Resumes the last game, or deals a new one. */
    public void start() {
        File autosave = Settings.autosaveFile();
        if (autosave.exists()) {
            try {
                Game saved = SaveFile.load(autosave);
                if (!saved.checkResult()) {
                    useGame(saved);
                    return;
                }
            } catch (IOException e) {
                // unreadable: deal a fresh game instead
            }
        }
        Rule rule = Settings.getRule();
        if (rule == null) {
            rule = LevelDialog.choose(this, Rule.EASY);
            if (rule == null)
                rule = Rule.EASY;
        }
        deal(rule, 0);
    }

    // ---- menus -------------------------------------------------------------

    private JMenuBar createMenus() {
        int cmd = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        gameMenu.add(item(newGame, KeyStroke.getKeyStroke(KeyEvent.VK_N, cmd)));
        gameMenu.add(item(restart, KeyStroke.getKeyStroke(KeyEvent.VK_R, cmd)));
        gameMenu.add(item(selectGame, KeyStroke.getKeyStroke(KeyEvent.VK_G, cmd)));
        gameMenu.addSeparator();
        gameMenu.add(item(undo, KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmd)));
        gameMenu.add(item(redo, isMac() ? KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmd | InputEvent.SHIFT_DOWN_MASK)
                                        : KeyStroke.getKeyStroke(KeyEvent.VK_Y, cmd)));
        gameMenu.add(item(hint, KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)));
        gameMenu.add(item(giveUp, null));
        gameMenu.addSeparator();
        gameMenu.add(item(save, KeyStroke.getKeyStroke(KeyEvent.VK_S, cmd)));
        gameMenu.add(item(load, KeyStroke.getKeyStroke(KeyEvent.VK_O, cmd)));
        gameMenu.addSeparator();
        gameMenu.add(item(statistics, null));
        if (!isMac()) {
            // On macOS, Settings lives in the application menu.
            gameMenu.add(item(settings, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, cmd)));
            gameMenu.addSeparator();
            gameMenu.add(item(exit, null));
        }
        bar.add(gameMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(item(contents, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)));
        if (!isMac()) {
            helpMenu.addSeparator();
            helpMenu.add(item(about, null));
        }
        bar.add(helpMenu);
        return bar;
    }

    /** The buttons in the bottom bar, for the things done most often. */
    private void createToolbar() {
        String cmd = isMac() ? "⌘" : "Ctrl+";
        status.addTool(newGame, Glyph.NEW_GAME, cmd + "N");
        status.addTool(undo, Glyph.UNDO, cmd + "Z");
        status.addTool(redo, Glyph.REDO, isMac() ? "⇧⌘Z" : "Ctrl+Y");
        status.addTool(hint, Glyph.HINT, "H");
        status.addTool(giveUp, Glyph.GIVE_UP, null);
        status.addTool(settings, Glyph.SETTINGS, cmd + ",");
        status.addMenu(Glyph.MORE, "More", restart, selectGame, null, save, load, null,
                       statistics, null, contents, about);
    }

    private static JMenuItem item(Action action, KeyStroke key) {
        JMenuItem item = new JMenuItem(action);
        if (key != null)
            item.setAccelerator(key);
        return item;
    }

    private static Action action(String name, Runnable run) {
        return new AbstractAction(name) {
            @Override public void actionPerformed(ActionEvent e) { run.run(); }
        };
    }

    private final Action newGame = action("New Game", () -> {
        if (confirmAbandon("Start a new game?"))
            deal(preferredRule(), 0);
    });
    private final Action restart = action("Restart This Game", () -> {
        if (game.getDealNumber() > 0 && confirmAbandon("Start this game again from the beginning?"))
            deal(game.getRule(), game.getDealNumber());
    });
    private final Action selectGame = action("Select Game Number…", this::selectGame);
    private final Action undo = action("Undo", () -> change(game::undo));
    private final Action redo = action("Redo", () -> change(game::redo));
    private final Action hint = action("Hint", this::hint);
    private final Action giveUp = action("Give Up…", this::giveUp);
    private final Action save = action("Save", this::save);
    private final Action load = action("Load", this::load);
    private final Action settings = action("Settings…", this::showSettings);
    private final Action statistics = action("Statistics…", () -> StatsDialog.show(this));
    private final Action exit = action("Exit", this::quit);
    private final Action contents = action("Contents", () -> new HelpDialog(this, APP_NAME).setVisible(true));
    private final Action about = action("About " + APP_NAME, this::showAbout);

    private void updateActions() {
        boolean playing = !over && !busy;
        undo.setEnabled(playing && game.canUndo());
        redo.setEnabled(playing && game.canRedo());
        hint.setEnabled(playing);
        giveUp.setEnabled(playing);
        save.setEnabled(playing);
        restart.setEnabled(!busy && game.getDealNumber() > 0);
        for (Action a : new Action[] {newGame, selectGame, settings, load})
            a.setEnabled(!busy);
    }

    private void showSettings() {
        SettingsDialog.Result chosen = SettingsDialog.show(this, preferredRule(), art);
        if (chosen == null)
            return;
        if (chosen.back != null && !(chosen.back.equals(art.getBackName())
                && (!CardArt.CUSTOM.equals(chosen.back) || chosen.customPath.equals(Settings.getCustomBackImage())))) {
            if (CardArt.CUSTOM.equals(chosen.back))
                Settings.setCustomBackImage(chosen.customPath);
            Settings.setCardBack(chosen.back);
            art.setBack(chosen.back, chosen.customPath);
            board.repaint();
        }
        if (chosen.rule != null && chosen.rule != preferredRule()) {
            Settings.setRule(chosen.rule);
            if (!busy && (over || game.getMoveCount() == 0))
                deal(chosen.rule, 0);     // nothing to lose: switch now
            else
                notice.show(name(chosen.rule) + " starts with your next game.", newGame);
        }
    }

    /** The level for the next new game. */
    private Rule preferredRule() {
        Rule rule = Settings.getRule();
        return rule != null ? rule : game.getRule();
    }

    // ---- dealing ------------------------------------------------------------

    /** Deals game {@code number}, or a random winnable one if 0. */
    private void deal(Rule rule, int number) {
        setBusy(true);
        notice.show(number == 0 ? "Shuffling for a winnable deal…" : "Dealing game " + format(number) + "…");
        new Thread(() -> {
            int n = number == 0 ? Dealer.winnableDeal(rule, random) : number;
            boolean checked = number == 0 || Dealer.check(rule, n) == Solver.Outcome.SOLVED;
            SwingUtilities.invokeLater(() -> {
                Game g = new Game();
                g.deal(rule, n);
                useGame(g);
                board.animateDeal();
                Settings.setRule(rule);
                if (!checked)
                    notice.show("The computer couldn't find a way to win game " + format(n)
                                + " at this level. It may be impossible.");
            });
        }, "dealer").start();
    }

    private void useGame(Game g) {
        game = g;
        over = false;
        version++;
        hintIndex = 0;
        clock.reset(g.getElapsedMillis());
        board.setGame(g);
        setBusy(false);
        notice.dismiss();
        autosave();
        updateStatus();
    }

    private void selectGame() {
        String text = JOptionPane.showInputDialog(this,
            "Game number (1 to " + format(Dealer.MAX_DEAL) + "):", "Select Game", JOptionPane.PLAIN_MESSAGE);
        if (text == null)
            return;
        try {
            int n = Integer.parseInt(text.replaceAll("[^0-9]", ""));
            if (n < 1 || n > Dealer.MAX_DEAL)
                throw new NumberFormatException();
            if (confirmAbandon("Start game " + format(n) + "?"))
                deal(game.getRule(), n);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a number from 1 to " + format(Dealer.MAX_DEAL) + ".");
        }
    }

    /** Asks before leaving a game in progress, which then counts as lost. */
    private boolean confirmAbandon(String question) {
        if (over || game.getMoveCount() == 0)
            return true;
        int answer = JOptionPane.showConfirmDialog(this,
            question + "\nThe game in progress will count as a loss.", APP_NAME,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION)
            return false;
        Settings.recordResult(game.getRule(), false);
        return true;
    }

    // ---- playing ------------------------------------------------------------

    /** Undo or redo, animated. */
    private void change(Runnable step) {
        if (over || busy)
            return;
        board.animate(step, BoardPanel.NORMAL);
        afterChange();
    }

    private void playerMoved() {
        if (over)
            return;
        clock.start();
        afterChange();
    }

    private void afterChange() {
        version++;
        hintIndex = 0;
        notice.dismiss();
        if (game.checkResult()) {
            won();
            return;
        }
        autosave();
        updateStatus();
        checkForDeadEnd();
    }

    private void won() {
        over = true;
        board.setInteractive(false);
        clock.stop();
        Settings.recordResult(game.getRule(), true);
        Settings.autosaveFile().delete();
        updateStatus();
        notice.show("You won in " + game.getMoveCount() + " moves and " + formatTime(game.getElapsedMillis()) + "!",
                    newGame);
    }

    private void hint() {
        if (over || busy)
            return;
        List<Move> moves = game.getAvailableMoves();
        if (!moves.isEmpty()) {
            board.showHint(moves.get(hintIndex++ % moves.size()));
        } else if (!game.isReserveUsed()) {
            board.showReserveHint();
        } else {
            notice.show("No moves left.", undo, giveUp);
        }
    }

    /** Looks, in the background, for proof the game can no longer be won. */
    private void checkForDeadEnd() {
        if (deadEndCheck != null)
            deadEndCheck.cancel();
        if (game.noMovesLeft()) {
            notice.show("No moves left.", undo, giveUp);
            return;
        }
        final int asked = version;
        final Game snapshot;
        try {
            snapshot = SaveFile.fromText(SaveFile.toText(game));
        } catch (IOException e) {
            return;
        }
        final Solver solver = new Solver(snapshot.getRule(), 600000);
        deadEndCheck = solver;
        Thread t = new Thread(() -> {
            Solver.Outcome outcome = solver.solve(snapshot.getBoard()).outcome;
            SwingUtilities.invokeLater(() -> {
                if (outcome == Solver.Outcome.UNSOLVABLE && asked == version && !over)
                    notice.show("There's no way to win from here.", undo, giveUp);
            });
        }, "dead-end check");
        t.setDaemon(true);
        t.start();
    }

    private void giveUp() {
        if (over || busy)
            return;
        int answer = JOptionPane.showConfirmDialog(this,
            "Give up? The computer will finish the game, taking back moves if it has to.\n"
            + "The game counts as a loss.", "Give Up", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION)
            return;
        over = true;
        clock.stop();
        Settings.recordResult(game.getRule(), false);
        Settings.autosaveFile().delete();
        if (deadEndCheck != null)
            deadEndCheck.cancel();
        setBusy(true);
        notice.show("Working out how to win…");
        Game playing = game;
        new Thread(() -> {
            Dealer.Plan plan = Dealer.finish(playing);
            SwingUtilities.invokeLater(() -> play(playing, plan));
        }, "finisher").start();
    }

    /** Plays a plan out on the table, one animated step at a time. */
    private void play(Game playing, Dealer.Plan plan) {
        if (playing != game)
            return;
        if (plan == null) {
            setBusy(false);
            notice.show("This game can't be won, even from the start.", newGame);
            return;
        }
        notice.show(plan.undo > 0 ? "Taking back " + plan.undo + (plan.undo == 1 ? " move…" : " moves…")
                                  : "Finishing the game…");
        Deque<Runnable> steps = new ArrayDeque<Runnable>();
        for (int i = 0; i < plan.undo; i++)
            steps.add(() -> board.animate(game::forceUndo, BoardPanel.NORMAL));
        steps.add(() -> notice.show("Finishing the game…"));
        for (Move m : plan.moves)
            steps.add(() -> board.animate(() -> {
                if (m == Solver.DEAL_RESERVE)
                    game.useReserve();
                else
                    game.move(m);
            }, BoardPanel.SLOW));
        Timer timer = new Timer(plan.undo > 12 ? 160 : 380, null);
        timer.addActionListener(e -> {
            if (playing != game) {
                timer.stop();
                return;
            }
            if (board.isAnimating())
                return;
            Runnable next = steps.poll();
            if (next != null) {
                next.run();
                if (steps.size() == plan.moves.size())
                    timer.setDelay(380);
                updateStatus();
                return;
            }
            timer.stop();
            setBusy(false);
            notice.show("That's how this game could be won.", newGame);
        });
        timer.start();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        board.setInteractive(!busy && !over);
        updateActions();
    }

    // ---- saving -------------------------------------------------------------

    private void autosave() {
        if (over)
            return;
        game.setElapsedMillis(clock.elapsed());
        try {
            SaveFile.save(game, Settings.autosaveFile());
        } catch (IOException e) {
            // Not fatal; the game just won't resume next time.
        }
    }

    private void save() {
        game.setElapsedMillis(clock.elapsed());
        try {
            SaveFile.save(game, Settings.saveFile());
            notice.show("Saved to " + Settings.saveFile() + ".");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save game.");
        }
    }

    private void load() {
        File file = Settings.saveFile();
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "There is no saved game yet.");
            return;
        }
        try {
            Game loaded = SaveFile.load(file);
            if (confirmAbandon("Load the saved game?"))
                useGame(loaded);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load game.");
        }
    }

    private void quit() {
        autosave();
        dispose();
        System.exit(0);
    }

    // ---- status and clock ----------------------------------------------------

    private void updateStatus() {
        String number = game.getDealNumber() > 0 ? "Game " + format(game.getDealNumber()) + "  ·  " : "";
        status.setLeft(number + name(game.getRule()));
        status.setRight("Moves " + game.getMoveCount() + "  ·  " + formatTime(clock.elapsed()));
        updateActions();
    }

    /** Game time, counted only while the window is in front and the game is on. */
    private final class Clock {
        void reset(long elapsed) {
            base = elapsed;
            since = 0;
            running = false;
        }

        void start() {
            if (!running && !over) {
                running = true;
                since = isActive() ? System.currentTimeMillis() : 0;
            }
        }

        void stop() {
            base = elapsed();
            since = 0;
            running = false;
            game.setElapsedMillis(base);
        }

        void pause() {
            if (since != 0) {
                base += System.currentTimeMillis() - since;
                since = 0;
            }
        }

        void resume() {
            if (running && since == 0)
                since = System.currentTimeMillis();
        }

        long elapsed() {
            return base + (since == 0 ? 0 : System.currentTimeMillis() - since);
        }

        private long base, since;
        private boolean running;
    }

    // ---- desktop -------------------------------------------------------------

    private void integrateWithDesktop() {
        if (!Desktop.isDesktopSupported())
            return;
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT))
            desktop.setAboutHandler(e -> showAbout());
        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES))
            desktop.setPreferencesHandler(e -> {
                if (!busy)
                    showSettings();
            });
        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER))
            desktop.setQuitHandler((e, response) -> quit());
    }

    private void showAbout() {
        new AboutDialog(this, icon, APP_NAME, APP_VERSION, "Aaron Ding", "scorpion@webthinking.io").setVisible(true);
    }

    static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    static String name(Rule rule) {
        String level = rule.getLevel();
        return Character.toUpperCase(level.charAt(0)) + level.substring(1);
    }

    static String formatTime(long millis) {
        long s = millis / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                         : String.format("%d:%02d", s / 60, s % 60);
    }

    private static String format(int number) {
        return String.format("#%,d", number);
    }

}
