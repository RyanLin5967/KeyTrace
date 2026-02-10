import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class RemapperGUI extends JFrame implements ActionListener{
    private DefaultTableModel model;
    private JTable table;
    private HashMap<String, Integer> stringToKeyCode;
    private KeyMap keymap = new KeyMap();

    // UI Components
    private JTextField enterKeyToMap = new JTextField(20);
    private JTextField enterKeyToRemap = new JTextField(20);
    private JLabel chooseKey = new JLabel("choose the key to remap");
    private JLabel chooseRemap = new JLabel("choose the key it'll remap to");
    private JButton chooseKeyToRemap = new JButton("submit");

    private VirtualKeyboard virtualKeyboard;
    private int visualStep = 0; // 0: Idle, 1: Source Selected, 2: Dest Selected
    private KeyButton sourceKeyBtn = null;
    private KeyButton destKeyBtn = null;
    private JButton confirmVisualBtn = new JButton("Confirm Visual Mapping");

    public RemapperGUI() {
        setupData();
        initUI();
        loadExistingMappings();
    }

    private void setupData() {
        stringToKeyCode = new HashMap<>();
        stringToKeyCode.put("space", KeyEvent.VK_SPACE);
        stringToKeyCode.put("tab", KeyEvent.VK_TAB);
        stringToKeyCode.put("caps lock", KeyEvent.VK_CAPS_LOCK);
        stringToKeyCode.put("shift", KeyEvent.VK_SHIFT);
        stringToKeyCode.put("backspace", KeyEvent.VK_BACK_SPACE);
        stringToKeyCode.put("ctrl", KeyEvent.VK_CONTROL);
        stringToKeyCode.put("enter", 13); 
    }

    private void initUI() {
    setTitle("Keyremapper");
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setUndecorated(true);
    
    // Switch to BorderLayout for better organization
    setLayout(new BorderLayout());

    // --- 1. TOP PANEL (Existing Text Controls + New Layout Selector) ---
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topPanel.setBackground(new Color(240, 240, 240));

    // Existing buttons
    JButton createMapping = new JButton("create keymap");
    JButton removeMapping = new JButton("remove selected mapping");
    JButton removeAllMappings = new JButton("remove all mappings");

    createMapping.setFocusable(false);
    removeMapping.setFocusable(false);
    removeAllMappings.setFocusable(false);

    String[] layoutOptions = {"100%", "75%", "65%"};
    JComboBox<String> layoutSelector = new JComboBox<>(layoutOptions);
    layoutSelector.addActionListener(e -> {
        String selected = (String) layoutSelector.getSelectedItem();
        if ("100%".equals(selected)) virtualKeyboard.render100Percent();
        else if ("75%".equals(selected)) virtualKeyboard.render75Percent();
        else if ("65%".equals(selected)) virtualKeyboard.render65Percent();
    });

    confirmVisualBtn.setEnabled(false); 

    topPanel.add(createMapping);
    topPanel.add(removeMapping);
    topPanel.add(removeAllMappings);
    topPanel.add(new JLabel(" | Layout: "));
    topPanel.add(layoutSelector);
    topPanel.add(confirmVisualBtn);

    // --- 2. INPUT PANEL (Your existing text fields) ---
    JPanel inputPanel = new JPanel(new FlowLayout());
    inputPanel.add(chooseKey); inputPanel.add(enterKeyToMap);
    inputPanel.add(chooseRemap); inputPanel.add(enterKeyToRemap);
    inputPanel.add(chooseKeyToRemap);
    
    // Combine Top and Input panels into a North container
    JPanel northContainer = new JPanel(new GridLayout(2, 1));
    northContainer.add(topPanel);
    northContainer.add(inputPanel);

    // --- 3. CENTER: Virtual Keyboard ---
    // Pass 'this' as the ActionListener to handle key clicks
    virtualKeyboard = new VirtualKeyboard(this); 
    JScrollPane kbScroll = new JScrollPane(virtualKeyboard);

    // --- 4. EAST: The Table ---
    model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { 
            return false; 
        }
    };
    table = new JTable(model);
    table.setFocusable(false);
    table.setRowSelectionAllowed(true);
    JScrollPane tableScroll = new JScrollPane(table);
    tableScroll.setPreferredSize(new Dimension(300, 0));

    // --- ACTION LISTENERS (Text Logic - Your original code) ---

    createMapping.addActionListener(e -> {
        toggleInputFields(true);
        visualStep = 1; // Start visual selection mode as well
        JOptionPane.showMessageDialog(this, "Visual Mode: Click the physical key you want to change (turns RED).");
    });

    chooseKeyToRemap.addActionListener(e -> {
        String keyStr = enterKeyToMap.getText();
        String remapStr = enterKeyToRemap.getText();

        if (stringToKeyCode.containsKey(keyStr) && stringToKeyCode.containsKey(remapStr) 
            && !Main.codeToCode.containsKey(stringToKeyCode.get(keyStr))) {
            
            int init = stringToKeyCode.get(keyStr);
            int fin = stringToKeyCode.get(remapStr);
            executeMapping(init, fin);
            
            enterKeyToMap.setText("");
            enterKeyToRemap.setText("");
            toggleInputFields(false);
        } else {
            JOptionPane.showMessageDialog(this, "Key(s) is invalid or already mapped.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    });

    confirmVisualBtn.addActionListener(e -> {
        if (sourceKeyBtn != null && destKeyBtn != null) {
            executeMapping(sourceKeyBtn.getKeyCode(), destKeyBtn.getKeyCode());
            resetVisualSelection();
        }
    });

    removeMapping.addActionListener(e -> {
    int row = table.getSelectedRow();
    if (row != -1) {
        // 1. Get the name exactly as it appears in the table (e.g., "Enter")
        String keyNameInTable = (String) table.getValueAt(row, 0);
        
        // 2. Remove from the HashMap by comparing the "Friendly Name"
        Main.codeToCode.entrySet().removeIf(entry -> 
            VirtualKeyboard.getName(entry.getKey()).equals(keyNameInTable)
        );

        // 3. Remove from UI and update the file
        model.removeRow(row);
        Main.updateTextFile();
    }
});

    removeAllMappings.addActionListener(e -> {
        Main.codeToCode.clear();
        model.setRowCount(0);
        Main.clearFile();
    });

    // --- FINAL ASSEMBLY ---
    add(northContainer, BorderLayout.NORTH);
    add(kbScroll, BorderLayout.CENTER);
    add(tableScroll, BorderLayout.EAST);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true);
}

    /**
     * Shared method to handle both text and visual mapping execution
     */
    private void executeMapping(int init, int fin) {
        Main.codeToCode.put(init, fin);
        Main.saveSingleMapping(init, fin);
        String nameInit = VirtualKeyboard.getName(init);
        String nameFin = VirtualKeyboard.getName(fin);

        model.addRow(new Object[]{ nameInit, nameFin });
        
        keymap.clear();
    }

    /**
     * This handles clicks from the VirtualKeyboard's KeyButtons
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof KeyButton) {
            KeyButton clickedBtn = (KeyButton) e.getSource();
            
            if (visualStep == 1) { // Selecting Source
                if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
                sourceKeyBtn = clickedBtn;
                sourceKeyBtn.setSelectedSource();
                visualStep = 2;
            } else if (visualStep == 2) { // Selecting Destination
                if (destKeyBtn != null) destKeyBtn.resetColor();
                destKeyBtn = clickedBtn;
                destKeyBtn.setSelectedDest();
                confirmVisualBtn.setEnabled(true);
            }
        }
    }

    private void resetVisualSelection() {
        if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
        if (destKeyBtn != null) destKeyBtn.resetColor();
        sourceKeyBtn = null;
        destKeyBtn = null;
        visualStep = 0;
        confirmVisualBtn.setEnabled(false);
    }
    private void toggleInputFields(boolean visible) {
        chooseKey.setVisible(visible);
        enterKeyToMap.setVisible(visible);
        chooseRemap.setVisible(visible);
        enterKeyToRemap.setVisible(visible);
        chooseKeyToRemap.setVisible(visible);
    }

    private void loadExistingMappings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/mappings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] codes = line.split(",");
                int init = Integer.parseInt(codes[0]);
                int fin = Integer.parseInt(codes[1]);
                Main.codeToCode.put(init, fin);
                model.addRow(new Object[]{ VirtualKeyboard.getName(init), VirtualKeyboard.getName(fin) });
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
