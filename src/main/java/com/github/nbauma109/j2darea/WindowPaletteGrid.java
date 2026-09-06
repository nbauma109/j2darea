package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Five-by-three visual selector for window frame, glass, curtain and trim colours. */
final class WindowPaletteGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel selectedName = new JLabel();
    private WindowPalette selectedPalette;

    WindowPaletteGrid(WindowPalette selected, WindowPalette automaticPreview,
            Consumer<WindowPalette> listener) {
        super(new BorderLayout(0, 4));
        selectedPalette = selected != null ? selected : WindowPalette.AUTO;
        JPanel swatches = new JPanel(new GridLayout(0, 5, 4, 4));
        ButtonGroup group = new ButtonGroup();
        for (WindowPalette palette : WindowPalette.values()) {
            WindowPalette visible = palette == WindowPalette.AUTO ? automaticPreview : palette;
            JToggleButton button = new JToggleButton(new PaletteIcon(visible,
                palette == WindowPalette.AUTO));
            button.setSelected(palette == selectedPalette);
            button.setPreferredSize(new Dimension(43, 30));
            button.setMargin(new java.awt.Insets(2, 2, 2, 2));
            button.setToolTipText(label(palette, automaticPreview));
            button.getAccessibleContext().setAccessibleName(button.getToolTipText());
            button.addActionListener(event -> {
                selectedPalette = palette;
                selectedName.setText(label(palette, automaticPreview));
                listener.accept(palette);
            });
            group.add(button);
            swatches.add(button);
        }
        selectedName.setText(label(selectedPalette, automaticPreview));
        selectedName.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        add(swatches, BorderLayout.CENTER);
        add(selectedName, BorderLayout.SOUTH);
    }

    int getSwatchCount() { return WindowPalette.values().length; }
    WindowPalette getSelectedPalette() { return selectedPalette; }

    private static String label(WindowPalette palette, WindowPalette automaticPreview) {
        if (palette != WindowPalette.AUTO || automaticPreview == null) return palette.getDisplayName();
        return palette.getDisplayName() + " (" + automaticPreview.getDisplayName() + ")";
    }

    private static final class PaletteIcon implements Icon {
        private final WindowPalette palette;
        private final boolean automatic;

        private PaletteIcon(WindowPalette palette, boolean automatic) {
            this.palette = palette;
            this.automatic = automatic;
        }

        @Override public int getIconWidth() { return 34; }
        @Override public int getIconHeight() { return 20; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            WindowPalette visible = palette != null ? palette : WindowPalette.DARK_OAK_RED;
            Color[] colors = { visible.getFrame(), visible.getGlass(), visible.getCurtain(), visible.getTrim() };
            for (int i = 0; i < colors.length; i++) {
                int left = x + (i * 17 / 2);
                int right = x + ((i + 1) * 17 / 2);
                graphics.setColor(colors[i]);
                graphics.fillRect(left, y, right - left, 20);
            }
            graphics.setColor(new Color(24, 24, 24));
            graphics.drawRect(x, y, 33, 19);
            if (automatic) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("A", x + 2, y + 14);
            }
        }
    }
}
