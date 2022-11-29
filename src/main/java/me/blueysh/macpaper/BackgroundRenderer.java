package me.blueysh.macpaper;

import javax.swing.JOptionPane;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class BackgroundRenderer {
    int frameIndex = 0;
    protected boolean isRendering = true;
    private final TimerTask renderTask = new TimerTask() {
        @Override
        public void run() {
            if (isRendering) renderBackground();
        }
    };

    public void start() {
        isRendering = true;
    }

    public void stop() {
        isRendering = false;
    }

    public void init() {
        long delay = (long) (new MacPaperOptions().getFrameRate() * 1000f);

        MacPaper.logger.info("Render delay set to " + delay + " milliseconds / " + ((float) delay / 1000f) + " seconds.");
        new Timer().scheduleAtFixedRate(renderTask, delay, delay);
    }

    public void renderBackground() {
        File frame;
        frame = Paths.get(MacPaper.class.getResource("/MacWall_NoFrameFoundError.png").getPath()).toFile();
        File directory = MacPaper.appFolder.resolve("cachedFrames").toFile();
        if (!directory.isDirectory() || !directory.exists()) {
            MacPaper.logger.warning("cachedFrames location was not a directory! Exiting.");
            JOptionPane.showMessageDialog(new Frame(), "cachedFrames location was not a directory! Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
        ArrayList<File> fileList = new ArrayList<>();
        for (File f:directory.listFiles()) {
            if (!f.getName().contains(".gif") || f.isDirectory()) f.delete();
            else fileList.add(f);
        }
        Collections.sort(fileList);

        try {
            if ((frameIndex + 1) <= fileList.size()) {
                for (File f : fileList) {
                    if (f.getName().equals(frameIndex + ".gif")) frame = f;
                }
                MacPaper.logger.info("Rendered frame " + frameIndex + " : " + frame.getName() + "@" + frame.getAbsolutePath());
                frameIndex++;
            } else {
                frameIndex = 0;
                for (File f:fileList) {
                    if (f.getName().equals(frameIndex + ".gif")) frame = f;
                }
                MacPaper.logger.info("Rendered frame " + frameIndex + " : " + frame.getName() + "@" + frame.getAbsolutePath());
                frameIndex++;
            }
        } catch (IndexOutOfBoundsException ex) {
            frameIndex = 0;
            for (File f:fileList) {
                if (f.getName().equals(frameIndex + ".gif")) frame = f;
            }
            MacPaper.logger.info("Rendered frame " + frameIndex + " : " + frame.getName() + "@" + frame.getAbsolutePath());
            frameIndex++;
        }

        String[] as = {
                "osascript",
                "-e", "tell application \"Finder\"",
                "-e", "set desktop picture to POSIX file \"" + frame.getAbsolutePath() + "\"",
                "-e", "end tell"
        };

        try {
            Runtime.getRuntime().exec(as);
        } catch (IOException ex) {
            MacPaper.logger.warning("error caught");
            JOptionPane.showMessageDialog(new Frame(), "An error occurred and MacWall must quit.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
    }
}
