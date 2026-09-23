/*
 * MainApp.java
 *
 * Created on November 29, 2006, 4:07 PM
 */

package com.family.solitaire;

import java.awt.Image;
import java.awt.Taskbar;

import javax.imageio.ImageIO;

import com.family.solitaire.ui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

/**
 * @author Aaron Ding
 */
public class MainApp {

    public static void main(String args[]) {
        // macOS shows "java" in the Dock and menu bar unless told otherwise
        System.setProperty("apple.awt.application.name", MainFrame.APP_NAME);
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // FlatLaf draws every control as vector shapes: crisp at any scale
        if (System.getProperty("os.name", "").toLowerCase().contains("mac"))
            FlatMacLightLaf.setup();
        else
            FlatLightLaf.setup();

        java.awt.EventQueue.invokeLater(() -> {
            Image icon = loadIcon();
            MainFrame frame = new MainFrame(icon);
            frame.setVisible(true);
            setDockIcon(icon);
            frame.start();
        });
    }

    private static Image loadIcon() {
        try {
            return ImageIO.read(MainApp.class.getClassLoader().getResource("res/images/icon.png"));
        } catch (Exception e) {
            return null;
        }
    }

    // The window icon doesn't reach the macOS Dock; the Taskbar API does.
    private static void setDockIcon(Image icon) {
        try {
            if (Taskbar.isTaskbarSupported()
                    && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().setIconImage(icon);
            }
        } catch (Exception e) {
        }
    }
}
