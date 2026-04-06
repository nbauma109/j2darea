package com.github.nbauma109.j2darea;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Loads an Infinity Engine area preview directly from game resources.
 */
public final class GameAreaImageLoader {

    private GameAreaImageLoader() {
    }

    public static boolean canLoadArea(String gameInstallPath, String areaResref) {
        try {
            validateInputs(gameInstallPath, areaResref);
            LocalGameResourceResolver resolver = new LocalGameResourceResolver(Paths.get(gameInstallPath.trim()));
            LocalWedOverlay overlay = LocalWedOverlay.read(resolver.loadResource(areaResref, "WED", LocalGameResourceResolver.RESOURCE_TYPE_WED));
            LocalTisResource tis = LocalTisResource.read(overlay.getTisResref(),
                resolver.loadResource(overlay.getTisResref(), "TIS", LocalGameResourceResolver.RESOURCE_TYPE_TIS), resolver);
            tis.getTile(overlay.getPrimaryTileIndex(0));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static BufferedImage loadAreaImage(String gameInstallPath, String areaResref) throws IOException {
        validateInputs(gameInstallPath, areaResref);

        LocalGameResourceResolver resolver = new LocalGameResourceResolver(Paths.get(gameInstallPath.trim()));
        LocalWedOverlay overlay = LocalWedOverlay.read(resolver.loadResource(areaResref, "WED", LocalGameResourceResolver.RESOURCE_TYPE_WED));
        LocalTisResource tis = LocalTisResource.read(overlay.getTisResref(),
            resolver.loadResource(overlay.getTisResref(), "TIS", LocalGameResourceResolver.RESOURCE_TYPE_TIS), resolver);

        BufferedImage image = new BufferedImage(overlay.getWidthInTiles() * 64, overlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            int tileCount = overlay.getWidthInTiles() * overlay.getHeightInTiles();
            for (int tileCellIndex = 0; tileCellIndex < tileCount; tileCellIndex++) {
                BufferedImage tile = tis.getTile(overlay.getPrimaryTileIndex(tileCellIndex));
                int tileX = (tileCellIndex % overlay.getWidthInTiles()) * 64;
                int tileY = (tileCellIndex / overlay.getWidthInTiles()) * 64;
                graphics.drawImage(tile, tileX, tileY, null);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void validateInputs(String gameInstallPath, String areaResref) throws IOException {
        if (gameInstallPath == null || gameInstallPath.trim().isEmpty()) {
            throw new IOException("Configure the game install path in preferences first.");
        }
        if (areaResref == null || areaResref.trim().isEmpty()) {
            throw new IOException("Destination area is empty.");
        }
    }
}
