DROP INDEX idx_auditconfig_transactionid ON [openidm].[auditconfig];
DROP INDEX idx_auditactivity_transactionid ON [openidm].[auditactivity];

CREATE INDEX idx_auditrecon_reconid ON [openidm].[auditrecon] (reconid ASC);
CREATE INDEX idx_auditrecon_entrytype ON [openidm].[auditrecon] (entrytype ASC);
