package com.berdachuk.medexpertmatch.chat.service;

import java.util.Map;

/**
 * GDPR-style user chat data lifecycle (M22).
 */
public interface ChatDataLifecycleService {

    /**
     * Soft-deletes all messages and removes non-default chats for the user.
     *
     * @return summary counts (chats removed, messages soft-deleted)
     */
    Map<String, Object> deleteAllUserData(String userId);

    /**
     * Exports all owned chats as a PHI-redacted bundle.
     */
    Map<String, Object> exportUserBundle(String userId);
}
