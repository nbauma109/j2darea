package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class J2DArea extends JFrame {

    private static final Pattern IMG_FILE_PATTERN = Pattern.compile(".+?\\.(png|jpe?g|gif|tiff?)$", Pattern.CASE_INSENSITIVE);

    private static final String ERROR = "Error";

    private static final String PLUS = "Plus";

    private static final String MINUS = "Minus";

    private static final String UP = "Up";
    
    private static final String DOWN = "Down";
    
    private static final String USER_HOME = "user.home";

    private static final Dimension MIN_SIZE = new Dimension(800, 800);

    public static final Dimension BUTTON_SIZE = new Dimension(25, 25);

    private static final long serialVersionUID = 1L;

    private int backgroundWidth = 5120;
    private int backgroundheight = 3840;
    private transient BufferedImage buildBackgroundImage = new BufferedImage(backgroundWidth, backgroundheight, BufferedImage.TYPE_INT_RGB);
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
    private boolean editingParallelogram;
    private List<Polygon> parallelograms = new ArrayList<>();

    private boolean editingPolygon;

    private boolean painting;

    private int brushRadius = 30;
    private transient BufferedImage brushTexture;
    private transient BufferedImage brushPreview;

    public J2DArea() {
        super("J2DArea");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabPane = new JTabbedPane(SwingConstants.BOTTOM);
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JPanel buildPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintObjects(g);
                g.setColor(Color.GREEN);
                for (Polygon parallelogram : parallelograms) {
                    if (parallelogram.npoints < 3) {
                        Polygon newPolygon = new Polygon(parallelogram.xpoints, parallelogram.ypoints, parallelogram.npoints);
                        newPolygon.addPoint(mousePosition.x, mousePosition.y);
                        g.drawPolygon(newPolygon);
                    } else {
                        g.setColor(Color.BLACK);
                        g.fillPolygon(parallelogram);
                        g.setColor(Color.GREEN);
                        g.drawPolygon(parallelogram);
                    }
                }
                if (movingRectangle != null) {
                    g.drawRect(movingRectangle.x, movingRectangle.y, movingRectangle.width, movingRectangle.height);
                }
            }

            @Override
            public Dimension getPreferredSize() {
                if (buildBackgroundImage != null) {
                    return new Dimension(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight());
                }
                return new Dimension(backgroundWidth, backgroundheight);
            }
        };

        JPanel extractPanel = new JPanel(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (extractionBackgroundImage != null) {
                    g.drawImage(extractionBackgroundImage, 0, 0, null);
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawLine(mousePosition.x, 0, mousePosition.x, getHeight());
                    g.drawLine(0, mousePosition.y, getWidth(), mousePosition.y);
                }
                Polygon newPolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                newPolygon.addPoint(mousePosition.x, mousePosition.y);
                if (polygon.npoints > 0 && Point2D.distance(mousePosition.x, mousePosition.y, polygon.xpoints[0], polygon.ypoints[0]) <= 3) {
                    g.setColor(Color.YELLOW);
                    g.drawPolygon(newPolygon);
                } else {
                    g.setColor(Color.GREEN);
                    g.drawPolyline(newPolygon.xpoints, newPolygon.ypoints, newPolygon.npoints);
                }
                if (isValidTileSetup()) {
                    tile.draw(g);
                }
            }

            @Override
            public Dimension getPreferredSize() {
                if (extractionBackgroundImage != null) {
                    return new Dimension(extractionBackgroundImage.getWidth(), extractionBackgroundImage.getHeight());
                }
                return new Dimension(backgroundWidth, backgroundheight);
            }
        };

        extractPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                tile.moveEndPoint(e);
                extractPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition.move(e.getX(), e.getY());
                extractPanel.repaint();
            }
        });

        extractPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                tile.moveStartPoint(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                tile.moveEndPoint(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (extractionBackgroundImage != null && editingPolygon) {
                    if (polygon.npoints > 0 && (SwingUtilities.isRightMouseButton(e) || Point2D.distance(e.getX(), e.getY(), polygon.xpoints[0], polygon.ypoints[0]) <= 3)) {
                        editingPolygon = false;
                        Rectangle r = polygon.getBounds();
                        Polygon relativePolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                        relativePolygon.translate(-r.x, -r.y);
                        BufferedImage subimage = extractionBackgroundImage.getSubimage(r.x, r.y, r.width, r.height);
                        PolygonSelectionView polygonSelectionView = new PolygonSelectionView(subimage, relativePolygon, r.getLocation());
                        polygonSelectionView.setLocation(e.getXOnScreen(), e.getYOnScreen());
                        polygon.reset();
                    } else {
                        polygon.addPoint(e.getX(), e.getY());
                    }
                    extractPanel.repaint();
                }
            }
        });

        MouseAdapter buildPanelMouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (painting) {
                    updateBrushStroke(e);
                    repaint();
                }
            }

            public void updateBrushStroke(MouseEvent e) {
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
                        }
                    }
                }
            }


            @Override
            public void mouseClicked(MouseEvent e) {
                if (editingParallelogram) {
                    if (parallelograms.isEmpty() || parallelograms.get(parallelograms.size() - 1).npoints == 4) {
                        Polygon parallelogram = new Polygon();
                        parallelogram.addPoint(e.getX(), e.getY());
                        parallelograms.add(parallelogram);
                    } else {
                        Polygon p = parallelograms.get(parallelograms.size() - 1);
                        p.addPoint(e.getX(), e.getY());
                        if (p.npoints == 3) {
                            p.addPoint(p.xpoints[0] + p.xpoints[2] - p.xpoints[1], p.ypoints[0] + p.ypoints[2] - p.ypoints[1]);
                            editingParallelogram = false;
                            buildPanel.repaint();
                            BufferedImage textureImage = chooseImageFile();
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
                    if (objectToMove == null) {
                        int idx = 0;
                        for (PastedObject pastedObject : pastedObjects) {
                            Rectangle rect = new Rectangle(pastedObject.getX(), pastedObject.getY(), pastedObject.getWidth(), pastedObject.getHeight());
                            if (rect.contains(e.getX(), e.getY()) && pastedObject.isOpaque(e.getX() - rect.x, e.getY() - rect.y)) {
                                movingRectangle = rect;
                                objectToMove = pastedObject;
                                objectToMoveIdx = idx;
                                deltaX = e.getX() - objectToMove.getX();
                                deltaY = e.getY() - objectToMove.getY();
                            }
                            idx++;
                        }
                        if (e.isControlDown() && objectToMove != null) {
                            objectToMove = new PastedObject(objectToMove.getLocation(), objectToMove.getImage());
                            pastedObjects.add(objectToMove);
                        }
                    } else {
                        objectToMove = null;
                        movingRectangle = null;
                    }
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition.move(e.getX(), e.getY());
                if (objectToMove != null) {
                    objectToMove.setLocation(new Point(e.getX() - deltaX, e.getY() - deltaY));
                    movingRectangle = new Rectangle(e.getX() - deltaX, e.getY() - deltaY, objectToMove.getWidth(), objectToMove.getHeight());
                }
                buildPanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (painting) {
                    updateBrushStroke(e);
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                brushRadius += e.getWheelRotation();
                buildBrushPreview();
                repaint();
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
        JScrollPane buildScrollPane = new JScrollPane(buildPanel);
        extractPanel.setLayout(new GridLayout());
        extractPanel.setBackground(Color.BLACK);
        JScrollPane extractScrollPane = new JScrollPane(extractPanel);
        tabPane.addTab("Build Area", buildScrollPane);
        tabPane.addTab("Extraction Area", extractScrollPane);

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        JButton newButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/new.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                String inputSize = JOptionPane.showInputDialog("Enter size: ", "5120x3840");
                if (inputSize != null) {
                    if (inputSize.matches("\\d+x\\d+")) {
                        String[] tokens = inputSize.split("x");
                        backgroundWidth = Integer.parseInt(tokens[0]);
                        backgroundheight = Integer.parseInt(tokens[1]);
                        buildBackgroundImage = new BufferedImage(backgroundWidth, backgroundheight, BufferedImage.TYPE_INT_RGB);
                        pastedObjects.clear();
                        objectToMove = null;
                        objectToMoveIdx = -1;
                        movingRectangle = null;
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
        menubar.add(newButton);

        JButton fillButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/background.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage textureImage = chooseImageFile();
                if (textureImage != null) {
                    for (int x = 0; x < buildBackgroundImage.getWidth(); x++) {
                        for (int y = 0; y < buildBackgroundImage.getHeight(); y++) {
                            buildBackgroundImage.setRGB(x, y, textureImage.getRGB(x % textureImage.getWidth(), y % textureImage.getHeight()));
                        }
                    }
                }
                repaint();
            }
        });
        fillButton.setMaximumSize(BUTTON_SIZE);
        fillButton.setToolTipText("Fill background with a seamless pattern image");
        menubar.add(fillButton);

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
                        pastedObjects = exportableArea.getPastedObjects();
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
        menubar.add(openButton);

        JButton openBackgroundButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-bg.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage chosenImageFile = chooseImageFile();
                if (chosenImageFile != null) {
                    if (tabPane.getSelectedComponent() == buildScrollPane) {
                        buildBackgroundImage = chosenImageFile;
                    }
                    if (tabPane.getSelectedComponent() == extractScrollPane) {
                        extractionBackgroundImage = chosenImageFile;
                        polygon.reset();
                    }
                    setExtendedState(Frame.MAXIMIZED_BOTH);
                    repaint();
                }
            }
        });
        openBackgroundButton.setMaximumSize(BUTTON_SIZE);
        openBackgroundButton.setToolTipText("Open a background image file");
        menubar.add(openBackgroundButton);

        JButton openBrushTextureButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/open-texture.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                brushTexture = chooseImageFile();
                buildBrushPreview();
            }
        });
        openBrushTextureButton.setMaximumSize(BUTTON_SIZE);
        openBrushTextureButton.setToolTipText("Choose texture for brush");
        menubar.add(openBrushTextureButton);

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
                    ExportableArea exportableArea = new ExportableArea(new ExportableImage(buildBackgroundImage), pastedObjects);
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
        menubar.add(saveButton);

        JButton exportButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-img.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("BMP Images", "bmp");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    BufferedImage imageToexport = new BufferedImage(buildPanel.getWidth(), buildPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
                    paintObjects(imageToexport.getGraphics());
                    boolean success;
                    try {
                        ImageIO.write(imageToexport, "bmp", chooser.getSelectedFile());
                        success = true;
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
        exportButton.setToolTipText("Export build area to a BMP image");
        menubar.add(exportButton);

        JButton tileSeamlessButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-texture.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                    extractPanel.repaint();
                    JFileChooser chooser = new JFileChooser(new File(System.getProperty(USER_HOME)));
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        boolean success;
                        try {
                            ImageIO.write(TileSeamless.createSeamlessTile(tile.getSubImage(extractionBackgroundImage)), "png", chooser.getSelectedFile());
                            success = true;
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
        menubar.add(tileSeamlessButton);

        JButton pasteFromButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/paste-from.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage choice = chooseImageFile();
                if (choice != null) {
                    PastedObject pastedObject = new PastedObject(mousePosition, new ExportableImage(choice));
                    pastedObjects.add(pastedObject);
                    objectToMove = pastedObject;
                    painting = false;
                    repaint();
                }
            }
        });
        menubar.add(pasteFromButton);
        pasteFromButton.setMaximumSize(BUTTON_SIZE);
        pasteFromButton.setToolTipText("Paste from an image file");

        JButton parallelogramButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/parallelogram.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editingParallelogram = true;
            }
        });
        parallelogramButton.setMaximumSize(BUTTON_SIZE);
        parallelogramButton.setToolTipText("Draw and fill a new parallelogram");
        menubar.add(parallelogramButton);

        JButton polygonButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/polygon.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editingPolygon = true;
                if (tabPane.getSelectedComponent() == buildScrollPane) {
                    tabPane.setSelectedComponent(extractScrollPane);
                }
            }
        });
        polygonButton.setMaximumSize(BUTTON_SIZE);
        polygonButton.setToolTipText("Polygon selection");
        menubar.add(polygonButton);

        JButton brushButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/pencil.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                painting = true;
            }
        });
        brushButton.setMaximumSize(BUTTON_SIZE);
        brushButton.setToolTipText("Use texture brush");
        menubar.add(brushButton);

        JButton cursorButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/cursor.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                painting = false;
            }
        });
        cursorButton.setMaximumSize(BUTTON_SIZE);
        cursorButton.setToolTipText("Select objects");
        menubar.add(cursorButton);

        JButton paint3dButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/paint3d.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isValidTileSetup()) {
                    try {
                        File tempFile = File.createTempFile("j2darea", ".png");
                        ImageIO.write(tile.getSubImage(extractionBackgroundImage), "png", tempFile);
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
        menubar.add(paint3dButton);

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
        menubar.add(subtractBackgroundButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabPane, BorderLayout.CENTER);
        setSize(MIN_SIZE);
        setMinimumSize(MIN_SIZE);
        setVisible(true);
    }

    private BufferedImage chooseImageFile() {
        FileDialog chooser = new FileDialog(this, "Choose a file", FileDialog.LOAD);
        chooser.setDirectory(System.getProperty(USER_HOME));
        chooser.setFilenameFilter((dir, name) -> IMG_FILE_PATTERN.matcher(name).matches());
        chooser.setVisible(true);

        String returnVal = chooser.getFile();
        if (returnVal != null) {
            try {
                return ImageIO.read(new File(returnVal));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error opening image.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private void paintObjects(Graphics g) {
        if (buildBackgroundImage != null) {
            g.drawImage(buildBackgroundImage, 0, 0, null);
        }
        if (painting && brushPreview != null) {
            g.drawImage(brushPreview, mousePosition.x - brushRadius, mousePosition.y - brushRadius, null);
        }
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.drawImage(g);
        }
    }

    private boolean isValidTileSetup() {
        return extractionBackgroundImage != null && polygon.npoints == 0 && !tile.isEmpty();
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
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(J2DArea::new);
    }

}
