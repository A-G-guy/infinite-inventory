package com.agguy.build.versioning;

public enum ReleaseImpact {
    NONE,
    PATCH,
    MINOR,
    MAJOR;

    public static ReleaseImpact max(ReleaseImpact left, ReleaseImpact right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public boolean isReleaseRequired() {
        return this != NONE;
    }

    public String displayName() {
        return name().toLowerCase();
    }
}
