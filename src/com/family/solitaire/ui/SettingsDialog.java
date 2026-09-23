package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.family.solitaire.model.Rule;

/**
 * Difficulty and card back in one window: Settings in the macOS app menu,
 * Game > Settings elsewhere.
 *
 * @author Aaron Ding
 */
final class SettingsDialog extends JDialog {

    /** What was chosen; null fields mean unchanged. */
    static final class Result {
        Rule rule;
        String back;
        String customPath;
    }

    /** Returns the choices, or null if cancelled. */
    static Result show(Frame owner, Rule level, CardArt art) {
        SettingsDialog dialog = new SettingsDialog(owner, level, art);
        dialog.setVisible(true);
        return dialog.result;
    }

    private SettingsDialog(Frame owner, Rule level, CardArt art) {
        super(owner, "Settings", true);
        LevelPicker levels = new LevelPicker(level);
        CardBackPicker backs = new CardBackPicker(art);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        body.add(heading("Difficulty"));
        body.add(Box.createVerticalStrut(2));
        body.add(note("Applies to the next game you start."));
        body.add(Box.createVerticalStrut(8));
        body.add(left(levels));
        body.add(Box.createVerticalStrut(14));
        body.add(left(new JSeparator()));
        body.add(Box.createVerticalStrut(12));
        body.add(heading("Card Back"));
        body.add(Box.createVerticalStrut(8));
        body.add(left(backs));

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            result = new Result();
            result.rule = levels.getRule();
            result.back = backs.getChosen();
            result.customPath = backs.getCustomPath();
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));
        buttons.add(cancel);
        buttons.add(ok);

        getContentPane().add(body, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static JComponent heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 2));
        return left(label);
    }

    private static JComponent note(String text) {
        JLabel label = new JLabel(text);
        label.setEnabled(false);
        return left(label);
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private Result result;
}
