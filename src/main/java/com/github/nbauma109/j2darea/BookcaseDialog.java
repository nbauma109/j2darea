package com.github.nbauma109.j2darea;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;

/** Live-preview editor for bookcases fitted to wall parallelograms. */
public class BookcaseDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;
    private final transient BookcaseSettings settings;
    private transient BookcaseSettings confirmedSettings;

    public BookcaseDialog(Frame owner, BookcaseSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Bookcase", parallelogram);
        settings = new BookcaseSettings(initialSettings);
        initialize(owner, "Fit this bookcase to the parallelogram");
    }

    public BookcaseSettings getConfirmedSettings() { return confirmedSettings; }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Construction");
        addSlider(constraints, "Shelf levels", 2, 7, settings.getShelves(),
            value -> Integer.toString(value), value -> settings.setShelves((int) Math.round(value)));
        addSlider(constraints, "Bays across", 1, 4, settings.getBays(),
            value -> Integer.toString(value), value -> settings.setBays((int) Math.round(value)));
        addSlider(constraints, "Frame width", 4, 14,
            (int) Math.round(settings.getFrameWidth() * 100d), percentLabel(),
            value -> settings.setFrameWidth(value / 100d));
        addPercentSlider(constraints, "Book density", settings.getBookDensity(),
            settings::setBookDensity);

        addSection(constraints, "Colours");
        addLabelledRow(constraints, "Scheme", new BookcasePaletteGrid(settings.getPalette(),
            settings.getAutomaticPalette(), palette -> {
                settings.setPalette(palette);
                schedulePreview();
            }));
        addSlider(constraints, "Brightness", 50, 150,
            (int) Math.round(settings.getBrightness() * 100d), percentLabel(),
            value -> settings.setBrightness(value / 100d));

        addSection(constraints, "Finish");
        addPercentSlider(constraints, "Wear", settings.getWear(), settings::setWear);
    }

    @Override protected void onSeedChanged() { rebuildControls(); }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        BookcaseSettings snapshot = new BookcaseSettings(settings);
        Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            BookcaseGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override protected void onConfirmed() { confirmedSettings = new BookcaseSettings(settings); }

    @Override
    protected void onReset() {
        BookcaseSettings defaults = new BookcaseSettings();
        defaults.setSeed(settings.getSeed());
        settings.copyFrom(defaults);
    }

    @Override protected long getSeed() { return settings.getSeed(); }
    @Override protected void setSeed(long seed) { settings.setSeed(seed); }
}
