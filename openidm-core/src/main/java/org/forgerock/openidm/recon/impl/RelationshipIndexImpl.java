/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.recon.impl;

import java.util.Map;
import java.util.HashMap;

import java.util.List;

import java.util.UUID;

import org.forgerock.openidm.recon.Situation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.objset.ObjectSetException;

/**
 * An interface for managing relationships between objects, their current {@link Situation} and
 * maintaining an index of those relationships.
 * <p/>
 * The relationship index stores identifiers of the {@code sourceObject} and {@code targetObject}, in addition
 * to the last reconciled {@link Situation}.
 * </p>
 * If the relationship is in the process of being provisioned, it's {@link Situation} will be indexed
 * as {@link org.forgerock.openidm.recon.Situation#PENDING}.
 */
public class RelationshipIndexImpl {

    //TODO extract to interface

    /**
     * The table name for relationships in orientdb
     */
    final static String RELATIONSHIP_TABLE = "recon/links/";

    final static String OBJECT_ID = "_id";

    final static String SOURCE_OBJECT_KEY = "sourceObjectId";

    final static String TARGET_OBJECT_KEY = "targetObjectId";

    final static String SITUATION = "situation";

    final static Logger logger = LoggerFactory.getLogger(RelationshipIndexImpl.class);

    @Reference(name = "RepositoryService", referenceInterface = RepositoryService.class,
            bind = "bindRepositoryService", unbind = "unbindRepositoryService",
            cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private RepositoryService repositoryService = null;

    /**
     * TODO create query string, named or native? Native makes sense here, but whole implementations,
     * would need to be created for each repository type.
     */
    final static String queryString = "";

    /**
     * Construct an instance and log for tracing.
     */
    public RelationshipIndexImpl() {
        logger.trace("{} was instantiated.", RelationshipIndexImpl.class.getName());
    }

    /**
     * Construct an instance, with the specified {@link org.forgerock.openidm.repo.RepositoryService}
     * and log for tracing.
     *
     * @param repositoryService
     */
    public RelationshipIndexImpl(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
        logger.trace("{} was instantiated.", RelationshipIndexImpl.class.getName());
    }

    /**
     * Create link relationship between objects, with the default {@link Situation} of {@link Situation#PENDING}.
     * This indicates that the link has been created but is pending changes e.g. the {@code sourceObject} has not been
     * fully {@code provisioned} or {@code synchronised} to individual {@code targetObjects}
     *
     * @param sourceObject to link in the {@link RelationshipIndexImpl}
     * @param targetObject to link in the {@link RelationshipIndexImpl}
     * @throws ReconciliationException if there was an error in creating the link
     */
    public void link(Map<String, Object> sourceObject, Map<String, Object> targetObject)
            throws LinkException {
        link(sourceObject, targetObject, Situation.PENDING);
    }

    /**
     * Create a link relationship between objects, with the specified {@link Situation}. If a relationship
     * doesn't already exist, it will be created.
     *
     * @param sourceObject to link in the {@link RelationshipIndexImpl}
     * @param targetObject to link in the {@link RelationshipIndexImpl}
     * @param situation    that indicates that relationship between {@code sourceObject} and {@code targetObject}
     * @throws LinkException if there was an error in creating the link
     */
    @SuppressWarnings("unchecked")
    public void link(Map<String, Object> sourceObject, Map<String, Object> targetObject, Situation situation)
            throws LinkException {

        List linkRelationship =
                queryRepositoryForLink(sourceObject, targetObject);

        if (linkRelationship.isEmpty()) {
            Map<String, Object> link = new HashMap<String, Object>();
            UUID uuid = UUID.randomUUID();
            String id = RELATIONSHIP_TABLE + uuid;
            link.put(OBJECT_ID, id);
            link.put(SOURCE_OBJECT_KEY, sourceObject.get(OBJECT_ID));
            link.put(TARGET_OBJECT_KEY, targetObject.get(OBJECT_ID));
            link.put(SITUATION, situation);
            try {
                repositoryService.create(RELATIONSHIP_TABLE, link);
            } catch (ObjectSetException e) {
                LinkException le = new LinkException(e);
                throw le;
            }
        } else {
            // There can only be one pairing TODO will this always be true?
            Map link = (Map) linkRelationship.get(0);
            link.put(SITUATION, situation);
            try {
                repositoryService.update((String) link.get(OBJECT_ID), null, link);
            } catch (ObjectSetException e) {
                LinkException le = new LinkException(e);
            }
        }
    }

    /**
     * Query the underlying {@link org.forgerock.openidm.repo.RepositoryService} to obtain the
     * link relationship between objects.
     *
     * @param sourceObject for the query
     * @param targetObject for the query
     * @return relationship record for the {@code sourceObject} and {@code targetObject}
     * @throws LinkException if there was an error in querying the {@link org.forgerock.openidm.repo.RepositoryService}
     *                       other exceptions
     *                       are wrapped and re-thrown
     */
    protected List queryRepositoryForLink(Map<String, Object> sourceObject,
                                          Map<String, Object> targetObject) throws LinkException {
        List resultSet = null;
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put(QueryConstants.QUERY_EXPRESSION,
                "select * from" + RELATIONSHIP_TABLE +
                        " where sourceObjectId = ${sourceObjectId} and targetObjectId = ${targetObjectId}");
        queryParameters.put(SOURCE_OBJECT_KEY, sourceObject.get(OBJECT_ID));
        queryParameters.put(TARGET_OBJECT_KEY, targetObject.get(OBJECT_ID));
        try {
            Map<String, Object> results = repositoryService.query(RELATIONSHIP_TABLE, queryParameters);
            resultSet = (List) results.get(QueryConstants.QUERY_RESULT);
        } catch (ObjectSetException e) {
            LinkException le = new LinkException(e);
            logger.error("There was an error querying the repository with {} ", queryParameters);
        }
        return resultSet;
    }

    /**
     * Determine if a link exists
     *
     * @param sourceObject to link
     * @param targetObject to link
     * @param situation    explaining the relationship
     * @throws LinkException
     */
    public Boolean updateLinkRelationship(Map<String, Object> sourceObject, Map<String, Object> targetObject,
                                          Situation situation) throws LinkException {

        List results = queryRepositoryForLink(sourceObject, targetObject);

        return !results.isEmpty();

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

}