package com.optum.admiral.gui;

import com.optum.admiral.io.LogStreamer;
import com.optum.admiral.key.TabKey;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * UI Thread-only Class
 */
public class JContainerView extends JTabbedPane  {
    private final Map<TabKey, StreamViewHandle> trackedContainers = new TreeMap<>();

    JContainerView() {
        this.setTabPlacement(JTabbedPane.LEFT);
    }

    public void createTabForStream_OnGUI(TabKey tabKey, LogStreamer logStreamer, Font font, Color foreground, Color background) {
        StreamViewHandle streamViewHandle = trackedContainers.get(tabKey);
        if (streamViewHandle !=null) {
            // Already have one.  Relink.  This happens if you stop/start a container.
            trackedContainers.remove(tabKey);
        }
        streamViewHandle = new StreamViewHandle(font, foreground, background);
        trackedContainers.put(tabKey, streamViewHandle);
        logStreamer.addListener(new JLogStreamerListener(streamViewHandle));
        // Isn't it lovely how tabs can't be inserted, only added?  To order them, delete them all and add them back.
        removeAll();
        for(Map.Entry<TabKey, StreamViewHandle> entry : trackedContainers.entrySet()) {
            add(entry.getKey().tabName, entry.getValue().getJComponent());
        }
    }

}
