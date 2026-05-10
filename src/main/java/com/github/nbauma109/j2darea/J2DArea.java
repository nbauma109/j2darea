package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.nbauma109.j2darea.ie.AREFile;
import com.github.nbauma109.j2darea.ie.PvrzTisFile;
import com.github.nbauma109.j2darea.ie.TISFile;
import com.github.nbauma109.j2darea.ie.WEDFile;
import com.github.nbauma109.j2darea.ie.WeiDUModPackager;

public class J2DArea extends JFrame {

    private static final String ERROR = "Error";

    private static final String PLUS = "Plus";

    private static final String MINUS = "Minus";

    private static final String UP = "Up";
    
    private static final String DOWN = "Down";
    
    private static final String USER_HOME = "user.home";
    private static final String NANO_BANANA_EXTRACTION_TITLE = "Nano Banana 2 Extraction";

    private static final Dimension MIN_SIZE = new Dimension(1200, 800);
    private static final int MAX_HISTORY_ENTRIES = 50;
    private static final int DOOR_PIXEL_SNAP_RADIUS = 10;

    public static final Dimension BUTTON_SIZE = new Dimension(25, 25);

    private static final long serialVersionUID = 1L;

    private static EnumMap<FileChooserLocation, String> directories = new EnumMap<>(FileChooserLocation.class);

    private int backgroundWidth = 5120;
    private int backgroundHeight = 3840;
    private transient BufferedImage buildBackgroundImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
    private transient BufferedImage buildBackgroundNightImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
    private transient BufferedImage texturePreviewImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
    private transient BufferedImage extractionBackgroundImage;
    private Polygon polygon = new Polygon();
    private Rectangle movingRectangle;
    private transient Tile tile = new Tile();
    private Point mousePosition = new Point();
    private List<PastedObject> pastedObjects = new ArrayList<>();
    private PastedObject objectToMove;
    private int objectToMoveIdx = -1;
    private transient WallGroupData selectedWallGroup;
    private transient Set<Point> selectedSearchMapCells = new LinkedHashSet<Point>();
    private transient Polygon movingWallGroupBasePolygon;
    private int deltaX;
    private int deltaY;
    private transient String movingCompositeGroupId;
    private transient LinkedHashMap<PastedObject, Point> movingCompositeBaseLocations = new LinkedHashMap<PastedObject, Point>();
    private transient LinkedHashMap<WallGroupData, Polygon> movingCompositeBaseWallPolygons = new LinkedHashMap<WallGroupData, Polygon>();
    private transient Point movingCompositeAnchorLocation;
    private boolean drawClosed;
    private boolean night;
    private boolean editingBlackParallelogram;
    private boolean editingTextureParallelogram;
    private transient NanoBananaEditorDialog nanoBananaEditorDialog;
    private transient String extractionGameAreaResref;
    private transient boolean extractionGameAreaClosedDoors = true;
    private List<Polygon> parallelograms = new ArrayList<>();

    // Separate collections for polygon-based area features (not PastedObjects)
    private List<RegionData> regions = new ArrayList<>();
    private List<ContainerData> containers = new ArrayList<>();
    private List<WallGroupData> wallGroups = new ArrayList<>();
    private AreaAttributes areaAttributes = new AreaAttributes();
    private SearchMapData searchMapData = new SearchMapData(backgroundWidth, backgroundHeight);

    private ExtractionCursorMode extractionCursorMode = ExtractionCursorMode.MAP_DRAG;
    private boolean showSearchMapGrid;

    private boolean painting;
    private transient JPanel buildPanel;
    private transient JPanel extractPanel;
    private transient JScrollPane buildScrollPane;
    private transient JScrollPane extractScrollPane;
    private transient JTabbedPane tabPane;
    private transient JMenuBar menubar;
    private transient JMenu editMenu;
    private transient JMenu backgroundMenu;
    private transient JMenu insertMenu;
    private transient JMenu cursorModeMenu;
    private transient JMenu viewMenu;
    private transient JMenu toolsMenu;
    private transient JMenuItem fillMenuItem;
    private transient JMenuItem openBrushTextureMenuItem;
    private transient JMenuItem tileSeamlessMenuItem;
    private transient JMenuItem saveDoorsMenuItem;
    private transient JMenuItem nanoBananaExtractionMenuItem;
    private transient JMenuItem paint3dMenuItem;
    private transient JMenuItem subtractBackgroundMenuItem;
    private transient JRadioButtonMenuItem cursorSelectMenuItem;
    private transient JRadioButtonMenuItem brushModeMenuItem;
    private transient JRadioButtonMenuItem searchMapPainterModeMenuItem;
    private transient JRadioButtonMenuItem searchMapEraserModeMenuItem;
    private transient JRadioButtonMenuItem extractionMapDragModeMenuItem;
    private transient JRadioButtonMenuItem polygonModeMenuItem;
    private transient JRadioButtonMenuItem rectangleModeMenuItem;
    private transient JCheckBoxMenuItem extractionClosedDoorMenuItem;
    private transient JCheckBoxMenuItem searchMapGridMenuItem;
    private transient LocalTransitionPlacementDialog localTransitionPlacementDialog;
    private transient JCheckBoxMenuItem drawClosedDoorMenuItem;
    private transient JCheckBoxMenuItem nightMenuItem;
    private transient JButton openBackgroundToolbarMenuButton;
    private transient JButton undoToolbarButton;
    private transient JButton redoToolbarButton;
    private transient JButton fillToolbarButton;
    private transient JButton openBrushTextureToolbarButton;
    private transient JButton exportDoorTilesToolbarButton;
    private transient JButton tileSeamlessToolbarButton;
    private transient JButton nanoBananaExtractionToolbarButton;
    private transient JButton paint3dToolbarButton;
    private transient JButton subtractBackgroundToolbarButton;
    private transient JButton regionsToolbarButton;
    private transient JButton wallGroupsToolbarButton;
    private transient JButton nonWalkableToolbarButton;
    private transient JButton pasteFromToolbarButton;
    private transient JButton pasteCompositeToolbarButton;
    private transient JButton parallelogramBlackToolbarButton;
    private transient JButton parallelogramTextureToolbarButton;
    private transient JButton pasteFromOpenDoorToolbarButton;
    private transient JButton pasteFromClosedDoorToolbarButton;
    private transient JButton pasteFromNightLightToolbarButton;
    private transient JButton entranceToolbarButton;
    private transient JToggleButton cursorToolbarButton;
    private transient JToggleButton brushToolbarButton;
    private transient JToggleButton searchMapPainterToolbarButton;
    private transient JToggleButton searchMapEraserToolbarButton;
    private transient JToggleButton extractionMapDragToolbarButton;
    private transient JToggleButton polygonToolbarButton;
    private transient JToggleButton rectangleToolbarButton;
    private transient JToggleButton extractionClosedDoorToggleButton;
    private transient JToggleButton searchMapGridToggleButton;
    private transient JToggleButton drawClosedDoorToggleButton;
    private transient JToggleButton nightToggleButton;
    private transient boolean buildPanning;
    private transient boolean buildPanDragged;
    private transient boolean suppressNextBuildClickAfterPan;
    private transient boolean buildBrushStrokeModified;
    private transient boolean buildSearchMapStrokeModified;
    private transient Point buildPanStartMouseScreen;
    private transient Point buildPanStartView;
    private transient boolean extractPanning;
    private transient boolean extractPanDragged;
    private transient boolean suppressNextExtractClickAfterPan;
    private transient boolean extractRectangleSelectionInProgress;
    private transient Point extractPanStartMouseScreen;
    private transient Point extractPanStartView;
    private transient List<Component> buildOnlyToolbarButtons = new ArrayList<Component>();
    private transient Deque<byte[]> undoHistory = new ArrayDeque<byte[]>();
    private transient Deque<byte[]> redoHistory = new ArrayDeque<byte[]>();
    private transient byte[] currentHistoryState;
    private TransitionPlacementSession transitionPlacementSession;
    private transient WallGroupPlacementSession wallGroupPlacementSession;
    private transient SearchMapSelectionSession searchMapSelectionSession;
    private transient DoorPointPlacementSession doorPointPlacementSession;
    private transient DoorEditorDialog doorEditorDialog;
    private transient PastedObject doorEditorOpenedDoorObject;
    private SearchMapEditMode searchMapEditMode = SearchMapEditMode.NONE;
    private SearchMapTileType selectedSearchMapPaintType = SearchMapTileType.NON_WALKABLE;
    private boolean showDoorPolygonOverlays = true;
    private boolean showDoorImpededBlockOverlays = true;

    private int brushRadius = 30;
    private double buildZoom = 1.0;
    private double extractZoom = 1.0;
    private transient BufferedImage backgroundTile;
    private transient BufferedImage brushTexture;
    private transient BufferedImage brushPreview;
    private transient BufferedImage brushNightPreview;

