DROP INDEX openidm.idx_auditconfig_transactionid;
DROP INDEX openidm.idx_auditactivity_transactionid;

CREATE INDEX idx_auditrecon_reconid ON openidm.auditrecon (reconid);
CREATE INDEX idx_auditrecon_entrytype ON openidm.auditrecon (entrytype);
