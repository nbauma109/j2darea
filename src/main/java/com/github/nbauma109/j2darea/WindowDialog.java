package com.github.nbauma109.j2darea;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;

/** Live-preview editor for framed windows and their optional curtains. */
public class WindowDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    private final transient WindowSettings settings;
    private transient WindowSettings confirmedSettings;

    public WindowDialog(Frame owner, WindowSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Window", parallelogram);
        settings = new WindowSettings(initialSettings);
        initialize(owner, "Fit this window to the parallelogram");
    }

    public WindowSettings getConfirmedSettings() { return confirmedSettings; }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Construction");
        addSlider(constraints, "Panes across", 1, 4, settings.getColumns(),
            value -> Integer.toString(value), value -> settings.setColumns((int) Math.round(value)));
        addSlider(constraints, "Panes down", 1, 3, settings.getRows(),
            value -> Integer.toString(value), value -> settings.setRows((int) Math.round(value)));
        addSlider(constraints, "Frame width", 4, 16,
            (int) Math.round(settings.getFrameWidth() * 100d), percentLabel(),
            value -> settings.setFrameWidth(value / 100d));

        addSection(constraints, "Colours");
        addLabelledRow(constraints, "Scheme", new WindowPaletteGrid(settings.getPalette(),
            settings.getAutomaticPalette(), palette -> {
                settings.setPalette(palette);
                schedulePreview();
            }));
        addSlider(constraints, "Brightness", 50, 150,
            (int) Math.round(settings.getBrightness() * 100d), percentLabel(),
            value -> settings.setBrightness(value / 100d));

        addSection(constraints, "Curtains");
        addCheckBox(constraints, "Curtains", "Hang curtains", settings.hasCurtains(),
            settings::setCurtains);
        addPercentSlider(constraints, "Opening", settings.getCurtainOpenness(),
            settings::setCurtainOpenness);
        addSlider(constraints, "Length", 35, 100,
            (int) Math.round(settings.getCurtainLength() * 100d), percentLabel(),
            value -> settings.setCurtainLength(value / 100d));

        addSection(constraints, "Finish");
        addPercentSlider(constraints, "Wear", settings.getWear(), settings::setWear);
    }

    @Override
    protected void onSeedChanged() { rebuildControls(); }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        WindowSettings snapshot = new WindowSettings(settings);
        Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            WindowGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() { confirmedSettings = new WindowSettings(settings); }

    @Override
    protected void onReset() {
        WindowSettings defaults = new WindowSettings();
        defaults.setSeed(settings.getSeed());
        settings.copyFrom(defaults);
    }

    @Override protected long getSeed() { return settings.getSeed(); }
    @Override protected void setSeed(long seed) { settings.setSeed(seed); }
}
