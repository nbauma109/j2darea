package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Dialog for editing polygon region metadata, including destination-side patch geometry.
 */
public class RegionEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final RegionData regionData;
    private final List<AreaReference> availableAreas;
    private final List<AreaReference> knownOwnedAreas;
    private final List<String> availableEntranceNames;
    private final Set<String> knownOwnedAreaResrefs;
    private final Set<String> reservedOwnedAreas;
    private final String exportPrefix;
    private final int areaWidth;
    private final int areaHeight;

    private boolean confirmed;
    private JTextField nameField;
    private JComboBox<Integer> typeCombo;
    private JRadioButton existingAreaRadio;
    private JRadioButton ownedAreaRadio;
    private JTextField destinationAreaField;
    private JButton selectExistingAreaButton;
    private JButton suggestOwnedAreaButton;
    private JTextField destinationEntranceField;
    private JTextField destinationPointXField;
    private JTextField destinationPointYField;
    private JComboBox<Integer> destinationOrientationCombo;
    private JComboBox<String> pairedEntranceCombo;
    private JButton editDestinationGeometryButton;
    private JLabel destinationHintLabel;
    private String destinationPreviewImagePath = "";
    private Polygon destinationReturnPolygon = new Polygon();

    public RegionEditorDialog(Frame owner, RegionData regionData, List<AreaReference> availableAreas,
            List<AreaReference> knownOwnedAreas, List<String> availableEntranceNames, String exportPrefix,
            Collection<String> reservedOwnedAreas, int areaWidth, int areaHeight) {
        super(owner, "Edit Region", true);
        this.regionData = regionData;
        this.availableAreas = availableAreas != null ? availableAreas : new ArrayList<AreaReference>();
        this.knownOwnedAreas = knownOwnedAreas != null ? knownOwnedAreas : new ArrayList<AreaReference>();
        this.availableEntranceNames = availableEntranceNames != null ? new ArrayList<String>(availableEntranceNames) : new ArrayList<String>();
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
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        initComponents();
        loadData();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        typeCombo = new JComboBox<>(new Integer[] {0, 1, 2});
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    int type = (Integer) value;
                    if (type == 0) {
                        setText("0 (Proximity Trigger)");
                    } else if (type == 1) {
                        setText("1 (Info Point)");
                    } else if (type == 2) {
                        setText("2 (Travel Region)");
                    }
                }
                return this;
            }
        });
        typeCombo.addActionListener(e -> updateDestinationControls());
        formPanel.add(typeCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        formPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.gridwidth = 1;

        row++;
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
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Destination Area:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel destAreaPanel = new JPanel(new BorderLayout(5, 0));
        destinationAreaField = new JTextField(15);
        destAreaPanel.add(destinationAreaField, BorderLayout.CENTER);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        selectExistingAreaButton = new JButton("Select...");
        selectExistingAreaButton.addActionListener(e -> {
            if (existingAreaRadio.isSelected()) {
                showAreaSelector();
            } else {
                showOwnedAreaSelector();
            }
        });
        buttonsPanel.add(selectExistingAreaButton);
        suggestOwnedAreaButton = new JButton("Suggest");
        buttonsPanel.add(suggestOwnedAreaButton);
        destAreaPanel.add(buttonsPanel, BorderLayout.EAST);
        formPanel.add(destAreaPanel, gbc);

        row++;
        gbc.gridx = 1;
        gbc.gridy = row;
        destinationHintLabel = new JLabel(" ");
        formPanel.add(destinationHintLabel, gbc);

        row++;
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
        formPanel.add(new JLabel("Dest Point X / Y:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel pointPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        destinationPointXField = new JTextField(5);
        destinationPointYField = new JTextField(5);
        pointPanel.add(destinationPointXField);
        pointPanel.add(new JLabel("/"));
        pointPanel.add(destinationPointYField);
        formPanel.add(pointPanel, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Dest Orientation:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        Integer[] orientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            orientations[i] = i;
        }
        destinationOrientationCombo = new JComboBox<>(orientations);
        formPanel.add(destinationOrientationCombo, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Paired Entrance:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        pairedEntranceCombo = new JComboBox<>();
        pairedEntranceCombo.addItem("");
        for (String entranceName : availableEntranceNames) {
            if (entranceName != null && !entranceName.trim().isEmpty()) {
                pairedEntranceCombo.addItem(entranceName.trim());
            }
        }
        pairedEntranceCombo.setEditable(false);
        formPanel.add(pairedEntranceCombo, gbc);

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onOK());
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
        existingAreaRadio.addActionListener(e -> updateDestinationControls());
        ownedAreaRadio.addActionListener(e -> updateDestinationControls());
    }

    private void loadData() {
        nameField.setText(regionData.getName());
        typeCombo.setSelectedItem(regionData.getType());
        if (regionData.getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
            ownedAreaRadio.setSelected(true);
        } else {
            existingAreaRadio.setSelected(true);
        }
        destinationAreaField.setText(regionData.getDestinationArea());
        destinationEntranceField.setText(regionData.getDestinationEntrance());
        destinationPointXField.setText(String.valueOf(regionData.getDestinationPointX()));
        destinationPointYField.setText(String.valueOf(regionData.getDestinationPointY()));
        destinationOrientationCombo.setSelectedItem(regionData.getDestinationPointOrientation());
        destinationPreviewImagePath = regionData.getDestinationPreviewImagePath();
        destinationReturnPolygon = regionData.getDestinationReturnPolygon();
        pairedEntranceCombo.setSelectedItem(regionData.getPairedEntranceName() != null ? regionData.getPairedEntranceName() : "");
        updateDestinationControls();
    }

    private void onOK() {
        try {
            regionData.setName(nameField.getText().trim());
            regionData.setType((Integer) typeCombo.getSelectedItem());
            regionData.setDestinationAreaType(
                ownedAreaRadio.isSelected() ? DestinationAreaType.OWNED_MOD_AREA : DestinationAreaType.EXISTING_GAME_AREA
            );
            regionData.setDestinationArea(destinationAreaField.getText().trim());
            regionData.setDestinationEntrance(destinationEntranceField.getText().trim());
            regionData.setDestinationPointX(Integer.parseInt(destinationPointXField.getText().trim()));
            regionData.setDestinationPointY(Integer.parseInt(destinationPointYField.getText().trim()));
            regionData.setDestinationPointOrientation((Integer) destinationOrientationCombo.getSelectedItem());
            regionData.setDestinationPreviewImagePath(destinationPreviewImagePath);
            regionData.setDestinationReturnPolygon(destinationReturnPolygon);
            regionData.setPairedEntranceName(pairedEntranceCombo.getSelectedItem() != null
                ? pairedEntranceCombo.getSelectedItem().toString().trim()
                : "");

            if (regionData.getType() == 2 && regionData.getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
                String validationError = validateOwnedDestination();
                if (validationError != null) {
                    JOptionPane.showMessageDialog(this, validationError, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (regionData.getType() == 2
                    && regionData.getDestinationAreaType() == DestinationAreaType.EXISTING_GAME_AREA
                    && !regionData.getDestinationArea().trim().isEmpty()
                    && regionData.getDestinationReturnPolygon().npoints < 3) {
                JOptionPane.showMessageDialog(this,
                    "Travel regions targeting existing in-game areas must define a destination-side return polygon from the game-loaded destination area.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (regionData.getType() == 2 && regionData.getPairedEntranceName().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Travel regions must be paired with an entrance point in the current area.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            confirmed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Invalid number format for destination coordinates.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDestinationControls() {
        boolean travelRegion = ((Integer) typeCombo.getSelectedItem()) == 2;
        boolean existingArea = existingAreaRadio.isSelected();
        boolean hasDestinationArea = destinationAreaField.getText() != null
            && !destinationAreaField.getText().trim().isEmpty();
        boolean enableDestination = travelRegion;
        boolean enableImageGeometry = travelRegion && existingArea && hasDestinationArea;
        existingAreaRadio.setEnabled(enableDestination);
        ownedAreaRadio.setEnabled(enableDestination);
        selectExistingAreaButton.setEnabled(enableDestination);
        suggestOwnedAreaButton.setEnabled(false);
        destinationAreaField.setEnabled(enableDestination);
        destinationEntranceField.setEnabled(enableImageGeometry
            && destinationEntranceField.getText() != null
            && !destinationEntranceField.getText().trim().isEmpty());
        destinationPointXField.setEnabled(enableImageGeometry);
        destinationPointYField.setEnabled(enableImageGeometry);
        destinationOrientationCombo.setEnabled(enableImageGeometry);
        pairedEntranceCombo.setEnabled(enableDestination);
        editDestinationGeometryButton.setEnabled(enableImageGeometry);

        if (!travelRegion) {
            destinationHintLabel.setText("Only travel regions use inter-area destination data.");
            return;
        }
        if (existingArea) {
            if (!hasDestinationArea) {
                destinationHintLabel.setText("Select a destination area before opening destination-side geometry.");
            } else if (destinationReturnPolygon != null && destinationReturnPolygon.npoints >= 3) {
                destinationHintLabel.setText("Destination-side polygon loaded: "
                    + destinationReturnPolygon.npoints + " vertices.");
            } else {
                destinationHintLabel.setText("Existing-area travel regions require destination-side geometry loaded from the configured game install.");
            }
        } else {
            String edgeDirection = detectEdgeDirection();
            if (edgeDirection != null) {
                destinationHintLabel.setText("Unknown owned destinations are only allowed for " + edgeDirection + " edge transitions.");
            } else {
                destinationHintLabel.setText("Owned destinations must already exist unless this travel region is a single-edge NORTH/SOUTH/EAST/WEST transition.");
            }
        }
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
            (Integer) destinationOrientationCombo.getSelectedItem(),
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
        destinationOrientationCombo.setSelectedItem(dialog.getPointOrientation());
        destinationEntranceField.setText(dialog.getDestinationEntranceName());
        updateDestinationControls();
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        return prefix.trim();
    }

    private String validateOwnedDestination() {
        String destinationArea = destinationAreaField.getText() != null
            ? destinationAreaField.getText().trim()
            : "";
        if (destinationArea.isEmpty()) {
            return "Owned in-mod destinations need an existing owned area, unless this is a cardinal edge transition.";
        }
        if (!destinationArea.matches("[^\\\\/:*?\"<>|\\s]{2,8}")) {
            return "Owned area resref must contain 2 to 8 filesystem-safe characters with no spaces.";
        }
        if (!exportPrefix.isEmpty() && !destinationArea.startsWith(exportPrefix)) {
            return "Owned area resref must start with the configured prefix " + exportPrefix + ".";
        }
        if (!knownOwnedAreaResrefs.contains(destinationArea) && detectEdgeDirection() == null) {
            return "Owned destination " + destinationArea
                + " is not a known existing area. Create the entry/exit on the other area first, or use a single-edge NORTH/SOUTH/EAST/WEST transition.";
        }
        destinationAreaField.setText(destinationArea);
        return null;
    }

    private String detectEdgeDirection() {
        Polygon polygon = regionData.getBounds();
        if (polygon == null || polygon.npoints < 3) {
            return null;
        }
        java.awt.Rectangle bounds = polygon.getBounds();
        boolean touchesNorth = bounds.y <= 0;
        boolean touchesSouth = bounds.y + bounds.height >= areaHeight;
        boolean touchesWest = bounds.x <= 0;
        boolean touchesEast = bounds.x + bounds.width >= areaWidth;
        int count = (touchesNorth ? 1 : 0) + (touchesSouth ? 1 : 0) + (touchesWest ? 1 : 0) + (touchesEast ? 1 : 0);
        if (count != 1) {
            return null;
        }
        if (touchesNorth) {
            return "NORTH";
        }
        if (touchesSouth) {
            return "SOUTH";
        }
        if (touchesWest) {
            return "WEST";
        }
        return "EAST";
    }
}
