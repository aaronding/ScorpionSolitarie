/*
 * ReserveCtrl.java
 *
 * Created on November 30, 2006, 4:39 PM
 *
 */

package com.family.solitaire.ui;

import static com.family.solitaire.ui.UIConstants.CARDHEIGHT;
import static com.family.solitaire.ui.UIConstants.CARDWIDTH;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

import com.family.solitaire.model.ColumnUI;
import com.family.solitaire.model.Reserve;

/**
 *
 * @author Aaron Ding
 */
public class ReserveCtrl extends JComponent implements ColumnUI {
    
    public ReserveCtrl(Reserve reserve) {
        super();
        
        this.reserve = reserve;
        
        this.reserve.register(this);
        setSize(CARDWIDTH+4, CARDHEIGHT+4);
    }
    
    public void repaintUI() {
        repaint();
    }
    
    @Override
    public void paintComponent(Graphics g) {

        if (!reserve.used()) {
            int x=0, y=0;
            for (int i=0; i<3; i++) {
                Image img = ImageStore.instance().getCurRear();
                g.drawImage(img, x, y, this);
                x+=2;
                y+=2;
            }
        } else {
            g.setColor(Color.BLACK);
            g.drawRoundRect(0, 0, CARDWIDTH-1, CARDHEIGHT-1, 5, 5);
            g.drawRoundRect(1, 1, CARDWIDTH-3, CARDHEIGHT-3, 3, 3);
            
            g.setColor(Color.GREEN);
            g.fillOval(7, 18, 57, 61);
            g.setColor(new Color(0, 128, 26));
            g.fillOval(14, 25, 43, 47);
        }
    }
    
    private Reserve reserve;
}
