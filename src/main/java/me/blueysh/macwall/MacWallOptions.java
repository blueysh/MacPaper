package me.blueysh.macwall;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;

public class MacWallOptions {
    // default value
    private float frameRate = 0.5f;

    public MacWallOptions() {
        try {
            BufferedReader reader = Files.newBufferedReader(MacWall.appFolder.resolve("options.txt"));

            for (String s:reader.lines().toList()) {
                if (s.startsWith("secondsPerFrame")) {
                    frameRate = Float.parseFloat(s.replace("secondsPerFrame=", ""));
                }
            }

            reader.close();
        } catch (IOException ex) {
            MacWall.logger.warning("Failed to read from the options file!");
            JOptionPane.showMessageDialog(new Frame(), "Failed to read from the options file! Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        } catch (NumberFormatException ex) {
            MacWall.logger.warning("Malformed options file!");
            JOptionPane.showMessageDialog(new Frame(), "Invalid number entered for a field! Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
    }

    public float getFrameRate() { return frameRate; }

    public static void saveDefault() {
        try {
            if (!MacWall.appFolder.resolve("options.txt").toFile().exists()) {
                BufferedWriter writer = Files.newBufferedWriter(MacWall.appFolder.resolve("options.txt"));

                writer.write("* secondsPerFrame - The time in seconds that the renderer will wait before rendering a new frame.\n");
                writer.write("* This value is set to 0.5 seconds per frame by default.\n");
                writer.write("secondsPerFrame=0.5");

                writer.close();
            } else {
                MacWall.logger.info("options.txt already exists; skipping creation.");
            }
        } catch (IOException e) {
            MacWall.logger.warning("Failed to write default options.txt!");
            JOptionPane.showMessageDialog(new Frame(), "Failed to write default options.txt file to app folder! Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
