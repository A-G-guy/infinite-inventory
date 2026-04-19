package com.agguy.build.versioning;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern STABLE_SEMVER = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final int major;
    private final int minor;
    private final int patch;

    public SemanticVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("版本号不能包含负数。");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemanticVersion parse(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("版本号不能为空。");
        }
        String normalized = rawValue.trim();
        Matcher matcher = STABLE_SEMVER.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("仅支持稳定版 semver，当前值为: " + normalized);
        }
        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    public SemanticVersion bump(ReleaseImpact impact) {
        return switch (impact) {
            case NONE -> this;
            case PATCH -> new SemanticVersion(major, minor, patch + 1);
            case MINOR -> new SemanticVersion(major, minor + 1, 0);
            case MAJOR -> new SemanticVersion(major + 1, 0, 0);
        };
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        int minorCompare = Integer.compare(this.minor, other.minor);
        if (minorCompare != 0) {
            return minorCompare;
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SemanticVersion that)) {
            return false;
        }
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
