
ALTER TABLE task ADD pk_depend_parent INTEGER;
ALTER TABLE task ADD int_depend_count INTEGER NOT NULL DEFAULT 0;
