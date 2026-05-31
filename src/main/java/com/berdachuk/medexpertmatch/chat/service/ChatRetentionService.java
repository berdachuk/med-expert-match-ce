package com.berdachuk.medexpertmatch.chat.service;

/**
 * Purges idle non-default chats past configured retention (M21).
 */
public interface ChatRetentionService {

    int purgeIdleChats();
}
