(function () {
    var activityEntries = [];
    var activityStartMs = null;
    var activityCollapsed = false;
    var currentAssistantBubble = null;
    var currentMarkdownBuffer = '';

    function apiHeaders() {
        var hiddenUserId = document.getElementById('currentUserId')?.value?.trim();
        if (hiddenUserId) {
            return { 'Content-Type': 'application/json', 'X-User-Id': hiddenUserId };
        }
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

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderMarkdown(text) {
        if (!text) return '';
        if (typeof marked === 'undefined' || typeof DOMPurify === 'undefined') {
            return escapeHtml(text);
        }
        var raw = marked.parse(text, { breaks: true });
        return DOMPurify.sanitize(raw);
    }

    function initHistoricalMarkdown() {
        document.querySelectorAll('[data-markdown]').forEach(function (el) {
            var md = el.getAttribute('data-markdown') || '';
            el.innerHTML = renderMarkdown(md);
        });
    }

    function hideEmptyState() {
        var empty = document.getElementById('chatEmptyState');
        if (empty) empty.classList.add('d-none');
    }

    function appendUserBubble(text) {
        hideEmptyState();
        var panel = document.getElementById('messagePanel');
        var wrap = document.createElement('div');
        wrap.className = 'mb-3 chat-message-row text-end';
        wrap.innerHTML = '<span class="badge mb-1 bg-primary">user</span>' +
            '<div class="p-2 rounded d-inline-block text-start bg-primary-subtle">' + escapeHtml(text) + '</div>';
        panel.appendChild(wrap);
        panel.scrollTop = panel.scrollHeight;
    }

    function beginAssistantBubble() {
        hideEmptyState();
        var panel = document.getElementById('messagePanel');
        var wrap = document.createElement('div');
        wrap.className = 'mb-3 chat-message-row text-start chat-streaming';
        wrap.innerHTML = '<span class="badge mb-1 bg-secondary">assistant</span>' +
            '<div class="p-2 rounded d-inline-block text-start bg-light border chat-markdown"></div>';
        panel.appendChild(wrap);
        currentAssistantBubble = wrap.querySelector('.chat-markdown');
        currentMarkdownBuffer = '';
        panel.scrollTop = panel.scrollHeight;
    }

    function updateAssistantBubble(live) {
        if (!currentAssistantBubble) return;
        if (live) {
            currentAssistantBubble.textContent = currentMarkdownBuffer;
        } else {
            currentAssistantBubble.innerHTML = renderMarkdown(currentMarkdownBuffer);
        }
        var panel = document.getElementById('messagePanel');
        panel.scrollTop = panel.scrollHeight;
    }

    function finalizeAssistantBubble() {
        if (currentAssistantBubble) {
            updateAssistantBubble(false);
            if (currentAssistantBubble.closest('.chat-streaming')) {
                currentAssistantBubble.closest('.chat-streaming').classList.remove('chat-streaming');
            }
        }
        currentAssistantBubble = null;
        currentMarkdownBuffer = '';
    }

    function handleSseBlock(block) {
        if (!block.trim()) return;
        var evt = parseSseBlock(block);
        if (evt.event === 'token') {
            currentMarkdownBuffer += parseTokenChunk(evt.data);
            updateAssistantBubble(true);
        } else if (evt.event === 'agent') {
            try {
                var agentPayload = JSON.parse(evt.data);
                if (agentPayload.type === 'agent_start') {
                    addActivityEntry('agent', 'Active: ' + agentPayload.agentId, agentPayload.agentId);
                } else if (agentPayload.type === 'agent_done') {
                    addActivityEntry('done', 'Completed', agentPayload.agentId);
                }
            } catch (ignore) { /* non-json agent payload */ }
        } else if (evt.event === 'activity') {
            try {
                var act = JSON.parse(evt.data);
                if (act.type === 'tool_call') {
                    addActivityEntry('tool', act.message || act.toolName, 'orchestrator');
                } else if (act.type === 'reasoning') {
                    addActivityEntry('reasoning', act.message || 'Thinking…', 'orchestrator');
                } else if (act.type === 'todo_update' && act.todos) {
                    renderTodosFromActivity(act.todos);
                    addActivityEntry('plan', 'Plan updated (' + act.todos.length + ' items)', 'orchestrator');
                }
            } catch (ignore) { /* non-json activity payload */ }
        } else if (evt.event === 'done') {
            applyDonePayload(evt.data);
            return 'done';
        }
        return null;
    }

    function processSseBuffer(bufferRef) {
        var parts = bufferRef.value.split('\n\n');
        bufferRef.value = parts.pop() || '';
        var streamDone = false;
        parts.forEach(function (block) {
            if (handleSseBlock(block) === 'done') {
                streamDone = true;
            }
        });
        return streamDone;
    }

    function activityPanels() {
        return [
            document.getElementById('agentActivityPanel'),
            document.getElementById('agentActivitySummary')
        ].filter(Boolean);
    }

    function resetActivityPanel() {
        activityEntries = [];
        activityStartMs = Date.now();
        activityCollapsed = false;
        var panel = document.getElementById('agentActivityPanel');
        var summary = document.getElementById('agentActivitySummary');
        if (panel) {
            panel.classList.remove('d-none');
            panel.innerHTML = '';
        }
        if (summary) {
            summary.classList.add('d-none');
            summary.innerHTML = '';
        }
    }

    function addActivityEntry(kind, message, agentId) {
        var entry = { kind: kind, message: message, agentId: agentId || 'orchestrator', ts: Date.now() };
        activityEntries.push(entry);
        renderActivityPanel();
    }

    function renderActivityPanel() {
        var panel = document.getElementById('agentActivityPanel');
        if (!panel || activityCollapsed) return;
        var html = '';
        activityEntries.forEach(function (e) {
            var label = e.kind;
            if (e.kind === 'tool') label = 'tool_call';
            if (e.kind === 'plan') label = 'todo';
            if (e.kind === 'reasoning') label = 'reasoning';
            html += '<div class="agent-activity-entry ' + escapeHtml(label) + '">' +
                '<div class="fw-semibold">' + escapeHtml(e.agentId) + '</div>' +
                '<div class="text-muted">' + escapeHtml(e.message) + '</div></div>';
        });
        panel.innerHTML = html || '<div class="text-muted">Working…</div>';
        panel.scrollTop = panel.scrollHeight;
    }

    function renderTodosFromActivity(todos) {
        var panel = document.getElementById('agentTodoPanel');
        if (!panel || !todos || !todos.length) return;
        panel.classList.remove('d-none');
        var html = '<div class="small fw-semibold mb-1">Agent plan</div><ul class="list-unstyled mb-0 small">';
        todos.forEach(function (t) {
            html += '<li><span class="badge bg-light text-dark me-1">' + escapeHtml(t.status) + '</span>' +
                escapeHtml(t.content) + '</li>';
        });
        html += '</ul>';
        panel.innerHTML = html;
    }

    function collapseActivityPanel() {
        activityCollapsed = true;
        var panel = document.getElementById('agentActivityPanel');
        var summary = document.getElementById('agentActivitySummary');
        if (!summary) return;
        var elapsedSec = activityStartMs ? Math.max(1, Math.round((Date.now() - activityStartMs) / 1000)) : 0;
        var agents = new Set(activityEntries.map(function (e) { return e.agentId; }));
        summary.textContent = '▸ ' + agents.size + ' agent(s) · ' + activityEntries.length + ' steps · ' + elapsedSec + 's — click to expand';
        summary.setAttribute('aria-expanded', 'false');
        summary.classList.remove('d-none');
        if (panel) panel.classList.add('d-none');
    }

    function expandActivityPanel() {
        activityCollapsed = false;
        var panel = document.getElementById('agentActivityPanel');
        var summary = document.getElementById('agentActivitySummary');
        if (summary) summary.classList.add('d-none');
        if (panel) panel.classList.remove('d-none');
        if (summary) summary.setAttribute('aria-expanded', 'true');
        renderActivityPanel();
    }

    function parseSseDataLine(line) {
        if (line.indexOf('data:') !== 0) return null;
        var value = line.slice(5);
        if (value.charAt(0) === ' ') {
            value = value.slice(1);
        }
        return value;
    }

    function parseSseBlock(block) {
        var lines = block.split('\n');
        var eventName = 'message';
        var dataLines = [];
        lines.forEach(function (line) {
            if (line.indexOf('event:') === 0) {
                eventName = line.substring(6).trim();
            }
            var dataValue = parseSseDataLine(line);
            if (dataValue !== null) {
                dataLines.push(dataValue);
            }
        });
        return { event: eventName, data: dataLines.join('\n') };
    }

    function parseTokenChunk(rawData) {
        if (!rawData) return '';
        try {
            var parsed = JSON.parse(rawData);
            if (typeof parsed === 'string') {
                return parsed;
            }
            if (parsed && typeof parsed.t === 'string') {
                return parsed.t;
            }
        } catch (ignore) { /* legacy plain-text token */ }
        return rawData;
    }

    function applyDonePayload(rawData) {
        if (!rawData) return;
        try {
            var parsed = JSON.parse(rawData);
            if (parsed && typeof parsed.content === 'string') {
                currentMarkdownBuffer = parsed.content;
            }
        } catch (ignore) { /* legacy done event carried message id only */ }
    }

    function startLogStream(sid) {
        if (!sid || typeof EventSource === 'undefined') return null;
        var source = new EventSource('/api/v1/logs/stream?sessionId=' + encodeURIComponent(sid));
        source.onmessage = function (e) {
            try {
                var payload = JSON.parse(e.data);
                addActivityEntry('tool', payload.message || e.data, 'orchestrator');
            } catch (err) {
                addActivityEntry('tool', e.data, 'orchestrator');
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
            html += '<li><span class="badge bg-light text-dark me-1">' + escapeHtml(t.status) + '</span>' +
                escapeHtml(t.content) + '</li>';
            addActivityEntry('plan', t.status + ': ' + t.content, 'orchestrator');
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
            html += '<div class="mb-2"><label class="form-label small">' + escapeHtml(q.question) + '</label>' +
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
        resetActivityPanel();
        addActivityEntry('start', 'Starting agent turn…', agentId || 'auto');
        appendUserBubble(text);
        beginAssistantBubble();
        var logSource = startLogStream(sid);

        fetch('/api/v1/chats/' + encodeURIComponent(chatId) + '/messages/stream', {
            method: 'POST',
            headers: apiHeaders(),
            body: JSON.stringify({ content: text, agentId: agentId || 'auto' })
        }).then(function (response) {
            if (!response.ok || !response.body) {
                return response.text().then(function (body) {
                    var detail = body && body.length < 200 ? body : ('HTTP ' + response.status);
                    throw new Error(detail || 'Stream failed');
                });
            }
            var reader = response.body.getReader();
            var decoder = new TextDecoder();
            var buffer = { value: '' };
            function finishStream() {
                if (logSource) logSource.close();
                finalizeAssistantBubble();
                collapseActivityPanel();
                pollAgenticState();
                if (btn) btn.disabled = false;
            }
            function pump() {
                return reader.read().then(function (result) {
                    if (result.value) {
                        buffer.value += decoder.decode(result.value, { stream: true });
                    }
                    var streamDone = processSseBuffer(buffer);
                    if (streamDone) {
                        finishStream();
                        return;
                    }
                    if (result.done) {
                        if (buffer.value.trim()) {
                            handleSseBlock(buffer.value);
                            buffer.value = '';
                        }
                        finishStream();
                        return;
                    }
                    return pump();
                });
            }
            return pump();
        }).catch(function (err) {
            console.error(err);
            if (logSource) logSource.close();
            addActivityEntry('done', 'Error: ' + (err && err.message ? err.message : 'stream failed'), agentId || 'auto');
            collapseActivityPanel();
            if (btn) btn.disabled = false;
        });
    }

    document.getElementById('agentActivitySummary')?.addEventListener('click', expandActivityPanel);
    document.getElementById('agentActivitySummary')?.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            expandActivityPanel();
        }
    });

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

    function showLifecycleToast(message) {
        var toast = document.getElementById('lifecycleToast');
        var msg = document.getElementById('lifecycleToastMessage');
        if (!toast || !msg) return;
        msg.textContent = message;
        toast.classList.remove('d-none');
        setTimeout(function () {
            toast.classList.add('d-none');
        }, 8000);
    }

    document.getElementById('exportBundleBtn')?.addEventListener('click', function () {
        fetch('/api/v1/chats/export-bundle', { headers: apiHeaders() })
            .then(function (r) {
                if (!r.ok) throw new Error('Export failed');
                return r.json();
            })
            .then(function (bundle) {
                var blob = new Blob([JSON.stringify(bundle, null, 2)], { type: 'application/json' });
                var url = URL.createObjectURL(blob);
                var a = document.createElement('a');
                a.href = url;
                a.download = 'chat-export-bundle.json';
                a.click();
                URL.revokeObjectURL(url);
                var ref = bundle.auditReferenceHash ? (' Audit ref: ' + bundle.auditReferenceHash.substring(0, 12) + '…') : '';
                showLifecycleToast('Export complete.' + ref);
            })
            .catch(function (e) { console.error('Export bundle failed', e); alert('Export failed'); });
    });

    document.getElementById('confirmDeleteAllDataBtn')?.addEventListener('click', function () {
        fetch('/api/v1/chats/data', { method: 'DELETE', headers: apiHeaders() })
            .then(function (r) {
                if (!r.ok) throw new Error('Delete failed');
                return r.json();
            })
            .then(function (result) {
                var ref = result.auditReferenceHash ? (' Audit ref: ' + result.auditReferenceHash.substring(0, 12) + '…') : '';
                showLifecycleToast('All chat data deleted.' + ref);
                setTimeout(function () { window.location.href = '/chat'; }, 1500);
            })
            .catch(function (e) { console.error('Delete all data failed', e); alert('Delete failed'); });
    });

    if (typeof syncUserIdCookie === 'function') {
        syncUserIdCookie();
    }

    initHistoricalMarkdown();
    pollAgenticState();
    setInterval(pollAgenticState, 5000);
})();
