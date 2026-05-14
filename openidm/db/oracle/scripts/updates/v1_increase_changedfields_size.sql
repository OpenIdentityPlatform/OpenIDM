ALTER TABLE openidm.auditconfig RENAME COLUMN changedfields TO changedfields_old;
ALTER TABLE openidm.auditconfig ADD (changedfields CLOB);
UPDATE openidm.auditconfig SET changedfields=changedfields_old;
ALTER TABLE openidm.auditconfig DROP COLUMN changedfields_old;

ALTER TABLE openidm.auditactivity RENAME COLUMN changedfields TO changedfields_old;
ALTER TABLE openidm.auditactivity ADD (changedfields CLOB);
UPDATE openidm.auditactivity SET changedfields=changedfields_old;
ALTER TABLE openidm.auditactivity DROP COLUMN changedfields_old;
