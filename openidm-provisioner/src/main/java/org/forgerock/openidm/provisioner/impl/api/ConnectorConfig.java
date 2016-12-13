/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.impl.api;

import javax.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * ICF connector configuration.
 */
@Title("Connector Config")
public class ConnectorConfig {

    private String name;
    private ConnectorRef connectorRef;
    private int producerBufferSize;
    private boolean connectorPoolingSupported;
    private PoolConfigOption poolConfigOption;
    private ResultsHandlerConfig resultsHandlerConfig;
    private OperationTimeout operationTimeout;
    private Map<String, Object> configurationProperties;
    private SyncFailureHandler syncFailureHandler;
    private Map<String, Object> objectTypes;
    private OperationOptions operationOptions;
    private List<SystemAction> systemActions;

    /**
     * Gets unique connector name.
     *
     * @return Unique connector name
     */
    @Description("Unique connector name")
    public String getName() {
        return name;
    }

    /**
     * Sets unique connector name.
     *
     * @param name Unique connector name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets connector identifiers.
     *
     * @return Connector identifiers
     */
    @Description("Connector identifiers")
    @NotNull
    public ConnectorRef getConnectorRef() {
        return connectorRef;
    }

    /**
     * Sets connector identifiers.
     *
     * @param connectorRef Connector identifiers
     */
    public void setConnectorRef(ConnectorRef connectorRef) {
        this.connectorRef = connectorRef;
    }

    /**
     * Gets size of SYNC/QUERY result-queue.
     *
     * @return Size of SYNC/QUERY result-queue
     */
    @Description("Size of SYNC/QUERY result-queue")
    @Default("100")
    public int getProducerBufferSize() {
        return producerBufferSize;
    }

    /**
     * Sets size of SYNC/QUERY result-queue.
     *
     * @param producerBufferSize Size of SYNC/QUERY result-queue
     */
    public void setProducerBufferSize(int producerBufferSize) {
        this.producerBufferSize = producerBufferSize;
    }

    /**
     * Gets connector-pooling flag.
     *
     * @return Enables connector pooling, when supported
     */
    @Description("Enables connector pooling, when supported")
    @Default("true")
    public boolean isConnectorPoolingSupported() {
        return connectorPoolingSupported;
    }

    /**
     * Sets connector-pooling flag.
     *
     * @param connectorPoolingSupported Enables connector pooling, when supported
     */
    public void setConnectorPoolingSupported(boolean connectorPoolingSupported) {
        this.connectorPoolingSupported = connectorPoolingSupported;
    }

    /**
     * Gets connector pool configuration.
     *
     * @return Connector pool configuration
     */
    @Description("Connector pool configuration")
    @NotNull
    public PoolConfigOption getPoolConfigOption() {
        return poolConfigOption;
    }

    /**
     * Sets connector pool configuration.
     *
     * @param poolConfigOption Connector pool configuration
     */
    public void setPoolConfigOption(PoolConfigOption poolConfigOption) {
        this.poolConfigOption = poolConfigOption;
    }

    /**
     * Gets results handler configuration.
     *
     * @return Results handler configuration
     */
    @Description("Results handler configuration")
    public ResultsHandlerConfig getResultsHandlerConfig() {
        return resultsHandlerConfig;
    }

    /**
     * Sets results handler configuration.
     *
     * @param resultsHandlerConfig Results handler configuration
     */
    public void setResultsHandlerConfig(ResultsHandlerConfig resultsHandlerConfig) {
        this.resultsHandlerConfig = resultsHandlerConfig;
    }

    /**
     * Gets timeouts per operation.
     *
     * @return Timeouts per operation
     */
    @Description("Timeouts per operation")
    public OperationTimeout getOperationTimeout() {
        return operationTimeout;
    }

    /**
     * Sets timeouts per operation.
     *
     * @param operationTimeout Timeouts per operation
     */
    public void setOperationTimeout(OperationTimeout operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    /**
     * Gets connector specific configuration.
     *
     * @return Connector specific configuration
     */
    @Description("Connector specific configuration")
    @NotNull
    public Map<String, Object> getConfigurationProperties() {
        return configurationProperties;
    }

    /**
     * Sets connector specific configuration.
     *
     * @param configurationProperties Connector specific configuration
     */
    public void setConfigurationProperties(Map<String, Object> configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    /**
     * Gets live-sync failures config.
     *
     * @return Configures how live-sync failures are handled
     */
    @Description("Configures how live-sync failures are handled")
    public SyncFailureHandler getSyncFailureHandler() {
        return syncFailureHandler;
    }

    /**
     * Sets live-sync failures config.
     *
     * @param syncFailureHandler Configures how live-sync failures are handled
     */
    public void setSyncFailureHandler(SyncFailureHandler syncFailureHandler) {
        this.syncFailureHandler = syncFailureHandler;
    }

    /**
     * Gets map of object-classes to JSON Schemas.
     *
     * @return Map of object-classes to JSON Schemas
     */
    @Description("Map of object-classes to JSON Schemas")
    public Map<String, Object> getObjectTypes() {
        return objectTypes;
    }

    /**
     * Sets map of object-classes to JSON Schemas.
     *
     * @param objectTypes Map of object-classes to JSON Schemas
     */
    public void setObjectTypes(Map<String, Object> objectTypes) {
        this.objectTypes = objectTypes;
    }

    /**
     * Gets configuration settings per operation.
     *
     * @return Configuration settings per operation
     */
    @Description("Configuration settings per operation")
    public OperationOptions getOperationOptions() {
        return operationOptions;
    }

    /**
     * Sets configuration settings per operation.
     *
     * @param operationOptions Configuration settings per operation
     */
    public void setOperationOptions(OperationOptions operationOptions) {
        this.operationOptions = operationOptions;
    }

    /**
     * Gets connector actions (e.g., scripts).
     *
     * @return Connector actions (e.g., scripts)
     */
    @Description("Connector actions (e.g., scripts)")
    public List<SystemAction> getSystemActions() {
        return systemActions;
    }

    /**
     * Sets connector actions (e.g., scripts).
     *
     * @param systemActions Connector actions (e.g., scripts)
     */
    public void setSystemActions(List<SystemAction> systemActions) {
        this.systemActions = systemActions;
    }

}
