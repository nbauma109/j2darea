package com.github.nbauma109.j2darea;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

public class DoorEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final Dimension ICON_BUTTON_SIZE = new Dimension(22, 22);

    interface Listener {
        void onToggleDoorPolygons(boolean selected);
        void onToggleImpededBlocks(boolean selected);
        void onDoorDataChanged(DoorData doorData);
        void onEditFlags();
        void onEditRegionLink();
        void onSaveEntrance(String name, int orientation, Point entrancePoint);
        void onEraseEntrance();
        void onSaveTravelRegion(String entranceName, Polygon exitPolygon);
        void onEraseTravelRegion();
    }

    private enum EditMode {
        NONE,
        OPEN_POLYGON,
        CLOSED_POLYGON,
        OPEN_IMPEDED,
        CLOSED_IMPEDED,
        SET_LAUNCH_POINT,
        SET_OPEN_LOCATION_FRONT,
        SET_OPEN_LOCATION_BACK
    }

    private static final class RowComponents {
        final JLabel valueLabel;
        final JButton editButton;

        RowComponents(JLabel valueLabel, JButton editButton) {
            this.valueLabel = valueLabel;
            this.editButton = editButton;
        }
    }

    private final Listener listener;
    private final JCheckBox showDoorPolygonsCheckBox;
    private final JCheckBox showImpededBlocksCheckBox;
    private final JButton editOpenedPolygonButton;
    private final JButton editClosedPolygonButton;
    private final JButton editOpenedImpededButton;
    private final JButton editClosedImpededButton;
    private final JLabel openedPolygonLabel;
    private final JLabel closedPolygonLabel;
    private final JLabel openedImpededLabel;
    private final JLabel closedImpededLabel;
    private final JLabel launchPointLabel;
    private final JLabel openFrontLabel;
    private final JLabel openBackLabel;
    private final JLabel flagsLabel;
    private final JLabel regionLinkLabel;
    private final JLabel modeLabelLine1;
    private final JLabel modeLabelLine2;
    private final JLabel modeLabelLine3;
    private final PreviewPanel previewPanel;

    private final ImageIcon pencilIcon;
    private final ImageIcon eraserIcon;
    private final ImageIcon checkmarkIcon;

    private DoorData currentDoorData = new DoorData();
    private BufferedImage openedDoorImage;
    private Point openedDoorLocation = new Point();
    private boolean showDoorPolygons = true;
    private boolean showImpededBlocks = true;
    private EditMode editMode = EditMode.NONE;
    private boolean hasPendingDoorDataChanges;
    private boolean inEntranceEditMode;
    private boolean inTravelRegionEditMode;
    private String pendingEntranceName;
    private int pendingEntranceOrientation;
    private Point pendingEntrancePoint;
    private Polygon pendingExitPolygon = new Polygon();
    private JComboBox<Integer> entranceOrientationCombo;
    private JLabel entranceSummaryLabel;
    private JLabel travelRegionSummaryLabel;
    private JPanel entranceEditPanel;
    private JPanel travelRegionEditPanel;
    private JLabel entranceLabel;
    private JLabel travelRegionLabel;
    private Point lastEntrancePoint;
    private Polygon lastExitPolygon;
    private int lastEntranceOrientation;
    private String lastEntranceName = "";
    private String suggestedEntranceName = "Entrance";

    public DoorEditorDialog(Frame owner, Listener listener) {
        super(owner, "Edit Door", false);
        this.listener = listener;
        this.pencilIcon = new ImageIcon(getClass().getResource("/icons/pencil.png"));
        this.eraserIcon = new ImageIcon(getClass().getResource("/icons/eraser.png"));
        this.checkmarkIcon = createCheckmarkIcon();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        previewPanel = new PreviewPanel();
        previewPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 6));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;

        modeLabelLine1 = new JLabel("Preview mode");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        contentPanel.add(modeLabelLine1, gbc);

        row++;
        modeLabelLine2 = new JLabel(" ");
        gbc.gridy = row;
        contentPanel.add(modeLabelLine2, gbc);

        row++;
        modeLabelLine3 = new JLabel(" ");
        gbc.gridy = row;
        contentPanel.add(modeLabelLine3, gbc);

        row++;
        showDoorPolygonsCheckBox = new JCheckBox("Show Door Polygons");
        showDoorPolygonsCheckBox.addActionListener(e -> {
            flushPendingDoorDataChanges();
            showDoorPolygons = showDoorPolygonsCheckBox.isSelected();
            this.listener.onToggleDoorPolygons(showDoorPolygons);
            refreshUi();
        });
        gbc.gridy = row;
        gbc.gridwidth = 2;
        contentPanel.add(showDoorPolygonsCheckBox, gbc);

        row++;
        showImpededBlocksCheckBox = new JCheckBox("Show Impeded Blocks");
        showImpededBlocksCheckBox.addActionListener(e -> {
            flushPendingDoorDataChanges();
            showImpededBlocks = showImpededBlocksCheckBox.isSelected();
            this.listener.onToggleImpededBlocks(showImpededBlocks);
            refreshUi();
        });
        gbc.gridy = row;
        contentPanel.add(showImpededBlocksCheckBox, gbc);

        gbc.gridwidth = 1;
        row++;
        RowComponents openPolyRow = addToggleRow(contentPanel, gbc, row,
            "Opened Door Polygon:",
            e -> togglePolygonMode(EditMode.OPEN_POLYGON),
            e -> deleteOpenPolygon());
        openedPolygonLabel = openPolyRow.valueLabel;
        editOpenedPolygonButton = openPolyRow.editButton;

        row++;
        RowComponents closedPolyRow = addToggleRow(contentPanel, gbc, row,
            "Closed Door Polygon:",
            e -> togglePolygonMode(EditMode.CLOSED_POLYGON),
            e -> deleteClosedPolygon());
        closedPolygonLabel = closedPolyRow.valueLabel;
        editClosedPolygonButton = closedPolyRow.editButton;

        row++;
        RowComponents openImpRow = addToggleRow(contentPanel, gbc, row,
            "Opened Door Impeded Blocks:",
            e -> toggleImpededMode(EditMode.OPEN_IMPEDED),
            e -> deleteOpenImpeded());
        openedImpededLabel = openImpRow.valueLabel;
        editOpenedImpededButton = openImpRow.editButton;

        row++;
        RowComponents closedImpRow = addToggleRow(contentPanel, gbc, row,
            "Closed Door Impeded Blocks:",
            e -> toggleImpededMode(EditMode.CLOSED_IMPEDED),
            e -> deleteClosedImpeded());
        closedImpededLabel = closedImpRow.valueLabel;
        editClosedImpededButton = closedImpRow.editButton;

        row++;
        launchPointLabel = addActionRow(contentPanel, gbc, row,
            "Launch Point:",
            e -> startPointMode(EditMode.SET_LAUNCH_POINT),
            e -> deleteLaunchPoint());

        row++;
        openFrontLabel = addActionRow(contentPanel, gbc, row,
            "Open Location Front:",
            e -> startPointMode(EditMode.SET_OPEN_LOCATION_FRONT),
            e -> deleteOpenLocationFront());

        row++;
        openBackLabel = addActionRow(contentPanel, gbc, row,
            "Open Location Back:",
            e -> startPointMode(EditMode.SET_OPEN_LOCATION_BACK),
            e -> deleteOpenLocationBack());

        row++;
        flagsLabel = addActionRow(contentPanel, gbc, row,
            "Flags:",
            e -> this.listener.onEditFlags(),
            e -> deleteFlags());

        row++;
        regionLinkLabel = addActionRow(contentPanel, gbc, row,
            "Region Link:",
            e -> this.listener.onEditRegionLink(),
            e -> deleteRegionLink());

        row++;
        entranceLabel = addActionRow(contentPanel, gbc, row,
            "Entrance:",
            e -> startEditEntrance(),
            e -> {
                listener.onEraseEntrance();
                lastEntranceName = "";
                lastEntrancePoint = null;
                refreshUi();
            });

        row++;
        travelRegionLabel = addActionRow(contentPanel, gbc, row,
            "Travel Region:",
            e -> startEditTravelRegion(),
            e -> {
                listener.onEraseTravelRegion();
                lastExitPolygon = null;
                refreshUi();
            });

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setPreferredSize(new Dimension(420, 520));
        formWrapper.add(contentPanel, BorderLayout.NORTH);
        entranceEditPanel = buildEntranceEditPanel();
        entranceEditPanel.setVisible(false);
        travelRegionEditPanel = buildTravelRegionEditPanel();
        travelRegionEditPanel.setVisible(false);
        JPanel bottomCardsPanel = new JPanel(new BorderLayout());
        bottomCardsPanel.add(entranceEditPanel, BorderLayout.NORTH);
        bottomCardsPanel.add(travelRegionEditPanel, BorderLayout.CENTER);
        formWrapper.add(bottomCardsPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewPanel, formWrapper);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setResizeWeight(0.72d);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setSize(Math.max(1100, getWidth()), Math.max(760, getHeight()));
        setMinimumSize(new Dimension(980, 700));
        configureKeyBindings();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                flushPendingDoorDataChanges();
            }
        });
        refreshUi();
    }

    @Override
    public void dispose() {
        flushPendingDoorDataChanges();
        super.dispose();
    }

    public void setDoorState(DoorData doorData, BufferedImage openedDoorImage, Point openedDoorLocation,
            boolean showDoorPolygons, boolean showImpededBlocks) {
        currentDoorData = doorData != null ? doorData.copy() : new DoorData();
        this.openedDoorImage = openedDoorImage;
        this.openedDoorLocation = openedDoorLocation != null ? new Point(openedDoorLocation) : new Point();
        this.showDoorPolygons = showDoorPolygons;
        this.showImpededBlocks = showImpededBlocks;
        hasPendingDoorDataChanges = false;
        refreshUi();
    }

    public void setLastEntranceState(String name, Point entrancePoint, Polygon exitPolygon, int orientation) {
        lastEntranceName = name != null ? name : "";
        lastEntrancePoint = entrancePoint != null ? new Point(entrancePoint) : null;
        lastExitPolygon = (exitPolygon != null && exitPolygon.npoints > 0)
            ? PolygonUtils.clonePolygon(exitPolygon) : null;
        lastEntranceOrientation = orientation;
        previewPanel.repaint();
    }

    public void setSuggestedEntranceName(String name) {
        suggestedEntranceName = name != null && !name.trim().isEmpty() ? name.trim() : "Entrance";
    }

    private RowComponents addToggleRow(JPanel panel, GridBagConstraints gbc, int row, String labelText,
            java.awt.event.ActionListener editDoneAction, java.awt.event.ActionListener deleteAction) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel valueLabel = new JLabel();
        panel.add(valueLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        buttonPanel.setOpaque(false);

        JButton editButton = new JButton(pencilIcon);
        editButton.setToolTipText("Edit");
        editButton.addActionListener(e -> {
            flushPendingDoorDataChanges();
            editDoneAction.actionPerformed(e);
        });
        configureIconButton(editButton);
        buttonPanel.add(editButton);

        JButton eraseButton = new JButton(eraserIcon);
        eraseButton.setToolTipText("Clear");
        eraseButton.addActionListener(e -> {
            flushPendingDoorDataChanges();
            deleteAction.actionPerformed(e);
        });
        configureIconButton(eraseButton);
        buttonPanel.add(eraseButton);

        panel.add(buttonPanel, gbc);
        return new RowComponents(valueLabel, editButton);
    }

    private JLabel addActionRow(JPanel panel, GridBagConstraints gbc, int row, String labelText,
            java.awt.event.ActionListener editAction, java.awt.event.ActionListener deleteAction) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel valueLabel = new JLabel();
        panel.add(valueLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        buttonPanel.setOpaque(false);

        JButton editButton = new JButton(pencilIcon);
        editButton.setToolTipText("Edit");
        editButton.addActionListener(e -> {
            flushPendingDoorDataChanges();
            editAction.actionPerformed(e);
        });
        configureIconButton(editButton);
        buttonPanel.add(editButton);

        if (deleteAction != null) {
            JButton eraseButton = new JButton(eraserIcon);
            eraseButton.setToolTipText("Clear");
            eraseButton.addActionListener(e -> {
                flushPendingDoorDataChanges();
                deleteAction.actionPerformed(e);
            });
            configureIconButton(eraseButton);
            buttonPanel.add(eraseButton);
        }

        panel.add(buttonPanel, gbc);
        return valueLabel;
    }

    private void configureIconButton(JButton button) {
        button.setFocusable(false);
        button.setMargin(new Insets(1, 1, 1, 1));
        button.setPreferredSize(ICON_BUTTON_SIZE);
        button.setMinimumSize(ICON_BUTTON_SIZE);
        button.setMaximumSize(ICON_BUTTON_SIZE);
    }

    private static ImageIcon createCheckmarkIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 180, 60));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(2, 8, 6, 13);
        g.drawLine(6, 13, 14, 3);
        g.dispose();
        return new ImageIcon(img);
    }

    // Entrance / Travel Region modes ---------------------------------------

    private void startEditEntrance() {
        if (lastEntranceName.isEmpty()) {
            String name = javax.swing.JOptionPane.showInputDialog(DoorEditorDialog.this, "Enter entrance name:", suggestedEntranceName);
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            lastEntranceName = name.trim();
        }
        if (inTravelRegionEditMode) {
            exitTravelRegionEditMode();
        }
        if (editMode != EditMode.NONE) {
            flushPendingDoorDataChanges();
            editMode = EditMode.NONE;
        }
        pendingEntranceName = lastEntranceName;
        pendingEntrancePoint = lastEntrancePoint != null ? new Point(lastEntrancePoint) : null;
        pendingEntranceOrientation = lastEntranceOrientation;
        inEntranceEditMode = true;
        if (entranceOrientationCombo != null) {
            entranceOrientationCombo.setSelectedItem(pendingEntranceOrientation);
        }
        updateEntranceSummary();
        entranceEditPanel.setVisible(true);
        travelRegionEditPanel.setVisible(false);
        refreshUi();
    }

    private void startEditTravelRegion() {
        if (lastEntranceName.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(DoorEditorDialog.this, "Please create an entrance first.");
            return;
        }
        if (inEntranceEditMode) {
            if (pendingEntranceName != null && pendingEntrancePoint != null) {
                flushPendingDoorDataChanges();
                listener.onSaveEntrance(pendingEntranceName, pendingEntranceOrientation, new Point(pendingEntrancePoint));
                lastEntranceName = pendingEntranceName;
                lastEntrancePoint = new Point(pendingEntrancePoint);
                lastEntranceOrientation = pendingEntranceOrientation;
            }
            exitEntranceEditMode();
        }
        if (editMode != EditMode.NONE) {
            flushPendingDoorDataChanges();
            editMode = EditMode.NONE;
        }
        pendingExitPolygon = lastExitPolygon != null ? PolygonUtils.clonePolygon(lastExitPolygon) : new Polygon();
        inTravelRegionEditMode = true;
        updateTravelRegionSummary();
        travelRegionEditPanel.setVisible(true);
        entranceEditPanel.setVisible(false);
        refreshUi();
    }

    // Delete actions -------------------------------------------------------

    private void deleteOpenPolygon() {
        if (editMode == EditMode.OPEN_POLYGON) {
            editMode = EditMode.NONE;
        }
        currentDoorData.setOpenPolygon(new Polygon());
        commitAndFlush();
    }

    private void deleteClosedPolygon() {
        if (editMode == EditMode.CLOSED_POLYGON) {
            editMode = EditMode.NONE;
        }
        currentDoorData.setClosedPolygon(new Polygon());
        commitAndFlush();
    }

    private void deleteOpenImpeded() {
        if (editMode == EditMode.OPEN_IMPEDED) {
            editMode = EditMode.NONE;
        }
        currentDoorData.setOpenImpededCells(new ArrayList<>());
        commitAndFlush();
    }

    private void deleteClosedImpeded() {
        if (editMode == EditMode.CLOSED_IMPEDED) {
            editMode = EditMode.NONE;
        }
        currentDoorData.setClosedImpededCells(new ArrayList<>());
        commitAndFlush();
    }

    private void deleteLaunchPoint() {
        currentDoorData.setLaunchPoint(new Point());
        commitAndFlush();
    }

    private void deleteOpenLocationFront() {
        currentDoorData.setOpenLocationFront(new Point());
        commitAndFlush();
    }

    private void deleteOpenLocationBack() {
        currentDoorData.setOpenLocationBack(new Point());
        commitAndFlush();
    }

    private void deleteFlags() {
        currentDoorData.setFlags(0);
        commitAndFlush();
    }

    private void deleteRegionLink() {
        currentDoorData.setRegionLinkName("");
        currentDoorData.setFlags(currentDoorData.getFlags() & ~DoorExportSupport.LINKED_FLAG);
        commitAndFlush();
    }

    // ----------------------------------------------------------------------

    private JPanel buildEntranceEditPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY), "Edit Entrance"),
            BorderFactory.createEmptyBorder(4, 8, 8, 8)
        ));

        JPanel orientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        orientPanel.add(new JLabel("Orientation:"));
        Integer[] orientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            orientations[i] = i;
        }
        entranceOrientationCombo = new JComboBox<>(orientations);
        entranceOrientationCombo.setSelectedIndex(0);
        entranceOrientationCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    int o = (Integer) value;
                    setText(o + " (" + DirectionMarker.getOrientationName(o) + ")");
                }
                return this;
            }
        });
        entranceOrientationCombo.addActionListener(e -> {
            Object sel = entranceOrientationCombo.getSelectedItem();
            pendingEntranceOrientation = sel instanceof Integer ? (Integer) sel : 0;
            updateEntranceSummary();
            previewPanel.repaint();
        });
        orientPanel.add(entranceOrientationCombo);
        orientPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, orientPanel.getPreferredSize().height));
        panel.add(orientPanel);
        panel.add(javax.swing.Box.createVerticalStrut(4));

        entranceSummaryLabel = new JLabel(" ");
        entranceSummaryLabel.setAlignmentX(0f);
        panel.add(entranceSummaryLabel);
        panel.add(javax.swing.Box.createVerticalStrut(6));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton confirmBtn = new JButton("Confirm");
        confirmBtn.addActionListener(e -> {
            if (pendingEntrancePoint == null) {
                javax.swing.JOptionPane.showMessageDialog(DoorEditorDialog.this, "Please pick an entrance point first.");
                return;
            }
            flushPendingDoorDataChanges();
            listener.onSaveEntrance(pendingEntranceName, pendingEntranceOrientation, new Point(pendingEntrancePoint));
            lastEntranceName = pendingEntranceName;
            lastEntrancePoint = new Point(pendingEntrancePoint);
            lastEntranceOrientation = pendingEntranceOrientation;
            exitEntranceEditMode();
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> exitEntranceEditMode());
        actionPanel.add(confirmBtn);
        actionPanel.add(cancelBtn);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, actionPanel.getPreferredSize().height));
        panel.add(actionPanel);

        return panel;
    }

    private JPanel buildTravelRegionEditPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY), "Edit Travel Region"),
            BorderFactory.createEmptyBorder(4, 8, 8, 8)
        ));

        JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton clearBtn = new JButton("Clear Polygon");
        clearBtn.addActionListener(e -> {
            pendingExitPolygon = new Polygon();
            updateTravelRegionSummary();
            previewPanel.repaint();
        });
        JButton undoBtn = new JButton("Undo Vertex");
        undoBtn.addActionListener(e -> {
            if (pendingExitPolygon.npoints > 0) {
                pendingExitPolygon = copyWithoutLastVertex(pendingExitPolygon);
                updateTravelRegionSummary();
                previewPanel.repaint();
            }
        });
        editButtonPanel.add(clearBtn);
        editButtonPanel.add(undoBtn);
        editButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, editButtonPanel.getPreferredSize().height));
        panel.add(editButtonPanel);
        panel.add(javax.swing.Box.createVerticalStrut(4));

        travelRegionSummaryLabel = new JLabel(" ");
        travelRegionSummaryLabel.setAlignmentX(0f);
        panel.add(travelRegionSummaryLabel);
        panel.add(javax.swing.Box.createVerticalStrut(6));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton confirmBtn = new JButton("Confirm");
        confirmBtn.addActionListener(e -> confirmTravelRegion());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> exitTravelRegionEditMode());
        actionPanel.add(confirmBtn);
        actionPanel.add(cancelBtn);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, actionPanel.getPreferredSize().height));
        panel.add(actionPanel);

        return panel;
    }

    private void exitEntranceEditMode() {
        inEntranceEditMode = false;
        pendingEntranceName = null;
        pendingEntrancePoint = null;
        pendingEntranceOrientation = 0;
        if (entranceEditPanel != null) {
            entranceEditPanel.setVisible(false);
        }
        refreshUi();
    }

    private void confirmTravelRegion() {
        if (pendingExitPolygon == null || pendingExitPolygon.npoints < 3) {
            javax.swing.JOptionPane.showMessageDialog(DoorEditorDialog.this, "Please draw a polygon with at least 3 vertices.");
            return;
        }
        flushPendingDoorDataChanges();
        listener.onSaveTravelRegion(lastEntranceName, PolygonUtils.clonePolygon(pendingExitPolygon));
        lastExitPolygon = PolygonUtils.clonePolygon(pendingExitPolygon);
        exitTravelRegionEditMode();
    }

    private void exitTravelRegionEditMode() {
        inTravelRegionEditMode = false;
        pendingExitPolygon = new Polygon();
        if (travelRegionEditPanel != null) {
            travelRegionEditPanel.setVisible(false);
        }
        refreshUi();
    }

    private void updateEntranceSummary() {
        if (entranceSummaryLabel == null) {
            return;
        }
        String pointStr = pendingEntrancePoint != null
            ? pendingEntrancePoint.x + ", " + pendingEntrancePoint.y
            : "(unset)";
        String orientStr = DirectionMarker.getOrientationName(pendingEntranceOrientation);
        entranceSummaryLabel.setText("Point: " + pointStr
            + "  |  Orientation: " + pendingEntranceOrientation + " (" + orientStr + ")");
    }

    private void updateTravelRegionSummary() {
        if (travelRegionSummaryLabel == null) {
            return;
        }
        String polygonStr = pendingExitPolygon != null && pendingExitPolygon.npoints > 0
            ? pendingExitPolygon.npoints + " vertices"
            : "(none)";
        travelRegionSummaryLabel.setText("Exit polygon: " + polygonStr);
    }

    private void togglePolygonMode(EditMode targetMode) {
        if (editMode == targetMode) {
            finishPolygonEdit();
        } else {
            flushPendingDoorDataChanges();
            editMode = targetMode;
            refreshUi();
        }
    }

    private void finishPolygonEdit() {
        autoComputeImpededCellsFromPolygon(editMode);
        flushPendingDoorDataChanges();
        editMode = EditMode.NONE;
        refreshUi();
    }

    private void autoComputeImpededCellsFromPolygon(EditMode polygonMode) {
        if (polygonMode == EditMode.OPEN_POLYGON) {
            List<Point> cells = computeImpededCellsFromPolygon(currentDoorData.getOpenPolygon());
            currentDoorData.setOpenImpededCells(cells);
            hasPendingDoorDataChanges = true;
        } else if (polygonMode == EditMode.CLOSED_POLYGON) {
            List<Point> cells = computeImpededCellsFromPolygon(currentDoorData.getClosedPolygon());
            currentDoorData.setClosedImpededCells(cells);
            hasPendingDoorDataChanges = true;
        }
    }

    static List<Point> computeImpededCellsFromPolygon(Polygon polygon) {
        List<Point> cells = new ArrayList<>();
        if (polygon == null || polygon.npoints < 3) {
            return cells;
        }
        Rectangle bounds = polygon.getBounds();
        int minCx = bounds.x / SearchMapData.CELL_WIDTH;
        int minCy = bounds.y / SearchMapData.CELL_HEIGHT;
        int maxCx = (bounds.x + bounds.width) / SearchMapData.CELL_WIDTH;
        int maxCy = (bounds.y + bounds.height) / SearchMapData.CELL_HEIGHT;
        for (int cy = minCy; cy <= maxCy; cy++) {
            for (int cx = minCx; cx <= maxCx; cx++) {
                int centerX = cx * SearchMapData.CELL_WIDTH + SearchMapData.CELL_WIDTH / 2;
                int centerY = cy * SearchMapData.CELL_HEIGHT + SearchMapData.CELL_HEIGHT / 2;
                if (polygon.contains(centerX, centerY)) {
                    cells.add(new Point(cx, cy));
                }
            }
        }
        return cells;
    }

    private void toggleImpededMode(EditMode targetMode) {
        flushPendingDoorDataChanges();
        if (editMode == targetMode) {
            editMode = EditMode.NONE;
        } else {
            editMode = targetMode;
        }
        refreshUi();
    }

    private void startPointMode(EditMode targetMode) {
        flushPendingDoorDataChanges();
        editMode = targetMode;
        refreshUi();
    }

    private void refreshUi() {
        showDoorPolygonsCheckBox.setSelected(showDoorPolygons);
        showImpededBlocksCheckBox.setSelected(showImpededBlocks);
        openedPolygonLabel.setText(formatPolygonSummary(currentDoorData.getOpenPolygon()));
        closedPolygonLabel.setText(formatPolygonSummary(currentDoorData.getClosedPolygon()));
        openedImpededLabel.setText(formatImpededSummary(currentDoorData.getOpenImpededCells().size()));
        closedImpededLabel.setText(formatImpededSummary(currentDoorData.getClosedImpededCells().size()));
        launchPointLabel.setText(formatPointSummary(currentDoorData.getLaunchPoint()));
        openFrontLabel.setText(formatPointSummary(currentDoorData.getOpenLocationFront()));
        openBackLabel.setText(formatPointSummary(currentDoorData.getOpenLocationBack()));
        flagsLabel.setText(String.valueOf(currentDoorData.getFlags()));
        regionLinkLabel.setText(trimToEmpty(currentDoorData.getRegionLinkName()).isEmpty()
            ? "(none)"
            : trimToEmpty(currentDoorData.getRegionLinkName()));

        if (entranceLabel != null) {
            if (!lastEntranceName.isEmpty() && lastEntrancePoint != null) {
                entranceLabel.setText(lastEntranceName + " (" + lastEntrancePoint.x + ", " + lastEntrancePoint.y + ")");
            } else if (!lastEntranceName.isEmpty()) {
                entranceLabel.setText(lastEntranceName);
            } else {
                entranceLabel.setText("(none)");
            }
        }
        if (travelRegionLabel != null) {
            if (lastExitPolygon != null && lastExitPolygon.npoints > 0) {
                travelRegionLabel.setText(lastExitPolygon.npoints + " vertices");
            } else {
                travelRegionLabel.setText("(none)");
            }
        }

        updateToggleButton(editOpenedPolygonButton, editMode == EditMode.OPEN_POLYGON);
        updateToggleButton(editClosedPolygonButton, editMode == EditMode.CLOSED_POLYGON);
        updateToggleButton(editOpenedImpededButton, editMode == EditMode.OPEN_IMPEDED);
        updateToggleButton(editClosedImpededButton, editMode == EditMode.CLOSED_IMPEDED);

        String[] modeLabelLines = buildModeLabelLines();
        modeLabelLine1.setText(modeLabelLines[0]);
        modeLabelLine2.setText(modeLabelLines[1]);
        modeLabelLine3.setText(modeLabelLines[2]);

        boolean crosshair = editMode != EditMode.NONE || inEntranceEditMode || inTravelRegionEditMode;
        previewPanel.setCursor(crosshair ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
        previewPanel.repaint();
    }

    private void updateToggleButton(JButton button, boolean inEditMode) {
        if (inEditMode) {
            button.setIcon(checkmarkIcon);
            button.setToolTipText("Done");
        } else {
            button.setIcon(pencilIcon);
            button.setToolTipText("Edit");
        }
    }

    private String[] buildModeLabelLines() {
        if (inEntranceEditMode) {
            return new String[] {
                "Placing entrance: " + (pendingEntranceName != null ? pendingEntranceName : ""),
                "Click on the preview to place the entrance point.",
                "Right click or Esc to cancel."
            };
        }
        if (inTravelRegionEditMode) {
            return new String[] {
                "Drawing travel region polygon (entrance: " + lastEntranceName + ")",
                "Click to add vertices.  Click near first vertex or right-click to close.",
                "Backspace removes last vertex.  Esc to cancel."
            };
        }
        switch (editMode) {
            case OPEN_POLYGON:
                return new String[] {
                    "Editing opened door polygon",
                    "Click to add vertices. Backspace removes the last one.",
                    "Right click, Enter, or Esc finishes the current edit."
                };
            case CLOSED_POLYGON:
                return new String[] {
                    "Editing closed door polygon",
                    "Click to add vertices. Backspace removes the last one.",
                    "Right click, Enter, or Esc finishes the current edit."
                };
            case OPEN_IMPEDED:
                return new String[] {
                    "Editing opened door impeded blocks",
                    "Left click or drag to toggle cells.",
                    "Enter or Esc finishes the current edit."
                };
            case CLOSED_IMPEDED:
                return new String[] {
                    "Editing closed door impeded blocks",
                    "Left click or drag to toggle cells.",
                    "Enter or Esc finishes the current edit."
                };
            case SET_LAUNCH_POINT:
                return new String[] {
                    "Setting Launch Point",
                    "Click on the preview to place the launch point.",
                    "Right click or Esc to cancel."
                };
            case SET_OPEN_LOCATION_FRONT:
                return new String[] {
                    "Setting Open Location Front",
                    "Click on the preview to place the open location (front).",
                    "Right click or Esc to cancel."
                };
            case SET_OPEN_LOCATION_BACK:
                return new String[] {
                    "Setting Open Location Back",
                    "Click on the preview to place the open location (back).",
                    "Right click or Esc to cancel."
                };
            default:
                return new String[] { "Preview mode", " ", " " };
        }
    }

    private String formatPolygonSummary(Polygon polygon) {
        if (polygon == null || polygon.npoints < 3) {
            return "(not set)";
        }
        return polygon.npoints + " vertices";
    }

    private String formatImpededSummary(int count) {
        return count == 0 ? "(none)" : count + " cells";
    }

    private String formatPointSummary(Point point) {
        Point safePoint = point != null ? point : new Point();
        return safePoint.x + ", " + safePoint.y;
    }

    private void commitDoorDataChange() {
        hasPendingDoorDataChanges = true;
        refreshUi();
    }

    private void commitAndFlush() {
        hasPendingDoorDataChanges = true;
        flushPendingDoorDataChanges();
        refreshUi();
    }

    private void flushPendingDoorDataChanges() {
        if (!hasPendingDoorDataChanges) {
            return;
        }
        listener.onDoorDataChanged(currentDoorData.copy());
        hasPendingDoorDataChanges = false;
    }

    private void configureKeyBindings() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "RemoveDoorPolygonVertex");
        getRootPane().getActionMap().put("RemoveDoorPolygonVertex", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (inTravelRegionEditMode) {
                    if (pendingExitPolygon.npoints > 0) {
                        pendingExitPolygon = copyWithoutLastVertex(pendingExitPolygon);
                        updateTravelRegionSummary();
                        previewPanel.repaint();
                    }
                    return;
                }
                if (editMode != EditMode.OPEN_POLYGON && editMode != EditMode.CLOSED_POLYGON) {
                    return;
                }
                Polygon polygon = getEditedPolygon();
                if (polygon.npoints == 0) {
                    return;
                }
                setEditedPolygon(copyWithoutLastVertex(polygon));
                commitDoorDataChange();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "FinishDoorEditMode");
        getRootPane().getActionMap().put("FinishDoorEditMode", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (inEntranceEditMode || inTravelRegionEditMode) {
                    return;
                }
                if (editMode == EditMode.NONE) {
                    return;
                }
                if (editMode == EditMode.OPEN_POLYGON || editMode == EditMode.CLOSED_POLYGON) {
                    finishPolygonEdit();
                } else {
                    flushPendingDoorDataChanges();
                    editMode = EditMode.NONE;
                    refreshUi();
                }
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CancelDoorEditMode");
        getRootPane().getActionMap().put("CancelDoorEditMode", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (inEntranceEditMode) {
                    exitEntranceEditMode();
                    return;
                }
                if (inTravelRegionEditMode) {
                    exitTravelRegionEditMode();
                    return;
                }
                if (editMode == EditMode.NONE) {
                    return;
                }
                if (editMode == EditMode.OPEN_POLYGON || editMode == EditMode.CLOSED_POLYGON) {
                    finishPolygonEdit();
                } else {
                    flushPendingDoorDataChanges();
                    editMode = EditMode.NONE;
                    refreshUi();
                }
            }
        });
    }

    private Polygon copyWithoutLastVertex(Polygon source) {
        if (source == null || source.npoints <= 1) {
            return new Polygon();
        }
        int[] xpoints = new int[source.npoints - 1];
        int[] ypoints = new int[source.npoints - 1];
        System.arraycopy(source.xpoints, 0, xpoints, 0, source.npoints - 1);
        System.arraycopy(source.ypoints, 0, ypoints, 0, source.npoints - 1);
        return new Polygon(xpoints, ypoints, source.npoints - 1);
    }

    private Polygon getEditedPolygon() {
        return editMode == EditMode.CLOSED_POLYGON ? currentDoorData.getClosedPolygon() : currentDoorData.getOpenPolygon();
    }

    private void setEditedPolygon(Polygon polygon) {
        if (editMode == EditMode.CLOSED_POLYGON) {
            currentDoorData.setClosedPolygon(polygon);
        } else {
            currentDoorData.setOpenPolygon(polygon);
        }
    }

    private List<Point> getEditedImpededCells() {
        return editMode == EditMode.CLOSED_IMPEDED ? currentDoorData.getClosedImpededCells() : currentDoorData.getOpenImpededCells();
    }

    private void setEditedImpededCells(List<Point> cells) {
        if (editMode == EditMode.CLOSED_IMPEDED) {
            currentDoorData.setClosedImpededCells(cells);
        } else {
            currentDoorData.setOpenImpededCells(cells);
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private final class PreviewPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private Rectangle imageDrawBounds = new Rectangle();
        private Boolean dragPaintValue;
        private Point currentMouseImagePoint;
        private EditMode draggedMarkerMode;
        private boolean markerDragOccurred;

        private PreviewPanel() {
            setOpaque(true);
            setBackground(new Color(36, 36, 36));

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point imagePoint = toImagePoint(e.getPoint());
                    if (inEntranceEditMode) {
                        handleEntranceMousePressed(imagePoint, e);
                        return;
                    }
                    if (inTravelRegionEditMode) {
                        handleTravelRegionMousePressed(imagePoint, e);
                        return;
                    }
                    if (editMode == EditMode.NONE
                            && (javax.swing.SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger())) {
                        showPreviewContextMenu(e);
                        return;
                    }
                    if (editMode == EditMode.SET_LAUNCH_POINT
                            || editMode == EditMode.SET_OPEN_LOCATION_FRONT
                            || editMode == EditMode.SET_OPEN_LOCATION_BACK) {
                        if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                            flushPendingDoorDataChanges();
                            editMode = EditMode.NONE;
                            refreshUi();
                            return;
                        }
                        if (javax.swing.SwingUtilities.isLeftMouseButton(e) && imagePoint != null) {
                            applyPointPlacement(imagePoint);
                        }
                        return;
                    }
                    if (imagePoint == null) {
                        return;
                    }
                    if (editMode == EditMode.NONE
                            && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        if (isNearMarker(currentDoorData.getLaunchPoint(), imagePoint)) {
                            draggedMarkerMode = EditMode.SET_LAUNCH_POINT;
                            markerDragOccurred = false;
                            return;
                        }
                        if (isNearMarker(currentDoorData.getOpenLocationFront(), imagePoint)) {
                            draggedMarkerMode = EditMode.SET_OPEN_LOCATION_FRONT;
                            markerDragOccurred = false;
                            return;
                        }
                        if (isNearMarker(currentDoorData.getOpenLocationBack(), imagePoint)) {
                            draggedMarkerMode = EditMode.SET_OPEN_LOCATION_BACK;
                            markerDragOccurred = false;
                            return;
                        }
                    }
                    if (editMode == EditMode.OPEN_POLYGON || editMode == EditMode.CLOSED_POLYGON) {
                        handlePolygonPress(imagePoint, e);
                    } else if (editMode == EditMode.OPEN_IMPEDED || editMode == EditMode.CLOSED_IMPEDED) {
                        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                            return;
                        }
                        dragPaintValue = null;
                        applyImpededCell(imagePoint);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedMarkerMode != null && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        Point ip = toImagePoint(e.getPoint());
                        if (ip != null) {
                            markerDragOccurred = true;
                            switch (draggedMarkerMode) {
                                case SET_LAUNCH_POINT: currentDoorData.setLaunchPoint(ip); break;
                                case SET_OPEN_LOCATION_FRONT: currentDoorData.setOpenLocationFront(ip); break;
                                case SET_OPEN_LOCATION_BACK: currentDoorData.setOpenLocationBack(ip); break;
                                default: break;
                            }
                            hasPendingDoorDataChanges = true;
                            repaint();
                        }
                        return;
                    }
                    if (editMode != EditMode.OPEN_IMPEDED && editMode != EditMode.CLOSED_IMPEDED) {
                        return;
                    }
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    Point imagePoint = toImagePoint(e.getPoint());
                    if (imagePoint == null) {
                        return;
                    }
                    applyImpededCell(imagePoint);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedMarkerMode != null && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        if (markerDragOccurred) {
                            flushPendingDoorDataChanges();
                            refreshUi();
                        }
                        draggedMarkerMode = null;
                        markerDragOccurred = false;
                        return;
                    }
                    if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                        dragPaintValue = null;
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean wasNear = isNearAnyMarker();
                    currentMouseImagePoint = toImagePoint(e.getPoint());
                    boolean isNear = isNearAnyMarker();
                    boolean trackPolygon = editMode == EditMode.OPEN_POLYGON || editMode == EditMode.CLOSED_POLYGON
                            || inTravelRegionEditMode;
                    if (editMode == EditMode.NONE && !inEntranceEditMode && !inTravelRegionEditMode) {
                        setCursor(isNear
                            ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                            : Cursor.getDefaultCursor());
                    }
                    if (trackPolygon || wasNear || isNear) {
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    currentMouseImagePoint = null;
                    if (editMode == EditMode.NONE && !inEntranceEditMode && !inTravelRegionEditMode) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(620, 620);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                paintCheckerboard(g2, getWidth(), getHeight());
                if (openedDoorImage == null) {
                    imageDrawBounds = new Rectangle();
                    g2.setColor(Color.WHITE);
                    g2.drawString("No opened-door image", 16, 24);
                    return;
                }
                double scale = Math.min(
                    Math.max(1.0d, getWidth() - 24d) / Math.max(1, openedDoorImage.getWidth()),
                    Math.max(1.0d, getHeight() - 24d) / Math.max(1, openedDoorImage.getHeight())
                );
                int drawWidth = Math.max(1, (int) Math.round(openedDoorImage.getWidth() * scale));
                int drawHeight = Math.max(1, (int) Math.round(openedDoorImage.getHeight() * scale));
                int drawX = (getWidth() - drawWidth) / 2;
                int drawY = (getHeight() - drawHeight) / 2;
                imageDrawBounds = new Rectangle(drawX, drawY, drawWidth, drawHeight);
                g2.drawImage(openedDoorImage, drawX, drawY, drawWidth, drawHeight, null);
                g2.setColor(new Color(255, 255, 255, 160));
                g2.drawRect(drawX, drawY, drawWidth, drawHeight);

                Graphics2D overlay = (Graphics2D) g2.create();
                try {
                    overlay.translate(drawX, drawY);
                    overlay.scale(scale, scale);
                    overlay.translate(-openedDoorLocation.x, -openedDoorLocation.y);
                    paintDoorOverlays(overlay);
                } finally {
                    overlay.dispose();
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintDoorOverlays(Graphics2D g2) {
            if (showDoorPolygons) {
                if (editMode == EditMode.OPEN_POLYGON) {
                    paintPolygonDraft(g2, currentDoorData.getOpenPolygon(), new Color(80, 220, 255, 45), new Color(80, 220, 255));
                } else {
                    paintPolygon(g2, currentDoorData.getOpenPolygon(), new Color(80, 220, 255, 45), new Color(80, 220, 255));
                }
                if (editMode == EditMode.CLOSED_POLYGON) {
                    paintPolygonDraft(g2, currentDoorData.getClosedPolygon(), new Color(255, 180, 0, 35), new Color(255, 180, 0));
                } else {
                    paintPolygon(g2, currentDoorData.getClosedPolygon(), new Color(255, 180, 0, 35), new Color(255, 180, 0));
                }
            }
            if (showImpededBlocks) {
                paintImpededGrid(g2);
                paintImpededCells(g2, currentDoorData.getOpenImpededCells(), new Color(80, 220, 255, 90));
                paintImpededCells(g2, currentDoorData.getClosedImpededCells(), new Color(255, 180, 0, 90));
            }
            boolean markerHoverActive = editMode == EditMode.NONE && !inEntranceEditMode && !inTravelRegionEditMode;
            paintPointMarker(g2, currentDoorData.getLaunchPoint(), new Color(0, 255, 80), "LP",
                markerHoverActive && isNearMarker(currentDoorData.getLaunchPoint()));
            paintPointMarker(g2, currentDoorData.getOpenLocationFront(), new Color(255, 220, 0), "OF",
                markerHoverActive && isNearMarker(currentDoorData.getOpenLocationFront()));
            paintPointMarker(g2, currentDoorData.getOpenLocationBack(), new Color(255, 120, 0), "OB",
                markerHoverActive && isNearMarker(currentDoorData.getOpenLocationBack()));
            if (inEntranceEditMode) {
                paintEntrancePointMarker(g2, pendingEntrancePoint, pendingEntranceOrientation);
                if (lastExitPolygon != null && lastExitPolygon.npoints > 0) {
                    paintPolygon(g2, lastExitPolygon, new Color(0, 255, 128, 40), new Color(0, 255, 128));
                }
            } else if (inTravelRegionEditMode) {
                if (lastEntrancePoint != null) {
                    paintEntrancePointMarker(g2, lastEntrancePoint, lastEntranceOrientation);
                }
                paintPolygonDraft(g2, pendingExitPolygon, new Color(0, 255, 128, 40), new Color(0, 255, 128));
            } else if (lastEntrancePoint != null) {
                paintEntrancePointMarker(g2, lastEntrancePoint, lastEntranceOrientation);
                if (lastExitPolygon != null && lastExitPolygon.npoints > 0) {
                    paintPolygon(g2, lastExitPolygon, new Color(0, 255, 128, 40), new Color(0, 255, 128));
                }
            }
        }

        private void paintPointMarker(Graphics2D g2, Point point, Color color, String label, boolean hovered) {
            if (point == null || (point.x == 0 && point.y == 0)) {
                return;
            }
            int r = 5;
            Color drawColor = hovered
                ? new Color(Math.min(255, color.getRed() + 80), Math.min(255, color.getGreen() + 80), Math.min(255, color.getBlue() + 80))
                : color;
            g2.setColor(drawColor);
            g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
            g2.drawLine(point.x - r, point.y, point.x + r, point.y);
            g2.drawLine(point.x, point.y - r, point.x, point.y + r);
            g2.setFont(g2.getFont().deriveFont(9f));
            g2.drawString(label, point.x + r + 2, point.y - 2);
        }

        private void paintPolygon(Graphics2D g2, Polygon polygon, Color fillColor, Color outlineColor) {
            if (polygon == null || polygon.npoints == 0) {
                return;
            }
            g2.setColor(outlineColor);
            g2.setStroke(new BasicStroke(1.25f));
            if (polygon.npoints >= 3) {
                g2.setColor(fillColor);
                g2.fillPolygon(polygon);
                g2.setColor(outlineColor);
                g2.drawPolygon(polygon);
            } else if (polygon.npoints == 2) {
                g2.drawLine(
                    polygon.xpoints[0],
                    polygon.ypoints[0],
                    polygon.xpoints[1],
                    polygon.ypoints[1]
                );
            }
            for (int i = 0; i < polygon.npoints; i++) {
                g2.fillOval(polygon.xpoints[i] - 2, polygon.ypoints[i] - 2, 4, 4);
            }
        }

        private void paintImpededGrid(Graphics2D g2) {
            if (openedDoorImage == null) {
                return;
            }
            g2.setColor(new Color(255, 255, 255, 65));
            int imageLeft = openedDoorLocation.x;
            int imageTop = openedDoorLocation.y;
            int imageRight = imageLeft + openedDoorImage.getWidth();
            int imageBottom = imageTop + openedDoorImage.getHeight();
            int firstGridX = Math.floorDiv(imageLeft, SearchMapData.CELL_WIDTH) * SearchMapData.CELL_WIDTH;
            int firstGridY = Math.floorDiv(imageTop, SearchMapData.CELL_HEIGHT) * SearchMapData.CELL_HEIGHT;
            for (int x = firstGridX; x <= imageRight; x += SearchMapData.CELL_WIDTH) {
                g2.drawLine(x, imageTop, x, imageBottom);
            }
            for (int y = firstGridY; y <= imageBottom; y += SearchMapData.CELL_HEIGHT) {
                g2.drawLine(imageLeft, y, imageRight, y);
            }
        }

        private void paintImpededCells(Graphics2D g2, List<Point> cells, Color color) {
            g2.setColor(color);
            for (Point cell : cells) {
                if (cell == null) {
                    continue;
                }
                g2.fillRect(
                    cell.x * SearchMapData.CELL_WIDTH,
                    cell.y * SearchMapData.CELL_HEIGHT,
                    SearchMapData.CELL_WIDTH,
                    SearchMapData.CELL_HEIGHT
                );
            }
        }

        private Point toImagePoint(Point panelPoint) {
            if (openedDoorImage == null || imageDrawBounds.width <= 0 || imageDrawBounds.height <= 0 || !imageDrawBounds.contains(panelPoint)) {
                return null;
            }
            double xRatio = (panelPoint.x - imageDrawBounds.x) / (double) imageDrawBounds.width;
            double yRatio = (panelPoint.y - imageDrawBounds.y) / (double) imageDrawBounds.height;
            int imageX = Math.max(0, Math.min(openedDoorImage.getWidth() - 1, (int) Math.floor(xRatio * openedDoorImage.getWidth())));
            int imageY = Math.max(0, Math.min(openedDoorImage.getHeight() - 1, (int) Math.floor(yRatio * openedDoorImage.getHeight())));
            return new Point(imageX + openedDoorLocation.x, imageY + openedDoorLocation.y);
        }

        private void applyPointPlacement(Point imagePoint) {
            switch (editMode) {
                case SET_LAUNCH_POINT:
                    currentDoorData.setLaunchPoint(imagePoint);
                    break;
                case SET_OPEN_LOCATION_FRONT:
                    currentDoorData.setOpenLocationFront(imagePoint);
                    break;
                case SET_OPEN_LOCATION_BACK:
                    currentDoorData.setOpenLocationBack(imagePoint);
                    break;
                default:
                    return;
            }
            hasPendingDoorDataChanges = true;
            flushPendingDoorDataChanges();
            editMode = EditMode.NONE;
            refreshUi();
        }

        private void handlePolygonPress(Point imagePoint, MouseEvent e) {
            Polygon polygon = getEditedPolygon();
            if ((javax.swing.SwingUtilities.isRightMouseButton(e) || isCloseToFirstVertex(polygon, imagePoint)) && polygon.npoints >= 3) {
                finishPolygonEdit();
                return;
            }
            if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            polygon.addPoint(imagePoint.x, imagePoint.y);
            setEditedPolygon(polygon);
            commitDoorDataChange();
        }

        private boolean isCloseToFirstVertex(Polygon polygon, Point imagePoint) {
            return polygon != null
                && polygon.npoints > 0
                && Point2D.distance(imagePoint.x, imagePoint.y, polygon.xpoints[0], polygon.ypoints[0]) <= 6.0d;
        }

        private void applyImpededCell(Point imagePoint) {
            List<Point> cells = getEditedImpededCells();
            Set<Point> set = new LinkedHashSet<Point>();
            for (Point point : cells) {
                if (point != null) {
                    set.add(new Point(point));
                }
            }
            Point cell = new Point(
                imagePoint.x / SearchMapData.CELL_WIDTH,
                imagePoint.y / SearchMapData.CELL_HEIGHT
            );
            if (dragPaintValue == null) {
                dragPaintValue = Boolean.valueOf(!set.contains(cell));
            }
            if (dragPaintValue.booleanValue()) {
                set.add(cell);
            } else {
                set.remove(cell);
            }
            setEditedImpededCells(new ArrayList<Point>(set));
            commitDoorDataChange();
        }

        private boolean isNearMarker(Point p) {
            return isNearMarker(p, currentMouseImagePoint);
        }

        private boolean isNearMarker(Point p, Point mouse) {
            if (p == null || (p.x == 0 && p.y == 0) || mouse == null) {
                return false;
            }
            return Point2D.distance(p.x, p.y, mouse.x, mouse.y) <= 8.0;
        }

        private boolean isNearAnyMarker() {
            return isNearMarker(currentDoorData.getLaunchPoint())
                || isNearMarker(currentDoorData.getOpenLocationFront())
                || isNearMarker(currentDoorData.getOpenLocationBack());
        }

        private void paintPolygonDraft(Graphics2D g2, Polygon polygon, Color fillColor, Color outlineColor) {
            if (polygon == null || polygon.npoints == 0) {
                return;
            }
            if (polygon.npoints >= 3) {
                g2.setColor(fillColor);
                g2.fillPolygon(polygon);
            }
            g2.setColor(outlineColor);
            g2.setStroke(new BasicStroke(1.25f));
            for (int i = 0; i < polygon.npoints - 1; i++) {
                g2.drawLine(polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[i + 1], polygon.ypoints[i + 1]);
            }
            if (currentMouseImagePoint != null && polygon.npoints > 0) {
                int lastX = polygon.xpoints[polygon.npoints - 1];
                int lastY = polygon.ypoints[polygon.npoints - 1];
                boolean nearFirst = polygon.npoints >= 3 && isCloseToFirstVertex(polygon, currentMouseImagePoint);
                float[] dash = { 4f, 3f };
                if (nearFirst) {
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
                    g2.setColor(Color.YELLOW);
                    g2.drawLine(lastX, lastY, polygon.xpoints[0], polygon.ypoints[0]);
                    g2.setStroke(new BasicStroke(1.25f));
                    g2.fillOval(polygon.xpoints[0] - 2, polygon.ypoints[0] - 2, 4, 4);
                } else {
                    int alpha = outlineColor.getAlpha();
                    g2.setColor(new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), Math.min(140, alpha)));
                    g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
                    g2.drawLine(lastX, lastY, currentMouseImagePoint.x, currentMouseImagePoint.y);
                    g2.setStroke(new BasicStroke(1.25f));
                }
            }
            g2.setColor(outlineColor);
            for (int i = 0; i < polygon.npoints; i++) {
                g2.fillOval(polygon.xpoints[i] - 2, polygon.ypoints[i] - 2, 4, 4);
            }
        }

        private void paintEntrancePointMarker(Graphics2D g2, Point point, int orientation) {
            if (point == null) {
                return;
            }
            BufferedImage markerImg = DirectionMarker.createEntranceMarkerImage(orientation);
            g2.drawImage(markerImg, point.x - markerImg.getWidth() / 2, point.y - markerImg.getHeight() / 2, null);
            g2.setColor(new Color(0, 255, 200));
            g2.setStroke(new BasicStroke(1.5f));
            int r = 9;
            g2.drawLine(point.x - r, point.y, point.x + r, point.y);
            g2.drawLine(point.x, point.y - r, point.x, point.y + r);
        }

        private void handleEntranceMousePressed(Point imagePoint, MouseEvent e) {
            if (javax.swing.SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                exitEntranceEditMode();
                return;
            }
            if (javax.swing.SwingUtilities.isLeftMouseButton(e) && imagePoint != null) {
                pendingEntrancePoint = new Point(imagePoint);
                updateEntranceSummary();
                repaint();
            }
        }

        private void handleTravelRegionMousePressed(Point imagePoint, MouseEvent e) {
            if (javax.swing.SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                if (pendingExitPolygon.npoints >= 3) {
                    confirmTravelRegion();
                } else {
                    exitTravelRegionEditMode();
                }
                return;
            }
            if (javax.swing.SwingUtilities.isLeftMouseButton(e) && imagePoint != null) {
                if (pendingExitPolygon.npoints >= 3 && isCloseToFirstVertex(pendingExitPolygon, imagePoint)) {
                    confirmTravelRegion();
                } else {
                    pendingExitPolygon.addPoint(imagePoint.x, imagePoint.y);
                    updateTravelRegionSummary();
                    repaint();
                }
            }
        }

        private void showPreviewContextMenu(MouseEvent e) {
            javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();

            javax.swing.JMenuItem editEntranceItem = new javax.swing.JMenuItem("Edit Entrance");
            editEntranceItem.addActionListener(ev -> startEditEntrance());
            menu.add(editEntranceItem);

            javax.swing.JMenuItem eraseEntranceItem = new javax.swing.JMenuItem("Erase Entrance");
            eraseEntranceItem.setEnabled(!lastEntranceName.isEmpty());
            eraseEntranceItem.addActionListener(ev -> {
                listener.onEraseEntrance();
                lastEntranceName = "";
                lastEntrancePoint = null;
                refreshUi();
            });
            menu.add(eraseEntranceItem);

            menu.addSeparator();

            javax.swing.JMenuItem editTravelRegionItem = new javax.swing.JMenuItem("Edit Travel Region");
            editTravelRegionItem.addActionListener(ev -> startEditTravelRegion());
            menu.add(editTravelRegionItem);

            javax.swing.JMenuItem eraseTravelRegionItem = new javax.swing.JMenuItem("Erase Travel Region");
            eraseTravelRegionItem.setEnabled(lastExitPolygon != null && lastExitPolygon.npoints > 0);
            eraseTravelRegionItem.addActionListener(ev -> {
                listener.onEraseTravelRegion();
                lastExitPolygon = null;
                refreshUi();
            });
            menu.add(eraseTravelRegionItem);

            menu.show(PreviewPanel.this, e.getX(), e.getY());
        }

        private void paintCheckerboard(Graphics2D g2, int width, int height) {
            int cellSize = 16;
            Color light = new Color(96, 96, 96);
            Color dark = new Color(68, 68, 68);
            for (int y = 0; y < height; y += cellSize) {
                for (int x = 0; x < width; x += cellSize) {
                    g2.setColor((((x / cellSize) + (y / cellSize)) & 1) == 0 ? light : dark);
                    g2.fillRect(x, y, cellSize, cellSize);
                }
            }
        }
    }
}
