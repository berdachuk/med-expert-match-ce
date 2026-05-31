package com.berdachuk.medexpertmatch.llm.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("eval-cli")
public class EvalCliRunner implements ApplicationRunner {

    private final EvaluationService evaluationService;
    private final ApplicationContext context;

    @Value("${medexpertmatch.evaluation.dataset:medical-eval-v1}")
    private String defaultDataset;

    @Value("${medexpertmatch.evaluation.semantic-threshold:0.80}")
    private double defaultThreshold;

    public EvalCliRunner(EvaluationService evaluationService, ApplicationContext context) {
        this.evaluationService = evaluationService;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        String dataset = args.getOptionValues("dataset") != null && !args.getOptionValues("dataset").isEmpty()
                ? args.getOptionValues("dataset").get(0) : defaultDataset;

        double threshold = defaultThreshold;
        if (args.getOptionValues("semanticPassThreshold") != null && !args.getOptionValues("semanticPassThreshold").isEmpty()) {
            try {
                threshold = Double.parseDouble(args.getOptionValues("semanticPassThreshold").get(0));
            } catch (NumberFormatException e) {
                log.warn("Invalid semantic threshold, using default {}: {}", defaultThreshold, e.getMessage());
            }
        }

        log.info("Starting evaluation: dataset={}, semanticThreshold={}", dataset, threshold);
        String report = evaluationService.run(dataset, threshold);
        log.info("Evaluation report:\n{}", report);
        log.info("Evaluation complete");

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}
