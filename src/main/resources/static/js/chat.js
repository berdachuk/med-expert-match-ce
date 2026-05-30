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

    function appendTrace(line) {
        var panel = document.getElementById('executionTracePanel');
        if (!panel) return;
        panel.classList.remove('d-none');
        panel.textContent += line + '\n';
        panel.scrollTop = panel.scrollHeight;
    }

    function startLogStream(sid) {
        if (!sid || typeof EventSource === 'undefined') return null;
        var panel = document.getElementById('executionTracePanel');
        if (panel) {
            panel.classList.remove('d-none');
            panel.textContent = '';
        }
        var source = new EventSource('/api/v1/logs/stream?sessionId=' + encodeURIComponent(sid));
        source.onmessage = function (e) {
            try {
                var data = JSON.parse(e.data);
                appendTrace((data.level || 'INFO') + ' ' + (data.message || e.data));
            } catch (err) {
                appendTrace(e.data);
            }
        };
        source.onerror = function () { source.close(); };
        return source;
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

    function sendMessageStream(chatId, text, agentId, btn) {
        var sid = sessionId();
        var logSource = startLogStream(sid);
        fetch('/api/v1/chats/' + encodeURIComponent(chatId) + '/messages/stream', {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify({ content: text, agentId: agentId || 'auto' })
        }).then(function (response) {
            if (!response.ok || !response.body) {
                throw new Error('Stream failed');
            }
            var reader = response.body.getReader();
            var decoder = new TextDecoder();
            var buffer = '';
            function pump() {
                return reader.read().then(function (result) {
                    if (result.done) {
                        reloadWithChat(chatId);
                        return;
                    }
                    buffer += decoder.decode(result.value, { stream: true });
                    var parts = buffer.split('\n\n');
                    buffer = parts.pop() || '';
                    parts.forEach(function (block) {
                        if (block.indexOf('event:done') >= 0) {
                            if (logSource) logSource.close();
                            reloadWithChat(chatId);
                        }
                    });
                    return pump();
                });
            }
            return pump();
        }).catch(function (err) {
            console.error(err);
            if (logSource) logSource.close();
            if (btn) btn.disabled = false;
        });
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
        input.value = '';
        sendMessageStream(chatId, text, document.getElementById('agentPicker')?.value || 'auto', btn);
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
