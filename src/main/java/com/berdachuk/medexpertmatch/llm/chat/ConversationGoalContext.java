package com.berdachuk.medexpertmatch.llm.chat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConversationGoalContext {

    private static final ConcurrentMap<String, Entry> STORE = new ConcurrentHashMap<>();

    private static final long TTL_SECONDS = 60;

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "goal-context-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        CLEANER.scheduleAtFixedRate(
                STORE::clear, TTL_SECONDS, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public record Entry(GoalType lastGoal, String lastCaseId, String sessionId) {}

    private ConversationGoalContext() {}

    public static void set(GoalType goal, String caseId, String sessionId) {
        STORE.put(sessionId, new Entry(goal, caseId, sessionId));
    }

    public static Entry get(String sessionId) {
        return STORE.get(sessionId);
    }

    public static void clear(String sessionId) {
        STORE.remove(sessionId);
    }
}
