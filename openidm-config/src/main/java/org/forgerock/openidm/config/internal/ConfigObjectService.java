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

package org.forgerock.openidm.config.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.ConfigurationManager;
import org.forgerock.openidm.config.ConfigurationManager.PID;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.util.ResourceUtil.URLParser;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to OSGi configuration
 * 
 * @author aegloff
 */
//@Component(name = "org.forgerock.openidm.config", immediate = true,
//        policy = ConfigurationPolicy.IGNORE)
//@Service
//@Properties({
//    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
//    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM configuration service"),
//    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/config*") })
public class ConfigObjectService implements RequestHandler {

    /**
     * Setup logging for the {@link ConfigObjectService}.
     */
    final static Logger logger = LoggerFactory.getLogger(ConfigObjectService.class);

    /** ConfigurationManager service. */
    // @Reference
    private ConfigurationManager configurationManager = null;

    private void bindConfigurationManager(final ConfigurationManager service) {
        configurationManager = service;
    }

    private void unbindConfigurationManager(final ConfigurationManager service) {
        configurationManager = null;
    }

    public ConfigObjectService(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            URLParser url = ResourceUtil.URLParser.parse(request.getResourceName());
            if (url.value().isEmpty()) {
                // GET /config
                final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
                JsonValue content = new JsonValue(new HashMap<String, Object>());
                content.put("result", result);
                configurationManager.queryConfigurations(null, new QueryResultHandler() {
                    @Override
                    public void handleError(final ResourceException error) {
                        handler.handleError(error);
                    }

                    @Override
                    public boolean handleResource(Resource resource) {
                        result.add(resource.getContent().asMap());
                        return true;
                    }

                    @Override
                    public void handleResult(QueryResult result) {
                        /* do nothing */
                    }
                });
                handler.handleResult(new Resource("/", null, content));
            }
            URLParser serviceName = url;
            if (serviceName.next() == serviceName) {
                // GET /config/{serviceName}
                PID pid = PID.serviceName(serviceName.value());
                handler.handleResult(configurationManager.readConfiguration(pid));
            }
            URLParser instanceAlias = serviceName.next();
            if (instanceAlias.next() == instanceAlias) {
                // GET /config/{serviceName}/{instanceAlias}
                PID pid = PID.serviceName(serviceName.value(), instanceAlias.value());
                handler.handleResult(configurationManager.readConfiguration(pid));
            } else {
                handler.handleError(new NotFoundException("Resource not found: "
                        + request.getResourceName()));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            URLParser serviceName = ResourceUtil.URLParser.parse(request.getResourceName());
            PID pid = null;
            if (serviceName.next() == serviceName) {
                if (serviceName.value().isEmpty() && null == request.getNewResourceId()) {
                    // POST /config or is BadRequestException
                    handler.handleError(new BadRequestException(
                            "The singleton resource '"+request.getResourceName()+"' cannot be created"));
                    return;
                } else if (serviceName.value().isEmpty()){
                    // PUT /config/{newResourceId} is OK
                    pid = PID.serviceName(request.getNewResourceId());
                } else {
                    if (null != request.getNewResourceId()) {
                        // PUT /config/{serviceName}/{newResourceId} is OK
                        pid = PID.serviceName(serviceName.value(), request.getNewResourceId());
                    } else {
                        // POST /config/{serviceName} is OK
                        pid = PID.serviceName(serviceName.value(), (String)null);
                    }
                }
            } else {
                // PUT /config/{serviceName}/{instanceAlias}/{newResourceId} is KO
                // POST /config/{serviceName}/{instanceAlias} is KO
                handler.handleError(new BadRequestException(
                        "The singleton resource '"+request.getResourceName()+"' cannot be created"));
                return;
            }
            handler.handleResult(configurationManager
                    .createConfiguration(pid, request.getContent()));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            URLParser url = ResourceUtil.URLParser.parse(request.getResourceName());
            if (url.value().isEmpty()) {
                // PUT /config
                handler.handleError(new BadRequestException(
                        "The singleton resource '/' cannot be updated"));
            }
            URLParser serviceName = url.next();
            if (serviceName.next() == serviceName) {
                // PUT /config/{serviceName}
                PID pid = PID.serviceName(serviceName.value());
                handler.handleResult(configurationManager.updateConfiguration(pid, request
                        .getRevision(), request.getNewContent()));
            }
            URLParser instanceAlias = serviceName.next();
            if (instanceAlias.next() == instanceAlias) {
                // PUT /config/{serviceName}/{instanceAlias}
                PID pid = PID.serviceName(serviceName.value(), instanceAlias.value());
                handler.handleResult(configurationManager.updateConfiguration(pid, request
                        .getRevision(), request.getNewContent()));
            } else {
                handler.handleError(new NotFoundException("Resource not found: "
                        + request.getResourceName()));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            if (null != request.getQueryExpression()) {
                handler.handleError(new BadRequestException("Query expression is not supported"));
                return;
            }

            if (null != request.getQueryFilter() ^ null != request.getQueryId()) {
                QueryFilter queryFilter = null;
                if (null != request.getQueryId()) {
                    if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {

                    } else {
                        handler.handleError(new BadRequestException("Not supported queryId:"
                                + request.getQueryId()));
                    }
                } else {
                    queryFilter = request.getQueryFilter();
                }
                URLParser url = ResourceUtil.URLParser.parse(request.getResourceName());
                if (url.value().isEmpty()) {
                    configurationManager.queryConfigurations(queryFilter, handler);
                } else {
                    handler.handleError(new BadRequestException("The singleton resource '"
                            + request.getResourceName() + "' cannot be queried"));
                }
            } else {
                handler.handleError(new BadRequestException(
                        "QueryId or Filter is mutually exclusive."));
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            URLParser serviceName = ResourceUtil.URLParser.parse(request.getResourceName());
            if (serviceName.value().isEmpty()) {
                // DELETE /config
                handler.handleError(new BadRequestException(
                        "The singleton resource '/' cannot be deleted"));
            } else if (serviceName.next() == serviceName) {
                // DELETE /config/{serviceName}
                PID pid = PID.serviceName(serviceName.value());
                handler.handleResult(configurationManager.deleteConfiguration(pid, request
                        .getRevision()));
            } else {
                URLParser instanceAlias = serviceName.next();
                if (instanceAlias.next() == instanceAlias) {
                    // DELETE /config/{serviceName}/{instanceAlias}
                    PID pid = PID.serviceName(serviceName.value(), instanceAlias.value());
                    handler.handleResult(configurationManager.deleteConfiguration(pid, request
                            .getRevision()));
                } else {
                    handler.handleError(new NotFoundException("Resource not found: "
                            + request.getResourceName()));
                }
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }
}
