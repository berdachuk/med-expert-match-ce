SELECT COALESCE(MAX(sequence_number), 0) + 1 AS next_seq
FROM medexpertmatch.chat_message
WHERE chat_id = :chatId
