package com.agguy.build.versioning;

public record AnalyzedCommit(
        String hash,
        String subject,
        ReleaseImpact impact,
        String reason
) {
}
