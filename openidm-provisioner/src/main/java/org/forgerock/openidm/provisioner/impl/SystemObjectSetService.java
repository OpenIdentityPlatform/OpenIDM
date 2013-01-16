/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.impl;

import org.apache.felix.scr.annotations.*;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ConfigurationService;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * SystemObjectSetService implement the {@link SingletonResourceProvider}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner", immediate = true, policy = ConfigurationPolicy.IGNORE, description = "OpenIDM System Object Set Service")
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = "/system")
})
public class SystemObjectSetService implements SingletonResourceProvider {
    private final static Logger TRACE = LoggerFactory.getLogger(SystemObjectSetService.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private ConfigurationService configurationService;


    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        if ("CREATECONFIGURATION".equalsIgnoreCase(request.getActionId())) {
            if (null != configurationService) {
            try {
                handler.handleResult(configurationService.configure(request.getContent()));
            } catch (ResourceException e) {
                handler.handleError(e);
            } catch (Exception e) {
                handler.handleError(new InternalServerErrorException(e));
            } } else {
                handler.handleError(new ServiceUnavailableException("The required service is not available"));
            }
        }   else {
            handler.handleError(new BadRequestException("Unsupported actionId: " + request.getActionId()));
        }
    }



    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Read are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Patch are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update are not supported for resource instances");
        handler.handleError(e);
    }


    /**
     * Called when a source object has been created.
     *
     * @param id    the fully-qualified identifier of the object that was created.
     * @param value the value of the object that was created.
     * @throws ResourceException
     *          if an exception occurs processing the notification.
     */
    public void onCreate(ServerContext context, String id, JsonValue value) throws ResourceException {
        ActionRequest request = Requests.newActionRequest("sync", "ONCREATE");
        request.setAdditionalActionParameter("id", id);
        request.setContent(value);
        context.getConnection().action(context, request);
    }

    /**
     * Called when a source object has been updated.
     *
     * @param id       the fully-qualified identifier of the object that was updated.
     * @param oldValue the old value of the object prior to the update.
     * @param newValue the new value of the object after the update.
     * @throws ResourceException
     *          if an exception occurs processing the notification.
     */
    public void onUpdate(ServerContext context, String id, JsonValue oldValue, JsonValue newValue)
            throws ResourceException {
        ActionRequest request = Requests.newActionRequest("sync", "ONUPDATE");
        request.setAdditionalActionParameter("id", id);
        request.setContent(newValue);
        context.getConnection().action(context, request);
    }

    /**
     * Called when a source object has been deleted.
     *
     * @param id the fully-qualified identifier of the object that was deleted.
     * @param oldValue the value before the delete, or null if not supplied
     * @throws ResourceException
     *          if an exception occurs processing the notification.
     */
    public void onDelete(ServerContext context, String id, JsonValue oldValue) throws ResourceException {
        ActionRequest request = Requests.newActionRequest("sync", "ONDELETE");
        request.setAdditionalActionParameter("id", id);
        request.setContent(oldValue);
        context.getConnection().action(context, request);
    }

    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param schedulerContext Context information passed by the scheduler service
     * @throws org.forgerock.openidm.quartz.impl.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
//    public void execute(Map<String, Object> schedulerContext) throws ExecutionException {
//        try {
//            JsonValue params = new JsonValue(schedulerContext).get(CONFIGURED_INVOKE_CONTEXT);
//            String action = params.get("action").asString();
//            if ("liveSync".equals(action) || "activeSync".equals(action)) {
//                Id id = new Id(params.get("source").asString());
//                String previousStageId = "repo/synchronisation/pooledSyncStage/" + id.toString().replace("/", "").toUpperCase();
//                try {
//                    JsonValue previousStage = null;
//                    try {
//                        JsonValue readRequest = new JsonValue(new HashMap());
//                        readRequest.put("type", "resource");
//                        readRequest.put("method", "read");
//                        readRequest.put("id", previousStageId);
//                        previousStage = router.handle(readRequest);
//
//                        JsonValue updateRequest = new JsonValue(new HashMap());
//                        updateRequest.put("type", "resource");
//                        updateRequest.put("method", "update");
//                        updateRequest.put("id", previousStageId);
//                        updateRequest.put("rev", previousStage.get("_rev"));
//                        updateRequest.put("value", locateService(id).liveSynchronize(id.getObjectType(), previousStage != null ? previousStage : null, this).asMap());
//                        router.handle(updateRequest);
//                    } catch (JsonResourceException e) {
//                        if (null == previousStage) {
//                            TRACE.info("PooledSyncStage object {} is not found. First execution.");
//                            JsonValue createRequest = new JsonValue(new HashMap());
//                            createRequest.put("type", "resource");
//                            createRequest.put("method", "create");
//                            createRequest.put("id", previousStageId);
//                            createRequest.put("value", locateService(id).liveSynchronize(id.getObjectType(), null, this).asMap());
//                            router.handle(createRequest);
//                        }
//                    }
//                } catch (JsonResourceException e) {
//                    throw new ExecutionException(e);
//                }
//            }
//        } catch (JsonValueException jve) {
//            throw new ExecutionException(jve);
//        } catch (JsonResourceException e) {
//            throw new ExecutionException(e);
//        }
//    }
}
