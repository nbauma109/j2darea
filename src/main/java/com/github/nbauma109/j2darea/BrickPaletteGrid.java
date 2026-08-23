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
import javax.swing.SwingConstants;

/** Compact shadow/midtone/highlight swatches used by both masonry editors. */
final class BrickPaletteGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMNS = 5;

    private final JLabel selectedName = new JLabel();
    private BrickPalette selectedPalette;

    BrickPaletteGrid(BrickPalette selected, BrickPalette automaticPreview,
            Consumer<BrickPalette> selectionListener) {
        super(new BorderLayout(0, 4));
        selectedPalette = selected != null ? selected : BrickPalette.AUTO;

        JPanel swatches = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
        ButtonGroup group = new ButtonGroup();
        for (BrickPalette palette : BrickPalette.values()) {
            BrickPalette rendered = palette == BrickPalette.AUTO ? automaticPreview : palette;
            JToggleButton button = new JToggleButton(new PaletteIcon(rendered,
                palette == BrickPalette.AUTO));
            button.setSelected(palette == selectedPalette);
            button.setPreferredSize(new Dimension(43, 30));
            button.setMargin(new java.awt.Insets(2, 2, 2, 2));
            button.setToolTipText(paletteLabel(palette, automaticPreview));
            button.getAccessibleContext().setAccessibleName(button.getToolTipText());
            button.addActionListener(event -> {
                selectedPalette = palette;
                selectedName.setText(paletteLabel(palette, automaticPreview));
                selectionListener.accept(palette);
            });
            group.add(button);
            swatches.add(button);
        }

        selectedName.setHorizontalAlignment(SwingConstants.LEFT);
        selectedName.setText(paletteLabel(selectedPalette, automaticPreview));
        selectedName.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        add(swatches, BorderLayout.CENTER);
        add(selectedName, BorderLayout.SOUTH);
    }

    BrickPalette getSelectedPalette() {
        return selectedPalette;
    }

    int getSwatchCount() {
        return BrickPalette.values().length;
    }

    private static String paletteLabel(BrickPalette palette, BrickPalette automaticPreview) {
        if (palette != BrickPalette.AUTO || automaticPreview == null) {
            return palette.getDisplayName();
        }
        return palette.getDisplayName() + " (" + automaticPreview.getDisplayName() + ")";
    }

    private static final class PaletteIcon implements Icon {

        private static final int WIDTH = 34;
        private static final int HEIGHT = 20;

        private final BrickPalette palette;
        private final boolean automatic;

        private PaletteIcon(BrickPalette palette, boolean automatic) {
            this.palette = palette;
            this.automatic = automatic;
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            BrickPalette visible = palette != null ? palette : BrickPalette.ASH_GRAY;
            int first = WIDTH / 3;
            int second = (WIDTH * 2) / 3;
            graphics.setColor(visible.getDark());
            graphics.fillRect(x, y, first, HEIGHT);
            graphics.setColor(visible.getMiddle());
            graphics.fillRect(x + first, y, second - first, HEIGHT);
            graphics.setColor(visible.getLight());
            graphics.fillRect(x + second, y, WIDTH - second, HEIGHT);
            graphics.setColor(new Color(24, 24, 24));
            graphics.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
            if (automatic) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("A", x + 3, y + 14);
            }
        }
    }
}
