/*
 * TmpColumnCtrl.java
 *
 * Created on December 1, 2006, 11:41 AM
 *
 */

package com.family.solitaire.ui;

import static com.family.solitaire.ui.UIConstants.CARDHEIGHT;
import static com.family.solitaire.ui.UIConstants.CARDWIDTH;
import static com.family.solitaire.ui.UIConstants.ONCARDSPACE;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

import com.family.solitaire.model.Card;

/**
 *
 * @author Aaron Ding
 */
public class TmpColumnCtrl extends JComponent {
    
    public TmpColumnCtrl() {
        super();
        setSize(CARDWIDTH, 0);
    }
    
    @Override
    public void paintComponent(Graphics g) {

        int y=0;
        for (int i=0; i<tmpColumn.length; i++) {
            Card card = tmpColumn[i];
            
            Image img = ImageStore.instance().getFace(card);
            if (i== tmpColumn.length-1) {
                g.drawImage(img, 0, y, getParent());
            } else {
                g.drawImage(img, 0, y, CARDWIDTH, y+cardSpace+2, 
                        0, 0, CARDWIDTH, cardSpace+2, this);
            }
            y += cardSpace;
        }
    }
    
    public void setCards(Card[] cards) {
        tmpColumn = cards;
    }
    
    public void calHeight() {
        int y=0;
        for (int i=0; i<tmpColumn.length-1; i++) {
            y += cardSpace;
        }
        
        y += CARDHEIGHT;
        setSize(getWidth(), y);
    }
    
    public void setCardSpace(int cardSpace) { this.cardSpace = cardSpace; }
    
    private Card[] tmpColumn;
    private int cardSpace = ONCARDSPACE;
}
