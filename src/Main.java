import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.MSG;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import java.util.HashMap;
import java.util.Map;

import java.io.*; //should this be only the imports you need?




import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

//MAKE UI LOOK VERY GOOD OR ELSE YOU FAIL
//add ability for combination later (copilot key)
public class Main implements NativeKeyListener {
    static final int IS_INJECTED = 16;
    static Robot robot;  
    static HashMap<Integer, Integer> codeToCode = new HashMap<>(); 
    
    public void simulateKeyPress(int code, boolean pressed) throws AWTException{
        if (code <= 0) {
            return;
        }
        if(code == 13){
                code = 10;
            }
        if (pressed){   
            robot = new Robot();
            robot.keyPress(code);
        }else{       
            robot.keyRelease(code);
        }
    }      
    public void nativeKeyPressed(NativeKeyEvent e) {  
        
    // have to not block injected keys but block the physical keypress 
        WinUser.LowLevelKeyboardProc keyboardHook = new WinUser.LowLevelKeyboardProc() {
            
            public LRESULT callback(int nCode, WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
                // If nCode is >= 0, we can process the event
                // need to check if the key is simulated by robot class or not 
                if (nCode >= 0) {            
                    boolean isInjected = (info.flags & IS_INJECTED) != 0;                                                                                                     
                    if (isInjected) {
                        return User32.INSTANCE.CallNextHookEx(null, nCode, wParam,
                                new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));   
                    }                           
                    int code = info.vkCode;
                    //for the visual keyboard thing, make left and right distinct not just check for both
                    if (code == 3675 || code == 3676 || code == 91 || code == 92) { 
                        code = KeyEvent.VK_WINDOWS; 
                    }
                    if(code == 160 || code == 161){
                        code = KeyEvent.VK_SHIFT;
                    } 
                    if(code == 162 || code == 163){
                        code = KeyEvent.VK_CONTROL;
                    }
                    if(code == 164 || code == 165){
                        code = KeyEvent.VK_ALT;
                    }
                    if(code == 10){
                        code = 13;
                    }     
                    if (codeToCode.containsKey(code)){    
                        int event = wParam.intValue(); 
                        
                        if (event == WinUser.WM_KEYDOWN || event == WinUser.WM_SYSKEYDOWN) {
                            try {
                                simulateKeyPress(codeToCode.get(code), true);
                            } catch (AWTException e) {
                                e.printStackTrace();
                            }
                        }  else if (event == WinUser.WM_KEYUP || event == WinUser.WM_SYSKEYUP) {
                            try{
                                simulateKeyPress(codeToCode.get(code), false);
                            }catch(AWTException e){
                                e.printStackTrace();
                            }   
                        }
                        //return here
                        return new LRESULT(1);
                    }
                }   
                                                                                                                                              
                return User32.INSTANCE.CallNextHookEx(null, nCode, wParam,
                        new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));
            }
        };
                                                   
        HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        HHOOK hhk = User32.INSTANCE.SetWindowsHookEx(
                WinUser.WH_KEYBOARD_LL,
                keyboardHook,
                hMod,
                0 // 0 means it hooks all threads (global)
        );

        if (hhk == null) {
            System.err.println("Failed to install hook.");
            return;
        }

        MSG msg = new MSG();
        int result;
        while ((result = User32.INSTANCE.GetMessage(msg, null, 0, 0)) != 0) {
            if (result == -1) {
                break;
            }
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        User32.INSTANCE.UnhookWindowsHookEx(hhk);

        // use esc key to stop program
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void nativeKeyReleased(NativeKeyEvent e) {
		System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}

	public void nativeKeyTyped(NativeKeyEvent e) {
        
        //System.out.println("Key Typed: " + e.getRawCode()); for testing 
        //remove this shit later
    //     if (e.getRawCode() >= 65 && e.getRawCode() <= 90){
    //         System.out.println("Key Typed: " + Character.toUpperCase((char)e.getRawCode()));
    //     // have to manually map space + backspace since it will return "Undefined" otherwise
    //     }else if(e.getRawCode() == 32) {
    //         System.out.println("Key Typed: "+ "Space");
    //     }else if(e.getRawCode() == 8){
    //         System.out.println("Key Typed: "+ "Backspace");
    //     }
	}
    public static void main(String[] args){
        //also need to allow users to put their unique keycodes
        //or allow users to see their keycodes
        //don't forget to clear keymap object after 

        //load everything into the hashmap:
        /* 
        DefaultTableModel model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };        
        JTable table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        JScrollPane scroll = new JScrollPane(table);

        try (BufferedReader reader = new BufferedReader(new FileReader("src/mappings.txt"))){
            String line;
            while ((line = reader.readLine()) != null){
                if (line.trim().isEmpty()){
                    continue;
                }
                String[] codes = line.split(",");
                int initKeyCode = Integer.parseInt(codes[0]);
                int finalKeyCode = Integer.parseInt(codes[1]);// change here
                model.addRow(new Object[] {KeyEvent.getKeyText(initKeyCode), KeyEvent.getKeyText(finalKeyCode)});

                codeToCode.put(initKeyCode, finalKeyCode);
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        
        KeyMap keymap = new KeyMap();
        HashMap<String, Integer> stringToKeyCode = new HashMap<>();
        stringToKeyCode.put("space", KeyEvent.VK_SPACE);
        stringToKeyCode.put("tab", KeyEvent.VK_TAB);
        stringToKeyCode.put("caps lock",KeyEvent.VK_CAPS_LOCK);
        stringToKeyCode.put("shift",KeyEvent.VK_SHIFT);
        stringToKeyCode.put("backspace",KeyEvent.VK_BACK_SPACE);
        stringToKeyCode.put("ctrl", KeyEvent.VK_CONTROL);
         
        JFrame frame = new JFrame("Keyremapper");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JButton chooseKeyToMap = new JButton("submit");
        JLabel chooseKey = new JLabel("choose the key to remap");
        JTextField enterKeyToMap = new JTextField(20);

        JButton chooseKeyToRemap = new JButton("submit");
        JLabel chooseRemap = new JLabel("choose the key it'll remap to");
        JTextField enterKeyToRemap = new JTextField(20);     
        
        JButton removeAllMappings = new JButton("remove all mappings");

        JButton removeMapping = new JButton("remove selected mapping");

        


        chooseKey.setVisible(false);
        chooseKeyToMap.setVisible(false);
        enterKeyToMap.setVisible(false);

        chooseKeyToRemap.setVisible(false);
        chooseRemap.setVisible(false);
        enterKeyToRemap.setVisible(false);

        JButton createMapping = new JButton("create keymap");
        frame.add(createMapping);
        createMapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                chooseKey.setVisible(true);
                //chooseKeyToMap.setVisible(true);
                enterKeyToMap.setVisible(true);
                chooseRemap.setVisible(true);
                enterKeyToRemap.setVisible(true); 
                chooseKeyToRemap.setVisible(true);
            }
        });
        
        removeMapping.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                int selectedRow = table.getSelectedRow();
                if(selectedRow != -1){
                    String keyToRemove = (String) table.getValueAt(selectedRow, 0); //change this 

                    Integer idToRemove = null;
                    for(Integer id: codeToCode.keySet()){
                        if(KeyEvent.getKeyText(id).equals(keyToRemove)){
                            idToRemove = id;
                        }
                    }
                    if (idToRemove != null){
                        codeToCode.remove(idToRemove);
                        ((DefaultTableModel) table.getModel()).removeRow(selectedRow);
                        updateTextFile();
                    }
                }
            }
        });
        removeAllMappings.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                codeToCode.clear();
                model.setRowCount(0);
                try(BufferedWriter writer = new BufferedWriter(new FileWriter("src/mappings.txt"))){
                    writer.write("");
                    writer.close();      
                }catch(IOException ex){}
            }
        });
        /* 
        //allow user to get the key they want to map
        chooseKeyToMap.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String keyToMap = enterKeyToMap.getText();
                enterKeyToMap.setText("");
                if (stringToKeyCode.containsKey(keyToMap)){
                    int code = stringToKeyCode.get(keyToMap);
                    keymap.setInitKeyCode(code);
                    chooseRemap.setVisible(true);
                    enterKeyToRemap.setVisible(true); 
                    chooseKeyToRemap.setVisible(true);
                } else{
                    enterKeyToMap.setText("Error in registering the key you want. ensure it is spelled correctly with spaces if nessesary"); //change this
                }
            }
        });
        
        chooseKeyToRemap.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String keyToMap = enterKeyToMap.getText();
                enterKeyToMap.setText("");
                String remapKey = enterKeyToRemap.getText();
                if (stringToKeyCode.containsKey(keyToMap) && stringToKeyCode.containsKey(remapKey) && !codeToCode.containsKey(stringToKeyCode.get(keyToMap))){
                    int initKeyCode = stringToKeyCode.get(keyToMap);
                    keymap.setInitKeyCode(initKeyCode);
                    enterKeyToRemap.setText("");
                    int finalKeyCode = stringToKeyCode.get(remapKey);
                    keymap.setFinalKeyCode(finalKeyCode);
                    codeToCode.put(keymap.getInitKeyCode(), keymap.getFinalKeyCode());
                    try(BufferedWriter writer = new BufferedWriter(new FileWriter("src/mappings.txt", true))){
                        writer.write(keymap.getInitKeyCode() + "," + keymap.getFinalKeyCode()); //change here
                        writer.newLine();
                        writer.close();     
                    }catch(IOException ex){
                        ex.printStackTrace();
                    }
                    model.addRow(new Object[] {KeyEvent.getKeyText(initKeyCode), KeyEvent.getKeyText(finalKeyCode)});
                    chooseKey.setVisible(false);
                    chooseKeyToMap.setVisible(false);
                    enterKeyToMap.setVisible(false);
                    // find a way to make this shit more efficient this is repeated too much. maybe put all into a box and make box invis/visible
                    chooseKeyToRemap.setVisible(false);
                    chooseRemap.setVisible(false);
                    enterKeyToRemap.setVisible(false);
                    keymap.clear();
                }else{
                    JOptionPane.showMessageDialog(
                        frame,                  
                        "Key(s) is invalid or key has already been mapped.", 
                        "Input Error",            
                        JOptionPane.ERROR_MESSAGE 
                    );
                    enterKeyToMap.setText("");
                    enterKeyToRemap.setText("");
                }
            }
        });    
        frame.add(chooseKey);
        frame.add(chooseKeyToMap);
        frame.add(enterKeyToMap);

        frame.add(chooseRemap);
        frame.add(enterKeyToRemap);
        frame.add(chooseKeyToRemap);

        frame.add(removeAllMappings);
        frame.add(removeMapping);
        frame.add(scroll);
        
        frame.setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        try{
            GlobalScreen.registerNativeHook();
        } catch(NativeHookException ex){
            System.err.println("error registering native hook");
            ex.printStackTrace();
        }
        GlobalScreen.addNativeKeyListener(new Main());
        */
        SwingUtilities.invokeLater(() -> new RemapperGUI());
        try{
            GlobalScreen.registerNativeHook();
        } catch(NativeHookException ex){
            System.err.println("error registering native hook");
            ex.printStackTrace();
        }          
        GlobalScreen.addNativeKeyListener(new Main());
    }
    public static void updateTextFile(){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/mappings.txt"))) {
            for (Map.Entry<Integer, Integer> entry: codeToCode.entrySet()) { 
                String line = entry.getKey() + "," + entry.getValue();
                writer.write(line);
                writer.newLine(); 
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }       
    }

    public static void saveSingleMapping(int init, int result) { 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/mappings.txt", true))) {
                writer.write(init + "," + result);
                writer.newLine(); 
        } catch (IOException ex) {
            ex.printStackTrace(); 
        }
    }    
    public static void clearFile() { 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/mappings.txt"))) { 
            writer.write(""); 
        } catch (IOException ex) { 
                ex.printStackTrace(); 
        } 
    }
}
