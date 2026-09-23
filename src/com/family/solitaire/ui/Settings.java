package com.family.solitaire.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.family.solitaire.model.Rule;

/**
 * Remembered choices and statistics, kept in settings.properties in the
 * game's data folder, next to the autosaved game:
 * <ul>
 * <li>macOS: ~/Library/Application Support/Scorpion Solitaire</li>
 * <li>Windows: %APPDATA%\Scorpion Solitaire</li>
 * <li>Linux: ~/.local/share/scorpion-solitaire</li>
 * </ul>
 *
 * @author Aaron Ding
 */
final class Settings {

    private static final Properties PROPS = new Properties();

    static {
        File file = settingsFile();
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                PROPS.load(in);
            } catch (IOException e) {
                // Start from defaults.
            }
        } else {
            importJavaPreferences();
        }
    }

    private Settings() { }

    /** The level last played, or null the first time. */
    static Rule getRule() {
        String level = PROPS.getProperty("level");
        try {
            return level == null ? null : Rule.forLevel(level);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static void setRule(Rule rule) {
        set("level", rule.getLevel());
    }

    /** Name of a built-in card back, or {@link CardArt#CUSTOM} for the player's own image. */
    static String getCardBack() {
        return PROPS.getProperty("cardBack", CardArt.DEFAULT_BACK);
    }

    static void setCardBack(String name) {
        set("cardBack", name);
    }

    static String getCustomBackImage() {
        return PROPS.getProperty("customBackImage");
    }

    static void setCustomBackImage(String path) {
        set("customBackImage", path);
    }

    // ---- statistics, per level --------------------------------------------

    static final class Stats {
        int played, won, streak, bestStreak;
    }

    private static final String[] STAT_KEYS = {"played", "won", "streak", "bestStreak"};

    static Stats getStats(Rule rule) {
        Stats s = new Stats();
        String p = rule.getLevel() + ".";
        s.played = getInt(p + "played");
        s.won = getInt(p + "won");
        s.streak = getInt(p + "streak");
        s.bestStreak = getInt(p + "bestStreak");
        return s;
    }

    static void recordResult(Rule rule, boolean won) {
        Stats s = getStats(rule);
        s.played++;
        if (won) {
            s.won++;
            s.streak++;
            s.bestStreak = Math.max(s.bestStreak, s.streak);
        } else {
            s.streak = 0;
        }
        String p = rule.getLevel() + ".";
        PROPS.setProperty(p + "played", String.valueOf(s.played));
        PROPS.setProperty(p + "won", String.valueOf(s.won));
        PROPS.setProperty(p + "streak", String.valueOf(s.streak));
        PROPS.setProperty(p + "bestStreak", String.valueOf(s.bestStreak));
        store();
    }

    static void resetStats() {
        for (Rule rule : Rule.values())
            for (String key : STAT_KEYS)
                PROPS.remove(rule.getLevel() + "." + key);
        store();
    }

    private static int getInt(String key) {
        try {
            return Integer.parseInt(PROPS.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void set(String key, String value) {
        if (value == null)
            PROPS.remove(key);
        else
            PROPS.setProperty(key, value);
        store();
    }

    /** Writes a temporary file first so a crash never leaves half the settings. */
    private static synchronized void store() {
        File file = settingsFile();
        File tmp = new File(file.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            PROPS.store(out, "Scorpion Solitaire settings and statistics");
        } catch (IOException e) {
            return;
        }
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            tmp.delete();
        }
    }

    /**
     * Earlier builds kept these in Java's shared preferences store; bring them
     * over once, then remove them from there.
     */
    private static void importJavaPreferences() {
        try {
            if (!Preferences.userRoot().nodeExists("com/family/solitaire"))
                return;
            Preferences old = Preferences.userRoot().node("com/family/solitaire");
            for (String key : old.keys())
                PROPS.setProperty(key, old.get(key, ""));
            store();
            old.removeNode();
            Preferences.userRoot().flush();
        } catch (BackingStoreException | IllegalStateException e) {
            // Nothing to import.
        }
    }

    // ---- files -------------------------------------------------------------

    private static File settingsFile() {
        return new File(dataDirectory(), "settings.properties");
    }

    /** Where the game in progress is kept between runs. */
    static File autosaveFile() {
        return new File(dataDirectory(), "autosave.dat");
    }

    /** The file the Save and Load menu items use. */
    static File saveFile() {
        return new File(System.getProperty("user.home"), "Scorpion.dat");
    }

    private static File dataDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        File dir;
        if (os.contains("mac")) {
            dir = new File(home, "Library/Application Support/Scorpion Solitaire");
        } else if (os.contains("win") && System.getenv("APPDATA") != null) {
            dir = new File(System.getenv("APPDATA"), "Scorpion Solitaire");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            dir = new File(xdg != null ? xdg : home + "/.local/share", "scorpion-solitaire");
        }
        dir.mkdirs();
        return dir;
    }
}
