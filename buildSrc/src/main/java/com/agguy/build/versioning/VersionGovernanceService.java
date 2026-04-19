package com.agguy.build.versioning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VersionGovernanceService {
    private static final String VERSION_FILE_PATH = "gradle.properties";

    private final ConventionalCommitAnalyzer analyzer;

    public VersionGovernanceService() {
        this(new ConventionalCommitAnalyzer());
    }

    public VersionGovernanceService(ConventionalCommitAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public VersionGovernanceReport analyze(RepositoryInspector repositoryInspector, VersionGovernanceRequest request) {
        SemanticVersion currentVersion = SemanticVersion.parse(request.currentVersion());
        BaselineSelection baseline = resolveBaseline(repositoryInspector, request);
        List<GitCommit> commits = repositoryInspector.listCommits(baseline.revisionRange());
        List<AnalyzedCommit> analyzedCommits = new ArrayList<>(commits.size());
        ReleaseImpact highestImpact = ReleaseImpact.NONE;
        for (GitCommit commit : commits) {
            AnalyzedCommit analyzedCommit = analyzer.analyze(commit);
            analyzedCommits.add(analyzedCommit);
            highestImpact = ReleaseImpact.max(highestImpact, analyzedCommit.impact());
        }
        SemanticVersion recommendedVersion = baseline.version().bump(highestImpact);
        boolean passed = currentVersion.compareTo(recommendedVersion) >= 0;
        String failureMessage = passed
                ? ""
                : "当前 mod_version=" + currentVersion + " 低于最低推荐值 " + recommendedVersion
                + "，请先更新 gradle.properties 中的 mod_version。";
        return new VersionGovernanceReport(
                baseline.label(),
                baseline.revisionRange(),
                baseline.version(),
                currentVersion,
                recommendedVersion,
                highestImpact,
                analyzedCommits,
                passed,
                failureMessage
        );
    }

    private BaselineSelection resolveBaseline(RepositoryInspector repositoryInspector, VersionGovernanceRequest request) {
        String override = normalize(request.baseRefOverride());
        if (!override.isEmpty()) {
            return resolveBranchBaseline(repositoryInspector, override);
        }

        Map<String, String> environment = request.environment();
        String eventName = environment.getOrDefault("GITHUB_EVENT_NAME", "");
        String baseRef = normalize(environment.get("GITHUB_BASE_REF"));
        if (eventName.startsWith("pull_request") && !baseRef.isEmpty()) {
            return resolveBranchBaseline(repositoryInspector, baseRef);
        }

        String refName = normalize(environment.get("GITHUB_REF_NAME"));
        if ("push".equals(eventName) && "main".equals(refName)) {
            return resolveTagBaseline(repositoryInspector);
        }

        if (repositoryInspector.refExists("refs/remotes/origin/main")) {
            return resolveBranchBaseline(repositoryInspector, "main");
        }
        return resolveTagBaseline(repositoryInspector);
    }

    private BaselineSelection resolveBranchBaseline(RepositoryInspector repositoryInspector, String baseRef) {
        String resolvedRef = resolveRefName(repositoryInspector, baseRef);
        String mergeBase = repositoryInspector.mergeBase("HEAD", resolvedRef);
        SemanticVersion baselineVersion = extractVersion(repositoryInspector.readFile(resolvedRef, VERSION_FILE_PATH));
        return new BaselineSelection(
                resolvedRef,
                resolvedRef,
                mergeBase + "..HEAD",
                baselineVersion
        );
    }

    private BaselineSelection resolveTagBaseline(RepositoryInspector repositoryInspector) {
        String tag = normalize(repositoryInspector.describeLatestVersionTag("HEAD"));
        if (tag.isEmpty()) {
            throw new IllegalStateException("未找到可用的 v* 版本基线 tag，请先创建例如 v1.0.0 的标注 tag。");
        }
        String versionText = tag.startsWith("v") ? tag.substring(1) : tag;
        return new BaselineSelection(tag, tag, tag + "..HEAD", SemanticVersion.parse(versionText));
    }

    private String resolveRefName(RepositoryInspector repositoryInspector, String baseRef) {
        for (String candidate : candidateRefs(baseRef)) {
            if (repositoryInspector.refExists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("未找到基线分支引用: " + baseRef + "。请确认远端分支已抓取。");
    }

    private List<String> candidateRefs(String baseRef) {
        List<String> candidates = new ArrayList<>();
        if (baseRef.startsWith("refs/")) {
            candidates.add(baseRef);
        }
        if (baseRef.startsWith("origin/")) {
            candidates.add("refs/remotes/" + baseRef);
            candidates.add(baseRef);
        } else {
            candidates.add("refs/remotes/origin/" + baseRef);
            candidates.add("origin/" + baseRef);
            candidates.add("refs/heads/" + baseRef);
            candidates.add(baseRef);
        }
        return candidates;
    }

    private SemanticVersion extractVersion(String gradlePropertiesContent) {
        for (String line : gradlePropertiesContent.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("mod_version=")) {
                return SemanticVersion.parse(trimmed.substring("mod_version=".length()).trim());
            }
        }
        throw new IllegalStateException("无法从 gradle.properties 解析 mod_version。");
    }

    private static String normalize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }
}
