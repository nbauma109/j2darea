package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
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
 * Shared editor for the generators that fill a drawn parallelogram: a live
 * preview of the shape on the left, a column of controls on the right, and a
 * seed the whole thing hangs off.
 *
 * <p>Everything about the preview is here — the two view modes, the panning, the
 * debounce, running the render off the event thread — because it is the same
 * work whichever surface is being generated. A subclass supplies only its own
 * controls and a way to render a region of its surface.
 *
 * <p>The preview either shows a native-resolution window of the surface, which
 * can be dragged around, or the whole shape scaled down so the layout can be
 * judged as a whole.
 */
public abstract class ShapeFillPreviewDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final int PREVIEW_WIDTH = 520;
    private static final int PREVIEW_HEIGHT = 470;
    private static final int CONTROL_WIDTH = 408;
    private static final int LABEL_WIDTH = 108;

    /** Renders a region of the surface. Must be a snapshot: it runs off the event thread. */
    public interface PreviewRenderer {
        BufferedImage render(double viewX, double viewY, int width, int height, double scale);
    }

    private final transient Polygon parallelogram;
    private final Rectangle shapeBounds;
    private final JTextField seedField = new JTextField(14);
    private final Timer previewTimer;
    private final PreviewPanel previewPanel = new PreviewPanel();
    private JPanel controlPanel;

    private boolean wholeShapePreview;
    private double previewCenterX;
    private double previewCenterY;
    private int previewRequestId;
    private boolean updatingSeedField;

    protected ShapeFillPreviewDialog(Frame owner, String title, Polygon parallelogram) {
        super(owner, title, true);
        this.parallelogram = parallelogram;
        this.shapeBounds = parallelogram.getBounds();
        this.previewCenterX = shapeBounds.getCenterX();
        this.previewCenterY = shapeBounds.getCenterY();
        this.previewTimer = new Timer(120, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshPreview();
            }
        });
        this.previewTimer.setRepeats(false);
    }

    /**
     * Builds the window. A subclass calls this at the end of its own constructor,
     * once its settings exist: the controls are built from them, and they are not
     * assigned until after {@code super(...)} has returned.
     */
    protected final void initialize(Frame owner, String generateTooltip) {
        initComponents(generateTooltip);
        pack();
        setLocationRelativeTo(owner);
        refreshPreview();
    }

    protected Polygon getParallelogram() {
        return parallelogram;
    }

    // ------------------------------------------------------------------
    // What a subclass has to provide
    // ------------------------------------------------------------------

    /** Adds the subclass's own rows to the control column. */
    protected abstract void buildControls(GridBagConstraints constraints);

    /** A renderer bound to a copy of the current settings, safe to use off the event thread. */
    protected abstract PreviewRenderer snapshotRenderer();

    /** Records the settings the user confirmed. */
    protected abstract void onConfirmed();

    /** Restores the default settings, keeping the current seed. */
    protected abstract void onReset();

    protected abstract long getSeed();

    protected abstract void setSeed(long seed);

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void initComponents(String generateTooltip) {
        setLayout(new BorderLayout(8, 8));

        JPanel previewSide = new JPanel(new BorderLayout(0, 6));
        previewSide.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
        previewPanel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        previewSide.add(previewPanel, BorderLayout.CENTER);
        previewSide.add(createPreviewModePanel(), BorderLayout.SOUTH);
        add(previewSide, BorderLayout.CENTER);

        JScrollPane controlScroll = new JScrollPane(createControlPanel());
        controlScroll.setBorder(BorderFactory.createEmptyBorder());
        controlScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);
        controlScroll.setPreferredSize(new Dimension(CONTROL_WIDTH, PREVIEW_HEIGHT + 30));
        add(controlScroll, BorderLayout.EAST);
        add(createButtonPanel(generateTooltip), BorderLayout.SOUTH);

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

    private JPanel createPreviewModePanel() {
        JPanel previewModePanel = new JPanel();
        previewModePanel.setLayout(new BoxLayout(previewModePanel, BoxLayout.X_AXIS));
        JRadioButton detailButton = new JRadioButton("Detail (1:1)", true);
        detailButton.setToolTipText("Show it at final resolution; drag the preview to look around");
        JRadioButton wholeShapeButton = new JRadioButton("Whole shape");
        wholeShapeButton.setToolTipText("Scale the whole parallelogram down to judge the layout");
        ButtonGroup previewModeGroup = new ButtonGroup();
        previewModeGroup.add(detailButton);
        previewModeGroup.add(wholeShapeButton);
        ActionListener previewModeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wholeShapePreview = wholeShapeButton.isSelected();
                schedulePreview();
            }
        };
        detailButton.addActionListener(previewModeListener);
        wholeShapeButton.addActionListener(previewModeListener);
        previewModePanel.add(detailButton);
        previewModePanel.add(wholeShapeButton);
        previewModePanel.add(Box.createHorizontalGlue());
        previewModePanel.add(new JLabel(shapeBounds.width + " x " + shapeBounds.height));
        return previewModePanel;
    }

    private JPanel createButtonPanel(String generateTooltip) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Restore the default settings");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onReset();
                rebuildControls();
            }
        });
        buttonPanel.add(resetButton);
        buttonPanel.add(Box.createHorizontalGlue());
        JButton generateButton = new JButton("Generate");
        generateButton.setToolTipText(generateTooltip);
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onConfirmed();
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(generateButton);
        buttonPanel.add(Box.createHorizontalStrut(6));
        buttonPanel.add(cancelButton);
        getRootPane().setDefaultButton(generateButton);
        return buttonPanel;
    }

    private JPanel createControlPanel() {
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rebuildControls();
        return controlPanel;
    }

    protected final void rebuildControls() {
        controlPanel.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 2, 0);

        addSection(constraints, "Seed");
        addRow(constraints, createSeedPanel());
        buildControls(constraints);

        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;
        controlPanel.add(Box.createGlue(), constraints);
        controlPanel.revalidate();
        controlPanel.repaint();
        schedulePreview();
    }

    private JPanel createSeedPanel() {
        JPanel seedPanel = new JPanel(new BorderLayout(4, 0));
        seedField.setColumns(10);
        updatingSeedField = true;
        seedField.setText(String.valueOf(getSeed()));
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
                setSeed(new Random().nextLong());
                updatingSeedField = true;
                seedField.setText(String.valueOf(getSeed()));
                updatingSeedField = false;
                onSeedChanged();
                schedulePreview();
            }
        });
        seedPanel.add(randomizeButton, BorderLayout.EAST);
        return seedPanel;
    }

    /**
     * Called after the seed changes. A subclass whose controls show what the seed
     * chose for it overrides this to refresh them.
     */
    protected void onSeedChanged() {
        // Nothing by default.
    }

    // ------------------------------------------------------------------
    // Control helpers for subclasses
    // ------------------------------------------------------------------

    protected final void addSection(GridBagConstraints constraints, String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        controlPanel.add(label, constraints);
        constraints.gridy++;
    }

    protected final void addRow(GridBagConstraints constraints, Component row) {
        controlPanel.add(row, constraints);
        constraints.gridy++;
    }

    /** A labelled row holding one control, laid out like the slider rows. */
    protected final void addLabelledRow(GridBagConstraints constraints, String label, JComponent control) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setPreferredSize(new Dimension(LABEL_WIDTH, nameLabel.getPreferredSize().height));
        row.add(nameLabel, BorderLayout.WEST);
        row.add(control, BorderLayout.CENTER);
        addRow(constraints, row);
    }

    protected final void addPercentSlider(GridBagConstraints constraints, String label, double value,
            DoubleConsumer setter) {
        addSlider(constraints, label, 0, 100, (int) Math.round(value * 100d), percentLabel(),
            sliderValue -> setter.accept(sliderValue / 100d));
    }

    protected static IntFunction<String> percentLabel() {
        return value -> value + "%";
    }

    protected static IntFunction<String> pixelLabel() {
        return value -> value + " px";
    }

    protected final void addSlider(GridBagConstraints constraints, String label, int min, int max,
            int value, IntFunction<String> valueLabelText, DoubleConsumer setter) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setPreferredSize(new Dimension(LABEL_WIDTH, nameLabel.getPreferredSize().height));
        row.add(nameLabel, BorderLayout.WEST);
        JSlider slider = new JSlider(min, max, Math.max(min, Math.min(max, value)));
        slider.setPreferredSize(new Dimension(136, slider.getPreferredSize().height));
        row.add(slider, BorderLayout.CENTER);
        JLabel valueLabel = new JLabel(valueLabelText.apply(slider.getValue()));
        valueLabel.setPreferredSize(new Dimension(56, valueLabel.getPreferredSize().height));
        row.add(valueLabel, BorderLayout.EAST);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                valueLabel.setText(valueLabelText.apply(slider.getValue()));
                setter.accept(slider.getValue());
                schedulePreview();
            }
        });
        addRow(constraints, row);
    }

    /** A drop-down of choices, named by the given function. */
    protected final <T> void addChoice(GridBagConstraints constraints, String label, T[] choices,
            T selected, final Function<T, String> naming, final Consumer<T> setter) {
        final JComboBox<T> comboBox = new JComboBox<T>(choices);
        comboBox.setSelectedItem(selected);
        comboBox.setRenderer(new DefaultListCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                @SuppressWarnings("unchecked")
                T typed = (T) value;
                // Named even when it is null: a null choice is a real one here, and
                // it is how "let the seed decide" is offered.
                setText(naming.apply(typed));
                return this;
            }
        });
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                @SuppressWarnings("unchecked")
                T typed = (T) comboBox.getSelectedItem();
                setter.accept(typed);
                schedulePreview();
            }
        });
        addLabelledRow(constraints, label, comboBox);
    }

    protected final void addCheckBox(GridBagConstraints constraints, String label, String text,
            boolean value, final Consumer<Boolean> setter) {
        final JCheckBox checkBox = new JCheckBox(text, value);
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setter.accept(Boolean.valueOf(checkBox.isSelected()));
                schedulePreview();
            }
        });
        addLabelledRow(constraints, label, checkBox);
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
            setSeed(Long.parseLong(text));
            onSeedChanged();
            schedulePreview();
        } catch (NumberFormatException ex) {
            // Keep the previous seed while the field holds something unparsable.
        }
    }

    // ------------------------------------------------------------------
    // Preview
    // ------------------------------------------------------------------

    protected final void schedulePreview() {
        previewTimer.restart();
    }

    private void refreshPreview() {
        final int requestId = ++previewRequestId;
        final PreviewRenderer renderer = snapshotRenderer();
        final int width = Math.max(1, previewPanel.getWidth());
        final int height = Math.max(1, previewPanel.getHeight());
        final boolean wholeShape = wholeShapePreview;
        final double scale = wholeShape
            ? Math.min(width / (double) Math.max(1, shapeBounds.width),
                height / (double) Math.max(1, shapeBounds.height))
            : 1d;
        final double viewX = wholeShape ? shapeBounds.x
            : clampView(previewCenterX, width, shapeBounds.x, shapeBounds.width);
        final double viewY = wholeShape ? shapeBounds.y
            : clampView(previewCenterY, height, shapeBounds.y, shapeBounds.height);
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return renderer.render(viewX, viewY,
                    wholeShape ? (int) Math.round(shapeBounds.width * scale) : width,
                    wholeShape ? (int) Math.round(shapeBounds.height * scale) : height,
                    scale);
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

    /** Keeps the 1:1 window over the shape, so panning cannot wander off into empty canvas. */
    private static double clampView(double center, int viewSize, int boundsStart, int boundsSize) {
        double half = viewSize / 2d;
        double clampedCenter = Math.max(boundsStart + Math.min(half, boundsSize / 2d),
            Math.min(boundsStart + Math.max(boundsSize - half, boundsSize / 2d), center));
        return clampedCenter - half;
    }

    /** Preview surface; in detail mode dragging it pans over the generated surface. */
    private final class PreviewPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private transient BufferedImage image;
        private int dragOriginX;
        private int dragOriginY;
        private double dragCenterX;
        private double dragCenterY;

        private PreviewPanel() {
            setBackground(new Color(24, 24, 24));
            setToolTipText("Drag to look at another part of it");
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
            if (wholeShapePreview) {
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
