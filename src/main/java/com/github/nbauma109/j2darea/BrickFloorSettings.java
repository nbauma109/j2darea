package com.github.nbauma109.j2darea;

import java.util.Random;

/** Parameters shared by the separate brick and floor-tile generators. */
public class BrickFloorSettings {

    public static final int MIN_BRICK_LENGTH = 10;
    public static final int MAX_BRICK_LENGTH = 160;
    public static final int MIN_BRICK_HEIGHT = 4;
    public static final int MAX_BRICK_HEIGHT = 80;
    public static final int MIN_TILE_SIZE = 6;
    public static final int MAX_TILE_SIZE = 80;

    private long seed;
    private MasonryMaterial material;
    private BrickApplication application;
    private boolean alongFirstEdge;
    private BrickBond bond;
    private BrickPalette palette;
    private int tileSize;
    private int brickLength;
    private int brickHeight;
    private double mortarWidth;
    private double mortarDarkness;
    private double relief;
    private double brightness;
    private double toneVariation;
    private double weathering;
    private double irregularity;
    private double lightUnevenness;

    public BrickFloorSettings() {
        this(MasonryMaterial.BRICKS);
    }

    public BrickFloorSettings(MasonryMaterial material) {
        seed = new Random().nextLong();
        this.material = material != null ? material : MasonryMaterial.BRICKS;
        application = BrickApplication.FLOOR;
        alongFirstEdge = true;
        bond = BrickBond.RUNNING;
        palette = BrickPalette.AUTO;
        tileSize = 28;
        brickLength = 16;
        brickHeight = 7;
        mortarWidth = 1.1d;
        mortarDarkness = 0.35d;
        relief = this.material == MasonryMaterial.FLOOR_TILES ? 0.12d : 0.35d;
        brightness = 1d;
        toneVariation = 0.28d;
        weathering = 0.65d;
        irregularity = this.material == MasonryMaterial.FLOOR_TILES ? 0d : 0.2d;
        lightUnevenness = this.material == MasonryMaterial.FLOOR_TILES ? 0d : 0.6d;
    }

    public BrickFloorSettings(BrickFloorSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(BrickFloorSettings source) {
        if (source == null) {
            return;
        }
        seed = source.seed;
        material = source.material;
        application = source.application;
        alongFirstEdge = source.alongFirstEdge;
        bond = source.bond;
        palette = source.palette;
        tileSize = source.tileSize;
        brickLength = source.brickLength;
        brickHeight = source.brickHeight;
        mortarWidth = source.mortarWidth;
        mortarDarkness = source.mortarDarkness;
        relief = source.relief;
        brightness = source.brightness;
        toneVariation = source.toneVariation;
        weathering = source.weathering;
        irregularity = source.irregularity;
        lightUnevenness = source.lightUnevenness;
    }

    public long getSeed() {
        return seed;
    }

    public MasonryMaterial getMaterial() {
        return material;
    }

    public void setMaterial(MasonryMaterial material) {
        this.material = material != null ? material : MasonryMaterial.BRICKS;
        if (this.material == MasonryMaterial.FLOOR_TILES) {
            application = BrickApplication.FLOOR;
        }
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public BrickApplication getApplication() {
        return application;
    }

    public void setApplication(BrickApplication application) {
        this.application = material == MasonryMaterial.FLOOR_TILES
            ? BrickApplication.FLOOR
            : (application != null ? application : BrickApplication.FLOOR);
    }

    public boolean isAlongFirstEdge() {
        return alongFirstEdge;
    }

    public void setAlongFirstEdge(boolean alongFirstEdge) {
        this.alongFirstEdge = alongFirstEdge;
    }

    public BrickBond getBond() {
        return bond;
    }

    public void setBond(BrickBond bond) {
        this.bond = bond != null ? bond : BrickBond.RUNNING;
    }

    public BrickPalette getPalette() {
        return palette;
    }

    public void setPalette(BrickPalette palette) {
        this.palette = palette != null ? palette : BrickPalette.AUTO;
    }

    public BrickPalette getResolvedPalette() {
        if (palette != BrickPalette.AUTO) {
            return palette;
        }
        return getAutomaticPalette();
    }

    public BrickPalette getAutomaticPalette() {
        return material == MasonryMaterial.FLOOR_TILES
            ? BrickPalette.ASH_GRAY : BrickPalette.EARTHEN_BROWN;
    }

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, tileSize));
    }

    public int getBrickLength() {
        return brickLength;
    }

    public void setBrickLength(int brickLength) {
        this.brickLength = Math.max(MIN_BRICK_LENGTH, Math.min(MAX_BRICK_LENGTH, brickLength));
    }

    public int getBrickHeight() {
        return brickHeight;
    }

    public void setBrickHeight(int brickHeight) {
        this.brickHeight = Math.max(MIN_BRICK_HEIGHT, Math.min(MAX_BRICK_HEIGHT, brickHeight));
    }

    public double getMortarWidth() {
        return mortarWidth;
    }

    public void setMortarWidth(double mortarWidth) {
        this.mortarWidth = Math.max(0d, Math.min(8d, mortarWidth));
    }

    public double getMortarDarkness() {
        return mortarDarkness;
    }

    public void setMortarDarkness(double mortarDarkness) {
        this.mortarDarkness = clampUnit(mortarDarkness);
    }

    public double getRelief() {
        return relief;
    }

    public void setRelief(double relief) {
        this.relief = clampUnit(relief);
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }

    public double getToneVariation() {
        return toneVariation;
    }

    public void setToneVariation(double toneVariation) {
        this.toneVariation = clampUnit(toneVariation);
    }

    public double getWeathering() {
        return weathering;
    }

    public void setWeathering(double weathering) {
        this.weathering = clampUnit(weathering);
    }

    public double getIrregularity() {
        return irregularity;
    }

    public void setIrregularity(double irregularity) {
        this.irregularity = clampUnit(irregularity);
    }

    public double getLightUnevenness() {
        return lightUnevenness;
    }

    public void setLightUnevenness(double lightUnevenness) {
        this.lightUnevenness = clampUnit(lightUnevenness);
    }

    private static double clampUnit(double value) {
        if (value < 0d) {
            return 0d;
        }
        return value > 1d ? 1d : value;
    }
}
