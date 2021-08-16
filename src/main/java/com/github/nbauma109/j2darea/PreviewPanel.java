package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PreviewPanel extends JPanel implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private ImagePreviewer previewer;

    public PreviewPanel() {
        previewer = new ImagePreviewer();

        setPreferredSize(new Dimension(150, 150));
        setBorder(BorderFactory.createEtchedBorder());

        setLayout(new BorderLayout());

        add(previewer, BorderLayout.CENTER);
    }

    public ImagePreviewer getImagePreviewer() {
        return previewer;
    }

    class ImagePreviewer extends JLabel {

        private static final long serialVersionUID = 1L;

        public void configure(File f) {
            Dimension size = getSize();
            Insets insets = getInsets();
            ImageIcon icon = new ImageIcon(f.getPath());

            int width = size.width - insets.left - insets.right;
            int height = size.height - insets.top - insets.bottom;
            setIcon(new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH)));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {

        if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File f = (File) e.getNewValue();
            if (f != null && f.isFile()) {
                String s = f.getPath();
                String suffix = null;
                int i = s.lastIndexOf('.');

                if (i > 0 && i < s.length() - 1) {
                    suffix = s.substring(i + 1).toLowerCase();

                    if (suffix.equals("gif") || suffix.equals("png") || suffix.equals("jpg")) {
                        getImagePreviewer().configure(f);
                    }
                }
            }

        }
    }
}