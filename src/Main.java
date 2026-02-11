import com.sun.jna.Library; 
import com.sun.jna.Native;  
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.io.*; 

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Main implements NativeKeyListener {
    static final int IS_INJECTED = 16;
    static Robot robot;  
    static HashMap<Integer, Integer> codeToCode = new HashMap<>(); 
    private static Set<Integer> heldKeys = new HashSet<>();
    
    private static CustomKey activeCustomKey = null; 
    private static int activeTargetCode = -1;      

    public static volatile boolean isRecording = false; 
    public static LinkedHashSet<Integer> recordingBuffer = new LinkedHashSet<>(); 
    
    public interface Win32User32 extends Library {
        Win32User32 INSTANCE = Native.load("user32", Win32User32.class);
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
    }

    private void cleanCopilotModifiers() {
        if (robot == null) {
            try { robot = new Robot(); } catch (AWTException e) {}
        }
        if (robot != null) {
            robot.keyRelease(KeyEvent.VK_WINDOWS);
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }

    private void doSafePress(int code, boolean pressed) throws AWTException {
        if (code == 13) code = 10;
        
        if (code == 134 || code == KeyEvent.VK_F23) {
            byte vk = (byte) 134; 
            byte scan = (byte) 0x6E; 
            int flags = pressed ? 0 : 2; 
            Win32User32.INSTANCE.keybd_event(vk, scan, flags, 0);
            return;
        }

        if (robot == null) robot = new Robot();
        if (pressed) robot.keyPress(code);
        else robot.keyRelease(code);
    }

    public void simulateCombo(CustomKey ck, boolean pressed) throws AWTException {
        if (ck != null) {
            if (pressed) {
                for (int c : ck.getRawCodes()) doSafePress(c, true);
            } else {
                for (int c : ck.getRawCodes()) doSafePress(c, false);
            }
        }
    }

    public void simulateKeyPress(int code, boolean pressed) throws AWTException {
        if (code <= 0) return;
        if (code == 13) code = 10;
        
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            simulateCombo(ck, pressed);
            return; 
        }
        doSafePress(code, pressed);
    }     

    public void nativeKeyPressed(NativeKeyEvent e) {
        WinUser.LowLevelKeyboardProc keyboardHook = new WinUser.LowLevelKeyboardProc() {
            public LRESULT callback(int nCode, WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
                if (nCode >= 0) {            
                    boolean isInjected = (info.flags & 16) != 0; 
                    if (isInjected) {
                        return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));   
                    }                   
                    
                    int code = info.vkCode;
                    boolean isPress = (wParam.intValue() == WinUser.WM_KEYDOWN || wParam.intValue() == WinUser.WM_SYSKEYDOWN);
                    boolean isRelease = (wParam.intValue() == WinUser.WM_KEYUP || wParam.intValue() == WinUser.WM_SYSKEYUP);

                    if (code == 3675 || code == 3676 || code == 91 || code == 92) code = KeyEvent.VK_WINDOWS;
                    if(code == 160 || code == 161) code = KeyEvent.VK_SHIFT;
                    if(code == 162 || code == 163) code = KeyEvent.VK_CONTROL;
                    if(code == 164 || code == 165) code = KeyEvent.VK_ALT;
                    if(code == 10) code = 13;

                    if (isRecording) {
                        if (isPress) recordingBuffer.add(code);
                        return new LRESULT(1); 
                    }

                    if (isPress) heldKeys.add(code);
                    else if (isRelease) heldKeys.remove(code);

                    if (isRelease && activeCustomKey != null) {
                        if (activeCustomKey.getRawCodes().contains(code)) {
                            try {
                                simulateKeyPress(activeTargetCode, false); 
                            } catch (Exception ex) {}
                            
                            activeCustomKey = null;
                            activeTargetCode = -1;
                            return new LRESULT(1);
                        }
                    }
                    if (isPress && activeCustomKey == null) {
                        CustomKey matched = CustomKeyManager.match(heldKeys);
                        if (matched != null) {
                            int pseudoID = matched.getPseudoCode();
                            if (codeToCode.containsKey(pseudoID)) {
                                activeCustomKey = matched;
                                activeTargetCode = codeToCode.get(pseudoID);
                                
                                if (matched.getRawCodes().contains(134)) {
                                    cleanCopilotModifiers();
                                }
                                
                                try {
                                    simulateKeyPress(activeTargetCode, true); 
                                } catch (Exception ex) {}
                                
                                return new LRESULT(1); 
                            }
                        }
                    }

                    if (isPress && activeCustomKey != null) {
                        if (activeCustomKey.getRawCodes().contains(code)) {
                            return new LRESULT(1);
                        }
                    }

                    if (codeToCode.containsKey(code)){    
                        if (code == 134) cleanCopilotModifiers();

                        if (isPress) {
                            try { simulateKeyPress(codeToCode.get(code), true); } catch (AWTException ex) {}
                        } else if (isRelease) {
                            try { simulateKeyPress(codeToCode.get(code), false); } catch (AWTException ex) {}   
                        }
                        return new LRESULT(1);
                    }
                }   
                return User32.INSTANCE.CallNextHookEx(null, nCode, wParam, new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));
            }
        };
                                                    
        HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        HHOOK hhk = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0);

        if (hhk == null) return;

        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }
        User32.INSTANCE.UnhookWindowsHookEx(hhk);
    }
    
    public void nativeKeyReleased(NativeKeyEvent e) {}
    public void nativeKeyTyped(NativeKeyEvent e) {}

    public static void main(String[] args){
        CustomKeyManager.load();
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