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

/** Five-by-three colour grid for wallpaper background, motif and accent inks. */
final class WallpaperPaletteGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel selectedName = new JLabel();
    private WallpaperPalette selectedPalette;

    WallpaperPaletteGrid(WallpaperPalette selected, WallpaperPalette automaticPreview,
            Consumer<WallpaperPalette> listener) {
        super(new BorderLayout(0, 4));
        selectedPalette = selected != null ? selected : WallpaperPalette.AUTO;
        JPanel swatches = new JPanel(new GridLayout(0, 5, 4, 4));
        ButtonGroup group = new ButtonGroup();
        for (WallpaperPalette palette : WallpaperPalette.values()) {
            WallpaperPalette visible = palette == WallpaperPalette.AUTO ? automaticPreview : palette;
            JToggleButton button = new JToggleButton(new PaletteIcon(visible,
                palette == WallpaperPalette.AUTO));
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

    int getSwatchCount() { return WallpaperPalette.values().length; }
    WallpaperPalette getSelectedPalette() { return selectedPalette; }

    private static String label(WallpaperPalette palette, WallpaperPalette automaticPreview) {
        if (palette != WallpaperPalette.AUTO || automaticPreview == null) {
            return palette.getDisplayName();
        }
        return palette.getDisplayName() + " (" + automaticPreview.getDisplayName() + ")";
    }

    private static final class PaletteIcon implements Icon {

        private final WallpaperPalette palette;
        private final boolean automatic;

        private PaletteIcon(WallpaperPalette palette, boolean automatic) {
            this.palette = palette;
            this.automatic = automatic;
        }

        @Override public int getIconWidth() { return 34; }
        @Override public int getIconHeight() { return 20; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            WallpaperPalette visible = palette != null ? palette : WallpaperPalette.CREAM_UMBER;
            graphics.setColor(visible.getBackground());
            graphics.fillRect(x, y, 12, 20);
            graphics.setColor(visible.getMotif());
            graphics.fillRect(x + 12, y, 11, 20);
            graphics.setColor(visible.getAccent());
            graphics.fillRect(x + 23, y, 11, 20);
            graphics.setColor(new Color(24, 24, 24));
            graphics.drawRect(x, y, 33, 19);
            if (automatic) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("A", x + 3, y + 14);
            }
        }
    }
}
