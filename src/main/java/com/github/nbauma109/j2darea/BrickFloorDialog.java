package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** Live-preview editor for generated brick floors and walls. */
public class BrickFloorDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    private final transient BrickFloorSettings settings;
    private transient BrickFloorSettings confirmedSettings;

    public BrickFloorDialog(Frame owner, BrickFloorSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Bricks", parallelogram);
        this.settings = new BrickFloorSettings(initialSettings);
        this.settings.setMaterial(MasonryMaterial.BRICKS);
        initialize(owner, "Fill the parallelogram with these bricks");
    }

    public BrickFloorSettings getConfirmedSettings() {
        return confirmedSettings;
    }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Use");
        addChoice(constraints, "Application", BrickApplication.values(), settings.getApplication(),
            BrickApplication::getDisplayName, application -> {
                settings.setApplication(application);
                rebuildControls();
            });

        addSection(constraints, "Layout");
        addRow(constraints, createDirectionPanel());
        addChoice(constraints, "Bond", BrickBond.values(), settings.getBond(),
            BrickBond::getDisplayName, settings::setBond);
        addSlider(constraints, "Brick length", BrickFloorSettings.MIN_BRICK_LENGTH,
            BrickFloorSettings.MAX_BRICK_LENGTH, settings.getBrickLength(), pixelLabel(),
            value -> settings.setBrickLength((int) Math.round(value)));
        addSlider(constraints, "Course height", BrickFloorSettings.MIN_BRICK_HEIGHT,
            BrickFloorSettings.MAX_BRICK_HEIGHT, settings.getBrickHeight(), pixelLabel(),
            value -> settings.setBrickHeight((int) Math.round(value)));
        if (settings.getApplication() == BrickApplication.WALL) {
            addPercentSlider(constraints, "Irregularity", settings.getIrregularity(),
                settings::setIrregularity);
        }

        addSection(constraints, "Joints");
        addSlider(constraints, "Mortar width", 0, 80,
            (int) Math.round(settings.getMortarWidth() * 10d),
            value -> String.format(Locale.ROOT, "%.1f px", Double.valueOf(value / 10d)),
            value -> settings.setMortarWidth(value / 10d));
        addPercentSlider(constraints, "Mortar darkness", settings.getMortarDarkness(),
            settings::setMortarDarkness);
        addPercentSlider(constraints, "Relief", settings.getRelief(), settings::setRelief);

        addSection(constraints, "Brick");
        addLabelledRow(constraints, "Colour", new BrickPaletteGrid(settings.getPalette(),
            settings.getAutomaticPalette(), palette -> {
                settings.setPalette(palette);
                schedulePreview();
            }));
        addSlider(constraints, "Brightness", 50, 150,
            (int) Math.round(settings.getBrightness() * 100d), percentLabel(),
            value -> settings.setBrightness(value / 100d));
        addPercentSlider(constraints, "Tone variation", settings.getToneVariation(),
            settings::setToneVariation);
        addPercentSlider(constraints, "Weathering", settings.getWeathering(),
            settings::setWeathering);

        if (settings.getApplication() == BrickApplication.WALL) {
            addSection(constraints, "Light");
            addPercentSlider(constraints, "Unevenness", settings.getLightUnevenness(),
                settings::setLightUnevenness);
        }
    }

    private JPanel createDirectionPanel() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        javax.swing.JLabel label = new javax.swing.JLabel("Courses along");
        label.setPreferredSize(new Dimension(108, label.getPreferredSize().height));
        row.add(label, BorderLayout.WEST);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JRadioButton firstEdgeButton = new JRadioButton("First edge", settings.isAlongFirstEdge());
        firstEdgeButton.setToolTipText("Align the masonry pattern to the first drawn edge");
        JRadioButton secondEdgeButton = new JRadioButton("Second edge", !settings.isAlongFirstEdge());
        secondEdgeButton.setToolTipText("Align the masonry pattern to the second drawn edge");
        ButtonGroup group = new ButtonGroup();
        group.add(firstEdgeButton);
        group.add(secondEdgeButton);
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setAlongFirstEdge(firstEdgeButton.isSelected());
                schedulePreview();
            }
        };
        firstEdgeButton.addActionListener(listener);
        secondEdgeButton.addActionListener(listener);
        buttons.add(firstEdgeButton);
        buttons.add(secondEdgeButton);
        row.add(buttons, BorderLayout.CENTER);
        return row;
    }

    @Override
    protected PreviewRenderer snapshotRenderer() {
        BrickFloorSettings snapshot = new BrickFloorSettings(settings);
        Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            BrickFloorGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() {
        confirmedSettings = new BrickFloorSettings(settings);
    }

    @Override
    protected void onReset() {
        BrickFloorSettings defaults = new BrickFloorSettings(MasonryMaterial.BRICKS);
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
