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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.objset.ObjectSetException;

/**
 * An interface for managing relationships between objects, their current {@link Situation} and
 * maintaining an index of those relationships.
 * <p/>
 * The relationship index stores identifiers of the {@code sourceObject} and {@code targetObject}, in addition
 * to the last reconciled {@link Situation}.
 * </p>
 * If the relationship is in the process of being provisioned, it's {@link Situation} will be indexed
 * as {@link Situation} PENDING.
 */
public class RelationshipIndexImpl {

    //TODO extract to interface

    final static Logger logger = LoggerFactory.getLogger(RelationshipIndexImpl.class);

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
     * Construct an instance, with the specified {@link org.forgerock.openidm.repo.RepositoryService} and log for tracing.
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
        Map<String, Object> linkRelationship =
                queryRepositoryForLink(null, sourceObject, targetObject);
    }

    /**
     * Create a link relationship between objects, with the specified {@link Situation}.
     *
     * @param sourceObject to link in the {@link RelationshipIndexImpl}
     * @param targetObject to link in the {@link RelationshipIndexImpl}
     * @param situation    that indicates that relationship between {@code sourceObject} and {@code targetObject}
     * @throws ReconciliationException if there was an error in creating the link
     */
    public void link(Map<String, Object> sourceObject, Map<String, Object> targetObject, Situation situation)
            throws LinkException {
        Map<String, Object> linkRelationship =
                queryRepositoryForLink(null, sourceObject, targetObject);
    }

    /**
     * Given a {@code sourceObject} and a{@code targetObject} update the link between them with the given
     * {@link Situation}.
     *
     * @param sourceObject to link in the {@link RelationshipIndexImpl}
     * @param targetObject to link in the {@link RelationshipIndexImpl}
     * @param situation    that indicates the relationship between {@code sourceObject} and {@code targetObject}
     * @throws ReconciliationException if there was an error in creating the link
     */
    public void updateLinkedSituation(Map<String, Object> sourceObject, Map<String, Object> targetObject, Situation situation)
            throws LinkException {
        Map<String, Object> linkRelationship =
                queryRepositoryForLink(null, sourceObject, targetObject);
    }

    /**
     * Query the underlying {@link org.forgerock.openidm.repo.RepositoryService} to obtain the
     * link relationship between objects.
     *
     * @param queryName    to use when retrieving the relationship record from the repository
     * @param sourceObject for the query
     * @param targetObject for the query
     * @return relationship record for the {@code sourceObject} and {@code targetObject}
     * @throws LinkException if there was an error in querying the {@link org.forgerock.openidm.repo.RepositoryService} other exceptions
     *                       are wrapped and re-thrown
     */
    protected Map<String, Object> queryRepositoryForLink(String queryName, Map<String, Object> sourceObject,
                                                         Map<String, Object> targetObject) throws LinkException {
        Map<String, Object> linkRelationship = null;
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        try {
            linkRelationship = repositoryService.query(queryName, null);
        } catch (ObjectSetException e) {
            LinkException le = new LinkException(e);
            logger.error("There was an error querying the repository with {} ", queryParameters);
        }
        return linkRelationship;
    }

    /**
     * Set the {@link org.forgerock.openidm.repo.RepositoryService} that this relationship index will be using.
     *
     * @param repositoryService
     */
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

}