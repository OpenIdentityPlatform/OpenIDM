/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openidm.provisioner.salesforce.internal;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.provisioner.ConnectorConfigurationHelper;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * ConnectorConfigurationHelper for Salesforce connector.
 */
@Component(name = SalesforceConfigurationHelper.PID,
        policy = ConfigurationPolicy.OPTIONAL,
        metatype = true,
        description = "Salesforce Connector Configuration Helper",
        immediate = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce Connector Configuration Helper") })
public class SalesforceConfigurationHelper implements MetaDataProvider, ConnectorConfigurationHelper {

    public static final String PID = "org.forgerock.openidm.provisioner.salesforce.confighelper";

    private final MetaDataProviderCallback[] callback = new MetaDataProviderCallback[1];

    private JsonValue connectorData = json(object());

    private JsonValue configurationProperties = json(object());

    private static final List<JsonPointer> PROPERTIES_TO_ENCRYPT = Arrays.asList(
            new JsonPointer("/" + CONFIGURATION_PROPERTIES + "/clientSecret"),
            new JsonPointer("/" + CONFIGURATION_PROPERTIES + "/refreshToken"));

    @Activate
    void activate(ComponentContext context) throws Exception {
        connectorData = SalesforceConnectorInfo.getConnectorInfo(context.getBundleContext().getBundle());
        configurationProperties = json(object(
                field("connectTimeout" , 120000),
                field("loginUrl", ""),
                field("idleCheckInterval", 10000),
                field("refreshToken", ""),
                field("clientSecret", ""),
                field("clientId", ""),
                field("instanceUrl", ""),
                field("version", 29)
        ));
    }

    @Deactivate
    void deactivate(ComponentContext context) throws Exception {
        connectorData = json(object());
        configurationProperties = json(object());
    }

    /**
     * {@inheritDoc}
     */
    public String getProvisionerType() {
        return "salesforce";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void test(JsonValue params) throws ResourceException {
        SalesforceConfiguration config = SalesforceConnection.mapper.convertValue(
                params.get(CONFIGURATION_PROPERTIES).required().expect(Map.class).asMap(),
                SalesforceConfiguration.class);
        SalesforceConnection connection = new SalesforceConnection(config);
        connection.test();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue getAvailableConnectors() throws ResourceException {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(1);
        result.add(connectorData.asMap());
        return json(object(field(CONNECTOR_REF, Collections.unmodifiableList(result))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue generateConnectorCoreConfig(JsonValue params) throws ResourceException {
        JsonValue coreConfig = params.copy();
        coreConfig.put(CONFIGURATION_PROPERTIES, configurationProperties.getObject());
        return coreConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonValue generateConnectorFullConfig(JsonValue params) throws ResourceException {
        // TODO More implementation?
        return params;
    }

    // --- MetaDataProvider implementation

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config) throws WaitForMetaData, NotConfiguration {
        return PROPERTIES_TO_ENCRYPT;
    }

    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        this.callback[0] = callback;
    }
}
