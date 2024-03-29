package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import static com.github.nbauma109.j2darea.J2DArea.BUTTON_SIZE;

public class BGSubtracterPreview extends JFrame {

    private static final long serialVersionUID = 1L;

    private transient BGSubtracter bgSubtracter;
    private int mouseX;
    private int mouseY;
    private int previewWidth;
    private int previewHeight;
    private int eraserSize;

    public BGSubtracterPreview(BufferedImage image) {
        this(image, null);
    }

    public BGSubtracterPreview(BufferedImage image, Polygon polygon) {
        setTitle("Background subtracter preview");
        eraserSize = 8;
        previewWidth = image.getWidth();
        previewHeight = image.getHeight();
        bgSubtracter = new BGSubtracter(image, polygon);
        bgSubtracter.subtractBackground(1, 0, false, false);
        JPanel previewPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(bgSubtracter.getPreviewImage(), 0, 0, null);
                g.drawRect(mouseX - eraserSize / 2, mouseY - eraserSize / 2, eraserSize, eraserSize);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(previewWidth, previewHeight);
            }
        };

        previewPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    restore(e);
                } else {
                    erase(e);
                }
            }
        });

        previewPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    restore(e);
                } else {
                    erase(e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

        });

        JSlider hueLimitSlider = newSlider(100);
        JSlider satLimitSlider = newSlider(0);
        JCheckBox hueCheckbox = new JCheckBox("Invert");
        JCheckBox satCheckbox = new JCheckBox("Invert");

        ItemListener itemListener = e -> {
            onChange(hueLimitSlider, satLimitSlider, hueCheckbox, satCheckbox);
            repaint();
        };
        ChangeListener changeListener = e -> {
            onChange(hueLimitSlider, satLimitSlider, hueCheckbox, satCheckbox);
            repaint();
        };

        hueCheckbox.addItemListener(itemListener);
        satCheckbox.addItemListener(itemListener);
        hueLimitSlider.addChangeListener(changeListener);
        satLimitSlider.addChangeListener(changeListener);

        setLayout(new BorderLayout());
        add(previewPanel, BorderLayout.CENTER);
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridLayout(1, 2));
        sliderPanel.add(createLabeledSliderPanel(hueLimitSlider, new JLabel("Hue"), hueCheckbox));
        sliderPanel.add(createLabeledSliderPanel(satLimitSlider, new JLabel("Saturation"), satCheckbox));
        add(sliderPanel, BorderLayout.EAST);
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        
        addMouseWheelListener(e -> {
            eraserSize += e.getWheelRotation();
            repaint();
        });
        
        JButton exportButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/save-img.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                File file = J2DArea.chooseFile(BGSubtracterPreview.this, FileDialog.SAVE, FileChooserLocation.OBJECT);
                if (file != null) {
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
            }
        });
        exportButton.setMaximumSize(BUTTON_SIZE);
        exportButton.setToolTipText("Save to a PNG image");
        menubar.add(exportButton);

        pack();
        setVisible(true);
    }

    private static JPanel createLabeledSliderPanel(JSlider slider, JLabel sliderLabel, JCheckBox checkbox) {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.add(slider);
        sliderPanel.add(sliderLabel);
        sliderPanel.add(checkbox);
        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sliderPanel;
    }

    public JSlider newSlider(int intialValue) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 100, intialValue);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    public void onChange(JSlider hueLimitSlider, JSlider satLimitSlider, JCheckBox hueCheckbox, JCheckBox satCheckbox) {
        bgSubtracter.subtractBackground(hueLimitSlider.getValue() / 100d, satLimitSlider.getValue() / 100d, hueCheckbox.isSelected(), satCheckbox.isSelected());
    }

    public void erase(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        for (int x = mouseX - eraserSize / 2; x < mouseX + eraserSize / 2; x++) {
            for (int y = mouseY - eraserSize / 2; y < mouseY + eraserSize / 2; y++) {
                if (x >= 0 && y >= 0 && x < previewWidth && y < previewHeight) {
                    bgSubtracter.getPreviewImage().setRGB(x, y, 0);
                }
            }
        }
        repaint();
    }

    public void restore(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        for (int x = mouseX - eraserSize / 2; x < mouseX + eraserSize / 2; x++) {
            for (int y = mouseY - eraserSize / 2; y < mouseY + eraserSize / 2; y++) {
                if (x > 0 && y > 0 && x < previewWidth && y < previewHeight) {
                    bgSubtracter.getPreviewImage().setRGB(x, y, bgSubtracter.getOriginalImage().getRGB(x, y));
                }
            }
        }
        repaint();
    }

}
