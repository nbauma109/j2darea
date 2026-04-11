package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

public class SearchMapDataTest {

    @Test
    public void classifyTextureUsesDominantColorRules() {
        assertEquals(SearchMapTileType.GRASS, SearchMapTileType.classifyTexture(createSolidImage(new Color(40, 140, 40))));
        assertEquals(SearchMapTileType.STONE, SearchMapTileType.classifyTexture(createSolidImage(new Color(120, 124, 122))));
        assertEquals(SearchMapTileType.WOOD, SearchMapTileType.classifyTexture(createSolidImage(new Color(130, 100, 60))));
    }

    @Test
    public void polygonCommitMarksCoveredTilesAsImpeded() {
        SearchMapData searchMapData = new SearchMapData(128, 128);
        searchMapData.setAll(SearchMapTileType.GRASS);
        searchMapData.applyPolygonImpeded(new Polygon(
            new int[] { 4, 12, 12, 4 },
            new int[] { 4, 4, 10, 10 },
            4
        ));

        assertTrue(searchMapData.isImpeded(0, 0));
        assertFalse(searchMapData.isImpeded(1, 0));
        assertFalse(searchMapData.isImpeded(0, 1));
    }

    @Test
    public void manualOverrideCanBeResetToBaseTerrain() {
        SearchMapData searchMapData = new SearchMapData(128, 128);
        searchMapData.setAll(SearchMapTileType.GRASS);

        searchMapData.setOverrideTileType(0, 0, SearchMapTileType.STONE);

        assertEquals(SearchMapTileType.STONE, searchMapData.getResolvedTileType(0, 0));

        searchMapData.resetOverrideTileType(0, 0);

        assertEquals(SearchMapTileType.GRASS, searchMapData.getResolvedTileType(0, 0));
    }

    @Test
    public void connectedImpededRegionSelectionAndDeleteRestoreBaseTerrain() {
        SearchMapData searchMapData = new SearchMapData(128, 128);
        searchMapData.setAll(SearchMapTileType.GRASS);
        searchMapData.setOverrideTileType(0, 0, SearchMapTileType.NON_WALKABLE);
        searchMapData.setOverrideTileType(1, 0, SearchMapTileType.NON_WALKABLE);
        searchMapData.setOverrideTileType(1, 1, SearchMapTileType.NON_WALKABLE);
        searchMapData.setOverrideTileType(3, 3, SearchMapTileType.NON_WALKABLE);

        List<Point> region = searchMapData.findConnectedImpededRegion(1, 1);

        assertEquals(3, region.size());
        assertTrue(region.contains(new Point(0, 0)));
        assertTrue(region.contains(new Point(1, 0)));
        assertTrue(region.contains(new Point(1, 1)));
        assertFalse(region.contains(new Point(3, 3)));
        assertEquals(SearchMapTileType.NON_WALKABLE, searchMapData.getResolvedTileType(1, 1));

        searchMapData.clearResolvedOverrides(region);

        assertEquals(SearchMapTileType.GRASS, searchMapData.getResolvedTileType(0, 0));
        assertEquals(SearchMapTileType.GRASS, searchMapData.getResolvedTileType(1, 0));
        assertEquals(SearchMapTileType.GRASS, searchMapData.getResolvedTileType(1, 1));
        assertEquals(SearchMapTileType.NON_WALKABLE, searchMapData.getResolvedTileType(3, 3));
    }

    @Test
    public void exportImageUsesReducedSearchMapDimensions() {
        SearchMapData searchMapData = new SearchMapData(128, 96);
        BufferedImage image = searchMapData.toImage(128, 96);

        assertEquals(8, image.getWidth());
        assertEquals(8, image.getHeight());
    }

    private BufferedImage createSolidImage(Color color) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }
}
