package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Searchable selector for known in-game areas.
 */
public class AreaSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<AreaReference> allAreas;
    private final DefaultListModel<AreaReference> filteredModel = new DefaultListModel<>();
    private final JList<AreaReference> areaList = new JList<>(filteredModel);
    private final JTextField filterField = new JTextField(24);
    private final JLabel previewLabel = new JLabel("Select an area", SwingConstants.CENTER);
    private final JLabel previewInfoLabel = new JLabel(" ", SwingConstants.CENTER);
    private static final int PREVIEW_SIZE = 192;
    private static final Map<String, ImageIcon> PREVIEW_CACHE = new LinkedHashMap<String, ImageIcon>() {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
            return size() > 24;
        }
    };

    private AreaReference selectedArea;
    private int previewRequestId;

    public AreaSelectionDialog(Frame owner, List<AreaReference> allAreas, String initialFilter) {
        this(owner, "Select Existing In-Game Area", allAreas, initialFilter);
    }

    public AreaSelectionDialog(Frame owner, String title, List<AreaReference> allAreas, String initialFilter) {
        super(owner, title, true);
        this.allAreas = allAreas != null ? allAreas : new ArrayList<AreaReference>();
        initComponents();
        filterField.setText(initialFilter != null ? initialFilter : "");
        updateFilter();
        setPreferredSize(new Dimension(900, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        filterPanel.add(filterField, BorderLayout.CENTER);
        add(filterPanel, BorderLayout.NORTH);

        areaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        areaList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AreaReference) {
                    AreaReference areaReference = (AreaReference) value;
                    setText(areaReference.getDisplayText());
                    setToolTipText(areaReference.getDisplayText());
                }
                return this;
            }
        });
        areaList.setVisibleRowCount(18);
        areaList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmSelection();
                }
            }
        });
        areaList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreview();
            }
        });

        JScrollPane listScrollPane = new JScrollPane(areaList);
        listScrollPane.setPreferredSize(new Dimension(460, 420));

        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(PREVIEW_SIZE + 24, PREVIEW_SIZE + 24));
        previewLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        previewInfoLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JPanel previewPanel = new JPanel(new BorderLayout(0, 6));
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 10),
            BorderFactory.createEtchedBorder()
        ));
        previewPanel.add(previewLabel, BorderLayout.CENTER);
        previewPanel.add(previewInfoLabel, BorderLayout.SOUTH);
        previewPanel.setPreferredSize(new Dimension(PREVIEW_SIZE + 120, PREVIEW_SIZE + 90));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, previewPanel);
        splitPane.setResizeWeight(0.62);
        splitPane.setDividerLocation(460);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        splitPane.setEnabled(false);
        add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Select");
        okButton.addActionListener(e -> confirmSelection());
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }
        });

        getRootPane().setDefaultButton(okButton);
    }

    private void updateFilter() {
        filteredModel.clear();
        String filter = filterField.getText() != null ? filterField.getText().trim().toLowerCase() : "";
        for (AreaReference area : allAreas) {
            if (filter.isEmpty()
                    || area.getResref().toLowerCase().contains(filter)
                    || area.getDescription().toLowerCase().contains(filter)) {
                filteredModel.addElement(area);
            }
        }
        if (!filteredModel.isEmpty()) {
            areaList.setSelectedIndex(0);
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("No matching area");
            previewInfoLabel.setText(" ");
        }
    }

    private void confirmSelection() {
        selectedArea = areaList.getSelectedValue();
        if (selectedArea != null) {
            dispose();
        }
    }

    public AreaReference getSelectedArea() {
        return selectedArea;
    }

    private void updatePreview() {
        AreaReference area = areaList.getSelectedValue();
        if (area == null) {
            previewLabel.setIcon(null);
            previewLabel.setText("Select an area");
            previewInfoLabel.setText(" ");
            return;
        }

        String cacheKey = area.getResref().trim().toUpperCase();
        ImageIcon cached = PREVIEW_CACHE.get(cacheKey);
        if (cached != null) {
            previewLabel.setIcon(cached);
            previewLabel.setText("");
            previewInfoLabel.setText(area.getDescription());
            return;
        }

        previewLabel.setIcon(null);
        previewLabel.setText("Loading preview...");
        previewInfoLabel.setText(area.getDescription());
        final int requestId = ++previewRequestId;
        final String areaResref = cacheKey;
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage image = GameAreaImageLoader.loadAreaImage(UserPreferences.getGameInstallPath(), areaResref);
                return createPreviewIcon(image);
            }

            @Override
            protected void done() {
                if (requestId != previewRequestId) {
                    return;
                }
                try {
                    ImageIcon icon = get();
                    PREVIEW_CACHE.put(areaResref, icon);
                    previewLabel.setIcon(icon);
                    previewLabel.setText("");
                } catch (Exception ex) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Preview unavailable");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage();
                    previewInfoLabel.setText((message != null && !message.trim().isEmpty()) ? message : area.getDescription());
                    return;
                }
                previewInfoLabel.setText(area.getDescription());
            }
        }.execute();
    }

    private static ImageIcon createPreviewIcon(BufferedImage image) {
        if (image == null) {
            return null;
        }
        double scale = Math.min((double) PREVIEW_SIZE / image.getWidth(), (double) PREVIEW_SIZE / image.getHeight());
        scale = Math.min(scale, 1.0d);
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return new ImageIcon(scaled);
    }
}
