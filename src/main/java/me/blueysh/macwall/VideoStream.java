package me.blueysh.macwall;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class VideoStream {
    File backgroundFile;
    MacWallOptions options = new MacWallOptions();
    int frameIndex = 0;
    protected boolean isRendering = false;

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            if (isRendering) renderToBackground();
        }
    };

    public VideoStream(File backgroundFile) {
        MacWall.logger.info("GIF file found as " + backgroundFile.getAbsolutePath() + ".");
        this.backgroundFile = backgroundFile;
        try {
            if (MacWall.appFolder.resolve("cachedFrames").toFile().exists() || MacWall.appFolder.resolve("cachedFrames").toFile().isDirectory()) {
                for (File f:MacWall.appFolder.resolve("cachedFrames").toFile().listFiles()) f.delete();
                Files.deleteIfExists(MacWall.appFolder.resolve("cachedFrames"));
            }
            Files.createDirectory(MacWall.appFolder.resolve("cachedFrames"));
        } catch (IOException e) {
            MacWall.logger.warning("Failed to reset frame cache!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(new Frame(), "Failed to reset frame cache. Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
        MacWall.logger.info("Caching frames from " + backgroundFile.getName() + "..");
        JOptionPane.showMessageDialog(new Frame(), "MacWall needs to load your background before running. This may take a while.\nA dialog will be shown when MacWall is active. Press OK to start loading.", "MacWall", JOptionPane.PLAIN_MESSAGE);
        cacheFrames();
        isRendering = true;
    }

    public void init() {
        new Timer().schedule(task, (long)(options.getFrameRate() * 1000f));
    }

    public void start() {
        isRendering = true;
    }

    public void stop() {
        isRendering = false;
    }

    private void renderToBackground() {
        File frame;
        File directory = MacWall.appFolder.resolve("cachedFrames").toFile();
        if (!directory.isDirectory() || !directory.exists()) {
            MacWall.logger.warning("cachedFrames location was not a directory! Exiting.");
            JOptionPane.showMessageDialog(new Frame(), "cachedFrames location was not a directory! Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
        File[] files = directory.listFiles();
        for (File f:files) {
            if (!f.getName().contains(".gif")) f.delete();
        }

        try {
            frame = files[frameIndex];
            frameIndex++;
        } catch (IndexOutOfBoundsException ex) {
            frameIndex = 0;
            frame = files[frameIndex];
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
            MacWall.logger.warning("error caught");
            JOptionPane.showMessageDialog(new Frame(), "An error occurred and MacWall must quit.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
            System.exit(-1);
        }
    }

    private void cacheFrames() {
        try {
            String[] imageatt = new String[]{
                    "imageLeftPosition",
                    "imageTopPosition",
                    "imageWidth",
                    "imageHeight"
            };

            ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("gif").next();
            ImageInputStream ciis = ImageIO.createImageInputStream(backgroundFile);
            reader.setInput(ciis, false);

            int noi = reader.getNumImages(true);
            BufferedImage master = null;

            for (int i = 0; i < noi; i++) {
                BufferedImage image = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
                NodeList children = tree.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    Node nodeItem = children.item(j);

                    if(nodeItem.getNodeName().equals("ImageDescriptor")){
                        Map<String, Integer> imageAttr = new HashMap<String, Integer>();

                        for (int k = 0; k < imageatt.length; k++) {
                            NamedNodeMap attr = nodeItem.getAttributes();
                            Node attnode = attr.getNamedItem(imageatt[k]);
                            imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));
                        }
                        if(i==0){
                            master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
                        }
                        master.getGraphics().drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);
                    }
                }
                ImageIO.write(master, "GIF", MacWall.appFolder.resolve("cachedFrames").resolve(i + ".gif").toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}