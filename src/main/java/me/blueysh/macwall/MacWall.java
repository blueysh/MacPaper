package me.blueysh.macwall;

import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.AWTException;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MacWall {
    public static final Logger logger = Logger.getLogger("MacWall");
    public static final Path homeFolder = Path.of(System.getProperty("user.home"));
    public static final Path appFolder = homeFolder.resolve("Library/Application Support/MacWall");
    static VideoStream stream;

    public static void main(String[] args) throws UnsupportedOperatingSystemException {
        logger.info("Starting MacWall");
        if (checkOS()) run();
        else throw new UnsupportedOperatingSystemException("MacWall is only designed to run on macOS.");
    }

    private static boolean checkOS() {
        return (System.getProperty("os.name").toLowerCase().contains("mac"));
    }

    private static void run() throws UnsupportedOperatingSystemException {
        MacWallOptions.saveDefault();
        if (Files.exists(appFolder)) {
            if (!Files.exists(appFolder.resolve("data"))) try {
                Files.createDirectory(appFolder.resolve("data"));
            } catch (IOException ex) {
                logger.warning("Failed to create data directory!");
                ex.printStackTrace();
                System.exit(-1);
            }

            List<File> files = Arrays.stream(appFolder.resolve("data").toFile().listFiles()).toList();

            if (!files.isEmpty()) {
                        File backgroundFile = null;

                        for (File file:files) {
                            if (file.getName().startsWith("background")) backgroundFile = file;
                            else backgroundFile = null;
                        }

                        if (backgroundFile != null) {
                            stream = new VideoStream(backgroundFile);
                            stream.init();
                        } else {
                            logger.warning("No supported file type found.");
                            JOptionPane.showMessageDialog(new Frame(), "Could not find a valid background file! Background files should\nbe named 'background' and be of type 'mp4' or 'gif'!");
                            System.exit(-1);
                        }
            } else {
                logger.warning("MacWall data folder is empty!");
                JOptionPane.showMessageDialog(new Frame(), "The MacWall data folder has no background file in it!", "MacWall Error", JOptionPane.PLAIN_MESSAGE);
                System.exit(-1);
            }

            TrayIcon MACWALL_TRAY_ELEMENT;
            if (SystemTray.isSupported()) {

                SystemTray tray = SystemTray.getSystemTray();
                Image macWallIcon = Toolkit.getDefaultToolkit().getImage(MacWall.class.getResource("/desktop_mac_.png"));

                JOptionPane.showMessageDialog(new Frame(), "MacWall is active! Click the Screen icon at the top of the screen to freeze/unfreeze or close MacWall.", "MacWall", JOptionPane.PLAIN_MESSAGE);

                PopupMenu menu = new PopupMenu("MacWall");
                MenuItem[] items = new MenuItem[]{
                        new MenuItem("Freeze/Unfreeze"),
                        new MenuItem("Open App Folder"),
                        new MenuItem("Report A Bug"),
                        new MenuItem("Close MacWall")
                };
                for (MenuItem i:items) {
                    i.addActionListener(MacWallActionListener.get());
                    menu.add(i);
                }

                MACWALL_TRAY_ELEMENT = new TrayIcon(macWallIcon, "MacWall", menu);

                try {
                    tray.add(MACWALL_TRAY_ELEMENT);
                } catch (AWTException ex) {
                    logger.warning("Something went wrong when starting MacWall!");
                    ex.printStackTrace();
                    System.exit(-1);
                }

            } else throw new UnsupportedOperatingSystemException("This system does not support MacWall!");

        } else {
            logger.warning("No app data folder found, making a new one");
            try {
                Files.createDirectory(appFolder);
                Files.createDirectory(appFolder.resolve("data"));

                logger.info("App data folder created!");
            } catch (IOException ex) {
                logger.info("Failed to create app data folder!");
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }
}

class MacWallActionListener {
    public static ActionListener get() {
        return e -> {
            MacWall.logger.info("Action::"+e.getActionCommand());
            switch (e.getActionCommand().toLowerCase()) {
                case "close macwall" -> {
                    MacWall.logger.info("Closing MacWall");
                    System.exit(0);
                }
                case "open app folder" -> {
                    MacWall.logger.info("Opening app data folder");
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", MacWall.appFolder.toString()});
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                case "report a bug" -> {
                    MacWall.logger.info("Opening MacWall GitHub page");
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", "https://github.com/blueysh/macwall/issues"});
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                case "freeze/unfreeze" -> {
                    if (MacWall.stream.isRendering) MacWall.stream.stop();
                    else MacWall.stream.start();
                }
                default -> MacWall.logger.warning("Unknown MacWall action registered.");
            }
        };
    }
}