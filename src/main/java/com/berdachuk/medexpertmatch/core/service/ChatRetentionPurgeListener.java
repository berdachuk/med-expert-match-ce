package com.berdachuk.medexpertmatch.core.service;

/**
 * Hook invoked when an idle chat is purged by retention (M141 session alignment).
 */
public interface ChatRetentionPurgeListener {

    void onChatPurged(String userId, String chatId);
}
