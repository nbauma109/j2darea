package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

/**
 * Lets the user load a destination area directly from the configured game install and pick a spawn point plus return-region polygon.
 */
public class DestinationPatchSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ImageCanvas canvas;
    private final JLabel summaryLabel;
    private final JRadioButton pointModeButton;
    private final JRadioButton polygonModeButton;
    private final JComboBox<Integer> orientationCombo;

    private BufferedImage previewImage;
    private final String areaResref;
    private int pointX;
    private int pointY;
    private int pointOrientation;
    private String destinationEntranceName;
    private Polygon returnPolygon;
    private boolean confirmed;

    public DestinationPatchSelectionDialog(Frame owner, String areaResref, int pointX, int pointY, int initialPointOrientation,
            String initialDestinationEntranceName, Polygon initialReturnPolygon) {
        super(owner, "Destination-side Patch Geometry", true);
        this.areaResref = areaResref != null ? areaResref.trim() : "";
        this.pointX = pointX;
        this.pointY = pointY;
        this.pointOrientation = initialPointOrientation;
        this.destinationEntranceName = initialDestinationEntranceName != null ? initialDestinationEntranceName.trim() : "";
        this.returnPolygon = clonePolygon(initialReturnPolygon);
        this.confirmed = false;

        setLayout(new BorderLayout(10, 10));
        canvas = new ImageCanvas();

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        pointModeButton = new JRadioButton("Pick Spawn Point", true);
        polygonModeButton = new JRadioButton("Draw Return Polygon");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(pointModeButton);
        modeGroup.add(polygonModeButton);
        toolPanel.add(pointModeButton);
        toolPanel.add(polygonModeButton);

        Integer[] orientations = new Integer[16];
        for (int i = 0; i < 16; i++) {
            orientations[i] = i;
        }
        orientationCombo = new JComboBox<>(orientations);
        orientationCombo.setSelectedItem(initialPointOrientation);
        orientationCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    int orientation = (Integer) value;
                    setText(orientation + " (" + DirectionMarker.getOrientationName(orientation) + ")");
                }
                return this;
            }
        });
        orientationCombo.addActionListener(e -> {
            pointOrientation =
                ((Integer) DestinationPatchSelectionDialog.this.orientationCombo.getSelectedItem()).intValue();
            canvas.repaint();
            updateSummary();
        });
        toolPanel.add(new JLabel("Orientation:"));
        toolPanel.add(orientationCombo);

        JButton clearPolygonButton = new JButton("Clear Polygon");
        clearPolygonButton.addActionListener(e -> {
            this.returnPolygon = new Polygon();
            canvas.repaint();
            updateSummary();
        });
        toolPanel.add(clearPolygonButton);

        JButton removeLastPointButton = new JButton("Undo Vertex");
        removeLastPointButton.addActionListener(e -> {
            if (this.returnPolygon.npoints > 0) {
                this.returnPolygon = copyWithoutLastVertex(this.returnPolygon);
                canvas.repaint();
                updateSummary();
            }
        });
        toolPanel.add(removeLastPointButton);

        add(toolPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        summaryLabel = new JLabel(" ");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        bottomPanel.add(summaryLabel);

        JLabel helpLabel = new JLabel("Click to place the spawn point. In polygon mode, click to add vertices; use Undo Vertex or Clear Polygon to revise.");
        helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottomPanel.add(helpLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> onOK());
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        loadAreaImageFromGame();
        updateSummary();
        getRootPane().setDefaultButton(okButton);
        setPreferredSize(new Dimension(900, 700));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getPointX() {
        return pointX;
    }

    public int getPointY() {
        return pointY;
    }

    public int getPointOrientation() {
        return pointOrientation;
    }

    public Polygon getReturnPolygon() {
        return clonePolygon(returnPolygon);
    }

    public String getDestinationEntranceName() {
        return destinationEntranceName;
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void loadAreaImageFromGame() {
        if (areaResref.isEmpty()) {
            previewImage = null;
            canvas.revalidate();
            canvas.repaint();
            updateSummary();
            return;
        }
        try {
            previewImage = GameAreaImageLoader.loadAreaImage(UserPreferences.getGameInstallPath(), areaResref);
            canvas.revalidate();
            canvas.repaint();
        } catch (Exception ex) {
            previewImage = null;
            JOptionPane.showMessageDialog(this,
                "Unable to load " + areaResref + " from the configured game install: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
        updateSummary();
    }

    private void updateSummary() {
        String imageStatus = previewImage == null ? "not loaded" : (areaResref.isEmpty() ? "loaded" : areaResref);
        summaryLabel.setText("Area: " + imageStatus + " | Spawn: " + pointX + ", " + pointY
            + " | Entrance: " + (destinationEntranceName.isEmpty() ? "(unset)" : destinationEntranceName)
            + " | Orientation: " + DirectionMarker.getOrientationName(pointOrientation)
            + " | Return polygon vertices: " + returnPolygon.npoints);
    }

    private boolean promptForEntranceNameAtPointPlacement() {
        String currentValue = destinationEntranceName != null ? destinationEntranceName.trim() : "";
        String initialValue = currentValue.isEmpty() ? buildSuggestedEntranceName() : currentValue;
        String enteredName = JOptionPane.showInputDialog(this, "Enter entrance name:", initialValue);
        if (enteredName == null) {
            return false;
        }
        String trimmedName = enteredName.trim();
        if (trimmedName.isEmpty()) {
            trimmedName = initialValue != null ? initialValue.trim() : "";
        }
        destinationEntranceName = trimmedName;
        return true;
    }

    private String buildSuggestedEntranceName() {
        if (!areaResref.isEmpty()) {
            return areaResref + "_ENTRANCE";
        }
        return "Entrance";
    }

    private static Polygon clonePolygon(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints];
        int[] ypoints = new int[polygon.npoints];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints);
        return new Polygon(xpoints, ypoints, polygon.npoints);
    }

    private static Polygon copyWithoutLastVertex(Polygon polygon) {
        if (polygon == null || polygon.npoints <= 1) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints - 1];
        int[] ypoints = new int[polygon.npoints - 1];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints - 1);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints - 1);
        return new Polygon(xpoints, ypoints, polygon.npoints - 1);
    }

    private final class ImageCanvas extends JPanel {

        private static final long serialVersionUID = 1L;

        private ImageCanvas() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleCanvasClick(e);
                }
            });
            setBackground(Color.DARK_GRAY);
        }

        @Override
        public Dimension getPreferredSize() {
            if (previewImage == null) {
                return new Dimension(640, 480);
            }
            return new Dimension(previewImage.getWidth(), previewImage.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (previewImage != null) {
                g2.drawImage(previewImage, 0, 0, null);
            } else {
                g2.setColor(Color.GRAY);
                g2.drawString("Select a destination area before opening this dialog.", 20, 30);
            }

            if (returnPolygon.npoints > 0) {
                g2.setColor(new Color(255, 215, 0, 80));
                if (returnPolygon.npoints >= 3) {
                    g2.fillPolygon(returnPolygon);
                }
                g2.setColor(new Color(255, 200, 0));
                g2.drawPolygon(returnPolygon);
                for (int i = 0; i < returnPolygon.npoints; i++) {
                    g2.fillOval(returnPolygon.xpoints[i] - 3, returnPolygon.ypoints[i] - 3, 7, 7);
                }
            }

            DirectionMarker.drawMarker(g2, pointX, pointY, pointOrientation, Color.CYAN, new Color(255, 230, 100), 6, 12);
            g2.dispose();
        }

        private void handleCanvasClick(MouseEvent e) {
            if (previewImage == null) {
                return;
            }
            Point point = e.getPoint();
            if (pointModeButton.isSelected()) {
                if (!promptForEntranceNameAtPointPlacement()) {
                    return;
                }
                pointX = point.x;
                pointY = point.y;
            } else if (polygonModeButton.isSelected()) {
                returnPolygon.addPoint(point.x, point.y);
            }
            repaint();
            updateSummary();
        }
    }
}
