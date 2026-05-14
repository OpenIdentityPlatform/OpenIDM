DROP INDEX idx_auditconfig_transactionid;
DROP INDEX idx_auditactivity_transid;

PROMPT Creating Index idx_auditrecon_reconid on auditrecon ...
CREATE INDEX idx_auditrecon_reconid ON auditrecon
(
  reconid
)
;

PROMPT Creating Index idx_auditrecon_entrytype on auditrecon ...
CREATE INDEX idx_auditrecon_entrytype ON auditrecon
(
  entrytype
)
;
