package com.github.nbauma109.j2darea;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;

/**
 * Live-preview editor for generated framed paintings.
 *
 * <p>Subject and frame finish each default to {@code Random}, which leaves them
 * to the seed: with both left alone, `Randomize` alone gives a painting that
 * shares nothing with the last one. The controls read back what the seed chose,
 * so it is always clear what is on the wall.
 */
public class PaintingDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    /** Shown in the frame drop-down for "let the seed choose". */
    private static final PaintingFramePalette[] PALETTE_CHOICES = paletteChoices();

    private final transient PaintingSettings settings;
    private transient PaintingSettings confirmedSettings;

    public PaintingDialog(Frame owner, PaintingSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Painting", parallelogram);
        settings = new PaintingSettings(initialSettings);
        initialize(owner, "Fit this painting to the parallelogram");
    }

    public PaintingSettings getConfirmedSettings() { return confirmedSettings; }

    /** The frame finishes, with {@code null} in front standing for "let the seed choose". */
    private static PaintingFramePalette[] paletteChoices() {
        PaintingFramePalette[] palettes = PaintingFramePalette.framed();
        PaintingFramePalette[] choices = new PaintingFramePalette[palettes.length + 1];
        System.arraycopy(palettes, 0, choices, 1, palettes.length);
        return choices;
    }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Subject");
        addChoice(constraints, "Subject", PaintingSubject.values(), settings.getSubject(),
            subject -> describe(subject.getDisplayName(),
                subject == PaintingSubject.AUTO ? settings.getResolvedSubject().getDisplayName() : null),
            settings::setSubject);

        addSection(constraints, "Frame");
        addChoice(constraints, "Finish", PALETTE_CHOICES, settings.getPalette(),
            palette -> palette == null
                ? describe("Random", settings.getResolvedPalette().getDisplayName())
                : palette.getDisplayName(),
            palette -> settings.setPalette(palette));
        addSlider(constraints, "Frame width", 3, 16,
            (int) Math.round(settings.getFrameWidth() * 100d), percentLabel(),
            value -> settings.setFrameWidth(value / 100d));
        addSlider(constraints, "Mat width", 0, 12,
            (int) Math.round(settings.getMatWidth() * 100d), percentLabel(),
            value -> settings.setMatWidth(value / 100d));

        addSection(constraints, "Finish");
        addSlider(constraints, "Brightness", 50, 150,
            (int) Math.round(settings.getBrightness() * 100d), percentLabel(),
            value -> settings.setBrightness(value / 100d));
        addPercentSlider(constraints, "Wear", settings.getWear(), settings::setWear);
    }

    /** "Random" reads better as "Random (Harbor at sunset)" once the seed has chosen one. */
    private static String describe(String name, String resolved) {
        return resolved == null ? name : name + " (" + resolved + ")";
    }

    @Override
    protected void onSeedChanged() { rebuildControls(); }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        PaintingSettings snapshot = new PaintingSettings(settings);
        Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            PaintingGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() { confirmedSettings = new PaintingSettings(settings); }

    @Override
    protected void onReset() {
        PaintingSettings defaults = new PaintingSettings();
        defaults.setSeed(settings.getSeed());
        settings.copyFrom(defaults);
    }

    @Override protected long getSeed() { return settings.getSeed(); }
    @Override protected void setSeed(long seed) { settings.setSeed(seed); }
}
