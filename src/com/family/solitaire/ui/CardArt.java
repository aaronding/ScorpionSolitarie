package com.family.solitaire.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.family.solitaire.model.Card;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Card faces and backs, drawn from SVG at the exact pixel size the screen
 * needs, so they stay sharp at any window size and on high-DPI displays.
 * Images for the current size are cached; when the size changes they are
 * redrawn in the background while the old ones are shown scaled.
 *
 * @author Aaron Ding
 */
final class CardArt {

    /** Width to height of every card. */
    static final double ASPECT = 333.0 / 234.0;

    static final String DEFAULT_BACK = "blue2";
    static final String CUSTOM = "custom";

    /** Built-in backs, in the order the picker shows them. */
    static final String[] BACKS = {
        "blue2", "red2", "blue", "red", "castle", "fish",
        "cars", "astronaut", "frog", "abstract", "abstract_clouds", "abstract_scene"
    };

    private static final String[] RANKS = {
        "ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king"
    };

    CardArt(Runnable repaint) {
        this.repaint = repaint;
    }

    /** Parses every SVG; call once, off the event thread if possible. */
    void load() {
        SVGLoader loader = new SVGLoader();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                Card card = new Card(rank, suit);
                faces[card.value()] = loader.load(resource(
                    suit.name().toLowerCase() + "_" + RANKS[rank.ordinal()] + ".svg"));
            }
        }
        for (String name : BACKS)
            backs.put(name, loader.load(resource("backs/" + name + ".svg")));
    }

    private static URL resource(String name) {
        return CardArt.class.getClassLoader().getResource("res/cards/" + name);
    }

    /** Chooses the back: a built-in name, or {@link #CUSTOM} with an image file. */
    synchronized void setBack(String name, String customPath) {
        customBack = null;
        if (CUSTOM.equals(name) && customPath != null) {
            try {
                customBack = ImageIO.read(new File(customPath));
            } catch (Exception e) {
                customBack = null;
            }
        }
        backName = customBack != null ? CUSTOM : backs.containsKey(name) ? name : DEFAULT_BACK;
        backImage = null;
        backImageWidth = 0;
    }

    String getBackName() {
        return backName;
    }

    /** The face of {@code card} at {@code w} x {@code h} device pixels, or the nearest available. */
    BufferedImage face(Card card, int w, int h) {
        ensureSize(w, h);
        return faceImages[card.value()];
    }

    BufferedImage back(int w, int h) {
        ensureSize(w, h);
        synchronized (this) {
            if (backImage == null || backImageWidth != w)
                backImage = renderBack(backName, w, h);
            backImageWidth = w;
            return backImage;
        }
    }

    /** A back by name, for the picker. */
    BufferedImage preview(String name, int w, int h) {
        return renderBack(name, w, h);
    }

    // ---- sizing ------------------------------------------------------------

    private void ensureSize(int w, int h) {
        if (w == width && h == height)
            return;
        if (width == 0) {
            renderAll(w, h);           // first paint: draw now
            return;
        }
        if (w == pendingWidth && h == pendingHeight)
            return;
        pendingWidth = w;
        pendingHeight = h;
        final int generation = ++pending;
        worker.submit(() -> {
            try {
                Thread.sleep(120);     // let a window resize settle
            } catch (InterruptedException e) {
                return;
            }
            if (generation != pending)
                return;
            BufferedImage[] fresh = new BufferedImage[faces.length];
            for (int i = 0; i < faces.length; i++)
                fresh[i] = render(faces[i], w, h);
            SwingUtilities.invokeLater(() -> {
                if (generation != pending)
                    return;
                faceImages = fresh;
                width = w;
                height = h;
                pendingWidth = pendingHeight = 0;
                synchronized (this) {
                    backImage = null;
                }
                repaint.run();
            });
        });
    }

    private void renderAll(int w, int h) {
        for (int i = 0; i < faces.length; i++)
            faceImages[i] = render(faces[i], w, h);
        width = w;
        height = h;
    }

    private BufferedImage renderBack(String name, int w, int h) {
        if (CUSTOM.equals(name) && customBack != null)
            return renderCustomBack(w, h);
        SVGDocument doc = backs.containsKey(name) ? backs.get(name) : backs.get(DEFAULT_BACK);
        return render(doc, w, h);
    }

    private static BufferedImage render(SVGDocument doc, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        quality(g);
        doc.render(null, g, new ViewBox(0, 0, w, h));
        g.dispose();
        return img;
    }

    /** The player's picture, cropped to fill a white-bordered card. */
    private BufferedImage renderCustomBack(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = img.createGraphics();
        quality(g);
        double arc = w * 0.1;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
        double inset = w * 0.045;
        RoundRectangle2D inner = new RoundRectangle2D.Double(
            inset, inset, w - 2 * inset, h - 2 * inset, arc * 0.6, arc * 0.6);
        g.clip(inner);
        double scale = Math.max(inner.getWidth() / customBack.getWidth(),
                                inner.getHeight() / customBack.getHeight());
        int iw = (int)Math.ceil(customBack.getWidth() * scale);
        int ih = (int)Math.ceil(customBack.getHeight() * scale);
        g.drawImage(customBack, (int)(w - iw) / 2, (int)(h - ih) / 2, iw, ih, null);
        g.setClip(null);
        g.setColor(new Color(0, 0, 0, 60));
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc));
        g.dispose();
        return img;
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private final Runnable repaint;
    private final SVGDocument[] faces = new SVGDocument[52];
    private final Map<String, SVGDocument> backs = new LinkedHashMap<String, SVGDocument>();
    private BufferedImage[] faceImages = new BufferedImage[52];
    private int width, height;
    private volatile int pending, pendingWidth, pendingHeight;

    private String backName = DEFAULT_BACK;
    private BufferedImage customBack;
    private BufferedImage backImage;
    private int backImageWidth;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "card-art");
        t.setDaemon(true);
        return t;
    });
}
