package com.agguy.build.versioning;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionGovernanceServiceTest {
    private final VersionGovernanceService service = new VersionGovernanceService();

    @Test
    void pullRequestShouldUseBaseBranchVersionAndRange() {
        FakeRepositoryInspector repository = new FakeRepositoryInspector()
                .withRef("refs/remotes/origin/main")
                .withMergeBase("HEAD", "refs/remotes/origin/main", "base123")
                .withFile("refs/remotes/origin/main", "gradle.properties", "mod_version=1.0.0\n")
                .withCommits("base123..HEAD", List.of(new GitCommit("abc1234", "feat: add public search", "")));

        VersionGovernanceReport report = service.analyze(
                repository,
                new VersionGovernanceRequest(
                        "1.0.0",
                        Map.of("GITHUB_EVENT_NAME", "pull_request", "GITHUB_BASE_REF", "main"),
                        ""
                )
        );

        assertFalse(report.passed());
        assertEquals("当前 mod_version=1.0.0 低于最低推荐值 1.1.0，请先更新 gradle.properties 中的 mod_version。", report.failureMessage());
        assertTrue(report.toMarkdown().contains("最低推荐版本: `1.1.0`"));
    }

    @Test
    void pushToMainShouldUseLatestVersionTag() {
        FakeRepositoryInspector repository = new FakeRepositoryInspector()
                .withVersionTag("v1.0.0")
                .withCommits("v1.0.0..HEAD", List.of(new GitCommit("abc1234", "fix: keep sync stable", "")));

        VersionGovernanceReport report = service.analyze(
                repository,
                new VersionGovernanceRequest(
                        "1.0.1",
                        Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF_NAME", "main"),
                        ""
                )
        );

        assertTrue(report.passed());
        assertTrue(report.toConsoleText().contains("最低推荐版本: 1.0.1"));
    }

    @Test
    void docsOnlyChangesShouldAllowVersionToStayUnchanged() {
        FakeRepositoryInspector repository = new FakeRepositoryInspector()
                .withRef("refs/remotes/origin/main")
                .withMergeBase("HEAD", "refs/remotes/origin/main", "base123")
                .withFile("refs/remotes/origin/main", "gradle.properties", "mod_version=1.0.0\n")
                .withCommits("base123..HEAD", List.of(new GitCommit("abc1234", "docs: refine release checklist", "")));

        VersionGovernanceReport report = service.analyze(
                repository,
                new VersionGovernanceRequest("1.0.0", Map.of(), "main")
        );

        assertTrue(report.passed());
        assertTrue(report.toMarkdown().contains("无。当前范围内没有 `feat`、`fix` 或 breaking change。"));
    }

    @Test
    void shouldFailWhenNoVersionTagExistsForMainPush() {
        FakeRepositoryInspector repository = new FakeRepositoryInspector();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.analyze(
                        repository,
                        new VersionGovernanceRequest(
                                "1.0.0",
                                Map.of("GITHUB_EVENT_NAME", "push", "GITHUB_REF_NAME", "main"),
                                ""
                        )
                )
        );

        assertEquals("未找到可用的 v* 版本基线 tag，请先创建例如 v1.0.0 的标注 tag。", exception.getMessage());
    }

    private static final class FakeRepositoryInspector implements RepositoryInspector {
        private final Map<String, String> files = new HashMap<>();
        private final Map<String, String> mergeBases = new HashMap<>();
        private final Map<String, List<GitCommit>> commits = new HashMap<>();
        private final Map<String, Boolean> refs = new HashMap<>();
        private String versionTag = "";

        FakeRepositoryInspector withRef(String refName) {
            refs.put(refName, Boolean.TRUE);
            return this;
        }

        FakeRepositoryInspector withMergeBase(String leftRef, String rightRef, String mergeBase) {
            mergeBases.put(leftRef + "->" + rightRef, mergeBase);
            return this;
        }

        FakeRepositoryInspector withFile(String refName, String path, String content) {
            files.put(refName + ":" + path, content);
            return this;
        }

        FakeRepositoryInspector withCommits(String revisionRange, List<GitCommit> commitList) {
            commits.put(revisionRange, commitList);
            return this;
        }

        FakeRepositoryInspector withVersionTag(String tagName) {
            versionTag = tagName;
            return this;
        }

        @Override
        public boolean refExists(String refName) {
            return refs.getOrDefault(refName, Boolean.FALSE);
        }

        @Override
        public String mergeBase(String leftRef, String rightRef) {
            return mergeBases.get(leftRef + "->" + rightRef);
        }

        @Override
        public List<GitCommit> listCommits(String revisionRange) {
            return commits.getOrDefault(revisionRange, List.of());
        }

        @Override
        public String readFile(String refName, String path) {
            return files.get(refName + ":" + path);
        }

        @Override
        public String describeLatestVersionTag(String refName) {
            return versionTag;
        }
    }
}
