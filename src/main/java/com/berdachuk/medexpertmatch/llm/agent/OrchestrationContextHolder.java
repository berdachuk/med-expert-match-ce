package com.berdachuk.medexpertmatch.llm.agent;

public final class OrchestrationContextHolder {

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    private OrchestrationContextHolder() {}

    public static void setSessionId(String sessionId) {
        SESSION_ID.set(sessionId);
    }

    public static String sessionIdOrNull() {
        return SESSION_ID.get();
    }

    public static void clear() {
        SESSION_ID.remove();
    }
}