    public J2DArea() {
        super("J2DArea");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        tabPane = new JTabbedPane(SwingConstants.BOTTOM);
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        buildPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.scale(buildZoom, buildZoom);
                paintObjects(g2);
                paintSearchMapOverlay(g2);
                g2.setColor(Color.GREEN);
                for (Polygon parallelogram : parallelograms) {
                    if (parallelogram.npoints < 3) {
                        Polygon newPolygon = new Polygon(parallelogram.xpoints, parallelogram.ypoints, parallelogram.npoints);
                        newPolygon.addPoint(mousePosition.x, mousePosition.y);
                        g2.drawPolygon(newPolygon);
                    } else {
                        g2.setColor(Color.BLACK);
                        g2.fillPolygon(parallelogram);
                        g2.setColor(Color.GREEN);
                        g2.drawPolygon(parallelogram);
                    }
                }
                if (movingRectangle != null) {
                    g2.drawRect(movingRectangle.x, movingRectangle.y, movingRectangle.width, movingRectangle.height);
                }
                paintTransitionPlacementDraft(g2);
                paintWallGroupPlacementDraft(g2);
                paintSearchMapSelectionDraft(g2);
                paintDoorPointPlacementDraft(g2);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                if (buildBackgroundImage != null) {
                    return scaleDimension(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight(), buildZoom);
                }
                return scaleDimension(backgroundWidth, backgroundHeight, buildZoom);
            }
        };

        extractPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.scale(extractZoom, extractZoom);
                if (extractionBackgroundImage != null) {
                    g2.drawImage(extractionBackgroundImage, 0, 0, null);
                    if (isExtractionRectangleMode()) {
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.drawLine(mousePosition.x, 0, mousePosition.x, extractionBackgroundImage.getHeight());
                        g2.drawLine(0, mousePosition.y, extractionBackgroundImage.getWidth(), mousePosition.y);
                    }
                }
                paintExtractionPolygonDraft(g2);
                if ((extractRectangleSelectionInProgress || isValidTileSetup()) && isExtractionRectangleMode()) {
                    g2.setColor(Color.GREEN);
                    tile.draw(g2);
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                if (extractionBackgroundImage != null) {
                    return scaleDimension(extractionBackgroundImage.getWidth(), extractionBackgroundImage.getHeight(), extractZoom);
                }
                return scaleDimension(backgroundWidth, backgroundHeight, extractZoom);
            }
        };

        JPanel texturePreviewPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(texturePreviewImage, 0, 0, null);
            }
        };

        extractPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isExtractionRectangleMode() && extractRectangleSelectionInProgress) {
                    MouseEvent scaledEvent = scaleMouseEvent(e, extractPanel, extractZoom);
                    tile.moveEndPoint(scaledEvent);
                    extractPanel.repaint();
                    return;
                }
                if (isExtractionMapDragMode() && extractPanning && extractScrollPane != null
                        && extractPanStartMouseScreen != null && extractPanStartView != null) {
                    int deltaX = e.getXOnScreen() - extractPanStartMouseScreen.x;
                    int deltaY = e.getYOnScreen() - extractPanStartMouseScreen.y;
                    if (deltaX != 0 || deltaY != 0) {
                        extractPanDragged = true;
                    }
                    JViewport viewport = extractScrollPane.getViewport();
                    Dimension preferredSize = extractPanel.getPreferredSize();
                    int maxX = Math.max(0, preferredSize.width - viewport.getWidth());
                    int maxY = Math.max(0, preferredSize.height - viewport.getHeight());
                    int viewX = Math.max(0, Math.min(extractPanStartView.x - deltaX, maxX));
                    int viewY = Math.max(0, Math.min(extractPanStartView.y - deltaY, maxY));
                    viewport.setViewPosition(new Point(viewX, viewY));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point areaPoint = toAreaPoint(e, extractZoom);
                mousePosition.move(areaPoint.x, areaPoint.y);
                extractPanel.repaint();
            }
        });

        extractPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (isExtractionRectangleMode()) {
                        MouseEvent scaledEvent = scaleMouseEvent(e, extractPanel, extractZoom);
                        tile.moveStartPoint(scaledEvent);
                        tile.moveEndPoint(scaledEvent);
                        extractRectangleSelectionInProgress = true;
                        extractPanel.repaint();
                    } else if (isExtractionMapDragMode()) {
                        extractPanning = true;
                        extractPanDragged = false;
                        extractPanStartMouseScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
                        extractPanStartView = extractScrollPane != null ? extractScrollPane.getViewport().getViewPosition() : new Point();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isExtractionRectangleMode() && extractRectangleSelectionInProgress) {
                    MouseEvent scaledEvent = scaleMouseEvent(e, extractPanel, extractZoom);
                    tile.moveEndPoint(scaledEvent);
                    extractRectangleSelectionInProgress = false;
                    updateTexturePreviewFromTileSelection();
                    extractPanel.repaint();
                    return;
                }
                boolean dragged = extractPanDragged;
                if (extractPanning) {
                    extractPanning = false;
                    extractPanStartMouseScreen = null;
                    extractPanStartView = null;
                    if (dragged) {
                        suppressNextExtractClickAfterPan = true;
                    }
                    extractPanDragged = false;
                }
                if (!dragged && isExtractionPolygonMode()) {
                    handleExtractPanelClick(e, extractPanel);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });

        extractPanel.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                e.consume();
                double oldZoom = extractZoom;
                double zoomFactor = UserPreferences.getZoomFactor();
                double newZoom = clampZoom(oldZoom * (e.getWheelRotation() < 0 ? zoomFactor : 1.0 / zoomFactor));
                if (Math.abs(newZoom - oldZoom) < 0.0001) {
                    return;
                }
                extractZoom = newZoom;
                applyZoom(extractScrollPane, extractPanel, oldZoom, newZoom);
            }
        });

        MouseAdapter buildPanelMouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
                if (handleDoorCanvasMousePressed(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                if (searchMapSelectionSession != null) {
                    return;
                }
                if (wallGroupPlacementSession != null) {
                    return;
                }
                if (transitionPlacementSession != null) {
                    return;
                }
                if (showBuildPanelContextMenu(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                if (!painting && searchMapEditMode == SearchMapEditMode.NONE && !editingBlackParallelogram && !editingTextureParallelogram
                        && objectToMove == null
                    && selectedWallGroup == null
                    && !hasSelectedSearchMapCells()
                    && SwingUtilities.isLeftMouseButton(e)
                    && findPastedObjectAtPoint(scaledEvent.getX(), scaledEvent.getY()) == null
                    && findWallGroupAtPoint(scaledEvent.getPoint()) == null
                    && !isImpededSearchMapCellAtPoint(scaledEvent.getPoint())) {
                    buildPanning = true;
                    buildPanDragged = false;
                    buildPanStartMouseScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
                    buildPanStartView = buildScrollPane != null ? buildScrollPane.getViewport().getViewPosition() : new Point();
                    return;
                }
                if (painting) {
                    updateBrushStroke(scaledEvent);
                    repaint();
                } else if (isSearchMapEditMode()) {
                    updateSearchMapCellStroke(scaledEvent);
                    repaint();
                }
            }

            public void updateBrushStroke(MouseEvent e) {
                if (brushTexture == null) {
                    return;
                }
                ensureSearchMapSized();
                buildBrushStrokeModified = true;
                for (int x = e.getX() - brushRadius; x < e.getX() + brushRadius; x++) {
                    for (int y = e.getY() - brushRadius; y < e.getY() + brushRadius; y++) {
                        double dist = Point2D.distance(x, y, e.getX(), e.getY());
                        if (dist < brushRadius && x >= 0 && y >= 0 && x < buildBackgroundImage.getWidth() && y < buildBackgroundImage.getHeight()) {
                            double blend = 1.0 - dist / brushRadius;

                            Color background = new Color(buildBackgroundImage.getRGB(x, y));
                            Color brush = new Color(brushTexture.getRGB(x % brushTexture.getWidth(), y % brushTexture.getHeight()));

                            int r = (int) (background.getRed() * (1.0 - blend) + brush.getRed() * blend);
                            int g = (int) (background.getGreen() * (1.0 - blend) + brush.getGreen() * blend);
                            int b = (int) (background.getBlue() * (1.0 - blend) + brush.getBlue() * blend);
                            int a = (int) (background.getAlpha() * (1.0 - blend) + brush.getAlpha() * blend);

                            buildBackgroundImage.setRGB(x, y, new Color(r, g, b, a).getRGB());
                            buildBackgroundNightImage.setRGB(x, y, new Color((int) (0.45 * r), (int) (0.45 * g), (int) (0.85 * b), a).getRGB());
                        }
                    }
                }
                searchMapData.applyCircleType(
                    e.getX(),
                    e.getY(),
                    brushRadius,
                    SearchMapTileType.classifyTexture(brushTexture)
                );
            }

            public void updateSearchMapCellStroke(MouseEvent e) {
                ensureSearchMapSized();
                int tileX = e.getX() / SearchMapData.CELL_WIDTH;
                int tileY = e.getY() / SearchMapData.CELL_HEIGHT;
                if (tileX < 0 || tileY < 0 || tileX >= searchMapData.getWidthInTiles() || tileY >= searchMapData.getHeightInTiles()) {
                    return;
                }
                clearSelectedSearchMapCells();
                if (searchMapEditMode == SearchMapEditMode.PAINTER) {
                    searchMapData.setOverrideTileType(tileX, tileY, selectedSearchMapPaintType);
                    buildSearchMapStrokeModified = true;
                } else if (searchMapEditMode == SearchMapEditMode.ERASER) {
                    searchMapData.resetOverrideTileType(tileX, tileY);
                    buildSearchMapStrokeModified = true;
                }
            }


            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
                if (handleDoorCanvasMouseReleased(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                boolean dragged = buildPanDragged;
                if (buildPanning) {
                    buildPanning = false;
                    buildPanStartMouseScreen = null;
                    buildPanStartView = null;
                    if (dragged) {
                        suppressNextBuildClickAfterPan = true;
                    }
                    buildPanDragged = false;
                }
                if (dragged) {
                    return;
                }
                if (painting) {
                    if (buildBrushStrokeModified) {
                        buildBrushStrokeModified = false;
                        recordHistoryState();
                    }
                    return;
                }
                if (isSearchMapEditMode()) {
                    if (buildSearchMapStrokeModified) {
                        buildSearchMapStrokeModified = false;
                        recordHistoryState();
                    }
                    return;
                }
                if (showBuildPanelContextMenu(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                handleBuildPanelClick(e, buildPanel);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point areaPoint = toAreaPoint(e, buildZoom);
                mousePosition.move(areaPoint.x, areaPoint.y);
                if (doorPointPlacementSession != null) {
                    buildPanel.repaint();
                }
                if (objectToMove != null) {
                    int newRectX = areaPoint.x - deltaX;
                    int newRectY = areaPoint.y - deltaY;
                    if (movingCompositeGroupId != null && movingCompositeAnchorLocation != null && !movingCompositeBaseLocations.isEmpty()) {
                        int offsetX = newRectX - movingCompositeAnchorLocation.x;
                        int offsetY = newRectY - movingCompositeAnchorLocation.y;
                        for (Map.Entry<PastedObject, Point> entry : movingCompositeBaseLocations.entrySet()) {
                            Point baseLocation = entry.getValue();
                            setPastedObjectLocation(entry.getKey(), baseLocation.x + offsetX, baseLocation.y + offsetY);
                        }
                        for (Map.Entry<WallGroupData, Polygon> entry : movingCompositeBaseWallPolygons.entrySet()) {
                            entry.getKey().setPolygon(PolygonUtils.translatedPolygon(entry.getValue(), offsetX, offsetY));
                        }
                    } else {
                        setPastedObjectLocation(objectToMove, newRectX, newRectY);
                    }
                    Rectangle anchorRect = getPastedObjectBounds(objectToMove);
                    movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
                } else if (selectedWallGroup != null) {
                    int newRectX = areaPoint.x - deltaX;
                    int newRectY = areaPoint.y - deltaY;
                    if (movingCompositeGroupId != null && movingCompositeAnchorLocation != null && !movingCompositeBaseWallPolygons.isEmpty()) {
                        int offsetX = newRectX - movingCompositeAnchorLocation.x;
                        int offsetY = newRectY - movingCompositeAnchorLocation.y;
                        for (Map.Entry<PastedObject, Point> entry : movingCompositeBaseLocations.entrySet()) {
                            Point baseLocation = entry.getValue();
                            setPastedObjectLocation(entry.getKey(), baseLocation.x + offsetX, baseLocation.y + offsetY);
                        }
                        for (Map.Entry<WallGroupData, Polygon> entry : movingCompositeBaseWallPolygons.entrySet()) {
                            entry.getKey().setPolygon(PolygonUtils.translatedPolygon(entry.getValue(), offsetX, offsetY));
                        }
                    } else if (movingWallGroupBasePolygon != null) {
                        Rectangle baseBounds = movingWallGroupBasePolygon.getBounds();
                        selectedWallGroup.setPolygon(PolygonUtils.translatedPolygon(
                            movingWallGroupBasePolygon,
                            newRectX - baseBounds.x,
                            newRectY - baseBounds.y));
                    }
                    Rectangle anchorRect = selectedWallGroup.getPolygon().getBounds();
                    movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
                if (handleDoorCanvasMouseDragged(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                if (transitionPlacementSession != null) {
                    return;
                }
                if (buildPanning && buildScrollPane != null && buildPanStartMouseScreen != null && buildPanStartView != null) {
                    int deltaX = e.getXOnScreen() - buildPanStartMouseScreen.x;
                    int deltaY = e.getYOnScreen() - buildPanStartMouseScreen.y;
                    if (deltaX != 0 || deltaY != 0) {
                        buildPanDragged = true;
                    }
                    JViewport viewport = buildScrollPane.getViewport();
                    Dimension preferredSize = buildPanel.getPreferredSize();
                    int maxX = Math.max(0, preferredSize.width - viewport.getWidth());
                    int maxY = Math.max(0, preferredSize.height - viewport.getHeight());
                    int viewX = Math.max(0, Math.min(buildPanStartView.x - deltaX, maxX));
                    int viewY = Math.max(0, Math.min(buildPanStartView.y - deltaY, maxY));
                    viewport.setViewPosition(new Point(viewX, viewY));
                    return;
                }
                if (painting) {
                    updateBrushStroke(scaledEvent);
                    repaint();
                } else if (isSearchMapEditMode()) {
                    updateSearchMapCellStroke(scaledEvent);
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isShiftDown() && objectToMove != null) {
                    e.consume();
                    objectToMove.flip();
                    recordHistoryState();
                    buildPanel.repaint();
                } else if (e.isShiftDown() && painting) {                
                    e.consume();
                    brushRadius += e.getWheelRotation();
                    buildBrushPreview();
                    buildPanel.repaint();
                } else {
                    e.consume();
                    double oldZoom = buildZoom;
                    double zoomFactor = UserPreferences.getZoomFactor();
                    double newZoom = clampZoom(oldZoom * (e.getWheelRotation() < 0 ? zoomFactor : 1.0 / zoomFactor));
                    if (Math.abs(newZoom - oldZoom) < 0.0001) {
                        return;
                    }
                    buildZoom = newZoom;
                    applyZoom(buildScrollPane, buildPanel, oldZoom, newZoom);
                }
            }

        };
        buildPanel.addMouseListener(buildPanelMouseAdapter);

        buildPanel.addMouseMotionListener(buildPanelMouseAdapter);

        buildPanel.addMouseWheelListener(buildPanelMouseAdapter);

        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), PLUS);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK), PLUS);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), MINUS);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0), MINUS);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), UP);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), DOWN);
        buildPanel.getActionMap().put("Delete", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean removed = false;
                if (objectToMove != null) {
                    pastedObjects.remove(objectToMove);
                    movingRectangle = null;
                    objectToMove = null;
                    objectToMoveIdx = -1;
                    clearCompositeMove();
                    removed = true;
                } else if (selectedWallGroup != null) {
                    wallGroups.remove(selectedWallGroup);
                    selectedWallGroup = null;
                    movingWallGroupBasePolygon = null;
                    movingRectangle = null;
                    clearCompositeMove();
                    removed = true;
                } else if (hasSelectedSearchMapCells()) {
                    searchMapData.clearResolvedOverrides(new ArrayList<Point>(selectedSearchMapCells));
                    clearSelectedSearchMapCells();
                    removed = true;
                }
                if (removed) {
                    recordHistoryState();
                    buildPanel.repaint();
                }
            }
        });
        buildPanel.getActionMap().put(PLUS, new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (objectToMove != null) {
                    if (objectToMoveIdx >= 0 && objectToMoveIdx < pastedObjects.size() - 1) {
                        PastedObject tmp = pastedObjects.get(objectToMoveIdx + 1);
                        pastedObjects.set(objectToMoveIdx + 1, objectToMove);
                        pastedObjects.set(objectToMoveIdx, tmp);
                        objectToMoveIdx++;
                    }
                    recordHistoryState();
                    buildPanel.repaint();
                }
            }
        });
        buildPanel.getActionMap().put(MINUS, new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (objectToMove != null) {
                    if (objectToMoveIdx > 0 && objectToMoveIdx < pastedObjects.size()) {
                        PastedObject tmp = pastedObjects.get(objectToMoveIdx - 1);
                        pastedObjects.set(objectToMoveIdx - 1, objectToMove);
                        pastedObjects.set(objectToMoveIdx, tmp);
                        objectToMoveIdx--;
                    }
                    recordHistoryState();
                    buildPanel.repaint();
                }
            }
        });
        buildPanel.getActionMap().put(UP, new AbstractAction() {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (objectToMove != null) {
                    if (objectToMoveIdx > 0 && objectToMoveIdx < pastedObjects.size()) {
                        pastedObjects.get(objectToMoveIdx).adjustUpwards();
                    }
                    recordHistoryState();
                    buildPanel.repaint();
                }
            }
        });
        buildPanel.getActionMap().put(DOWN, new AbstractAction() {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (objectToMove != null) {
                    if (objectToMoveIdx > 0 && objectToMoveIdx < pastedObjects.size()) {
                        pastedObjects.get(objectToMoveIdx).adjustDownwards();
                    }
                    recordHistoryState();
                    buildPanel.repaint();
                }
            }
        });

        buildPanel.setLayout(new GridLayout());
        buildPanel.setBackground(Color.BLACK);
        buildScrollPane = new JScrollPane(buildPanel);
        configureCanvasScrollPane(buildScrollPane);
        extractPanel.setLayout(new GridLayout());
        extractPanel.setBackground(Color.BLACK);
        extractScrollPane = new JScrollPane(extractPanel);
        configureCanvasScrollPane(extractScrollPane);
        tabPane.addTab("Build Area", buildScrollPane);
        tabPane.addTab("Extraction Area", extractScrollPane);
        tabPane.addTab("Texture Preview", new JScrollPane(texturePreviewPanel));

        menubar = new JMenuBar();
        setJMenuBar(menubar);
        JMenu fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        backgroundMenu = new JMenu("Background");
        insertMenu = new JMenu("Insert");
        cursorModeMenu = new JMenu("Cursor Mode");
        viewMenu = new JMenu("View");
        toolsMenu = new JMenu("Tools");
        JMenu settingsMenu = new JMenu("Settings");
        JMenu helpMenu = new JMenu("Help");
        menubar.add(fileMenu);
        menubar.add(editMenu);
        menubar.add(backgroundMenu);
        menubar.add(insertMenu);
        menubar.add(cursorModeMenu);
        menubar.add(viewMenu);
        menubar.add(toolsMenu);
        menubar.add(settingsMenu);
        menubar.add(helpMenu);
        menubar.add(Box.createHorizontalStrut(8));
        menubar.add(Box.createHorizontalGlue());
        JButton undoButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/undo.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                performUndo();
            }
        });
        undoButton.setToolTipText("Undo");
        configureToolbarButton(undoButton);
        undoToolbarButton = undoButton;
        JMenuItem undoMenuItem = new JMenuItem(undoButton.getAction());
        undoMenuItem.setText("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        editMenu.add(undoMenuItem);

        JButton redoButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/redo.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                performRedo();
            }
        });
        redoButton.setToolTipText("Redo");
        configureToolbarButton(redoButton);
        redoToolbarButton = redoButton;
        JMenuItem redoMenuItem = new JMenuItem(redoButton.getAction());
        redoMenuItem.setText("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        editMenu.add(redoMenuItem);

        JButton newButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/new.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                String inputSize = JOptionPane.showInputDialog("Enter size: ", "5120x3840");
                if (inputSize != null) {
                    if (inputSize.matches("\\d+x\\d+")) {
                        String[] tokens = inputSize.split("x");
                        backgroundWidth = Integer.parseInt(tokens[0]);
                        backgroundHeight = Integer.parseInt(tokens[1]);
                        buildBackgroundImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
                        buildBackgroundNightImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
                        searchMapData = new SearchMapData(backgroundWidth, backgroundHeight);
                        pastedObjects.clear();
                        regions.clear();
                        containers.clear();
                        wallGroups.clear();
                        areaAttributes = new AreaAttributes();
                        searchMapSelectionSession = null;
                        wallGroupPlacementSession = null;
                        cancelDoorEditingSessions(false);
                        clearObjectMoveSelection();
                        polygon.reset();
                        resetHistoryToCurrentState();
                        setExtendedState(Frame.MAXIMIZED_BOTH);
                        repaint();
                    } else {
                        JOptionPane.showMessageDialog(null, "Bad input", ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        newButton.setMaximumSize(BUTTON_SIZE);
        newButton.setToolTipText("Create a new area");
        configureToolbarButton(newButton);
        JMenuItem newMenuItem = new JMenuItem(newButton.getAction());
        newMenuItem.setText("New Area");
        fileMenu.add(newMenuItem);
        JButton newCompositeButton = new JButton(new AbstractAction(null, loadOptionalIcon("/icons/new-composite.png", "/icons/new.png")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                openCompositeObjectEditor();
            }
        });
        newCompositeButton.setMaximumSize(BUTTON_SIZE);
        newCompositeButton.setToolTipText("Create a new composite object");
        configureToolbarButton(newCompositeButton);
        JMenuItem newCompositeMenuItem = new JMenuItem(newCompositeButton.getAction());
        newCompositeMenuItem.setText("New Composite Object...");
        fileMenu.add(newCompositeMenuItem);
        JButton openCompositeButton = new JButton(new AbstractAction(null, loadOptionalIcon("/icons/open-composite.png", "/icons/open.png")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                openExistingCompositeObjectEditor();
            }
        });
        openCompositeButton.setMaximumSize(BUTTON_SIZE);
        openCompositeButton.setToolTipText("Open a composite object");
        configureToolbarButton(openCompositeButton);
        JMenuItem openCompositeMenuItem = new JMenuItem(openCompositeButton.getAction());
        openCompositeMenuItem.setText("Open Composite Object...");
        fileMenu.add(openCompositeMenuItem);

        JButton fillButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/background.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage textureImage = chooseImageFile(FileChooserLocation.TEXTURE);
                if (textureImage != null) {
                    backgroundTile = textureImage;
                    for (int x = 0; x < buildBackgroundImage.getWidth(); x++) {
                        for (int y = 0; y < buildBackgroundImage.getHeight(); y++) {
                            buildBackgroundImage.setRGB(x, y, textureImage.getRGB(x % textureImage.getWidth(), y % textureImage.getHeight()));
                        }
                    }
                    buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
                    applyWholeBackgroundSearchType(textureImage);
                    recordHistoryState();
                }
                repaint();
            }
        });
        fillButton.setMaximumSize(BUTTON_SIZE);
        fillButton.setToolTipText("Fill background with a seamless pattern image");
        configureToolbarButton(fillButton);
        fillToolbarButton = fillButton;
        fillMenuItem = new JMenuItem(fillButton.getAction());
        fillMenuItem.setText("Fill With Pattern...");
        backgroundMenu.add(fillMenuItem);

        JButton openButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("J2DArea project files (*.xml)", "xml");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        byte[] xmlBytes;
                        try (FileInputStream fis = new FileInputStream(chooser.getSelectedFile())) {
                            xmlBytes = fis.readAllBytes();
                        }
                        ExportableArea exportableArea = new ExportableArea();
                        try {
                            exportableArea.fromXml(XmlIO.parseDocument(xmlBytes).getDocumentElement());
                        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException xmlEx) {
                            throw new IOException("Failed to parse area XML", xmlEx);
                        }
                        buildBackgroundImage = exportableArea.getBackgroundImage().getImage();
                        backgroundTile = exportableArea.getBackgroundTile() != null
                            ? exportableArea.getBackgroundTile().getImage() : null;
                        backgroundWidth = buildBackgroundImage.getWidth();
                        backgroundHeight = buildBackgroundImage.getHeight();
                        buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
                        pastedObjects = exportableArea.getPastedObjects();
                        regions = exportableArea.getRegions();
                        containers = exportableArea.getContainers();
                        wallGroups = exportableArea.getWallGroups();
                        areaAttributes = exportableArea.getAreaAttributes();
                        searchMapData = exportableArea.getSearchMapData() != null
                            ? exportableArea.getSearchMapData()
                            : new SearchMapData(backgroundWidth, backgroundHeight);
                        searchMapData.resizeForPixels(backgroundWidth, backgroundHeight);
                        searchMapSelectionSession = null;
                        wallGroupPlacementSession = null;
                        refreshEntranceMarkers();
                        clearObjectMoveSelection();
                        resetHistoryToCurrentState();
                        setExtendedState(Frame.MAXIMIZED_BOTH);
                        repaint();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error opening file.", ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        openButton.setMaximumSize(BUTTON_SIZE);
        openButton.setToolTipText("Open a project file");
        configureToolbarButton(openButton);
        JMenuItem openMenuItem = new JMenuItem(openButton.getAction());
        openMenuItem.setText("Open Project...");
        fileMenu.add(openMenuItem);

        JButton openBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage chosenImageFile = chooseImageFile(FileChooserLocation.OPEN_BG);
                if (chosenImageFile != null) {
                    applyBackgroundImageToSelectedTab(chosenImageFile);
                }
            }
        });
        openBackgroundButton.setMaximumSize(BUTTON_SIZE);
        openBackgroundButton.setToolTipText("Open a background image");
        configureToolbarButton(openBackgroundButton);
        JMenu openBackgroundFolderMenu = new JMenu("Open From");
        openBackgroundFolderMenu.setIcon(new ImageIcon(getClass().getResource("/icons/open-bg.png")));
        JMenuItem openBackgroundMenuItem = new JMenuItem(openBackgroundButton.getAction());
        openBackgroundMenuItem.setText("Image...");
        openBackgroundMenuItem.setIcon(null);
        openBackgroundFolderMenu.add(openBackgroundMenuItem);

        JButton openGameBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                loadBackgroundFromGameArea();
            }
        });
        openGameBackgroundButton.setMaximumSize(BUTTON_SIZE);
        openGameBackgroundButton.setToolTipText("Open background image from a game area");
        configureToolbarButton(openGameBackgroundButton);
        JMenuItem openGameBackgroundMenuItem = new JMenuItem(openGameBackgroundButton.getAction());
        openGameBackgroundMenuItem.setText("Game ARE...");
        openGameBackgroundMenuItem.setIcon(null);
        openBackgroundFolderMenu.add(openGameBackgroundMenuItem);
        backgroundMenu.add(openBackgroundFolderMenu);
        JPopupMenu openBackgroundToolbarPopup = new JPopupMenu();
        JMenuItem openBackgroundToolbarMenuItem = new JMenuItem(openBackgroundButton.getAction());
        openBackgroundToolbarMenuItem.setText("Open Background Image...");
        openBackgroundToolbarMenuItem.setIcon(null);
        openBackgroundToolbarPopup.add(openBackgroundToolbarMenuItem);
        JMenuItem openGameBackgroundToolbarMenuItem = new JMenuItem(openGameBackgroundButton.getAction());
        openGameBackgroundToolbarMenuItem.setText("Open Background From Game ARE...");
        openGameBackgroundToolbarMenuItem.setIcon(null);
        openBackgroundToolbarPopup.add(openGameBackgroundToolbarMenuItem);
        openBackgroundToolbarMenuButton = new JButton(new ImageIcon(getClass().getResource("/icons/open-bg.png")));
        openBackgroundToolbarMenuButton.setToolTipText("Open background");
        configureToolbarButton(openBackgroundToolbarMenuButton);
        openBackgroundToolbarMenuButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openBackgroundToolbarPopup.show(openBackgroundToolbarMenuButton, 0, openBackgroundToolbarMenuButton.getHeight());
            }
        });

        JButton openBrushTextureButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-texture.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                brushTexture = chooseImageFile(FileChooserLocation.TEXTURE);
                buildBrushPreview();
            }
        });
        openBrushTextureButton.setMaximumSize(BUTTON_SIZE);
        openBrushTextureButton.setToolTipText("Choose texture for brush");
        configureToolbarButton(openBrushTextureButton);
        openBrushTextureToolbarButton = openBrushTextureButton;
        openBrushTextureMenuItem = new JMenuItem(openBrushTextureButton.getAction());
        openBrushTextureMenuItem.setText("Choose Brush Texture...");
        backgroundMenu.add(openBrushTextureMenuItem);

        JButton saveButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("J2DArea project files (*.xml)", "xml");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    boolean success;
                    File saveFile = chooser.getSelectedFile();
                    if (!saveFile.getName().toLowerCase().endsWith(".xml")) {
                        saveFile = new File(saveFile.getAbsolutePath() + ".xml");
                    }
                    ExportableArea exportableArea;
                    if (backgroundTile != null) {
                        exportableArea = new ExportableArea(
                            new ExportableImage(backgroundTile),
                            backgroundWidth,
                            backgroundHeight,
                            pastedObjects,
                            regions,
                            containers,
                            wallGroups,
                            areaAttributes,
                            searchMapData
                        );
                    } else {
                        exportableArea = new ExportableArea(
                            new ExportableImage(buildBackgroundImage),
                            pastedObjects,
                            regions,
                            containers,
                            wallGroups,
                            areaAttributes,
                            searchMapData
                        );
                    }
                    try {
                        byte[] xmlBytes;
                        try {
                            xmlBytes = exportableArea.toXmlBytes();
                        } catch (javax.xml.parsers.ParserConfigurationException | javax.xml.transform.TransformerException xmlEx) {
                            throw new IOException("Failed to serialize area to XML", xmlEx);
                        }
                        try (FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
                            fileOutputStream.write(xmlBytes);
                        }
                        success = true;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        success = false;
                    }
                    if (success) {
                        JOptionPane.showMessageDialog(null, "Project saved.");
                    } else {
                        JOptionPane.showMessageDialog(null, "Project save failed.", ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        saveButton.setMaximumSize(BUTTON_SIZE);
        saveButton.setToolTipText("Save build area to a project file");
        configureToolbarButton(saveButton);
        JMenuItem saveMenuItem = new JMenuItem(saveButton.getAction());
        saveMenuItem.setText("Save Project...");
        fileMenu.add(saveMenuItem);

        JButton exportButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-img.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                File file = chooseFile(J2DArea.this, FileDialog.SAVE, FileChooserLocation.SAVE_BG);
                if (file != null) {
                    BufferedImage imageToexport = new BufferedImage(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                    paintObjects(imageToexport.getGraphics());
                    boolean success;
                    try {
                        success = writeImage(file, imageToexport);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        success = false;
                    }
                    if (success) {
                        JOptionPane.showMessageDialog(null, "Image saved.");
                    } else {
                        JOptionPane.showMessageDialog(null, "Image save failed.", ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        exportButton.setMaximumSize(BUTTON_SIZE);
        exportButton.setToolTipText("Export build area to an image");
        configureToolbarButton(exportButton);
        fileMenu.addSeparator();
        JMenuItem exportMenuItem = new JMenuItem(exportButton.getAction());
        exportMenuItem.setText("Export Area Image...");
        fileMenu.add(exportMenuItem);

        JButton exportModButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-package.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                exportAsBaldursGateMod();
            }
        });
        exportModButton.setMaximumSize(BUTTON_SIZE);
        exportModButton.setToolTipText("Export as Baldur's Gate mod (WeiDU package)");
        configureToolbarButton(exportModButton);
        JMenuItem exportModMenuItem = new JMenuItem(exportModButton.getAction());
        exportModMenuItem.setText("Export Mod Package...");
        fileMenu.add(exportModMenuItem);

        JButton prefixButton = new JButton(new AbstractAction("Prefix", new ImageIcon(getClass().getResource("/icons/blackwyrmlair.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editExportPrefix();
            }
        });
        prefixButton.setToolTipText("Select the reserved resource prefix used for exports");
        JMenuItem prefixMenuItem = new JMenuItem(prefixButton.getAction());
        prefixMenuItem.setText("Prefix...");
        settingsMenu.add(prefixMenuItem);

        JButton preferencesButton = new JButton(new AbstractAction("Prefs") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editUserPreferences();
            }
        });
        preferencesButton.setToolTipText("Edit user preferences, including the configured game install path");
        JMenuItem preferencesMenuItem = new JMenuItem(preferencesButton.getAction());
        preferencesMenuItem.setText("Preferences...");
        settingsMenu.add(preferencesMenuItem);

        JButton regionsButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/region.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editRegions();
            }
        });
        regionsButton.setToolTipText("Create and edit polygon regions, including destination-side travel geometry");
        configureToolbarButton(regionsButton);
        regionsToolbarButton = regionsButton;
        JMenuItem regionsMenuItem = new JMenuItem(regionsButton.getAction());
        regionsMenuItem.setText("Regions...");
        insertMenu.add(regionsMenuItem);

        JButton wallGroupsButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/brick-wall.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editWallGroups();
            }
        });
        wallGroupsButton.setToolTipText("Create and edit wallgroup polygons");
        configureToolbarButton(wallGroupsButton);
        wallGroupsToolbarButton = wallGroupsButton;
        JMenuItem wallGroupsMenuItem = new JMenuItem(wallGroupsButton.getAction());
        wallGroupsMenuItem.setText("Wallgroups...");
        insertMenu.add(wallGroupsMenuItem);

        JButton nonWalkableButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/polygon.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                startSearchMapSelection();
            }
        });
        nonWalkableButton.setToolTipText("Mark non-walkable search-map tiles with a polygon");
        configureToolbarButton(nonWalkableButton);
        nonWalkableToolbarButton = nonWalkableButton;
        JMenuItem nonWalkableMenuItem = new JMenuItem(nonWalkableButton.getAction());
        nonWalkableMenuItem.setText("Mark Non-Walkable Area...");
        insertMenu.add(nonWalkableMenuItem);

        JButton tileSeamlessButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-texture.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                    extractPanel.repaint();
                    File file = J2DArea.chooseFile(J2DArea.this, FileDialog.SAVE, FileChooserLocation.TEXTURE);
                    if (file != null) {
                        boolean success;
                        try {
                            success = J2DArea.writeImage(file, TileSeamless.createSeamlessTile(tile.getSubImage(extractionBackgroundImage)));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            success = false;
                        } finally {
                            tile.reset();
                            extractPanel.repaint();
                        }
                        if (success) {
                            JOptionPane.showMessageDialog(null, "Image saved.");
                        } else {
                            JOptionPane.showMessageDialog(null, "Image save failed.", ERROR, JOptionPane.ERROR_MESSAGE);
                        }

                    } else {
                        tile.reset();
                        extractPanel.repaint();
                    }
                }
            }
        });
        tileSeamlessButton.setMaximumSize(BUTTON_SIZE);
        tileSeamlessButton.setToolTipText("Create and export seamless tile from selection to PNG image");
        configureToolbarButton(tileSeamlessButton);
        tileSeamlessToolbarButton = tileSeamlessButton;
        tileSeamlessMenuItem = new JMenuItem(tileSeamlessButton.getAction());
        tileSeamlessMenuItem.setText("Export Seamless Tile...");
        toolsMenu.add(tileSeamlessMenuItem);

        JButton nanoBananaExtractionButton = new JButton(new AbstractAction(null, loadOptionalIcon("/icons/gemini.png", null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                cleanupAndExtractSelectionWithNanoBanana();
            }
        });
        nanoBananaExtractionButton.setMaximumSize(BUTTON_SIZE);
        nanoBananaExtractionButton.setToolTipText("Edit and extract the current selection with Nano Banana 2");
        configureToolbarButton(nanoBananaExtractionButton);
        nanoBananaExtractionToolbarButton = nanoBananaExtractionButton;
        nanoBananaExtractionMenuItem = new JMenuItem(nanoBananaExtractionButton.getAction());
        nanoBananaExtractionMenuItem.setText("Edit and Extract Selection with Nano Banana 2...");
        toolsMenu.add(nanoBananaExtractionMenuItem);
        
        JButton saveDoorsButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-doors.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    List<Rectangle> rectangles = new ArrayList<>(); 
                    int cnt = exportOpenDoorTiles(chooser.getSelectedFile(), rectangles);
                    cnt += exportClosedDoorTiles(chooser.getSelectedFile(), rectangles);
                    if (cnt == 0) {
                        JOptionPane.showMessageDialog(null, "No door tile exported.");
                    } else {
                        JOptionPane.showMessageDialog(null, cnt + " door tiles exported.");
                    }
                }
            }
        });
        saveDoorsButton.setMaximumSize(BUTTON_SIZE);
        saveDoorsButton.setToolTipText("Export all door tiles");
        configureToolbarButton(saveDoorsButton);
        exportDoorTilesToolbarButton = saveDoorsButton;
        saveDoorsMenuItem = new JMenuItem(saveDoorsButton.getAction());
        saveDoorsMenuItem.setText("Export Door Tiles...");
        toolsMenu.add(saveDoorsMenuItem);

        JButton pasteFromButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/paste-from.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage choice = chooseImageFile(FileChooserLocation.OBJECT);
                if (choice != null) {
                    PastedObject pastedObject = new PastedObject(mousePosition, new ExportableImage(choice));
                    pastedObjects.add(pastedObject);
                    objectToMove = pastedObject;
                    objectToMoveIdx = pastedObjects.size() - 1;
                    deltaX = 0;
                    deltaY = 0;
                    painting = false;
                    repaint();
                }
            }
        });
        pasteFromButton.setMaximumSize(BUTTON_SIZE);
        pasteFromButton.setToolTipText("Paste from an image file");
        configureToolbarButton(pasteFromButton);
        pasteFromToolbarButton = pasteFromButton;
        JMenuItem pasteFromMenuItem = new JMenuItem(pasteFromButton.getAction());
        pasteFromMenuItem.setText("Paste Image...");
        insertMenu.add(pasteFromMenuItem);

        JButton pasteCompositeButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/composite.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                pasteCompositeObjectFromFile();
            }
        });
        pasteCompositeButton.setMaximumSize(BUTTON_SIZE);
        pasteCompositeButton.setToolTipText("Paste a composite object file");
        configureToolbarButton(pasteCompositeButton);
        pasteCompositeToolbarButton = pasteCompositeButton;
        JMenuItem pasteCompositeMenuItem = new JMenuItem(pasteCompositeButton.getAction());
        pasteCompositeMenuItem.setText("Paste Composite Object...");
        insertMenu.add(pasteCompositeMenuItem);

        JButton parallelogramBlackButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/parallelogram-black.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editingBlackParallelogram = true;
                painting = false;
                repaint();
            }
        });
        parallelogramBlackButton.setMaximumSize(BUTTON_SIZE);
        parallelogramBlackButton.setToolTipText("Draw and fill a new black parallelogram");
        configureToolbarButton(parallelogramBlackButton);
        parallelogramBlackToolbarButton = parallelogramBlackButton;
        insertMenu.addSeparator();
        JMenuItem parallelogramBlackMenuItem = new JMenuItem(parallelogramBlackButton.getAction());
        parallelogramBlackMenuItem.setText("Black Parallelogram");
        insertMenu.add(parallelogramBlackMenuItem);

        JButton parallelogramTextureButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/parallelogram-texture.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                editingTextureParallelogram = true;
                painting = false;
                repaint();
            }
        });
        parallelogramTextureButton.setMaximumSize(BUTTON_SIZE);
        parallelogramTextureButton.setToolTipText("Draw and fill a new parallelogram with a texture");
        configureToolbarButton(parallelogramTextureButton);
        parallelogramTextureToolbarButton = parallelogramTextureButton;
        JMenuItem parallelogramTextureMenuItem = new JMenuItem(parallelogramTextureButton.getAction());
        parallelogramTextureMenuItem.setText("Textured Parallelogram");
        insertMenu.add(parallelogramTextureMenuItem);

        JButton pasteFromOpenDoorButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/opened_door.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserLocation fileChooserLocation;
                PastedObjectType pastedObjectType;
                if (night) {
                    fileChooserLocation = FileChooserLocation.OPENED_DOOR_NIGHT;
                    pastedObjectType = PastedObjectType.OPENED_DOOR_NIGHT;
                } else {
                    fileChooserLocation = FileChooserLocation.OPENED_DOOR;
                    pastedObjectType = PastedObjectType.OPENED_DOOR;
                }
                BufferedImage choice = chooseImageFile(fileChooserLocation);
                if (choice != null) {
                    PastedObject pastedObject = new PastedObject(mousePosition, new ExportableImage(choice), pastedObjectType);
                    pastedObjects.add(pastedObject);
                    objectToMove = pastedObject;
                    objectToMoveIdx = pastedObjects.size() - 1;
                    deltaX = 0;
                    deltaY = 0;
                    painting = false;
                    setDrawClosedState(false);
                }
            }
        });
        pasteFromOpenDoorButton.setMaximumSize(BUTTON_SIZE);
        pasteFromOpenDoorButton.setToolTipText("Paste from an image file of opened door");
        configureToolbarButton(pasteFromOpenDoorButton);
        pasteFromOpenDoorToolbarButton = pasteFromOpenDoorButton;
        JMenuItem pasteFromOpenDoorMenuItem = new JMenuItem(pasteFromOpenDoorButton.getAction());
        pasteFromOpenDoorMenuItem.setText("Paste Open Door...");
        insertMenu.add(pasteFromOpenDoorMenuItem);
        
        JButton pasteFromClosedDoorButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/closed_door.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage choice = chooseImageFile(FileChooserLocation.CLOSED_DOOR);
                if (choice != null) {
                    PastedObject pastedObject = new PastedObject(mousePosition, new ExportableImage(choice), PastedObjectType.CLOSED_DOOR);
                    pastedObjects.add(pastedObject);
                    objectToMove = pastedObject;
                    objectToMoveIdx = pastedObjects.size() - 1;
                    deltaX = 0;
                    deltaY = 0;
                    painting = false;
                    setDrawClosedState(true);
                }
            }
        });
        pasteFromClosedDoorButton.setMaximumSize(BUTTON_SIZE);
        pasteFromClosedDoorButton.setToolTipText("Paste from an image file of closed door");
        configureToolbarButton(pasteFromClosedDoorButton);
        pasteFromClosedDoorToolbarButton = pasteFromClosedDoorButton;
        JMenuItem pasteFromClosedDoorMenuItem = new JMenuItem(pasteFromClosedDoorButton.getAction());
        pasteFromClosedDoorMenuItem.setText("Paste Closed Door...");
        insertMenu.add(pasteFromClosedDoorMenuItem);
        
        JButton pasteFromNightLightButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/night_light.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage choice = chooseImageFile(FileChooserLocation.NIGHT_LIGHT);
                if (choice != null) {
                    PastedObject pastedObject = new PastedObject(mousePosition, new ExportableImage(choice), PastedObjectType.NIGHT_LIGHT);
                    pastedObjects.add(pastedObject);
                    objectToMove = pastedObject;
                    objectToMoveIdx = pastedObjects.size() - 1;
                    deltaX = 0;
                    deltaY = 0;
                    painting = false;
                    night = true;
                    repaint();
                }
            }
        });
        pasteFromNightLightButton.setMaximumSize(BUTTON_SIZE);
        pasteFromNightLightButton.setToolTipText("Paste from an image file of night time light");
        configureToolbarButton(pasteFromNightLightButton);
        pasteFromNightLightToolbarButton = pasteFromNightLightButton;
        JMenuItem pasteFromNightLightMenuItem = new JMenuItem(pasteFromNightLightButton.getAction());
        pasteFromNightLightMenuItem.setText("Paste Night Light...");
        insertMenu.add(pasteFromNightLightMenuItem);

        JButton entranceButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/entrance.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                String entranceName = JOptionPane.showInputDialog("Enter entrance name:", "Entrance" + (countEntrances() + 1));
                if (entranceName != null && !entranceName.trim().isEmpty()) {
                    beginTransitionPlacement(entranceName.trim());
                    painting = false;
                    repaint();
                }
            }
        });
        entranceButton.setMaximumSize(BUTTON_SIZE);
        entranceButton.setToolTipText("Place an entrance/exit point for area transitions");
        configureToolbarButton(entranceButton);
        entranceToolbarButton = entranceButton;
        insertMenu.addSeparator();
        JMenuItem entranceMenuItem = new JMenuItem(entranceButton.getAction());
        entranceMenuItem.setText("New Transition Entry/Exit...");
        insertMenu.add(entranceMenuItem);

        JButton drawClosedDoorButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/draw_closed.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                setDrawClosedState(!drawClosed);
            }
        });
        drawClosedDoorButton.setMaximumSize(BUTTON_SIZE);
        drawClosedDoorButton.setToolTipText("Toggle draw closed doors");
        drawClosedDoorMenuItem = new JCheckBoxMenuItem("Closed Doors", drawClosed);
        drawClosedDoorMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/draw_closed.png")));
        drawClosedDoorMenuItem.setToolTipText("Toggle draw closed doors");
        drawClosedDoorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                setDrawClosedState(drawClosedDoorMenuItem.isSelected());
            }
        });
        viewMenu.add(drawClosedDoorMenuItem);
        
        JButton nightButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/night.png"))) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                night = !night;
                painting = false;
                repaint();
            }
        });
        nightButton.setMaximumSize(BUTTON_SIZE);
        nightButton.setToolTipText("Toggle day/night");
        nightMenuItem = new JCheckBoxMenuItem("Day/Night", night);
        nightMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/night.png")));
        nightMenuItem.setToolTipText("Toggle day/night");
        nightMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                setNightModeState(nightMenuItem.isSelected());
            }
        });
        viewMenu.add(nightMenuItem);
        ImageIcon searchMapGridIcon = createSearchMapGridIcon();
        searchMapGridMenuItem = new JCheckBoxMenuItem("Show Search Grid", showSearchMapGrid);
        searchMapGridMenuItem.setIcon(searchMapGridIcon);
        searchMapGridMenuItem.setToolTipText("Show search-map tile overlay and grid");
        searchMapGridMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowSearchMapGridState(searchMapGridMenuItem.isSelected());
            }
        });
        viewMenu.add(searchMapGridMenuItem);
        drawClosedDoorToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/draw_closed.png")));
        drawClosedDoorToggleButton.setSelected(drawClosed);
        drawClosedDoorToggleButton.setToolTipText("Toggle draw closed doors");
        drawClosedDoorToggleButton.setMaximumSize(BUTTON_SIZE);
        drawClosedDoorToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                setDrawClosedState(drawClosedDoorToggleButton.isSelected());
            }
        });
        configureToolbarButton(drawClosedDoorToggleButton);
        nightToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/night.png")));
        nightToggleButton.setSelected(night);
        nightToggleButton.setToolTipText("Toggle day/night");
        nightToggleButton.setMaximumSize(BUTTON_SIZE);
        nightToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                setNightModeState(nightToggleButton.isSelected());
            }
        });
        configureToolbarButton(nightToggleButton);
        searchMapGridToggleButton = new JToggleButton(searchMapGridIcon);
        searchMapGridToggleButton.setSelected(showSearchMapGrid);
        searchMapGridToggleButton.setToolTipText("Show search-map tile overlay and grid");
        searchMapGridToggleButton.setMaximumSize(BUTTON_SIZE);
        searchMapGridToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowSearchMapGridState(searchMapGridToggleButton.isSelected());
            }
        });
        configureToolbarButton(searchMapGridToggleButton);
        JToggleButton polygonButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/polygon.png")));
        JToggleButton extractionMapDragButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        extractionMapDragButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.MAP_DRAG);
                syncCursorModeUi();
                if (tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
            }
        });
        extractionMapDragButton.setMaximumSize(BUTTON_SIZE);
        extractionMapDragButton.setToolTipText("Move map");
        configureToolbarButton(extractionMapDragButton);
        extractionMapDragToolbarButton = extractionMapDragButton;
        polygonButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.POLYGON);
                syncCursorModeUi();
                if (tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
            }
        });
        polygonButton.setMaximumSize(BUTTON_SIZE);
        polygonButton.setToolTipText("Polygon selection");
        configureToolbarButton(polygonButton);
        polygonToolbarButton = polygonButton;
        ButtonGroup buildCursorModeGroup = new ButtonGroup();
        cursorSelectMenuItem = new JRadioButtonMenuItem("Select Objects", !painting);
        cursorSelectMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        cursorSelectMenuItem.setToolTipText("Select objects");
        cursorSelectMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBuildCursorSelectMode();
            }
        });
        buildCursorModeGroup.add(cursorSelectMenuItem);
        cursorModeMenu.add(cursorSelectMenuItem);

        JToggleButton brushButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/pencil.png")));
        brushButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableTextureBrushMode();
            }
        });
        brushButton.setMaximumSize(BUTTON_SIZE);
        brushButton.setToolTipText("Use texture brush");
        configureToolbarButton(brushButton);
        brushToolbarButton = brushButton;
        brushModeMenuItem = new JRadioButtonMenuItem("Texture Brush", painting);
        brushModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/pencil.png")));
        brushModeMenuItem.setToolTipText("Use texture brush");
        brushModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableTextureBrushMode();
            }
        });
        buildCursorModeGroup.add(brushModeMenuItem);
        cursorModeMenu.add(brushModeMenuItem);

        ImageIcon searchMapPainterIcon = createSearchMapPaintToolIcon(selectedSearchMapPaintType);
        JToggleButton searchMapPainterButton = new JToggleButton(searchMapPainterIcon);
        searchMapPainterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableSearchMapPainterMode();
            }
        });
        searchMapPainterButton.setMaximumSize(BUTTON_SIZE);
        searchMapPainterButton.setToolTipText("Paint search-map cells");
        configureToolbarButton(searchMapPainterButton);
        searchMapPainterToolbarButton = searchMapPainterButton;
        searchMapPainterModeMenuItem = new JRadioButtonMenuItem("Search Map Painter", searchMapEditMode == SearchMapEditMode.PAINTER);
        searchMapPainterModeMenuItem.setIcon(searchMapPainterIcon);
        searchMapPainterModeMenuItem.setToolTipText("Paint search-map cells");
        searchMapPainterModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableSearchMapPainterMode();
            }
        });
        buildCursorModeGroup.add(searchMapPainterModeMenuItem);
        cursorModeMenu.add(searchMapPainterModeMenuItem);

        JToggleButton searchMapEraserButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/eraser.png")));
        searchMapEraserButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableSearchMapEraserMode();
            }
        });
        searchMapEraserButton.setMaximumSize(BUTTON_SIZE);
        searchMapEraserButton.setToolTipText("Reset search-map cells to their base terrain");
        configureToolbarButton(searchMapEraserButton);
        searchMapEraserToolbarButton = searchMapEraserButton;
        searchMapEraserModeMenuItem = new JRadioButtonMenuItem("Search Map Eraser", searchMapEditMode == SearchMapEditMode.ERASER);
        searchMapEraserModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/eraser.png")));
        searchMapEraserModeMenuItem.setToolTipText("Reset search-map cells to their base terrain");
        searchMapEraserModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableSearchMapEraserMode();
            }
        });
        buildCursorModeGroup.add(searchMapEraserModeMenuItem);
        cursorModeMenu.add(searchMapEraserModeMenuItem);

        JToggleButton cursorButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        cursorButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setBuildCursorSelectMode();
            }
        });
        cursorButton.setMaximumSize(BUTTON_SIZE);
        cursorButton.setToolTipText("Select objects");
        configureToolbarButton(cursorButton);
        cursorToolbarButton = cursorButton;
        ButtonGroup extractionCursorModeGroup = new ButtonGroup();
        extractionMapDragModeMenuItem = new JRadioButtonMenuItem("Map Drag", extractionCursorMode == ExtractionCursorMode.MAP_DRAG);
        extractionMapDragModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        extractionMapDragModeMenuItem.setToolTipText("Move map");
        extractionMapDragModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.MAP_DRAG);
                syncCursorModeUi();
            }
        });
        extractionCursorModeGroup.add(extractionMapDragModeMenuItem);
        cursorModeMenu.add(extractionMapDragModeMenuItem);
        polygonModeMenuItem = new JRadioButtonMenuItem("Polygon Selection", extractionCursorMode == ExtractionCursorMode.POLYGON);
        polygonModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/polygon.png")));
        polygonModeMenuItem.setToolTipText("Polygon selection");
        polygonModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.POLYGON);
                if (isExtractionPolygonMode() && tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
                syncCursorModeUi();
            }
        });
        extractionCursorModeGroup.add(polygonModeMenuItem);
        cursorModeMenu.add(polygonModeMenuItem);
        rectangleModeMenuItem = new JRadioButtonMenuItem("Rectangle Selection", extractionCursorMode == ExtractionCursorMode.RECTANGLE);
        rectangleModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/rectangle.png")));
        rectangleModeMenuItem.setToolTipText("Rectangle selection");
        rectangleModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.RECTANGLE);
                syncCursorModeUi();
            }
        });
        extractionCursorModeGroup.add(rectangleModeMenuItem);
        cursorModeMenu.add(rectangleModeMenuItem);
        JToggleButton rectangleButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/rectangle.png")));
        rectangleButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionCursorMode(ExtractionCursorMode.RECTANGLE);
                syncCursorModeUi();
                if (tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
            }
        });
        rectangleButton.setMaximumSize(BUTTON_SIZE);
        rectangleButton.setToolTipText("Rectangle selection");
        configureToolbarButton(rectangleButton);
        rectangleToolbarButton = rectangleButton;
        extractionClosedDoorMenuItem = new JCheckBoxMenuItem("Closed Door", extractionGameAreaClosedDoors);
        extractionClosedDoorMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/draw_closed.png")));
        extractionClosedDoorMenuItem.setToolTipText("Reload the current game ARE background with closed or opened doors");
        extractionClosedDoorMenuItem.setEnabled(false);
        extractionClosedDoorMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadExtractionGameAreaWithClosedDoors(extractionClosedDoorMenuItem.isSelected());
            }
        });
        viewMenu.add(extractionClosedDoorMenuItem);
        extractionClosedDoorToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/draw_closed.png")));
        extractionClosedDoorToggleButton.setSelected(extractionGameAreaClosedDoors);
        extractionClosedDoorToggleButton.setToolTipText("Reload the current game ARE background with closed or opened doors");
        extractionClosedDoorToggleButton.setMaximumSize(BUTTON_SIZE);
        extractionClosedDoorToggleButton.setEnabled(false);
        extractionClosedDoorToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadExtractionGameAreaWithClosedDoors(extractionClosedDoorToggleButton.isSelected());
            }
        });
        configureToolbarButton(extractionClosedDoorToggleButton);

        JButton paint3dButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/paint3d.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                    try {
                        File tempFile = File.createTempFile("j2darea", ".png");
                        J2DArea.writeImage(tempFile, tile.getSubImage(extractionBackgroundImage));
                        ProcessBuilder processBuilder = new ProcessBuilder("mspaint", "\"" + tempFile.getAbsolutePath() + "\"", "/ForceBootstrapPaint3D");
                        processBuilder.start();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        paint3dButton.setMaximumSize(BUTTON_SIZE);
        paint3dButton.setToolTipText("Edit selection in Paint 3D");
        configureToolbarButton(paint3dButton);
        paint3dToolbarButton = paint3dButton;
        paint3dMenuItem = new JMenuItem(paint3dButton.getAction());
        paint3dMenuItem.setText("Edit Selection in Paint 3D");
        toolsMenu.add(paint3dMenuItem);

        JButton subtractBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/remove-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                	BackgroundSubtractionEditorFrame bgSubtracterPreview = new BackgroundSubtractionEditorFrame(tile.getSubImage(extractionBackgroundImage));
                    bgSubtracterPreview.setLocation(tile.getXOnScreen(), tile.getYOnScreen());
                    bgSubtracterPreview.setVisible(true);
                }
            }
        });
        subtractBackgroundButton.setMaximumSize(BUTTON_SIZE);
        subtractBackgroundButton.setToolTipText("Subtract background from selection");
        configureToolbarButton(subtractBackgroundButton);
        subtractBackgroundToolbarButton = subtractBackgroundButton;
        subtractBackgroundMenuItem = new JMenuItem(subtractBackgroundButton.getAction());
        subtractBackgroundMenuItem.setText("Subtract Background");
        toolsMenu.add(subtractBackgroundMenuItem);

        menubar.add(undoToolbarButton);
        menubar.add(redoToolbarButton);
        menubar.add(newButton);
        menubar.add(newCompositeButton);
        menubar.add(openButton);
        menubar.add(openCompositeButton);
        menubar.add(openBackgroundToolbarMenuButton);
        menubar.add(openBrushTextureToolbarButton);
        menubar.add(saveButton);
        menubar.add(exportButton);
        menubar.add(exportModButton);
        menubar.add(exportDoorTilesToolbarButton);
        menubar.add(fillToolbarButton);
        menubar.add(regionsToolbarButton);
        menubar.add(wallGroupsToolbarButton);
        menubar.add(nonWalkableToolbarButton);
        menubar.add(pasteFromToolbarButton);
        menubar.add(pasteCompositeToolbarButton);
        menubar.add(parallelogramBlackToolbarButton);
        menubar.add(parallelogramTextureToolbarButton);
        menubar.add(pasteFromOpenDoorToolbarButton);
        menubar.add(pasteFromClosedDoorToolbarButton);
        menubar.add(pasteFromNightLightToolbarButton);
        menubar.add(entranceToolbarButton);
        menubar.add(cursorToolbarButton);
        menubar.add(brushToolbarButton);
        menubar.add(searchMapPainterToolbarButton);
        menubar.add(searchMapEraserToolbarButton);
        menubar.add(extractionMapDragToolbarButton);
        menubar.add(polygonToolbarButton);
        menubar.add(rectangleToolbarButton);
        menubar.add(extractionClosedDoorToggleButton);
        menubar.add(tileSeamlessToolbarButton);
        menubar.add(nanoBananaExtractionToolbarButton);
        menubar.add(paint3dToolbarButton);
        menubar.add(subtractBackgroundToolbarButton);
        menubar.add(searchMapGridToggleButton);
        menubar.add(drawClosedDoorToggleButton);
        menubar.add(nightToggleButton);
        buildOnlyToolbarButtons.clear();
        buildOnlyToolbarButtons.add(undoToolbarButton);
        buildOnlyToolbarButtons.add(redoToolbarButton);
        buildOnlyToolbarButtons.add(newButton);
        buildOnlyToolbarButtons.add(newCompositeButton);
        buildOnlyToolbarButtons.add(openButton);
        buildOnlyToolbarButtons.add(openCompositeButton);
        buildOnlyToolbarButtons.add(saveButton);
        buildOnlyToolbarButtons.add(exportButton);
        buildOnlyToolbarButtons.add(exportModButton);
        buildOnlyToolbarButtons.add(fillToolbarButton);
        buildOnlyToolbarButtons.add(openBrushTextureToolbarButton);
        buildOnlyToolbarButtons.add(regionsToolbarButton);
        buildOnlyToolbarButtons.add(wallGroupsToolbarButton);
        buildOnlyToolbarButtons.add(nonWalkableToolbarButton);
        buildOnlyToolbarButtons.add(pasteFromToolbarButton);
        buildOnlyToolbarButtons.add(pasteCompositeToolbarButton);
        buildOnlyToolbarButtons.add(parallelogramBlackToolbarButton);
        buildOnlyToolbarButtons.add(parallelogramTextureToolbarButton);
        buildOnlyToolbarButtons.add(pasteFromOpenDoorToolbarButton);
        buildOnlyToolbarButtons.add(pasteFromClosedDoorToolbarButton);
        buildOnlyToolbarButtons.add(pasteFromNightLightToolbarButton);
        buildOnlyToolbarButtons.add(entranceToolbarButton);
        buildOnlyToolbarButtons.add(cursorToolbarButton);
        buildOnlyToolbarButtons.add(brushToolbarButton);
        buildOnlyToolbarButtons.add(searchMapPainterToolbarButton);
        buildOnlyToolbarButtons.add(searchMapEraserToolbarButton);
        buildOnlyToolbarButtons.add(searchMapGridToggleButton);
        buildOnlyToolbarButtons.add(drawClosedDoorToggleButton);
        buildOnlyToolbarButtons.add(nightToggleButton);

        JMenuItem commandsMenuItem = new JMenuItem("Commands...");
        commandsMenuItem.setToolTipText("Show mouse, keyboard, and workflow help");
        commandsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCommandsHelp();
            }
        });
        helpMenu.add(commandsMenuItem);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        getRootPane().getActionMap().put("Undo", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                performUndo();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        getRootPane().getActionMap().put("Redo", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                performRedo();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "RemovePolygonVertex");
        getRootPane().getActionMap().put("RemovePolygonVertex", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                removeLastExtractionPolygonVertex();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CancelExtractionPolygon");
        getRootPane().getActionMap().put("CancelExtractionPolygon", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (doorPointPlacementSession != null) {
                    cancelDoorEditingSessions(true);
                } else {
                    cancelExtractionPolygonSelection();
                }
            }
        });

        tabPane.addChangeListener(e -> updateTabSpecificUi());
        resetHistoryToCurrentState();
        syncCursorModeUi();
        updateTabSpecificUi();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabPane, BorderLayout.CENTER);
        setSize(MIN_SIZE);
        setMinimumSize(MIN_SIZE);
        setVisible(true);
    }

    private int exportClosedDoorTiles(File destDir, List<Rectangle> rectangles) {
        drawClosed = true;
        BufferedImage imageToexport = new BufferedImage(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        paintObjects(imageToexport.getGraphics());
        int cnt = 0;
        for (Rectangle r : rectangles) {
            int x = (int) (64 * Math.floor(r.getX() / 64.));
            int y = (int) (64 * Math.floor(r.getY() / 64.));
            int w = (int) (64 * Math.ceil((r.getX() + r.getWidth()) / 64.)) - x;
            int h = (int) (64 * Math.ceil((r.getY() + r.getHeight()) / 64.)) - y;
            try {
                String fileName = String.format("door_at_%dx_%dy_closed.bmp", x, y);
                if (ImageIO.write(imageToexport.getSubimage(x, y, w, h), "bmp", new File(destDir, fileName))) {
                    cnt++;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error exporting closed door tiles.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
        return cnt;
    }

    private int exportOpenDoorTiles(File destDir, List<Rectangle> rectangles) {
        drawClosed = false;
        BufferedImage imageToexport = new BufferedImage(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        paintObjects(imageToexport.getGraphics());
        int cnt = 0;
        PastedObjectType pastedObjectType = night ? PastedObjectType.OPENED_DOOR_NIGHT : PastedObjectType.OPENED_DOOR;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.getPastedObjectType() == pastedObjectType) {
                int x = (int) (64 * Math.floor(pastedObject.getX() / 64.));
                int y = (int) (64 * Math.floor(pastedObject.getY() / 64.));
                int w = (int) (64 * Math.ceil((pastedObject.getX() + pastedObject.getWidth()) / 64.)) - x;
                int h = (int) (64 * Math.ceil((pastedObject.getY() + pastedObject.getHeight()) / 64.)) - y;
                rectangles.add(new Rectangle(x, y, w, h));
                try {
                    String fileName = String.format("door_at_%dx_%dy_open.bmp", x, y);
                    if (ImageIO.write(imageToexport.getSubimage(x, y, w, h), "bmp", new File(destDir, fileName))) {
                        cnt++;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error exporting open door tiles.", ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return cnt;
    }

    public static boolean writeImage(File file, BufferedImage imageToexport) throws IOException {
        String[] formats = ImageIO.getWriterFormatNames();
        for (String format : formats) {
            if (file.getName().endsWith('.' + format)) {
                return ImageIO.write(imageToexport, format, file);
            }
        }
        JOptionPane.showMessageDialog(null, "Extension must be one of " + Arrays.toString(formats), ERROR, JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private BufferedImage chooseImageFile(FileChooserLocation fileChooserLocation) {
        File file = chooseFile(this, FileDialog.LOAD, fileChooserLocation);
        if (file != null) {
            try {
                return ImageIO.read(file);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error opening image.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private boolean ensureBrushTextureSelected() {
        if (brushTexture != null) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
            this,
            "No texture brush is selected. Choose one from an image file now ?",
            "Texture Brush",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return false;
        }
        BufferedImage selectedTexture = chooseImageFile(FileChooserLocation.TEXTURE);
        if (selectedTexture == null) {
            return false;
        }
        brushTexture = selectedTexture;
        buildBrushPreview();
        return true;
    }

    private CompositeObjectData chooseCompositeObjectFile() {
        File file = chooseFile(this, FileDialog.LOAD, FileChooserLocation.COMPOSITE_OBJECT);
        if (file == null) {
            return null;
        }
        try {
            byte[] xmlBytes;
            try (FileInputStream fis = new FileInputStream(file)) {
                xmlBytes = fis.readAllBytes();
            }
            CompositeObjectData compositeObjectData = new CompositeObjectData();
            try {
                compositeObjectData.fromXml(XmlIO.parseDocument(xmlBytes).getDocumentElement());
            } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException xmlEx) {
                throw new IOException("Failed to parse composite object XML", xmlEx);
            }
            return compositeObjectData;
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening composite object file.", ERROR, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void openCompositeObjectEditor() {
        BufferedImage initialImage = chooseImageFile(FileChooserLocation.OBJECT);
        if (initialImage != null) {
            CompositeObjectEditorDialog dialog = new CompositeObjectEditorDialog(this, initialImage);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    private void openExistingCompositeObjectEditor() {
        CompositeObjectData compositeObjectData = chooseCompositeObjectFile();
        if (compositeObjectData != null) {
            CompositeObjectEditorDialog dialog = new CompositeObjectEditorDialog(this, compositeObjectData);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
    }

    private void pasteCompositeObjectFromFile() {
        CompositeObjectData compositeObjectData = chooseCompositeObjectFile();
        if (compositeObjectData == null) {
            return;
        }
        String compositeGroupId = newCompositeGroupId();
        List<PastedObject> instances = compositeObjectData.instantiate(new Point(mousePosition), compositeGroupId);
        List<WallGroupData> wallGroupInstances = compositeObjectData.instantiateWallGroups(new Point(mousePosition), compositeGroupId);
        if (instances.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Composite object is empty.", ERROR, JOptionPane.WARNING_MESSAGE);
            return;
        }
        pastedObjects.addAll(instances);
        wallGroups.addAll(wallGroupInstances);
        objectToMove = instances.get(instances.size() - 1);
        objectToMoveIdx = pastedObjects.size() - 1;
        beginCompositeMove(objectToMove);
        Rectangle anchorRect = getPastedObjectBounds(objectToMove);
        deltaX = mousePosition.x - anchorRect.x;
        deltaY = mousePosition.y - anchorRect.y;
        movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
        refreshEntranceMarkers();
        repaint();
    }

    public static File chooseFile(Frame parent, int mode, FileChooserLocation fileChooserLocation) {
        FileDialog chooser = new FileDialog(parent, "Choose a file", mode);
        chooser.setDirectory(directories.get(fileChooserLocation));
        chooser.setVisible(true);
        if (chooser.getFile() == null) {
            return null;
        }
        directories.put(fileChooserLocation, chooser.getDirectory());
        return new File(chooser.getDirectory(), chooser.getFile());
    }

    private void paintObjects(Graphics g) {
        paintObjects(g, drawClosed, night, painting, true);
    }

    private void paintObjects(Graphics g, boolean closedDoors, boolean nightMode, boolean includeBrush) {
        paintObjects(g, closedDoors, nightMode, includeBrush, false);
    }

    private void paintObjects(Graphics g, boolean closedDoors, boolean nightMode, boolean includeBrush, boolean includeEditorOverlays) {
        if (buildBackgroundImage != null) {
            if (nightMode) {
                g.drawImage(buildBackgroundNightImage, 0, 0, null);
            } else {
                g.drawImage(buildBackgroundImage, 0, 0, null);
            }
        }
        if (includeBrush && brushPreview != null) {
            if (nightMode) {
                g.drawImage(brushNightPreview, mousePosition.x - brushRadius, mousePosition.y - brushRadius, null);
            } else {
                g.drawImage(brushPreview, mousePosition.x - brushRadius, mousePosition.y - brushRadius, null);
            }
        }
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.isVisible(closedDoors, nightMode)) {
                pastedObject.drawImage(g, nightMode);
                if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE &&
                    pastedObject.getEntranceData() != null) {
                    g.setColor(Color.WHITE);
                    g.drawString(pastedObject.getEntranceData().getName(),
                        pastedObject.getX() + 2,
                        pastedObject.getY() - 2);
                }
            }
        }
        if (includeEditorOverlays) {
            drawRegionOverlays(g);
            drawWallGroupOverlays(g);
            drawDoorMetadataOverlays(g);
        }
    }

    private void drawRegionOverlays(Graphics g) {
        for (RegionData regionData : regions) {
            Polygon bounds = regionData.getBounds();
            if (bounds == null || bounds.npoints < 3) {
                continue;
            }
            Color fillColor = regionData.getType() == 2
                ? new Color(255, 165, 0, 50)
                : new Color(0, 200, 255, 40);
            Color outlineColor = regionData.getType() == 2
                ? new Color(255, 200, 0)
                : new Color(0, 220, 255);
            g.setColor(fillColor);
            g.fillPolygon(bounds);
            g.setColor(outlineColor);
            g.drawPolygon(bounds);
            if (regionData.getName() != null && !regionData.getName().trim().isEmpty()) {
                Rectangle boundsRect = bounds.getBounds();
                g.drawString(regionData.getName(), boundsRect.x + 2, boundsRect.y - 2);
            }
        }
    }

    private void drawWallGroupOverlays(Graphics g) {
        for (WallGroupData wallGroup : wallGroups) {
            Polygon polygonBounds = wallGroup.getPolygon();
            if (polygonBounds == null || polygonBounds.npoints < 3) {
                continue;
            }
            g.setColor(new Color(0, 180, 255, 40));
            g.fillPolygon(polygonBounds);
            drawWallGroupSegments(g, polygonBounds);
            Rectangle bounds = polygonBounds.getBounds();
            g.drawString(wallGroup.getDisplayName(), bounds.x + 2, bounds.y - 2);
        }
    }

    private void paintWallGroupPlacementDraft(Graphics2D graphics) {
        if (wallGroupPlacementSession == null) {
            return;
        }
        Polygon draft = wallGroupPlacementSession.polygon;
        if (draft == null) {
            return;
        }
        Polygon preview = clonePolygon(draft);
        preview.addPoint(mousePosition.x, mousePosition.y);
        boolean closeCandidate = draft.npoints > 0
            && Point2D.distance(mousePosition.x, mousePosition.y, draft.xpoints[0], draft.ypoints[0]) <= 6;
        if (draft.npoints >= 2) {
            graphics.setColor(new Color(0, 180, 255, 40));
            graphics.fillPolygon(preview);
        }
        if (draft.npoints > 0) {
            if (closeCandidate) {
                drawWallGroupSegments(graphics, preview);
                graphics.setColor(Color.YELLOW);
                graphics.drawPolygon(preview);
            } else {
                drawWallGroupOpenSegments(graphics, preview);
            }
        }
    }

    private void paintSearchMapOverlay(Graphics g) {
        if (!showSearchMapGrid || searchMapData == null) {
            return;
        }
        ensureSearchMapSized();
        for (int tileY = 0; tileY < searchMapData.getHeightInTiles(); tileY++) {
            for (int tileX = 0; tileX < searchMapData.getWidthInTiles(); tileX++) {
                int x = tileX * SearchMapData.CELL_WIDTH;
                int y = tileY * SearchMapData.CELL_HEIGHT;
                int width = Math.min(SearchMapData.CELL_WIDTH, backgroundWidth - x);
                int height = Math.min(SearchMapData.CELL_HEIGHT, backgroundHeight - y);
                SearchMapTileType tileType = searchMapData.getResolvedTileType(tileX, tileY);
                if (tileType != SearchMapTileType.UNKNOWN) {
                    g.setColor(tileType.getOverlayColor());
                    g.fillRect(x, y, width, height);
                }
                g.setColor(new Color(255, 255, 255, 65));
                g.drawRect(x, y, width, height);
                if (isSelectedSearchMapCell(tileX, tileY)) {
                    paintSelectedSearchMapCellEdges(g, tileX, tileY, x, y, width, height);
                }
            }
        }
    }

    private void paintSelectedSearchMapCellEdges(Graphics g, int tileX, int tileY, int x, int y, int width, int height) {
        g.setColor(new Color(255, 255, 160));
        if (!isSelectedSearchMapCell(tileX, tileY - 1)) {
            g.drawLine(x, y, x + width, y);
        }
        if (!isSelectedSearchMapCell(tileX, tileY + 1)) {
            g.drawLine(x, y + height, x + width, y + height);
        }
        if (!isSelectedSearchMapCell(tileX - 1, tileY)) {
            g.drawLine(x, y, x, y + height);
        }
        if (!isSelectedSearchMapCell(tileX + 1, tileY)) {
            g.drawLine(x + width, y, x + width, y + height);
        }
    }

    private void paintSearchMapSelectionDraft(Graphics2D graphics) {
        if (searchMapSelectionSession == null || searchMapSelectionSession.polygon == null) {
            return;
        }
        Polygon draft = searchMapSelectionSession.polygon;
        if (draft.npoints == 0) {
            return;
        }
        Polygon preview = clonePolygon(draft);
        preview.addPoint(mousePosition.x, mousePosition.y);
        boolean closeCandidate = Point2D.distance(mousePosition.x, mousePosition.y, draft.xpoints[0], draft.ypoints[0]) <= 6;
        Point parallelogramClosePoint = getSearchMapParallelogramClosePoint(draft);
        boolean parallelogramCloseCandidate = isWithinSearchMapParallelogramCloseZone(draft, mousePosition.x, mousePosition.y);
        if (draft.npoints >= 2) {
            graphics.setColor(new Color(220, 40, 40, 40));
            graphics.fillPolygon(preview);
        }
        if (parallelogramClosePoint != null) {
            graphics.setColor(parallelogramCloseCandidate ? Color.YELLOW : new Color(255, 255, 255, 120));
            graphics.drawOval(
                parallelogramClosePoint.x - SearchMapSelectionSession.PARALLELOGRAM_CLOSE_RADIUS,
                parallelogramClosePoint.y - SearchMapSelectionSession.PARALLELOGRAM_CLOSE_RADIUS,
                SearchMapSelectionSession.PARALLELOGRAM_CLOSE_RADIUS * 2,
                SearchMapSelectionSession.PARALLELOGRAM_CLOSE_RADIUS * 2
            );
        }
        if (parallelogramCloseCandidate && parallelogramClosePoint != null) {
            Polygon parallelogramPreview = clonePolygon(draft);
            parallelogramPreview.addPoint(parallelogramClosePoint.x, parallelogramClosePoint.y);
            graphics.setColor(new Color(220, 40, 40, 40));
            graphics.fillPolygon(parallelogramPreview);
            graphics.setColor(Color.YELLOW);
            graphics.drawPolygon(parallelogramPreview);
        } else if (closeCandidate && draft.npoints >= 2) {
            graphics.setColor(Color.YELLOW);
            graphics.drawPolygon(preview);
        } else {
            graphics.setColor(new Color(220, 40, 40));
            graphics.drawPolyline(preview.xpoints, preview.ypoints, preview.npoints);
        }
    }

    private void drawWallGroupSegments(Graphics g, Polygon polygonBounds) {
        if (polygonBounds == null) {
            return;
        }
        for (int i = 0; i < polygonBounds.npoints; i++) {
            int next = (i + 1) % polygonBounds.npoints;
            g.setColor(i == 0 ? new Color(0, 0, 139) : Color.GREEN);
            g.drawLine(
                polygonBounds.xpoints[i],
                polygonBounds.ypoints[i],
                polygonBounds.xpoints[next],
                polygonBounds.ypoints[next]
            );
        }
    }

    private void drawWallGroupOpenSegments(Graphics g, Polygon polygonBounds) {
        if (polygonBounds == null || polygonBounds.npoints < 2) {
            return;
        }
        for (int i = 0; i < polygonBounds.npoints - 1; i++) {
            g.setColor(i == 0 ? new Color(0, 0, 139) : Color.GREEN);
            g.drawLine(
                polygonBounds.xpoints[i],
                polygonBounds.ypoints[i],
                polygonBounds.xpoints[i + 1],
                polygonBounds.ypoints[i + 1]
            );
        }
    }

    private void paintExtractionPolygonDraft(Graphics2D graphics) {
        if (polygon.npoints == 0) {
            return;
        }

        Polygon preview = clonePolygon(polygon);
        preview.addPoint(mousePosition.x, mousePosition.y);
        boolean closeCandidate = Point2D.distance(mousePosition.x, mousePosition.y, polygon.xpoints[0], polygon.ypoints[0]) <= 3;
        graphics.setColor(closeCandidate ? Color.YELLOW : Color.GREEN);
        if (closeCandidate) {
            graphics.drawPolygon(preview);
        } else {
            graphics.drawPolyline(preview.xpoints, preview.ypoints, preview.npoints);
        }
    }

    private void removeLastExtractionPolygonVertex() {
        if (!isExtractionPolygonMode() || polygon.npoints == 0) {
            return;
        }

        Polygon updatedPolygon = new Polygon();
        for (int i = 0; i < polygon.npoints - 1; i++) {
            updatedPolygon.addPoint(polygon.xpoints[i], polygon.ypoints[i]);
        }
        polygon = updatedPolygon;
        if (extractPanel != null) {
            extractPanel.repaint();
        }
    }

    private void drawDoorMetadataOverlays(Graphics g) {
        if (!showDoorPolygonOverlays && !showDoorImpededBlockOverlays) {
            return;
        }
        Set<PastedObject> processedDoors = new LinkedHashSet<PastedObject>();
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject == null || !pastedObject.getPastedObjectType().isOpenDoor() || processedDoors.contains(pastedObject)) {
                continue;
            }
            DoorEditContext doorContext = buildDoorEditContext(pastedObject);
            if (doorContext == null) {
                continue;
            }
            processedDoors.add(doorContext.primaryDoorObject);
            if (doorContext.openDoorObject != null) {
                processedDoors.add(doorContext.openDoorObject);
            }
            if (doorContext.closedDoorObject != null) {
                processedDoors.add(doorContext.closedDoorObject);
            }
            if (showDoorPolygonOverlays) {
                paintDoorPolygonOverlay(g, doorContext.sharedData.getOpenPolygon(), new Color(80, 220, 255, 45), new Color(80, 220, 255));
                paintDoorPolygonOverlay(g, doorContext.sharedData.getClosedPolygon(), new Color(255, 180, 0, 35), new Color(255, 180, 0));
            }
            if (showDoorImpededBlockOverlays) {
                paintDoorImpededCellsOverlay(g, doorContext.sharedData.getOpenImpededCells(), new Color(80, 220, 255, 90));
                paintDoorImpededCellsOverlay(g, doorContext.sharedData.getClosedImpededCells(), new Color(255, 180, 0, 90));
            }
        }
    }

    private void paintDoorPolygonOverlay(Graphics g, Polygon polygon, Color fillColor, Color outlineColor) {
        if (polygon == null || polygon.npoints < 3) {
            return;
        }
        g.setColor(fillColor);
        g.fillPolygon(polygon);
        g.setColor(outlineColor);
        g.drawPolygon(polygon);
    }

    private void paintDoorImpededCellsOverlay(Graphics g, List<Point> cells, Color color) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        g.setColor(color);
        for (Point cell : cells) {
            if (cell == null) {
                continue;
            }
            g.fillRect(
                cell.x * SearchMapData.CELL_WIDTH,
                cell.y * SearchMapData.CELL_HEIGHT,
                SearchMapData.CELL_WIDTH,
                SearchMapData.CELL_HEIGHT
            );
        }
    }

    private void paintDoorPointPlacementDraft(Graphics2D graphics) {
        if (doorPointPlacementSession == null) {
            return;
        }
        DirectionMarker.drawMarker(
            graphics,
            mousePosition.x,
            mousePosition.y,
            0,
            Color.CYAN,
            new Color(255, 230, 100),
            7,
            9
        );
        graphics.setColor(Color.WHITE);
        graphics.drawString(
            doorPointPlacementSession.title + " - click on the map to set it",
            mousePosition.x + 10,
            mousePosition.y - 10
        );
    }

    private void cancelExtractionPolygonSelection() {
        if (!isExtractionPolygonMode() || polygon.npoints == 0) {
            return;
        }
        polygon.reset();
        if (extractPanel != null) {
            extractPanel.repaint();
        }
    }

    private boolean isValidTileSetup() {
        return extractionBackgroundImage != null && polygon.npoints == 0 && !tile.isEmpty();
    }

    private void cleanupAndExtractSelectionWithNanoBanana() {
        if (!isValidTileSetup()) {
            showNanoBananaMessage(
                "Nano Banana 2 extraction requires a rectangle selection in the Extraction Area.",
                ERROR,
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String apiKey = UserPreferences.getGoogleAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            showNanoBananaMessage(
                "Set a Google AI API key in Settings -> User Preferences before using Nano Banana 2 extraction.",
                NANO_BANANA_EXTRACTION_TITLE,
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (nanoBananaEditorDialog != null && nanoBananaEditorDialog.isDisplayable()) {
            nanoBananaEditorDialog.loadSelection(tile.getSubImage(extractionBackgroundImage));
            nanoBananaEditorDialog.toFront();
            nanoBananaEditorDialog.requestFocus();
            return;
        }
        nanoBananaEditorDialog = new NanoBananaEditorDialog(this, tile.getSubImage(extractionBackgroundImage));
        nanoBananaEditorDialog.setVisible(true);
    }

    private void showNanoBananaMessage(String message, String title, int messageType) {
        JTextArea messageArea = new JTextArea(message, 6, 56);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setOpaque(false);
        messageArea.setBorder(null);
        messageArea.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, messageArea, title, messageType);
    }

    private void updateTexturePreviewFromTileSelection() {
        if (!isValidTileSetup()) {
            return;
        }
        BufferedImage textureImage = TileSeamless.createSeamlessTile(tile.getSubImage(extractionBackgroundImage));
        if (textureImage == null) {
            return;
        }
        for (int x = 0; x < texturePreviewImage.getWidth(); x++) {
            for (int y = 0; y < texturePreviewImage.getHeight(); y++) {
                texturePreviewImage.setRGB(x, y, textureImage.getRGB(x % textureImage.getWidth(), y % textureImage.getHeight()));
            }
        }
    }

    private void handleExtractPanelClick(MouseEvent event, JPanel panel) {
        if (suppressNextExtractClickAfterPan) {
            suppressNextExtractClickAfterPan = false;
            return;
        }
        if (extractionBackgroundImage == null) {
            return;
        }
        Point areaPoint = toAreaPoint(event, extractZoom);
        if (isExtractionPolygonMode()) {
            if (polygon.npoints > 0
                    && (SwingUtilities.isRightMouseButton(event)
                            || Point2D.distance(areaPoint.x, areaPoint.y, polygon.xpoints[0], polygon.ypoints[0]) <= 3)) {
                Rectangle r = clampToImageBounds(polygon.getBounds(), extractionBackgroundImage);
                if (r.width > 0 && r.height > 0) {
                    Polygon relativePolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                    relativePolygon.translate(-r.x, -r.y);
                    BufferedImage subimage = extractionBackgroundImage.getSubimage(r.x, r.y, r.width, r.height);
                    PolygonSelectionView polygonSelectionView = new PolygonSelectionView(subimage, relativePolygon);
                    polygonSelectionView.setLocation(event.getXOnScreen(), event.getYOnScreen());
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "The selected polygon is too small to extract.",
                        ERROR,
                        JOptionPane.WARNING_MESSAGE
                    );
                }
                polygon.reset();
            } else {
                polygon.addPoint(areaPoint.x, areaPoint.y);
            }
            panel.repaint();
            return;
        }
    }

    private void handleBuildPanelClick(MouseEvent event, JPanel panel) {
        MouseEvent scaledEvent = scaleMouseEvent(event, panel, buildZoom);
        if (suppressNextBuildClickAfterPan) {
            suppressNextBuildClickAfterPan = false;
            return;
        }
        if (handleSearchMapSelectionCanvasClick(scaledEvent)) {
            panel.repaint();
            return;
        }
        if (handleWallGroupPlacementCanvasClick(scaledEvent)) {
            panel.repaint();
            return;
        }
        if (handleTransitionPlacementCanvasClick(scaledEvent)) {
            panel.repaint();
            return;
        }
        if (editingBlackParallelogram || editingTextureParallelogram) {
            if (parallelograms.isEmpty() || parallelograms.get(parallelograms.size() - 1).npoints == 4) {
                Polygon parallelogram = new Polygon();
                parallelogram.addPoint(scaledEvent.getX(), scaledEvent.getY());
                parallelograms.add(parallelogram);
            } else {
                Polygon p = parallelograms.get(parallelograms.size() - 1);
                p.addPoint(scaledEvent.getX(), scaledEvent.getY());
                if (p.npoints == 3) {
                    p.addPoint(p.xpoints[0] + p.xpoints[2] - p.xpoints[1], p.ypoints[0] + p.ypoints[2] - p.ypoints[1]);
                    panel.repaint();
                    BufferedImage textureImage = editingBlackParallelogram ? null : chooseImageFile(FileChooserLocation.TEXTURE);
                    editingBlackParallelogram = editingTextureParallelogram = false;
                    BufferedImage floorImage = new BufferedImage(p.getBounds().width, p.getBounds().height, BufferedImage.TYPE_INT_ARGB);
                    for (int x = 0; x < floorImage.getWidth(); x++) {
                        for (int y = 0; y < floorImage.getHeight(); y++) {
                            if (p.contains(p.getBounds().getMinX() + x, p.getBounds().getMinY() + y)) {
                                if (textureImage != null) {
                                    floorImage.setRGB(x, y, textureImage.getRGB(x % textureImage.getWidth(), y % textureImage.getHeight()));
                                } else {
                                    floorImage.setRGB(x, y, Color.BLACK.getRGB());
                                }
                            } else {
                                floorImage.setRGB(x, y, 0);
                            }
                        }
                    }
                    if (textureImage != null) {
                        pastedObjects.add(0, new PastedObject(new Point(p.getBounds().x, p.getBounds().y), new ExportableImage(floorImage)));
                    } else {
                        pastedObjects.add(new PastedObject(new Point(p.getBounds().x, p.getBounds().y), new ExportableImage(floorImage)));
                    }
                    parallelograms.remove(parallelograms.size() - 1);
                    recordHistoryState();
                }
            }
        } else {
            if (SwingUtilities.isRightMouseButton(scaledEvent) || scaledEvent.isPopupTrigger()) {
                return;
            }

            if (objectToMove == null && selectedWallGroup == null && !hasSelectedSearchMapCells()) {
                if (selectSearchMapRegionAtPoint(scaledEvent.getPoint())) {
                    panel.repaint();
                    return;
                }
                int idx = 0;
                for (PastedObject pastedObject : pastedObjects) {
                    Rectangle rect = new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
                    if (rect.contains(scaledEvent.getX(), scaledEvent.getY())
                            && isClickablePastedObjectHit(pastedObject, scaledEvent.getX() - rect.x, scaledEvent.getY() - rect.y)
                            && pastedObject.isVisible(drawClosed, night)) {
                        movingRectangle = rect;
                        objectToMove = pastedObject;
                        objectToMoveIdx = idx;
                        Rectangle anchorRect = getPastedObjectBounds(pastedObject);
                        deltaX = scaledEvent.getX() - anchorRect.x;
                        deltaY = scaledEvent.getY() - anchorRect.y;
                        beginCompositeMove(pastedObject);
                    }
                    idx++;
                }
                if (scaledEvent.isControlDown() && objectToMove != null) {
                    objectToMove = objectToMove.copy();
                    pastedObjects.add(objectToMove);
                    objectToMoveIdx = pastedObjects.size() - 1;
                    deltaX = 0;
                    deltaY = 0;
                    beginCompositeMove(objectToMove);
                }
                if (objectToMove == null) {
                    WallGroupData wallGroup = findWallGroupAtPoint(scaledEvent.getPoint());
                    if (wallGroup != null) {
                        selectedWallGroup = wallGroup;
                        movingWallGroupBasePolygon = wallGroup.getPolygon();
                        Rectangle anchorRect = movingWallGroupBasePolygon.getBounds();
                        movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
                        deltaX = scaledEvent.getX() - anchorRect.x;
                        deltaY = scaledEvent.getY() - anchorRect.y;
                        beginCompositeMove(wallGroup);
                    }
                }
            } else {
                if (objectToMove != null || selectedWallGroup != null) {
                    snapMovingDoorToBestPixelMatch();
                    clearObjectMoveSelection();
                    recordHistoryState();
                } else if (hasSelectedSearchMapCells()) {
                    if (!selectSearchMapRegionAtPoint(scaledEvent.getPoint())) {
                        clearSelectedSearchMapCells();
                    }
                }
            }
        }
        panel.repaint();
    }

    private void snapMovingDoorToBestPixelMatch() {
        if (objectToMove == null || !objectToMove.getPastedObjectType().isDoor()) {
            return;
        }
        BufferedImage renderedDoorImage = objectToMove.getRenderedImage(night);
        BufferedImage underlyingImage = renderBuildContentUnderMovingObject();
        int currentScore = DoorPixelMatcher.countMatchingOpaquePixels(
            renderedDoorImage,
            underlyingImage,
            objectToMove.getX(),
            objectToMove.getY()
        );
        Point bestLocation = DoorPixelMatcher.findBestLocation(
            renderedDoorImage,
            underlyingImage,
            objectToMove.getX(),
            objectToMove.getY(),
            DOOR_PIXEL_SNAP_RADIUS
        );
        int bestScore = DoorPixelMatcher.countMatchingOpaquePixels(
            renderedDoorImage,
            underlyingImage,
            bestLocation.x,
            bestLocation.y
        );
        if (bestScore <= currentScore || (bestLocation.x == objectToMove.getX() && bestLocation.y == objectToMove.getY())) {
            return;
        }
        moveCurrentObjectSelectionBy(bestLocation.x - objectToMove.getX(), bestLocation.y - objectToMove.getY());
    }

    private BufferedImage renderBuildContentUnderMovingObject() {
        BufferedImage underlyingImage = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = underlyingImage.getGraphics();
        if (night) {
            graphics.drawImage(buildBackgroundNightImage, 0, 0, null);
        } else {
            graphics.drawImage(buildBackgroundImage, 0, 0, null);
        }
        int movingObjectIndex = objectToMoveIdx >= 0 ? objectToMoveIdx : pastedObjects.indexOf(objectToMove);
        int upperBound = movingObjectIndex >= 0 ? movingObjectIndex : pastedObjects.size();
        for (int i = 0; i < upperBound; i++) {
            PastedObject pastedObject = pastedObjects.get(i);
            if (pastedObject == objectToMove || isMovingCompositeObject(pastedObject) || !pastedObject.isVisible(drawClosed, night)) {
                continue;
            }
            pastedObject.drawImage(graphics, night);
        }
        graphics.dispose();
        return underlyingImage;
    }

    private boolean isMovingCompositeObject(PastedObject pastedObject) {
        return movingCompositeGroupId != null
            && pastedObject != null
            && movingCompositeGroupId.equals(pastedObject.getCompositeGroupId());
    }

    private void moveCurrentObjectSelectionBy(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (movingCompositeGroupId != null && !movingCompositeBaseLocations.isEmpty()) {
            for (PastedObject pastedObject : movingCompositeBaseLocations.keySet()) {
                setPastedObjectLocation(pastedObject, pastedObject.getX() + dx, pastedObject.getY() + dy);
            }
            for (WallGroupData wallGroupData : movingCompositeBaseWallPolygons.keySet()) {
                wallGroupData.setPolygon(PolygonUtils.translatedPolygon(wallGroupData.getPolygon(), dx, dy));
            }
        } else {
            setPastedObjectLocation(objectToMove, objectToMove.getX() + dx, objectToMove.getY() + dy);
        }
        Rectangle anchorRect = getPastedObjectBounds(objectToMove);
        movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
    }

    private void startSearchMapSelection() {
        ensureSearchMapSized();
        searchMapSelectionSession = new SearchMapSelectionSession();
        cancelDoorEditingSessions(false);
        painting = false;
        searchMapEditMode = SearchMapEditMode.NONE;
        clearObjectMoveSelection();
        wallGroupPlacementSession = null;
        if (transitionPlacementSession != null) {
            cancelTransitionPlacement(true);
        }
        if (tabPane != null) {
            tabPane.setSelectedComponent(buildScrollPane);
        }
        syncCursorModeUi();
        repaint();
    }

    private boolean handleSearchMapSelectionCanvasClick(MouseEvent event) {
        if (searchMapSelectionSession == null) {
            return false;
        }
        Polygon draft = searchMapSelectionSession.polygon;
        if (!SwingUtilities.isLeftMouseButton(event) && !SwingUtilities.isRightMouseButton(event)) {
            return true;
        }
        if (draft.npoints > 0
                && (SwingUtilities.isRightMouseButton(event)
                        || Point2D.distance(event.getX(), event.getY(), draft.xpoints[0], draft.ypoints[0]) <= 6)) {
            if (draft.npoints >= 3) {
                searchMapData.applyPolygonImpeded(draft);
                clearSelectedSearchMapCells();
                setShowSearchMapGridState(true);
                recordHistoryState();
            }
            searchMapSelectionSession = null;
            return true;
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point parallelogramClosePoint = getSearchMapParallelogramClosePoint(draft);
            if (parallelogramClosePoint != null && isWithinSearchMapParallelogramCloseZone(draft, event.getX(), event.getY())) {
                Polygon completedPolygon = clonePolygon(draft);
                completedPolygon.addPoint(parallelogramClosePoint.x, parallelogramClosePoint.y);
                searchMapData.applyPolygonImpeded(completedPolygon);
                clearSelectedSearchMapCells();
                setShowSearchMapGridState(true);
                recordHistoryState();
                searchMapSelectionSession = null;
                return true;
            }
            draft.addPoint(event.getX(), event.getY());
        }
        return true;
    }

    private Point getSearchMapParallelogramClosePoint(Polygon polygonBounds) {
        if (polygonBounds == null || polygonBounds.npoints != 3) {
            return null;
        }
        return new Point(
            polygonBounds.xpoints[0] + polygonBounds.xpoints[2] - polygonBounds.xpoints[1],
            polygonBounds.ypoints[0] + polygonBounds.ypoints[2] - polygonBounds.ypoints[1]
        );
    }

    private boolean isWithinSearchMapParallelogramCloseZone(Polygon polygonBounds, int x, int y) {
        Point closePoint = getSearchMapParallelogramClosePoint(polygonBounds);
        return closePoint != null
            && Point2D.distance(x, y, closePoint.x, closePoint.y) <= SearchMapSelectionSession.PARALLELOGRAM_CLOSE_RADIUS;
    }

    public void buildBrushPreview() {
        if (brushTexture != null) {
            int diameter = 2 * brushRadius;
            brushPreview = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < diameter; x++) {
                for (int y = 0; y < diameter; y++) {
                    double dist = Point2D.distance(x, y, brushRadius, brushRadius);
                    if (dist < brushRadius) {
                        brushPreview.setRGB(x, y, brushTexture.getRGB(x % brushTexture.getWidth(), y % brushTexture.getHeight()));
                    } else {
                        brushPreview.setRGB(x, y, 0);
                    }
                }
            }
            brushNightPreview = ImageFilter.applyNightFilter(brushPreview);
        }
    }

    /**
     * Export the current area design as a complete Baldur's Gate mod package.
     */
    private void exportAsBaldursGateMod() {
        try {
            String prefix = requireConfiguredPrefix();
            if (prefix == null) {
                return;
            }

            String modName = JOptionPane.showInputDialog(this,
                "Enter mod name (e.g., MyCustomMod):",
                "Export Baldur's Gate Mod",
                JOptionPane.QUESTION_MESSAGE);

            if (modName == null || modName.trim().isEmpty()) {
                return;
            }
            String areaName;
            int maxAreaIdLength = 6 - prefix.length();
            if (maxAreaIdLength < 1) {
                JOptionPane.showMessageDialog(this,
                    "The configured prefix is too long. Prefix + area id must leave room for the night suffix.",
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String areaId = JOptionPane.showInputDialog(this,
                "Enter owned area id without prefix (max " + maxAreaIdLength + " chars, night suffix reserved):",
                "Export Baldur's Gate Mod",
                JOptionPane.QUESTION_MESSAGE);

            if (areaId == null || areaId.trim().isEmpty()) {
                return;
            }

        areaId = areaId.trim().toUpperCase();
            if (areaId.length() > maxAreaIdLength) {
                areaId = areaId.substring(0, maxAreaIdLength);
            }
            if (!areaId.matches("[A-Z0-9]+")) {
                JOptionPane.showMessageDialog(this,
                    "Area id must only contain letters and digits.",
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            areaName = prefix + areaId;
            String nightAreaName = areaName + 'N';

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose output directory for mod");

            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File outputDir = chooser.getSelectedFile();
            WeiDUModPackager packager = new WeiDUModPackager(modName, areaName, nightAreaName, outputDir);
            String validationError = validateExistingAreaPatchGeometry();
            if (validationError != null) {
                JOptionPane.showMessageDialog(this,
                    validationError,
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            validationError = validateOwnedAreaDestinations();
            if (validationError != null) {
                JOptionPane.showMessageDialog(this,
                    validationError,
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerKnownOwnedAreas(areaName);
            BufferedImage dayOpenRender = renderArea(false, false);
            BufferedImage dayClosedRender = renderArea(true, false);
            BufferedImage nightOpenRender = renderArea(false, true);
            BufferedImage nightClosedRender = renderArea(true, true);
            List<BufferedImage> dayTiles = new ArrayList<>(TISFile.splitImage(dayOpenRender));
            List<BufferedImage> nightTiles = new ArrayList<>(TISFile.splitImage(nightOpenRender));
            List<DoorExportData> doorExports = collectDoorExports();
            WEDFile wedFile = new WEDFile(backgroundWidth, backgroundHeight, areaName);
            WEDFile nightWedFile = new WEDFile(backgroundWidth, backgroundHeight, nightAreaName);

            for (DoorExportData doorExport : doorExports) {
                for (Integer tileCell : doorExport.tileCells) {
                    if (tileCell >= 0 && tileCell < dayTiles.size()) {
                        BufferedImage closedTile = cropTile(dayClosedRender, tileCell);
                        BufferedImage nightClosedTile = cropTile(nightClosedRender, tileCell);
                        int alternateTileIndex = dayTiles.size();
                        dayTiles.add(closedTile);
                        nightTiles.add(nightClosedTile);
                        wedFile.setAlternateTileIndex(tileCell, alternateTileIndex);
                        nightWedFile.setAlternateTileIndex(tileCell, alternateTileIndex);
                    }
                }
                WEDFile.DoorDefinition dayDoor = new WEDFile.DoorDefinition(
                    doorExport.name,
                    true,
                    doorExport.tileCells,
                    Arrays.asList(doorExport.openPolygon),
                    Arrays.asList(doorExport.closedPolygon)
                );
                wedFile.addDoor(dayDoor);
                nightWedFile.addDoor(new WEDFile.DoorDefinition(
                    doorExport.name,
                    true,
                    doorExport.tileCells,
                    Arrays.asList(doorExport.openPolygon),
                    Arrays.asList(doorExport.closedPolygon)
                ));
            }
            for (WallGroupData wallGroup : wallGroups) {
                Polygon wallPolygon = wallGroup.getPolygon();
                if (wallPolygon != null && wallPolygon.npoints >= 3) {
                    wedFile.addWallPolygon(new WEDFile.WallPolygonDefinition(
                        wallPolygon,
                        wallGroup.getFlags(),
                        0
                    ));
                    nightWedFile.addWallPolygon(new WEDFile.WallPolygonDefinition(
                        wallPolygon,
                        wallGroup.getFlags(),
                        0
                    ));
                }
            }

            AREFile areFile = new AREFile();
            areFile.setAreaResRef(areaName);
            areFile.setWedResource(areaName);
            areFile.setAreaAttributes(areaAttributes);
            areFile.setWidth(backgroundWidth);
            areFile.setHeight(backgroundHeight);

            for (DoorExportData doorExport : doorExports) {
                AREFile.AREDoor door = new AREFile.AREDoor(
                    doorExport.name,
                    doorExport.id,
                    doorExport.openPolygon,
                    doorExport.closedPolygon
                );
                door.setFlags(doorExport.flags);
                door.setOpenImpededCells(doorExport.openImpededCells);
                door.setClosedImpededCells(doorExport.closedImpededCells);
                door.setTravelTriggerName(doorExport.regionLinkName);
                door.setOpenLocationFront(doorExport.openLocationFront);
                door.setOpenLocationBack(doorExport.openLocationBack);
                door.setLaunchPoint(doorExport.launchPoint);
                door.setCursorIndex(doorExport.cursorIndex);
                areFile.addDoor(door);
            }

            for (PastedObject obj : pastedObjects) {
                if (obj.getPastedObjectType() == PastedObjectType.ENTRANCE && obj.getEntranceData() != null) {
                    AREFile.AREEntrance entrance = new AREFile.AREEntrance(
                        obj.getEntranceData().getName(),
                        obj.getEntranceData().getX(),
                        obj.getEntranceData().getY(),
                        obj.getEntranceData().getOrientation()
                    );
                    areFile.addEntrance(entrance);

                    if (!obj.getEntranceData().getDestinationArea().trim().isEmpty()) {
                        AREFile.ARERegion exitRegion = new AREFile.ARERegion(
                            obj.getEntranceData().getName() + "_EXIT",
                            2,
                            rectanglePolygon(new Rectangle(obj.getX() - 24, obj.getY() - 24, 48, 48))
                        );
                        exitRegion.setDestinationArea(obj.getEntranceData().getDestinationArea());
                        exitRegion.setDestinationEntrance(resolveDestinationEntranceName(obj.getEntranceData()));
                        exitRegion.setFlags(0x0004);
                        areFile.addRegion(exitRegion);
                    }
                }
            }

            addSyntheticTravelRegionEntrances(areFile);

            for (RegionData regionData : regions) {
                AREFile.ARERegion region = new AREFile.ARERegion(
                    regionData.getName(),
                    regionData.getType(),
                    regionData.getBounds()
                );
                region.setScript(regionData.getScript());
                region.setDestinationArea(regionData.getDestinationArea());
                region.setDestinationEntrance(regionData.getDestinationEntrance());
                region.setFlags(regionData.getFlags());
                region.setTrapDetectionDifficulty(regionData.getTrapDetectionDifficulty());
                region.setTrapRemovalDifficulty(regionData.getTrapRemovalDifficulty());
                region.setTrapped(regionData.isTrapped());
                region.setTrapDetected(regionData.isTrapDetected());
                areFile.addRegion(region);
            }

            for (ContainerData containerData : containers) {
                Polygon bounds = containerData.getBounds() != null
                    ? containerData.getBounds()
                    : rectanglePolygon(new Rectangle(containerData.getX() - 24, containerData.getY() - 16, 48, 32));
                AREFile.AREContainer container = new AREFile.AREContainer(
                    containerData.getName(),
                    containerData.getX(),
                    containerData.getY(),
                    containerData.getContainerType() + 1,
                    bounds
                );
                int flags = 0;
                if (containerData.isLocked()) {
                    flags |= 0x0001;
                }
                if (containerData.isTrapped()) {
                    flags |= 0x0008;
                }
                container.setFlags(flags);
                container.setLockDifficulty(containerData.getLockDifficulty());
                container.setTrapDetectionDifficulty(containerData.getTrapDetectionDifficulty());
                container.setTrapRemovalDifficulty(containerData.getTrapRemovalDifficulty());
                container.setTrapped(containerData.isTrapped());
                container.setTrapDetected(containerData.isTrapDetected());
                container.setKeyItem(containerData.getKeyItem());
                container.setScript(containerData.getScript());
                areFile.addContainer(container);
            }

            PvrzTisFile tisFile = new PvrzTisFile(areaName, dayTiles);
            PvrzTisFile nightTisFile = new PvrzTisFile(nightAreaName, nightTiles);
            BufferedImage searchMap = createSearchMap();
            BufferedImage lightMap = createLightMap();
            BufferedImage heightMap = createHeightMap();
            Map<String, String> existingAreaPatches = buildExistingAreaTransitionPatches(areaName);
            packager.createModPackage(
                areFile,
                wedFile,
                nightWedFile,
                tisFile,
                nightTisFile,
                searchMap,
                lightMap,
                heightMap,
                existingAreaPatches
            );

            StringBuilder successMessage = new StringBuilder();
            successMessage.append("Mod package created successfully!\n\n");
            successMessage.append("Location: ").append(new File(outputDir, modName).getAbsolutePath()).append("\n\n");
            successMessage.append("Prefix: ").append(prefix).append('\n');
            successMessage.append("Files created:\n");
            successMessage.append("  - ").append(areaName).append(".ARE / .WED / .TIS\n");
            successMessage.append("  - ").append(nightAreaName).append(".WED / .TIS\n");
            successMessage.append("  - PVRZ component pages for day and night tilesets\n");
            successMessage.append("  - ").append(areaName).append("SR.bmp / LM.bmp / HT.bmp\n");
            if (!existingAreaPatches.isEmpty()) {
                successMessage.append("  - patches/*.tpa for ").append(existingAreaPatches.size())
                    .append(existingAreaPatches.size() == 1 ? " existing destination area\n" : " existing destination areas\n");
            }
            successMessage.append("  - ").append(modName).append(".tp2 (WeiDU installer)\n");
            successMessage.append("  - setup-").append(modName).append(".bat / .command\n\n");
            successMessage.append("PVRZ naming follows Near Infinity's TIS v2 convention.");
            if (!existingAreaPatches.isEmpty()) {
                successMessage.append("\n\nExisting-area patches were generated only for transitions marked to create a destination-side return path.");
            }

            JOptionPane.showMessageDialog(this,
                successMessage.toString(),
                "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                ERROR,
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                ERROR,
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage renderArea(boolean closedDoors, boolean nightMode) {
        BufferedImage image = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        try {
            paintObjects(graphics, closedDoors, nightMode, false);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void editExportPrefix() {
        List<PrefixReservation> reservations = loadPrefixReservations("/prefixes/ie-prefix-reservations.tsv");
        if (reservations.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No reserved prefix catalog could be loaded.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        PrefixSelectionDialog dialog = new PrefixSelectionDialog(this, reservations, UserPreferences.getExportPrefix());
        dialog.setVisible(true);
        PrefixReservation selected = dialog.getSelectedPrefix();
        if (selected == null) {
            return;
        }
        UserPreferences.setExportPrefix(selected.getPrefix());
        JOptionPane.showMessageDialog(this, "Saved export prefix: " + selected.getPrefix());
    }

    private String requireConfiguredPrefix() {
        String prefix = normalizeExportPrefix(UserPreferences.getExportPrefix());
        if (prefix != null) {
            return prefix;
        }
        JOptionPane.showMessageDialog(this,
            "Set an export prefix first. Use the Prefix button in the toolbar.",
            ERROR,
            JOptionPane.ERROR_MESSAGE);
        return null;
    }

    private String normalizeExportPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        prefix = prefix.trim();
        for (PrefixReservation reservation : loadPrefixReservations("/prefixes/ie-prefix-reservations.tsv")) {
            if (reservation.getPrefix().equals(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private void editUserPreferences() {
        JTextField gamePathField = new JTextField(UserPreferences.getGameInstallPath(), 33);
        JTextField googleAiApiKeyField = new JTextField(UserPreferences.getGoogleAiApiKey(), 33);
        JTextField storageLocationField = new JTextField(UserPreferences.getStorageLocation(), 33);
        storageLocationField.setEditable(false);
        storageLocationField.setCaretPosition(0);
        JSpinner zoomFactorSpinner = new JSpinner(new SpinnerNumberModel(
            Double.valueOf(UserPreferences.getZoomFactor()),
            Double.valueOf(1.01d),
            Double.valueOf(2.0d),
            Double.valueOf(0.01d)
        ));
        JSpinner.NumberEditor zoomFactorEditor = new JSpinner.NumberEditor(zoomFactorSpinner, "0.00");
        zoomFactorSpinner.setEditor(zoomFactorEditor);
        JButton browseGamePathButton = new JButton(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseGamePathButton.setToolTipText("Browse for game install directory");
        browseGamePathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select game install directory");
                if (chooser.showOpenDialog(J2DArea.this) == JFileChooser.APPROVE_OPTION) {
                    gamePathField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        JPanel gamePathPanel = new JPanel(new BorderLayout(5, 0));
        gamePathPanel.add(gamePathField, BorderLayout.CENTER);
        gamePathPanel.add(browseGamePathButton, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 6, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Game install path:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        contentPanel.add(gamePathPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Zoom factor:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        contentPanel.add(zoomFactorSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Google AI API key:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        contentPanel.add(googleAiApiKeyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Preferences file:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        contentPanel.add(storageLocationField, gbc);

        int result = JOptionPane.showConfirmDialog(
            this,
            contentPanel,
            "User Preferences",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            UserPreferences.setGameInstallPath(gamePathField.getText());
            UserPreferences.setZoomFactor(((Number) zoomFactorSpinner.getValue()).doubleValue());
            UserPreferences.setGoogleAiApiKey(googleAiApiKeyField.getText());
        }
    }

    private void showCommandsHelp() {
        JEditorPane helpTextPane = new JEditorPane("text/html",
            "<html><body style='font-family:sans-serif;font-size:12pt;padding:8px'>"
                + "<b>Build Area</b><br><br>"
                + "<b>Mouse</b><br>"
                + "<b>Left-drag</b> on a selected object: move it.<br>"
                + "<b>Ctrl + Click</b> on an object: duplicate it and start moving the copy.<br>"
                + "<b>Right-click</b> an object or transition marker: open its context menu.<br>"
                + "<b>Right-click</b> an opened door: choose <b>Edit Door</b> to open the full door editor dialog.<br>"
                + "<b>Door metadata overlays</b>: toggle polygon / impeded-block visibility from the door editor dialog.<br>"
                + "<b>Left-drag</b> in Search Map Painter / Eraser mode: edit search-map cells.<br>"
                + "<b>Door polygon editing</b>: use the door editor dialog to draw directly on the resized opened-door preview; <b>Backspace</b> removes the last vertex and <b>Enter</b> / <b>Esc</b> finish the current edit mode.<br>"
                + "<b>Door impeded-block editing</b>: use the door editor dialog to paint cells directly on the opened-door preview; <b>Enter</b> / <b>Esc</b> finish the current edit mode.<br>"
                + "<b>Door point editing</b>: click on the map to place the point, <b>Esc</b> to cancel.<br>"
                + "<b>Mouse Wheel</b>: zoom.<br>"
                + "<b>Shift + Mouse Wheel</b>: flip the selected object, or change brush size while painting.<br><br>"
                + "<b>Keyboard</b><br>"
                + "<b>Delete</b>: remove the selected object, wallgroup, or impeded-cell region.<br>"
                + "<b>+</b> or <b>Shift+=</b>: bring the selected object forward.<br>"
                + "<b>-</b> or <b>NumPad-</b> or <b>6</b>: send the selected object backward.<br>"
                + "<b>Up</b> / <b>Down</b>: adjust the selected object's vertical placement.<br><br>"
                + "<b>Extraction Area</b><br><br>"
                + "<b>Mouse</b><br>"
                + "<b>Map Drag</b> mode is the default extraction cursor mode.<br>"
                + "<b>Left-drag</b> in Rectangle Selection mode: create a rectangle selection.<br>"
                + "<b>Left-click</b> in Polygon Selection mode: add polygon vertices.<br>"
                + "<b>Right-click</b> or <b>click near the first vertex</b> in Polygon Selection mode: close the polygon.<br>"
                + "<b>Left-drag</b> in Map Drag mode: move the map.<br>"
                + "<b>Backspace</b>: remove the last polygon vertex.<br>"
                + "<b>Esc</b>: cancel the whole polygon selection.<br>"
                + "<b>Mouse Wheel</b>: zoom."
                + "</body></html>"
        );
        helpTextPane.setEditable(false);
        helpTextPane.setOpaque(false);
        helpTextPane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(helpTextPane);
        scrollPane.setPreferredSize(new Dimension(560, 420));
        JOptionPane.showMessageDialog(this, scrollPane, "Commands", JOptionPane.INFORMATION_MESSAGE);
    }

    private Map<String, String> buildExistingAreaTransitionPatches(String ownedAreaResref) {
        Map<String, StringBuilder> patchBuilders = new LinkedHashMap<>();

        for (PastedObject obj : pastedObjects) {
            if (obj.getPastedObjectType() != PastedObjectType.ENTRANCE || obj.getEntranceData() == null) {
                continue;
            }

            EntranceData entranceData = obj.getEntranceData();
            if (!shouldCreateExistingAreaPatch(entranceData)) {
                continue;
            }

            String targetArea = normalizeAreaResref(entranceData.getDestinationArea());
            StringBuilder patchBuilder = patchBuilders.get(targetArea);
            if (patchBuilder == null) {
                patchBuilder = new StringBuilder();
                patchBuilder.append("  // Generated by J2DArea for patching an existing destination area.\n");
                patchBuilder.append("  // Target area: ").append(targetArea).append('\n');
                patchBuilder.append("  // Owned source area: ").append(ownedAreaResref).append("\n\n");
                patchBuilders.put(targetArea, patchBuilder);
            }

            String destinationEntranceName = resolveDestinationEntranceName(entranceData);
            EntranceData destinationEntrance = new EntranceData(
                destinationEntranceName,
                entranceData.getDestinationPointX(),
                entranceData.getDestinationPointY()
            );
            destinationEntrance.setOrientation(entranceData.getDestinationPointOrientation());
            appendEntrancePatch(patchBuilder, destinationEntrance);

            appendRegionPatch(
                patchBuilder,
                buildDestinationReturnRegionName(ownedAreaResref, entranceData),
                2,
                resolveDestinationReturnPolygon(entranceData),
                ownedAreaResref,
                trimToEmpty(entranceData.getName()),
                0x0004,
                "",
                0,
                0,
                false,
                false
            );
        }

        for (RegionData regionData : regions) {
            if (!shouldCreateExistingAreaPatch(regionData)) {
                continue;
            }

            String targetArea = normalizeAreaResref(regionData.getDestinationArea());
            StringBuilder patchBuilder = patchBuilders.get(targetArea);
            if (patchBuilder == null) {
                patchBuilder = new StringBuilder();
                patchBuilder.append("  // Generated by J2DArea for patching an existing destination area.\n");
                patchBuilder.append("  // Target area: ").append(targetArea).append('\n');
                patchBuilder.append("  // Owned source area: ").append(ownedAreaResref).append("\n\n");
                patchBuilders.put(targetArea, patchBuilder);
            }

            EntranceData destinationEntrance = new EntranceData(
                resolveDestinationEntranceName(regionData),
                regionData.getDestinationPointX(),
                regionData.getDestinationPointY()
            );
            destinationEntrance.setOrientation(regionData.getDestinationPointOrientation());
            appendEntrancePatch(patchBuilder, destinationEntrance);

            appendRegionPatch(
                patchBuilder,
                buildDestinationReturnRegionName(ownedAreaResref, regionData),
                2,
                regionData.getDestinationReturnPolygon(),
                ownedAreaResref,
                resolveOwnedAreaReturnEntranceName(regionData),
                0x0004,
                "",
                0,
                0,
                false,
                false
            );
        }

        Map<String, String> patches = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : patchBuilders.entrySet()) {
            patches.put(entry.getKey(), entry.getValue().toString());
        }
        return patches;
    }

    private void appendEntrancePatch(StringBuilder out, EntranceData entranceData) {
        out.append("  LPF fj_are_structure\n");
        out.append("    INT_VAR\n");
        out.append("      fj_loc_x             = ").append(entranceData.getX()).append('\n');
        out.append("      fj_loc_y             = ").append(entranceData.getY()).append('\n');
        out.append("      fj_orientation       = ").append(entranceData.getOrientation()).append('\n');
        out.append("    STR_VAR\n");
        out.append("      fj_structure_type    = entrance\n");
        out.append("      fj_name              = ~").append(escapeWeiDUString(entranceData.getName())).append("~\n");
        out.append("  END\n\n");
    }

    private void appendRegionPatch(StringBuilder out, String name, int type, Polygon polygon,
            String destinationArea, String destinationEntrance, int flags, String script,
            int trapDetectionDifficulty, int trapRemovalDifficulty, boolean trapped, boolean trapDetected) {
        if (polygon == null || polygon.npoints == 0) {
            return;
        }
        Rectangle bounds = polygon.getBounds();
        out.append("  LPF fj_are_structure\n");
        out.append("    INT_VAR\n");
        out.append("      fj_type              = ").append(type).append('\n');
        out.append("      fj_box_left          = ").append(bounds.x).append('\n');
        out.append("      fj_box_top           = ").append(bounds.y).append('\n');
        out.append("      fj_box_right         = ").append(bounds.x + bounds.width).append('\n');
        out.append("      fj_box_bottom        = ").append(bounds.y + bounds.height).append('\n');
        out.append("      fj_cursor_idx        = 34\n");
        out.append("      fj_flags             = ").append(flags).append('\n');
        out.append("      fj_trap_detect       = ").append(trapDetectionDifficulty).append('\n');
        out.append("      fj_trap_remove       = ").append(trapRemovalDifficulty).append('\n');
        out.append("      fj_trap_active       = ").append(trapped ? 1 : 0).append('\n');
        out.append("      fj_trap_status       = ").append(trapDetected ? 1 : 0).append('\n');
        out.append("      fj_loc_x             = ").append(bounds.x + (bounds.width / 2)).append('\n');
        out.append("      fj_loc_y             = ").append(bounds.y + (bounds.height / 2)).append('\n');
        for (int i = 0; i < polygon.npoints; i++) {
            int vertex = polygon.xpoints[i] + (polygon.ypoints[i] << 16);
            out.append("      fj_vertex_").append(i).append("          = ").append(vertex).append('\n');
        }
        out.append("    STR_VAR\n");
        out.append("      fj_structure_type    = region\n");
        out.append("      fj_name              = ~").append(escapeWeiDUString(name)).append("~\n");
        out.append("      fj_destination_area  = ~").append(escapeWeiDUString(destinationArea)).append("~\n");
        out.append("      fj_destination_name  = ~").append(escapeWeiDUString(destinationEntrance)).append("~\n");
        out.append("      fj_reg_script        = ~").append(escapeWeiDUString(script)).append("~\n");
        out.append("  END\n\n");
    }

    private String escapeWeiDUString(String value) {
        return value != null ? value.replace("~", "") : "";
    }

    private void addSyntheticTravelRegionEntrances(AREFile areFile) {
        Set<String> existingEntranceNames = collectExistingEntranceNames();
        for (RegionData regionData : regions) {
            if (!shouldCreateExistingAreaPatch(regionData)) {
                continue;
            }
            String localEntranceName = resolveOwnedAreaReturnEntranceName(regionData);
            if (localEntranceName.isEmpty() || existingEntranceNames.contains(localEntranceName)) {
                continue;
            }
            Point center = getPolygonCenter(regionData.getBounds());
            areFile.addEntrance(new AREFile.AREEntrance(localEntranceName, center.x, center.y, 0));
            existingEntranceNames.add(localEntranceName);
        }
    }

    private Set<String> collectExistingEntranceNames() {
        Set<String> names = new LinkedHashSet<>();
        for (PastedObject obj : pastedObjects) {
            if (obj.getPastedObjectType() == PastedObjectType.ENTRANCE && obj.getEntranceData() != null) {
                String name = trimToEmpty(obj.getEntranceData().getName());
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private boolean shouldCreateExistingAreaPatch(EntranceData entranceData) {
        return entranceData != null
            && entranceData.getDestinationAreaType() == DestinationAreaType.EXISTING_GAME_AREA
            && entranceData.isCreateDestinationReturnTransition()
            && !trimToEmpty(entranceData.getDestinationArea()).isEmpty();
    }

    private boolean shouldCreateExistingAreaPatch(RegionData regionData) {
        return regionData != null
            && regionData.getType() == 2
            && regionData.getDestinationAreaType() == DestinationAreaType.EXISTING_GAME_AREA
            && !trimToEmpty(regionData.getDestinationArea()).isEmpty()
            && regionData.getBounds() != null
            && regionData.getBounds().npoints > 0
            && regionData.getDestinationReturnPolygon() != null
            && regionData.getDestinationReturnPolygon().npoints >= 3;
    }

    private String resolveDestinationEntranceName(EntranceData entranceData) {
        String destinationEntrance = trimToEmpty(entranceData.getDestinationEntrance());
        return destinationEntrance.isEmpty() ? trimToEmpty(entranceData.getName()) : destinationEntrance;
    }

    private String resolveDestinationEntranceName(RegionData regionData) {
        String destinationEntrance = trimToEmpty(regionData.getDestinationEntrance());
        return destinationEntrance.isEmpty() ? trimToEmpty(regionData.getName()) : destinationEntrance;
    }

    private String resolveOwnedAreaReturnEntranceName(RegionData regionData) {
        return trimToEmpty(regionData.getName());
    }

    private String buildDestinationReturnRegionName(String ownedAreaResref, EntranceData entranceData) {
        String baseName = trimToEmpty(ownedAreaResref) + "_" + trimToEmpty(entranceData.getName()) + "_EXIT";
        return baseName.length() > 32 ? baseName.substring(0, 32) : baseName;
    }

    private String buildDestinationReturnRegionName(String ownedAreaResref, RegionData regionData) {
        String baseName = trimToEmpty(ownedAreaResref) + "_" + trimToEmpty(regionData.getName()) + "_EXIT";
        return baseName.length() > 32 ? baseName.substring(0, 32) : baseName;
    }

    private Point getPolygonCenter(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Point(0, 0);
        }
        Rectangle bounds = polygon.getBounds();
        return new Point(bounds.x + (bounds.width / 2), bounds.y + (bounds.height / 2));
    }

    private String detectEdgeDirection(Polygon polygon) {
        if (polygon == null || polygon.npoints < 3) {
            return null;
        }
        Rectangle bounds = polygon.getBounds();
        boolean touchesNorth = bounds.y <= 0;
        boolean touchesSouth = bounds.y + bounds.height >= backgroundHeight;
        boolean touchesWest = bounds.x <= 0;
        boolean touchesEast = bounds.x + bounds.width >= backgroundWidth;
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

    private String validateExistingAreaPatchGeometry() {
        List<String> invalidTravelRegions = new ArrayList<>();
        for (RegionData regionData : regions) {
            if (regionData == null
                    || regionData.getType() != 2
                    || regionData.getDestinationAreaType() != DestinationAreaType.EXISTING_GAME_AREA
                    || trimToEmpty(regionData.getDestinationArea()).isEmpty()) {
                continue;
            }
            Polygon destinationPolygon = regionData.getDestinationReturnPolygon();
            if (destinationPolygon == null || destinationPolygon.npoints < 3) {
                invalidTravelRegions.add(trimToEmpty(regionData.getName()).isEmpty() ? "<unnamed travel region>" : regionData.getName());
            }
        }
        if (invalidTravelRegions.isEmpty()) {
            return null;
        }
        StringBuilder message = new StringBuilder();
        message.append("Existing-area travel regions need explicit destination-side geometry from the configured game install before export.\n\n");
        message.append("Missing destination polygons:\n");
        for (String regionName : invalidTravelRegions) {
            message.append(" - ").append(regionName).append('\n');
        }
        message.append("\nOpen Regions, edit each travel region, pair it with an entrance, and use 'Select In Area...' to draw the destination-side return polygon.");
        return message.toString();
    }

    private Polygon resolveDestinationReturnPolygon(EntranceData entranceData) {
        Polygon polygon = entranceData.getDestinationReturnPolygon();
        if (polygon != null && polygon.npoints >= 3) {
            return polygon;
        }
        return rectanglePolygon(new Rectangle(
            entranceData.getDestinationPointX() - 24,
            entranceData.getDestinationPointY() - 24,
            48,
            48
        ));
    }

    private String normalizeAreaResref(String value) {
        return trimToEmpty(value).toUpperCase();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private List<DoorExportData> collectDoorExports() {
        List<DoorExportData> exports = new ArrayList<>();
        Set<PastedObject> processedDoors = new LinkedHashSet<PastedObject>();
        int index = 1;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject == null || !pastedObject.getPastedObjectType().isDoor() || processedDoors.contains(pastedObject)) {
                continue;
            }
            DoorEditContext doorContext = buildDoorEditContext(pastedObject);
            if (doorContext == null) {
                continue;
            }
            if (doorContext.openDoorObject != null) {
                processedDoors.add(doorContext.openDoorObject);
            }
            if (doorContext.closedDoorObject != null) {
                processedDoors.add(doorContext.closedDoorObject);
            }
            exports.add(createDoorExportData(index++, doorContext));
        }
        return exports;
    }

    private DoorExportData createDoorExportData(int index, DoorEditContext doorContext) {
        Rectangle openBounds = resolveDoorBounds(
            doorContext.openDoorObject,
            doorContext.sharedData.getOpenPolygon(),
            doorContext.closedDoorObject != null ? objectBounds(doorContext.closedDoorObject) : null
        );
        Rectangle closedBounds = resolveDoorBounds(
            doorContext.closedDoorObject,
            doorContext.sharedData.getClosedPolygon(),
            doorContext.openDoorObject != null ? objectBounds(doorContext.openDoorObject) : null
        );
        Rectangle unionBounds = openBounds.union(closedBounds);
        List<Integer> tileCells = new ArrayList<>();
        int startTileX = Math.max(0, unionBounds.x / 64);
        int endTileX = Math.min((backgroundWidth - 1) / 64, (unionBounds.x + unionBounds.width - 1) / 64);
        int startTileY = Math.max(0, unionBounds.y / 64);
        int endTileY = Math.min((backgroundHeight - 1) / 64, (unionBounds.y + unionBounds.height - 1) / 64);
        int tilesPerRow = (backgroundWidth + 63) / 64;
        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                tileCells.add(tileY * tilesPerRow + tileX);
            }
        }
        String doorName = "DOOR" + String.format("%04d", index);
        String doorId = doorName;
        DoorExportSupport.DoorAutoLink autoLink = DoorExportSupport.autoLink(
            openBounds,
            closedBounds,
            regions,
            collectEntrancesByName()
        );
        int flags = doorContext.sharedData.getFlags();
        String regionLinkName = trimToEmpty(doorContext.sharedData.getRegionLinkName());
        Point openLocationFront = doorContext.sharedData.getOpenLocationFront();
        Point openLocationBack = doorContext.sharedData.getOpenLocationBack();
        Point launchPoint = doorContext.sharedData.getLaunchPoint();
        if (regionLinkName.isEmpty() && !trimToEmpty(autoLink.getRegionName()).isEmpty()) {
            flags |= autoLink.getFlags();
            regionLinkName = autoLink.getRegionName();
            openLocationFront = autoLink.getOpenLocationFront();
            openLocationBack = autoLink.getOpenLocationBack();
            launchPoint = autoLink.getLaunchPoint();
        }
        return new DoorExportData(
            doorName,
            doorId,
            resolveDoorPolygon(doorContext.sharedData.getOpenPolygon(), doorContext.openDoorObject, openBounds),
            resolveDoorPolygon(doorContext.sharedData.getClosedPolygon(), doorContext.closedDoorObject, closedBounds),
            tileCells,
            doorContext.sharedData.getOpenImpededCells(),
            doorContext.sharedData.getClosedImpededCells(),
            flags,
            regionLinkName,
            openLocationFront,
            openLocationBack,
            launchPoint,
            doorContext.sharedData.getCursorIndex()
        );
    }

    private Rectangle objectBounds(PastedObject pastedObject) {
        if (pastedObject == null) {
            return new Rectangle();
        }
        return new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
    }

    private Polygon createDoorPolygon(Rectangle bounds) {
        int left = bounds.x;
        int right = bounds.x + bounds.width;
        int top = bounds.y;
        int bottom = bounds.y + bounds.height;
        return new Polygon(
            new int[] {right, right, left, left},
            new int[] {bottom, top, top, bottom},
            4
        );
    }

    private Rectangle resolveDoorBounds(PastedObject doorObject, Polygon polygon, Rectangle fallbackBounds) {
        Rectangle bounds = fallbackBounds != null ? new Rectangle(fallbackBounds) : new Rectangle();
        if (doorObject != null) {
            bounds = objectBounds(doorObject);
        }
        if (polygon != null && polygon.npoints >= 3) {
            bounds = bounds.isEmpty() ? polygon.getBounds() : bounds.union(polygon.getBounds());
        }
        return bounds;
    }

    private Polygon resolveDoorPolygon(Polygon polygon, PastedObject doorObject, Rectangle fallbackBounds) {
        if (polygon != null && polygon.npoints >= 3) {
            return clonePolygon(polygon);
        }
        Rectangle bounds = resolveDoorBounds(doorObject, null, fallbackBounds);
        return createDoorPolygon(bounds);
    }

    private Polygon rectanglePolygon(Rectangle rectangle) {
        return new Polygon(
            new int[] {rectangle.x, rectangle.x + rectangle.width, rectangle.x + rectangle.width, rectangle.x},
            new int[] {rectangle.y, rectangle.y, rectangle.y + rectangle.height, rectangle.y + rectangle.height},
            4
        );
    }

    private BufferedImage cropTile(BufferedImage image, int tileCellIndex) {
        int tilesPerRow = (backgroundWidth + 63) / 64;
        int tileX = (tileCellIndex % tilesPerRow) * 64;
        int tileY = (tileCellIndex / tilesPerRow) * 64;
        int width = Math.min(64, image.getWidth() - tileX);
        int height = Math.min(64, image.getHeight() - tileY);
        BufferedImage tileImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = tileImage.getGraphics();
        try {
            graphics.drawImage(image, 0, 0, width, height, tileX, tileY, tileX + width, tileY + height, null);
        } finally {
            graphics.dispose();
        }
        return tileImage;
    }

    private BufferedImage createSearchMap() {
        ensureSearchMapSized();
        return searchMapData.toImage(backgroundWidth, backgroundHeight);
    }

    private void ensureSearchMapSized() {
        if (searchMapData == null) {
            searchMapData = new SearchMapData(backgroundWidth, backgroundHeight);
        } else {
            searchMapData.resizeForPixels(backgroundWidth, backgroundHeight);
        }
    }

    private void applyWholeBackgroundSearchType(BufferedImage sourceImage) {
        ensureSearchMapSized();
        searchMapData.setAll(SearchMapTileType.classifyTexture(sourceImage));
    }

    private BufferedImage createLightMap() {
        BufferedImage image = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        try {
            graphics.setColor(new Color(128, 128, 128));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private BufferedImage createHeightMap() {
        BufferedImage image = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static class DoorExportData {
        private final String name;
        private final String id;
        private final Polygon openPolygon;
        private final Polygon closedPolygon;
        private final List<Integer> tileCells;
        private final List<Point> openImpededCells;
        private final List<Point> closedImpededCells;
        private final int flags;
        private final String regionLinkName;
        private final Point openLocationFront;
        private final Point openLocationBack;
        private final Point launchPoint;
        private final int cursorIndex;

        private DoorExportData(String name, String id, Polygon openPolygon, Polygon closedPolygon, List<Integer> tileCells,
                List<Point> openImpededCells, List<Point> closedImpededCells, int flags, String regionLinkName,
                Point openLocationFront, Point openLocationBack, Point launchPoint, int cursorIndex) {
            this.name = name;
            this.id = id;
            this.openPolygon = openPolygon;
            this.closedPolygon = closedPolygon;
            this.tileCells = tileCells;
            this.openImpededCells = openImpededCells;
            this.closedImpededCells = closedImpededCells;
            this.flags = flags;
            this.regionLinkName = regionLinkName;
            this.openLocationFront = openLocationFront;
            this.openLocationBack = openLocationBack;
            this.launchPoint = launchPoint;
            this.cursorIndex = cursorIndex;
        }
    }

    private int countEntrances() {
        int count = 0;
        for (PastedObject obj : pastedObjects) {
            if (obj.getPastedObjectType() == PastedObjectType.ENTRANCE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Opens the entrance editor dialog for editing entrance properties.
     */
    private void editEntrance(PastedObject entranceObject) {
        if (entranceObject == null || entranceObject.getEntranceData() == null) {
            return;
        }
        syncEntranceDataFromObject(entranceObject);

        List<AreaReference> availableAreas = collectAvailableAreas();
        EntranceEditorDialog dialog = new EntranceEditorDialog(
            this,
            entranceObject.getEntranceData(),
            availableAreas,
            collectKnownOwnedAreaReferences(),
            UserPreferences.getExportPrefix(),
            collectReservedOwnedAreaResrefs()
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            syncEntranceMarker(entranceObject);
            recordHistoryState();
        }
    }

    private void editRegion(RegionData regionData) {
        if (regionData == null) {
            return;
        }
        RegionEditorDialog dialog = new RegionEditorDialog(
            this,
            regionData,
            collectAvailableAreas(),
            collectKnownOwnedAreaReferences(),
            collectEntranceNames(),
            UserPreferences.getExportPrefix(),
            collectReservedOwnedAreaResrefs(),
            backgroundWidth,
            backgroundHeight
        );
        dialog.setVisible(true);
        recordHistoryState();
    }

    private void editRegions() {
        RegionManagerDialog dialog = new RegionManagerDialog(
            this,
            regions,
            collectAvailableAreas(),
            collectKnownOwnedAreaReferences(),
            collectEntranceNames(),
            UserPreferences.getExportPrefix(),
            collectReservedOwnedAreaResrefs(),
            clonePolygon(polygon),
            backgroundWidth,
            backgroundHeight
        );
        dialog.setVisible(true);
        recordHistoryState();
        if (dialog.isUsedCurrentSelection()) {
            setExtractionCursorMode(ExtractionCursorMode.RECTANGLE);
        }
        repaint();
    }

    private void editWallGroups() {
        startWallGroupPlacement(null);
        if (tabPane != null) {
            tabPane.setSelectedComponent(buildScrollPane);
        }
        repaint();
    }

    private void editWallGroup(WallGroupData wallGroup) {
        if (wallGroup == null) {
            return;
        }
        WallGroupEditorDialog dialog = new WallGroupEditorDialog(this, wallGroup);
        dialog.setVisible(true);
        recordHistoryState();
    }

    private List<String> collectEntranceNames() {
        List<String> names = new ArrayList<>();
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE && pastedObject.getEntranceData() != null) {
                String name = trimToEmpty(pastedObject.getEntranceData().getName());
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private void refreshEntranceMarkers() {
        for (PastedObject pastedObject : pastedObjects) {
            syncEntranceMarker(pastedObject);
        }
    }

    private void syncEntranceMarker(PastedObject pastedObject) {
        if (pastedObject == null || pastedObject.getPastedObjectType() != PastedObjectType.ENTRANCE
                || pastedObject.getEntranceData() == null) {
            return;
        }
        pastedObject.setImage(new ExportableImage(
            DirectionMarker.createEntranceMarkerImage(pastedObject.getEntranceData().getOrientation())
        ));
        pastedObject.setLocation(new Point(
            pastedObject.getEntranceData().getX() - (pastedObject.getImage().getWidth() / 2),
            pastedObject.getEntranceData().getY() - (pastedObject.getImage().getHeight() / 2)
        ));
        pastedObject.initBuffers();
    }

    private void syncEntranceDataFromObject(PastedObject pastedObject) {
        if (pastedObject == null || pastedObject.getPastedObjectType() != PastedObjectType.ENTRANCE
                || pastedObject.getEntranceData() == null) {
            return;
        }
        pastedObject.getEntranceData().setX(pastedObject.getX() + (pastedObject.getWidth() / 2));
        pastedObject.getEntranceData().setY(pastedObject.getY() + (pastedObject.getHeight() / 2));
    }

    private void beginTransitionPlacement(String entranceName) {
        beginTransitionPlacement(entranceName, null);
    }

    private void beginTransitionPlacement(String entranceName, PastedObject linkedDoorObject) {
        cancelTransitionPlacement(false);
        cancelDoorEditingSessions(false);
        searchMapSelectionSession = null;
        wallGroupPlacementSession = null;
        transitionPlacementSession = new TransitionPlacementSession(entranceName, linkedDoorObject);
        localTransitionPlacementDialog = new LocalTransitionPlacementDialog(
            this,
            "Local Transition Geometry",
            transitionPlacementSession.orientation,
            new LocalTransitionPlacementDialog.Listener() {
                @Override
                public void onStateChanged() {
                    if (transitionPlacementSession != null) {
                        transitionPlacementSession.orientation = localTransitionPlacementDialog.getSelectedOrientation();
                        updateTransitionPlacementSummary();
                        repaint();
                    }
                }

                @Override
                public void onClearPolygon() {
                    if (transitionPlacementSession != null) {
                        transitionPlacementSession.localPolygon = new Polygon();
                        updateTransitionPlacementSummary();
                        repaint();
                    }
                }

                @Override
                public void onUndoVertex() {
                    if (transitionPlacementSession != null && transitionPlacementSession.localPolygon.npoints > 0) {
                        transitionPlacementSession.localPolygon = copyWithoutLastVertex(transitionPlacementSession.localPolygon);
                        updateTransitionPlacementSummary();
                        repaint();
                    }
                }

                @Override
                public void onConfirm() {
                    confirmTransitionPlacement();
                }

                @Override
                public void onCancel() {
                    cancelTransitionPlacement(true);
                }
            }
        );
        updateTransitionPlacementSummary();
        localTransitionPlacementDialog.setVisible(true);
        buildPanel.repaint();
    }

    private boolean handleTransitionPlacementCanvasClick(MouseEvent e) {
        if (transitionPlacementSession == null || localTransitionPlacementDialog == null) {
            return false;
        }
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return true;
        }
        transitionPlacementSession.orientation = localTransitionPlacementDialog.getSelectedOrientation();
        if (localTransitionPlacementDialog.isPointModeSelected()) {
            transitionPlacementSession.hasPoint = true;
            transitionPlacementSession.entranceX = e.getX();
            transitionPlacementSession.entranceY = e.getY();
        } else {
            transitionPlacementSession.localPolygon.addPoint(e.getX(), e.getY());
        }
        updateTransitionPlacementSummary();
        return true;
    }

    private void updateTransitionPlacementSummary() {
        if (localTransitionPlacementDialog == null || transitionPlacementSession == null) {
            return;
        }
        localTransitionPlacementDialog.setSelectionSummary(
            transitionPlacementSession.hasPoint,
            transitionPlacementSession.entranceX,
            transitionPlacementSession.entranceY,
            transitionPlacementSession.localPolygon.npoints
        );
    }

    private void confirmTransitionPlacement() {
        if (transitionPlacementSession == null) {
            return;
        }
        if (!transitionPlacementSession.hasPoint) {
            JOptionPane.showMessageDialog(this,
                "Pick the entrance spawn point on the area before confirming.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (transitionPlacementSession.localPolygon.npoints < 3) {
            JOptionPane.showMessageDialog(this,
                "Draw the local travel-region exit polygon before confirming.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        EntranceData entranceData = new EntranceData(
            transitionPlacementSession.entranceName,
            transitionPlacementSession.entranceX,
            transitionPlacementSession.entranceY
        );
        entranceData.setOrientation(transitionPlacementSession.orientation);

        PastedObject entranceObject = new PastedObject(
            new Point(transitionPlacementSession.entranceX, transitionPlacementSession.entranceY),
            new ExportableImage(DirectionMarker.createEntranceMarkerImage(transitionPlacementSession.orientation)),
            PastedObjectType.ENTRANCE
        );
        entranceObject.setEntranceData(entranceData);
        syncEntranceMarker(entranceObject);
        pastedObjects.add(entranceObject);

        RegionData regionData = new RegionData(buildPairedTravelRegionName(transitionPlacementSession.entranceName), 2,
            clonePolygon(transitionPlacementSession.localPolygon));
        regionData.setPairedEntranceName(transitionPlacementSession.entranceName);
        regions.add(regionData);
        if (transitionPlacementSession.linkedDoorObject != null) {
            linkDoorToRegion(transitionPlacementSession.linkedDoorObject, regionData);
        } else {
            offerToLinkDoorToRegion(regionData);
        }

        cancelTransitionPlacement(false);
        recordHistoryState();
        repaint();
    }

    private void cancelTransitionPlacement(boolean disposeDialog) {
        transitionPlacementSession = null;
        if (localTransitionPlacementDialog != null) {
            LocalTransitionPlacementDialog dialog = localTransitionPlacementDialog;
            localTransitionPlacementDialog = null;
            if (disposeDialog || dialog.isDisplayable()) {
                dialog.dispose();
            }
        }
        repaint();
    }

    private void paintTransitionPlacementDraft(Graphics g) {
        if (transitionPlacementSession == null) {
            return;
        }
        Graphics g2 = g.create();
        try {
            Polygon polygonDraft = transitionPlacementSession.localPolygon;
            if (polygonDraft != null && polygonDraft.npoints > 0) {
                g2.setColor(new Color(255, 165, 0, 50));
                if (polygonDraft.npoints >= 3) {
                    g2.fillPolygon(polygonDraft);
                }
                g2.setColor(new Color(255, 200, 0));
                g2.drawPolygon(polygonDraft);
                for (int i = 0; i < polygonDraft.npoints; i++) {
                    g2.fillOval(polygonDraft.xpoints[i] - 3, polygonDraft.ypoints[i] - 3, 7, 7);
                }
            }
            if (transitionPlacementSession.hasPoint) {
                DirectionMarker.drawMarker(
                    (Graphics2D) g2,
                    transitionPlacementSession.entranceX,
                    transitionPlacementSession.entranceY,
                    transitionPlacementSession.orientation,
                    Color.CYAN,
                    new Color(255, 230, 100),
                    7,
                    9
                );
            }
        } finally {
            g2.dispose();
        }
    }

    private boolean handleWallGroupPlacementCanvasClick(MouseEvent e) {
        if (wallGroupPlacementSession == null) {
            return false;
        }
        if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            finishWallGroupPlacement();
            return true;
        }
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return true;
        }
        Polygon draft = wallGroupPlacementSession.polygon;
        if (draft.npoints > 0 && Point2D.distance(e.getX(), e.getY(), draft.xpoints[0], draft.ypoints[0]) <= 6) {
            if (draft.npoints >= 3) {
                finishWallGroupPlacement();
            }
            return true;
        }
        draft.addPoint(e.getX(), e.getY());
        return true;
    }

    private void startWallGroupPlacement(WallGroupData targetWallGroup) {
        wallGroupPlacementSession = new WallGroupPlacementSession(targetWallGroup);
        cancelDoorEditingSessions(false);
        searchMapSelectionSession = null;
        clearObjectMoveSelection();
        if (transitionPlacementSession != null) {
            cancelTransitionPlacement(true);
        }
    }

    private void finishWallGroupPlacement() {
        if (wallGroupPlacementSession == null) {
            return;
        }
        Polygon draft = wallGroupPlacementSession.polygon;
        if (draft.npoints < 3) {
            wallGroupPlacementSession = null;
            repaint();
            return;
        }

        WallGroupData target = wallGroupPlacementSession.targetWallGroup;
        boolean creating = target == null;
        if (creating) {
            target = new WallGroupData("Wallgroup" + (wallGroups.size() + 1), draft);
        } else {
            if (target.isHoveringWall() && draft.npoints < 5) {
                JOptionPane.showMessageDialog(this,
                    "Hovering walls need at least five vertices.",
                    ERROR,
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            target.setPolygon(draft);
        }

        if (creating) {
            WallGroupEditorDialog dialog = new WallGroupEditorDialog(this, target);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                wallGroups.add(target);
            }
        }
        wallGroupPlacementSession = null;
        recordHistoryState();
        repaint();
    }

    private boolean showBuildPanelContextMenu(MouseEvent e) {
        if (e == null || !e.isPopupTrigger()) {
            return false;
        }

        PastedObject doorObject = findDoorAtPoint(e.getX(), e.getY());
        if (doorObject != null) {
            clearObjectMoveSelection();
            showDoorContextMenu(e, doorObject);
            return true;
        }

        WallGroupData wallGroup = findWallGroupAtPoint(e.getPoint());
        if (wallGroup != null) {
            clearObjectMoveSelection();
            showWallGroupContextMenu(e, wallGroup);
            return true;
        }

        PastedObject entranceObject = findEntranceAtPoint(e.getX(), e.getY());
        if (entranceObject != null) {
            clearObjectMoveSelection();
            showEntranceContextMenu(e, entranceObject);
            return true;
        }

        RegionData regionData = findTravelRegionAtPoint(e.getPoint());
        if (regionData != null) {
            clearObjectMoveSelection();
            showTravelRegionContextMenu(e, regionData);
            return true;
        }
        return false;
    }

    private void showDoorContextMenu(MouseEvent e, PastedObject doorObject) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editDoorItem = new JMenuItem("Edit Door");
        editDoorItem.addActionListener(evt -> openDoorEditor(doorObject));
        menu.add(editDoorItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showWallGroupContextMenu(MouseEvent e, WallGroupData wallGroup) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editWallGroupItem = new JMenuItem("Edit Wallgroup");
        editWallGroupItem.addActionListener(evt -> {
            selectedWallGroup = wallGroup;
            movingWallGroupBasePolygon = wallGroup.getPolygon();
            Rectangle bounds = movingWallGroupBasePolygon.getBounds();
            movingRectangle = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            editWallGroup(wallGroup);
            repaint();
        });
        menu.add(editWallGroupItem);

        JMenuItem redrawPolygonItem = new JMenuItem("Redraw Polygon");
        redrawPolygonItem.addActionListener(evt -> {
            selectedWallGroup = wallGroup;
            movingWallGroupBasePolygon = wallGroup.getPolygon();
            Rectangle bounds = movingWallGroupBasePolygon.getBounds();
            movingRectangle = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            startWallGroupPlacement(wallGroup);
            repaint();
        });
        menu.add(redrawPolygonItem);

        JMenuItem removeWallGroupItem = new JMenuItem("Remove Wallgroup");
        removeWallGroupItem.addActionListener(evt -> {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Remove wallgroup '" + wallGroup.getDisplayName() + "'?",
                    "Remove Wallgroup",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                if (selectedWallGroup == wallGroup) {
                    clearObjectMoveSelection();
                }
                wallGroups.remove(wallGroup);
                recordHistoryState();
                repaint();
            }
        });
        menu.add(removeWallGroupItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showEntranceContextMenu(MouseEvent e, PastedObject entranceObject) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editEntranceItem = new JMenuItem("Edit Entrance");
        editEntranceItem.addActionListener(evt -> {
            editEntrance(entranceObject);
            repaint();
        });
        menu.add(editEntranceItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showTravelRegionContextMenu(MouseEvent e, RegionData regionData) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editRegionItem = new JMenuItem("Edit Region");
        editRegionItem.addActionListener(evt -> {
            editRegion(regionData);
            repaint();
        });
        menu.add(editRegionItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void openDoorEditor(PastedObject doorObject) {
        DoorEditContext doorContext = buildDoorEditContext(doorObject);
        if (doorContext == null || doorContext.primaryDoorObject == null) {
            return;
        }
        doorEditorOpenedDoorObject = doorContext.primaryDoorObject;
        if (doorEditorDialog == null || !doorEditorDialog.isDisplayable()) {
            doorEditorDialog = new DoorEditorDialog(this, new DoorEditorDialog.Listener() {
                @Override
                public void onToggleDoorPolygons(boolean selected) {
                    showDoorPolygonOverlays = selected;
                    repaint();
                }

                @Override
                public void onToggleImpededBlocks(boolean selected) {
                    showDoorImpededBlockOverlays = selected;
                    repaint();
                }

                @Override
                public void onDoorDataChanged(DoorData doorData) {
                    if (doorEditorOpenedDoorObject != null) {
                        DoorEditContext updatedContext = buildDoorEditContext(doorEditorOpenedDoorObject);
                        if (updatedContext != null) {
                            applyDoorData(new DoorEditContext(
                                updatedContext.primaryDoorObject,
                                updatedContext.openDoorObject,
                                updatedContext.closedDoorObject,
                                doorData != null ? doorData.copy() : new DoorData()
                            ));
                            recordHistoryState();
                            repaint();
                        }
                    }
                }

                @Override
                public void onEditFlags() {
                    if (doorEditorOpenedDoorObject != null) {
                        editDoorFlags(doorEditorOpenedDoorObject);
                    }
                }

                @Override
                public void onEditRegionLink() {
                    if (doorEditorOpenedDoorObject != null) {
                        editDoorRegionLink(doorEditorOpenedDoorObject);
                    }
                }

                @Override
                public void onSaveEntrance(String name, int orientation, java.awt.Point entrancePoint) {
                    PastedObject existing = findEntranceByName(name);
                    if (existing != null) {
                        existing.getEntranceData().setX(entrancePoint.x);
                        existing.getEntranceData().setY(entrancePoint.y);
                        existing.getEntranceData().setOrientation(orientation);
                        syncEntranceMarker(existing);
                    } else {
                        java.awt.image.BufferedImage markerImage = DirectionMarker.createEntranceMarkerImage(orientation);
                        PastedObject entranceObject = new PastedObject(
                            new java.awt.Point(entrancePoint.x - markerImage.getWidth() / 2, entrancePoint.y - markerImage.getHeight() / 2),
                            new ExportableImage(markerImage), PastedObjectType.ENTRANCE
                        );
                        EntranceData entranceData = new EntranceData(name, entrancePoint.x, entrancePoint.y);
                        entranceData.setOrientation(orientation);
                        entranceObject.setEntranceData(entranceData);
                        syncEntranceMarker(entranceObject);
                        pastedObjects.add(entranceObject);
                    }
                    recordHistoryState();
                    repaint();
                }

                @Override
                public void onEraseEntrance() {
                    if (doorEditorOpenedDoorObject == null) return;
                    DoorEditContext ctx = buildDoorEditContext(doorEditorOpenedDoorObject);
                    if (ctx == null) return;
                    RegionData linkedRegion = findRegionByName(trimToEmpty(ctx.sharedData.getRegionLinkName()));
                    if (linkedRegion != null) {
                        PastedObject entrance = findEntranceByName(trimToEmpty(linkedRegion.getPairedEntranceName()));
                        if (entrance != null) pastedObjects.remove(entrance);
                    }
                    recordHistoryState();
                    repaint();
                }

                @Override
                public void onSaveTravelRegion(String entranceName, java.awt.Polygon exitPolygon) {
                    if (doorEditorOpenedDoorObject == null) return;
                    DoorEditContext ctx = buildDoorEditContext(doorEditorOpenedDoorObject);
                    if (ctx == null) return;
                    RegionData linkedRegion = findRegionByName(trimToEmpty(ctx.sharedData.getRegionLinkName()));
                    if (linkedRegion != null) {
                        linkedRegion.setBounds(clonePolygon(exitPolygon));
                    } else {
                        RegionData regionData = new RegionData(buildPairedTravelRegionName(entranceName), 2, clonePolygon(exitPolygon));
                        regionData.setPairedEntranceName(entranceName);
                        regions.add(regionData);
                        linkDoorToRegion(doorEditorOpenedDoorObject, regionData);
                    }
                    recordHistoryState();
                    repaint();
                }

                @Override
                public void onEraseTravelRegion() {
                    if (doorEditorOpenedDoorObject == null) return;
                    DoorEditContext ctx = buildDoorEditContext(doorEditorOpenedDoorObject);
                    if (ctx == null) return;
                    RegionData linkedRegion = findRegionByName(trimToEmpty(ctx.sharedData.getRegionLinkName()));
                    if (linkedRegion != null) {
                        linkedRegion.setBounds(new Polygon());
                    }
                    recordHistoryState();
                    repaint();
                }
            });
            doorEditorDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    doorEditorDialog = null;
                    doorEditorOpenedDoorObject = null;
                }
            });
        }
        refreshDoorEditorDialog();
        doorEditorDialog.setLocationRelativeTo(this);
        doorEditorDialog.setVisible(true);
        doorEditorDialog.toFront();
        doorEditorDialog.requestFocus();
    }

    private void editDoorRegionLink(PastedObject doorObject) {
        DoorEditContext doorContext = buildDoorEditContext(doorObject);
        if (doorContext == null) {
            return;
        }
        String currentValue = trimToEmpty(doorContext.sharedData.getRegionLinkName());
        String value = JOptionPane.showInputDialog(this, "Enter linked region name:", currentValue);
        if (value == null) {
            return;
        }
        value = trimToEmpty(value);
        doorContext.sharedData.setRegionLinkName(value);
        if (value.isEmpty()) {
            doorContext.sharedData.setFlags(doorContext.sharedData.getFlags() & ~DoorExportSupport.LINKED_FLAG);
        } else {
            doorContext.sharedData.setFlags(doorContext.sharedData.getFlags() | DoorExportSupport.LINKED_FLAG);
        }
        applyDoorData(doorContext);
        recordHistoryState();
    }

    private void refreshDoorEditorDialog() {
        if (doorEditorDialog == null || !doorEditorDialog.isDisplayable() || doorEditorOpenedDoorObject == null) {
            return;
        }
        DoorEditContext doorContext = buildDoorEditContext(doorEditorOpenedDoorObject);
        if (doorContext == null) {
            return;
        }
        BufferedImage openedDoorImage = doorContext.openDoorObject != null && doorContext.openDoorObject.getImage() != null
            ? doorContext.openDoorObject.getImage().getImage()
            : null;
        Point openedDoorLocation = doorContext.openDoorObject != null
            ? doorContext.openDoorObject.getLocation()
            : (doorContext.primaryDoorObject != null ? doorContext.primaryDoorObject.getLocation() : new Point());
        doorEditorDialog.setDoorState(
            doorContext.sharedData,
            openedDoorImage,
            openedDoorLocation,
            showDoorPolygonOverlays,
            showDoorImpededBlockOverlays
        );
        doorEditorDialog.setSuggestedEntranceName(buildSuggestedDoorEntranceName(doorContext));
        String regionLink = trimToEmpty(doorContext.sharedData.getRegionLinkName());
        if (!regionLink.isEmpty()) {
            RegionData linkedRegion = findRegionByName(regionLink);
            if (linkedRegion != null) {
                PastedObject linkedEntrance = findEntranceByName(trimToEmpty(linkedRegion.getPairedEntranceName()));
                if (linkedEntrance != null) {
                    doorEditorDialog.setLastEntranceState(
                        trimToEmpty(linkedEntrance.getEntranceData().getName()),
                        new Point(linkedEntrance.getEntranceData().getX(), linkedEntrance.getEntranceData().getY()),
                        linkedRegion.getBounds(),
                        linkedEntrance.getEntranceData().getOrientation()
                    );
                }
            }
        }
    }

    private void editDoorFlags(PastedObject doorObject) {
        DoorEditContext doorContext = buildDoorEditContext(doorObject);
        if (doorContext == null) {
            return;
        }
        DoorFlagsDialog dialog = new DoorFlagsDialog(this, doorContext.sharedData.getFlags());
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        doorContext.sharedData.setFlags(dialog.getFlags());
        applyDoorData(doorContext);
        recordHistoryState();
    }

    private boolean handleDoorCanvasMousePressed(MouseEvent event) {
        if (doorPointPlacementSession != null) {
            if (SwingUtilities.isLeftMouseButton(event)) {
                applyDoorPointPlacement(new Point(event.getX(), event.getY()));
            } else if (SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
                cancelDoorEditingSessions(true);
            }
            return true;
        }
        return false;
    }

    private boolean handleDoorCanvasMouseDragged(MouseEvent event) {
        return doorPointPlacementSession != null;
    }

    private boolean handleDoorCanvasMouseReleased(MouseEvent event) {
        return doorPointPlacementSession != null;
    }

    private void applyDoorPointPlacement(Point point) {
        if (doorPointPlacementSession == null) {
            return;
        }
        DoorEditContext doorContext = buildDoorEditContext(doorPointPlacementSession.openedDoorObject);
        if (doorContext == null) {
            cancelDoorEditingSessions(true);
            return;
        }
        switch (doorPointPlacementSession.pointType) {
            case LAUNCH_POINT:
                doorContext.sharedData.setLaunchPoint(point);
                break;
            case OPEN_LOCATION_FRONT:
                doorContext.sharedData.setOpenLocationFront(point);
                break;
            case OPEN_LOCATION_BACK:
                doorContext.sharedData.setOpenLocationBack(point);
                break;
            default:
                throw new IllegalArgumentException();
        }
        applyDoorData(doorContext);
        doorPointPlacementSession = null;
        recordHistoryState();
        repaint();
    }

    private void cancelDoorEditingSessions(boolean repaintPanel) {
        doorPointPlacementSession = null;
        refreshDoorEditorDialog();
        if (repaintPanel) {
            repaint();
        }
    }

    private void clearObjectMoveSelection() {
        objectToMove = null;
        objectToMoveIdx = -1;
        selectedWallGroup = null;
        clearSelectedSearchMapCells();
        movingWallGroupBasePolygon = null;
        movingRectangle = null;
        clearCompositeMove();
    }

    private void beginCompositeMove(PastedObject anchorObject) {
        clearCompositeMove();
        if (anchorObject == null) {
            return;
        }
        movingCompositeGroupId = anchorObject.getCompositeGroupId();
        movingCompositeAnchorLocation = new Point(anchorObject.getLocation());
        if (movingCompositeGroupId == null) {
            return;
        }
        for (PastedObject pastedObject : pastedObjects) {
            if (movingCompositeGroupId.equals(pastedObject.getCompositeGroupId())) {
                movingCompositeBaseLocations.put(pastedObject, new Point(pastedObject.getLocation()));
            }
        }
        for (WallGroupData wallGroup : wallGroups) {
            if (movingCompositeGroupId.equals(wallGroup.getCompositeGroupId())) {
                movingCompositeBaseWallPolygons.put(wallGroup, wallGroup.getPolygon());
            }
        }
    }

    private void beginCompositeMove(WallGroupData anchorWallGroup) {
        clearCompositeMove();
        if (anchorWallGroup == null) {
            return;
        }
        movingCompositeGroupId = anchorWallGroup.getCompositeGroupId();
        movingCompositeAnchorLocation = anchorWallGroup.getPolygon().getBounds().getLocation();
        if (movingCompositeGroupId == null) {
            return;
        }
        for (PastedObject pastedObject : pastedObjects) {
            if (movingCompositeGroupId.equals(pastedObject.getCompositeGroupId())) {
                movingCompositeBaseLocations.put(pastedObject, new Point(pastedObject.getLocation()));
            }
        }
        for (WallGroupData wallGroup : wallGroups) {
            if (movingCompositeGroupId.equals(wallGroup.getCompositeGroupId())) {
                movingCompositeBaseWallPolygons.put(wallGroup, wallGroup.getPolygon());
            }
        }
    }

    private void clearCompositeMove() {
        movingCompositeGroupId = null;
        movingCompositeAnchorLocation = null;
        movingCompositeBaseLocations.clear();
        movingCompositeBaseWallPolygons.clear();
    }

    private void setPastedObjectLocation(PastedObject pastedObject, int x, int y) {
        if (pastedObject == null) {
            return;
        }
        if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE && pastedObject.getEntranceData() != null) {
            pastedObject.getEntranceData().setX(x + (pastedObject.getWidth() / 2));
            pastedObject.getEntranceData().setY(y + (pastedObject.getHeight() / 2));
            syncEntranceMarker(pastedObject);
        } else {
            if (pastedObject.getPastedObjectType().isDoor()) {
                translateDoorDataForObjectMove(pastedObject, x - pastedObject.getX(), y - pastedObject.getY());
            }
            pastedObject.setLocation(new Point(x, y));
        }
    }

    private String newCompositeGroupId() {
        return "composite-" + System.nanoTime();
    }

    private void setDrawClosedState(boolean drawClosed) {
        this.drawClosed = drawClosed;
        if (drawClosedDoorMenuItem != null && drawClosedDoorMenuItem.isSelected() != drawClosed) {
            drawClosedDoorMenuItem.setSelected(drawClosed);
        }
        if (drawClosedDoorToggleButton != null && drawClosedDoorToggleButton.isSelected() != drawClosed) {
            drawClosedDoorToggleButton.setSelected(drawClosed);
        }
        repaint();
    }

    private void syncExtractionClosedDoorUi() {
        boolean enabled = extractionGameAreaResref != null && !extractionGameAreaResref.trim().isEmpty();
        if (extractionClosedDoorMenuItem != null) {
            extractionClosedDoorMenuItem.setEnabled(enabled);
            if (extractionClosedDoorMenuItem.isSelected() != extractionGameAreaClosedDoors) {
                extractionClosedDoorMenuItem.setSelected(extractionGameAreaClosedDoors);
            }
        }
        if (extractionClosedDoorToggleButton != null) {
            extractionClosedDoorToggleButton.setEnabled(enabled);
            if (extractionClosedDoorToggleButton.isSelected() != extractionGameAreaClosedDoors) {
                extractionClosedDoorToggleButton.setSelected(extractionGameAreaClosedDoors);
            }
        }
    }

    private void clearExtractionGameAreaSource() {
        extractionGameAreaResref = null;
        extractionGameAreaClosedDoors = true;
        syncExtractionClosedDoorUi();
    }

    private void setExtractionGameAreaSource(String areaResref, boolean closedDoors) {
        extractionGameAreaResref = LocalIeIO.normalizeResref(areaResref);
        extractionGameAreaClosedDoors = closedDoors;
        syncExtractionClosedDoorUi();
    }

    private void reloadExtractionGameAreaWithClosedDoors(boolean closedDoors) {
        if (extractionGameAreaResref == null || extractionGameAreaResref.trim().isEmpty()) {
            syncExtractionClosedDoorUi();
            return;
        }
        String gameInstallPath = UserPreferences.getGameInstallPath();
        if (gameInstallPath == null || gameInstallPath.trim().isEmpty()) {
            syncExtractionClosedDoorUi();
            JOptionPane.showMessageDialog(this,
                "Configure the game install path in preferences first.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            BufferedImage loadedImage = GameAreaImageLoader.loadAreaImage(gameInstallPath, extractionGameAreaResref, closedDoors);
            applyExtractionBackgroundImage(loadedImage);
            setExtractionGameAreaSource(extractionGameAreaResref, closedDoors);
            setExtendedState(Frame.MAXIMIZED_BOTH);
            repaint();
        } catch (IOException ex) {
            syncExtractionClosedDoorUi();
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                ex.getMessage() != null ? ex.getMessage() : "Error loading game area background.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setNightModeState(boolean night) {
        this.night = night;
        if (nightMenuItem != null && nightMenuItem.isSelected() != night) {
            nightMenuItem.setSelected(night);
        }
        if (nightToggleButton != null && nightToggleButton.isSelected() != night) {
            nightToggleButton.setSelected(night);
        }
        repaint();
    }

    private void setShowSearchMapGridState(boolean showSearchMapGrid) {
        this.showSearchMapGrid = showSearchMapGrid;
        if (searchMapGridMenuItem != null && searchMapGridMenuItem.isSelected() != showSearchMapGrid) {
            searchMapGridMenuItem.setSelected(showSearchMapGrid);
        }
        if (searchMapGridToggleButton != null && searchMapGridToggleButton.isSelected() != showSearchMapGrid) {
            searchMapGridToggleButton.setSelected(showSearchMapGrid);
        }
        repaint();
    }

    private void setBuildCursorSelectMode() {
        painting = false;
        searchMapEditMode = SearchMapEditMode.NONE;
        searchMapSelectionSession = null;
        cancelDoorEditingSessions(false);
        clearSelectedSearchMapCells();
        syncCursorModeUi();
        repaint();
    }

    private void enableTextureBrushMode() {
        painting = ensureBrushTextureSelected();
        if (painting) {
            searchMapEditMode = SearchMapEditMode.NONE;
            searchMapSelectionSession = null;
            cancelDoorEditingSessions(false);
            clearSelectedSearchMapCells();
        }
        syncCursorModeUi();
        repaint();
    }

    private void enableSearchMapPainterMode() {
        SearchMapTileType chosenType = chooseSearchMapPaintType();
        if (chosenType == null) {
            syncCursorModeUi();
            return;
        }
        selectedSearchMapPaintType = chosenType;
        painting = false;
        searchMapEditMode = SearchMapEditMode.PAINTER;
        searchMapSelectionSession = null;
        cancelDoorEditingSessions(false);
        clearSelectedSearchMapCells();
        setShowSearchMapGridState(true);
        syncCursorModeUi();
        repaint();
    }

    private void enableSearchMapEraserMode() {
        painting = false;
        searchMapEditMode = SearchMapEditMode.ERASER;
        searchMapSelectionSession = null;
        cancelDoorEditingSessions(false);
        clearSelectedSearchMapCells();
        setShowSearchMapGridState(true);
        syncCursorModeUi();
        repaint();
    }

    private SearchMapTileType chooseSearchMapPaintType() {
        SearchMapTileType[] values = SearchMapTileType.values();
        JComboBox<SearchMapTileType> comboBox = new JComboBox<SearchMapTileType>(values);
        comboBox.setSelectedItem(selectedSearchMapPaintType);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SearchMapTileType) {
                    setText(humanizeSearchMapTileType((SearchMapTileType) value));
                }
                return component;
            }
        });
        int result = JOptionPane.showConfirmDialog(
            this,
            comboBox,
            "Select Search Map Cell Value",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        return result == JOptionPane.OK_OPTION ? (SearchMapTileType) comboBox.getSelectedItem() : null;
    }

    private String humanizeSearchMapTileType(SearchMapTileType type) {
        if (type == null) {
            return "Base";
        }
        return type.name().replace('_', ' ').toLowerCase();
    }

    private boolean isSearchMapEditMode() {
        return searchMapEditMode != SearchMapEditMode.NONE;
    }

    private void syncCursorModeUi() {
        if (cursorSelectMenuItem != null) {
            cursorSelectMenuItem.setSelected(!painting && searchMapEditMode == SearchMapEditMode.NONE);
        }
        if (brushModeMenuItem != null) {
            brushModeMenuItem.setSelected(painting);
        }
        if (searchMapPainterModeMenuItem != null) {
            searchMapPainterModeMenuItem.setSelected(searchMapEditMode == SearchMapEditMode.PAINTER);
            searchMapPainterModeMenuItem.setIcon(createSearchMapPaintToolIcon(selectedSearchMapPaintType));
            searchMapPainterModeMenuItem.setToolTipText("Paint search-map cells as " + humanizeSearchMapTileType(selectedSearchMapPaintType));
        }
        if (searchMapEraserModeMenuItem != null) {
            searchMapEraserModeMenuItem.setSelected(searchMapEditMode == SearchMapEditMode.ERASER);
        }
        if (cursorToolbarButton != null) {
            cursorToolbarButton.setSelected(!painting && searchMapEditMode == SearchMapEditMode.NONE);
        }
        if (brushToolbarButton != null) {
            brushToolbarButton.setSelected(painting);
        }
        if (searchMapPainterToolbarButton != null) {
            searchMapPainterToolbarButton.setSelected(searchMapEditMode == SearchMapEditMode.PAINTER);
            searchMapPainterToolbarButton.setIcon(createSearchMapPaintToolIcon(selectedSearchMapPaintType));
            searchMapPainterToolbarButton.setToolTipText("Paint search-map cells as " + humanizeSearchMapTileType(selectedSearchMapPaintType));
        }
        if (searchMapEraserToolbarButton != null) {
            searchMapEraserToolbarButton.setSelected(searchMapEditMode == SearchMapEditMode.ERASER);
        }
        if (extractionMapDragModeMenuItem != null && extractionMapDragModeMenuItem.isSelected() != isExtractionMapDragMode()) {
            extractionMapDragModeMenuItem.setSelected(isExtractionMapDragMode());
        }
        if (polygonModeMenuItem != null && polygonModeMenuItem.isSelected() != isExtractionPolygonMode()) {
            polygonModeMenuItem.setSelected(isExtractionPolygonMode());
        }
        if (rectangleModeMenuItem != null && rectangleModeMenuItem.isSelected() != isExtractionRectangleMode()) {
            rectangleModeMenuItem.setSelected(isExtractionRectangleMode());
        }
        if (extractionMapDragToolbarButton != null) {
            extractionMapDragToolbarButton.setSelected(isExtractionMapDragMode());
        }
        if (polygonToolbarButton != null) {
            polygonToolbarButton.setSelected(isExtractionPolygonMode());
        }
        if (rectangleToolbarButton != null) {
            rectangleToolbarButton.setSelected(isExtractionRectangleMode());
        }
    }

    private void updateTabSpecificUi() {
        syncCursorModeUi();
        boolean buildTabSelected = tabPane != null && tabPane.getSelectedComponent() == buildScrollPane;
        boolean extractionTabSelected = tabPane != null && tabPane.getSelectedComponent() == extractScrollPane;
        boolean areaEditingTabSelected = buildTabSelected || extractionTabSelected;

        setUiVisible(backgroundMenu, areaEditingTabSelected);
        setUiVisible(editMenu, buildTabSelected);
        setUiVisible(insertMenu, buildTabSelected);
        setUiVisible(cursorModeMenu, areaEditingTabSelected);
        setUiVisible(viewMenu, buildTabSelected || extractionTabSelected);
        setUiVisible(toolsMenu, buildTabSelected || extractionTabSelected);

        setUiVisible(fillMenuItem, buildTabSelected);
        setUiVisible(openBrushTextureMenuItem, buildTabSelected);
        setUiVisible(tileSeamlessMenuItem, extractionTabSelected);
        setUiVisible(saveDoorsMenuItem, buildTabSelected);
        setUiVisible(nanoBananaExtractionMenuItem, extractionTabSelected);
        setUiVisible(paint3dMenuItem, extractionTabSelected);
        setUiVisible(subtractBackgroundMenuItem, extractionTabSelected);
        setUiVisible(cursorSelectMenuItem, buildTabSelected);
        setUiVisible(brushModeMenuItem, buildTabSelected);
        setUiVisible(searchMapPainterModeMenuItem, buildTabSelected);
        setUiVisible(searchMapEraserModeMenuItem, buildTabSelected);
        setUiVisible(extractionMapDragModeMenuItem, extractionTabSelected);
        setUiVisible(polygonModeMenuItem, extractionTabSelected);
        setUiVisible(rectangleModeMenuItem, extractionTabSelected);
        setUiVisible(drawClosedDoorMenuItem, buildTabSelected);
        setUiVisible(nightMenuItem, buildTabSelected);
        setUiVisible(extractionClosedDoorMenuItem, extractionTabSelected);
        setUiVisible(searchMapGridMenuItem, buildTabSelected);

        setUiVisible(openBackgroundToolbarMenuButton, areaEditingTabSelected);
        for (Component component : buildOnlyToolbarButtons) {
            setUiVisible(component, buildTabSelected);
        }
        setUiVisible(exportDoorTilesToolbarButton, buildTabSelected);
        setUiVisible(tileSeamlessToolbarButton, extractionTabSelected);
        setUiVisible(nanoBananaExtractionToolbarButton, extractionTabSelected);
        setUiVisible(paint3dToolbarButton, extractionTabSelected);
        setUiVisible(subtractBackgroundToolbarButton, extractionTabSelected);
        setUiVisible(cursorToolbarButton, buildTabSelected);
        setUiVisible(brushToolbarButton, buildTabSelected);
        setUiVisible(searchMapPainterToolbarButton, buildTabSelected);
        setUiVisible(searchMapEraserToolbarButton, buildTabSelected);
        setUiVisible(extractionMapDragToolbarButton, extractionTabSelected);
        setUiVisible(polygonToolbarButton, extractionTabSelected);
        setUiVisible(rectangleToolbarButton, extractionTabSelected);
        setUiVisible(extractionClosedDoorToggleButton, extractionTabSelected);
        setUiVisible(searchMapGridToggleButton, buildTabSelected);
        syncExtractionClosedDoorUi();

        if (menubar != null) {
            menubar.revalidate();
            menubar.repaint();
        }
    }

    private void setUiVisible(java.awt.Component component, boolean visible) {
        if (component != null) {
            component.setVisible(visible);
        }
    }

    private static enum ExtractionCursorMode {
        MAP_DRAG,
        POLYGON,
        RECTANGLE
    }

    private void configureToolbarButton(AbstractButton button) {
        if (button == null) {
            return;
        }
        button.setFocusable(false);
        button.setText(null);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setPreferredSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
    }

    private ImageIcon loadOptionalIcon(String preferredResourcePath, String fallbackResourcePath) {
        java.net.URL preferred = getClass().getResource(preferredResourcePath);
        if (preferred != null) {
            return new ImageIcon(preferred);
        }
        if (fallbackResourcePath == null) {
            return null;
        }
        java.net.URL fallback = getClass().getResource(fallbackResourcePath);
        return fallback != null ? new ImageIcon(fallback) : null;
    }

    private ImageIcon createSearchMapGridIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0, 120, 0));
            graphics.fillRect(1, 1, 14, 14);
            graphics.setColor(new Color(220, 255, 220));
            graphics.drawRect(1, 1, 14, 14);
            graphics.drawLine(5, 1, 5, 15);
            graphics.drawLine(10, 1, 10, 15);
            graphics.drawLine(1, 5, 15, 5);
            graphics.drawLine(1, 10, 15, 10);
            graphics.setColor(new Color(220, 40, 40));
            graphics.fillRect(10, 10, 5, 5);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(image);
    }

    private ImageIcon createSearchMapPaintToolIcon(SearchMapTileType type) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(240, 240, 240));
            graphics.fillRect(1, 1, 14, 14);
            graphics.setColor(new Color(160, 160, 160));
            graphics.drawRect(1, 1, 14, 14);
            graphics.drawLine(5, 1, 5, 15);
            graphics.drawLine(10, 1, 10, 15);
            graphics.drawLine(1, 5, 15, 5);
            graphics.drawLine(1, 10, 15, 10);
            graphics.setColor(type != null ? type.getExportColor() : Color.WHITE);
            graphics.fillRect(9, 9, 5, 5);
            graphics.setColor(new Color(30, 90, 180));
            graphics.drawRect(9, 9, 5, 5);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(image);
    }

    private void configureCanvasScrollPane(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    }

    private void setExtractionCursorMode(ExtractionCursorMode extractionCursorMode) {
        this.extractionCursorMode = extractionCursorMode != null ? extractionCursorMode : ExtractionCursorMode.MAP_DRAG;
        if (isExtractionRectangleMode()) {
            polygon.reset();
            return;
        }
        tile.reset();
        extractRectangleSelectionInProgress = false;
    }

    private boolean isExtractionMapDragMode() {
        return extractionCursorMode == ExtractionCursorMode.MAP_DRAG;
    }

    private boolean isExtractionPolygonMode() {
        return extractionCursorMode == ExtractionCursorMode.POLYGON;
    }

    private boolean isExtractionRectangleMode() {
        return extractionCursorMode == ExtractionCursorMode.RECTANGLE;
    }

    private void loadBackgroundFromGameArea() {
        String gameInstallPath = UserPreferences.getGameInstallPath();
        if (gameInstallPath == null || gameInstallPath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Configure the game install path in preferences first.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean extractionTabSelected = tabPane.getSelectedComponent() == extractScrollPane;
        AreaSelectionDialog dialog = extractionTabSelected
            ? new AreaSelectionDialog(this, "Select Game Area Background", collectAvailableAreas(), "", "Closed door", true)
            : new AreaSelectionDialog(this, "Select Game Area Background", collectAvailableAreas(), "");
        dialog.setVisible(true);
        AreaReference selectedArea = dialog.getSelectedArea();
        if (selectedArea == null) {
            return;
        }

        try {
            if (extractionTabSelected) {
                boolean closedDoors = dialog.isOptionSelected();
                BufferedImage loadedImage = GameAreaImageLoader.loadAreaImage(gameInstallPath, selectedArea.getResref(), closedDoors);
                applyExtractionBackgroundImage(loadedImage);
                setExtractionGameAreaSource(selectedArea.getResref(), closedDoors);
                setExtendedState(Frame.MAXIMIZED_BOTH);
                repaint();
            } else {
                BufferedImage loadedImage = GameAreaImageLoader.loadAreaImage(gameInstallPath, selectedArea.getResref());
                applyBackgroundImageToSelectedTab(loadedImage);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                ex.getMessage() != null ? ex.getMessage() : "Error loading game area background.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyBackgroundImageToSelectedTab(BufferedImage chosenImageFile) {
        if (chosenImageFile == null) {
            return;
        }
        if (tabPane.getSelectedComponent() == buildScrollPane) {
            buildBackgroundImage = chosenImageFile;
            backgroundTile = null;
            backgroundWidth = chosenImageFile.getWidth();
            backgroundHeight = chosenImageFile.getHeight();
            buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
            applyWholeBackgroundSearchType(chosenImageFile);
            searchMapSelectionSession = null;
            recordHistoryState();
        }
        if (tabPane.getSelectedComponent() == extractScrollPane) {
            applyExtractionBackgroundImage(chosenImageFile);
            clearExtractionGameAreaSource();
        }
        setExtendedState(Frame.MAXIMIZED_BOTH);
        repaint();
    }

    private void applyExtractionBackgroundImage(BufferedImage chosenImageFile) {
        extractionBackgroundImage = chosenImageFile;
        polygon.reset();
        tile.reset();
        extractRectangleSelectionInProgress = false;
    }

    private void performUndo() {
        if (undoHistory.isEmpty()) {
            return;
        }
        try {
            if (currentHistoryState != null) {
                redoHistory.push(currentHistoryState);
            }
            byte[] targetState = undoHistory.pop();
            applyHistoryState(targetState);
            currentHistoryState = targetState;
            updateHistoryButtons();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Undo failed.", ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performRedo() {
        if (redoHistory.isEmpty()) {
            return;
        }
        try {
            if (currentHistoryState != null) {
                undoHistory.push(currentHistoryState);
            }
            byte[] targetState = redoHistory.pop();
            applyHistoryState(targetState);
            currentHistoryState = targetState;
            updateHistoryButtons();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Redo failed.", ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetHistoryToCurrentState() {
        try {
            undoHistory.clear();
            redoHistory.clear();
            currentHistoryState = serializeHistoryState();
            updateHistoryButtons();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void recordHistoryState() {
        try {
            byte[] newState = serializeHistoryState();
            if (currentHistoryState != null && Arrays.equals(currentHistoryState, newState)) {
                updateHistoryButtons();
                return;
            }
            if (currentHistoryState != null) {
                undoHistory.push(currentHistoryState);
                while (undoHistory.size() > MAX_HISTORY_ENTRIES) {
                    undoHistory.removeLast();
                }
            }
            currentHistoryState = newState;
            redoHistory.clear();
            updateHistoryButtons();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private byte[] serializeHistoryState() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try {
            ExportableArea exportableArea = new ExportableArea(
                new ExportableImage(buildBackgroundImage),
                pastedObjects,
                regions,
                containers,
                wallGroups,
                areaAttributes,
                searchMapData
            );
            HistoryState historyState = new HistoryState(exportableArea, drawClosed, night);
            historyState.writeExternal(objectOutputStream);
            objectOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } finally {
            objectOutputStream.close();
        }
    }

    private void applyHistoryState(byte[] stateBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(stateBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        try {
            HistoryState historyState = new HistoryState();
            historyState.readExternal(objectInputStream);
            ExportableArea exportableArea = historyState.getArea();
            buildBackgroundImage = exportableArea.getBackgroundImage().getImage();
            backgroundWidth = buildBackgroundImage.getWidth();
            backgroundHeight = buildBackgroundImage.getHeight();
            buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
            pastedObjects = exportableArea.getPastedObjects();
            regions = exportableArea.getRegions();
            containers = exportableArea.getContainers();
            wallGroups = exportableArea.getWallGroups();
            areaAttributes = exportableArea.getAreaAttributes();
            searchMapData = exportableArea.getSearchMapData() != null
                ? exportableArea.getSearchMapData()
                : new SearchMapData(backgroundWidth, backgroundHeight);
            searchMapData.resizeForPixels(backgroundWidth, backgroundHeight);
            searchMapSelectionSession = null;
            wallGroupPlacementSession = null;
            cancelDoorEditingSessions(false);
            refreshEntranceMarkers();
            clearObjectMoveSelection();
            setDrawClosedState(historyState.isDrawClosed());
            setNightModeState(historyState.isNight());
            repaint();
        } finally {
            objectInputStream.close();
        }
    }

    private void updateHistoryButtons() {
        if (undoToolbarButton != null) {
            undoToolbarButton.setEnabled(!undoHistory.isEmpty());
        }
        if (redoToolbarButton != null) {
            redoToolbarButton.setEnabled(!redoHistory.isEmpty());
        }
        if (editMenu != null) {
            for (int i = 0; i < editMenu.getItemCount(); i++) {
                JMenuItem item = editMenu.getItem(i);
                if (item == null || item.getAction() == null) {
                    continue;
                }
                Object action = item.getAction();
                if (action == undoToolbarButton.getAction()) {
                    item.setEnabled(!undoHistory.isEmpty());
                } else if (action == redoToolbarButton.getAction()) {
                    item.setEnabled(!redoHistory.isEmpty());
                }
            }
        }
    }

    private Dimension scaleDimension(int width, int height, double zoom) {
        int scaledWidth = Math.max(1, (int) Math.round(width * zoom));
        int scaledHeight = Math.max(1, (int) Math.round(height * zoom));
        return new Dimension(scaledWidth, scaledHeight);
    }

    private Rectangle clampToImageBounds(Rectangle bounds, BufferedImage image) {
        if (bounds == null || image == null) {
            return new Rectangle();
        }
        int left = Math.max(0, bounds.x);
        int top = Math.max(0, bounds.y);
        int right = Math.min(image.getWidth(), bounds.x + bounds.width);
        int bottom = Math.min(image.getHeight(), bounds.y + bounds.height);
        int width = Math.max(0, right - left);
        int height = Math.max(0, bottom - top);
        return new Rectangle(left, top, width, height);
    }

    private Point toAreaPoint(MouseEvent event, double zoom) {
        int x = (int) Math.floor(event.getX() / zoom);
        int y = (int) Math.floor(event.getY() / zoom);
        return new Point(Math.max(0, x), Math.max(0, y));
    }

    private MouseEvent scaleMouseEvent(MouseEvent event, JPanel panel, double zoom) {
        Point areaPoint = toAreaPoint(event, zoom);
        return new MouseEvent(
            panel,
            event.getID(),
            event.getWhen(),
            event.getModifiersEx(),
            areaPoint.x,
            areaPoint.y,
            event.getXOnScreen(),
            event.getYOnScreen(),
            event.getClickCount(),
            event.isPopupTrigger(),
            event.getButton()
        );
    }

    private double clampZoom(double zoom) {
        return Math.max(0.25, Math.min(zoom, 4.0));
    }

    private void applyZoom(JScrollPane scrollPane, JPanel panel, double oldZoom, double newZoom) {
        if (scrollPane == null || panel == null) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        Point viewPosition = viewport.getViewPosition();
        int anchorX = viewport.getWidth() / 2;
        int anchorY = viewport.getHeight() / 2;
        double contentX = (viewPosition.x + anchorX) / oldZoom;
        double contentY = (viewPosition.y + anchorY) / oldZoom;
        panel.revalidate();
        panel.repaint();
        int newViewX = (int) Math.round(contentX * newZoom - anchorX);
        int newViewY = (int) Math.round(contentY * newZoom - anchorY);
        Dimension preferredSize = panel.getPreferredSize();
        int maxX = Math.max(0, preferredSize.width - viewport.getWidth());
        int maxY = Math.max(0, preferredSize.height - viewport.getHeight());
        viewport.setViewPosition(new Point(
            Math.max(0, Math.min(newViewX, maxX)),
            Math.max(0, Math.min(newViewY, maxY))
        ));
    }

    private PastedObject findEntranceAtPoint(int x, int y) {
        for (int i = pastedObjects.size() - 1; i >= 0; i--) {
            PastedObject pastedObject = pastedObjects.get(i);
            if (pastedObject.getPastedObjectType() != PastedObjectType.ENTRANCE
                    || pastedObject.getEntranceData() == null
                    || !pastedObject.isVisible(drawClosed, night)) {
                continue;
            }
            Rectangle rect = new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
            rect = getPastedObjectBounds(pastedObject);
            if (rect.contains(x, y) && isClickablePastedObjectHit(pastedObject, x - rect.x, y - rect.y)) {
                return pastedObject;
            }
        }
        return null;
    }

    private PastedObject findDoorAtPoint(int x, int y) {
        for (int i = pastedObjects.size() - 1; i >= 0; i--) {
            PastedObject pastedObject = pastedObjects.get(i);
            if (pastedObject == null || !pastedObject.getPastedObjectType().isOpenDoor()) {
                continue;
            }
            Rectangle rect = getPastedObjectBounds(pastedObject);
            if (rect.contains(x, y) && isClickablePastedObjectHit(pastedObject, x - rect.x, y - rect.y)) {
                return pastedObject;
            }
        }
        return null;
    }

    private PastedObject findEntranceByName(String entranceName) {
        String normalizedName = trimToEmpty(entranceName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject == null || pastedObject.getPastedObjectType() != PastedObjectType.ENTRANCE || pastedObject.getEntranceData() == null) {
                continue;
            }
            if (normalizedName.equalsIgnoreCase(trimToEmpty(pastedObject.getEntranceData().getName()))) {
                return pastedObject;
            }
        }
        return null;
    }

    private PastedObject findPastedObjectAtPoint(int x, int y) {
        for (int i = pastedObjects.size() - 1; i >= 0; i--) {
            PastedObject pastedObject = pastedObjects.get(i);
            if (!pastedObject.isVisible(drawClosed, night)) {
                continue;
            }
            Rectangle rect = getPastedObjectBounds(pastedObject);
            if (rect.contains(x, y) && isClickablePastedObjectHit(pastedObject, x - rect.x, y - rect.y)) {
                return pastedObject;
            }
        }
        return null;
    }

    private boolean isClickablePastedObjectHit(PastedObject pastedObject, int localX, int localY) {
        if (pastedObject == null) {
            return false;
        }
        if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE) {
            return localX >= 0 && localY >= 0
                && localX < pastedObject.getWidth()
                && localY < pastedObject.getHeight();
        }
        return pastedObject.isOpaque(localX, localY);
    }

    private Rectangle getPastedObjectBounds(PastedObject pastedObject) {
        if (pastedObject == null) {
            return new Rectangle();
        }
        if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE) {
            return new Rectangle(
                pastedObject.getX(),
                pastedObject.getY(),
                pastedObject.getWidth(),
                pastedObject.getHeight()
            );
        }
        return new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
    }

    private RegionData findTravelRegionAtPoint(Point point) {
        if (point == null) {
            return null;
        }
        for (int i = regions.size() - 1; i >= 0; i--) {
            RegionData regionData = regions.get(i);
            Polygon bounds = regionData.getBounds();
            if (regionData.getType() == 2 && bounds != null && bounds.npoints >= 3 && bounds.contains(point)) {
                return regionData;
            }
        }
        return null;
    }

    private RegionData findRegionByName(String regionName) {
        String normalizedName = trimToEmpty(regionName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        for (RegionData regionData : regions) {
            if (normalizedName.equalsIgnoreCase(trimToEmpty(regionData.getName()))) {
                return regionData;
            }
        }
        return null;
    }

    private DoorEditContext buildDoorEditContext(PastedObject doorObject) {
        if (doorObject == null || !doorObject.getPastedObjectType().isDoor()) {
            return null;
        }

        PastedObject openDoorObject = doorObject.getPastedObjectType().isOpenDoor() ? doorObject : findDoorPair(doorObject, true);
        PastedObject closedDoorObject = doorObject.getPastedObjectType().isClosedDoor() ? doorObject : findDoorPair(doorObject, false);
        DoorData sharedData = doorObject.getDoorData();
        if ((sharedData == null || isDoorDataEmpty(sharedData)) && openDoorObject != null && openDoorObject != doorObject) {
            sharedData = openDoorObject.getDoorData();
        }
        if ((sharedData == null || isDoorDataEmpty(sharedData)) && closedDoorObject != null && closedDoorObject != doorObject) {
            sharedData = closedDoorObject.getDoorData();
        }
        if (sharedData == null) {
            sharedData = new DoorData();
        } else {
            sharedData = sharedData.copy();
        }

        Rectangle openFallbackBounds = objectBounds(openDoorObject != null ? openDoorObject : doorObject);
        Rectangle closedFallbackBounds = objectBounds(closedDoorObject != null ? closedDoorObject : doorObject);
        Rectangle anchorBounds = openFallbackBounds.union(closedFallbackBounds);
        Point anchorPoint = new Point(anchorBounds.x + (anchorBounds.width / 2), anchorBounds.y + (anchorBounds.height / 2));
        if (sharedData.getCursorIndex() <= 0) {
            sharedData.setCursorIndex(DoorExportSupport.DEFAULT_CURSOR_INDEX);
        }
        if (sharedData.getLaunchPoint().x == 0 && sharedData.getLaunchPoint().y == 0) {
            sharedData.setLaunchPoint(new Point(anchorPoint.x, anchorBounds.y + anchorBounds.height / 4));
        }
        if (sharedData.getOpenLocationFront().x == 0 && sharedData.getOpenLocationFront().y == 0) {
            sharedData.setOpenLocationFront(new Point(anchorPoint.x, anchorBounds.y + anchorBounds.height * 3 / 4));
        }
        if (sharedData.getOpenLocationBack().x == 0 && sharedData.getOpenLocationBack().y == 0) {
            sharedData.setOpenLocationBack(new Point(anchorPoint.x, anchorBounds.y + anchorBounds.height / 2));
        }
        return new DoorEditContext(doorObject, openDoorObject, closedDoorObject, sharedData);
    }

    private void applyDoorData(DoorEditContext doorContext) {
        if (doorContext == null) {
            return;
        }
        DoorData sharedCopy = doorContext.sharedData.copy();
        if (doorContext.openDoorObject != null) {
            doorContext.openDoorObject.setDoorData(sharedCopy.copy());
        }
        if (doorContext.closedDoorObject != null) {
            doorContext.closedDoorObject.setDoorData(sharedCopy.copy());
        }
        if (doorContext.openDoorObject == null && doorContext.closedDoorObject == null && doorContext.primaryDoorObject != null) {
            doorContext.primaryDoorObject.setDoorData(sharedCopy.copy());
        }
        refreshDoorEditorDialog();
    }

    private void translateDoorDataForObjectMove(PastedObject doorObject, int dx, int dy) {
        if (doorObject == null || (dx == 0 && dy == 0) || !doorObject.getPastedObjectType().isDoor()) {
            return;
        }
        PastedObject openDoorObject = doorObject.getPastedObjectType().isOpenDoor() ? doorObject : findDoorPair(doorObject, true);
        if (openDoorObject != null && openDoorObject != doorObject) {
            return;
        }
        DoorEditContext doorContext = buildDoorEditContext(doorObject);
        if (doorContext == null) {
            return;
        }
        String regionLinkName = trimToEmpty(doorContext.sharedData.getRegionLinkName());
        translateDoorData(doorContext.sharedData, dx, dy);
        applyDoorData(doorContext);
        if (!regionLinkName.isEmpty()) {
            RegionData linkedRegion = findRegionByName(regionLinkName);
            if (linkedRegion != null) {
                if (linkedRegion.getBounds() != null && linkedRegion.getBounds().npoints > 0) {
                    linkedRegion.setBounds(translatePolygon(linkedRegion.getBounds(), dx, dy));
                }
                PastedObject linkedEntrance = findEntranceByName(trimToEmpty(linkedRegion.getPairedEntranceName()));
                if (linkedEntrance != null) {
                    setPastedObjectLocation(linkedEntrance, linkedEntrance.getX() + dx, linkedEntrance.getY() + dy);
                }
            }
        }
    }

    private void translateDoorData(DoorData doorData, int dx, int dy) {
        if (doorData == null || (dx == 0 && dy == 0)) {
            return;
        }
        Polygon newOpenPolygon = translatePolygon(doorData.getOpenPolygon(), dx, dy);
        Polygon newClosedPolygon = translatePolygon(doorData.getClosedPolygon(), dx, dy);
        doorData.setOpenPolygon(newOpenPolygon);
        doorData.setClosedPolygon(newClosedPolygon);
        if (newOpenPolygon.npoints >= 3) {
            doorData.setOpenImpededCells(DoorEditorDialog.computeImpededCellsFromPolygon(newOpenPolygon));
        } else {
            doorData.setOpenImpededCells(translateSearchMapCells(doorData.getOpenImpededCells(), dx, dy));
        }
        if (newClosedPolygon.npoints >= 3) {
            doorData.setClosedImpededCells(DoorEditorDialog.computeImpededCellsFromPolygon(newClosedPolygon));
        } else {
            doorData.setClosedImpededCells(translateSearchMapCells(doorData.getClosedImpededCells(), dx, dy));
        }
        doorData.setLaunchPoint(translatePoint(doorData.getLaunchPoint(), dx, dy));
        doorData.setOpenLocationFront(translatePoint(doorData.getOpenLocationFront(), dx, dy));
        doorData.setOpenLocationBack(translatePoint(doorData.getOpenLocationBack(), dx, dy));
    }

    private PastedObject findDoorPair(PastedObject doorObject, boolean openDoor) {
        if (doorObject == null || trimToEmpty(doorObject.getCompositeGroupId()).isEmpty()) {
            return null;
        }
        for (PastedObject candidate : pastedObjects) {
            if (candidate == null || candidate == doorObject || !doorObject.getCompositeGroupId().equals(candidate.getCompositeGroupId())) {
                continue;
            }
            if (openDoor && candidate.getPastedObjectType().isOpenDoor()) {
                return candidate;
            }
            if (!openDoor && candidate.getPastedObjectType().isClosedDoor()) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isDoorDataEmpty(DoorData doorData) {
        return doorData == null
            || (doorData.getOpenPolygon().npoints == 0
                && doorData.getClosedPolygon().npoints == 0
                && doorData.getOpenImpededCells().isEmpty()
                && doorData.getClosedImpededCells().isEmpty()
                && doorData.getFlags() == 0
                && trimToEmpty(doorData.getRegionLinkName()).isEmpty());
    }

    private Map<String, EntranceData> collectEntrancesByName() {
        Map<String, EntranceData> entrancesByName = new LinkedHashMap<String, EntranceData>();
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject == null || pastedObject.getPastedObjectType() != PastedObjectType.ENTRANCE || pastedObject.getEntranceData() == null) {
                continue;
            }
            String entranceName = trimToEmpty(pastedObject.getEntranceData().getName());
            if (!entranceName.isEmpty()) {
                entrancesByName.put(entranceName.toUpperCase(), pastedObject.getEntranceData());
            }
        }
        return entrancesByName;
    }

    private String buildSuggestedDoorEntranceName(DoorEditContext doorContext) {
        return "Door" + String.format("%02d", countDoorObjects()) + "_ENTRANCE";
    }

    private int countDoorObjects() {
        int count = 0;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject != null && pastedObject.getPastedObjectType().isDoor()) {
                count++;
            }
        }
        return count;
    }

    private void offerToLinkDoorToRegion(RegionData regionData) {
        PastedObject nearbyDoor = findBestDoorForRegion(regionData != null ? regionData.getBounds() : null);
        if (nearbyDoor == null || regionData == null) {
            return;
        }
        String regionName = trimToEmpty(regionData.getName());
        int answer = JOptionPane.showConfirmDialog(
            this,
            "Link travel region '" + (regionName.isEmpty() ? "<unnamed>" : regionName) + "' to the nearby door?",
            "Link Door",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION) {
            linkDoorToRegion(nearbyDoor, regionData);
        }
    }

    private void linkDoorToRegion(PastedObject doorObject, RegionData regionData) {
        DoorData rawData = doorObject.getDoorData();
        boolean lpAlreadySet = rawData != null && (rawData.getLaunchPoint().x != 0 || rawData.getLaunchPoint().y != 0);
        boolean ofAlreadySet = rawData != null && (rawData.getOpenLocationFront().x != 0 || rawData.getOpenLocationFront().y != 0);
        boolean obAlreadySet = rawData != null && (rawData.getOpenLocationBack().x != 0 || rawData.getOpenLocationBack().y != 0);
        DoorEditContext doorContext = buildDoorEditContext(doorObject);
        if (doorContext == null || regionData == null) {
            return;
        }
        Rectangle openBounds = resolveDoorBounds(
            doorContext.openDoorObject,
            doorContext.sharedData.getOpenPolygon(),
            doorContext.closedDoorObject != null ? objectBounds(doorContext.closedDoorObject) : null
        );
        Rectangle closedBounds = resolveDoorBounds(
            doorContext.closedDoorObject,
            doorContext.sharedData.getClosedPolygon(),
            doorContext.openDoorObject != null ? objectBounds(doorContext.openDoorObject) : null
        );
        DoorExportSupport.DoorAutoLink autoLink = DoorExportSupport.autoLink(
            openBounds,
            closedBounds,
            Arrays.asList(regionData),
            collectEntrancesByName()
        );
        doorContext.sharedData.setRegionLinkName(trimToEmpty(regionData.getName()));
        doorContext.sharedData.setFlags(doorContext.sharedData.getFlags() | DoorExportSupport.LINKED_FLAG);
        if (!trimToEmpty(autoLink.getRegionName()).isEmpty()) {
            if (!lpAlreadySet) doorContext.sharedData.setLaunchPoint(autoLink.getLaunchPoint());
            if (!ofAlreadySet) doorContext.sharedData.setOpenLocationFront(autoLink.getOpenLocationFront());
            if (!obAlreadySet) doorContext.sharedData.setOpenLocationBack(autoLink.getOpenLocationBack());
        } else {
            Rectangle regionBounds = regionData.getBounds() != null ? regionData.getBounds().getBounds() : new Rectangle();
            Point center = new Point(regionBounds.x + (regionBounds.width / 2), regionBounds.y + (regionBounds.height / 2));
            if (!lpAlreadySet) doorContext.sharedData.setLaunchPoint(center);
            if (!ofAlreadySet) doorContext.sharedData.setOpenLocationFront(center);
            if (!obAlreadySet) doorContext.sharedData.setOpenLocationBack(center);
        }
        applyDoorData(doorContext);
    }

    private PastedObject findBestDoorForRegion(Polygon regionPolygon) {
        if (regionPolygon == null || regionPolygon.npoints < 3) {
            return null;
        }
        Rectangle regionBounds = regionPolygon.getBounds();
        PastedObject bestDoor = null;
        int bestArea = Integer.MAX_VALUE;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject == null || !pastedObject.getPastedObjectType().isDoor()) {
                continue;
            }
            DoorEditContext doorContext = buildDoorEditContext(pastedObject);
            if (doorContext == null) {
                continue;
            }
            Rectangle openBounds = resolveDoorBounds(
                doorContext.openDoorObject,
                doorContext.sharedData.getOpenPolygon(),
                doorContext.closedDoorObject != null ? objectBounds(doorContext.closedDoorObject) : null
            );
            Rectangle closedBounds = resolveDoorBounds(
                doorContext.closedDoorObject,
                doorContext.sharedData.getClosedPolygon(),
                doorContext.openDoorObject != null ? objectBounds(doorContext.openDoorObject) : null
            );
            Rectangle unionBounds = openBounds.union(closedBounds);
            if (!unionBounds.contains(regionBounds)) {
                continue;
            }
            boolean allVerticesInside = true;
            for (int i = 0; i < regionPolygon.npoints; i++) {
                if (!unionBounds.contains(regionPolygon.xpoints[i], regionPolygon.ypoints[i])) {
                    allVerticesInside = false;
                    break;
                }
            }
            if (!allVerticesInside) {
                continue;
            }
            int area = Math.max(1, unionBounds.width) * Math.max(1, unionBounds.height);
            if (bestDoor == null || area < bestArea) {
                bestDoor = pastedObject;
                bestArea = area;
            }
        }
        return bestDoor;
    }

    private WallGroupData findWallGroupAtPoint(Point point) {
        if (point == null) {
            return null;
        }
        for (int i = wallGroups.size() - 1; i >= 0; i--) {
            WallGroupData wallGroup = wallGroups.get(i);
            Polygon polygonBounds = wallGroup.getPolygon();
            if (polygonBounds != null && polygonBounds.npoints >= 3 && polygonBounds.contains(point)) {
                return wallGroup;
            }
        }
        return null;
    }

    private boolean isImpededSearchMapCellAtPoint(Point point) {
        if (point == null || searchMapData == null) {
            return false;
        }
        int tileX = point.x / SearchMapData.CELL_WIDTH;
        int tileY = point.y / SearchMapData.CELL_HEIGHT;
        return searchMapData.isImpeded(tileX, tileY);
    }

    private boolean selectSearchMapRegionAtPoint(Point point) {
        if (point == null || searchMapData == null) {
            return false;
        }
        int tileX = point.x / SearchMapData.CELL_WIDTH;
        int tileY = point.y / SearchMapData.CELL_HEIGHT;
        List<Point> region = searchMapData.findConnectedImpededRegion(tileX, tileY);
        if (region.isEmpty()) {
            return false;
        }
        setSelectedSearchMapCells(region);
        return true;
    }

    private void setSelectedSearchMapCells(List<Point> cells) {
        selectedSearchMapCells = new LinkedHashSet<Point>();
        if (cells != null) {
            for (Point point : cells) {
                if (point != null) {
                    selectedSearchMapCells.add(new Point(point));
                }
            }
        }
    }

    private void clearSelectedSearchMapCells() {
        selectedSearchMapCells = new LinkedHashSet<Point>();
    }

    private boolean hasSelectedSearchMapCells() {
        return selectedSearchMapCells != null && !selectedSearchMapCells.isEmpty();
    }

    private boolean isSelectedSearchMapCell(int tileX, int tileY) {
        return selectedSearchMapCells != null && selectedSearchMapCells.contains(new Point(tileX, tileY));
    }

    private String buildPairedTravelRegionName(String entranceName) {
        entranceName = trimToEmpty(entranceName);
        String baseName = entranceName.isEmpty() ? "TravelRegion" : entranceName + "_EXIT";
        return baseName.length() > 32 ? baseName.substring(0, 32) : baseName;
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

    /**
     * Collects a list of available area names from saved project files or a predefined list.
     */
    private List<AreaReference> collectAvailableAreas() {
        return loadAreaCatalog("/areas/eet-areas.csv");
    }

    private List<String> collectReservedOwnedAreaResrefs() {
        List<String> reserved = new ArrayList<>();
        for (String knownArea : UserPreferences.getKnownOwnedAreas()) {
            if (knownArea != null && !knownArea.trim().isEmpty()) {
                reserved.add(knownArea.trim());
            }
        }
        for (PastedObject obj : pastedObjects) {
            if (obj.getPastedObjectType() == PastedObjectType.ENTRANCE && obj.getEntranceData() != null
                    && obj.getEntranceData().getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
                String destinationArea = obj.getEntranceData().getDestinationArea();
                if (destinationArea != null && !destinationArea.trim().isEmpty()) {
                    reserved.add(destinationArea.trim());
                }
            }
        }
        for (RegionData regionData : regions) {
            if (regionData.getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
                String destinationArea = regionData.getDestinationArea();
                if (destinationArea != null && !destinationArea.trim().isEmpty()) {
                    reserved.add(destinationArea.trim());
                }
            }
        }
        return reserved;
    }

    private List<AreaReference> collectKnownOwnedAreaReferences() {
        List<AreaReference> references = new ArrayList<>();
        for (String areaResref : UserPreferences.getKnownOwnedAreas()) {
            if (areaResref != null && !areaResref.trim().isEmpty()) {
                references.add(new AreaReference(areaResref, "Known owned area"));
            }
        }
        return references;
    }

    private void registerKnownOwnedAreas(String currentAreaResref) {
        UserPreferences.addKnownOwnedArea(currentAreaResref);
    }

    private String validateOwnedAreaDestinations() {
        Set<String> knownOwnedAreas = new LinkedHashSet<String>();
        for (String areaResref : UserPreferences.getKnownOwnedAreas()) {
            if (areaResref != null && !areaResref.trim().isEmpty()) {
                knownOwnedAreas.add(areaResref.trim());
            }
        }

        List<String> invalidTargets = new ArrayList<String>();
        for (PastedObject obj : pastedObjects) {
            if (obj.getPastedObjectType() != PastedObjectType.ENTRANCE || obj.getEntranceData() == null) {
                continue;
            }
            EntranceData entranceData = obj.getEntranceData();
            if (entranceData.getDestinationAreaType() == DestinationAreaType.OWNED_MOD_AREA) {
                String destinationArea = trimToEmpty(entranceData.getDestinationArea());
                if (!destinationArea.isEmpty() && !knownOwnedAreas.contains(destinationArea)) {
                    invalidTargets.add("Entrance '" + trimToEmpty(entranceData.getName()) + "' -> " + destinationArea);
                }
            }
        }

        for (RegionData regionData : regions) {
            if (regionData.getType() != 2 || regionData.getDestinationAreaType() != DestinationAreaType.OWNED_MOD_AREA) {
                continue;
            }
            String destinationArea = trimToEmpty(regionData.getDestinationArea());
            if (destinationArea.isEmpty()) {
                continue;
            }
            if (!knownOwnedAreas.contains(destinationArea) && detectEdgeDirection(regionData.getBounds()) == null) {
                invalidTargets.add("Travel region '" + trimToEmpty(regionData.getName()) + "' -> " + destinationArea);
            }
        }

        if (invalidTargets.isEmpty()) {
            return null;
        }

        StringBuilder message = new StringBuilder();
        message.append("In-mod transitions must target an existing owned area.\n");
        message.append("The only exception is a single-edge NORTH/SOUTH/EAST/WEST travel region.\n\n");
        message.append("Invalid destinations:\n");
        for (String invalidTarget : invalidTargets) {
            message.append(" - ").append(invalidTarget).append('\n');
        }
        return message.toString();
    }

    private List<AreaReference> loadAreaCatalog(String resourcePath) {
        List<AreaReference> areas = new ArrayList<>();
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                return areas;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (firstLine) {
                        firstLine = false;
                    }
                    String[] parts = line.split(",", 2);
                    if (parts.length != 2) {
                        continue;
                    }
                    String resref = parseCsvValue(parts[0].trim());
                    String description = parseCsvValue(parts[1].trim());
                    if ("resref".equalsIgnoreCase(resref) && "description".equalsIgnoreCase(description)) {
                        continue;
                    }
                    if (!resref.isEmpty() && !description.isEmpty()) {
                        areas.add(new AreaReference(resref, description));
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return areas;
    }

    private List<PrefixReservation> loadPrefixReservations(String resourcePath) {
        List<PrefixReservation> reservations = new ArrayList<>();
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                return reservations;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    if (parts.length < 5) {
                        continue;
                    }
                    reservations.add(new PrefixReservation(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        parts[4].trim()
                    ));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return reservations;
    }

    private String parseCsvValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }

    private Polygon clonePolygon(Polygon source) {
        if (source == null || source.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[source.npoints];
        int[] ypoints = new int[source.npoints];
        System.arraycopy(source.xpoints, 0, xpoints, 0, source.npoints);
        System.arraycopy(source.ypoints, 0, ypoints, 0, source.npoints);
        return new Polygon(xpoints, ypoints, source.npoints);
    }

    private Polygon translatePolygon(Polygon source, int dx, int dy) {
        Polygon translated = clonePolygon(source);
        translated.translate(dx, dy);
        return translated;
    }

    private Point translatePoint(Point source, int dx, int dy) {
        Point point = source != null ? new Point(source) : new Point();
        point.translate(dx, dy);
        return point;
    }

    private List<Point> translateSearchMapCells(List<Point> cells, int dx, int dy) {
        List<Point> translated = new ArrayList<Point>();
        if (cells == null) {
            return translated;
        }
        int cellDx = (int) Math.round(dx / (double) SearchMapData.CELL_WIDTH);
        int cellDy = (int) Math.round(dy / (double) SearchMapData.CELL_HEIGHT);
        for (Point cell : cells) {
            if (cell == null) {
                continue;
            }
            translated.add(new Point(cell.x + cellDx, cell.y + cellDy));
        }
        return translated;
    }

    private static final class HistoryState {
        private ExportableArea area;
        private boolean drawClosed;
        private boolean night;

        private HistoryState() {
        }

        private HistoryState(ExportableArea area, boolean drawClosed, boolean night) {
            this.area = area;
            this.drawClosed = drawClosed;
            this.night = night;
        }

        private void writeExternal(ObjectOutputStream out) throws IOException {
            area.writeExternal(out);
            out.writeBoolean(drawClosed);
            out.writeBoolean(night);
        }

        private void readExternal(ObjectInputStream in) throws IOException, ClassNotFoundException {
            area = new ExportableArea();
            area.readExternal(in);
            drawClosed = in.readBoolean();
            night = in.readBoolean();
        }

        private ExportableArea getArea() {
            return area;
        }

        private boolean isDrawClosed() {
            return drawClosed;
        }

        private boolean isNight() {
            return night;
        }
    }

    private static final class TransitionPlacementSession {
        private final String entranceName;
        private final PastedObject linkedDoorObject;
        private int entranceX;
        private int entranceY;
        private int orientation;
        private boolean hasPoint;
        private Polygon localPolygon;

        private TransitionPlacementSession(String entranceName, PastedObject linkedDoorObject) {
            this.entranceName = entranceName != null ? entranceName.trim() : "";
            this.linkedDoorObject = linkedDoorObject;
            this.orientation = 0;
            this.hasPoint = false;
            this.localPolygon = new Polygon();
        }
    }

    private static final class WallGroupPlacementSession {
        private final WallGroupData targetWallGroup;
        private Polygon polygon;

        private WallGroupPlacementSession(WallGroupData targetWallGroup) {
            this.targetWallGroup = targetWallGroup;
            this.polygon = new Polygon();
        }
    }

    private static final class SearchMapSelectionSession {
        private static final int PARALLELOGRAM_CLOSE_RADIUS = 5;
        private Polygon polygon = new Polygon();
    }

    private static final class DoorPointPlacementSession {
        private final PastedObject openedDoorObject;
        private final DoorPointType pointType;
        private final String title;

        private DoorPointPlacementSession(PastedObject openedDoorObject, DoorPointType pointType, String title) {
            this.openedDoorObject = openedDoorObject;
            this.pointType = pointType;
            this.title = title != null ? title : "Set Point";
        }
    }

    private static enum SearchMapEditMode {
        NONE,
        PAINTER,
        ERASER
    }

    private static enum DoorPointType {
        LAUNCH_POINT,
        OPEN_LOCATION_FRONT,
        OPEN_LOCATION_BACK
    }

    private static final class DoorEditContext {
        private final PastedObject primaryDoorObject;
        private final PastedObject openDoorObject;
        private final PastedObject closedDoorObject;
        private final DoorData sharedData;

        private DoorEditContext(PastedObject primaryDoorObject, PastedObject openDoorObject,
                PastedObject closedDoorObject, DoorData sharedData) {
            this.primaryDoorObject = primaryDoorObject;
            this.openDoorObject = openDoorObject;
            this.closedDoorObject = closedDoorObject;
            this.sharedData = sharedData;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(J2DArea::new);
    }
}
