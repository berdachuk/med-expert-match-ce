/**
 * Web module - Web UI with Thymeleaf templates (server-side rendering).
 * <p>
 * This module provides:
 * - Thymeleaf controllers for web UI pages
 * - Log streaming service
 * - Web UI for all use cases
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: service", "llm", "llm :: rest", "llm :: service", "medicalcase", "medicalcase :: domain", "medicalcase :: repository", "doctor", "doctor :: domain", "doctor :: repository", "clinicalexperience", "clinicalexperience :: domain", "clinicalexperience :: repository", "graph", "ingestion", "ingestion :: service", "retrieval", "retrieval :: repository"})
package com.berdachuk.medexpertmatch.web;
