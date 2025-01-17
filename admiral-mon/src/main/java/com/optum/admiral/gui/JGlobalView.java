package com.optum.admiral.gui;

import com.optum.admiral.io.LogStreamer;
import com.optum.admiral.key.TabKey;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class JGlobalView extends JTabbedPane {
    private final Font font;
    private final Color foreground;
    private final Color background;
    private final Map<TabKey, JContainerView> followingContainers = new TreeMap<>();

    JGlobalView(Font font, Color foreground, Color background) {
        this.font = font;
        this.foreground = foreground;
        this.background = background;
        this.setTabPlacement(JTabbedPane.TOP);
    }

    public void createTabForContainerStream_OnGUI(LogStreamer logStreamer) {
        TabKey containerName = logStreamer.getPrimaryKey().containerName;
        TabKey tabKey = logStreamer.getPrimaryKey().streamName;
        JContainerView jContainerView = followingContainers.get(containerName);
        if (jContainerView != null) {
        } else {
            jContainerView = new JContainerView();
            followingContainers.put(containerName, jContainerView);
            // Isn't it lovely how tabs can't be inserted, only added?  To order them, delete them all and add them back.
            removeAll();
            for(Map.Entry<TabKey, JContainerView> entry : followingContainers.entrySet()) {
                add(entry.getKey().tabName, entry.getValue());
            }
        }
        jContainerView.createTabForStream_OnGUI(tabKey, logStreamer, font, foreground, background);
    }

    private TabKey getTabKeyForContainerName(String containerName) {
        for(TabKey tabKey : followingContainers.keySet()) {
            if (tabKey.tabName.equals(containerName))
                return tabKey;
        }
        return null;
    }

    public void removeTabForContainer_OnGUI(String containerName) {
        TabKey tabKey = getTabKeyForContainerName(containerName);
        if (tabKey == null) {
            return;
        }
        JContainerView jContainerView = followingContainers.get(tabKey);
        if (jContainerView == null) {
            return;
        }

        this.remove(jContainerView);
    }

}
