package com.github.nbauma109.j2darea;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for editing entrance properties including destination area and entrance point.
 */
public class EntranceEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final EntranceData entranceData;
    private final List<String> availableAreas;
    private boolean confirmed;

    private JTextField nameField;
    private JTextField xField;
    private JTextField yField;
    private JComboBox<Integer> orientationCombo;
    private JTextField destinationAreaField;
    private JTextField destinationEntranceField;

    public EntranceEditorDialog(Frame owner, EntranceData entranceData, List<String> availableAreas) {
        super(owner, "Edit Entrance", true);
        this.entranceData = entranceData;
        this.availableAreas = availableAreas != null ? availableAreas : new ArrayList<>();
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

        if (!availableAreas.isEmpty()) {
            JButton browseButton = new JButton("...");
            browseButton.setPreferredSize(new Dimension(30, 20));
            browseButton.addActionListener(e -> showAreaSelector());
            destAreaPanel.add(browseButton, BorderLayout.EAST);
        }
        formPanel.add(destAreaPanel, gbc);

        row++;

        // Destination entrance
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Destination Entrance:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        destinationEntranceField = new JTextField(15);
        formPanel.add(destinationEntranceField, gbc);

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
    }

    private void showAreaSelector() {
        String[] areas = availableAreas.toArray(new String[0]);
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "Select destination area:",
            "Area Selector",
            JOptionPane.QUESTION_MESSAGE,
            null,
            areas,
            destinationAreaField.getText()
        );
        if (selected != null) {
            destinationAreaField.setText(selected);
        }
    }

    private void loadData() {
        nameField.setText(entranceData.getName());
        xField.setText(String.valueOf(entranceData.getX()));
        yField.setText(String.valueOf(entranceData.getY()));
        orientationCombo.setSelectedItem(entranceData.getOrientation());
        destinationAreaField.setText(entranceData.getDestinationArea());
        destinationEntranceField.setText(entranceData.getDestinationEntrance());
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

            confirmed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Invalid number format for X or Y coordinate.",
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
}
