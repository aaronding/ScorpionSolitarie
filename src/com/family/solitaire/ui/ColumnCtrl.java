/*
 * ColumnCtrl.java
 *
 * Created on November 30, 2006, 1:39 PM
 *
 */

package com.family.solitaire.ui;

import static com.family.solitaire.model.CardConstants.NCARDS;
import static com.family.solitaire.ui.UIConstants.BOTTOMMARGIN;
import static com.family.solitaire.ui.UIConstants.CARDHEIGHT;
import static com.family.solitaire.ui.UIConstants.CARDWIDTH;
import static com.family.solitaire.ui.UIConstants.OFFCARDSPACE;
import static com.family.solitaire.ui.UIConstants.ONCARDSPACE;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;

import com.family.solitaire.model.Card;
import com.family.solitaire.model.Column;
import com.family.solitaire.model.ColumnUI;

/**
 *
 * @author Aaron Ding
 */
public class ColumnCtrl extends JComponent implements ColumnUI {
    
    public ColumnCtrl(Column column) {
        super();
        
        this.column = column;
        
        setSize(CARDWIDTH, CARDHEIGHT);
        
        cardSpace = ONCARDSPACE;
        viewRow = -1;
        highlightRow = -2;  // highlightRow == -1 means highlight an empty row
        selectedRow = -1;
        
        column.registerUI(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (highlightRow >-2) {
            if (column.isEmpty()) {
                drawHighlightEmptyColumn(g);
            } else {
                drawHighlightColumn(g);
            }
        } else {
            if (column.isEmpty()) {
                drawEmptyColumn(g);
            } else {
                drawNormalColumn(g);
            }
        }
    }
    
    public void repaintUI() {
        calHeight();
        resetCardsSpace();
        repaint();
    }
    
    private void drawHighlightEmptyColumn(Graphics g) {
        g.setColor(Color.RED);
        g.drawRoundRect(0, 0, CARDWIDTH-1, CARDHEIGHT-1, 5, 5);
        g.drawRoundRect(1, 1, CARDWIDTH-3, CARDHEIGHT-3, 3, 3);
    }
    
    private void drawEmptyColumn(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawRoundRect(0, 0, CARDWIDTH-1, CARDHEIGHT-1, 5, 5);
        g.drawRoundRect(1, 1, CARDWIDTH-3, CARDHEIGHT-3, 3, 3);
    }
    
    private void drawNormalColumn(Graphics g) {
        
        if (selectedRow == 0) {
            drawEmptyColumn(g);
            return;
        }
        
        int y=0;
        int size = column.getSize();
        if (selectedRow>-1)
            size = selectedRow;
        for (int i=0; i<size; i++) {
            Card card = column.getCard(i);
            
            Image img = ImageStore.instance().getCardImage(card);
            if (i== size-1) {
                g.drawImage(img, 0, y, this);
            } else {
                // I need 2 pixels more because the cards have a arc corner
                g.drawImage(img, 0, y, CARDWIDTH, y+cardSpace+2, 
                        0, 0, CARDWIDTH, cardSpace+2, this);
            }
            if (card.isFacedUp())
                y += cardSpace;
            else
                y += OFFCARDSPACE;
        }
        if (viewRow > -1 && column.getCard(viewRow).isFacedUp()) {
            drawViewCard(g);
        }
    }
    
    private void drawViewCard(Graphics g) {
        Image img = ImageStore.instance().getFace(column.getCard(viewRow));
        g.drawImage(img, 0, calRowY(viewRow), this);
    }
    
    // highlightRow >=0
    private void drawHighlightColumn(Graphics g) {
        int y=0;
        for (int i=0; i<highlightRow; i++) {
            Card card = column.getCard(i);
            
            Image img = ImageStore.instance().getCardImage(card);

            // I need 2 pixels more because the cards have a arc corner
            g.drawImage(img, 0, y, CARDWIDTH, y+cardSpace+2, 0, 
                0, CARDWIDTH, cardSpace+2, this);

            if (card.isFacedUp())
                y += cardSpace;
            else
                y += OFFCARDSPACE;
        }

        int size = column.getSize();
        for (int i=highlightRow; i<size; i++) {
            Card card = column.getCard(i);
            
            Image img = ImageStore.instance().getInvertedFace(card);
            if (i== size-1) {
                g.drawImage(img, 0, y, this);
            } else {
                // I need 2 pixels more because the cards have a arc corner
                g.drawImage(img, 0, y, CARDWIDTH, y+cardSpace+2, 
                        0, 0, CARDWIDTH, cardSpace+2, this);
            }
            
            if (card.isFacedUp())
                y += cardSpace;
            else
                y += OFFCARDSPACE;
        }
    }
    
    public void highlight(int row) {
        highlightRow = row;
        repaint();
    }
    
    public void highlightOff() {
        highlightRow = -2;
        repaint();
    }
    
    public int getColumn() { return column.getIndex(); }
    
    public int getRow(int y) {
        y-=getBounds().y;  // get relative Y
        int t=0;
        for (int i=0; i<NCARDS; i++) {
            Card card = column.getCard(i);
            if (card == null)
                break;
            
            if (card.isFacedUp()) {
                if (column.getCard(i+1) == null)
                    t += CARDHEIGHT;
                else
                    t += cardSpace;
            } else {
                t += OFFCARDSPACE;
            }
            
            if (t > y)
                return i;
        }
        return -1;
    }
    
    public Point getCardPosition(int row) {
        Rectangle rv = getBounds();
        return new Point(rv.x, calRowY(row) + rv.y);
    }
    
    public void viewCard(int row) {
        viewRow = row;
        repaint();
    }
    
    public void viewCardOff() {
        if (viewRow != -1) {
            viewRow = -1;
            repaint();
        }
    }
    
    public void resetCardsSpace() {
        int oldSpace = cardSpace;
        cardSpace = calCardsSpace();
        calHeight();  // this function depends on cardSpace
        if (cardSpace != oldSpace) {
            repaint();
        }
    }
    
    private int calCardsSpace() {
        int all = column.getSize();
        if (all == 0) 
            return ONCARDSPACE;
        
        int off = column.getFacedOffCardsCount();

        if (all == off+1)
            return ONCARDSPACE;

        int cardSpace = (getParent().getHeight() - getBounds().y - BOTTOMMARGIN  
                - off * OFFCARDSPACE - CARDHEIGHT) / (all - off - 1);
        if (cardSpace > ONCARDSPACE)
            return ONCARDSPACE;
        else
            return cardSpace;
    }
    
    public void selectRow(int row) {
        selectedRow = row;
        repaint();
    }
    
    public void resetSelectedRow() {
        if (selectedRow != -1) {
            selectedRow = -1;
            repaint();
        }
    }
    
    private int calRowY(int row) {
        int t = 0;
        for (int i=0; i<row; i++) {
            Card card = column.getCard(i);
            
            if (card.isFacedUp()) {
                t += cardSpace;
            } else {
                t += OFFCARDSPACE;
            }
        }
        return t;
    }
    
    public Rectangle getLastCardBounds() {
        int last = column.getSize();
        last--;
        int y = calRowY(last);
        Rectangle rv = getBounds();
        rv.y += y;
        rv.height = CARDHEIGHT;
        return rv;
    }

    public Rectangle getLastCardBounds(Rectangle rv) {
        int last = column.getSize();
        last--;
        int y = calRowY(last);
        getBounds(rv);
        rv.y += y;
        rv.height = CARDHEIGHT;
        return rv;
    }
    
    public int getCardSpace() { return cardSpace; }
    
    public int getCardssize() {
        return column.getSize();
    }
    
    private void calHeight() {
        int y=0;

        for (int i=0; i<column.getSize()-1; i++) {
            Card card = column.getCard(i);
            if (card.isFacedDown()) {
                y += OFFCARDSPACE;
            } else {
                y += cardSpace;
            }
        }
        
        y += CARDHEIGHT;
        setSize(getWidth(), y);
    }
    
    private Column column;
    
    private int cardSpace;
    private int viewRow;
    private int highlightRow;
    private int selectedRow;
}
