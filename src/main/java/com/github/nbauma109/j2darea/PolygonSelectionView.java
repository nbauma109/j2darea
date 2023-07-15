package com.github.nbauma109.j2darea;

import static com.github.nbauma109.j2darea.J2DArea.BUTTON_SIZE;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PolygonSelectionView extends JFrame {

    private static final long serialVersionUID = 1L;

    private transient BGSubtracter bgSubtracter;
    private int previewWidth;
    private int previewHeight;

    public PolygonSelectionView(BufferedImage image, Polygon relativePolygon, Point location) {
        setTitle("Polygon selection view");
        previewWidth = image.getWidth();
        previewHeight = image.getHeight();
        bgSubtracter = new BGSubtracter(image, relativePolygon);
        bgSubtracter.subtractBackground(1, 0, false, false);
        JPanel previewPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(bgSubtracter.getPreviewImage(), 0, 0, null);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(previewWidth, previewHeight);
            }
        };

        previewPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                JEditorPane scriptPane = new JEditorPane();
                scriptPane.setFont(new Font("Consolas", Font.PLAIN, 12));
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                logPolygon(relativePolygon, location, pw);
                logPoint(e, location, pw);
                scriptPane.setText(sw.toString());
                JFrame scriptFrame = new JFrame("Weidu script");
                scriptFrame.add(scriptPane);
                scriptFrame.setVisible(true);
                scriptFrame.setSize(400, 500);
                scriptFrame.setLocationRelativeTo(null);
            }
        });

        add(previewPanel);
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JButton exportButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-img.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.home"), "Pictures"));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    boolean success;
                    try {
                        ImageIO.write(bgSubtracter.getPreviewImage(), "png", chooser.getSelectedFile());
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
        exportButton.setToolTipText("Save to a PNG image");
        menubar.add(exportButton);

        pack();
        setVisible(true);
    }
    
    private static void logPolygon(Polygon polygon, Point location, PrintWriter out) {
        Rectangle r = polygon.getBounds();
        out.printf("  LPF fj_are_structure%n");
        out.printf("    INT_VAR%n");
        out.printf("    fj_type              = 2%n");
        out.printf("    fj_box_left          = %d%n", location.x + r.x);
        out.printf("    fj_box_top           = %d%n", location.y + r.y);
        out.printf("    fj_box_right         = %d%n", location.x + r.x + r.width);
        out.printf("    fj_box_bottom        = %d%n", location.y + r.y + r.height);
        out.printf("    fj_cursor_idx        = %d%n", 34);
        for (int i = 0; i < polygon.npoints; i++) {
            int x = location.x + polygon.xpoints[i];
            int y = location.y + polygon.ypoints[i];
            out.printf("    fj_vertex_%d          = %d + (%d << 16)%n", i, x, y);
        }
        out.println("    STR_VAR");
        out.println("    fj_structure_type    = region");
        out.println("    fj_name              = ......");
        out.println("    fj_destination_area  = ......");
        out.println("    fj_destination_name  = ......");
        out.println("  END");

    }

    private static void logPoint(MouseEvent e, Point location, PrintWriter out) {
        out.println("  LPF fj_are_structure");
        out.println("    INT_VAR");
        out.println("    fj_loc_x             = " + (location.x + e.getX()));
        out.println("    fj_loc_y             = " + (location.y + e.getY()));
        out.println("    fj_orientation       = 2");
        out.println("    STR_VAR");           
        out.println("    fj_structure_type    = entrance");
        out.println("    fj_name              = ......");
        out.println("  END");
    }

}
