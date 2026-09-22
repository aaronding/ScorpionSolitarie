/*
 * MainApp.java
 *
 * Created on November 29, 2006, 4:07 PM
 *
 */

package com.family.solitaire;

import com.family.solitaire.ui.ImageStore;
import com.family.solitaire.ui.MainFrame;
import java.awt.Taskbar;
import javax.swing.UIManager;

/**
 *
 * @author Aaron Ding
 */
public class MainApp {

    public MainApp() { }

    public static void main(String args[]) {
        // macOS shows "java" in the Dock and menu bar unless told otherwise
        System.setProperty("apple.awt.application.name", "Scorpion Solitaire");
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {

        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
                setDockIcon();
            }
        });
    }

    // The window icon doesn't reach the macOS Dock; the Taskbar API does.
    private static void setDockIcon() {
        try {
            if (Taskbar.isTaskbarSupported()
                    && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().setIconImage(ImageStore.instance().getIcon());
            }
        } catch (Exception e) {
        }
    }
}
