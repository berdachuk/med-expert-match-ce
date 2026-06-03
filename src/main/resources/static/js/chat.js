(function () {
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
        } catch (ignore) { }
        return rawData;
    }

    function applyDonePayload(rawData) {
        if (!rawData) return;
        try {
            var parsed = JSON.parse(rawData);
            if (parsed && typeof parsed.content === 'string') {
                currentMarkdownBuffer = parsed.content;
            }
        } catch (ignore) { }
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

    function createAgentPanel() {
        var wrap = document.createElement('div');
        wrap.className = 'mb-3 agent-panel-wrap';
        wrap.innerHTML =
            '<div class="small border rounded p-2 bg-light agent-panel-expanded">' +
                '<div class="agent-activity-header small fw-semibold text-muted">' +
                    '<span class="agent-activity-spinner"></span>Agent Execution' +
                    '<button class="agent-activity-collapse" type="button" title="Collapse">\u25b2</button>' +
                '</div>' +
                '<div class="overflow-auto agent-panel-entries"></div>' +
            '</div>' +
            '<div class="small border rounded p-2 bg-light agent-panel-summary d-none" role="button" tabindex="0" aria-expanded="false"></div>';
        return wrap;
    }

    function addActivityEntryToPanel(panelWrap, kind, message, agentId) {
        var entriesEl = panelWrap.querySelector('.agent-panel-entries');
        var entries = panelWrap._entries || [];
        entries.push({ kind: kind, message: message, agentId: agentId || 'orchestrator', ts: Date.now() });
        panelWrap._entries = entries;
        var isStreaming = kind !== 'done';
        var headerSpinner = panelWrap.querySelector('.agent-activity-header .agent-activity-spinner');
        if (headerSpinner) headerSpinner.style.display = isStreaming ? '' : 'none';
        var label = kind;
        if (kind === 'tool') label = 'tool_call';
        if (kind === 'plan') label = 'todo';
        if (kind === 'reasoning') label = 'reasoning';
        var showSpinner = isStreaming;
        var entryDiv = document.createElement('div');
        entryDiv.className = 'agent-activity-entry ' + escapeHtml(label);
        entryDiv.innerHTML = (showSpinner ? '<span class="agent-activity-spinner"></span>' : '') +
            '<div class="fw-semibold">' + escapeHtml(agentId || 'orchestrator') + '</div>' +
            '<div class="text-muted">' + escapeHtml(message) + '</div>';
        entriesEl.appendChild(entryDiv);
        entriesEl.scrollTop = entriesEl.scrollHeight;
    }

    function collapseAgentPanel(panelWrap, activityStartMs) {
        var expanded = panelWrap.querySelector('.agent-panel-expanded');
        var summary = panelWrap.querySelector('.agent-panel-summary');
        var entries = panelWrap._entries || [];
        if (!summary) return;
        var elapsedSec = activityStartMs ? Math.max(1, Math.round((Date.now() - activityStartMs) / 1000)) : 0;
        var agents = new Set(entries.map(function (e) { return e.agentId; }));
        summary.textContent = '\u25b8 ' + agents.size + ' agent(s) \u00b7 ' + entries.length + ' steps \u00b7 ' + elapsedSec + 's \u2014 click to expand';
        summary.setAttribute('aria-expanded', 'false');
        summary.classList.remove('d-none');
        if (expanded) expanded.classList.add('d-none');
    }

    function expandPanelFromSummary(summaryEl) {
        var wrap = summaryEl.closest('.agent-panel-wrap');
        if (!wrap) return;
        var expanded = wrap.querySelector('.agent-panel-expanded');
        var summary = wrap.querySelector('.agent-panel-summary');
        if (summary) summary.classList.add('d-none');
        if (expanded) expanded.classList.remove('d-none');
    }

    function togglePanelFromHeader(headerEl) {
        var wrap = headerEl.closest('.agent-panel-wrap');
        if (!wrap) return;
        var expanded = wrap.querySelector('.agent-panel-expanded');
        var summary = wrap.querySelector('.agent-panel-summary');
        if (expanded && !expanded.classList.contains('d-none')) {
            // collapse
            var entries = wrap._entries || [];
            var startMs = wrap._startMs;
            var elapsedSec = startMs ? Math.max(1, Math.round((Date.now() - startMs) / 1000)) : 0;
            var agents = new Set(entries.map(function (e) { return e.agentId; }));
            summary.textContent = '\u25b8 ' + agents.size + ' agent(s) \u00b7 ' + entries.length + ' steps \u00b7 ' + elapsedSec + 's \u2014 click to expand';
            summary.classList.remove('d-none');
            expanded.classList.add('d-none');
        } else if (summary && !summary.classList.contains('d-none')) {
            summary.classList.add('d-none');
            if (expanded) expanded.classList.remove('d-none');
        }
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

    function appendAgentPanel() {
        hideEmptyState();
        var panel = document.getElementById('messagePanel');
        var agentWrap = createAgentPanel();
        panel.appendChild(agentWrap);
        // wire header click to toggle
        var header = agentWrap.querySelector('.agent-activity-header');
        if (header) {
            header.addEventListener('click', function (e) {
                if (e.target.closest('.agent-activity-collapse')) return;
                togglePanelFromHeader(header);
            });
            var collapseBtn = header.querySelector('.agent-activity-collapse');
            if (collapseBtn) {
                collapseBtn.addEventListener('click', function (e) {
                    e.stopPropagation();
                    togglePanelFromHeader(header);
                });
            }
        }
        var summary = agentWrap.querySelector('.agent-panel-summary');
        if (summary) {
            summary.addEventListener('click', function () { expandPanelFromSummary(summary); });
        }
        panel.scrollTop = panel.scrollHeight;
        return agentWrap;
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

    function sendMessageStream(chatId, text, agentId, btn) {
        var sid = sessionId();
        var startMs = Date.now();
        appendUserBubble(text);
        var agentPanelWrap = appendAgentPanel();
        agentPanelWrap._startMs = startMs;
        agentPanelWrap._entries = [];
        beginAssistantBubble();

        var logSource = null;
        if (sid && typeof EventSource !== 'undefined') {
            logSource = new EventSource('/api/v1/logs/stream?sessionId=' + encodeURIComponent(sid));
            logSource.addEventListener('log', function (e) {
                var msg = e.data || '';
                var clean = msg.replace(/\[.*?\]\s*\[.*?\]\s*/, '');
                if (clean) addActivityEntryToPanel(agentPanelWrap, 'tool', clean, 'orchestrator');
            });
            logSource.onerror = function () { logSource.close(); };
        }

        addActivityEntryToPanel(agentPanelWrap, 'start', 'Starting agent turn\u2026', agentId || 'auto');

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
                collapseAgentPanel(agentPanelWrap, startMs);
                pollAgenticState();
                if (btn) btn.disabled = false;
            }
            function pump() {
                return reader.read().then(function (result) {
                    if (result.value) {
                        buffer.value += decoder.decode(result.value, { stream: true });
                    }
                    var parts = buffer.value.split('\n\n');
                    buffer.value = parts.pop() || '';
                    var streamDone = false;
                    parts.forEach(function (block) {
                        if (!block.trim()) return;
                        var evt = parseSseBlock(block);
                        if (evt.event === 'token') {
                            currentMarkdownBuffer += parseTokenChunk(evt.data);
                            updateAssistantBubble(true);
                        } else if (evt.event === 'agent') {
                            try {
                                var agentPayload = JSON.parse(evt.data);
                                if (agentPayload.type === 'agent_start') {
                                    addActivityEntryToPanel(agentPanelWrap, 'agent', 'Active: ' + agentPayload.agentId, agentPayload.agentId);
                                } else if (agentPayload.type === 'agent_done') {
                                    addActivityEntryToPanel(agentPanelWrap, 'done', 'Completed', agentPayload.agentId);
                                }
                            } catch (ignore) { }
                        } else if (evt.event === 'activity') {
                            try {
                                var act = JSON.parse(evt.data);
                                if (act.type === 'tool_call') {
                                    addActivityEntryToPanel(agentPanelWrap, 'tool', act.message || act.toolName, 'orchestrator');
                                } else if (act.type === 'reasoning') {
                                    addActivityEntryToPanel(agentPanelWrap, 'reasoning', act.message || 'Thinking\u2026', 'orchestrator');
                                } else if (act.type === 'todo_update' && act.todos) {
                                    addActivityEntryToPanel(agentPanelWrap, 'plan', 'Plan updated (' + act.todos.length + ' items)', 'orchestrator');
                                }
                            } catch (ignore) { }
                        } else if (evt.event === 'pipeline_stage') {
                            try {
                                var ps = JSON.parse(evt.data);
                                var statusIcon = ps.status === 'completed' ? '\u2713' : (ps.status === 'failed' ? '\u2717' : '\u25B6');
                                addActivityEntryToPanel(agentPanelWrap, 'pipeline ' + ps.status, statusIcon + ' ' + ps.stage + ' (' + ps.agent + ')', ps.agent);
                            } catch (ignore) { }
                        } else if (evt.event === 'done') {
                            applyDonePayload(evt.data);
                            streamDone = true;
                        }
                    });
                    if (streamDone) {
                        finishStream();
                        return;
                    }
                    if (result.done) {
                        if (buffer.value.trim()) {
                            var evt = parseSseBlock(buffer.value);
                            if (evt.event === 'done') applyDonePayload(evt.data);
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
            addActivityEntryToPanel(agentPanelWrap, 'done', 'Error: ' + (err && err.message ? err.message : 'stream failed'), agentId || 'auto');
            collapseAgentPanel(agentPanelWrap, startMs);
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
                var ref = bundle.auditReferenceHash ? (' Audit ref: ' + bundle.auditReferenceHash.substring(0, 12) + '\u2026') : '';
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
                var ref = result.auditReferenceHash ? (' Audit ref: ' + result.auditReferenceHash.substring(0, 12) + '\u2026') : '';
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