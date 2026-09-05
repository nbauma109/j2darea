package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Graphics2D;

final class TransparencyPreviewPainter {

    private static final Color LIGHT_TILE = new Color(220, 220, 220);
    private static final Color DARK_TILE = new Color(190, 190, 190);
    private static final int TILE_SIZE = 12;

    private TransparencyPreviewPainter() {
    }

    static void paintCheckerboard(Graphics2D graphics, int x, int y, int width, int height) {
        for (int row = 0; row < height; row += TILE_SIZE) {
            for (int column = 0; column < width; column += TILE_SIZE) {
                graphics.setColor((((row / TILE_SIZE) + (column / TILE_SIZE)) % 2 == 0) ? LIGHT_TILE : DARK_TILE);
                graphics.fillRect(x + column, y + row, Math.min(TILE_SIZE, width - column), Math.min(TILE_SIZE, height - row));
            }
        }
    }
}
