package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "AI Chat", description = "Per-user chat sessions and message history")
@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserContext userContext;

    public ChatController(ChatService chatService, UserContext userContext) {
        this.chatService = chatService;
        this.userContext = userContext;
    }

    @Operation(summary = "List chats for current user")
    @GetMapping
    public List<Chat> listChats() {
        return chatService.listChats(userContext.currentUserId());
    }

    @Operation(summary = "Create a new chat")
    @PostMapping
    public Chat createChat(@RequestBody(required = false) Map<String, String> body) {
        String name = body != null ? body.getOrDefault("name", "New Chat") : "New Chat";
        String agentId = body != null ? body.getOrDefault("agentId", "auto") : "auto";
        return chatService.createChat(userContext.currentUserId(), name, agentId);
    }

    @Operation(summary = "Get chat message history")
    @GetMapping("/{chatId}/history")
    public List<ChatMessage> history(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return chatService.getHistory(chatId, userContext.currentUserId(), limit, offset);
    }

    @Operation(summary = "Delete a chat (not the default chat)")
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Map<String, String>> deleteChat(@PathVariable String chatId) {
        boolean deleted = chatService.deleteChat(chatId, userContext.currentUserId());
        if (!deleted) {
            return ResponseEntity.badRequest().body(Map.of("status", "not_deleted"));
        }
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @Operation(summary = "Post a user message (returns user + placeholder assistant reply)")
    @PostMapping("/{chatId}/messages")
    public Map<String, Object> postMessage(
            @PathVariable String chatId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        ChatMessage userMsg = chatService.appendUserMessage(chatId, userContext.currentUserId(), content.trim());
        ChatMessage assistantMsg = chatService.appendAssistantMessage(
                chatId,
                userContext.currentUserId(),
                "Agent routing and LLM responses will be wired in the next phase (M14). Your message was saved.");
        return Map.of("userMessage", userMsg, "assistantMessage", assistantMsg);
    }
}
