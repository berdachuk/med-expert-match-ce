package com.berdachuk.medexpertmatch.llm.eval;

import java.nio.file.Path;

/**
 * CLI entry point for the unified eval flywheel report (M62).
 */
public final class EvalFlywheelMain {

    private EvalFlywheelMain() {
    }

    public static void main(String[] args) throws Exception {
        Path outputDir = args.length >= 1 ? Path.of(args[0]) : Path.of("target/eval");
        EvalFlywheelReport report = EvalFlywheelAggregator.runDeterministicSuites();
        Path written = new EvalFlywheelReportWriter().write(report, outputDir);
        System.out.println("Release gate: " + (report.releaseGatePassed() ? "GO" : "NO-GO"));
        System.out.println("Wrote flywheel report to " + written.toAbsolutePath());
        if (!report.releaseGatePassed()) {
            System.exit(1);
        }
    }
}
