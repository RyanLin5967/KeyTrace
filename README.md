# Keyremapper

A Windows key remapping tool with a virtual keyboard GUI, heatmap tracking, custom key combos, and a "2026 Wrapped" year-in-review for your keyboard usage.

Built with Java Swing + JNA for low-level keyboard hooks.

---

## Demo

https://youtu.be/kiBghic8V1o

---

## Features

### Key Remapping
Remap any key to any other key using a visual point-and-click workflow — click "Create Mapping," select a source key on the virtual keyboard, select a destination key, and confirm. Mappings persist across sessions via `config.json`. Remappings work system-wide through a low-level Windows keyboard hook.

### Virtual Keyboard
An interactive on-screen keyboard that supports three layouts: **100%** (full-size with numpad), **75%**, and **65%**. Keys are clickable for creating mappings and visually highlight source/destination selections.

### Heatmap
Toggle a live heatmap overlay on the virtual keyboard. Keys are colored from cool to hot red based on press frequency, updated every second. Hover over any key to see its exact press count. The heatmap also tracks shifted key variants (e.g. `Shift+9` for `(`) when Shift mode is toggled.

### Custom Keys
Record multi-key combinations (e.g. `Ctrl+Shift+F5`) and save them as named custom keys. Custom keys appear in a dedicated panel below the keyboard and can be used as either the source or destination of a mapping. Right-click to delete.

### Shift Mode
Toggle Shift mode to view and interact with shifted key labels (`!`, `@`, `#`, `{`, etc.). Shifted variants can be used as mapping sources and have their own independent heatmap counts.

### 2026 Wrapped
A full-screen, animated year-in-review of your keyboard usage — inspired by Spotify Wrapped. Slides include:

- **Total keypresses** with an animated count-up
- **Most used key** with a fire particle theme
- **Top 5 leaderboard** with animated bar charts
- **Your archetype** — The Gamer, The Developer, The Editor, The Vibe Coder, or The Generalist — determined by your usage patterns, complete with themed particle effects and confetti

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven**
- **Windows** (uses JNA for a Win32 low-level keyboard hook)

### Build & Run

```
mvn clean package
java -jar target/keyremapper-1.0-SNAPSHOT.jar
```

Or on Windows, just run:

```
.\r.bat
```
- **You can also just download and run the .jar file in releases**
---

## How It Works

The app installs a system-wide low-level keyboard hook (`WH_KEYBOARD_LL`) via JNA. When a remapped key is pressed, the original keypress is consumed and the mapped key is simulated using `java.awt.Robot` and `keybd_event`. Key repeat behavior is handled with a scheduled executor to match natural hold-to-repeat timing.

All mappings, custom keys, and heatmap data are saved to `config.json` and auto-saved every 5 minutes.

---

## Project Structure

```
src/main/java/com/ryanlin/remapper/
├── Main.java              # Entry point, keyboard hook, key simulation
├── RemapperGUI.java       # Main window and UI logic
├── VirtualKeyboard.java   # On-screen keyboard rendering and heatmap
├── KeyButton.java         # Individual key button component
├── KeyRecorder.java       # Dialog for recording key combinations
├── CustomKey.java         # Custom key data model
├── CustomKeyManager.java  # Custom key registry
├── HeatmapManager.java    # Keystroke counting and heatmap data
├── ConfigManager.java     # JSON persistence (Gson)
├── AppConfig.java         # Serialization model
├── WrappedWindow.java     # 2026 Wrapped presentation UI
├── WrappedAnalyzer.java   # Analyzes heatmap data for Wrapped
├── WrappedData.java       # Wrapped statistics model
├── ParticlePanel.java     # Particle effects and themes
├── AnimatedLabel.java     # Typewriter and count-up label animations
└── AnimatedBar.java       # Animated horizontal bar chart
```

---

## License

[MIT](LICENSE)
