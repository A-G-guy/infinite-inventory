package com.agguy.build.versioning;

import java.util.List;
import java.util.stream.Collectors;

public final class VersionGovernanceReport {
    private final String baselineLabel;
    private final String revisionRange;
    private final SemanticVersion baselineVersion;
    private final SemanticVersion currentVersion;
    private final SemanticVersion recommendedVersion;
    private final ReleaseImpact highestImpact;
    private final List<AnalyzedCommit> analyzedCommits;
    private final List<AnalyzedCommit> releaseImpactCommits;
    private final boolean passed;
    private final String failureMessage;

    public VersionGovernanceReport(
            String baselineLabel,
            String revisionRange,
            SemanticVersion baselineVersion,
            SemanticVersion currentVersion,
            SemanticVersion recommendedVersion,
            ReleaseImpact highestImpact,
            List<AnalyzedCommit> analyzedCommits,
            boolean passed,
            String failureMessage
    ) {
        this.baselineLabel = baselineLabel;
        this.revisionRange = revisionRange;
        this.baselineVersion = baselineVersion;
        this.currentVersion = currentVersion;
        this.recommendedVersion = recommendedVersion;
        this.highestImpact = highestImpact;
        this.analyzedCommits = List.copyOf(analyzedCommits);
        this.releaseImpactCommits = this.analyzedCommits.stream()
                .filter(commit -> commit.impact().isReleaseRequired())
                .toList();
        this.passed = passed;
        this.failureMessage = failureMessage;
    }

    public boolean passed() {
        return passed;
    }

    public String failureMessage() {
        return failureMessage;
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Mod Version Governance").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- 基线: `").append(baselineLabel).append("` (`").append(baselineVersion).append("`)").append(System.lineSeparator());
        builder.append("- 分析范围: `").append(revisionRange).append("`").append(System.lineSeparator());
        builder.append("- 当前 `mod_version`: `").append(currentVersion).append("`").append(System.lineSeparator());
        builder.append("- 最低推荐版本: `").append(recommendedVersion).append("`").append(System.lineSeparator());
        builder.append("- 最高变更级别: `").append(highestImpact.displayName()).append("`").append(System.lineSeparator());
        builder.append("- 结果: `").append(passed ? "PASS" : "FAIL").append("`").append(System.lineSeparator());
        if (!passed && failureMessage != null && !failureMessage.isBlank()) {
            builder.append(System.lineSeparator())
                    .append("> ")
                    .append(failureMessage)
                    .append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("## 触发版本推进的提交").append(System.lineSeparator()).append(System.lineSeparator());
        if (releaseImpactCommits.isEmpty()) {
            builder.append("- 无。当前范围内没有 `feat`、`fix` 或 breaking change。").append(System.lineSeparator());
        } else {
            for (AnalyzedCommit commit : releaseImpactCommits) {
                builder.append("- `")
                        .append(shortHash(commit.hash()))
                        .append("` `")
                        .append(commit.impact().displayName())
                        .append("` ")
                        .append(commit.subject())
                        .append(System.lineSeparator());
            }
        }
        long ignoredCount = analyzedCommits.size() - releaseImpactCommits.size();
        if (ignoredCount > 0) {
            builder.append(System.lineSeparator())
                    .append("忽略了 `")
                    .append(ignoredCount)
                    .append("` 个无发版要求的提交。")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    public String toConsoleText() {
        StringBuilder builder = new StringBuilder();
        builder.append("版本治理报告").append(System.lineSeparator());
        builder.append("基线: ").append(baselineLabel).append(" (").append(baselineVersion).append(")").append(System.lineSeparator());
        builder.append("分析范围: ").append(revisionRange).append(System.lineSeparator());
        builder.append("当前 mod_version: ").append(currentVersion).append(System.lineSeparator());
        builder.append("最低推荐版本: ").append(recommendedVersion).append(System.lineSeparator());
        builder.append("最高变更级别: ").append(highestImpact.displayName()).append(System.lineSeparator());
        builder.append("结果: ").append(passed ? "PASS" : "FAIL").append(System.lineSeparator());
        if (!releaseImpactCommits.isEmpty()) {
            builder.append("需要发版的提交:").append(System.lineSeparator());
            builder.append(releaseImpactCommits.stream()
                    .map(commit -> "- " + shortHash(commit.hash()) + " [" + commit.impact().displayName() + "] " + commit.subject())
                    .collect(Collectors.joining(System.lineSeparator())));
            builder.append(System.lineSeparator());
        }
        if (!passed && failureMessage != null && !failureMessage.isBlank()) {
            builder.append(failureMessage).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private static String shortHash(String hash) {
        return hash.length() <= 7 ? hash : hash.substring(0, 7);
    }
}
