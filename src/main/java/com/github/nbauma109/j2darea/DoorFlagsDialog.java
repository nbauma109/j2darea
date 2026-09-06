package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class DoorFlagsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] FLAG_LABELS = new String[] {
        "Door open",
        "Door locked",
        "Trap resets",
        "Detectable trap",
        "Door forced",
        "Cannot close",
        "Door located",
        "Door secret",
        "Secret door detected",
        "Can be looked through",
        "Uses key",
        "Sliding door",
        "Unknown (12)",
        "Unknown (13)",
        "Unknown (14)",
        "Unknown (15)"
    };

    private final JCheckBox[] checkBoxes = new JCheckBox[FLAG_LABELS.length];
    private final JLabel valueLabel = new JLabel();
    private boolean confirmed;

    public DoorFlagsDialog(Frame owner, int initialFlags) {
        super(owner, "Edit Door Flags", true);
        setLayout(new BorderLayout(10, 10));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 8, 4));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        for (int bit = 0; bit < FLAG_LABELS.length; bit++) {
            final int mask = 1 << bit;
            JCheckBox checkBox = new JCheckBox(FLAG_LABELS[bit] + " (" + bit + ")");
            checkBox.setSelected((initialFlags & mask) != 0);
            checkBox.addActionListener(e -> updateValueLabel());
            checkBoxes[bit] = checkBox;
            gridPanel.add(checkBox);
        }
        add(gridPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        valueLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        bottomPanel.add(valueLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        javax.swing.JButton okButton = new javax.swing.JButton("OK");
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        updateValueLabel();
        getRootPane().setDefaultButton(okButton);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getFlags() {
        int value = 0;
        for (int bit = 0; bit < checkBoxes.length; bit++) {
            if (checkBoxes[bit].isSelected()) {
                value |= (1 << bit);
            }
        }
        return value;
    }

    private void updateValueLabel() {
        valueLabel.setText("Flags value: " + getFlags());
    }
}
