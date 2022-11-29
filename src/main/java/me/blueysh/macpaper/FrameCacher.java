package me.blueysh.macpaper;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.Frame;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class FrameCacher {
    File backgroundFile;
    MacPaperOptions options = new MacPaperOptions();

    public FrameCacher(File backgroundFile) {
        MacPaper.logger.info("GIF file found as " + backgroundFile.getAbsolutePath() + ".");
        this.backgroundFile = backgroundFile;
        if (!MacPaper.pargs.contains("no-caching")) {
            try {
                if (MacPaper.appFolder.resolve("cachedFrames").toFile().exists() || MacPaper.appFolder.resolve("cachedFrames").toFile().isDirectory()) {
                    for (File f : MacPaper.appFolder.resolve("cachedFrames").toFile().listFiles()) f.delete();
                    Files.deleteIfExists(MacPaper.appFolder.resolve("cachedFrames"));
                }
                Files.createDirectory(MacPaper.appFolder.resolve("cachedFrames"));
            } catch (IOException e) {
                MacPaper.logger.warning("Failed to reset frame cache!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(new Frame(), "Failed to reset frame cache. Exiting.", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
                System.exit(-1);
            }
        }
        MacPaper.logger.info("Caching frames from " + backgroundFile.getName() + "..");
        JOptionPane.showMessageDialog(new Frame(), "MacWall needs to load your background before running. This may take a while.\nA dialog will be shown when MacWall is active. Press OK to start loading.", "MacWall", JOptionPane.PLAIN_MESSAGE);
        if (!MacPaper.pargs.contains("no-caching")) cacheFrames();
    }

    private void cacheFrames() {
        try {
            String[] imageatt = new String[]{
                    "imageLeftPosition",
                    "imageTopPosition",
                    "imageWidth",
                    "imageHeight"
            };

            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            ImageInputStream ciis = ImageIO.createImageInputStream(backgroundFile);
            reader.setInput(ciis, false);

            int noi = reader.getNumImages(true);
            BufferedImage master = null;

            JProgressBar bar = new JProgressBar(0, noi);
            bar.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            bar.setName("Loading");
            bar.setStringPainted(true);

            JFrame fr = new JFrame("MacWall");
            fr.setResizable(false);
            fr.setContentPane(bar);
            fr.pack();
            fr.setVisible(true);

            for (int i = 0; i < noi; i++) {
                bar.setValue(i);
                if ((i + 1) == noi) fr.setVisible(false);

                BufferedImage image = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
                NodeList children = tree.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    Node nodeItem = children.item(j);

                    if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                        Map<String, Integer> imageAttr = new HashMap<String, Integer>();

                        for (int k = 0; k < imageatt.length; k++) {
                            NamedNodeMap attr = nodeItem.getAttributes();
                            Node attnode = attr.getNamedItem(imageatt[k]);
                            imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));
                        }
                        if (i == 0) {
                            master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
                        }
                        master.getGraphics().drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);
                    }
                }
                ImageIO.write(master, "GIF", MacPaper.appFolder.resolve("cachedFrames").resolve(i + ".gif").toFile());
                MacPaper.logger.info("Cached frame " + i + " of " + noi);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}