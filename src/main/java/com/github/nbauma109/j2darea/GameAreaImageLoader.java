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
            LocalWedOverlay overlay = LocalWedOverlay.read(
                resolver.loadResource(resolveWedResref(resolver, areaResref), "WED", LocalGameResourceResolver.RESOURCE_TYPE_WED));
            LocalTisResource tis = LocalTisResource.read(overlay.getTisResref(),
                resolver.loadResource(overlay.getTisResref(), "TIS", LocalGameResourceResolver.RESOURCE_TYPE_TIS), resolver);
            tis.getTile(overlay.getPrimaryTileIndex(0));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static BufferedImage loadAreaImage(String gameInstallPath, String areaResref) throws IOException {
        return loadAreaImage(gameInstallPath, areaResref, true);
    }

    public static BufferedImage loadAreaImage(String gameInstallPath, String areaResref, boolean closedDoors) throws IOException {
        validateInputs(gameInstallPath, areaResref);

        LocalGameResourceResolver resolver = new LocalGameResourceResolver(Paths.get(gameInstallPath.trim()));
        String wedResref = resolveWedResref(resolver, areaResref);
        LocalWedOverlay overlay = LocalWedOverlay.read(resolver.loadResource(wedResref, "WED", LocalGameResourceResolver.RESOURCE_TYPE_WED));
        LocalTisResource tis = LocalTisResource.read(overlay.getTisResref(),
            resolver.loadResource(overlay.getTisResref(), "TIS", LocalGameResourceResolver.RESOURCE_TYPE_TIS), resolver);

        BufferedImage image = new BufferedImage(overlay.getWidthInTiles() * 64, overlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            int tileCount = overlay.getWidthInTiles() * overlay.getHeightInTiles();
            for (int tileCellIndex = 0; tileCellIndex < tileCount; tileCellIndex++) {
                BufferedImage tile = tis.getTile(overlay.getRenderedTileIndex(tileCellIndex, closedDoors));
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

    private static String resolveWedResref(LocalGameResourceResolver resolver, String areaResref) throws IOException {
        String normalizedAreaResref = LocalIeIO.normalizeResref(areaResref);
        byte[] areBytes = resolver.loadResource(normalizedAreaResref, "ARE", LocalGameResourceResolver.RESOURCE_TYPE_ARE);
        if (areBytes.length >= 0x10 && LocalIeIO.readAscii(areBytes, 0, 4).startsWith("AREA")) {
            String wedResref = LocalIeIO.readResref(areBytes, 8, 8);
            if (!wedResref.isEmpty()) {
                return wedResref;
            }
        }
        return normalizedAreaResref;
    }
}
