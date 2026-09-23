package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

/**
 * The line under the table: game number and level on the left, moves and
 * time on the right.
 *
 * @author Aaron Ding
 */
final class StatusBar extends JPanel {

    StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(2, 12, 2, 12)));
        add(left, BorderLayout.WEST);
        add(tools, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        tools.setOpaque(false);
    }

    /** Adds an icon button for {@code action}, with its name (and shortcut) as the tooltip. */
    JButton addTool(Action action, Glyph glyph, String shortcut) {
        JButton b = new JButton(action);
        b.setText(null);
        b.setIcon(glyph);
        b.setDisabledIcon(glyph);
        String name = ((String)action.getValue(Action.NAME)).replace("…", "");
        b.setToolTipText(shortcut == null ? name : name + "  (" + shortcut + ")");
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        tools.add(b);
        return b;
    }

    /** A button that opens a menu of further actions. */
    void addMenu(Glyph glyph, String tip, Action... actions) {
        JButton b = new JButton(glyph);
        b.setToolTipText(tip);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (Action a : actions) {
                if (a == null)
                    menu.addSeparator();
                else
                    menu.add(new JMenuItem(a));
            }
            menu.show(b, 0, -menu.getPreferredSize().height);
        });
        tools.add(b);
    }

    void setLeft(String text) {
        left.setText(text);
    }

    void setRight(String text) {
        right.setText(text);
    }

    private final JLabel left = new JLabel(" ");
    private final JLabel right = new JLabel(" ");
    private final JPanel tools = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
}
