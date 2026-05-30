(function () {
    function apiHeaders() {
        if (typeof getApiUserHeaders === 'function') {
            return getApiUserHeaders();
        }
        return { 'Content-Type': 'application/json', 'X-User-Id': 'anonymous-user' };
    }

    function reloadWithChat(chatId) {
        var url = new URL(window.location.href);
        url.pathname = '/chat';
        url.searchParams.set('chatId', chatId);
        window.location.href = url.toString();
    }

    document.getElementById('newChatBtn')?.addEventListener('click', function () {
        fetch('/api/v1/chats', {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify({ name: 'New Chat', agentId: document.getElementById('agentPicker')?.value || 'auto' })
        }).then(function (r) { return r.json(); })
            .then(function (chat) { reloadWithChat(chat.id); })
            .catch(function (e) { console.error('Failed to create chat', e); });
    });

    document.getElementById('chatList')?.addEventListener('click', function (e) {
        var deleteBtn = e.target.closest('.chat-delete-btn');
        if (deleteBtn) {
            e.stopPropagation();
            var chatId = deleteBtn.getAttribute('data-chat-id');
            if (!confirm('Delete this chat?')) return;
            fetch('/api/v1/chats/' + encodeURIComponent(chatId), {
                method: 'DELETE',
                headers: apiHeaders()
            }).then(function () { window.location.href = '/chat'; });
            return;
        }
        var row = e.target.closest('.chat-item');
        if (row) {
            reloadWithChat(row.getAttribute('data-chat-id'));
        }
    });

    document.getElementById('chatMessageForm')?.addEventListener('submit', function (e) {
        e.preventDefault();
        var chatId = document.getElementById('currentChatId')?.value;
        var input = document.getElementById('messageInput');
        var text = input?.value?.trim();
        if (!chatId || !text) return;
        var btn = document.getElementById('sendBtn');
        if (btn) btn.disabled = true;
        fetch('/api/v1/chats/' + encodeURIComponent(chatId) + '/messages', {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify({ content: text, agentId: document.getElementById('agentPicker')?.value || 'auto' })
        }).then(function () {
            reloadWithChat(chatId);
        }).catch(function (err) {
            console.error(err);
            if (btn) btn.disabled = false;
        });
    });

    if (typeof syncUserIdCookie === 'function') {
        syncUserIdCookie();
    }
})();
