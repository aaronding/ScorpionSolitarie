package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.family.solitaire.model.Rule;

/**
 * Asks which difficulty level to play, the first time the game runs.
 *
 * @author Aaron Ding
 */
final class LevelDialog extends JDialog {

    /** Returns the chosen level, or null if cancelled. */
    static Rule choose(Frame owner, Rule current) {
        LevelDialog dialog = new LevelDialog(owner, current);
        dialog.setVisible(true);
        return dialog.chosen;
    }

    private LevelDialog(Frame owner, Rule current) {
        super(owner, "Difficulty", true);
        LevelPicker picker = new LevelPicker(current);
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        body.add(new JLabel("Choose how hard the game should be. You can change it later in Settings."),
                 BorderLayout.NORTH);
        body.add(picker, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            chosen = picker.getRule();
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

    private Rule chosen;
}
