package com.family.solitaire.ui;

import static com.family.solitaire.model.CardConstants.NCOLS;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.family.solitaire.model.Card;
import com.family.solitaire.model.Column;
import com.family.solitaire.model.Game;
import com.family.solitaire.model.Move;
import com.family.solitaire.model.Rule;

/**
 * Draws the table and handles the mouse. The layout scales with the window.
 *
 * <p>Animation works by remembering where every card was drawn before a
 * change to the game, and gliding each card that moved (or turned over) from
 * there to its new place. That one mechanism covers dragging, double-click,
 * undo and redo, dealing, and the computer finishing a game.
 *
 * @author Aaron Ding
 */
final class BoardPanel extends JComponent {

    /** Told when the player changes the game. */
    interface Listener {
        void playerMoved();
    }

    static final int QUICK = 120, NORMAL = 220, SLOW = 320;

    private final Map<Integer, Anim> anims = new HashMap<Integer, Anim>();

    BoardPanel(Game game, CardArt art, Listener listener) {
        this.game = game;
        this.art = art;
        this.listener = listener;
        setOpaque(true);
        MouseAdapter mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setGame(Game game) {
        this.game = game;
        anims.clear();
        dragging = null;
        clearHint();
        repaint();
    }

    /** Stops accepting moves, e.g. while the computer plays. */
    void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive)
            dragging = null;
        setCursor(Cursor.getDefaultCursor());
    }

    // ---- animation ---------------------------------------------------------

    /** Applies {@code change} to the game and animates the cards it moved. */
    void animate(Runnable change, int duration) {
        Map<Integer, Spot> before = spots();
        change.run();
        animateFrom(before, duration);
    }

    /** Deals: every card flies out from the reserve corner. */
    void animateDeal() {
        Layout lay = currentLayout();
        Map<Integer, Spot> before = new HashMap<Integer, Spot>();
        Map<Integer, Integer> delays = new HashMap<Integer, Integer>();
        int order = 0;
        for (int row = 0; row < 7; row++) {
            for (int c = 0; c < NCOLS; c++) {
                Column column = game.getColumn(c);
                if (row < column.getSize()) {
                    int v = column.getCard(row).value();
                    before.put(v, new Spot(lay.reserveX, lay.top, false));
                    delays.put(v, order++ * 14);
                }
            }
        }
        long now = System.currentTimeMillis();
        anims.clear();
        for (Map.Entry<Integer, Spot> e : before.entrySet())
            anims.put(e.getKey(), new Anim(e.getValue(), now + delays.get(e.getKey()), 260));
        startTimer();
    }

    private void animateFrom(Map<Integer, Spot> before, int duration) {
        Map<Integer, Spot> after = spots();
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Spot> e : after.entrySet()) {
            Spot was = before.get(e.getKey());
            if (was == null)
                continue;
            Anim running = anims.get(e.getKey());
            if (running != null)
                was = running.current(e.getValue(), now);   // continue from where it is now
            Spot now2 = e.getValue();
            if (Math.abs(was.x - now2.x) > 0.5 || Math.abs(was.y - now2.y) > 0.5 || was.faceUp != now2.faceUp)
                anims.put(e.getKey(), new Anim(was, now, duration));
        }
        startTimer();
        repaint();
    }

    boolean isAnimating() {
        return !anims.isEmpty();
    }

    private void startTimer() {
        if (!timer.isRunning())
            timer.start();
    }

    private final Timer timer = new Timer(15, e -> {
        long now = System.currentTimeMillis();
        anims.values().removeIf(a -> a.done(now));
        if (anims.isEmpty())
            ((Timer)e.getSource()).stop();
        repaint();
    });

    /** Where a card is drawn, and which side is up. */
    private static final class Spot {
        Spot(double x, double y, boolean faceUp) {
            this.x = x;
            this.y = y;
            this.faceUp = faceUp;
        }
        final double x, y;
        final boolean faceUp;
    }

    private static final class Anim {
        Anim(Spot from, long start, int duration) {
            this.from = from;
            this.start = start;
            this.duration = duration;
        }
        final Spot from;
        final long start;
        final int duration;

        double progress(long now) {
            return Math.max(0, Math.min(1, (now - start) / (double)duration));
        }

        boolean done(long now) {
            return now >= start + duration;
        }

        boolean waiting(long now) {
            return now < start;
        }

        Spot current(Spot to, long now) {
            double t = ease(progress(now));
            return new Spot(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t,
                            progress(now) < 0.5 ? from.faceUp : to.faceUp);
        }

        static double ease(double t) {
            return 1 - Math.pow(1 - t, 3);
        }
    }

    /** Where every card is drawn when nothing is moving. */
    private Map<Integer, Spot> spots() {
        Layout lay = currentLayout();
        Map<Integer, Spot> map = new HashMap<Integer, Spot>();
        for (int c = 0; c < NCOLS; c++) {
            Column column = game.getColumn(c);
            for (int r = 0; r < column.getSize(); r++) {
                Card card = column.getCard(r);
                Spot spot = new Spot(lay.columnX[c], lay.rowY(c, r), card.isFacedUp());
                if (dragging != null && c == dragging.column && r >= dragging.row)
                    spot = new Spot(spot.x + dragging.dx, spot.y + dragging.dy, true);
                map.put(card.value(), spot);
            }
        }
        Card[] reserve = game.getReserve().getCards();
        for (int i = 0; i < reserve.length; i++)
            if (reserve[i] != null)
                map.put(reserve[i].value(), new Spot(lay.reserveX + lay.reserveOffset(i), lay.top + lay.reserveOffset(i), false));
        return map;
    }

    // ---- hints -------------------------------------------------------------

    /** Flashes the cards to move, then where they go. */
    void showHint(Move move) {
        clearHint();
        Column from = game.getColumn(move.fromColumn);
        for (int r = move.fromRow; r < from.getSize(); r++)
            hintCards.add(from.getCard(r).value());
        hintPhase = 0;
        hintMove = move;
        hintTimer.restart();
        repaint();
    }

    /** Flashes the reserve, when dealing it is the only thing left. */
    void showReserveHint() {
        clearHint();
        hintReserve = true;
        hintPhase = 2;
        hintTimer.restart();
        repaint();
    }

    private void clearHint() {
        hintCards.clear();
        hintEmptyColumn = -1;
        hintReserve = false;
        hintMove = null;
        hintTimer.stop();
        repaint();
    }

    private final Timer hintTimer = new Timer(550, e -> nextHintPhase());

    private void nextHintPhase() {
        hintPhase++;
        hintCards.clear();
        if (hintPhase == 1 && hintMove != null) {
            Column to = game.getColumn(hintMove.toColumn);
            if (to.isEmpty())
                hintEmptyColumn = hintMove.toColumn;
            else
                hintCards.add(to.getLastCard().value());
        } else {
            clearHint();
        }
        repaint();
    }

    private final Set<Integer> hintCards = new HashSet<Integer>();
    private int hintEmptyColumn = -1;
    private boolean hintReserve;
    private int hintPhase;
    private Move hintMove;

    // ---- layout ------------------------------------------------------------

    /** Positions, recomputed from the window size and the cards on the table. */
    private final class Layout {
        Layout(double width, double height) {
            double margin = Math.max(10, Math.min(width, height) * 0.025);
            double gapRatio = 0.16;
            cardW = (width - 2 * margin) / (8 + 7 * gapRatio + 0.35);
            cardW = Math.min(cardW, (height - 2 * margin) / (CardArt.ASPECT * 2.6));
            cardW = Math.max(cardW, 36);
            cardH = cardW * CardArt.ASPECT;
            double gap = cardW * gapRatio;
            double total = 8 * cardW + 7 * gap + cardW * 0.35;
            double x0 = (width - total) / 2;
            reserveX = x0;
            for (int c = 0; c < NCOLS; c++)
                columnX[c] = x0 + cardW * 1.35 + gap + c * (cardW + gap);
            top = margin;
            bottom = height - margin;
            double faceDown = cardH * 0.10;
            double faceUp = cardH * 0.25;
            for (int c = 0; c < NCOLS; c++) {
                Column column = game.getColumn(c);
                int down = column.getFaceDownCount();
                int up = column.getSize() - down;
                downStep[c] = faceDown;
                upStep[c] = faceUp;
                if (up > 1) {
                    double room = bottom - top - cardH - down * faceDown;
                    upStep[c] = Math.max(cardH * 0.08, Math.min(faceUp, room / (up - 1)));
                }
            }
        }

        double rowY(int c, int row) {
            Column column = game.getColumn(c);
            double y = top;
            for (int r = 0; r < row; r++)
                y += column.getCard(r).isFacedDown() ? downStep[c] : upStep[c];
            return y;
        }

        double reserveOffset(int i) {
            return i * cardW * 0.035;
        }

        /** The card under the point, as {column, row}, or null. */
        int[] hit(double x, double y) {
            for (int c = 0; c < NCOLS; c++) {
                if (x < columnX[c] || x >= columnX[c] + cardW)
                    continue;
                Column column = game.getColumn(c);
                for (int r = column.getSize() - 1; r >= 0; r--) {
                    double cy = rowY(c, r);
                    if (y >= cy && y < cy + cardH)
                        return new int[] {c, r};
                }
            }
            return null;
        }

        boolean onReserve(double x, double y) {
            return x >= reserveX && x < reserveX + cardW * 1.1 && y >= top && y < top + cardH * 1.1;
        }

        /** Where a card dropped at (x, y) would land in column c. */
        Rectangle2D target(int c) {
            Column column = game.getColumn(c);
            double y = column.isEmpty() ? top : rowY(c, column.getSize() - 1);
            return new Rectangle2D.Double(columnX[c], y, cardW, cardH);
        }

        double cardW, cardH, reserveX, top, bottom;
        final double[] columnX = new double[NCOLS];
        final double[] downStep = new double[NCOLS];
        final double[] upStep = new double[NCOLS];
    }

    private Layout currentLayout() {
        return new Layout(getWidth(), getHeight());
    }

    // ---- painting ----------------------------------------------------------

    private static final Color FELT_TOP = new Color(0x1E7A45);
    private static final Color FELT_BOTTOM = new Color(0x0E5530);
    private static final Color SLOT = new Color(255, 255, 255, 50);
    private static final Color SLOT_FILL = new Color(0, 0, 0, 28);
    private static final Color GLOW = new Color(0xFFD54F);
    private static final Color TARGET_GLOW = new Color(0x80FFFFFF, true);

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics.create();
        CardArt.quality(g);
        g.setPaint(new GradientPaint(0, 0, FELT_TOP, 0, getHeight(), FELT_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());

        Layout lay = currentLayout();
        double scale = g.getTransform().getScaleX();
        int pw = (int)Math.round(lay.cardW * scale);
        int ph = (int)Math.round(pw * CardArt.ASPECT);
        long now = System.currentTimeMillis();

        // Empty places
        drawSlot(g, lay, lay.reserveX, lay.top, hintReserve);
        for (int c = 0; c < NCOLS; c++)
            drawSlot(g, lay, lay.columnX[c], lay.top, hintEmptyColumn == c || dropColumn == c && game.getColumn(c).isEmpty());

        // Resting cards, then moving ones on top in the order they will land
        Map<Integer, Spot> spots = spots();
        List<Integer> moving = new ArrayList<Integer>();
        List<Integer> waiting = new ArrayList<Integer>();
        Card[] reserve = game.getReserve().getCards();
        for (int i = 0; i < reserve.length; i++)
            if (reserve[i] != null)
                paintCard(g, lay, reserve[i], spots.get(reserve[i].value()), pw, ph, now, moving, waiting);
        for (int c = 0; c < NCOLS; c++) {
            Column column = game.getColumn(c);
            for (int r = 0; r < column.getSize(); r++) {
                Card card = column.getCard(r);
                if (dragging != null && c == dragging.column && r >= dragging.row) {
                    moving.add(card.value());
                    continue;
                }
                paintCard(g, lay, card, spots.get(card.value()), pw, ph, now, moving, waiting);
            }
        }
        for (int v : waiting)
            drawCard(g, lay, v, anims.get(v).from, pw, ph, 1, false);
        for (int v : moving) {
            Spot to = spots.get(v);
            Anim a = anims.get(v);
            if (a == null) {
                drawCard(g, lay, v, to, pw, ph, 1, dragging != null);
                continue;
            }
            Spot at = a.current(to, now);
            double t = a.progress(now);
            double flip = a.from.faceUp != to.faceUp ? Math.abs(1 - 2 * t) : 1;
            drawCard(g, lay, v, at, pw, ph, Math.max(0.02, flip), true);
        }

        // A card being looked at with the right button
        if (peek != null) {
            Spot s = spots.get(peek.value());
            if (s != null)
                drawCard(g, lay, peek.value(), s, pw, ph, 1, true);
        }
        g.dispose();
    }

    private void paintCard(Graphics2D g, Layout lay, Card card, Spot spot, int pw, int ph, long now,
                           List<Integer> moving, List<Integer> waiting) {
        Anim a = anims.get(card.value());
        if (a != null) {
            (a.waiting(now) ? waiting : moving).add(card.value());
            return;
        }
        drawCard(g, lay, card.value(), spot, pw, ph, 1, false);
        if (hintCards.contains(card.value()) || dropHighlight(card))
            glow(g, lay, spot, hintCards.contains(card.value()) ? GLOW : TARGET_GLOW);
    }

    private boolean dropHighlight(Card card) {
        return dropColumn >= 0 && card.equals(game.getColumn(dropColumn).getLastCard());
    }

    private void drawSlot(Graphics2D g, Layout lay, double x, double y, boolean lit) {
        RoundRectangle2D r = new RoundRectangle2D.Double(x, y, lay.cardW, lay.cardH, lay.cardW * 0.1, lay.cardW * 0.1);
        g.setColor(SLOT_FILL);
        g.fill(r);
        g.setColor(lit ? GLOW : SLOT);
        g.setStroke(new BasicStroke(lit ? 3f : 1.5f));
        g.draw(r);
    }

    private void glow(Graphics2D g, Layout lay, Spot s, Color color) {
        double pad = 1.5;
        RoundRectangle2D r = new RoundRectangle2D.Double(s.x - pad, s.y - pad, lay.cardW + 2 * pad,
            lay.cardH + 2 * pad, lay.cardW * 0.11, lay.cardW * 0.11);
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.draw(r);
    }

    /** Draws a card with a soft shadow; {@code widthFactor} below 1 squeezes it for a flip. */
    private void drawCard(Graphics2D g, Layout lay, int value, Spot s, int pw, int ph,
                          double widthFactor, boolean lifted) {
        Card card = Card.valueOf(value);
        BufferedImage img = s.faceUp ? art.face(card, pw, ph) : art.back(pw, ph);
        double w = lay.cardW * widthFactor;
        double x = s.x + (lay.cardW - w) / 2;

        double arc = lay.cardW * CORNER;
        double lift = lifted ? lay.cardW * 0.03 : lay.cardW * 0.012;
        g.setColor(new Color(0, 0, 0, lifted ? 70 : 45));
        g.fill(new RoundRectangle2D.Double(x + lift * 0.3, s.y + lift, w, lay.cardH, arc, arc));
        // A faint shadow just above the top edge, where the card overlaps the one beneath.
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new RoundRectangle2D.Double(x, s.y - lay.cardW * 0.012, w, lay.cardH, arc, arc));

        if (widthFactor == 1 && img.getWidth() == pw) {
            // Draw on whole device pixels so the art stays crisp.
            AffineTransform t = g.getTransform();
            double dx = Math.round(t.getTranslateX() + x * t.getScaleX());
            double dy = Math.round(t.getTranslateY() + s.y * t.getScaleY());
            g.setTransform(AffineTransform.getTranslateInstance(dx, dy));
            g.drawImage(img, 0, 0, null);
            g.setTransform(t);
        } else {
            g.drawImage(img, (int)Math.round(x), (int)Math.round(s.y), (int)Math.round(w), (int)Math.round(lay.cardH), null);
        }

        // Outline every card, so backs printed edge to edge don't run into each other.
        double px = 1 / g.getTransform().getScaleX();
        g.setColor(EDGE);
        g.setStroke(new BasicStroke((float)px));
        g.draw(new RoundRectangle2D.Double(x + px / 2, s.y + px / 2, w - px, lay.cardH - px, arc, arc));
    }

    /** Corner rounding of the card art, as a fraction of card width (diameter). */
    private static final double CORNER = 0.076;
    private static final Color EDGE = new Color(0, 0, 0, 90);

    // ---- mouse -------------------------------------------------------------

    /** Cards being dragged: from a column and row, offset from where they lie. */
    private static final class Drag {
        int column, row;
        double dx, dy;
        Point pressed;
        boolean started;
    }

    private Drag dragging;
    private int dropColumn = -1;
    private Card peek;

    private final class Mouse extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (!interactive || game == null)
                return;
            clearHint();
            Layout lay = currentLayout();
            if (SwingUtilities.isRightMouseButton(e) || e.isControlDown()) {
                int[] at = lay.hit(e.getX(), e.getY());
                if (at != null && game.getColumn(at[0]).getCard(at[1]).isFacedUp()) {
                    peek = game.getColumn(at[0]).getCard(at[1]);
                    repaint();
                }
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e))
                return;
            if (lay.onReserve(e.getX(), e.getY())) {
                if (!game.isReserveUsed()) {
                    animate(game::useReserve, SLOW);
                    listener.playerMoved();
                }
                return;
            }
            int[] at = lay.hit(e.getX(), e.getY());
            if (at == null || game.getColumn(at[0]).getCard(at[1]).isFacedDown())
                return;
            if (e.getClickCount() == 2) {
                Move best = bestMove(game, at[0], at[1]);
                if (best != null) {
                    animate(() -> game.move(best), NORMAL);
                    listener.playerMoved();
                }
                return;
            }
            dragging = new Drag();
            dragging.column = at[0];
            dragging.row = at[1];
            dragging.pressed = e.getPoint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragging == null)
                return;
            dragging.dx = e.getX() - dragging.pressed.x;
            dragging.dy = e.getY() - dragging.pressed.y;
            if (!dragging.started && Math.hypot(dragging.dx, dragging.dy) < 4)
                return;
            dragging.started = true;
            dropColumn = dropTarget(currentLayout());
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (peek != null) {
                peek = null;
                repaint();
            }
            if (dragging == null)
                return;
            Drag d = dragging;
            if (!d.started) {
                dragging = null;
                return;
            }
            int target = dropTarget(currentLayout());
            Map<Integer, Spot> before = spots();   // includes the drag offset
            dragging = null;
            dropColumn = -1;
            if (target >= 0 && game.move(new Move(d.column, d.row, target))) {
                animateFrom(before, QUICK);
                listener.playerMoved();
            } else {
                animateFrom(before, NORMAL);    // slide back
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!interactive || game == null)
                return;
            Layout lay = currentLayout();
            int[] at = lay.hit(e.getX(), e.getY());
            boolean active = (at != null && game.getColumn(at[0]).getCard(at[1]).isFacedUp())
                || (lay.onReserve(e.getX(), e.getY()) && !game.isReserveUsed());
            setCursor(active ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }
    }

    /** The legal column the dragged cards overlap most, or -1. */
    private int dropTarget(Layout lay) {
        if (dragging == null)
            return -1;
        double x = lay.columnX[dragging.column] + dragging.dx;
        double y = lay.rowY(dragging.column, dragging.row) + dragging.dy;
        Rectangle2D held = new Rectangle2D.Double(x, y, lay.cardW, lay.cardH);
        int best = -1;
        double bestArea = 0;
        for (int c = 0; c < NCOLS; c++) {
            if (c == dragging.column || !game.getRule().isValidMove(new Move(dragging.column, dragging.row, c), game.getBoard()))
                continue;
            Rectangle2D overlap = held.createIntersection(lay.target(c));
            double area = overlap.isEmpty() ? 0 : overlap.getWidth() * overlap.getHeight();
            if (area > bestArea) {
                bestArea = area;
                best = c;
            }
        }
        return best;
    }

    /**
     * Where a double-clicked card should go: onto the next card up in its own
     * suit if possible, then to an empty column if it is a King, then anywhere legal.
     */
    static Move bestMove(Game game, int column, int row) {
        Rule rule = game.getRule();
        Card card = game.getColumn(column).getCard(row);
        Move fallback = null;
        Move toEmpty = null;
        for (int c = 0; c < NCOLS; c++) {
            Move m = new Move(column, row, c);
            if (!rule.isValidMove(m, game.getBoard()))
                continue;
            Card onto = game.getColumn(c).getLastCard();
            if (onto == null) {
                if (row > 0 && toEmpty == null)
                    toEmpty = m;
            } else if (onto.suit() == card.suit()) {
                return m;
            } else if (fallback == null) {
                fallback = m;
            }
        }
        return toEmpty != null ? toEmpty : fallback;
    }

    private Game game;
    private final CardArt art;
    private final Listener listener;
    private boolean interactive = true;
}
