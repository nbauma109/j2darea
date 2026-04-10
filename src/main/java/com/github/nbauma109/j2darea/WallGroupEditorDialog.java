package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class WallGroupEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final WallGroupData wallGroupData;

    private boolean confirmed;
    private JTextField nameField;
    private JCheckBox wallCheckBox;
    private JCheckBox semiTransparentCheckBox;
    private JCheckBox hoveringCheckBox;
    private JCheckBox coverAnimationsCheckBox;
    private JCheckBox doorCheckBox;
    private JLabel polygonInfoLabel;

    public WallGroupEditorDialog(Frame owner, WallGroupData wallGroupData) {
        super(owner, "Edit Wallgroup", true);
        this.wallGroupData = wallGroupData;
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

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        nameField = new JTextField(20);
        form.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        form.add(new JLabel("Polygon:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        polygonInfoLabel = new JLabel();
        form.add(polygonInfoLabel, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        wallCheckBox = new JCheckBox("Wall");
        form.add(wallCheckBox, gbc);

        row++;
        gbc.gridy = row;
        semiTransparentCheckBox = new JCheckBox("Semi-transparent");
        form.add(semiTransparentCheckBox, gbc);

        row++;
        gbc.gridy = row;
        hoveringCheckBox = new JCheckBox("Hovering Wall");
        form.add(hoveringCheckBox, gbc);

        row++;
        gbc.gridy = row;
        coverAnimationsCheckBox = new JCheckBox("Cover Animations");
        form.add(coverAnimationsCheckBox, gbc);

        row++;
        gbc.gridy = row;
        doorCheckBox = new JCheckBox("Door");
        form.add(doorCheckBox, gbc);
        gbc.gridwidth = 1;

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onOk());
        buttons.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void loadData() {
        nameField.setText(wallGroupData.getName());
        wallCheckBox.setSelected(wallGroupData.isWall());
        semiTransparentCheckBox.setSelected(wallGroupData.isSemiTransparent());
        hoveringCheckBox.setSelected(wallGroupData.isHoveringWall());
        coverAnimationsCheckBox.setSelected(wallGroupData.isCoverAnimations());
        doorCheckBox.setSelected(wallGroupData.isDoor());
        refreshPolygonLabel();
    }

    private void refreshPolygonLabel() {
        int pointCount = wallGroupData != null && wallGroupData.getPolygon() != null ? wallGroupData.getPolygon().npoints : 0;
        polygonInfoLabel.setText(pointCount >= 3 ? pointCount + " vertices" : "No polygon selected");
    }

    private void onOk() {
        int pointCount = wallGroupData != null && wallGroupData.getPolygon() != null ? wallGroupData.getPolygon().npoints : 0;
        if (pointCount < 3) {
            JOptionPane.showMessageDialog(this,
                "A wallgroup polygon needs at least three vertices.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (hoveringCheckBox.isSelected() && pointCount < 5) {
            JOptionPane.showMessageDialog(this,
                "Hovering walls need at least five vertices.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        wallGroupData.setName(nameField.getText().trim());
        wallGroupData.setWall(wallCheckBox.isSelected());
        wallGroupData.setSemiTransparent(semiTransparentCheckBox.isSelected());
        wallGroupData.setHoveringWall(hoveringCheckBox.isSelected());
        wallGroupData.setCoverAnimations(coverAnimationsCheckBox.isSelected());
        wallGroupData.setDoor(doorCheckBox.isSelected());
        wallGroupData.setHeight(0);
        confirmed = true;
        dispose();
    }
}
