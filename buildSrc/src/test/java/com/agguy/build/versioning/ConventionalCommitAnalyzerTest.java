package com.agguy.build.versioning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConventionalCommitAnalyzerTest {
    private final ConventionalCommitAnalyzer analyzer = new ConventionalCommitAnalyzer();

    @Test
    void featShouldRequireMinorBump() {
        AnalyzedCommit analyzedCommit = analyzer.analyze(new GitCommit("abc1234", "feat: add search panel", ""));
        assertEquals(ReleaseImpact.MINOR, analyzedCommit.impact());
    }

    @Test
    void fixShouldRequirePatchBump() {
        AnalyzedCommit analyzedCommit = analyzer.analyze(new GitCommit("abc1234", "fix(ui): keep search focus", ""));
        assertEquals(ReleaseImpact.PATCH, analyzedCommit.impact());
    }

    @Test
    void breakingChangeShouldRequireMajorBump() {
        AnalyzedCommit analyzedCommit = analyzer.analyze(new GitCommit(
                "abc1234",
                "refactor!: simplify protocol",
                "BREAKING CHANGE: remove legacy payload shape"
        ));
        assertEquals(ReleaseImpact.MAJOR, analyzedCommit.impact());
    }

    @Test
    void docsShouldNotRequireRelease() {
        AnalyzedCommit analyzedCommit = analyzer.analyze(new GitCommit("abc1234", "docs: update release guide", ""));
        assertEquals(ReleaseImpact.NONE, analyzedCommit.impact());
    }
}
