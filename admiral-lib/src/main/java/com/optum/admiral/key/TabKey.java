package com.optum.admiral.key;

/**
 * We want to sort categories, then tabs.  It basically makes a way for stdout/stderr to be at top.
 */
public class TabKey implements Comparable<TabKey> {
    public final String groupName;
    public final String tabName;

    public TabKey(String groupName, String tabName) {
        this.groupName = groupName;
        this.tabName = tabName;
    }

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;
        if (!(o instanceof TabKey))
            return false;
        TabKey other = (TabKey) o;
        return groupName.equals(other.groupName) && tabName.equals(other.tabName);
    }

    @Override
    public int hashCode() {
        return groupName.hashCode() + tabName.hashCode();
    }

    @Override
    public int compareTo(TabKey o) {
        int first = groupName.compareTo(o.groupName);
        if (first!=0)
            return first;
        return tabName.compareTo(o.tabName);
    }
}
