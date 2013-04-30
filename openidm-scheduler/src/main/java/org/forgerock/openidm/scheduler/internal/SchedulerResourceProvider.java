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

package org.forgerock.openidm.scheduler.internal.metadata;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestVisitor;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.ContextUtil;
import org.quartz.Calendar;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.calendar.CronCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class SchedulerResourceProvider implements CollectionResourceProvider/*,  RequestVisitor<Resource, SchedulerResourceProvider.Type>*/{

    /**
     * Setup logging for the {@link SchedulerResourceProvider}.
     */
    final static Logger logger = LoggerFactory.getLogger(SchedulerResourceProvider.class);

    private final Scheduler scheduler;

    private enum Type {
        jobs {
            @Override
            protected Resource create(CreateRequest request, String resourceType, String newResourceId, ResultHandler<Resource> handler) throws Exception {
                //TODO Parse the calendar Json
                CronCalendar calendar =  new CronCalendar("* * 0-7,18-23 ? * *");

                //TODO Catch the already exists exceptions
                SchedulerResourceProvider.this.scheduler.addCalendar(newResourceId, calendar, false, false);
                JsonValue content = new JsonValue(new HashMap<String, Object>(2));
                content.put(Resource.FIELD_CONTENT_ID, newResourceId);
                return new Resource(newResourceId, null, content);
            }
        },
        triggers {
            @Override
            protected Resource create(CreateRequest request, String resourceType, String newResourceId, ResultHandler<Resource> handler) throws Exception {
                Trigger trigger = newTrigger().withIdentity(newResourceId, resourceType).
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        },
        calendars {
            @Override
            protected Resource create(CreateRequest request, String resourceType, String newResourceId, ResultHandler<Resource> handler) throws Exception {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        protected abstract Resource create(CreateRequest request, String resourceType, String newResourceId, ResultHandler<Resource> handler) throws Exception;

        public Resource create(ServerContext context, Map<String, String> uriVariables, CreateRequest request,
                               ResultHandler<Resource> handler) {

            String name = StringUtils.isNotBlank(request.getNewResourceId()) ? request.getNewResourceId() : UUID.randomUUID().toString();
            String group = null;


            try {
                return this.create();
            } catch (Exception e) {
                handler.handleError(null);
            }
        };

        @Override
        public Type visitActionRequest(Object o, ActionRequest request) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


    }


    /**
     * Performs the provided
     * {@link org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ActionRequest, org.forgerock.json.resource.ResultHandler)
     * action} against the resource collection.
     * 
     * @param context
     *            The request server context.
     * @param request
     *            The action request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ActionRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void actionCollection(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            Map<String, String> uriVariables = ContextUtil.getUriTemplateVariables(context);
            Type type = getType(uriVariables);


        } catch (ResourceException e) {
           handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    private Type getType(Map<String, String> uriVariables) throws ResourceException{
        if (null == uriVariables) {
           throw new ForbiddenException("Direct access without Router to this service is forbidden.");
        }
        try {
            String type = uriVariables.get("type");
            return Type.valueOf(type);
//                switch (t) {
//                    case calendars:
//                        break;
//                    case jobs:
//                        break;
//                    case triggers:
//                        break;
//                }
        } catch (Exception e) {
            //Catch IllegalArgumentException and NullPointerException
            throw new BadRequestException("Not supported type: ",e);
        }

    }

    /**
     * Performs the provided
     * {@link org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ActionRequest, org.forgerock.json.resource.ResultHandler)
     * action} against a resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The action request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ActionRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleCreate(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.CreateRequest, org.forgerock.json.resource.ResultHandler)
     * Adds} a new resource newBuilder to the collection.
     * <p/>
     * Create requests are targeted at the collection itself and may include a
     * user-provided resource ID for the new resource as part of the request
     * itself. The user-provider resource ID may be accessed using the method
     * {@link org.forgerock.json.resource.CreateRequest#getNewResourceId()}.
     * 
     * @param context
     *            The request server context.
     * @param request
     *            The create request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleCreate(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.CreateRequest,
     *      org.forgerock.json.resource.ResultHandler)
     * @see org.forgerock.json.resource.CreateRequest#getNewResourceId()
     */
    public void createInstance(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            Map<String, String> uriVariables = ContextUtil.getUriTemplateVariables(context);
            Type type = getType(uriVariables);



        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleDelete(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.DeleteRequest, org.forgerock.json.resource.ResultHandler)
     * Removes} a resource newBuilder from the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The delete request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleDelete(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.DeleteRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handlePatch(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.PatchRequest, org.forgerock.json.resource.ResultHandler)
     * Patches} an existing resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The patch request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handlePatch(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.PatchRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleQuery(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.QueryRequest, org.forgerock.json.resource.QueryResultHandler)
     * Searches} the collection for all resources which match the query request
     * criteria.
     * 
     * @param context
     *            The request server context.
     * @param request
     *            The query request.
     * @param handler
     *            The query result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleQuery(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.QueryRequest,
     *      org.forgerock.json.resource.QueryResultHandler)
     */
    public void queryCollection(ServerContext context, QueryRequest request,
            QueryResultHandler handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleRead(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ReadRequest, org.forgerock.json.resource.ResultHandler)
     * Reads} an existing resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The read request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleRead(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ReadRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleUpdate(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.UpdateRequest, org.forgerock.json.resource.ResultHandler)
     * Updates} an existing resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The update request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleUpdate(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.UpdateRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }
}
