package com.family.solitaire.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * Small toolbar icons, drawn as shapes so they are sharp at any scale.
 * Each is designed on a 24-unit grid and scaled to the icon size.
 *
 * @author Aaron Ding
 */
enum Glyph implements Icon {

    NEW_GAME {
        void draw(Graphics2D g, Color bg) {
            RoundRectangle2D back = new RoundRectangle2D.Double(9, 3, 11, 15, 2.5, 2.5);
            RoundRectangle2D front = new RoundRectangle2D.Double(4, 6.5, 11, 15, 2.5, 2.5);
            g.draw(back);
            Color fg = g.getColor();
            g.setColor(bg);
            g.fill(front);
            g.setColor(fg);
            g.draw(front);
        }
    },
    UNDO {
        void draw(Graphics2D g, Color bg) {
            Path2D p = new Path2D.Double();
            p.moveTo(5, 10);
            p.lineTo(14, 10);
            p.append(new Arc2D.Double(9.5, 10, 9, 9, 90, -180, Arc2D.OPEN), true);
            p.lineTo(9, 19);
            g.draw(p);
            Path2D head = new Path2D.Double();
            head.moveTo(9, 5.5);
            head.lineTo(4.5, 10);
            head.lineTo(9, 14.5);
            g.draw(head);
        }
    },
    REDO {
        void draw(Graphics2D g, Color bg) {
            g.translate(24, 0);
            g.scale(-1, 1);
            UNDO.draw(g, bg);
        }
    },
    HINT {
        void draw(Graphics2D g, Color bg) {
            g.draw(new Arc2D.Double(6, 3, 12, 12, -50, 280, Arc2D.OPEN));
            g.draw(new Line2D.Double(9.8, 14.2, 9.8, 17.5));
            g.draw(new Line2D.Double(14.2, 14.2, 14.2, 17.5));
            g.draw(new Line2D.Double(9.5, 17.5, 14.5, 17.5));
            g.draw(new Line2D.Double(10.5, 21, 13.5, 21));
        }
    },
    GIVE_UP {
        void draw(Graphics2D g, Color bg) {
            g.draw(new Line2D.Double(6, 3.5, 6, 21));
            Path2D flag = new Path2D.Double();
            flag.moveTo(6, 4);
            flag.lineTo(18.5, 4);
            flag.lineTo(15.5, 8.25);
            flag.lineTo(18.5, 12.5);
            flag.lineTo(6, 12.5);
            flag.closePath();
            g.fill(flag);
            g.draw(flag);
        }
    },
    SETTINGS {
        void draw(Graphics2D g, Color bg) {
            Area gear = new Area(new Ellipse2D.Double(5, 5, 14, 14));
            for (int i = 0; i < 8; i++) {
                Area tooth = new Area(new RoundRectangle2D.Double(10.2, 1.8, 3.6, 5, 1.2, 1.2));
                tooth.transform(java.awt.geom.AffineTransform.getRotateInstance(Math.PI / 4 * i, 12, 12));
                gear.add(tooth);
            }
            gear.subtract(new Area(new Ellipse2D.Double(8.8, 8.8, 6.4, 6.4)));
            g.fill(gear);
        }
    },
    MORE {
        void draw(Graphics2D g, Color bg) {
            for (int y = 7; y <= 17; y += 5)
                g.draw(new Line2D.Double(5, y, 19, y));
        }
    };

    static final int SIZE = 18;

    abstract void draw(Graphics2D g, Color background);

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D)graphics.create();
        CardArt.quality(g);
        g.translate(x, y);
        g.scale(SIZE / 24.0, SIZE / 24.0);
        Color fg = UIManager.getColor(c.isEnabled() ? "Button.foreground" : "Button.disabledText");
        g.setColor(fg != null ? fg : Color.DARK_GRAY);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color bg = c.getParent() != null ? c.getParent().getBackground() : Color.WHITE;
        draw(g, bg);
        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
