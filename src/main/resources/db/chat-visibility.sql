-- Apply before deploying when Hibernate schema updates are disabled (PostgreSQL).
ALTER TABLE conversation_participant ADD COLUMN IF NOT EXISTS hidden_at timestamp;
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS edited_at timestamp;
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS deleted_at timestamp;
