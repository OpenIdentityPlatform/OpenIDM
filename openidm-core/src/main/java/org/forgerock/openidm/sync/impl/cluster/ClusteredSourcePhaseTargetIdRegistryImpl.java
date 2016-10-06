/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl.cluster;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.util.ContextUtil;
import org.slf4j.Logger;

/**
 * @see ClusteredSourcePhaseTargetIdRegistry
 */
public class ClusteredSourcePhaseTargetIdRegistryImpl implements ClusteredSourcePhaseTargetIdRegistry {
    private static final String REPO_TARGET_IDS_PATH = "/repo/clusteredrecontargetids";
    private static final String RECON_ID = "reconId";
    private static final String TARGET_ID = "targetId";
    private static final String DELETE_TARGET_IDS_COMMAND = "delete-target-ids-for-recon";
    private static final String COMMAND_ACTION = "command";
    private static final String COMMAND_ID = "commandId";
    private static final String COMMAND_RECON_ID_PARAM = "reconId";
    private static final JsonPointer RECON_ID_POINTER = new JsonPointer(RECON_ID);
    private static final JsonPointer TARGET_ID_POINTER = new JsonPointer(TARGET_ID);

    /*
    Note that this data structure must be a concurrent Set. Concurrent, as multiple threads will be calling targetIdReconciled.
    A Set, as the DB indices in the clusteredrecon/targetids table must be unique across the tuple <reconId, targetId>, and
    it is possible that a single target is reconciled to > 1 source. In this case, only the single target id will be written
    to the database. This is correct because the DB must contain a record of all target ids reconciled to source ids during
    the source phase.
     */
    private final ConcurrentSkipListSet<String> sourcePhaseReconciledTargetIds;
    private final ConnectionFactory connectionFactory;
    private final Logger logger;

    public ClusteredSourcePhaseTargetIdRegistryImpl(ConnectionFactory connectionFactory, Logger logger) {
        sourcePhaseReconciledTargetIds = new ConcurrentSkipListSet<>();
        this.connectionFactory = connectionFactory;
        this.logger = logger;
    }

    @Override
    public void targetIdReconciled(String targetId) {
        sourcePhaseReconciledTargetIds.add(targetId);
    }

    @Override
    public Collection<String> getTargetPhaseIds(String reconId, Collection<String> allTargetIds) {
        Set<String> targetPhaseIds = new HashSet();
        for (String targetId : allTargetIds) {
            if (targetIdNotReconciled(reconId, targetId)) {
                targetPhaseIds.add(targetId);
            }
        }
        return targetPhaseIds;
    }

    @Override
    public void persistTargetIds(String reconId) throws SynchronizationException {
        for (String targetId : sourcePhaseReconciledTargetIds) {
            persistTargetId(reconId, targetId);
        }
    }

    @Override
    public void deletePersistedTargetIds(String reconId) throws SynchronizationException {
        try {
            connectionFactory.getConnection().action(ContextUtil.createInternalContext(),
                    Requests.newActionRequest(REPO_TARGET_IDS_PATH, COMMAND_ACTION)
                            .setAdditionalParameter(COMMAND_ID, DELETE_TARGET_IDS_COMMAND)
                            .setAdditionalParameter(COMMAND_RECON_ID_PARAM, reconId));
        } catch (ResourceException e) {
            logger.error("Exception caught deleting target ids for recon id: {}", reconId, e);
        }
    }

    private void persistTargetId(String reconId, String targetId) {
        try {
            connectionFactory.getConnection().create(ContextUtil.createInternalContext(),
                    Requests.newCreateRequest(REPO_TARGET_IDS_PATH, getTargetIdCreateRequest(reconId, targetId)));
        } catch (ResourceException e) {
            logger.error("Exception persisting target id {}. This will likely result in a specious target " +
                    "reconciliation for this id.", targetId, e);
        }
    }

    private JsonValue getTargetIdCreateRequest(String reconId, String targetId) {
        return json(object(field(RECON_ID, reconId), field(TARGET_ID, targetId)));
    }

    private boolean targetIdNotReconciled(String reconId, String targetId) {
        final List<ResourceResponse> queryResult = new ArrayList<>();
        try {
            connectionFactory.getConnection().query(
                ContextUtil.createInternalContext(),
                Requests.newQueryRequest(REPO_TARGET_IDS_PATH).setQueryFilter(
                    and(
                        equalTo(RECON_ID_POINTER, reconId),
                        equalTo(TARGET_ID_POINTER, targetId)
                    )),
                queryResult);
            return queryResult.isEmpty();
        } catch (ResourceException e) {
            logger.error("Exception caught determining if target id {} was reconciled during the source phase. " +
                    "This target id will not be handled in the target phase.", targetId, e);
            return false;
        }
    }
}
