package com.berdachuk.medexpertmatch.ingestion.runner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbSeedRunnerTest {

    @Test
    void detectsSeedFlag() {
        assertTrue(DbSeedRunner.hasFlag(new String[] {"--seed"}, "--seed"));
        assertFalse(DbSeedRunner.hasFlag(new String[] {"--backup"}, "--seed"));
    }

    @Test
    void detectsBackupAndRestoreFlags() {
        assertTrue(DbSeedRunner.hasFlag(new String[] {"--backup", "--restore"}, "--backup"));
        assertTrue(DbSeedRunner.hasFlag(new String[] {"--backup", "--restore"}, "--restore"));
    }
}
