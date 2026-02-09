import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class RemapperGUI extends JFrame {
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
    }

    private void initUI() {
        setTitle("Keyremapper");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setLayout(new FlowLayout());

        model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        JScrollPane scroll = new JScrollPane(table);

        JButton createMapping = new JButton("create keymap");
        JButton removeMapping = new JButton("remove selected mapping");
        JButton removeAllMappings = new JButton("remove all mappings");

        createMapping.setFocusPainted(false);
        removeMapping.setFocusPainted(false);
        removeAllMappings.setFocusPainted(false);

        // Set visibility off for input fields initially
        toggleInputFields(false);

        // --- ACTION LISTENERS ---

        createMapping.addActionListener(e -> toggleInputFields(true));

        chooseKeyToRemap.addActionListener(e -> {
            String keyStr = enterKeyToMap.getText();
            String remapStr = enterKeyToRemap.getText();

            if (stringToKeyCode.containsKey(keyStr) && stringToKeyCode.containsKey(remapStr) 
                && !Main.codeToCode.containsKey(stringToKeyCode.get(keyStr))) {
                
                int init = stringToKeyCode.get(keyStr);
                int fin = stringToKeyCode.get(remapStr);

                Main.codeToCode.put(init, fin);
                Main.saveSingleMapping(init, fin);
                
                model.addRow(new Object[]{KeyEvent.getKeyText(init), KeyEvent.getKeyText(fin)});
                
                enterKeyToMap.setText("");
                enterKeyToRemap.setText("");
                toggleInputFields(false);
                keymap.clear();
            } else {
                JOptionPane.showMessageDialog(this, "Key(s) is invalid or already mapped.", "Error", JOptionPane.ERROR_MESSAGE);
                //clear the fields or no?
            }
        });

        removeMapping.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String keyName = (String) table.getValueAt(row, 0);
                Main.codeToCode.entrySet().removeIf(entry -> KeyEvent.getKeyText(entry.getKey()).equals(keyName));
                model.removeRow(row);
                Main.updateTextFile();
            }
        });

        removeAllMappings.addActionListener(e -> {
            Main.codeToCode.clear();
            model.setRowCount(0);
            Main.clearFile();
        });

        add(createMapping);
        add(chooseKey); add(enterKeyToMap);
        add(chooseRemap); add(enterKeyToRemap);
        add(chooseKeyToRemap);
        add(removeMapping);
        add(removeAllMappings);
        add(scroll);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
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
                model.addRow(new Object[]{KeyEvent.getKeyText(init), KeyEvent.getKeyText(fin)});
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
