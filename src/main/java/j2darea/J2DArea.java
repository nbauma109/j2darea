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
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class J2DArea extends JFrame {

    private static final long serialVersionUID = 1L;

    private BufferedImage buildBackgroundImage, extractionBackgroundImage;
    private Polygon polygon = new Polygon();
    private Rectangle rectangle;
    private int mouseX;
    private int mouseY;
    private List<Entry<BufferedImage, Point>> pastedObjects = new ArrayList<>();
    private Entry<BufferedImage, Point> objectToMove;
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
                if (buildBackgroundImage != null) {
                    g.drawImage(buildBackgroundImage, 0, 0, null);
                }
                for (Entry<BufferedImage, Point> pastedObjectEntry : pastedObjects) {
                    g.drawImage(pastedObjectEntry.getKey(), (int) pastedObjectEntry.getValue().getX(), (int) pastedObjectEntry.getValue().getY(), null);
                }
                g.setColor(Color.GREEN);
                for (Polygon parallelogram : parallelograms) {
                    if (parallelogram.npoints < 3) {
                        Polygon newPolygon = new Polygon(parallelogram.xpoints, parallelogram.ypoints, parallelogram.npoints);
                        newPolygon.addPoint(mouseX, mouseY);
                        g.drawPolygon(newPolygon);
                    } else {
                        g.drawPolygon(parallelogram);
                    }
                }
                if (rectangle != null) {
                    g.drawRect((int) rectangle.getLocation().getX(), (int) rectangle.getLocation().getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
                }
            }

            @Override
            public Dimension getPreferredSize() {
                if (buildBackgroundImage != null) {
                    return new Dimension(buildBackgroundImage.getWidth(), buildBackgroundImage.getHeight());
                }
                return new Dimension(300, 200);
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
                newPolygon.addPoint(mouseX, mouseY);
                g.drawPolygon(newPolygon);
            }

            @Override
            public Dimension getPreferredSize() {
                if (extractionBackgroundImage != null) {
                    return new Dimension(extractionBackgroundImage.getWidth(), extractionBackgroundImage.getHeight());
                }
                return new Dimension(300, 200);
            }
        };

        extractPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        Rectangle rect = polygon.getBounds();
                        int width = (int) Math.round(rect.getWidth());
                        int height = (int) Math.round(rect.getHeight());
                        BufferedImage imageFromSelection = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {
                                if (polygon.contains(rect.getMinX() + x, rect.getMinY() + y)) {
                                    imageFromSelection.setRGB(x, y, extractionBackgroundImage.getRGB((int) Math.round(rect.getMinX() + x), (int) Math.round(rect.getMinY() + y)));
                                } else {
                                    imageFromSelection.setRGB(x, y, 0);
                                }
                            }
                        }
                        try {
                            ImageIO.write(imageFromSelection, "png", chooser.getSelectedFile());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    polygon.reset();
                } else {
                    polygon.addPoint(e.getX(), e.getY());
                }
                extractPanel.repaint();
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
                            BufferedImage floorImage = new BufferedImage((int) p.getBounds().getWidth(), (int) p.getBounds().getHeight(), BufferedImage.TYPE_INT_ARGB);
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
                                pastedObjects.add(0, new SimpleEntry<>(floorImage, p.getBounds().getLocation()));
                            } else {
                                pastedObjects.add(new SimpleEntry<>(floorImage, p.getBounds().getLocation()));
                            }
                            parallelograms.remove(parallelograms.size() - 1);
                        }
                    }
                } else {
                    if (objectToMove == null) {
                        int idx = 0;
                        for (Entry<BufferedImage, Point> pastedObjectEntry : pastedObjects) {
                            Rectangle rect = new Rectangle((int) pastedObjectEntry.getValue().getX(), (int) pastedObjectEntry.getValue().getY(),
                                    pastedObjectEntry.getKey().getWidth(), pastedObjectEntry.getKey().getHeight());
                            if (rect.contains(e.getX(), e.getY())) {
                                rectangle = rect;
                                objectToMove = pastedObjectEntry;
                                objectToMoveIdx = idx;
                                deltaX = (int) (e.getX() - objectToMove.getValue().getX());
                                deltaY = (int) (e.getY() - objectToMove.getValue().getY());
                            }
                            idx++;
                        }
                        if (e.isControlDown()) {
                            objectToMove = new SimpleEntry<>(objectToMove.getKey(), objectToMove.getValue());
                            pastedObjects.add(objectToMove);
                        }
                    } else {
                        objectToMove = null;
                        rectangle = null;
                    }
                }
                buildPanel.repaint();
            }
        });
        buildPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                if (objectToMove != null) {
                    objectToMove.setValue(new Point(e.getX() - deltaX, e.getY() - deltaY));
                    rectangle = new Rectangle(e.getX() - deltaX, e.getY() - deltaY, objectToMove.getKey().getWidth(), objectToMove.getKey().getHeight());
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
                    rectangle = null;
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
                    if (objectToMoveIdx < pastedObjects.size() - 1) {
                        Entry<BufferedImage, Point> tmp = pastedObjects.get(objectToMoveIdx + 1);
                        pastedObjects.set(objectToMoveIdx + 1, objectToMove);
                        pastedObjects.set(objectToMoveIdx, tmp);
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
                    if (objectToMoveIdx > 0) {
                        Entry<BufferedImage, Point> tmp = pastedObjects.get(objectToMoveIdx - 1);
                        pastedObjects.set(objectToMoveIdx - 1, objectToMove);
                        pastedObjects.set(objectToMoveIdx, tmp);
                    }
                    buildPanel.repaint();
                }
            }
        });

        extractPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                extractPanel.repaint();
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
        JMenu fileMenu = new JMenu("File");
        menubar.add(fileMenu);
        fileMenu.add(new JMenuItem(new AbstractAction("Open") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (tabPane.getSelectedComponent() == buildScrollPane) {
                    buildBackgroundImage = chooseImageFile();
                }
                if (tabPane.getSelectedComponent() == extractScrollPane) {
                    extractionBackgroundImage = chooseImageFile();
                    polygon.reset();
                }
                J2DArea.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                J2DArea.this.repaint();
            }
        }));
        JMenu editMenu = new JMenu("Edit");
        menubar.add(editMenu);
        editMenu.add(new JMenuItem(new AbstractAction("Paste From") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                pastedObjects.add(new SimpleEntry<>(chooseImageFile(), new Point(0, 0)));
                J2DArea.this.repaint();
            }
        }));
        editMenu.add(new JMenuItem(new AbstractAction("Parallelogram") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                editingParallelogram = true;
            }
        }));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabPane, BorderLayout.CENTER);
        setSize(500, 500);
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
            try {
                return ImageIO.read(chooser.getSelectedFile());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new J2DArea();
        });
    }

}
