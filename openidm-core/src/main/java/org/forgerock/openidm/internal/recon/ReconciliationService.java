/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.internal.recon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciliation service implementation
 *
 * @author aegloff
 */
//@Component(name = ReconciliationService.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Reconciliation Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/recon") })
@References({ @Reference(referenceInterface = CollectionResourceProvider.class,
        bind = "bindCollectionResourceProvider", unbind = "unbindCollectionResourceProvider",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
        target = "(" + ServerConstants.ROUTER_PREFIX + "=/recon/*)") })
public class ReconciliationService implements SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.reconciliation";

    /**
     * Setup logging for the {@link ReconciliationService}.
     */
    final static Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    private void bindCollectionResourceProvider(final CollectionResourceProvider service) {
        if (service instanceof ReconciliationExecutorService) {
            ReconciliationExecutorService recon = (ReconciliationExecutorService) service;
            executorService.put(recon.getFactoryPid(), recon);
        }
    }

    private void unbindCollectionResourceProvider(final CollectionResourceProvider service) {
        if (service instanceof ReconciliationExecutorService) {
            ReconciliationExecutorService recon = (ReconciliationExecutorService) service;
            executorService.remove(recon.getFactoryPid());
        }
    }

    private final ConcurrentMap<String, ReconciliationExecutorService> executorService =
            new ConcurrentHashMap<String, ReconciliationExecutorService>();

    @Activate
    void activate(ComponentContext compContext) {
        logger.info("Reconciliation service activated.");
    }

    @Modified
    void modified(ComponentContext context) {
        logger.info("Reconciliation service modified.");
    }

    @Deactivate
    void deactivate(ComponentContext context) {
        logger.info("Reconciliation service deactivated.");
    }

    // ----- Implementation of CollectionResourceProvider interface

    @Override
    public void readInstance(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            Resource resource =
                    new Resource(request.getResourceName(), null, new JsonValue(
                            new HashMap<String, Object>()));
            List<Map<String, Object>> result =
                    new ArrayList<Map<String, Object>>(executorService.size());
            resource.getContent().put("result", result);
            for (Map.Entry<String,ReconciliationExecutorService> executor : executorService.entrySet()) {
                  result.add(executor.getValue().read(executor.getKey()));
            }
            handler.handleResult(resource);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void actionInstance(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }


    // public Map<String, Object> action(String id, Map<String, Object> params)
    // throws ResourceException {
    // Map<String, Object> result = new LinkedHashMap<String, Object>();
    //
    // JsonValue paramsVal = new JsonValue(params);
    // String action = paramsVal.get("_action").asString();
    // if (action == null) {
    // throw new
    // BadRequestException("Action parameter is not present or value is null");
    // }
    //
    // if (id == null) {
    // // operation on collection
    // if ("recon".equalsIgnoreCase(action)) {
    // try {
    // JsonValue mapping = paramsVal.get("mapping").required();
    // logger.debug("Reconciliation action of mapping {}", mapping);
    // Boolean waitForCompletion = Boolean.FALSE;
    // JsonValue waitParam =
    // paramsVal.get("waitForCompletion").defaultTo(Boolean.FALSE);
    // if (waitParam.isBoolean()) {
    // waitForCompletion = waitParam.asBoolean();
    // } else {
    // waitForCompletion = Boolean.parseBoolean(waitParam.asString());
    // }
    // result.put("_id", reconcile(mapping, waitForCompletion));
    // } catch (SynchronizationException se) {
    // throw new ConflictException(se);
    // }
    // } else {
    // throw new BadRequestException("Action " + action
    // + " on reconciliation not supported " + params);
    // }
    // } else {
    // // operation on individual resource
    // ReconciliationContext foundRun = reconRuns.get(id);
    // if (foundRun == null) {
    // throw new NotFoundException("Reconciliation with id " + id +
    // " not found.");
    // }
    //
    // if ("cancel".equalsIgnoreCase(action)) {
    // foundRun.cancel();
    // result.put("_id", foundRun.getReconId());
    // result.put("action", action);
    // result.put("status", "SUCCESS");
    // } else {
    // throw new BadRequestException("Action " + action + " on recon run " + id
    // + " not supported " + params);
    // }
    // }
    //
    // return result;
    // }
    //
    // /**
    // * Full reconciliation
    // *
    // * @param mapping
    // * the
    // * @param synchronous
    // * whether to synchrnously (TRUE) wait for the reconciliation
    // * run, or to return immediately (FALSE) with the recon id, which
    // * can then be used for subsequent queries / actions on that
    // * reconciliation run.
    // */
    // public String reconcile(final JsonValue mapping, Boolean synchronous)
    // throws SynchronizationException {
    // final ReconciliationContext reconContext = newReconContext(mapping);
    // if (Boolean.TRUE.equals(synchronous)) {
    // reconcile(mapping, reconContext);
    // } else {
    // final JsonValue threadContext = ObjectSetContext.get();
    // Runnable command = new Runnable() {
    // @Override
    // public void run() {
    // try {
    // ObjectSetContext.push(threadContext);
    // reconcile(mapping, reconContext);
    // } catch (SynchronizationException ex) {
    // logger.info("Reconciliation reported exception", ex);
    // } catch (Exception ex) {
    // logger.warn("Reconciliation failed with unexpected exception", ex);
    // }
    // }
    // };
    // fullReconExecutor.execute(command);
    // }
    // return reconContext.getReconId();
    // }
    //
    //
    // /**
    // * Start a full reconcliation run
    // *
    // * @param mapping
    // * the object mapping to reconclie
    // * @param reconContext
    // * a new reconciliation context. Do not re-use these contexts for
    // * more than one call to reconcile.
    // * @throws SynchronizationException
    // */
    // private void reconcile(JsonValue mapping, ReconciliationContext
    // reconContext)
    // throws SynchronizationException {
    // addReconRun(reconContext);
    // try {
    // reconContext.getObjectMapping().recon(reconContext); // throws
    // // SynchronizationException
    // } catch (SynchronizationException ex) {
    // if (reconContext.isCanceled()) {
    // reconContext.setStage(ReconStage.COMPLETED_CANCELED);
    // } else {
    // reconContext.setStage(ReconStage.COMPLETED_FAILED);
    // }
    // throw ex;
    // } catch (RuntimeException ex) {
    // reconContext.setStage(ReconStage.COMPLETED_FAILED);
    // throw ex;
    // }
    // reconContext.setStage(ReconStage.COMPLETED_SUCCESS);
    // }

}
