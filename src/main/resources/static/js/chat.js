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

    var CONTENT_SECTION_PATTERN = /(?:^|[\n.]\s*)(Case Summary|Clinical Presentation|Matched Doctors|Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\s*:?(?=\s*(?:\n|$))/gim;
    var NUMBERED_SECTION_PATTERN = /\d+\.\s*(Case Summary|Clinical Presentation|Matched Doctors|Matching Rationale(?: Explanation)?|Evidence Summary|Recommendations)\b/gim;

    function isChecklistSectionLine(line) {
        if (!line) return false;
        var lower = line.toLowerCase();
        if (line.indexOf('?') >= 0 && (lower.indexOf('yes') >= 0 || lower.indexOf('no') >= 0)) return true;
        return lower.indexOf('brief overview') >= 0 || lower.indexOf('only analysis') >= 0
            || lower.indexOf('invent demographics') >= 0 || lower.indexOf('will monitor') >= 0;
    }

    function lineAtIndex(text, idx) {
        var lineStart = idx;
        while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') lineStart--;
        var lineEnd = text.indexOf('\n', idx);
        if (lineEnd < 0) lineEnd = text.length;
        return text.substring(lineStart, lineEnd).trim();
    }

    function isValidSectionMatch(text, idx) {
        return !isChecklistSectionLine(lineAtIndex(text, idx));
    }

    function findAllSectionStarts(text) {
        var starts = [];
        var m;
        NUMBERED_SECTION_PATTERN.lastIndex = 0;
        while ((m = NUMBERED_SECTION_PATTERN.exec(text)) !== null) {
            if (isValidSectionMatch(text, m.index)) starts.push(m.index);
        }
        CONTENT_SECTION_PATTERN.lastIndex = 0;
        while ((m = CONTENT_SECTION_PATTERN.exec(text)) !== null) {
            if (isValidSectionMatch(text, m.index)) starts.push(m.index);
        }
        return starts;
    }

    function hasPlanningMarkers(lower) {
        return lower.indexOf('mental sandbox') >= 0 || lower.indexOf('constraint checklist') >= 0
            || lower.indexOf('confidence score') >= 0 || lower.indexOf('strategizing complete') >= 0
            || lower.indexOf('the user wants') >= 0 || lower.indexOf('summarize the case:') >= 0
            || lower.indexOf('explain matching rationale:') >= 0
            || lower.indexOf('present matched doctors:') >= 0
            || lower.indexOf('provide recommendations:') >= 0
            || lower.indexOf('key learnings') >= 0;
    }

    function findBestContentStart(text) {
        var lower = text.toLowerCase();
        var stratIdx = lower.lastIndexOf('strategizing complete');
        if (stratIdx >= 0) {
            var tail = text.substring(stratIdx + 'strategizing complete'.length);
            var tailStarts = findAllSectionStarts(tail);
            if (tailStarts.length) {
                return stratIdx + 'strategizing complete'.length + Math.min.apply(null, tailStarts);
            }
        }
        var starts = findAllSectionStarts(text);
        if (!starts.length) return -1;
        if (hasPlanningMarkers(lower)) {
            return Math.max.apply(null, starts);
        }
        return Math.min.apply(null, starts);
    }

    function splitReasoningFromText(text) {
        if (!text) return { reasoning: null, content: text || '' };
        if (text.indexOf('class="llm-thinking"') >= 0 || text.indexOf('llm-thinking') >= 0) {
            return { reasoning: null, content: text };
        }
        var normalized = text.replace(/<unused\d+>/gi, '').trim();
        var contentStart = findBestContentStart(normalized);
        if (contentStart > 40) {
            return {
                reasoning: normalized.substring(0, contentStart).trim(),
                content: normalized.substring(contentStart).trim()
            };
        }
        if (contentStart > 0 && hasPlanningMarkers(normalized.toLowerCase())) {
            return {
                reasoning: normalized.substring(0, contentStart).trim(),
                content: normalized.substring(contentStart).trim()
            };
        }
        if (hasPlanningMarkers(normalized.toLowerCase())) {
            var fallback = normalized.toLowerCase().lastIndexOf('case summary');
            if (fallback > 40) {
                return {
                    reasoning: normalized.substring(0, fallback).trim(),
                    content: normalized.substring(fallback).trim()
                };
            }
        }
        return { reasoning: null, content: normalized };
    }

    function sanitizeAssistantHtml(html) {
        if (typeof DOMPurify === 'undefined') {
            return html;
        }
        return DOMPurify.sanitize(html, {
            ADD_TAGS: ['details', 'summary', 'div'],
            ADD_ATTR: ['class', 'open']
        });
    }

    function renderMarkdown(text) {
        if (!text) return '';
        if (typeof marked === 'undefined' || typeof DOMPurify === 'undefined') {
            return escapeHtml(text);
        }
        var raw = marked.parse(text, { breaks: true });
        return DOMPurify.sanitize(raw);
    }

    function unwrapAnswerMarkdown(remainder) {
        return remainder
            .replace(/<div class="llm-answer-label"[^>]*>\s*Response\s*<\/div>/gi, '')
            .replace(/<\/?div[^>]*class="llm-answer"[^>]*>/gi, '')
            .trim();
    }

    function renderPreformattedAssistant(text) {
        var detailsEnd = text.indexOf('</details>');
        if (detailsEnd > 0) {
            var htmlPart = text.substring(0, detailsEnd + '</details>'.length);
            var answerMd = unwrapAnswerMarkdown(text.substring(detailsEnd + '</details>'.length));
            return sanitizeAssistantHtml(
                htmlPart + '<div class="llm-answer-label">Response</div><div class="llm-answer">'
                + renderMarkdown(answerMd) + '</div>'
            );
        }
        return null;
    }

    function renderAssistantContent(text) {
        if (!text) return '';
        var preformatted = renderPreformattedAssistant(text);
        if (preformatted) return preformatted;

        var split = splitReasoningFromText(text);
        var parts = [];
        if (split.reasoning) {
            parts.push(
                '<details class="llm-thinking"><summary>Model reasoning (click to expand)</summary>' +
                '<div class="llm-thinking-body">' + escapeHtml(split.reasoning) + '</div></details>' +
                '<div class="llm-answer-label">Response</div>'
            );
        }
        parts.push('<div class="llm-answer">' + renderMarkdown(split.content || text) + '</div>');
        return sanitizeAssistantHtml(parts.join(''));
    }

    function initHistoricalMarkdown() {
        document.querySelectorAll('.assistant-raw-pending').forEach(function (el) {
            var raw = el.textContent || '';
            el.classList.remove('assistant-raw-pending');
            el.innerHTML = renderAssistantContent(raw);
        });
        document.querySelectorAll('[data-markdown]').forEach(function (el) {
            var md = el.getAttribute('data-markdown') || '';
            el.innerHTML = renderAssistantContent(md);
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
        applyDonePackaging(rawData);
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
                    '<span class="agent-activity-spinner" style="display:none"></span>Agent Execution' +
                    '<button class="agent-activity-collapse" type="button" title="Collapse">\u25b2</button>' +
                '</div>' +
                '<div class="overflow-auto agent-panel-entries"></div>' +
            '</div>' +
            '<div class="small border rounded p-2 bg-light agent-panel-summary d-none" role="button" tabindex="0" aria-expanded="false"></div>';
        return wrap;
    }

    function clearEntrySpinners(entriesEl) {
        if (!entriesEl) return;
        entriesEl.querySelectorAll('.agent-activity-entry .agent-activity-spinner').forEach(function (el) {
            el.remove();
        });
    }

    function syncPanelSpinners(panelWrap) {
        if (!panelWrap) return;
        var entriesEl = panelWrap.querySelector('.agent-panel-entries');
        var active = panelWrap._streamActive === true;
        clearEntrySpinners(entriesEl);
        if (active) {
            panelWrap.classList.add('agent-panel-active');
            var entryNodes = entriesEl ? entriesEl.querySelectorAll('.agent-activity-entry') : [];
            if (entryNodes.length > 0) {
                var spinner = document.createElement('span');
                spinner.className = 'agent-activity-spinner';
                entryNodes[entryNodes.length - 1].insertBefore(spinner, entryNodes[entryNodes.length - 1].firstChild);
            }
        } else {
            panelWrap.classList.remove('agent-panel-active');
        }
    }

    function addActivityEntryToPanel(panelWrap, kind, message, agentId) {
        var entriesEl = panelWrap.querySelector('.agent-panel-entries');
        var entries = panelWrap._entries || [];
        entries.push({ kind: kind, message: message, agentId: agentId || 'orchestrator', ts: Date.now() });
        panelWrap._entries = entries;
        var label = kind;
        if (kind === 'tool') label = 'tool_call';
        if (kind === 'plan') label = 'todo';
        if (kind === 'reasoning') label = 'reasoning';
        if (kind === 'llm') label = 'llm';
        var entryDiv = document.createElement('div');
        entryDiv.className = 'agent-activity-entry ' + escapeHtml(label);
        entryDiv.innerHTML =
            '<div class="fw-semibold">' + escapeHtml(agentId || 'orchestrator') + '</div>' +
            '<div class="text-muted">' + escapeHtml(message) + '</div>';
        entriesEl.appendChild(entryDiv);
        syncPanelSpinners(panelWrap);
        entriesEl.scrollTop = entriesEl.scrollHeight;
    }

    function collapseAgentPanel(panelWrap, activityStartMs) {
        var expanded = panelWrap.querySelector('.agent-panel-expanded');
        var summary = panelWrap.querySelector('.agent-panel-summary');
        var entries = panelWrap._entries || [];
        if (!summary) return;
        var elapsedSec = activityStartMs ? Math.max(1, Math.round((Date.now() - activityStartMs) / 1000)) : 0;
        var agents = new Set(entries.map(function (e) { return e.agentId; }));
        var summaryText = '\u25b8 ' + agents.size + ' agent(s) \u00b7 ' + entries.length + ' steps \u00b7 ' + elapsedSec + 's';
        if (panelWrap._llmRollup) {
            summaryText += ' \u00b7 ' + panelWrap._llmRollup;
        }
        summaryText += ' \u2014 click to expand';
        summary.textContent = summaryText;
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
        wrap.className = 'chat-message-row chat-message-row--user';
        wrap.innerHTML =
            '<div class="chat-message-label"><span class="badge bg-primary">user</span></div>' +
            '<div class="chat-message-bubble p-2 rounded bg-primary-subtle">' + escapeHtml(text) + '</div>';
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
        wrap.className = 'chat-message-row chat-message-row--assistant chat-streaming';
        wrap.innerHTML =
            '<div class="chat-message-label"><span class="badge bg-secondary">assistant</span></div>' +
            '<div class="chat-message-bubble p-2 rounded bg-light border chat-markdown"></div>';
        panel.appendChild(wrap);
        currentAssistantBubble = wrap.querySelector('.chat-markdown');
        currentMarkdownBuffer = '';
        panel.scrollTop = panel.scrollHeight;
    }

    function updateAssistantBubble() {
        if (!currentAssistantBubble) return;
        currentAssistantBubble.innerHTML = renderAssistantContent(currentMarkdownBuffer);
        var panel = document.getElementById('messagePanel');
        panel.scrollTop = panel.scrollHeight;
    }

    function finalizeAssistantBubble() {
        if (currentAssistantBubble) {
            updateAssistantBubble();
            if (currentAssistantBubble.closest('.chat-streaming')) {
                currentAssistantBubble.closest('.chat-streaming').classList.remove('chat-streaming');
            }
        }
        currentAssistantBubble = null;
        currentMarkdownBuffer = '';
    }

    function selectedChatMode() {
        return 'expert_match';
    }

    function updateChatModeCostHint() {
    }

    function renderExplainabilityPanel(parentBubble, payload) {
        if (!parentBubble || !payload || !payload.matchExplainability || !payload.matchExplainability.length) {
            return;
        }
        var existing = parentBubble.parentElement?.querySelector('.chat-explainability-panel');
        if (existing) existing.remove();
        var panel = document.createElement('div');
        panel.className = 'chat-explainability-panel';
        var title = document.createElement('div');
        title.className = 'fw-semibold mb-1';
        title.textContent = 'Match signal breakdown (research only)';
        panel.appendChild(title);
        if (payload.relativeCostHint) {
            var cost = document.createElement('div');
            cost.className = 'text-muted mb-1';
            cost.textContent = payload.chatMode + ' · ' + payload.routingTier + ' · ' + payload.relativeCostHint;
            panel.appendChild(cost);
        }
        var table = document.createElement('table');
        table.className = 'table table-sm table-borderless mb-0';
        table.innerHTML = '<thead><tr><th>Doctor</th><th>Score</th><th>Vector</th><th>Graph</th><th>History</th></tr></thead>';
        var tbody = document.createElement('tbody');
        payload.matchExplainability.forEach(function (row) {
            var tr = document.createElement('tr');
            tr.innerHTML =
                '<td>' + escapeHtml(row.doctorName || row.doctorId || '') + '</td>' +
                '<td>' + escapeHtml(String(row.overallScore != null ? row.overallScore : '')) + '</td>' +
                '<td>' + escapeHtml(String(row.vectorPercent != null ? row.vectorPercent + '%' : '')) + '</td>' +
                '<td>' + escapeHtml(String(row.graphPercent != null ? row.graphPercent + '%' : '')) + '</td>' +
                '<td>' + escapeHtml(String(row.historyPercent != null ? row.historyPercent + '%' : '')) + '</td>';
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        panel.appendChild(table);
        var row = parentBubble.closest('.chat-message-row');
        if (row) row.appendChild(panel);
    }

    function applyDonePackaging(rawData) {
        if (!rawData) return;
        try {
            var parsed = JSON.parse(rawData);
            if (parsed && typeof parsed.content === 'string') {
                currentMarkdownBuffer = parsed.content;
            }
            if (currentAssistantBubble) {
                renderExplainabilityPanel(currentAssistantBubble, parsed);
            }
        } catch (ignore) { }
    }

    function sendMessageStream(chatId, text, agentId, btn) {
        var sid = sessionId();
        var startMs = Date.now();
        appendUserBubble(text);
        var agentPanelWrap = appendAgentPanel();
        agentPanelWrap._startMs = startMs;
        agentPanelWrap._entries = [];
        agentPanelWrap._streamActive = true;
        agentPanelWrap.classList.add('agent-panel-active');
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
            body: JSON.stringify({
                content: text,
                agentId: agentId || 'auto',
                chatMode: selectedChatMode()
            })
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
                agentPanelWrap._streamActive = false;
                syncPanelSpinners(agentPanelWrap);
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
                            updateAssistantBubble();
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
                                } else if (act.type === 'llm_call') {
                                    addActivityEntryToPanel(agentPanelWrap, 'llm', act.message || 'LLM call', act.clientType || 'llm');
                                } else if (act.type === 'llm_turn_summary') {
                                    agentPanelWrap._llmRollup = act.message;
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
            agentPanelWrap._streamActive = false;
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
            }).then(function (r) {
                if (!r.ok) throw new Error('Delete failed');
                window.location.href = '/chat';
            }).catch(function (e) { console.error('Failed to delete chat', e); alert('Delete failed'); });
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