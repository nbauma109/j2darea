package com.github.nbauma109.j2darea;

import javax.swing.*;
import java.awt.*;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog for editing entrance properties including destination area and entrance point.
 */
public class EntranceEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final EntranceData entranceData;
    private final List<AreaReference> availableAreas;
    private final List<AreaReference> knownOwnedAreas;
    private final Set<String> knownOwnedAreaResrefs;
    private final Set<String> reservedOwnedAreas;
    private final String exportPrefix;
    private boolean confirmed;

    private JTextField nameField;
    private JTextField xField;
    private JTextField yField;
    private JComboBox<Integer> orientationCombo;
    private JRadioButton existingAreaRadio;
    private JRadioButton ownedAreaRadio;
    private JTextField destinationAreaField;
    private JButton selectExistingAreaButton;
    private JButton suggestOwnedAreaButton;
    private JLabel destinationHintLabel;
    private JTextField destinationEntranceField;
    private JCheckBox createDestinationReturnTransitionBox;
    private JTextField destinationPointXField;
    private JTextField destinationPointYField;
    private JComboBox<Integer> destinationPointOrientationCombo;
    private JButton editDestinationGeometryButton;
    private String destinationPreviewImagePath = "";
    private Polygon destinationReturnPolygon = new Polygon();

    public EntranceEditorDialog(Frame owner, EntranceData entranceData, List<AreaReference> availableAreas,
            List<AreaReference> knownOwnedAreas, String exportPrefix, Collection<String> reservedOwnedAreas) {
        super(owner, "Edit Entrance", true);
        this.entranceData = entranceData;
        this.availableAreas = availableAreas != null ? availableAreas : new ArrayList<AreaReference>();
        this.knownOwnedAreas = knownOwnedAreas != null ? knownOwnedAreas : new ArrayList<AreaReference>();
        this.knownOwnedAreaResrefs = new LinkedHashSet<String>();
        for (AreaReference areaReference : this.knownOwnedAreas) {
            if (areaReference != null && areaReference.getResref() != null) {
                this.knownOwnedAreaResrefs.add(areaReference.getResref().trim());
            }
        }
        this.exportPrefix = normalizePrefix(exportPrefix);
        this.reservedOwnedAreas = new LinkedHashSet<String>();
        if (reservedOwnedAreas != null) {
            for (String area : reservedOwnedAreas) {
                if (area != null && !area.trim().isEmpty()) {
                    this.reservedOwnedAreas.add(area.trim().toUpperCase());
                }
            }
        }
        this.confirmed = false;

        initComponents();
        loadData();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Main panel with form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        row++;

        // X coordinate
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("X:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        xField = new JTextField(10);
        formPanel.add(xField, gbc);

        row++;

        // Y coordinate
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Y:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        yField = new JTextField(10);
        formPanel.add(yField, gbc);

        row++;

        // Orientation
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Orientation:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        Integer[] orientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            orientations[i] = i;
        }
        orientationCombo = new JComboBox<>(orientations);
        orientationCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    int orientation = (Integer) value;
                    String[] directions = {"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
                                          "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE"};
                    setText(orientation + " (" + directions[orientation] + ")");
                }
                return this;
            }
        });
        formPanel.add(orientationCombo, gbc);

        row++;

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 10, 5);
        formPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        row++;

        // Destination type
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Destination Type:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel destinationTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        existingAreaRadio = new JRadioButton("Existing In-Game Area");
        ownedAreaRadio = new JRadioButton("In-Mod Area");
        ButtonGroup destinationTypeGroup = new ButtonGroup();
        destinationTypeGroup.add(existingAreaRadio);
        destinationTypeGroup.add(ownedAreaRadio);
        destinationTypePanel.add(existingAreaRadio);
        destinationTypePanel.add(Box.createHorizontalStrut(10));
        destinationTypePanel.add(ownedAreaRadio);
        formPanel.add(destinationTypePanel, gbc);

        row++;

        // Destination area
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Destination Area:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel destAreaPanel = new JPanel(new BorderLayout(5, 0));
        destinationAreaField = new JTextField(15);
        destAreaPanel.add(destinationAreaField, BorderLayout.CENTER);
        JPanel destinationButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        selectExistingAreaButton = new JButton("Select...");
        selectExistingAreaButton.addActionListener(e -> {
            if (existingAreaRadio.isSelected()) {
                showAreaSelector();
            } else {
                showOwnedAreaSelector();
            }
        });
        destinationButtonPanel.add(selectExistingAreaButton);
        suggestOwnedAreaButton = new JButton("Suggest");
        destinationButtonPanel.add(suggestOwnedAreaButton);
        destAreaPanel.add(destinationButtonPanel, BorderLayout.EAST);
        formPanel.add(destAreaPanel, gbc);

        row++;

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        destinationHintLabel = new JLabel(" ");
        formPanel.add(destinationHintLabel, gbc);

        row++;

        // Destination entrance
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Destination Entrance:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        destinationEntranceField = new JTextField(15);
        destinationEntranceField.setEditable(false);
        formPanel.add(destinationEntranceField, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Return Patch:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        createDestinationReturnTransitionBox = new JCheckBox("Create entrance and destination-side return patch");
        createDestinationReturnTransitionBox.addActionListener(e -> {
            maybePrefillDestinationPatchGeometry();
            updateDestinationControls();
        });
        formPanel.add(createDestinationReturnTransitionBox, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Dest Point X / Y:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel destinationPointPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        destinationPointXField = new JTextField(5);
        destinationPointYField = new JTextField(5);
        destinationPointPanel.add(destinationPointXField);
        destinationPointPanel.add(new JLabel("/"));
        destinationPointPanel.add(destinationPointYField);
        formPanel.add(destinationPointPanel, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Dest Orientation:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        Integer[] destinationOrientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            destinationOrientations[i] = i;
        }
        destinationPointOrientationCombo = new JComboBox<>(destinationOrientations);
        destinationPointOrientationCombo.setRenderer(orientationCombo.getRenderer());
        formPanel.add(destinationPointOrientationCombo, gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Dest Geometry:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        editDestinationGeometryButton = new JButton("Select In Area...");
        editDestinationGeometryButton.addActionListener(e -> editDestinationGeometry());
        formPanel.add(editDestinationGeometryButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onOK());
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);

        existingAreaRadio.addActionListener(e -> updateDestinationControls());
        ownedAreaRadio.addActionListener(e -> updateDestinationControls());
    }

    private void showAreaSelector() {
        AreaSelectionDialog dialog = new AreaSelectionDialog(
            (Frame) getOwner(),
            availableAreas,
            destinationAreaField.getText()
        );
        dialog.setVisible(true);
        AreaReference selected = dialog.getSelectedArea();
        if (selected != null) {
            destinationAreaField.setText(selected.getResref());
            updateDestinationControls();
        }
    }

    private void showOwnedAreaSelector() {
        if (knownOwnedAreas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No known owned areas are registered yet. Export the other area first, then select it here.",
                "Owned Areas",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AreaSelectionDialog dialog = new AreaSelectionDialog(
            (Frame) getOwner(),
            "Select Planned In-Mod Area",
            knownOwnedAreas,
            destinationAreaField.getText()
        );
        dialog.setVisible(true);
        AreaReference selected = dialog.getSelectedArea();
        if (selected != null) {
            destinationAreaField.setText(selected.getResref());
        }
    }

    private void loadData() {
        nameField.setText(entranceData.getName());
        xField.setText(String.valueOf(entranceData.getX()));
        yField.setText(String.valueOf(entranceData.getY()));
        orientationCombo.setSelectedItem(entranceData.getOrientation());
        if (entranceData.getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
            ownedAreaRadio.setSelected(true);
        } else {
            existingAreaRadio.setSelected(true);
        }
        destinationAreaField.setText(entranceData.getDestinationArea());
        destinationEntranceField.setText(entranceData.getDestinationEntrance());
        createDestinationReturnTransitionBox.setSelected(entranceData.isCreateDestinationReturnTransition());
        destinationPointXField.setText(String.valueOf(entranceData.getDestinationPointX()));
        destinationPointYField.setText(String.valueOf(entranceData.getDestinationPointY()));
        destinationPointOrientationCombo.setSelectedItem(entranceData.getDestinationPointOrientation());
        destinationPreviewImagePath = entranceData.getDestinationPreviewImagePath();
        destinationReturnPolygon = entranceData.getDestinationReturnPolygon();
        updateDestinationControls();
    }

    private void onOK() {
        try {
            // Validate and save data
            entranceData.setName(nameField.getText().trim());
            entranceData.setX(Integer.parseInt(xField.getText().trim()));
            entranceData.setY(Integer.parseInt(yField.getText().trim()));
            entranceData.setOrientation((Integer) orientationCombo.getSelectedItem());
            entranceData.setDestinationArea(destinationAreaField.getText().trim());
            entranceData.setDestinationEntrance(destinationEntranceField.getText().trim());
            entranceData.setDestinationAreaType(
                ownedAreaRadio.isSelected() ? DestinationAreaType.OWNED_MOD_AREA : DestinationAreaType.EXISTING_GAME_AREA
            );
            entranceData.setCreateDestinationReturnTransition(createDestinationReturnTransitionBox.isSelected());
            entranceData.setDestinationPointX(Integer.parseInt(destinationPointXField.getText().trim()));
            entranceData.setDestinationPointY(Integer.parseInt(destinationPointYField.getText().trim()));
            entranceData.setDestinationPointOrientation((Integer) destinationPointOrientationCombo.getSelectedItem());
            entranceData.setDestinationPreviewImagePath(destinationPreviewImagePath);
            entranceData.setDestinationReturnPolygon(destinationReturnPolygon);

            if (ownedAreaRadio.isSelected()) {
                String validationError = validateOwnedDestination();
                if (validationError != null) {
                    JOptionPane.showMessageDialog(this, validationError, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            confirmed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Invalid number format for coordinates.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateDestinationControls() {
        boolean existingArea = existingAreaRadio.isSelected();
        boolean hasDestinationArea = destinationAreaField.getText() != null
            && !destinationAreaField.getText().trim().isEmpty();
        selectExistingAreaButton.setEnabled(true);
        suggestOwnedAreaButton.setEnabled(false);
        boolean enableDestinationPatchFields = existingArea && createDestinationReturnTransitionBox.isSelected();
        boolean enableDestinationGeometry = enableDestinationPatchFields && hasDestinationArea;
        createDestinationReturnTransitionBox.setEnabled(existingArea);
        destinationEntranceField.setEnabled(enableDestinationGeometry
            && destinationEntranceField.getText() != null
            && !destinationEntranceField.getText().trim().isEmpty());
        destinationPointXField.setEnabled(enableDestinationGeometry);
        destinationPointYField.setEnabled(enableDestinationGeometry);
        destinationPointOrientationCombo.setEnabled(enableDestinationGeometry);
        editDestinationGeometryButton.setEnabled(enableDestinationGeometry);
        if (existingArea) {
            if (enableDestinationPatchFields) {
                if (!hasDestinationArea) {
                    destinationHintLabel.setText("Select a destination area before opening destination-side geometry.");
                } else if (destinationReturnPolygon != null && destinationReturnPolygon.npoints >= 3) {
                    destinationHintLabel.setText("Patch geometry loaded from the configured game install: "
                        + destinationReturnPolygon.npoints + " return-region vertices.");
                } else {
                    destinationHintLabel.setText("Patch will add an entrance and destination-side return exit into the existing area.");
                }
            } else {
                destinationHintLabel.setText("Filter by resref or description when selecting an in-game area.");
            }
        } else {
            destinationHintLabel.setText("Owned areas must already exist. Select a known owned area from the registry.");
        }
    }

    private String validateOwnedDestination() {
        String destinationArea = destinationAreaField.getText() != null
            ? destinationAreaField.getText().trim()
            : "";
        if (destinationArea.isEmpty()) {
            return "Owned in-mod destinations must target an existing owned area.";
        }
        if (!destinationArea.matches("[^\\\\/:*?\"<>|\\s]{2,8}")) {
            return "Owned area resref must contain 2 to 8 filesystem-safe characters with no spaces.";
        }
        if (!exportPrefix.isEmpty() && !destinationArea.startsWith(exportPrefix)) {
            return "Owned area resref must start with the configured prefix " + exportPrefix + ".";
        }
        if (!knownOwnedAreaResrefs.contains(destinationArea)) {
            return "Owned destination " + destinationArea
                + " is not a known existing area. Create the entry/exit on the other area first.";
        }
        destinationAreaField.setText(destinationArea);
        return null;
    }

    private void maybePrefillDestinationPatchGeometry() {
        if (createDestinationReturnTransitionBox.isSelected() && destinationPointXField.getText().trim().isEmpty()) {
            destinationPointXField.setText(xField.getText().trim());
            destinationPointYField.setText(yField.getText().trim());
            destinationPointOrientationCombo.setSelectedItem(orientationCombo.getSelectedItem());
        }
    }

    private void editDestinationGeometry() {
        int pointX;
        int pointY;
        try {
            pointX = Integer.parseInt(destinationPointXField.getText().trim());
            pointY = Integer.parseInt(destinationPointYField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Destination X/Y must be valid numbers before opening the area selector.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        DestinationPatchSelectionDialog dialog = new DestinationPatchSelectionDialog(
            (Frame) getOwner(),
            destinationAreaField.getText().trim(),
            pointX,
            pointY,
            (Integer) destinationPointOrientationCombo.getSelectedItem(),
            destinationEntranceField.getText().trim(),
            destinationReturnPolygon
        );
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        destinationPreviewImagePath = "";
        destinationReturnPolygon = dialog.getReturnPolygon();
        destinationPointXField.setText(String.valueOf(dialog.getPointX()));
        destinationPointYField.setText(String.valueOf(dialog.getPointY()));
        destinationPointOrientationCombo.setSelectedItem(dialog.getPointOrientation());
        destinationEntranceField.setText(dialog.getDestinationEntranceName());
        updateDestinationControls();
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        return prefix.trim();
    }
}
