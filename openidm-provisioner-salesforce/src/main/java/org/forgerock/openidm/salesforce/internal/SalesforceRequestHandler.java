/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.salesforce.internal.async.AbstractAsyncResourceProvider;
import org.forgerock.openidm.salesforce.internal.async.AsyncBatchResourceProvider;
import org.forgerock.openidm.salesforce.internal.async.AsyncBatchResultResourceProvider;
import org.forgerock.openidm.salesforce.internal.async.AsyncJobResourceProvider;
import org.forgerock.openidm.salesforce.internal.data.GenericResourceProvider;
import org.forgerock.openidm.salesforce.internal.data.QueryResourceProvider;
import org.forgerock.openidm.salesforce.internal.data.SObjectsResourceProvider;
import org.forgerock.openidm.salesforce.internal.metadata.MetadataResourceProvider;
import org.forgerock.openidm.sync.SynchronizationListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
@Component(name = SalesforceRequestHandler.PID, immediate = true,
        policy = ConfigurationPolicy.REQUIRE)
@Service(value = { ProvisionerService.class })
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce sample") })
public class SalesforceRequestHandler implements ProvisionerService {

    public static final String PID = "org.forgerock.openidm.salesforce";

    /**
     * Setup logging for the {@link SalesforceRequestHandler}.
     */
    final static Logger logger = LoggerFactory.getLogger(SalesforceRequestHandler.class);

    private Map<Pattern, JsonResource> routes = new HashMap<Pattern, JsonResource>();

    private SalesforceConnection connection = null;

    private String organizationName = null;

    @Activate
    void activate(ComponentContext context) throws Exception {
        JsonValue configuration = null;
        try {
            configuration = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
        } catch (RuntimeException ex) {
            logger.warn(
                    "Configuration invalid and could not be parsed, can not start Salesforce Connector: "
                    + ex.getMessage(), ex);
            throw ex;
        }

        Object o = context.getProperties().get("config.factory-pid");
        if (null == o || (!(o instanceof String)) || StringUtils.isBlank((String)o)) {
            logger.warn("Configuration invalid, factory configuration expected");
            throw new ComponentException("Factory configuration expected");
        }
        organizationName = (String)o;

        SalesforceConfiguration config = null;
        try {
         config = parseConfiguration(configuration
                .get("configurationProperties"));
        } catch (Exception ex) {
            //Invalid configuration value, permanent error.
            logger.warn(
                    "Configuration invalid and could not be used, can not start Salesforce Connector: "
                            + ex.getMessage(), ex);
            throw ex;
        }
        if (!configuration.get("enabled").defaultTo(true).asBoolean()) {
            logger.info("Salesforce Connector {} is disabled, \"enabled\" set to false in configuration", organizationName);
            return;
        }

        connection = new SalesforceConnection(parseConfiguration(configuration.get("configurationProperties")));
        connection.test();

        try {
            connection = new SalesforceConnection(config);
            connection.test();
        } catch (ResourceException e){
            //authenticate or the test throws this, temporary error
            logger.warn("TODO define message that explains that the service is temporary not available now, can not start Salesforce Connector: ");
        } catch (Exception ex) {
            //Invalid configuration value, permanent error.
            logger.warn(
                    "Configuration invalid and could not be validated, can not start Salesforce Connector: "
                            + ex.getMessage(), ex);
            throw ex;
        }

        AbstractAsyncResourceProvider async = new AsyncJobResourceProvider(connection);

        routes.put(Pattern.compile("(\\Qasync/job\\E)"), async);
        routes.put(Pattern.compile("\\Qasync/job/\\E(([^/]+))"), async);

        async = new AsyncBatchResourceProvider(connection);

        routes.put(Pattern.compile("\\Qasync/job/\\E(([^/]+))\\Q/batch\\E"), async);
        routes.put(Pattern.compile("\\Qasync/job/\\E([^/]+)\\Q/batch/\\E(([^/]+))"), async);

        async = new AsyncBatchResultResourceProvider(connection);

        routes.put(Pattern.compile("\\Qasync/job/\\E([^/]+)\\Q/batch/\\E(([^/]+))\\Q/result\\E"),
                async);
        routes.put(Pattern
                .compile("\\Qasync/job/\\E([^/]+)\\Q/batch/\\E([^/]+)\\Q/result/\\E(([^/]+))"),
                async);

        SObjectsResourceProvider data = new SObjectsResourceProvider(connection);

        routes.put(Pattern.compile("\\Qsobjects/\\E(([^/])+)"), data);
        routes.put(Pattern.compile("\\Qsobjects/\\E([^/]+)/(([^/]+))"), data);

        routes.put(Pattern.compile("(licensing|connect|search|tooling|chatter|recent)((\\/.*)+)"),
                new GenericResourceProvider(connection));

        routes.put(Pattern.compile("query"), new QueryResourceProvider(connection));


        MetadataResourceProvider meta = new MetadataResourceProvider(connection);
        routes.put(Pattern.compile("\\Qmetadata/\\E(([^/])+)"), meta);
        routes.put(Pattern.compile("\\Qmetadata/\\E([^/]+)/(([^/]+))"), meta);

    }

    @Deactivate
    void deactivate(ComponentContext context) throws Exception {
        routes.clear();
        if (null != connection) {
            connection.dispose();
            connection = null;
        }
    }

    public SalesforceConfiguration parseConfiguration(JsonValue config) {
        return SalesforceConnection.mapper.convertValue(
                config.required().expect(Map.class).asMap(), SalesforceConfiguration.class);
    }

    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        String id = request.get("id").required().asString();
        if (id.endsWith("/")) {
            id = id.substring(organizationName.length() + 1, id.length() - 1);
        } else {
            id = id.substring(organizationName.length() + 1);
        }
        for (Map.Entry<Pattern, JsonResource> entry : routes.entrySet()) {
            Matcher matcher = entry.getKey().matcher(id);
            if (matcher.matches()) {
                ServerContext.build(matcher);
                try {
                    return entry.getValue().handle(request);
                } finally {
                    ServerContext.clear();
                }
            }
        }
        throw new JsonResourceException(JsonResourceException.NOT_FOUND, "Route not found: " + id);
    }

    @Override
    public SystemIdentifier getSystemIdentifier() {
        return new SystemIdentifier() {
            @Override
            public boolean is(SystemIdentifier other) {
                return false;
            }

            @Override
            public boolean is(Id id) {
                return organizationName.equals(id.getSystemName());
            }
        };
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        try {
            result.put("name", organizationName);
            JsonValue request = new JsonValue(new HashMap<String, Object>(2));
            request.put("id", organizationName + "/connect/organization");
            request.put("method", "read");
            handle(request);
            result.put("ok", true);
        } catch (Throwable e) {
            result.put("error", e.getMessage());
            result.put("ok", false);
        }
        return result;
    }

    @Override
    public Map<String, Object> testConfig(JsonValue config) {
        Map<String, Object> jv = new LinkedHashMap<String, Object>();
        jv.put("name", organizationName);
        try {
            parseConfiguration(config.get("configurationProperties")).validate();
            jv.put("ok", true);
        } catch (Exception e) {
            e.printStackTrace();
            jv.put("ok", false);
            jv.put("error", e.getMessage());
        }
        return jv;
    }

    @Override
    public JsonValue liveSynchronize(String objectType, JsonValue previousStage,
            SynchronizationListener synchronizationListener) throws JsonResourceException {
        return previousStage;
    }
}
