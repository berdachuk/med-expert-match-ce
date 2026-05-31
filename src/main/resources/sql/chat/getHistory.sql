SELECT id, chat_id, role, content, sequence_number, tokens_used, created_at
FROM medexpertmatch.chat_message
WHERE chat_id = :chatId
  AND deleted_at IS NULL
ORDER BY sequence_number ASC
LIMIT :limit OFFSET :offset
