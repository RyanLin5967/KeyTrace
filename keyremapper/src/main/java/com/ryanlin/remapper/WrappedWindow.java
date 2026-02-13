package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class WrappedWindow extends JFrame {
    private WrappedData data;
    
    // State
    private int currentSlideIndex = 0;
    private int revealStage = 0; 
    private boolean isFinished = false;

    // Visuals
    private ParticlePanel mainPanel; 
    private JLabel mainText;
    private JLabel subText;
    private JLabel extraText;
    
    // TIMERS (The Fix: Track them so we can kill them)
    private Timer backgroundLoopTimer;
    private Timer activeCountUpTimer; // <--- NEW: Tracks text animations

    // Images
    private BufferedImage currentImage1 = null;
    private BufferedImage currentImage2 = null;
    private BufferedImage currentImage3 = null;

    // Bars
    private JPanel barsPanel; 
    private List<AnimatedBar> statBars = new ArrayList<>();

    public WrappedWindow(Frame owner) {
        super("2026 Wrapped");
        
        try {
            this.data = WrappedAnalyzer.analyze();
        } catch (Exception e) {
            this.data = new WrappedData(); 
            this.data.topKeyName = "Space";
        }

        // Setup
        setUndecorated(true); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(0,0);
        setAlwaysOnTop(true);

        // Main Panel
        mainPanel = new ParticlePanel();
        mainPanel.setLayout(new GridBagLayout()); 
        this.setContentPane(mainPanel); 

        initContentComponents();

        // Inputs
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                advanceStory();
            }
        });
        
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "close");
        mainPanel.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // Loop
        loadSlide(0);
        
        backgroundLoopTimer = new Timer(16, e -> {
            mainPanel.setImages(currentImage1, currentImage2);
            mainPanel.updateParticles();
            mainPanel.repaint();
        });
        backgroundLoopTimer.start();
        
        setVisible(true);
    }
    
    // --- THE FIX: STOP OLD ANIMATIONS ---
    private void stopActiveAnimations() {
        if (activeCountUpTimer != null && activeCountUpTimer.isRunning()) {
            activeCountUpTimer.stop();
        }
    }

    private void initContentComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        mainText = new JLabel("", SwingConstants.CENTER);
        mainText.setFont(new Font("Arial", Font.BOLD, 100)); 
        mainText.setForeground(Color.WHITE);
        mainPanel.add(mainText, gbc);

        gbc.gridy++;
        subText = new JLabel("", SwingConstants.CENTER);
        subText.setFont(new Font("Arial", Font.PLAIN, 40));
        subText.setForeground(Color.LIGHT_GRAY);
        mainPanel.add(subText, gbc);

        gbc.gridy++;
        barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setOpaque(false);
        mainPanel.add(barsPanel, gbc);

        gbc.gridy++;
        extraText = new JLabel("", SwingConstants.CENTER);
        extraText.setFont(new Font("Arial", Font.ITALIC, 20));
        extraText.setForeground(Color.GRAY);
        mainPanel.add(extraText, gbc);
    }

    private void advanceStory() {
        if (isFinished) { dispose(); return; }

        if (revealStage == 0) {
            revealStage = 1;
            animateReveal(); 
        } else {
            currentSlideIndex++;
            revealStage = 0;
            loadSlide(currentSlideIndex);
        }
    }

    private void loadSlide(int index) {
        // CRITICAL: Stop previous animations before loading new slide
        stopActiveAnimations();
        
        mainText.setText("");
        subText.setText("");
        extraText.setText("");
        barsPanel.removeAll();
        statBars.clear();
        currentImage1 = null; 
        currentImage2 = null;
        
        mainText.setFont(new Font("Arial", Font.BOLD, 60));
        mainText.setForeground(Color.WHITE);
        mainPanel.setTheme(ParticlePanel.THEME_DEFAULT);

        switch (index) {
            case 0:
                mainText.setText("2026 WRAPPED");
                extraText.setText("Click to start");
                revealStage = 1; 
                break;
            case 1:
                mainText.setText("Activity Summary");
                break;
            case 2:
                mainText.setText("Most Used Key");
                break;
            case 3:
                mainText.setText("Top 5 Keys"); 
                break;
            case 4:
                mainText.setText("Your Archetype");
                break;
            case 5:
                mainText.setText("See you in 2027!");
                extraText.setText("Click to close");
                isFinished = true;
                break;
            default: dispose();
        }
    }

    private void animateReveal() {
        // CRITICAL: Stop animations here too, just in case
        stopActiveAnimations();
        
        switch (currentSlideIndex) {
            case 1: // Total
                mainPanel.setTheme(ParticlePanel.THEME_RAIN);
                animateCountUp(mainText, data.totalPresses, "");
                subText.setText("Total Keypresses");
                break;
                
            case 2: // Top Key
                mainText.setFont(new Font("Arial", Font.BOLD, 140));
                mainText.setForeground(new Color(255, 80, 80)); 
                mainText.setText(data.topKeyName); // <--- This won't get overwritten now
                
                animateCountUp(subText, data.topKeyCount, " presses");
                
                mainPanel.setTheme(ParticlePanel.THEME_FIRE);
                break;
                
            case 3: // Top 5 Bars
                mainText.setText("The Leaderboard");
                subText.setText("");
                mainPanel.setTheme(ParticlePanel.THEME_DEFAULT);
                
                long maxVal = data.top5Keys.isEmpty() ? 1 : data.top5Keys.get(0).getValue();
                Color[] palette = {
                    new Color(255, 80, 80), new Color(255, 160, 80),  
                    new Color(255, 220, 80), new Color(100, 255, 100), new Color(80, 160, 255)
                };

                int i = 0;
                for (java.util.Map.Entry<String, Long> entry : data.top5Keys) {
                    AnimatedBar bar = new AnimatedBar(entry.getKey(), entry.getValue(), maxVal, palette[i % palette.length]);
                    barsPanel.add(bar);
                    barsPanel.add(Box.createVerticalStrut(10));
                    statBars.add(bar);
                    i++;
                }
                barsPanel.revalidate();
                for (AnimatedBar bar : statBars) bar.animate();
                break;

            case 4: // Archetype
                String type = data.archetype;
                mainText.setText(type.toUpperCase());
                subText.setText(data.archetypeDescription);
                
                mainPanel.spawnConfetti(); // Boom

                if (type.contains("Gamer")) {
                    mainText.setFont(new Font("Impact", Font.BOLD, 100));
                    mainText.setForeground(new Color(255, 50, 50)); 
                    mainPanel.setTheme(ParticlePanel.THEME_GAMER);

                    currentImage1 = loadImage("src/resources/gamer_3.png"); 
                    currentImage2 = loadImage("src/resources/gamer_2.png"); 
                    
                } else if (type.contains("Developer")) {
                    mainText.setFont(new Font("Consolas", Font.BOLD, 80));
                    mainText.setForeground(Color.CYAN);
                    mainPanel.setTheme(ParticlePanel.THEME_MATRIX);
                    
                    currentImage1 = loadImage("dev_1.png"); 
                    currentImage2 = loadImage("dev_2.png"); 
                    
                } else {
                    mainText.setFont(new Font("Serif", Font.BOLD, 90));
                    mainText.setForeground(Color.ORANGE);
                    mainPanel.setTheme(ParticlePanel.THEME_GOLD);
                }
                break;
        }
    }
    
    private BufferedImage loadImage(String path) {
        try { return ImageIO.read(new File(path)); } catch (Exception e) { return null; }
    }

    private void animateCountUp(JLabel label, long target, String suffix) {
        // Stop any old timer first
        stopActiveAnimations();

        activeCountUpTimer = new Timer(20, null);
        long start = System.currentTimeMillis();
        
        activeCountUpTimer.addActionListener(e -> {
            long now = System.currentTimeMillis();
            float p = (now - start) / 1000f; 
            if (p >= 1f) {
                label.setText(String.format("%,d%s", target, suffix));
                activeCountUpTimer.stop();
            } else {
                long val = (long)(target * Math.pow(p, 3)); 
                label.setText(String.format("%,d%s", val, suffix));
            }
        });
        activeCountUpTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WrappedWindow(null));
    }
}