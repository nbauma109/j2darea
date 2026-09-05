package com.github.nbauma109.j2darea;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;
import java.util.Locale;

/**
 * Editor for {@link CarpetSettings}, with a live preview of the carpet the
 * current settings weave into the parallelogram that was just drawn.
 *
 * <p>Field pattern, border motif, medallion and dye set each default to
 * {@code Random}, which leaves them to the seed: with all four left alone,
 * `Randomize` alone gives a carpet that shares nothing with the last one. The
 * controls read back what the seed chose, so it is always clear what is on the
 * loom.
 */
public class CarpetDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    /** Shown in the dye-set drop-down for "let the seed choose". */
    private static final CarpetPalette[] PALETTE_CHOICES = paletteChoices();

    private final transient CarpetSettings settings;
    private transient CarpetSettings confirmedSettings;

    public CarpetDialog(Frame owner, CarpetSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Carpet", parallelogram);
        this.settings = new CarpetSettings(initialSettings);
        initialize(owner, "Fill the parallelogram with this carpet");
    }

    /** Settings the user confirmed, or {@code null} when the dialog was cancelled. */
    public CarpetSettings getConfirmedSettings() {
        return confirmedSettings;
    }

    /** The dye sets, with {@code null} in front of them standing for "let the seed choose". */
    private static CarpetPalette[] paletteChoices() {
        CarpetPalette[] palettes = CarpetPalette.values();
        CarpetPalette[] choices = new CarpetPalette[palettes.length + 1];
        System.arraycopy(palettes, 0, choices, 1, palettes.length);
        return choices;
    }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Pattern");
        addChoice(constraints, "Field", CarpetFieldPattern.values(), settings.getFieldPattern(),
            pattern -> describe(pattern.getDisplayName(),
                pattern == CarpetFieldPattern.AUTO ? settings.getResolvedFieldPattern().getDisplayName() : null),
            pattern -> settings.setFieldPattern(pattern));
        addChoice(constraints, "Border", CarpetBorderPattern.values(), settings.getBorderPattern(),
            pattern -> describe(pattern.getDisplayName(),
                pattern == CarpetBorderPattern.AUTO ? settings.getResolvedBorderPattern().getDisplayName() : null),
            pattern -> settings.setBorderPattern(pattern));
        addChoice(constraints, "Dyes", PALETTE_CHOICES, settings.getPalette(),
            palette -> palette == null
                ? describe("Random", settings.getResolvedPalette().getDisplayName())
                : palette.getDisplayName(),
            palette -> settings.setPalette(palette));

        addSection(constraints, "Weaving");
        addSlider(constraints, "Motif size", CarpetSettings.MIN_MOTIF_SIZE, CarpetSettings.MAX_MOTIF_SIZE,
            settings.getMotifSize(), pixelLabel(),
            value -> settings.setMotifSize((int) Math.round(value)));
        addSlider(constraints, "Border width", 0, CarpetSettings.MAX_BORDER_WIDTH,
            settings.getBorderWidth(), pixelLabel(),
            value -> settings.setBorderWidth((int) Math.round(value)));
        addChoice(constraints, "Medallion", CarpetMedallion.values(), settings.getMedallion(),
            medallion -> describe(medallion.getDisplayName(),
                medallion == CarpetMedallion.AUTO ? settings.getResolvedMedallion().getDisplayName() : null),
            medallion -> settings.setMedallion(medallion));
        addSlider(constraints, "Knot size", 0, 60, (int) Math.round(settings.getKnotSize() * 10d),
            value -> String.format(Locale.ROOT, "%.1f px", Double.valueOf(value / 10d)),
            value -> settings.setKnotSize(value / 10d));
        addCheckBox(constraints, "Fringe", "Ends left in undyed warp", settings.hasFringe(),
            value -> settings.setFringe(value.booleanValue()));

        addSection(constraints, "Pile");
        addSlider(constraints, "Brightness", 50, 150, (int) Math.round(settings.getBrightness() * 100d),
            percentLabel(), value -> settings.setBrightness(value / 100d));
        addPercentSlider(constraints, "Weave", settings.getWeave(),
            value -> settings.setWeave(value));
        addPercentSlider(constraints, "Wear", settings.getWear(),
            value -> settings.setWear(value));

        addSection(constraints, "Light");
        addPercentSlider(constraints, "Unevenness", settings.getLightUnevenness(),
            value -> settings.setLightUnevenness(value));
    }

    /** "Random" reads better as "Random (Greek key)" once the seed has chosen one. */
    private static String describe(String name, String resolved) {
        return resolved == null ? name : name + " (" + resolved + ")";
    }

    @Override
    protected void onSeedChanged() {
        // The three Random rows name what the seed picked, so they have to be
        // rebuilt when it changes.
        rebuildControls();
    }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        final CarpetSettings snapshot = new CarpetSettings(settings);
        final Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            CarpetGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() {
        confirmedSettings = new CarpetSettings(settings);
    }

    @Override
    protected void onReset() {
        CarpetSettings defaults = new CarpetSettings();
        defaults.setSeed(settings.getSeed());
        settings.copyFrom(defaults);
    }

    @Override
    protected long getSeed() {
        return settings.getSeed();
    }

    @Override
    protected void setSeed(long seed) {
        settings.setSeed(seed);
    }
}
