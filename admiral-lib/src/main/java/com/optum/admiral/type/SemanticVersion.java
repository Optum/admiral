package com.optum.admiral.type;

import com.optum.admiral.type.exception.InvalidSemanticVersion;

public class SemanticVersion implements Comparable<SemanticVersion> {
    public final int major;
    public final int minor;
    public final int patch;

    public SemanticVersion(final int major, final int minor, final int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemanticVersion parse(String s) throws InvalidSemanticVersion {
        if (s==null)
            return null;

        String[] pieces = s.split("\\.");
        if (pieces.length != 3)
            throw new InvalidSemanticVersion(s);

        try {
            int major = Integer.parseInt(pieces[0]);
            int minor = Integer.parseInt(pieces[1]);
            int patch = Integer.parseInt(pieces[2]);
            return new SemanticVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new InvalidSemanticVersion(s);
        }
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public int hashCode() {
        return (major<<16) + (minor<<8) + patch;
    }

    @Override
    public boolean equals(Object o) {
        if (o==null)
            return false;

        if (!(o instanceof SemanticVersion))
            return false;

        SemanticVersion other = (SemanticVersion)o;
        return (major==other.major) && (minor==other.minor) && (patch==other.patch);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        final int first = major - other.major;
        if (first != 0)
            return first;

        final int second = minor - other.minor;
        if (second != 0)
            return second;

        return patch - other.patch;
    }

    public boolean atLeast(SemanticVersion other) {
        return (this.compareTo(other)>=0);
    }

    public boolean atMost(SemanticVersion other) {
        return (this.compareTo(other)<=0);
    }

}
