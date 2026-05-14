DROP INDEX `idx_auditaccess_status` ON openidm.auditaccess;
DROP INDEX `idx_auditconfig_transactionid` ON openidm.auditconfig;
DROP INDEX `idx_auditactivity_transactionid` ON openidm.auditactivity;

DROP INDEX `idx_auditrecon_targetobjectid` ON openidm.auditrecon;
DROP INDEX `idx_auditrecon_sourceobjectid` ON openidm.auditrecon;
DROP INDEX `idx_auditrecon_activitydate` ON openidm.auditrecon;
DROP INDEX `idx_auditrecon_mapping` ON openidm.auditrecon;
DROP INDEX `idx_auditrecon_situation` ON openidm.auditrecon;
DROP INDEX `idx_auditrecon_status` ON openidm.auditrecon;
