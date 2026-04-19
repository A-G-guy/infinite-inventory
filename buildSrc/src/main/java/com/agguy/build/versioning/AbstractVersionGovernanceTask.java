package com.agguy.build.versioning;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class AbstractVersionGovernanceTask extends DefaultTask {
    public AbstractVersionGovernanceTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    @Input
    public abstract Property<String> getRepositoryRootPath();

    @Input
    public abstract Property<String> getCurrentVersion();

    @Input
    public abstract Property<String> getGithubEventName();

    @Input
    public abstract Property<String> getGithubBaseRef();

    @Input
    public abstract Property<String> getGithubRefName();

    @Optional
    @Input
    public abstract Property<String> getBaseRefOverride();

    @OutputFile
    public abstract RegularFileProperty getMarkdownReportFile();

    @OutputFile
    public abstract RegularFileProperty getTextReportFile();

    @TaskAction
    public void runTask() {
        try {
            VersionGovernanceReport report = analyze();
            writeReports(report.toMarkdown(), report.toConsoleText() + System.lineSeparator());
            getLogger().lifecycle(report.toConsoleText());
            if (shouldFailOnInsufficientVersion() && !report.passed()) {
                throw new GradleException(report.failureMessage());
            }
        } catch (Exception exception) {
            writeErrorReport(exception);
            throw exception;
        }
    }

    protected abstract boolean shouldFailOnInsufficientVersion();

    private VersionGovernanceReport analyze() {
        VersionGovernanceService service = new VersionGovernanceService();
        return service.analyze(
                new GitCliRepositoryInspector(new File(getRepositoryRootPath().get()).toPath()),
                new VersionGovernanceRequest(
                        getCurrentVersion().get(),
                        java.util.Map.of(
                                "GITHUB_EVENT_NAME", getGithubEventName().getOrElse(""),
                                "GITHUB_BASE_REF", getGithubBaseRef().getOrElse(""),
                                "GITHUB_REF_NAME", getGithubRefName().getOrElse("")
                        ),
                        getBaseRefOverride().getOrElse("")
                )
        );
    }

    private void writeErrorReport(Exception exception) {
        String message = exception.getMessage() == null ? "未知错误。" : exception.getMessage();
        String markdown = """
# Mod Version Governance

- 结果: `ERROR`

> %s
""".formatted(message).trim() + System.lineSeparator();
        String text = """
版本治理报告
结果: ERROR
%s
""".formatted(message).trim() + System.lineSeparator();
        writeReports(markdown, text);
    }

    private void writeReports(String markdown, String text) {
        writeString(getMarkdownReportFile().get().getAsFile(), markdown);
        writeString(getTextReportFile().get().getAsFile(), text);
    }

    private void writeString(File file, String content) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("无法创建报告目录: " + parent.getAbsolutePath());
        }
        try {
            java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("写入版本治理报告失败: " + file.getAbsolutePath(), exception);
        }
    }
}
