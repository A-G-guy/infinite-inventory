package com.agguy.build.versioning;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConventionalCommitAnalyzer {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([a-z]+)(\\([^)]*\\))?(!)?:\\s+.+$");
    private static final Pattern BREAKING_PATTERN = Pattern.compile("(?m)^BREAKING CHANGE:\\s+.+$");

    public AnalyzedCommit analyze(GitCommit commit) {
        String subject = sanitize(commit.subject());
        String body = sanitize(commit.body());
        String hash = sanitize(commit.hash());
        Matcher matcher = HEADER_PATTERN.matcher(subject);
        if (!matcher.matches()) {
            return new AnalyzedCommit(hash, subject, ReleaseImpact.NONE, "未匹配 Conventional Commit 头。");
        }

        String commitType = matcher.group(1).toLowerCase(Locale.ROOT);
        boolean breakingInHeader = matcher.group(3) != null;
        boolean breakingInBody = BREAKING_PATTERN.matcher(body).find();
        if (breakingInHeader || breakingInBody) {
            return new AnalyzedCommit(hash, subject, ReleaseImpact.MAJOR, "检测到 breaking change 标记。");
        }
        if ("feat".equals(commitType)) {
            return new AnalyzedCommit(hash, subject, ReleaseImpact.MINOR, "feat 提交需要 minor 版本推进。");
        }
        if ("fix".equals(commitType)) {
            return new AnalyzedCommit(hash, subject, ReleaseImpact.PATCH, "fix 提交需要 patch 版本推进。");
        }
        return new AnalyzedCommit(hash, subject, ReleaseImpact.NONE, commitType + " 提交默认不要求发版。");
    }

    private static String sanitize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }
}
