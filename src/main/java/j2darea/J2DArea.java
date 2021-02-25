package j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class J2DArea extends JFrame {

    public static final Dimension BUTTON_SIZE = new Dimension(25, 25);

    private static final long serialVersionUID = 1L;

    private int backgroundWidth = 5120;
    private int backgroundheight = 3840;
    private BufferedImage buildBackgroundImage = new BufferedImage(backgroundWidth, backgroundheight, BufferedImage.TYPE_INT_RGB);
    private BufferedImage extractionBackgroundImage;
    private Polygon polygon = new Polygon();
    private Rectangle movingRectangle;
    private Tile tile = new Tile();
    private Point mousePosition = new Point();
    private List<PastedObject> pastedObjects = new ArrayList<>();
    private PastedObject objectToMove;
    private int objectToMoveIdx = -1;
    private int deltaX, deltaY;
    private boolean editingParallelogram;
    private List<Polygon> parallelograms = new ArrayList<>();

    public J2DArea() {
        super("J2DArea");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabPane = new JTabbedPane(JTabbedPane.BOTTOM);
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
                }
                g.setColor(Color.GREEN);
                Polygon newPolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                newPolygon.addPoint(mousePosition.x, mousePosition.y);
                g.drawPolygon(newPolygon);
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
                tile.getEndPoint().move(e.getX(), e.getY());
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
                tile.getStartPoint().move(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                tile.getEndPoint().move(e.getX(), e.getY());
                if (isValidTileSetup()) {
                    extractPanel.repaint();
                    JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                    chooser.setFileFilter(filter);
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        BufferedImage imageFromSelection = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_INT_RGB);
                        for (int x = 0; x < tile.getWidth(); x++) {
                            for (int y = 0; y < tile.getHeight(); y++) {
                                imageFromSelection.setRGB(x, y, extractionBackgroundImage.getRGB(x + tile.getX(), y + tile.getY()));
                            }
                        }
                        boolean success;
                        try {
                            ImageIO.write(TileSeamless.createSeamlessTile(imageFromSelection), "png", chooser.getSelectedFile());
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
                            JOptionPane.showMessageDialog(null, "Image save failed.", "Error", JOptionPane.ERROR_MESSAGE);
                        }

                    } else {
                        tile.reset();
                        extractPanel.repaint();
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (extractionBackgroundImage != null) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int choice = JOptionPane.showOptionDialog(null, "Remove background", "Remove the remaining background inside the polygon ?", JOptionPane.DEFAULT_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, new Object[] { "Yes", "No" }, "No");
                        if (choice == 0) {
                            Rectangle r = polygon.getBounds();
                            Polygon relativePolygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                            relativePolygon.translate(-r.x, -r.y);
                            new BGSubtracterPreview(extractionBackgroundImage.getSubimage(r.x, r.y, r.width, r.height), relativePolygon);
                        } else {
                            JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
                            FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                            chooser.setFileFilter(filter);
                            int returnVal = chooser.showSaveDialog(null);
                            if (returnVal == JFileChooser.APPROVE_OPTION) {
                                Rectangle rect = polygon.getBounds();
                                BufferedImage imageFromSelection = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
                                for (int x = 0; x < rect.width; x++) {
                                    for (int y = 0; y < rect.height; y++) {
                                        int xx = rect.x + x;
                                        int yy = rect.y + y;
                                        if (polygon.contains(xx, yy)) {
                                            imageFromSelection.setRGB(x, y, extractionBackgroundImage.getRGB(xx, yy));
                                        } else {
                                            imageFromSelection.setRGB(x, y, 0);
                                        }
                                    }
                                }
                                boolean success;
                                try {
                                    ImageIO.write(imageFromSelection, "png", chooser.getSelectedFile());
                                    success = true;
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
                        }
                        polygon.reset();
                    } else {
                        polygon.addPoint(e.getX(), e.getY());
                    }
                    extractPanel.repaint();
                }
            }
        });

        buildPanel.addMouseListener(new MouseAdapter() {
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
                        if (e.isControlDown()) {
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
        });
        buildPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition.move(e.getX(), e.getY());
                if (objectToMove != null) {
                    objectToMove.setLocation(new Point(e.getX() - deltaX, e.getY() - deltaY));
                    movingRectangle = new Rectangle(e.getX() - deltaX, e.getY() - deltaY, objectToMove.getWidth(), objectToMove.getHeight());
                }
                buildPanel.repaint();
            }
        });

        buildPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
        buildPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "Plus");
        buildPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK), "Plus");
        buildPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "Minus");
        buildPanel.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0), "Minus");
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
        buildPanel.getActionMap().put("Plus", new AbstractAction() {

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
        buildPanel.getActionMap().put("Minus", new AbstractAction() {

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
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                        repaint();
                    } else {
                        JOptionPane.showMessageDialog(null, "Bad input", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        newButton.setMaximumSize(BUTTON_SIZE);
        newButton.setToolTipText("Create a new area");
        menubar.add(newButton);

        JButton fillButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/stone.png"))) {

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
                JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("J2DArea project files", "j2da");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    boolean success;
                    try (FileInputStream fileInputStream = new FileInputStream(chooser.getSelectedFile())) {
                        try (GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
                            try (ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream)) {
                                ExportableArea exportableArea = new ExportableArea();
                                exportableArea.readExternal(objectInputStream);
                                buildBackgroundImage = exportableArea.getBackgroundImage().getImage();
                                pastedObjects = exportableArea.getPastedObjects();
                                setExtendedState(JFrame.MAXIMIZED_BOTH);
                                repaint();
                            } catch (ClassNotFoundException ex) {
                                ex.printStackTrace();
                                success = false;
                            }
                        }
                        success = true;
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        success = false;
                    }
                    if (!success) {
                        JOptionPane.showMessageDialog(null, "Error opening file.", "Error", JOptionPane.ERROR_MESSAGE);
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
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                    repaint();
                }
            }
        });
        openBackgroundButton.setMaximumSize(BUTTON_SIZE);
        openBackgroundButton.setToolTipText("Open a background image file");
        menubar.add(openBackgroundButton);

        JButton saveButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home")));
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
                        JOptionPane.showMessageDialog(null, "Project save failed.", "Error", JOptionPane.ERROR_MESSAGE);
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
                JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    BufferedImage imageToexport = new BufferedImage(buildPanel.getWidth(), buildPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
                    paintObjects(imageToexport.getGraphics());
                    boolean success;
                    try {
                        ImageIO.write(imageToexport, "png", chooser.getSelectedFile());
                        success = true;
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
            }
        });
        exportButton.setMaximumSize(BUTTON_SIZE);
        exportButton.setToolTipText("Export build area to a PNG image");
        menubar.add(exportButton);

        JButton pasteFromButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/paste-from.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage choice = chooseImageFile();
                if (choice != null) {
                    pastedObjects.add(new PastedObject(new Point(0, 0), new ExportableImage(choice)));
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

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabPane, BorderLayout.CENTER);
        setSize(500, 500);
        setMinimumSize(new Dimension(300, 300));
        setVisible(true);
    }

    private BufferedImage chooseImageFile() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
        PreviewPanel previewPanel = new PreviewPanel();
        chooser.setAccessory(previewPanel);
        chooser.addPropertyChangeListener(previewPanel);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GIF, JPG & PNG Images", "gif", "jpg", "png");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            boolean success;
            try {
                BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                success = true;
                return img;
            } catch (IOException ex) {
                ex.printStackTrace();
                success = false;
            }
            if (!success) {
                JOptionPane.showMessageDialog(null, "Error opening image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    private void paintObjects(Graphics g) {
        if (buildBackgroundImage != null) {
            g.drawImage(buildBackgroundImage, 0, 0, null);
        }
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.drawImage(g);
        }
    }

    private boolean isValidTileSetup() {
        return extractionBackgroundImage != null && polygon.npoints == 0 && !tile.isEmpty();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new J2DArea();
        });
    }

}
