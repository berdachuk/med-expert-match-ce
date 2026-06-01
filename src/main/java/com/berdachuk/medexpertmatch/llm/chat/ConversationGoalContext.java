package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.chat.repository.ChatGoalContextRepositoryImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ConversationGoalContext {

    private static final Cache<String, Entry> STORE = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    private static volatile ChatGoalContextRepositoryImpl repository;

    public record Entry(GoalType lastGoal, String lastCaseId, String sessionId) {}

    private ConversationGoalContext() {}

    public static void setRepository(ChatGoalContextRepositoryImpl repo) {
        repository = repo;
    }

    public static void set(GoalType goal, String caseId, String sessionId) {
        STORE.put(sessionId, new Entry(goal, caseId, sessionId));
        persistToDb(sessionId, goal, caseId);
    }

    public static Entry get(String sessionId) {
        Entry entry = STORE.getIfPresent(sessionId);
        if (entry != null) {
            return entry;
        }
        return loadFromDb(sessionId).map(dbEntry -> {
            STORE.put(sessionId, dbEntry);
            return dbEntry;
        }).orElse(null);
    }

    public static void clear(String sessionId) {
        STORE.invalidate(sessionId);
        removeFromDb(sessionId);
    }

    private static void persistToDb(String sessionId, GoalType goal, String caseId) {
        ChatGoalContextRepositoryImpl repo = repository;
        if (repo != null) {
            try {
                repo.upsert(sessionId, goal.name(), caseId);
            } catch (Exception ignored) {
            }
        }
    }

    private static Optional<Entry> loadFromDb(String sessionId) {
        ChatGoalContextRepositoryImpl repo = repository;
        if (repo == null) {
            return Optional.empty();
        }
        try {
            return repo.findBySessionId(sessionId).map(row -> {
                GoalType goalType = GoalType.valueOf(row.goalType());
                return new Entry(goalType, row.caseId(), row.sessionId());
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void removeFromDb(String sessionId) {
        ChatGoalContextRepositoryImpl repo = repository;
        if (repo != null) {
            try {
                repo.deleteBySessionId(sessionId);
            } catch (Exception ignored) {
            }
        }
    }
}
