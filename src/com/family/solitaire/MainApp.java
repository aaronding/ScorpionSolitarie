/*
 * MainApp.java
 *
 * Created on November 29, 2006, 4:07 PM
 *
 */

package com.family.solitaire;

import com.family.solitaire.ui.MainFrame;
import javax.swing.UIManager;

/**
 *
 * @author Aaron Ding
 */
public class MainApp {
    
    public MainApp() { }
    
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}
