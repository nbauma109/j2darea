package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

/**
 * Modeless control popup for authoring the local-side entrance point and travel polygon on the main area canvas.
 */
public class LocalTransitionPlacementDialog extends JDialog {

    public interface Listener {
        void onStateChanged();
        void onClearPolygon();
        void onUndoVertex();
        void onConfirm();
        void onCancel();
    }

    private static final long serialVersionUID = 1L;

    private final JRadioButton pointModeButton;
    private final JRadioButton polygonModeButton;
    private final JComboBox<Integer> orientationCombo;
    private final JLabel summaryLabel;

    public LocalTransitionPlacementDialog(Frame owner, String title, int initialOrientation, Listener listener) {
        super(owner, title, false);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(10, 10));

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pointModeButton = new JRadioButton("Pick Entrance Point", true);
        polygonModeButton = new JRadioButton("Draw Exit Travel Region Polygon");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(pointModeButton);
        modeGroup.add(polygonModeButton);
        pointModeButton.addActionListener(e -> listener.onStateChanged());
        polygonModeButton.addActionListener(e -> listener.onStateChanged());
        toolPanel.add(pointModeButton);
        toolPanel.add(polygonModeButton);

        Integer[] orientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            orientations[i] = i;
        }
        orientationCombo = new JComboBox<>(orientations);
        orientationCombo.setSelectedItem(Integer.valueOf(initialOrientation));
        orientationCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    int orientation = ((Integer) value).intValue();
                    setText(orientation + " (" + DirectionMarker.getOrientationName(orientation) + ")");
                }
                return this;
            }
        });
        orientationCombo.addActionListener(e -> listener.onStateChanged());
        toolPanel.add(new JLabel("Orientation:"));
        toolPanel.add(orientationCombo);

        JButton clearPolygonButton = new JButton("Clear Polygon");
        clearPolygonButton.addActionListener(e -> listener.onClearPolygon());
        toolPanel.add(clearPolygonButton);

        JButton undoVertexButton = new JButton("Undo Vertex");
        undoVertexButton.addActionListener(e -> listener.onUndoVertex());
        toolPanel.add(undoVertexButton);

        getContentPane().add(toolPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        summaryLabel = new JLabel(" ");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        bottomPanel.add(summaryLabel);

        JLabel helpLabel = new JLabel("Use the main area canvas to place the entrance point or add polygon vertices.");
        helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottomPanel.add(helpLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> listener.onConfirm());
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> listener.onCancel());
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isPointModeSelected() {
        return pointModeButton.isSelected();
    }

    public int getSelectedOrientation() {
        Object value = orientationCombo.getSelectedItem();
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    public void setSelectionSummary(boolean hasPoint, int pointX, int pointY, int polygonVertices) {
        String pointLabel = hasPoint ? (pointX + ", " + pointY) : "(unset)";
        summaryLabel.setText("Entrance: " + pointLabel
            + " | Orientation: " + DirectionMarker.getOrientationName(getSelectedOrientation())
            + " | Exit polygon vertices: " + polygonVertices);
    }
}
