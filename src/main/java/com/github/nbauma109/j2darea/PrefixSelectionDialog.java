package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Searchable selector for reserved IE filename prefixes.
 */
public class PrefixSelectionDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<PrefixReservation> allPrefixes;
    private final DefaultListModel<PrefixReservation> filteredModel = new DefaultListModel<>();
    private final JList<PrefixReservation> prefixList = new JList<>(filteredModel);
    private final JTextField filterField = new JTextField(24);

    private PrefixReservation selectedPrefix;

    public PrefixSelectionDialog(Frame owner, List<PrefixReservation> allPrefixes, String initialFilter) {
        super(owner, "Select Reserved Prefix", true);
        this.allPrefixes = allPrefixes != null ? allPrefixes : new ArrayList<PrefixReservation>();
        initComponents();
        filterField.setText(initialFilter != null ? initialFilter : "");
        updateFilter();
        setPreferredSize(new Dimension(720, 420));
        pack();
        setLocationRelativeTo(owner);
    }

    public PrefixReservation getSelectedPrefix() {
        return selectedPrefix;
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        filterPanel.add(filterField, BorderLayout.CENTER);
        add(filterPanel, BorderLayout.NORTH);

        prefixList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        prefixList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrefixReservation) {
                    setText(((PrefixReservation) value).getDisplayText());
                }
                return this;
            }
        });
        prefixList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    confirmSelection();
                }
            }
        });
        add(new JScrollPane(prefixList), BorderLayout.CENTER);

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
        for (PrefixReservation prefixReservation : allPrefixes) {
            if (filter.isEmpty()
                    || prefixReservation.getPrefix().toLowerCase().contains(filter)
                    || prefixReservation.getProject().toLowerCase().contains(filter)
                    || prefixReservation.getStatus().toLowerCase().contains(filter)
                    || prefixReservation.getComments().toLowerCase().contains(filter)
                    || prefixReservation.getScope().toLowerCase().contains(filter)) {
                filteredModel.addElement(prefixReservation);
            }
        }
        if (!filteredModel.isEmpty()) {
            prefixList.setSelectedIndex(0);
        }
    }

    private void confirmSelection() {
        selectedPrefix = prefixList.getSelectedValue();
        if (selectedPrefix != null) {
            dispose();
        }
    }
}
