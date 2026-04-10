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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

    private static final Dimension MIN_SIZE = new Dimension(1200, 800);

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
    private List<Polygon> parallelograms = new ArrayList<>();

    // Separate collections for polygon-based area features (not PastedObjects)
    private List<RegionData> regions = new ArrayList<>();
    private List<ContainerData> containers = new ArrayList<>();
    private List<WallGroupData> wallGroups = new ArrayList<>();
    private AreaAttributes areaAttributes = new AreaAttributes();

    private boolean editingPolygon;

    private boolean painting;
    private transient JPanel buildPanel;
    private transient JScrollPane buildScrollPane;
    private transient JScrollPane extractScrollPane;
    private transient JTabbedPane tabPane;
    private transient JMenuBar menubar;
    private transient JMenu backgroundMenu;
    private transient JMenu insertMenu;
    private transient JMenu cursorModeMenu;
    private transient JMenu viewMenu;
    private transient JMenu toolsMenu;
    private transient JMenuItem fillMenuItem;
    private transient JMenuItem openBrushTextureMenuItem;
    private transient JMenuItem tileSeamlessMenuItem;
    private transient JMenuItem saveDoorsMenuItem;
    private transient JMenuItem paint3dMenuItem;
    private transient JMenuItem subtractBackgroundMenuItem;
    private transient JRadioButtonMenuItem cursorSelectMenuItem;
    private transient JRadioButtonMenuItem brushModeMenuItem;
    private transient JRadioButtonMenuItem polygonModeMenuItem;
    private transient JRadioButtonMenuItem rectangleModeMenuItem;
    private transient LocalTransitionPlacementDialog localTransitionPlacementDialog;
    private transient JCheckBoxMenuItem drawClosedDoorMenuItem;
    private transient JCheckBoxMenuItem nightMenuItem;
    private transient JButton openBackgroundToolbarButton;
    private transient JButton fillToolbarButton;
    private transient JButton openBrushTextureToolbarButton;
    private transient JButton exportDoorTilesToolbarButton;
    private transient JButton tileSeamlessToolbarButton;
    private transient JButton paint3dToolbarButton;
    private transient JButton subtractBackgroundToolbarButton;
    private transient JButton regionsToolbarButton;
    private transient JButton wallGroupsToolbarButton;
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
    private transient JToggleButton polygonToolbarButton;
    private transient JToggleButton rectangleToolbarButton;
    private transient JToggleButton drawClosedDoorToggleButton;
    private transient JToggleButton nightToggleButton;
    private transient boolean buildPanning;
    private transient boolean buildPanDragged;
    private transient boolean suppressNextBuildClickAfterPan;
    private transient Point buildPanStartMouseScreen;
    private transient Point buildPanStartView;
    private transient boolean extractPanning;
    private transient boolean extractPanDragged;
    private transient boolean suppressNextExtractClickAfterPan;
    private transient boolean extractRectangleSelectionInProgress;
    private transient Point extractPanStartMouseScreen;
    private transient Point extractPanStartView;
    private transient List<Component> buildOnlyToolbarButtons = new ArrayList<Component>();
    private TransitionPlacementSession transitionPlacementSession;
    private transient WallGroupPlacementSession wallGroupPlacementSession;

    private int brushRadius = 30;
    private double buildZoom = 1.0;
    private double extractZoom = 1.0;
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

        JPanel extractPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.scale(extractZoom, extractZoom);
                if (extractionBackgroundImage != null) {
                    g2.drawImage(extractionBackgroundImage, 0, 0, null);
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawLine(mousePosition.x, 0, mousePosition.x, extractionBackgroundImage.getHeight());
                    g2.drawLine(0, mousePosition.y, extractionBackgroundImage.getWidth(), mousePosition.y);
                }
                Polygon newPolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                newPolygon.addPoint(mousePosition.x, mousePosition.y);
                if (polygon.npoints > 0 && Point2D.distance(mousePosition.x, mousePosition.y, polygon.xpoints[0], polygon.ypoints[0]) <= 3) {
                    g2.setColor(Color.YELLOW);
                    g2.drawPolygon(newPolygon);
                } else {
                    g2.setColor(Color.GREEN);
                    g2.drawPolyline(newPolygon.xpoints, newPolygon.ypoints, newPolygon.npoints);
                }
                if (isValidTileSetup()) {
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
                if (extractPanning && extractScrollPane != null && extractPanStartMouseScreen != null && extractPanStartView != null) {
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
                if (!editingPolygon && extractRectangleSelectionInProgress) {
                    MouseEvent scaledEvent = scaleMouseEvent(e, extractPanel, extractZoom);
                    tile.moveEndPoint(scaledEvent);
                }
                extractPanel.repaint();
            }
        });

        extractPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    extractPanning = true;
                    extractPanDragged = false;
                    extractPanStartMouseScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
                    extractPanStartView = extractScrollPane != null ? extractScrollPane.getViewport().getViewPosition() : new Point();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (extractPanning) {
                    extractPanning = false;
                    extractPanStartMouseScreen = null;
                    extractPanStartView = null;
                    if (extractPanDragged) {
                        suppressNextExtractClickAfterPan = true;
                    }
                    extractPanDragged = false;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (suppressNextExtractClickAfterPan) {
                    suppressNextExtractClickAfterPan = false;
                    return;
                }
                if (extractionBackgroundImage == null) {
                    return;
                }
                Point areaPoint = toAreaPoint(e, extractZoom);
                if (editingPolygon) {
                    if (polygon.npoints > 0 && (SwingUtilities.isRightMouseButton(e) || Point2D.distance(areaPoint.x, areaPoint.y, polygon.xpoints[0], polygon.ypoints[0]) <= 3)) {
                        Rectangle r = clampToImageBounds(polygon.getBounds(), extractionBackgroundImage);
                        if (r.width > 0 && r.height > 0) {
                            Polygon relativePolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                            relativePolygon.translate(-r.x, -r.y);
                            BufferedImage subimage = extractionBackgroundImage.getSubimage(r.x, r.y, r.width, r.height);
                            PolygonSelectionView polygonSelectionView = new PolygonSelectionView(subimage, relativePolygon, r.getLocation());
                            polygonSelectionView.setLocation(e.getXOnScreen(), e.getYOnScreen());
                        } else {
                            JOptionPane.showMessageDialog(
                                J2DArea.this,
                                "The selected polygon is too small to extract.",
                                ERROR,
                                JOptionPane.WARNING_MESSAGE
                            );
                        }
                        setExtractionPolygonMode(false);
                        syncCursorModeUi();
                    } else {
                        polygon.addPoint(areaPoint.x, areaPoint.y);
                    }
                    extractPanel.repaint();
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                MouseEvent scaledEvent = scaleMouseEvent(e, extractPanel, extractZoom);
                if (!extractRectangleSelectionInProgress) {
                    tile.moveStartPoint(scaledEvent);
                    tile.moveEndPoint(scaledEvent);
                    extractRectangleSelectionInProgress = true;
                } else {
                    tile.moveEndPoint(scaledEvent);
                    extractRectangleSelectionInProgress = false;
                    updateTexturePreviewFromTileSelection();
                }
                extractPanel.repaint();
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
                applyZoom(extractScrollPane, extractPanel, oldZoom, newZoom, e.getPoint());
            }
        });

        MouseAdapter buildPanelMouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
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
                if (!painting && !editingBlackParallelogram && !editingTextureParallelogram
                        && objectToMove == null
                        && SwingUtilities.isLeftMouseButton(e)
                        && findPastedObjectAtPoint(scaledEvent.getX(), scaledEvent.getY()) == null) {
                    buildPanning = true;
                    buildPanDragged = false;
                    buildPanStartMouseScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
                    buildPanStartView = buildScrollPane != null ? buildScrollPane.getViewport().getViewPosition() : new Point();
                    return;
                }
                if (painting) {
                    updateBrushStroke(scaledEvent);
                    repaint();
                }
            }

            public void updateBrushStroke(MouseEvent e) {
                if (brushTexture == null) {
                    return;
                }
                for (int x = e.getX() - brushRadius; x < e.getX() + brushRadius; x++) {
                    for (int y = e.getY() - brushRadius; y < e.getY() + brushRadius; y++) {
                        double dist = Point2D.distance(x, y, e.getX(), e.getY());
                        if (dist < brushRadius && x >= 0 && y >= 0 && x < buildBackgroundImage.getWidth() && y < buildBackgroundImage.getHeight()) {
                            // Calculate blend factor based on distance to edge of brush.
                            // This will be 1.0 at the center of the brush and 0.0 at the edge.
                            double blend = 1.0 - dist / brushRadius;

                            Color background = new Color(buildBackgroundImage.getRGB(x, y));
                            Color brush = new Color(brushTexture.getRGB(x % brushTexture.getWidth(), y % brushTexture.getHeight()));

                            // Linearly interpolate between the background and brush colors based on the blend factor.
                            int r = (int)(background.getRed() * (1.0 - blend) + brush.getRed() * blend);
                            int g = (int)(background.getGreen() * (1.0 - blend) + brush.getGreen() * blend);
                            int b = (int)(background.getBlue() * (1.0 - blend) + brush.getBlue() * blend);
                            int a = (int)(background.getAlpha() * (1.0 - blend) + brush.getAlpha() * blend);
                            
                            buildBackgroundImage.setRGB(x, y, new Color(r, g, b, a).getRGB());
                            buildBackgroundNightImage.setRGB(x, y, new Color((int) (0.45 * r), (int) (0.45 * g), (int) (0.85 * b), a).getRGB());
                        }
                    }
                }
            }


            @Override
            public void mouseClicked(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
                if (suppressNextBuildClickAfterPan) {
                    suppressNextBuildClickAfterPan = false;
                    return;
                }
                if (handleWallGroupPlacementCanvasClick(scaledEvent)) {
                    buildPanel.repaint();
                    return;
                }
                if (handleTransitionPlacementCanvasClick(scaledEvent)) {
                    buildPanel.repaint();
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
                            buildPanel.repaint();
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
                        }
                    }
                } else {
                    if (SwingUtilities.isRightMouseButton(scaledEvent) || scaledEvent.isPopupTrigger()) {
                        return;
                    }

                    if (objectToMove == null) {
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
                    } else {
                        objectToMove = null;
                        objectToMoveIdx = -1;
                        movingRectangle = null;
                        clearCompositeMove();
                    }
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
                if (buildPanning) {
                    buildPanning = false;
                    buildPanStartMouseScreen = null;
                    buildPanStartView = null;
                    if (buildPanDragged) {
                        suppressNextBuildClickAfterPan = true;
                    }
                    buildPanDragged = false;
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
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point areaPoint = toAreaPoint(e, buildZoom);
                mousePosition.move(areaPoint.x, areaPoint.y);
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
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e, buildPanel, buildZoom);
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
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isShiftDown() && objectToMove != null) {
                    e.consume();
                    objectToMove.flip();
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
                    applyZoom(buildScrollPane, buildPanel, oldZoom, newZoom, e.getPoint());
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
                if (objectToMove != null) {
                    pastedObjects.remove(objectToMove);
                    movingRectangle = null;
                    objectToMove = null;
                    objectToMoveIdx = -1;
                    clearCompositeMove();
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
        backgroundMenu = new JMenu("Background");
        insertMenu = new JMenu("Insert");
        cursorModeMenu = new JMenu("Cursor Mode");
        viewMenu = new JMenu("View");
        toolsMenu = new JMenu("Tools");
        JMenu settingsMenu = new JMenu("Settings");
        JMenu helpMenu = new JMenu("Help");
        menubar.add(fileMenu);
        menubar.add(backgroundMenu);
        menubar.add(insertMenu);
        menubar.add(cursorModeMenu);
        menubar.add(viewMenu);
        menubar.add(toolsMenu);
        menubar.add(settingsMenu);
        menubar.add(helpMenu);
        menubar.add(Box.createHorizontalStrut(8));
        menubar.add(Box.createHorizontalGlue());
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
                        pastedObjects.clear();
                        regions.clear();
                        containers.clear();
                        wallGroups.clear();
                        areaAttributes = new AreaAttributes();
                        objectToMove = null;
                        objectToMoveIdx = -1;
                        movingRectangle = null;
                        clearCompositeMove();
                        polygon.reset();
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
        JMenuItem newMenuItem = new JMenuItem(newButton.getAction());
        newMenuItem.setText("New Area");
        fileMenu.add(newMenuItem);
        JMenuItem newCompositeMenuItem = new JMenuItem("New Composite Object...");
        newCompositeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCompositeObjectEditor();
            }
        });
        fileMenu.add(newCompositeMenuItem);
        JMenuItem openCompositeMenuItem = new JMenuItem("Open Composite Object...");
        openCompositeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openExistingCompositeObjectEditor();
            }
        });
        fileMenu.add(openCompositeMenuItem);

        JButton fillButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/background.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage textureImage = chooseImageFile(FileChooserLocation.TEXTURE);
                if (textureImage != null) {
                    for (int x = 0; x < buildBackgroundImage.getWidth(); x++) {
                        for (int y = 0; y < buildBackgroundImage.getHeight(); y++) {
                            buildBackgroundImage.setRGB(x, y, textureImage.getRGB(x % textureImage.getWidth(), y % textureImage.getHeight()));
                        }
                    }
                    buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter("J2DArea project files", "j2da");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try (FileInputStream fileInputStream = new FileInputStream(chooser.getSelectedFile());
                            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream)) {
                        ExportableArea exportableArea = new ExportableArea();
                        exportableArea.readExternal(objectInputStream);
                        buildBackgroundImage = exportableArea.getBackgroundImage().getImage();
                        backgroundWidth = buildBackgroundImage.getWidth();
                        backgroundHeight = buildBackgroundImage.getHeight();
                        buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
                        pastedObjects = exportableArea.getPastedObjects();
                        regions = exportableArea.getRegions();
                        containers = exportableArea.getContainers();
                        wallGroups = exportableArea.getWallGroups();
                        areaAttributes = exportableArea.getAreaAttributes();
                        refreshEntranceMarkers();
                        objectToMove = null;
                        objectToMoveIdx = -1;
                        movingRectangle = null;
                        clearCompositeMove();
                        setExtendedState(Frame.MAXIMIZED_BOTH);
                        repaint();
                    } catch (IOException | ClassNotFoundException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error opening file.", ERROR, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        openButton.setMaximumSize(BUTTON_SIZE);
        openButton.setToolTipText("Open a project file");
        JMenuItem openMenuItem = new JMenuItem(openButton.getAction());
        openMenuItem.setText("Open Project...");
        fileMenu.add(openMenuItem);

        JButton openBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage chosenImageFile = chooseImageFile(FileChooserLocation.OPEN_BG);
                if (chosenImageFile != null) {
                    if (tabPane.getSelectedComponent() == buildScrollPane) {
                        buildBackgroundImage = chosenImageFile;
                        backgroundWidth = chosenImageFile.getWidth();
                        backgroundHeight = chosenImageFile.getHeight();
                        buildBackgroundNightImage = ImageFilter.applyNightFilter(buildBackgroundImage);
                    }
                    if (tabPane.getSelectedComponent() == extractScrollPane) {
                        extractionBackgroundImage = chosenImageFile;
                        polygon.reset();
                        tile.reset();
                        extractRectangleSelectionInProgress = false;
                    }
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                    repaint();
                }
            }
        });
        openBackgroundButton.setMaximumSize(BUTTON_SIZE);
        openBackgroundButton.setToolTipText("Open a background image file");
        configureToolbarButton(openBackgroundButton);
        openBackgroundToolbarButton = openBackgroundButton;
        JMenuItem openBackgroundMenuItem = new JMenuItem(openBackgroundButton.getAction());
        openBackgroundMenuItem.setText("Open Background Image...");
        backgroundMenu.add(openBackgroundMenuItem);

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
                FileNameExtensionFilter filter = new FileNameExtensionFilter("J2DArea project files", "j2da");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    boolean success;
                    ExportableArea exportableArea = new ExportableArea(
                        new ExportableImage(buildBackgroundImage),
                        pastedObjects,
                        regions,
                        containers,
                        wallGroups,
                        areaAttributes
                    );
                    try (FileOutputStream fileOutputStream = new FileOutputStream(chooser.getSelectedFile())) {
                        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
                            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream)) {
                                exportableArea.writeExternal(objectOutputStream);
                            }
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
        fileMenu.addSeparator();
        JMenuItem exportMenuItem = new JMenuItem(exportButton.getAction());
        exportMenuItem.setText("Export Area Image...");
        fileMenu.add(exportMenuItem);

        JButton exportModButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-doors.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                exportAsBaldursGateMod();
            }
        });
        exportModButton.setMaximumSize(BUTTON_SIZE);
        exportModButton.setToolTipText("Export as Baldur's Gate mod (WeiDU package)");
        JMenuItem exportModMenuItem = new JMenuItem(exportModButton.getAction());
        exportModMenuItem.setText("Export Mod Package...");
        fileMenu.add(exportModMenuItem);

        JButton prefixButton = new JButton(new AbstractAction("Prefix") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editExportPrefix();
            }
        });
        prefixButton.setToolTipText("Select the reserved resource prefix used for exports");
        JMenuItem prefixMenuItem = new JMenuItem(prefixButton.getAction());
        prefixMenuItem.setText("Export Prefix...");
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
                    drawClosed = false;
                    repaint();
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
                    drawClosed = true;
                    repaint();
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
                drawClosed = !drawClosed;
                painting = false;
                repaint();
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
        JToggleButton polygonButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/polygon.png")));
        polygonButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionPolygonMode(true);
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
        viewMenu.addSeparator();
        ButtonGroup buildCursorModeGroup = new ButtonGroup();
        cursorSelectMenuItem = new JRadioButtonMenuItem("Select Objects", !painting);
        cursorSelectMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        cursorSelectMenuItem.setToolTipText("Select objects");
        cursorSelectMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                syncCursorModeUi();
                repaint();
            }
        });
        buildCursorModeGroup.add(cursorSelectMenuItem);
        cursorModeMenu.add(cursorSelectMenuItem);

        JToggleButton brushButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/pencil.png")));
        brushButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                painting = ensureBrushTextureSelected();
                syncCursorModeUi();
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
                painting = ensureBrushTextureSelected();
                syncCursorModeUi();
            }
        });
        buildCursorModeGroup.add(brushModeMenuItem);
        cursorModeMenu.add(brushModeMenuItem);

        JToggleButton cursorButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/cursor.png")));
        cursorButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
                syncCursorModeUi();
                repaint();
            }
        });
        cursorButton.setMaximumSize(BUTTON_SIZE);
        cursorButton.setToolTipText("Select objects");
        configureToolbarButton(cursorButton);
        cursorToolbarButton = cursorButton;
        ButtonGroup extractionCursorModeGroup = new ButtonGroup();
        polygonModeMenuItem = new JRadioButtonMenuItem("Polygon Selection", editingPolygon);
        polygonModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/polygon.png")));
        polygonModeMenuItem.setToolTipText("Polygon selection");
        polygonModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionPolygonMode(true);
                if (editingPolygon && tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
                syncCursorModeUi();
            }
        });
        extractionCursorModeGroup.add(polygonModeMenuItem);
        cursorModeMenu.add(polygonModeMenuItem);
        rectangleModeMenuItem = new JRadioButtonMenuItem("Rectangle Selection", !editingPolygon);
        rectangleModeMenuItem.setIcon(new ImageIcon(getClass().getResource("/icons/rectangle.png")));
        rectangleModeMenuItem.setToolTipText("Rectangle selection");
        rectangleModeMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionPolygonMode(false);
                syncCursorModeUi();
            }
        });
        extractionCursorModeGroup.add(rectangleModeMenuItem);
        cursorModeMenu.add(rectangleModeMenuItem);
        JToggleButton rectangleButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/rectangle.png")));
        rectangleButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setExtractionPolygonMode(false);
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
        toolsMenu.addSeparator();
        paint3dMenuItem = new JMenuItem(paint3dButton.getAction());
        paint3dMenuItem.setText("Edit Selection in Paint 3D");
        toolsMenu.add(paint3dMenuItem);

        JButton subtractBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/remove-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                    BGSubtracterPreview bgSubtracterPreview = new BGSubtracterPreview(tile.getSubImage(extractionBackgroundImage));
                    bgSubtracterPreview.setLocation(tile.getXOnScreen(), tile.getYOnScreen());
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

        menubar.add(openBackgroundToolbarButton);
        menubar.add(fillToolbarButton);
        menubar.add(openBrushTextureToolbarButton);
        menubar.add(regionsToolbarButton);
        menubar.add(wallGroupsToolbarButton);
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
        menubar.add(polygonToolbarButton);
        menubar.add(rectangleToolbarButton);
        menubar.add(exportDoorTilesToolbarButton);
        menubar.add(tileSeamlessToolbarButton);
        menubar.add(paint3dToolbarButton);
        menubar.add(subtractBackgroundToolbarButton);
        menubar.add(drawClosedDoorToggleButton);
        menubar.add(nightToggleButton);
        buildOnlyToolbarButtons.clear();
        buildOnlyToolbarButtons.add(fillToolbarButton);
        buildOnlyToolbarButtons.add(openBrushTextureToolbarButton);
        buildOnlyToolbarButtons.add(regionsToolbarButton);
        buildOnlyToolbarButtons.add(wallGroupsToolbarButton);
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

        tabPane.addChangeListener(e -> updateTabSpecificUi());
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
            FileInputStream fileInputStream = new FileInputStream(file);
            GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            try {
                CompositeObjectData compositeObjectData = new CompositeObjectData();
                compositeObjectData.readExternal(objectInputStream);
                return compositeObjectData;
            } finally {
                objectInputStream.close();
            }
        } catch (IOException | ClassNotFoundException ex) {
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

    private boolean isValidTileSetup() {
        return extractionBackgroundImage != null && polygon.npoints == 0 && !tile.isEmpty();
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

    private BufferedImage renderEditorSnapshot() {
        return renderArea(drawClosed, night);
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
                + "<b>Mouse Wheel</b>: zoom.<br>"
                + "<b>Shift + Mouse Wheel</b>: flip the selected object, or change brush size while painting.<br><br>"
                + "<b>Keyboard</b><br>"
                + "<b>Delete</b>: remove the selected object.<br>"
                + "<b>+</b> or <b>Shift+=</b>: bring the selected object forward.<br>"
                + "<b>-</b> or <b>NumPad-</b> or <b>6</b>: send the selected object backward.<br>"
                + "<b>Up</b> / <b>Down</b>: adjust the selected object's vertical placement.<br><br>"
                + "<b>Extraction Area</b><br><br>"
                + "<b>Mouse</b><br>"
                + "<b>Left-drag</b>: move the map.<br>"
                + "<b>Left-click</b> in Rectangle Selection mode: start the selection, then click again to finish it.<br>"
                + "<b>Left-click</b> in Polygon Selection mode: add polygon vertices.<br>"
                + "<b>Right-click</b> or <b>click near the first vertex</b> in Polygon Selection mode: close the polygon.<br>"
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
        List<DoorExportData> exports = new ArrayList<DoorExportData>();
        int index = 1;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.getPastedObjectType() == PastedObjectType.OPENED_DOOR
                    || pastedObject.getPastedObjectType() == PastedObjectType.CLOSED_DOOR) {
                exports.add(createDoorExportData(index++, pastedObject));
            }
        }
        return exports;
    }

    private DoorExportData createDoorExportData(int index, PastedObject doorObject) {
        Rectangle openBounds = objectBounds(doorObject);
        Rectangle closedBounds = openBounds;
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
        return new DoorExportData(
            doorName,
            doorId,
            createDoorPolygon(openBounds),
            createDoorPolygon(closedBounds),
            tileCells,
            new ArrayList<Point>(),
            new ArrayList<Point>(),
            0,
            "",
            new Point(),
            new Point(),
            new Point(),
            30
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
        BufferedImage image = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        return image;
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
        if (dialog.isUsedCurrentSelection()) {
            setExtractionPolygonMode(false);
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
        cancelTransitionPlacement(false);
        transitionPlacementSession = new TransitionPlacementSession(entranceName);
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

        cancelTransitionPlacement(false);
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
        repaint();
    }

    private boolean showBuildPanelContextMenu(MouseEvent e) {
        if (e == null || !e.isPopupTrigger()) {
            return false;
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

    private void showWallGroupContextMenu(MouseEvent e, WallGroupData wallGroup) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editWallGroupItem = new JMenuItem("Edit Wallgroup");
        editWallGroupItem.addActionListener(evt -> {
            editWallGroup(wallGroup);
            repaint();
        });
        menu.add(editWallGroupItem);

        JMenuItem redrawPolygonItem = new JMenuItem("Redraw Polygon");
        redrawPolygonItem.addActionListener(evt -> {
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
                wallGroups.remove(wallGroup);
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

    private void clearObjectMoveSelection() {
        objectToMove = null;
        objectToMoveIdx = -1;
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

    private void syncCursorModeUi() {
        if (cursorSelectMenuItem != null) {
            cursorSelectMenuItem.setSelected(!painting);
        }
        if (brushModeMenuItem != null) {
            brushModeMenuItem.setSelected(painting);
        }
        if (cursorToolbarButton != null) {
            cursorToolbarButton.setSelected(!painting);
        }
        if (brushToolbarButton != null) {
            brushToolbarButton.setSelected(painting);
        }
        if (polygonModeMenuItem != null && polygonModeMenuItem.isSelected() != editingPolygon) {
            polygonModeMenuItem.setSelected(editingPolygon);
        }
        if (rectangleModeMenuItem != null && rectangleModeMenuItem.isSelected() == editingPolygon) {
            rectangleModeMenuItem.setSelected(!editingPolygon);
        }
        if (polygonToolbarButton != null) {
            polygonToolbarButton.setSelected(editingPolygon);
        }
        if (rectangleToolbarButton != null) {
            rectangleToolbarButton.setSelected(!editingPolygon);
        }
    }

    private void updateTabSpecificUi() {
        syncCursorModeUi();
        boolean buildTabSelected = tabPane != null && tabPane.getSelectedComponent() == buildScrollPane;
        boolean extractionTabSelected = tabPane != null && tabPane.getSelectedComponent() == extractScrollPane;
        boolean areaEditingTabSelected = buildTabSelected || extractionTabSelected;

        setUiVisible(backgroundMenu, areaEditingTabSelected);
        setUiVisible(insertMenu, buildTabSelected);
        setUiVisible(cursorModeMenu, areaEditingTabSelected);
        setUiVisible(viewMenu, buildTabSelected);
        setUiVisible(toolsMenu, buildTabSelected || extractionTabSelected);

        setUiVisible(fillMenuItem, buildTabSelected);
        setUiVisible(openBrushTextureMenuItem, buildTabSelected);
        setUiVisible(tileSeamlessMenuItem, extractionTabSelected);
        setUiVisible(saveDoorsMenuItem, buildTabSelected);
        setUiVisible(paint3dMenuItem, extractionTabSelected);
        setUiVisible(subtractBackgroundMenuItem, extractionTabSelected);
        setUiVisible(cursorSelectMenuItem, buildTabSelected);
        setUiVisible(brushModeMenuItem, buildTabSelected);
        setUiVisible(polygonModeMenuItem, extractionTabSelected);
        setUiVisible(rectangleModeMenuItem, extractionTabSelected);

        setUiVisible(openBackgroundToolbarButton, areaEditingTabSelected);
        for (Component component : buildOnlyToolbarButtons) {
            setUiVisible(component, buildTabSelected);
        }
        setUiVisible(exportDoorTilesToolbarButton, buildTabSelected);
        setUiVisible(tileSeamlessToolbarButton, extractionTabSelected);
        setUiVisible(paint3dToolbarButton, extractionTabSelected);
        setUiVisible(subtractBackgroundToolbarButton, extractionTabSelected);
        setUiVisible(cursorToolbarButton, buildTabSelected);
        setUiVisible(brushToolbarButton, buildTabSelected);
        setUiVisible(polygonToolbarButton, extractionTabSelected);
        setUiVisible(rectangleToolbarButton, extractionTabSelected);

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

    private void configureCanvasScrollPane(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    }

    private void setExtractionPolygonMode(boolean editingPolygon) {
        this.editingPolygon = editingPolygon;
        if (editingPolygon) {
            tile.reset();
            extractRectangleSelectionInProgress = false;
        } else {
            polygon.reset();
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

    private void applyZoom(JScrollPane scrollPane, JPanel panel, double oldZoom, double newZoom, Point mousePoint) {
        if (scrollPane == null || panel == null || mousePoint == null) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        Point viewPosition = viewport.getViewPosition();
        double contentX = (viewPosition.x + mousePoint.x) / oldZoom;
        double contentY = (viewPosition.y + mousePoint.y) / oldZoom;
        panel.revalidate();
        panel.repaint();
        int newViewX = (int) Math.round(contentX * newZoom - mousePoint.x);
        int newViewY = (int) Math.round(contentY * newZoom - mousePoint.y);
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

    private boolean editTransitionPair(EntranceData entranceData) {
        if (entranceData == null || entranceData.getDestinationAreaType() != DestinationAreaType.EXISTING_GAME_AREA) {
            return false;
        }
        RegionData pairedRegion = findRegionPairedWithEntrance(entranceData.getName());
        return editTransitionPair(entranceData, pairedRegion);
    }

    private boolean editTransitionPair(RegionData regionData) {
        if (regionData == null || trimToEmpty(regionData.getPairedEntranceName()).isEmpty()) {
            return false;
        }
        PastedObject entranceObject = findEntranceByName(regionData.getPairedEntranceName());
        if (entranceObject == null || entranceObject.getEntranceData() == null) {
            return false;
        }
        return editTransitionPair(entranceObject.getEntranceData(), regionData);
    }

    private boolean editTransitionPair(EntranceData entranceData, RegionData regionData) {
        if (entranceData == null || entranceData.getDestinationAreaType() != DestinationAreaType.EXISTING_GAME_AREA) {
            return false;
        }

        String entranceDestinationArea = trimToEmpty(entranceData.getDestinationArea());
        String regionDestinationArea = regionData != null ? trimToEmpty(regionData.getDestinationArea()) : "";
        String destinationArea = !entranceDestinationArea.isEmpty() ? entranceDestinationArea : regionDestinationArea;
        if (destinationArea.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Set the destination area first before editing the destination-side transition pair.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return true;
        }
        if (!entranceDestinationArea.isEmpty() && !regionDestinationArea.isEmpty()
                && !entranceDestinationArea.equalsIgnoreCase(regionDestinationArea)) {
            JOptionPane.showMessageDialog(this,
                "The paired entrance and travel region target different destination areas. Fix that mismatch in the editors first.",
                ERROR,
                JOptionPane.ERROR_MESSAGE);
            return true;
        }

        Polygon localPolygon = regionData != null ? clonePolygon(regionData.getBounds()) : new Polygon();
        LocalTransitionRegionSelectionDialog localDialog = new LocalTransitionRegionSelectionDialog(
            this,
            buildBackgroundImage,
            localPolygon
        );
        localDialog.setVisible(true);
        if (!localDialog.isConfirmed()) {
            return true;
        }
        localPolygon = localDialog.getPolygon();

        int pointX = entranceData.getDestinationPointX();
        int pointY = entranceData.getDestinationPointY();
        int pointOrientation = entranceData.getDestinationPointOrientation();
        if (pointX == 0 && pointY == 0 && regionData != null) {
            pointX = regionData.getDestinationPointX();
            pointY = regionData.getDestinationPointY();
            pointOrientation = regionData.getDestinationPointOrientation();
        }
        Polygon polygon = regionData != null && regionData.getDestinationReturnPolygon().npoints >= 3
            ? regionData.getDestinationReturnPolygon()
            : entranceData.getDestinationReturnPolygon();
        String destinationEntranceName = trimToEmpty(entranceData.getDestinationEntrance());
        if (destinationEntranceName.isEmpty() && regionData != null) {
            destinationEntranceName = trimToEmpty(regionData.getDestinationEntrance());
        }

        DestinationPatchSelectionDialog dialog = new DestinationPatchSelectionDialog(
            this,
            destinationArea,
            pointX,
            pointY,
            pointOrientation,
            destinationEntranceName,
            polygon
        );
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return true;
        }

        RegionData effectiveRegion = regionData;
        if (effectiveRegion == null) {
            effectiveRegion = new RegionData(buildPairedTravelRegionName(entranceData), 2, localPolygon);
            regions.add(effectiveRegion);
        } else {
            effectiveRegion.setBounds(localPolygon);
            if (trimToEmpty(effectiveRegion.getName()).isEmpty()) {
                effectiveRegion.setName(buildPairedTravelRegionName(entranceData));
            }
        }

        entranceData.setDestinationArea(destinationArea);
        entranceData.setDestinationEntrance(dialog.getDestinationEntranceName());
        entranceData.setCreateDestinationReturnTransition(true);
        entranceData.setDestinationPointX(dialog.getPointX());
        entranceData.setDestinationPointY(dialog.getPointY());
        entranceData.setDestinationPointOrientation(dialog.getPointOrientation());
        entranceData.setDestinationReturnPolygon(dialog.getReturnPolygon());
        entranceData.setDestinationPreviewImagePath("");

        effectiveRegion.setType(2);
        effectiveRegion.setBounds(localPolygon);
        effectiveRegion.setDestinationAreaType(DestinationAreaType.EXISTING_GAME_AREA);
        effectiveRegion.setDestinationArea(destinationArea);
        effectiveRegion.setDestinationEntrance(dialog.getDestinationEntranceName());
        effectiveRegion.setDestinationPointX(dialog.getPointX());
        effectiveRegion.setDestinationPointY(dialog.getPointY());
        effectiveRegion.setDestinationPointOrientation(dialog.getPointOrientation());
        effectiveRegion.setDestinationReturnPolygon(dialog.getReturnPolygon());
        effectiveRegion.setDestinationPreviewImagePath("");
        effectiveRegion.setPairedEntranceName(trimToEmpty(entranceData.getName()));
        repaint();
        return true;
    }

    private String buildPairedTravelRegionName(EntranceData entranceData) {
        String entranceName = trimToEmpty(entranceData != null ? entranceData.getName() : "");
        return buildPairedTravelRegionName(entranceName);
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

    private RegionData findRegionPairedWithEntrance(String entranceName) {
        String normalizedName = trimToEmpty(entranceName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        for (RegionData regionData : regions) {
            if (normalizedName.equals(trimToEmpty(regionData.getPairedEntranceName()))) {
                return regionData;
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
            if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE
                    && pastedObject.getEntranceData() != null
                    && normalizedName.equals(trimToEmpty(pastedObject.getEntranceData().getName()))) {
                return pastedObject;
            }
        }
        return null;
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

    private static final class TransitionPlacementSession {
        private final String entranceName;
        private int entranceX;
        private int entranceY;
        private int orientation;
        private boolean hasPoint;
        private Polygon localPolygon;

        private TransitionPlacementSession(String entranceName) {
            this.entranceName = entranceName != null ? entranceName.trim() : "";
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(J2DArea::new);
    }
}
