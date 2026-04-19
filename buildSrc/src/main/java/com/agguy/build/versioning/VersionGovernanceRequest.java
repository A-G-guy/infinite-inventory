package com.agguy.build.versioning;

import java.util.Map;

public record VersionGovernanceRequest(
        String currentVersion,
        Map<String, String> environment,
        String baseRefOverride
) {
}
