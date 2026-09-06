package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Polygon;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

/**
 * Minimal region manager for adding/editing polygon regions and travel links.
 */
public class RegionManagerDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<RegionData> regions;
    private final List<AreaReference> availableAreas;
    private final List<AreaReference> knownOwnedAreas;
    private final List<String> availableEntranceNames;
    private final String exportPrefix;
    private final List<String> reservedOwnedAreas;
    private final Polygon currentSelection;
    private final int areaWidth;
    private final int areaHeight;

    private final javax.swing.DefaultListModel<RegionData> listModel = new javax.swing.DefaultListModel<>();
    private final JList<RegionData> regionList = new JList<>(listModel);
    private boolean usedCurrentSelection;

    public RegionManagerDialog(Frame owner, List<RegionData> regions, List<AreaReference> availableAreas,
            List<AreaReference> knownOwnedAreas, List<String> availableEntranceNames, String exportPrefix,
            List<String> reservedOwnedAreas, Polygon currentSelection, int areaWidth, int areaHeight) {
        super(owner, "Regions", true);
        this.regions = regions;
        this.availableAreas = availableAreas;
        this.knownOwnedAreas = knownOwnedAreas;
        this.availableEntranceNames = availableEntranceNames;
        this.exportPrefix = exportPrefix;
        this.reservedOwnedAreas = reservedOwnedAreas;
        this.currentSelection = clonePolygon(currentSelection);
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        initComponents();
        refreshList();
        setSize(520, 360);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public boolean isUsedCurrentSelection() {
        return usedCurrentSelection;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        regionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        regionList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RegionData) {
                    RegionData regionData = (RegionData) value;
                    setText(regionData.getName() + " [" + regionData.getTypeName() + "]");
                }
                return this;
            }
        });
        add(new JScrollPane(regionList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add From Current Polygon");
        addButton.addActionListener(e -> addFromCurrentPolygon());
        addButton.setEnabled(currentSelection.npoints >= 3);
        buttonPanel.add(addButton);

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> editSelectedRegion());
        buttonPanel.add(editButton);

        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelectedRegion());
        buttonPanel.add(removeButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void refreshList() {
        listModel.clear();
        for (RegionData region : regions) {
            listModel.addElement(region);
        }
    }

    private void addFromCurrentPolygon() {
        if (currentSelection.npoints < 3) {
            JOptionPane.showMessageDialog(this,
                "Draw a polygon on the Extraction Area first, then open Regions.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        RegionData region = new RegionData("Region" + (regions.size() + 1), 2, clonePolygon(currentSelection));
        RegionEditorDialog dialog = new RegionEditorDialog(
            (Frame) getOwner(),
            region,
            availableAreas,
            knownOwnedAreas,
            availableEntranceNames,
            exportPrefix,
            reservedOwnedAreas,
            areaWidth,
            areaHeight
        );
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        regions.add(region);
        reservedOwnedAreas.addAll(java.util.Collections.singletonList(region.getDestinationArea()));
        usedCurrentSelection = true;
        refreshList();
        regionList.setSelectedValue(region, true);
    }

    private void editSelectedRegion() {
        RegionData region = regionList.getSelectedValue();
        if (region == null) {
            return;
        }
        RegionEditorDialog dialog = new RegionEditorDialog(
            (Frame) getOwner(),
            region,
            availableAreas,
            knownOwnedAreas,
            availableEntranceNames,
            exportPrefix,
            reservedOwnedAreas,
            areaWidth,
            areaHeight
        );
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            refreshList();
            regionList.setSelectedValue(region, true);
        }
    }

    private void removeSelectedRegion() {
        RegionData region = regionList.getSelectedValue();
        if (region == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(
                this,
                "Remove region '" + region.getName() + "'?",
                "Remove Region",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        regions.remove(region);
        refreshList();
    }

    private Polygon clonePolygon(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints];
        int[] ypoints = new int[polygon.npoints];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints);
        return new Polygon(xpoints, ypoints, polygon.npoints);
    }
}
