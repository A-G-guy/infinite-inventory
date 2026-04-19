package com.agguy.build.versioning;

public record BaselineSelection(
        String label,
        String refName,
        String revisionRange,
        SemanticVersion version
) {
}
