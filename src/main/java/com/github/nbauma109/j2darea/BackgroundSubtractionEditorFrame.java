package com.github.nbauma109.j2darea;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class BackgroundSubtractionEditorFrame extends JFrame {

    private static final int PANEL_PADDING = 12;
    private static final int BACKGROUND_TOLERANCE = 20;
    private static final int CLEANUP_SIZE = 80;
    private static final int DEFAULT_WAND_COLOR_TOLERANCE = 100;
    private static final int GRID_CELL_SIZE = 16;
    private static final int INITIAL_EDGE_WIDTH = 1;
    private static final int INITIAL_ERASER_RADIUS = 8;
    private static final int MIN_ERASER_RADIUS = 1;
    private static final int MAX_ERASER_RADIUS = 200;
    private static final Dimension BUTTON_DIMENSION = new Dimension(25, 25);

    private final BufferedImage sourceImage;
    private final OriginalImagePanel originalImagePanel;
    private final ResultImagePanel resultImagePanel;

    private final JCheckBox northEdgeCheckbox;
    private final JCheckBox southEdgeCheckbox;
    private final JCheckBox eastEdgeCheckbox;
    private final JCheckBox westEdgeCheckbox;
    private final JSpinner edgeWidthSpinner;

    private final JLabel parameterInfoLabel;
    private final JLabel backgroundInfoLabel;
    private final JLabel toolInfoLabel;

    private final JToggleButton eraserButton;
    private final JToggleButton wandButton;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JButton exportButton;

    private final Deque<EditorState> undoStack;
    private final Deque<EditorState> redoStack;
    private final ExecutorService recomputeExecutor;

    private boolean[][] manualDeletedMask;
    private boolean[][] backgroundCellSelectionMask;
    private boolean[][] finalForegroundMask;
    private boolean[][] hoveredWandMask;
    private BufferedImage renderedImage;

    private boolean usingFallbackBackgroundReference;
    private boolean recomputePending;
    private volatile int latestRecomputeRequestId;
    private Tool activeTool;
    private int eraserRadius;
    private int wandColorTolerance;
    private Point hoveredImagePoint;

    public BackgroundSubtractionEditorFrame(BufferedImage sourceImage) {
        super("Background Cleanup Editor");
        this.sourceImage = copyImage(Objects.requireNonNull(sourceImage, "sourceImage must not be null"));
        this.originalImagePanel = new OriginalImagePanel();
        this.resultImagePanel = new ResultImagePanel();

        this.northEdgeCheckbox = new JCheckBox("North", true);
        this.southEdgeCheckbox = new JCheckBox("South", true);
        this.eastEdgeCheckbox = new JCheckBox("East", true);
        this.westEdgeCheckbox = new JCheckBox("West", true);
        this.edgeWidthSpinner = new JSpinner(new SpinnerNumberModel(
                INITIAL_EDGE_WIDTH,
                1,
                Math.max(this.sourceImage.getWidth(), this.sourceImage.getHeight()),
                1
        ));

        this.parameterInfoLabel = new JLabel("", SwingConstants.CENTER);
        this.backgroundInfoLabel = new JLabel("", SwingConstants.CENTER);
        this.toolInfoLabel = new JLabel("", SwingConstants.CENTER);

        this.eraserButton = createToolButton("eraser.png", "Eraser");
        this.wandButton = createToolButton("wand.png", "Magic Wand");
        this.undoButton = createButton("undo.png", "Undo");
        this.redoButton = createButton("redo.png", "Redo");
        this.exportButton = createButton("save-img.png", "Export transparent PNG");

        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.recomputeExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "BackgroundSubtractionRecompute");
            thread.setDaemon(true);
            return thread;
        });

        this.manualDeletedMask = new boolean[this.sourceImage.getHeight()][this.sourceImage.getWidth()];
        this.backgroundCellSelectionMask = new boolean[getBackgroundCellRowCount()][getBackgroundCellColumnCount()];
        this.finalForegroundMask = new boolean[this.sourceImage.getHeight()][this.sourceImage.getWidth()];
        this.hoveredWandMask = null;
        this.renderedImage = null;

        this.usingFallbackBackgroundReference = false;
        this.recomputePending = false;
        this.latestRecomputeRequestId = 0;
        this.activeTool = Tool.MAGIC_WAND;
        this.eraserRadius = INITIAL_ERASER_RADIUS;
        this.wandColorTolerance = DEFAULT_WAND_COLOR_TOLERANCE;
        this.hoveredImagePoint = null;

        initializeUi();
        bindKeyboardShortcuts();
        installOriginalInteraction();
        installResultInteraction();
        recomputeAndRepaint();
    }

    private void initializeUi() {
        setLayout(new BorderLayout(8, 8));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        ButtonGroup toolGroup = new ButtonGroup();
        toolGroup.add(eraserButton);
        toolGroup.add(wandButton);
        wandButton.setSelected(true);

        eraserButton.addActionListener(event -> setActiveTool(Tool.ERASER));
        wandButton.addActionListener(event -> setActiveTool(Tool.MAGIC_WAND));
        undoButton.addActionListener(event -> undo());
        redoButton.addActionListener(event -> redo());
        exportButton.addActionListener(event -> exportRenderedImage());

        toolBar.add(eraserButton);
        toolBar.add(wandButton);
        toolBar.addSeparator();
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.addSeparator();
        toolBar.add(exportButton);

        JPanel comparisonPanel = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        comparisonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        comparisonPanel.add(originalImagePanel);
        comparisonPanel.add(resultImagePanel);

        JPanel edgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        edgePanel.add(new JLabel("Background edges:"));
        edgePanel.add(northEdgeCheckbox);
        edgePanel.add(southEdgeCheckbox);
        edgePanel.add(eastEdgeCheckbox);
        edgePanel.add(westEdgeCheckbox);
        edgePanel.add(new JLabel("Width:"));
        edgePanel.add(edgeWidthSpinner);

        northEdgeCheckbox.addActionListener(event -> recomputeAndRepaint());
        southEdgeCheckbox.addActionListener(event -> recomputeAndRepaint());
        eastEdgeCheckbox.addActionListener(event -> recomputeAndRepaint());
        westEdgeCheckbox.addActionListener(event -> recomputeAndRepaint());
        edgeWidthSpinner.addChangeListener(event -> recomputeAndRepaint());

        JPanel labelsPanel = new JPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        labelsPanel.add(parameterInfoLabel);
        labelsPanel.add(backgroundInfoLabel);
        labelsPanel.add(toolInfoLabel);

        JPanel controlsPanel = new JPanel(new BorderLayout(8, 6));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        controlsPanel.add(edgePanel, BorderLayout.NORTH);
        controlsPanel.add(labelsPanel, BorderLayout.CENTER);

        add(toolBar, BorderLayout.NORTH);
        add(comparisonPanel, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);

        Dimension initialSize = calculateInitialFrameSize();
        comparisonPanel.setPreferredSize(new Dimension(initialSize.width - 40, initialSize.height - 180));

        setMinimumSize(new Dimension(900, 650));
        pack();
        setSize(initialSize);
        setLocationRelativeTo(null);
    }

    private void bindKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK), "undo");
        inputMap.put(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK), "redo");

        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                undo();
            }
        });

        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                redo();
            }
        });
    }

    private void installOriginalInteraction() {
        MouseAdapter mouseAdapter = new MouseAdapter() {

            private Boolean selectBackground;
            private boolean snapshotTaken;
            private Point lastDragCell;
            private AWTEventListener globalDragListener;

            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }

                Point cell = toOriginalCell(event, true);
                if (cell == null) {
                    return;
                }

                selectBackground = !isBackgroundCellSelected(cell.x, cell.y);
                lastDragCell = cell;
                snapshotTaken = applyBackgroundCellSelectionSegmentByCell(cell, cell, selectBackground, true);
                startGlobalDragTracking();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (selectBackground == null) {
                    return;
                }

                Point cell = toOriginalCell(event, true);
                if (cell == null) {
                    return;
                }

                // Always apply the drag segment; snapshotTaken only controls whether we capture undo once.
                boolean changed = applyBackgroundCellSelectionSegmentByCell(lastDragCell, cell, selectBackground, !snapshotTaken);
                snapshotTaken = snapshotTaken || changed;
                lastDragCell = cell;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Point cell = toOriginalCell(event, true);
                if (selectBackground != null && lastDragCell != null && cell != null) {
                    boolean changed = applyBackgroundCellSelectionSegmentByCell(lastDragCell, cell, selectBackground, !snapshotTaken);
                    snapshotTaken = snapshotTaken || changed;
                }
                stopGlobalDragTracking();
                selectBackground = null;
                snapshotTaken = false;
                lastDragCell = null;
                flushScheduledRecompute();
            }

            private void continueDrag() {
                if (selectBackground == null) {
                    return;
                }
                Point cell = null;
                if (globalDragListenerEventSource instanceof MouseEvent) {
                    cell = toOriginalCell((MouseEvent) globalDragListenerEventSource, true);
                }
                if (cell == null) {
                    return;
                }
                boolean changed = applyBackgroundCellSelectionSegmentByCell(lastDragCell, cell, selectBackground, !snapshotTaken);
                snapshotTaken = snapshotTaken || changed;
                lastDragCell = cell;
            }

            private void startGlobalDragTracking() {
                if (globalDragListener != null) {
                    return;
                }
                globalDragListener = event -> {
                    if (!(event instanceof MouseEvent)) {
                        return;
                    }
                    MouseEvent mouseEvent = (MouseEvent) event;
                    if (mouseEvent.getID() == MouseEvent.MOUSE_DRAGGED) {
                        globalDragListenerEventSource = mouseEvent;
                        continueDrag();
                    }
                };
                Toolkit.getDefaultToolkit().addAWTEventListener(globalDragListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
            }

            private void stopGlobalDragTracking() {
                if (globalDragListener == null) {
                    return;
                }
                Toolkit.getDefaultToolkit().removeAWTEventListener(globalDragListener);
                globalDragListener = null;
            }
            private MouseEvent globalDragListenerEventSource;
        };

        originalImagePanel.addMouseListener(mouseAdapter);
        originalImagePanel.addMouseMotionListener(mouseAdapter);
    }

    private void installResultInteraction() {
        MouseAdapter mouseAdapter = new MouseAdapter() {

            private boolean erasing;
            private boolean snapshotTaken;
            private Point lastDragPoint;
            private AWTEventListener globalDragListener;

            @Override
            public void mouseMoved(MouseEvent event) {
                updateHover(event.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoveredImagePoint = null;
                hoveredWandMask = null;
                resultImagePanel.repaint();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    return;
                }

                Point point = toResultImagePoint(event, true);
                if (point == null) {
                    return;
                }

                hoveredImagePoint = point;
                lastDragPoint = point;
                if (activeTool == Tool.ERASER) {
                    erasing = true;
                    snapshotTaken = applyEraserLine(point, point, true);
                    startGlobalDragTracking();
                } else {
                    hoveredWandMask = buildMagicWandSelection(point.x, point.y);
                    if (applySelectionToDeletedMask(hoveredWandMask, true)) {
                        recomputeAndRepaint();
                    } else {
                        resultImagePanel.repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                Point point = resultImagePanel.toNearestInteractiveImagePoint(event.getPoint());
                if (point == null) {
                    hoveredImagePoint = null;
                    hoveredWandMask = null;
                    resultImagePanel.repaint();
                    return;
                }

                hoveredImagePoint = point;
                if (activeTool == Tool.ERASER && erasing) {
                    // Do not short-circuit the apply call after the first change, or later drag points are ignored.
                    boolean changed = applyEraserLine(lastDragPoint, point, !snapshotTaken);
                    snapshotTaken = snapshotTaken || changed;
                } else if (activeTool == Tool.MAGIC_WAND) {
                    hoveredWandMask = buildMagicWandSelection(point.x, point.y);
                    resultImagePanel.repaint();
                }
                lastDragPoint = point;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Point point = toResultImagePoint(event, true);
                if (erasing && lastDragPoint != null && point != null) {
                    boolean changed = applyEraserLine(lastDragPoint, point, !snapshotTaken);
                    snapshotTaken = snapshotTaken || changed;
                }
                stopGlobalDragTracking();
                erasing = false;
                snapshotTaken = false;
                lastDragPoint = null;
                flushScheduledRecompute();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                if (activeTool == Tool.ERASER) {
                    int updatedRadius = Math.max(MIN_ERASER_RADIUS, Math.min(MAX_ERASER_RADIUS, eraserRadius - event.getWheelRotation()));
                    if (updatedRadius != eraserRadius) {
                        eraserRadius = updatedRadius;
                        updateLabels();
                        resultImagePanel.repaint();
                    }
                } else if (activeTool == Tool.MAGIC_WAND) {
                    int updatedTolerance = Math.max(1, Math.min(255, wandColorTolerance - event.getWheelRotation()));
                    if (updatedTolerance != wandColorTolerance) {
                        wandColorTolerance = updatedTolerance;
                        if (hoveredImagePoint != null) {
                            hoveredWandMask = buildMagicWandSelection(hoveredImagePoint.x, hoveredImagePoint.y);
                        }
                        updateLabels();
                        resultImagePanel.repaint();
                    }
                }
            }

            private void updateHover(Point panelPoint) {
                Point point = resultImagePanel.toInteractiveImagePoint(panelPoint);
                hoveredImagePoint = point;
                hoveredWandMask = point == null || activeTool == Tool.ERASER ? null : buildMagicWandSelection(point.x, point.y);
                resultImagePanel.repaint();
            }

            private void continueDrag() {
                if (!erasing) {
                    return;
                }
                Point point = null;
                if (globalDragListenerEventSource instanceof MouseEvent) {
                    point = toResultImagePoint((MouseEvent) globalDragListenerEventSource, true);
                }
                if (point == null) {
                    return;
                }
                hoveredImagePoint = point;
                boolean changed = applyEraserLine(lastDragPoint, point, !snapshotTaken);
                snapshotTaken = snapshotTaken || changed;
                lastDragPoint = point;
            }

            private void startGlobalDragTracking() {
                if (globalDragListener != null) {
                    return;
                }
                globalDragListener = event -> {
                    if (!(event instanceof MouseEvent)) {
                        return;
                    }
                    MouseEvent mouseEvent = (MouseEvent) event;
                    if (mouseEvent.getID() == MouseEvent.MOUSE_DRAGGED) {
                        globalDragListenerEventSource = mouseEvent;
                        continueDrag();
                    }
                };
                Toolkit.getDefaultToolkit().addAWTEventListener(globalDragListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
            }

            private void stopGlobalDragTracking() {
                if (globalDragListener == null) {
                    return;
                }
                Toolkit.getDefaultToolkit().removeAWTEventListener(globalDragListener);
                globalDragListener = null;
            }
            private MouseEvent globalDragListenerEventSource;
        };

        resultImagePanel.addMouseListener(mouseAdapter);
        resultImagePanel.addMouseMotionListener(mouseAdapter);
        resultImagePanel.addMouseWheelListener(mouseAdapter);
    }

    private void setActiveTool(Tool tool) {
        activeTool = tool;
        if (tool == Tool.ERASER) {
            hoveredWandMask = null;
        }
        updateLabels();
        resultImagePanel.repaint();
    }

    private void exportRenderedImage() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Export Transparent PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        File outputFile = ensurePngExtension(selectedFile);
        boolean success;
        try {
            success = ImageIO.write(renderedImage, "png", outputFile);
        } catch (IOException exception) {
            exception.printStackTrace();
            success = false;
        }

        if (success) {
            JOptionPane.showMessageDialog(this, "Image saved.");
        } else {
            JOptionPane.showMessageDialog(this, "Image save failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        recomputeExecutor.shutdownNow();
        super.dispose();
    }

    private void pushUndoSnapshot() {
        undoStack.push(new EditorState(copyMask(manualDeletedMask), copyMask(backgroundCellSelectionMask)));
        redoStack.clear();
        updateUndoRedoButtons();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }

        redoStack.push(new EditorState(copyMask(manualDeletedMask), copyMask(backgroundCellSelectionMask)));
        EditorState state = undoStack.pop();
        manualDeletedMask = copyMask(state.manualDeletedMask);
        backgroundCellSelectionMask = copyMask(state.backgroundCellSelectionMask);
        recomputeAndRepaint();
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }

        undoStack.push(new EditorState(copyMask(manualDeletedMask), copyMask(backgroundCellSelectionMask)));
        EditorState state = redoStack.pop();
        manualDeletedMask = copyMask(state.manualDeletedMask);
        backgroundCellSelectionMask = copyMask(state.backgroundCellSelectionMask);
        recomputeAndRepaint();
    }

    private boolean applyBackgroundCellSelection(int column, int row, boolean selected) {
        if (column < 0 || column >= getBackgroundCellColumnCount() || row < 0 || row >= getBackgroundCellRowCount()) {
            return false;
        }
        if (backgroundCellSelectionMask[row][column] == selected) {
            return false;
        }
        backgroundCellSelectionMask[row][column] = selected;
        return true;
    }

    private boolean applyBackgroundCellSelectionSegmentByCell(Point fromCell, Point toCell, boolean selected, boolean takeSnapshot) {
        boolean changed = false;
        int steps = Math.max(Math.abs(toCell.x - fromCell.x), Math.abs(toCell.y - fromCell.y));
        for (int step = 0; step <= steps; step++) {
            double ratio = steps == 0 ? 0.0d : step / (double) steps;
            int column = (int) Math.round(fromCell.x + ((toCell.x - fromCell.x) * ratio));
            int row = (int) Math.round(fromCell.y + ((toCell.y - fromCell.y) * ratio));
            if (wouldChangeBackgroundCellSelection(column, row, selected)) {
                if (takeSnapshot && !changed) {
                    pushUndoSnapshot();
                }
                applyBackgroundCellSelection(column, row, selected);
                changed = true;
            }
        }
        if (changed) {
            scheduleRecompute();
            originalImagePanel.repaint();
            resultImagePanel.repaint();
        }
        return changed;
    }

    private boolean applyEraserAt(Point point) {
        int minX = Math.max(0, point.x - eraserRadius);
        int minY = Math.max(0, point.y - eraserRadius);
        int maxX = Math.min(sourceImage.getWidth() - 1, point.x + eraserRadius);
        int maxY = Math.min(sourceImage.getHeight() - 1, point.y + eraserRadius);
        boolean changed = false;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - point.x;
                int dy = y - point.y;
                if ((dx * dx) + (dy * dy) <= eraserRadius * eraserRadius && !manualDeletedMask[y][x]) {
                    changed = true;
                }
            }
        }
        if (!changed) {
            return false;
        }
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - point.x;
                int dy = y - point.y;
                if ((dx * dx) + (dy * dy) <= eraserRadius * eraserRadius) {
                    manualDeletedMask[y][x] = true;
                    if (finalForegroundMask != null) {
                        finalForegroundMask[y][x] = false;
                    }
                    if (renderedImage != null) {
                        renderedImage.setRGB(x, y, 0x00000000);
                    }
                }
            }
        }
        return true;
    }

    private boolean applyEraserLine(Point fromPoint, Point toPoint, boolean takeSnapshot) {
        boolean changed = false;
        int steps = Math.max(Math.abs(toPoint.x - fromPoint.x), Math.abs(toPoint.y - fromPoint.y));
        for (int step = 0; step <= steps; step++) {
            double ratio = steps == 0 ? 0.0d : step / (double) steps;
            int x = (int) Math.round(fromPoint.x + ((toPoint.x - fromPoint.x) * ratio));
            int y = (int) Math.round(fromPoint.y + ((toPoint.y - fromPoint.y) * ratio));
            Point point = new Point(x, y);
            if (wouldApplyEraserAt(point)) {
                if (takeSnapshot && !changed) {
                    pushUndoSnapshot();
                }
                applyEraserAt(point);
                changed = true;
            }
        }
        if (changed) {
            scheduleRecompute();
            resultImagePanel.repaint();
        } else {
            resultImagePanel.repaint();
        }
        return changed;
    }

    private void scheduleRecompute() {
        recomputePending = true;
    }

    private void flushScheduledRecompute() {
        if (!recomputePending) {
            return;
        }
        recomputePending = false;
        startRecomputeAsync();
    }

    private boolean wouldChangeBackgroundCellSelection(int column, int row, boolean selected) {
        return column >= 0
                && column < getBackgroundCellColumnCount()
                && row >= 0
                && row < getBackgroundCellRowCount()
                && backgroundCellSelectionMask[row][column] != selected;
    }

    private boolean wouldApplyEraserAt(Point point) {
        int minX = Math.max(0, point.x - eraserRadius);
        int minY = Math.max(0, point.y - eraserRadius);
        int maxX = Math.min(sourceImage.getWidth() - 1, point.x + eraserRadius);
        int maxY = Math.min(sourceImage.getHeight() - 1, point.y + eraserRadius);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - point.x;
                int dy = y - point.y;
                if ((dx * dx) + (dy * dy) <= eraserRadius * eraserRadius && !manualDeletedMask[y][x]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean applySelectionToDeletedMask(boolean[][] selection, boolean takeSnapshot) {
        if (selection == null) {
            return false;
        }
        boolean changed = false;
        for (int y = 0; y < selection.length && !changed; y++) {
            for (int x = 0; x < selection[y].length; x++) {
                if (selection[y][x] && !manualDeletedMask[y][x]) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) {
            return false;
        }
        if (takeSnapshot) {
            pushUndoSnapshot();
        }
        for (int y = 0; y < selection.length; y++) {
            for (int x = 0; x < selection[y].length; x++) {
                if (selection[y][x]) {
                    manualDeletedMask[y][x] = true;
                }
            }
        }
        return true;
    }

    private void recomputeAndRepaint() {
        recomputePending = false;
        startRecomputeAsync();
    }

    private void startRecomputeAsync() {
        RecomputeSnapshot snapshot = new RecomputeSnapshot(
                copyMask(manualDeletedMask),
                copyMask(backgroundCellSelectionMask),
                northEdgeCheckbox.isSelected(),
                southEdgeCheckbox.isSelected(),
                eastEdgeCheckbox.isSelected(),
                westEdgeCheckbox.isSelected(),
                getEdgeWidth()
        );
        int requestId = ++latestRecomputeRequestId;

        try {
            recomputeExecutor.submit(() -> {
                RecomputeResult result = computeRecomputeResult(snapshot);
                SwingUtilities.invokeLater(() -> applyRecomputeResult(requestId, result));
            });
        } catch (RejectedExecutionException exception) {
            // Frame is shutting down; ignore late recompute requests.
        }
    }

    private RecomputeResult computeRecomputeResult(RecomputeSnapshot snapshot) {
        BackgroundReferenceMask backgroundReferenceMask = buildBackgroundReferenceMask(snapshot);
        BackgroundStatistics statistics = BackgroundSubtractor.computeBackgroundStatistics(sourceImage, backgroundReferenceMask.mask);
        boolean[][] automaticMask = BackgroundSubtractor.createForegroundMask(sourceImage, statistics, BACKGROUND_TOLERANCE);
        boolean[][] maskAfterManualDeletion = BackgroundSubtractor.applyDeletedMask(automaticMask, snapshot.manualDeletedMask);
        boolean[][] computedForegroundMask = BackgroundSubtractor.removeSmallForegroundComponents(maskAfterManualDeletion, CLEANUP_SIZE);
        BufferedImage computedRenderedImage = BackgroundSubtractor.renderMaskedImage(sourceImage, computedForegroundMask);
        return new RecomputeResult(computedForegroundMask, computedRenderedImage, backgroundReferenceMask.usingFallbackBackgroundReference);
    }

    private void applyRecomputeResult(int requestId, RecomputeResult result) {
        if (requestId != latestRecomputeRequestId) {
            return;
        }

        finalForegroundMask = result.finalForegroundMask;
        renderedImage = result.renderedImage;
        usingFallbackBackgroundReference = result.usingFallbackBackgroundReference;
        hoveredWandMask = hoveredImagePoint != null && activeTool == Tool.MAGIC_WAND
                ? buildMagicWandSelection(hoveredImagePoint.x, hoveredImagePoint.y)
                : null;
        updateLabels();
        updateUndoRedoButtons();
        originalImagePanel.repaint();
        resultImagePanel.repaint();
    }

    private BackgroundReferenceMask buildBackgroundReferenceMask(RecomputeSnapshot snapshot) {
        boolean[][] mask = new boolean[sourceImage.getHeight()][sourceImage.getWidth()];
        boolean fallback = false;
        applyEdgeSelection(mask, snapshot.northEdgeSelected, snapshot.southEdgeSelected, snapshot.eastEdgeSelected, snapshot.westEdgeSelected, snapshot.edgeWidth);

        for (int row = 0; row < snapshot.backgroundCellSelectionMask.length; row++) {
            for (int column = 0; column < snapshot.backgroundCellSelectionMask[row].length; column++) {
                if (!snapshot.backgroundCellSelectionMask[row][column]) {
                    continue;
                }
                int startX = column * GRID_CELL_SIZE;
                int startY = row * GRID_CELL_SIZE;
                int endX = Math.min(sourceImage.getWidth(), startX + GRID_CELL_SIZE);
                int endY = Math.min(sourceImage.getHeight(), startY + GRID_CELL_SIZE);
                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        mask[y][x] = true;
                    }
                }
            }
        }

        if (!hasAnyTrue(mask)) {
            fallback = true;
            applyEdgeSelection(mask, true, true, true, true, 1);
        }
        return new BackgroundReferenceMask(mask, fallback);
    }

    private void applyEdgeSelection(boolean[][] mask, boolean north, boolean south, boolean east, boolean west, int edgeWidth) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int size = Math.max(1, edgeWidth);

        if (north) {
            for (int y = 0; y < Math.min(size, height); y++) {
                for (int x = 0; x < width; x++) {
                    mask[y][x] = true;
                }
            }
        }
        if (south) {
            for (int y = Math.max(0, height - size); y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mask[y][x] = true;
                }
            }
        }
        if (west) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < Math.min(size, width); x++) {
                    mask[y][x] = true;
                }
            }
        }
        if (east) {
            for (int y = 0; y < height; y++) {
                for (int x = Math.max(0, width - size); x < width; x++) {
                    mask[y][x] = true;
                }
            }
        }
    }

    private void updateLabels() {
        parameterInfoLabel.setText("Background tolerance: " + BACKGROUND_TOLERANCE + " | Cleanup: " + CLEANUP_SIZE + " | Wand tolerance: " + wandColorTolerance + " | Grid: " + GRID_CELL_SIZE + "px");
        backgroundInfoLabel.setText(usingFallbackBackgroundReference
                ? "Background reference: none selected, using fallback 1px border"
                : "Background reference: edges " + buildSelectedEdgesLabel() + " @ " + getEdgeWidth() + "px | Selected cells: " + getSelectedBackgroundCellCount());
        toolInfoLabel.setText(activeTool == Tool.ERASER
                ? "Original: left-drag toggles grid cells | Result: drag eraser, mouse wheel changes radius (" + eraserRadius + "px)"
                : "Original: left-drag toggles grid cells | Result: click a contiguous color region, mouse wheel changes threshold (" + wandColorTolerance + ")");
    }

    private void updateUndoRedoButtons() {
        undoButton.setEnabled(!undoStack.isEmpty());
        redoButton.setEnabled(!redoStack.isEmpty());
    }

    private boolean[][] buildMagicWandSelection(int startX, int startY) {
        if (!isInsideImage(startX, startY) || renderedImage == null) {
            return null;
        }
        boolean[][] visibleMask = FuzzySelection.buildVisibleMask(renderedImage);
        if (!visibleMask[startY][startX]) {
            return null;
        }
        return FuzzySelection.extractColorConstrainedComponent(
                renderedImage,
                visibleMask,
                startX,
                startY,
                wandColorTolerance
        );
    }

    private boolean isInsideImage(int x, int y) {
        return x >= 0 && x < sourceImage.getWidth() && y >= 0 && y < sourceImage.getHeight();
    }

    private int getEdgeWidth() {
        return ((Number) edgeWidthSpinner.getValue()).intValue();
    }

    private int getBackgroundCellColumnCount() {
        return (sourceImage.getWidth() + GRID_CELL_SIZE - 1) / GRID_CELL_SIZE;
    }

    private int getBackgroundCellRowCount() {
        return (sourceImage.getHeight() + GRID_CELL_SIZE - 1) / GRID_CELL_SIZE;
    }

    private int getSelectedBackgroundCellCount() {
        int count = 0;
        for (int row = 0; row < backgroundCellSelectionMask.length; row++) {
            for (int column = 0; column < backgroundCellSelectionMask[row].length; column++) {
                if (backgroundCellSelectionMask[row][column]) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isBackgroundCellSelected(int column, int row) {
        return column >= 0
                && column < getBackgroundCellColumnCount()
                && row >= 0
                && row < getBackgroundCellRowCount()
                && backgroundCellSelectionMask[row][column];
    }

    private Point toOriginalCell(MouseEvent event, boolean nearest) {
        Point panelPoint = new Point(event.getXOnScreen(), event.getYOnScreen());
        SwingUtilities.convertPointFromScreen(panelPoint, originalImagePanel);
        return originalImagePanel.toGridCell(panelPoint, nearest);
    }

    private Point toResultImagePoint(MouseEvent event, boolean nearest) {
        Point point = new Point(event.getXOnScreen(), event.getYOnScreen());
        SwingUtilities.convertPointFromScreen(point, resultImagePanel);
        return nearest ? resultImagePanel.toNearestInteractiveImagePoint(point) : resultImagePanel.toInteractiveImagePoint(point);
    }

    private String buildSelectedEdgesLabel() {
        StringBuilder builder = new StringBuilder();
        appendSelectedEdge(builder, northEdgeCheckbox.isSelected(), "N");
        appendSelectedEdge(builder, southEdgeCheckbox.isSelected(), "S");
        appendSelectedEdge(builder, eastEdgeCheckbox.isSelected(), "E");
        appendSelectedEdge(builder, westEdgeCheckbox.isSelected(), "W");
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private void appendSelectedEdge(StringBuilder builder, boolean selected, String label) {
        if (!selected) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('/');
        }
        builder.append(label);
    }

    private Dimension calculateInitialFrameSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(Math.max((sourceImage.getWidth() * 2) + 220, 1100), (int) (screenSize.width * 0.9));
        int height = Math.min(Math.max(sourceImage.getHeight() + 220, 720), (int) (screenSize.height * 0.9));
        return new Dimension(width, height);
    }

    private void paintMaskOverlay(Graphics2D graphics2d, boolean[][] mask, ImageViewMetrics view, Color color) {
        if (mask == null) {
            return;
        }
        graphics2d.setColor(color);
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                if (mask[y][x]) {
                    int x1 = view.imageToPanelX(x);
                    int y1 = view.imageToPanelY(y);
                    int x2 = view.imageToPanelX(x + 1);
                    int y2 = view.imageToPanelY(y + 1);
                    graphics2d.fillRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
                }
            }
        }
    }

    private void paintEdgeOverlay(Graphics2D graphics2d, ImageViewMetrics view) {
        graphics2d.setColor(new Color(255, 215, 0, 55));
        int edgeWidth = getEdgeWidth();
        if (northEdgeCheckbox.isSelected()) {
            graphics2d.fillRect(view.x, view.y, view.width, Math.max(1, view.imageToPanelY(Math.min(sourceImage.getHeight(), edgeWidth)) - view.y));
        }
        if (southEdgeCheckbox.isSelected()) {
            int y = view.imageToPanelY(Math.max(0, sourceImage.getHeight() - edgeWidth));
            graphics2d.fillRect(view.x, y, view.width, Math.max(1, view.y + view.height - y));
        }
        if (westEdgeCheckbox.isSelected()) {
            graphics2d.fillRect(view.x, view.y, Math.max(1, view.imageToPanelX(Math.min(sourceImage.getWidth(), edgeWidth)) - view.x), view.height);
        }
        if (eastEdgeCheckbox.isSelected()) {
            int x = view.imageToPanelX(Math.max(0, sourceImage.getWidth() - edgeWidth));
            graphics2d.fillRect(x, view.y, Math.max(1, view.x + view.width - x), view.height);
        }
    }

    private void paintBackgroundCells(Graphics2D graphics2d, ImageViewMetrics view) {
        graphics2d.setColor(new Color(0, 110, 255, 70));
        for (int row = 0; row < backgroundCellSelectionMask.length; row++) {
            for (int column = 0; column < backgroundCellSelectionMask[row].length; column++) {
                if (!backgroundCellSelectionMask[row][column]) {
                    continue;
                }
                int startX = column * GRID_CELL_SIZE;
                int startY = row * GRID_CELL_SIZE;
                int endX = Math.min(sourceImage.getWidth(), startX + GRID_CELL_SIZE);
                int endY = Math.min(sourceImage.getHeight(), startY + GRID_CELL_SIZE);
                int x1 = view.imageToPanelX(startX);
                int y1 = view.imageToPanelY(startY);
                int x2 = view.imageToPanelX(endX);
                int y2 = view.imageToPanelY(endY);
                graphics2d.fillRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
            }
        }
    }

    private void paintGrid(Graphics2D graphics2d, ImageViewMetrics view) {
        graphics2d.setColor(new Color(255, 255, 255, 80));
        for (int x = GRID_CELL_SIZE; x < sourceImage.getWidth(); x += GRID_CELL_SIZE) {
            int panelX = view.imageToPanelX(x);
            graphics2d.drawLine(panelX, view.y, panelX, view.y + view.height);
        }
        for (int y = GRID_CELL_SIZE; y < sourceImage.getHeight(); y += GRID_CELL_SIZE) {
            int panelY = view.imageToPanelY(y);
            graphics2d.drawLine(view.x, panelY, view.x + view.width, panelY);
        }
    }

    private void paintBrushPreview(Graphics2D graphics2d, ImageViewMetrics view) {
        if (hoveredImagePoint == null || activeTool != Tool.ERASER) {
            return;
        }
        double centerX = view.imageToPanelCenterX(hoveredImagePoint.x);
        double centerY = view.imageToPanelCenterY(hoveredImagePoint.y);
        double radius = Math.max(1.0d, eraserRadius * view.scale);
        Shape brush = new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0d, radius * 2.0d);
        graphics2d.setColor(new Color(0, 120, 255, 55));
        graphics2d.fill(brush);
        graphics2d.setColor(new Color(0, 120, 255));
        graphics2d.draw(brush);
    }

    private static boolean hasAnyTrue(boolean[][] mask) {
        if (mask == null) {
            return false;
        }
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                if (mask[y][x]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JToggleButton createToolButton(String iconFileName, String toolTipText) {
        JToggleButton button = new JToggleButton(loadIcon(iconFileName));
        button.setFocusable(false);
        button.setToolTipText(toolTipText);
        button.setPreferredSize(BUTTON_DIMENSION);
        button.setMinimumSize(BUTTON_DIMENSION);
        button.setMaximumSize(BUTTON_DIMENSION);
        return button;
    }

    private static JButton createButton(String iconFileName, String toolTipText) {
        JButton button = new JButton(loadIcon(iconFileName));
        button.setFocusable(false);
        button.setToolTipText(toolTipText);
        button.setPreferredSize(BUTTON_DIMENSION);
        button.setMinimumSize(BUTTON_DIMENSION);
        button.setMaximumSize(BUTTON_DIMENSION);
        return button;
    }

    private static ImageIcon loadIcon(String iconFileName) {
        return new ImageIcon(BackgroundSubtractionEditorFrame.class.getResource("/icons/" + iconFileName));
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

    private static boolean[][] copyMask(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][source[0].length];
        for (int y = 0; y < source.length; y++) {
            System.arraycopy(source[y], 0, copy[y], 0, source[y].length);
        }
        return copy;
    }

    private static File ensurePngExtension(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".png") ? file : new File(file.getParentFile(), file.getName() + ".png");
    }

    private enum Tool {
        ERASER,
        MAGIC_WAND
    }

    private static final class EditorState {
        private final boolean[][] manualDeletedMask;
        private final boolean[][] backgroundCellSelectionMask;

        private EditorState(boolean[][] manualDeletedMask, boolean[][] backgroundCellSelectionMask) {
            this.manualDeletedMask = manualDeletedMask;
            this.backgroundCellSelectionMask = backgroundCellSelectionMask;
        }
    }

    private static final class RecomputeSnapshot {
        private final boolean[][] manualDeletedMask;
        private final boolean[][] backgroundCellSelectionMask;
        private final boolean northEdgeSelected;
        private final boolean southEdgeSelected;
        private final boolean eastEdgeSelected;
        private final boolean westEdgeSelected;
        private final int edgeWidth;

        private RecomputeSnapshot(
                boolean[][] manualDeletedMask,
                boolean[][] backgroundCellSelectionMask,
                boolean northEdgeSelected,
                boolean southEdgeSelected,
                boolean eastEdgeSelected,
                boolean westEdgeSelected,
                int edgeWidth) {
            this.manualDeletedMask = manualDeletedMask;
            this.backgroundCellSelectionMask = backgroundCellSelectionMask;
            this.northEdgeSelected = northEdgeSelected;
            this.southEdgeSelected = southEdgeSelected;
            this.eastEdgeSelected = eastEdgeSelected;
            this.westEdgeSelected = westEdgeSelected;
            this.edgeWidth = edgeWidth;
        }
    }

    private static final class BackgroundReferenceMask {
        private final boolean[][] mask;
        private final boolean usingFallbackBackgroundReference;

        private BackgroundReferenceMask(boolean[][] mask, boolean usingFallbackBackgroundReference) {
            this.mask = mask;
            this.usingFallbackBackgroundReference = usingFallbackBackgroundReference;
        }
    }

    private static final class RecomputeResult {
        private final boolean[][] finalForegroundMask;
        private final BufferedImage renderedImage;
        private final boolean usingFallbackBackgroundReference;

        private RecomputeResult(boolean[][] finalForegroundMask, BufferedImage renderedImage, boolean usingFallbackBackgroundReference) {
            this.finalForegroundMask = finalForegroundMask;
            this.renderedImage = renderedImage;
            this.usingFallbackBackgroundReference = usingFallbackBackgroundReference;
        }
    }

    private static final class ImageViewMetrics {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final double scale;

        private ImageViewMetrics(int x, int y, int width, int height, double scale) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }

        private boolean contains(Point point) {
            return point.x >= x && point.x < x + width && point.y >= y && point.y < y + height;
        }

        private int imageToPanelX(int imageX) {
            return x + (int) Math.round(imageX * scale);
        }

        private int imageToPanelY(int imageY) {
            return y + (int) Math.round(imageY * scale);
        }

        private double imageToPanelCenterX(int imageX) {
            return x + ((imageX + 0.5d) * scale);
        }

        private double imageToPanelCenterY(int imageY) {
            return y + ((imageY + 0.5d) * scale);
        }
    }

    private abstract class PreviewPanel extends JPanel {

        private PreviewPanel(String title) {
            setOpaque(true);
            setBackground(new Color(58, 58, 58));
            setBorder(BorderFactory.createTitledBorder(title));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 480);
        }

        protected final ImageViewMetrics getViewMetrics() {
            Insets insets = getInsets();
            int availableWidth = getWidth() - insets.left - insets.right - (PANEL_PADDING * 2);
            int availableHeight = getHeight() - insets.top - insets.bottom - (PANEL_PADDING * 2);
            if (availableWidth <= 0 || availableHeight <= 0) {
                return null;
            }
            double scale = Math.min(availableWidth / (double) sourceImage.getWidth(), availableHeight / (double) sourceImage.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));
            int drawX = insets.left + PANEL_PADDING + ((availableWidth - drawWidth) / 2);
            int drawY = insets.top + PANEL_PADDING + ((availableHeight - drawHeight) / 2);
            return new ImageViewMetrics(drawX, drawY, drawWidth, drawHeight, scale);
        }

        protected final Point toImagePoint(Point panelPoint) {
            ImageViewMetrics view = getViewMetrics();
            if (view == null || !view.contains(panelPoint)) {
                return null;
            }
            return toImagePoint(panelPoint, view);
        }

        protected final Point toNearestImagePoint(Point panelPoint) {
            ImageViewMetrics view = getViewMetrics();
            if (view == null) {
                return null;
            }
            int clampedX = Math.max(view.x, Math.min(view.x + view.width - 1, panelPoint.x));
            int clampedY = Math.max(view.y, Math.min(view.y + view.height - 1, panelPoint.y));
            return toImagePoint(new Point(clampedX, clampedY), view);
        }

        private Point toImagePoint(Point panelPoint, ImageViewMetrics view) {
            int imageX = (int) Math.floor((panelPoint.x - view.x) / view.scale);
            int imageY = (int) Math.floor((panelPoint.y - view.y) / view.scale);
            return new Point(
                    Math.max(0, Math.min(sourceImage.getWidth() - 1, imageX)),
                    Math.max(0, Math.min(sourceImage.getHeight() - 1, imageY))
            );
        }

    }

    private final class OriginalImagePanel extends PreviewPanel {
        private OriginalImagePanel() {
            super("Original");
        }

        private Point toGridCell(Point panelPoint, boolean nearest) {
            ImageViewMetrics view = getViewMetrics();
            if (view == null) {
                return null;
            }

            Point effectivePoint = panelPoint;
            if (nearest) {
                int clampedX = Math.max(view.x, Math.min(view.x + view.width - 1, panelPoint.x));
                int clampedY = Math.max(view.y, Math.min(view.y + view.height - 1, panelPoint.y));
                effectivePoint = new Point(clampedX, clampedY);
            } else if (!view.contains(panelPoint)) {
                return null;
            }

            double scaledCellWidth = GRID_CELL_SIZE * view.scale;
            double scaledCellHeight = GRID_CELL_SIZE * view.scale;
            int column = (int) Math.floor((effectivePoint.x - view.x) / scaledCellWidth);
            int row = (int) Math.floor((effectivePoint.y - view.y) / scaledCellHeight);
            column = Math.max(0, Math.min(getBackgroundCellColumnCount() - 1, column));
            row = Math.max(0, Math.min(getBackgroundCellRowCount() - 1, row));
            return new Point(column, row);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            ImageViewMetrics view = getViewMetrics();
            if (view == null) {
                return;
            }
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics2d.drawImage(sourceImage, view.x, view.y, view.width, view.height, null);
                paintEdgeOverlay(graphics2d, view);
                paintBackgroundCells(graphics2d, view);
                paintGrid(graphics2d, view);
                graphics2d.setColor(Color.GRAY);
                graphics2d.drawRect(view.x, view.y, view.width, view.height);
            } finally {
                graphics2d.dispose();
            }
        }
    }

    private final class ResultImagePanel extends PreviewPanel {
        private ResultImagePanel() {
            super("Transparent Result");
        }

        private Point toInteractiveImagePoint(Point panelPoint) {
            return toImagePoint(panelPoint);
        }

        private Point toNearestInteractiveImagePoint(Point panelPoint) {
            return toNearestImagePoint(panelPoint);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            ImageViewMetrics view = getViewMetrics();
            if (view == null || renderedImage == null) {
                return;
            }
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                TransparencyPreviewPainter.paintCheckerboard(graphics2d, view.x, view.y, view.width, view.height);
                graphics2d.drawImage(renderedImage, view.x, view.y, view.width, view.height, null);
                paintMaskOverlay(graphics2d, hoveredWandMask, view, new Color(0, 160, 255, 70));
                paintBrushPreview(graphics2d, view);
                graphics2d.setColor(Color.GRAY);
                graphics2d.drawRect(view.x, view.y, view.width, view.height);
            } finally {
                graphics2d.dispose();
            }
        }
    }
}

