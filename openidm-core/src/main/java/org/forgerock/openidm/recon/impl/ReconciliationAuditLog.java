package org.forgerock.openidm.recon.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.ObjectSetException;

import org.forgerock.openidm.repo.RepositoryService;

/**
 * Audit logger class to log native changes in reconciliation. For now whole objects
 * are stored, but in the future differences should be stored as
 * {@link org.forgerock.openidm.objset.Patch}.
 *
 */
public class ReconciliationAuditLog {

    final static Logger logger = LoggerFactory.getLogger(ReconciliationAuditLog.class);

    final static String RECONCILIATION_AUDIT = "recon/audit/";

    private RepositoryService repositoryService;

    public ReconciliationAuditLog(RepositoryService reconciliationService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Log the patch ( or whole object ) whatever it is. This creates an audit entry
     * with a record that contains a json structure as:
     *
     * <ul>
     * <li>_id record id</li>
     * <li>sourceObjectId is the source object identifier for look ups</li>
     * <li>sourceObject is the whole json structure of the sourceObject (to be removed in the future)</li>
     * <li>changes is the json structure patch object</li>
     * </ul>
     * @param sourceObject that had native changes detected
     * @param patch json structure (or maybe managed object for now, as json structure)
     */
    public void logNativeChange(Map<String, Object> sourceObject, Map<String, Object> patch) {
        UUID uuid = UUID.randomUUID();
        String id = RECONCILIATION_AUDIT + uuid.toString();
        Map<String, Object> auditEntry = new HashMap<String, Object>();
        auditEntry.put("_id", id);
        auditEntry.put("sourceObjectId", sourceObject.get("_id"));
        auditEntry.put("sourceObject", sourceObject);
        auditEntry.put("changes", patch);
        try {
            repositoryService.create(id, auditEntry);
        } catch (ObjectSetException e) {
            logger.error("There was an error in writing to the audit log: {} ", e.getLocalizedMessage());
        }
    }
}
