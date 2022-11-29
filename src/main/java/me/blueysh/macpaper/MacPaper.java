package me.blueysh.macpaper;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MacPaper {
    public static final Logger logger = Logger.getLogger("MacPaper");
    public static final Path homeFolder = Path.of(System.getProperty("user.home"));
    public static final Path appFolder = homeFolder.resolve("Library/Application Support/MacPaper");
    static FrameCacher cacher;
    static BackgroundRenderer renderer = new BackgroundRenderer();
    protected static List<String> pargs;

    public static void main(String[] args) throws UnsupportedOperatingSystemException {
        logger.info("Starting MacPaper");
        pargs = Arrays.stream(args).toList();
        if (checkOS()) run();
        else throw new UnsupportedOperatingSystemException("MacPaper is only designed to run on macOS.");
    }

    private static boolean checkOS() {
        return (System.getProperty("os.name").toLowerCase().contains("mac"));
    }

    private static void run() throws UnsupportedOperatingSystemException {
        MacPaperOptions.saveDefault();
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
                            cacher = new FrameCacher(backgroundFile);
                            renderer.init();
                            renderer.start();
                        } else {
                            logger.warning("No supported file type found.");
                            JOptionPane.showMessageDialog(new Frame(), "Could not find a valid background file! Background files should\nbe named 'background.gif'!");
                            System.exit(-1);
                        }
            } else {
                logger.warning("MacPaper data folder is empty!");
                JOptionPane.showMessageDialog(new Frame(), "The MacPaper data folder has no background file in it!", "MacPaper Error", JOptionPane.PLAIN_MESSAGE);
                System.exit(-1);
            }

            TrayIcon MacPaper_TRAY_ELEMENT;
            if (SystemTray.isSupported()) {

                SystemTray tray = SystemTray.getSystemTray();
                Image MacPaperIcon = Toolkit.getDefaultToolkit().getImage(MacPaper.class.getResource("/desktop_mac_.png"));

                JOptionPane.showMessageDialog(new Frame(), "MacPaper is active! Click the Screen icon at the top of the screen to freeze/unfreeze or close MacPaper.", "MacPaper", JOptionPane.PLAIN_MESSAGE);

                PopupMenu menu = new PopupMenu("MacPaper Menu");
                MenuItem[] items = new MenuItem[]{
                        new MenuItem("Freeze/Unfreeze"),
                        new MenuItem("Open App Folder"),
                        new MenuItem("Open Frame Cache Folder"),
                        new MenuItem("Report A Bug"),
                        new MenuItem("Close MacPaper")
                };
                for (MenuItem i:items) {
                    i.addActionListener(MacPaperActionListener.get());
                    menu.add(i);
                }

                MacPaper_TRAY_ELEMENT = new TrayIcon(MacPaperIcon, "MacPaper", menu);

                try {
                    tray.add(MacPaper_TRAY_ELEMENT);
                } catch (AWTException ex) {
                    logger.warning("Something went wrong when starting MacPaper!");
                    ex.printStackTrace();
                    System.exit(-1);
                }

            } else throw new UnsupportedOperatingSystemException("This system does not support MacPaper!");

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

class MacPaperActionListener {
    public static ActionListener get() {
        return e -> {
            MacPaper.logger.info("Action::"+e.getActionCommand());
            switch (e.getActionCommand().toLowerCase()) {
                case "close macpaper" -> {
                    MacPaper.logger.info("Closing MacPaper");
                    System.exit(0);
                }
                case "open frame cache folder" -> {
                    MacPaper.logger.info("Opening frame cache folder");
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", MacPaper.appFolder.resolve("cachedFrames").toString()});
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                case "open app folder" -> {
                    MacPaper.logger.info("Opening app data folder");
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", MacPaper.appFolder.toString()});
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                case "report a bug" -> {
                    MacPaper.logger.info("Opening MacPaper GitHub page");
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", "https://github.com/blueysh/MacPaper/issues"});
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                case "freeze/unfreeze" -> {
                    if (MacPaper.renderer.isRendering) MacPaper.renderer.stop();
                    else MacPaper.renderer.start();
                }
                default -> MacPaper.logger.warning("Unknown MacPaper action registered.");
            }
        };
    }
}