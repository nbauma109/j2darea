package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Lets the user draw the local-side travel-region polygon directly on the current area.
 */
public class LocalTransitionRegionSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final BufferedImage previewImage;
    private final ImageCanvas canvas;
    private final JLabel summaryLabel;
    private Polygon polygon;
    private boolean confirmed;

    public LocalTransitionRegionSelectionDialog(Frame owner, BufferedImage previewImage, Polygon initialPolygon) {
        super(owner, "Local-side Travel Region", true);
        this.previewImage = previewImage;
        this.polygon = clonePolygon(initialPolygon);
        this.confirmed = false;

        setLayout(new BorderLayout(10, 10));
        canvas = new ImageCanvas();

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearPolygonButton = new JButton("Clear Polygon");
        clearPolygonButton.addActionListener(e -> {
            polygon = new Polygon();
            canvas.repaint();
            updateSummary();
        });
        toolPanel.add(clearPolygonButton);

        JButton undoVertexButton = new JButton("Undo Vertex");
        undoVertexButton.addActionListener(e -> {
            polygon = copyWithoutLastVertex(polygon);
            canvas.repaint();
            updateSummary();
        });
        toolPanel.add(undoVertexButton);
        add(toolPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        summaryLabel = new JLabel(" ");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        bottomPanel.add(summaryLabel);

        JLabel helpLabel = new JLabel("Left-click to add vertices. Right-click or click near the first vertex to finish the polygon.");
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

        updateSummary();
        getRootPane().setDefaultButton(okButton);
        setPreferredSize(new Dimension(900, 700));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Polygon getPolygon() {
        return clonePolygon(polygon);
    }

    private void onOK() {
        if (polygon.npoints < 3) {
            JOptionPane.showMessageDialog(this,
                "Draw at least three vertices for the local-side travel region polygon.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    private void updateSummary() {
        summaryLabel.setText("Local travel-region vertices: " + polygon.npoints);
    }

    private static Polygon clonePolygon(Polygon source) {
        if (source == null || source.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[source.npoints];
        int[] ypoints = new int[source.npoints];
        System.arraycopy(source.xpoints, 0, xpoints, 0, source.npoints);
        System.arraycopy(source.ypoints, 0, ypoints, 0, source.npoints);
        return new Polygon(xpoints, ypoints, source.npoints);
    }

    private static Polygon copyWithoutLastVertex(Polygon source) {
        if (source == null || source.npoints <= 1) {
            return new Polygon();
        }
        int[] xpoints = new int[source.npoints - 1];
        int[] ypoints = new int[source.npoints - 1];
        System.arraycopy(source.xpoints, 0, xpoints, 0, source.npoints - 1);
        System.arraycopy(source.ypoints, 0, ypoints, 0, source.npoints - 1);
        return new Polygon(xpoints, ypoints, source.npoints - 1);
    }

    private final class ImageCanvas extends JPanel {

        private static final long serialVersionUID = 1L;

        private ImageCanvas() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleClick(e);
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
                g2.drawString("Current area preview is not available.", 20, 30);
            }

            if (polygon.npoints > 0) {
                if (polygon.npoints >= 3) {
                    g2.setColor(new Color(0, 120, 255, 60));
                    g2.fillPolygon(polygon);
                }
                g2.setColor(new Color(0, 180, 255));
                if (polygon.npoints >= 2) {
                    g2.drawPolygon(polygon);
                } else {
                    g2.fillOval(polygon.xpoints[0] - 3, polygon.ypoints[0] - 3, 7, 7);
                }
                for (int i = 0; i < polygon.npoints; i++) {
                    g2.fillOval(polygon.xpoints[i] - 3, polygon.ypoints[i] - 3, 7, 7);
                }
            }
            g2.dispose();
        }

        private void handleClick(MouseEvent e) {
            if (previewImage == null || SwingUtilities.isLeftMouseButton(e) == false && SwingUtilities.isRightMouseButton(e) == false) {
                return;
            }
            if (polygon.npoints > 0 && (SwingUtilities.isRightMouseButton(e) || isNearFirstVertex(e.getX(), e.getY()))) {
                repaint();
                updateSummary();
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            polygon.addPoint(e.getX(), e.getY());
            repaint();
            updateSummary();
        }

        private boolean isNearFirstVertex(int x, int y) {
            if (polygon.npoints == 0) {
                return false;
            }
            int dx = x - polygon.xpoints[0];
            int dy = y - polygon.ypoints[0];
            return (dx * dx) + (dy * dy) <= 64;
        }
    }
}
