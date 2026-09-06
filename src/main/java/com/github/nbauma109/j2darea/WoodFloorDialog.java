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

/**
 * Editor for {@link WoodFloorSettings}, with a live preview of the floor the
 * current settings lay in the parallelogram that was just drawn.
 */
public class WoodFloorDialog extends ShapeFillPreviewDialog {

    private static final long serialVersionUID = 1L;

    private final transient WoodFloorSettings settings;
    private transient WoodFloorSettings confirmedSettings;

    public WoodFloorDialog(Frame owner, WoodFloorSettings initialSettings, Polygon parallelogram) {
        super(owner, "Generate Wood Floor", parallelogram);
        this.settings = new WoodFloorSettings(initialSettings);
        initialize(owner, "Fill the parallelogram with this wood floor");
    }

    /** Settings the user confirmed, or {@code null} when the dialog was cancelled. */
    public WoodFloorSettings getConfirmedSettings() {
        return confirmedSettings;
    }

    @Override
    protected void buildControls(GridBagConstraints constraints) {
        addSection(constraints, "Boards");
        addRow(constraints, createDirectionPanel());
        addSlider(constraints, "Width", WoodFloorSettings.MIN_PLANK_WIDTH, WoodFloorSettings.MAX_PLANK_WIDTH,
            settings.getPlankWidth(), pixelLabel(),
            value -> settings.setPlankWidth((int) Math.round(value)));
        addSlider(constraints, "Length", WoodFloorSettings.MIN_PLANK_LENGTH,
            WoodFloorSettings.MAX_PLANK_LENGTH, settings.getPlankLength(), pixelLabel(),
            value -> settings.setPlankLength((int) Math.round(value)));
        addPercentSlider(constraints, "Width variation", settings.getWidthVariation(),
            value -> settings.setWidthVariation(value));
        addPercentSlider(constraints, "Length variation", settings.getLengthVariation(),
            value -> settings.setLengthVariation(value));
        addPercentSlider(constraints, "Stagger", settings.getStagger(),
            value -> settings.setStagger(value));
        addPercentSlider(constraints, "Irregularity", settings.getIrregularity(),
            value -> settings.setIrregularity(value));

        addSection(constraints, "Joints");
        addSlider(constraints, "Seam width", 0, 60, (int) Math.round(settings.getSeamWidth() * 10d),
            value -> String.format(Locale.ROOT, "%.1f px", Double.valueOf(value / 10d)),
            value -> settings.setSeamWidth(value / 10d));
        addPercentSlider(constraints, "Seam darkness", settings.getSeamDarkness(),
            value -> settings.setSeamDarkness(value));
        addPercentSlider(constraints, "Relief", settings.getRelief(),
            value -> settings.setRelief(value));

        addSection(constraints, "Wood");
        addSlider(constraints, "Brightness", 50, 150, (int) Math.round(settings.getBrightness() * 100d),
            percentLabel(), value -> settings.setBrightness(value / 100d));
        addSlider(constraints, "Warmth", -100, 100, (int) Math.round(settings.getWarmth() * 100d),
            percentLabel(), value -> settings.setWarmth(value / 100d));
        addPercentSlider(constraints, "Tone variation", settings.getToneVariation(),
            value -> settings.setToneVariation(value));
        addPercentSlider(constraints, "Grain", settings.getGrainAmount(),
            value -> settings.setGrainAmount(value));
        addPercentSlider(constraints, "Knots", settings.getKnotDensity(),
            value -> settings.setKnotDensity(value));
        addPercentSlider(constraints, "Wear", settings.getWear(),
            value -> settings.setWear(value));

        addSection(constraints, "Light");
        addPercentSlider(constraints, "Unevenness", settings.getLightUnevenness(),
            value -> settings.setLightUnevenness(value));
    }

    /** Which of the two drawn edges the boards are laid along. */
    private JPanel createDirectionPanel() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        javax.swing.JLabel label = new javax.swing.JLabel("Run along");
        label.setPreferredSize(new Dimension(108, label.getPreferredSize().height));
        row.add(label, BorderLayout.WEST);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        JRadioButton firstEdgeButton = new JRadioButton("First edge", settings.isAlongFirstEdge());
        firstEdgeButton.setToolTipText("Lay the boards along the edge drawn between the first two clicks");
        JRadioButton secondEdgeButton = new JRadioButton("Second edge", !settings.isAlongFirstEdge());
        secondEdgeButton.setToolTipText("Lay the boards along the edge drawn between the second and third clicks");
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
        final WoodFloorSettings snapshot = new WoodFloorSettings(settings);
        final Polygon shape = getParallelogram();
        return (viewX, viewY, width, height, scale) ->
            WoodFloorGenerator.render(snapshot, shape, viewX, viewY, width, height, scale, null);
    }

    @Override
    protected void onConfirmed() {
        confirmedSettings = new WoodFloorSettings(settings);
    }

    @Override
    protected void onReset() {
        WoodFloorSettings defaults = new WoodFloorSettings();
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
