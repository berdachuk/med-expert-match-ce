/**
 * Core module - Shared infrastructure and utilities.
 * <p>
 * This module provides:
 * - Configuration classes (SpringAIConfig, MedicalAgentConfiguration, PromptTemplateConfig)
 * - Exception handling (MedExpertMatchException, RetrievalException, ErrorCode)
 * - Monitoring (MedGemmaToolCallingMonitor)
 * - Utilities (IdGenerator, LlmCallLimiter, LlmClientType, RetryWithBackoff)
 * - Health monitoring (HealthCheck, DatabaseHealthCheck, HealthCheckService)
 * - Log streaming (LogStreamService)
 * - SQL injection utilities (InjectSql, SqlInjectBeanPostProcessor)
 * <p>
 * This is a shared infrastructure module intentionally used across all modules.
 */
@org.springframework.modulith.ApplicationModule
package com.berdachuk.medexpertmatch.core;
