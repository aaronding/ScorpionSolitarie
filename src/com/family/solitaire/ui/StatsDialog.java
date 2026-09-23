package com.family.solitaire.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.family.solitaire.model.Rule;

/**
 * Games played and won at each level.
 *
 * @author Aaron Ding
 */
final class StatsDialog {

    private StatsDialog() { }

    static void show(Frame owner) {
        JDialog dialog = new JDialog(owner, "Statistics", true);
        String[] columns = {"Level", "Played", "Won", "Win rate", "Streak", "Best streak"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        fill(model);
        JTable table = new JTable(model);
        table.setRowHeight(table.getRowHeight() + 6);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(520, table.getRowHeight() * 3 + 34));
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dialog, "Clear all statistics?", "Statistics",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                Settings.resetStats();
                fill(model);
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        buttons.add(reset);
        buttons.add(close);

        dialog.getContentPane().add(scroll, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void fill(DefaultTableModel model) {
        model.setRowCount(0);
        for (Rule rule : Rule.values()) {
            Settings.Stats s = Settings.getStats(rule);
            String rate = s.played == 0 ? "–" : Math.round(100.0 * s.won / s.played) + "%";
            model.addRow(new Object[] {MainFrame.name(rule), s.played, s.won, rate, s.streak, s.bestStreak});
        }
    }
}
