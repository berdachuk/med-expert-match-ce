(function () {
    function apiHeaders() {
        if (typeof getApiUserHeaders === 'function') {
            return getApiUserHeaders();
        }
        return { 'Content-Type': 'application/json', 'X-User-Id': 'anonymous-user' };
    }

    function sessionId() {
        var chatId = document.getElementById('currentChatId')?.value;
        var userId = document.getElementById('currentUserId')?.value || 'anonymous-user';
        return chatId ? userId + '-' + chatId : null;
    }

    function reloadWithChat(chatId) {
        var url = new URL(window.location.href);
        url.pathname = '/chat';
        url.searchParams.set('chatId', chatId);
        window.location.href = url.toString();
    }

    function renderTodos(todos) {
        var panel = document.getElementById('agentTodoPanel');
        if (!panel) return;
        if (!todos || !todos.length) {
            panel.classList.add('d-none');
            panel.innerHTML = '';
            return;
        }
        panel.classList.remove('d-none');
        var html = '<div class="small fw-semibold mb-1">Agent plan</div><ul class="list-unstyled mb-0 small">';
        todos.forEach(function (t) {
            html += '<li><span class="badge bg-light text-dark me-1">' + t.status + '</span>' + t.content + '</li>';
        });
        html += '</ul>';
        panel.innerHTML = html;
    }

    function renderQuestions(data) {
        var panel = document.getElementById('agentQuestionPanel');
        if (!panel || !data || !data.questions) return;
        panel.classList.remove('d-none');
        var html = '<div class="small fw-semibold mb-1">Clarification needed</div>';
        data.questions.forEach(function (q, idx) {
            html += '<div class="mb-2"><label class="form-label small">' + q.question + '</label>' +
                '<input class="form-control form-control-sm agent-answer" data-qidx="' + idx + '" type="text"/></div>';
        });
        html += '<button class="btn btn-sm btn-outline-primary" id="submitAgentAnswers" type="button">Submit answers</button>';
        panel.innerHTML = html;
        document.getElementById('submitAgentAnswers')?.addEventListener('click', function () {
            var sid = sessionId();
            if (!sid) return;
            var answers = {};
            panel.querySelectorAll('.agent-answer').forEach(function (input, i) {
                answers['q' + i] = input.value;
            });
            fetch('/api/v1/agent/questions/answer?sessionId=' + encodeURIComponent(sid), {
                method: 'POST',
                headers: apiHeaders(),
                body: JSON.stringify(answers)
            }).then(function () { pollAgenticState(); });
        });
    }

    function pollAgenticState() {
        fetch('/api/v1/agent/todos/latest', { headers: apiHeaders() })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (data) { if (data && data.todos) renderTodos(data.todos); })
            .catch(function () { });

        var sid = sessionId();
        if (!sid) return;
        fetch('/api/v1/agent/questions/pending?sessionId=' + encodeURIComponent(sid), { headers: apiHeaders() })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (data) { if (data) renderQuestions(data); })
            .catch(function () { });
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

    document.getElementById('messageInput')?.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            document.getElementById('chatMessageForm')?.requestSubmit();
        }
    });

    if (typeof syncUserIdCookie === 'function') {
        syncUserIdCookie();
    }

    pollAgenticState();
    setInterval(pollAgenticState, 5000);
})();
