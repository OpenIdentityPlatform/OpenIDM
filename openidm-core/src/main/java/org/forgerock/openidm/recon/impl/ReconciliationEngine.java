package org.forgerock.openidm.recon.impl;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;

import org.forgerock.openidm.objset.ObjectSet;
//import org.forgerock.openidm.provisioner.ProvisionerService;

import org.forgerock.openidm.objset.ObjectSetException;

/**
 * Engine that coordinates reconciliation.
 */
public class ReconciliationEngine {

    final static Logger logger = LoggerFactory.getLogger(ReconciliationEngine.class);

    final static String MANAGED_USERS = "managed/user";

    final static String SYSTEM_OBJECTS = "system/";

    /**
     * enum for recon type
     */
    public enum ReconType {
        FULL, INCREMENTAL
    }

    private RelationshipIndexImpl relationshipIndex;

    @Reference(name = "RepositoryService", referenceInterface = RepositoryService.class,
            bind = "bindRepositoryService", unbind = "unbindRepositoryService", cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC)
    private RepositoryService repositoryService;

    @Reference(name = "ProvisionerService", referenceInterface = ObjectSet.class,
            bind = "bindProvisionerService", unbind = "unbindProvisionerService",
            cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private ObjectSet provisionerService;

    private ReconciliationConfigurationEntry configurationEntry;

    private Correlator correlator = new Correlator();

    public ReconciliationEngine(ReconciliationConfigurationEntry configurationEntry) {
        this.configurationEntry = configurationEntry;
    }

    protected void reconcile() throws ReconciliationException {

        String type = configurationEntry.getReconciliationType();
        if (ReconType.valueOf(type).equals(ReconType.INCREMENTAL)) {
            processIncrementalReconciliation();
        } else if (ReconType.valueOf(type).equals(ReconType.FULL)) {
            processFullReconciliation();
        } else {
            throw new ReconciliationException("Unrecognized reconciliation type");
        }

    }

    private void processIncrementalReconciliation() throws ReconciliationException {
    }

    //TODO update
    @SuppressWarnings("unchecked")
    private void processFullReconciliation() throws ReconciliationException {
        try {
            Correlator correlator = new Correlator();
            List<Map<String, Object>> correlatedAccounts = new ArrayList<Map<String, Object>>();
            Map<String, Object> sourceResults = provisionerService.query(null, null);
            Map<String, Object> targetResults = repositoryService.query(null, null);
            List<Map<String, Object>> sourceSet = (List<Map<String, Object>>) sourceResults.get(QueryConstants.QUERY_RESULT);
            List<Map<String, Object>> targetSet = (List<Map<String, Object>>) targetResults.get(QueryConstants.QUERY_RESULT);
            for (Map<String, Object> sourceObject : sourceSet) {
                //correlator.correlates()
            }
        } catch (ObjectSetException e) {
            throw new ReconciliationException(e);
        }
    }

    public RelationshipIndexImpl getRelationshipIndex() {
        return relationshipIndex;
    }

    public void setRelationshipIndex(RelationshipIndexImpl relationshipIndex) {
        this.relationshipIndex = relationshipIndex;
    }


    /**
     * TODO What else needs to be done in the case of a bind
     */
    protected void bindRepositoryService(RepositoryService repositoryService) {
        logger.debug("RepositoryService was bound");
        this.repositoryService = repositoryService;
    }

    /**
     * TODO What else needs to be done in the case of an unbind, it really can't function if null
     */
    protected void unbindRepositoryService() {
        logger.debug("RepositoryService was unbound");
        this.repositoryService = null;
    }

    /**
     * TODO What else needs to be done in the case of a bind
     */
    protected void bindProvisionerService(ObjectSet provisionerService) {
        logger.debug("ProvisionerService was bound");
        this.provisionerService = provisionerService;
    }

    /**
     * TODO What else needs to be done in the case of an unbind, it really can't function if null
     */
    protected void unbindProvisionerService(ObjectSet provisionerService) {
        logger.debug("ProvisionerService was unbound");
        this.provisionerService = provisionerService;
    }

    public void setConfigurationEntry(ReconciliationConfigurationEntry configurationEntry) {
        this.configurationEntry = configurationEntry;
    }

}