import javax.swing.*;
import java.awt.*;

public class KeyButton extends JButton {
    private final int keyCode;
    private final Color defaultColor;

    // Now we pass width and height explicitly
    public KeyButton(String text, int keyCode, int width, int height) {
        super(text);
        this.keyCode = keyCode;
        
        this.defaultColor = new Color(245, 245, 245);
        this.setBackground(defaultColor);
        this.setFocusable(false);
        this.setFont(new Font("Arial", Font.BOLD, 12));
        
        this.setPreferredSize(new Dimension(width, height));

        this.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void resetColor() {
        setBackground(defaultColor);
    }

    public void setSelectedSource() {
        setBackground(new Color(255, 100, 100)); // Red
    }

    public void setSelectedDest() {
        setBackground(new Color(100, 255, 100)); // Green 
    }
}