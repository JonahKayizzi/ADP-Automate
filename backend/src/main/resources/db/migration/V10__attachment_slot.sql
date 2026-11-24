ALTER TABLE attachments
  ADD COLUMN slot VARCHAR(32);

CREATE INDEX idx_attachments_owner_slot
  ON attachments (owner_type, owner_id, slot)
  WHERE retired = FALSE;
