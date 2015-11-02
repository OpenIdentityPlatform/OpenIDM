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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.datasource.jdbc.impl;

import static com.fasterxml.jackson.core.Version.unknownVersion;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.datasource.DataSourceService;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DataSource Manager for JDBC DataSources.
 *
 * This service exposes a DataSourceService for based on JDBC DataSource configuration.  DataSources through
 * JDNI, OSGi service-registration, non-pooled connections, and BoneCP are supported.
 */
@Component(name = JDBCDataSourceService.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE,
        configurationFactory = true, enabled = true)
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "DataSource Service using JDBC"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME)
})
public class JDBCDataSourceService implements DataSourceService {
    public static final String PID = "org.forgerock.openidm.datasource.jdbc";

    private static final Logger logger = LoggerFactory.getLogger(JDBCDataSourceService.class);

    private JsonValue config;

    private DataSourceConfig dataSourceConfig;
    private DataSourceFactory dataSourceFactory;
    private DataSource dataSource;

    /**
     * Enhanced configuration service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    /**
     * Populate and return a DataSourceService that can be used before the config service is available.
     *
     * @param config the bootstrap configuration
     * @param context the bundle context
     * @return the boot repository service. This newBuilder is not managed by
     *         SCR and needs to be manually registered.
     */
    public static DataSourceService getBootService(JsonValue config, BundleContext context) {
        JDBCDataSourceService bootService = new JDBCDataSourceService();
        bootService.init(config, context);
        return bootService;
    }

    /**
     * A custom JsonDeserializer to implement the polymorphic deserialization from datasource config
     * to DataSourceConfig objects.  It uses the presence of specific attributes (keys in the JSON config)
     * to identity a particular class type.  This avoids the need for complex naming and Jackson annotations.
     */
    private static final JsonDeserializer<DataSourceConfig> DESERIALZER = new JsonDeserializer<DataSourceConfig>() {
        @Override
        public DataSourceConfig deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final ObjectNode node = mapper.readTree(jsonParser);
            final Iterable<Map.Entry<String, JsonNode>> fields = new Iterable<Map.Entry<String, JsonNode>>() {
                @Override
                public Iterator<Map.Entry<String, JsonNode>> iterator() {
                    return node.fields();
                }
            };
            for (Map.Entry<String, JsonNode> element : fields) {
                final String key = element.getKey();
                if ("jndiName".equals(key)) {
                    return mapper.treeToValue(node, JndiDataSourceConfig.class);
                } else if ("osgiName".equals(key)) {
                    return mapper.treeToValue(node, OsgiDataSourceConfig.class);
                } else if ("connectionPool".equals(key)) {
                    final String type = element.getValue().get("type").asText();
                    if ("bonecp".equals(type)) {
                        return mapper.treeToValue(node, BoneCPDataSourceConfig.class);
                    }
                    // implement other types of pooling configs here
                }
                // implement other self-contained, non-connectionPool configs here
            }
            // no special keys ~ non-pooling data source that returns a new connection when asked
            return mapper.treeToValue(node, NonPoolingDataSourceConfig.class);
        }
    };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(
                        new SimpleModule("DataSourceConfigModule", unknownVersion())
                                .addDeserializer(DataSourceConfig.class, DESERIALZER));
    }

    private static DataSourceConfig parseJson(JsonValue config) {
        return OBJECT_MAPPER.convertValue(config.getObject(), DataSourceConfig.class);
    }

    /**
     * Initializes the JDBC Connection Service with the supplied configuration
     *
     * @param config the configuration object
     * @param bundleContext the bundle context
     */
    private void init(JsonValue config, final BundleContext bundleContext) {
        dataSourceConfig = parseJson(config);
        dataSourceFactory = dataSourceConfig.accept(new DataSourceFactoryConfigVisitor(bundleContext), null);
        dataSource = dataSourceFactory.newInstance();
    }

    /**
     * Activates the JDBC Repository Service
     *
     * @param compContext the component context
     */
    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating ConnectionManager Service with configuration {}", compContext.getProperties());
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
            init(config, compContext.getBundleContext());
            logger.info("JDBCConnectionManager started.");
        } catch (RuntimeException e) {
            logger.warn("Configuration invalid and could not be parsed, can not start JDBC repository: ", e);
            throw e;
        }
    }

    /**
     * Deactivates the JDBC Repository Service
     *
     * @param compContext the component context
     */
    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        shutdown();
        logger.info("Repository stopped.");
    }

    /**
     * Handles configuration updates without interrupting the service
     *
     * @param compContext the component context
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Reconfiguring the JDBC Repository Service with configuration {}", compContext.getProperties());
        try {
            JsonValue newConfig = enhancedConfig.getConfigurationAsJson(compContext);
            if (hasConfigChanged(config, newConfig)) {
                deactivate(compContext);
                activate(compContext);
                logger.info("Reconfigured the JDBC Repository Service {}", compContext.getProperties());
            }
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure the JDBC Repository Service.", ex);
            throw ex;
        }
    }

    /**
     * Compares the current configuration with a new configuration to determine
     * if the configuration has changed
     *
     * @param existingConfig the current configuration object
     * @param newConfig the new configuration object
     * @return true if the configurations differ, false otherwise
     */
    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return JsonPatch.diff(existingConfig, newConfig).size() > 0;
    }

    @Override
    public String getDatabaseName() {
        return dataSourceConfig.getDatabaseName();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void shutdown() {
        dataSourceFactory.shutdown(dataSource);
    }
}
