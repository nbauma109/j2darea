package j2darea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import static j2darea.J2DArea.BUTTON_SIZE;

public class BGSubtracterPreview extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int ERASER_MAX_SIZE = 16;
    private static final int ERASER_MIN_SIZE = 2;

    private BGSubstracter bgSubstracter;
    private int mouseX, mouseY;
    private int width, height;
    private int eraserSize;

    public BGSubtracterPreview(BufferedImage image, Polygon polygon) {
        eraserSize = 8;
        width = image.getWidth();
        height = image.getHeight();
        bgSubstracter = new BGSubstracter(image, polygon);
        bgSubstracter.substractBackground(1, 0, false, false);
        JPanel previewPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(bgSubstracter.getPreviewImage(), 0, 0, null);
                g.drawRect(mouseX - eraserSize / 2, mouseY - eraserSize / 2, eraserSize, eraserSize);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, height);
            }
        };

        previewPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                erase(e);
            }
        });

        previewPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                erase(e);
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

        ItemListener itemListener = new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                onChange(hueLimitSlider, satLimitSlider, hueCheckbox, satCheckbox);
                repaint();
            }
        };
        ChangeListener changeListener = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                onChange(hueLimitSlider, satLimitSlider, hueCheckbox, satCheckbox);
                repaint();
            }
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
        JButton eraserPlusButton = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/eraser+.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (eraserSize < ERASER_MAX_SIZE) {
                    eraserSize++;
                    repaint();
                }
            }

        });
        eraserPlusButton.setMaximumSize(BUTTON_SIZE);
        eraserPlusButton.setToolTipText("Increase eraser size");
        menubar.add(eraserPlusButton);
        JButton eraserMinus = new JButton(new AbstractAction(null, new ImageIcon(getClass().getResource("/icons/eraser-.png"))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (eraserSize > ERASER_MIN_SIZE) {
                    eraserSize--;
                    repaint();
                }
            }

        });
        eraserMinus.setMaximumSize(BUTTON_SIZE);
        eraserMinus.setToolTipText("Decrease eraser size");
        menubar.add(eraserMinus);
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
                        ImageIO.write(bgSubstracter.getPreviewImage(), "png", chooser.getSelectedFile());
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

    private JPanel createLabeledSliderPanel(JSlider slider, JLabel sliderLabel, JCheckBox checkbox) {
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
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, intialValue);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    public void onChange(JSlider hueLimitSlider, JSlider satLimitSlider, JCheckBox hueCheckbox, JCheckBox satCheckbox) {
        bgSubstracter.substractBackground(hueLimitSlider.getValue() / 100d, satLimitSlider.getValue() / 100d, hueCheckbox.isSelected(), satCheckbox.isSelected());
    }

    public void erase(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        for (int x = mouseX - eraserSize / 2; x < mouseX + eraserSize / 2; x++) {
            for (int y = mouseY - eraserSize / 2; y < mouseY + eraserSize / 2; y++) {
                if (x > 0 && y > 0 && x < width && y < height) {
                    bgSubstracter.getPreviewImage().setRGB(x, y, 0);
                }
            }
        }
        repaint();
    }

}
