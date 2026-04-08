package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class CompositeObjectEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String ERROR = "Error";

    private static final Dimension BUTTON_SIZE = new Dimension(25, 25);

    private final Frame owner;
    private final List<PastedObject> pastedObjects = new ArrayList<PastedObject>();
    private final JPanel buildPanel;
    private final JScrollPane scrollPane;

    private int canvasWidth;
    private int canvasHeight;
    private Point mousePosition = new Point();
    private PastedObject objectToMove;
    private int objectToMoveIdx = -1;
    private Rectangle movingRectangle;
    private int deltaX;
    private int deltaY;
    private boolean drawClosed;
    private boolean night;
    private double zoom = 1.0;
    private boolean panning;
    private boolean panDragged;
    private boolean suppressNextClickAfterPan;
    private Point panStartMouseScreen;
    private Point panStartView;

    public CompositeObjectEditorDialog(Frame owner, BufferedImage initialImage) {
        super(owner, "Composite Object Editor", false);
        this.owner = owner;
        this.canvasWidth = Math.max(1, initialImage.getWidth());
        this.canvasHeight = Math.max(1, initialImage.getHeight());
        addInitialObject(initialImage);

        buildPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.scale(zoom, zoom);
                paintCanvas(g2);
                if (movingRectangle != null) {
                    g2.setColor(Color.GREEN);
                    g2.drawRect(movingRectangle.x, movingRectangle.y, movingRectangle.width, movingRectangle.height);
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return scaleDimension(canvasWidth, canvasHeight, zoom);
            }
        };
        buildPanel.setLayout(new GridLayout());
        buildPanel.setBackground(Color.DARK_GRAY);
        scrollPane = new JScrollPane(buildPanel);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e);
                if (objectToMove == null
                        && SwingUtilities.isLeftMouseButton(e)
                        && findPastedObjectAtPoint(scaledEvent.getX(), scaledEvent.getY()) == null) {
                    panning = true;
                    panDragged = false;
                    panStartMouseScreen = new Point(e.getXOnScreen(), e.getYOnScreen());
                    panStartView = scrollPane.getViewport().getViewPosition();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) {
                    panning = false;
                    panStartMouseScreen = null;
                    panStartView = null;
                    if (panDragged) {
                        suppressNextClickAfterPan = true;
                    }
                    panDragged = false;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                MouseEvent scaledEvent = scaleMouseEvent(e);
                if (suppressNextClickAfterPan) {
                    suppressNextClickAfterPan = false;
                    return;
                }
                if (SwingUtilities.isRightMouseButton(scaledEvent) || scaledEvent.isPopupTrigger()) {
                    return;
                }
                if (objectToMove == null) {
                    int idx = 0;
                    for (PastedObject pastedObject : pastedObjects) {
                        Rectangle rect = getPastedObjectBounds(pastedObject);
                        if (rect.contains(scaledEvent.getX(), scaledEvent.getY())
                                && isClickablePastedObjectHit(pastedObject, scaledEvent.getX() - rect.x, scaledEvent.getY() - rect.y)
                                && pastedObject.isVisible(drawClosed, night)) {
                            movingRectangle = rect;
                            objectToMove = pastedObject;
                            objectToMoveIdx = idx;
                            deltaX = scaledEvent.getX() - rect.x;
                            deltaY = scaledEvent.getY() - rect.y;
                        }
                        idx++;
                    }
                    if (scaledEvent.isControlDown() && objectToMove != null) {
                        objectToMove = objectToMove.copy();
                        pastedObjects.add(objectToMove);
                        objectToMoveIdx = pastedObjects.size() - 1;
                        deltaX = 0;
                        deltaY = 0;
                        Rectangle bounds = getPastedObjectBounds(objectToMove);
                        movingRectangle = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
                    }
                } else {
                    objectToMove = null;
                    objectToMoveIdx = -1;
                    movingRectangle = null;
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point areaPoint = toAreaPoint(e);
                mousePosition.move(areaPoint.x, areaPoint.y);
                if (objectToMove != null) {
                    int newRectX = Math.max(0, areaPoint.x - deltaX);
                    int newRectY = Math.max(0, areaPoint.y - deltaY);
                    setPastedObjectLocation(objectToMove, newRectX, newRectY);
                    ensureCanvasContains(objectToMove);
                    Rectangle anchorRect = getPastedObjectBounds(objectToMove);
                    movingRectangle = new Rectangle(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height);
                    buildPanel.revalidate();
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panning && panStartMouseScreen != null && panStartView != null) {
                    int dx = e.getXOnScreen() - panStartMouseScreen.x;
                    int dy = e.getYOnScreen() - panStartMouseScreen.y;
                    if (dx != 0 || dy != 0) {
                        panDragged = true;
                    }
                    JViewport viewport = scrollPane.getViewport();
                    Dimension preferredSize = buildPanel.getPreferredSize();
                    int maxX = Math.max(0, preferredSize.width - viewport.getWidth());
                    int maxY = Math.max(0, preferredSize.height - viewport.getHeight());
                    int viewX = Math.max(0, Math.min(panStartView.x - dx, maxX));
                    int viewY = Math.max(0, Math.min(panStartView.y - dy, maxY));
                    viewport.setViewPosition(new Point(viewX, viewY));
                    return;
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isShiftDown() && objectToMove != null) {
                    e.consume();
                    objectToMove.flip();
                    buildPanel.repaint();
                    return;
                }
                e.consume();
                double oldZoom = zoom;
                double zoomFactor = UserPreferences.getZoomFactor();
                double newZoom = clampZoom(oldZoom * (e.getWheelRotation() < 0 ? zoomFactor : 1.0 / zoomFactor));
                if (Math.abs(newZoom - oldZoom) < 0.0001) {
                    return;
                }
                zoom = newZoom;
                applyZoom(oldZoom, newZoom, e.getPoint());
            }
        };
        buildPanel.addMouseListener(mouseAdapter);
        buildPanel.addMouseMotionListener(mouseAdapter);
        buildPanel.addMouseWheelListener(mouseAdapter);
        buildPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
        buildPanel.getActionMap().put("Delete", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (objectToMove != null) {
                    pastedObjects.remove(objectToMove);
                    objectToMove = null;
                    objectToMoveIdx = -1;
                    movingRectangle = null;
                    buildPanel.repaint();
                }
            }
        });

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 2));
        toolbar.add(createToolbarButton("/icons/paste-from.png", "Paste image", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertObjectFromImage(FileChooserLocation.OBJECT, PastedObjectType.STANDARD);
            }
        }));
        toolbar.add(createToolbarButton("/icons/opened_door.png", "Paste open door", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertObjectFromImage(night ? FileChooserLocation.OPENED_DOOR_NIGHT : FileChooserLocation.OPENED_DOOR,
                    night ? PastedObjectType.OPENED_DOOR_NIGHT : PastedObjectType.OPENED_DOOR);
            }
        }));
        toolbar.add(createToolbarButton("/icons/closed_door.png", "Paste closed door", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertObjectFromImage(FileChooserLocation.CLOSED_DOOR, PastedObjectType.CLOSED_DOOR);
            }
        }));
        toolbar.add(createToolbarButton("/icons/night_light.png", "Paste night light", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertObjectFromImage(FileChooserLocation.NIGHT_LIGHT, PastedObjectType.NIGHT_LIGHT);
                night = true;
                buildPanel.repaint();
            }
        }));
        toolbar.add(createToolbarButton("/icons/entrance.png", "Paste entrance marker", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertEntrance();
            }
        }));
        JToggleButton drawClosedButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/draw_closed.png")));
        drawClosedButton.setSelected(drawClosed);
        drawClosedButton.setToolTipText("Toggle closed doors");
        drawClosedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawClosed = drawClosedButton.isSelected();
                buildPanel.repaint();
            }
        });
        configureToolbarButton(drawClosedButton);
        toolbar.add(drawClosedButton);
        JToggleButton nightButton = new JToggleButton(new ImageIcon(getClass().getResource("/icons/night.png")));
        nightButton.setSelected(night);
        nightButton.setToolTipText("Toggle day/night");
        nightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                night = nightButton.isSelected();
                buildPanel.repaint();
            }
        });
        configureToolbarButton(nightButton);
        toolbar.add(nightButton);
        toolbar.add(createToolbarButton("/icons/save.png", "Export composite object", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCompositeObject();
            }
        }));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        setMinimumSize(new Dimension(700, 500));
        setSize(900, 650);
    }

    public CompositeObjectEditorDialog(Frame owner, CompositeObjectData compositeObjectData) {
        this(owner, createTransparentImage(
            compositeObjectData != null ? compositeObjectData.getWidth() : 1,
            compositeObjectData != null ? compositeObjectData.getHeight() : 1
        ));
        loadCompositeObjectData(compositeObjectData);
    }

    private void addInitialObject(BufferedImage initialImage) {
        pastedObjects.add(new PastedObject(new Point(0, 0), new ExportableImage(initialImage)));
    }

    private static BufferedImage createTransparentImage(int width, int height) {
        return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    private void loadCompositeObjectData(CompositeObjectData compositeObjectData) {
        pastedObjects.clear();
        objectToMove = null;
        objectToMoveIdx = -1;
        movingRectangle = null;
        mousePosition = new Point();
        drawClosed = false;
        night = false;
        if (compositeObjectData == null) {
            canvasWidth = 1;
            canvasHeight = 1;
        } else {
            canvasWidth = Math.max(1, compositeObjectData.getWidth());
            canvasHeight = Math.max(1, compositeObjectData.getHeight());
            for (PastedObject pastedObject : compositeObjectData.getPastedObjects()) {
                PastedObject copy = pastedObject.copy();
                copy.setLocation(new Point(pastedObject.getLocation()));
                copy.setCompositeGroupId(null);
                if (copy.getEntranceData() != null && pastedObject.getEntranceData() != null) {
                    copy.getEntranceData().setX(pastedObject.getEntranceData().getX());
                    copy.getEntranceData().setY(pastedObject.getEntranceData().getY());
                    syncEntranceMarker(copy);
                }
                pastedObjects.add(copy);
            }
        }
        buildPanel.revalidate();
        buildPanel.repaint();
    }

    private JButton createToolbarButton(String iconPath, String tooltip, ActionListener listener) {
        JButton button = new JButton(new ImageIcon(getClass().getResource(iconPath)));
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        configureToolbarButton(button);
        return button;
    }

    private void configureToolbarButton(AbstractButton button) {
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setPreferredSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
    }

    private void insertObjectFromImage(FileChooserLocation fileChooserLocation, PastedObjectType pastedObjectType) {
        BufferedImage image = chooseImageFile(fileChooserLocation);
        if (image == null) {
            return;
        }
        PastedObject pastedObject = new PastedObject(new Point(mousePosition), new ExportableImage(image), pastedObjectType);
        pastedObjects.add(pastedObject);
        objectToMove = pastedObject;
        objectToMoveIdx = pastedObjects.size() - 1;
        deltaX = 0;
        deltaY = 0;
        ensureCanvasContains(pastedObject);
        Rectangle bounds = getPastedObjectBounds(pastedObject);
        movingRectangle = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        buildPanel.revalidate();
        buildPanel.repaint();
    }

    private void insertEntrance() {
        String entranceName = JOptionPane.showInputDialog(this, "Enter entrance name:", "Entrance" + countEntrances());
        if (entranceName == null || entranceName.trim().isEmpty()) {
            return;
        }
        BufferedImage markerImage = DirectionMarker.createEntranceMarkerImage(0);
        PastedObject entranceObject = new PastedObject(new Point(0, 0), new ExportableImage(markerImage), PastedObjectType.ENTRANCE);
        entranceObject.getEntranceData().setName(entranceName.trim());
        entranceObject.getEntranceData().setX(mousePosition.x);
        entranceObject.getEntranceData().setY(mousePosition.y);
        syncEntranceMarker(entranceObject);
        pastedObjects.add(entranceObject);
        objectToMove = entranceObject;
        objectToMoveIdx = pastedObjects.size() - 1;
        deltaX = markerImage.getWidth() / 2;
        deltaY = markerImage.getHeight() / 2;
        ensureCanvasContains(entranceObject);
        movingRectangle = getPastedObjectBounds(entranceObject);
        buildPanel.revalidate();
        buildPanel.repaint();
    }

    private int countEntrances() {
        int count = 0;
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE) {
                count++;
            }
        }
        return count + 1;
    }

    private void exportCompositeObject() {
        if (pastedObjects.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Composite object is empty.", ERROR, JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = J2DArea.chooseFile(owner, FileDialog.SAVE, FileChooserLocation.COMPOSITE_OBJECT);
        if (file == null) {
            return;
        }
        if (!file.getName().contains(".")) {
            file = new File(file.getParentFile(), file.getName() + ".j2dcmp");
        }
        CompositeObjectData compositeObjectData = new CompositeObjectData(canvasWidth, canvasHeight, copyObjectsForExport());
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
            try {
                compositeObjectData.writeExternal(objectOutputStream);
            } finally {
                objectOutputStream.close();
            }
            JOptionPane.showMessageDialog(this, "Composite object exported.");
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Composite object export failed.", ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<PastedObject> copyObjectsForExport() {
        List<PastedObject> copies = new ArrayList<PastedObject>(pastedObjects.size());
        for (PastedObject pastedObject : pastedObjects) {
            PastedObject copy = pastedObject.copy();
            copy.setLocation(new Point(pastedObject.getLocation()));
            if (copy.getEntranceData() != null && pastedObject.getEntranceData() != null) {
                copy.getEntranceData().setX(pastedObject.getEntranceData().getX());
                copy.getEntranceData().setY(pastedObject.getEntranceData().getY());
            }
            copies.add(copy);
        }
        return copies;
    }

    private BufferedImage chooseImageFile(FileChooserLocation fileChooserLocation) {
        File file = J2DArea.chooseFile(owner, FileDialog.LOAD, fileChooserLocation);
        if (file == null) {
            return null;
        }
        try {
            return javax.imageio.ImageIO.read(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening image.", ERROR, JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void paintCanvas(Graphics2D graphics) {
        int checker = 32;
        for (int y = 0; y < canvasHeight; y += checker) {
            for (int x = 0; x < canvasWidth; x += checker) {
                boolean dark = ((x / checker) + (y / checker)) % 2 == 0;
                graphics.setColor(dark ? new Color(88, 88, 88) : new Color(118, 118, 118));
                graphics.fillRect(x, y, checker, checker);
            }
        }
        for (PastedObject pastedObject : pastedObjects) {
            if (pastedObject.isVisible(drawClosed, night)) {
                pastedObject.drawImage(graphics, night);
                if (pastedObject.getPastedObjectType() == PastedObjectType.ENTRANCE && pastedObject.getEntranceData() != null) {
                    graphics.setColor(Color.WHITE);
                    graphics.drawString(
                        pastedObject.getEntranceData().getName(),
                        pastedObject.getX() + 2,
                        pastedObject.getY() - 2
                    );
                }
            }
        }
    }

    private void ensureCanvasContains(PastedObject pastedObject) {
        if (pastedObject == null) {
            return;
        }
        canvasWidth = Math.max(canvasWidth, pastedObject.getX() + pastedObject.getWidth());
        canvasHeight = Math.max(canvasHeight, pastedObject.getY() + pastedObject.getHeight());
    }

    private Point toAreaPoint(MouseEvent event) {
        int x = (int) Math.floor(event.getX() / zoom);
        int y = (int) Math.floor(event.getY() / zoom);
        return new Point(Math.max(0, x), Math.max(0, y));
    }

    private MouseEvent scaleMouseEvent(MouseEvent event) {
        Point areaPoint = toAreaPoint(event);
        return new MouseEvent(
            buildPanel,
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

    private Rectangle getPastedObjectBounds(PastedObject pastedObject) {
        if (pastedObject == null) {
            return new Rectangle();
        }
        return new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
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
            return localX >= 0 && localY >= 0 && localX < pastedObject.getWidth() && localY < pastedObject.getHeight();
        }
        if (localX < 0 || localY < 0 || localX >= pastedObject.getWidth() || localY >= pastedObject.getHeight()) {
            return false;
        }
        return pastedObject.isOpaque(localX, localY);
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

    private double clampZoom(double newZoom) {
        return Math.max(0.25, Math.min(newZoom, 4.0));
    }

    private void applyZoom(double oldZoom, double newZoom, Point mousePoint) {
        JViewport viewport = scrollPane.getViewport();
        Point viewPosition = viewport.getViewPosition();
        double contentX = (viewPosition.x + mousePoint.x) / oldZoom;
        double contentY = (viewPosition.y + mousePoint.y) / oldZoom;
        buildPanel.revalidate();
        buildPanel.repaint();
        int newViewX = (int) Math.round(contentX * newZoom - mousePoint.x);
        int newViewY = (int) Math.round(contentY * newZoom - mousePoint.y);
        Dimension preferredSize = buildPanel.getPreferredSize();
        int maxX = Math.max(0, preferredSize.width - viewport.getWidth());
        int maxY = Math.max(0, preferredSize.height - viewport.getHeight());
        viewport.setViewPosition(new Point(
            Math.max(0, Math.min(newViewX, maxX)),
            Math.max(0, Math.min(newViewY, maxY))
        ));
    }

    private Dimension scaleDimension(int width, int height, double scale) {
        return new Dimension(
            Math.max(1, (int) Math.round(width * scale)),
            Math.max(1, (int) Math.round(height * scale))
        );
    }
}
