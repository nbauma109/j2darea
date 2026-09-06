package com.github.nbauma109.j2darea;

import static com.github.nbauma109.j2darea.J2DArea.BUTTON_SIZE;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class PolygonSelectionView extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int PANEL_PADDING = 12;
    private static final int INITIAL_ERASER_RADIUS = 8;
    private static final int MIN_ERASER_RADIUS = 1;
    private static final int MAX_ERASER_RADIUS = 200;
    private static final int INITIAL_WAND_TOLERANCE = 20;
    private static final int MIN_WAND_TOLERANCE = 1;
    private static final int MAX_WAND_TOLERANCE = 255;
    private static final int MAX_HISTORY_ENTRIES = 50;

    private final transient BGSubtracter bgSubtracter;
    private final transient BufferedImage previewImage;
    private final PreviewPanel previewPanel;
    private final Deque<BufferedImage> undoStack = new ArrayDeque<BufferedImage>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<BufferedImage>();
    private JToggleButton eraserButton;
    private JToggleButton wandButton;
    private JButton undoButton;
    private JButton redoButton;

    public PolygonSelectionView(BufferedImage image, java.awt.Polygon relativePolygon) {
        setTitle("Polygon preview");
        this.bgSubtracter = new BGSubtracter(image, relativePolygon);
        this.bgSubtracter.subtractBackground();
        this.previewImage = bgSubtracter.getPreviewImage();
        this.previewPanel = new PreviewPanel();

        setLayout(new BorderLayout());
        add(previewPanel, BorderLayout.CENTER);

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        eraserButton = new JToggleButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/eraser.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                setActiveTool(((JToggleButton) event.getSource()).isSelected() ? Tool.ERASER : Tool.NONE);
            }
        });
        eraserButton.setMaximumSize(BUTTON_SIZE);
        eraserButton.setToolTipText("Toggle eraser");
        menubar.add(eraserButton);

        wandButton = new JToggleButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/wand.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                setActiveTool(((JToggleButton) event.getSource()).isSelected() ? Tool.WAND : Tool.NONE);
            }
        });
        wandButton.setMaximumSize(BUTTON_SIZE);
        wandButton.setToolTipText("Toggle fuzzy select");
        menubar.add(wandButton);

        undoButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/undo.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                undo();
            }
        });
        undoButton.setMaximumSize(BUTTON_SIZE);
        undoButton.setToolTipText("Undo");
        menubar.add(undoButton);

        redoButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/redo.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                redo();
            }
        });
        redoButton.setMaximumSize(BUTTON_SIZE);
        redoButton.setToolTipText("Redo");
        menubar.add(redoButton);

        JButton exportButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-img.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                File file = J2DArea.chooseFile(PolygonSelectionView.this, FileDialog.SAVE, FileChooserLocation.OBJECT);
                if (file == null) {
                    return;
                }

                boolean success;
                try {
                    success = J2DArea.writeImage(file, previewImage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    success = false;
                }

                if (success) {
                    JOptionPane.showMessageDialog(null, "Image saved.");
                } else {
                    JOptionPane.showMessageDialog(null, "Image save failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        exportButton.setMaximumSize(BUTTON_SIZE);
        exportButton.setToolTipText("Save to a PNG image");
        menubar.add(exportButton);
        updateUndoRedoButtons();

        pack();
        setMinimumSize(new Dimension(480, 360));
        setSize(calculateInitialSize());
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setActiveTool(Tool tool) {
        eraserButton.setSelected(tool == Tool.ERASER);
        wandButton.setSelected(tool == Tool.WAND);
        previewPanel.setActiveTool(tool);
    }

    private Dimension calculateInitialSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(Math.max(previewImage.getWidth() + 160, 720), (int) (screenSize.width * 0.8));
        int height = Math.min(Math.max(previewImage.getHeight() + 120, 540), (int) (screenSize.height * 0.8));
        return new Dimension(width, height);
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(copyImage(previewImage));
        replacePreviewImage(undoStack.pop());
        updateUndoRedoButtons();
        previewPanel.refreshHoverPreview();
        previewPanel.repaint();
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(copyImage(previewImage));
        replacePreviewImage(redoStack.pop());
        updateUndoRedoButtons();
        previewPanel.refreshHoverPreview();
        previewPanel.repaint();
    }

    private void pushUndoSnapshot() {
        undoStack.push(copyImage(previewImage));
        while (undoStack.size() > MAX_HISTORY_ENTRIES) {
            undoStack.removeLast();
        }
        redoStack.clear();
        updateUndoRedoButtons();
    }

    private void replacePreviewImage(BufferedImage image) {
        Graphics2D graphics2d = previewImage.createGraphics();
        try {
            graphics2d.setComposite(AlphaComposite.Src);
            graphics2d.drawImage(image, 0, 0, null);
        } finally {
            graphics2d.dispose();
        }
    }

    private void updateUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setEnabled(!undoStack.isEmpty());
        }
        if (redoButton != null) {
            redoButton.setEnabled(!redoStack.isEmpty());
        }
    }

    private static BufferedImage copyImage(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2d = copy.createGraphics();
        try {
            graphics2d.drawImage(image, 0, 0, null);
        } finally {
            graphics2d.dispose();
        }
        return copy;
    }

    private final class PreviewPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private Tool activeTool = Tool.NONE;
        private Point hoverPoint;
        private Point lastErasePoint;
        private int eraserRadius = INITIAL_ERASER_RADIUS;
        private int wandColorTolerance = INITIAL_WAND_TOLERANCE;
        private boolean[][] hoverWandSelection;

        private PreviewPanel() {
            MouseAdapter mouseAdapter = new MouseAdapter() {

                @Override
                public void mouseMoved(MouseEvent event) {
                    hoverPoint = toImagePoint(event.getPoint(), false);
                    refreshHoverPreview();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hoverPoint = null;
                    hoverWandSelection = null;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent event) {
                    if (!javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                        return;
                    }
                    Point point = toImagePoint(event.getPoint(), activeTool == Tool.ERASER);
                    if (activeTool == Tool.ERASER) {
                        if (point == null) {
                            return;
                        }
                        pushUndoSnapshot();
                        applyEraserLine(point, point);
                        lastErasePoint = point;
                        hoverPoint = point;
                        refreshHoverPreview();
                        return;
                    }
                    if (activeTool != Tool.WAND || point == null) {
                        return;
                    }
                    boolean[][] selection = buildWandSelection(point);
                    hoverPoint = point;
                    hoverWandSelection = selection;
                    if (selection == null) {
                        repaint();
                        return;
                    }
                    pushUndoSnapshot();
                    applyWandSelection(selection);
                    refreshHoverPreview();
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    Point point = toImagePoint(event.getPoint(), activeTool == Tool.ERASER);
                    hoverPoint = point;
                    if (activeTool == Tool.WAND) {
                        refreshHoverPreview();
                        return;
                    }
                    if (activeTool != Tool.ERASER || lastErasePoint == null || point == null) {
                        repaint();
                        return;
                    }
                    applyEraserLine(lastErasePoint, point);
                    lastErasePoint = point;
                    refreshHoverPreview();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    lastErasePoint = null;
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    if (activeTool == Tool.ERASER) {
                        int updatedRadius = Math.max(
                            MIN_ERASER_RADIUS,
                            Math.min(MAX_ERASER_RADIUS, eraserRadius - event.getWheelRotation())
                        );
                        if (updatedRadius != eraserRadius) {
                            eraserRadius = updatedRadius;
                            hoverPoint = toImagePoint(event.getPoint(), false);
                            updateToolTip();
                            repaint();
                        }
                        event.consume();
                        return;
                    }
                    if (activeTool != Tool.WAND) {
                        return;
                    }
                    int updatedTolerance = Math.max(
                        MIN_WAND_TOLERANCE,
                        Math.min(MAX_WAND_TOLERANCE, wandColorTolerance - event.getWheelRotation())
                    );
                    if (updatedTolerance != wandColorTolerance) {
                        wandColorTolerance = updatedTolerance;
                        hoverPoint = toImagePoint(event.getPoint(), false);
                        refreshHoverPreview();
                    }
                    event.consume();
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            addMouseWheelListener(mouseAdapter);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(640, 480);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2d = (Graphics2D) g.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                ImageLayout layout = getImageLayout();
                if (layout == null) {
                    return;
                }
                TransparencyPreviewPainter.paintCheckerboard(graphics2d, layout.x, layout.y, layout.width, layout.height);
                graphics2d.drawImage(previewImage, layout.x, layout.y, layout.width, layout.height, null);
                if (activeTool == Tool.WAND && hoverWandSelection != null) {
                    paintWandSelectionOverlay(graphics2d, layout);
                }
                if (activeTool == Tool.ERASER && hoverPoint != null) {
                    double radius = Math.max(1.0d, eraserRadius * layout.scale);
                    double centerX = layout.x + ((hoverPoint.x + 0.5d) * layout.scale);
                    double centerY = layout.y + ((hoverPoint.y + 0.5d) * layout.scale);
                    graphics2d.setColor(new Color(0, 120, 255, 60));
                    graphics2d.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0d, radius * 2.0d));
                    graphics2d.setColor(new Color(0, 120, 255));
                    graphics2d.setStroke(new BasicStroke(1.5f));
                    graphics2d.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0d, radius * 2.0d));
                }
            } finally {
                graphics2d.dispose();
            }
        }

        private void setActiveTool(Tool tool) {
            this.activeTool = tool;
            if (tool != Tool.ERASER) {
                lastErasePoint = null;
            }
            refreshHoverPreview();
        }

        private void refreshHoverPreview() {
            hoverWandSelection = activeTool == Tool.WAND ? buildWandSelection(hoverPoint) : null;
            updateToolTip();
            repaint();
        }

        private void updateToolTip() {
            if (activeTool == Tool.ERASER) {
                setToolTipText("Eraser active: mouse wheel changes radius (" + eraserRadius + "px)");
            } else if (activeTool == Tool.WAND) {
                setToolTipText("Fuzzy select active: click contiguous color region, mouse wheel changes threshold (" + wandColorTolerance + ")");
            } else {
                setToolTipText(null);
            }
        }

        private void paintWandSelectionOverlay(Graphics2D graphics2d, ImageLayout layout) {
            graphics2d.setColor(new Color(0, 160, 255, 70));
            for (int y = 0; y < hoverWandSelection.length; y++) {
                for (int x = 0; x < hoverWandSelection[y].length; x++) {
                    if (!hoverWandSelection[y][x]) {
                        continue;
                    }
                    int drawX = layout.x + (int) Math.floor(x * layout.scale);
                    int drawY = layout.y + (int) Math.floor(y * layout.scale);
                    int drawWidth = Math.max(1, (int) Math.ceil((x + 1) * layout.scale) - (int) Math.floor(x * layout.scale));
                    int drawHeight = Math.max(1, (int) Math.ceil((y + 1) * layout.scale) - (int) Math.floor(y * layout.scale));
                    graphics2d.fillRect(drawX, drawY, drawWidth, drawHeight);
                }
            }
        }

        private boolean[][] buildWandSelection(Point point) {
            if (point == null || ((previewImage.getRGB(point.x, point.y) >>> 24) & 0xFF) == 0) {
                return null;
            }
            // Use the current opaque preview as the contiguous-selection domain, like a fuzzy select on the live image.
            return FuzzySelection.extractColorConstrainedComponent(
                previewImage,
                FuzzySelection.buildVisibleMask(previewImage),
                point.x,
                point.y,
                wandColorTolerance
            );
        }

        private void applyWandSelection(boolean[][] selection) {
            for (int y = 0; y < selection.length; y++) {
                for (int x = 0; x < selection[y].length; x++) {
                    if (selection[y][x]) {
                        previewImage.setRGB(x, y, 0);
                    }
                }
            }
        }

        private ImageLayout getImageLayout() {
            int availableWidth = getWidth() - (PANEL_PADDING * 2);
            int availableHeight = getHeight() - (PANEL_PADDING * 2);
            if (availableWidth <= 0 || availableHeight <= 0) {
                return null;
            }
            double scale = Math.min(
                availableWidth / (double) previewImage.getWidth(),
                availableHeight / (double) previewImage.getHeight()
            );
            int drawWidth = Math.max(1, (int) Math.round(previewImage.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(previewImage.getHeight() * scale));
            int drawX = (getWidth() - drawWidth) / 2;
            int drawY = (getHeight() - drawHeight) / 2;
            return new ImageLayout(drawX, drawY, drawWidth, drawHeight, scale);
        }

        private Point toImagePoint(Point panelPoint, boolean clampToImage) {
            ImageLayout layout = getImageLayout();
            if (layout == null) {
                return null;
            }
            int effectiveX = panelPoint.x;
            int effectiveY = panelPoint.y;
            if (clampToImage) {
                effectiveX = Math.max(layout.x, Math.min(layout.x + layout.width - 1, panelPoint.x));
                effectiveY = Math.max(layout.y, Math.min(layout.y + layout.height - 1, panelPoint.y));
            } else if (!layout.contains(panelPoint)) {
                return null;
            }

            int imageX = (int) Math.floor((effectiveX - layout.x) / layout.scale);
            int imageY = (int) Math.floor((effectiveY - layout.y) / layout.scale);
            return new Point(
                Math.max(0, Math.min(previewImage.getWidth() - 1, imageX)),
                Math.max(0, Math.min(previewImage.getHeight() - 1, imageY))
            );
        }

        private void applyEraserLine(Point fromPoint, Point toPoint) {
            int steps = Math.max(Math.abs(toPoint.x - fromPoint.x), Math.abs(toPoint.y - fromPoint.y));
            for (int step = 0; step <= steps; step++) {
                double ratio = steps == 0 ? 0.0d : step / (double) steps;
                int x = (int) Math.round(fromPoint.x + ((toPoint.x - fromPoint.x) * ratio));
                int y = (int) Math.round(fromPoint.y + ((toPoint.y - fromPoint.y) * ratio));
                applyEraserAt(x, y);
            }
            repaint();
        }

        private void applyEraserAt(int centerX, int centerY) {
            int minX = Math.max(0, centerX - eraserRadius);
            int minY = Math.max(0, centerY - eraserRadius);
            int maxX = Math.min(previewImage.getWidth() - 1, centerX + eraserRadius);
            int maxY = Math.min(previewImage.getHeight() - 1, centerY + eraserRadius);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int dx = x - centerX;
                    int dy = y - centerY;
                    if ((dx * dx) + (dy * dy) <= eraserRadius * eraserRadius) {
                        previewImage.setRGB(x, y, 0);
                    }
                }
            }
        }
    }

    private enum Tool {
        NONE,
        ERASER,
        WAND
    }

    private static final class ImageLayout {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final double scale;

        private ImageLayout(int x, int y, int width, int height, double scale) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }

        private boolean contains(Point point) {
            return point.x >= x && point.x < x + width && point.y >= y && point.y < y + height;
        }
    }
}
