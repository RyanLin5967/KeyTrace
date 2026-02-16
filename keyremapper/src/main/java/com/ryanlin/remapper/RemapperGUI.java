package com.ryanlin.remapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemapperGUI extends JFrame implements ActionListener {
    private DefaultTableModel model;
    private JTable table;
    private KeyMap keymap = new KeyMap();

    private JTextField enterKeyToMap = new JTextField(20);
    private JTextField enterKeyToRemap = new JTextField(20);
    private JLabel chooseKey = new JLabel("choose the key to remap");
    private JLabel chooseRemap = new JLabel("choose the key it'll remap to");
    private JButton chooseKeyToRemap = new JButton("submit");
    private JButton createMapping = new JButton("Create Mapping");
    private Color orgColor = createMapping.getBackground();

    private VirtualKeyboard virtualKeyboard;
    private int visualStep = 0; // 0: Idle, 1: Source Selected, 2: Destination Selected
    private KeyButton sourceKeyBtn = null;
    private KeyButton destKeyBtn = null;
    private JButton confirmVisualBtn = new JButton("Confirm Mapping");
    
    public RemapperGUI() {
        
        initUI();
        loadExistingMappings();
        pack();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setTitle("Keyremapper");
        setUndecorated(false);    
        setLayout(new BorderLayout());    
      

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(null);

        JButton heatmapBtn = new JButton("Show Heatmap");
        heatmapBtn.setFocusable(false);

        JButton shiftToggleBtn = new JButton("Shift");
        shiftToggleBtn.setFocusable(false);
        
        //createMapping = new JButton("Create mapping");
        
        JButton removeMapping = new JButton("Remove selected mapping");
        JButton removeAllMappings = new JButton("Remove all mappings");
        JButton addCustomKeyBtn = new JButton("Add Custom Key");

        createMapping.setFocusable(false);
        removeMapping.setFocusable(false);
        removeAllMappings.setFocusable(false);
        addCustomKeyBtn.setFocusable(false);

        String[] layoutOptions = {"100%", "75%", "65%"};
        JComboBox<String> layoutSelector = new JComboBox<>(layoutOptions);
        layoutSelector.addActionListener(e -> {
            String selected = (String) layoutSelector.getSelectedItem();
            if ("100%".equals(selected)) virtualKeyboard.render100Percent();
            else if ("75%".equals(selected)) virtualKeyboard.render75Percent();
            else if ("65%".equals(selected)) virtualKeyboard.render65Percent();
            if (VirtualKeyboard.isHeatmapOn == true) {
                heatmapBtn.setText("Hide Heatmap");
            } else {
                heatmapBtn.setText("Show Heatmap");
            }
        });
        layoutSelector.setFocusable(false);

        confirmVisualBtn.setEnabled(false); 

        topPanel.add(createMapping);
        topPanel.add(removeMapping);
        topPanel.add(removeAllMappings);
        topPanel.add(new JLabel("  "));
        topPanel.add(addCustomKeyBtn);
        topPanel.add(new JLabel(" Size: "));
        topPanel.add(layoutSelector);
        topPanel.add(confirmVisualBtn);

        virtualKeyboard = new VirtualKeyboard(this); 
        JScrollPane kbScroll = new JScrollPane(virtualKeyboard);

        model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(300, 0));

        JPanel northContainer = new JPanel(new GridLayout(1, 1));
        northContainer.add(topPanel);

        addCustomKeyBtn.addActionListener(e -> recordNewCustomKey());

        createMapping.addActionListener(e -> {
            createMapping.setBackground(Color.GRAY);
            toggleInputFields(true);
            visualStep = 1; 
        });
        confirmVisualBtn.setFocusable(false);

        confirmVisualBtn.addActionListener(e -> {
            if (sourceKeyBtn != null && destKeyBtn != null) {
                
                int finalSourceCode;
                createMapping.setBackground(null);
                // CHECK IF WE ARE MAPPING A SHIFT-COMBO
                Boolean isShifted = (Boolean) sourceKeyBtn.getClientProperty("isShiftedCombo");
                if (isShifted != null && isShifted) {
                    // Use the Custom Key ID (e.g. 90001) we found earlier
                    finalSourceCode = (int) sourceKeyBtn.getClientProperty("shiftedPseudoCode");
                } else {
                    // Use normal ID (e.g. 49)
                    finalSourceCode = sourceKeyBtn.getKeyCode();
                }

                int destCode = destKeyBtn.getKeyCode();

                if (Main.codeToCode.containsKey(finalSourceCode)) {
                    JOptionPane.showMessageDialog(this, "This mapping already exists. Remove it first.");
                    return; 
                }
                executeMapping(finalSourceCode, destCode);
                resetVisualSelection();
            }
        });

        removeMapping.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String keyNameInTable = (String) table.getValueAt(row, 0);
                Main.codeToCode.entrySet().removeIf(entry -> 
                    getKeyName(entry.getKey()).equals(keyNameInTable)
                );
                model.removeRow(row);
                ConfigManager.save(); // Save to JSON
            }
        });
        
        topPanel.add(shiftToggleBtn);

        heatmapBtn.addActionListener(e -> {
            virtualKeyboard.toggleHeatmap();
            if (VirtualKeyboard.isHeatmapOn == true) {
                heatmapBtn.setText("Hide Heatmap");
            } else {
                heatmapBtn.setText("Show Heatmap");
            }
        });

        topPanel.add(heatmapBtn);

        shiftToggleBtn.addActionListener(e -> {
            virtualKeyboard.toggleShiftMode();
            
            // Update button text to show state
            if (VirtualKeyboard.isShifted) {
                shiftToggleBtn.setText("Unshift");
            } else {
                shiftToggleBtn.setText("Shift");
                shiftToggleBtn.setBackground(null);
            }
        });
        
        JButton wrappedBtn = new JButton("2026 Wrapped");
        wrappedBtn.setFocusable(false);

        wrappedBtn.addActionListener(e -> {
            WrappedWindow wrapped = new WrappedWindow((JFrame) SwingUtilities.getWindowAncestor(this));
            wrapped.setVisible(true);
        });

        topPanel.add(wrappedBtn);
        removeAllMappings.addActionListener(e -> {
            Main.codeToCode.clear();
            model.setRowCount(0);
            ConfigManager.save(); // Save to JSON
        });

        add(northContainer, BorderLayout.NORTH);
        add(kbScroll, BorderLayout.CENTER);
        add(tableScroll, BorderLayout.EAST);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void recordNewCustomKey() {
        KeyRecorder recorder = new KeyRecorder(this);
        recorder.setVisible(true);

        if (recorder.result != null && !recorder.result.isEmpty()) {
            
            if (CustomKeyManager.isCombinationTaken(recorder.result)) {
                JOptionPane.showMessageDialog(this, 
                    "This key combination is already used by another Custom Key!", 
                    "Duplicate Combination", 
                    JOptionPane.ERROR_MESSAGE);
                return; 
            }

            String name = JOptionPane.showInputDialog(this, "Name this custom key (e.g. 'Copilot'):");
            
            if (name != null && !name.trim().isEmpty()) {
                name = name.trim();

                if (CustomKeyManager.isNameTaken(name)) {
                    JOptionPane.showMessageDialog(this, 
                        "A Custom Key with this name already exists!", 
                        "Duplicate Name", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                CustomKeyManager.add(name, recorder.result);
                List<Integer> modifiers = new ArrayList<>();
        int primaryKey = -1;

        // separate Modifiers from the Main Key
        for (int code : recorder.result) {
            if (isModifier(code)) {
                modifiers.add(code);
            } else {
                primaryKey = code;
            }
        }

        // We can only backfill if we have exactly 1 primary key (e.g. "Ctrl+C", not "Ctrl+C+D")
        if (primaryKey != -1) {
            StringBuilder lookupName = new StringBuilder();

            // Append modifiers in a standard order (optional, but good for consistency)
            // You might need to adjust this order to match how HeatmapManager saves them.
            if (modifiers.contains(17)) lookupName.append("Ctrl+");
            if (modifiers.contains(18)) lookupName.append("Alt+");
            if (modifiers.contains(16)) lookupName.append("Shift+");
            if (modifiers.contains(524)) lookupName.append("Win+");

            lookupName.append(VirtualKeyboard.getName(primaryKey));
            
            String finalLookupString = lookupName.toString(); // e.g., "Ctrl+C" or "Shift+1"

            // CHECK THE DATABASE
            if (HeatmapManager.comboCounts.containsKey(finalLookupString)) {
                long history = HeatmapManager.comboCounts.get(finalLookupString);
                
                if (history > 0) {
                    CustomKey newKey = CustomKeyManager.customKeys.get(CustomKeyManager.customKeys.size() - 1);
                    int newKeyID = newKey.getPseudoCode();
                    
                    HeatmapManager.setExplicitCount(newKeyID, history);
                }
            }
        }
                virtualKeyboard.rebuildCustomKeys();
            }
        }
    }

    private boolean isModifier(int code) {
        return code == 16 || code == 17 || code == 18 || code == 524 || code == 91 || code == 92;  
    }
    private void executeMapping(int init, int fin) {
        Main.codeToCode.put(init, fin);
        ConfigManager.save(); // Save to JSON
        
        String nameInit = getKeyName(init);
        String nameFin = getKeyName(fin);

        model.addRow(new Object[]{ nameInit, nameFin });
        keymap.clear();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof KeyButton) {
            KeyButton clickedBtn = (KeyButton) e.getSource();
            int keyCode = clickedBtn.getKeyCode();

            if (keyCode == 0) {
                JOptionPane.showMessageDialog(this, "The Fn key cannot be remapped nor can its keypresses be recorded.", "Error", JOptionPane.ERROR_MESSAGE);
                return; 
            }
            
            if (visualStep == 1) { 
                if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
                sourceKeyBtn = clickedBtn;
                
                if (VirtualKeyboard.isShifted && VirtualKeyboard.isKeyShifted(keyCode)) {
                    String baseName = VirtualKeyboard.getName(keyCode);
                    String customName = "Shift+" + baseName;
                    
                    java.util.List<Integer> combo = new java.util.ArrayList<>();
                    combo.add(16); // Shift
                    combo.add(keyCode);

                    CustomKey existing = null;
                    for (CustomKey ck : CustomKeyManager.customKeys) {
                        if (ck.matches(new java.util.HashSet<>(combo))) {
                            existing = ck;
                            break;                                                                                                                         
                        }                                  
                    }

                    if (existing == null) {                                                                                                                                                                                                                                                        
                        CustomKeyManager.add(customName, combo);
                        existing = CustomKeyManager.getByPseudoCode(
                            CustomKeyManager.customKeys.get(CustomKeyManager.customKeys.size()-1).getPseudoCode()
                        );
                        
                        existing.setHidden(true);
                        virtualKeyboard.rebuildCustomKeys();
                    }                                                          
                                                                                        
                    sourceKeyBtn.putClientProperty("isShiftedCombo", true);
                    sourceKeyBtn.putClientProperty("shiftedPseudoCode", existing.getPseudoCode());
                }

                sourceKeyBtn.setSelectedSource();
                visualStep = 2; 
            } 
            else if (visualStep == 2) { 
                if (clickedBtn == sourceKeyBtn) {
                    resetVisualSelection();
                    return; 
                }
                if (destKeyBtn != null) destKeyBtn.resetColor();
                destKeyBtn = clickedBtn;
                destKeyBtn.setSelectedDest();
                confirmVisualBtn.setEnabled(true);
            }
        }
    }

    private void resetVisualSelection() {
    if (sourceKeyBtn != null) sourceKeyBtn.resetStyle();
    if (destKeyBtn != null) destKeyBtn.resetStyle();
    
    createMapping.setBackground(orgColor);
    sourceKeyBtn = null;
    destKeyBtn = null;
    visualStep = 0;
    confirmVisualBtn.setEnabled(false);

    if (VirtualKeyboard.isHeatmapOn) {
        virtualKeyboard.repaintHeatmap();
    }
}

    private void toggleInputFields(boolean visible) {
        chooseKey.setVisible(visible);
        enterKeyToMap.setVisible(visible);
        chooseRemap.setVisible(visible);
        enterKeyToRemap.setVisible(visible);
        chooseKeyToRemap.setVisible(visible);
    }

    private void loadExistingMappings() {
        for (Map.Entry<Integer, Integer> entry : Main.codeToCode.entrySet()) {
            int init = entry.getKey();
            int fin = entry.getValue();
            model.addRow(new Object[]{ getKeyName(init), getKeyName(fin) });
        }
    }
    
    private String getKeyName(int code) {
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            return (ck != null) ? ck.getName() : "Unknown";
        }
        return VirtualKeyboard.getName(code);
    }
    
    public void removeMappingByPseudoCode(int customKeyCode) {
        String keyName = VirtualKeyboard.getName(customKeyCode);
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            String rowKey = (String) model.getValueAt(i, 0);
            String rowMap = (String) model.getValueAt(i, 1);
            
            if (rowKey.equals(keyName) || rowMap.equals(keyName)) {
                model.removeRow(i);
            }
        }
        
        Main.codeToCode.entrySet().removeIf(entry -> 
            entry.getKey() == customKeyCode || entry.getValue() == customKeyCode
        );
        
        ConfigManager.save();
    }
}