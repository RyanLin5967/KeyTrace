package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class KeyRecorder extends JDialog {
    private JLabel statusLabel;
    public List<Integer> result = null;
    private javax.swing.Timer pollingTimer;

    public KeyRecorder(JFrame parent) {
        super(parent, "Record Key Combo/Custom Key", true);
        setSize(400, 200);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parent);

        statusLabel = new JLabel("Press keys (e.g., Win, then Shift, then S)...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(statusLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton clearBtn = new JButton("Clear");
        JButton cancelBtn = new JButton("Cancel");
        JButton doneBtn = new JButton("Done");

        btnPanel.add(clearBtn);
        btnPanel.add(cancelBtn);
        btnPanel.add(doneBtn);
        add(btnPanel, BorderLayout.SOUTH);

        Main.recordingBuffer.clear();
        Main.isRecording = true;

        pollingTimer = new javax.swing.Timer(50, e -> {
            if (!Main.recordingBuffer.isEmpty()) {
                statusLabel.setText("Detected: " + getKeyNames(Main.recordingBuffer));
            } else {
                statusLabel.setText("Press keys...");
            }
        });
        pollingTimer.start();

        clearBtn.addActionListener(e -> { 
            Main.recordingBuffer.clear();
            statusLabel.setText("Cleared.");
        });

        cancelBtn.addActionListener(e -> {
            closeDialog(false); 
        });

        doneBtn.addActionListener(e -> {
            closeDialog(true);
        });
 
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog(false);
            }
        });
    }

    private void closeDialog(boolean save) {
        pollingTimer.stop();
        Main.isRecording = false; 
        
        if (save && !Main.recordingBuffer.isEmpty()) {
            result = new ArrayList<>(Main.recordingBuffer);
        } else {
            result = null; 
        }
        dispose();
    }

    private String getKeyNames(Set<Integer> codes) {
        StringBuilder sb = new StringBuilder();
        for (int i : codes) sb.append(KeyEvent.getKeyText(i)).append("+");
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}