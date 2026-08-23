package com.github.nbauma109.j2darea;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * A lit ring of choices that opens under the pointer: the radial selector of a
 * science-fiction console rather than a row of buttons in a message box.
 *
 * <p>It is a compass as much as a menu. The choices sit at its bearings, the
 * outer ring carries its cardinals and its ticks, and the hub reads back whatever
 * the pointer is currently over. Picking an option is one throw of the mouse from
 * wherever the click that opened it landed, which a message box in the middle of
 * the screen never is.
 *
 * <p>Nothing on it moves. A selector is read once and answered immediately, so
 * anything that pulses or sweeps under the pointer is a distraction during the
 * one second the thing is on screen, and it costs a repaint of a translucent
 * window every frame to be one. The only thing that redraws is the choice under
 * the pointer, and only when it actually changes.
 *
 * <p>The dialog is modal and returns the index of the chosen option, or
 * {@link #CANCELLED} when the hub, the space outside the ring, or Escape ended
 * it instead.
 */
public class RadialMenuDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** Returned when the user dismissed the selector without choosing. */
    public static final int CANCELLED = -1;

    private static final int DIAMETER = 440;
    private static final double OUTER_RADIUS = 208d;
    private static final double RING_OUTER_RADIUS = 186d;
    private static final double RING_INNER_RADIUS = 104d;
    private static final double HUB_RADIUS = 82d;
    /** Angular gap left between two neighbouring segments, in degrees. */
    private static final double SEGMENT_GAP = 5d;

    private static final Color GLOW = new Color(90, 240, 255);
    private static final Color GLOW_DIM = new Color(52, 132, 148);
    private static final Color ACCENT = new Color(255, 108, 205);
    private static final Color BACKDROP_INNER = new Color(9, 24, 34, 236);
    private static final Color BACKDROP_OUTER = new Color(2, 7, 11, 232);

    private final transient List<Option> options;
    private final String title;

    private int hoveredIndex = CANCELLED;
    private int chosenIndex = CANCELLED;

    /**
     * One choice on the ring.
     *
     * @param label read on the ring, in capitals
     * @param detail one line read back in the hub while the choice is under the pointer
     * @param symbol drawn above the label, or {@code null} for none
     */
    public static final class Option {

        private final String label;
        private final String detail;
        private final transient SymbolPainter symbol;

        public Option(String label, String detail, SymbolPainter symbol) {
            this.label = label != null ? label : "";
            this.detail = detail != null ? detail : "";
            this.symbol = symbol;
        }

        public String getLabel() {
            return label;
        }

        public String getDetail() {
            return detail;
        }
    }

    /** Draws an option's symbol, centred on the origin, inside a box of the given size. */
    public interface SymbolPainter {
        void paint(Graphics2D graphics, int size, Color color);
    }

    /**
     * Opens the selector and blocks until the user picks or dismisses it.
     *
     * @param anchorOnScreen where to centre the ring, usually the pointer; when
     *     {@code null} the current pointer location is used
     * @return the index of the chosen option, or {@link #CANCELLED}
     */
    public static int choose(Window owner, String title, List<Option> options, Point anchorOnScreen) {
        if (options == null || options.isEmpty()) {
            return CANCELLED;
        }
        RadialMenuDialog dialog = new RadialMenuDialog(owner, title, options);
        dialog.setLocationAround(anchorOnScreen);
        dialog.setVisible(true);
        return dialog.chosenIndex;
    }

    private RadialMenuDialog(Window owner, String title, List<Option> options) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.title = title != null ? title.toUpperCase(Locale.ROOT) : "";
        this.options = new ArrayList<Option>(options);
        setUndecorated(true);
        setResizable(false);
        // A round selector needs a window with no corners: where the toolkit can
        // give us a per-pixel alpha window we paint the backdrop ourselves, and
        // where it cannot we settle for a square of near-black.
        if (supportsTransparency()) {
            setBackground(new Color(0, 0, 0, 0));
        } else {
            setBackground(BACKDROP_OUTER);
        }
        WheelPanel wheel = new WheelPanel();
        wheel.setPreferredSize(new Dimension(DIAMETER, DIAMETER));
        setContentPane(wheel);
        pack();
        installKeyBindings(wheel);
    }

    private static boolean supportsTransparency() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
    }

    /** Centres the ring on a point, pulled back onto the screen when it would hang off it. */
    private void setLocationAround(Point anchorOnScreen) {
        Point anchor = anchorOnScreen;
        if (anchor == null) {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            anchor = pointerInfo != null ? pointerInfo.getLocation() : null;
        }
        if (anchor == null) {
            setLocationRelativeTo(getOwner());
            return;
        }
        Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        if (getOwner() != null && getOwner().getGraphicsConfiguration() != null) {
            screen = getOwner().getGraphicsConfiguration().getBounds();
        }
        int x = anchor.x - (getWidth() / 2);
        int y = anchor.y - (getHeight() / 2);
        x = Math.max(screen.x, Math.min((screen.x + screen.width) - getWidth(), x));
        y = Math.max(screen.y, Math.min((screen.y + screen.height) - getHeight(), y));
        setLocation(x, y);
    }

    private void installKeyBindings(JComponent component) {
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                finish(CANCELLED);
            }
        });
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm", new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                finish(hoveredIndex);
            }
        });
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next", stepAction(1));
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "nextDown", stepAction(1));
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previous", stepAction(-1));
        bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "previousUp", stepAction(-1));
        for (int i = 0; i < Math.min(9, options.size()); i++) {
            final int index = i;
            bind(component, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), "choose" + i, new AbstractAction() {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    finish(index);
                }
            });
        }
    }

    private AbstractAction stepAction(final int step) {
        return new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int count = options.size();
                int next = hoveredIndex == CANCELLED ? (step > 0 ? 0 : count - 1) : hoveredIndex + step;
                hoveredIndex = ((next % count) + count) % count;
                repaint();
            }
        };
    }

    private static void bind(JComponent component, KeyStroke keyStroke, String name, AbstractAction action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        component.getActionMap().put(name, action);
    }

    private void finish(int index) {
        chosenIndex = index;
        dispose();
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    /** Bearing of the middle of a segment, in Java2D degrees, with the first option at the top. */
    static double segmentCenterAngle(int index, int optionCount) {
        return 90d - (index * (360d / optionCount));
    }

    /** The segment a bearing falls in, or {@link #CANCELLED} when it lands in a gap. */
    static int segmentAt(double angleDegrees, int optionCount) {
        double sweep = 360d / optionCount;
        for (int i = 0; i < optionCount; i++) {
            double delta = normalizeDegrees(angleDegrees - segmentCenterAngle(i, optionCount));
            if (Math.abs(delta) <= (sweep - SEGMENT_GAP) / 2d) {
                return i;
            }
        }
        return CANCELLED;
    }

    private double segmentCenterAngle(int index) {
        return segmentCenterAngle(index, options.size());
    }

    private double segmentSweep() {
        return 360d / options.size();
    }

    private int segmentAt(double angleDegrees) {
        return segmentAt(angleDegrees, options.size());
    }

    /** Folds an angle into {@code (-180, 180]}. */
    private static double normalizeDegrees(double degrees) {
        double value = degrees % 360d;
        if (value > 180d) {
            value -= 360d;
        }
        if (value <= -180d) {
            value += 360d;
        }
        return value;
    }

    private static Shape annularSector(double centerX, double centerY, double innerRadius,
            double outerRadius, double startAngle, double extent) {
        Area area = new Area(new Arc2D.Double(centerX - outerRadius, centerY - outerRadius,
            outerRadius * 2d, outerRadius * 2d, startAngle, extent, Arc2D.PIE));
        area.subtract(new Area(new Ellipse2D.Double(centerX - innerRadius, centerY - innerRadius,
            innerRadius * 2d, innerRadius * 2d)));
        return area;
    }

    private static Shape circle(double centerX, double centerY, double radius) {
        return new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2d, radius * 2d);
    }

    private static Point2D pointAt(double centerX, double centerY, double radius, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        return new Point2D.Double(centerX + (Math.cos(radians) * radius),
            centerY - (Math.sin(radians) * radius));
    }

    private static Color alpha(Color color, int value) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.max(0, Math.min(255, value)));
    }

    /**
     * Strokes a shape several times, wider and fainter each time, which is how a
     * line gets its bloom without a shader.
     */
    private static void drawGlow(Graphics2D graphics, Shape shape, Color color, float width, int layers) {
        for (int layer = layers; layer >= 1; layer--) {
            float layerWidth = width * layer;
            int layerAlpha = layer == 1 ? color.getAlpha() : color.getAlpha() / (4 * layer);
            graphics.setStroke(new BasicStroke(layerWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(alpha(color, layerAlpha));
            graphics.draw(shape);
        }
    }

    // ------------------------------------------------------------------
    // The wheel
    // ------------------------------------------------------------------

    private final class WheelPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        /**
         * The parts of the console that never change: the disc, its scanlines, the
         * ticked ring, the cardinals and the title. They are the expensive half of
         * a frame and none of it depends on the pointer, so it is drawn once and
         * blitted afterwards.
         */
        private transient BufferedImage console;
        private transient BufferedImage frame;

        private WheelPanel() {
            setOpaque(false);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateHover(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    updateHover(e.getX(), e.getY());
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    updateHover(e.getX(), e.getY());
                    finish(hoveredIndex);
                }
            });
        }

        /**
         * Tracks the pointer, and repaints only when what is drawn would actually
         * differ. Nothing on the selector animates, so a repaint that changes no
         * pixel is not merely wasted: on a translucent window it is a chance to
         * flicker for no reason at all.
         */
        private void updateHover(int x, int y) {
            double centerX = getWidth() / 2d;
            double centerY = getHeight() / 2d;
            double deltaX = x - centerX;
            double deltaY = centerY - y;
            double radius = Math.hypot(deltaX, deltaY);
            double bearing = Math.toDegrees(Math.atan2(deltaY, deltaX));
            boolean inside = radius > HUB_RADIUS && radius <= RING_OUTER_RADIUS;
            int hovered = inside ? segmentAt(bearing) : CANCELLED;
            if (hovered == hoveredIndex) {
                return;
            }
            hoveredIndex = hovered;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
                frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }

            // Compose the complete translucent frame away from the window. Clearing
            // the live surface before repainting it can briefly expose the desktop on
            // some window managers, which makes the wheel flash while the mouse moves.
            Graphics2D graphics = frame.createGraphics();
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, width, height);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double centerX = getWidth() / 2d;
            double centerY = getHeight() / 2d;
            graphics.drawImage(console(), 0, 0, null);
            paintSegments(graphics, centerX, centerY);
            paintHub(graphics, centerX, centerY);
            paintSelectionMarker(graphics, centerX, centerY);
            graphics.dispose();

            Graphics2D target = (Graphics2D) g.create();
            target.setComposite(AlphaComposite.Src);
            target.drawImage(frame, 0, 0, null);
            target.dispose();
        }

        /** Builds, once, the still image of the console the choices are drawn on. */
        private BufferedImage console() {
            if (console != null && console.getWidth() == getWidth() && console.getHeight() == getHeight()) {
                return console;
            }
            BufferedImage image = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()),
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double centerX = getWidth() / 2d;
            double centerY = getHeight() / 2d;
            paintBackdrop(graphics, centerX, centerY);
            paintCompassRing(graphics, centerX, centerY);
            graphics.dispose();
            console = image;
            return console;
        }

        private void paintBackdrop(Graphics2D graphics, double centerX, double centerY) {
            Shape disc = circle(centerX, centerY, OUTER_RADIUS);
            graphics.setPaint(new RadialGradientPaint(
                new Point2D.Double(centerX, centerY), (float) OUTER_RADIUS,
                new float[] { 0f, 0.62f, 1f },
                new Color[] { BACKDROP_INNER, BACKDROP_OUTER, alpha(BACKDROP_OUTER, 210) }));
            graphics.fill(disc);

            // Scanlines, clipped to the disc: the console is a screen, not paper.
            Shape oldClip = graphics.getClip();
            graphics.clip(disc);
            graphics.setColor(alpha(GLOW, 12));
            graphics.setStroke(new BasicStroke(1f));
            for (double y = centerY - OUTER_RADIUS; y <= centerY + OUTER_RADIUS; y += 3d) {
                graphics.draw(new Line2D.Double(centerX - OUTER_RADIUS, y, centerX + OUTER_RADIUS, y));
            }
            graphics.setClip(oldClip);

            drawGlow(graphics, disc, alpha(GLOW, 150), 1.4f, 3);
        }

        private void paintSegments(Graphics2D graphics, double centerX, double centerY) {
            double sweep = segmentSweep() - SEGMENT_GAP;
            for (int i = 0; i < options.size(); i++) {
                boolean hovered = i == hoveredIndex;
                double center = segmentCenterAngle(i);
                Shape sector = annularSector(centerX, centerY, RING_INNER_RADIUS, RING_OUTER_RADIUS,
                    center - (sweep / 2d), sweep);
                graphics.setColor(hovered ? alpha(ACCENT, 62) : alpha(GLOW, 18));
                graphics.fill(sector);
                drawGlow(graphics, sector, hovered ? alpha(ACCENT, 235) : alpha(GLOW_DIM, 190),
                    hovered ? 1.7f : 1.1f, hovered ? 4 : 2);
                paintSegmentContent(graphics, centerX, centerY, i, hovered);
            }
        }

        private void paintSegmentContent(Graphics2D graphics, double centerX, double centerY,
                int index, boolean hovered) {
            Option option = options.get(index);
            double center = segmentCenterAngle(index);
            double labelRadius = (RING_INNER_RADIUS + RING_OUTER_RADIUS) / 2d;
            Point2D anchor = pointAt(centerX, centerY, labelRadius, center);
            Color color = hovered ? Color.WHITE : alpha(GLOW, 205);

            if (option.symbol != null) {
                Graphics2D symbolGraphics = (Graphics2D) graphics.create();
                symbolGraphics.translate(anchor.getX(), anchor.getY() - 16d);
                option.symbol.paint(symbolGraphics, 30, color);
                symbolGraphics.dispose();
            }

            graphics.setFont(labelFont(hovered));
            String[] lines = wrapLabel(option.getLabel());
            int lineHeight = graphics.getFontMetrics().getHeight();
            double firstLineY = anchor.getY() + (option.symbol != null ? 18d : 4d);
            for (int line = 0; line < lines.length; line++) {
                int textWidth = graphics.getFontMetrics().stringWidth(lines[line]);
                float textX = (float) (anchor.getX() - (textWidth / 2d));
                float textY = (float) (firstLineY + (line * lineHeight));
                if (hovered) {
                    graphics.setColor(alpha(ACCENT, 90));
                    graphics.drawString(lines[line], textX, textY + 1f);
                }
                graphics.setColor(color);
                graphics.drawString(lines[line], textX, textY);
            }

            // The shortcut number, on the inner edge of its own segment.
            if (index < 9) {
                graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                double keyAngle = center + ((segmentSweep() - SEGMENT_GAP) / 2d) - 8d;
                Point2D keyAnchor = pointAt(centerX, centerY, RING_INNER_RADIUS + 14d, keyAngle);
                String key = String.valueOf(index + 1);
                int keyWidth = graphics.getFontMetrics().stringWidth(key);
                graphics.setColor(alpha(hovered ? ACCENT : GLOW, hovered ? 220 : 120));
                graphics.drawString(key, (float) (keyAnchor.getX() - (keyWidth / 2d)),
                    (float) keyAnchor.getY());
            }
        }

        /** The ticked outer ring, its cardinals, and the selected option marker. */
        private void paintCompassRing(Graphics2D graphics, double centerX, double centerY) {
            graphics.setStroke(new BasicStroke(1f));
            for (int degrees = 0; degrees < 360; degrees += 5) {
                if (degrees % 90 == 0) {
                    // The cardinal letter stands where its tick would have been.
                    continue;
                }
                boolean major = degrees % 45 == 0;
                double innerRadius = OUTER_RADIUS - (major ? 14d : 6d);
                Point2D from = pointAt(centerX, centerY, innerRadius, degrees);
                Point2D to = pointAt(centerX, centerY, OUTER_RADIUS - 2d, degrees);
                graphics.setColor(alpha(GLOW, major ? 190 : 70));
                graphics.draw(new Line2D.Double(from, to));
            }
            graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            String[] cardinals = { "E", "N", "W", "S" };
            for (int i = 0; i < cardinals.length; i++) {
                if (i == 1 && !title.isEmpty()) {
                    // North is where the title plate goes.
                    continue;
                }
                Point2D anchor = pointAt(centerX, centerY, OUTER_RADIUS - 10d, i * 90d);
                int textWidth = graphics.getFontMetrics().stringWidth(cardinals[i]);
                graphics.setColor(alpha(GLOW, 150));
                graphics.drawString(cardinals[i], (float) (anchor.getX() - (textWidth / 2d)),
                    (float) (anchor.getY() + 4d));
            }
            drawGlow(graphics, circle(centerX, centerY, RING_OUTER_RADIUS + 6d), alpha(GLOW_DIM, 150), 1f, 2);

            if (!title.isEmpty()) {
                paintTitlePlate(graphics, centerX, centerY);
            }
        }

        /** Marks the selected segment without animating in response to every mouse pixel. */
        private void paintSelectionMarker(Graphics2D graphics, double centerX, double centerY) {
            if (hoveredIndex == CANCELLED) {
                return;
            }
            double bearing = segmentCenterAngle(hoveredIndex);
            Point2D marker = pointAt(centerX, centerY, OUTER_RADIUS - 9d, bearing);
            Point2D markerInner = pointAt(centerX, centerY, OUTER_RADIUS - 24d, bearing);
            drawGlow(graphics, new Line2D.Double(markerInner, marker), alpha(ACCENT, 235), 2f, 3);
        }

        /**
         * The title, on a plate laid over the top of the ring. A choice sits under
         * every bearing, so there is no clear arc to write it in: it has to be a
         * plate over them, with its own backing to stay readable.
         */
        private void paintTitlePlate(Graphics2D graphics, double centerX, double centerY) {
            graphics.setFont(spacedFont(new Font(Font.MONOSPACED, Font.BOLD, 12), 0.22f));
            int textWidth = graphics.getFontMetrics().stringWidth(title);
            double plateWidth = textWidth + 30d;
            double plateHeight = 22d;
            double plateY = centerY - OUTER_RADIUS + 8d;
            Shape plate = new RoundRectangle2D.Double(centerX - (plateWidth / 2d), plateY,
                plateWidth, plateHeight, plateHeight, plateHeight);
            graphics.setColor(alpha(BACKDROP_OUTER, 244));
            graphics.fill(plate);
            drawGlow(graphics, plate, alpha(GLOW_DIM, 190), 1f, 2);
            graphics.setColor(alpha(GLOW, 225));
            graphics.drawString(title, (float) (centerX - (textWidth / 2d)),
                (float) (plateY + plateHeight - 7d));
        }

        /** The hub, which reads back whatever the pointer is over and cancels when clicked. */
        private void paintHub(Graphics2D graphics, double centerX, double centerY) {
            boolean cancelHovered = hoveredIndex == CANCELLED;
            Shape hub = circle(centerX, centerY, HUB_RADIUS);
            graphics.setColor(alpha(BACKDROP_OUTER, 240));
            graphics.fill(hub);
            drawGlow(graphics, hub, cancelHovered ? alpha(ACCENT, 210) : alpha(GLOW_DIM, 170), 1.2f, 2);
            drawGlow(graphics, circle(centerX, centerY, HUB_RADIUS - 8d), alpha(GLOW, 46), 1f, 1);

            if (cancelHovered) {
                graphics.setFont(spacedFont(new Font(Font.MONOSPACED, Font.BOLD, 13), 0.24f));
                drawCentered(graphics, "CANCEL", centerX, centerY - 2d, Color.WHITE);
                graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
                drawCentered(graphics, "ESC", centerX, centerY + 16d, alpha(GLOW, 150));
                return;
            }
            Option option = options.get(hoveredIndex);
            double bearing = segmentCenterAngle(hoveredIndex);
            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            drawCentered(graphics, String.format(Locale.ROOT, "BRG %03d",
                Integer.valueOf((int) Math.round(((450d - bearing) % 360d)))),
                centerX, centerY - 30d, alpha(GLOW, 150));
            graphics.setFont(fittedHubFont(graphics, option.getLabel()));
            drawCentered(graphics, option.getLabel(), centerX, centerY - 8d, Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            double y = centerY + 10d;
            for (String line : wrapDetail(option.getDetail(), graphics)) {
                drawCentered(graphics, line, centerX, y, alpha(GLOW, 190));
                y += graphics.getFontMetrics().getHeight();
            }
        }

        private void drawCentered(Graphics2D graphics, String text, double centerX, double y, Color color) {
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            graphics.setColor(color);
            graphics.drawString(text, (float) (centerX - (textWidth / 2d)), (float) y);
        }

        /** The largest hub font, down to a floor, that keeps a label inside the hub. */
        private Font fittedHubFont(Graphics2D graphics, String text) {
            int maxWidth = (int) ((HUB_RADIUS * 2d) - 22d);
            for (int size = 13; size > 9; size--) {
                Font candidate = spacedFont(new Font(Font.MONOSPACED, Font.BOLD, size), 0.2f);
                if (graphics.getFontMetrics(candidate).stringWidth(text) <= maxWidth) {
                    return candidate;
                }
            }
            return spacedFont(new Font(Font.MONOSPACED, Font.BOLD, 9), 0.1f);
        }

        private Font labelFont(boolean hovered) {
            return spacedFont(new Font(Font.MONOSPACED, Font.BOLD, hovered ? 14 : 13), 0.16f);
        }

        private String[] wrapLabel(String label) {
            String[] words = label.split(" ");
            if (words.length <= 1) {
                return words;
            }
            // Two lines at most: a label on a ring has no room for more.
            int split = (words.length + 1) / 2;
            return new String[] {
                String.join(" ", Arrays.asList(words).subList(0, split)),
                String.join(" ", Arrays.asList(words).subList(split, words.length))
            };
        }

        private List<String> wrapDetail(String detail, Graphics2D graphics) {
            List<String> lines = new ArrayList<String>();
            StringBuilder current = new StringBuilder();
            int maxWidth = (int) ((HUB_RADIUS * 2d) - 26d);
            for (String word : detail.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (graphics.getFontMetrics().stringWidth(candidate) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
            return lines;
        }
    }

    private static Font spacedFont(Font font, float tracking) {
        java.util.Map<java.awt.font.TextAttribute, Object> attributes =
            new java.util.HashMap<java.awt.font.TextAttribute, Object>();
        attributes.put(java.awt.font.TextAttribute.TRACKING, Float.valueOf(tracking));
        return font.deriveFont(attributes);
    }
}
