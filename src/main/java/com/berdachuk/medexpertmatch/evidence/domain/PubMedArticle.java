package com.berdachuk.medexpertmatch.evidence.domain;

/**
 * PubMed article domain entity.
 * Represents a medical literature article from PubMed.
 */
public record PubMedArticle(
        /**
         * Article title.
         */
        String title,

        /**
         * Article abstract.
         */
        String abstractText,

        /**
         * List of author names.
         */
        java.util.List<String> authors,

        /**
         * Journal name.
         */
        String journal,

        /**
         * Publication year.
         */
        Integer year,

        /**
         * PubMed ID (PMID).
         */
        String pmid
) {
}
