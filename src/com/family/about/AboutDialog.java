/*
 * AboutDialog.java
 *
 * Created on December 11, 2006, 11:34 AM
 */

package com.family.about;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Name, version, author, license and credits.
 *
 * @author ading
 */
public class AboutDialog extends JDialog {

    private static final String SOURCE_URL = "https://github.com/aaronding/ScorpionSolitarie";

    public AboutDialog(Frame parent, Image icon, String appName, String appVer, String author, String email) {
        super(parent, "About " + appName, true);

        JLabel iconLabel = new JLabel(new ImageIcon(icon.getScaledInstance(96, 96, Image.SCALE_SMOOTH)));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);
        JLabel name = new JLabel(appName);
        name.setFont(name.getFont().deriveFont(Font.BOLD, name.getFont().getSize2D() + 9));
        name.setAlignmentX(CENTER_ALIGNMENT);
        JLabel version = new JLabel("Version " + appVer);
        version.setAlignmentX(CENTER_ALIGNMENT);
        JLabel by = new JLabel("By " + author);
        by.setAlignmentX(CENTER_ALIGNMENT);
        JPanel bugs = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bugs.setOpaque(false);
        bugs.add(new JLabel("Report bugs to "));
        bugs.add(mailLink(email));
        bugs.setAlignmentX(CENTER_ALIGNMENT);
        bugs.setMaximumSize(bugs.getPreferredSize());

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));
        top.add(iconLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(name);
        top.add(Box.createVerticalStrut(2));
        top.add(version);
        top.add(Box.createVerticalStrut(10));
        top.add(by);
        top.add(Box.createVerticalStrut(2));
        top.add(bugs);

        JTextArea text = new JTextArea(
            "This program is free software; you can redistribute it and/or modify it under the terms of "
            + "the GNU General Public License as published by the Free Software Foundation; either version 2 "
            + "of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; "
            + "without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. "
            + "See the GNU General Public License for more details.\n\n"
            + "Card art: SVG Playing Cards by Tek Eye (tekeye.uk), public domain.\n"
            + "Uses FlatLaf (Apache License 2.0) and JSVG (MIT License).");
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setRows(9);
        text.setColumns(46);
        text.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 18, 0, 18), scroll.getBorder()));

        JButton license = new JButton("Full License");
        license.addActionListener(e -> showLicense());
        JButton source = new JButton("Source Code on GitHub");
        source.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(SOURCE_URL));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "The source code is at\n" + SOURCE_URL);
            }
        });
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        buttons.add(license);
        buttons.add(source);
        buttons.add(ok);

        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /** The address as a link that starts an email; copies it if there is no mail app. */
    private JLabel mailLink(String email) {
        JLabel link = new JLabel("<html><a href=''>" + email + "</a></html>");
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText("Write an email");
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().mail(new URI("mailto:" + email + "?subject=Scorpion%20Solitaire"));
                } catch (Exception ex) {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                           .setContents(new StringSelection(email), null);
                    JOptionPane.showMessageDialog(AboutDialog.this,
                        "No email app is set up, so the address has been copied:\n" + email);
                }
            }
        });
        return link;
    }

    private void showLicense() {
        String license;
        try (InputStream in = AboutDialog.class.getResourceAsStream("license.txt")) {
            license = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            license = "See https://www.gnu.org/licenses/old-licenses/gpl-2.0.html";
        }
        JTextArea area = new JTextArea(license, 30, 76);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, area.getFont().getSize()));
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "GNU General Public License",
                                      JOptionPane.PLAIN_MESSAGE);
    }
}
