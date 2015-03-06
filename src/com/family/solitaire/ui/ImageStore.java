/*
 * ImageStore.java
 *
 * Created on November 30, 2006, 2:43 PM
 *
 */

package com.family.solitaire.ui;

import static com.family.solitaire.model.CardConstants.NCARDS;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorModel;
import java.awt.image.LookupOp;
import java.awt.image.PixelGrabber;

import javax.swing.ImageIcon;

import com.family.solitaire.model.Card;

/**
 *
 * @author Aaron Ding
 */
public class ImageStore {
    
    private ImageStore() {
        faceImages = new Image[NCARDS];
        faceInvertImages = new Image[NCARDS];
        rearImages = new Image[10];
        rearIndex = (int)(Math.random()*10);
        
        try {  init();  } catch (Exception e) { e.printStackTrace(); }
    }
    
    private static ImageStore instance = new ImageStore();
    
    public static ImageStore instance() { return instance; }
    
    private void init() throws java.io.IOException {
        
        Toolkit tk = Toolkit.getDefaultToolkit();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (int i=0; i<NCARDS; i++) {
            faceImages[i] = tk.createImage(loader.getResource("res/images/"+i+".gif"));
            faceInvertImages[i] = invert(faceImages[i]);
        }
        
        for (int i=0; i<10; i++) {
            rearImages[i] = tk.createImage(loader.getResource("res/images/back" + i + ".gif"));
            new ImageIcon(rearImages[i]).getImage();
        }
        
        icon = tk.createImage(loader.getResource("res/images/icon.png"));
    }
    
    private Image invert(Image src) {
        BufferedImage biSrc = toBufferedImage(src);

        BufferedImage biDst = new BufferedImage(biSrc.getWidth(),
                biSrc.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE); 

        byte invert[] = new byte[256];
        for (int j = 0; j < 256 ; j++) {
            invert[j] = (byte) (255-j);
        }
        byte[][] ftable = { invert };
        LookupOp lo = new LookupOp(new ByteLookupTable(0,ftable), null);
        lo.filter(biSrc, biDst);

        return Toolkit.getDefaultToolkit().createImage(biDst.getSource());
    }
    
    private BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
    
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        
        int type = hasAlpha(image) ? BufferedImage.TYPE_INT_ARGB : 
            BufferedImage.TYPE_INT_RGB;
        // Create a buffered image using the default color model
        bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), 
                    type);
    
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
    
        return bimage;
    }
    
    private boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
    
        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }
    
        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    public Image getCardImage(Card card) {
        if (card.isFacedUp())
            return faceImages[card.value()];
        else
            return getCurRear();
    }
    
    public void setRearIndex(int index) {
        rearIndex = index;
    }
    
    public int getRearIndex() {
        return rearIndex;
    }
    
    public Image getFace(Card card) { return faceImages[card.value()];  }
    public Image getFace(int value) { return faceImages[value];  }
    public Image getInvertedFace(Card card) { return faceInvertImages[card.value()];  }
    public Image getInvertedFace(int value) { return faceInvertImages[value];  }
    public Image getCurRear() {  return rearImages[rearIndex];  }
    
    public Image[] getRearImages() { return rearImages; }
    
    public Image getIcon() { return icon; }

    private Image[] faceImages;
    private Image[] faceInvertImages;
    private Image[] rearImages;
    private Image icon;
    
    private int rearIndex;
}
