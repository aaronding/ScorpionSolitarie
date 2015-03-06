/*
 * UIConstants.java
 *
 * Created on November 30, 2006, 12:52 PM
 *
 */

package com.family.solitaire.ui;

import static com.family.solitaire.model.CardConstants.RESERVE_SIZE;

/**
 *
 * @author Aaron Ding
 */
public class UIConstants {
    
    public UIConstants() {
    }
    
    public static final int CARDWIDTH = 71;	// the width of card
    public static final int CARDHEIGHT = 96;	// the height of card

    public static final int ONCARDSPACE = 28;
    public static final int OFFCARDSPACE = 7;
    
    public static final int TOPMARGIN = 10;
    public static final int BOTTOMMARGIN = 10;
    public static final int LEFTMARGIN = 10;
    public static final int RIGHTMARGIN = 10;
    public static final int FIRSTCARDX = LEFTMARGIN + CARDWIDTH + (RESERVE_SIZE-1)*2;
}