final class BackgroundSubtractor {

    private BackgroundSubtractor() {
    }

    public static BackgroundStatistics computeBackgroundStatistics(BufferedImage image, boolean[][] backgroundMask) {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(backgroundMask, "backgroundMask must not be null");

        long redSum = 0L;
        long greenSum = 0L;
        long blueSum = 0L;
        long pixelCount = 0L;

        for (int y = 0; y < backgroundMask.length; y++) {
            for (int x = 0; x < backgroundMask[y].length; x++) {
                if (!backgroundMask[y][x]) {
                    continue;
                }
                int argb = image.getRGB(x, y);
                redSum += (argb >>> 16) & 0xFF;
                greenSum += (argb >>> 8) & 0xFF;
                blueSum += argb & 0xFF;
                pixelCount++;
            }
        }

        if (pixelCount == 0L) {
            throw new IllegalArgumentException("backgroundMask must contain at least one pixel");
        }

        return new BackgroundStatistics(
                (int) (redSum / pixelCount),
                (int) (greenSum / pixelCount),
                (int) (blueSum / pixelCount)
        );
    }

    public static boolean[][] createForegroundMask(
            BufferedImage image,
            BackgroundStatistics statistics,
            int tolerance) {

        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(statistics, "statistics must not be null");

        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] mask = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                double distance = colorDistance(
                        red,
                        green,
                        blue,
                        statistics.getMeanRed(),
                        statistics.getMeanGreen(),
                        statistics.getMeanBlue()
                );

