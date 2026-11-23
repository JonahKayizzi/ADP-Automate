ALTER TABLE processing_forms
    ALTER COLUMN step_number TYPE INTEGER USING step_number::integer;
