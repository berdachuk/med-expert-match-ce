package com.berdachuk.medexpertmatch.llm.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class EvalFlywheelReportWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public EvalFlywheelReportWriter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Path write(EvalFlywheelReport report, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        String date = LocalDate.now().format(DATE);
        Path jsonPath = outputDirectory.resolve("flywheel-" + date + ".json");
        Path mdPath = outputDirectory.resolve("flywheel-" + date + ".md");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), report);
        Files.writeString(mdPath, toMarkdown(report));
        return mdPath;
    }

    public static String toMarkdown(EvalFlywheelReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Eval flywheel report\n\n");
        md.append("Generated: ").append(report.generatedAt()).append("\n\n");
        md.append("Overall pass rate: ")
                .append(String.format(Locale.ROOT, "%.1f%%", report.overallPassRate() * 100.0))
                .append("\n\n");
        md.append("Release gate: **").append(report.releaseGatePassed() ? "GO" : "NO-GO").append("**\n\n");

        md.append("## Scenario families\n\n");
        md.append("| Family | Tier | Passed | Total | Pass rate |\n");
        md.append("|--------|------|--------|-------|----------|\n");
        for (EvalFamilyResult family : report.families()) {
            md.append(String.format(Locale.ROOT, "| %s | %s | %d | %d | %.1f%% |\n",
                    family.family(),
                    family.tier(),
                    family.passed(),
                    family.total(),
                    family.passRate() * 100.0));
        }
        md.append("\n");

        if (!report.roiEntries().isEmpty()) {
            md.append("## ROI (high-stakes categories)\n\n");
            md.append("| Category | Before | After | Δ quality | Δ cost | roi_index | Go |\n");
            md.append("|----------|--------|-------|-----------|--------|-----------|----|\n");
            for (EvalFlywheelRoiEntry entry : report.roiEntries()) {
                md.append(String.format(Locale.ROOT,
                        "| %s | %.1f%% | %.1f%% | %+.1f%% | %+.1f%% | %.3f | %s |\n",
                        entry.category(),
                        entry.beforeQualityPct(),
                        entry.afterQualityPct(),
                        entry.deltaQualityPct(),
                        entry.deltaCostPct(),
                        entry.roiIndex(),
                        entry.go() ? "yes" : "no"));
            }
            md.append("\n");
        }

        md.append("## release_gate\n\n");
        md.append("Deterministic gate: all `*EvalTest` families at 100% pass rate.\n");
        md.append("Live FunctionGemma compare: `ToolSelectionEvalCompareMain` (nightly / pre-release).\n");
        return md.toString();
    }
}
