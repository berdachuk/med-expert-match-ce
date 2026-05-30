package com.berdachuk.medexpertmatch.core.security;

/**
 * Resolves the current user id for chat isolation and audit.
 */
public interface UserContext {

    String currentUserId();
}