                mask[y][x] = distance > tolerance;
            }
        }

        return mask;
    }

    public static boolean[][] applyDeletedMask(boolean[][] foregroundMask, boolean[][] deletedMask) {
        Objects.requireNonNull(foregroundMask, "foregroundMask must not be null");

        boolean[][] result = copyMask(foregroundMask);
        if (deletedMask == null) {
            return result;
        }

        for (int y = 0; y < result.length; y++) {
            for (int x = 0; x < result[y].length; x++) {
                if (deletedMask[y][x]) {
                    result[y][x] = false;
                }
            }
        }

        return result;
    }

    public static boolean[][] removeSmallForegroundComponents(boolean[][] foregroundMask, int minComponentSize) {
        Objects.requireNonNull(foregroundMask, "foregroundMask must not be null");

        if (minComponentSize <= 0) {
            return copyMask(foregroundMask);
        }

        int height = foregroundMask.length;
        int width = foregroundMask[0].length;

        boolean[][] result = copyMask(foregroundMask);
        boolean[][] visited = new boolean[height][width];

        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int[] componentX = new int[width * height];
        int[] componentY = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!result[y][x] || visited[y][x]) {
                    continue;
                }

                int head = 0;
                int tail = 0;
                int componentSize = 0;

                queueX[tail] = x;
                queueY[tail] = y;
                tail++;
                visited[y][x] = true;

                while (head < tail) {
                    int currentX = queueX[head];
                    int currentY = queueY[head];
                    head++;

                    if (!result[currentY][currentX]) {
                        continue;
                    }

                    componentX[componentSize] = currentX;
                    componentY[componentSize] = currentY;
                    componentSize++;

                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        for (int offsetX = -1; offsetX <= 1; offsetX++) {
                            if (offsetX == 0 && offsetY == 0) {
                                continue;
                            }

                            int nextX = currentX + offsetX;
                            int nextY = currentY + offsetY;
                            if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height || visited[nextY][nextX]) {
                                continue;
                            }

                            visited[nextY][nextX] = true;
                            queueX[tail] = nextX;
                            queueY[tail] = nextY;
                            tail++;
                        }
                    }
                }

                if (componentSize < minComponentSize) {
                    for (int i = 0; i < componentSize; i++) {
                        result[componentY[i]][componentX[i]] = false;
                    }
                }
            }
        }

        return result;
    }

    public static BufferedImage renderMaskedImage(BufferedImage sourceImage, boolean[][] foregroundMask) {
        Objects.requireNonNull(sourceImage, "sourceImage must not be null");
        Objects.requireNonNull(foregroundMask, "foregroundMask must not be null");

        BufferedImage output = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < sourceImage.getHeight(); y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                output.setRGB(x, y, foregroundMask[y][x] ? sourceImage.getRGB(x, y) : 0x00000000);
            }
        }

        return output;
    }

    private static boolean[][] copyMask(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][source[0].length];
        for (int y = 0; y < source.length; y++) {
            System.arraycopy(source[y], 0, copy[y], 0, source[y].length);
        }
        return copy;
    }

    private static double colorDistance(
            int red1,
            int green1,
            int blue1,
            int red2,
            int green2,
            int blue2) {

        int deltaRed = red1 - red2;
        int deltaGreen = green1 - green2;
        int deltaBlue = blue1 - blue2;

        return Math.sqrt(
                deltaRed * deltaRed
                        + deltaGreen * deltaGreen
                        + deltaBlue * deltaBlue
        );
    }
}

final class BackgroundStatistics {

    private final int meanRed;
    private final int meanGreen;
    private final int meanBlue;

    BackgroundStatistics(int meanRed, int meanGreen, int meanBlue) {
        this.meanRed = meanRed;
        this.meanGreen = meanGreen;
        this.meanBlue = meanBlue;
    }

    int getMeanRed() {
        return meanRed;
    }

    int getMeanGreen() {
        return meanGreen;
    }

    int getMeanBlue() {
        return meanBlue;
    }
}
