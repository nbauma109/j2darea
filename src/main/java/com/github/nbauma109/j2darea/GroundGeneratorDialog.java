package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.DoubleConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Editor for {@link GroundGeneratorSettings}, with a live preview of the ground
 * the current settings produce.
 *
 * <p>The preview either shows a native-resolution window of the ground, which
 * can be dragged around, or the whole area scaled down so the patch layout can
 * be judged as a whole.
 */
public class GroundGeneratorDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int PREVIEW_WIDTH = 520;
    private static final int PREVIEW_HEIGHT = 470;

    private final GroundGeneratorSettings settings;
    private final int canvasWidth;
    private final int canvasHeight;
    private final JTextField seedField = new JTextField(14);
    private final Timer previewTimer;
    private final PreviewPanel previewPanel = new PreviewPanel();
    private JPanel controlPanel;

    private boolean wholeAreaPreview;
    private double previewCenterX;
    private double previewCenterY;
    private int previewRequestId;
    private boolean updatingSeedField;
    private GroundGeneratorSettings confirmedSettings;

    public GroundGeneratorDialog(Frame owner, GroundGeneratorSettings initialSettings,
            int canvasWidth, int canvasHeight) {
        super(owner, "Generate Random Ground", true);
        this.settings = new GroundGeneratorSettings(initialSettings);
        this.canvasWidth = Math.max(1, canvasWidth);
        this.canvasHeight = Math.max(1, canvasHeight);
        this.previewCenterX = this.canvasWidth / 2d;
        this.previewCenterY = this.canvasHeight / 2d;
        this.previewTimer = new Timer(120, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshPreview();
            }
        });
        this.previewTimer.setRepeats(false);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        refreshPreview();
    }

    /** Settings the user confirmed, or {@code null} when the dialog was cancelled. */
    public GroundGeneratorSettings getConfirmedSettings() {
        return confirmedSettings;
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel previewSide = new JPanel(new BorderLayout(0, 6));
        previewSide.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
        previewPanel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        previewSide.add(previewPanel, BorderLayout.CENTER);

        JPanel previewModePanel = new JPanel();
        previewModePanel.setLayout(new BoxLayout(previewModePanel, BoxLayout.X_AXIS));
        JRadioButton detailButton = new JRadioButton("Detail (1:1)", true);
        detailButton.setToolTipText("Show the ground at final resolution; drag the preview to look around");
        JRadioButton wholeAreaButton = new JRadioButton("Whole area");
        wholeAreaButton.setToolTipText("Scale the whole area down to judge the patch layout");
        ButtonGroup previewModeGroup = new ButtonGroup();
        previewModeGroup.add(detailButton);
        previewModeGroup.add(wholeAreaButton);
        ActionListener previewModeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wholeAreaPreview = wholeAreaButton.isSelected();
                schedulePreview();
            }
        };
        detailButton.addActionListener(previewModeListener);
        wholeAreaButton.addActionListener(previewModeListener);
        previewModePanel.add(detailButton);
        previewModePanel.add(wholeAreaButton);
        previewModePanel.add(Box.createHorizontalGlue());
        previewModePanel.add(new JLabel(canvasWidth + " x " + canvasHeight));
        previewSide.add(previewModePanel, BorderLayout.SOUTH);
        add(previewSide, BorderLayout.CENTER);

        JScrollPane controlScroll = new JScrollPane(createControlPanel());
        controlScroll.setBorder(BorderFactory.createEmptyBorder());
        controlScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);
        controlScroll.setPreferredSize(new Dimension(408, PREVIEW_HEIGHT + 30));
        add(controlScroll, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Restore the default ground settings");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GroundGeneratorSettings defaults = new GroundGeneratorSettings();
                defaults.setSeed(settings.getSeed());
                settings.copyFrom(defaults);
                rebuildControls();
            }
        });
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalGlue());
        JButton generateButton = new JButton("Generate");
        generateButton.setToolTipText("Fill the whole build area background with this ground");
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmedSettings = new GroundGeneratorSettings(settings);
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmedSettings = null;
                dispose();
            }
        });
        buttonPanel.add(generateButton);
        buttonPanel.add(Box.createHorizontalStrut(6));
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(generateButton);

        previewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                previewPanel.beginDrag(e.getPoint().x, e.getPoint().y);
            }
        });
        previewPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                previewPanel.dragTo(e.getPoint().x, e.getPoint().y);
            }
        });
    }

    private JPanel createControlPanel() {
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rebuildControls();
        return controlPanel;
    }

    private void rebuildControls() {
        controlPanel.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 2, 0);

        JPanel seedPanel = new JPanel(new BorderLayout(4, 0));
        seedField.setColumns(10);
        updatingSeedField = true;
        seedField.setText(String.valueOf(settings.getSeed()));
        updatingSeedField = false;
        seedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                seedFieldChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                seedFieldChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                seedFieldChanged();
            }
        });
        seedPanel.add(seedField, BorderLayout.CENTER);
        JButton randomizeButton = new JButton("Randomize");
        randomizeButton.setToolTipText("Pick a new random seed");
        randomizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setSeed(new Random().nextLong());
                updatingSeedField = true;
                seedField.setText(String.valueOf(settings.getSeed()));
                updatingSeedField = false;
                schedulePreview();
            }
        });
        seedPanel.add(randomizeButton, BorderLayout.EAST);
        addSection(constraints, "Seed");
        controlPanel.add(seedPanel, constraints);
        constraints.gridy++;

        addSection(constraints, "Shape");
        addSlider(constraints, "Patch size", GroundGeneratorSettings.MIN_PATCH_SIZE,
            GroundGeneratorSettings.MAX_PATCH_SIZE, settings.getPatchSize(), " px",
            value -> settings.setPatchSize((int) Math.round(value)));
        addPercentSlider(constraints, "Edge irregularity", settings.getEdgeIrregularity(),
            value -> settings.setEdgeIrregularity(value));
        addPercentSlider(constraints, "Edge softness", settings.getEdgeSoftness(),
            value -> settings.setEdgeSoftness(value));

        addSection(constraints, "Patch coverage");
        for (GroundMaterial material : GroundMaterial.patchMaterials()) {
            final GroundMaterial patchMaterial = material;
            addSlider(constraints, material.getDisplayName(), 0, 70,
                (int) Math.round(settings.getCoverage(material) * 100d), "%",
                value -> settings.setCoverage(patchMaterial, value / 100d));
        }

        addSection(constraints, "Grass");
        addPercentSlider(constraints, "Tone variation", settings.getGrassToneVariation(),
            value -> settings.setGrassToneVariation(value));
        addSlider(constraints, "Dryness", -100, 100, (int) Math.round(settings.getGrassDryness() * 100d), "%",
            value -> settings.setGrassDryness(value / 100d));

        addSection(constraints, "Surface");
        addSlider(constraints, "Brightness", 50, 150, (int) Math.round(settings.getBrightness() * 100d), "%",
            value -> settings.setBrightness(value / 100d));
        addPercentSlider(constraints, "Detail", settings.getDetailAmount(),
            value -> settings.setDetailAmount(value));
        addPercentSlider(constraints, "Flowers", settings.getFlowerDensity(),
            value -> settings.setFlowerDensity(value));
        addPercentSlider(constraints, "Stones", settings.getPebbleDensity(),
            value -> settings.setPebbleDensity(value));

        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;
        controlPanel.add(Box.createGlue(), constraints);
        controlPanel.revalidate();
        controlPanel.repaint();
        schedulePreview();
    }

    private void addSection(GridBagConstraints constraints, String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        controlPanel.add(label, constraints);
        constraints.gridy++;
    }

    private void addPercentSlider(GridBagConstraints constraints, String label, double value,
            DoubleConsumer setter) {
        addSlider(constraints, label, 0, 100, (int) Math.round(value * 100d), "%",
            sliderValue -> setter.accept(sliderValue / 100d));
    }

    private void addSlider(GridBagConstraints constraints, String label, int min, int max, int value,
            String unit, DoubleConsumer setter) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setPreferredSize(new Dimension(104, nameLabel.getPreferredSize().height));
        row.add(nameLabel, BorderLayout.WEST);
        JSlider slider = new JSlider(min, max, Math.max(min, Math.min(max, value)));
        slider.setPreferredSize(new Dimension(140, slider.getPreferredSize().height));
        row.add(slider, BorderLayout.CENTER);
        JLabel valueLabel = new JLabel(slider.getValue() + unit);
        valueLabel.setPreferredSize(new Dimension(52, valueLabel.getPreferredSize().height));
        row.add(valueLabel, BorderLayout.EAST);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                valueLabel.setText(slider.getValue() + unit);
                setter.accept(slider.getValue());
                schedulePreview();
            }
        });
        controlPanel.add(row, constraints);
        constraints.gridy++;
    }

    private void seedFieldChanged() {
        if (updatingSeedField) {
            return;
        }
        String text = seedField.getText().trim();
        if (text.isEmpty() || "-".equals(text)) {
            return;
        }
        try {
            settings.setSeed(Long.parseLong(text));
            schedulePreview();
        } catch (NumberFormatException ex) {
            // Keep the previous seed while the field holds something unparsable.
        }
    }

    private void schedulePreview() {
        previewTimer.restart();
    }

    private void refreshPreview() {
        final int requestId = ++previewRequestId;
        final GroundGeneratorSettings snapshot = new GroundGeneratorSettings(settings);
        final int width = Math.max(1, previewPanel.getWidth());
        final int height = Math.max(1, previewPanel.getHeight());
        final double scale = wholeAreaPreview
            ? Math.min(width / (double) canvasWidth, height / (double) canvasHeight)
            : 1d;
        final double viewX = wholeAreaPreview ? 0d : clampViewX(width);
        final double viewY = wholeAreaPreview ? 0d : clampViewY(height);
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return GroundGenerator.render(snapshot, viewX, viewY,
                    wholeAreaPreview ? (int) Math.round(canvasWidth * scale) : width,
                    wholeAreaPreview ? (int) Math.round(canvasHeight * scale) : height,
                    scale, null);
            }

            @Override
            protected void done() {
                if (requestId != previewRequestId) {
                    return;
                }
                try {
                    previewPanel.setImage(get());
                } catch (Exception ex) {
                    previewPanel.setImage(null);
                }
            }
        }.execute();
    }

    private double clampViewX(int width) {
        double half = width / 2d;
        double center = Math.max(half, Math.min(canvasWidth - half, previewCenterX));
        return Math.max(0d, center - half);
    }

    private double clampViewY(int height) {
        double half = height / 2d;
        double center = Math.max(half, Math.min(canvasHeight - half, previewCenterY));
        return Math.max(0d, center - half);
    }

    /** Preview surface; in detail mode dragging it pans over the generated area. */
    private final class PreviewPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private transient BufferedImage image;
        private int dragOriginX;
        private int dragOriginY;
        private double dragCenterX;
        private double dragCenterY;

        private PreviewPanel() {
            setBackground(Color.BLACK);
            setToolTipText("Drag to look at another part of the generated ground");
        }

        private void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        private void beginDrag(int x, int y) {
            dragOriginX = x;
            dragOriginY = y;
            dragCenterX = previewCenterX;
            dragCenterY = previewCenterY;
        }

        private void dragTo(int x, int y) {
            if (wholeAreaPreview) {
                return;
            }
            previewCenterX = dragCenterX - (x - dragOriginX);
            previewCenterY = dragCenterY - (y - dragOriginY);
            schedulePreview();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                return;
            }
            int x = Math.max(0, (getWidth() - image.getWidth()) / 2);
            int y = Math.max(0, (getHeight() - image.getHeight()) / 2);
            g.drawImage(image, x, y, null);
        }
    }
}
