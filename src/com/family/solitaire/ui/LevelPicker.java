package com.family.solitaire.ui;

import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.family.solitaire.model.Rule;

/**
 * The three difficulty levels as radio buttons, each with its rules.
 *
 * @author Aaron Ding
 */
final class LevelPicker extends JPanel {

    private static final String[][] TEXT = {
        {"Easy", "Build down in any suit. Undo anything."},
        {"Medium", "Build down in the same color. Undo until the reserve is dealt."},
        {"Difficult", "Build down in the same suit. No undo."},
    };

    LevelPicker(Rule current) {
        super(new GridLayout(0, 1, 0, 8));
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Rule rule : Rule.values()) {
            String[] text = TEXT[rule.ordinal()];
            JRadioButton radio = new JRadioButton("<html><b>" + text[0] + "</b><br>" + text[1] + "</html>");
            radio.setFont(radio.getFont().deriveFont(Font.PLAIN));
            radio.setSelected(rule == current);
            group.add(radio);
            add(radio);
            radios[rule.ordinal()] = radio;
        }
    }

    Rule getRule() {
        for (Rule rule : Rule.values())
            if (radios[rule.ordinal()].isSelected())
                return rule;
        return null;
    }

    private final JRadioButton[] radios = new JRadioButton[Rule.values().length];
}
