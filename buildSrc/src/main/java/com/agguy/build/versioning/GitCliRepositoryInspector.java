package com.agguy.build.versioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GitCliRepositoryInspector implements RepositoryInspector {
    private static final String RECORD_SEPARATOR = "\u001e";
    private static final String FIELD_SEPARATOR = "\u001f";

    private final Path repositoryRoot;

    public GitCliRepositoryInspector(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    @Override
    public boolean refExists(String refName) {
        ProcessResult result = run(true, "git", "rev-parse", "--verify", "--quiet", refName);
        return result.exitCode() == 0;
    }

    @Override
    public String mergeBase(String leftRef, String rightRef) {
        return run(false, "git", "merge-base", leftRef, rightRef).stdout().trim();
    }

    @Override
    public List<GitCommit> listCommits(String revisionRange) {
        String output = run(false, "git", "log", "--format=%H%x1f%s%x1f%B%x1e", revisionRange).stdout();
        List<GitCommit> commits = new ArrayList<>();
        if (output.isBlank()) {
            return commits;
        }
        for (String record : output.split(RECORD_SEPARATOR)) {
            String trimmedRecord = record.trim();
            if (trimmedRecord.isEmpty()) {
                continue;
            }
            String[] fields = trimmedRecord.split(FIELD_SEPARATOR, 3);
            if (fields.length < 3) {
                throw new IllegalStateException("无法解析 git log 输出记录。");
            }
            commits.add(new GitCommit(fields[0].trim(), fields[1].trim(), fields[2].trim()));
        }
        return commits;
    }

    @Override
    public String readFile(String refName, String path) {
        return run(false, "git", "show", refName + ":" + path).stdout();
    }

    @Override
    public String describeLatestVersionTag(String refName) {
        return run(false, "git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*", refName).stdout().trim();
    }

    private ProcessResult run(boolean allowFailure, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repositoryRoot.toFile());
        try {
            Process process = builder.start();
            byte[] stdoutBytes = process.getInputStream().readAllBytes();
            byte[] stderrBytes = process.getErrorStream().readAllBytes();
            int exitCode = process.waitFor();
            ProcessResult result = new ProcessResult(
                    exitCode,
                    new String(stdoutBytes, StandardCharsets.UTF_8),
                    new String(stderrBytes, StandardCharsets.UTF_8)
            );
            if (!allowFailure && exitCode != 0) {
                throw new IllegalStateException(
                        "Git 命令执行失败: " + String.join(" ", command) + System.lineSeparator() + result.stderr().trim()
                );
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("无法执行 Git 命令。", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("执行 Git 命令时被中断。", exception);
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
