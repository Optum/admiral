package com.optum.admiral.gui;

import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.prefs.Preferences;

public class ControllableFrame implements WindowListener {
    private String frameTitle;
    private JFrame frame_OnGUI;
    private JComponent contentPane;

    private final Preferences localStorage;

    public ControllableFrame(String frameTitle, Preferences localStorage, JComponent contentPane) {
        this.frameTitle = frameTitle;
        this.localStorage = localStorage;
        this.contentPane = contentPane;

        try {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                doShowGUI_OnGUI();
            }
        });
    }

    private String getScreenIdToUse() {
        return localStorage.get("DEVICE", null);
    }

    private void setWindowGeometryFromUserPreferences(GraphicsConfiguration graphicsConfiguration) {
        Rectangle rectangle = graphicsConfiguration.getBounds();
        // If this is the first time ever loading the GUI (or if one of the prefs get deleted) we set our dimentions
        // to 75% x 75% centered (centered = offset by 1/8th).
        int width = localStorage.getInt("WIDTH", Math.round(rectangle.width*0.75F));
        int height = localStorage.getInt("HEIGHT", Math.round(rectangle.height*0.75F));
        int x = localStorage.getInt("X", rectangle.x + Math.round(rectangle.width*0.125F));
        int y = localStorage.getInt("Y", rectangle.y + Math.round(rectangle.height*0.125F));

        // Make sure we are within the screen's dimensions.
        if (!rectangle.contains(x, y, width, height)) {
            x = Math.max(rectangle.x, x);
            y = Math.max(rectangle.y, y);
            width = Math.min(width, rectangle.width);
            height = Math.min(height, rectangle.height);
        }
        frame_OnGUI.setSize(width,height);
        frame_OnGUI.setLocation(x, y);
    }

    /**
     * Null means use default.
     */
    private GraphicsConfiguration getGraphicsConfiguration(String screenToUse) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Start with the default screen.
        GraphicsDevice graphicsDevice = ge.getDefaultScreenDevice();

        // If we are looking for a specific screen, try to find it.
        // If we can't find it, we'll end up using the default from above.
        if (screenToUse!=null) {
            GraphicsDevice[] screens = ge.getScreenDevices();
            for (GraphicsDevice screen : screens) {
                if (screenToUse.equals(screen.getIDstring())) {
                    graphicsDevice = screen;
                    break;
                }
            }
        }

        // Return the configuration for the chosen device.
        return graphicsDevice.getDefaultConfiguration();
    }

    private void saveWindowGeometryToUserPreferences() {
        int width = frame_OnGUI.getWidth();
        int height = frame_OnGUI.getHeight();
        int x = frame_OnGUI.getX();
        int y = frame_OnGUI.getY();
        GraphicsDevice device = frame_OnGUI.getGraphicsConfiguration().getDevice();
        localStorage.put("DEVICE", device.getIDstring());
        localStorage.putInt("WIDTH", width);
        localStorage.putInt("HEIGHT", height);
        localStorage.putInt("X", x);
        localStorage.putInt("Y", y);
    }

    // Here is the code if we decide to use it.
    private void clearUserPrefererences() {
        localStorage.remove("DEVICE");
        localStorage.remove("WIDTH");
        localStorage.remove("HEIGHT");
        localStorage.remove("X");
        localStorage.remove("Y");
    }

    private void doShowGUI_OnGUI() {
        if (frame_OnGUI == null) {
            String screenIdToUse = getScreenIdToUse();
            GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration(screenIdToUse);
            frame_OnGUI = new JFrame(frameTitle, graphicsConfiguration);
            frame_OnGUI.addWindowListener(this);
            frame_OnGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            setWindowGeometryFromUserPreferences(graphicsConfiguration);
            frame_OnGUI.getContentPane().add(contentPane, BorderLayout.PAGE_END);
//            frame_OnGUI.setPreferredSize(contentPane.getPreferredSize());
//            frame_OnGUI.revalidate();
            frame_OnGUI.setResizable(false);
            frame_OnGUI.pack();
            frame_OnGUI.setVisible(true);
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        shutdown_OnGUI();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public void shutdown() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                shutdown_OnGUI();
            }
        });
    }

    private void shutdown_OnGUI() {
        if (frame_OnGUI!=null) {
            saveWindowGeometryToUserPreferences();
            frame_OnGUI.setVisible(false);
            frame_OnGUI.dispose();
        }
    }
}
