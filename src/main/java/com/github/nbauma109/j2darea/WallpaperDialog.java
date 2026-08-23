package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** Live-preview editor for seamless repeated wallpaper. */
public class WallpaperDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    private final transient WallpaperSettings settings;
    private transient WallpaperSettings confirmedSettings;

    public WallpaperDialog(Frame owner, WallpaperSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Wallpaper", parallelogram);
        settings = new WallpaperSettings(initialSettings);
        initialize(owner, "Cover the parallelogram with this wallpaper");
    }

    public WallpaperSettings getConfirmedSettings() {
        return confirmedSettings;
    }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Repeat");
        addChoice(constraints, "Pattern", WallpaperPattern.values(), settings.getPattern(),
            pattern -> pattern == WallpaperPattern.AUTO
                ? pattern.getDisplayName() + " (" + settings.getResolvedPattern().getDisplayName() + ")"
                : pattern.getDisplayName(), settings::setPattern);
        addRow(constraints, createDirectionPanel());
        addSlider(constraints, "Repeat size", WallpaperSettings.MIN_REPEAT_SIZE,
            WallpaperSettings.MAX_REPEAT_SIZE, settings.getRepeatSize(), pixelLabel(),
            value -> settings.setRepeatSize((int) Math.round(value)));
        addPercentSlider(constraints, "Line weight", settings.getLineWeight(),
            settings::setLineWeight);

        addSection(constraints, "Colours");
        addLabelledRow(constraints, "Scheme", new WallpaperPaletteGrid(settings.getPalette(),
            settings.getAutomaticPalette(), palette -> {
                settings.setPalette(palette);
                schedulePreview();
            }));
        addSlider(constraints, "Brightness", 50, 150,
            (int) Math.round(settings.getBrightness() * 100d), percentLabel(),
            value -> settings.setBrightness(value / 100d));

        addSection(constraints, "Surface");
        addPercentSlider(constraints, "Fading", settings.getFade(), settings::setFade);
        addPercentSlider(constraints, "Wear", settings.getWear(), settings::setWear);
        addPercentSlider(constraints, "Uneven light", settings.getLightUnevenness(),
            settings::setLightUnevenness);
    }

    private JPanel createDirectionPanel() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        javax.swing.JLabel label = new javax.swing.JLabel("Repeat along");
        label.setPreferredSize(new Dimension(108, label.getPreferredSize().height));
        row.add(label, BorderLayout.WEST);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JRadioButton first = new JRadioButton("First edge", settings.isAlongFirstEdge());
        JRadioButton second = new JRadioButton("Second edge", !settings.isAlongFirstEdge());
        first.setToolTipText("Align the repeat to the first drawn edge");
        second.setToolTipText("Align the repeat to the second drawn edge");
        ButtonGroup group = new ButtonGroup();
        group.add(first);
        group.add(second);
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                settings.setAlongFirstEdge(first.isSelected());
                schedulePreview();
            }
        };
        first.addActionListener(listener);
        second.addActionListener(listener);
        buttons.add(first);
        buttons.add(second);
        row.add(buttons, BorderLayout.CENTER);
        return row;
    }

    @Override
    protected void onSeedChanged() {
        rebuildControls();
    }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        WallpaperSettings snapshot = new WallpaperSettings(settings);
        Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            WallpaperGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() {
        confirmedSettings = new WallpaperSettings(settings);
    }

    @Override
    protected void onReset() {
        WallpaperSettings defaults = new WallpaperSettings();
        defaults.setSeed(settings.getSeed());
        settings.copyFrom(defaults);
    }

    @Override protected long getSeed() { return settings.getSeed(); }
    @Override protected void setSeed(long seed) { settings.setSeed(seed); }
}
