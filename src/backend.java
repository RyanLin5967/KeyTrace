import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.awt.Robot;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class backend implements NativeKeyListener{
    
    //maybe make it return string
    public void nativeKeyPressed(NativeKeyEvent e){
        System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        //use esc key to stop program
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE){
            try{
                GlobalScreen.unregisterNativeHook();
            } catch(NativeHookException ex){
                ex.printStackTrace();
            }
        }
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
		System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}

	public void nativeKeyTyped(NativeKeyEvent e) {
        //System.out.println("Key Typed: " + e.getRawCode()); for testing 
        if (e.getRawCode() >= 65 && e.getRawCode() <=90){
            System.out.println("Key Typed: " + Character.toUpperCase((char)e.getRawCode()));
        // have to manually map space + backspace since it will return "Undefined" otherwise
        }else if(e.getRawCode() == 32) {
            System.out.println("Key Typed: "+ "Space");
        }else if(e.getRawCode() == 8){
            System.out.println("Key Typed: "+ "Backspace");
        }
	}
    public static void main(String[] args){
        try{
            GlobalScreen.registerNativeHook();
        } catch(NativeHookException ex){
            System.err.println("error registering native hook");
            ex.printStackTrace();
        }
        GlobalScreen.addNativeKeyListener(new backend());
    }
}
