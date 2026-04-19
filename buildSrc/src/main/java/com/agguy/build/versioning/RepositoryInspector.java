package com.agguy.build.versioning;

import java.util.List;

public interface RepositoryInspector {
    boolean refExists(String refName);

    String mergeBase(String leftRef, String rightRef);

    List<GitCommit> listCommits(String revisionRange);

    String readFile(String refName, String path);

    String describeLatestVersionTag(String refName);
}
