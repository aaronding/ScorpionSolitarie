package com.family.solitaire.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The card backs to choose from: the built-in designs, or any picture.
 *
 * @author Aaron Ding
 */
final class CardBackPicker extends JPanel {

    private static final int W = 80;
    private static final int H = (int)Math.round(W * CardArt.ASPECT);

    CardBackPicker(CardArt art) {
        super(new BorderLayout(12, 0));
        setOpaque(false);
        chosen = art.getBackName();
        customPath = Settings.getCustomBackImage();

        JPanel grid = new JPanel(new GridLayout(2, 0, 8, 8));
        grid.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (String name : CardArt.BACKS) {
            JToggleButton b = new Tile(icon(art.preview(name, W * 2, H * 2)));
            b.setSelected(name.equals(chosen));
            b.addActionListener(e -> chosen = name);
            group.add(b);
            grid.add(b);
        }

        customTile = new Tile(null);
        customTile.setText(customPath == null ? "Your picture" : null);
        if (customPath != null)
            showCustom(customPath);
        customTile.setSelected(CardArt.CUSTOM.equals(chosen));
        customTile.addActionListener(e -> {
            if (customPath == null)
                pickPicture();
            else
                chosen = CardArt.CUSTOM;
        });
        group.add(customTile);

        JButton pick = new JButton("Choose Picture…");
        pick.addActionListener(e -> pickPicture());
        JPanel custom = new JPanel(new BorderLayout(0, 6));
        custom.setOpaque(false);
        custom.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        custom.add(customTile, BorderLayout.NORTH);
        custom.add(pick, BorderLayout.SOUTH);

        add(grid, BorderLayout.CENTER);
        add(custom, BorderLayout.EAST);
    }

    /** A built-in back's name, or {@link CardArt#CUSTOM}. */
    String getChosen() {
        return chosen;
    }

    String getCustomPath() {
        return customPath;
    }

    private void pickPicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Pictures", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();
        try {
            if (ImageIO.read(file) == null)
                throw new IllegalArgumentException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "That file isn't a picture this game can read.");
            return;
        }
        customPath = file.getAbsolutePath();
        showCustom(customPath);
        customTile.setSelected(true);
        chosen = CardArt.CUSTOM;
    }

    private void showCustom(String path) {
        CardArt preview = new CardArt(() -> { });
        preview.setBack(CardArt.CUSTOM, path);
        if (CardArt.CUSTOM.equals(preview.getBackName())) {
            customTile.setIcon(icon(preview.preview(CardArt.CUSTOM, W * 2, H * 2)));
            customTile.setText(null);
        }
    }

    /** A card back to pick: ringed in the accent color when chosen, outlined on hover. */
    private static final class Tile extends JToggleButton {
        Tile(ImageIcon icon) {
            super(icon);
            setPreferredSize(new Dimension(W + 16, H + 16));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            CardArt.quality(g2);
            RoundRectangle2D ring = new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, 14, 14);
            if (isSelected()) {
                Color accent = UIManager.getColor("Component.accentColor");
                if (accent == null)
                    accent = new Color(0x2675BF);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g2.fill(ring);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(3.5f));
                g2.draw(ring);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(0, 0, 0, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(ring);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** An icon drawn from a double-size image, so it is sharp on high-DPI screens. */
    private static ImageIcon icon(BufferedImage big) {
        Image small = big.getScaledInstance(W, H, Image.SCALE_SMOOTH);
        BufferedImage b = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        b.getGraphics().drawImage(small, 0, 0, null);
        return new ImageIcon(new BaseMultiResolutionImage(b, big));
    }

    private final JToggleButton customTile;
    private String customPath;
    private String chosen;
}
