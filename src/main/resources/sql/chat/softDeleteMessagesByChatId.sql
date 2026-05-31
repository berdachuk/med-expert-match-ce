UPDATE medexpertmatch.chat_message
SET content = '[deleted]',
    deleted_at = CURRENT_TIMESTAMP
WHERE chat_id = :chatId
  AND deleted_at IS NULL
