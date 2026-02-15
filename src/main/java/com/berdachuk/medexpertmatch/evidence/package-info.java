/**
 * Evidence module - Clinical evidence retrieval.
 * <p>
 * This module provides:
 * - PubMed service for research paper retrieval
 * - Clinical guidelines search
 * - Evidence domain entities
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: exception", "evidence :: domain", "evidence :: service"})
package com.berdachuk.medexpertmatch.evidence;
