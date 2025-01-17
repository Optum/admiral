package com.optum.admiral.gui;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Keymap;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class StreamViewHandle {
    final JTextArea logText;
    final JScrollPane scrollPane;
    public StreamViewHandle(Font font, Color foreground, Color background) {
        logText = new JTextArea();
        if (font!=null)
            logText.setFont(font);
        if (foreground!=null) {
            logText.setForeground(foreground);
        }
        if (background!=null) {
            logText.setBackground(background);
        }
        String osName = System.getProperties().getProperty("os.name");
        if (osName.startsWith("Mac OS X")) {
            Keymap km = logText.getKeymap();
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                    InputEvent.META_DOWN_MASK);
            km.addActionForKeyStroke(ks, TransferHandler.getCopyAction());
        }

        DefaultCaret caret = (DefaultCaret)logText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        scrollPane = new JScrollPane(logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JComponent getJComponent() {
        return scrollPane;
    }

    public void addLine_OnGUI(String line) {
        boolean someoneIsDraggingTheScrollbar = scrollPane.getVerticalScrollBar().getValueIsAdjusting();
        logText.append(line);
        if (!someoneIsDraggingTheScrollbar) {
            logText.setCaretPosition(logText.getDocument().getLength());
        }
    }
}
