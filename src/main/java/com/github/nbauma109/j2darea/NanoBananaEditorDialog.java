package com.github.nbauma109.j2darea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

public class NanoBananaEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_PROMPT =
        "extract only the main object without ground into a transparent background image";

    private final EditorPanel editorPanel;
    private final JTextArea promptTextArea;
    private final JLabel statusLabel;
    private final JButton okButton;
    private final JButton saveButton;

    private BufferedImage currentImage;
    private boolean busy;

    public NanoBananaEditorDialog(Frame owner, BufferedImage initialImage) {
        super(owner, "Nano Banana 2 Extraction", false);
        this.currentImage = initialImage;
        this.editorPanel = new EditorPanel();
        this.promptTextArea = new JTextArea(DEFAULT_PROMPT, 3, 56);
        this.statusLabel = new JLabel("Open selection. Edit the prompt, click OK, and export the current preview when you want to keep it.", SwingConstants.LEFT);
        this.okButton = new JButton("OK");
        this.okButton.setToolTipText("Run Nano Banana 2 with the current prompt");
        this.okButton.addActionListener(e -> applyPrompt());
        this.saveButton = createToolbarButton("/icons/save-img.png", "Export current image", e -> saveCurrentImage());
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void loadSelection(BufferedImage image) {
        if (image == null) {
            return;
        }
        updateCurrentImage(image);
        promptTextArea.setText(DEFAULT_PROMPT);
        statusLabel.setText("Loaded a new selection. Edit the prompt, click OK, and export the current preview when you want to keep it.");
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JLabel instructions = new JLabel(
            "<html>Edit the prompt, click OK to apply it with Nano Banana 2, and use Save to export the current preview.</html>",
            SwingConstants.LEFT
        );
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(instructions, BorderLayout.NORTH);

        JScrollPane imageScrollPane = new JScrollPane(editorPanel);
        imageScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(imageScrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        JScrollPane promptScrollPane = new JScrollPane(promptTextArea);
        promptScrollPane.setPreferredSize(new Dimension(640, 84));
        southPanel.add(promptScrollPane, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controlsPanel.add(okButton);
        controlsPanel.add(saveButton);
        southPanel.add(controlsPanel, BorderLayout.NORTH);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    private JButton createToolbarButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(new ImageIcon(getClass().getResource(iconPath)));
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setFocusable(false);
        button.setPreferredSize(J2DArea.BUTTON_SIZE);
        button.setMinimumSize(J2DArea.BUTTON_SIZE);
        button.setMaximumSize(J2DArea.BUTTON_SIZE);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        return button;
    }

    private void applyPrompt() {
        String prompt = promptTextArea.getText() != null ? promptTextArea.getText().trim() : "";
        if (prompt.isEmpty()) {
            showMessage("Enter a prompt before clicking OK.", "Nano Banana 2 Extraction", JOptionPane.ERROR_MESSAGE);
            return;
        }
        runNanoBananaTask(
            "Running Nano Banana 2...",
            "Prompt applied.",
            () -> new NanoBananaImageClient().editImage(getApiKey(), prompt, currentImage)
        );
    }

    private String getApiKey() throws IOException {
        String apiKey = UserPreferences.getGoogleAiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("Set a Google AI API key in Settings -> User Preferences before using Nano Banana 2.");
        }
        return apiKey;
    }

    private void runNanoBananaTask(String runningStatus, String finishedStatus, NanoBananaOperation operation) {
        setBusy(true, runningStatus);
        new SwingWorker<NanoBananaImageClient.Result, Void>() {
            @Override
            protected NanoBananaImageClient.Result doInBackground() throws Exception {
                return operation.run();
            }

            @Override
            protected void done() {
                try {
                    NanoBananaImageClient.Result result = get();
                    updateCurrentImage(result.getImage());
                    if (result.getNote() != null && !result.getNote().trim().isEmpty()) {
                        setBusy(false, finishedStatus + " " + result.getNote().replace('\n', ' '));
                    } else {
                        setBusy(false, finishedStatus);
                    }
                } catch (Exception ex) {
                    Throwable rootCause = unwrapSwingWorkerException(ex);
                    if (!(rootCause instanceof IOException)) {
                        rootCause.printStackTrace();
                    }
                    String message = rootCause.getMessage();
                    setBusy(false, "Ready.");
                    showMessage(message != null && !message.trim().isEmpty() ? message : "Nano Banana 2 request failed.",
                        "Nano Banana 2 Extraction", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void saveCurrentImage() {
        File file = J2DArea.chooseFile((Frame) getOwner(), FileDialog.SAVE, FileChooserLocation.SAVE_BG);
        if (file == null) {
            return;
        }
        boolean success;
        try {
            success = J2DArea.writeImage(file, currentImage);
        } catch (IOException ex) {
            ex.printStackTrace();
            success = false;
        }
        if (success) {
            showMessage("Image saved.", "Nano Banana 2 Extraction", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Image saved to " + file.getAbsolutePath());
        } else {
            showMessage("Image save failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCurrentImage(BufferedImage image) {
        this.currentImage = image;
        editorPanel.refreshPreferredSize();
        editorPanel.revalidate();
        editorPanel.repaint();
    }

    private void setBusy(boolean busy, String status) {
        this.busy = busy;
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        okButton.setEnabled(!busy);
        saveButton.setEnabled(!busy);
        promptTextArea.setEnabled(!busy);
        statusLabel.setText(status);
    }

    private void showMessage(String message, String title, int messageType) {
        JLabel label = new JLabel("<html>" + message.replace("\n", "<br>") + "</html>");
        JOptionPane.showMessageDialog(this, label, title, messageType);
    }

    private Throwable unwrapSwingWorkerException(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current != null ? current : throwable;
    }

    private interface NanoBananaOperation {
        NanoBananaImageClient.Result run() throws Exception;
    }

    private final class EditorPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private EditorPanel() {
            refreshPreferredSize();
        }

        private void refreshPreferredSize() {
            setPreferredSize(new Dimension(
                Math.max(1, currentImage.getWidth()),
                Math.max(1, currentImage.getHeight())
            ));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            try {
                graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                paintCheckerboard(graphics2d);
                graphics2d.drawImage(currentImage, 0, 0, null);
            } finally {
                graphics2d.dispose();
            }
        }

        private void paintCheckerboard(Graphics2D graphics2d) {
            int cellSize = 12;
            Color light = new Color(240, 240, 240);
            Color dark = new Color(215, 215, 215);
            for (int y = 0; y < getHeight(); y += cellSize) {
                for (int x = 0; x < getWidth(); x += cellSize) {
                    graphics2d.setColor((((x / cellSize) + (y / cellSize)) & 1) == 0 ? light : dark);
                    graphics2d.fillRect(x, y, cellSize, cellSize);
                }
            }
        }
    }
}
