package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.chat.repository.ChatGoalContextRepositoryImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ConversationGoalContextBootstrap {

    private final ChatGoalContextRepositoryImpl repository;

    public ConversationGoalContextBootstrap(ChatGoalContextRepositoryImpl repository) {
        this.repository = repository;
    }

    @PostConstruct
    void init() {
        ConversationGoalContext.setRepository(repository);
    }
}
