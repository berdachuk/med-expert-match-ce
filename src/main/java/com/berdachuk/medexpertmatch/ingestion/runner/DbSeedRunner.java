package com.berdachuk.medexpertmatch.ingestion.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * CLI entry point for database seed, backup, and restore operations.
 * Activate with {@code --spring.profiles.active=seed-cli} plus one of {@code --seed}, {@code --backup}, or {@code --restore}.
 */
@Slf4j
@Component
@Profile("seed-cli")
public class DbSeedRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        if (args == null || args.length == 0) {
            log.info("DbSeedRunner: no operation requested. Use --seed, --backup, or --restore.");
            return;
        }
        boolean seed = hasFlag(args, "--seed");
        boolean backup = hasFlag(args, "--backup");
        boolean restore = hasFlag(args, "--restore");
        if (seed) {
            log.info("DbSeedRunner: --seed requested (use Synthetic Data Generator API or ingestion module for full seeding).");
        }
        if (backup) {
            log.info("DbSeedRunner: --backup requested (use scripts/backup-db.sh for pg_dump-based backup).");
        }
        if (restore) {
            log.info("DbSeedRunner: --restore requested (restore from JSONL backups is not yet automated).");
        }
        if (!seed && !backup && !restore) {
            log.warn("DbSeedRunner: unrecognized args {}", Arrays.toString(args));
        }
    }

    static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg != null && arg.toLowerCase(Locale.ROOT).equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
