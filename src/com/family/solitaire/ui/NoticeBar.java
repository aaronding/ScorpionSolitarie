package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A strip above the table for messages that need no dialog: "You won",
 * "There's no way to win from here", and so on, with buttons for what to do next.
 *
 * @author Aaron Ding
 */
final class NoticeBar extends JPanel {

    NoticeBar() {
        super(new BorderLayout());
        setBackground(new Color(0xFFF4CE));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE6D38F)),
            BorderFactory.createEmptyBorder(6, 14, 6, 8)));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, label.getFont().getSize2D() + 1));
        add(label, BorderLayout.CENTER);
        buttons.setOpaque(false);
        add(buttons, BorderLayout.EAST);
        setVisible(false);
    }

    /** Shows a message with a button for each action given. */
    void show(String message, Action... actions) {
        label.setText(message);
        buttons.removeAll();
        for (Action action : actions) {
            JButton button = new JButton(action);
            button.setText(((String)action.getValue(Action.NAME)).replace("…", ""));
            buttons.add(button);
        }
        setVisible(true);
        revalidate();
        repaint();
    }

    void dismiss() {
        setVisible(false);
    }

    private final JLabel label = new JLabel();
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
}
