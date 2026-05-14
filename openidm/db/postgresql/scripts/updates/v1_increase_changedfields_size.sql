-- Much faster way of updating column length in postgres
-- Instant change vs full-rewrite with ALTER
UPDATE pg_attribute SET atttypmod = -1 WHERE attrelid = 'openidm.auditconfig'::regclass AND attname = 'changedfields';
UPDATE pg_attribute SET atttypmod = -1 WHERE attrelid = 'openidm.auditactivity'::regclass AND attname = 'changedfields';
