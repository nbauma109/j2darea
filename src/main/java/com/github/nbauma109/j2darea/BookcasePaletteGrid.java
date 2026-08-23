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

/** Five-by-three visual selector for bookcase wood and book-cloth schemes. */
final class BookcasePaletteGrid extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel selectedName = new JLabel();
    private BookcasePalette selectedPalette;

    BookcasePaletteGrid(BookcasePalette selected, BookcasePalette automaticPreview,
            Consumer<BookcasePalette> listener) {
        super(new BorderLayout(0, 4));
        selectedPalette = selected != null ? selected : BookcasePalette.AUTO;
        JPanel swatches = new JPanel(new GridLayout(0, 5, 4, 4));
        ButtonGroup group = new ButtonGroup();
        for (BookcasePalette palette : BookcasePalette.values()) {
            BookcasePalette visible = palette == BookcasePalette.AUTO ? automaticPreview : palette;
            JToggleButton button = new JToggleButton(new PaletteIcon(visible,
                palette == BookcasePalette.AUTO));
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

    int getSwatchCount() { return BookcasePalette.values().length; }
    BookcasePalette getSelectedPalette() { return selectedPalette; }

    private static String label(BookcasePalette palette, BookcasePalette automaticPreview) {
        if (palette != BookcasePalette.AUTO || automaticPreview == null) return palette.getDisplayName();
        return palette.getDisplayName() + " (" + automaticPreview.getDisplayName() + ")";
    }

    private static final class PaletteIcon implements Icon {
        private final BookcasePalette palette;
        private final boolean automatic;

        private PaletteIcon(BookcasePalette palette, boolean automatic) {
            this.palette = palette;
            this.automatic = automatic;
        }

        @Override public int getIconWidth() { return 34; }
        @Override public int getIconHeight() { return 20; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            BookcasePalette visible = palette != null ? palette : BookcasePalette.DARK_OAK;
            graphics.setColor(visible.getBacking());
            graphics.fillRect(x, y, 34, 20);
            graphics.setColor(visible.getWood());
            graphics.fillRect(x, y, 8, 20);
            for (int i = 0; i < 4; i++) {
                graphics.setColor(visible.getBookColor(i));
                graphics.fillRect(x + 9 + (i * 6), y + 4 + (i % 2) * 3, 5, 16 - (i % 2) * 3);
            }
            graphics.setColor(visible.getTrim());
            graphics.drawLine(x + 8, y + 18, x + 33, y + 18);
            graphics.setColor(new Color(20, 18, 15));
            graphics.drawRect(x, y, 33, 19);
            if (automatic) {
                graphics.setColor(Color.WHITE);
                graphics.drawString("A", x + 1, y + 14);
            }
        }
    }
}
