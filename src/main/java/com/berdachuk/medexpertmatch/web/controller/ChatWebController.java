package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.security.UserContext;
import com.berdachuk.medexpertmatch.web.domain.ChatMessageDisplay;
import com.berdachuk.medexpertmatch.web.service.ChatMarkdownRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatWebController {

    private final ChatService chatService;
    private final UserContext userContext;
    private final ChatMarkdownRenderer chatMarkdownRenderer;

    public ChatWebController(ChatService chatService, UserContext userContext,
                             ChatMarkdownRenderer chatMarkdownRenderer) {
        this.chatService = chatService;
        this.userContext = userContext;
        this.chatMarkdownRenderer = chatMarkdownRenderer;
    }

    @GetMapping
    public String chatPage(@RequestParam(required = false) String chatId, Model model) {
        String userId = userContext.currentUserId();
        chatService.getOrCreateDefaultChat(userId);
        List<Chat> chats = chatService.listChats(userId);
        Chat current = chats.stream()
                .filter(c -> chatId != null && chatId.equals(c.id()))
                .findFirst()
                .orElse(chats.isEmpty() ? chatService.getOrCreateDefaultChat(userId) : chats.get(0));

        List<ChatMessage> messages = chatService.getHistory(current.id(), userId, 200, 0);
        List<ChatMessageDisplay> displayMessages = messages.stream()
                .map(message -> ChatMessageDisplay.from(message, chatMarkdownRenderer))
                .toList();

        model.addAttribute("currentPage", "chat");
        model.addAttribute("currentUserId", userId);
        model.addAttribute("chats", chats);
        model.addAttribute("currentChat", current);
        model.addAttribute("messages", displayMessages);
        return "chat";
    }
}
