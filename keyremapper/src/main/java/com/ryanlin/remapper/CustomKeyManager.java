package com.ryanlin.remapper;

import java.io.*;
import java.util.*;

public class CustomKeyManager {
    public static List<CustomKey> customKeys = new ArrayList<>();
    private static int nextPseudoCode = 10000; 

    public static void load() {
        customKeys.clear();
        File f = new File("src/custom_keys.txt");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(new File("src/custom_keys.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                CustomKey ck = CustomKey.fromFileString(line);
                if (ck != null) {
                    customKeys.add(ck);
                    if (ck.getPseudoCode() >= nextPseudoCode) nextPseudoCode = ck.getPseudoCode() + 1;
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/custom_keys.txt"))) {
            for (CustomKey ck : customKeys) {
                bw.write(ck.toFileString());
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static CustomKey add(String name, List<Integer> codes) {
        CustomKey ck = new CustomKey(name, nextPseudoCode++, codes);
        customKeys.add(ck);
        save();
        return ck;
    }

    public static void remove(CustomKey key) {
        customKeys.remove(key);
        save();
    }

    public static CustomKey getByPseudoCode(int code) {
        for (CustomKey ck : customKeys) if (ck.getPseudoCode() == code) return ck;
        return null;
    }

    // matches if currently held keys contain all the keys required for this custom key
    public static CustomKey match(Set<Integer> pressedKeys) {
        for (CustomKey ck : customKeys) {
            if (pressedKeys.containsAll(ck.getRawCodes())) return ck;
        }
        return null;
    }
}