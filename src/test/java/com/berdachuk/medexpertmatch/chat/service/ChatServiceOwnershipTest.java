package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceOwnershipTest {

    private final ChatRepository chatRepository = mock(ChatRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatService chatService = new ChatServiceImpl(chatRepository, chatMessageRepository);

    @Test
    void requireOwnedChatRejectsOtherUser() {
        when(chatRepository.findById("chat-1")).thenReturn(Optional.of(
                new Chat("chat-1", "user-a", "Test", "auto", false, null, null, null, 0)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.requireOwnedChat("chat-1", "user-b"));
        assertEquals(403, ex.getStatusCode().value());
    }
}
