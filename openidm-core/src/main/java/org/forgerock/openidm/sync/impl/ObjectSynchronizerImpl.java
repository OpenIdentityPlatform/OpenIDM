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

package org.forgerock.openidm.sync.impl;

import java.util.Map;
import java.util.Collection;

import org.apache.felix.scr.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.Constants;

import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;

import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.openidm.repo.RepositoryService;//hand routed TODO need to remove in the future
import org.forgerock.openidm.provisioner.ProvisionerService; //hand routed TODO need to remove in the future

import org.forgerock.openidm.sync.ObjectSynchronizer;

/**
 * A default implementation for the {@link org.forgerock.openidm.sync.ObjectSynchronizer} interface implemented
 * as an OSGi Service.
 */
@Component(name = "object-synchronization", immediate = true)
@Service(value = ObjectSynchronizer.class)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Object Synchronizer Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = "ForgeRock AS")
})
public class ObjectSynchronizerImpl implements ObjectSynchronizer {

    final static Logger logger = LoggerFactory.getLogger(ObjectSynchronizerImpl.class);

    @Reference(name = "RepositoryService", referenceInterface = RepositoryService.class,
            bind = "bindRepositoryService", unbind = "unbindRepositoryService",
            cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private RepositoryService repositoryService;

    @Reference(name = "ProvisionerService", referenceInterface = ProvisionerService.class,
            bind = "bindProvisionerService", unbind = "unbindProvisionerService",
            cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private ProvisionerService provisionerService;

    // TODO fix, this is hand routing for now
    private ObjectSet managedObjects;
    private ObjectSet systemObjects;

    private MappingConfiguration mappingConfiguration;
    private Projection projection = new Projection();

    public ObjectSynchronizerImpl() {
    }

    @Override
    public void onCreate(String id, Map<String, Object> newValue) throws ObjectSynchronizationException {
        try {

            processSynchronousMappings(id, newValue);
            processAsynchronousMappings(id, newValue);

            Collection<MapEntry> asynchronousEntries =
                    mappingConfiguration.getAsynchronousEntriesFor(id);

            Collection<MapEntry> entries = mappingConfiguration.getMapEntries();
            Map<String, Object> sourceObject = null; // get sourceObject

            //Map<String, Object> value = projection.projectValues(sourceObject, mappingConfiguration);

            repositoryService.create(id, newValue);
        } catch (ObjectSetException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.error("{} caused by {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), e.getLocalizedMessage()});
            throw ose;
        }// catch (JsonPathException jpe) {
        //  ObjectSynchronizationException ose = new ObjectSynchronizationException(jpe);
        //  logger.error("{} caused by {}",
        //          new Object[]{ObjectSynchronizerImpl.class.getName(), ose.getLocalizedMessage()});
        //}
    }

    @Override
    public void onUpdate(String id, Map<String, Object> newValue) throws ObjectSynchronizationException {
        try {
            repositoryService.update(id, null, newValue);
        } catch (ObjectSetException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.error("{} caused by {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), e.getLocalizedMessage()});
            throw ose;
        }
    }

    @Override
    public void onUpdate(String id, Map<String, Object> oldValue, Map<String, Object> newValue)
            throws ObjectSynchronizationException {
        try {
            repositoryService.update(id, null, newValue);
        } catch (ObjectSetException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.error("{} caused by {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), e.getLocalizedMessage()});
            throw ose;
        }
    }

    @Override
    public void onDelete(String id, Map<String, Object> objectValue) throws ObjectSynchronizationException {
        try {
            repositoryService.delete(id, null);
        } catch (ObjectSetException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.error("{} caused by {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), e.getLocalizedMessage()});
            throw ose;
        }
    }

    @Override
    public void onDelete(String id) throws ObjectSynchronizationException {
        try {
            repositoryService.delete(id, null);
        } catch (ObjectSetException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.error("{} caused by {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), e.getLocalizedMessage()});
            throw ose;
        }
    }


    private void processSynchronousMappings(String id, Map<String, Object> newValue) {
        Collection<MapEntry> synchronousEntries =
                mappingConfiguration.getSynchronousEntriesFor(id);
        for (MapEntry entry : synchronousEntries) {

        }
    }

    private void processAsynchronousMappings(String id, Map<String, Object> newValue) {

        Collection<MapEntry> asynchronousEntries =
                mappingConfiguration.getAsynchronousEntriesFor(id);
        for (MapEntry entry : asynchronousEntries) {

        }
    }

    @Activate
    private void activate(Map<String, Object> configuration) throws ObjectSynchronizationException {
        logger.debug("{} was activated with: {}",
                new Object[]{ObjectSynchronizerImpl.class.getName(), configuration});

        try {
            mappingConfiguration = new MappingConfiguration(configuration);
        } catch (JsonNodeException e) {
            ObjectSynchronizationException ose = new ObjectSynchronizationException(e);
            logger.debug("{} configuration error: {}",
                    new Object[]{ObjectSynchronizerImpl.class.getName(), ose.getLocalizedMessage()});
        }
    }

    @Deactivate
    private void deactivate(Map<String, Object> configuration) throws ObjectSynchronizationException {
        logger.debug("{} was deactivated with: {}",
                new Object[]{ObjectSynchronizerImpl.class.getName(), configuration});
    }


    /**
     * TODO What else needs to be done in the case of a bind
     * Bind the {@link RepositoryService} in use.
     *
     * @param repositoryService to use
     */
    protected void bindRepositoryService(RepositoryService repositoryService) {
        logger.debug("RepositoryService was bound");
        this.repositoryService = repositoryService;
    }

    /**
     * TODO What else needs to be done in the case of an unbind, it really can't function if null
     * Unbind the {@link RepositoryService} in use, this will leave recon in an unusable state
     */
    protected void unbindRepositoryService(RepositoryService repositoryService) {
        logger.debug("RepositoryService was unbound");
        this.repositoryService = null;
    }

    /**
     * TODO What else needs to be done in the case of a bind
     * Bind the {@link ProvisionerService} in use.
     *
     * @param provisionerService to use
     */
    protected void bindProvisionerService(ProvisionerService provisionerService) {
        logger.debug("ProvisionerService was bound");
        this.provisionerService = provisionerService;
    }

    /**
     * TODO What else needs to be done in the case of an unbind, it really can't function if null
     * Unbind the {@link ProvisionerService} this will leave recon in an unusable state
     */
    protected void unbindProvisionerService(ProvisionerService provisionerService) {
        logger.debug("ProvisionerService was unbound");
        this.provisionerService = provisionerService;
    }

}