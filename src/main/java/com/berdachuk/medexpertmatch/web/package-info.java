/**
 * Web module - Web UI with Thymeleaf templates (server-side rendering).
 * <p>
 * This module provides:
 * - Thymeleaf controllers for web UI pages
 * - Log streaming service
 * - Web UI for all use cases
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "llm", "medicalcase", "doctor", "clinicalexperience", "graph", "ingestion", "retrieval"})
package com.berdachuk.medexpertmatch.web;
