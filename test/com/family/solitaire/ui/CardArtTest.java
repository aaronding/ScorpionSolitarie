package com.family.solitaire.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.family.solitaire.model.Card;

/** Every face and back must actually draw: a card the SVG renderer can't handle comes out blank. */
class CardArtTest {

    @Test
    void everyCardDraws() {
        CardArt art = new CardArt(() -> { });
        art.load();
        int w = 60, h = (int)Math.round(w * CardArt.ASPECT);
        for (int v = 0; v < 52; v++)
            assertTrue(coverage(art.face(Card.valueOf(v), w, h)) > 0.9, Card.valueOf(v).toString());
        for (String back : CardArt.BACKS)
            assertTrue(coverage(art.preview(back, w, h)) > 0.9, back);
    }

    private static double coverage(BufferedImage img) {
        int painted = 0;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if ((img.getRGB(x, y) >>> 24) > 0)
                    painted++;
        return painted / (double)(img.getWidth() * img.getHeight());
    }
}
