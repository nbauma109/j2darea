package com.github.nbauma109.j2darea;

import static com.github.nbauma109.j2darea.J2DArea.BUTTON_SIZE;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class PolygonSelectionView extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient BGSubtracter bgSubtracter;
    private final transient BufferedImage previewImage;

    public PolygonSelectionView(BufferedImage image, java.awt.Polygon relativePolygon) {
        setTitle("Polygon preview");
        this.bgSubtracter = new BGSubtracter(image, relativePolygon);
        this.bgSubtracter.subtractBackground();
        this.previewImage = bgSubtracter.getPreviewImage();

        JPanel previewPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D graphics2d = (Graphics2D) g.create();
                try {
                    TransparencyPreviewPainter.paintCheckerboard(graphics2d, 0, 0, previewImage.getWidth(), previewImage.getHeight());
                    graphics2d.drawImage(previewImage, 0, 0, null);
                } finally {
                    graphics2d.dispose();
                }
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(previewImage.getWidth(), previewImage.getHeight());
            }
        };

        add(previewPanel);

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

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
                    success = J2DArea.writeImage(file, bgSubtracter.getPreviewImage());
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

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
