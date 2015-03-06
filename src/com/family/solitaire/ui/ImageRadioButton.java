/*
 * ImageRadioButton.java
 *
 * Created on December 10, 2006, 8:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.family.solitaire.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JRadioButton;

/**
 *
 * @author Aaron
 */
public class ImageRadioButton extends JRadioButton {
    
    /** Creates a new instance of ImageRadioButton */
    public ImageRadioButton(Image image) {
        this.image = image;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        
        if (this.isSelected()) {
            g.drawImage(image, 0, 0, this);
            g.setColor(Color.YELLOW);
            g.drawRect(0, 0, 70, 95);
            g.drawRect(1, 1, 68, 93);
            g.drawRect(2, 2, 66, 91);
        } else {
            g.drawImage(image, 0, 0, this);
        }
    }
    
    private Image image;
}
