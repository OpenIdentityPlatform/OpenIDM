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

import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptRegistry;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */

@Component(name = ReconciliationExecutorService.PID, immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Reconciliation Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME) })
public class ReconciliationExecutorService implements CollectionResourceProvider {

    private enum Action {
        recon, cancel;
    }

    public static final String PID = "org.forgerock.openidm.recon";

    /**
     * Setup logging for the {@link ReconciliationService}.
     */
    final static Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    private final AtomicReference<ReconExecutor> active = new AtomicReference<ReconExecutor>();

    private String factoryPid = null;

    private ServiceRegistration<CollectionResourceProvider> serviceRegistration = null;

    private ExecutorPreferences.Builder builder = null;

    private ConcurrentMap<String, ExecutorPreferences> reconciliationProcesses =
            new ConcurrentHashMap<String, ExecutorPreferences>();

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ScriptRegistry scriptRegistry;

    protected void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    protected void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    @Activate
    protected void activate(ComponentContext context) {
        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }
        init(config.getConfigurationAsJson(context));

        // ExecutorPreferences preferences =
        // ExecutorPreferences.builder().scriptRegistry(scriptRegistry).configurationProvider(
        // configurationProvider).build(context);

        Dictionary properties = context.getProperties();
        properties.put(ServerConstants.ROUTER_PREFIX, "/recon/" + factoryPid);

        serviceRegistration =
                context.getBundleContext().registerService(CollectionResourceProvider.class, this,
                        properties);

        logger.info("Reconciliation/{} service activated.", factoryPid);
    }

    @Modified
    void modified(ComponentContext context) {
        init(JSONEnhancedConfig.newInstance().getConfigurationAsJson(context));
        logger.info("Reconciliation/{} service modified.", factoryPid);
    }

    @Deactivate
    void deactivate(ComponentContext context) {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        builder = null;
        logger.info("Reconciliation/{} service deactivated.", factoryPid);
    }

    private void init(JsonValue configuration) {
        configuration.put(ServerConstants.CONFIG_FACTORY_PID, factoryPid);
        ConfigurationProvider configurationProvider =
                new ConfigurationProvider(factoryPid, configuration.get("relation"));
        configurationProvider.setTarget(configuration.get("target"));
        configurationProvider.setSource(configuration.get("source"));
        configurationProvider.setLink(configuration.get("link"));
        configurationProvider.setPolicies(configuration.get("policies"));
        builder =
                ExecutorPreferences.builder().scriptRegistry(scriptRegistry).configurationProvider(
                        configurationProvider);
    }

    String getFactoryPid() {
        return factoryPid;
    }

    /**
     * Get the the list of all reconciliations, or details of one specific recon
     * newBuilder
     * 
     * {@inheritDoc}
     */
    Map<String, Object> read(String resourceCollection) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        final ReconExecutor executor = active.get();
        if (null != executor) {
            JsonPointer pointer =
                    new JsonPointer(new String[] { resourceCollection, factoryPid,
                        executor.toString() });
            result.putAll(executor.getStatistics().asMap());
            result.put(Resource.FIELD_CONTENT_ID, pointer.toString());
        } else {
            result.put("state", "idle");
        }
        return result;
    }

    // ----- Implementation of CollectionResourceProvider interface

    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {

            // } catch (ResourceException e) {
            // handler.handleError(e);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void actionInstance(final ServerContext context, final String resourceId,
            final ActionRequest request, final ResultHandler<JsonValue> handler) {
        try {

            // } catch (ResourceException e) {
            // handler.handleError(e);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            String reconId = request.getNewResourceId();
            if (null == reconId) {
                reconId = UUID.randomUUID().toString();
            }

            if (reconciliationProcesses.containsKey(reconId)) {
                handler.handleError(new ConflictException("The reconciliation with ID '" + reconId
                        + "' could not be created because "
                        + "there is already another reconciliation with the same ID"));
                return;
            }

            // TODO Create a RECON authenticated context
            ExecutorPreferences preferences = builder.build(reconId, context);
            ExecutorPreferences previous =
                    reconciliationProcesses.putIfAbsent(reconId, preferences);
            if (null != previous) {
                handler.handleError(new ConflictException("The reconciliation with ID '" + reconId
                        + "' could not be created because "
                        + "there is already another reconciliation with the same ID"));
            } else {
                if (active.get() == null) {
                    active.compareAndSet(null, new ReconExecutor(context, preferences));
                    handler.handleResult(active.get().executeAsync(new ResultHandler<JsonValue>() {
                        @Override
                        public void handleError(final ResourceException error) {
                            // TODO update status
                            active.set(null);
                        }

                        @Override
                        public void handleResult(final JsonValue result) {
                            // TODO update status
                            active.set(null);
                        }
                    }));
                }
            }
            // } catch (ResourceException e) {
            // handler.handleError(e);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId,
            final DeleteRequest request, final ResultHandler<Resource> handler) {
        try {
            ExecutorPreferences current = reconciliationProcesses.remove(resourceId);
            if (null != context) {
                if (active.get() != null) {
                    // TODO cancel if current match
                }
                Resource resource = new Resource(resourceId, null, new JsonValue(null));
                handler.handleResult(resource);
            } else {
                handler.handleError(new NotFoundException("The reconciliation with ID '"
                        + resourceId + "' could not be deleted because it does not exist"));
            }
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, final ResultHandler<Resource> handler) {
        try {
            ExecutorPreferences current = reconciliationProcesses.remove(resourceId);
            if (null != context) {

                Map<String, Object> status = null;

                Resource resource = new Resource(resourceId, null, new JsonValue(status));
                handler.handleResult(resource);
            } else {
                handler.handleError(new NotFoundException("The reconciliation with ID '"
                        + resourceId + "' could not be read because it does not exist"));
            }
            // } catch (ResourceException e) {
            // handler.handleError(e);
        } catch (JsonValueException e) {
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    @Override
    public void patchInstance(final ServerContext context, final String resourceId,
            final PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        final ResourceException e = new NotSupportedException("Query operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    // Send report
    // private void doResults(ReconciliationContext reconContext) throws
    // ResourceException {
    // if (resultScript != null) {
    // Map<String, Object> scope = service.newScope();
    // scope.put("source",
    // reconContext.getStatistics().getSourceStat().asMap());
    // scope.put("target",
    // reconContext.getStatistics().getSourceStat().asMap());
    // scope.put("global", reconContext.getStatistics().asMap());
    // try {
    // resultScript.exec(scope);
    // } catch (ScriptException se) {
    // logger.debug("{} result script encountered exception", name, se);
    // throw new ResourceException(se);
    // }
    // }
    // }
}
